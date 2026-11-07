/*
 * Filename: InvestmentExtensions.kt
 * Created on: March 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.extension

import org.moinex.model.dto.BondOperationStateDTO
import org.moinex.model.enums.AssetType
import org.moinex.model.enums.BondType
import org.moinex.model.enums.OperationType
import org.moinex.model.investment.Bond
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

/**
 * Bonds held in reais (CDB, LCI and LCA) have no unit price: what is held is an amount, saved as a quantity at a unit
 * price of one.
 */
fun Bond.isHeldInReais(): Boolean = type == BondType.CDB || type == BondType.LCI || type == BondType.LCA

/**
 * An operation that was entered as an amount: a bond held in reais at a unit price of one. Older operations of the same
 * bond types, saved with another unit price, keep showing their quantity and price.
 */
fun BondOperation.isAmountOnly(): Boolean = bond.isHeldInReais() && unitPrice.compareTo(BigDecimal.ONE) == 0

/**
 * Interest that leaves the bond with a sale: what the user received as profit plus the taxes and fees paid on it, never
 * negative. A purchase takes no interest out.
 */
fun BondOperation.realizedInterest(): BigDecimal =
    if (isSale()) netProfit.add(taxes).add(fees).max(BigDecimal.ZERO) else BigDecimal.ZERO

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
