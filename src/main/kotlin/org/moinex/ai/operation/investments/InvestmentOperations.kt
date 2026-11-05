/*
 * Filename: InvestmentOperations.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation.investments

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
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.model.enums.OperationType
import org.moinex.model.enums.WalletTransactionStatus
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
 * operations.
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
)

abstract class InvestmentOperation(
    private val walletService: WalletService,
    private val categoryResolver: CategoryResolver,
    private val preferencesService: PreferencesService,
) : BaseOperation<InvestmentPayload>() {
    final override val domain = OperationDomain.INVESTMENTS
    override val risk = OperationRisk.LOW

    protected open val usesQuantityAndPrice = true
    protected open val usesBond = false
    protected open val usesFees = false

    override fun modelRequired() =
        listOf("date", "categoryName", "assetName", "walletName") +
            if (usesQuantityAndPrice) listOf("quantity", "unitPrice") else listOf("amount")

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
                    "REQUIRED and greater than 0 for stock purchase/sale and bond application/redemption. Leave at " +
                        "0 for record_dividend.",
                ),
            "unitPrice" to
                property(
                    "number",
                    "REQUIRED and greater than 0 for stock purchase/sale and bond application/redemption. Leave at " +
                        "0 for record_dividend.",
                ),
            "fees" to property("number", "Only for apply_to_bond/redeem_bond. 0 otherwise."),
            "taxes" to property("number", "Only for apply_to_bond/redeem_bond. 0 otherwise."),
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

        return InvestmentPayload(
            context =
                InvestmentContext(
                    date = arguments.requireDate("date"),
                    description = arguments.optString("description"),
                    walletId = catalog.wallet(arguments.optString("walletName"))?.id,
                    categoryName = catalog.category(categoryName)?.name ?: categoryName,
                ),
            assetId = ticker?.id ?: bond?.id,
            quantity = arguments.optDecimalOrZero("quantity"),
            unitPrice = arguments.optDecimalOrZero("unitPrice"),
            amount = arguments.optDecimalOrZero("dividendAmount"),
            fees = arguments.optDecimalOrZero("fees"),
            taxes = arguments.optDecimalOrZero("taxes"),
            assetLabel = ticker?.let { "${it.symbol} - ${it.name}" } ?: bond?.name ?: assetName,
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
            else -> ValidationResult.OK
        }

    override fun missingFromModel(payload: InvestmentPayload): List<String> =
        buildList {
            if (usesQuantityAndPrice) {
                if (payload.quantity.signum() <= 0) add("quantity must be greater than 0")
                if (payload.unitPrice.signum() <= 0) add("unitPrice must be greater than 0")
            } else if (payload.amount.signum() <= 0) {
                add("dividendAmount must be greater than 0")
            }
        }

    override fun categoryNeeded(payload: InvestmentPayload) = payload.context.categoryName.takeIf { it.isNotBlank() }

    override fun summary(payload: InvestmentPayload) =
        "${payload.assetLabel.ifBlank { payload.context.description }}, ${payload.context.date}"

    override fun draftSpec(): List<DraftField> =
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
            if (usesQuantityAndPrice) {
                add(DraftField("quantity", "aidraft.field.quantity", DraftFieldType.DECIMAL))
                add(DraftField("unitPrice", "aidraft.field.unitPrice", DraftFieldType.DECIMAL))
            } else {
                add(DraftField("amount", "aidraft.field.amount", DraftFieldType.DECIMAL))
            }
            if (usesFees) {
                add(DraftField("fees", "aidraft.field.fees", DraftFieldType.DECIMAL, required = false))
                add(DraftField("taxes", "aidraft.field.taxes", DraftFieldType.DECIMAL, required = false))
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
                "amount" to payload.amount,
                "fees" to payload.fees,
                "taxes" to payload.taxes,
                "walletId" to payload.context.walletId,
                "categoryName" to payload.context.categoryName,
            ),
        )

    override fun fromDraft(draft: DraftValues): InvestmentPayload? {
        val fields = draft.fields
        return InvestmentPayload(
            context =
                InvestmentContext(
                    date = fields["date"].asDate() ?: return null,
                    description = fields["description"] as? String ?: "",
                    walletId = fields["walletId"].asInt(),
                    categoryName = fields["categoryName"] as? String ?: "",
                ),
            assetId = fields["assetId"].asInt(),
            quantity = fields["quantity"].asDecimal() ?: BigDecimal.ZERO,
            unitPrice = fields["unitPrice"].asDecimal() ?: BigDecimal.ZERO,
            amount = fields["amount"].asDecimal() ?: BigDecimal.ZERO,
            fees = fields["fees"].asDecimal() ?: BigDecimal.ZERO,
            taxes = fields["taxes"].asDecimal() ?: BigDecimal.ZERO,
            assetLabel = fields["assetLabel"] as? String ?: "",
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

@Component
class ApplyToBondOperation(
    private val bondService: BondService,
    walletService: WalletService,
    categoryResolver: CategoryResolver,
    preferencesService: PreferencesService,
) : InvestmentOperation(walletService, categoryResolver, preferencesService) {
    override val name = "apply_to_bond"
    override val usesBond = true
    override val usesFees = true

    override fun modelDescription() =
        "apply_to_bond: investing in a CDB/fixed-income bond (assetName = an existing bond, never a ticker)."

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
    private val bondService: BondService,
    walletService: WalletService,
    categoryResolver: CategoryResolver,
    preferencesService: PreferencesService,
) : InvestmentOperation(walletService, categoryResolver, preferencesService) {
    override val name = "redeem_bond"
    override val risk = OperationRisk.MEDIUM
    override val usesBond = true
    override val usesFees = true

    override fun modelDescription() =
        "redeem_bond: redeeming a CDB/fixed-income bond (assetName = an existing bond, never a ticker)."

    override fun execute(payload: InvestmentPayload): ApplyResult.Success {
        val bond = bondService.getBondById(payload.assetId!!)
        val averageUnitPrice = bondService.getAverageUnitPrice(bond)
        val netProfit =
            payload.unitPrice
                .subtract(averageUnitPrice)
                .multiply(payload.quantity)
                .subtract(payload.fees)
                .subtract(payload.taxes)

        bondService.createBondOperation(
            BondOperation(
                bond = bond,
                operationType = OperationType.SELL,
                quantity = payload.quantity,
                unitPrice = payload.unitPrice,
                fees = payload.fees,
                taxes = payload.taxes,
                netProfit = netProfit,
            ),
            walletContext(payload.context),
        )
        return done(payload)
    }
}
