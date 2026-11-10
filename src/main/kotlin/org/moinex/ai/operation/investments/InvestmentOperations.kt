/*
 * Filename: InvestmentOperations.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation.investments

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
import org.moinex.ai.operation.cardText
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isHeldInReais
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.model.enums.OperationType
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondOperation
import org.moinex.model.investment.Dividend
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPurchase
import org.moinex.model.investment.TickerSale
import org.moinex.service.PreferencesService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode
import java.time.LocalDate

/** What every investment operation needs to create the wallet transaction that goes with it. */
data class InvestmentContext(
    val date: LocalDate,
    val description: String,
    val walletId: Int?,
    val categoryName: String,
)

/**
 * [assetId] is a ticker id for stock purchase/sale/dividend and a bond id for bond application/redemption, null while
 * the model's asset name matched nothing; [assetLabel] is what the model called it. [quantity] and [unitPrice] are
 * zero for a dividend, [amount] is only used by a dividend (the total received), [fees] and [taxes] only by bond
 * operations. A bond held in reais ([amountOnly]: CDB, LCI, LCA) has no unit price: the value in reais is kept as
 * the [quantity] at a [unitPrice] of one, the same way the app's own forms save it. For a redemption of such a bond
 * [quantity] is the gross value redeemed, [interest] the part of it that is yield and [autoCalculate] asks the app to
 * work out the interest and the tax itself, as the manual form does; it is off unless the user turns it on.
 */
data class InvestmentPayload(
    val context: InvestmentContext,
    val assetId: Int?,
    val quantity: BigDecimal = BigDecimal.ZERO,
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val amount: BigDecimal = BigDecimal.ZERO,
    val fees: BigDecimal = BigDecimal.ZERO,
    val taxes: BigDecimal = BigDecimal.ZERO,
    val assetLabel: String = "",
    val amountOnly: Boolean = false,
    val interest: BigDecimal = BigDecimal.ZERO,
    val autoCalculate: Boolean = false,
)

