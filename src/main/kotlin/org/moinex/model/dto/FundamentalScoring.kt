/*
 * Filename: FundamentalScoring.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import java.math.BigDecimal

enum class ValuationLevel {
    CHEAP,
    FAIR,
    EXPENSIVE,
    UNAVAILABLE,
}

/**
 * Where the price stands against the fair value. [fairValue] is Graham's revised fair value when it exists and the
 * Graham number otherwise. When the level is [ValuationLevel.UNAVAILABLE], [reason] says why.
 */
data class ValuationVerdict(
    val level: ValuationLevel,
    val price: BigDecimal?,
    val grahamNumber: BigDecimal?,
    val fairValue: BigDecimal?,
    val marginOfSafetyPct: BigDecimal?,
    val reason: MetricReason?,
)

enum class QualityDimensionType {
    PROFITABILITY,
    DEBT,
    GROWTH,
    CASH,
}

enum class QualityLabel {
    WEAK,
    REGULAR,
    GOOD,
    EXCELLENT,
}

data class QualityDimension(
    val type: QualityDimensionType,
    val score: Int?,
    val weight: Int,
)

/**
 * Quality score from 0 to 100. A dimension without data leaves the score and its weight is redistributed; with fewer
 * than two dimensions the [total] and the [label] are null.
 */
data class QualityScore(
    val total: Int?,
    val label: QualityLabel?,
    val dimensions: List<QualityDimension>,
)

data class SectorComparison(
    val median: BigDecimal?,
    val peers: Int,
) {
    val enough: Boolean get() = median != null && peers >= MIN_PEERS

    companion object {
        const val MIN_PEERS = 3
    }
}
