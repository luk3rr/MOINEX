package org.moinex.service.matching

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.factory.creditcard.CreditCardDebtFactory
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.wallet.TransferFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.service.PreferencesService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class TransactionMatchingServiceTest :
    BehaviorSpec({
        val walletTransactionRepository = mockk<WalletTransactionRepository>()
        val transferRepository = mockk<TransferRepository>()
        val creditCardDebtRepository = mockk<CreditCardDebtRepository>()
        val preferencesService = mockk<PreferencesService>()
        val service =
            TransactionMatchingService(
                walletTransactionRepository,
                transferRepository,
                creditCardDebtRepository,
                preferencesService,
            )

        val day = LocalDate.of(2026, 10, 10)
        val wallet = WalletFactory.create(id = 1, name = "Bank")
        val otherWallet = WalletFactory.create(id = 2, name = "Savings")
        val card = CreditCardFactory.create(id = 7, name = "Card")

        beforeSpec {
            every { preferencesService.translate(any()) } answers { firstArg() }
        }
        afterContainer { clearAllMocks(answers = false) }

        fun stored(
            transactions: List<org.moinex.model.wallettransaction.WalletTransaction> = emptyList(),
            transfers: List<org.moinex.model.wallettransaction.Transfer> = emptyList(),
            debts: List<org.moinex.model.creditcard.CreditCardDebt> = emptyList(),
        ) {
            every { walletTransactionRepository.findTransactionsBetweenDates(any(), any()) } returns transactions
            every { transferRepository.findBetweenDates(any(), any()) } returns transfers
            every { creditCardDebtRepository.findByDateBetween(any(), any()) } returns debts
        }

        fun transaction(
            id: Int,
            amount: String,
            date: LocalDate,
            description: String? = null,
            status: WalletTransactionStatus = WalletTransactionStatus.CONFIRMED,
            type: WalletTransactionType = WalletTransactionType.EXPENSE,
            inWallet: org.moinex.model.wallettransaction.Wallet = wallet,
        ) = WalletTransactionFactory.create(
            id = id,
            amount = BigDecimal(amount),
            date = date.atTime(12, 0),
            description = description,
            status = status,
            type = type,
            wallet = inWallet,
        )

        Given("a confirmed wallet transaction recorded under a different name") {
            stored(transactions = listOf(transaction(198, "99.90", day.plusDays(1), "Academia")))

            When("a statement line has the same amount, nearly the same date and the same wallet") {
                val matches =
                    service.findMatches(
                        MatchQuery(
                            BigDecimal("99.90"),
                            day,
                            walletId = 1,
                            isIncome = false,
                            description = "PIX ACADEMIA FIT",
                        ),
                    )

                Then("it matches the stored one although the names differ") {
                    matches.map { it.targetId } shouldBe listOf(198)
                    matches[0].status shouldBe MatchStatus.CONFIRMED
                    matches[0].targetType shouldBe MatchTargetType.WALLET_TRANSACTION
                    matches[0].reasons shouldContain "aimatch.reason.exactAmount"
                    matches[0].reasons shouldContain "aimatch.reason.sameWallet"
                }
            }

            When("the amount differs by one cent") {
                val matches = service.findMatches(MatchQuery(BigDecimal("99.91"), day, walletId = 1))

                Then("it does not match") {
                    matches.shouldBeEmpty()
                }
            }

            When("the date is three days away from the stored one") {
                val matches = service.findMatches(MatchQuery(BigDecimal("99.90"), day.minusDays(2), walletId = 1))

                Then("it still matches at three days, and not at four") {
                    matches.map { it.targetId } shouldBe listOf(198)
                    service.findMatches(MatchQuery(BigDecimal("99.90"), day.minusDays(3), walletId = 1)).shouldBeEmpty()
                }
            }

            When("the line belongs to another wallet") {
                val matches = service.findMatches(MatchQuery(BigDecimal("99.90"), day, walletId = 2))

                Then("it does not match") {
                    matches.shouldBeEmpty()
                }
            }

            When("the line is an income but the stored record is an expense") {
                val matches = service.findMatches(MatchQuery(BigDecimal("99.90"), day, walletId = 1, isIncome = true))

                Then("the direction keeps them apart") {
                    matches.shouldBeEmpty()
                }
            }

            When("no wallet is informed") {
                val matches = service.findMatches(MatchQuery(BigDecimal("99.90"), day))

                Then("it still matches, with a lower score and the unknown account reason") {
                    matches.size shouldBe 1
                    matches[0].reasons shouldContain "aimatch.reason.accountUnknown"
                    (matches[0].score < 0.95) shouldBe true
                }
            }
        }

        Given("a pending and a confirmed transaction with the same amount and date") {
            stored(
                transactions =
                    listOf(
                        transaction(1, "50.00", day, "Gym", status = WalletTransactionStatus.CONFIRMED),
                        transaction(2, "50.00", day, "Gym membership", status = WalletTransactionStatus.PENDING),
                    ),
            )

            When("matching a line with a similar description") {
                val matches =
                    service.findMatches(
                        MatchQuery(BigDecimal("50.00"), day, walletId = 1, description = "Gym membership"),
                    )

                Then("both are returned, the more similar description first, with their status") {
                    matches.map { it.targetId } shouldBe listOf(2, 1)
                    matches[0].status shouldBe MatchStatus.PENDING
                    matches[1].status shouldBe MatchStatus.CONFIRMED
                }
            }
        }

        Given("a transfer between two wallets") {
            stored(
                transfers =
                    listOf(
                        TransferFactory.create(
                            id = 9,
                            senderWallet = wallet,
                            receiverWallet = otherWallet,
                            date = day.atTime(9, 0),
                            amount = BigDecimal("300.00"),
                        ),
                    ),
            )

            When("the line is an expense of the sender wallet") {
                val matches = service.findMatches(MatchQuery(BigDecimal("300.00"), day, walletId = 1, isIncome = false))

                Then("it matches the transfer") {
                    matches.map { it.targetId } shouldBe listOf(9)
                    matches[0].targetType shouldBe MatchTargetType.TRANSFER
                }
            }

            When("the line is an income of the sender wallet") {
                Then("it does not match, because the sender only sends") {
                    service
                        .findMatches(
                            MatchQuery(BigDecimal("300.00"), day, walletId = 1, isIncome = true),
                        ).shouldBeEmpty()
                }
            }

            When("the line is an income of the receiver wallet") {
                Then("it matches") {
                    service
                        .findMatches(
                            MatchQuery(BigDecimal("300.00"), day, walletId = 2, isIncome = true),
                        ).map { it.targetId } shouldBe
                        listOf(9)
                }
            }
        }

        Given("an installment purchase of 1000.00 in 10 installments on a card") {
            stored(
                debts =
                    listOf(
                        CreditCardDebtFactory.create(
                            id = 31,
                            creditCard = card,
                            installments = 10,
                            amount = BigDecimal("1000.00"),
                            date = LocalDateTime.of(2026, 9, 5, 15, 0),
                            description = "Notebook",
                        ),
                    ),
            )

            When("the statement shows the 3/10 line with the original purchase date") {
                val matches =
                    service.findMatches(
                        MatchQuery(
                            BigDecimal("100.00"),
                            LocalDate.of(2026, 9, 5),
                            creditCardId = 7,
                            description = "NOTEBOOK LOJA",
                        ),
                    )

                Then("the line matches the stored purchase through its installment value") {
                    matches.map { it.targetId } shouldBe listOf(31)
                    matches[0].targetType shouldBe MatchTargetType.CREDIT_CARD_DEBT
                    matches[0].reasons shouldContain "aimatch.reason.installment"
                }
            }

            When("the line is on another card") {
                Then("it does not match") {
                    service
                        .findMatches(
                            MatchQuery(BigDecimal("100.00"), LocalDate.of(2026, 9, 5), creditCardId = 8),
                        ).shouldBeEmpty()
                }
            }

            When("a wallet line has the installment value") {
                Then("a card purchase is never matched by a wallet line") {
                    service
                        .findMatches(
                            MatchQuery(BigDecimal("100.00"), LocalDate.of(2026, 9, 5), walletId = 1),
                        ).shouldBeEmpty()
                }
            }
        }

        Given("an installment total that does not divide evenly") {
            stored(
                debts =
                    listOf(
                        CreditCardDebtFactory.create(
                            id = 32,
                            creditCard = card,
                            installments = 3,
                            amount = BigDecimal("100.00"),
                            date = day.atTime(10, 0),
                        ),
                    ),
            )

            When("the statement shows the rounded-up first installment") {
                Then("the neighbouring cent still matches") {
                    service
                        .findMatches(
                            MatchQuery(BigDecimal("33.34"), day, creditCardId = 7),
                        ).map { it.targetId } shouldBe
                        listOf(32)
                    service
                        .findMatches(
                            MatchQuery(BigDecimal("33.33"), day, creditCardId = 7),
                        ).map { it.targetId } shouldBe
                        listOf(32)
                }
            }
        }

        Given("several statement lines at once") {
            stored(transactions = listOf(transaction(5, "10.00", day), transaction(6, "20.00", day.plusDays(20))))

            When("matching them in one call") {
                val result =
                    service.findMatches(
                        listOf(
                            MatchQuery(BigDecimal("10.00"), day, walletId = 1),
                            MatchQuery(BigDecimal("20.00"), day.plusDays(20), walletId = 1),
                            MatchQuery(BigDecimal("30.00"), day, walletId = 1),
                        ),
                    )

                Then("each query gets its own answer and the database is read once") {
                    result.map { list -> list.map { it.targetId } } shouldBe listOf(listOf(5), listOf(6), emptyList())
                    verify(exactly = 1) { walletTransactionRepository.findTransactionsBetweenDates(any(), any()) }
                }
            }
        }
    })
