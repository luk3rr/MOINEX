/*
 * Filename: InterestRecalculationRowDTO.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import java.math.BigDecimal
import java.time.YearMonth

data class InterestRecalculationRowDTO(
    val month: YearMonth,
    val manuallyAdjusted: Boolean,
    val monthlyBefore: BigDecimal?,
    val monthlyAfter: BigDecimal,
    val accumulatedBefore: BigDecimal?,
    val accumulatedAfter: BigDecimal,
)
