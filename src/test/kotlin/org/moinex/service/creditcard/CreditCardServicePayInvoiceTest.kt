package org.moinex.service.creditcard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.moinex.factory.CreditCardFactory
import org.moinex.factory.CreditCardOperatorFactory
import org.moinex.factory.CreditCardPaymentFactory
import org.moinex.factory.WalletFactory
import org.moinex.repository.creditcard.CreditCardCreditRepository
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardOperatorRepository
import org.moinex.repository.creditcard.CreditCardPaymentRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.service.CreditCardService
import java.math.BigDecimal
import java.time.YearMonth
import java.util.Optional

class CreditCardServicePayInvoiceTest :
    BehaviorSpec({
        val creditCardRepository = mockk<CreditCardRepository>()
        val creditCardOperatorRepository = mockk<CreditCardOperatorRepository>()
        val walletRepository = mockk<WalletRepository>()
        val creditCardDebtRepository = mockk<CreditCardDebtRepository>()
        val creditCardPaymentRepository = mockk<CreditCardPaymentRepository>()
        val creditCardCreditRepository = mockk<CreditCardCreditRepository>()

        val service =
            CreditCardService(
                creditCardDebtRepository,
                creditCardPaymentRepository,
                creditCardRepository,
                creditCardOperatorRepository,
                walletRepository,
                creditCardCreditRepository,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("a valid credit card and wallet with pending payments") {
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

            val payment1 =
                CreditCardPaymentFactory.create(
                    id = 1,
                    amount = BigDecimal("100.00"),
                    rebateUsed = BigDecimal.ZERO,
                )
            val payment2 =
                CreditCardPaymentFactory.create(
                    id = 2,
                    amount = BigDecimal("200.00"),
                    rebateUsed = BigDecimal.ZERO,
                )

            When("paying invoice without rebate") {
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { walletRepository.findById(1) } returns Optional.of(wallet)
                every {
                    creditCardPaymentRepository.getPendingCreditCardPayments(1, 3, 2026)
                } returns listOf(payment1, payment2)

                service.payInvoice(1, 1, invoiceDate, BigDecimal.ZERO)

                Then("should deduct total payment amount from wallet balance") {
                    wallet.balance.compareTo(BigDecimal("4700.00")) shouldBe 0
                }

                Then("should not change available rebate") {
                    creditCard.availableRebate.compareTo(BigDecimal("500.00")) shouldBe 0
                }

                Then("should set wallet for all payments") {
                    payment1.wallet shouldBe wallet
                    payment2.wallet shouldBe wallet
                }

                Then("should set rebateUsed to zero for all payments") {
                    payment1.rebateUsed.compareTo(BigDecimal.ZERO) shouldBe 0
                    payment2.rebateUsed.compareTo(BigDecimal.ZERO) shouldBe 0
                }
            }
        }

        Given("a credit card with pending payments and partial rebate") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Mastercard")
            val creditCard =
                CreditCardFactory.create(
                    id = 2,
                    name = "Test Card 2",
                    operator = operator,
                    availableRebate = BigDecimal("100.00"),
                )
            val wallet = WalletFactory.create(id = 2, name = "Billing Wallet", balance = BigDecimal("3000.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            val payment1 =
                CreditCardPaymentFactory.create(
                    id = 3,
                    amount = BigDecimal("100.00"),
                    rebateUsed = BigDecimal.ZERO,
                )
            val payment2 =
                CreditCardPaymentFactory.create(
                    id = 4,
                    amount = BigDecimal("100.00"),
                    rebateUsed = BigDecimal.ZERO,
                )

            When("paying invoice with rebate less than total amount") {
                every { creditCardRepository.findById(2) } returns Optional.of(creditCard)
                every { walletRepository.findById(2) } returns Optional.of(wallet)
                every {
                    creditCardPaymentRepository.getPendingCreditCardPayments(2, 3, 2026)
                } returns listOf(payment1, payment2)

                service.payInvoice(2, 2, invoiceDate, BigDecimal("50.00"))

                Then("should deduct total payment minus rebate from wallet") {
                    wallet.balance.compareTo(BigDecimal("2850.00")) shouldBe 0
                }

                Then("should deduct rebate from available rebate") {
                    creditCard.availableRebate.compareTo(BigDecimal("50.00")) shouldBe 0
                }

                Then("should distribute rebate proportionally to payments") {
                    payment1.rebateUsed.compareTo(BigDecimal("25.00")) shouldBe 0
                    payment2.rebateUsed.compareTo(BigDecimal("25.00")) shouldBe 0
                    (payment1.rebateUsed + payment2.rebateUsed).compareTo(BigDecimal("50.00")) shouldBe 0
                }
            }
        }

        Given("a credit card with pending payments and rebate exceeding total amount") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Amex")
            val creditCard =
                CreditCardFactory.create(
                    id = 3,
                    name = "Test Card 3",
                    operator = operator,
                    availableRebate = BigDecimal("500.00"),
                )
            val wallet = WalletFactory.create(id = 3, name = "Billing Wallet", balance = BigDecimal("2000.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            val payment1 =
                CreditCardPaymentFactory.create(
                    id = 5,
                    amount = BigDecimal("100.00"),
                    rebateUsed = BigDecimal.ZERO,
                )
            val payment2 =
                CreditCardPaymentFactory.create(
                    id = 6,
                    amount = BigDecimal("100.00"),
                    rebateUsed = BigDecimal.ZERO,
                )

            When("paying invoice with rebate greater than total amount") {
                every { creditCardRepository.findById(3) } returns Optional.of(creditCard)
                every { walletRepository.findById(3) } returns Optional.of(wallet)
                every {
                    creditCardPaymentRepository.getPendingCreditCardPayments(3, 3, 2026)
                } returns listOf(payment1, payment2)

                service.payInvoice(3, 3, invoiceDate, BigDecimal("300.00"))

                Then("should not deduct anything from wallet") {
                    wallet.balance.compareTo(BigDecimal("2000.00")) shouldBe 0
                }

                Then("should deduct only the total amount from available rebate") {
                    creditCard.availableRebate.compareTo(BigDecimal("300.00")) shouldBe 0
                }

                Then("should distribute total amount as rebate to payments") {
                    payment1.rebateUsed.compareTo(BigDecimal("100.00")) shouldBe 0
                    payment2.rebateUsed.compareTo(BigDecimal("100.00")) shouldBe 0
                    (payment1.rebateUsed + payment2.rebateUsed).compareTo(BigDecimal("200.00")) shouldBe 0
                }
            }
        }

        Given("a credit card with single pending payment") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 4,
                    name = "Test Card 4",
                    operator = operator,
                    availableRebate = BigDecimal("100.00"),
                )
            val wallet = WalletFactory.create(id = 4, name = "Billing Wallet", balance = BigDecimal("1000.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            val payment =
                CreditCardPaymentFactory.create(
                    id = 7,
                    amount = BigDecimal("250.00"),
                    rebateUsed = BigDecimal.ZERO,
                )

            When("paying invoice with single payment and rebate") {
                every { creditCardRepository.findById(4) } returns Optional.of(creditCard)
                every { walletRepository.findById(4) } returns Optional.of(wallet)
                every {
                    creditCardPaymentRepository.getPendingCreditCardPayments(4, 3, 2026)
                } returns listOf(payment)

                service.payInvoice(4, 4, invoiceDate, BigDecimal("50.00"))

                Then("should deduct total payment minus rebate from wallet") {
                    wallet.balance.compareTo(BigDecimal("800.00")) shouldBe 0
                }

                Then("should apply full rebate to single payment") {
                    payment.rebateUsed.compareTo(BigDecimal("50.00")) shouldBe 0
                }

                Then("should deduct rebate from available rebate") {
                    creditCard.availableRebate.compareTo(BigDecimal("50.00")) shouldBe 0
                }
            }
        }

        Given("a credit card with non-existent id") {
            val invoiceDate = YearMonth.of(2026, 3)

            When("paying invoice with non-existent credit card") {
                every { creditCardRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<Exception> {
                        service.payInvoice(999, 1, invoiceDate)
                    }
                }
            }
        }

        Given("a wallet with non-existent id") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 5,
                    name = "Test Card 5",
                    operator = operator,
                    availableRebate = BigDecimal("100.00"),
                )
            val invoiceDate = YearMonth.of(2026, 3)

            When("paying invoice with non-existent wallet") {
                every { creditCardRepository.findById(5) } returns Optional.of(creditCard)
                every { walletRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<Exception> {
                        service.payInvoice(5, 999, invoiceDate)
                    }
                }
            }
        }

        Given("a credit card with negative rebate") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 6,
                    name = "Test Card 6",
                    operator = operator,
                    availableRebate = BigDecimal("100.00"),
                )
            val wallet = WalletFactory.create(id = 5, name = "Billing Wallet", balance = BigDecimal("1000.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            When("paying invoice with negative rebate") {
                every { creditCardRepository.findById(6) } returns Optional.of(creditCard)
                every { walletRepository.findById(5) } returns Optional.of(wallet)

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.payInvoice(6, 5, invoiceDate, BigDecimal("-50.00"))
                    }
                }
            }
        }

        Given("a credit card with insufficient available rebate") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 7,
                    name = "Test Card 7",
                    operator = operator,
                    availableRebate = BigDecimal("30.00"),
                )
            val wallet = WalletFactory.create(id = 6, name = "Billing Wallet", balance = BigDecimal("1000.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            When("paying invoice with rebate greater than available rebate") {
                every { creditCardRepository.findById(7) } returns Optional.of(creditCard)
                every { walletRepository.findById(6) } returns Optional.of(wallet)

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.payInvoice(7, 6, invoiceDate, BigDecimal("50.00"))
                    }
                }
            }
        }

        Given("a credit card with multiple payments and unequal amounts") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 8,
                    name = "Test Card 8",
                    operator = operator,
                    availableRebate = BigDecimal("100.00"),
                )
            val wallet = WalletFactory.create(id = 7, name = "Billing Wallet", balance = BigDecimal("5000.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            val payment1 =
                CreditCardPaymentFactory.create(
                    id = 8,
                    amount = BigDecimal("150.00"),
                    rebateUsed = BigDecimal.ZERO,
                )
            val payment2 =
                CreditCardPaymentFactory.create(
                    id = 9,
                    amount = BigDecimal("350.00"),
                    rebateUsed = BigDecimal.ZERO,
                )

            When("paying invoice with unequal payments and rebate") {
                every { creditCardRepository.findById(8) } returns Optional.of(creditCard)
                every { walletRepository.findById(7) } returns Optional.of(wallet)
                every {
                    creditCardPaymentRepository.getPendingCreditCardPayments(8, 3, 2026)
                } returns listOf(payment1, payment2)

                service.payInvoice(8, 7, invoiceDate, BigDecimal("50.00"))

                Then("should distribute rebate proportionally") {
                    payment1.rebateUsed.compareTo(BigDecimal("15.00")) shouldBe 0
                    payment2.rebateUsed.compareTo(BigDecimal("35.00")) shouldBe 0
                    (payment1.rebateUsed + payment2.rebateUsed).compareTo(BigDecimal("50.00")) shouldBe 0
                }

                Then("should deduct correct amount from wallet") {
                    wallet.balance.compareTo(BigDecimal("4550.00")) shouldBe 0
                }

                Then("should deduct rebate from available rebate") {
                    creditCard.availableRebate.compareTo(BigDecimal("50.00")) shouldBe 0
                }
            }
        }

        Given("a credit card with three payments") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 9,
                    name = "Test Card 9",
                    operator = operator,
                    availableRebate = BigDecimal("200.00"),
                )
            val wallet = WalletFactory.create(id = 8, name = "Billing Wallet", balance = BigDecimal("5000.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            val payment1 =
                CreditCardPaymentFactory.create(
                    id = 10,
                    amount = BigDecimal("100.00"),
                    rebateUsed = BigDecimal.ZERO,
                )
            val payment2 =
                CreditCardPaymentFactory.create(
                    id = 11,
                    amount = BigDecimal("100.00"),
                    rebateUsed = BigDecimal.ZERO,
                )
            val payment3 =
                CreditCardPaymentFactory.create(
                    id = 12,
                    amount = BigDecimal("100.00"),
                    rebateUsed = BigDecimal.ZERO,
                )

            When("paying invoice with three equal payments and rebate") {
                every { creditCardRepository.findById(9) } returns Optional.of(creditCard)
                every { walletRepository.findById(8) } returns Optional.of(wallet)
                every {
                    creditCardPaymentRepository.getPendingCreditCardPayments(9, 3, 2026)
                } returns listOf(payment1, payment2, payment3)

                service.payInvoice(9, 8, invoiceDate, BigDecimal("60.00"))

                Then("should distribute rebate to all payments") {
                    payment1.rebateUsed.compareTo(BigDecimal("19.80")) shouldBe 0
                    payment2.rebateUsed.compareTo(BigDecimal("19.80")) shouldBe 0
                    payment3.rebateUsed.compareTo(BigDecimal("20.40")) shouldBe 0
                    (payment1.rebateUsed + payment2.rebateUsed + payment3.rebateUsed).compareTo(BigDecimal("60.00")) shouldBe 0
                }

                Then("should set wallet for all payments") {
                    payment1.wallet shouldBe wallet
                    payment2.wallet shouldBe wallet
                    payment3.wallet shouldBe wallet
                }

                Then("should deduct correct amount from wallet") {
                    wallet.balance.compareTo(BigDecimal("4760.00")) shouldBe 0
                }
            }
        }

        Given("a credit card with no pending payments") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 10,
                    name = "Test Card 10",
                    operator = operator,
                    availableRebate = BigDecimal("100.00"),
                )
            val wallet = WalletFactory.create(id = 9, name = "Billing Wallet", balance = BigDecimal("5000.00"))
            val invoiceDate = YearMonth.of(2026, 3)

            When("paying invoice with no pending payments") {
                every { creditCardRepository.findById(10) } returns Optional.of(creditCard)
                every { walletRepository.findById(9) } returns Optional.of(wallet)
                every {
                    creditCardPaymentRepository.getPendingCreditCardPayments(10, 3, 2026)
                } returns emptyList()

                service.payInvoice(10, 9, invoiceDate, BigDecimal.ZERO)

                Then("should not change wallet balance") {
                    wallet.balance.compareTo(BigDecimal("5000.00")) shouldBe 0
                }

                Then("should not change available rebate") {
                    creditCard.availableRebate.compareTo(BigDecimal("100.00")) shouldBe 0
                }
            }
        }
    })
