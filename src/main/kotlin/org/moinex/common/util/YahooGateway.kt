/*
 * Filename: YahooGateway.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.util

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.moinex.common.ClockProvider
import org.moinex.exception.MoinexException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * The single entry point for calls that reach Yahoo Finance. It lets at most [maxConcurrentCalls] run at once and,
 * after a call reports a rate limit, refuses every new call until a cooldown ends. The cooldown starts at
 * [baseCooldown], doubles with each consecutive rate limit up to [maxCooldown], and resets on the first call that
 * finishes without one.
 */
class YahooGateway(
    private val clockProvider: ClockProvider,
    maxConcurrentCalls: Int = DEFAULT_MAX_CONCURRENT_CALLS,
    private val baseCooldown: Duration = DEFAULT_BASE_COOLDOWN,
    private val maxCooldown: Duration = DEFAULT_MAX_COOLDOWN,
) {
    companion object {
        const val DEFAULT_MAX_CONCURRENT_CALLS = 2
        val DEFAULT_BASE_COOLDOWN: Duration = Duration.ofMinutes(5)
        val DEFAULT_MAX_COOLDOWN: Duration = Duration.ofMinutes(60)

        const val ERROR_KIND_FIELD = "error_kind"
        const val RATE_LIMIT_KIND = "rate_limit"

        private val RATE_LIMIT_MESSAGE_MARKERS =
            listOf("429", "rate limit", "ratelimit", "rate_limit", "too many requests")
        private const val MAX_RESULT_DEPTH = 2
        private const val MAX_COOLDOWN_DOUBLINGS = 20
    }

    private val logger = LoggerFactory.getLogger(YahooGateway::class.java)
    private val semaphore = Semaphore(maxConcurrentCalls)
    private val stateLock = Any()

    private var cooldownEnd: LocalDateTime? = null
    private var consecutiveRateLimits = 0

    @Volatile
    var onCooldownStarted: ((LocalDateTime) -> Unit)? = null

    /**
     * The moment calls are accepted again, or null when no cooldown is running.
     */
    fun cooldownUntil(): LocalDateTime? =
        synchronized(stateLock) {
            cooldownEnd?.takeIf { it.isAfter(clockProvider.now()) }
        }

    /**
     * Marks the calls made inside [ignoringCooldown] as asked for by the user, so the cooldown does not refuse them.
     */
    private object IgnoreCooldown : AbstractCoroutineContextElement(Key) {
        object Key : CoroutineContext.Key<IgnoreCooldown>
    }

    /**
     * Runs [block] with the cooldown switched off for every call it makes, as the user asked for that update
     * explicitly. The concurrency limit still applies. A call that succeeds ends the cooldown; one that is rate
     * limited while a cooldown is running changes nothing.
     */
    suspend fun <T> ignoringCooldown(block: suspend () -> T): T = withContext(IgnoreCooldown) { block() }

    /**
     * Runs [block], the real call to [script] for [symbolCount] symbols, under the concurrency limit.
     * @throws MoinexException.YahooCooldownException while a cooldown is running; [block] is not run then, unless
     * the call is made inside [ignoringCooldown]
     */
    suspend fun call(
        script: String,
        symbolCount: Int,
        block: suspend () -> JSONObject,
    ): JSONObject {
        val ignoreCooldown = currentCoroutineContext()[IgnoreCooldown.Key] != null

        if (!ignoreCooldown) failIfCooling(script)

        return semaphore.withPermit {
            if (!ignoreCooldown) failIfCooling(script)

            val startedAt = System.nanoTime()
            val outcome = runCatching { block() }
            val millis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis()

            val rateLimited =
                outcome.fold(
                    onSuccess = { containsRateLimit(it) },
                    onFailure = { messageIndicatesRateLimit(it.message) },
                )

            when {
                rateLimited -> openCooldown()
                outcome.isSuccess -> resetCooldown()
            }

            logger.info(
                "[yahoo] script={} symbols={} duration={}ms result={}",
                script,
                symbolCount,
                millis,
                when {
                    rateLimited -> "rate_limit"
                    outcome.isSuccess -> "ok"
                    else -> "error"
                },
            )

            outcome.getOrThrow()
        }
    }

    private fun failIfCooling(script: String) {
        val until = cooldownUntil() ?: return

        logger.info("[yahoo] script={} refused: cooldown until {}", script, until)
        throw MoinexException.YahooCooldownException(until, "Yahoo calls are paused until $until")
    }

    private fun openCooldown() {
        val until =
            synchronized(stateLock) {
                val now = clockProvider.now()
                if (cooldownEnd?.isAfter(now) == true) return

                consecutiveRateLimits++
                val factor = 1L shl (consecutiveRateLimits - 1).coerceAtMost(MAX_COOLDOWN_DOUBLINGS)
                val length = baseCooldown.multipliedBy(factor).coerceAtMost(maxCooldown)
                now.plus(length).also { cooldownEnd = it }
            }

        logger.warn("[yahoo] rate limit: calls paused until {}", until)
        onCooldownStarted?.let { runCatching { it(until) } }
    }

    private fun resetCooldown() =
        synchronized(stateLock) {
            consecutiveRateLimits = 0
            cooldownEnd = null
        }

    private fun Duration.coerceAtMost(limit: Duration): Duration = if (this > limit) limit else this

    private fun containsRateLimit(
        json: JSONObject,
        depth: Int = 0,
    ): Boolean =
        json.optString(ERROR_KIND_FIELD) == RATE_LIMIT_KIND ||
            (
                depth < MAX_RESULT_DEPTH &&
                    json.keySet().any { key ->
                        json.optJSONObject(key)?.let { containsRateLimit(it, depth + 1) } ?: false
                    }
            )

    private fun messageIndicatesRateLimit(message: String?): Boolean =
        message?.lowercase()?.let { text -> RATE_LIMIT_MESSAGE_MARKERS.any { it in text } } ?: false
}
