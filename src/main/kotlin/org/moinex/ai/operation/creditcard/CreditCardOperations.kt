/*
 * Filename: CreditCardOperations.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation.creditcard

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
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.model.dto.CreditCardInvoicePaymentDTO
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.matching.MatchQuery
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/**
 * [amount] is the whole purchase (the value of one installment times [installments]). [installmentNumber] greater
 * than 1 means the line continues a purchase made earlier: it is posted from the card's current invoice on.
 * [creditCardId] is null while the model's card name matched no card.
 */
data class CreditCardDebtPayload(
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val installments: Int,
    val installmentNumber: Int,
    val creditCardId: Int?,
    val categoryName: String,
)

data class CreditCardInvoicePaymentPayload(
    val creditCardId: Int?,
    val invoiceMonth: YearMonth,
    val amount: BigDecimal,
)

private fun CreditCardService.findCard(id: Int): CreditCard =
    getAllNonArchivedCreditCardsOrderedByName().firstOrNull { it.id == id }
        ?: error("Credit card $id not found")

@Component
class CreateCreditCardDebtOperation(
    private val creditCardService: CreditCardService,
    private val categoryResolver: CategoryResolver,
    private val preferencesService: PreferencesService,
) : BaseOperation<CreditCardDebtPayload>() {
    override val name = "create_credit_card_debt"
    override val domain = OperationDomain.CREDIT_CARD
    override val risk = OperationRisk.LOW

    override fun modelRequired() = listOf("date", "amount", "creditCardName", "categoryName")

    override fun modelDescription() =
        "create_credit_card_debt: a purchase on a credit card (use creditCardName); amount is always the value of " +
            "ONE installment as printed, and the app multiplies it by the installments left."

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf(
            "installments" to
                property(
                    "integer",
                    "Only for create_credit_card_debt. The TOTAL number of installments in the purchase, as " +
                        "printed on the statement (e.g. \"1/10\" or \"3/10\" both mean installments=10). 1 for a " +
                        "single/à-vista purchase.",
                ),
            "installmentNumber" to
                property(
                    "integer",
                    "Only for create_credit_card_debt. Which installment this specific statement line is, as " +
                        "printed (e.g. \"3/10\" means installmentNumber=3, installments=10). 1 when the line has " +
                        "no installment marker, or shows \"1/N\". A number greater than 1 means the purchase was " +
                        "made before this statement: send the ORIGINAL purchase date as date, and the app skips " +
                        "the line on its own when that purchase is already recorded.",
                ),
        )

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): CreditCardDebtPayload {
        val categoryName = arguments.getString("categoryName")
        val lineAmount = arguments.getBigDecimal("amount").abs()
        val totalInstallments = (arguments.optIntOrNull("installments") ?: 1).coerceIn(1, Constants.MAX_INSTALLMENTS)
        val reportedNumber = (arguments.optIntOrNull("installmentNumber") ?: 1).coerceAtLeast(1)
        val remaining = (totalInstallments - reportedNumber + 1).coerceIn(1, Constants.MAX_INSTALLMENTS)

        return CreditCardDebtPayload(
            date = arguments.requireDate("date"),
            description = arguments.optString("description"),
            amount = lineAmount.multiply(remaining.toBigDecimal()),
            installments = remaining,
            installmentNumber = reportedNumber,
            creditCardId = catalog.creditCard(arguments.optString("creditCardName"))?.id,
            categoryName = catalog.category(categoryName)?.name ?: categoryName,
        )
    }

    override fun validate(payload: CreditCardDebtPayload): ValidationResult =
        when {
            payload.creditCardId == null ->
                ValidationResult.error(preferencesService.translate(TranslationKeys.AI_REVIEW_NO_WALLET_SELECTED))
            payload.amount.signum() <= 0 -> ValidationResult.error("amount must be positive")
            payload.installments !in 1..Constants.MAX_INSTALLMENTS ->
                ValidationResult.error("installments must be between 1 and ${Constants.MAX_INSTALLMENTS}")
            payload.categoryName.isBlank() -> ValidationResult.error("categoryName is required")
            else -> ValidationResult.OK
        }

    override fun missingFromModel(payload: CreditCardDebtPayload) =
        if (payload.amount.signum() <= 0) listOf("amount must be greater than 0") else emptyList()

    override fun categoryNeeded(payload: CreditCardDebtPayload) = payload.categoryName.takeIf { it.isNotBlank() }

    override fun summary(payload: CreditCardDebtPayload) =
        "${payload.description}, ${installmentValue(payload).toPlainString()}, ${payload.date}"

    private fun installmentValue(payload: CreditCardDebtPayload): BigDecimal =
        payload.amount.divide(payload.installments.toBigDecimal(), 2, RoundingMode.HALF_UP)

    override fun matchQuery(payload: CreditCardDebtPayload) =
        MatchQuery(
            amount = installmentValue(payload),
            date = payload.date,
            creditCardId = payload.creditCardId,
            description = payload.description.takeIf { it.isNotBlank() },
        )

    override fun draftSpec() =
        listOf(
            DraftField("date", "aidraft.field.date", DraftFieldType.DATE),
            DraftField("description", "aidraft.field.description", DraftFieldType.TEXT, required = false),
            DraftField("amount", "aidraft.field.totalAmount", DraftFieldType.DECIMAL),
            DraftField("installments", "aidraft.field.installments", DraftFieldType.INTEGER),
            DraftField("creditCardId", "aidraft.field.creditCard", DraftFieldType.CHOICE, DraftChoices.CREDIT_CARDS),
            DraftField("categoryName", "aidraft.field.category", DraftFieldType.CHOICE, DraftChoices.CATEGORIES),
        )

    override fun toDraft(payload: CreditCardDebtPayload) =
        DraftValues(
            name,
            mapOf(
                "date" to payload.date,
                "description" to payload.description,
                "amount" to payload.amount,
                "installments" to payload.installments,
                "installmentNumber" to payload.installmentNumber,
                "creditCardId" to payload.creditCardId,
                "categoryName" to payload.categoryName,
            ),
        )

    override fun fromDraft(draft: DraftValues): CreditCardDebtPayload? {
        val fields = draft.fields
        return CreditCardDebtPayload(
            date = fields["date"].asDate() ?: return null,
            description = fields["description"] as? String ?: "",
            amount = fields["amount"].asDecimal()?.abs() ?: return null,
            installments = fields["installments"].asInt() ?: 1,
            installmentNumber = fields["installmentNumber"].asInt() ?: 1,
            creditCardId = fields["creditCardId"].asInt(),
            categoryName = fields["categoryName"] as? String ?: "",
        )
    }

    override fun execute(payload: CreditCardDebtPayload): ApplyResult.Success {
        val creditCard = creditCardService.findCard(payload.creditCardId!!)
        val invoiceMonth =
            if (payload.installmentNumber > 1) {
                creditCardService.currentInvoiceMonth(creditCard)
            } else {
                creditCardService.invoiceMonthForPurchaseDate(creditCard, payload.date)
            }

        val id =
            creditCardService.createDebt(
                CreditCardDebt(
                    category = categoryResolver.resolveOrCreate(payload.categoryName),
                    installments = payload.installments,
                    creditCard = creditCard,
                    date = payload.date.atStartOfDay(),
                    amount = payload.amount,
                    description = payload.description,
                ),
                invoiceMonth = invoiceMonth,
                publishNotification = true,
            )
        return ApplyResult.Success(listOf(id), payload.description)
    }
}

