/*
 * Filename: BondTaxEstimator.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import org.moinex.model.enums.BondType
import org.moinex.model.investment.BondOperation
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Estimate of the income tax on the interest of a bond, by the regressive table. It is an estimate for the user to
 * compare gross and net values, not a tax statement.
 */
object BondTaxEstimator {
    private const val FIRST_BRACKET_DAYS = 180
    private const val SECOND_BRACKET_DAYS = 360
    private const val THIRD_BRACKET_DAYS = 720
    private val FIRST_RATE = BigDecimal("0.225")
    private val SECOND_RATE = BigDecimal("0.20")
    private val THIRD_RATE = BigDecimal("0.175")
    private val LAST_RATE = BigDecimal("0.15")
    private val MC = MathContext(20)

    /**
     * Money kept in the bond that was bought [ageDays] days ago.
     */
    data class Lot(
        val principal: BigDecimal,
        val ageDays: Long,
    )

    /**
     * [applicable] is false when the bond type has no estimate (the tax is then zero and the net is the gross).
     */
    data class Estimate(
        val tax: BigDecimal,
        val applicable: Boolean,
    )

    fun rateFor(ageDays: Long): BigDecimal =
        when {
            ageDays <= FIRST_BRACKET_DAYS -> FIRST_RATE
            ageDays <= SECOND_BRACKET_DAYS -> SECOND_RATE
            ageDays <= THIRD_BRACKET_DAYS -> THIRD_RATE
            else -> LAST_RATE
        }

    /**
     * Share of the interest that the tax takes, as a fraction; zero when there is no interest or no estimate.
     */
    fun effectiveRate(
        type: BondType,
        lots: List<Lot>,
        interest: BigDecimal,
    ): BigDecimal {
        if (interest.signum() <= 0) return BigDecimal.ZERO

        return estimate(type, lots, interest).tax.divide(interest, MC)
    }

    /**
     * The money kept in the bond on [date], one lot per purchase: a sale takes the same share out of every lot, and
     * each lot is as old as its purchase.
     */
    fun lotsAt(
        operations: List<BondOperation>,
        date: LocalDate,
    ): List<Lot> {
        val lots = mutableListOf<Pair<LocalDate, BigDecimal>>()

        operations
            .filter { it.localDate <= date }
            .sortedBy { it.localDate }
            .forEach { operation ->
                val value = operation.quantity.multiply(operation.unitPrice)

                if (operation.isPurchase()) {
                    lots.add(operation.localDate to value)
                } else {
                    val total = lots.fold(BigDecimal.ZERO) { acc, lot -> acc.add(lot.second) }

                    if (total.signum() > 0) {
                        val kept = BigDecimal.ONE.subtract(value.divide(total, MC)).max(BigDecimal.ZERO)
                        lots.replaceAll { it.first to it.second.multiply(kept) }
                    }
                }
            }

        return lots.map { Lot(it.second, ChronoUnit.DAYS.between(it.first, date)) }
    }

    fun estimate(
        type: BondType,
        lots: List<Lot>,
        interest: BigDecimal,
    ): Estimate =
        when (type) {
            BondType.LCI, BondType.LCA -> Estimate(BigDecimal.ZERO, true)
            BondType.INTERNATIONAL, BondType.OTHER -> Estimate(BigDecimal.ZERO, false)
            else -> Estimate(taxOn(lots, interest), true)
        }

    private fun taxOn(
        lots: List<Lot>,
        interest: BigDecimal,
    ): BigDecimal {
        if (interest.signum() <= 0 || lots.isEmpty()) return BigDecimal.ZERO

        val weights = lots.map { it.principal.multiply(BigDecimal(maxOf(it.ageDays, 1L))) }
        val totalWeight = weights.fold(BigDecimal.ZERO, BigDecimal::add)

        if (totalWeight.signum() <= 0) return BigDecimal.ZERO

        return lots
            .zip(weights)
            .fold(BigDecimal.ZERO) { tax, (lot, weight) ->
                tax.add(interest.multiply(weight).divide(totalWeight, MC).multiply(rateFor(lot.ageDays)))
            }.setScale(2, RoundingMode.HALF_UP)
    }
}