abstract class InvestmentOperation(
    private val walletService: WalletService,
    private val categoryResolver: CategoryResolver,
    protected val preferencesService: PreferencesService,
) : BaseOperation<InvestmentPayload>() {
    final override val domain = OperationDomain.INVESTMENTS
    override val risk = OperationRisk.LOW

    protected open val usesQuantityAndPrice = true
    protected open val usesBond = false
    protected open val usesFees = false
    protected open val usesRedemptionBreakdown = false

    protected open fun assetHeldInReais(assetId: Int?): Boolean = false

    override fun modelRequired() =
        listOf("date", "categoryName", "assetName", "walletName", "description") +
            when {
                usesBond -> emptyList()
                usesQuantityAndPrice -> listOf("quantity", "unitPrice")
                else -> listOf("amount")
            }

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf(
            "assetName" to
                property(
                    "string",
                    "The ticker symbol (e.g. PETR4) for record_stock_purchase/record_stock_sale/record_dividend, or " +
                        "the bond's name/symbol for apply_to_bond/redeem_bond, matched against the existing " +
                        "tickers/bonds listed in context. Never invent one that doesn't exist there.",
                ),
            "quantity" to
                property(
                    "number",
                    "REQUIRED and greater than 0 for stock purchase/sale and for bonds that are not CDB, LCI or LCA. " +
                        "Never send it for a CDB, LCI or LCA (send amount). Leave at 0 for record_dividend.",
                ),
            "unitPrice" to
                property(
                    "number",
                    "REQUIRED and greater than 0 for stock purchase/sale and for bonds that are not CDB, LCI or LCA. " +
                        "Never send it for a CDB, LCI or LCA (send amount). Leave at 0 for record_dividend.",
                ),
            "amount" to
                property(
                    "number",
                    "ONLY for apply_to_bond/redeem_bond on a CDB, LCI or LCA, which have no quantity or unit price: " +
                        "the value in reais invested, or the value redeemed. REQUIRED for them.",
                ),
            "interest" to
                property(
                    "number",
                    "Only for redeem_bond on a CDB, LCI or LCA, and only when the user stated how much of the " +
                        "redemption is yield. Otherwise leave it out: never estimate or calculate it.",
                ),
            "fees" to
                property(
                    "number",
                    "Only for apply_to_bond/redeem_bond, and only when the user stated them; never estimate. " +
                        "0 otherwise.",
                ),
            "taxes" to
                property(
                    "number",
                    "Only for apply_to_bond/redeem_bond, and only when the user stated them; never estimate or " +
                        "calculate taxes. 0 otherwise.",
                ),
            "dividendAmount" to property("number", "Total amount received. Only for record_dividend."),
        )

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): InvestmentPayload {
        val assetName = arguments.optString("assetName")
        val ticker = if (!usesBond) catalog.ticker(assetName) else null
        val bond = if (usesBond) catalog.bond(assetName) else null
        val categoryName = arguments.getString("categoryName")

        val assetLabel = ticker?.let { "${it.symbol} - ${it.name}" } ?: bond?.name ?: assetName
        val amountOnly = bond?.isHeldInReais() == true
        val quantity = arguments.optDecimalOrZero("quantity")
        val unitPrice = arguments.optDecimalOrZero("unitPrice")

        return InvestmentPayload(
            context =
                InvestmentContext(
                    date = arguments.requireDate("date"),
                    description = arguments.optString("description"),
                    walletId = catalog.wallet(arguments.optString("walletName"))?.id,
                    categoryName = catalog.category(categoryName)?.name ?: categoryName,
                ),
            assetId = ticker?.id ?: bond?.id,
            quantity =
                if (amountOnly) {
                    arguments.optDecimalOrZero("amount").takeIf { it.signum() > 0 } ?: quantity.multiply(unitPrice)
                } else {
                    quantity
                },
            unitPrice = if (amountOnly) BigDecimal.ONE else unitPrice,
            amount = if (amountOnly) BigDecimal.ZERO else arguments.optDecimalOrZero("dividendAmount"),
            fees = arguments.optDecimalOrZero("fees"),
            taxes = arguments.optDecimalOrZero("taxes"),
            assetLabel = assetLabel,
            amountOnly = amountOnly,
            interest = if (amountOnly && usesRedemptionBreakdown) arguments.optDecimalOrZero("interest") else ZERO,
        )
    }

    override fun validate(payload: InvestmentPayload): ValidationResult =
        when {
            payload.context.walletId == null ->
                ValidationResult.error(preferencesService.translate(TranslationKeys.AI_REVIEW_INV_NO_WALLET_SELECTED))
            payload.assetId == null ->
                ValidationResult.error(preferencesService.translate(TranslationKeys.AI_REVIEW_INV_NO_ASSET_SELECTED))
            payload.context.categoryName.isBlank() -> ValidationResult.error("categoryName is required")
            usesQuantityAndPrice && (payload.quantity.signum() <= 0 || payload.unitPrice.signum() <= 0) ->
                ValidationResult.error("quantity and unitPrice must be positive")
            !usesQuantityAndPrice && payload.amount.signum() <= 0 -> ValidationResult.error("amount must be positive")
            payload.amountOnly && !payload.autoCalculate && payload.interest >= payload.quantity ->
                ValidationResult.error("interest must be less than the amount redeemed")
            else -> ValidationResult.OK
        }

    override fun missingFromModel(payload: InvestmentPayload): List<String> =
        buildList {
            addAll(missingDescription(payload.context.description))
            if (usesBond && payload.assetId == null) {
                return@buildList
            } else if (payload.amountOnly) {
                if (payload.quantity.signum() <= 0) add("amount must be greater than 0")
            } else if (usesQuantityAndPrice) {
                if (payload.quantity.signum() <= 0) add("quantity must be greater than 0")
                if (payload.unitPrice.signum() <= 0) add("unitPrice must be greater than 0")
            } else if (payload.amount.signum() <= 0) {
                add("dividendAmount must be greater than 0")
            }
        }

    override fun categoryNeeded(payload: InvestmentPayload) = payload.context.categoryName.takeIf { it.isNotBlank() }

    override fun summary(payload: InvestmentPayload) =
        "${payload.assetLabel.ifBlank { payload.context.description }}, ${payload.context.date}"

    override fun card(payload: InvestmentPayload): CardInfo =
        CardInfo(
            payload.assetLabel.ifBlank { payload.context.description },
            buildList {
                when {
                    payload.amountOnly -> add(cardMoney(payload.quantity))
                    usesQuantityAndPrice -> {
                        add(
                            cardText(
                                payload.quantity.stripTrailingZeros().toPlainString(),
                                preferencesService.translate("aidraft.field.quantity"),
                            ),
                        )
                        add(cardMoney(payload.unitPrice, preferencesService.translate("aidraft.field.unitPrice")))
                    }
                    else -> add(cardMoney(payload.amount))
                }
                add(cardDay(payload.context.date))
            },
        )

    override fun draftSpec(): List<DraftField> = draftSpecFor(amountOnly = false)

    override fun draftSpecFor(payload: InvestmentPayload): List<DraftField> = draftSpecFor(payload.amountOnly)

    private fun draftSpecFor(amountOnly: Boolean): List<DraftField> =
        buildList {
            add(DraftField("date", "aidraft.field.date", DraftFieldType.DATE))
            add(DraftField("description", "aidraft.field.description", DraftFieldType.TEXT, required = false))
            add(
                DraftField(
                    "assetId",
                    "aidraft.field.asset",
                    DraftFieldType.CHOICE,
                    if (usesBond) DraftChoices.BONDS else DraftChoices.TICKERS,
                ),
            )
            if (amountOnly) {
                add(DraftField("amount", "aidraft.field.amount", DraftFieldType.DECIMAL))
                if (usesRedemptionBreakdown) {
                    add(
                        DraftField(
                            "interest",
                            "aidraft.field.redeemInterest",
                            DraftFieldType.DECIMAL,
                            required = false,
                        ),
                    )
                }
            } else if (usesQuantityAndPrice) {
                add(DraftField("quantity", "aidraft.field.quantity", DraftFieldType.DECIMAL))
                add(DraftField("unitPrice", "aidraft.field.unitPrice", DraftFieldType.DECIMAL))
            } else {
                add(DraftField("amount", "aidraft.field.amount", DraftFieldType.DECIMAL))
            }
            if (usesFees) {
                add(DraftField("fees", "aidraft.field.fees", DraftFieldType.DECIMAL, required = false))
                add(DraftField("taxes", "aidraft.field.taxes", DraftFieldType.DECIMAL, required = false))
            }
            if (amountOnly && usesRedemptionBreakdown) {
                add(
                    DraftField(
                        "autoCalculate",
                        "aidraft.field.autoCalculate",
                        DraftFieldType.BOOLEAN,
                        required = false,
                    ),
                )
            }
            add(DraftField("walletId", "aidraft.field.wallet", DraftFieldType.CHOICE, DraftChoices.WALLETS))
            add(DraftField("categoryName", "aidraft.field.category", DraftFieldType.CHOICE, DraftChoices.CATEGORIES))
        }

    override fun toDraft(payload: InvestmentPayload) =
        DraftValues(
            name,
            mapOf(
                "date" to payload.context.date,
                "description" to payload.context.description,
                "assetId" to payload.assetId,
                "assetLabel" to payload.assetLabel,
                "quantity" to payload.quantity,
                "unitPrice" to payload.unitPrice,
                "amount" to if (payload.amountOnly) payload.quantity else payload.amount,
                "fees" to payload.fees,
                "taxes" to payload.taxes,
                "interest" to payload.interest,
                "autoCalculate" to payload.autoCalculate,
                "walletId" to payload.context.walletId,
                "categoryName" to payload.context.categoryName,
            ),
        )

    override fun fromDraft(draft: DraftValues): InvestmentPayload? {
        val fields = draft.fields
        val assetId = fields["assetId"].asInt()
        val amountOnly = usesBond && assetHeldInReais(assetId)
        val quantity = fields["quantity"].asDecimal() ?: BigDecimal.ZERO
        val unitPrice = fields["unitPrice"].asDecimal() ?: BigDecimal.ZERO
        val amount = fields["amount"].asDecimal() ?: BigDecimal.ZERO
        return InvestmentPayload(
            context =
                InvestmentContext(
                    date = fields["date"].asDate() ?: return null,
                    description = fields["description"] as? String ?: "",
                    walletId = fields["walletId"].asInt(),
                    categoryName = fields["categoryName"] as? String ?: "",
                ),
            assetId = assetId,
            quantity = if (amountOnly) amount.takeIf { it.signum() > 0 } ?: quantity.multiply(unitPrice) else quantity,
            unitPrice = if (amountOnly) BigDecimal.ONE else unitPrice,
            amount = if (amountOnly) BigDecimal.ZERO else amount,
            fees = fields["fees"].asDecimal() ?: BigDecimal.ZERO,
            taxes = fields["taxes"].asDecimal() ?: BigDecimal.ZERO,
            assetLabel = fields["assetLabel"] as? String ?: "",
            amountOnly = amountOnly,
            interest =
                if (amountOnly && usesRedemptionBreakdown) fields["interest"].asDecimal() ?: ZERO else ZERO,
            autoCalculate = amountOnly && usesRedemptionBreakdown && fields["autoCalculate"].asBoolean(),
        )
    }

    protected fun walletContext(context: InvestmentContext): WalletTransactionContextDTO =
        WalletTransactionContextDTO(
            wallet = walletService.getWalletById(context.walletId!!),
            date = context.date.atStartOfDay(),
            category = categoryResolver.resolveOrCreate(context.categoryName),
            description = context.description,
            status = WalletTransactionStatus.CONFIRMED,
            includeInAnalysis = true,
        )

    protected fun done(payload: InvestmentPayload) =
        ApplyResult.Success(listOf(payload.assetId!!), payload.context.description)
}

