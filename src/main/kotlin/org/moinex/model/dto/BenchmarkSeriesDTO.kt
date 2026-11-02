/*
 * Filename: BenchmarkSeriesDTO.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.moinex.model.investment.Ticker
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

enum class PerformancePeriod(
    val months: Int,
) {
    M3(3),
    M6(6),
    M12(12),
}

/**
 * Accumulated return, in percent, since the first day of the period.
 */
data class SeriesPointDTO(
    val date: LocalDate,
    val pct: BigDecimal,
)

/**
 * The variable income portfolio and the benchmarks on the same days. A benchmark with no data in the period has an empty
 * list.
 */
data class BenchmarkSeriesDTO(
    val portfolio: List<SeriesPointDTO>,
    val cdi: List<SeriesPointDTO>,
    val ibovespa: List<SeriesPointDTO>,
    val ipca: List<SeriesPointDTO>,
) {
    fun isEmpty(): Boolean = portfolio.size < 2
}

/**
 * How much a ticker added to (or took from) the portfolio, in money, over a period: its change in value, discounting
 * what was bought or sold, plus the dividends it paid.
 */
data class ContributionDTO(
    val ticker: Ticker,
    val amount: BigDecimal,
)

/**
 * Dividends of one ticker, one value per month of the panel (zero when it paid nothing). The tickers beyond the ones
 * shown are added up in a series with [symbol] null.
 */
data class DividendSeriesDTO(
    val symbol: String?,
    val values: List<BigDecimal>,
)

/**
 * The dividends of the last twelve months for the panel: [months] oldest first, the biggest payers as [series], and the
 * numbers under them. [projection12m] is an estimate: what was received in the last twelve months, repeated.
 */
data class DividendPanelDTO(
    val months: List<YearMonth>,
    val series: List<DividendSeriesDTO>,
    val total12m: BigDecimal,
    val monthlyAverage: BigDecimal,
    val projection12m: BigDecimal,
)
