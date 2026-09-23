package org.moinex.common.extension

import org.moinex.model.dto.BondOperationStateDTO
import org.moinex.model.enums.AssetType
import org.moinex.model.enums.OperationType
import org.moinex.model.investment.BondOperation
import org.moinex.model.investment.FundamentalAnalysis
import org.moinex.model.investment.Ticker
import org.moinex.service.investment.FundamentalAnalysisService.Companion.CACHE_VALIDITY_HOURS
import org.moinex.service.investment.FundamentalAnalysisService.Companion.RECOMMENDED_UPDATE_HOURS
import org.moinex.service.investment.FundamentalAnalysisService.Companion.VALID_TICKER_TYPES
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate
import java.time.LocalDateTime

fun List<BondOperation>.operationsUntil(date: LocalDate) =
    this@operationsUntil.asSequence().takeWhile {
        it.localDate <=
            date
    }

/**
 * Cost basis of the position held at [date], using the average-cost method: purchases add their total value,
 * and sales remove the average cost of the units sold.
 */
fun List<BondOperation>.calculateInvestedAmountUntil(date: LocalDate): BigDecimal =
    operationsUntil(date)
        .sortedBy { it.localDate }
        .fold(BigDecimal.ZERO to BigDecimal.ZERO) { (quantity, cost), operation ->
            when (operation.operationType) {
                OperationType.BUY -> quantity.add(operation.quantity) to cost.add(operation.totalValue)
                OperationType.SELL -> {
                    val remainingQuantity = quantity.subtract(operation.quantity).max(BigDecimal.ZERO)
                    val remainingCost =
                        when {
                            quantity <= BigDecimal.ZERO -> BigDecimal.ZERO
                            else -> cost.multiply(remainingQuantity).divide(quantity, MathContext.DECIMAL128)
                        }
                    remainingQuantity to remainingCost
                }
            }
        }.second

fun List<BondOperation>.calculateOperationStateUntil(date: LocalDate): BondOperationStateDTO =
    this
        .operationsUntil(date)
        .sortedBy { it.localDate }
        .fold(BondOperationStateDTO(BigDecimal.ZERO, BigDecimal.ZERO, null)) { state, operation ->
            when (operation.operationType) {
                OperationType.BUY ->
                    BondOperationStateDTO(
                        quantity = state.quantity + operation.quantity,
                        lastSpread = operation.spread ?: state.lastSpread,
                        lastBuyPrice = operation.unitPrice,
                    )
                OperationType.SELL ->
                    state.copy(
                        quantity = state.quantity - operation.quantity,
                    )
            }
        }

fun List<BondOperation>.calculateQuantityUntil(date: LocalDate): BigDecimal =
    calculateOperationStateUntil(date).quantity

fun List<BondOperation>.getLastBuyPrice(): BigDecimal? =
    filter { it.isPurchase() }
        .maxByOrNull { it.localDate }
        ?.unitPrice

fun Ticker.isCryptocurrency(): Boolean = this.type == AssetType.CRYPTOCURRENCY

fun Ticker.isValidForFundamentalAnalysis(): Boolean = type in VALID_TICKER_TYPES

fun FundamentalAnalysis?.isCacheExpired(now: LocalDateTime): Boolean {
    if (this == null) return true
    return lastUpdate.plusHours(CACHE_VALIDITY_HOURS.toLong()).isBefore(now)
}

fun FundamentalAnalysis?.isUpdateRecommended(now: LocalDateTime): Boolean {
    if (this == null) return true
    return lastUpdate.plusHours(RECOMMENDED_UPDATE_HOURS.toLong()).isBefore(now)
}
