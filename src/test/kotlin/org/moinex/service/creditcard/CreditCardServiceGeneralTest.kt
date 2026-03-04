package org.moinex.service.creditcard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.common.ClockProvider
import org.moinex.factory.CreditCardDebtFactory
import org.moinex.factory.CreditCardFactory
import org.moinex.factory.CreditCardOperatorFactory
import org.moinex.factory.CreditCardPaymentFactory
import org.moinex.model.enums.CreditCardInvoiceStatus
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

class CreditCardServiceGeneralTest :
    BehaviorSpec({
        val creditCardRepository = mockk<CreditCardRepository>()
        val creditCardOperatorRepository = mockk<CreditCardOperatorRepository>()
        val walletRepository = mockk<WalletRepository>()
        val creditCardDebtRepository = mockk<CreditCardDebtRepository>()
        val creditCardPaymentRepository = mockk<CreditCardPaymentRepository>()
        val creditCardCreditRepository = mockk<CreditCardCreditRepository>()

        val fixedNow = LocalDateTime.of(2026, 3, 2, 19, 0)
        val clockProvider = mockk<ClockProvider>()

        val service =
            CreditCardService(
                creditCardDebtRepository,
                creditCardPaymentRepository,
                creditCardRepository,
                creditCardOperatorRepository,
                walletRepository,
                creditCardCreditRepository,
                clockProvider,
            )

        beforeContainer {
            every { clockProvider.now() } returns fixedNow
        }

        afterContainer { clearAllMocks(answers = true) }

        Given("a valid credit card with no debts") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    operator = operator,
                )

            When("deleting the credit card") {
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardDebtRepository.getDebtCountByCreditCard(1) } returns 0
                every { creditCardRepository.delete(creditCard) } returns Unit

                service.deleteCreditCard(1)

                Then("should delete the credit card") {
                    verify { creditCardRepository.delete(creditCard) }
                }
            }
        }

        Given("a credit card with existing debts") {
            val operator = CreditCardOperatorFactory.create(id = 2, name = "Mastercard")
            val creditCard =
                CreditCardFactory.create(
                    id = 2,
                    name = "Test Card 2",
                    operator = operator,
                )

            When("attempting to delete a credit card with debts") {
                every { creditCardRepository.findById(2) } returns Optional.of(creditCard)
                every { creditCardDebtRepository.getDebtCountByCreditCard(2) } returns 3

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.deleteCreditCard(2)
                    }
                }
            }
        }

        Given("a non-existent credit card to delete") {
            When("attempting to delete a non-existent credit card") {
                every { creditCardRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<Exception> {
                        service.deleteCreditCard(999)
                    }
                }
            }
        }

        Given("a credit card with zero debts") {
            val operator = CreditCardOperatorFactory.create(id = 3, name = "Amex")
            val creditCard =
                CreditCardFactory.create(
                    id = 3,
                    name = "Test Card 3",
                    operator = operator,
                )

            When("deleting a credit card with exactly zero debts") {
                every { creditCardRepository.findById(3) } returns Optional.of(creditCard)
                every { creditCardDebtRepository.getDebtCountByCreditCard(3) } returns 0
                every { creditCardRepository.delete(creditCard) } returns Unit

                service.deleteCreditCard(3)

                Then("should successfully delete the credit card") {
                    verify { creditCardRepository.delete(creditCard) }
                }
            }
        }

        Given("a debt with associated payments") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard = CreditCardFactory.create(id = 1, operator = operator)
            val debt =
                CreditCardDebtFactory.create(
                    id = 1,
                    creditCard = creditCard,
                    amount = BigDecimal("500.00"),
                    installments = 3,
                )

            val payment1 = CreditCardPaymentFactory.create(id = 1, amount = BigDecimal("166.67"))
            val payment2 = CreditCardPaymentFactory.create(id = 2, amount = BigDecimal("166.67"))
            val payment3 = CreditCardPaymentFactory.create(id = 3, amount = BigDecimal("166.66"))

            When("deleting the debt") {
                every { creditCardDebtRepository.findById(1) } returns Optional.of(debt)
                every {
                    creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(1)
                } returns listOf(payment1, payment2, payment3)
                every { creditCardPaymentRepository.findById(1) } returns Optional.of(payment1)
                every { creditCardPaymentRepository.findById(2) } returns Optional.of(payment2)
                every { creditCardPaymentRepository.findById(3) } returns Optional.of(payment3)
                every { creditCardPaymentRepository.delete(any()) } returns Unit
                every { creditCardDebtRepository.delete(debt) } returns Unit

                service.deleteDebt(1)

                Then("should delete all associated payments") {
                    verify(exactly = 3) { creditCardPaymentRepository.delete(any()) }
                }

                Then("should delete the debt") {
                    verify { creditCardDebtRepository.delete(debt) }
                }
            }
        }

        Given("a debt with no payments") {
            val operator = CreditCardOperatorFactory.create(id = 2, name = "Mastercard")
            val creditCard = CreditCardFactory.create(id = 2, operator = operator)
            val debt =
                CreditCardDebtFactory.create(
                    id = 2,
                    creditCard = creditCard,
                    amount = BigDecimal("100.00"),
                )

            When("deleting a debt with no payments") {
                every { creditCardDebtRepository.findById(2) } returns Optional.of(debt)
                every {
                    creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(2)
                } returns emptyList()
                every { creditCardDebtRepository.delete(debt) } returns Unit

                service.deleteDebt(2)

                Then("should delete the debt") {
                    verify { creditCardDebtRepository.delete(debt) }
                }
            }
        }

        Given("a non-existent debt") {
            When("attempting to delete a non-existent debt") {
                every { creditCardDebtRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<Exception> {
                        service.deleteDebt(999)
                    }
                }
            }
        }

        Given("a debt with multiple installments") {
            val operator = CreditCardOperatorFactory.create(id = 3, name = "Amex")
            val creditCard = CreditCardFactory.create(id = 3, operator = operator)
            val debt =
                CreditCardDebtFactory.create(
                    id = 3,
                    creditCard = creditCard,
                    amount = BigDecimal("1200.00"),
                    installments = 12,
                )

            val payments =
                (1..12).map { i ->
                    CreditCardPaymentFactory.create(
                        id = i,
                        amount = BigDecimal("100.00"),
                    )
                }

            When("deleting a debt with 12 installments") {
                every { creditCardDebtRepository.findById(3) } returns Optional.of(debt)
                every {
                    creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(3)
                } returns payments
                payments.forEach { payment ->
                    every { creditCardPaymentRepository.findById(payment.id!!) } returns Optional.of(payment)
                    every { creditCardPaymentRepository.delete(payment) } returns Unit
                }
                every { creditCardDebtRepository.delete(debt) } returns Unit

                service.deleteDebt(3)

                Then("should delete all 12 payments") {
                    verify(exactly = 12) { creditCardPaymentRepository.delete(any()) }
                }

                Then("should delete the debt") {
                    verify { creditCardDebtRepository.delete(debt) }
                }
            }
        }

        Given("a credit card with no pending payments") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    operator = operator,
                    isArchived = false,
                )

            When("archiving the credit card") {
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(1) } returns BigDecimal.ZERO

                service.archiveCreditCard(1)

                Then("should set isArchived to true") {
                    creditCard.isArchived shouldBe true
                }
            }
        }

        Given("a credit card with pending payments") {
            val operator = CreditCardOperatorFactory.create(id = 2, name = "Mastercard")
            val creditCard =
                CreditCardFactory.create(
                    id = 2,
                    name = "Test Card 2",
                    operator = operator,
                    isArchived = false,
                )

            When("attempting to archive a credit card with pending payments") {
                every { creditCardRepository.findById(2) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(2) } returns BigDecimal("500.00")

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.archiveCreditCard(2)
                    }
                }
            }
        }

        Given("a non-existent credit card to archive") {
            When("attempting to archive a non-existent credit card") {
                every { creditCardRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<Exception> {
                        service.archiveCreditCard(999)
                    }
                }
            }
        }

        Given("an already archived credit card") {
            val operator = CreditCardOperatorFactory.create(id = 3, name = "Amex")
            val creditCard =
                CreditCardFactory.create(
                    id = 3,
                    name = "Test Card 3",
                    operator = operator,
                    isArchived = true,
                )

            When("archiving an already archived credit card") {
                every { creditCardRepository.findById(3) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(3) } returns BigDecimal.ZERO

                service.archiveCreditCard(3)

                Then("should remain archived") {
                    creditCard.isArchived shouldBe true
                }
            }
        }

        Given("an archived credit card") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Test Card",
                    operator = operator,
                    isArchived = true,
                )

            When("unarchiving the credit card") {
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)

                service.unarchiveCreditCard(1)

                Then("should set isArchived to false") {
                    creditCard.isArchived shouldBe false
                }
            }
        }

        Given("a non-archived credit card") {
            val operator = CreditCardOperatorFactory.create(id = 2, name = "Mastercard")
            val creditCard =
                CreditCardFactory.create(
                    id = 2,
                    name = "Test Card 2",
                    operator = operator,
                    isArchived = false,
                )

            When("unarchiving a non-archived credit card") {
                every { creditCardRepository.findById(2) } returns Optional.of(creditCard)

                service.unarchiveCreditCard(2)

                Then("should remain non-archived") {
                    creditCard.isArchived shouldBe false
                }
            }
        }

        Given("a non-existent credit card to unarchive") {
            When("attempting to unarchive a non-existent credit card") {
                every { creditCardRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<Exception> {
                        service.unarchiveCreditCard(999)
                    }
                }
            }
        }

        Given("a date in the past with debts and payments") {
            val pastDate = LocalDateTime.of(2025, 12, 15, 12, 0)

            When("getting debt at a past date") {
                every {
                    creditCardDebtRepository.findAllCreatedUpToDate(pastDate)
                } returns
                    listOf(
                        CreditCardDebtFactory.create(amount = BigDecimal("500.00")),
                        CreditCardDebtFactory.create(amount = BigDecimal("300.00")),
                    )

                every {
                    creditCardPaymentRepository.findAllPaidUpToDate(pastDate)
                } returns
                    listOf(
                        CreditCardPaymentFactory.create(amount = BigDecimal("200.00")),
                    )

                val result = service.getDebtAtDate(pastDate)

                Then("should return correct debt calculation") {
                    result.compareTo(BigDecimal("600.00")) shouldBe 0
                }
            }
        }

        Given("a date in the future with pending payments") {
            val futureDate = LocalDateTime.of(2026, 6, 15, 12, 0)

            When("getting debt at a future date") {
                every {
                    creditCardDebtRepository.findAllCreatedUpToDate(futureDate)
                } returns
                    listOf(
                        CreditCardDebtFactory.create(amount = BigDecimal("1000.00")),
                    )

                every {
                    creditCardPaymentRepository.findAllPaidUpToDate(futureDate)
                } returns emptyList()

                every {
                    creditCardPaymentRepository.getPendingPaymentsByMonth(3, 2026)
                } returns BigDecimal("100.00")
                every {
                    creditCardPaymentRepository.getPendingPaymentsByMonth(4, 2026)
                } returns BigDecimal("100.00")
                every {
                    creditCardPaymentRepository.getPendingPaymentsByMonth(5, 2026)
                } returns BigDecimal("100.00")
                every {
                    creditCardPaymentRepository.getPendingPaymentsByMonth(6, 2026)
                } returns BigDecimal("100.00")

                val result = service.getDebtAtDate(futureDate)

                Then("should include projected pending payments") {
                    result.compareTo(BigDecimal.ZERO) shouldBe 1
                }
            }
        }

        Given("a date with no debts") {
            val date = LocalDateTime.of(2026, 3, 15, 12, 0)

            When("getting debt at a date with no debts") {
                every {
                    creditCardDebtRepository.findAllCreatedUpToDate(date)
                } returns emptyList()

                every {
                    creditCardPaymentRepository.findAllPaidUpToDate(date)
                } returns emptyList()

                every {
                    creditCardPaymentRepository.getPendingPaymentsByMonth(3, 2026)
                } returns BigDecimal.ZERO

                val result = service.getDebtAtDate(date)

                Then("should return zero") {
                    result.compareTo(BigDecimal.ZERO) shouldBe 0
                }
            }
        }

        Given("a date with debts and full payments") {
            val date = LocalDateTime.of(2025, 12, 15, 12, 0)

            When("getting debt at a date where all debts are paid") {
                every {
                    creditCardDebtRepository.findAllCreatedUpToDate(date)
                } returns
                    listOf(
                        CreditCardDebtFactory.create(amount = BigDecimal("1000.00")),
                    )

                every {
                    creditCardPaymentRepository.findAllPaidUpToDate(date)
                } returns
                    listOf(
                        CreditCardPaymentFactory.create(amount = BigDecimal("1000.00")),
                    )

                val result = service.getDebtAtDate(date)

                Then("should return zero") {
                    result.compareTo(BigDecimal.ZERO) shouldBe 0
                }
            }
        }

        Given("a date in the future with multiple months of pending payments") {
            val futureDate = LocalDateTime.of(2026, 8, 15, 12, 0)

            When("getting debt at a future date spanning multiple months") {
                every {
                    creditCardDebtRepository.findAllCreatedUpToDate(futureDate)
                } returns
                    listOf(
                        CreditCardDebtFactory.create(amount = BigDecimal("2000.00")),
                    )

                every {
                    creditCardPaymentRepository.findAllPaidUpToDate(futureDate)
                } returns emptyList()

                every {
                    creditCardPaymentRepository.getPendingPaymentsByMonth(3, 2026)
                } returns BigDecimal("100.00")
                every {
                    creditCardPaymentRepository.getPendingPaymentsByMonth(4, 2026)
                } returns BigDecimal("100.00")
                every {
                    creditCardPaymentRepository.getPendingPaymentsByMonth(5, 2026)
                } returns BigDecimal("100.00")
                every {
                    creditCardPaymentRepository.getPendingPaymentsByMonth(6, 2026)
                } returns BigDecimal("100.00")
                every {
                    creditCardPaymentRepository.getPendingPaymentsByMonth(7, 2026)
                } returns BigDecimal("100.00")
                every {
                    creditCardPaymentRepository.getPendingPaymentsByMonth(8, 2026)
                } returns BigDecimal("100.00")

                val result = service.getDebtAtDate(futureDate)

                Then("should accumulate pending payments for all months until date") {
                    result.compareTo(BigDecimal.ZERO) shouldBe 1
                }
            }
        }

        Given("a date with partial payments") {
            val date = LocalDateTime.of(2025, 12, 15, 12, 0)

            When("getting debt at a date with partial payments") {
                every {
                    creditCardDebtRepository.findAllCreatedUpToDate(date)
                } returns
                    listOf(
                        CreditCardDebtFactory.create(amount = BigDecimal("1000.00")),
                        CreditCardDebtFactory.create(amount = BigDecimal("500.00")),
                    )

                every {
                    creditCardPaymentRepository.findAllPaidUpToDate(date)
                } returns
                    listOf(
                        CreditCardPaymentFactory.create(amount = BigDecimal("600.00")),
                    )

                val result = service.getDebtAtDate(date)

                Then("should return remaining debt after payments") {
                    result.compareTo(BigDecimal("900.00")) shouldBe 0
                }
            }
        }

        Given("multiple credit cards with different debt counts") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val card1 = CreditCardFactory.create(id = 1, name = "Card 1", operator = operator, isArchived = false)
            val card2 = CreditCardFactory.create(id = 2, name = "Card 2", operator = operator, isArchived = false)
            val card3 = CreditCardFactory.create(id = 3, name = "Card 3", operator = operator, isArchived = false)

            When("getting non-archived cards ordered by debt count descending") {
                every { creditCardRepository.findAllByIsArchivedFalse() } returns listOf(card1, card2, card3)
                every { creditCardDebtRepository.getDebtCountByCreditCard(1) } returns 5
                every { creditCardDebtRepository.getDebtCountByCreditCard(2) } returns 10
                every { creditCardDebtRepository.getDebtCountByCreditCard(3) } returns 3

                val result = service.getAllNonArchivedCreditCardsOrderedByDebtCountDesc()

                Then("should return cards ordered by debt count descending") {
                    result.size shouldBe 3
                    result[0].id shouldBe 2
                    result[1].id shouldBe 1
                    result[2].id shouldBe 3
                }
            }
        }

        Given("a credit card with specific closing and billing days") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    operator = operator,
                    closingDay = 5,
                    billingDueDay = 15,
                )

            When("calculating next invoice date when current day is before closing day") {
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getNextInvoiceDate(1) } returns null

                val result = service.getNextInvoiceDate(creditCard)

                Then("should return billing due day of current month") {
                    result.dayOfMonth shouldBe 15
                    result.monthValue shouldBe 3
                    result.year shouldBe 2026
                }
            }
        }

        Given("a credit card with closing day before current day") {
            val operator = CreditCardOperatorFactory.create(id = 2, name = "Mastercard")
            val creditCard =
                CreditCardFactory.create(
                    id = 2,
                    operator = operator,
                    closingDay = 1,
                    billingDueDay = 15,
                )

            When("calculating next invoice date when current day is after closing day") {
                every { creditCardRepository.findById(2) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getNextInvoiceDate(2) } returns null

                val result = service.getNextInvoiceDate(creditCard)

                Then("should return billing due day of next month") {
                    result.dayOfMonth shouldBe 15
                    result.monthValue shouldBe 4
                    result.year shouldBe 2026
                }
            }
        }

        Given("a credit card with invoice status check") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    id = 1,
                    operator = operator,
                    closingDay = 5,
                    billingDueDay = 15,
                )

            When("checking invoice status for current month") {
                every { creditCardRepository.findById(1) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getNextInvoiceDate(1) } returns LocalDateTime.of(2026, 3, 15, 23, 59)

                val invoiceMonth = YearMonth.of(2026, 3)
                val result = service.getInvoiceStatus(1, invoiceMonth)

                Then("should return OPEN status") {
                    result shouldBe CreditCardInvoiceStatus.OPEN
                }
            }
        }

        Given("a credit card with closed invoice status") {
            val operator = CreditCardOperatorFactory.create(id = 2, name = "Mastercard")
            val creditCard =
                CreditCardFactory.create(
                    id = 2,
                    operator = operator,
                    closingDay = 5,
                    billingDueDay = 15,
                )

            When("checking invoice status for past month") {
                every { creditCardRepository.findById(2) } returns Optional.of(creditCard)
                every { creditCardPaymentRepository.getNextInvoiceDate(2) } returns LocalDateTime.of(2026, 4, 15, 23, 59)

                val invoiceMonth = YearMonth.of(2026, 3)
                val result = service.getInvoiceStatus(2, invoiceMonth)

                Then("should return CLOSED status") {
                    result shouldBe CreditCardInvoiceStatus.CLOSED
                }
            }
        }
    })
