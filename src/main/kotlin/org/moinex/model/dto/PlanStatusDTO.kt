/*
 * Filename: PlanStatusDTO.kt
 * Created on: July 27, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.moinex.model.financialplanning.BudgetGroup
import java.math.BigDecimal

data class PlanStatusDTO(
    val group: BudgetGroup,
    val spentAmount: BigDecimal,
)
