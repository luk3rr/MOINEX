/*
 * Filename: APIUtils.kt (original filename: APIUtils.java)
 * Created on: January 17, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 10/03/2026
 */

package org.moinex.common.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Scripts
import org.moinex.common.extension.toBACENFormat
import org.moinex.common.extension.toNoTimeFormat
import org.moinex.config.AppDispatchers
import org.moinex.exception.MoinexException
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.PeriodType
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempFile
import kotlin.io.path.pathString
import org.moinex.common.constant.Files as FilesConstants

object APIUtils {
    private val scope = CoroutineScope(SupervisorJob() + AppDispatchers.IO)
    private val runningProcesses = mutableListOf<Process>()
    private val processMutex = Mutex()
    private val logger = LoggerFactory.getLogger(APIUtils::class.java)

    @Volatile
    private var shuttingDown = false

    private const val DEFAULT_SCRIPT_TIMEOUT_SECONDS = 300L
    private const val PROCESS_TERMINATION_TIMEOUT_SECONDS = 5L

    suspend fun shutdownExecutor() {
        processMutex.withLock {
            if (shuttingDown) return
            shuttingDown = true
        }

        logger.info("Shutting down API utilities")

        shutdownProcesses()
        scope.cancel("Application shutting down")

        logger.info("API utilities shutdown complete")
    }

    private suspend fun shutdownProcesses() =
        processMutex.withLock {
            logger.info("Shutting down ${runningProcesses.size} running processes")

            runningProcesses.forEach { process ->
                runCatching {
                    process.destroy()
                    if (!process.waitFor(PROCESS_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        logger.warn("Process did not terminate in time. Forcing shutdown...")
                        process.destroyForcibly()
                    }
                    logger.debug("Process terminated: {}", process)
                }.onFailure { e ->
                    when (e) {
                        is InterruptedException -> {
                            Thread.currentThread().interrupt()
                            logger.warn("Process interrupted. Forcing shutdown...")
                            process.destroyForcibly()
                        }
                        else -> logger.warn("Error during process shutdown: ${e.message}")
                    }
                }
            }

            logger.info("All processes terminated")
            runningProcesses.clear()
        }

    private suspend fun registerProcess(process: Process) =
        processMutex.withLock {
            check(!shuttingDown) { "Application is shutting down" }
            runningProcesses.add(process)
        }

    private suspend fun removeProcess(process: Process) =
        processMutex.withLock {
            runningProcesses.remove(process)
        }

    suspend fun fetchStockPrices(symbols: List<String>): JSONObject =
        runPythonScript(Scripts.GET_STOCK_PRICE_SCRIPT, symbols)

    suspend fun fetchBrazilianMarketIndicators(): JSONObject =
        runPythonScript(Scripts.GET_BRAZILIAN_MARKET_INDICATORS_SCRIPT)

    suspend fun fetchFundamentalAnalysis(
        symbol: String,
        period: PeriodType,
    ): JSONObject =
        runPythonScript(
            Scripts.GET_FUNDAMENTAL_DATA_SCRIPT,
            listOf(symbol, "--period", period.name.lowercase(), "--format", "json"),
        )

    suspend fun fetchMarketIndicatorHistory(
        indicatorType: InterestIndex,
        startDate: LocalDate,
        endDate: LocalDate,
    ): JSONObject =
        runPythonScript(
            Scripts.GET_MARKET_INDICATOR_HISTORY_SCRIPT,
            listOf(
                indicatorType.name,
                startDate.toBACENFormat(),
                endDate.toBACENFormat(),
            ),
        )

    suspend fun fetchStockLogos(websites: List<String>): JSONObject =
        runPythonScript(Scripts.GET_STOCK_LOGO_SCRIPT, websites)

    suspend fun fetchStockPriceHistory(
        symbol: String,
        startDate: String,
        endDate: String,
        specificDates: Set<LocalDate>? = null,
    ): JSONObject {
        val args =
            if (specificDates.isNullOrEmpty()) {
                listOf(symbol, startDate, endDate)
            } else {
                listOf(symbol, startDate, endDate, JSONArray(specificDates.map { it.toNoTimeFormat() }).toString())
            }
        return runPythonScript(Scripts.GET_STOCK_PRICE_HISTORY_SCRIPT, args)
    }

    suspend fun runPythonScript(
        script: String,
        args: List<String> = emptyList(),
        timeoutSeconds: Long = DEFAULT_SCRIPT_TIMEOUT_SECONDS,
    ): JSONObject {
        check(!shuttingDown) { "Application is shutting down" }

        val scriptInputStream =
            APIUtils::class.java.getResourceAsStream("${FilesConstants.SCRIPT_PATH}$script")
                ?: throw MoinexException.ScriptNotFoundException("Python script '$script' not found")

        return scriptInputStream.use { stream ->
            val tempFile =
                createTempFile(script.substringBeforeLast('.'), ".py")
                    .also { setSecurePermissions(it) }

            Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING)

            val command =
                buildList {
                    add(Constants.PYTHON_INTERPRETER)
                    add(tempFile.pathString)
                    addAll(args)
                }

            logger.debug("Executing: ${command.joinToString(" ")}")

            executePythonProcess(script, command, timeoutSeconds)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun executePythonProcess(
        scriptName: String,
        command: List<String>,
        timeoutSeconds: Long,
    ): JSONObject {
        val process =
            ProcessBuilder(command)
                .redirectErrorStream(false)
                .start()

        registerProcess(process)

        return runCatching {
            val (output, errorOutput) =
                process.inputStream.bufferedReader().use { reader ->
                    process.errorStream.bufferedReader().use { errorReader ->
                        val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)

                        if (!completed) {
                            process.destroyForcibly()
                            throw MoinexException.APIFetchException(
                                "Script '$scriptName' timed out after $timeoutSeconds seconds",
                            )
                        }

                        reader.readText() to errorReader.readText()
                    }
                }

            val exitCode = process.exitValue()

            if (errorOutput.isNotBlank()) {
                errorOutput
                    .lines()
                    .filter { it.isNotBlank() }
                    .forEach { logger.debug("[{}] {}", scriptName, it) }
            }

            when {
                exitCode != 0 -> {
                    val errorMessage = errorOutput.lines().lastOrNull { it.isNotBlank() } ?: errorOutput
                    logger.error("Script '$scriptName' failed with exit code: $exitCode - $errorMessage")
                    throw MoinexException.APIFetchException(
                        "Script '$scriptName' failed with exit code: $exitCode - $errorMessage",
                    )
                }
                else -> {
                    logger.debug("Script '$scriptName' executed successfully")
                    JSONObject(output)
                }
            }
        }.onFailure { e ->
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }.getOrElse { e ->
            throw MoinexException.APIFetchException(
                "Script execution failed: ${e.message}",
            )
        }.also {
            removeProcess(process)
        }
    }

    private fun setSecurePermissions(file: Path) {
        if (!System.getProperty("os.name").lowercase().contains("win")) {
            runCatching {
                Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
            }.onFailure { e ->
                logger.warn("Failed to set secure permissions on temp file: ${e.message}")
            }
        }
    }
}
