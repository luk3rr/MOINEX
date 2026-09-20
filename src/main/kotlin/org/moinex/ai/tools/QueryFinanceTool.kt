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
import org.moinex.model.enums.WalletTransactionType
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.networth.NetWorthService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Service
import java.math.BigDecimal
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
    private val preferencesService: PreferencesService,
) {
    fun availableTools(): List<LlmTool> =
        listOf(
            GetNetWorthTool(),
            GetSpendingByCategoryTool(),
            GetTransactionsInPeriodTool(),
            GetCreditCardInvoicePaymentStatusTool(),
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

    private fun parseMonthArgument(argumentsJson: String): YearMonth? {
        val monthText = runCatching { JSONObject(argumentsJson).optString("month", "") }.getOrDefault("")
        if (monthText.isBlank()) return YearMonth.now()

        return runCatching { YearMonth.parse(monthText) }.getOrNull()
    }

    private fun errorResult(code: String): String = JSONObject().put("error", code).toString()
}
