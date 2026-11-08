/*
 * Filename: PortfolioPerformanceCalculator.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import org.moinex.model.dto.ContributionDTO
import org.moinex.model.dto.DailyPricePointDTO
import org.moinex.model.dto.SeriesPointDTO
import org.moinex.model.investment.Dividend
import org.moinex.model.investment.Ticker
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/**
 * The maths of the performance chart: the return of the portfolio and of each benchmark as accumulated percentages on
 * the same days. No access to the database.
 */
object PortfolioPerformanceCalculator {
    private const val PCT_SCALE = 2
    private const val INDEX_SCALE = 10
    private val HUNDRED = BigDecimal(100)
    private val MC = MathContext(INDEX_SCALE + 6)

    /**
     * A buy ([signedQuantity] > 0) or a sell (< 0) of a ticker.
     */
    data class Trade(
        val date: LocalDate,
        val signedQuantity: BigDecimal,
        val price: BigDecimal,
    )

    /**
     * Money received from holding a ticker (a dividend), on the [date] it was paid.
     */
    data class Payout(
        val date: LocalDate,
        val amount: BigDecimal,
    )

    /**
     * A bond or any holding valued directly in money: its value at the close of each day it has one ([values]) and the
     * money put in (positive) or taken out (negative) on each date ([flows]). Money put in does not earn on the day it
     * goes in, so it is not part of the capital at work that day.
     */
    data class FixedIncomePosition(
        val values: List<Pair<LocalDate, BigDecimal>>,
        val flows: List<Pair<LocalDate, BigDecimal>>,
    )

    data class Position(
        val ticker: Ticker,
        val trades: List<Trade>,
        val prices: List<DailyPricePointDTO>,
        val payouts: List<Payout> = emptyList(),
    )

    /**
     * Return of the portfolio between [start] and [end], compounded day by day. The quantity held on a day is the current
     * quantity minus the trades made after it. A day's return is its profit (the change in value, less the money put
     * in) over the capital at work that day: the value of the day before plus what was bought. Dividing by the capital
     * at work, instead of by the previous value alone, keeps a big purchase on a small portfolio from turning the
     * difference between the price paid and the close into a huge percentage. Days run over the dates that have prices.
     * Positions without prices are left out.
     */
    fun portfolioReturn(
        positions: List<Position>,
        start: LocalDate,
        end: LocalDate,
        includeDividends: Boolean = false,
        fixedIncome: List<FixedIncomePosition> = emptyList(),
    ): List<SeriesPointDTO> {
        val priced = positions.filter { it.prices.isNotEmpty() }
        val days =
            (
                priced.flatMap { it.prices.map { point -> point.date } } +
                    fixedIncome.flatMap { it.values.map { v -> v.first } }
            ).filter { !it.isBefore(start) && !it.isAfter(end) }
                .distinct()
                .sorted()
        if (days.size < 2) return emptyList()

        val effective = withEffectiveDates(priced, days)
        val effectiveFixed = fixedIncome.map { fixed -> fixed.copy(flows = fixed.flows.map { moveToDay(it, days) }) }
        var index = BigDecimal.ONE
        var previousValue: BigDecimal? = null
        val points = mutableListOf<SeriesPointDTO>()

        days.forEach { day ->
            val value =
                effective
                    .fold(BigDecimal.ZERO) { total, position -> total.add(valueOn(position, day)) }
                    .add(effectiveFixed.fold(BigDecimal.ZERO) { total, fixed -> total.add(fixedValueOn(fixed, day)) })
            val flow =
                effective
                    .fold(BigDecimal.ZERO) { total, position -> total.add(flowOn(position, day)) }
                    .add(effectiveFixed.fold(BigDecimal.ZERO) { total, fixed -> total.add(fixedFlowOn(fixed, day)) })

            val paid = if (includeDividends) payoutsOn(effective, day, days) else BigDecimal.ZERO

            val base = previousValue?.add(boughtOn(effective, day))
            if (base != null && base.signum() > 0) {
                val profit = value.subtract(previousValue).subtract(flow).add(paid)
                val dayReturn = BigDecimal.ONE.add(profit.divide(base, MC), MC)
                index = index.multiply(dayReturn, MC)
            }
            previousValue = value
            points.add(SeriesPointDTO(day, toPct(index)))
        }

        return points
    }

