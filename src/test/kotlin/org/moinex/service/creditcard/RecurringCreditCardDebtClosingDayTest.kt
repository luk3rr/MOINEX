package org.moinex.service.creditcard

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.moinex.common.ClockProvider
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.creditcard.RecurringCreditCardDebtFactory
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.creditcard.RecurringCreditCardDebtRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.Optional

class RecurringCreditCardDebtClosingDayTest :
    BehaviorSpec({
        val recurringRepository = mockk<RecurringCreditCardDebtRepository>()
        val creditCardService = mockk<CreditCardService>()

        val stubInvoiceMonth = {
            every { creditCardService.invoiceMonthForPurchaseDate(any(), any()) } answers {
                val card = firstArg<CreditCard>()
                val date = secondArg<LocalDate>()
                YearMonth.from(date).let { if (date.dayOfMonth > card.closingDay) it.plusMonths(1) else it }
            }
        }
        stubInvoiceMonth()

        fun serviceAt(today: LocalDate): RecurringCreditCardDebtService =
            RecurringCreditCardDebtService(
                recurringRepository,
                mockk<CreditCardDebtRepository>(),
                mockk<CreditCardRepository>(),
                creditCardService,
                mockk<NotificationService>(relaxed = true),
                mockk<PreferencesService>(relaxed = true),
                ClockProvider(
                    Clock.fixed(today.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()),
                ),
            )

        afterContainer {
            clearAllMocks(answers = true)
            stubInvoiceMonth()
        }

        val card = CreditCardFactory.create(id = 1, closingDay = 5)

        Given("a card that closes on day 5 and a recurring scheduled for day 20") {
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    creditCard = card,
                    dayOfMonth = 20,
                    nextInvoiceMonth = YearMonth.of(2026, 12),
                    startDate = LocalDate.of(2026, 11, 20),
                )

            Then("the purchase date for the December invoice is 2026-11-20") {
                recurring.occurrenceDateFor(YearMonth.of(2026, 12)) shouldBe LocalDate.of(2026, 11, 20)
            }

            When("processing before the purchase date arrives") {
                every { recurringRepository.findAllByStatus(RecurringTransactionStatus.ACTIVE) } returns
                    listOf(recurring)
                val service = serviceAt(LocalDate.of(2026, 11, 19))

                service.processRecurringDebts()

                Then("nothing is generated and the invoice month does not move") {
                    verify(exactly = 0) { creditCardService.createDebt(any(), any(), any()) }
                    recurring.nextInvoiceMonth shouldBe YearMonth.of(2026, 12)
                }
            }

            When("processing on the purchase date") {
                every { recurringRepository.findAllByStatus(RecurringTransactionStatus.ACTIVE) } returns
                    listOf(recurring)
                every { recurringRepository.findMaterializedDebtForMonth(1, any(), any()) } returns emptyList()
                val debt = slot<CreditCardDebt>()
                val invoiceMonth = slot<YearMonth>()
                every { creditCardService.createDebt(capture(debt), capture(invoiceMonth), any()) } returns 10
                val service = serviceAt(LocalDate.of(2026, 11, 20))

                service.processRecurringDebts()

                Then("the debt is dated 2026-11-20 and lands on the December invoice, not the closed November one") {
                    debt.captured.date shouldBe LocalDateTime.of(2026, 11, 20, 0, 0)
                    invoiceMonth.captured shouldBe YearMonth.of(2026, 12)
                }

                Then("the next invoice month advances to January") {
                    recurring.nextInvoiceMonth shouldBe YearMonth.of(2027, 1)
                }
            }
        }

        Given("a recurring scheduled for day 3, before the closing day 5") {
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 2,
                    creditCard = card,
                    dayOfMonth = 3,
                    nextInvoiceMonth = YearMonth.of(2026, 11),
                    startDate = LocalDate.of(2026, 11, 3),
                )

            Then("the purchase date for the November invoice is 2026-11-03") {
                recurring.occurrenceDateFor(YearMonth.of(2026, 11)) shouldBe LocalDate.of(2026, 11, 3)
            }

            When("processing on the purchase date") {
                every { recurringRepository.findAllByStatus(RecurringTransactionStatus.ACTIVE) } returns
                    listOf(recurring)
                every { recurringRepository.findMaterializedDebtForMonth(2, any(), any()) } returns emptyList()
                val invoiceMonth = slot<YearMonth>()
                every { creditCardService.createDebt(any(), capture(invoiceMonth), any()) } returns 11

                serviceAt(LocalDate.of(2026, 11, 3)).processRecurringDebts()

                Then("it lands on the open November invoice") {
                    invoiceMonth.captured shouldBe YearMonth.of(2026, 11)
                }
            }
        }

        Given("a recurring scheduled exactly on the closing day") {
            val recurring =
                RecurringCreditCardDebtFactory.create(id = 3, creditCard = card, dayOfMonth = 5)

            Then("a purchase on the closing day still belongs to that month's invoice") {
                recurring.occurrenceDateFor(YearMonth.of(2026, 11)) shouldBe LocalDate.of(2026, 11, 5)
            }
        }

        Given("the same day-20 recurring viewed before the purchase date") {
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    creditCard = card,
                    dayOfMonth = 20,
                    nextInvoiceMonth = YearMonth.of(2026, 12),
                    startDate = LocalDate.of(2026, 11, 20),
                )

            When("projecting occurrences") {
                every { recurringRepository.findById(1) } returns Optional.of(recurring)

                val result = serviceAt(LocalDate.of(2026, 11, 10)).getProjectedOccurrences(1, monthsAhead = 2)

                Then("the first projection is on the December invoice with purchase date 2026-11-20") {
                    result.first().invoiceMonth shouldBe YearMonth.of(2026, 12)
                    result.first().occurrenceDate shouldBe LocalDate.of(2026, 11, 20)
                }

                Then("nothing is projected for the closed November invoice") {
                    result.filter { it.invoiceMonth == YearMonth.of(2026, 11) }.shouldBeEmpty()
                }
            }
        }
    })
