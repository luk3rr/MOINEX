/*
 * Filename: SparklineGeometry.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import java.math.BigDecimal

enum class SparklineTrend {
    UP,
    DOWN,
    FLAT,
}

/**
 * The maths behind a sparkline, kept apart from JavaFX so it can be tested.
 */
object SparklineGeometry {
    const val MIN_POINTS = 2

    /**
     * Points of the line inside a [width] x [height] box with [padding] on every side. The first value is at the left
     * and the highest value at the top. A series that never changes is drawn as a flat line in the middle. Fewer than
     * [MIN_POINTS] values give no line.
     */
    fun points(
        values: List<BigDecimal>,
        width: Double,
        height: Double,
        padding: Double = 2.0,
    ): List<Pair<Double, Double>> {
        if (values.size < MIN_POINTS) return emptyList()

        val min = values.min().toDouble()
        val max = values.max().toDouble()
        val range = max - min
        val usableWidth = width - 2 * padding
        val usableHeight = height - 2 * padding
        val step = usableWidth / (values.size - 1)

        return values.mapIndexed { index, value ->
            val x = padding + step * index
            val y =
                if (range == 0.0) {
                    height / 2
                } else {
                    padding + usableHeight * (1 - (value.toDouble() - min) / range)
                }
            x to y
        }
    }

    fun trend(values: List<BigDecimal>): SparklineTrend {
        if (values.size < MIN_POINTS) return SparklineTrend.FLAT

        return when (values.last().compareTo(values.first())) {
            1 -> SparklineTrend.UP
            -1 -> SparklineTrend.DOWN
            else -> SparklineTrend.FLAT
        }
    }
}
