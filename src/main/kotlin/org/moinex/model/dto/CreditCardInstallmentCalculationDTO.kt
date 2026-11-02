/*
 * Filename: CreditCardInstallmentCalculationDTO.kt
 * Created on: March 11, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import java.math.BigDecimal

data class CreditCardInstallmentCalculationDTO(
    val installmentValue: BigDecimal,
    val remainder: BigDecimal,
)
