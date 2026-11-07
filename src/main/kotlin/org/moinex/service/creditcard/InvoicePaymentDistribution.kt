/*
 * Filename: InvoicePaymentDistribution.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.creditcard

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * How an amount paid against an invoice is shared among its pending installments: in proportion to what each one still
 * owes, to the cent, never more than an installment owes, and adding up to exactly the amount paid.
 */
object InvoicePaymentDistribution {
    private const val CENT_SCALE = 2
    private val MC = MathContext.DECIMAL128

    fun distribute(
        owed: List<BigDecimal>,
        amount: BigDecimal,
    ): List<BigDecimal> {
        val total = owed.fold(BigDecimal.ZERO, BigDecimal::add)

        if (owed.isEmpty() || total.signum() <= 0 || amount.signum() <= 0) {
            return owed.map { BigDecimal.ZERO.setScale(CENT_SCALE) }
        }

        val capped = amount.min(total)

        val shares =
            owed
                .map {
                    capped
                        .multiply(it, MC)
                        .divide(total, MC)
                        .setScale(CENT_SCALE, RoundingMode.HALF_UP)
                        .min(it)
                }.toMutableList()

        var difference = capped.subtract(shares.fold(BigDecimal.ZERO, BigDecimal::add))

        if (difference.signum() > 0) {
            owed.indices.forEach { index ->
                val room = owed[index].subtract(shares[index])
                val extra = difference.min(room).max(BigDecimal.ZERO)
                shares[index] = shares[index].add(extra)
                difference = difference.subtract(extra)
            }
        } else if (difference.signum() < 0) {
            owed.indices.reversed().forEach { index ->
                val taken = difference.negate().min(shares[index])
                shares[index] = shares[index].subtract(taken)
                difference = difference.add(taken)
            }
        }

        return shares
    }
}
