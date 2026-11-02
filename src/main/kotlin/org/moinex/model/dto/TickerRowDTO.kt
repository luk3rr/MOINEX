/*
 * Filename: TickerRowDTO.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.moinex.model.investment.Ticker
import java.math.BigDecimal
import java.time.LocalDate

/**
 * One closing price of a ticker on a trading day, read in bulk for the whole portfolio.
 */
data class DailyPricePointDTO(
    val tickerId: Int,
    val date: LocalDate,
    val price: BigDecimal,
)

enum class TickerSort {
    NAME,
    RETURN,
    WEIGHT,
    DAY_CHANGE,
}

/**
 * What the stocks and funds table shows for a ticker. [returnPct] and [dyOnCostPct] are null when the position has no
 * cost, [dayChangePct] when there is no previous close, and [sparkline] holds the closing prices of the last months,
 * oldest first.
 */
data class TickerRowDTO(
    val ticker: Ticker,
    val value: BigDecimal,
    val profitLoss: BigDecimal,
    val returnPct: BigDecimal?,
    val weightPct: BigDecimal,
    val dyOnCostPct: BigDecimal?,
    val dayChangePct: BigDecimal?,
    val sparkline: List<BigDecimal> = emptyList(),
)

/**
 * Everything the stocks and funds screen shows, read once: the rows of all non-archived tickers (not filtered) and the
 * total of dividends ever received.
 */
data class PortfolioSnapshotDTO(
    val rows: List<TickerRowDTO>,
    val totalDividends: BigDecimal,
)

enum class TradeSide {
    BUY,
    SELL,
}

data class TradeMarkerDTO(
    val date: LocalDate,
    val side: TradeSide,
    val quantity: BigDecimal,
    val price: BigDecimal,
)

/**
 * What the ticker detail dialog draws: the daily closes of the chosen period, the user's buys and sells in it, and the
 * numbers of the current position.
 */
data class TickerDetailDTO(
    val ticker: Ticker,
    val prices: List<DailyPricePointDTO>,
    val trades: List<TradeMarkerDTO>,
    val averagePrice: BigDecimal,
    val quantity: BigDecimal,
    val profitLoss: BigDecimal,
    val returnPct: BigDecimal?,
    val dayChangePct: BigDecimal?,
    val position: TickerPositionDTO,
    val income: TickerIncomeDTO,
)

/**
 * The position of the user in one ticker and where its price stands in the last twelve months. [highest12m] and
 * [lowest12m] are null without daily prices.
 */
data class TickerPositionDTO(
    val value: BigDecimal,
    val cost: BigDecimal,
    val weightPct: BigDecimal,
    val highest12m: BigDecimal?,
    val lowest12m: BigDecimal?,
    val distanceFromHighestPct: BigDecimal?,
)

/**
 * The dividends the user received from one ticker. [dyOnCostPct] is the twelve months over the current cost and is null
 * when the position has no cost.
 */
data class TickerIncomeDTO(
    val panel: DividendPanelDTO,
    val total12m: BigDecimal,
    val totalReceived: BigDecimal,
    val lastAmount: BigDecimal?,
    val lastDate: LocalDate?,
    val dyOnCostPct: BigDecimal?,
) {
    fun hasDividends(): Boolean = totalReceived.signum() > 0
}
