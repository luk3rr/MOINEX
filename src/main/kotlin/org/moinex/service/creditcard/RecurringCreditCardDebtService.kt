/*
 * Filename: RecurringCreditCardDebtService.kt
 * Created on: April 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.creditcard

import org.moinex.common.constant.Constants
import org.moinex.common.extension.atEndOfDay
import org.moinex.common.extension.findByIdOrThrow
import org.moinex.common.extension.isBeforeOrEqual
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.model.creditcard.RecurringCreditCardDebt
import org.moinex.model.dto.RecurringCreditCardDebtOccurrenceDTO
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.creditcard.RecurringCreditCardDebtRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Month
import java.time.Year
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Service
class RecurringCreditCardDebtService(
    private val recurringCreditCardDebtRepository: RecurringCreditCardDebtRepository,
    private val creditCardDebtRepository: CreditCardDebtRepository,
    private val creditCardRepository: CreditCardRepository,
    private val creditCardService: CreditCardService,
) {
    private val logger = LoggerFactory.getLogger(RecurringCreditCardDebtService::class.java)

    @Transactional
    fun createRecurring(recurring: RecurringCreditCardDebt): Int {
        check(creditCardRepository.existsById(recurring.creditCard.id!!)) {
            "${recurring.creditCard} does not exist"
        }

        val newRecurringTransaction = recurringCreditCardDebtRepository.save(recurring)

        logger.info("$newRecurringTransaction created successfully")

        return newRecurringTransaction.id!!
    }

    @Transactional
    fun updateRecurring(updated: RecurringCreditCardDebt) {
        val recurringTransactionFromDatabase = recurringCreditCardDebtRepository.findByIdOrThrow(updated.id!!)

        check(creditCardRepository.existsById(updated.creditCard.id!!)) {
            "${updated.creditCard} does not exist"
        }

        recurringTransactionFromDatabase.apply {
            creditCard = updated.creditCard
            category = updated.category
            amount = updated.amount
            description = updated.description
            dayOfMonth = updated.dayOfMonth
            frequency = updated.frequency
            endDate = updated.endDate
            status = updated.status
        }

        logger.info("$recurringTransactionFromDatabase updated successfully")
    }

    @Transactional
    fun deleteRecurring(id: Int) {
        val recurringTransactionFromDatabase = recurringCreditCardDebtRepository.findByIdOrThrow(id)

        check(!creditCardDebtRepository.existsByRecurringSourceId(id)) {
            "Cannot delete $recurringTransactionFromDatabase because there are materialized debts linked to it"
        }

        recurringCreditCardDebtRepository.delete(recurringTransactionFromDatabase)
        logger.info("$recurringTransactionFromDatabase deleted permanently")
    }

    @Transactional
    fun processRecurringDebts() {
        val activeRecurringTransactions =
            recurringCreditCardDebtRepository.findAllByStatus(
                RecurringTransactionStatus.ACTIVE,
            )

        activeRecurringTransactions.forEach { recurring ->
            materializeDebtsUpTo(recurring, YearMonth.now())
        }
    }

    fun getProjectedOccurrences(
        recurringId: Int,
        monthsAhead: Int = Constants.DEFAULT_PROJECTED_MONTHS_AHEAD,
    ): List<RecurringCreditCardDebtOccurrenceDTO> {
        val recurringTransactionFromDatabase = recurringCreditCardDebtRepository.findByIdOrThrow(recurringId)

        if (recurringTransactionFromDatabase.isInactive()) return emptyList()

        val startMonth = maxOf(recurringTransactionFromDatabase.nextInvoiceMonth, YearMonth.now())
        val endMonth = YearMonth.now().plusMonths(monthsAhead.toLong())

        return buildList {
            var current = startMonth

            while (current.isBeforeOrEqual(endMonth)) {
                val occurrenceDate = current.atDay(recurringTransactionFromDatabase.dayOfMonth)

                if (occurrenceDate.isAfter(recurringTransactionFromDatabase.endDate)) break

                add(
                    RecurringCreditCardDebtOccurrenceDTO(
                        recurringDebt = recurringTransactionFromDatabase,
                        invoiceMonth = current,
                        amount = recurringTransactionFromDatabase.amount,
                    ),
                )

                current = current.plus(1, recurringTransactionFromDatabase.frequency.chronoUnit)
            }
        }
    }

    fun getProjectedOccurrencesByCard(
        creditCardId: Int,
        monthsAhead: Int = Constants.DEFAULT_PROJECTED_MONTHS_AHEAD,
    ): List<RecurringCreditCardDebtOccurrenceDTO> =
        recurringCreditCardDebtRepository
            .findAllByCreditCardAndStatus(creditCardId, RecurringTransactionStatus.ACTIVE)
            .flatMap { recurring -> getProjectedOccurrences(recurring.id!!, monthsAhead) }

    fun getAllRecurringByCreditCard(creditCardId: Int): List<RecurringCreditCardDebt> =
        recurringCreditCardDebtRepository.findAllByCreditCard(creditCardId)

    fun getAllRecurringDebts(): List<RecurringCreditCardDebt> = recurringCreditCardDebtRepository.findAll()

    fun getTotalProjectedAmountForMonth(month: YearMonth): BigDecimal =
        getProjectedOccurrencesForMonth(month).sumOf { it.amount }

    fun getTotalProjectedAmountForMonthAndCreditCard(
        month: YearMonth,
        creditCardId: Int,
    ): BigDecimal = getProjectedOccurrencesForMonthAndCreditCard(month, creditCardId).sumOf { it.amount }

    fun getTotalProjectedAmountUntilMonth(targetMonth: YearMonth): BigDecimal {
        if (targetMonth.isBeforeOrEqual(YearMonth.now())) return BigDecimal.ZERO

        val monthsAhead = YearMonth.now().until(targetMonth, ChronoUnit.MONTHS).toInt() + 1

        return recurringCreditCardDebtRepository
            .findAllByStatus(RecurringTransactionStatus.ACTIVE)
            .flatMap { recurring -> getProjectedOccurrences(recurring.id!!, monthsAhead) }
            .filter { it.invoiceMonth.isAfter(YearMonth.now()) && it.invoiceMonth.isBeforeOrEqual(targetMonth) }
            .sumOf { it.amount }
    }

    fun getTotalProjectedAmountForYear(year: Year): BigDecimal =
        Month.entries
            .map { month -> year.atMonth(month) }
            .flatMap { yearMonth -> getProjectedOccurrencesForMonth(yearMonth) }
            .sumOf { it.amount }

    fun getProjectedOccurrencesForMonthAndCreditCard(
        month: YearMonth,
        creditCardId: Int,
    ): List<RecurringCreditCardDebtOccurrenceDTO> {
        val recurringDebts =
            recurringCreditCardDebtRepository.findAllByCreditCardAndStatus(
                creditCardId,
                RecurringTransactionStatus.ACTIVE,
            )
        return projectOccurrences(month, recurringDebts)
    }

    fun getProjectedOccurrencesForMonth(month: YearMonth): List<RecurringCreditCardDebtOccurrenceDTO> {
        val recurringDebts = recurringCreditCardDebtRepository.findAllByStatus(RecurringTransactionStatus.ACTIVE)
        return projectOccurrences(month, recurringDebts)
    }

    private fun projectOccurrences(
        month: YearMonth,
        recurringDebts: List<RecurringCreditCardDebt>,
    ): List<RecurringCreditCardDebtOccurrenceDTO> {
        if (month.isBeforeOrEqual(YearMonth.now())) return emptyList()

        val monthsAhead = YearMonth.now().until(month, ChronoUnit.MONTHS).toInt() + 1

        return recurringDebts
            .flatMap { recurring -> getProjectedOccurrences(recurring.id!!, monthsAhead) }
            .filter { it.invoiceMonth == month }
    }

    private fun materializeDebtsUpTo(
        recurring: RecurringCreditCardDebt,
        upTo: YearMonth,
    ) {
        var current = recurring.nextInvoiceMonth

        while (current.isBeforeOrEqual(upTo)) {
            val occurrenceDate = current.atDay(recurring.dayOfMonth)

            if (occurrenceDate.isAfter(recurring.endDate)) {
                recurring.status = RecurringTransactionStatus.INACTIVE
                logger.info("$recurring reached end date — deactivated")
                break
            }

            if (isAlreadyMaterialized(recurring, current)) {
                logger.info("$recurring already materialized for $current — skipping")
                current = current.plus(1, recurring.frequency.chronoUnit)
                continue
            }

            val debt =
                CreditCardDebt(
                    creditCard = recurring.creditCard,
                    category = recurring.category,
                    amount = recurring.amount,
                    description = recurring.description,
                    date = occurrenceDate.atEndOfDay(),
                    installments = 1,
                    recurringSource = recurring,
                )

            runCatching {
                creditCardService.createDebt(debt, current)
                logger.info("Materialized debt for $recurring on invoice month $current")
            }.onFailure { e ->
                logger.error("Failed to materialize debt for $recurring on $current: ${e.message}")
            }

            current = current.plus(1, recurring.frequency.chronoUnit)
        }

        recurring.nextInvoiceMonth = current
    }

    private fun isAlreadyMaterialized(
        recurring: RecurringCreditCardDebt,
        month: YearMonth,
    ): Boolean {
        val monthStart = month.atDay(1).atStartOfDay()
        val monthEnd = month.atEndOfMonth().atEndOfDay()

        return recurringCreditCardDebtRepository
            .findMaterializedDebtForMonth(recurring.id!!, monthStart, monthEnd)
            .isNotEmpty()
    }
}
