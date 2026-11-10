/*
 * Filename: WalletOperations.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation.transactions

import org.json.JSONObject
import org.moinex.ai.operation.ApplyResult
import org.moinex.ai.operation.BaseOperation
import org.moinex.ai.operation.CardInfo
import org.moinex.ai.operation.DraftChoices
import org.moinex.ai.operation.DraftField
import org.moinex.ai.operation.DraftFieldType
import org.moinex.ai.operation.DraftValues
import org.moinex.ai.operation.EntityCatalog
import org.moinex.ai.operation.OperationDomain
import org.moinex.ai.operation.OperationRisk
import org.moinex.ai.operation.Preview
import org.moinex.ai.operation.ValidationResult
import org.moinex.ai.operation.cardMoney
import org.moinex.ai.operation.cardText
import org.moinex.model.wallettransaction.Wallet
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Component
import java.math.BigDecimal

private const val WALLET_NAME_MAX_LENGTH = 50

data class CreateWalletPayload(
    val name: String,
    val typeName: String,
    val initialBalance: BigDecimal,
)

/** [walletId] is null while the model's wallet name matched no wallet; a blank [typeName] keeps the current type. */
data class UpdateWalletPayload(
    val walletId: Int?,
    val currentName: String,
    val name: String,
    val typeName: String,
)

data class WalletArchivePayload(
    val walletId: Int?,
    val currentName: String,
)

private fun WalletService.walletTypeByName(name: String) =
    getAllWalletTypes().firstOrNull { it.name.equals(name, ignoreCase = true) }

@Component
class CreateWalletOperation(
    private val walletService: WalletService,
) : BaseOperation<CreateWalletPayload>() {
    override val name = "create_wallet"
    override val domain = OperationDomain.MAINTENANCE
    override val risk = OperationRisk.LOW

    override fun modelRequired() = listOf("name", "walletType")

    override fun modelDescription() =
        "create_wallet: a new wallet (name, walletType from the existing wallet types, amount as the initial " +
            "balance, 0 when not given)."

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf("walletType" to property("string", "For create_wallet and update_wallet: an existing wallet type."))

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ) = CreateWalletPayload(
        arguments.getString("name").trim(),
        arguments.optString("walletType").let { walletService.walletTypeByName(it)?.name ?: it },
        if (arguments.has("amount")) arguments.getBigDecimal("amount") else BigDecimal.ZERO,
    )

    override fun validate(payload: CreateWalletPayload): ValidationResult =
        when {
            payload.name.isBlank() -> ValidationResult.error("name is required")
            payload.name.length > WALLET_NAME_MAX_LENGTH -> ValidationResult.error("name is too long")
            walletService.walletTypeByName(payload.typeName) == null ->
                ValidationResult.error("wallet type '${payload.typeName}' not found")
            walletService.existsByName(
                payload.name,
            ) -> ValidationResult.error("wallet '${payload.name}' already exists")
            else -> ValidationResult.OK
        }

    override fun card(payload: CreateWalletPayload) =
        CardInfo(payload.name, listOf(cardText(payload.typeName), cardMoney(payload.initialBalance)))

    override fun summary(payload: CreateWalletPayload) =
        "${payload.name}, ${payload.typeName}, ${payload.initialBalance.toPlainString()}"

    override fun draftSpec() =
        listOf(
            DraftField("name", "aidraft.field.name", DraftFieldType.TEXT),
            DraftField("typeName", "aidraft.field.walletType", DraftFieldType.CHOICE, DraftChoices.WALLET_TYPES),
            DraftField("initialBalance", "aidraft.field.initialBalance", DraftFieldType.DECIMAL),
        )

    override fun toDraft(payload: CreateWalletPayload) =
        DraftValues(
            name,
            mapOf(
                "name" to payload.name,
                "typeName" to payload.typeName,
                "initialBalance" to payload.initialBalance,
            ),
        )

    override fun fromDraft(draft: DraftValues): CreateWalletPayload? {
        val fields = draft.fields
        return CreateWalletPayload(
            (fields["name"] as? String)?.trim() ?: return null,
            fields["typeName"] as? String ?: return null,
            fields["initialBalance"].asDecimal() ?: return null,
        )
    }

    override fun execute(payload: CreateWalletPayload): ApplyResult.Success {
        val type = walletService.walletTypeByName(payload.typeName)!!
        val id = walletService.createWallet(Wallet(type = type, name = payload.name, balance = payload.initialBalance))
        return ApplyResult.Success(listOf(id), payload.name)
    }
}

