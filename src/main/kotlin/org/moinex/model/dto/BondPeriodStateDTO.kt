package org.moinex.model.dto

import java.math.BigDecimal
import java.time.LocalDate

data class BondPeriodStateDTO(
    val quantity: BigDecimal,
    val lastSpread: BigDecimal,
    val lastBuyPrice: BigDecimal?,
    val interest: BigDecimal,
    val lastCalculationDate: LocalDate,
    val actualLastDate: LocalDate,
)
