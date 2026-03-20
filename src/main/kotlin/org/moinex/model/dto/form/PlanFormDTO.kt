package org.moinex.model.dto.form

data class PlanFormDTO(
    val name: String,
    val baseIncome: String,
) {
    fun isValid(): Boolean = name.isNotEmpty() && baseIncome.isNotEmpty()
}
