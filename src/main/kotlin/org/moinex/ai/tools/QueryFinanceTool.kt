/*
 * Filename: QueryFinanceTool.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.tools

import org.json.JSONObject
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.atEndOfDay
import org.moinex.model.enums.WalletTransactionType
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.networth.NetWorthService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Read-only finance queries exposed to the chat assistant as tools.
 */
@Service
class QueryFinanceTool(
    private val walletService: WalletService,
    private val creditCardService: CreditCardService,
    private val netWorthService: NetWorthService,
    private val preferencesService: PreferencesService,
) {
    fun availableTools(): List<LlmTool> = listOf(GetNetWorthTool(), GetSpendingByCategoryTool())

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

            val walletTotals =
                walletService
                    .getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(month)
                    .filter { it.type == WalletTransactionType.EXPENSE }
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
            val byCategory =
                categories.associateWith { category ->
                    (walletTotals[category] ?: BigDecimal.ZERO) + (creditCardTotals[category] ?: BigDecimal.ZERO)
                }

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

    private fun parseMonthArgument(argumentsJson: String): YearMonth? {
        val monthText = runCatching { JSONObject(argumentsJson).optString("month", "") }.getOrDefault("")
        if (monthText.isBlank()) return YearMonth.now()

        return runCatching { YearMonth.parse(monthText) }.getOrNull()
    }

    private fun errorResult(code: String): String = JSONObject().put("error", code).toString()
}
