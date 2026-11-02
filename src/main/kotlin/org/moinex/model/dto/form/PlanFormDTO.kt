/*
 * Filename: PlanFormDTO.kt
 * Created on: March 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto.form

import java.time.LocalDate

data class PlanFormDTO(
    val name: String,
    val baseIncome: String,
    val startDate: LocalDate? = null,
) {
    fun isValid(): Boolean = name.isNotEmpty() && baseIncome.isNotEmpty() && startDate != null
}
