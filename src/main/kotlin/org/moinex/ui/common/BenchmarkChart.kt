/*
 * Filename: BenchmarkChart.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common

import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.layout.StackPane
import javafx.util.StringConverter
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.ChartCrosshair
import org.moinex.common.ui.CrosshairSeries
import org.moinex.common.util.UIUtils
import org.moinex.model.dto.BenchmarkSeriesDTO
import org.moinex.model.dto.SeriesPointDTO
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Line chart of accumulated returns: the portfolio and its benchmarks over the same days. The series keep a fixed
 * position (portfolio, CDI, Ibovespa, IPCA) even when empty, so that each one always has the same color; the colors are
 * in `benchmark-chart.css`.
 */
object BenchmarkChart {
    private const val STYLE_CLASS = "benchmark-chart"
    private const val TICK_COUNT = 6
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM")

    fun create(
        series: BenchmarkSeriesDTO,
        translate: (String) -> String,
    ): StackPane {
        val firstDay =
            series.portfolio
                .first()
                .date
                .toEpochDay()
                .toDouble()
        val lastDay =
            series.portfolio
                .last()
                .date
                .toEpochDay()
                .toDouble()
        val span = (lastDay - firstDay).coerceAtLeast(1.0)

        val xAxis =
            NumberAxis(firstDay, lastDay, (span / TICK_COUNT).coerceAtLeast(1.0)).apply {
                isMinorTickVisible = false
                tickLabelFormatter =
                    object : StringConverter<Number>() {
                        override fun toString(value: Number): String =
                            LocalDate.ofEpochDay(value.toLong()).format(DATE_FORMAT)

                        override fun fromString(string: String): Number = 0
                    }
            }
        val yAxis =
            NumberAxis().apply {
                isForceZeroInRange = true
                tickLabelFormatter =
                    object : StringConverter<Number>() {
                        override fun toString(value: Number): String =
                            UIUtils.formatPercentageForFundamentalAnalysis(value)

                        override fun fromString(string: String): Number = 0
                    }
            }

        val chart =
            LineChart(xAxis, yAxis).apply {
                data.addAll(listOf(series.portfolio, series.cdi, series.ibovespa, series.ipca).map(::toSeries))
                isLegendVisible = false
                animated = false
                createSymbols = false
                UIUtils.applyDefaultChartStyle(this, STYLE_CLASS)
                stylesheets.add(
                    BenchmarkChart::class.java.getResource(Files.BENCHMARK_CHART_STYLE_SHEET)?.toExternalForm()
                        ?: error("Benchmark chart stylesheet not found"),
                )
            }

        return ChartCrosshair.wrap(
            chart,
            STYLE_CLASS,
            listOf(
                crosshairSeries(
                    TranslationKeys.SAVINGS_PERFORMANCE_SERIES_PORTFOLIO,
                    "crosshair-series-portfolio",
                    series.portfolio,
                    translate,
                ),
                crosshairSeries(
                    TranslationKeys.SAVINGS_PERFORMANCE_SERIES_CDI,
                    "crosshair-series-cdi",
                    series.cdi,
                    translate,
                ),
                crosshairSeries(
                    TranslationKeys.SAVINGS_PERFORMANCE_SERIES_IBOVESPA,
                    "crosshair-series-ibovespa",
                    series.ibovespa,
                    translate,
                ),
                crosshairSeries(
                    TranslationKeys.SAVINGS_PERFORMANCE_SERIES_IPCA,
                    "crosshair-series-ipca",
                    series.ipca,
                    translate,
                ),
            ).filter { it.points.isNotEmpty() },
        )
    }

    private fun crosshairSeries(
        nameKey: String,
        styleClass: String,
        points: List<SeriesPointDTO>,
        translate: (String) -> String,
    ): CrosshairSeries =
        CrosshairSeries(
            translate(nameKey),
            styleClass,
            points.map { it.date to it.pct.toDouble() },
        ) { formatSigned(it) }

    private fun formatSigned(pct: Double): String {
        val sign =
            when {
                pct > 0 -> "+"
                pct < 0 -> "−"
                else -> ""
            }

        return sign + UIUtils.formatPercentageForFundamentalAnalysis(kotlin.math.abs(pct))
    }

    private fun toSeries(points: List<SeriesPointDTO>): XYChart.Series<Number, Number> =
        XYChart.Series<Number, Number>().apply {
            points.forEach { data.add(XYChart.Data(it.date.toEpochDay(), it.pct)) }
        }
}
