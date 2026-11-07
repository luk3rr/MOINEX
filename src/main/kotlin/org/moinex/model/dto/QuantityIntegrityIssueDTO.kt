/*
 * Filename: QuantityIntegrityIssueDTO.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import java.math.BigDecimal

enum class QuantityIssueKind {
    TICKER_QUANTITY_DIFFERS,
    BOND_WITHOUT_PURCHASE,
}

data class QuantityIntegrityIssueDTO(
    val kind: QuantityIssueKind,
    val name: String,
    val heldQuantity: BigDecimal,
    val registeredQuantity: BigDecimal,
    val difference: BigDecimal,
)
