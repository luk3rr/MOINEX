/*
 * Filename: SparklineCell.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import javafx.geometry.Pos
import javafx.scene.canvas.Canvas
import javafx.scene.control.TableCell
import java.math.BigDecimal

/**
 * Table cell that draws the closing prices of a row as a small line, green when the period ended higher than it
 * started and red when lower. A row without enough prices shows a dash.
 */
class SparklineCell<S>(
    private val isDarkMode: () -> Boolean,
) : TableCell<S, List<BigDecimal>>() {
    companion object {
        const val WIDTH = 80.0
        const val HEIGHT = 24.0
        private const val LINE_WIDTH = 1.6
        private const val FLAT_OPACITY = 0.6
        private const val NO_DATA = "—"
    }

    init {
        alignment = Pos.CENTER
    }

    override fun updateItem(
        item: List<BigDecimal>?,
        empty: Boolean,
    ) {
        super.updateItem(item, empty)

        val points = if (empty || item == null) emptyList() else SparklineGeometry.points(item, WIDTH, HEIGHT)
        if (empty || item == null) {
            text = null
            graphic = null
        } else if (points.isEmpty()) {
            text = NO_DATA
            graphic = null
        } else {
            text = null
            graphic = draw(points, SparklineGeometry.trend(item))
        }
    }

    private fun draw(
        points: List<Pair<Double, Double>>,
        trend: SparklineTrend,
    ): Canvas =
        Canvas(WIDTH, HEIGHT).apply {
            val dark = isDarkMode()
            graphicsContext2D.stroke =
                when (trend) {
                    SparklineTrend.UP -> ThemeColors.positive(dark)
                    SparklineTrend.DOWN -> ThemeColors.negative(dark)
                    SparklineTrend.FLAT -> ThemeColors.textPrimary(dark).deriveColor(0.0, 1.0, 1.0, FLAT_OPACITY)
                }
            graphicsContext2D.lineWidth = LINE_WIDTH
            graphicsContext2D.beginPath()
            points.forEachIndexed { index, (x, y) ->
                if (index == 0) graphicsContext2D.moveTo(x, y) else graphicsContext2D.lineTo(x, y)
            }
            graphicsContext2D.stroke()
        }
}
