/*
 * Filename: TransferOperations.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation.transactions

import org.json.JSONObject
import org.moinex.ai.operation.ApplyResult
import org.moinex.ai.operation.BaseOperation
import org.moinex.ai.operation.CardInfo
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
import org.moinex.ai.operation.cardDay
import org.moinex.ai.operation.cardMoney
import org.moinex.model.Category
import org.moinex.model.wallettransaction.Transfer
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/**
 * [transferId] is set only when an existing transfer is being changed. A wallet id is null while the model's wallet
 * name matched no wallet; the user fixes it in review. [categoryName] is blank for a transfer with no category.
 */
data class TransferPayload(
    val transferId: Int?,
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val senderWalletId: Int?,
    val receiverWalletId: Int?,
    val categoryName: String,
    val before: String? = null,
    val keptCategory: String? = null,
)

private fun transferProperties(): Map<String, JSONObject> =
    mapOf(
        "fromWalletName" to
            JSONObject()
                .put("type", "string")
                .put("description", "For transfers: the existing wallet the money leaves."),
        "toWalletName" to
            JSONObject()
                .put("type", "string")
                .put("description", "For transfers: the existing wallet the money arrives in."),
    )

private fun validateTransfer(payload: TransferPayload): ValidationResult =
    when {
        payload.senderWalletId == null -> ValidationResult.error("origin wallet not found")
        payload.receiverWalletId == null -> ValidationResult.error("destination wallet not found")
        payload.senderWalletId == payload.receiverWalletId ->
            ValidationResult.error("origin and destination must be different wallets")
        payload.amount.signum() <= 0 -> ValidationResult.error("amount must be positive")
        else -> ValidationResult.OK
    }

private val TRANSFER_DRAFT_SPEC =
    listOf(
        DraftField("date", "aidraft.field.date", DraftFieldType.DATE),
        DraftField("description", "aidraft.field.description", DraftFieldType.TEXT, required = false),
        DraftField("amount", "aidraft.field.amount", DraftFieldType.DECIMAL),
        DraftField("senderWalletId", "aidraft.field.fromWallet", DraftFieldType.CHOICE, DraftChoices.WALLETS),
        DraftField("receiverWalletId", "aidraft.field.toWallet", DraftFieldType.CHOICE, DraftChoices.WALLETS),
        DraftField("categoryName", "aidraft.field.category", DraftFieldType.CHOICE, DraftChoices.CATEGORIES, false),
    )

/** Shared by the two transfer operations, which differ only in how the payload starts and how it is written. */
abstract class TransferOperation(
    protected val walletService: WalletService,
    private val categoryResolver: CategoryResolver,
) : BaseOperation<TransferPayload>() {
    override val domain = OperationDomain.TRANSACTIONS

    override fun modelProperties(): Map<String, JSONObject> = transferProperties()

    override fun validate(payload: TransferPayload) = validateTransfer(payload)

    override fun categoryNeeded(payload: TransferPayload) =
        payload.categoryName.takeIf { it.isNotBlank() && !it.equals(payload.keptCategory, ignoreCase = true) }

    override fun card(payload: TransferPayload): CardInfo {
        val sender = payload.senderWalletId?.let { runCatching { walletService.getWalletById(it).name }.getOrNull() }
        val receiver =
            payload.receiverWalletId?.let { runCatching { walletService.getWalletById(it).name }.getOrNull() }
        return CardInfo(
            "${sender ?: "?"} → ${receiver ?: "?"}",
            listOf(cardMoney(payload.amount), cardDay(payload.date)),
        )
    }

    override fun summary(payload: TransferPayload): String {
        val sender = payload.senderWalletId?.let { runCatching { walletService.getWalletById(it).name }.getOrNull() }
        val receiver =
            payload.receiverWalletId?.let { runCatching { walletService.getWalletById(it).name }.getOrNull() }
        return "${sender ?: "?"} → ${receiver ?: "?"}, ${payload.amount.toPlainString()}, ${payload.date}"
    }

    override fun draftSpec() = TRANSFER_DRAFT_SPEC

    override fun toDraft(payload: TransferPayload) =
        DraftValues(
            name,
            mapOf(
                "transferId" to payload.transferId,
                "date" to payload.date,
                "description" to payload.description,
                "amount" to payload.amount,
                "senderWalletId" to payload.senderWalletId,
                "receiverWalletId" to payload.receiverWalletId,
                "categoryName" to payload.categoryName,
            ),
        )

    override fun fromDraft(draft: DraftValues): TransferPayload? {
        val fields = draft.fields
        return TransferPayload(
            transferId = fields["transferId"].asInt(),
            date = fields["date"].asDate() ?: return null,
            description = fields["description"] as? String ?: "",
            amount = fields["amount"].asDecimal()?.abs() ?: return null,
            senderWalletId = fields["senderWalletId"].asInt(),
            receiverWalletId = fields["receiverWalletId"].asInt(),
            categoryName = fields["categoryName"] as? String ?: "",
        )
    }

    protected fun toTransfer(
        payload: TransferPayload,
        existingCategory: Category? = null,
    ) = Transfer(
        id = payload.transferId,
        senderWallet = walletService.getWalletById(payload.senderWalletId!!),
        receiverWallet = walletService.getWalletById(payload.receiverWalletId!!),
        date = payload.date.atStartOfDay(),
        amount = payload.amount,
        description = payload.description.takeIf { it.isNotBlank() },
        category =
            payload.categoryName.takeIf { it.isNotBlank() }?.let { name ->
                existingCategory?.takeIf { it.name.equals(name, ignoreCase = true) }
                    ?: categoryResolver.resolveOrCreate(name)
            },
    )
}

