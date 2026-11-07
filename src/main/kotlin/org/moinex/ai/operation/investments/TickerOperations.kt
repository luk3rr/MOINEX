/*
 * Filename: TickerOperations.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation.investments

import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.moinex.ai.operation.ApplyResult
import org.moinex.ai.operation.BaseOperation
import org.moinex.ai.operation.DraftField
import org.moinex.ai.operation.DraftFieldType
import org.moinex.ai.operation.DraftValues
import org.moinex.ai.operation.EntityCatalog
import org.moinex.ai.operation.OperationDomain
import org.moinex.ai.operation.OperationRisk
import org.moinex.ai.operation.Preview
import org.moinex.ai.operation.ValidationResult
import org.moinex.common.util.APIUtils
import org.moinex.model.enums.AssetType
import org.moinex.model.investment.Ticker
import org.moinex.service.investment.TickerPriceHistoryService
import org.moinex.service.investment.TickerService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime

private val TICKER_TYPES = AssetType.entries.filter { it != AssetType.BOND }

private fun String?.toTickerType(): AssetType? = TICKER_TYPES.firstOrNull { it.name.equals(this, ignoreCase = true) }

/**
 * A ticker to register or, when [tickerId] is set, to rename or retype; its quantity and prices are never touched.
 * [assetType] is null while the model's type matched none.
 */
data class TickerPayload(
    val tickerId: Int?,
    val symbol: String,
    val name: String,
    val assetType: AssetType?,
    val currentPrice: BigDecimal,
    val before: String? = null,
)

/** [tickerId] is null while the model's asset matched no ticker; [rebuild] throws the stored history away first. */
data class RefreshTickerPayload(
    val tickerId: Int?,
    val label: String,
    val rebuild: Boolean,
)

private val TICKER_DRAFT_SPEC =
    listOf(
        DraftField("symbol", "aidraft.field.symbol", DraftFieldType.TEXT),
        DraftField("name", "aidraft.field.name", DraftFieldType.TEXT),
        DraftField("assetType", "aidraft.field.assetType", DraftFieldType.TEXT),
        DraftField("currentPrice", "aidraft.field.unitPrice", DraftFieldType.DECIMAL, required = false),
    )

/** Shared by the ticker creation and change operations. */
abstract class TickerManagementOperation : BaseOperation<TickerPayload>() {
    override val domain = OperationDomain.INVESTMENTS

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf(
            "symbol" to property("string", "For create_ticker/update_ticker: the market symbol, e.g. PETR4.SA."),
            "assetType" to
                property("string", "For create_ticker/update_ticker: the kind of asset.")
                    .put("enum", JSONArray(TICKER_TYPES.map { it.name.lowercase() })),
        )

    override fun validate(payload: TickerPayload): ValidationResult =
        when {
            payload.symbol.isBlank() -> ValidationResult.error("symbol is required")
            payload.name.isBlank() -> ValidationResult.error("name is required")
            payload.assetType == null ->
                ValidationResult.error(
                    "assetType must be one of ${TICKER_TYPES.map { it.name.lowercase() }}",
                )
            payload.currentPrice.signum() < 0 -> ValidationResult.error("price cannot be negative")
            else -> ValidationResult.OK
        }

    override fun summary(payload: TickerPayload) =
        "${payload.symbol}, ${payload.name}, ${payload.assetType?.name?.lowercase() ?: "?"}"

    override fun draftSpec() = TICKER_DRAFT_SPEC

    override fun toDraft(payload: TickerPayload) =
        DraftValues(
            name,
            mapOf(
                "tickerId" to payload.tickerId,
                "symbol" to payload.symbol,
                "name" to payload.name,
                "assetType" to payload.assetType?.name?.lowercase(),
                "currentPrice" to payload.currentPrice,
            ),
        )

    override fun fromDraft(draft: DraftValues): TickerPayload? {
        val fields = draft.fields
        return TickerPayload(
            tickerId = fields["tickerId"].asInt(),
            symbol = (fields["symbol"] as? String)?.trim() ?: return null,
            name = (fields["name"] as? String)?.trim() ?: return null,
            assetType = (fields["assetType"] as? String).toTickerType(),
            currentPrice = fields["currentPrice"].asDecimal() ?: BigDecimal.ZERO,
        )
    }
}

