/*
 * Filename: DividendChart.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common

import javafx.geometry.Side
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.StackedBarChart
import javafx.scene.chart.XYChart
import javafx.util.StringConverter
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.model.dto.DividendPanelDTO
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter

/**
 * Stacked bars of the dividends received per month, one series per ticker (the colors are in `dividend-chart.css`).
 */
object DividendChart {
    private const val STYLE_CLASS = "dividend-chart"
    private val HUNDRED = BigDecimal(100)
    private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MM/yy")

    fun create(
        panel: DividendPanelDTO,
        translate: (String) -> String,
    ): StackedBarChart<String, Number> {
        val yAxis =
            NumberAxis().apply {
                tickLabelFormatter =
                    object : StringConverter<Number>() {
                        override fun toString(value: Number): String =
                            UIUtils.formatCurrency(
                                value as? java.math.BigDecimal ?: java.math.BigDecimal(value.toString()),
                            )

                        override fun fromString(string: String): Number = 0
                    }
            }

        return StackedBarChart(CategoryAxis(), yAxis).apply {
            panel.series.forEach { dividends ->
                data.add(
                    XYChart.Series<String, Number>().apply {
                        name = dividends.symbol ?: translate(TranslationKeys.SAVINGS_PERFORMANCE_DIVIDENDS_OTHERS)
                        panel.months.forEachIndexed { index, month ->
                            data.add(XYChart.Data(month.format(MONTH_FORMAT), dividends.values[index]))
                        }
                    },
                )
            }
            animated = false
            isLegendVisible = true
            legendSide = Side.RIGHT
            UIUtils.applyDefaultChartStyle(this, STYLE_CLASS)
            stylesheets.add(
                DividendChart::class.java.getResource(Files.DIVIDEND_CHART_STYLE_SHEET)?.toExternalForm()
                    ?: error("Dividend chart stylesheet not found"),
            )
            addTooltips(this, panel)
        }
    }

    /**
     * Gives each part of each stack a tooltip with the ticker, the month, what it paid, its share of the month and the
     * total of the month, like the other stacked charts of the application.
     */
    private fun addTooltips(
        chart: StackedBarChart<String, Number>,
        panel: DividendPanelDTO,
    ) {
        val monthTotals =
            panel.months.indices.map { index ->
                panel.series.fold(BigDecimal.ZERO) { acc, series -> acc.add(series.values[index]) }
            }

        chart.data.forEach { series ->
            series.data.forEachIndexed { index, data ->
                val node = data.node ?: return@forEachIndexed
                val value = data.yValue as? BigDecimal ?: BigDecimal(data.yValue.toString())
                val total = monthTotals[index]
                val share =
                    if (total.signum() >
                        0
                    ) {
                        value.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP)
                    } else {
                        BigDecimal.ZERO
                    }

                UIUtils.addTooltipToXYChartNode(
                    node,
                    "${series.name} · ${data.xValue}: ${UIUtils.formatCurrency(value)} " +
                        "(${UIUtils.formatPercentage(share)})\nTotal: ${UIUtils.formatCurrency(total)}",
                )
            }
        }
    }
}
