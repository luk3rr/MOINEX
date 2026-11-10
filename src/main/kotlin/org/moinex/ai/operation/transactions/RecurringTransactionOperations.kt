/*
 * Filename: RecurringTransactionOperations.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation.transactions

import org.json.JSONArray
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
import org.moinex.ai.operation.cardTranslated
import org.moinex.common.constant.Constants
import org.moinex.model.enums.RecurringTransactionFrequency
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.RecurringTransaction
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/**
 * [recurringId] is set only when an existing recurring transaction is being changed; then [startDate] is the stored
 * one and cannot change. A null [walletId] means the model's wallet name matched no wallet.
 */
data class RecurringTransactionPayload(
    val recurringId: Int?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val frequency: RecurringTransactionFrequency,
    val description: String,
    val amount: BigDecimal,
    val movementType: WalletTransactionType,
    val walletId: Int?,
    val categoryName: String,
    val before: String? = null,
    val keptCategory: String? = null,
)

private fun String?.toFrequency(default: RecurringTransactionFrequency) =
    RecurringTransactionFrequency.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: default

private fun String?.toMovementType(default: WalletTransactionType) =
    when (this?.lowercase()) {
        "income" -> WalletTransactionType.INCOME
        "expense" -> WalletTransactionType.EXPENSE
        else -> default
    }

/** Shared by the two recurring transaction operations. */
abstract class RecurringTransactionOperation(
    protected val walletService: WalletService,
    protected val recurringTransactionService: RecurringTransactionService,
    protected val categoryResolver: CategoryResolver,
) : BaseOperation<RecurringTransactionPayload>() {
    override val domain = OperationDomain.TRANSACTIONS

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf(
            "frequency" to
                property("string", "For recurring transactions: how often it repeats.")
                    .put("enum", JSONArray(RecurringTransactionFrequency.entries.map { it.name.lowercase() })),
            "endDate" to
                property("string", "For recurring transactions: optional last date (YYYY-MM-DD); open-ended if unset."),
            "recurringId" to property("integer", "Only for update_recurring_transaction: the id to change."),
        )

    override fun validate(payload: RecurringTransactionPayload): ValidationResult =
        when {
            payload.walletId == null -> ValidationResult.error("wallet not found")
            payload.amount.signum() <= 0 -> ValidationResult.error("amount must be positive")
            payload.categoryName.isBlank() -> ValidationResult.error("categoryName is required")
            !payload.startDate.isBefore(payload.endDate) -> ValidationResult.error("end date must be after the start")
            else -> ValidationResult.OK
        }

    override fun categoryNeeded(payload: RecurringTransactionPayload) =
        payload.categoryName.takeIf { it.isNotBlank() && !it.equals(payload.keptCategory, ignoreCase = true) }

    override fun card(payload: RecurringTransactionPayload) =
        CardInfo(
            payload.description,
            listOf(
                cardMoney(payload.amount),
                cardTranslated("recurringtransaction.frequency.${payload.frequency.name.lowercase()}"),
                cardDay(payload.startDate),
            ),
        )

    override fun summary(payload: RecurringTransactionPayload) =
        "${payload.description}, ${payload.amount.toPlainString()}, ${payload.frequency.name.lowercase()}, " +
            "${payload.startDate}"

    override fun draftSpec() =
        listOf(
            DraftField("startDate", "aidraft.field.date", DraftFieldType.DATE),
            DraftField("endDate", "aidraft.field.endDate", DraftFieldType.DATE),
            DraftField("frequency", "aidraft.field.frequency", DraftFieldType.CHOICE, DraftChoices.FREQUENCIES),
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

    override fun toDraft(payload: RecurringTransactionPayload) =
        DraftValues(
            name,
            mapOf(
                "recurringId" to payload.recurringId,
                "startDate" to payload.startDate,
                "endDate" to payload.endDate,
                "frequency" to payload.frequency.name.lowercase(),
                "description" to payload.description,
                "amount" to payload.amount,
                "movementType" to payload.movementType.name.lowercase(),
                "walletId" to payload.walletId,
                "categoryName" to payload.categoryName,
            ),
        )

    override fun fromDraft(draft: DraftValues): RecurringTransactionPayload? {
        val fields = draft.fields
        return RecurringTransactionPayload(
            recurringId = fields["recurringId"].asInt(),
            startDate = fields["startDate"].asDate() ?: return null,
            endDate = fields["endDate"].asDate() ?: Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE,
            frequency = (fields["frequency"] as? String).toFrequency(RecurringTransactionFrequency.MONTHLY),
            description = fields["description"] as? String ?: "",
            amount = fields["amount"].asDecimal()?.abs() ?: return null,
            movementType = (fields["movementType"] as? String).toMovementType(WalletTransactionType.EXPENSE),
            walletId = fields["walletId"].asInt(),
            categoryName = fields["categoryName"] as? String ?: "",
        )
    }

    protected fun payloadOf(
        existing: RecurringTransaction,
        categoryName: String = existing.category.name,
    ) = RecurringTransactionPayload(
        existing.id,
        existing.startDate,
        existing.endDate,
        existing.frequency,
        existing.description.orEmpty(),
        existing.amount,
        existing.type,
        existing.wallet.id,
        categoryName,
    )
}

@Component
class CreateRecurringTransactionOperation(
    walletService: WalletService,
    recurringTransactionService: RecurringTransactionService,
    categoryResolver: CategoryResolver,
) : RecurringTransactionOperation(walletService, recurringTransactionService, categoryResolver) {
    override val name = "create_recurring_transaction"
    override val risk = OperationRisk.LOW

    override fun modelRequired() =
        listOf("date", "amount", "walletName", "categoryName", "movementType", "frequency", "description")

    override fun modelDescription() =
        "create_recurring_transaction: an income or expense that repeats (date is the first occurrence, frequency, " +
            "optional endDate, movementType, walletName, categoryName)."

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): RecurringTransactionPayload {
        val category = arguments.getString("categoryName")
        return RecurringTransactionPayload(
            recurringId = null,
            startDate = arguments.requireDate("date"),
            endDate =
                arguments.optString("endDate").takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
                    ?: Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE,
            frequency = arguments.optString("frequency").toFrequency(RecurringTransactionFrequency.MONTHLY),
            description = arguments.optString("description"),
            amount = arguments.getBigDecimal("amount").abs(),
            movementType = arguments.optString("movementType").toMovementType(WalletTransactionType.EXPENSE),
            walletId = catalog.wallet(arguments.optString("walletName"))?.id,
            categoryName = catalog.category(category)?.name ?: category,
        )
    }

    override fun missingFromModel(payload: RecurringTransactionPayload) =
        (if (payload.amount.signum() <= 0) listOf("amount must be greater than 0") else emptyList()) +
            missingDescription(payload.description)

    override fun execute(payload: RecurringTransactionPayload): ApplyResult.Success {
        val id =
            recurringTransactionService.createRecurringTransaction(
                RecurringTransaction(
                    startDate = payload.startDate,
                    endDate = payload.endDate,
                    nextDueDate = payload.startDate,
                    frequency = payload.frequency,
                    description = payload.description,
                    wallet = walletService.getWalletById(payload.walletId!!),
                    category = categoryResolver.resolveOrCreate(payload.categoryName),
                    type = payload.movementType,
                    amount = payload.amount,
                ),
            )
        return ApplyResult.Success(listOf(id), summary(payload))
    }
}