private fun TickerService.findTicker(id: Int): Ticker =
    getAllNonArchivedTickers().firstOrNull { it.id == id } ?: error("Ticker $id not found")

@Component
class RecordStockPurchaseOperation(
    private val tickerService: TickerService,
    walletService: WalletService,
    categoryResolver: CategoryResolver,
    preferencesService: PreferencesService,
) : InvestmentOperation(walletService, categoryResolver, preferencesService) {
    override val name = "record_stock_purchase"

    override fun modelDescription() =
        "record_stock_purchase: buying a stock, FII, ETF or crypto ticker (assetName = an existing ticker)."

    override fun execute(payload: InvestmentPayload): ApplyResult.Success {
        val ticker = tickerService.findTicker(payload.assetId!!)
        tickerService.createTickerPurchase(
            TickerPurchase(ticker = ticker, quantity = payload.quantity, unitPrice = payload.unitPrice),
            walletContext(payload.context),
        )
        return done(payload)
    }
}

@Component
class RecordStockSaleOperation(
    private val tickerService: TickerService,
    walletService: WalletService,
    categoryResolver: CategoryResolver,
    preferencesService: PreferencesService,
) : InvestmentOperation(walletService, categoryResolver, preferencesService) {
    override val name = "record_stock_sale"
    override val risk = OperationRisk.MEDIUM

    override fun modelDescription() =
        "record_stock_sale: selling a stock, FII, ETF or crypto ticker (assetName = an existing ticker)."

    override fun execute(payload: InvestmentPayload): ApplyResult.Success {
        val ticker = tickerService.findTicker(payload.assetId!!)
        tickerService.createTickerSale(
            TickerSale(
                ticker = ticker,
                averageCost = ticker.averageUnitValue,
                quantity = payload.quantity,
                unitPrice = payload.unitPrice,
            ),
            walletContext(payload.context),
        )
        return done(payload)
    }
}

