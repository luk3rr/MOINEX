/*
 * Filename: ProfitabilityMetricsDTO.kt
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model.dto

import java.math.BigDecimal

data class ProfitabilityMetricsDTO(
    val totalInvested: BigDecimal,
    val currentValue: BigDecimal,
    val profitLoss: BigDecimal,
    val returnPercentage: BigDecimal,
    val dividendYield: BigDecimal,
    val totalDividends: BigDecimal,
) {
    fun isProfitable(): Boolean = profitLoss.compareTo(BigDecimal.ZERO) > 0

    fun isNeutral(): Boolean = profitLoss.compareTo(BigDecimal.ZERO) == 0

    fun isLoss(): Boolean = profitLoss.compareTo(BigDecimal.ZERO) < 0
}
