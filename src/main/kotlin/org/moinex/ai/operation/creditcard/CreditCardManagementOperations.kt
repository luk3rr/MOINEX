/*
 * Filename: CreditCardManagementOperations.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation.creditcard

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
import org.moinex.ai.operation.cardDay
import org.moinex.ai.operation.cardMoney
import org.moinex.ai.operation.cardText
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.CreditCardCredit
import org.moinex.model.enums.CreditCardCreditType
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

private const val CARD_NAME_MAX_LENGTH = 50
private const val MAX_DAY = 31

/**
 * A credit card to create or, when [cardId] is set, to change. [operatorName] is matched against the app's card
 * operators; a null [billingWalletId] means no default billing wallet.
 */
data class CreditCardPayload(
    val cardId: Int?,
    val name: String,
    val operatorName: String,
    val closingDay: Int,
    val dueDay: Int,
    val maxDebt: BigDecimal,
    val billingWalletId: Int?,
    val lastFourDigits: String,
    val before: String? = null,
)

data class CreditCardArchivePayload(
    val cardId: Int?,
    val currentName: String,
)

/** [cardId] is null while the model's card name matched no card. */
data class CreditCardCreditPayload(
    val cardId: Int?,
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val type: CreditCardCreditType,
)

private val CARD_DRAFT_SPEC =
    listOf(
        DraftField("name", "aidraft.field.name", DraftFieldType.TEXT),
        DraftField("operatorName", "aidraft.field.operator", DraftFieldType.TEXT),
        DraftField("closingDay", "aidraft.field.closingDay", DraftFieldType.INTEGER),
        DraftField("dueDay", "aidraft.field.dueDay", DraftFieldType.INTEGER),
        DraftField("maxDebt", "aidraft.field.maxDebt", DraftFieldType.DECIMAL),
        DraftField("billingWalletId", "aidraft.field.wallet", DraftFieldType.CHOICE, DraftChoices.WALLETS, false),
        DraftField("lastFourDigits", "aidraft.field.lastFour", DraftFieldType.TEXT, required = false),
    )

/** Shared by the card creation and change operations. */
abstract class CreditCardManagementOperation(
    protected val creditCardService: CreditCardService,
) : BaseOperation<CreditCardPayload>() {
    override val domain = OperationDomain.CREDIT_CARD

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf(
            "operator" to property("string", "For create_credit_card/update_credit_card: the card operator/brand."),
            "closingDay" to property("integer", "Invoice closing day of the month (1-31)."),
            "dueDay" to property("integer", "Invoice due day of the month (1-31)."),
            "maxDebt" to property("number", "The card limit."),
            "billingWalletName" to property("string", "Optional default wallet that pays the invoice."),
            "lastFourDigits" to property("string", "Optional last four digits of the card."),
        )

    override fun validate(payload: CreditCardPayload): ValidationResult =
        when {
            payload.name.isBlank() -> ValidationResult.error("name is required")
            payload.name.length > CARD_NAME_MAX_LENGTH -> ValidationResult.error("name is too long")
            payload.closingDay !in 1..MAX_DAY || payload.dueDay !in 1..MAX_DAY ->
                ValidationResult.error("closing and due days must be between 1 and $MAX_DAY")
            payload.maxDebt.signum() <= 0 -> ValidationResult.error("maxDebt must be positive")
            operatorOf(payload) == null -> ValidationResult.error("operator '${payload.operatorName}' not found")
            payload.lastFourDigits.isNotBlank() && !payload.lastFourDigits.matches(Regex("\\d{4}")) ->
                ValidationResult.error("lastFourDigits must have four digits")
            else -> ValidationResult.OK
        }

    protected fun operatorOf(payload: CreditCardPayload) =
        creditCardService.getAllCreditCardOperatorsOrderedByName().firstOrNull {
            it.name.equals(payload.operatorName, ignoreCase = true)
        }

    override fun card(payload: CreditCardPayload) =
        CardInfo(
            payload.name,
            listOf(
                cardText(payload.operatorName),
                cardMoney(payload.maxDebt),
                cardText("${payload.closingDay}/${payload.dueDay}"),
            ),
        )

    override fun summary(payload: CreditCardPayload) =
        "${payload.name}, ${payload.operatorName}, ${payload.maxDebt.toPlainString()}, " +
            "${payload.closingDay}/${payload.dueDay}"

    override fun draftSpec() = CARD_DRAFT_SPEC

    override fun toDraft(payload: CreditCardPayload) =
        DraftValues(
            name,
            mapOf(
                "cardId" to payload.cardId,
                "name" to payload.name,
                "operatorName" to payload.operatorName,
                "closingDay" to payload.closingDay,
                "dueDay" to payload.dueDay,
                "maxDebt" to payload.maxDebt,
                "billingWalletId" to payload.billingWalletId,
                "lastFourDigits" to payload.lastFourDigits,
            ),
        )

    override fun fromDraft(draft: DraftValues): CreditCardPayload? {
        val fields = draft.fields
        return CreditCardPayload(
            cardId = fields["cardId"].asInt(),
            name = (fields["name"] as? String)?.trim() ?: return null,
            operatorName = fields["operatorName"] as? String ?: return null,
            closingDay = fields["closingDay"].asInt() ?: return null,
            dueDay = fields["dueDay"].asInt() ?: return null,
            maxDebt = fields["maxDebt"].asDecimal() ?: return null,
            billingWalletId = fields["billingWalletId"].asInt(),
            lastFourDigits = fields["lastFourDigits"] as? String ?: "",
        )
    }
}