@Component
class UpdateWalletOperation(
    private val walletService: WalletService,
) : BaseOperation<UpdateWalletPayload>() {
    override val name = "update_wallet"
    override val domain = OperationDomain.MAINTENANCE
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("walletName")

    override fun modelDescription() =
        "update_wallet: rename a wallet (walletName is the current name, name the new one) and/or change its " +
            "walletType. It never changes the balance."

    override fun modelProperties(): Map<String, JSONObject> = emptyMap()

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): UpdateWalletPayload {
        val current = arguments.getString("walletName")
        val wallet = catalog.wallet(current)
        return UpdateWalletPayload(
            wallet?.id,
            wallet?.name ?: current,
            arguments.optString("name").trim().ifBlank { wallet?.name ?: current },
            arguments.optString("walletType").let { walletService.walletTypeByName(it)?.name ?: it },
        )
    }

    override fun validate(payload: UpdateWalletPayload): ValidationResult =
        when {
            payload.walletId == null -> ValidationResult.error("wallet '${payload.currentName}' not found")
            payload.name.isBlank() -> ValidationResult.error("name is required")
            payload.name.length > WALLET_NAME_MAX_LENGTH -> ValidationResult.error("name is too long")
            payload.typeName.isNotBlank() && walletService.walletTypeByName(payload.typeName) == null ->
                ValidationResult.error("wallet type '${payload.typeName}' not found")
            else -> ValidationResult.OK
        }

    override fun summary(payload: UpdateWalletPayload) =
        buildString {
            append(payload.currentName)
            if (payload.name != payload.currentName) append(" → ").append(payload.name)
            if (payload.typeName.isNotBlank()) append(", ").append(payload.typeName)
        }

    override fun preview(payload: UpdateWalletPayload) = Preview(listOf(summary(payload)))

    override fun draftSpec() =
        listOf(
            DraftField("name", "aidraft.field.name", DraftFieldType.TEXT),
            DraftField(
                "typeName",
                "aidraft.field.walletType",
                DraftFieldType.CHOICE,
                DraftChoices.WALLET_TYPES,
                required = false,
            ),
        )

    override fun toDraft(payload: UpdateWalletPayload) =
        DraftValues(
            name,
            mapOf(
                "walletId" to payload.walletId,
                "currentName" to payload.currentName,
                "name" to payload.name,
                "typeName" to payload.typeName,
            ),
        )

    override fun fromDraft(draft: DraftValues): UpdateWalletPayload? {
        val fields = draft.fields
        return UpdateWalletPayload(
            fields["walletId"].asInt(),
            fields["currentName"] as? String ?: "",
            (fields["name"] as? String)?.trim() ?: return null,
            fields["typeName"] as? String ?: "",
        )
    }

    override fun execute(payload: UpdateWalletPayload): ApplyResult.Success {
        val wallet = walletService.getWalletById(payload.walletId!!)
        if (payload.name != wallet.name) {
            walletService.renameWallet(
                Wallet(id = wallet.id, type = wallet.type, name = payload.name, balance = wallet.balance),
            )
        }
        walletService.walletTypeByName(payload.typeName)?.takeIf { it.id != wallet.type.id }?.let { type ->
            walletService.changeWalletType(
                Wallet(id = wallet.id, type = type, name = payload.name, balance = wallet.balance),
            )
        }
        return ApplyResult.Success(listOf(wallet.id!!), summary(payload))
    }
}

/** Archive and unarchive differ only in the list the wallet is found in and the service call. */
abstract class WalletArchiveOperation(
    protected val walletService: WalletService,
) : BaseOperation<WalletArchivePayload>() {
    override val domain = OperationDomain.MAINTENANCE
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("walletName")

    override fun modelProperties(): Map<String, JSONObject> = emptyMap()

    protected abstract fun candidates(catalog: EntityCatalog): List<Wallet>

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): WalletArchivePayload {
        val current = arguments.getString("walletName")
        val wallet = candidates(catalog).firstOrNull { it.name.equals(current.trim(), ignoreCase = true) }
        return WalletArchivePayload(wallet?.id, wallet?.name ?: current)
    }

    override fun validate(payload: WalletArchivePayload): ValidationResult =
        if (payload.walletId == null) {
            ValidationResult.error("wallet '${payload.currentName}' not found")
        } else {
            ValidationResult.OK
        }

    override fun summary(payload: WalletArchivePayload) = payload.currentName
}

@Component
class ArchiveWalletOperation(
    walletService: WalletService,
) : WalletArchiveOperation(walletService) {
    override val name = "archive_wallet"

    override fun modelDescription() = "archive_wallet: archive an existing wallet (walletName); nothing is deleted."

    override fun candidates(catalog: EntityCatalog) = catalog.wallets

    override fun execute(payload: WalletArchivePayload): ApplyResult.Success {
        walletService.archiveWallet(payload.walletId!!)
        return ApplyResult.Success(listOf(payload.walletId), payload.currentName)
    }
}

@Component
class UnarchiveWalletOperation(
    walletService: WalletService,
) : WalletArchiveOperation(walletService) {
    override val name = "unarchive_wallet"

    override fun modelDescription() = "unarchive_wallet: bring back an archived wallet (walletName)."

    override fun candidates(catalog: EntityCatalog) = walletService.getAllArchivedWallets()

    override fun execute(payload: WalletArchivePayload): ApplyResult.Success {
        walletService.unarchiveWallet(payload.walletId!!)
        return ApplyResult.Success(listOf(payload.walletId), payload.currentName)
    }
}
