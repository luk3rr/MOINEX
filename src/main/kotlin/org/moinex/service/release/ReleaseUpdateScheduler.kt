/*
 * Filename: ReleaseUpdateScheduler.kt
 * Created on: November 2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.release

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.moinex.common.util.FxUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.coroutines.cancellation.CancellationException

@Component
class ReleaseUpdateScheduler(
    private val releaseUpdateService: ReleaseUpdateService,
) {
    private val logger = LoggerFactory.getLogger(ReleaseUpdateScheduler::class.java)

    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job =
            FxUtils.launchOnBackground {
                while (true) {
                    checkSafely()
                    delay(CHECK_INTERVAL_MS)
                }
            }
    }

    @PreDestroy
    fun stop() {
        job?.cancel()
    }

    internal fun checkSafely(): Boolean =
        try {
            releaseUpdateService.checkForUpdate()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.debug("Update check failed", e)
            false
        }

    companion object {
        const val CHECK_INTERVAL_MS: Long = 24L * 60 * 60 * 1000
    }
}
