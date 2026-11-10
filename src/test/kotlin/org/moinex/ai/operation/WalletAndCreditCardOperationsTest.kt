package org.moinex.ai.operation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.moinex.ai.operation.creditcard.CreateCreditCardDebtOperation
import org.moinex.ai.operation.creditcard.CreditCardDebtPayload
import org.moinex.ai.operation.creditcard.CreditCardInvoicePaymentPayload
import org.moinex.ai.operation.creditcard.PayCreditCardInvoiceOperation
import org.moinex.ai.operation.transactions.CreateWalletTransactionOperation
import org.moinex.ai.operation.transactions.WalletTransactionPayload
import org.moinex.common.constant.TranslationKeys
import org.moinex.factory.CategoryFactory
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.model.dto.CreditCardInvoicePaymentDTO
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class WalletAndCreditCardOperationsTest :
    BehaviorSpec({
        val walletService = mockk<WalletService>()
        val creditCardService = mockk<CreditCardService>()
        val categoryResolver = mockk<CategoryResolver>()
        val preferencesService = mockk<PreferencesService>()

        val wallet = WalletFactory.create(id = 1, name = "Bank")
        val groceries = CategoryFactory.create(id = 3, name = "Groceries")
        val billing = WalletFactory.create(id = 2, name = "Billing")
        val card = CreditCardFactory.create(id = 7, name = "Card", defaultBillingWallet = billing)
        val cardWithoutBilling = CreditCardFactory.create(id = 8, name = "NoBilling")

        val walletOperation = CreateWalletTransactionOperation(walletService, categoryResolver, preferencesService)
        val debtOperation = CreateCreditCardDebtOperation(creditCardService, categoryResolver, preferencesService)
        val paymentOperation = PayCreditCardInvoiceOperation(creditCardService, preferencesService)

        beforeSpec {
            every { walletService.getWalletById(1) } returns wallet
            every { categoryResolver.resolveOrCreate("Groceries") } returns groceries
            every { creditCardService.getAllNonArchivedCreditCardsOrderedByName() } returns
                listOf(card, cardWithoutBilling)
            every { preferencesService.translate(any()) } answers { firstArg() }
            every { preferencesService.translate(TranslationKeys.AI_REVIEW_NO_BILLING_WALLET) } returns
                "no billing wallet"
        }
        afterContainer { clearAllMocks(answers = false) }

        val payload =
            WalletTransactionPayload(
                date = LocalDate.of(2026, 10, 10),
                description = "Market",
                amount = BigDecimal("150.30"),
                movementType = WalletTransactionType.EXPENSE,
                walletId = 1,
                categoryName = "Groceries",
            )

        Given("a new wallet transaction") {
            val saved = slot<WalletTransaction>()
            every { walletService.createWalletTransaction(capture(saved), true) } returns 55

            When("applied") {
                val result = walletOperation.apply(payload)

                Then("it creates a confirmed transaction through the service and reports its id") {
                    result shouldBe ApplyResult.Success(listOf(55), "Market")
                    saved.captured.status shouldBe WalletTransactionStatus.CONFIRMED
                    saved.captured.wallet shouldBe wallet
                    saved.captured.category shouldBe groceries
                    saved.captured.type shouldBe WalletTransactionType.EXPENSE
                    saved.captured.amount shouldBe BigDecimal("150.30")
                    saved.captured.date shouldBe LocalDate.of(2026, 10, 10).atStartOfDay()
                }
            }
        }

        Given("a wallet proposal that the user reviews") {
            Then("the card puts the description on the main line and the amount and date as facts") {
                val card = walletOperation.card(payload)
                card.title shouldBe "Market"
                card.facts.take(2) shouldBe
                    listOf(cardMoney(BigDecimal("150.30")), cardDay(LocalDate.of(2026, 10, 10)))
            }

            Then("the summary leaves the type out and a tag says expense or income") {
                walletOperation.summary(payload) shouldBe "Market, 150.30, 2026-10-10"
                walletOperation.tag(payload) shouldBe
                    OperationTag(TranslationKeys.TRANSACTION_TYPE_EXPENSE, TagTone.NEGATIVE)
                walletOperation.tag(payload.copy(movementType = WalletTransactionType.INCOME)) shouldBe
                    OperationTag(TranslationKeys.TRANSACTION_TYPE_INCOME, TagTone.POSITIVE)
            }
        }

        Given("a payload that confirms an existing pending transaction") {
            val updated = slot<WalletTransaction>()
            every { walletService.updateWalletTransaction(capture(updated)) } returns Unit

            When("applied") {
                val result = walletOperation.apply(payload.copy(existingTransactionId = 214))

                Then("it updates that transaction instead of creating one") {
                    result shouldBe ApplyResult.Success(listOf(214), "Market")
                    updated.captured.id shouldBe 214
                    verify(exactly = 0) { walletService.createWalletTransaction(any(), any()) }
                }
            }
        }

        Given("an invalid wallet transaction payload") {
            When("the amount is zero") {
                val result = walletOperation.apply(payload.copy(amount = BigDecimal.ZERO))

                Then("it fails without touching the service") {
                    result.shouldBeInstanceOf<ApplyResult.Failure>()
                    verify(exactly = 0) { walletService.createWalletTransaction(any(), any()) }
                }
            }
        }

        Given("the service rejects the write") {
            every { walletService.createWalletTransaction(any(), any()) } throws IllegalStateException("boom")

            When("applied") {
                val result = walletOperation.apply(payload)

                Then("the exception becomes a failure with its message") {
                    result shouldBe ApplyResult.Failure("boom")
                }
            }
        }

        Given("arguments from the model naming a wallet and a category") {
            val catalog = EntityCatalog(wallets = listOf(wallet), categories = listOf(groceries))

            Then("names become ids, the amount is positive and the direction is read") {
                val parsed =
                    walletOperation.fromModelArguments(
                        JSONObject(
                            """{"date":"2026-10-10","description":"Salary","amount":-3000,"movementType":"income",
                                "walletName":"BANK","categoryName":"groceries"}""",
                        ),
                        catalog,
                    )
                parsed.amount.compareTo(BigDecimal("3000")) shouldBe 0
                parsed.movementType shouldBe WalletTransactionType.INCOME
                parsed.walletId shouldBe 1
                parsed.categoryName shouldBe "Groceries"
            }
        }

        Given("a payload whose wallet could not be resolved") {
            When("validated or applied") {
                val unresolved = payload.copy(walletId = null)

                Then("it is invalid and applying fails without touching the service") {
                    walletOperation.validate(unresolved).isValid shouldBe false
                    walletOperation.apply(unresolved).shouldBeInstanceOf<ApplyResult.Failure>()
                    verify(exactly = 0) { walletService.createWalletTransaction(any(), any()) }
                }
            }
        }

        Given("a wallet payload edited in the fallback editor") {
            Then("the draft carries every editable field and rebuilds the same payload") {
                val draft = walletOperation.toDraft(payload)
                draft.fields["walletId"] shouldBe 1
                walletOperation.fromDraft(draft) shouldBe payload
            }
        }

        Given("a pending transaction offered for confirmation") {
            every { walletService.getWalletTransactionById(214) } returns
                WalletTransactionFactory.create(
                    id = 214,
                    status = WalletTransactionStatus.PENDING,
                    description = "Academia",
                )
            every { walletService.getWalletTransactionById(215) } returns
                WalletTransactionFactory.create(id = 215, status = WalletTransactionStatus.CONFIRMED)

            Then("only a pending one can be confirmed, and it keeps its own description") {
                walletOperation.confirmingPending(payload, 214)!!.description shouldBe "Academia"
                walletOperation.confirmingPending(payload, 215) shouldBe null
            }
        }

        val debtPayload =
            CreditCardDebtPayload(
                date = LocalDate.of(2026, 9, 5),
                description = "Notebook",
                amount = BigDecimal("1000.00"),
                installments = 10,
                installmentNumber = 1,
                creditCardId = 7,
                categoryName = "Groceries",
            )

        Given("a first-seen card purchase") {
            val debt = slot<CreditCardDebt>()
            val month = slot<YearMonth>()
            every { creditCardService.invoiceMonthForPurchaseDate(card, debtPayload.date) } returns
                YearMonth.of(2026, 9)
            every { creditCardService.createDebt(capture(debt), capture(month), true) } returns 31

            When("applied") {
                val result = debtOperation.apply(debtPayload)

                Then("the invoice month comes from the purchase date and the whole purchase is posted") {
                    result shouldBe ApplyResult.Success(listOf(31), "Notebook")
                    month.captured shouldBe YearMonth.of(2026, 9)
                    debt.captured.installments shouldBe 10
                    debt.captured.amount shouldBe BigDecimal("1000.00")
                }
            }
        }

        Given("a line that continues an earlier installment purchase") {
            val month = slot<YearMonth>()
            every { creditCardService.currentInvoiceMonth(card) } returns YearMonth.of(2026, 11)
            every { creditCardService.createDebt(any(), capture(month), true) } returns 32

            When("applied") {
                debtOperation.apply(debtPayload.copy(installmentNumber = 3, installments = 8))

                Then("it is posted from the card's current invoice") {
                    month.captured shouldBe YearMonth.of(2026, 11)
                }
            }
        }

        Given("an invoice payment") {
            val dto = slot<CreditCardInvoicePaymentDTO>()
            every { creditCardService.payInvoice(capture(dto)) } returns Unit

            When("applied for a card with a default billing wallet") {
                val result =
                    paymentOperation.apply(
                        CreditCardInvoicePaymentPayload(7, YearMonth.of(2026, 9), BigDecimal("500.00")),
                    )

                Then("it pays from that wallet") {
                    result.shouldBeInstanceOf<ApplyResult.Success>()
                    dto.captured.billingWalletId shouldBe 2
                    dto.captured.invoiceDate shouldBe YearMonth.of(2026, 9)
                }
            }

            When("applied for a card without a billing wallet") {
                val result =
                    paymentOperation.apply(
                        CreditCardInvoicePaymentPayload(8, YearMonth.of(2026, 9), BigDecimal("500.00")),
                    )

                Then("it fails with the translated message") {
                    result shouldBe ApplyResult.Failure("no billing wallet")
                }
            }
        }
    })
