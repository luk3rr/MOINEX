/*
 * Filename: BondAccrualCalculator.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

/**
 * The maths of the interest of a bond over a period, in the order the money works: each business day of the index
 * earns on the whole balance (principal plus the interest already earned), so interest compounds. No access to the
 * database.
 *
 * A purchase starts earning on the first business day after its date and a sale takes the money out after the
 * business day of its date, so the day of the purchase does not earn and the day of the sale does.
 */
object BondAccrualCalculator {
    const val BUSINESS_DAYS_PER_YEAR = 252
    const val MONTHS_PER_YEAR = 12
    private val MC = MathContext(30)
    private val HUNDRED = BigDecimal(100)

    data class IndexPoint(
        val date: LocalDate,
        val value: BigDecimal,
    )

    /**
     * A purchase ([amount] > 0) or a sale (< 0) of the bond. A [rate] replaces the rate of the bond for the money of
     * that purchase.
     */
    data class Flow(
        val date: LocalDate,
        val amount: BigDecimal,
        val rate: BigDecimal? = null,
    )

    /**
     * The share of the opening balance held at [rate], given as a weight (the principal bought at that rate).
     */
    data class OpeningLot(
        val weight: BigDecimal,
        val rate: BigDecimal? = null,
    )

    private class Lot(
        val rate: BigDecimal,
        var balance: BigDecimal,
    )

    /**
     * Factor of one row of the index for money that earns [rate] under [type]. For [InterestType.FLOATING] the rate is
     * a percentage of the index; for [InterestType.FLOATING_SPREAD] it is a yearly rate added to the index; for
     * [InterestType.FIXED] and [InterestType.ZERO_COUPON] it is a yearly rate and the index only gives the business
     * days. The CDI row is a daily rate, the Selic row a yearly rate and the IPCA row a monthly rate.
     */
    fun rowFactor(
        type: InterestType,
        index: InterestIndex,
        rate: BigDecimal,
        indexValue: BigDecimal,
    ): BigDecimal {
        val periodsPerYear = if (index == InterestIndex.IPCA) MONTHS_PER_YEAR else BUSINESS_DAYS_PER_YEAR

        return when (type) {
            InterestType.FIXED, InterestType.ZERO_COUPON ->
                root(
                    BigDecimal.ONE.add(rate.divide(HUNDRED, MC)),
                    periodsPerYear,
                )
            InterestType.FLOATING ->
                BigDecimal.ONE.add(
                    baseFactor(index, indexValue).subtract(BigDecimal.ONE).multiply(rate.divide(HUNDRED, MC)),
                )
            InterestType.FLOATING_SPREAD ->
                baseFactor(index, indexValue).multiply(
                    root(BigDecimal.ONE.add(rate.divide(HUNDRED, MC)), periodsPerYear),
                    MC,
                )
        }
    }

    private fun baseFactor(
        index: InterestIndex,
        indexValue: BigDecimal,
    ): BigDecimal {
        val rate = BigDecimal.ONE.add(indexValue.divide(HUNDRED, MC))

        return if (index == InterestIndex.SELIC) root(rate, BUSINESS_DAYS_PER_YEAR) else rate
    }

    private fun root(
        base: BigDecimal,
        periods: Int,
    ): BigDecimal = BigDecimal(Math.pow(base.toDouble(), 1.0 / periods), MC)

    /**
     * Interest earned over the business days in [points] by a balance of [opening], split in [openingLots], with the
     * purchases and sales in [flows]. The rows are the business days of the period and decide what counts.
     */
    fun periodInterest(
        type: InterestType,
        index: InterestIndex,
        bondRate: BigDecimal,
        opening: BigDecimal,
        openingLots: List<OpeningLot>,
        flows: List<Flow>,
        points: List<IndexPoint>,
    ): BigDecimal {
        val lots = startingLots(bondRate, opening, openingLots)
        val pending = flows.sortedBy { it.date }.toMutableList()
        var interest = BigDecimal.ZERO

        points.sortedBy { it.date }.forEach { point ->
            while (pending.isNotEmpty() && pending.first().date.isBefore(point.date)) {
                apply(pending.removeFirst(), bondRate, lots)
            }

            lots.forEach { lot ->
                val earned =
                    lot.balance.multiply(
                        rowFactor(type, index, lot.rate, point.value).subtract(BigDecimal.ONE),
                        MC,
                    )
                lot.balance = lot.balance.add(earned)
                interest = interest.add(earned)
            }
        }

        return interest
    }

    /**
     * What the money held in the bond is worth at the close of each business day in [points], from the first of
     * [flows]: the same earning rules as [periodInterest], with the purchases and sales of a day already counted in its
     * close.
     */
    fun dailyValues(
        type: InterestType,
        index: InterestIndex,
        bondRate: BigDecimal,
        flows: List<Flow>,
        points: List<IndexPoint>,
    ): List<IndexPoint> {
        val lots = mutableListOf<Lot>()
        val pending = flows.sortedBy { it.date }.toMutableList()

        return points.sortedBy { it.date }.map { point ->
            while (pending.isNotEmpty() && pending.first().date.isBefore(point.date)) {
                apply(pending.removeFirst(), bondRate, lots)
            }

            lots.forEach { lot ->
                lot.balance =
                    lot.balance.add(
                        lot.balance.multiply(
                            rowFactor(type, index, lot.rate, point.value).subtract(BigDecimal.ONE),
                            MC,
                        ),
                    )
            }

            val held = lots.fold(BigDecimal.ZERO) { acc, lot -> acc.add(lot.balance) }
            val sameDay =
                pending
                    .filter {
                        !it.date.isAfter(point.date)
                    }.fold(BigDecimal.ZERO) { acc, flow -> acc.add(flow.amount) }

            IndexPoint(point.date, held.add(sameDay).max(BigDecimal.ZERO))
        }
    }

    private fun startingLots(
        bondRate: BigDecimal,
        opening: BigDecimal,
        openingLots: List<OpeningLot>,
    ): MutableList<Lot> {
        if (opening.signum() <= 0) return mutableListOf()

        val totalWeight = openingLots.fold(BigDecimal.ZERO) { acc, lot -> acc.add(lot.weight) }

        if (totalWeight.signum() <= 0) return mutableListOf(Lot(bondRate, opening))

        return openingLots
            .filter { it.weight.signum() > 0 }
            .map { Lot(it.rate ?: bondRate, opening.multiply(it.weight).divide(totalWeight, MC)) }
            .toMutableList()
    }

    private fun apply(
        flow: Flow,
        bondRate: BigDecimal,
        lots: MutableList<Lot>,
    ) {
        if (flow.amount.signum() > 0) {
            lots.add(Lot(flow.rate ?: bondRate, flow.amount))
            return
        }

        val total = lots.fold(BigDecimal.ZERO) { acc, lot -> acc.add(lot.balance) }
        if (total.signum() <= 0) return

        val taken = flow.amount.negate().min(total)
        val kept = BigDecimal.ONE.subtract(taken.divide(total, MC))
        lots.forEach { it.balance = it.balance.multiply(kept, MC) }
    }
}
