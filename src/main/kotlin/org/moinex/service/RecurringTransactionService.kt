/*
 * Filename: RecurringTransactionService.kt (original filename: RecurringTransactionService.java)
 * Created on: November 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/08/2026
 */

package org.moinex.service

import org.moinex.common.extension.findByIdOrThrow
import org.moinex.common.extension.isAfterOrEqual
import org.moinex.common.extension.isBeforeOrEqual
import org.moinex.common.extension.isOpenEnded
import org.moinex.model.enums.RecurringTransactionFrequency
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.RecurringTransaction
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.repository.wallettransaction.RecurringTransactionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth

@Service
class RecurringTransactionService(
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val walletService: WalletService,
) {
    private val logger = LoggerFactory.getLogger(RecurringTransactionService::class.java)

    @Transactional
    fun createRecurringTransaction(recurringTransaction: RecurringTransaction): Int {
        val newRecurringTransaction = recurringTransactionRepository.save(recurringTransaction)

        logger.info("$newRecurringTransaction created successfully")

        return newRecurringTransaction.id!!
    }

    @Transactional
    fun deleteRecurringTransaction(id: Int) {
        val recurringTransactionFromDatabase = recurringTransactionRepository.findByIdOrThrow(id)

        recurringTransactionRepository.delete(recurringTransactionFromDatabase)

        logger.info("$recurringTransactionFromDatabase deleted successfully")
    }

    @Transactional
    fun updateRecurringTransaction(updatedRecurringTransaction: RecurringTransaction) {
        val recurringTransactionFromDatabase = recurringTransactionRepository.findByIdOrThrow(updatedRecurringTransaction.id!!)

        recurringTransactionFromDatabase.apply {
            wallet = updatedRecurringTransaction.wallet
            category = updatedRecurringTransaction.category
            type = updatedRecurringTransaction.type
            amount = updatedRecurringTransaction.amount
            endDate = updatedRecurringTransaction.endDate
            nextDueDate = updatedRecurringTransaction.nextDueDate
            description = updatedRecurringTransaction.description
            frequency = updatedRecurringTransaction.frequency
            status = updatedRecurringTransaction.status
            includeInAnalysis = updatedRecurringTransaction.includeInAnalysis
            includeInNetWorth = updatedRecurringTransaction.includeInNetWorth
        }

        logger.info("$recurringTransactionFromDatabase updated successfully")
    }

    @Transactional
    fun processRecurringTransactions() {
        val now = LocalDateTime.now()
        val activeRecurringTransactions = recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE)
        val earliestDueDate = activeRecurringTransactions.minOfOrNull { it.nextDueDate } ?: now.toLocalDate()

        val transactions =
            generateRecurringTransactionsInRange(
                startInterval = earliestDueDate,
                endInterval = now.toLocalDate(),
                recurringTransactions = activeRecurringTransactions,
                updateRecurringTransactionState = true,
            )

        transactions.forEach { transaction ->
            walletService.createWalletTransaction(transaction)
            logger.info("Transaction created with date ${transaction.date} for recurring transaction")
        }
    }

    fun getExpectedRemainingAmountFromRecurringTransaction(id: Int): BigDecimal? {
        val recurringTransactionFromDatabase = recurringTransactionRepository.findByIdOrThrow(id)

        if (recurringTransactionFromDatabase.endDate.isOpenEnded()) {
            return null
        }

        if (recurringTransactionFromDatabase.endDate.isBefore(LocalDate.now())) {
            return BigDecimal.ZERO
        }

        var expectedAmount = BigDecimal.ZERO
        var nextDueDate = recurringTransactionFromDatabase.nextDueDate

        while (nextDueDate.isBeforeOrEqual(recurringTransactionFromDatabase.endDate)) {
            expectedAmount = expectedAmount.add(recurringTransactionFromDatabase.amount)
            nextDueDate = calculateNextDueDate(nextDueDate, recurringTransactionFromDatabase.frequency!!)
        }

        return expectedAmount
    }

    fun getFutureRecurringTransactionsByYear(
        startYear: Year,
        endYear: Year,
    ): List<WalletTransaction> =
        generateRecurringTransactionsInRange(
            startInterval = startYear.atDay(1),
            endInterval = endYear.atMonth(12).atDay(31),
            recurringTransactions = recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE),
        )

    fun getFutureRecurringTransactionsByMonthForAnalysis(
        startMonth: YearMonth,
        endMonth: YearMonth,
    ): List<WalletTransaction> =
        generateRecurringTransactionsInRange(
            startInterval = startMonth.atDay(1),
            endInterval = endMonth.atEndOfMonth(),
            recurringTransactions = recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE),
            includeOnlyForAnalysis = true,
        )

    fun getLastTransactionDate(
        startDate: LocalDate,
        endDate: LocalDate,
        frequency: RecurringTransactionFrequency,
    ): LocalDate {
        val interval = frequency.chronoUnit.between(startDate, endDate)
        return startDate.plus(interval, frequency.chronoUnit)
    }

    fun getAllRecurringTransactions(): List<RecurringTransaction> = recurringTransactionRepository.findAll()

    fun getAllRecurringTransactionsByType(type: WalletTransactionType): List<RecurringTransaction> =
        recurringTransactionRepository.findByType(type)

    private fun generateRecurringTransactionsInRange(
        startInterval: LocalDate,
        endInterval: LocalDate,
        recurringTransactions: List<RecurringTransaction>,
        includeOnlyForAnalysis: Boolean = false,
        updateRecurringTransactionState: Boolean = false,
    ): List<WalletTransaction> =
        buildList {
            recurringTransactions
                .filter { !includeOnlyForAnalysis || it.includeInAnalysis }
                .forEach { recurringTransaction ->
                    var currentDate = recurringTransaction.nextDueDate

                    while (shouldGenerateWalletTransaction(currentDate, endInterval, recurringTransaction)) {
                        if (currentDate.isAfterOrEqual(startInterval)) {
                            add(createPendingWalletTransaction(recurringTransaction, currentDate))
                        }

                        currentDate = calculateNextDueDate(currentDate, recurringTransaction.frequency)
                    }

                    if (updateRecurringTransactionState) updateRecurringTransactionState(recurringTransaction, currentDate, endInterval)
                }
        }

    private fun shouldGenerateWalletTransaction(
        currentDate: LocalDate,
        endInterval: LocalDate,
        recurringTransaction: RecurringTransaction,
    ) = currentDate.isBeforeOrEqual(endInterval) && recurringTransaction.endDate.isAfterOrEqual(currentDate)

    private fun createPendingWalletTransaction(
        recurringTransaction: RecurringTransaction,
        date: LocalDate,
    ): WalletTransaction =
        WalletTransaction(
            date = date.atStartOfDay(),
            status = WalletTransactionStatus.PENDING,
            description = recurringTransaction.description,
            includeInAnalysis = recurringTransaction.includeInAnalysis,
            wallet = recurringTransaction.wallet,
            category = recurringTransaction.category,
            type = recurringTransaction.type,
            amount = recurringTransaction.amount,
        )

    private fun updateRecurringTransactionState(
        recurringTransaction: RecurringTransaction,
        newDueDate: LocalDate,
        endInterval: LocalDate,
    ) {
        if (newDueDate.isAfter(recurringTransaction.nextDueDate)) {
            recurringTransaction.nextDueDate = newDueDate
        }

        if (recurringTransaction.endDate.isBefore(endInterval)) {
            recurringTransaction.status = RecurringTransactionStatus.INACTIVE
        }
    }

    private fun calculateNextDueDate(
        currentDueDate: LocalDate,
        frequency: RecurringTransactionFrequency,
    ): LocalDate = currentDueDate.plus(1, frequency.chronoUnit)
}