@Component
class PayCreditCardInvoiceOperation(
    private val creditCardService: CreditCardService,
    private val preferencesService: PreferencesService,
) : BaseOperation<CreditCardInvoicePaymentPayload>() {
    override val name = "pay_credit_card_invoice"
    override val domain = OperationDomain.CREDIT_CARD
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("date", "amount", "creditCardName")

    override fun modelDescription() =
        "pay_credit_card_invoice: paying a card's bill in full - use this INSTEAD of a wallet expense for a " +
            "statement line that pays off a card invoice, since the app already debits the wallet directly when an " +
            "invoice is paid; a wallet expense would double count it. Check get_credit_card_invoice_payment_status " +
            "first and do not propose it for an invoice that is already fully paid."

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf(
            "invoiceMonth" to
                property(
                    "string",
                    "Billing month (YYYY-MM) of the invoice being paid - only for pay_credit_card_invoice. Leave " +
                        "unset if unknown; defaults to the payment date's own month.",
                ),
        )

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): CreditCardInvoicePaymentPayload =
        CreditCardInvoicePaymentPayload(
            creditCardId = catalog.creditCard(arguments.optString("creditCardName"))?.id,
            invoiceMonth =
                arguments
                    .optString("invoiceMonth")
                    .takeIf { it.isNotBlank() }
                    ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
                    ?: YearMonth.from(arguments.requireDate("date")),
            amount = arguments.getBigDecimal("amount").abs(),
        )

    override fun validate(payload: CreditCardInvoicePaymentPayload): ValidationResult =
        when {
            payload.creditCardId == null ->
                ValidationResult.error(preferencesService.translate(TranslationKeys.AI_REVIEW_NO_WALLET_SELECTED))
            payload.amount.signum() <= 0 -> ValidationResult.error("amount must be positive")
            else -> ValidationResult.OK
        }

    override fun missingFromModel(payload: CreditCardInvoicePaymentPayload) =
        if (payload.amount.signum() <= 0) listOf("amount must be greater than 0") else emptyList()

    override fun summary(payload: CreditCardInvoicePaymentPayload) =
        "${payload.invoiceMonth}, ${payload.amount.toPlainString()}"

    override fun draftSpec() =
        listOf(
            DraftField("creditCardId", "aidraft.field.creditCard", DraftFieldType.CHOICE, DraftChoices.CREDIT_CARDS),
            DraftField("invoiceMonth", "aidraft.field.invoiceMonth", DraftFieldType.TEXT),
            DraftField("amount", "aidraft.field.amount", DraftFieldType.DECIMAL),
        )

    override fun toDraft(payload: CreditCardInvoicePaymentPayload) =
        DraftValues(
            name,
            mapOf(
                "creditCardId" to payload.creditCardId,
                "invoiceMonth" to payload.invoiceMonth.toString(),
                "amount" to payload.amount,
            ),
        )

    override fun fromDraft(draft: DraftValues): CreditCardInvoicePaymentPayload? {
        val fields = draft.fields
        return CreditCardInvoicePaymentPayload(
            creditCardId = fields["creditCardId"].asInt(),
            invoiceMonth =
                (fields["invoiceMonth"] as? String)?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
                    ?: return null,
            amount = fields["amount"].asDecimal()?.abs() ?: return null,
        )
    }

    override fun execute(payload: CreditCardInvoicePaymentPayload): ApplyResult.Success {
        val creditCard = creditCardService.findCard(payload.creditCardId!!)
        val billingWallet =
            creditCard.defaultBillingWallet
                ?: error(preferencesService.translate(TranslationKeys.AI_REVIEW_NO_BILLING_WALLET))

        creditCardService.payInvoice(
            CreditCardInvoicePaymentDTO(
                creditCardId = creditCard.id!!,
                billingWalletId = billingWallet.id!!,
                invoiceDate = payload.invoiceMonth,
                amount = payload.amount,
            ),
        )
        return ApplyResult.Success(listOf(creditCard.id!!), "${creditCard.name} ${payload.invoiceMonth}")
    }
}
