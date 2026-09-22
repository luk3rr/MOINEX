package org.moinex.ai.tools

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.factory.CategoryFactory
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.dto.ai.TransactionKind
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService

class ProposeTransactionToolTest :
    BehaviorSpec({
        val categoryService = mockk<CategoryService>()
        val walletService = mockk<WalletService>()
        val creditCardService = mockk<CreditCardService>()
        val preferencesService = mockk<PreferencesService>(relaxed = true)

        val groceries = CategoryFactory.create(id = 1, name = "Groceries")
        val nubank = WalletFactory.create(id = 10, name = "Nubank")
        val card = CreditCardFactory.create(id = 20, name = "Nubank Mastercard")

        every { categoryService.getNonArchivedCategoriesOrderedByName() } returns listOf(groceries)
        every { walletService.getAllNonArchivedWalletsOrderedByName() } returns listOf(nubank)
        every { creditCardService.getAllNonArchivedCreditCardsOrderedByName() } returns listOf(card)

        val tool = ProposeTransactionTool(categoryService, walletService, creditCardService, preferencesService)

        Given("a candidate whose category and wallet names match existing ones") {
            val arguments =
                """
                {"transactions":[
                    {"date":"2026-09-05","description":"Market","amount":150.30,"kind":"wallet",
                     "walletName":"nubank","categoryName":"groceries","confidence":0.9}
                ]}
                """.trimIndent()

            When("executing the tool") {
                val result = ProposeTransactionTool.parseResultJson(tool.execute(arguments))

                Then("it resolves the existing category and wallet ids, case-insensitively") {
                    result.size shouldBe 1
                    result[0].suggestedCategoryId shouldBe groceries.id
                    result[0].suggestedWalletId shouldBe nubank.id
                    result[0].kind shouldBe TransactionKind.WALLET
                }
            }
        }

        Given("a candidate with a category name that doesn't exist yet") {
            val arguments =
                """
                {"transactions":[
                    {"date":"2026-09-05","description":"New stuff","amount":50.00,"kind":"credit_card",
                     "creditCardName":"Nubank Mastercard","categoryName":"Brand New Category","confidence":0.4}
                ]}
                """.trimIndent()

            When("executing the tool") {
                val result = ProposeTransactionTool.parseResultJson(tool.execute(arguments))

                Then("suggestedCategoryId is null and the proposed name is preserved") {
                    result[0].suggestedCategoryId shouldBe null
                    result[0].suggestedCategoryName shouldBe "Brand New Category"
                    result[0].suggestedCreditCardId shouldBe card.id
                    result[0].kind shouldBe TransactionKind.CREDIT_CARD
                }
            }
        }

        Given("a candidate missing a required field") {
            val arguments =
                """
                {"transactions":[
                    {"description":"Missing date","amount":10,"kind":"wallet","categoryName":"Groceries"}
                ]}
                """.trimIndent()

            When("executing the tool") {
                val result = ProposeTransactionTool.parseResultJson(tool.execute(arguments))

                Then("the malformed entry is skipped instead of failing the whole batch") {
                    result.size shouldBe 0
                }
            }
        }

        Given("the tool is asked to describe the available context") {
            When("calling describeAvailableContext") {
                val context = tool.describeAvailableContext()

                Then("it lists the existing categories and wallets") {
                    context.contains("Groceries") shouldBe true
                    context.contains("Nubank") shouldBe true
                }
            }
        }

        Given("a candidate with a negative amount and movementType income") {
            val arguments =
                """
                {"transactions":[
                    {"date":"2026-09-05","description":"Salary","amount":-3000.00,"kind":"wallet",
                     "movementType":"income","categoryName":"Groceries","confidence":0.9}
                ]}
                """.trimIndent()

            When("executing the tool") {
                val result = ProposeTransactionTool.parseResultJson(tool.execute(arguments))

                Then("the amount is normalized to positive and movementType is preserved") {
                    result[0].amount.signum() shouldBe 1
                    result[0].movementType shouldBe WalletTransactionType.INCOME
                }
            }
        }

        Given("a candidate with no movementType at all") {
            val arguments =
                """
                {"transactions":[
                    {"date":"2026-09-05","description":"Market","amount":50.00,"kind":"wallet",
                     "categoryName":"Groceries","confidence":0.9}
                ]}
                """.trimIndent()

            When("executing the tool") {
                val result = ProposeTransactionTool.parseResultJson(tool.execute(arguments))

                Then("it defaults to expense") {
                    result[0].movementType shouldBe WalletTransactionType.EXPENSE
                }
            }
        }

        Given("a candidate whose existingTransactionId points to a still-pending wallet transaction") {
            val pending = WalletTransactionFactory.create(id = 99, status = WalletTransactionStatus.PENDING)
            every { walletService.getWalletTransactionById(99) } returns pending

            val arguments =
                """
                {"transactions":[
                    {"date":"2026-09-05","description":"Market","amount":150.30,"kind":"wallet",
                     "existingTransactionId":99,"walletName":"nubank","categoryName":"groceries","confidence":0.9}
                ]}
                """.trimIndent()

            When("executing the tool") {
                val result = ProposeTransactionTool.parseResultJson(tool.execute(arguments))

                Then(
                    "existingTransactionId is carried through so the review screen updates it instead of creating one",
                ) {
                    result[0].existingTransactionId shouldBe 99
                }
            }
        }

        Given("a candidate whose existingTransactionId points to an already-confirmed transaction") {
            val confirmed = WalletTransactionFactory.create(id = 98, status = WalletTransactionStatus.CONFIRMED)
            every { walletService.getWalletTransactionById(98) } returns confirmed

            val arguments =
                """
                {"transactions":[
                    {"date":"2026-09-05","description":"Market","amount":150.30,"kind":"wallet",
                     "existingTransactionId":98,"walletName":"nubank","categoryName":"groceries"}
                ]}
                """.trimIndent()

            When("executing the tool") {
                val result = ProposeTransactionTool.parseResultJson(tool.execute(arguments))

                Then("the id is discarded - never overwrite a transaction that is not pending") {
                    result[0].existingTransactionId shouldBe null
                }
            }
        }

        Given("a candidate whose existingTransactionId doesn't exist") {
            every { walletService.getWalletTransactionById(97) } throws NoSuchElementException("not found")

            val arguments =
                """
                {"transactions":[
                    {"date":"2026-09-05","description":"Market","amount":150.30,"kind":"wallet",
                     "existingTransactionId":97,"walletName":"nubank","categoryName":"groceries"}
                ]}
                """.trimIndent()

            When("executing the tool") {
                val result = ProposeTransactionTool.parseResultJson(tool.execute(arguments))

                Then("the id is discarded instead of failing the whole candidate") {
                    result[0].existingTransactionId shouldBe null
                }
            }
        }

        Given("a candidate with existingTransactionId set but kind credit_card") {
            val arguments =
                """
                {"transactions":[
                    {"date":"2026-09-05","description":"Market","amount":50.00,"kind":"credit_card",
                     "existingTransactionId":99,"creditCardName":"Nubank Mastercard","categoryName":"groceries"}
                ]}
                """.trimIndent()

            When("executing the tool") {
                val result = ProposeTransactionTool.parseResultJson(tool.execute(arguments))

                Then("the id is discarded - only a wallet transaction can be confirmed this way") {
                    result[0].existingTransactionId shouldBe null
                }
            }
        }

        Given("any candidate list, valid or not") {
            val arguments =
                """
                {"transactions":[
                    {"date":"2026-09-05","description":"Market","amount":1,"kind":"wallet","categoryName":"Groceries"}
                ]}
                """.trimIndent()

            When("executing the tool") {
                tool.execute(arguments)

                Then("it never calls a write method on CategoryService") {
                    verify(exactly = 0) { categoryService.createCategory(any()) }
                }
            }
        }
    })
