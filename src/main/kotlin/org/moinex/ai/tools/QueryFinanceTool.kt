/*
 * Filename: QueryFinanceTool.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.tools

import org.json.JSONArray
import org.json.JSONObject
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.atEndOfDay
import org.moinex.common.extension.isExpense
import org.moinex.common.extension.isIncome
import org.moinex.common.extension.isNotZero
import org.moinex.common.extension.toRounded
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.financialplanning.FinancialPlanningService
import org.moinex.service.goal.GoalService
import org.moinex.service.investment.InvestmentPerformanceService
import org.moinex.service.networth.NetWorthService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/**
 * Read-only finance queries exposed to the chat assistant as tools.
 */
@Service
class QueryFinanceTool(
    private val walletService: WalletService,
    private val creditCardService: CreditCardService,
    private val netWorthService: NetWorthService,
    private val goalService: GoalService,
    private val financialPlanningService: FinancialPlanningService,
    private val investmentPerformanceService: InvestmentPerformanceService,
    private val preferencesService: PreferencesService,
) {
    fun availableTools(): List<LlmTool> =
        listOf(
            GetNetWorthTool(),
            GetSpendingByCategoryTool(),
            GetTransactionsInPeriodTool(),
            GetCreditCardInvoicePaymentStatusTool(),
            GetIncomeVsExpensesTool(),
            ComparePeriodsTool(),
            GetGoalsProgressTool(),
            GetBudgetVsActualTool(),
            GetPortfolioSummaryTool(),
        )

    private fun monthParamSchema(monthDescriptionKey: String): String =
        JSONObject()
            .put("type", "object")
            .put(
                "properties",
                JSONObject().put(
                    "month",
                    JSONObject()
                        .put("type", "string")
                        .put("description", preferencesService.translate(monthDescriptionKey)),
                ),
            ).toString()

    /** Wallet expenses + credit card purchases for the month, grouped by category (accrual view). */
    private fun expensesByCategory(month: YearMonth): Map<String, BigDecimal> {
        val walletTotals =
            walletService
                .getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(month)
                .filter { it.isExpense() }
                .groupBy { it.category.name }
                .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }

        val startOfMonth = month.atDay(1).atStartOfDay()
        val endOfMonth = month.atEndOfMonth().atEndOfDay()

        val creditCardTotals =
            creditCardService
                .getDebtsBetweenForAnalysis(startOfMonth, endOfMonth)
                .groupBy { it.category.name }
                .mapValues { (_, debts) -> debts.sumOf { it.amount } }

        val categories = walletTotals.keys + creditCardTotals.keys
        return categories.associateWith { category ->
            (walletTotals[category] ?: BigDecimal.ZERO) + (creditCardTotals[category] ?: BigDecimal.ZERO)
        }
    }

    /** Wallet income confirmed in the month. */
    private fun totalIncome(month: YearMonth): BigDecimal =
        walletService
            .getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(month)
            .filter { it.isIncome() }
            .sumOf { it.amount }

    private inner class GetNetWorthTool : LlmTool {
        override val name = "get_net_worth"
        override val description = preferencesService.translate(TranslationKeys.AI_TOOL_NET_WORTH_DESCRIPTION)
        override val jsonSchema = monthParamSchema(TranslationKeys.AI_TOOL_NET_WORTH_MONTH_PARAM)

        override fun execute(argumentsJson: String): String {
            val month = parseMonthArgument(argumentsJson) ?: return errorResult("invalid_month")
            val snapshot = netWorthService.getSnapshot(month)

            return JSONObject()
                .apply {
                    put("month", month.toString())
                    if (snapshot == null) {
                        put("available", false)
                    } else {
                        put("available", true)
                        put("netWorth", snapshot.netWorth)
                        put("assets", snapshot.assets)
                        put("liabilities", snapshot.liabilities)
                    }
                }.toString()
        }
    }

    private inner class GetSpendingByCategoryTool : LlmTool {
        override val name = "get_spending_by_category"
        override val description = preferencesService.translate(TranslationKeys.AI_TOOL_SPENDING_DESCRIPTION)
        override val jsonSchema = monthParamSchema(TranslationKeys.AI_TOOL_SPENDING_MONTH_PARAM)

        override fun execute(argumentsJson: String): String {
            val month = parseMonthArgument(argumentsJson) ?: return errorResult("invalid_month")
            val byCategory = expensesByCategory(month)

            return JSONObject()
                .apply {
                    put("month", month.toString())
                    put(
                        "byCategory",
                        JSONObject().apply {
                            byCategory.forEach { (category, total) -> put(category, total) }
                        },
                    )
                    put("total", byCategory.values.fold(BigDecimal.ZERO, BigDecimal::add))
                }.toString()
        }
    }

    private inner class GetTransactionsInPeriodTool : LlmTool {
        override val name = "get_transactions_in_period"
        override val description =
            preferencesService.translate(TranslationKeys.AI_TOOL_TRANSACTIONS_IN_PERIOD_DESCRIPTION)
        override val jsonSchema =
            JSONObject()
                .put("type", "object")
                .put(
                    "properties",
                    JSONObject()
                        .put(
                            "startDate",
                            JSONObject()
                                .put("type", "string")
                                .put(
                                    "description",
                                    preferencesService.translate(TranslationKeys.AI_TOOL_TRANSACTIONS_START_PARAM),
                                ),
                        ).put(
                            "endDate",
                            JSONObject()
                                .put("type", "string")
                                .put(
                                    "description",
                                    preferencesService.translate(TranslationKeys.AI_TOOL_TRANSACTIONS_END_PARAM),
                                ),
                        ),
                ).put("required", JSONArray(listOf("startDate", "endDate")))
                .toString()

        override fun execute(argumentsJson: String): String {
            val json = JSONObject(argumentsJson)
            val start =
                runCatching { LocalDate.parse(json.getString("startDate")) }.getOrNull()
                    ?: return errorResult("invalid_period")
            val end =
                runCatching { LocalDate.parse(json.getString("endDate")) }.getOrNull()
                    ?: return errorResult("invalid_period")

            val walletTransactions =
                walletService.getAllNonArchivedWalletTransactionsBetweenDates(start.atStartOfDay(), end.atEndOfDay())
            val creditCardDebts = creditCardService.getDebtsBetweenForAnalysis(start.atStartOfDay(), end.atEndOfDay())

            val walletJson =
                JSONArray(
                    walletTransactions.map { t ->
                        JSONObject()
                            .put("date", t.date.toLocalDate().toString())
                            .put("amount", t.amount)
                            .put("description", t.description.orEmpty())
                            .put("category", t.category.name)
                            .put("type", t.type.name.lowercase())
                    },
                )
            val creditCardJson =
                JSONArray(
                    creditCardDebts.map { d ->
                        JSONObject()
                            .put("date", d.date.toLocalDate().toString())
                            .put("amount", d.amount)
                            .put("description", d.description.orEmpty())
                            .put("category", d.category.name)
                            .put("creditCard", d.creditCard.name)
                    },
                )

            return JSONObject()
                .put("walletTransactions", walletJson)
                .put("creditCardDebts", creditCardJson)
                .toString()
        }
    }

    private inner class GetCreditCardInvoicePaymentStatusTool : LlmTool {
        override val name = "get_credit_card_invoice_payment_status"
        override val description = preferencesService.translate(TranslationKeys.AI_TOOL_INVOICE_STATUS_DESCRIPTION)
        override val jsonSchema =
            JSONObject()
                .put("type", "object")
                .put(
                    "properties",
                    JSONObject()
                        .put(
                            "creditCardName",
                            JSONObject()
                                .put("type", "string")
                                .put(
                                    "description",
                                    preferencesService.translate(TranslationKeys.AI_TOOL_INVOICE_STATUS_CARD_PARAM),
                                ),
                        ).put(
                            "month",
                            JSONObject()
                                .put("type", "string")
                                .put(
                                    "description",
                                    preferencesService.translate(TranslationKeys.AI_TOOL_INVOICE_STATUS_MONTH_PARAM),
                                ),
                        ),
                ).put("required", JSONArray(listOf("creditCardName", "month")))
                .toString()

        override fun execute(argumentsJson: String): String {
            val json = JSONObject(argumentsJson)
            val cardName = json.optString("creditCardName", "")
            val month =
                runCatching { YearMonth.parse(json.optString("month", "")) }.getOrNull()
                    ?: return errorResult("invalid_month")
            val card =
                creditCardService
                    .getAllNonArchivedCreditCardsOrderedByName()
                    .firstOrNull { it.name.equals(cardName, ignoreCase = true) }
                    ?: return errorResult("credit_card_not_found")

            return JSONObject()
                .put("creditCardName", card.name)
                .put("month", month.toString())
                .put("paid", creditCardService.isInvoicePaid(card.id!!, month))
                .toString()
        }
    }

    private inner class GetIncomeVsExpensesTool : LlmTool {
        override val name = "get_income_vs_expenses"
        override val description =
            preferencesService.translate(TranslationKeys.AI_TOOL_INCOME_VS_EXPENSES_DESCRIPTION)
        override val jsonSchema = monthParamSchema(TranslationKeys.AI_TOOL_INCOME_VS_EXPENSES_MONTH_PARAM)

        override fun execute(argumentsJson: String): String {
            val month = parseMonthArgument(argumentsJson) ?: return errorResult("invalid_month")
            val income = totalIncome(month)
            val expenses = expensesByCategory(month).values.fold(BigDecimal.ZERO, BigDecimal::add)
            val savings = income.subtract(expenses)
            val savingsRatePercent =
                if (income.isNotZero()) {
                    savings.divide(income, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toRounded(1)
                } else {
                    null
                }

            return JSONObject()
                .apply {
                    put("month", month.toString())
                    put("income", income)
                    put("expenses", expenses)
                    put("savings", savings)
                    put("savingsRatePercent", savingsRatePercent ?: JSONObject.NULL)
                }.toString()
        }
    }

    private inner class ComparePeriodsTool : LlmTool {
        override val name = "compare_periods"
        override val description = preferencesService.translate(TranslationKeys.AI_TOOL_COMPARE_PERIODS_DESCRIPTION)
        override val jsonSchema =
            JSONObject()
                .put("type", "object")
                .put(
                    "properties",
                    JSONObject()
                        .put(
                            "month1",
                            JSONObject()
                                .put("type", "string")
                                .put(
                                    "description",
                                    preferencesService.translate(TranslationKeys.AI_TOOL_COMPARE_PERIODS_MONTH1_PARAM),
                                ),
                        ).put(
                            "month2",
                            JSONObject()
                                .put("type", "string")
                                .put(
                                    "description",
                                    preferencesService.translate(TranslationKeys.AI_TOOL_COMPARE_PERIODS_MONTH2_PARAM),
                                ),
                        ),
                ).put("required", JSONArray(listOf("month1", "month2")))
                .toString()

        override fun execute(argumentsJson: String): String {
            val json = JSONObject(argumentsJson)
            val month1 =
                runCatching { YearMonth.parse(json.getString("month1")) }.getOrNull()
                    ?: return errorResult("invalid_month")
            val month2 =
                runCatching { YearMonth.parse(json.getString("month2")) }.getOrNull()
                    ?: return errorResult("invalid_month")

            val income1 = totalIncome(month1)
            val income2 = totalIncome(month2)
            val byCategory1 = expensesByCategory(month1)
            val byCategory2 = expensesByCategory(month2)
            val expenses1 = byCategory1.values.fold(BigDecimal.ZERO, BigDecimal::add)
            val expenses2 = byCategory2.values.fold(BigDecimal.ZERO, BigDecimal::add)

            val categories = byCategory1.keys + byCategory2.keys
            val byCategoryDelta =
                categories.associateWith { category ->
                    (byCategory2[category] ?: BigDecimal.ZERO).subtract(byCategory1[category] ?: BigDecimal.ZERO)
                }

            return JSONObject()
                .apply {
                    put("month1", month1.toString())
                    put("month2", month2.toString())
                    put("income1", income1)
                    put("income2", income2)
                    put("incomeDelta", income2.subtract(income1))
                    put("expenses1", expenses1)
                    put("expenses2", expenses2)
                    put("expensesDelta", expenses2.subtract(expenses1))
                    put(
                        "byCategoryDelta",
                        JSONObject().apply {
                            byCategoryDelta.forEach { (category, delta) -> put(category, delta) }
                        },
                    )
                }.toString()
        }
    }

    private inner class GetGoalsProgressTool : LlmTool {
        override val name = "get_goals_progress"
        override val description = preferencesService.translate(TranslationKeys.AI_TOOL_GOALS_PROGRESS_DESCRIPTION)
        override val jsonSchema =
            JSONObject().put("type", "object").put("properties", JSONObject()).toString()

        override fun execute(argumentsJson: String): String {
            val goals =
                goalService
                    .getAllGoals()
                    .filter { !it.isArchived }
                    .map { goal ->
                        val denominator = goal.targetBalance.subtract(goal.initialBalance)
                        val progressPercentage =
                            if (denominator.isNotZero()) {
                                goal.balance
                                    .subtract(goal.initialBalance)
                                    .divide(denominator, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal(100))
                                    .toRounded(1)
                                    .coerceIn(BigDecimal.ZERO, BigDecimal(100))
                            } else if (goal.balance >= goal.targetBalance) {
                                BigDecimal(100)
                            } else {
                                BigDecimal.ZERO
                            }

                        JSONObject()
                            .put("name", goal.name)
                            .put("initialBalance", goal.initialBalance)
                            .put("targetBalance", goal.targetBalance)
                            .put("currentBalance", goal.balance)
                            .put("targetDate", goal.targetDate.toString())
                            .put("completionDate", goal.completionDate?.toString() ?: JSONObject.NULL)
                            .put("progressPercentage", progressPercentage)
                    }

            return JSONObject().put("goals", JSONArray(goals)).toString()
        }
    }

    private inner class GetBudgetVsActualTool : LlmTool {
        override val name = "get_budget_vs_actual"
        override val description = preferencesService.translate(TranslationKeys.AI_TOOL_BUDGET_VS_ACTUAL_DESCRIPTION)
        override val jsonSchema = monthParamSchema(TranslationKeys.AI_TOOL_BUDGET_VS_ACTUAL_MONTH_PARAM)

        override fun execute(argumentsJson: String): String {
            val month = parseMonthArgument(argumentsJson) ?: return errorResult("invalid_month")
            val plan =
                financialPlanningService.getActivePlan()
                    ?: return JSONObject().put("available", false).toString()

            val groups =
                financialPlanningService.getPlanStatus(plan.id!!, month).map { status ->
                    val targetAmount =
                        plan.baseIncome
                            .multiply(status.group.targetPercentage)
                            .divide(BigDecimal(100))
                            .toRounded()

                    JSONObject()
                        .put("name", status.group.name)
                        .put("targetPercentage", status.group.targetPercentage)
                        .put("targetAmount", targetAmount)
                        .put("spentAmount", status.spentAmount)
                        .put("remaining", targetAmount.subtract(status.spentAmount))
                        .put("overBudget", status.spentAmount > targetAmount)
                }

            return JSONObject()
                .apply {
                    put("available", true)
                    put("planName", plan.name)
                    put("month", month.toString())
                    put("baseIncome", plan.baseIncome)
                    put("groups", JSONArray(groups))
                }.toString()
        }
    }

    private inner class GetPortfolioSummaryTool : LlmTool {
        override val name = "get_portfolio_summary"
        override val description =
            preferencesService.translate(TranslationKeys.AI_TOOL_PORTFOLIO_SUMMARY_DESCRIPTION)
        override val jsonSchema =
            JSONObject().put("type", "object").put("properties", JSONObject()).toString()

        override fun execute(argumentsJson: String): String {
            val data = investmentPerformanceService.getPerformanceData()
            val latestMonth =
                data.portfolioValues.keys.maxOrNull() ?: return JSONObject().put("available", false).toString()

            return JSONObject()
                .apply {
                    put("available", true)
                    put("month", latestMonth.toString())
                    put("portfolioValue", data.portfolioValues[latestMonth] ?: BigDecimal.ZERO)
                    put("investedThisMonth", data.monthlyInvested[latestMonth] ?: BigDecimal.ZERO)
                    put("monthlyGains", data.monthlyGains[latestMonth] ?: BigDecimal.ZERO)
                    put("accumulatedGains", data.accumulatedGains[latestMonth] ?: BigDecimal.ZERO)
                }.toString()
        }
    }

    private fun parseMonthArgument(argumentsJson: String): YearMonth? {
        val monthText = runCatching { JSONObject(argumentsJson).optString("month", "") }.getOrDefault("")
        if (monthText.isBlank()) return YearMonth.now()

        return runCatching { YearMonth.parse(monthText) }.getOrNull()
    }

    private fun errorResult(code: String): String = JSONObject().put("error", code).toString()
}
