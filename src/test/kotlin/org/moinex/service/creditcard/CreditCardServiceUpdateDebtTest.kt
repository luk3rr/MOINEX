package org.moinex.service.creditcard

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
import org.moinex.model.creditcard.CreditCardPayment
import org.moinex.repository.creditcard.CreditCardCreditRepository
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardOperatorRepository
import org.moinex.repository.creditcard.CreditCardPaymentRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.service.CreditCardService
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Optional

class CreditCardServiceUpdateDebtTest :
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

        Given("a valid debt with updated amount") {
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
            val originalDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("500.00"),
                    installments = 1,
                    description = "Original debt",
                )
            val updatedDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("600.00"),
                    installments = 1,
                    description = "Updated debt",
                )
            val payment =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("500.00"),
                    installment = 1,
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("updating the debt") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(originalDebt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns listOf(payment)
                every { creditCardRepository.existsById(1) } returns true

                service.updateDebt(updatedDebt, invoiceMonth)

                Then("should update the debt amount") {
                    originalDebt.amount shouldBe BigDecimal("600.00")
                }

                Then("should update the debt description") {
                    originalDebt.description shouldBe "Updated debt"
                }
            }
        }

        Given("a debt with updated invoice month") {
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
            val originalDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("500.00"),
                    installments = 1,
                    description = "Original debt",
                )
            val updatedDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("500.00"),
                    installments = 1,
                    description = "Original debt",
                )
            val payment =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("500.00"),
                    installment = 1,
                    date = LocalDateTime.of(2026, 3, 15, 23, 59),
                )
            val newInvoiceMonth = YearMonth.of(2026, 4)

            When("updating the invoice month") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(originalDebt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns listOf(payment)
                every { creditCardRepository.existsById(1) } returns true

                service.updateDebt(updatedDebt, newInvoiceMonth)

                Then("should update payment date to new invoice month") {
                    verify {
                        creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1)
                    }
                }
            }
        }

        Given("a debt with updated installments (increase)") {
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
            val originalDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("1000.00"),
                    installments = 2,
                    description = "Original debt",
                )
            val updatedDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("1000.00"),
                    installments = 3,
                    description = "Original debt",
                )
            val payment1 =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("500.00"),
                    installment = 1,
                    date = LocalDateTime.of(2026, 3, 15, 23, 59),
                )
            val payment2 =
                CreditCardPaymentFactory.create(
                    id = 2,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("500.00"),
                    installment = 2,
                    date = LocalDateTime.of(2026, 4, 15, 23, 59),
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("updating installments from 2 to 3") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(originalDebt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns
                    mutableListOf(payment1, payment2)
                every { creditCardRepository.existsById(1) } returns true
                every { creditCardPaymentRepository.saveAll(any<List<CreditCardPayment>>()) } returns emptyList()

                service.updateDebt(updatedDebt, invoiceMonth)

                Then("should update installments count") {
                    originalDebt.installments shouldBe 3
                }

                Then("should create new payment for 3rd installment") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments.size == 1 &&
                                    payments[0].installment == 3
                            },
                        )
                    }
                }

                Then("should update existing payment amounts") {
                    payment1.amount shouldBe BigDecimal("333.34")
                    payment2.amount shouldBe BigDecimal("333.33")
                }
            }
        }

        Given("a debt with updated installments (decrease)") {
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
            val originalDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("1000.00"),
                    installments = 3,
                    description = "Original debt",
                )
            val updatedDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("1000.00"),
                    installments = 2,
                    description = "Original debt",
                )
            val payment1 =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("333.34"),
                    installment = 1,
                    date = LocalDateTime.of(2026, 3, 15, 23, 59),
                )
            val payment2 =
                CreditCardPaymentFactory.create(
                    id = 2,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("333.33"),
                    installment = 2,
                    date = LocalDateTime.of(2026, 4, 15, 23, 59),
                )
            val payment3 =
                CreditCardPaymentFactory.create(
                    id = 3,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("333.33"),
                    installment = 3,
                    date = LocalDateTime.of(2026, 5, 15, 23, 59),
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("updating installments from 3 to 2") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(originalDebt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns
                    mutableListOf(payment1, payment2, payment3)
                every { creditCardRepository.existsById(1) } returns true
                every { creditCardPaymentRepository.findById(3) } returns Optional.of(payment3)
                every { creditCardPaymentRepository.delete(any()) } returns Unit

                service.updateDebt(updatedDebt, invoiceMonth)

                Then("should update installments count") {
                    originalDebt.installments shouldBe 2
                }

                Then("should update remaining payment amounts") {
                    payment1.amount shouldBe BigDecimal("500.00")
                    payment2.amount shouldBe BigDecimal("500.00")
                }

                Then("should delete payment 3") {
                    verify {
                        creditCardPaymentRepository.delete(payment3)
                    }
                }
            }
        }

        Given("a debt with decreased installments and paid payment to delete") {
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
            val wallet = WalletFactory.create(id = 1, balance = BigDecimal("500.00"))
            val originalDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("900.00"),
                    installments = 3,
                    description = "Original debt",
                )
            val updatedDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("900.00"),
                    installments = 2,
                    description = "Original debt",
                )
            val payment1 =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("300.00"),
                    installment = 1,
                    date = LocalDateTime.of(2026, 3, 15, 23, 59),
                )
            val payment2 =
                CreditCardPaymentFactory.create(
                    id = 2,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("300.00"),
                    installment = 2,
                    date = LocalDateTime.of(2026, 4, 15, 23, 59),
                )
            val payment3 =
                CreditCardPaymentFactory.create(
                    id = 3,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("300.00"),
                    installment = 3,
                    date = LocalDateTime.of(2026, 5, 15, 23, 59),
                    wallet = wallet,
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("deleting a paid payment when reducing installments") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(originalDebt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns
                    mutableListOf(payment1, payment2, payment3)
                every { creditCardRepository.existsById(1) } returns true
                every { creditCardPaymentRepository.findById(3) } returns Optional.of(payment3)
                every { creditCardPaymentRepository.delete(any()) } returns Unit

                service.updateDebt(updatedDebt, invoiceMonth)

                Then("should refund payment amount to wallet") {
                    wallet.balance shouldBe BigDecimal("800.00")
                }

                Then("should delete the paid payment") {
                    verify {
                        creditCardPaymentRepository.delete(payment3)
                    }
                }

                Then("should update remaining payments") {
                    payment1.amount shouldBe BigDecimal("450.00")
                    payment2.amount shouldBe BigDecimal("450.00")
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
            val originalDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("500.00"),
                    installments = 1,
                    description = "Original debt",
                )
            val updatedDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("600.00"),
                    installments = 1,
                    description = "Updated debt",
                )
            val refundedPayment =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("500.00"),
                    installment = 1,
                    refunded = true,
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("updating a refunded debt") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(originalDebt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns
                    listOf(refundedPayment)

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.updateDebt(updatedDebt, invoiceMonth)
                    }
                }
            }
        }

        Given("a debt with non-existent credit card") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 999,
                    name = "Non-existent Card",
                    billingDueDay = 15,
                    maxDebt = BigDecimal("10000.00"),
                    operator = operator,
                )
            val category = CategoryFactory.create(id = 1)
            val originalDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("500.00"),
                    installments = 1,
                    description = "Original debt",
                )
            val updatedDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("600.00"),
                    installments = 1,
                    description = "Updated debt",
                )
            val payment =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("500.00"),
                    installment = 1,
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("updating debt with non-existent credit card") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(originalDebt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns listOf(payment)
                every { creditCardRepository.existsById(999) } returns false

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.updateDebt(updatedDebt, invoiceMonth)
                    }
                }
            }
        }

        Given("a debt with updated category") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    billingDueDay = 15,
                    maxDebt = BigDecimal("10000.00"),
                    operator = operator,
                )
            val originalCategory = CategoryFactory.create(id = 1, name = "Food")
            val newCategory = CategoryFactory.create(id = 2, name = "Transport")
            val originalDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = originalCategory,
                    amount = BigDecimal("500.00"),
                    installments = 1,
                    description = "Original debt",
                )
            val updatedDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = newCategory,
                    amount = BigDecimal("500.00"),
                    installments = 1,
                    description = "Original debt",
                )
            val payment =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("500.00"),
                    installment = 1,
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("updating the debt category") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(originalDebt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns listOf(payment)
                every { creditCardRepository.existsById(1) } returns true

                service.updateDebt(updatedDebt, invoiceMonth)

                Then("should update the debt category") {
                    originalDebt.category.id shouldBe 2
                }
            }
        }

        Given("a debt with multiple payments and amount change") {
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
            val originalDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("600.00"),
                    installments = 3,
                    description = "Original debt",
                )
            val updatedDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("900.00"),
                    installments = 3,
                    description = "Original debt",
                )
            val payment1 =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("200.00"),
                    installment = 1,
                )
            val payment2 =
                CreditCardPaymentFactory.create(
                    id = 2,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("200.00"),
                    installment = 2,
                )
            val payment3 =
                CreditCardPaymentFactory.create(
                    id = 3,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("200.00"),
                    installment = 3,
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("updating debt amount with multiple payments") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(originalDebt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns
                    mutableListOf(payment1, payment2, payment3)
                every { creditCardRepository.existsById(1) } returns true

                service.updateDebt(updatedDebt, invoiceMonth)

                Then("should update debt amount") {
                    originalDebt.amount shouldBe BigDecimal("900.00")
                }

                Then("should redistribute payment amounts correctly") {
                    payment1.amount shouldBe BigDecimal("300.00")
                    payment2.amount shouldBe BigDecimal("300.00")
                    payment3.amount shouldBe BigDecimal("300.00")
                }

                Then("total of all payments should equal new debt amount") {
                    (payment1.amount + payment2.amount + payment3.amount) shouldBe BigDecimal("900.00")
                }
            }
        }

        Given("a debt with amount change and paid payment") {
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
            val wallet = WalletFactory.create(id = 1, balance = BigDecimal("1000.00"))
            val originalDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("600.00"),
                    installments = 3,
                    description = "Original debt",
                )
            val updatedDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("900.00"),
                    installments = 3,
                    description = "Original debt",
                )
            val payment1 =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("200.00"),
                    installment = 1,
                    wallet = wallet,
                )
            val payment2 =
                CreditCardPaymentFactory.create(
                    id = 2,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("200.00"),
                    installment = 2,
                )
            val payment3 =
                CreditCardPaymentFactory.create(
                    id = 3,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("200.00"),
                    installment = 3,
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("updating debt amount with a paid payment") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(originalDebt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns
                    mutableListOf(payment1, payment2, payment3)
                every { creditCardRepository.existsById(1) } returns true
                every { walletRepository.save(any()) } returns wallet

                service.updateDebt(updatedDebt, invoiceMonth)

                Then("should update debt amount") {
                    originalDebt.amount shouldBe BigDecimal("900.00")
                }

                Then("should update paid payment amount") {
                    payment1.amount shouldBe BigDecimal("300.00")
                }

                Then("should update wallet balance with difference") {
                    wallet.balance shouldBe BigDecimal("1100.00")
                }

                Then("should update unpaid payment amounts") {
                    payment2.amount shouldBe BigDecimal("300.00")
                    payment3.amount shouldBe BigDecimal("300.00")
                }
            }
        }

        Given("a debt with all fields updated") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    billingDueDay = 15,
                    maxDebt = BigDecimal("10000.00"),
                    operator = operator,
                )
            val originalCategory = CategoryFactory.create(id = 1, name = "Food")
            val newCategory = CategoryFactory.create(id = 2, name = "Transport")
            val originalDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = originalCategory,
                    amount = BigDecimal("500.00"),
                    installments = 1,
                    description = "Original debt",
                )
            val updatedDebt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    category = newCategory,
                    amount = BigDecimal("750.00"),
                    installments = 2,
                    description = "Updated debt with new description",
                )
            val payment =
                CreditCardPaymentFactory.create(
                    id = 1,
                    creditCardDebt = originalDebt,
                    amount = BigDecimal("500.00"),
                    installment = 1,
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("updating all debt fields") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(originalDebt)
                every { creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1) } returns
                    mutableListOf(payment)
                every { creditCardRepository.existsById(1) } returns true
                every { creditCardPaymentRepository.saveAll(any<List<CreditCardPayment>>()) } returns emptyList()

                service.updateDebt(updatedDebt, invoiceMonth)

                Then("should update amount") {
                    originalDebt.amount shouldBe BigDecimal("750.00")
                }

                Then("should update installments") {
                    originalDebt.installments shouldBe 2
                }

                Then("should update category") {
                    originalDebt.category.id shouldBe 2
                }

                Then("should update description") {
                    originalDebt.description shouldBe "Updated debt with new description"
                }

                Then("should update first payment amount") {
                    payment.amount shouldBe BigDecimal("375.00")
                }

                Then("should create new payment for second installment") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments.size == 1 &&
                                    payments[0].installment == 2 &&
                                    payments[0].amount == BigDecimal("375.00")
                            },
                        )
                    }
                }
            }
        }
    })