@Component
class CreateCreditCardOperation(
    creditCardService: CreditCardService,
    private val walletService: WalletService,
) : CreditCardManagementOperation(creditCardService) {
    override val name = "create_credit_card"
    override val risk = OperationRisk.LOW

    override fun modelRequired() = listOf("name", "operator", "closingDay", "dueDay", "maxDebt")

    override fun modelDescription() =
        "create_credit_card: a new credit card (name, operator, closingDay, dueDay, maxDebt, optional " +
            "billingWalletName and lastFourDigits)."

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ) = CreditCardPayload(
        cardId = null,
        name = arguments.getString("name").trim(),
        operatorName =
            arguments.getString("operator").let { given ->
                creditCardService
                    .getAllCreditCardOperatorsOrderedByName()
                    .firstOrNull { it.name.equals(given, ignoreCase = true) }
                    ?.name ?: given
            },
        closingDay = arguments.getInt("closingDay"),
        dueDay = arguments.getInt("dueDay"),
        maxDebt = arguments.getBigDecimal("maxDebt"),
        billingWalletId = catalog.wallet(arguments.optString("billingWalletName"))?.id,
        lastFourDigits = arguments.optString("lastFourDigits"),
    )

    override fun execute(payload: CreditCardPayload): ApplyResult.Success {
        val id =
            creditCardService.createCreditCard(
                CreditCard(
                    operator = operatorOf(payload)!!,
                    defaultBillingWallet = payload.billingWalletId?.let { walletService.getWalletById(it) },
                    name = payload.name,
                    billingDueDay = payload.dueDay,
                    closingDay = payload.closingDay,
                    maxDebt = payload.maxDebt,
                    lastFourDigits = payload.lastFourDigits.takeIf { it.isNotBlank() },
                ),
            )
        return ApplyResult.Success(listOf(id), summary(payload))
    }
}

