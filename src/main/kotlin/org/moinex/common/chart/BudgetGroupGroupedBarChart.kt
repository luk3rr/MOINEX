/*
 * Filename: BudgetGroupGroupedBarChart.kt
 * Created on: April 26, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.chart

import javafx.scene.chart.BarChart
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.shape.Line
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.model.dto.BudgetGroupHistoricalDataDTO
import org.moinex.service.PreferencesService
import java.math.BigDecimal
import java.math.RoundingMode

class BudgetGroupGroupedBarChart : BarChart<String, Number>(CategoryAxis(), NumberAxis()) {
    var preferencesService: PreferencesService? = null

    private val targetByDataPoint = mutableMapOf<Data<String, Number>, BigDecimal>()
    private val tooltipByDataPoint = mutableMapOf<Data<String, Number>, String>()
    private val targetLines = mutableListOf<Line>()

    companion object {
        private const val BUDGET_TARGET_MARKER_STYLE = "budget-target-marker"
        private const val MIN_BAR_WIDTH = 1.0
    }

    init {
        isLegendVisible = true
        verticalGridLinesVisible = false
        animated = false
        barGap = 2.0
        categoryGap = 20.0

        (yAxis as NumberAxis).apply {
            isAutoRanging = true
            isForceZeroInRange = true
        }

        UIUtils.formatCurrencyYAxis(yAxis as NumberAxis)

        (xAxis as CategoryAxis).apply {
            isAutoRanging = true
        }
    }

    override fun layoutPlotChildren() {
        super.layoutPlotChildren()

        plotChildren.removeAll(targetLines.toSet())
        targetLines.clear()

        val catAxis = xAxis as CategoryAxis
        val numAxis = yAxis as NumberAxis

        val maxN =
            catAxis.categories
                .maxOfOrNull { category ->
                    data.count { series -> series.data.any { it.xValue == category && it.node != null } }
                }?.coerceAtLeast(1) ?: 1

        val availableSpace = catAxis.categorySpacing - categoryGap
        val uniformBarWidth = ((availableSpace - (maxN - 1) * barGap) / maxN).coerceAtLeast(MIN_BAR_WIDTH)

        catAxis.categories.forEach { category ->
            val present =
                data.mapNotNull { series ->
                    series.data.firstOrNull { it.xValue == category && it.node != null }
                }

            if (present.isEmpty()) return@forEach

            val n = present.size
            val catCenter = catAxis.getDisplayPosition(category)
            val totalWidth = n * uniformBarWidth + (n - 1) * barGap
            val startX = catCenter - totalWidth / 2.0

            present.forEachIndexed { i, dataPoint ->
                val node = dataPoint.node ?: return@forEachIndexed

                val barX = startX + i * (uniformBarWidth + barGap)
                node.layoutX = barX
                node.resize(uniformBarWidth, node.layoutBounds.height)

                tooltipByDataPoint[dataPoint]?.let { UIUtils.addTooltipToNode(node, it) }

                val target = targetByDataPoint[dataPoint] ?: return@forEachIndexed
                val yPos = numAxis.getDisplayPosition(target)
                val line =
                    Line(barX - 1, yPos, barX + uniformBarWidth + 1, yPos).apply {
                        styleClass.add(BUDGET_TARGET_MARKER_STYLE)
                    }
                targetLines.add(line)
                plotChildren.add(line)
            }
        }
    }

    fun updateData(historicalData: List<BudgetGroupHistoricalDataDTO>?) {
        plotChildren.removeAll(targetLines.toSet())
        targetLines.clear()
        targetByDataPoint.clear()
        tooltipByDataPoint.clear()
        data.clear()

        if (historicalData.isNullOrEmpty()) return

        val actualLabel = preferencesService?.translate(TranslationKeys.PLAN_TIMELINE_ACTUAL) ?: ""
        val targetLabel = preferencesService?.translate(TranslationKeys.PLAN_TIMELINE_TARGET) ?: ""
        val pctLabel = preferencesService?.translate(TranslationKeys.PLAN_TIMELINE_PCT_OF_TARGET) ?: ""

        val sortedPeriods = historicalData.map { it.period }.distinct().sorted()
        val groupNames = historicalData.map { it.groupName }.distinct()

        val byGroupAndPeriod =
            historicalData.associateBy { UIUtils.formatShortMonthYear(it.period) to it.groupName }

        groupNames.forEach { groupName ->
            val series = Series<String, Number>()
            series.name = groupName

            sortedPeriods.forEach { period ->
                val periodLabel = UIUtils.formatShortMonthYear(period)
                val entry = byGroupAndPeriod[periodLabel to groupName] ?: return@forEach

                val spent = entry.spentAmount
                val target = entry.targetAmount
                val pct =
                    if (target > BigDecimal.ZERO) {
                        spent
                            .divide(target, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal("100"))
                    } else {
                        BigDecimal.ZERO
                    }

                val tooltipText =
                    "$groupName – $periodLabel\n" +
                        "$actualLabel: ${UIUtils.formatCurrency(spent)}\n" +
                        "$targetLabel: ${UIUtils.formatCurrency(target)}\n" +
                        "$pctLabel: ${UIUtils.formatPercentage(pct)}"

                val dataPoint = Data<String, Number>(periodLabel, spent)
                targetByDataPoint[dataPoint] = target
                tooltipByDataPoint[dataPoint] = tooltipText

                series.data.add(dataPoint)
            }

            data.add(series)
        }
    }
}