@Component
class CreateTransferOperation(
    walletService: WalletService,
    categoryResolver: CategoryResolver,
) : TransferOperation(walletService, categoryResolver) {
    override val name = "create_transfer"
    override val risk = OperationRisk.LOW

    override fun modelRequired() = listOf("date", "amount", "fromWalletName", "toWalletName", "description")

    override fun modelDescription() =
        "create_transfer: money moved between two of the user's own wallets (fromWalletName, toWalletName, " +
            "date, amount); not an income or expense."

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ) = TransferPayload(
        transferId = null,
        date = arguments.requireDate("date"),
        description = arguments.optString("description"),
        amount = arguments.getBigDecimal("amount").abs(),
        senderWalletId = catalog.wallet(arguments.optString("fromWalletName"))?.id,
        receiverWalletId = catalog.wallet(arguments.optString("toWalletName"))?.id,
        categoryName = arguments.optString("categoryName").let { catalog.category(it)?.name ?: it },
    )

    override fun missingFromModel(payload: TransferPayload) =
        (if (payload.amount.signum() <= 0) listOf("amount must be greater than 0") else emptyList()) +
            missingDescription(payload.description)

    override fun execute(payload: TransferPayload): ApplyResult.Success {
        val id = walletService.createTransfer(toTransfer(payload))
        return ApplyResult.Success(listOf(id), summary(payload))
    }
}

@Component
class UpdateTransferOperation(
    walletService: WalletService,
    categoryResolver: CategoryResolver,
) : TransferOperation(walletService, categoryResolver) {
    override val name = "update_transfer"
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("transactionId")

    override fun modelDescription() =
        "update_transfer: change an existing transfer (transactionId is the transfer id). Send only the fields " +
            "to change; the others keep their value."

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): TransferPayload {
        val id = arguments.getInt("transactionId")
        val existing =
            walletService.getAllTransfers().firstOrNull { it.id == id } ?: error("transfer $id not found")
        val before = summary(existing.toPayload())

        val from = arguments.optString("fromWalletName")
        val to = arguments.optString("toWalletName")
        val category = arguments.optString("categoryName")
        return TransferPayload(
            transferId = id,
            date = if (arguments.has("date")) arguments.requireDate("date") else existing.date.toLocalDate(),
            description =
                if (arguments.has(
                        "description",
                    )
                ) {
                    arguments.getString("description")
                } else {
                    existing.description.orEmpty()
                },
            amount = if (arguments.has("amount")) arguments.getBigDecimal("amount").abs() else existing.amount,
            senderWalletId = if (from.isBlank()) existing.senderWallet.id else catalog.wallet(from)?.id,
            receiverWalletId = if (to.isBlank()) existing.receiverWallet.id else catalog.wallet(to)?.id,
            categoryName =
                if (category.isBlank()) {
                    existing.category?.name.orEmpty()
                } else {
                    catalog.category(category)?.name
                        ?: category
                },
            before = before,
            keptCategory = existing.category?.name,
        )
    }

    override fun preview(payload: TransferPayload) =
        payload.before?.let { Preview(listOf(it, "→ " + summary(payload))) }

    override fun execute(payload: TransferPayload): ApplyResult.Success {
        val existing = walletService.getAllTransfers().first { it.id == payload.transferId }
        walletService.updateTransfer(toTransfer(payload, existing.category))
        return ApplyResult.Success(listOf(payload.transferId!!), summary(payload))
    }

    private fun Transfer.toPayload() =
        TransferPayload(id, date.toLocalDate(), description.orEmpty(), amount, senderWallet.id, receiverWallet.id, "")
}
