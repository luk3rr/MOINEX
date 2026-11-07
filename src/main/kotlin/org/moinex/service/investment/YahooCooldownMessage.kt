/*
 * Filename: YahooCooldownMessage.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The text that tells the user when the app will ask Yahoo again, shared by every place that shows it.
 */
object YahooCooldownMessage {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Fills the time of [until] into [template], the translated `yahoo.cooldown.message`.
     */
    fun text(
        template: String,
        until: LocalDateTime,
    ): String = template.format(until.format(timeFormatter))
}
