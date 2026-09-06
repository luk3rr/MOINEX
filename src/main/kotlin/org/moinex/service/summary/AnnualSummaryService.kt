/*
 * Filename: AnnualSummaryService.kt
 * Created on: September 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.summary

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
    private val insightGenerator: FinancialInsightGenerator,
    private val preferencesService: PreferencesService,
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
        val monthlyFlows = buildMonthlyFlows(months, incomeItems, expenseItems)
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
    ): List<MonthlyFlowDTO> =
        months.map { month ->
            val income = incomeItems.filter { YearMonth.from(it.date) == month }.total()
            val expense = expenseItems.filter { YearMonth.from(it.date) == month }.total()
            MonthlyFlowDTO(
                period = month,
                income = income.setScale(2, RoundingMode.HALF_UP),
                expense = expense.setScale(2, RoundingMode.HALF_UP),
                net = (income - expense).setScale(2, RoundingMode.HALF_UP),
                savingsRatePercentage = percentage(income - expense, income),
            )
        }

    private fun List<TopLineItemDTO>.total(): BigDecimal = fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }

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
