/*
 * Filename: CreateWalletTransactionOperation.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation.transactions

import org.json.JSONObject
import org.moinex.ai.operation.ApplyResult
import org.moinex.ai.operation.BaseOperation
import org.moinex.ai.operation.CategoryResolver
import org.moinex.ai.operation.DraftChoices
import org.moinex.ai.operation.DraftField
import org.moinex.ai.operation.DraftFieldType
import org.moinex.ai.operation.DraftValues
import org.moinex.ai.operation.EntityCatalog
import org.moinex.ai.operation.OperationDomain
import org.moinex.ai.operation.OperationRisk
import org.moinex.ai.operation.ValidationResult
import org.moinex.common.constant.TranslationKeys
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.PreferencesService
import org.moinex.service.matching.MatchQuery
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/**
 * [existingTransactionId], when set, is a PENDING transaction that this operation confirms and updates instead of
 * creating a new one. [walletId] is null while the model's wallet name matched no wallet; the user fixes it in review.
 */
data class WalletTransactionPayload(
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val movementType: WalletTransactionType,
    val walletId: Int?,
    val categoryName: String,
    val existingTransactionId: Int? = null,
)

@Component
class CreateWalletTransactionOperation(
    private val walletService: WalletService,
    private val categoryResolver: CategoryResolver,
    private val preferencesService: PreferencesService,
) : BaseOperation<WalletTransactionPayload>() {
    override val name = NAME
    override val domain = OperationDomain.TRANSACTIONS
    override val risk = OperationRisk.LOW

    override fun modelRequired() = listOf("date", "amount", "walletName", "categoryName", "movementType")

    companion object {
        const val NAME = "create_wallet_transaction"
    }

    override fun modelDescription() =
        "create_wallet_transaction: a regular income or expense on a wallet (use movementType); never a card " +
            "purchase, a card invoice payment or a market operation."

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf(
            "movementType" to
                property(
                    "string",
                    "Only for create_wallet_transaction: income (money coming in) or expense (going out).",
                ).put("enum", org.json.JSONArray(listOf("income", "expense"))),
            "existingTransactionId" to
                property(
                    "integer",
                    "Only for create_wallet_transaction. The id (from get_transactions_in_period) of an " +
                        "already-recorded PENDING wallet transaction that this change confirms/updates - e.g. the " +
                        "user asked to confirm a pending entry. Leave unset to create a new one. Never set it for " +
                        "a transaction that is already confirmed. When set, the app keeps the existing " +
                        "transaction's own description regardless of what description you send.",
                ),
        )

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): WalletTransactionPayload {
        val categoryName = arguments.getString("categoryName")
        return WalletTransactionPayload(
            date = arguments.requireDate("date"),
            description = arguments.optString("description"),
            amount = arguments.getBigDecimal("amount").abs(),
            movementType =
                if (arguments.optString("movementType").equals("income", ignoreCase = true)) {
                    WalletTransactionType.INCOME
                } else {
                    WalletTransactionType.EXPENSE
                },
            walletId = catalog.wallet(arguments.optString("walletName"))?.id,
            categoryName = catalog.category(categoryName)?.name ?: categoryName,
        )
    }

    override fun validate(payload: WalletTransactionPayload): ValidationResult =
        when {
            payload.walletId == null ->
                ValidationResult.error(preferencesService.translate(TranslationKeys.AI_REVIEW_NO_WALLET_SELECTED))
            payload.amount.signum() <= 0 -> ValidationResult.error("amount must be positive")
            payload.categoryName.isBlank() -> ValidationResult.error("categoryName is required")
            else -> ValidationResult.OK
        }

    override fun missingFromModel(payload: WalletTransactionPayload) =
        if (payload.amount.signum() <= 0) listOf("amount must be greater than 0") else emptyList()

    override fun categoryNeeded(payload: WalletTransactionPayload) = payload.categoryName.takeIf { it.isNotBlank() }

    override fun summary(payload: WalletTransactionPayload) =
        "${payload.description}, ${payload.amount.toPlainString()}, ${payload.date}"

    override fun matchQuery(payload: WalletTransactionPayload) =
        MatchQuery(
            amount = payload.amount,
            date = payload.date,
            walletId = payload.walletId,
            isIncome = payload.movementType == WalletTransactionType.INCOME,
            description = payload.description.takeIf { it.isNotBlank() },
        )

    override fun confirmingPending(
        payload: WalletTransactionPayload,
        existingTransactionId: Int,
    ): WalletTransactionPayload? {
        val existing = runCatching { walletService.getWalletTransactionById(existingTransactionId) }.getOrNull()
        if (existing?.status != WalletTransactionStatus.PENDING) return null

        return payload.copy(
            existingTransactionId = existingTransactionId,
            description = existing.description?.takeIf { it.isNotBlank() } ?: payload.description,
        )
    }

    override fun draftSpec() =
        listOf(
            DraftField("date", "aidraft.field.date", DraftFieldType.DATE),
            DraftField("description", "aidraft.field.description", DraftFieldType.TEXT, required = false),
            DraftField("amount", "aidraft.field.amount", DraftFieldType.DECIMAL),
            DraftField(
                "movementType",
                "aidraft.field.movementType",
                DraftFieldType.CHOICE,
                DraftChoices.MOVEMENT_TYPES,
            ),
            DraftField("walletId", "aidraft.field.wallet", DraftFieldType.CHOICE, DraftChoices.WALLETS),
            DraftField("categoryName", "aidraft.field.category", DraftFieldType.CHOICE, DraftChoices.CATEGORIES),
        )

    override fun toDraft(payload: WalletTransactionPayload) =
        DraftValues(
            name,
            mapOf(
                "date" to payload.date,
                "description" to payload.description,
                "amount" to payload.amount,
                "movementType" to payload.movementType.name.lowercase(),
                "walletId" to payload.walletId,
                "categoryName" to payload.categoryName,
                "existingTransactionId" to payload.existingTransactionId,
            ),
        )

    override fun fromDraft(draft: DraftValues): WalletTransactionPayload? {
        val fields = draft.fields
        return WalletTransactionPayload(
            date = fields["date"].asDate() ?: return null,
            description = fields["description"] as? String ?: "",
            amount = fields["amount"].asDecimal()?.abs() ?: return null,
            movementType =
                if ((fields["movementType"] as? String).equals("income", ignoreCase = true)) {
                    WalletTransactionType.INCOME
                } else {
                    WalletTransactionType.EXPENSE
                },
            walletId = fields["walletId"].asInt(),
            categoryName = fields["categoryName"] as? String ?: "",
            existingTransactionId = (fields["existingTransactionId"]).asInt(),
        )
    }

    override fun execute(payload: WalletTransactionPayload): ApplyResult.Success {
        val wallet = walletService.getWalletById(payload.walletId!!)
        val transaction =
            WalletTransaction(
                id = payload.existingTransactionId,
                date = payload.date.atStartOfDay(),
                status = WalletTransactionStatus.CONFIRMED,
                description = payload.description,
                includeInAnalysis = true,
                wallet = wallet,
                category = categoryResolver.resolveOrCreate(payload.categoryName),
                type = payload.movementType,
                amount = payload.amount,
            )

        return if (payload.existingTransactionId != null) {
            walletService.updateWalletTransaction(transaction)
            ApplyResult.Success(listOf(payload.existingTransactionId), payload.description)
        } else {
            val id = walletService.createWalletTransaction(transaction, publishNotification = true)
            ApplyResult.Success(listOf(id), payload.description)
        }
    }
}
