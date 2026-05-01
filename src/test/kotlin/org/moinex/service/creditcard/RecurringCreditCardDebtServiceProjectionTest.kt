package org.moinex.service.creditcard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityNotFoundException
import org.moinex.common.constant.Constants
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.creditcard.RecurringCreditCardDebtFactory
import org.moinex.model.enums.CreditCardRecurringFrequency
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.creditcard.RecurringCreditCardDebtRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import java.time.YearMonth
import java.util.Optional

class RecurringCreditCardDebtServiceProjectionTest :
    BehaviorSpec({
        val recurringCreditCardDebtRepository = mockk<RecurringCreditCardDebtRepository>()
        val creditCardDebtRepository = mockk<CreditCardDebtRepository>()
        val creditCardRepository = mockk<CreditCardRepository>()
        val creditCardService = mockk<CreditCardService>()
        val notificationService = mockk<NotificationService>(relaxed = true)
        val preferencesService = mockk<PreferencesService>(relaxed = true)

        val service =
            RecurringCreditCardDebtService(
                recurringCreditCardDebtRepository,
                creditCardDebtRepository,
                creditCardRepository,
                creditCardService,
                notificationService,
                preferencesService,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("an active MONTHLY open-ended recurring") {
            val now = YearMonth.now()
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    frequency = CreditCardRecurringFrequency.MONTHLY,
                    nextInvoiceMonth = now,
                    endDate = Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE,
                )

            When("projecting 6 months ahead") {
                every { recurringCreditCardDebtRepository.findById(1) } returns Optional.of(recurring)

                val result = service.getProjectedOccurrences(1, monthsAhead = 6)

                Then("should return 7 occurrences (current month + 6 ahead)") {
                    result shouldHaveSize 7
                }

                Then("first occurrence should be the current month") {
                    result.first().invoiceMonth shouldBe now
                }

                Then("last occurrence should be 6 months ahead") {
                    result.last().invoiceMonth shouldBe now.plusMonths(6)
                }

                Then("each occurrence should carry the recurring's amount") {
                    result.all { it.amount == recurring.amount } shouldBe true
                }
            }
        }

        Given("an active YEARLY open-ended recurring") {
            val now = YearMonth.now()
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    frequency = CreditCardRecurringFrequency.YEARLY,
                    nextInvoiceMonth = now,
                    endDate = Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE,
                )

            When("projecting 24 months ahead") {
                every { recurringCreditCardDebtRepository.findById(1) } returns Optional.of(recurring)

                val result = service.getProjectedOccurrences(1, monthsAhead = 24)

                Then("should return 3 occurrences (current year, +1 year, +2 years)") {
                    result shouldHaveSize 3
                }

                Then("occurrences should be exactly 12 months apart") {
                    result[1].invoiceMonth shouldBe result[0].invoiceMonth.plusYears(1)
                    result[2].invoiceMonth shouldBe result[1].invoiceMonth.plusYears(1)
                }
            }
        }

        Given("an inactive recurring debt") {
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    status = RecurringTransactionStatus.INACTIVE,
                )

            When("projecting occurrences") {
                every { recurringCreditCardDebtRepository.findById(1) } returns Optional.of(recurring)

                val result = service.getProjectedOccurrences(1, monthsAhead = 6)

                Then("should return empty list") {
                    result.shouldBeEmpty()
                }
            }
        }

        Given("an active recurring with an endDate within the projection window") {
            val now = YearMonth.now()
            val endDate = now.plusMonths(3).atDay(1)
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    frequency = CreditCardRecurringFrequency.MONTHLY,
                    nextInvoiceMonth = now,
                    endDate = endDate,
                )

            When("projecting 12 months ahead") {
                every { recurringCreditCardDebtRepository.findById(1) } returns Optional.of(recurring)

                val result = service.getProjectedOccurrences(1, monthsAhead = 12)

                Then("should stop at the endDate month") {
                    // now, +1, +2 (month of endDate day 1 is included only if occurrence <= endDate)
                    result.shouldHaveSize(3)
                    result.all { it.invoiceMonth <= YearMonth.from(endDate) } shouldBe true
                }
            }
        }

        Given("a recurring whose nextInvoiceMonth is in the past") {
            val now = YearMonth.now()
            val pastMonth = now.minusMonths(3)
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    frequency = CreditCardRecurringFrequency.MONTHLY,
                    nextInvoiceMonth = pastMonth,
                )

            When("projecting 3 months ahead") {
                every { recurringCreditCardDebtRepository.findById(1) } returns Optional.of(recurring)

                val result = service.getProjectedOccurrences(1, monthsAhead = 3)

                Then("should start from the current month, not the past") {
                    result.first().invoiceMonth shouldBe now
                }

                Then("should return 4 occurrences (current + 3 ahead)") {
                    result shouldHaveSize 4
                }
            }
        }

        Given("a recurring that does not exist") {
            When("projecting occurrences") {
                every { recurringCreditCardDebtRepository.findById(99) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.getProjectedOccurrences(99, monthsAhead = 6)
                    }
                }
            }
        }

        Given("a credit card with two active recurring debts") {
            val now = YearMonth.now()
            val card = CreditCardFactory.create(id = 1)
            val recurring1 =
                RecurringCreditCardDebtFactory.create(id = 1, creditCard = card, nextInvoiceMonth = now)
            val recurring2 =
                RecurringCreditCardDebtFactory.create(id = 2, creditCard = card, nextInvoiceMonth = now)

            When("projecting 1 month ahead for the card") {
                every {
                    recurringCreditCardDebtRepository.findAllByCreditCardAndStatus(
                        card.id!!,
                        RecurringTransactionStatus.ACTIVE,
                    )
                } returns listOf(recurring1, recurring2)
                every { recurringCreditCardDebtRepository.findById(1) } returns Optional.of(recurring1)
                every { recurringCreditCardDebtRepository.findById(2) } returns Optional.of(recurring2)

                val result = service.getProjectedOccurrencesByCard(1, monthsAhead = 1)

                Then("should return 4 occurrences total (2 recurrings × 2 months)") {
                    result shouldHaveSize 4
                }
            }
        }

        Given("a credit card with no active recurring debts") {
            val card = CreditCardFactory.create(id = 1)

            When("projecting occurrences for the card") {
                every {
                    recurringCreditCardDebtRepository.findAllByCreditCardAndStatus(
                        card.id!!,
                        RecurringTransactionStatus.ACTIVE,
                    )
                } returns emptyList()

                val result = service.getProjectedOccurrencesByCard(1)

                Then("should return empty list") {
                    result.shouldBeEmpty()
                }
            }
        }
    })
