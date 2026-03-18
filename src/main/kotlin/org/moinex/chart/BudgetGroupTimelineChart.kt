/*
 * Filename: BudgetGroupTimelineChart.kt (original filename: BudgetGroupTimelineChart.java)
 * Created on: [Original date]
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.chart

import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import org.moinex.common.extension.isZero
import org.moinex.constants.TranslationKeys
import org.moinex.model.dto.BudgetGroupHistoricalDataDTO
import org.moinex.service.PreferencesService
import org.moinex.util.Constants
import org.moinex.util.UIUtils
import java.math.BigDecimal

class BudgetGroupTimelineChart : LineChart<String, Number>(CategoryAxis(), NumberAxis()) {
    var preferencesService: PreferencesService? = null

    init {
        setupChart()
        stylesheets.add(javaClass.getResource(Constants.TIMELINE_CHART_STYLE_SHEET)!!.toExternalForm())
    }

    private fun setupChart() {
        isLegendVisible = true
        animated = true
        createSymbols = false

        (yAxis as NumberAxis).apply {
            isAutoRanging = true
            isForceZeroInRange = true
        }

        (xAxis as CategoryAxis).apply {
            isAutoRanging = true
            tickLabelRotation = 45.0
        }
    }

    fun updateData(historicalData: List<BudgetGroupHistoricalDataDTO>?) {
        data.clear()

        if (historicalData.isNullOrEmpty()) {
            return
        }

        val groupedData = historicalData.groupBy { it.groupName }

        groupedData.forEach { (groupName, dataList) ->
            val actualSeriesData = Series<String, Number>()
            val targetSeriesData = Series<String, Number>()

            val actualLabel = preferencesService?.translate(TranslationKeys.PLAN_TIMELINE_ACTUAL) ?: ""
            val targetLabel = preferencesService?.translate(TranslationKeys.PLAN_TIMELINE_TARGET) ?: ""

            actualSeriesData.name = "$groupName ($actualLabel)"
            targetSeriesData.name = "$groupName ($targetLabel)"

            dataList.forEach { data ->
                val periodLabel = UIUtils.formatShortMonthYear(data.period, preferencesService!!)

                val actualAmount = data.spentAmount
                var targetAmount = data.targetAmount

                if (actualAmount.isZero()) {
                    targetAmount = targetAmount.add(BigDecimal("0.01"))
                }

                actualSeriesData.data.add(Data(periodLabel, actualAmount))
                targetSeriesData.data.add(Data(periodLabel, targetAmount))
            }

            data.addAll(actualSeriesData, targetSeriesData)
        }

        applyStyling()
    }

    private fun applyStyling() {
        val prefs = preferencesService ?: return

        val actualLabel = prefs.translate(TranslationKeys.PLAN_TIMELINE_ACTUAL)
        val targetLabel = prefs.translate(TranslationKeys.PLAN_TIMELINE_TARGET)

        data.forEach { series ->
            series.node?.let { node ->
                val seriesName = series.name
                val groupName =
                    seriesName
                        .replace(" ($actualLabel)", "")
                        .replace(" ($targetLabel)", "")

                val isActual = seriesName.contains(actualLabel)
                val typeLabel = if (isActual) actualLabel else targetLabel

                val tooltipText = "$groupName - $typeLabel"
                UIUtils.addTooltipToNode(node, tooltipText)
            }
        }
    }

    fun setXAxisLabel(label: String) {
        (xAxis as CategoryAxis).label = label
    }

    fun setYAxisLabel(label: String) {
        yAxis.label = label
    }
}