@Component
class RecordDividendOperation(
    private val tickerService: TickerService,
    walletService: WalletService,
    categoryResolver: CategoryResolver,
    preferencesService: PreferencesService,
) : InvestmentOperation(walletService, categoryResolver, preferencesService) {
    override val name = "record_dividend"
    override val usesQuantityAndPrice = false

    override fun modelDescription() =
        "record_dividend: cash received for holding a ticker; only dividendAmount matters, quantity and unitPrice " +
            "stay 0."

    override fun execute(payload: InvestmentPayload): ApplyResult.Success {
        val ticker = tickerService.findTicker(payload.assetId!!)
        tickerService.createDividend(Dividend(ticker = ticker), payload.amount, walletContext(payload.context))
        return done(payload)
    }
}

abstract class BondInvestmentOperation(
    protected val bondService: BondService,
    walletService: WalletService,
    categoryResolver: CategoryResolver,
    preferencesService: PreferencesService,
) : InvestmentOperation(walletService, categoryResolver, preferencesService) {
    final override val usesBond = true
    final override val usesFees = true

    override fun assetHeldInReais(assetId: Int?): Boolean =
        assetId?.let { runCatching { bondService.getBondById(it).isHeldInReais() }.getOrDefault(false) } ?: false
}

@Component
class ApplyToBondOperation(
    bondService: BondService,
    walletService: WalletService,
    categoryResolver: CategoryResolver,
    preferencesService: PreferencesService,
) : BondInvestmentOperation(bondService, walletService, categoryResolver, preferencesService) {
    override val name = "apply_to_bond"

    override fun modelDescription() =
        "apply_to_bond: investing in a CDB/fixed-income bond (assetName = an existing bond, never a ticker). " +
            "For a CDB, LCI or LCA send only amount (the value in reais), never quantity or unitPrice."

    override fun execute(payload: InvestmentPayload): ApplyResult.Success {
        val bond = bondService.getBondById(payload.assetId!!)
        bondService.createBondOperation(
            BondOperation(
                bond = bond,
                operationType = OperationType.BUY,
                quantity = payload.quantity,
                unitPrice = payload.unitPrice,
                fees = payload.fees,
                taxes = payload.taxes,
                netProfit = BigDecimal.ZERO,
            ),
            walletContext(payload.context),
        )
        return done(payload)
    }
}

