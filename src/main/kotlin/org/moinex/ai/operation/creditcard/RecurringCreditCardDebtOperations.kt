/*
 * Filename: RecurringCreditCardDebtOperations.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation.creditcard

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
import org.moinex.ai.operation.cardMoney
import org.moinex.ai.operation.cardTranslated
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isBeforeOrEqual
import org.moinex.model.creditcard.RecurringCreditCardDebt
import org.moinex.model.enums.CreditCardRecurringFrequency
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.creditcard.RecurringCreditCardDebtService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * A charge that repeats on a card. [recurringId] is set only when an existing one is being changed; then
 * [startDate] is the stored one and cannot change. A null [cardId] means the card name matched no card.
 */
data class RecurringCardDebtPayload(
    val recurringId: Int?,
    val cardId: Int?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val dayOfMonth: Int,
    val frequency: CreditCardRecurringFrequency,
    val description: String,
    val amount: BigDecimal,
    val categoryName: String,
    val before: String? = null,
    val keptCategory: String? = null,
)

private fun String?.toCardFrequency(default: CreditCardRecurringFrequency) =
    CreditCardRecurringFrequency.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: default

/** Shared by the two operations on recurring card charges. */
abstract class RecurringCardDebtOperation(
    protected val creditCardService: CreditCardService,
    protected val recurringService: RecurringCreditCardDebtService,
    protected val categoryResolver: CategoryResolver,
) : BaseOperation<RecurringCardDebtPayload>() {
    override val domain = OperationDomain.CREDIT_CARD

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf(
            "dayOfMonth" to property("integer", "For recurring card charges: the day of the month it is charged."),
            "cardFrequency" to
                property("string", "For recurring card charges: how often it repeats.")
                    .put("enum", JSONArray(CreditCardRecurringFrequency.entries.map { it.name.lowercase() })),
            "cardRecurringId" to property("integer", "Only for update_recurring_card_debt: the id to change."),
        )

    override fun validate(payload: RecurringCardDebtPayload): ValidationResult =
        when {
            payload.cardId == null -> ValidationResult.error("credit card not found")
            payload.amount.signum() <= 0 -> ValidationResult.error("amount must be positive")
            payload.categoryName.isBlank() -> ValidationResult.error("categoryName is required")
            payload.dayOfMonth !in 1..Constants.MAX_BILLING_DUE_DAY ->
                ValidationResult.error("dayOfMonth must be between 1 and ${Constants.MAX_BILLING_DUE_DAY}")
            !payload.startDate.plus(1, payload.frequency.chronoUnit).isBeforeOrEqual(payload.endDate) ->
                ValidationResult.error(
                    "endDate must be at least one ${payload.frequency.name.lowercase()} after the start",
                )
            else -> ValidationResult.OK
        }

    override fun categoryNeeded(payload: RecurringCardDebtPayload) =
        payload.categoryName.takeIf { it.isNotBlank() && !it.equals(payload.keptCategory, ignoreCase = true) }

    override fun card(payload: RecurringCardDebtPayload) =
        CardInfo(
            payload.description,
            listOf(
                cardMoney(payload.amount),
                cardTranslated("creditCard.recurring.frequency.${payload.frequency.name.lowercase()}"),
                cardTranslated(TranslationKeys.AIPANEL_FACT_DAY_OF_MONTH, payload.dayOfMonth),
            ),
        )

    override fun summary(payload: RecurringCardDebtPayload) =
        "${payload.description}, ${payload.amount.toPlainString()}, ${payload.frequency.name.lowercase()}, " +
            "day ${payload.dayOfMonth}"

    override fun draftSpec() =
        listOf(
            DraftField("startDate", "aidraft.field.date", DraftFieldType.DATE),
            DraftField("endDate", "aidraft.field.endDate", DraftFieldType.DATE),
            DraftField("dayOfMonth", "aidraft.field.dayOfMonth", DraftFieldType.INTEGER),
            DraftField("frequency", "aidraft.field.frequency", DraftFieldType.TEXT),
            DraftField("description", "aidraft.field.description", DraftFieldType.TEXT, required = false),
            DraftField("amount", "aidraft.field.amount", DraftFieldType.DECIMAL),
            DraftField("cardId", "aidraft.field.creditCard", DraftFieldType.CHOICE, DraftChoices.CREDIT_CARDS),
            DraftField("categoryName", "aidraft.field.category", DraftFieldType.CHOICE, DraftChoices.CATEGORIES),
        )

    override fun toDraft(payload: RecurringCardDebtPayload) =
        DraftValues(
            name,
            mapOf(
                "recurringId" to payload.recurringId,
                "startDate" to payload.startDate,
                "endDate" to payload.endDate,
                "dayOfMonth" to payload.dayOfMonth,
                "frequency" to payload.frequency.name.lowercase(),
                "description" to payload.description,
                "amount" to payload.amount,
                "cardId" to payload.cardId,
                "categoryName" to payload.categoryName,
            ),
        )

    override fun fromDraft(draft: DraftValues): RecurringCardDebtPayload? {
        val fields = draft.fields
        return RecurringCardDebtPayload(
            recurringId = fields["recurringId"].asInt(),
            cardId = fields["cardId"].asInt(),
            startDate = fields["startDate"].asDate() ?: return null,
            endDate = fields["endDate"].asDate() ?: Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE,
            dayOfMonth = fields["dayOfMonth"].asInt() ?: return null,
            frequency = (fields["frequency"] as? String).toCardFrequency(CreditCardRecurringFrequency.MONTHLY),
            description = fields["description"] as? String ?: "",
            amount = fields["amount"].asDecimal()?.abs() ?: return null,
            categoryName = fields["categoryName"] as? String ?: "",
        )
    }
}

