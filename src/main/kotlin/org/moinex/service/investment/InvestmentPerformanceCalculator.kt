/*
 * Filename: InvestmentPerformanceCalculator.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate
import java.time.YearMonth

/**
 * The maths of the monthly performance of the investments, with no access to the database. The gain of a month is
 * what the portfolio is worth at its end, less what it was worth at the start of the month, less the money put in
 * during it, plus what it paid out. Realised and unrealised results are never added twice because both are inside the
 * change of value.
 */
object InvestmentPerformanceCalculator {
    private val MC = MathContext.DECIMAL128

    data class Trade(
        val date: LocalDate,
        val quantity: BigDecimal,
        val price: BigDecimal,
    )

    data class Payout(
        val date: LocalDate,
        val amount: BigDecimal,
    )

    /**
     * What is known of a ticker: the units held before the first registered trade, the cost assumed for them, the
     * trades and payouts, and the closing price on or before a date (null when unknown).
     */
    data class TickerFacts(
        val openingQuantity: BigDecimal,
        val openingUnitCost: BigDecimal,
        val purchases: List<Trade>,
        val sales: List<Trade>,
        val payouts: List<Payout>,
        val priceAt: (LocalDate) -> BigDecimal?,
    )

    /**
     * What is known of a bond: the cost of what it holds on a date, the interest still held in it at the end of a
     * month, and its purchases (positive) and sales (negative, with the interest taken out in them) by date.
     */
    data class BondFacts(
        val investedOn: (LocalDate) -> BigDecimal,
        val interestHeldAt: (YearMonth) -> BigDecimal,
        val flows: List<Payout>,
    )

    data class MonthResult(
        val invested: BigDecimal = BigDecimal.ZERO,
        val value: BigDecimal = BigDecimal.ZERO,
        val gain: BigDecimal = BigDecimal.ZERO,
    ) {
        operator fun plus(other: MonthResult) =
            MonthResult(invested.add(other.invested), value.add(other.value), gain.add(other.gain))
    }

    fun tickerMonths(
        facts: TickerFacts,
        months: List<YearMonth>,
        today: LocalDate,
    ): Map<YearMonth, MonthResult> =
        months.associateWith { month ->
            val end = minOf(month.atEndOfMonth(), today)
            val previousEnd = month.minusMonths(1).atEndOfMonth()

            val valueEnd = valueOn(facts, end)
            val valuePrevious = valueOn(facts, previousEnd, listOf(previousEnd.plusDays(1), end))
            val flows = flowsBetween(facts, previousEnd, end)
            val payouts =
                facts.payouts
                    .filter { it.date.isAfter(previousEnd) && !it.date.isAfter(end) }
                    .fold(BigDecimal.ZERO) { acc, payout -> acc.add(payout.amount) }

            MonthResult(
                invested = costOn(facts, end),
                value = valueEnd,
                gain = valueEnd.subtract(valuePrevious).subtract(flows).add(payouts),
            )
        }

    fun bondMonths(
        facts: BondFacts,
        months: List<YearMonth>,
        today: LocalDate,
    ): Map<YearMonth, MonthResult> =
        months.associateWith { month ->
            val end = minOf(month.atEndOfMonth(), today)
            val previousEnd = month.minusMonths(1).atEndOfMonth()

            val invested = facts.investedOn(end)
            val value = invested.add(facts.interestHeldAt(month))
            val valuePrevious =
                facts.investedOn(previousEnd).add(facts.interestHeldAt(month.minusMonths(1)))
            val flows =
                facts.flows
                    .filter { it.date.isAfter(previousEnd) && !it.date.isAfter(end) }
                    .fold(BigDecimal.ZERO) { acc, flow -> acc.add(flow.amount) }

            MonthResult(invested, value, value.subtract(valuePrevious).subtract(flows))
        }

    fun quantityOn(
        facts: TickerFacts,
        date: LocalDate,
    ): BigDecimal =
        facts.openingQuantity
            .add(
                facts.purchases
                    .filter {
                        !it.date.isAfter(
                            date,
                        )
                    }.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.quantity) },
            ).subtract(
                facts.sales.filter { !it.date.isAfter(date) }.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.quantity) },
            ).max(BigDecimal.ZERO)

    private fun valueOn(
        facts: TickerFacts,
        date: LocalDate,
        fallbackDates: List<LocalDate> = emptyList(),
    ): BigDecimal {
        val quantity = quantityOn(facts, date)
        if (quantity.signum() <= 0) return BigDecimal.ZERO

        val price =
            facts.priceAt(date)
                ?: fallbackDates.firstNotNullOfOrNull { facts.priceAt(it) }
                ?: lastTradePrice(facts, date)

        return quantity.multiply(price ?: BigDecimal.ZERO)
    }

    private fun lastTradePrice(
        facts: TickerFacts,
        date: LocalDate,
    ): BigDecimal? =
        (facts.purchases + facts.sales)
            .filter { !it.date.isAfter(date) }
            .maxByOrNull { it.date }
            ?.price

    private fun flowsBetween(
        facts: TickerFacts,
        after: LocalDate,
        until: LocalDate,
    ): BigDecimal {
        val bought =
            facts.purchases
                .filter { it.date.isAfter(after) && !it.date.isAfter(until) }
                .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.quantity.multiply(t.price)) }
        val sold =
            facts.sales
                .filter { it.date.isAfter(after) && !it.date.isAfter(until) }
                .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.quantity.multiply(t.price)) }

        return bought.subtract(sold)
    }

    /**
     * Cost of the units held on [date] by the average-cost method: a purchase adds its value and a sale removes the
     * average cost of the units sold. The opening units cost [TickerFacts.openingUnitCost] each.
     */
    private fun costOn(
        facts: TickerFacts,
        date: LocalDate,
    ): BigDecimal {
        var quantity = facts.openingQuantity
        var cost = facts.openingQuantity.multiply(facts.openingUnitCost)

        (facts.purchases.map { it.date to it } + facts.sales.map { it.date to it })
            .filter { !it.first.isAfter(date) }
            .sortedBy { it.first }
            .forEach { (_, trade) ->
                if (facts.purchases.contains(trade)) {
                    quantity = quantity.add(trade.quantity)
                    cost = cost.add(trade.quantity.multiply(trade.price))
                } else if (quantity.signum() > 0) {
                    val sold = trade.quantity.min(quantity)
                    cost = cost.subtract(cost.multiply(sold).divide(quantity, MC))
                    quantity = quantity.subtract(sold)
                }
            }

        return if (quantity.signum() <= 0) BigDecimal.ZERO else cost
    }
}
