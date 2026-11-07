/*
 * Filename: BondRedemptionEstimateDTO.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import java.math.BigDecimal

/**
 * What a redemption of a bond takes out besides the principal: the proportional share of the interest held in the
 * bond and the estimated income tax on it. [taxApplicable] is false when the bond type has no tax estimate.
 */
data class BondRedemptionEstimateDTO(
    val interestShare: BigDecimal,
    val estimatedTax: BigDecimal,
    val taxApplicable: Boolean,
    val principal: BigDecimal = BigDecimal.ZERO,
) {
    val netInterest: BigDecimal
        get() = interestShare.subtract(estimatedTax)
}
