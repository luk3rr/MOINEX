/*
 * Filename: TickerPerformanceDTO.kt
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model.dto

import java.math.BigDecimal

data class TickerPerformanceDTO(
    val name: String,
    val symbol: String,
    val profitLossPercentage: BigDecimal,
    val profitLossValue: BigDecimal,
    val currentValue: BigDecimal,
) {
    fun isPositive(): Boolean = profitLossPercentage.compareTo(BigDecimal.ZERO) > 0

    fun isNegative(): Boolean = profitLossPercentage.compareTo(BigDecimal.ZERO) < 0

    fun isNeutral(): Boolean = profitLossPercentage.compareTo(BigDecimal.ZERO) == 0

    fun getSign(): String =
        when {
            isNeutral() -> ""
            isPositive() -> "+ "
            else -> "- "
        }
}
