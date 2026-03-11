package org.moinex.config

import kotlin.math.pow
import kotlin.random.Random

data class RetryConfig(
    val maxRetries: Int = 7,
    val initialDelayMs: Long = 1000,
    val multiplier: Double = 1.5,
    val maxDelayMs: Long = 30000,
    val jitter: Boolean = true,
    val retryIf: (Throwable) -> Boolean = { true },
) {
    init {
        require(maxRetries > 0) { "maxRetries must be positive" }
        require(initialDelayMs > 0) { "initialDelayMs must be positive" }
        require(multiplier >= 1.0) { "multiplier must be >= 1.0" }
        require(maxDelayMs >= initialDelayMs) { "maxDelayMs must be >= initialDelayMs" }
    }

    fun delayFor(attempt: Int): Long {
        val base = (initialDelayMs * multiplier.pow((attempt - 1).toDouble())).toLong()
        val capped = minOf(base, maxDelayMs)

        if (!jitter) return capped

        val factor = Random.Default.nextDouble(0.5, 1.5)
        return (capped * factor).toLong()
    }

    companion object {
        val API_CALLS = RetryConfig()

        val MARKET_DATA =
            RetryConfig(
                initialDelayMs = 2000,
            )

        val BACEN_API =
            RetryConfig(
                maxRetries = 3,
                initialDelayMs = 1000,
            )

        val FUNDAMENTAL_ANALYSIS =
            RetryConfig(
                maxRetries = 5,
                initialDelayMs = 2000,
            )
    }
}
