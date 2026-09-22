/*
 * Filename: ProposeInvestmentOperationTool.kt
 * Created on: September 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.tools

import org.json.JSONArray
import org.json.JSONObject
import org.moinex.common.constant.TranslationKeys
import org.moinex.model.Category
import org.moinex.model.dto.ai.ExtractedInvestmentOperationDTO
import org.moinex.model.dto.ai.InvestmentOperationKind
import org.moinex.model.investment.Bond
import org.moinex.model.investment.Ticker
import org.moinex.model.wallettransaction.Wallet
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Proposes stock/FII/crypto purchases and sales, dividends, and CDB (bond) applications and
 * redemptions for the user to review. Like [ProposeTransactionTool], this never writes anything
 * directly - it only resolves the candidates against existing tickers/bonds/wallets/categories and
 * returns the normalized result, which [org.moinex.ai.chat.AiChatService] intercepts to route to a
 * dedicated review screen instead of the wallet/credit-card one, since these operations persist
 * through [TickerService]/[BondService] rather than a plain wallet transaction.
 */
@Service
class ProposeInvestmentOperationTool(
    private val categoryService: CategoryService,
    private val walletService: WalletService,
    private val tickerService: TickerService,
    private val bondService: BondService,
    private val preferencesService: PreferencesService,
) : LlmTool {
    companion object {
        const val NAME = "propose_investment_operations"

        fun parseResultJson(json: String): List<ExtractedInvestmentOperationDTO> {
            val array = JSONArray(json)
            return (0 until array.length()).map { toDto(array.getJSONObject(it)) }
        }

        private fun toDto(json: JSONObject): ExtractedInvestmentOperationDTO =
            ExtractedInvestmentOperationDTO(
                date = LocalDate.parse(json.getString("date")),
                kind = parseKind(json.getString("kind")),
                description = json.optString("description"),
                quantity = json.optBigDecimalOrZero("quantity"),
                unitPrice = json.optBigDecimalOrZero("unitPrice"),
                fees = json.optBigDecimalOrZero("fees"),
                taxes = json.optBigDecimalOrZero("taxes"),
                dividendAmount = json.optBigDecimalOrZero("dividendAmount"),
                suggestedTickerId = json.optIntOrNull("suggestedTickerId"),
                suggestedBondId = json.optIntOrNull("suggestedBondId"),
                suggestedAssetLabel = json.optString("suggestedAssetLabel"),
                suggestedWalletId = json.optIntOrNull("suggestedWalletId"),
                suggestedCategoryId = json.optIntOrNull("suggestedCategoryId"),
                suggestedCategoryName = json.optString("suggestedCategoryName"),
                confidence = json.optDouble("confidence", 0.5),
            )

        private fun parseKind(value: String): InvestmentOperationKind =
            when (value.lowercase()) {
                "stock_sale" -> InvestmentOperationKind.STOCK_SALE
                "dividend" -> InvestmentOperationKind.DIVIDEND
                "bond_application" -> InvestmentOperationKind.BOND_APPLICATION
                "bond_redemption" -> InvestmentOperationKind.BOND_REDEMPTION
                else -> InvestmentOperationKind.STOCK_PURCHASE
            }

        private fun usesBond(kind: InvestmentOperationKind) =
            kind == InvestmentOperationKind.BOND_APPLICATION || kind == InvestmentOperationKind.BOND_REDEMPTION

        private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key) || !has(key)) null else getInt(key)

        private fun JSONObject.optBigDecimalOrZero(key: String): BigDecimal =
            if (isNull(key) || !has(key)) BigDecimal.ZERO else getBigDecimal(key)
    }

    override val name = NAME
    override val description =
        preferencesService.translate(TranslationKeys.AI_TOOL_PROPOSE_INVESTMENT_OPERATIONS_DESCRIPTION)

    override val jsonSchema = buildJsonSchema().toString()

    private fun buildJsonSchema(): JSONObject {
        val kindProperty =
            JSONObject()
                .put("type", "string")
                .put(
                    "enum",
                    JSONArray(
                        listOf("stock_purchase", "stock_sale", "dividend", "bond_application", "bond_redemption"),
                    ),
                ).put(
                    "description",
                    "stock_purchase/stock_sale: buying/selling a stock, FII, ETF or crypto ticker - match " +
                        "assetName against an existing ticker. dividend: cash received for holding a ticker - " +
                        "match assetName against an existing ticker; only dividendAmount matters, leave quantity " +
                        "and unitPrice at 0. bond_application/bond_redemption: investing in or redeeming a " +
                        "CDB/fixed-income bond - match assetName against an existing bond, never a ticker.",
                )

        val assetNameProperty =
            JSONObject()
                .put("type", "string")
                .put(
                    "description",
                    "The ticker symbol (e.g. PETR4) for stock_purchase/stock_sale/dividend, or the bond's " +
                        "name/symbol for bond_application/bond_redemption, matched against the existing " +
                        "tickers/bonds listed in context. Never invent one that doesn't exist there.",
                )

        val quantityProperty =
            JSONObject()
                .put("type", "number")
                .put("description", "Number of units traded. Leave at 0 for kind dividend.")

        val unitPriceProperty =
            JSONObject()
                .put("type", "number")
                .put("description", "Price per unit. Leave at 0 for kind dividend.")

        val feesProperty =
            JSONObject()
                .put("type", "number")
                .put("description", "Only meaningful for bond_application/bond_redemption. 0 otherwise.")

        val taxesProperty =
            JSONObject()
                .put("type", "number")
                .put("description", "Only meaningful for bond_application/bond_redemption. 0 otherwise.")

        val dividendAmountProperty =
            JSONObject()
                .put("type", "number")
                .put("description", "Total amount received. Only meaningful for kind dividend. 0 otherwise.")

        val itemProperties =
            JSONObject()
                .put("date", JSONObject().put("type", "string").put("format", "date"))
                .put("kind", kindProperty)
                .put("assetName", assetNameProperty)
                .put("description", JSONObject().put("type", "string"))
                .put("quantity", quantityProperty)
                .put("unitPrice", unitPriceProperty)
                .put("fees", feesProperty)
                .put("taxes", taxesProperty)
                .put("dividendAmount", dividendAmountProperty)
                .put("walletName", JSONObject().put("type", "string"))
                .put("categoryName", JSONObject().put("type", "string"))
                .put("confidence", JSONObject().put("type", "number"))

        val requiredFields =
            JSONArray(listOf("date", "kind", "assetName", "description", "walletName", "categoryName"))

        val item =
            JSONObject()
                .put("type", "object")
                .put("properties", itemProperties)
                .put("required", requiredFields)

        val operationsProperty = JSONObject().put("type", "array").put("items", item)

        return JSONObject()
            .put("type", "object")
            .put("properties", JSONObject().put("operations", operationsProperty))
            .put("required", JSONArray(listOf("operations")))
    }

    fun describeAvailableContext(): String {
        val tickers =
            tickerService.getAllNonArchivedTickers().joinToString(", ") { "${it.symbol} (${it.name})" }
        val bonds =
            bondService.getAllNonArchivedBonds().joinToString(", ") { bond ->
                bond.symbol?.let { "$it (${bond.name})" } ?: bond.name
            }

        return "Existing tickers (stocks/FIIs/ETFs/crypto): $tickers\nExisting bonds/CDBs: $bonds"
    }

    override fun execute(argumentsJson: String): String {
        val candidates = JSONObject(argumentsJson).optJSONArray("operations") ?: JSONArray()
        val categories = categoryService.getNonArchivedCategoriesOrderedByName()
        val wallets = walletService.getAllNonArchivedWalletsOrderedByName()
        val tickers = tickerService.getAllNonArchivedTickers()
        val bonds = bondService.getAllNonArchivedBonds()

        val resolved =
            (0 until candidates.length()).mapNotNull { i ->
                runCatching { resolve(candidates.getJSONObject(i), categories, wallets, tickers, bonds) }.getOrNull()
            }

        return JSONArray(resolved.map { toJson(it) }).toString()
    }

    private fun resolve(
        candidate: JSONObject,
        categories: List<Category>,
        wallets: List<Wallet>,
        tickers: List<Ticker>,
        bonds: List<Bond>,
    ): ExtractedInvestmentOperationDTO {
        val kind = parseKind(candidate.optString("kind"))
        val assetName = candidate.optString("assetName")

        val categoryName = candidate.getString("categoryName")
        val matchedCategory = categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
        val matchedWallet =
            wallets.firstOrNull { it.name.equals(candidate.optString("walletName"), ignoreCase = true) }

        val matchedTicker =
            if (!usesBond(kind)) {
                tickers.firstOrNull {
                    it.symbol.equals(assetName, ignoreCase = true) || it.name.equals(assetName, ignoreCase = true)
                }
            } else {
                null
            }
        val matchedBond =
            if (usesBond(kind)) {
                bonds.firstOrNull {
                    it.symbol?.equals(assetName, ignoreCase = true) == true ||
                        it.name.equals(assetName, ignoreCase = true)
                }
            } else {
                null
            }
        val assetLabel =
            matchedTicker?.let { "${it.symbol} - ${it.name}" } ?: matchedBond?.name ?: assetName

        return ExtractedInvestmentOperationDTO(
            date = LocalDate.parse(candidate.getString("date")),
            kind = kind,
            description = candidate.optString("description"),
            quantity = candidate.optBigDecimalOrZero("quantity"),
            unitPrice = candidate.optBigDecimalOrZero("unitPrice"),
            fees = candidate.optBigDecimalOrZero("fees"),
            taxes = candidate.optBigDecimalOrZero("taxes"),
            dividendAmount = candidate.optBigDecimalOrZero("dividendAmount"),
            suggestedTickerId = matchedTicker?.id,
            suggestedBondId = matchedBond?.id,
            suggestedAssetLabel = assetLabel,
            suggestedWalletId = matchedWallet?.id,
            suggestedCategoryId = matchedCategory?.id,
            suggestedCategoryName = matchedCategory?.name ?: categoryName,
            confidence = candidate.optDouble("confidence", 0.5),
        )
    }

    private fun toJson(dto: ExtractedInvestmentOperationDTO): JSONObject =
        JSONObject()
            .put("date", dto.date.toString())
            .put("kind", dto.kind.name.lowercase())
            .put("description", dto.description)
            .put("quantity", dto.quantity)
            .put("unitPrice", dto.unitPrice)
            .put("fees", dto.fees)
            .put("taxes", dto.taxes)
            .put("dividendAmount", dto.dividendAmount)
            .put("suggestedTickerId", dto.suggestedTickerId ?: JSONObject.NULL)
            .put("suggestedBondId", dto.suggestedBondId ?: JSONObject.NULL)
            .put("suggestedAssetLabel", dto.suggestedAssetLabel)
            .put("suggestedWalletId", dto.suggestedWalletId ?: JSONObject.NULL)
            .put("suggestedCategoryId", dto.suggestedCategoryId ?: JSONObject.NULL)
            .put("suggestedCategoryName", dto.suggestedCategoryName)
            .put("confidence", dto.confidence)
}
