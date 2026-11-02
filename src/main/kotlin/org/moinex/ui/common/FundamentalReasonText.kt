/*
 * Filename: FundamentalReasonText.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common

import org.moinex.common.constant.TranslationKeys
import org.moinex.model.dto.MetricReason

object FundamentalReasonText {
    fun translationKey(reason: MetricReason): String =
        when (reason) {
            MetricReason.NEGATIVE_EPS -> TranslationKeys.FUNDAMENTAL_ANALYSIS_REASON_NEGATIVE_EPS
            MetricReason.NEGATIVE_EQUITY -> TranslationKeys.FUNDAMENTAL_ANALYSIS_REASON_NEGATIVE_EQUITY
            MetricReason.NEGATIVE_GROWTH -> TranslationKeys.FUNDAMENTAL_ANALYSIS_REASON_NEGATIVE_GROWTH
            MetricReason.INSUFFICIENT_HISTORY -> TranslationKeys.FUNDAMENTAL_ANALYSIS_REASON_INSUFFICIENT_HISTORY
            MetricReason.NOT_APPLICABLE -> TranslationKeys.FUNDAMENTAL_ANALYSIS_REASON_NOT_APPLICABLE
            MetricReason.DATA_UNAVAILABLE -> TranslationKeys.FUNDAMENTAL_ANALYSIS_REASON_DATA_UNAVAILABLE
        }
}
