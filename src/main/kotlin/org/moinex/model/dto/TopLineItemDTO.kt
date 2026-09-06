/*
 * Filename: TopLineItemDTO.kt
 * Created on: September 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.moinex.model.enums.TransactionSource
import java.math.BigDecimal
import java.time.LocalDateTime

data class TopLineItemDTO(
    val date: LocalDateTime,
    val description: String,
    val amount: BigDecimal,
    val categoryName: String,
    val source: TransactionSource,
)
