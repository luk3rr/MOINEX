package org.moinex.service.creditcard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.factory.CategoryFactory
import org.moinex.factory.creditcard.CreditCardDebtFactory
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.creditcard.CreditCardOperatorFactory
import org.moinex.model.creditcard.CreditCardPayment
import org.moinex.repository.creditcard.CreditCardCreditRepository
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardOperatorRepository
import org.moinex.repository.creditcard.CreditCardPaymentRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.service.creditcard.CreditCardService
import org.moinex.util.Constants
import java.math.BigDecimal
import java.time.YearMonth
import java.util.Optional

class CreditCardServiceCreateDebtTest :
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

        Given("a valid debt with single installment") {
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
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("500.00"),
                    installments = 1,
                    description = "Single installment debt",
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("creating the debt") {
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(1) } returns BigDecimal("0.00")
                every { creditCardDebtRepository.save(debt) } returns debt
                every { creditCardPaymentRepository.saveAll(any<List<CreditCardPayment>>()) } returns emptyList()

                service.createDebt(debt, invoiceMonth)

                Then("should save the debt") {
                    verify { creditCardDebtRepository.save(debt) }
                }

                Then("should create exactly one payment") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments.size == 1
                            },
                        )
                    }
                }

                Then("payment should have correct amount") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments[0].amount == BigDecimal("500.00")
                            },
                        )
                    }
                }

                Then("payment should have correct installment number") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments[0].installment == 1
                            },
                        )
                    }
                }

                Then("payment should have correct date") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments[0].date.year == 2026 && payments[0].date.monthValue == 3 && payments[0].date.dayOfMonth == 15
                            },
                        )
                    }
                }
            }
        }

        Given("a valid debt with multiple installments") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    billingDueDay = 20,
                    maxDebt = BigDecimal("10000.00"),
                    operator = operator,
                )
            val category = CategoryFactory.create(id = 1)
            val debt =
                CreditCardDebtFactory.create(
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("1000.00"),
                    installments = 3,
                    description = "Three installments debt",
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("creating the debt") {
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(1) } returns BigDecimal("0.00")
                every { creditCardDebtRepository.save(debt) } returns debt
                every { creditCardPaymentRepository.saveAll(any<List<CreditCardPayment>>()) } returns emptyList()

                service.createDebt(debt, invoiceMonth)

                Then("should create exactly three payments") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments.size == 3
                            },
                        )
                    }
                }

                Then("payments should be distributed correctly") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments[0].installment == 1 &&
                                    payments[1].installment == 2 &&
                                    payments[2].installment == 3
                            },
                        )
                    }
                }

                Then("first payment should include remainder") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments[0].amount == BigDecimal("333.34")
                            },
                        )
                    }
                }

                Then("subsequent payments should have equal amounts") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments[1].amount == BigDecimal("333.33") &&
                                    payments[2].amount == BigDecimal("333.33")
                            },
                        )
                    }
                }

                Then("payments should have correct dates") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments[0].date.year == 2026 &&
                                    payments[0].date.monthValue == 3 &&
                                    payments[1].date.year == 2026 &&
                                    payments[1].date.monthValue == 4 &&
                                    payments[2].date.year == 2026 &&
                                    payments[2].date.monthValue == 5
                            },
                        )
                    }
                }
            }
        }

        Given("a debt with amount that divides evenly by installments") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Mastercard")
            val creditCard =
                CreditCardFactory.create(
                    id = 2,
                    name = "Test Card 2",
                    billingDueDay = 10,
                    maxDebt = BigDecimal("5000.00"),
                    operator = operator,
                )
            val category = CategoryFactory.create(id = 1)
            val debt =
                CreditCardDebtFactory.create(
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("600.00"),
                    installments = 3,
                    description = "Even division debt",
                )
            val invoiceMonth = YearMonth.of(2026, 4)

            When("creating the debt") {
                every { creditCardRepository.findById(2) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(2) } returns BigDecimal("0.00")
                every { creditCardDebtRepository.save(debt) } returns debt
                every { creditCardPaymentRepository.saveAll(any<List<CreditCardPayment>>()) } returns emptyList()

                service.createDebt(debt, invoiceMonth)

                Then("all payments should have equal amounts") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments[0].amount == BigDecimal("200.00") &&
                                    payments[1].amount == BigDecimal("200.00") &&
                                    payments[2].amount == BigDecimal("200.00")
                            },
                        )
                    }
                }
            }
        }

        Given("a debt that exceeds available credit") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    billingDueDay = 15,
                    maxDebt = BigDecimal("1000.00"),
                    operator = operator,
                )
            val category = CategoryFactory.create(id = 1)
            val debt =
                CreditCardDebtFactory.create(
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("2000.00"),
                    installments = 1,
                    description = "Exceeds credit debt",
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("creating the debt") {
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(1) } returns BigDecimal("500.00")

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.createDebt(debt, invoiceMonth)
                    }
                }

                Then("should not save the debt") {
                    verify(exactly = 0) { creditCardDebtRepository.save(any()) }
                }
            }
        }

        Given("a debt with exactly available credit") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    billingDueDay = 15,
                    maxDebt = BigDecimal("1000.00"),
                    operator = operator,
                )
            val category = CategoryFactory.create(id = 1)
            val debt =
                CreditCardDebtFactory.create(
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("500.00"),
                    installments = 1,
                    description = "Exact credit debt",
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("creating the debt") {
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(1) } returns BigDecimal("500.00")
                every { creditCardDebtRepository.save(debt) } returns debt
                every { creditCardPaymentRepository.saveAll(any<List<CreditCardPayment>>()) } returns emptyList()

                service.createDebt(debt, invoiceMonth)

                Then("should save the debt successfully") {
                    verify { creditCardDebtRepository.save(debt) }
                }
            }
        }

        Given("a debt with many installments") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    billingDueDay = 15,
                    maxDebt = BigDecimal("100000.00"),
                    operator = operator,
                )
            val category = CategoryFactory.create(id = 1)
            val debt =
                CreditCardDebtFactory.create(
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("12000.00"),
                    installments = 12,
                    description = "Twelve installments debt",
                )
            val invoiceMonth = YearMonth.of(2026, 1)

            When("creating the debt") {
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(1) } returns BigDecimal("0.00")
                every { creditCardDebtRepository.save(debt) } returns debt
                every { creditCardPaymentRepository.saveAll(any<List<CreditCardPayment>>()) } returns emptyList()

                service.createDebt(debt, invoiceMonth)

                Then("should create exactly twelve payments") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments.size == 12
                            },
                        )
                    }
                }

                Then("payments should span twelve months") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments[0].date.monthValue == 1 &&
                                    payments[11].date.monthValue == 12 &&
                                    payments[11].date.year == 2026
                            },
                        )
                    }
                }

                Then("total of all payments should equal debt amount") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments.sumOf { it.amount } == BigDecimal("12000.00")
                            },
                        )
                    }
                }
            }
        }

        Given("a debt with billing due day at month end") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    billingDueDay = Constants.MAX_BILLING_DUE_DAY,
                    maxDebt = BigDecimal("10000.00"),
                    operator = operator,
                )
            val category = CategoryFactory.create(id = 1)
            val debt =
                CreditCardDebtFactory.create(
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("1000.00"),
                    installments = 2,
                    description = "End of month billing",
                )
            val invoiceMonth = YearMonth.of(2026, 2)

            When("creating the debt") {
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(1) } returns BigDecimal("0.00")
                every { creditCardDebtRepository.save(debt) } returns debt
                every { creditCardPaymentRepository.saveAll(any<List<CreditCardPayment>>()) } returns emptyList()

                service.createDebt(debt, invoiceMonth)

                Then("should handle month-end dates correctly") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments[0].date.dayOfMonth == Constants.MAX_BILLING_DUE_DAY &&
                                    payments[1].date.dayOfMonth == Constants.MAX_BILLING_DUE_DAY
                            },
                        )
                    }
                }
            }
        }

        Given("a debt with very small amount") {
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
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("0.01"),
                    installments = 1,
                    description = "Minimal debt",
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("creating the debt") {
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(1) } returns BigDecimal("0.00")
                every { creditCardDebtRepository.save(debt) } returns debt
                every { creditCardPaymentRepository.saveAll(any<List<CreditCardPayment>>()) } returns emptyList()

                service.createDebt(debt, invoiceMonth)

                Then("should save the debt successfully") {
                    verify { creditCardDebtRepository.save(debt) }
                }

                Then("payment should have correct amount") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments[0].amount == BigDecimal("0.01")
                            },
                        )
                    }
                }
            }
        }

        Given("a debt with large amount") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    billingDueDay = 15,
                    maxDebt = BigDecimal("1000000.00"),
                    operator = operator,
                )
            val category = CategoryFactory.create(id = 1)
            val debt =
                CreditCardDebtFactory.create(
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("999999.99"),
                    installments = 1,
                    description = "Large debt",
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("creating the debt") {
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(1) } returns BigDecimal("0.00")
                every { creditCardDebtRepository.save(debt) } returns debt
                every { creditCardPaymentRepository.saveAll(any<List<CreditCardPayment>>()) } returns emptyList()

                service.createDebt(debt, invoiceMonth)

                Then("should save the debt successfully") {
                    verify { creditCardDebtRepository.save(debt) }
                }

                Then("payment should have correct amount") {
                    verify {
                        creditCardPaymentRepository.saveAll(
                            match<List<CreditCardPayment>> { payments ->
                                payments[0].amount == BigDecimal("999999.99")
                            },
                        )
                    }
                }
            }
        }

        Given("multiple debts on same credit card") {
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
            val debt1 =
                CreditCardDebtFactory.create(
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("300.00"),
                    installments = 1,
                    description = "First debt",
                )
            val debt2 =
                CreditCardDebtFactory.create(
                    creditCard = creditCard,
                    category = category,
                    amount = BigDecimal("200.00"),
                    installments = 1,
                    description = "Second debt",
                )
            val invoiceMonth = YearMonth.of(2026, 3)

            When("creating multiple debts sequentially") {
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(1) } returnsMany
                    listOf(
                        BigDecimal("0.00"),
                        BigDecimal("300.00"),
                    )
                every { creditCardDebtRepository.save(any()) } returnsMany listOf(debt1, debt2)
                every { creditCardPaymentRepository.saveAll(any<List<CreditCardPayment>>()) } returns emptyList()

                service.createDebt(debt1, invoiceMonth)
                service.createDebt(debt2, invoiceMonth)

                Then("should save both debts") {
                    verify(exactly = 2) { creditCardDebtRepository.save(any()) }
                }

                Then("should create payments for both debts") {
                    verify(exactly = 2) { creditCardPaymentRepository.saveAll(any<List<CreditCardPayment>>()) }
                }
            }
        }
    })