@Component
class UpdateCreditCardOperation(
    creditCardService: CreditCardService,
    private val walletService: WalletService,
) : CreditCardManagementOperation(creditCardService) {
    override val name = "update_credit_card"
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("creditCardName")

    override fun modelDescription() =
        "update_credit_card: change an existing card (creditCardName is the current name, name the new one). " +
            "Send only the fields to change."

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): CreditCardPayload {
        val current = arguments.getString("creditCardName")
        val card = catalog.creditCard(current) ?: error("credit card '$current' not found")
        val stored =
            CreditCardPayload(
                card.id,
                card.name,
                card.operator.name,
                card.closingDay,
                card.billingDueDay,
                card.maxDebt,
                card.defaultBillingWallet?.id,
                card.lastFourDigits.orEmpty(),
            )
        val billing = arguments.optString("billingWalletName")
        return stored.copy(
            name = arguments.optString("name").trim().ifBlank { stored.name },
            operatorName =
                arguments.optString("operator").takeIf { it.isNotBlank() }?.let { given ->
                    creditCardService
                        .getAllCreditCardOperatorsOrderedByName()
                        .firstOrNull { it.name.equals(given, ignoreCase = true) }
                        ?.name ?: given
                } ?: stored.operatorName,
            closingDay = arguments.optIntOrNull("closingDay") ?: stored.closingDay,
            dueDay = arguments.optIntOrNull("dueDay") ?: stored.dueDay,
            maxDebt = if (arguments.has("maxDebt")) arguments.getBigDecimal("maxDebt") else stored.maxDebt,
            billingWalletId = if (billing.isBlank()) stored.billingWalletId else catalog.wallet(billing)?.id,
            lastFourDigits = arguments.optString("lastFourDigits").ifBlank { stored.lastFourDigits },
            before = summary(stored),
        )
    }

    override fun preview(payload: CreditCardPayload) =
        payload.before?.let { Preview(listOf(it, "→ " + summary(payload))) }

    override fun execute(payload: CreditCardPayload): ApplyResult.Success {
        creditCardService.updateCreditCard(
            CreditCard(
                id = payload.cardId,
                operator = operatorOf(payload)!!,
                defaultBillingWallet = payload.billingWalletId?.let { walletService.getWalletById(it) },
                name = payload.name,
                billingDueDay = payload.dueDay,
                closingDay = payload.closingDay,
                maxDebt = payload.maxDebt,
                lastFourDigits = payload.lastFourDigits.takeIf { it.isNotBlank() },
            ),
        )
        return ApplyResult.Success(listOf(payload.cardId!!), summary(payload))
    }
}

/** Archive and unarchive differ only in the list the card is found in and the service call. */
abstract class CreditCardArchiveOperation(
    protected val creditCardService: CreditCardService,
) : BaseOperation<CreditCardArchivePayload>() {
    override val domain = OperationDomain.CREDIT_CARD
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("creditCardName")

    override fun modelProperties(): Map<String, JSONObject> = emptyMap()

    protected abstract fun candidates(catalog: EntityCatalog): List<CreditCard>

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): CreditCardArchivePayload {
        val current = arguments.getString("creditCardName")
        val card = candidates(catalog).firstOrNull { it.name.equals(current.trim(), ignoreCase = true) }
        return CreditCardArchivePayload(card?.id, card?.name ?: current)
    }

    override fun validate(payload: CreditCardArchivePayload): ValidationResult =
        if (payload.cardId == null) {
            ValidationResult.error("credit card '${payload.currentName}' not found")
        } else {
            ValidationResult.OK
        }

    override fun summary(payload: CreditCardArchivePayload) = payload.currentName
}

@Component
class ArchiveCreditCardOperation(
    creditCardService: CreditCardService,
) : CreditCardArchiveOperation(creditCardService) {
    override val name = "archive_credit_card"

    override fun modelDescription() =
        "archive_credit_card: archive a card (creditCardName); the app refuses it while the card has pending payments."

    override fun candidates(catalog: EntityCatalog) = catalog.creditCards

    override fun execute(payload: CreditCardArchivePayload): ApplyResult.Success {
        creditCardService.archiveCreditCard(payload.cardId!!)
        return ApplyResult.Success(listOf(payload.cardId), payload.currentName)
    }
}

@Component
class UnarchiveCreditCardOperation(
    creditCardService: CreditCardService,
) : CreditCardArchiveOperation(creditCardService) {
    override val name = "unarchive_credit_card"

    override fun modelDescription() = "unarchive_credit_card: bring back an archived card (creditCardName)."

    override fun candidates(catalog: EntityCatalog) = creditCardService.getAllArchivedCreditCards()

    override fun execute(payload: CreditCardArchivePayload): ApplyResult.Success {
        creditCardService.unarchiveCreditCard(payload.cardId!!)
        return ApplyResult.Success(listOf(payload.cardId), payload.currentName)
    }
}

