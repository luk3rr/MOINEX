/*
 * Filename: CircularProgressBar.kt (original filename: CircularProgressBar.java)
 * Created on: December 15, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.chart

import javafx.geometry.VPos
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.scene.shape.ArcType
import javafx.scene.text.TextAlignment
import org.moinex.service.PreferencesService
import org.moinex.util.UIUtils

class CircularProgressBar(
    radius: Double,
    private val progressWidth: Double,
) : Canvas(radius, radius) {
    private val progressColor: Color = Color.web(BELIZE_BLUE)
    private val backgroundColor: Color = Color.web(LIGHT_GRAY)
    private val fontColor: Color = Color.web(BLACK)
    var preferencesService: PreferencesService? = null

    companion object {
        private const val BELIZE_BLUE = "#3498db"
        private const val LIGHT_GRAY = "#D3D3D3"
        private const val BLACK = "#000000"
    }

    init {
        graphicsContext2D.apply {
            textAlign = TextAlignment.CENTER
            textBaseline = VPos.CENTER
        }
    }

    fun draw(percent: Double) {
        val clampedPercent = percent.coerceIn(0.0, 100.0)
        val gc = graphicsContext2D

        gc.clearRect(0.0, 0.0, width, height)

        gc.stroke = backgroundColor
        gc.lineWidth = progressWidth
        gc.strokeArc(
            progressWidth / 2,
            progressWidth / 2,
            width - progressWidth,
            height - progressWidth,
            0.0,
            360.0,
            ArcType.OPEN,
        )

        gc.stroke = progressColor
        gc.lineWidth = progressWidth
        gc.strokeArc(
            progressWidth / 2,
            progressWidth / 2,
            width - progressWidth,
            height - progressWidth,
            90.0,
            -(clampedPercent / 100 * 360),
            ArcType.OPEN,
        )

        gc.fill = fontColor
        val formattedPercent =
            preferencesService?.let { UIUtils.formatPercentage(clampedPercent, it) }
                ?: String.format("%.1f %%", clampedPercent)
        gc.fillText(formattedPercent, width / 2, height / 2)
    }
}
