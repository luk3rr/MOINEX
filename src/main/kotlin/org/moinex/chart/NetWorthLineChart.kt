/*
 * Filename: NetWorthLineChart.kt (original filename: NetWorthLineChart.java)
 * Created on: [Original date]
 * Author: [Original author]
 *
 * Migrated to Kotlin on 16/03/2026
 */

package org.moinex.chart

import javafx.geometry.Side
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.shape.Line
import javafx.util.StringConverter
import org.moinex.constants.Constants
import org.moinex.constants.TranslationKeys
import org.moinex.model.dto.NetWorthDataPointDTO
import org.moinex.service.PreferencesService
import org.moinex.util.FxUtils
import org.moinex.util.UIUtils
import java.time.YearMonth

class NetWorthLineChart : LineChart<String, Number>(CategoryAxis(), NumberAxis()) {
    var preferencesService: PreferencesService? = null
    private var currentDateLine: Line? = null
    private val currentMonth: YearMonth = YearMonth.now()
    private var dataPoints: List<NetWorthDataPointDTO> = emptyList()

    init {
        setupChart()
        stylesheets.add(javaClass.getResource(Constants.TIMELINE_CHART_STYLE_SHEET)!!.toExternalForm())
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
            val periodLabel = UIUtils.formatShortMonthYear(dataPoint.period, preferencesService!!)

            assetsSeries.data.add(Data(periodLabel, dataPoint.assets))
            liabilitiesSeries.data.add(Data(periodLabel, dataPoint.liabilities))
            netWorthSeries.data.add(Data(periodLabel, dataPoint.netWorth))
        }

        data.addAll(assetsSeries, liabilitiesSeries, netWorthSeries)

        applyStyling()

        FxUtils.launchOnFxThread {
            addTooltipsToAxisLabels(dataPoints)
            addCurrentDateLine(dataPoints)
        }
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

    private fun addTooltipsToAxisLabels(dataPoints: List<NetWorthDataPointDTO>) {
        val xAxis = xAxis as CategoryAxis

        widthProperty().addListener { _, _, _ ->
            FxUtils.launchOnFxThread {
                reapplyTooltips(dataPoints, xAxis)
                addCurrentDateLine(dataPoints)
            }
        }

        heightProperty().addListener { _, _, _ ->
            FxUtils.launchOnFxThread {
                reapplyTooltips(dataPoints, xAxis)
                addCurrentDateLine(dataPoints)
            }
        }

        FxUtils.launchOnFxThread {
            reapplyTooltips(dataPoints, xAxis)
            addCurrentDateLine(dataPoints)
        }
    }

    private fun reapplyTooltips(
        dataPoints: List<NetWorthDataPointDTO>,
        xAxis: CategoryAxis,
    ) {
        dataPoints.forEach { dataPoint ->
            val periodLabel = UIUtils.formatShortMonthYear(dataPoint.period, preferencesService!!)
            val tooltipText = createTooltipText(dataPoint)
            UIUtils.addTooltipToAxisLabel(xAxis, periodLabel, tooltipText)
        }
    }

    private fun createTooltipText(dataPoint: NetWorthDataPointDTO): String {
        val prefs = preferencesService!!
        val assetsLabel = prefs.translate(TranslationKeys.HOME_NET_WORTH_ASSETS)
        val liabilitiesLabel = prefs.translate(TranslationKeys.HOME_NET_WORTH_LIABILITIES)
        val netWorthLabel = prefs.translate(TranslationKeys.HOME_NET_WORTH_NET_WORTH)
        val periodLabel = UIUtils.formatFullMonthYear(dataPoint.period, prefs)

        return """
            $periodLabel
            $assetsLabel: ${UIUtils.formatCurrency(dataPoint.assets)}
            $liabilitiesLabel: ${UIUtils.formatCurrency(dataPoint.liabilities)}
            $netWorthLabel: ${UIUtils.formatCurrency(dataPoint.netWorth)}
            """.trimIndent()
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
