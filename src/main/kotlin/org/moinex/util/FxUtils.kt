/*
 * Filename: FxUtils.kt
 * Created on: March 16, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * JavaFX utilities for Kotlin with coroutines support
 */

package org.moinex.util

import javafx.application.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Execute a block of code on the JavaFX Application Thread.
 * If already on the FX thread, executes immediately.
 * Otherwise, schedules execution via Platform.runLater.
 *
 * @param action The code block to execute on the FX thread
 */
fun <T> onFxThread(action: () -> T): T =
    if (Platform.isFxApplicationThread()) {
        action()
    } else {
        var result: T? = null
        Platform.runLater {
            result = action()
        }
        // Wait for result (blocking, but necessary for synchronous execution)
        while (result == null) {
            Thread.sleep(1)
        }
        result!!
    }

/**
 * Launch a coroutine on the JavaFX Application Thread.
 * Useful for long-running operations that should not block the UI.
 *
 * Example:
 * ```
 * launchOnFxThread {
 *     val loader = FXMLLoader(...)
 *     val root = loader.load<Parent>()
 *     scene.root = root
 * }
 * ```
 *
 * @param action The suspend function to execute on the FX thread
 */
fun launchOnFxThread(action: suspend () -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        action()
    }
}

/**
 * Execute a block of code on a background thread without blocking the UI.
 * Useful for I/O operations, database queries, API calls, etc.
 *
 * Example:
 * ```
 * launchOnBackground {
 *     val data = fetchDataFromApi()
 *     onFxThread {
 *         updateUI(data)
 *     }
 * }
 * ```
 *
 * @param action The suspend function to execute on a background thread
 */
fun launchOnBackground(action: suspend () -> Unit) {
    CoroutineScope(Dispatchers.Default).launch {
        action()
    }
}

/**
 * Execute a block of code on a background thread and then update the UI.
 * Combines background work with UI updates in a single call.
 *
 * Example:
 * ```
 * launchBackgroundThenUI(
 *     background = { fetchDataFromApi() },
 *     onUI = { data -> updateUI(data) }
 * )
 * ```
 *
 * @param background The suspend function to execute on a background thread
 * @param onUI The suspend function to execute on the FX thread with the result
 */
fun <T> launchBackgroundThenUI(
    background: suspend () -> T,
    onUI: suspend (T) -> Unit,
) {
    CoroutineScope(Dispatchers.Default).launch {
        val result = background()
        CoroutineScope(Dispatchers.Main).launch {
            onUI(result)
        }
    }
}
