/*
 * Filename: FundamentalInsightText.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common

import org.moinex.common.constant.TranslationKeys
import org.moinex.model.dto.QualityDimensionType
import org.moinex.model.dto.QualityLabel
import org.moinex.model.dto.ValuationLevel

object FundamentalInsightText {
    fun valuationKey(level: ValuationLevel): String =
        when (level) {
            ValuationLevel.CHEAP -> TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_VALUATION_CHEAP
            ValuationLevel.FAIR -> TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_VALUATION_FAIR
            ValuationLevel.EXPENSIVE -> TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_VALUATION_EXPENSIVE
            ValuationLevel.UNAVAILABLE -> TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_VALUATION_UNAVAILABLE
        }

    fun valuationStyleClass(level: ValuationLevel): String =
        when (level) {
            ValuationLevel.CHEAP -> "valuation-badge-cheap"
            ValuationLevel.FAIR -> "valuation-badge-fair"
            ValuationLevel.EXPENSIVE -> "valuation-badge-expensive"
            ValuationLevel.UNAVAILABLE -> "valuation-badge-unavailable"
        }

    fun qualityKey(label: QualityLabel): String =
        when (label) {
            QualityLabel.WEAK -> TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_QUALITY_WEAK
            QualityLabel.REGULAR -> TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_QUALITY_REGULAR
            QualityLabel.GOOD -> TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_QUALITY_GOOD
            QualityLabel.EXCELLENT -> TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_QUALITY_EXCELLENT
        }

    fun qualityStyleClass(label: QualityLabel): String =
        when (label) {
            QualityLabel.WEAK -> "quality-badge-weak"
            QualityLabel.REGULAR -> "quality-badge-regular"
            QualityLabel.GOOD -> "quality-badge-good"
            QualityLabel.EXCELLENT -> "quality-badge-excellent"
        }

    fun dimensionKey(type: QualityDimensionType): String =
        when (type) {
            QualityDimensionType.PROFITABILITY -> TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_PROFITABILITY
            QualityDimensionType.DEBT -> TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_DEBT
            QualityDimensionType.GROWTH -> TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_GROWTH
            QualityDimensionType.CASH -> TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_CASH_GENERATION
        }

    fun dimensionTooltipKey(type: QualityDimensionType): String =
        when (type) {
            QualityDimensionType.PROFITABILITY ->
                TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_QUALITY_TIP_PROFITABILITY
            QualityDimensionType.DEBT -> TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_QUALITY_TIP_DEBT
            QualityDimensionType.GROWTH -> TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_QUALITY_TIP_GROWTH
            QualityDimensionType.CASH -> TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_QUALITY_TIP_CASH
        }
}