@Component
class AddCreditCardCreditOperation(
    private val creditCardService: CreditCardService,
) : BaseOperation<CreditCardCreditPayload>() {
    override val name = "add_credit_card_credit"
    override val domain = OperationDomain.CREDIT_CARD
    override val risk = OperationRisk.LOW

    override fun modelRequired() = listOf("creditCardName", "date", "amount", "description")

    override fun modelDescription() =
        "add_credit_card_credit: a credit on a card (creditType cashback, refund or reward) that raises the " +
            "card's available rebate (creditCardName, date, amount)."

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf(
            "creditType" to
                property("string", "Only for add_credit_card_credit.")
                    .put("enum", org.json.JSONArray(CreditCardCreditType.entries.map { it.name.lowercase() })),
        )

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ) = CreditCardCreditPayload(
        cardId = catalog.creditCard(arguments.optString("creditCardName"))?.id,
        date = arguments.requireDate("date"),
        description = arguments.optString("description"),
        amount = arguments.getBigDecimal("amount").abs(),
        type =
            CreditCardCreditType.entries.firstOrNull { it.name.equals(arguments.optString("creditType"), true) }
                ?: CreditCardCreditType.CASHBACK,
    )

    override fun validate(payload: CreditCardCreditPayload): ValidationResult =
        when {
            payload.cardId == null -> ValidationResult.error("credit card not found")
            payload.amount.signum() <= 0 -> ValidationResult.error("amount must be positive")
            else -> ValidationResult.OK
        }

    override fun missingFromModel(payload: CreditCardCreditPayload) =
        (if (payload.amount.signum() <= 0) listOf("amount must be greater than 0") else emptyList()) +
            missingDescription(payload.description)

    override fun card(payload: CreditCardCreditPayload) =
        CardInfo(
            payload.description.ifBlank { payload.type.name.lowercase() },
            listOf(cardMoney(payload.amount), cardDay(payload.date)),
        )

    override fun summary(payload: CreditCardCreditPayload) =
        "${payload.type.name.lowercase()}, ${payload.amount.toPlainString()}, ${payload.date}"

    override fun draftSpec() =
        listOf(
            DraftField("date", "aidraft.field.date", DraftFieldType.DATE),
            DraftField("description", "aidraft.field.description", DraftFieldType.TEXT, required = false),
            DraftField("amount", "aidraft.field.amount", DraftFieldType.DECIMAL),
            DraftField("cardId", "aidraft.field.creditCard", DraftFieldType.CHOICE, DraftChoices.CREDIT_CARDS),
        )

    override fun toDraft(payload: CreditCardCreditPayload) =
        DraftValues(
            name,
            mapOf(
                "date" to payload.date,
                "description" to payload.description,
                "amount" to payload.amount,
                "cardId" to payload.cardId,
                "type" to payload.type.name,
            ),
        )

    override fun fromDraft(draft: DraftValues): CreditCardCreditPayload? {
        val fields = draft.fields
        return CreditCardCreditPayload(
            cardId = fields["cardId"].asInt(),
            date = fields["date"].asDate() ?: return null,
            description = fields["description"] as? String ?: "",
            amount = fields["amount"].asDecimal()?.abs() ?: return null,
            type =
                CreditCardCreditType.entries.firstOrNull { it.name == fields["type"] }
                    ?: CreditCardCreditType.CASHBACK,
        )
    }

    override fun execute(payload: CreditCardCreditPayload): ApplyResult.Success {
        val card = creditCardService.getAllNonArchivedCreditCardsOrderedByName().first { it.id == payload.cardId }
        creditCardService.addRebate(
            CreditCardCredit(
                type = payload.type,
                creditCard = card,
                date = payload.date.atStartOfDay(),
                amount = payload.amount,
                description = payload.description.takeIf { it.isNotBlank() },
            ),
        )
        return ApplyResult.Success(listOf(payload.cardId!!), summary(payload))
    }
}
