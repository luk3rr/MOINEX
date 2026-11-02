/*
 * Filename: BondPeriodInterestResultDTO.kt
 * Created on: March 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import java.math.BigDecimal
import java.time.LocalDate

data class BondPeriodInterestResultDTO(
    val interest: BigDecimal,
    val lastDateWithData: LocalDate,
)
