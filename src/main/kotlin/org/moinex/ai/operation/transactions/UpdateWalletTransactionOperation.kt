/*
 * Filename: UpdateWalletTransactionOperation.kt
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
import org.moinex.ai.operation.Preview
import org.moinex.ai.operation.ValidationResult
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/** What a stored transaction looked like before the change, to show "before → after" in the card. */
data class TransactionSnapshot(
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val movementType: WalletTransactionType,
    val walletName: String,
    val categoryName: String,
)

/** The whole transaction as it will be after the change; [walletId] is null while the wallet name matched none. */
data class UpdateWalletTransactionPayload(
    val transactionId: Int,
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val movementType: WalletTransactionType,
    val walletId: Int?,
    val categoryName: String,
    val before: TransactionSnapshot? = null,
)

@Component
class UpdateWalletTransactionOperation(
    private val walletService: WalletService,
    private val categoryResolver: CategoryResolver,
) : BaseOperation<UpdateWalletTransactionPayload>() {
    override val name = "update_wallet_transaction"
    override val domain = OperationDomain.TRANSACTIONS
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("transactionId")

    override fun modelDescription() =
        "update_wallet_transaction: change an already recorded wallet transaction (transactionId, from " +
            "get_transactions_in_period). Send only the fields to change; the others keep their value. It never " +
            "changes the status; to confirm a pending one use create_wallet_transaction with existingTransactionId."

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf(
            "transactionId" to
                property(
                    "integer",
                    "Only for update_wallet_transaction and update_transfer: the id of the record to change.",
                ),
        )

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): UpdateWalletTransactionPayload {
        val id = arguments.getInt("transactionId")
        val existing = walletService.getWalletTransactionById(id)
        val before =
            TransactionSnapshot(
                existing.date.toLocalDate(),
                existing.description.orEmpty(),
                existing.amount,
                existing.type,
                existing.wallet.name,
                existing.category.name,
            )

        val walletName = arguments.optString("walletName")
        val newCategory = arguments.optString("categoryName")
        return UpdateWalletTransactionPayload(
            transactionId = id,
            date = if (arguments.has("date")) arguments.requireDate("date") else before.date,
            description = if (arguments.has("description")) arguments.getString("description") else before.description,
            amount = if (arguments.has("amount")) arguments.getBigDecimal("amount").abs() else before.amount,
            movementType =
                when (arguments.optString("movementType").lowercase()) {
                    "income" -> WalletTransactionType.INCOME
                    "expense" -> WalletTransactionType.EXPENSE
                    else -> before.movementType
                },
            walletId = if (walletName.isBlank()) existing.wallet.id else catalog.wallet(walletName)?.id,
            categoryName =
                if (newCategory.isBlank()) before.categoryName else catalog.category(newCategory)?.name ?: newCategory,
            before = before,
        )
    }

    override fun validate(payload: UpdateWalletTransactionPayload): ValidationResult =
        when {
            payload.walletId == null -> ValidationResult.error("wallet not found")
            payload.amount.signum() <= 0 -> ValidationResult.error("amount must be positive")
            payload.categoryName.isBlank() -> ValidationResult.error("categoryName is required")
            else -> ValidationResult.OK
        }

    override fun categoryNeeded(payload: UpdateWalletTransactionPayload) =
        payload.categoryName.takeIf { it.isNotBlank() && !it.equals(payload.before?.categoryName, ignoreCase = true) }

    override fun summary(payload: UpdateWalletTransactionPayload) =
        "#${payload.transactionId}: ${payload.description}, ${payload.amount.toPlainString()}, ${payload.date}"

    override fun preview(payload: UpdateWalletTransactionPayload): Preview? {
        val before = payload.before ?: return null
        val lines =
            listOfNotNull(
                change("date", before.date, payload.date),
                change("description", before.description, payload.description),
                change("amount", before.amount.toPlainString(), payload.amount.toPlainString()),
                change("type", before.movementType.name.lowercase(), payload.movementType.name.lowercase()),
                change("category", before.categoryName, payload.categoryName),
            )
        return Preview(lines)
    }

    private fun change(
        field: String,
        old: Any,
        new: Any,
    ) = if (old == new) null else "$field: $old → $new"

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

    override fun toDraft(payload: UpdateWalletTransactionPayload) =
        DraftValues(
            name,
            mapOf(
                "transactionId" to payload.transactionId,
                "date" to payload.date,
                "description" to payload.description,
                "amount" to payload.amount,
                "movementType" to payload.movementType.name.lowercase(),
                "walletId" to payload.walletId,
                "categoryName" to payload.categoryName,
            ),
        )

    override fun fromDraft(draft: DraftValues): UpdateWalletTransactionPayload? {
        val fields = draft.fields
        return UpdateWalletTransactionPayload(
            transactionId = fields["transactionId"].asInt() ?: return null,
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
        )
    }

    override fun execute(payload: UpdateWalletTransactionPayload): ApplyResult.Success {
        val existing = walletService.getWalletTransactionById(payload.transactionId)
        val updated =
            WalletTransaction(
                id = payload.transactionId,
                date = payload.date.atStartOfDay(),
                status = existing.status,
                description = payload.description,
                includeInAnalysis = existing.includeInAnalysis,
                wallet = walletService.getWalletById(payload.walletId!!),
                category =
                    existing.category.takeIf { it.name.equals(payload.categoryName, ignoreCase = true) }
                        ?: categoryResolver.resolveOrCreate(payload.categoryName),
                type = payload.movementType,
                amount = payload.amount,
            )
        walletService.updateWalletTransaction(updated)
        return ApplyResult.Success(listOf(payload.transactionId), summary(payload))
    }
}
