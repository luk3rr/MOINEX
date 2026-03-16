/*
 * Filename: FxUtils.kt
 * Created on: March 16, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * JavaFX utilities for Kotlin with coroutine support.
 *
 * These helpers simplify switching between the JavaFX UI thread and
 * background threads using Kotlin coroutines while keeping code readable
 * and safe with respect to thread confinement rules of JavaFX.
 */

package org.moinex.util

import javafx.application.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.moinex.config.AppDispatchers
import java.util.concurrent.CountDownLatch

object FxUtils {
    /**
     * Global coroutine scope used for JavaFX operations.
     *
     * The scope uses:
     * - SupervisorJob to prevent failure in one coroutine from cancelling others
     * - Dispatchers.JavaFx to ensure execution on the JavaFX Application Thread
     *
     * All asynchronous UI operations launched through this utility run inside
     * this scope.
     */
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.JavaFx)

    /**
     * Executes a block synchronously on the JavaFX Application Thread.
     *
     * If the current thread is already the FX thread, the block is executed
     * immediately. Otherwise the block is scheduled with `Platform.runLater`
     * and the calling thread waits until execution completes.
     *
     * This method is useful when a result from a UI operation must be obtained
     * synchronously from a background thread.
     *
     * The implementation avoids busy waiting and guarantees proper memory
     * visibility between threads.
     *
     * @param action block to execute on the JavaFX thread
     * @return the value returned by the block
     */
    fun <T> onFxThread(action: () -> T): T {
        if (Platform.isFxApplicationThread()) {
            return action()
        }

        val latch = CountDownLatch(1)
        var result: Result<T>? = null

        Platform.runLater {
            result =
                runCatching {
                    action()
                }
            latch.countDown()
        }

        latch.await()

        return result!!.getOrThrow()
    }

    /**
     * Launches a coroutine on the JavaFX Application Thread.
     *
     * This function is intended for asynchronous UI work that should not block
     * the caller thread. The coroutine runs inside the shared UI scope.
     *
     * Typical uses include:
     * - updating UI controls
     * - loading JavaFX scenes
     * - reacting to background results
     *
     * @param block suspend function executed on the FX thread
     * @return the Job representing the launched coroutine
     */
    fun launchOnFxThread(block: suspend () -> Unit): Job =
        uiScope.launch {
            block()
        }

    /**
     * Launches a coroutine on a background thread pool optimized for I/O work.
     *
     * This function should be used for operations that must not run on the
     * JavaFX thread, such as:
     *
     * - database access
     * - network requests
     * - disk I/O
     *
     * The coroutine is still bound to the shared application scope so it can
     * safely interact with UI operations when needed.
     *
     * @param block suspend function executed in the background
     * @return the Job representing the launched coroutine
     */
    fun launchOnBackground(block: suspend () -> Unit): Job =
        uiScope.launch(AppDispatchers.IO) {
            block()
        }

    /**
     * Executes I/O work in the background and delivers the result to the UI thread.
     *
     * This helper provides a convenient pattern for common UI applications:
     *
     * 1. Perform a slow operation in the background
     * 2. Update the UI once the result becomes available
     *
     * Internally this uses `withContext` to switch execution contexts while
     * preserving structured concurrency.
     *
     * @param background suspend function executed in the background
     * @param onUI suspend function executed on the FX thread with the result
     * @return the Job representing the launched coroutine
     */
    fun <T> launchBackgroundThenUI(
        background: suspend () -> T,
        onUI: suspend (T) -> Unit,
    ): Job =
        uiScope.launch {
            val result =
                withContext(AppDispatchers.IO) {
                    background()
                }

            onUI(result)
        }

    /**
     * Switches execution to the JavaFX Application Thread and returns the result.
     *
     * This suspend function is useful inside coroutines when UI interaction
     * is required while preserving a sequential programming style.
     *
     * Example:
     *
     * val data = withBackground { repository.load() }
     * withFxThread { table.items.setAll(data) }
     *
     * @param block suspend function executed on the FX thread
     * @return the value returned by the block
     */
    suspend fun <T> withFxThread(block: suspend () -> T): T =
        withContext(AppDispatchers.UI) {
            block()
        }

    /**
     * Switches execution to a background dispatcher optimized for I/O tasks.
     *
     * This helper allows suspending code to perform expensive operations
     * without blocking the JavaFX thread while keeping a simple sequential flow.
     *
     * @param block suspend function executed in background
     * @return the value returned by the block
     */
    suspend fun <T> withBackground(block: suspend () -> T): T =
        withContext(AppDispatchers.IO) {
            block()
        }

    /**
     * Switches execution to a CPU-optimized dispatcher for heavy computations.
     *
     * This helper is designed for CPU-intensive operations that require all
     * available processor cores, such as:
     *
     * - complex calculations
     * - data processing and transformations
     * - heavy computations that don't involve I/O
     *
     * Uses Dispatchers.Default which limits concurrency to the number of CPU cores.
     *
     * @param block suspend function executed on CPU dispatcher
     * @return the value returned by the block
     */
    suspend fun <T> withCpu(block: suspend () -> T): T =
        withContext(AppDispatchers.CPU) {
            block()
        }

    /**
     * Launches a coroutine on a CPU-optimized dispatcher for heavy computations.
     *
     * This function should be used for CPU-intensive background work that should
     * not block the caller thread, such as:
     *
     * - complex calculations
     * - data processing and transformations
     * - heavy computations
     *
     * The coroutine is bound to the shared application scope.
     *
     * @param block suspend function executed on CPU dispatcher
     * @return the Job representing the launched coroutine
     */
    fun launchOnCpu(block: suspend () -> Unit): Job =
        uiScope.launch(AppDispatchers.CPU) {
            block()
        }

    /**
     * Executes heavy CPU work in the background and delivers the result to the UI thread.
     *
     * This helper provides a convenient pattern for CPU-intensive operations:
     *
     * 1. Perform a heavy computation on a CPU-optimized dispatcher
     * 2. Update the UI once the result becomes available
     *
     * @param background suspend function with heavy computation
     * @param onUI suspend function executed on the FX thread with the result
     * @return the Job representing the launched coroutine
     */
    fun <T> launchCpuThenUI(
        background: suspend () -> T,
        onUI: suspend (T) -> Unit,
    ): Job =
        uiScope.launch {
            val result =
                withContext(AppDispatchers.CPU) {
                    background()
                }

            onUI(result)
        }
}
