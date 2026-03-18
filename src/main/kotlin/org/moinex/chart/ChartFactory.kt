/*
 * Filename: ChartFactory.kt
 * Created on: March 18, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.chart

import javafx.collections.ObservableList
import javafx.scene.chart.PieChart
import org.moinex.service.PreferencesService
import org.springframework.stereotype.Component

@Component
class ChartFactory(
    private val preferencesService: PreferencesService,
) {
    fun createDoughnutChart(pieData: ObservableList<PieChart.Data>): DoughnutChart =
        DoughnutChart(pieData).apply {
            this.preferencesService = this@ChartFactory.preferencesService
        }

    fun createBudgetGroupTimelineChart(): BudgetGroupTimelineChart =
        BudgetGroupTimelineChart().apply {
            this.preferencesService = this@ChartFactory.preferencesService
        }

    fun createCircularProgressBar(
        radius: Double,
        width: Double,
    ): CircularProgressBar =
        CircularProgressBar(radius, width).apply {
            this.preferencesService = this@ChartFactory.preferencesService
        }

    fun createNetWorthLineChart(): NetWorthLineChart =
        NetWorthLineChart().apply {
            this.preferencesService = this@ChartFactory.preferencesService
        }
}
