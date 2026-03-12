package org.moinex.model.dto

import java.math.BigDecimal

data class CreditCardInstallmentCalculationDTO(
    val installmentValue: BigDecimal,
    val remainder: BigDecimal,
)
