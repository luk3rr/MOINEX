package org.moinex.service.creditcard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.creditcard.CreditCardOperatorFactory
import org.moinex.factory.creditcard.CreditCardPaymentFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.model.dto.CreditCardInvoicePaymentDTO
import org.moinex.repository.creditcard.CreditCardCreditRepository
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardOperatorRepository
import org.moinex.repository.creditcard.CreditCardPaymentRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import java.math.BigDecimal
import java.time.YearMonth
import java.util.Optional

class CreditCardServicePayInvoicePartiallyTest :
    BehaviorSpec({
        val creditCardRepository = mockk<CreditCardRepository>()
        val creditCardOperatorRepository = mockk<CreditCardOperatorRepository>()
        val walletRepository = mockk<WalletRepository>()
        val creditCardDebtRepository = mockk<CreditCardDebtRepository>()
        val creditCardPaymentRepository = mockk<CreditCardPaymentRepository>()
        val creditCardCreditRepository = mockk<CreditCardCreditRepository>()
        val preferencesService = mockk<PreferencesService>(relaxed = true)
        val notificationService = mockk<NotificationService>(relaxed = true)

        val service =
            CreditCardService(
                creditCardDebtRepository,
                creditCardPaymentRepository,
                creditCardRepository,
                creditCardOperatorRepository,
                walletRepository,
                creditCardCreditRepository,
                notificationService,
                preferencesService,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("a credit card and wallet with two equal pending payments of 200 each") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    operator = operator,
                    availableRebate = BigDecimal("500.00"),
                )
            val wallet = WalletFactory.create(id = 1, name = "Billing Wallet", balance = BigDecimal("5000.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            val payment1 = CreditCardPaymentFactory.create(id = 1, amount = BigDecimal("200.00"))
            val payment2 = CreditCardPaymentFactory.create(id = 2, amount = BigDecimal("200.00"))

            every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
            every { walletRepository.findById(1) } returns Optional.of(wallet)
            every {
                creditCardPaymentRepository.getPendingCreditCardPayments(1, 3, 2026)
            } returns listOf(payment1, payment2)

            When("a partial payment of 100 is applied without rebate") {
                service.payInvoice(
                    CreditCardInvoicePaymentDTO(
                        creditCardId = 1,
                        billingWalletId = 1,
                        invoiceDate = invoiceDate,
                        amount = BigDecimal("100.00"),
                    ),
                )

                Then("paidAmount should be distributed proportionally (50 each)") {
                    payment1.paidAmount.compareTo(BigDecimal("50.00")) shouldBe 0
                    payment2.paidAmount.compareTo(BigDecimal("50.00")) shouldBe 0
                }

                Then("wallet balance should be reduced by the partial amount") {
                    wallet.balance.compareTo(BigDecimal("4900.00")) shouldBe 0
                }

                Then("payments should remain pending (wallet not set)") {
                    payment1.wallet shouldBe null
                    payment2.wallet shouldBe null
                }

                Then("available rebate should not change") {
                    creditCard.availableRebate.compareTo(BigDecimal("500.00")) shouldBe 0
                }
            }
        }

        Given("a credit card and wallet with two unequal pending payments (150 and 350)") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Mastercard")
            val creditCard =
                CreditCardFactory.create(
                    id = 2,
                    name = "Test Card 2",
                    operator = operator,
                    availableRebate = BigDecimal("200.00"),
                )
            val wallet = WalletFactory.create(id = 2, name = "Billing Wallet", balance = BigDecimal("3000.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            val payment1 = CreditCardPaymentFactory.create(id = 3, amount = BigDecimal("150.00"))
            val payment2 = CreditCardPaymentFactory.create(id = 4, amount = BigDecimal("350.00"))

            every { creditCardRepository.findById(2) } returns Optional.of(creditCard)
            every { walletRepository.findById(2) } returns Optional.of(wallet)
            every {
                creditCardPaymentRepository.getPendingCreditCardPayments(2, 3, 2026)
            } returns listOf(payment1, payment2)

            When("a partial payment of 200 with rebate of 20 is applied") {
                service.payInvoice(
                    CreditCardInvoicePaymentDTO(
                        creditCardId = 2,
                        billingWalletId = 2,
                        invoiceDate = invoiceDate,
                        amount = BigDecimal("200.00"),
                        rebate = BigDecimal("20.00"),
                    ),
                )

                Then("paidAmount should be distributed proportionally") {
                    // fraction: 150/500 = 0.30 → 200 * 0.30 = 60.00
                    // last payment gets remainder: 200 - 60 = 140.00
                    payment1.paidAmount.compareTo(BigDecimal("60.00")) shouldBe 0
                    payment2.paidAmount.compareTo(BigDecimal("140.00")) shouldBe 0
                    (payment1.paidAmount + payment2.paidAmount).compareTo(BigDecimal("200.00")) shouldBe 0
                }

                Then("rebateUsed should be distributed proportionally") {
                    // fraction: 150/500 = 0.30 → 20 * 0.30 = 6.00
                    // last payment gets remainder: 20 - 6 = 14.00
                    payment1.rebateUsed.compareTo(BigDecimal("6.00")) shouldBe 0
                    payment2.rebateUsed.compareTo(BigDecimal("14.00")) shouldBe 0
                    (payment1.rebateUsed + payment2.rebateUsed).compareTo(BigDecimal("20.00")) shouldBe 0
                }

                Then("wallet balance should be reduced by effectiveAmount (200 - 20 = 180)") {
                    wallet.balance.compareTo(BigDecimal("2820.00")) shouldBe 0
                }

                Then("available rebate should be reduced by the rebate amount") {
                    creditCard.availableRebate.compareTo(BigDecimal("180.00")) shouldBe 0
                }

                Then("payments should remain pending (wallet not set)") {
                    payment1.wallet shouldBe null
                    payment2.wallet shouldBe null
                }
            }
        }

        Given("a credit card with two pending payments receiving two sequential partial payments") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Amex")
            val creditCard =
                CreditCardFactory.create(
                    id = 3,
                    name = "Test Card 3",
                    operator = operator,
                    availableRebate = BigDecimal("500.00"),
                )
            val wallet = WalletFactory.create(id = 3, name = "Billing Wallet", balance = BigDecimal("5000.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            val payment1 = CreditCardPaymentFactory.create(id = 5, amount = BigDecimal("200.00"))
            val payment2 = CreditCardPaymentFactory.create(id = 6, amount = BigDecimal("200.00"))

            every { creditCardRepository.findById(3) } returns Optional.of(creditCard)
            every { walletRepository.findById(3) } returns Optional.of(wallet)
            every {
                creditCardPaymentRepository.getPendingCreditCardPayments(3, 3, 2026)
            } returns listOf(payment1, payment2)

            When("two partial payments of 100 are applied sequentially") {
                service.payInvoice(
                    CreditCardInvoicePaymentDTO(
                        creditCardId = 3,
                        billingWalletId = 3,
                        invoiceDate = invoiceDate,
                        amount = BigDecimal("100.00"),
                    ),
                )

                // After first partial: paidAmount = 50 each, remaining = 150 each (total 300)
                service.payInvoice(
                    CreditCardInvoicePaymentDTO(
                        creditCardId = 3,
                        billingWalletId = 3,
                        invoiceDate = invoiceDate,
                        amount = BigDecimal("100.00"),
                    ),
                )

                Then("paidAmount should accumulate across both partial payments") {
                    payment1.paidAmount.compareTo(BigDecimal("100.00")) shouldBe 0
                    payment2.paidAmount.compareTo(BigDecimal("100.00")) shouldBe 0
                }

                Then("wallet balance should reflect both deductions") {
                    wallet.balance.compareTo(BigDecimal("4800.00")) shouldBe 0
                }

                Then("payments should still be pending after partial payments") {
                    payment1.wallet shouldBe null
                    payment2.wallet shouldBe null
                }
            }
        }

        Given("a credit card with a partial amount equal to the total remaining") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 4,
                    name = "Test Card 4",
                    operator = operator,
                    availableRebate = BigDecimal("500.00"),
                )
            val wallet = WalletFactory.create(id = 4, name = "Billing Wallet", balance = BigDecimal("5000.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            val payment1 = CreditCardPaymentFactory.create(id = 7, amount = BigDecimal("200.00"))
            val payment2 = CreditCardPaymentFactory.create(id = 8, amount = BigDecimal("200.00"))

            every { creditCardRepository.findById(4) } returns Optional.of(creditCard)
            every { walletRepository.findById(4) } returns Optional.of(wallet)
            every {
                creditCardPaymentRepository.getPendingCreditCardPayments(4, 3, 2026)
            } returns listOf(payment1, payment2)

            When("amount equals the total remaining (400)") {
                service.payInvoice(
                    CreditCardInvoicePaymentDTO(
                        creditCardId = 4,
                        billingWalletId = 4,
                        invoiceDate = invoiceDate,
                        amount = BigDecimal("400.00"),
                    ),
                )

                Then("should be treated as full payment — wallet set on all payments") {
                    payment1.wallet shouldBe wallet
                    payment2.wallet shouldBe wallet
                }

                Then("paidAmount should equal amount for all payments") {
                    payment1.paidAmount.compareTo(payment1.amount) shouldBe 0
                    payment2.paidAmount.compareTo(payment2.amount) shouldBe 0
                }
            }
        }

        Given("a credit card with rebate equal to the partial payment amount") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 5,
                    name = "Test Card 5",
                    operator = operator,
                    availableRebate = BigDecimal("500.00"),
                )
            val wallet = WalletFactory.create(id = 5, name = "Billing Wallet", balance = BigDecimal("5000.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            every { creditCardRepository.findById(5) } returns Optional.of(creditCard)
            every { walletRepository.findById(5) } returns Optional.of(wallet)
            every {
                creditCardPaymentRepository.getPendingCreditCardPayments(5, 3, 2026)
            } returns listOf(CreditCardPaymentFactory.create(id = 9, amount = BigDecimal("400.00")))

            When("rebate equals partial amount (effectiveAmount = 0)") {
                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.payInvoice(
                            CreditCardInvoicePaymentDTO(
                                creditCardId = 5,
                                billingWalletId = 5,
                                invoiceDate = invoiceDate,
                                amount = BigDecimal("100.00"),
                                rebate = BigDecimal("100.00"),
                            ),
                        )
                    }
                }
            }
        }

        Given("a credit card with insufficient wallet balance for partial payment") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 6,
                    name = "Test Card 6",
                    operator = operator,
                    availableRebate = BigDecimal("500.00"),
                )
            val wallet = WalletFactory.create(id = 6, name = "Billing Wallet", balance = BigDecimal("30.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            every { creditCardRepository.findById(6) } returns Optional.of(creditCard)
            every { walletRepository.findById(6) } returns Optional.of(wallet)
            every {
                creditCardPaymentRepository.getPendingCreditCardPayments(6, 3, 2026)
            } returns listOf(CreditCardPaymentFactory.create(id = 10, amount = BigDecimal("400.00")))

            When("partial amount exceeds wallet balance") {
                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.payInvoice(
                            CreditCardInvoicePaymentDTO(
                                creditCardId = 6,
                                billingWalletId = 6,
                                invoiceDate = invoiceDate,
                                amount = BigDecimal("100.00"),
                            ),
                        )
                    }
                }
            }
        }

        Given("a credit card with rebate exceeding available rebate on partial payment") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 7,
                    name = "Test Card 7",
                    operator = operator,
                    availableRebate = BigDecimal("10.00"),
                )
            val wallet = WalletFactory.create(id = 7, name = "Billing Wallet", balance = BigDecimal("5000.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            every { creditCardRepository.findById(7) } returns Optional.of(creditCard)
            every { walletRepository.findById(7) } returns Optional.of(wallet)

            When("rebate exceeds available rebate") {
                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.payInvoice(
                            CreditCardInvoicePaymentDTO(
                                creditCardId = 7,
                                billingWalletId = 7,
                                invoiceDate = invoiceDate,
                                amount = BigDecimal("100.00"),
                                rebate = BigDecimal("50.00"),
                            ),
                        )
                    }
                }
            }
        }

        Given("a credit card with a prior partial payment followed by full payment") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 8,
                    name = "Test Card 8",
                    operator = operator,
                    availableRebate = BigDecimal("500.00"),
                )
            val wallet = WalletFactory.create(id = 8, name = "Billing Wallet", balance = BigDecimal("5000.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            val payment1 = CreditCardPaymentFactory.create(id = 11, amount = BigDecimal("200.00"))
            val payment2 = CreditCardPaymentFactory.create(id = 12, amount = BigDecimal("200.00"))

            every { creditCardRepository.findById(8) } returns Optional.of(creditCard)
            every { walletRepository.findById(8) } returns Optional.of(wallet)
            every {
                creditCardPaymentRepository.getPendingCreditCardPayments(8, 3, 2026)
            } returns listOf(payment1, payment2)

            When("partial payment of 200 is applied followed by full payment of the remaining 200") {
                service.payInvoice(
                    CreditCardInvoicePaymentDTO(
                        creditCardId = 8,
                        billingWalletId = 8,
                        invoiceDate = invoiceDate,
                        amount = BigDecimal("200.00"),
                    ),
                )

                // After partial: paidAmount = 100 each, remaining = 100 each (total 200)
                service.payInvoice(
                    CreditCardInvoicePaymentDTO(
                        creditCardId = 8,
                        billingWalletId = 8,
                        invoiceDate = invoiceDate,
                        amount = BigDecimal("200.00"),
                    ),
                )

                Then("payments should be fully paid (wallet set)") {
                    payment1.wallet shouldBe wallet
                    payment2.wallet shouldBe wallet
                }

                Then("paidAmount should equal amount for all payments") {
                    payment1.paidAmount.compareTo(payment1.amount) shouldBe 0
                    payment2.paidAmount.compareTo(payment2.amount) shouldBe 0
                }

                Then("wallet balance should reflect both deductions (total 400)") {
                    wallet.balance.compareTo(BigDecimal("4600.00")) shouldBe 0
                }
            }
        }
    })
