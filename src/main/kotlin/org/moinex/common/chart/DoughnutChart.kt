/*
 * Filename: DoughnutChart.kt (original filename: DoughnutChart.java)
 * Created on: October 7, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.common.chart

import javafx.animation.ScaleTransition
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.chart.PieChart
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.util.Duration
import org.moinex.common.util.UIUtils

class DoughnutChart(
    pieData: ObservableList<Data>,
) : PieChart(pieData) {
    private val innerCircle = Circle()
    private val centerLabel = Label()
    private val stackPane = StackPane()
    private val seriesTotal: Double
    private var showCenterLabel = true

    init {
        innerCircle.apply {
            fill = Color.WHITE
            stroke = Color.WHITE
        }

        pieData.forEach { data ->
            if (data.pieValue < 0) {
                data.pieValue = 0.0
            }
        }

        seriesTotal = pieData.sumOf { it.pieValue }

        data.forEach { data ->
            val node = data.node
            val percentage = (data.pieValue / seriesTotal) * 100
            val formattedPercent = UIUtils.formatPercentage(percentage)

            UIUtils.addTooltipToNode(node, formattedPercent)

            node.setOnMouseEntered {
                ScaleTransition(Duration.millis(100.0), node).apply {
                    toX = 1.1
                    toY = 1.1
                    play()
                }
            }

            node.setOnMouseExited {
                ScaleTransition(Duration.millis(100.0), node).apply {
                    toX = 1.0
                    toY = 1.0
                    play()
                }
            }
        }
    }

    override fun layoutChartChildren(
        top: Double,
        left: Double,
        contentWidth: Double,
        contentHeight: Double,
    ) {
        super.layoutChartChildren(top, left, contentWidth, contentHeight)

        addInnerCircleIfNotPresent()
        updateInnerCircleLayout()

        setCenterLabelTextStyle(
            UIUtils.formatCurrency(seriesTotal),
            "-fx-font-size: 16px; -fx-font-weight: bold;",
        )
    }

    private fun addInnerCircleIfNotPresent() {
        if (data.isNotEmpty()) {
            val pie = data[0].node
            (pie.parent as? Pane)?.let { parent ->
                if (!parent.children.contains(innerCircle)) {
                    parent.children.add(innerCircle)
                }

                if (showCenterLabel && !parent.children.contains(stackPane)) {
                    parent.children.add(stackPane)
                }
            }
        }

        if (showCenterLabel && !stackPane.children.contains(centerLabel)) {
            stackPane.children.add(centerLabel)
        }
    }

    private fun updateInnerCircleLayout() {
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = Double.MIN_VALUE
        var maxY = Double.MIN_VALUE

        data.forEach { data ->
            val bounds = data.node.boundsInParent

            if (bounds.minX < minX) minX = bounds.minX
            if (bounds.minY < minY) minY = bounds.minY
            if (bounds.maxX > maxX) maxX = bounds.maxX
            if (bounds.maxY > maxY) maxY = bounds.maxY
        }

        innerCircle.apply {
            centerX = minX + (maxX - minX) / 2
            centerY = minY + (maxY - minY) / 2
            radius = (maxX - minX) / 3.5
        }

        stackPane.apply {
            layoutX = innerCircle.centerX - width / 2
            layoutY = innerCircle.centerY - height / 2
            style = "-fx-background-color: transparent;"
        }
    }

    fun setShowCenterLabel(show: Boolean) {
        showCenterLabel = show
        stackPane.isVisible = show
        stackPane.isManaged = show
    }

    fun setCenterLabelTextStyle(
        text: String,
        style: String,
    ) {
        centerLabel.apply {
            this.text = text
            this.style = style
            minWidth = innerCircle.radius * 2
            minHeight = innerCircle.radius * 2
            alignment = Pos.CENTER
        }
    }
}
