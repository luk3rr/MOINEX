/*
 * Filename: NetWorthLineChart.kt (original filename: NetWorthLineChart.java)
 * Created on: [Original date]
 * Author: [Original author]
 *
 * Migrated to Kotlin on 16/03/2026
 */

package org.moinex.common.chart

import javafx.geometry.Side
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.layout.StackPane
import javafx.scene.shape.Line
import javafx.util.StringConverter
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.ChartCrosshair
import org.moinex.common.util.FxUtils
import org.moinex.common.util.UIUtils
import org.moinex.model.dto.NetWorthDataPointDTO
import org.moinex.service.PreferencesService
import java.time.YearMonth

class NetWorthLineChart : LineChart<String, Number>(CategoryAxis(), NumberAxis()) {
    private companion object {
        const val CHART_STYLE_CLASS = "net-worth-chart"
    }

    var preferencesService: PreferencesService? = null
    private var currentDateLine: Line? = null
    private val currentMonth: YearMonth = YearMonth.now()
    private var dataPoints: List<NetWorthDataPointDTO> = emptyList()

    init {
        setupChart()
        stylesheets.add(javaClass.getResource(Files.TIMELINE_CHART_STYLE_SHEET)!!.toExternalForm())

        widthProperty().addListener { _, _, _ -> FxUtils.launchOnFxThread { addCurrentDateLine(dataPoints) } }
        heightProperty().addListener { _, _, _ -> FxUtils.launchOnFxThread { addCurrentDateLine(dataPoints) } }
    }

    private fun setupChart() {
        isLegendVisible = true
        legendSide = Side.BOTTOM
        animated = true
        createSymbols = true

        (yAxis as NumberAxis).apply {
            isAutoRanging = true
            isForceZeroInRange = false
            tickLabelFormatter =
                object : StringConverter<Number>() {
                    override fun toString(value: Number): String = UIUtils.formatCurrency(value)

                    override fun fromString(string: String): Number = 0
                }
        }

        (xAxis as CategoryAxis).apply {
            isAutoRanging = true
            tickLabelRotation = 45.0
        }
    }

    fun updateData(dataPoints: List<NetWorthDataPointDTO>?) {
        data.clear()

        if (dataPoints.isNullOrEmpty()) {
            return
        }

        this.dataPoints = dataPoints

        val assetsSeries = Series<String, Number>()
        val liabilitiesSeries = Series<String, Number>()
        val netWorthSeries = Series<String, Number>()

        preferencesService?.let { prefs ->
            assetsSeries.name = prefs.translate(TranslationKeys.HOME_NET_WORTH_ASSETS)
            liabilitiesSeries.name = prefs.translate(TranslationKeys.HOME_NET_WORTH_LIABILITIES)
            netWorthSeries.name = prefs.translate(TranslationKeys.HOME_NET_WORTH_NET_WORTH)
        }

        dataPoints.forEach { dataPoint ->
            val periodLabel = UIUtils.formatShortMonthYear(dataPoint.period)

            assetsSeries.data.add(Data(periodLabel, dataPoint.assets))
            liabilitiesSeries.data.add(Data(periodLabel, dataPoint.liabilities))
            netWorthSeries.data.add(Data(periodLabel, dataPoint.netWorth))
        }

        data.addAll(assetsSeries, liabilitiesSeries, netWorthSeries)

        applyStyling()

        FxUtils.launchOnFxThread { addCurrentDateLine(dataPoints) }
    }

    /**
     * Wraps this chart with the crosshair: a vertical line that snaps to the month under the mouse, a dot on each
     * series and a box with the assets, the liabilities and the net worth of that month. Call it after [updateData]
     * and put the returned node in the screen in place of the chart.
     */
    fun withCrosshair(): StackPane {
        val headers =
            dataPoints.associate {
                UIUtils.formatShortMonthYear(it.period) to UIUtils.formatFullMonthYear(it.period)
            }

        return ChartCrosshair.wrapCategories(
            this,
            CHART_STYLE_CLASS,
            { UIUtils.formatCurrency(it) },
            { headers[it] ?: it },
        )
    }

    private fun applyStyling() {
        if (preferencesService == null) return

        data.forEachIndexed { i, series ->
            val styleClass =
                when (i) {
                    0 -> "assets-series"
                    1 -> "liabilities-series"
                    2 -> "net-worth-series"
                    else -> ""
                }

            if (series.node != null && styleClass.isNotEmpty()) {
                series.node.styleClass.add(styleClass)
            }
        }
    }

    private fun addCurrentDateLine(dataPoints: List<NetWorthDataPointDTO>) {
        currentDateLine?.let { plotChildren.remove(it) }

        val currentIndex = dataPoints.indexOfFirst { it.period == currentMonth }

        if (currentIndex == -1) {
            return
        }

        currentDateLine =
            Line().apply {
                styleClass.add("current-date-line")
                strokeWidth = 2.0
                strokeDashArray.addAll(5.0, 5.0)
            }

        layoutBoundsProperty().addListener { _, _, _ ->
            updateCurrentDateLinePosition(currentIndex, dataPoints.size)
        }

        plotChildren.add(currentDateLine)

        layoutPlotChildren()
        updateCurrentDateLinePosition(currentIndex, dataPoints.size)
    }

    private fun updateCurrentDateLinePosition(
        currentIndex: Int,
        totalPoints: Int,
    ) {
        val line = currentDateLine ?: return
        if (totalPoints == 0) return

        val xAxis = xAxis as CategoryAxis
        val yAxis = yAxis as NumberAxis

        if (xAxis.categories.isEmpty() || currentIndex >= xAxis.categories.size) {
            return
        }

        val xPosition = xAxis.getDisplayPosition(xAxis.categories[currentIndex])

        line.startX = xPosition
        line.endX = xPosition
        line.startY = 0.0
        line.endY = yAxis.height
    }
}
