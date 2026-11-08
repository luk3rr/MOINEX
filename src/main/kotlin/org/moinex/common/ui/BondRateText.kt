/*
 * Filename: BondRateText.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import java.math.BigDecimal
import java.text.MessageFormat

/**
 * The contracted rate of a bond in words: a percentage of the index, the index plus a yearly rate, or a yearly rate.
 * The three patterns come from the translations and take the rate as {0} and the index as {1}.
 */
object BondRateText {
    private const val NONE = "-"

    fun format(
        type: InterestType?,
        index: InterestIndex?,
        rate: BigDecimal?,
        percentOfIndexPattern: String,
        indexPlusRatePattern: String,
        yearlyRatePattern: String,
    ): String {
        if (type == null || rate == null) return NONE

        val rateText = rate.stripTrailingZeros().toPlainString()
        val indexText = index?.name ?: NONE

        return when (type) {
            InterestType.FLOATING -> MessageFormat.format(percentOfIndexPattern, rateText, indexText)
            InterestType.FLOATING_SPREAD -> MessageFormat.format(indexPlusRatePattern, rateText, indexText)
            InterestType.FIXED, InterestType.ZERO_COUPON -> MessageFormat.format(yearlyRatePattern, rateText, indexText)
        }
    }
}
