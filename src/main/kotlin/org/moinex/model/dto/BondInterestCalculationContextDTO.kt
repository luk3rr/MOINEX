package org.moinex.model.dto

import java.math.BigDecimal
import java.time.YearMonth

data class BondInterestCalculationContextDTO(
    var startMonth: YearMonth,
    var accumulatedInterest: BigDecimal,
)
