package org.moinex.model.dto

import java.math.BigDecimal
import java.time.LocalDate

data class BondOperationStateDTO(
    val quantity: BigDecimal,
    val lastSpread: BigDecimal,
    val lastBuyPrice: BigDecimal?,
    val interest: BigDecimal? = null,
    val lastDate: LocalDate? = null,
)
