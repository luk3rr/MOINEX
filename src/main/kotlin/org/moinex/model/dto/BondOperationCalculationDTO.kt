/*
 * Filename: BondOperationCalculationDTO.kt
 * Created on: March 15, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.moinex.model.investment.Bond
import java.math.BigDecimal

data class BondOperationCalculationDTO(
    val quantity: BigDecimal = BigDecimal.ZERO,
    val price: BigDecimal = BigDecimal.ZERO,
    val bond: Bond? = null,
)
