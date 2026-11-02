/*
 * Filename: FundamentalScoringService.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import org.moinex.model.dto.AssetKind
import org.moinex.model.dto.FundamentalMetric
import org.moinex.model.dto.FundamentalSnapshot
import org.moinex.model.dto.MetricReason
import org.moinex.model.dto.QualityDimension
import org.moinex.model.dto.QualityDimensionType
import org.moinex.model.dto.QualityLabel
import org.moinex.model.dto.QualityScore
import org.moinex.model.dto.SectorComparison
import org.moinex.model.dto.ValuationLevel
import org.moinex.model.dto.ValuationVerdict
import org.moinex.model.investment.FundamentalAnalysis
import org.moinex.repository.investment.FundamentalAnalysisRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Reads a [FundamentalSnapshot] and answers three questions: is the price cheap or expensive against the fair value,
 * how good is the company, and how do its numbers compare with the sector of the user's other tickers.
 */
@Service
class FundamentalScoringService(
    private val fundamentalAnalysisRepository: FundamentalAnalysisRepository,
) {
    companion object {
        val CHEAP_MARGIN_PCT: BigDecimal = BigDecimal("20")
        val EXPENSIVE_MARGIN_PCT: BigDecimal = BigDecimal("-20")

        const val MIN_DIMENSIONS = 2
        const val FINANCIAL_SECTOR = "Financial Services"
        const val LABEL_REGULAR_FROM = 40
        const val LABEL_GOOD_FROM = 60
        const val LABEL_EXCELLENT_FROM = 80

        private const val PERCENT = 100
        private const val MARGIN_SCALE = 2
        private const val MEDIAN_SCALE = 4
        private const val MAX_SCORE = 100.0

        private const val SECTION_PROFITABILITY = "profitability"
        private const val SECTION_DEBT = "debt"
        private const val SECTION_GROWTH = "growth"
        private const val SECTION_CASH = "cash_generation"
        private const val SECTION_VALUATION = "valuation"

        val DIMENSION_WEIGHTS: Map<QualityDimensionType, Int> =
            mapOf(
                QualityDimensionType.PROFITABILITY to 35,
                QualityDimensionType.DEBT to 25,
                QualityDimensionType.GROWTH to 20,
                QualityDimensionType.CASH to 20,
            )

        private val ROE_BANDS = listOf(15.0 to 100, 10.0 to 75, 5.0 to 50, 0.0 to 25)
        private val EBITDA_MARGIN_BANDS = listOf(25.0 to 100, 15.0 to 75, 8.0 to 50, 0.0 to 25)
        private val CURRENT_RATIO_BANDS = listOf(2.0 to 100, 1.5 to 75, 1.0 to 50, 0.7 to 25)
        private val GROWTH_BANDS = listOf(15.0 to 100, 8.0 to 75, 3.0 to 50, 0.0 to 25)
        private val CASH_CONVERSION_BANDS = listOf(1.0 to 100, 0.7 to 75, 0.4 to 50, 0.0 to 25)
        private val FCF_YIELD_BANDS = listOf(8.0 to 100, 5.0 to 75, 3.0 to 50, 0.0 to 25)
        private val DEBT_MULTIPLE_LIMITS = listOf(1.0 to 100, 2.0 to 75, 3.0 to 50, 4.0 to 25)

        /** Metrics shown next to the sector median, as (section, key). */
        val SECTOR_COMPARABLE_METRICS: List<Pair<String, String>> =
            listOf(
                SECTION_PROFITABILITY to "roe",
                SECTION_PROFITABILITY to "roic",
                SECTION_PROFITABILITY to "net_margin",
                SECTION_PROFITABILITY to "ebitda_margin",
                SECTION_DEBT to "net_debt_to_ebitda",
                SECTION_DEBT to "current_ratio",
                SECTION_VALUATION to "pe_ratio",
                SECTION_VALUATION to "ev_to_ebitda",
                SECTION_VALUATION to "earnings_yield",
                SECTION_VALUATION to "fcf_yield",
            )

        fun sectorKey(
            section: String,
            key: String,
        ): String = "$section.$key"
    }

    fun valuation(snapshot: FundamentalSnapshot): ValuationVerdict {
        val price = snapshot.metric(SECTION_VALUATION, "current_price").value
        val graham = snapshot.metric(SECTION_VALUATION, "graham_number")
        val revised = snapshot.metric(SECTION_VALUATION, "graham_fair_value")
        val fairMetric = if (revised.hasValue()) revised else graham
        val fairValue = fairMetric.value

        if (price == null || fairValue == null || fairValue.signum() <= 0) {
            val reason =
                when {
                    price == null -> MetricReason.DATA_UNAVAILABLE
                    fairValue == null -> fairMetric.reason ?: MetricReason.DATA_UNAVAILABLE
                    else -> MetricReason.NOT_APPLICABLE
                }
            return ValuationVerdict(ValuationLevel.UNAVAILABLE, price, graham.value, fairValue, null, reason)
        }

        val margin =
            fairValue
                .subtract(price)
                .multiply(BigDecimal(PERCENT))
                .divide(fairValue, MARGIN_SCALE, RoundingMode.HALF_UP)
        val level =
            when {
                margin >= CHEAP_MARGIN_PCT -> ValuationLevel.CHEAP
                margin <= EXPENSIVE_MARGIN_PCT -> ValuationLevel.EXPENSIVE
                else -> ValuationLevel.FAIR
            }

        return ValuationVerdict(level, price, graham.value, fairValue, margin, null)
    }

    /**
     * The quality score of [snapshot]. Debt and cash generation do not describe a bank or insurer (net debt over EBITDA
     * does not apply and the cash flow of the operation is not comparable), so for the financial sector they are left
     * out and the weights of the other dimensions are redistributed.
     */
    fun quality(snapshot: FundamentalSnapshot): QualityScore {
        val financial = snapshot.sector == FINANCIAL_SECTOR
        val scores =
            mapOf(
                QualityDimensionType.PROFITABILITY to profitabilityScore(snapshot),
                QualityDimensionType.DEBT to debtScore(snapshot).takeUnless { financial },
                QualityDimensionType.GROWTH to growthScore(snapshot),
                QualityDimensionType.CASH to cashScore(snapshot).takeUnless { financial },
            )
        val dimensions =
            QualityDimensionType.entries.map { QualityDimension(it, scores[it], DIMENSION_WEIGHTS.getValue(it)) }
        val present = dimensions.filter { it.score != null }

        if (present.size < MIN_DIMENSIONS) return QualityScore(null, null, dimensions)

        val weightSum = present.sumOf { it.weight }
        val total = Math.round(present.sumOf { it.score!! * it.weight }.toDouble() / weightSum).toInt()

        return QualityScore(total, labelFor(total), dimensions)
    }

    fun labelFor(total: Int): QualityLabel =
        when {
            total >= LABEL_EXCELLENT_FROM -> QualityLabel.EXCELLENT
            total >= LABEL_GOOD_FROM -> QualityLabel.GOOD
            total >= LABEL_REGULAR_FROM -> QualityLabel.REGULAR
            else -> QualityLabel.WEAK
        }

    /**
     * Median of each comparable metric among the other tickers of the same sector that have an analysis of the same
     * period, keyed by [sectorKey]. A ticker without sector compares with nobody.
     */
    fun sectorComparisons(analysis: FundamentalAnalysis): Map<String, SectorComparison> {
        val sector = analysis.sector
        val peers =
            if (sector.isNullOrBlank()) {
                emptyList()
            } else {
                fundamentalAnalysisRepository
                    .findAllForActiveTickers()
                    .filter {
                        it.sector == sector &&
                            it.periodType == analysis.periodType &&
                            it.ticker.id != analysis.ticker.id
                    }.mapNotNull { runCatching { FundamentalParser.parse(it.dataJson) }.getOrNull() }
                    .filter { it.kind == AssetKind.COMPANY }
            }

        return SECTOR_COMPARABLE_METRICS.associate { (section, key) ->
            val values = peers.mapNotNull { it.metric(section, key).value }
            sectorKey(section, key) to SectorComparison(median(values), values.size)
        }
    }

    fun median(values: List<BigDecimal>): BigDecimal? {
        if (values.isEmpty()) return null

        val sorted = values.sorted()
        val middle = sorted.size / 2

        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            sorted[middle - 1].add(sorted[middle]).divide(BigDecimal(2), MEDIAN_SCALE, RoundingMode.HALF_UP)
        }
    }

    private fun profitabilityScore(snapshot: FundamentalSnapshot): Int? =
        average(
            band(snapshot.metric(SECTION_PROFITABILITY, "roe"), ROE_BANDS),
            band(snapshot.metric(SECTION_PROFITABILITY, "net_margin"), ROE_BANDS),
            band(snapshot.metric(SECTION_PROFITABILITY, "ebitda_margin"), EBITDA_MARGIN_BANDS),
        )

    private fun debtScore(snapshot: FundamentalSnapshot): Int? {
        val multiple = snapshot.metric(SECTION_DEBT, "net_debt_to_ebitda")
        val multipleScore =
            when {
                multiple.reason == MetricReason.NOT_APPLICABLE -> 0
                multiple.value == null -> null
                multiple.value.signum() <= 0 -> MAX_SCORE.toInt()
                else -> lowerIsBetter(multiple.value.toDouble(), DEBT_MULTIPLE_LIMITS)
            }

        return average(multipleScore, band(snapshot.metric(SECTION_DEBT, "current_ratio"), CURRENT_RATIO_BANDS))
    }

    private fun growthScore(snapshot: FundamentalSnapshot): Int? {
        val epsGrowth = snapshot.metric(SECTION_VALUATION, "eps_cagr")
        val epsScore = if (epsGrowth.reason == MetricReason.NEGATIVE_EPS) 0 else band(epsGrowth, GROWTH_BANDS)

        return average(band(snapshot.metric(SECTION_GROWTH, "cagr"), GROWTH_BANDS), epsScore)
    }

    private fun cashScore(snapshot: FundamentalSnapshot): Int? =
        average(
            band(snapshot.metric(SECTION_CASH, "fcf_to_net_income"), CASH_CONVERSION_BANDS),
            band(snapshot.metric(SECTION_VALUATION, "fcf_yield"), FCF_YIELD_BANDS),
        )

    private fun band(
        metric: FundamentalMetric,
        bands: List<Pair<Double, Int>>,
    ): Int? {
        val value = metric.value?.toDouble() ?: return null

        return bands.firstOrNull { value >= it.first }?.second ?: 0
    }

    private fun lowerIsBetter(
        value: Double,
        limits: List<Pair<Double, Int>>,
    ): Int = limits.firstOrNull { value <= it.first }?.second ?: 0

    private fun average(vararg scores: Int?): Int? {
        val present = scores.filterNotNull()

        return if (present.isEmpty()) null else Math.round(present.average()).toInt()
    }
}
