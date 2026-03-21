/*
 * Filename: AnimationUtils.kt (original filename: AnimationUtils.java)
 * Created on: October 12, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.common.util

import javafx.animation.FadeTransition
import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.animation.TranslateTransition
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.stage.Stage
import javafx.util.Duration
import org.moinex.common.constant.Constants
import kotlin.math.ceil

object AnimationUtils {
    /**
     * Animates a bar chart
     *
     * @param data The data to be animated
     * @param targetValue The target value for the bar
     */
    fun xyChartAnimation(
        data: XYChart.Data<String, Number>,
        targetValue: Double,
    ) {
        val currentValue = SimpleDoubleProperty(0.0)

        currentValue.addListener { _, _, newVal ->
            data.yValue = newVal
        }

        Timeline(
            KeyFrame(
                Duration.seconds(Constants.XYBAR_CHART_ANIMATION_DURATION),
                KeyValue(currentValue, targetValue, Interpolator.EASE_BOTH),
            ),
        ).play()
    }

    /**
     * Animates a stacked bar chart
     *
     * @param data The data to be animated
     * @param targetValues The target values for each part of the stacked bar
     */
    fun stackedXYChartAnimation(
        data: List<XYChart.Data<String, Number>>,
        targetValues: List<Double>,
    ) {
        require(data.size == targetValues.size) {
            "Data and targetValues lists must have the same size."
        }

        data.forEach { it.yValue = 0.0 }

        val increments = targetValues.map { it / Constants.XYBAR_CHART_ANIMATION_FRAMES }

        val timeline =
            Timeline().apply {
                (0 until Constants.XYBAR_CHART_ANIMATION_FRAMES).forEach { frame ->
                    keyFrames.add(
                        KeyFrame(
                            Duration.seconds(
                                Constants.XYBAR_CHART_ANIMATION_DURATION /
                                    Constants.XYBAR_CHART_ANIMATION_FRAMES * (frame + 1),
                            ),
                            {
                                var accumulatedValue = 0.0

                                data.indices.forEach { i ->
                                    accumulatedValue += increments[i]
                                    val newYValue = accumulatedValue.coerceAtMost(targetValues[i])
                                    data[i].yValue = newYValue
                                }
                            },
                        ),
                    )
                }

                setOnFinished {
                    data.indices.forEach { i ->
                        data[i].yValue = targetValues[i]
                    }
                }
            }

        timeline.play()
    }

    /**
     * Sets the bounds of the Y axis of a bar chart dynamically
     *
     * @param numberAxis The Y axis to be updated
     * @param maxValue The maximum value of the data
     */
    fun setDynamicYAxisBounds(
        numberAxis: NumberAxis,
        maxValue: Double,
    ) {
        numberAxis.apply {
            isAutoRanging = false
            lowerBound = 0.0
            upperBound = ceil(maxValue / 10) * 10
            tickUnit = ceil(upperBound / Constants.XYBAR_CHART_TICKS / 10) * 10
        }
    }

    /**
     * Applies a fade-in animation to the window
     *
     * @param stage The stage to apply the animation
     */
    fun applyFadeInAnimation(stage: Stage) {
        FadeTransition(
            Duration.seconds(Constants.FADE_IN_ANIMATION_DURATION),
            stage.scene.root,
        ).apply {
            fromValue = 0.0
            toValue = 1.0
            play()
        }
    }

    /**
     * Applies a fade-out animation to the window
     *
     * @param stage The stage to apply the animation
     */
    fun applyFadeOutAnimation(stage: Stage) {
        FadeTransition(
            Duration.seconds(Constants.FADE_OUT_ANIMATION_DURATION),
            stage.scene.root,
        ).apply {
            fromValue = 1.0
            toValue = 0.0
            play()
        }
    }

    /**
     * Applies a slide-in animation to the window
     *
     * @param stage The stage to apply the animation
     */
    fun applySlideInAnimation(stage: Stage) {
        TranslateTransition(
            Duration.seconds(Constants.SLIDE_ANIMATION_DURATION),
            stage.scene.root,
        ).apply {
            fromX = -stage.width
            toX = 0.0
            play()
        }
    }
}
