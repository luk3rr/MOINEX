/*
 * Filename: AnnualSummaryDTO.kt
 * Created on: September 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.moinex.model.enums.SpendAccountingMode
import java.math.BigDecimal
import java.time.LocalDate

data class AnnualSummaryDTO(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val mode: SpendAccountingMode,
    val monthCount: Int,
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val netBalance: BigDecimal,
    val savingsRatePercentage: BigDecimal,
    val expenseByCategory: List<CategoryBreakdownDTO>,
    val incomeByCategory: List<CategoryBreakdownDTO>,
    val monthlyFlows: List<MonthlyFlowDTO>,
    val topExpenses: List<TopLineItemDTO>,
    val insights: List<FinancialInsightDTO>,
)
