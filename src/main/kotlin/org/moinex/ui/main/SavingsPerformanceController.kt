/*
 * Filename: SavingsPerformanceController.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main

import atlantafx.base.controls.SegmentedControl
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.SegmentedFilter
import org.moinex.common.util.FxUtils
import org.moinex.common.util.UIUtils
import org.moinex.model.dto.BenchmarkSeriesDTO
import org.moinex.model.dto.ContributionDTO
import org.moinex.model.dto.DividendPanelDTO
import org.moinex.model.dto.PerformancePeriod
import org.moinex.model.dto.SeriesPointDTO
import org.moinex.service.PreferencesService
import org.moinex.service.investment.PerformanceBenchmarkService
import org.moinex.service.investment.PortfolioAnalyticsService
import org.moinex.ui.common.BenchmarkChart
import org.moinex.ui.common.ContributionBars
import org.moinex.ui.common.DividendChart
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import java.math.BigDecimal

/**
 * The Performance tab of the savings screen: the variable income portfolio against the CDI, the Ibovespa and the IPCA.
 */
@Controller
class SavingsPerformanceController(
    private val performanceBenchmarkService: PerformanceBenchmarkService,
    private val portfolioAnalyticsService: PortfolioAnalyticsService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var periodSegment: SegmentedControl

    @FXML
    private lateinit var legendBox: HBox

    @FXML
    private lateinit var chartHolder: StackPane

    @FXML
    private lateinit var contributionHolder: StackPane

    @FXML
    private lateinit var dividendHolder: StackPane

    @FXML
    private lateinit var dividendAverageLabel: Label

    @FXML
    private lateinit var dividendProjectionLabel: Label

    private lateinit var periodFilter: SegmentedFilter<PerformancePeriod>
    private var loadSequence = 0
    private var benchmarksRefreshed = false

    companion object {
        private val logger = LoggerFactory.getLogger(SavingsPerformanceController::class.java)
        private const val LEGEND_DOT = "●"
        private const val LEGEND_GAP = 4.0
    }

    @FXML
    fun initialize() {
        periodFilter =
            SegmentedFilter(
                periodSegment,
                listOf(
                    "3M" to PerformancePeriod.M3,
                    "6M" to PerformancePeriod.M6,
                    "12M" to PerformancePeriod.M12,
                ),
            )
        periodFilter.value = PerformancePeriod.M12
        periodFilter.onChange { load() }

        load()
        refreshBenchmarksOnce()
    }

    private fun refreshBenchmarksOnce() {
        if (benchmarksRefreshed) return
        benchmarksRefreshed = true

        FxUtils.launchBackgroundThenUI(
            background = { runCatching { performanceBenchmarkService.refreshBenchmarks() } },
            onUI = { result ->
                result.onFailure { logger.error("Could not refresh the benchmarks", it) }
                load()
            },
        )
    }

    private fun load() {
        val period = periodFilter.value
        val request = ++loadSequence

        FxUtils.launchBackgroundThenUI(
            background = {
                runCatching {
                    Triple(
                        performanceBenchmarkService.benchmarkSeries(period),
                        performanceBenchmarkService.contributions(period),
                        portfolioAnalyticsService.dividendPanel(),
                    )
                }
            },
            onUI = { result ->
                if (request != loadSequence) return@launchBackgroundThenUI

                result
                    .onSuccess { (series, contributions, dividends) ->
                        show(series)
                        showContributions(contributions)
                        showDividends(dividends)
                    }.onFailure { logger.error("Could not load the performance series", it) }
            },
        )
    }

    private fun show(series: BenchmarkSeriesDTO) {
        chartHolder.children.clear()
        legendBox.children.clear()

        if (series.isEmpty()) {
            chartHolder.children.add(
                Label(preferencesService.translate(TranslationKeys.SAVINGS_PERFORMANCE_BENCHMARKS_EMPTY)).apply {
                    isWrapText = true
                    alignment = Pos.CENTER
                },
            )
            return
        }

        legendBox.children.addAll(
            legendLabel(
                TranslationKeys.SAVINGS_PERFORMANCE_SERIES_PORTFOLIO,
                "benchmark-legend-portfolio",
                series.portfolio,
            ),
            legendLabel(TranslationKeys.SAVINGS_PERFORMANCE_SERIES_CDI, "benchmark-legend-cdi", series.cdi),
            legendLabel(
                TranslationKeys.SAVINGS_PERFORMANCE_SERIES_IBOVESPA,
                "benchmark-legend-ibovespa",
                series.ibovespa,
            ),
            legendLabel(TranslationKeys.SAVINGS_PERFORMANCE_SERIES_IPCA, "benchmark-legend-ipca", series.ipca),
        )
        chartHolder.children.add(BenchmarkChart.create(series, preferencesService::translate))
    }

    private fun showContributions(contributions: List<ContributionDTO>) {
        contributionHolder.children.setAll(ContributionBars(preferencesService::translate).build(contributions))
    }

    private fun showDividends(panel: DividendPanelDTO) {
        dividendAverageLabel.text =
            "${preferencesService.translate(TranslationKeys.SAVINGS_PERFORMANCE_DIVIDENDS_AVERAGE)}: " +
            UIUtils.formatCurrency(panel.monthlyAverage)
        dividendProjectionLabel.text =
            "${preferencesService.translate(TranslationKeys.SAVINGS_PERFORMANCE_DIVIDENDS_PROJECTION)}: " +
            UIUtils.formatCurrency(panel.projection12m)
        UIUtils.addTooltipToNode(
            dividendProjectionLabel,
            preferencesService.translate(TranslationKeys.SAVINGS_PERFORMANCE_DIVIDENDS_NOTE),
        )

        dividendHolder.children.clear()
        if (panel.series.isEmpty()) {
            dividendHolder.children.add(
                Label(preferencesService.translate(TranslationKeys.SAVINGS_PERFORMANCE_DIVIDENDS_EMPTY)),
            )
        } else {
            dividendHolder.children.add(DividendChart.create(panel, preferencesService::translate))
        }
    }

    private fun legendLabel(
        nameKey: String,
        styleClass: String,
        points: List<SeriesPointDTO>,
    ): HBox {
        val last: BigDecimal? = points.lastOrNull()?.pct
        val value = last?.let { formatSigned(it) } ?: Constants.NA_DATA

        return HBox(
            LEGEND_GAP,
            Label(LEGEND_DOT).apply { this.styleClass.add(styleClass) },
            Label("${preferencesService.translate(nameKey)} $value"),
        ).apply { alignment = Pos.CENTER_LEFT }
    }

    private fun formatSigned(value: BigDecimal): String {
        val sign =
            when {
                value.signum() > 0 -> "+"
                value.signum() < 0 -> "−"
                else -> ""
            }

        return sign + UIUtils.formatPercentageForFundamentalAnalysis(value.abs())
    }
}
