/*
 * Filename: MarketDataFreshness.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import java.time.Duration
import java.time.LocalDateTime

/**
 * When market data is recent enough to skip a call to Yahoo. The automatic refresh on startup leaves fresh data
 * alone; the refresh buttons ignore this and always ask.
 */
object MarketDataFreshness {
    val TTL: Duration = Duration.ofMinutes(60)

    /**
     * True when [lastUpdate] is less than [ttl] before [now]. A missing date, or one in the future (the clock moved
     * back), counts as stale.
     */
    fun isFresh(
        lastUpdate: LocalDateTime?,
        now: LocalDateTime,
        ttl: Duration = TTL,
    ): Boolean {
        if (lastUpdate == null) return false

        val age = Duration.between(lastUpdate, now)
        return !age.isNegative && age < ttl
    }
}
