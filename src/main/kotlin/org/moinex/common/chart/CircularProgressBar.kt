/*
 * Filename: CircularProgressBar.kt (original filename: CircularProgressBar.java)
 * Created on: December 15, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.common.chart

import javafx.geometry.VPos
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.scene.shape.ArcType
import javafx.scene.text.TextAlignment
import org.moinex.common.ui.ThemeColors
import org.moinex.common.util.UIUtils

class CircularProgressBar(
    radius: Double,
    private val progressWidth: Double,
    isDarkMode: Boolean,
) : Canvas(radius, radius) {
    private val progressColor: Color = ThemeColors.progress(isDarkMode)
    private val backgroundColor: Color = ThemeColors.allocTrack(isDarkMode)
    private val fontColor: Color = ThemeColors.textPrimary(isDarkMode)

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
        val formattedPercent = UIUtils.formatPercentage(clampedPercent)
        gc.fillText(formattedPercent, width / 2, height / 2)
    }
}
