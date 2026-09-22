/*
 * Filename: ProposeTransactionTool.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.tools

import org.json.JSONArray
import org.json.JSONObject
import org.moinex.common.constant.TranslationKeys
import org.moinex.model.Category
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.dto.ai.ExtractedTransactionDTO
import org.moinex.model.dto.ai.TransactionKind
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.Wallet
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class ProposeTransactionTool(
    private val categoryService: CategoryService,
    private val walletService: WalletService,
    private val creditCardService: CreditCardService,
    private val preferencesService: PreferencesService,
) : LlmTool {
    companion object {
        const val NAME = "propose_transactions"

        fun parseResultJson(json: String): List<ExtractedTransactionDTO> {
            val array = JSONArray(json)
            return (0 until array.length()).map { toDto(array.getJSONObject(it)) }
        }

        private fun toDto(json: JSONObject): ExtractedTransactionDTO =
            ExtractedTransactionDTO(
                date = LocalDate.parse(json.getString("date")),
                description = json.optString("description"),
                amount = json.getBigDecimal("amount"),
                kind = parseKind(json.getString("kind")),
                movementType = parseMovementType(json.optString("movementType")),
                invoiceMonth = json.optYearMonthOrNull("invoiceMonth"),
                existingTransactionId = json.optIntOrNull("existingTransactionId"),
                suggestedWalletId = json.optIntOrNull("suggestedWalletId"),
                suggestedCreditCardId = json.optIntOrNull("suggestedCreditCardId"),
                suggestedCategoryId = json.optIntOrNull("suggestedCategoryId"),
                suggestedCategoryName = json.optString("suggestedCategoryName"),
                confidence = json.optDouble("confidence", 0.5),
            )

        private fun parseKind(value: String): TransactionKind =
            when (value.lowercase()) {
                "credit_card" -> TransactionKind.CREDIT_CARD
                "credit_card_payment" -> TransactionKind.CREDIT_CARD_PAYMENT
                else -> TransactionKind.WALLET
            }

        private fun parseMovementType(value: String): WalletTransactionType =
            if (value.lowercase() == "income") WalletTransactionType.INCOME else WalletTransactionType.EXPENSE

        private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key) || !has(key)) null else getInt(key)

        private fun JSONObject.optYearMonthOrNull(key: String): YearMonth? =
            optString(key).takeIf { it.isNotBlank() }?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
    }

    override val name = NAME
    override val description = preferencesService.translate(TranslationKeys.AI_TOOL_PROPOSE_TRANSACTIONS_DESCRIPTION)

    override val jsonSchema = buildJsonSchema().toString()

    private fun buildJsonSchema(): JSONObject {
        val amountProperty =
            JSONObject()
                .put("type", "number")
                .put("description", "Always positive - never negative, regardless of direction")

        val kindProperty =
            JSONObject()
                .put("type", "string")
                .put("enum", JSONArray(listOf("wallet", "credit_card", "credit_card_payment")))
                .put(
                    "description",
                    "wallet: a regular income/expense. credit_card: a purchase/debt on a card. " +
                        "credit_card_payment: paying a card's bill in full - use this INSTEAD of wallet for a " +
                        "statement line that pays off a card invoice, since the app already debits the wallet " +
                        "directly when an invoice is paid; proposing it as a wallet expense would double count it. " +
                        "Check get_credit_card_invoice_payment_status first - do not propose a credit_card_payment " +
                        "for an invoice that is already fully paid.",
                )

        val movementTypeProperty =
            JSONObject()
                .put("type", "string")
                .put("enum", JSONArray(listOf("income", "expense")))
                .put(
                    "description",
                    "Only meaningful when kind is wallet - money coming in vs going out. Always \"expense\" for " +
                        "kind credit_card or credit_card_payment.",
                )

        val invoiceMonthProperty =
            JSONObject()
                .put("type", "string")
                .put(
                    "description",
                    "Billing month (YYYY-MM) of the invoice being paid - only for kind credit_card_payment. " +
                        "Leave unset if unknown; defaults to the payment date's own month.",
                )

        val existingTransactionIdProperty =
            JSONObject()
                .put("type", "integer")
                .put(
                    "description",
                    "Only for kind wallet. The id (from get_transactions_in_period) of an already-recorded " +
                        "PENDING wallet transaction that this proposal confirms/updates - e.g. the user asked to " +
                        "confirm a pending entry, or this line plausibly matches one already listed as pending " +
                        "(similar date/amount/description). Leave unset to create a new transaction instead. " +
                        "Never set this for a transaction that is already confirmed.",
                )

        val itemProperties =
            JSONObject()
                .put("date", JSONObject().put("type", "string").put("format", "date"))
                .put("description", JSONObject().put("type", "string"))
                .put("amount", amountProperty)
                .put("kind", kindProperty)
                .put("movementType", movementTypeProperty)
                .put("invoiceMonth", invoiceMonthProperty)
                .put("existingTransactionId", existingTransactionIdProperty)
                .put("walletName", JSONObject().put("type", "string"))
                .put("creditCardName", JSONObject().put("type", "string"))
                .put("categoryName", JSONObject().put("type", "string"))
                .put("confidence", JSONObject().put("type", "number"))

        val requiredFields = JSONArray(listOf("date", "description", "amount", "kind", "movementType", "categoryName"))

        val item =
            JSONObject()
                .put("type", "object")
                .put("properties", itemProperties)
                .put("required", requiredFields)

        val transactionsProperty = JSONObject().put("type", "array").put("items", item)

        return JSONObject()
            .put("type", "object")
            .put("properties", JSONObject().put("transactions", transactionsProperty))
            .put("required", JSONArray(listOf("transactions")))
    }

    fun describeAvailableContext(): String {
        val categories = categoryService.getNonArchivedCategoriesOrderedByName().joinToString(", ") { it.name }
        val wallets = walletService.getAllNonArchivedWalletsOrderedByName().joinToString(", ") { it.name }
        val creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByName().joinToString(", ") { it.name }

        return "Existing categories: $categories\nExisting wallets: $wallets\nExisting credit cards: $creditCards"
    }

    override fun execute(argumentsJson: String): String {
        val candidates = JSONObject(argumentsJson).optJSONArray("transactions") ?: JSONArray()
        val categories = categoryService.getNonArchivedCategoriesOrderedByName()
        val wallets = walletService.getAllNonArchivedWalletsOrderedByName()
        val creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByName()

        val resolved =
            (0 until candidates.length()).mapNotNull { i ->
                runCatching { resolve(candidates.getJSONObject(i), categories, wallets, creditCards) }.getOrNull()
            }

        return JSONArray(resolved.map { toJson(it) }).toString()
    }

    private fun resolve(
        candidate: JSONObject,
        categories: List<Category>,
        wallets: List<Wallet>,
        creditCards: List<CreditCard>,
    ): ExtractedTransactionDTO {
        val kind = parseKind(candidate.optString("kind"))

        val categoryName = candidate.getString("categoryName")
        val matchedCategory = categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
        val matchedWallet =
            wallets.firstOrNull { it.name.equals(candidate.optString("walletName"), ignoreCase = true) }
        val matchedCreditCard =
            creditCards.firstOrNull { it.name.equals(candidate.optString("creditCardName"), ignoreCase = true) }

        // Only honored for an id that actually exists, still PENDING and on a wallet transaction -
        // never trust the model's id blindly, since confirming the wrong transaction (or one
        // that's already confirmed) would silently corrupt real data instead of just duplicating it.
        val existingTransactionId =
            candidate
                .optIntOrNull("existingTransactionId")
                ?.takeIf { id ->
                    kind == TransactionKind.WALLET &&
                        runCatching { walletService.getWalletTransactionById(id) }
                            .getOrNull()
                            ?.status == WalletTransactionStatus.PENDING
                }

        return ExtractedTransactionDTO(
            date = LocalDate.parse(candidate.getString("date")),
            description = candidate.optString("description"),
            amount = candidate.getBigDecimal("amount").abs(),
            kind = kind,
            movementType = parseMovementType(candidate.optString("movementType")),
            invoiceMonth = candidate.optYearMonthOrNull("invoiceMonth"),
            existingTransactionId = existingTransactionId,
            suggestedWalletId = matchedWallet?.id,
            suggestedCreditCardId = matchedCreditCard?.id,
            suggestedCategoryId = matchedCategory?.id,
            suggestedCategoryName = matchedCategory?.name ?: categoryName,
            confidence = candidate.optDouble("confidence", 0.5),
        )
    }

    private fun toJson(dto: ExtractedTransactionDTO): JSONObject =
        JSONObject()
            .put("date", dto.date.toString())
            .put("description", dto.description)
            .put("amount", dto.amount)
            .put("kind", dto.kind.name.lowercase())
            .put("movementType", dto.movementType.name.lowercase())
            .put("invoiceMonth", dto.invoiceMonth?.toString() ?: JSONObject.NULL)
            .put("existingTransactionId", dto.existingTransactionId ?: JSONObject.NULL)
            .put("suggestedWalletId", dto.suggestedWalletId ?: JSONObject.NULL)
            .put("suggestedCreditCardId", dto.suggestedCreditCardId ?: JSONObject.NULL)
            .put("suggestedCategoryId", dto.suggestedCategoryId ?: JSONObject.NULL)
            .put("suggestedCategoryName", dto.suggestedCategoryName)
            .put("confidence", dto.confidence)
}