@Component
class CreateRecurringCardDebtOperation(
    creditCardService: CreditCardService,
    recurringService: RecurringCreditCardDebtService,
    categoryResolver: CategoryResolver,
) : RecurringCardDebtOperation(creditCardService, recurringService, categoryResolver) {
    override val name = "create_recurring_card_debt"
    override val risk = OperationRisk.LOW

    override fun modelRequired() = listOf("creditCardName", "categoryName", "amount", "date", "description")

    override fun modelDescription() =
        "create_recurring_card_debt: a charge that repeats on a credit card (creditCardName, categoryName, amount, " +
            "dayOfMonth, cardFrequency monthly or yearly, date as the first charge, optional endDate)."

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): RecurringCardDebtPayload {
        val category = arguments.getString("categoryName")
        val start = arguments.requireDate("date")
        return RecurringCardDebtPayload(
            recurringId = null,
            cardId = catalog.creditCard(arguments.optString("creditCardName"))?.id,
            startDate = start,
            endDate =
                arguments.optString("endDate").takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
                    ?: Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE,
            dayOfMonth = arguments.optIntOrNull("dayOfMonth") ?: start.dayOfMonth,
            frequency = arguments.optString("cardFrequency").toCardFrequency(CreditCardRecurringFrequency.MONTHLY),
            description = arguments.optString("description"),
            amount = arguments.getBigDecimal("amount").abs(),
            categoryName = catalog.category(category)?.name ?: category,
        )
    }

    override fun missingFromModel(payload: RecurringCardDebtPayload) =
        (if (payload.amount.signum() <= 0) listOf("amount must be greater than 0") else emptyList()) +
            missingDescription(payload.description)

    override fun execute(payload: RecurringCardDebtPayload): ApplyResult.Success {
        val card = creditCardService.getAllNonArchivedCreditCardsOrderedByName().first { it.id == payload.cardId }
        val id =
            recurringService.createRecurring(
                RecurringCreditCardDebt(
                    creditCard = card,
                    category = categoryResolver.resolveOrCreate(payload.categoryName),
                    amount = payload.amount,
                    description = payload.description.takeIf { it.isNotBlank() },
                    dayOfMonth = payload.dayOfMonth,
                    frequency = payload.frequency,
                    startDate = payload.startDate,
                    endDate = payload.endDate,
                    nextInvoiceMonth = YearMonth.from(payload.startDate),
                ),
            )
        return ApplyResult.Success(listOf(id), summary(payload))
    }
}

@Component
class UpdateRecurringCardDebtOperation(
    creditCardService: CreditCardService,
    recurringService: RecurringCreditCardDebtService,
    categoryResolver: CategoryResolver,
) : RecurringCardDebtOperation(creditCardService, recurringService, categoryResolver) {
    override val name = "update_recurring_card_debt"
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("cardRecurringId")

    override fun modelDescription() =
        "update_recurring_card_debt: change a recurring card charge (cardRecurringId, from list_records kind " +
            "card_recurring). Send only the fields to change; its start date and status are kept."

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): RecurringCardDebtPayload {
        val id = arguments.getInt("cardRecurringId")
        val existing =
            recurringService.getAllRecurringDebts().firstOrNull { it.id == id }
                ?: error("recurring card charge $id not found")
        val stored =
            RecurringCardDebtPayload(
                id,
                existing.creditCard.id,
                existing.startDate,
                existing.endDate,
                existing.dayOfMonth,
                existing.frequency,
                existing.description.orEmpty(),
                existing.amount,
                existing.category.name,
            )
        val card = arguments.optString("creditCardName")
        val category = arguments.optString("categoryName")
        return stored.copy(
            cardId = if (card.isBlank()) stored.cardId else catalog.creditCard(card)?.id,
            endDate =
                arguments.optString("endDate").takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
                    ?: stored.endDate,
            dayOfMonth = arguments.optIntOrNull("dayOfMonth") ?: stored.dayOfMonth,
            frequency = arguments.optString("cardFrequency").toCardFrequency(stored.frequency),
            description = if (arguments.has("description")) arguments.getString("description") else stored.description,
            amount = if (arguments.has("amount")) arguments.getBigDecimal("amount").abs() else stored.amount,
            categoryName =
                if (category.isBlank()) stored.categoryName else catalog.category(category)?.name ?: category,
            before = summary(stored),
            keptCategory = stored.categoryName,
        )
    }

    override fun preview(payload: RecurringCardDebtPayload) =
        payload.before?.let { Preview(listOf(it, "→ " + summary(payload))) }

    override fun execute(payload: RecurringCardDebtPayload): ApplyResult.Success {
        val existing = recurringService.getAllRecurringDebts().first { it.id == payload.recurringId }
        val card = creditCardService.getAllNonArchivedCreditCardsOrderedByName().first { it.id == payload.cardId }
        recurringService.updateRecurring(
            RecurringCreditCardDebt(
                id = existing.id,
                creditCard = card,
                category =
                    existing.category.takeIf { it.name.equals(payload.categoryName, ignoreCase = true) }
                        ?: categoryResolver.resolveOrCreate(payload.categoryName),
                amount = payload.amount,
                description = payload.description.takeIf { it.isNotBlank() },
                dayOfMonth = payload.dayOfMonth,
                frequency = payload.frequency,
                status = existing.status,
                startDate = existing.startDate,
                endDate = payload.endDate,
                nextInvoiceMonth = existing.nextInvoiceMonth,
            ),
        )
        return ApplyResult.Success(listOf(payload.recurringId!!), summary(payload))
    }
}
