package org.moinex.service.creditcard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.common.ClockProvider
import org.moinex.common.constant.Constants
import org.moinex.factory.creditcard.RecurringCreditCardDebtFactory
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.creditcard.RecurringCreditCardDebtRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import java.time.YearMonth

class RecurringCreditCardDebtServiceMaterializeTest :
    BehaviorSpec({
        val recurringCreditCardDebtRepository = mockk<RecurringCreditCardDebtRepository>()
        val creditCardDebtRepository = mockk<CreditCardDebtRepository>()
        val creditCardRepository = mockk<CreditCardRepository>()
        val creditCardService = mockk<CreditCardService>()
        val notificationService = mockk<NotificationService>(relaxed = true)
        val preferencesService = mockk<PreferencesService>(relaxed = true)
        val clockProvider = ClockProvider()

        val service =
            RecurringCreditCardDebtService(
                recurringCreditCardDebtRepository,
                creditCardDebtRepository,
                creditCardRepository,
                creditCardService,
                notificationService,
                preferencesService,
                clockProvider,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("an active recurring with nextInvoiceMonth one month ahead") {
            val nextMonth = YearMonth.now().plusMonths(1)
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    nextInvoiceMonth = nextMonth,
                )

            When("materializing for that next month") {
                every {
                    recurringCreditCardDebtRepository.findById(1)
                } returns java.util.Optional.of(recurring)
                every {
                    recurringCreditCardDebtRepository.findMaterializedDebtForMonth(1, any(), any())
                } returns emptyList()
                every { creditCardService.createDebt(any(), any(), any()) } returns 10

                service.materializeForMonth(1, nextMonth)

                Then("should create the debt for that month") {
                    verify(exactly = 1) { creditCardService.createDebt(any(), nextMonth, any()) }
                }

                Then("should advance nextInvoiceMonth by one period") {
                    recurring.nextInvoiceMonth shouldBe nextMonth.plusMonths(1)
                }
            }
        }

        Given("an active recurring that is already materialized for the target month") {
            val nextMonth = YearMonth.now().plusMonths(1)
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    nextInvoiceMonth = nextMonth,
                )
            val existingDebt = listOf(mockk<org.moinex.model.creditcard.CreditCardDebt>())

            When("trying to materialize for that month again") {
                every {
                    recurringCreditCardDebtRepository.findById(1)
                } returns java.util.Optional.of(recurring)
                every {
                    recurringCreditCardDebtRepository.findMaterializedDebtForMonth(1, any(), any())
                } returns existingDebt

                service.materializeForMonth(1, nextMonth)

                Then("should not create a duplicate debt") {
                    verify(exactly = 0) { creditCardService.createDebt(any(), any(), any()) }
                }
            }
        }

        Given("an active recurring where nextInvoiceMonth is two months ahead") {
            val nextMonth = YearMonth.now().plusMonths(1)
            val twoMonthsAhead = YearMonth.now().plusMonths(2)
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    nextInvoiceMonth = nextMonth,
                )

            When("trying to materialize a month that is not the next scheduled one") {
                every {
                    recurringCreditCardDebtRepository.findById(1)
                } returns java.util.Optional.of(recurring)

                Then("should throw an IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.materializeForMonth(1, twoMonthsAhead)
                    }
                }

                Then("should not create any debt") {
                    verify(exactly = 0) { creditCardService.createDebt(any(), any(), any()) }
                }
            }
        }

        Given("an active recurring whose endDate is before the target month's occurrence date") {
            val nextMonth = YearMonth.now().plusMonths(1)
            val pastEndDate = nextMonth.atDay(1).minusDays(1)
            val startDate = nextMonth.minusMonths(6).atDay(1)
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    nextInvoiceMonth = nextMonth,
                    startDate = startDate,
                    endDate = pastEndDate,
                )

            When("trying to materialize for the target month") {
                every {
                    recurringCreditCardDebtRepository.findById(1)
                } returns java.util.Optional.of(recurring)

                Then("should throw an IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.materializeForMonth(1, nextMonth)
                    }
                }

                Then("should not create any debt") {
                    verify(exactly = 0) { creditCardService.createDebt(any(), any(), any()) }
                }
            }
        }

        Given("an inactive recurring debt") {
            val nextMonth = YearMonth.now().plusMonths(1)
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    nextInvoiceMonth = nextMonth,
                    status = RecurringTransactionStatus.INACTIVE,
                )

            When("trying to materialize") {
                every {
                    recurringCreditCardDebtRepository.findById(1)
                } returns java.util.Optional.of(recurring)

                Then("should throw an IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.materializeForMonth(1, nextMonth)
                    }
                }

                Then("should not create any debt") {
                    verify(exactly = 0) { creditCardService.createDebt(any(), any(), any()) }
                }
            }
        }

        Given("an active recurring materialized early, then processRecurringDebts runs for the same month") {
            val nextMonth = YearMonth.now().plusMonths(1)
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    nextInvoiceMonth = nextMonth,
                )

            When("early materialization succeeds and then processRecurringDebts is called for that month") {
                every {
                    recurringCreditCardDebtRepository.findById(1)
                } returns java.util.Optional.of(recurring)
                every {
                    recurringCreditCardDebtRepository.findMaterializedDebtForMonth(1, any(), any())
                } returns emptyList() andThen
                    listOf(mockk<org.moinex.model.creditcard.CreditCardDebt>())
                every {
                    recurringCreditCardDebtRepository.findAllByStatus(RecurringTransactionStatus.ACTIVE)
                } returns listOf(recurring)
                every { creditCardService.createDebt(any(), any(), any()) } returns 10

                service.materializeForMonth(1, nextMonth)

                Then("nextInvoiceMonth should have advanced past the early-materialized month") {
                    recurring.nextInvoiceMonth shouldBe nextMonth.plusMonths(1)
                }

                Then("processRecurringDebts for the current month should not re-materialize the future month") {
                    verify(exactly = 1) { creditCardService.createDebt(any(), any(), any()) }
                }
            }
        }

        Given("an active YEARLY recurring with nextInvoiceMonth one year ahead") {
            val nextYear = YearMonth.now().plusYears(1)
            val startDate = nextYear.minusYears(1).atDay(1)
            val endDate = Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    nextInvoiceMonth = nextYear,
                    startDate = startDate,
                    endDate = endDate,
                    frequency = org.moinex.model.enums.CreditCardRecurringFrequency.YEARLY,
                )

            When("materializing for that next year") {
                every {
                    recurringCreditCardDebtRepository.findById(1)
                } returns java.util.Optional.of(recurring)
                every {
                    recurringCreditCardDebtRepository.findMaterializedDebtForMonth(1, any(), any())
                } returns emptyList()
                every { creditCardService.createDebt(any(), any(), any()) } returns 10

                service.materializeForMonth(1, nextYear)

                Then("should advance nextInvoiceMonth by one year") {
                    recurring.nextInvoiceMonth shouldBe nextYear.plusYears(1)
                }
            }
        }
    })
