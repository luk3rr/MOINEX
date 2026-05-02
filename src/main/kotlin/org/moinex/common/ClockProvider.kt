/*
 * Filename: ClockProvider.kt
 * Created on: March 2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Component
class ClockProvider(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun now(): LocalDateTime = LocalDateTime.now(clock)

    fun today(): LocalDate = LocalDate.now(clock)

    fun currentMonth(): YearMonth = YearMonth.now(clock)

    fun getClock(): Clock = clock
}
