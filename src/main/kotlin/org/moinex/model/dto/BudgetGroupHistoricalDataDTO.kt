/*
 * Filename: BudgetGroupHistoricalDataDTO.kt
 * Created on: July 27, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import java.math.BigDecimal
import java.time.YearMonth

data class BudgetGroupHistoricalDataDTO(
    val groupName: String,
    val period: YearMonth,
    val spentAmount: BigDecimal,
    val targetAmount: BigDecimal,
)
