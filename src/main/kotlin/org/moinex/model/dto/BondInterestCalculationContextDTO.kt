/*
 * Filename: BondInterestCalculationContextDTO.kt
 * Created on: March 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import java.math.BigDecimal
import java.time.YearMonth

data class BondInterestCalculationContextDTO(
    var startMonth: YearMonth,
    var accumulatedInterest: BigDecimal,
)
