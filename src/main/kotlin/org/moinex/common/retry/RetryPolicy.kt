/*
 * Filename: RetryPolicy.kt
 * Created on: March 9, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.retry

import kotlinx.coroutines.delay
import org.moinex.config.RetryConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RetryException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)

private val defaultLogger: Logger = LoggerFactory.getLogger("RetryPolicy")

suspend fun <T> retry(
    config: RetryConfig = RetryConfig.API_CALLS,
    logger: Logger = defaultLogger,
    operationName: String = "Operation",
    operation: suspend () -> T,
): T {
    var lastError: Throwable? = null

    repeat(config.maxRetries) { attempt ->
        try {
            if (attempt > 0) {
                logger.info("$operationName - Attempt ${attempt + 1}/${config.maxRetries}")
            }

            return operation()
        } catch (e: Throwable) {
            if (!config.retryIf(e)) throw e

            lastError = e

            if (attempt == config.maxRetries - 1) {
                throw RetryException(
                    "$operationName failed after ${config.maxRetries} attempts",
                    e,
                )
            }

            val delayMs = config.delayFor(attempt + 1)

            logger.warn(
                "$operationName failed (attempt ${attempt + 1}/${config.maxRetries}): ${e.message}",
            )

            logger.info("Retrying in $delayMs ms")

            delay(delayMs)
        }
    }

    throw RetryException(
        "$operationName failed after ${config.maxRetries} attempts",
        lastError!!,
    )
}
