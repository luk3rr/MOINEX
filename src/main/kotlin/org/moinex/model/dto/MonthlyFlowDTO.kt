/*
 * Filename: MonthlyFlowDTO.kt
 * Created on: September 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import java.math.BigDecimal
import java.time.YearMonth

data class MonthlyFlowDTO(
    val period: YearMonth,
    val income: BigDecimal,
    val expense: BigDecimal,
    val net: BigDecimal,
    val savingsRatePercentage: BigDecimal,
)
