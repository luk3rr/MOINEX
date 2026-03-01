package org.moinex.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.factory.CategoryFactory
import org.moinex.factory.CreditCardDebtFactory
import org.moinex.factory.CreditCardFactory
import org.moinex.factory.CreditCardOperatorFactory
import org.moinex.factory.CreditCardPaymentFactory
import org.moinex.factory.WalletFactory
import org.moinex.model.creditcard.CreditCardCredit
import org.moinex.model.enums.CreditCardCreditType
import org.moinex.repository.creditcard.CreditCardCreditRepository
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardOperatorRepository
import org.moinex.repository.creditcard.CreditCardPaymentRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.wallettransaction.WalletRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class CreditCardServiceRefundDebtTest :
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

        Given("a debt with mixed paid and unpaid payments") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    billingDueDay = 15,
                    maxDebt = BigDecimal("10000.00"),
                    operator = operator,
                )
            val category = CategoryFactory.create(id = 1)
            val debt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("900.00"),
                    installments = 3,
                    description = "Debt to refund",
                )
            val wallet1 = WalletFactory.create(id = 1)
            val payment1 =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = debt,
                    amount = BigDecimal("300.00"),
                    installment = 1,
                    date = LocalDateTime.of(2026, 3, 15, 23, 59),
                    refunded = false,
                    wallet = wallet1,
                )
            val payment2 =
                CreditCardPaymentFactory.create(
                    id = 2,
                    creditCardDebt = debt,
                    amount = BigDecimal("300.00"),
                    installment = 2,
                    date = LocalDateTime.of(2026, 4, 15, 23, 59),
                    refunded = false,
                )
            val payment3 =
                CreditCardPaymentFactory.create(
                    id = 3,
                    creditCardDebt = debt,
                    amount = BigDecimal("300.00"),
                    installment = 3,
                    date = LocalDateTime.of(2026, 5, 15, 23, 59),
                    refunded = false,
                    wallet = WalletFactory.create(id = 2),
                )

            When("refunding debt with mixed payment statuses") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(debt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns
                    listOf(payment1, payment2, payment3)
                every { creditCardPaymentRepository.getNextInvoiceDate(1) } returns
                    LocalDateTime.of(2026, 6, 15, 23, 59)
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardCreditRepository.save(any()) } returns mockk()

                service.refundDebt(1)

                Then("should mark all payments as refunded") {
                    payment1.refunded shouldBe true
                    payment2.refunded shouldBe true
                    payment3.refunded shouldBe true
                }

                Then("should calculate total refund for paid payments only") {
                    verify {
                        creditCardCreditRepository.save(
                            match<CreditCardCredit> { credit ->
                                credit.amount == BigDecimal("600.00") &&
                                    credit.type == CreditCardCreditType.REFUND
                            },
                        )
                    }
                }
            }
        }

        Given("a debt with all paid payments") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    billingDueDay = 15,
                    maxDebt = BigDecimal("10000.00"),
                    operator = operator,
                )
            val category = CategoryFactory.create(id = 1)
            val debt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("600.00"),
                    installments = 2,
                    description = "Debt with all paid",
                )
            val wallet1 = WalletFactory.create(id = 1)
            val wallet2 = WalletFactory.create(id = 2)
            val payment1 =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = debt,
                    amount = BigDecimal("300.00"),
                    installment = 1,
                    date = LocalDateTime.of(2026, 3, 15, 23, 59),
                    refunded = false,
                    wallet = wallet1,
                )
            val payment2 =
                CreditCardPaymentFactory.create(
                    id = 2,
                    creditCardDebt = debt,
                    amount = BigDecimal("300.00"),
                    installment = 2,
                    date = LocalDateTime.of(2026, 4, 15, 23, 59),
                    refunded = false,
                    wallet = wallet2,
                )

            When("refunding debt with all paid payments") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(debt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns
                    listOf(payment1, payment2)
                every { creditCardPaymentRepository.getNextInvoiceDate(1) } returns
                    LocalDateTime.of(2026, 5, 15, 23, 59)
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardCreditRepository.save(any()) } returns mockk()

                service.refundDebt(1)

                Then("should mark all payments as refunded") {
                    payment1.refunded shouldBe true
                    payment2.refunded shouldBe true
                }

                Then("should create credit with total refund amount") {
                    verify {
                        creditCardCreditRepository.save(
                            match<CreditCardCredit> { credit ->
                                credit.amount == BigDecimal("600.00") &&
                                    credit.type == CreditCardCreditType.REFUND
                            },
                        )
                    }
                }
            }
        }

        Given("a debt with all unpaid payments") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    billingDueDay = 15,
                    maxDebt = BigDecimal("10000.00"),
                    operator = operator,
                )
            val category = CategoryFactory.create(id = 1)
            val debt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("500.00"),
                    installments = 2,
                    description = "Debt with all unpaid",
                )
            val payment1 =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = debt,
                    amount = BigDecimal("250.00"),
                    installment = 1,
                    date = LocalDateTime.of(2026, 3, 15, 23, 59),
                    refunded = false,
                )
            val payment2 =
                CreditCardPaymentFactory.create(
                    id = 2,
                    creditCardDebt = debt,
                    amount = BigDecimal("250.00"),
                    installment = 2,
                    date = LocalDateTime.of(2026, 4, 15, 23, 59),
                    refunded = false,
                )

            When("refunding debt with all unpaid payments") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(debt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns
                    listOf(payment1, payment2)
                every { creditCardPaymentRepository.getNextInvoiceDate(1) } returns
                    LocalDateTime.of(2026, 5, 15, 23, 59)
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardCreditRepository.save(any()) } returns mockk()

                service.refundDebt(1)

                Then("should mark all payments as refunded") {
                    payment1.refunded shouldBe true
                    payment2.refunded shouldBe true
                }

                Then("should update unpaid payment dates to next invoice date") {
                    payment1.date shouldBe LocalDateTime.of(2026, 5, 15, 23, 59)
                    payment2.date shouldBe LocalDateTime.of(2026, 5, 15, 23, 59)
                }

                Then("should not create credit when no payments were paid") {
                    verify(exactly = 0) {
                        creditCardCreditRepository.save(any())
                    }
                }
            }
        }

        Given("a debt that has already been refunded") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    billingDueDay = 15,
                    maxDebt = BigDecimal("10000.00"),
                    operator = operator,
                )
            val category = CategoryFactory.create(id = 1)
            val debt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("500.00"),
                    installments = 1,
                    description = "Already refunded debt",
                )
            val refundedPayment =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = debt,
                    amount = BigDecimal("500.00"),
                    installment = 1,
                    date = LocalDateTime.of(2026, 3, 15, 23, 59),
                    refunded = true,
                )

            When("attempting to refund already refunded debt") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(debt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns
                    listOf(refundedPayment)

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.refundDebt(1)
                    }
                }
            }
        }

        Given("a debt with some paid and some unpaid payments") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    billingDueDay = 15,
                    maxDebt = BigDecimal("10000.00"),
                    operator = operator,
                )
            val category = CategoryFactory.create(id = 1)
            val debt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("1000.00"),
                    installments = 4,
                    description = "Mixed payment debt",
                )
            val wallet1 = WalletFactory.create(id = 1)
            val wallet3 = WalletFactory.create(id = 3)
            val payment1 =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = debt,
                    amount = BigDecimal("250.00"),
                    installment = 1,
                    date = LocalDateTime.of(2026, 3, 15, 23, 59),
                    refunded = false,
                    wallet = wallet1,
                )
            val payment2 =
                CreditCardPaymentFactory.create(
                    id = 2,
                    creditCardDebt = debt,
                    amount = BigDecimal("250.00"),
                    installment = 2,
                    date = LocalDateTime.of(2026, 4, 15, 23, 59),
                    refunded = false,
                )
            val payment3 =
                CreditCardPaymentFactory.create(
                    id = 3,
                    creditCardDebt = debt,
                    amount = BigDecimal("250.00"),
                    installment = 3,
                    date = LocalDateTime.of(2026, 5, 15, 23, 59),
                    refunded = false,
                    wallet = wallet3,
                )
            val payment4 =
                CreditCardPaymentFactory.create(
                    id = 4,
                    creditCardDebt = debt,
                    amount = BigDecimal("250.00"),
                    installment = 4,
                    date = LocalDateTime.of(2026, 6, 15, 23, 59),
                    refunded = false,
                )

            When("refunding debt with mixed payment statuses") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(debt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns
                    listOf(payment1, payment2, payment3, payment4)
                every { creditCardPaymentRepository.getNextInvoiceDate(1) } returns
                    LocalDateTime.of(2026, 7, 15, 23, 59)
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardCreditRepository.save(any()) } returns mockk()

                service.refundDebt(1)

                Then("should mark all payments as refunded") {
                    payment1.refunded shouldBe true
                    payment2.refunded shouldBe true
                    payment3.refunded shouldBe true
                    payment4.refunded shouldBe true
                }

                Then("should update unpaid payment dates") {
                    payment2.date shouldBe LocalDateTime.of(2026, 7, 15, 23, 59)
                    payment4.date shouldBe LocalDateTime.of(2026, 7, 15, 23, 59)
                }

                Then("should create credit with only paid payments amount") {
                    verify {
                        creditCardCreditRepository.save(
                            match<CreditCardCredit> { credit ->
                                credit.amount == BigDecimal("500.00") &&
                                    credit.type == CreditCardCreditType.REFUND
                            },
                        )
                    }
                }
            }
        }
    })