@Component
class CreateTickerOperation(
    private val tickerService: TickerService,
) : TickerManagementOperation() {
    override val name = "create_ticker"
    override val risk = OperationRisk.LOW

    override fun modelRequired() = listOf("symbol", "name", "assetType")

    override fun modelDescription() =
        "create_ticker: register a new stock, FII/REIT, fund, ETF or crypto (symbol, name, assetType, optional " +
            "amount as the current price). It holds no quantity until a purchase is recorded."

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ) = TickerPayload(
        tickerId = null,
        symbol = arguments.getString("symbol").trim(),
        name = arguments.getString("name").trim(),
        assetType = arguments.optString("assetType").toTickerType(),
        currentPrice = if (arguments.has("amount")) arguments.getBigDecimal("amount") else BigDecimal.ZERO,
    )

    override fun validate(payload: TickerPayload): ValidationResult {
        val base = super.validate(payload)
        return if (base.isValid && tickerService.getAllTickers().any { it.symbol.equals(payload.symbol, true) }) {
            ValidationResult.error("ticker ${payload.symbol} already exists")
        } else {
            base
        }
    }

    override fun execute(payload: TickerPayload): ApplyResult.Success {
        val now = LocalDateTime.now()
        val id =
            tickerService.createTicker(
                Ticker(
                    createdAt = now,
                    lastUpdate = now,
                    name = payload.name,
                    symbol = payload.symbol,
                    type = payload.assetType!!,
                    currentQuantity = BigDecimal.ZERO,
                    currentUnitValue = payload.currentPrice,
                    averageUnitValue = BigDecimal.ZERO,
                ),
            )
        return ApplyResult.Success(listOf(id), summary(payload))
    }
}

@Component
class UpdateTickerOperation(
    private val tickerService: TickerService,
) : TickerManagementOperation() {
    override val name = "update_ticker"
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("assetName")

    override fun modelDescription() =
        "update_ticker: rename or retype an existing ticker (assetName is the current symbol or name). Send only " +
            "the fields to change; quantity and prices are never changed."

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): TickerPayload {
        val given = arguments.getString("assetName")
        val ticker = catalog.ticker(given) ?: error("ticker '$given' not found")
        val stored = TickerPayload(ticker.id, ticker.symbol, ticker.name, ticker.type, ticker.currentUnitValue)
        return stored.copy(
            symbol = arguments.optString("symbol").trim().ifBlank { stored.symbol },
            name = arguments.optString("name").trim().ifBlank { stored.name },
            assetType = arguments.optString("assetType").takeIf { it.isNotBlank() }?.toTickerType() ?: stored.assetType,
            before = summary(stored),
        )
    }

    override fun preview(payload: TickerPayload) = payload.before?.let { Preview(listOf(it, "→ " + summary(payload))) }

    override fun execute(payload: TickerPayload): ApplyResult.Success {
        val stored = tickerService.getAllTickers().first { it.id == payload.tickerId }
        tickerService.updateTicker(
            Ticker(
                id = stored.id,
                createdAt = stored.createdAt,
                lastUpdate = stored.lastUpdate,
                name = payload.name,
                symbol = payload.symbol,
                type = payload.assetType!!,
                currentQuantity = stored.currentQuantity,
                currentUnitValue = stored.currentUnitValue,
                averageUnitValue = stored.averageUnitValue,
            ),
        )
        return ApplyResult.Success(listOf(payload.tickerId!!), summary(payload))
    }
}

@Component
class RefreshTickerDataOperation(
    private val tickerService: TickerService,
    private val priceHistoryService: TickerPriceHistoryService,
) : BaseOperation<RefreshTickerPayload>() {
    override val name = "refresh_ticker_data"
    override val domain = OperationDomain.INVESTMENTS
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("assetName")

    override fun modelDescription() =
        "refresh_ticker_data: fetch the current price of one existing asset (assetName) and sync its history; " +
            "set rebuild true only when the user asks to reset and fetch the history again."

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf("rebuild" to property("boolean", "Only for refresh_ticker_data: throw the stored history away first."))

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): RefreshTickerPayload {
        val given = arguments.getString("assetName")
        val ticker = catalog.ticker(given)
        return RefreshTickerPayload(ticker?.id, ticker?.symbol ?: given, arguments.optBoolean("rebuild", false))
    }

    override fun validate(payload: RefreshTickerPayload): ValidationResult =
        if (payload.tickerId ==
            null
        ) {
            ValidationResult.error("ticker '${payload.label}' not found")
        } else {
            ValidationResult.OK
        }

    override fun summary(payload: RefreshTickerPayload) = payload.label + if (payload.rebuild) " (rebuild)" else ""

    override fun execute(payload: RefreshTickerPayload): ApplyResult.Success {
        val ticker = tickerService.getAllNonArchivedTickers().first { it.id == payload.tickerId }
        runBlocking {
            APIUtils.withoutYahooCooldown {
                check(tickerService.updateTickersPriceFromApi(listOf(ticker)).isEmpty()) {
                    "could not fetch the price of ${ticker.symbol}"
                }
                if (payload.rebuild) {
                    priceHistoryService.rebuildPriceHistory(
                        ticker,
                    )
                } else {
                    priceHistoryService.syncDaily(ticker)
                }
            }
        }
        return ApplyResult.Success(listOf(payload.tickerId!!), summary(payload))
    }
}
