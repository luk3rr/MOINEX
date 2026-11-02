/*
 * Filename: DividendHistoryChart.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common

import javafx.scene.chart.BarChart
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import org.moinex.common.util.UIUtils
import org.moinex.model.dto.MonthlyDividend
import java.time.format.DateTimeFormatter

/**
 * Bar chart with the dividends a fund paid per month, oldest to newest.
 */
object DividendHistoryChart {
    private const val CHART_HEIGHT = 220.0
    private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MM/yy")

    fun create(history: List<MonthlyDividend>): BarChart<String, Number> {
        val series = XYChart.Series<String, Number>()
        history.forEach { series.data.add(XYChart.Data(it.month.format(MONTH_FORMAT), it.value)) }

        return BarChart(CategoryAxis(), NumberAxis()).apply {
            data.add(series)
            isLegendVisible = false
            animated = false
            prefHeight = CHART_HEIGHT
            minHeight = CHART_HEIGHT
            UIUtils.applyDefaultChartStyle(this)
        }
    }
}
