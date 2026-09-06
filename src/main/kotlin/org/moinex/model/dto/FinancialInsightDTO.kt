/*
 * Filename: FinancialInsightDTO.kt
 * Created on: September 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.moinex.model.enums.FinancialInsightSeverity
import org.moinex.model.enums.FinancialInsightType

data class FinancialInsightDTO(
    val type: FinancialInsightType,
    val severity: FinancialInsightSeverity,
    val messageKey: String,
    val messageArgs: List<Any> = emptyList(),
)
