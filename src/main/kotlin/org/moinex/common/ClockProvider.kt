/*
 * Filename: ClockProvider.kt
 * Created on: March 2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

@Component
class ClockProvider(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun now(): LocalDateTime = LocalDateTime.now(clock)

    fun getClock(): Clock = clock
}
