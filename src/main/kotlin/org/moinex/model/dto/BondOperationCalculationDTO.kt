package org.moinex.model.dto

import org.moinex.model.investment.Bond
import java.math.BigDecimal

data class BondOperationCalculationDTO(
    val quantity: BigDecimal = BigDecimal.ZERO,
    val price: BigDecimal = BigDecimal.ZERO,
    val bond: Bond? = null,
)
