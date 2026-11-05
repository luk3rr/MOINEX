/*
 * Filename: ReadTools.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.tools

import org.json.JSONArray
import org.json.JSONObject
import org.moinex.service.CategoryService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.creditcard.RecurringCreditCardDebtService
import org.moinex.service.goal.GoalService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The read-only lookups that let the assistant search and list the user's records with pages, so it can answer from
 * real data instead of guessing. None of them writes anything.
 */
@Component
class ReadTools(
    private val walletService: WalletService,
    private val creditCardService: CreditCardService,
    private val categoryService: CategoryService,
    private val tickerService: TickerService,
    private val recurringTransactionService: RecurringTransactionService,
    private val goalService: GoalService,
    private val bondService: BondService,
    private val recurringCardDebtService: RecurringCreditCardDebtService,
) {
    companion object {
        const val SEARCH_TRANSACTIONS = "search_transactions"
        const val LIST_RECORDS = "list_records"
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 50
        private val DEFAULT_START: LocalDate = LocalDate.of(2000, 1, 1)
        private val DEFAULT_END: LocalDate = LocalDate.of(2100, 12, 31)
        val KINDS =
            listOf("wallets", "categories", "credit_cards", "tickers", "recurring", "goals", "bonds", "card_recurring")
    }

    fun tools(): List<LlmTool> = listOf(SearchTransactionsTool(), ListRecordsTool())

    /** One page of [rows] with the total, so the model can ask for the next one. */
    private fun page(
        rows: List<JSONObject>,
        arguments: JSONObject,
    ): String {
        val size = arguments.optInt("pageSize", DEFAULT_PAGE_SIZE).coerceIn(1, MAX_PAGE_SIZE)
        val page = arguments.optInt("page", 1).coerceAtLeast(1)
        val from = ((page - 1) * size).coerceAtMost(rows.size)
        return JSONObject()
            .put("total", rows.size)
            .put("page", page)
            .put("pageSize", size)
            .put("hasMore", from + size < rows.size)
            .put("items", JSONArray(rows.subList(from, (from + size).coerceAtMost(rows.size))))
            .toString()
    }

    private fun pagingProperties() =
        JSONObject()
            .put("page", JSONObject().put("type", "integer").put("description", "1-based page, default 1."))
            .put(
                "pageSize",
                JSONObject().put("type", "integer").put("description", "Items per page, default 20, at most 50."),
            )

    private inner class SearchTransactionsTool : LlmTool {
        override val name = SEARCH_TRANSACTIONS
        override val description =
            "Searches the user's wallet transactions and credit card purchases by text in the description, " +
                "category, wallet or card, amount range and period, newest first, with pages and the total. Use " +
                "it to find a record (and its id) before editing it or to answer questions about specific entries."
        override val jsonSchema: String =
            JSONObject()
                .put("type", "object")
                .put(
                    "properties",
                    pagingProperties()
                        .put("text", JSONObject().put("type", "string").put("description", "Part of the description."))
                        .put("category", JSONObject().put("type", "string"))
                        .put(
                            "walletOrCard",
                            JSONObject().put("type", "string").put("description", "Wallet or card name."),
                        ).put("minAmount", JSONObject().put("type", "number"))
                        .put("maxAmount", JSONObject().put("type", "number"))
                        .put("startDate", JSONObject().put("type", "string").put("format", "date"))
                        .put("endDate", JSONObject().put("type", "string").put("format", "date")),
                ).toString()

        override fun execute(argumentsJson: String): String {
            val arguments = JSONObject(argumentsJson)
            val start = arguments.optString("startDate").takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
            val end = arguments.optString("endDate").takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
            val from = (start ?: DEFAULT_START).atStartOfDay()
            val to = (end ?: DEFAULT_END).atTime(23, 59, 59)
            val text = arguments.optString("text").trim()
            val category = arguments.optString("category").trim()
            val owner = arguments.optString("walletOrCard").trim()
            val min = if (arguments.has("minAmount")) arguments.getBigDecimal("minAmount") else null
            val max = if (arguments.has("maxAmount")) arguments.getBigDecimal("maxAmount") else null

            fun matches(
                description: String?,
                categoryName: String,
                ownerName: String,
                amount: BigDecimal,
            ) = (text.isEmpty() || description.orEmpty().contains(text, ignoreCase = true)) &&
                (category.isEmpty() || categoryName.equals(category, ignoreCase = true)) &&
                (owner.isEmpty() || ownerName.equals(owner, ignoreCase = true)) &&
                (min == null || amount >= min) &&
                (max == null || amount <= max)

            val wallet =
                walletService
                    .getAllNonArchivedWalletTransactionsBetweenDates(from, to)
                    .filter { matches(it.description, it.category.name, it.wallet.name, it.amount) }
                    .map {
                        it.date to
                            JSONObject()
                                .put("kind", "wallet_transaction")
                                .put("id", it.id)
                                .put("date", it.date.toLocalDate().toString())
                                .put("description", it.description.orEmpty())
                                .put("amount", it.amount)
                                .put("type", it.type.name.lowercase())
                                .put("status", it.status.name.lowercase())
                                .put("wallet", it.wallet.name)
                                .put("category", it.category.name)
                    }
            val card =
                creditCardService
                    .getDebtsBetweenForAnalysis(from, to)
                    .filter { matches(it.description, it.category.name, it.creditCard.name, it.amount) }
                    .map {
                        it.date to
                            JSONObject()
                                .put("kind", "credit_card_debt")
                                .put("id", it.id)
                                .put("date", it.date.toLocalDate().toString())
                                .put("description", it.description.orEmpty())
                                .put("amount", it.amount)
                                .put("creditCard", it.creditCard.name)
                                .put("category", it.category.name)
                    }

            return page((wallet + card).sortedByDescending { it.first }.map { it.second }, arguments)
        }
    }

    private inner class ListRecordsTool : LlmTool {
        override val name = LIST_RECORDS
        override val description =
            "Lists the user's records of one kind, with pages and the total: ${KINDS.joinToString()}."
        override val jsonSchema: String =
            JSONObject()
                .put("type", "object")
                .put(
                    "properties",
                    pagingProperties()
                        .put("kind", JSONObject().put("type", "string").put("enum", JSONArray(KINDS)))
                        .put(
                            "includeArchived",
                            JSONObject()
                                .put(
                                    "type",
                                    "boolean",
                                ).put("description", "Only for wallets, cards and goals."),
                        ),
                ).put("required", JSONArray(listOf("kind")))
                .toString()

        override fun execute(argumentsJson: String): String {
            val arguments = JSONObject(argumentsJson)
            val includeArchived = arguments.optBoolean("includeArchived", false)

            val rows: List<JSONObject> =
                when (val kind = arguments.getString("kind")) {
                    "wallets" ->
                        (
                            if (includeArchived) {
                                walletService.getAllWalletsOrderedByName()
                            } else {
                                walletService
                                    .getAllNonArchivedWalletsOrderedByName()
                            }
                        ).map {
                            JSONObject()
                                .put("id", it.id)
                                .put("name", it.name)
                                .put("type", it.type.name)
                                .put("balance", it.balance)
                                .put("archived", it.isArchived)
                        }
                    "categories" ->
                        categoryService.getCategories().filter { includeArchived || !it.isArchived }.map {
                            JSONObject().put("id", it.id).put("name", it.name).put("archived", it.isArchived)
                        }
                    "credit_cards" ->
                        (
                            creditCardService.getAllNonArchivedCreditCardsOrderedByName() +
                                if (includeArchived) creditCardService.getAllArchivedCreditCards() else emptyList()
                        ).map {
                            JSONObject()
                                .put("id", it.id)
                                .put("name", it.name)
                                .put("limit", it.maxDebt)
                                .put("closingDay", it.closingDay)
                                .put("dueDay", it.billingDueDay)
                                .put("archived", it.isArchived)
                        }
                    "tickers" ->
                        tickerService.getAllNonArchivedTickers().map {
                            JSONObject()
                                .put("id", it.id)
                                .put("symbol", it.symbol)
                                .put("name", it.name)
                                .put("type", it.type.name.lowercase())
                                .put("quantity", it.currentQuantity)
                                .put("price", it.currentUnitValue)
                        }
                    "recurring" ->
                        recurringTransactionService.getAllRecurringTransactions().map {
                            JSONObject()
                                .put("id", it.id)
                                .put("description", it.description.orEmpty())
                                .put("amount", it.amount)
                                .put("type", it.type.name.lowercase())
                                .put("frequency", it.frequency.name.lowercase())
                                .put("wallet", it.wallet.name)
                                .put("category", it.category.name)
                                .put("status", it.status.name.lowercase())
                                .put("nextDueDate", it.nextDueDate.toString())
                        }
                    "goals" ->
                        goalService.getAllGoals().filter { includeArchived || !it.isArchived }.map {
                            JSONObject()
                                .put("id", it.id)
                                .put("name", it.name)
                                .put("balance", it.balance)
                                .put("target", it.targetBalance)
                                .put("targetDate", it.targetDate.toString())
                                .put("archived", it.isArchived)
                        }
                    "bonds" ->
                        bondService.getAllNonArchivedBonds().map {
                            JSONObject()
                                .put("id", it.id)
                                .put("name", it.name)
                                .put("symbol", it.symbol.orEmpty())
                                .put("type", it.type.name.lowercase())
                                .put("maturityDate", it.maturityDate?.toString().orEmpty())
                        }
                    "card_recurring" ->
                        recurringCardDebtService.getAllRecurringDebts().map {
                            JSONObject()
                                .put("id", it.id)
                                .put("description", it.description.orEmpty())
                                .put("amount", it.amount)
                                .put("creditCard", it.creditCard.name)
                                .put("category", it.category.name)
                                .put("frequency", it.frequency.name.lowercase())
                                .put("dayOfMonth", it.dayOfMonth)
                                .put("status", it.status.name.lowercase())
                        }
                    else -> error("unknown kind '$kind'; use one of ${KINDS.joinToString()}")
                }
            return page(rows, arguments)
        }
    }
}