@Component
class RedeemBondOperation(
    bondService: BondService,
    walletService: WalletService,
    categoryResolver: CategoryResolver,
    preferencesService: PreferencesService,
) : BondInvestmentOperation(bondService, walletService, categoryResolver, preferencesService) {
    override val name = "redeem_bond"
    override val risk = OperationRisk.MEDIUM
    override val usesRedemptionBreakdown = true

    override fun modelDescription() =
        "redeem_bond: redeeming a CDB/fixed-income bond (assetName = an existing bond, never a ticker). " +
            "For a CDB, LCI or LCA send only amount (the value redeemed), never quantity or unitPrice, and do not " +
            "calculate or estimate interest, fees or taxes: send them only when the user stated them. The user " +
            "can add them, or ask the app to calculate them, when reviewing."

    override fun execute(payload: InvestmentPayload): ApplyResult.Success {
        val bond = bondService.getBondById(payload.assetId!!)
        val operation =
            if (payload.amountOnly) {
                val redemption = redemptionInReais(bond, payload)
                BondOperation(
                    bond = bond,
                    operationType = OperationType.SELL,
                    quantity = redemption.principal,
                    unitPrice = BigDecimal.ONE,
                    fees = payload.fees,
                    taxes = redemption.taxes,
                    netProfit = redemption.netProfit,
                )
            } else {
                redemptionByUnits(bond, payload)
            }

        bondService.createBondOperation(operation, walletContext(payload.context))
        return done(payload)
    }

    override fun preview(payload: InvestmentPayload): Preview? {
        if (!payload.amountOnly || payload.assetId == null || payload.quantity.signum() <= 0) return null

        return runCatching {
            val redemption = redemptionInReais(bondService.getBondById(payload.assetId), payload)
            val credited = redemption.principal.add(redemption.netProfit)

            fun fact(
                key: String,
                value: BigDecimal,
            ) = cardMoney(value, preferencesService.translate(key))

            Preview(
                emptyList(),
                listOf(
                    listOf(
                        fact(TranslationKeys.AIPANEL_FACT_PRINCIPAL, redemption.principal),
                        fact(TranslationKeys.AIDRAFT_FIELD_REDEEM_INTEREST, redemption.interest),
                        fact(TranslationKeys.AIDRAFT_FIELD_TAXES, redemption.taxes),
                        fact(TranslationKeys.AIPANEL_FACT_INTO_WALLET, credited),
                    ),
                ),
            )
        }.getOrNull()
    }

    override fun suggest(draft: DraftValues): Map<String, Any?> {
        val payload = fromDraft(draft)

        if (payload == null || !payload.autoCalculate || payload.assetId == null || payload.quantity.signum() <= 0) {
            return emptyMap()
        }

        return runCatching {
            val estimate =
                bondService.estimateRedemptionByGross(
                    bondService.getBondById(payload.assetId),
                    payload.quantity,
                    payload.context.date,
                )
            mapOf(
                "interest" to estimate.interestShare.setScale(2, RoundingMode.HALF_UP),
                "taxes" to
                    (if (estimate.taxApplicable) estimate.estimatedTax else ZERO).setScale(2, RoundingMode.HALF_UP),
            )
        }.getOrDefault(emptyMap())
    }

    private data class Redemption(
        val principal: BigDecimal,
        val interest: BigDecimal,
        val taxes: BigDecimal,
        val netProfit: BigDecimal,
    )

    private fun redemptionByUnits(
        bond: Bond,
        payload: InvestmentPayload,
    ): BondOperation {
        val averageUnitPrice = bondService.getAverageUnitPrice(bond)
        val netProfit =
            payload.unitPrice
                .subtract(averageUnitPrice)
                .multiply(payload.quantity)
                .subtract(payload.fees)
                .subtract(payload.taxes)

        return BondOperation(
            bond = bond,
            operationType = OperationType.SELL,
            quantity = payload.quantity,
            unitPrice = payload.unitPrice,
            fees = payload.fees,
            taxes = payload.taxes,
            netProfit = netProfit,
        )
    }

    /**
     * The value redeemed is split as the redemption form does: the interest leaves the principal, which is the rest.
     * Interest and taxes are what the user typed (none by default); with [InvestmentPayload.autoCalculate] they are
     * the app's estimate instead.
     */
    private fun redemptionInReais(
        bond: Bond,
        payload: InvestmentPayload,
    ): Redemption {
        val gross = payload.quantity
        val estimate =
            if (payload.autoCalculate) {
                bondService.estimateRedemptionByGross(bond, gross, payload.context.date)
            } else {
                null
            }
        val interest = estimate?.interestShare ?: payload.interest
        val taxes = estimate?.let { if (it.taxApplicable) it.estimatedTax else ZERO } ?: payload.taxes
        val principal = gross.subtract(interest).max(ZERO)
        val netProfit =
            BigDecimal.ONE
                .subtract(bondService.getAverageUnitPrice(bond))
                .multiply(principal)
                .add(interest)
                .subtract(payload.fees)
                .subtract(taxes)

        return Redemption(principal, interest, taxes, netProfit)
    }
}