    /**
     * Profit of each position between the first and the last day that have prices in [start]..[end]: the value at the
     * end, less the value at the start, less the money put in after the start (sales count as money taken out), plus
     * the [dividends] paid after the start. Positions without prices are left out, and the biggest profit comes first.
     */
    fun contributions(
        positions: List<Position>,
        dividends: List<Dividend>,
        start: LocalDate,
        end: LocalDate,
    ): List<ContributionDTO> {
        val priced = positions.filter { it.prices.isNotEmpty() }
        val days =
            priced
                .flatMap { it.prices.map { point -> point.date } }
                .filter { !it.isBefore(start) && !it.isAfter(end) }
                .distinct()
                .sorted()
        if (days.size < 2) return emptyList()

        val first = days.first()
        val last = days.last()

        return withEffectiveDates(priced, days)
            .map { position ->
                val flows =
                    position.trades
                        .filter { it.date.isAfter(first) && !it.date.isAfter(last) }
                        .fold(BigDecimal.ZERO) { acc, trade -> acc.add(trade.signedQuantity.multiply(trade.price)) }
                val paid =
                    dividends
                        .filter { it.ticker.id == position.ticker.id }
                        .mapNotNull { it.walletTransaction }
                        .filter { it.date.toLocalDate().isAfter(first) && !it.date.toLocalDate().isAfter(last) }
                        .fold(BigDecimal.ZERO) { acc, transaction -> acc.add(transaction.amount) }

                ContributionDTO(
                    position.ticker,
                    valueOn(position, last).subtract(valueOn(position, first)).subtract(flows).add(paid),
                )
            }.sortedByDescending { it.amount }
    }

    /**
     * Accumulated return of an index given by its daily [rates] in percent (like the CDI), at each of [days]. Rates
     * of the first day are not counted: the return starts there.
     */
    fun cumulativeFromDailyRates(
        rates: List<Pair<LocalDate, BigDecimal>>,
        days: List<LocalDate>,
    ): List<SeriesPointDTO> {
        if (rates.isEmpty() || days.isEmpty()) return emptyList()

        val sorted = rates.sortedBy { it.first }
        var index = BigDecimal.ONE
        var cursor = 0

        return days.mapIndexed { position, day ->
            while (cursor < sorted.size && !sorted[cursor].first.isAfter(day)) {
                if (position > 0 && sorted[cursor].first.isAfter(days.first())) {
                    index = index.multiply(BigDecimal.ONE.add(sorted[cursor].second.divide(HUNDRED, MC), MC), MC)
                }
                cursor++
            }
            SeriesPointDTO(day, toPct(index))
        }
    }

    /**
     * Accumulated return of an index given by monthly [rates] in percent dated on the first day of the month (like the
     * IPCA). The rate of a month counts once that month is over, and months that started before the first day do not
     * count.
     */
    fun cumulativeFromMonthlyRates(
        rates: List<Pair<LocalDate, BigDecimal>>,
        days: List<LocalDate>,
    ): List<SeriesPointDTO> {
        if (rates.isEmpty() || days.isEmpty()) return emptyList()

        val first = days.first()

        return days.map { day ->
            val index =
                rates
                    .filter { (date, _) ->
                        val month = YearMonth.from(date)
                        !month.atDay(1).isBefore(first) && !month.atEndOfMonth().isAfter(day)
                    }.fold(BigDecimal.ONE) { acc, (_, rate) ->
                        acc.multiply(BigDecimal.ONE.add(rate.divide(HUNDRED, MC), MC), MC)
                    }
            SeriesPointDTO(day, toPct(index))
        }
    }

    /**
     * Return of an index given by its [levels] (like the Ibovespa), from the level on the first of [days], forward
     * filled over days without a quote.
     */
    fun cumulativeFromLevels(
        levels: List<Pair<LocalDate, BigDecimal>>,
        days: List<LocalDate>,
    ): List<SeriesPointDTO> {
        if (levels.isEmpty() || days.isEmpty()) return emptyList()

        val sorted = levels.sortedBy { it.first }
        val base = levelOn(sorted, days.first()) ?: return emptyList()
        if (base.signum() <= 0) return emptyList()

        return days.mapNotNull { day ->
            levelOn(sorted, day)?.let { SeriesPointDTO(day, toPct(it.divide(base, MC))) }
        }
    }

