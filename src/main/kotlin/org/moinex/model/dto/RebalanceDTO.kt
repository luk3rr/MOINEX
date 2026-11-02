/*
 * Filename: RebalanceDTO.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.moinex.model.enums.AssetType
import java.math.BigDecimal

enum class RebalanceAction {
    BUY,
    SELL,
    NONE,
}

/**
 * What to do with one asset type to bring the portfolio closer to its allocation targets. [deviationPp] is the current
 * share minus the target, in percentage points.
 */
data class RebalanceRowDTO(
    val assetType: AssetType,
    val typeName: String,
    val currentPct: BigDecimal,
    val targetPct: BigDecimal,
    val deviationPp: BigDecimal,
    val action: RebalanceAction,
    val amount: BigDecimal,
)
