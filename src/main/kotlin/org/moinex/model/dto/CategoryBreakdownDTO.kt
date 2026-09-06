/*
 * Filename: CategoryBreakdownDTO.kt
 * Created on: September 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import java.math.BigDecimal

data class CategoryBreakdownDTO(
    val categoryName: String,
    val total: BigDecimal,
    val percentage: BigDecimal,
    val monthlyAverage: BigDecimal,
)
