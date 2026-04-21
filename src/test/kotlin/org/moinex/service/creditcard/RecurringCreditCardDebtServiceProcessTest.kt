package org.moinex.service.creditcard

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.creditcard.RecurringCreditCardDebtFactory
import org.moinex.model.enums.CreditCardRecurringFrequency
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.creditcard.RecurringCreditCardDebtRepository
import java.time.YearMonth

class RecurringCreditCardDebtServiceProcessTest :
    BehaviorSpec({
        val recurringCreditCardDebtRepository = mockk<RecurringCreditCardDebtRepository>()
        val creditCardDebtRepository = mockk<CreditCardDebtRepository>()
        val creditCardRepository = mockk<CreditCardRepository>()
        val creditCardService = mockk<CreditCardService>()

        val service =
            RecurringCreditCardDebtService(
                recurringCreditCardDebtRepository,
                creditCardDebtRepository,
                creditCardRepository,
                creditCardService,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("one active recurring with nextInvoiceMonth equal to the current month") {
            val now = YearMonth.now()
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    nextInvoiceMonth = now,
                )

            When("processing recurring debts") {
                every {
                    recurringCreditCardDebtRepository.findAllByStatus(RecurringTransactionStatus.ACTIVE)
                } returns listOf(recurring)
                every {
                    recurringCreditCardDebtRepository.findMaterializedDebtForMonth(1, any(), any())
                } returns emptyList()
                every { creditCardService.createDebt(any(), any()) } returns 10

                service.processRecurringDebts()

                Then("should materialize exactly one debt") {
                    verify(exactly = 1) { creditCardService.createDebt(any(), any()) }
                }

                Then("should advance nextInvoiceMonth by one month") {
                    recurring.nextInvoiceMonth shouldBe now.plusMonths(1)
                }
            }
        }

        Given("one active recurring that is two months overdue") {
            val twoMonthsAgo = YearMonth.now().minusMonths(2)
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    nextInvoiceMonth = twoMonthsAgo,
                )

            When("processing recurring debts") {
                every {
                    recurringCreditCardDebtRepository.findAllByStatus(RecurringTransactionStatus.ACTIVE)
                } returns listOf(recurring)
                every {
                    recurringCreditCardDebtRepository.findMaterializedDebtForMonth(1, any(), any())
                } returns emptyList()
                every { creditCardService.createDebt(any(), any()) } returns 10

                service.processRecurringDebts()

                Then("should materialize three debts (overdue months + current)") {
                    verify(exactly = 3) { creditCardService.createDebt(any(), any()) }
                }

                Then("should advance nextInvoiceMonth past the current month") {
                    recurring.nextInvoiceMonth shouldBe YearMonth.now().plusMonths(1)
                }
            }
        }

        Given("a recurring already materialized for the current month") {
            val now = YearMonth.now()
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    nextInvoiceMonth = now,
                )
            val existingDebt = listOf(mockk<org.moinex.model.creditcard.CreditCardDebt>())

            When("processing recurring debts") {
                every {
                    recurringCreditCardDebtRepository.findAllByStatus(RecurringTransactionStatus.ACTIVE)
                } returns listOf(recurring)
                every {
                    recurringCreditCardDebtRepository.findMaterializedDebtForMonth(1, any(), any())
                } returns existingDebt

                service.processRecurringDebts()

                Then("should not create any new debt") {
                    verify(exactly = 0) { creditCardService.createDebt(any(), any()) }
                }

                Then("should still advance nextInvoiceMonth") {
                    recurring.nextInvoiceMonth shouldBe now.plusMonths(1)
                }
            }
        }

        Given("a recurring whose endDate is in the past relative to the current invoice month") {
            val now = YearMonth.now()
            // startDate must be before endDate by at least 1 frequency period
            val startDate = now.minusMonths(6).atDay(1)
            val pastEndDate = now.minusMonths(1).atDay(1)
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    nextInvoiceMonth = now,
                    startDate = startDate,
                    endDate = pastEndDate,
                )

            When("processing recurring debts") {
                every {
                    recurringCreditCardDebtRepository.findAllByStatus(RecurringTransactionStatus.ACTIVE)
                } returns listOf(recurring)

                service.processRecurringDebts()

                Then("should not materialize any debt") {
                    verify(exactly = 0) { creditCardService.createDebt(any(), any()) }
                }

                Then("should deactivate the recurring") {
                    recurring.status shouldBe RecurringTransactionStatus.INACTIVE
                }
            }
        }

        Given("a YEARLY recurring with nextInvoiceMonth equal to the current month") {
            val now = YearMonth.now()
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    frequency = CreditCardRecurringFrequency.YEARLY,
                    nextInvoiceMonth = now,
                )

            When("processing recurring debts") {
                every {
                    recurringCreditCardDebtRepository.findAllByStatus(RecurringTransactionStatus.ACTIVE)
                } returns listOf(recurring)
                every {
                    recurringCreditCardDebtRepository.findMaterializedDebtForMonth(1, any(), any())
                } returns emptyList()
                every { creditCardService.createDebt(any(), any()) } returns 10

                service.processRecurringDebts()

                Then("should materialize exactly one debt") {
                    verify(exactly = 1) { creditCardService.createDebt(any(), any()) }
                }

                Then("should advance nextInvoiceMonth by one year") {
                    recurring.nextInvoiceMonth shouldBe now.plusYears(1)
                }
            }
        }

        Given("no active recurring debts") {
            When("processing recurring debts") {
                every {
                    recurringCreditCardDebtRepository.findAllByStatus(RecurringTransactionStatus.ACTIVE)
                } returns emptyList()

                service.processRecurringDebts()

                Then("should not call createDebt") {
                    verify(exactly = 0) { creditCardService.createDebt(any(), any()) }
                }
            }
        }

        Given("a recurring whose createDebt fails due to insufficient credit") {
            val now = YearMonth.now()
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    nextInvoiceMonth = now,
                )

            When("processing recurring debts") {
                every {
                    recurringCreditCardDebtRepository.findAllByStatus(RecurringTransactionStatus.ACTIVE)
                } returns listOf(recurring)
                every {
                    recurringCreditCardDebtRepository.findMaterializedDebtForMonth(1, any(), any())
                } returns emptyList()
                every {
                    creditCardService.createDebt(any(), any())
                } throws IllegalStateException("Not enough credit")

                service.processRecurringDebts()

                Then("should not throw and should still advance nextInvoiceMonth") {
                    recurring.nextInvoiceMonth shouldBe now.plusMonths(1)
                }
            }
        }

        Given("multiple active recurring debts from different cards") {
            val now = YearMonth.now()
            val card1 = CreditCardFactory.create(id = 1)
            val card2 = CreditCardFactory.create(id = 2, name = "Card 2")
            val recurring1 = RecurringCreditCardDebtFactory.create(id = 1, creditCard = card1, nextInvoiceMonth = now)
            val recurring2 = RecurringCreditCardDebtFactory.create(id = 2, creditCard = card2, nextInvoiceMonth = now)

            When("processing recurring debts") {
                every {
                    recurringCreditCardDebtRepository.findAllByStatus(RecurringTransactionStatus.ACTIVE)
                } returns listOf(recurring1, recurring2)
                every {
                    recurringCreditCardDebtRepository.findMaterializedDebtForMonth(any(), any(), any())
                } returns emptyList()
                every { creditCardService.createDebt(any(), any()) } returns 10

                service.processRecurringDebts()

                Then("should materialize one debt per recurring") {
                    verify(exactly = 2) { creditCardService.createDebt(any(), any()) }
                }
            }
        }
    })
