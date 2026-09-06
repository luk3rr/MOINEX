/*
 * Filename: AnnualSummaryService.kt
 * Created on: September 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.summary

import org.moinex.common.ClockProvider
import org.moinex.model.dto.AnnualSummaryDTO
import org.moinex.model.dto.CategoryBreakdownDTO
import org.moinex.model.dto.MonthlyFlowDTO
import org.moinex.model.dto.TopLineItemDTO
import org.moinex.model.enums.SpendAccountingMode
import org.moinex.model.enums.TransactionSource
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

@Service
class AnnualSummaryService(
    private val walletService: WalletService,
    private val creditCardService: CreditCardService,
    private val recurringTransactionService: RecurringTransactionService,
    private val insightGenerator: FinancialInsightGenerator,
    private val preferencesService: PreferencesService,
    private val clockProvider: ClockProvider,
) {
    fun buildSummary(
        startDate: LocalDate,
        endDate: LocalDate,
        mode: SpendAccountingMode,
    ): AnnualSummaryDTO {
        require(!endDate.isBefore(startDate)) { "endDate must not be before startDate" }

        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.atTime(LocalTime.MAX)

        val months =
            generateSequence(YearMonth.from(startDate)) { it.plusMonths(1) }
                .takeWhile { !it.isAfter(YearMonth.from(endDate)) }
                .toList()
        val monthCount = months.size

        val walletTransactions =
            months
                .flatMap { walletService.getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(it) }
                .filter { !it.date.isBefore(startDateTime) && !it.date.isAfter(endDateTime) }

        val incomeTransactions = walletTransactions.filter { it.type == WalletTransactionType.INCOME }
        val walletExpenses = walletTransactions.filter { it.type == WalletTransactionType.EXPENSE }

        val expenseItems = buildExpenseItems(walletExpenses, mode, months, startDateTime, endDateTime)
        val incomeItems =
            incomeTransactions.map {
                TopLineItemDTO(it.date, it.description ?: "", it.amount, it.category.name, TransactionSource.WALLET)
            }

        val totalExpense = expenseItems.total()
        val totalIncome = incomeItems.total()
        val netBalance = totalIncome - totalExpense
        val savingsRate = percentage(netBalance, totalIncome)

        val expenseByCategory = breakdownByCategory(expenseItems, totalExpense, monthCount)
        val incomeByCategory = breakdownByCategory(incomeItems, totalIncome, monthCount)
        val monthlyFlows = buildMonthlyFlows(months, incomeItems, expenseItems, mode)
        val topExpenses = expenseItems.sortedByDescending { it.amount }.take(TOP_EXPENSES_LIMIT)

        val summary =
            AnnualSummaryDTO(
                startDate = startDate,
                endDate = endDate,
                mode = mode,
                monthCount = monthCount,
                totalIncome = totalIncome.setScale(2, RoundingMode.HALF_UP),
                totalExpense = totalExpense.setScale(2, RoundingMode.HALF_UP),
                netBalance = netBalance.setScale(2, RoundingMode.HALF_UP),
                savingsRatePercentage = savingsRate,
                expenseByCategory = expenseByCategory,
                incomeByCategory = incomeByCategory,
                monthlyFlows = monthlyFlows,
                topExpenses = topExpenses,
                insights = emptyList(),
            )

        return summary.copy(
            insights = insightGenerator.generate(summary, preferencesService.savingsRateTarget),
        )
    }

    private fun buildExpenseItems(
        walletExpenses: List<WalletTransaction>,
        mode: SpendAccountingMode,
        months: List<YearMonth>,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
    ): List<TopLineItemDTO> {
        val walletItems =
            walletExpenses.map {
                TopLineItemDTO(it.date, it.description ?: "", it.amount, it.category.name, TransactionSource.WALLET)
            }

        val creditCardItems =
            when (mode) {
                SpendAccountingMode.ACCRUAL ->
                    creditCardService.getDebtsBetweenForAnalysis(startDateTime, endDateTime).map {
                        TopLineItemDTO(
                            it.date,
                            it.description ?: "",
                            it.amount,
                            it.category.name,
                            TransactionSource.CREDIT_CARD,
                        )
                    }

                SpendAccountingMode.CASH_FLOW ->
                    months
                        .flatMap { creditCardService.getAllPaidPaymentsByMonth(it) }
                        .filter {
                            !it.refunded && !it.date.isBefore(startDateTime) && !it.date.isAfter(endDateTime)
                        }.map {
                            TopLineItemDTO(
                                it.date,
                                it.creditCardDebt.description ?: "",
                                it.paidAmount,
                                it.creditCardDebt.category.name,
                                TransactionSource.CREDIT_CARD,
                            )
                        }
            }

        return walletItems + creditCardItems
    }

    private fun breakdownByCategory(
        items: List<TopLineItemDTO>,
        total: BigDecimal,
        monthCount: Int,
    ): List<CategoryBreakdownDTO> =
        items
            .groupBy { it.categoryName }
            .map { (name, categoryItems) ->
                val categoryTotal = categoryItems.total()
                CategoryBreakdownDTO(
                    categoryName = name,
                    total = categoryTotal.setScale(2, RoundingMode.HALF_UP),
                    percentage = percentage(categoryTotal, total),
                    monthlyAverage =
                        categoryTotal
                            .divide(BigDecimal(monthCount), 2, RoundingMode.HALF_UP),
                )
            }.sortedByDescending { it.total }

    private fun buildMonthlyFlows(
        months: List<YearMonth>,
        incomeItems: List<TopLineItemDTO>,
        expenseItems: List<TopLineItemDTO>,
        mode: SpendAccountingMode,
    ): List<MonthlyFlowDTO> {
        val currentMonth = clockProvider.currentMonth()

        return months.map { month ->
            val monthIncomeItems = incomeItems.filter { YearMonth.from(it.date) == month }
            val monthExpenseItems = expenseItems.filter { YearMonth.from(it.date) == month }
            val income = monthIncomeItems.total()
            val expense = monthExpenseItems.total()
            val isCurrentMonth = month == currentMonth

            val projection = if (isCurrentMonth) buildCurrentMonthProjection(month, mode) else null

            MonthlyFlowDTO(
                period = month,
                income = income.setScale(2, RoundingMode.HALF_UP),
                expense = expense.setScale(2, RoundingMode.HALF_UP),
                net = (income - expense).setScale(2, RoundingMode.HALF_UP),
                savingsRatePercentage = percentage(income - expense, income),
                isCurrentMonth = isCurrentMonth,
                projectedIncome = projection?.income,
                projectedExpense = projection?.expense,
                projectedNet = projection?.net,
                projectedSavingsRatePercentage = projection?.savingsRate,
            )
        }
    }

    /**
     * Projects the current month's totals using everything already known about it, regardless of
     * whether it falls before or after "today": wallet transactions already recorded (confirmed or
     * pending), recurring wallet transactions due this month but not yet materialized (mirrors the
     * pattern used by HomeController/WalletController for "this month" figures), and the full month's
     * credit card debt — accrued purchases (including recurring debts, which are materialized for the
     * whole current month up front) for ACCRUAL, or paid + already-billed-but-unpaid for CASH_FLOW.
     *
     * The outer query's start/end date only bounds which months are *shown*; the projection for the
     * current month always looks at that month's full range, since it is inherently forward-looking.
     */
    private fun buildCurrentMonthProjection(
        month: YearMonth,
        mode: SpendAccountingMode,
    ): CurrentMonthProjection {
        val recordedTransactions = walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(month)
        val notYetMaterializedRecurring =
            recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(month, month)
        val allTransactions = recordedTransactions + notYetMaterializedRecurring

        val projectedIncome = allTransactions.filter { it.type == WalletTransactionType.INCOME }.sumAmount()
        val projectedWalletExpense = allTransactions.filter { it.type == WalletTransactionType.EXPENSE }.sumAmount()
        val projectedExpense = projectedWalletExpense + creditCardExpenseForFullMonth(month, mode)
        val projectedNet = projectedIncome - projectedExpense

        return CurrentMonthProjection(
            income = projectedIncome.setScale(2, RoundingMode.HALF_UP),
            expense = projectedExpense.setScale(2, RoundingMode.HALF_UP),
            net = projectedNet.setScale(2, RoundingMode.HALF_UP),
            savingsRate = percentage(projectedNet, projectedIncome),
        )
    }

    private fun creditCardExpenseForFullMonth(
        month: YearMonth,
        mode: SpendAccountingMode,
    ): BigDecimal =
        when (mode) {
            SpendAccountingMode.ACCRUAL -> {
                val monthStart = month.atDay(1).atStartOfDay()
                val monthEnd = month.atEndOfMonth().atTime(LocalTime.MAX)
                creditCardService
                    .getDebtsBetweenForAnalysis(monthStart, monthEnd)
                    .fold(BigDecimal.ZERO) { acc, debt -> acc + debt.amount }
            }

            SpendAccountingMode.CASH_FLOW -> {
                val paid =
                    creditCardService
                        .getAllPaidPaymentsByMonth(month)
                        .filter { !it.refunded }
                        .fold(BigDecimal.ZERO) { acc, payment -> acc + payment.paidAmount }
                paid + creditCardService.getTotalPendingPaymentsByMonth(month)
            }
        }

    private fun List<TopLineItemDTO>.total(): BigDecimal = fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }

    private fun List<WalletTransaction>.sumAmount(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }

    private data class CurrentMonthProjection(
        val income: BigDecimal,
        val expense: BigDecimal,
        val net: BigDecimal,
        val savingsRate: BigDecimal,
    )

    private fun percentage(
        part: BigDecimal,
        whole: BigDecimal,
    ): BigDecimal =
        if (whole.signum() == 0) {
            BigDecimal.ZERO.setScale(2)
        } else {
            part.divide(whole, 6, RoundingMode.HALF_UP).multiply(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
        }

    companion object {
        const val TOP_EXPENSES_LIMIT = 15
    }
}