@Component
class UpdateRecurringTransactionOperation(
    walletService: WalletService,
    recurringTransactionService: RecurringTransactionService,
    categoryResolver: CategoryResolver,
) : RecurringTransactionOperation(walletService, recurringTransactionService, categoryResolver) {
    override val name = "update_recurring_transaction"
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("recurringId")

    override fun modelDescription() =
        "update_recurring_transaction: change an existing recurring transaction (recurringId). Send only the " +
            "fields to change; the start date cannot change."

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): RecurringTransactionPayload {
        val id = arguments.getInt("recurringId")
        val existing =
            recurringTransactionService.getAllRecurringTransactions().firstOrNull { it.id == id }
                ?: error("recurring transaction $id not found")
        val current = payloadOf(existing)

        val wallet = arguments.optString("walletName")
        val category = arguments.optString("categoryName")
        return current.copy(
            endDate =
                arguments.optString("endDate").takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
                    ?: current.endDate,
            frequency = arguments.optString("frequency").toFrequency(current.frequency),
            description = if (arguments.has("description")) arguments.getString("description") else current.description,
            amount = if (arguments.has("amount")) arguments.getBigDecimal("amount").abs() else current.amount,
            movementType = arguments.optString("movementType").toMovementType(current.movementType),
            walletId = if (wallet.isBlank()) current.walletId else catalog.wallet(wallet)?.id,
            categoryName =
                if (category.isBlank()) current.categoryName else catalog.category(category)?.name ?: category,
            before = summary(current),
            keptCategory = existing.category.name,
        )
    }

    override fun preview(payload: RecurringTransactionPayload) =
        payload.before?.let { Preview(listOf(it, "→ " + summary(payload))) }

    override fun execute(payload: RecurringTransactionPayload): ApplyResult.Success {
        val existing =
            recurringTransactionService.getAllRecurringTransactions().first { it.id == payload.recurringId }
        recurringTransactionService.updateRecurringTransaction(
            RecurringTransaction(
                id = existing.id,
                startDate = existing.startDate,
                endDate = payload.endDate,
                nextDueDate = existing.nextDueDate,
                frequency = payload.frequency,
                status = existing.status,
                includeInNetWorth = existing.includeInNetWorth,
                description = payload.description,
                wallet = walletService.getWalletById(payload.walletId!!),
                category =
                    existing.category.takeIf { it.name.equals(payload.categoryName, ignoreCase = true) }
                        ?: categoryResolver.resolveOrCreate(payload.categoryName),
                type = payload.movementType,
                amount = payload.amount,
                includeInAnalysis = existing.includeInAnalysis,
            ),
        )
        return ApplyResult.Success(listOf(payload.recurringId!!), summary(payload))
    }
}
