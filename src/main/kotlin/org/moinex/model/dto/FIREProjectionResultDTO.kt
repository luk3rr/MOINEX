package org.moinex.model.dto

import java.math.BigDecimal
import java.time.LocalDate

data class FIREProjectionResultDTO(
    val monthsToFire: Int?,
    val fireDate: LocalDate?,
    val ageAtFire: Int?,
    val fireTarget: BigDecimal,
    val projectedNetWorthAtFire: BigDecimal?,
    val dataPoints: List<Pair<Int, BigDecimal>>,
)
