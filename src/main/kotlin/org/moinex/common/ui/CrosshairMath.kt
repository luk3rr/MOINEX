/*
 * Filename: CrosshairMath.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import java.time.LocalDate

/**
 * The lookups behind the chart crosshair, without JavaFX: which day the mouse is closest to and what each series was on
 * that day.
 */
object CrosshairMath {
    /**
     * The day among [days] (sorted, oldest first) closest to [target], given as an epoch day, or null when there are none.
     */
    fun nearestDay(
        days: List<LocalDate>,
        target: Double,
    ): LocalDate? {
        if (days.isEmpty()) return null

        var low = 0
        var high = days.size - 1
        while (low < high) {
            val middle = (low + high) / 2
            if (days[middle].toEpochDay() < target) low = middle + 1 else high = middle
        }

        val after = days[low]
        val before = days.getOrNull(low - 1) ?: return after

        return if (target - before.toEpochDay() <= after.toEpochDay() - target) before else after
    }

    /**
     * The value of a series (points sorted by date) on [day]: the point of that day or, failing it, the last one before.
     * Null before the series starts.
     */
    fun valueOn(
        points: List<Pair<LocalDate, Double>>,
        day: LocalDate,
    ): Double? {
        var low = 0
        var high = points.size - 1
        var found: Double? = null
        while (low <= high) {
            val middle = (low + high) / 2
            if (points[middle].first.isAfter(day)) {
                high = middle - 1
            } else {
                found = points[middle].second
                low = middle + 1
            }
        }

        return found
    }

    /**
     * The notes dated after [previousDay] up to [day], so that something that happened on a day without data shows on
     * the next day that has data. The first day also takes everything up to itself.
     */
    fun notesFor(
        notes: List<Pair<LocalDate, String>>,
        previousDay: LocalDate?,
        day: LocalDate,
    ): List<String> =
        notes
            .filter { (date, _) -> !date.isAfter(day) && (previousDay == null || date.isAfter(previousDay)) }
            .sortedBy { it.first }
            .map { it.second }

    /**
     * The day before [day] in [days] (sorted), or null when [day] is the first.
     */
    fun previousDay(
        days: List<LocalDate>,
        day: LocalDate,
    ): LocalDate? = days.lastOrNull { it.isBefore(day) }
}
