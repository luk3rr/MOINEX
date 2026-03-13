package org.moinex.model.dto

import java.math.BigDecimal
import java.time.LocalDate

data class BondPeriodInterestResultDTO(
    val interest: BigDecimal,
    val lastDateWithData: LocalDate,
)