    private fun levelOn(
        sorted: List<Pair<LocalDate, BigDecimal>>,
        day: LocalDate,
    ): BigDecimal? = sorted.lastOrNull { !it.first.isAfter(day) }?.second ?: sorted.firstOrNull()?.second

    /**
     * Moves each trade to the first of [days] on or after its date, so that a buy or sell made on a day without prices
     * (a weekend or holiday) is counted when the market reopens. A trade after the last day is moved one day past it:
     * it still counts as "later" for every day but is the money flow of none.
     */
    private fun withEffectiveDates(
        positions: List<Position>,
        days: List<LocalDate>,
    ): List<Position> {
        val afterLast = days.last().plusDays(1)

        return positions.map { position ->
            position.copy(
                trades =
                    position.trades.map { trade ->
                        trade.copy(date = days.firstOrNull { !it.isBefore(trade.date) } ?: afterLast)
                    },
            )
        }
    }

    /**
     * Dividends paid on [day], or since the previous day with prices when [day] follows a weekend or holiday.
     */
    private fun payoutsOn(
        positions: List<Position>,
        day: LocalDate,
        days: List<LocalDate>,
    ): BigDecimal {
        val previousDay = days.lastOrNull { it.isBefore(day) }

        return positions.fold(BigDecimal.ZERO) { total, position ->
            total.add(
                position.payouts
                    .filter { (previousDay == null || it.date.isAfter(previousDay)) && !it.date.isAfter(day) }
                    .fold(BigDecimal.ZERO) { acc, payout -> acc.add(payout.amount) },
            )
        }
    }

    private fun moveToDay(
        flow: Pair<LocalDate, BigDecimal>,
        days: List<LocalDate>,
    ): Pair<LocalDate, BigDecimal> =
        (days.firstOrNull { !it.isBefore(flow.first) } ?: days.last().plusDays(1)) to flow.second

    private fun fixedValueOn(
        fixed: FixedIncomePosition,
        day: LocalDate,
    ): BigDecimal = fixed.values.lastOrNull { !it.first.isAfter(day) }?.second ?: BigDecimal.ZERO

    private fun fixedFlowOn(
        fixed: FixedIncomePosition,
        day: LocalDate,
    ): BigDecimal = fixed.flows.filter { it.first == day }.fold(BigDecimal.ZERO) { acc, flow -> acc.add(flow.second) }

    private fun boughtOn(
        positions: List<Position>,
        day: LocalDate,
    ): BigDecimal =
        positions.fold(BigDecimal.ZERO) { total, position ->
            total.add(
                position.trades
                    .filter { it.date == day && it.signedQuantity.signum() > 0 }
                    .fold(BigDecimal.ZERO) { acc, trade -> acc.add(trade.signedQuantity.multiply(trade.price)) },
            )
        }

    private fun valueOn(
        position: Position,
        day: LocalDate,
    ): BigDecimal {
        val quantity =
            position.ticker.currentQuantity.subtract(
                position.trades.filter { it.date.isAfter(day) }.fold(BigDecimal.ZERO) { acc, trade ->
                    acc.add(trade.signedQuantity)
                },
            )
        val price =
            position.prices.lastOrNull { !it.date.isAfter(day) }?.price ?: position.prices.first().price

        return quantity.max(BigDecimal.ZERO).multiply(price)
    }

    private fun flowOn(
        position: Position,
        day: LocalDate,
    ): BigDecimal =
        position.trades
            .filter { it.date == day }
            .fold(BigDecimal.ZERO) { acc, trade -> acc.add(trade.signedQuantity.multiply(trade.price)) }

    private fun toPct(index: BigDecimal): BigDecimal =
        index.subtract(BigDecimal.ONE).multiply(HUNDRED).setScale(PCT_SCALE, RoundingMode.HALF_UP)
}
