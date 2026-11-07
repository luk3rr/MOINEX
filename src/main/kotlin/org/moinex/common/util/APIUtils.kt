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
import org.moinex.common.ClockProvider
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Scripts
import org.moinex.common.extension.toBACENFormat
import org.moinex.common.extension.toNoTimeFormat
import org.moinex.config.AppDispatchers
import org.moinex.exception.MoinexException
import org.moinex.model.dto.StockHistoryRequest
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.PeriodType
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.time.LocalDate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString
import org.moinex.common.constant.Files as FilesConstants

object APIUtils {
    private val scope = CoroutineScope(SupervisorJob() + AppDispatchers.IO)
    private val runningProcesses = mutableListOf<Process>()
    private val processMutex = Mutex()

    @Volatile
    internal var yahooGateway = YahooGateway(ClockProvider())
    private val logger = LoggerFactory.getLogger(APIUtils::class.java)

    @Volatile
    private var shuttingDown = false

    private const val DEFAULT_SCRIPT_TIMEOUT_SECONDS = 300L
    private const val PROCESS_TERMINATION_TIMEOUT_SECONDS = 5L

    fun isShuttingDown(): Boolean = shuttingDown

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

    /**
     * Runs [block] with the Yahoo cooldown switched off, for an update the user asked for by hand.
     */
    suspend fun <T> withoutYahooCooldown(block: suspend () -> T): T = yahooGateway.ignoringCooldown(block)

    suspend fun fetchStockPrices(symbols: List<String>): JSONObject =
        yahooGateway.call(Scripts.GET_STOCK_PRICE_SCRIPT, symbols.size) {
            runPythonScript(Scripts.GET_STOCK_PRICE_SCRIPT, symbols)
        }

    suspend fun fetchBrazilianMarketIndicators(): JSONObject =
        runPythonScript(Scripts.GET_BRAZILIAN_MARKET_INDICATORS_SCRIPT)

    suspend fun fetchFundamentalAnalysis(
        symbol: String,
        period: PeriodType,
    ): JSONObject =
        yahooGateway.call(Scripts.GET_FUNDAMENTAL_DATA_SCRIPT, 1) {
            runPythonScript(
                Scripts.GET_FUNDAMENTAL_DATA_SCRIPT,
                listOf(symbol, "--period", period.name.lowercase(), "--format", "json"),
            )
        }

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
        return yahooGateway.call(Scripts.GET_STOCK_PRICE_HISTORY_SCRIPT, 1) {
            runPythonScript(Scripts.GET_STOCK_PRICE_HISTORY_SCRIPT, args)
        }
    }

    /**
     * Fetches the daily history of several symbols with one script run. The result maps each symbol to its
     * history, or to an object with `error` and `error_kind` when that symbol failed.
     */
    suspend fun fetchStockPriceHistoryBatch(requests: List<StockHistoryRequest>): JSONObject {
        val payload =
            JSONArray(
                requests.map { request ->
                    JSONObject()
                        .put("symbol", request.symbol)
                        .put("start", request.start.toNoTimeFormat())
                        .put("end", request.end.toNoTimeFormat())
                        .apply {
                            request.currency?.let { put("currency", it) }
                            request.dates?.takeIf { it.isNotEmpty() }?.let { dates ->
                                put("dates", JSONArray(dates.map { it.toNoTimeFormat() }))
                            }
                        }
                },
            )

        return yahooGateway
            .call(Scripts.GET_STOCK_PRICE_HISTORY_SCRIPT, requests.size) {
                runPythonScript(Scripts.GET_STOCK_PRICE_HISTORY_SCRIPT, listOf("--batch", payload.toString()))
            }.getJSONObject("results")
    }

    suspend fun runPythonScript(
        script: String,
        args: List<String> = emptyList(),
        timeoutSeconds: Long = DEFAULT_SCRIPT_TIMEOUT_SECONDS,
    ): JSONObject {
        check(!shuttingDown) { "Application is shutting down" }

        val workDir = prepareScriptDirectory(script)

        return try {
            val command =
                buildList {
                    add(Constants.PYTHON_INTERPRETER)
                    add(workDir.resolve(script).pathString)
                    addAll(args)
                }

            logger.debug("Executing: ${command.joinToString(" ")}")

            executePythonProcess(script, command, timeoutSeconds)
        } finally {
            runCatching { workDir.toFile().deleteRecursively() }
                .onFailure { logger.warn("Failed to remove {}: {}", workDir, it.message) }
        }
    }

    /**
     * Copies [script] and the module the scripts share into a new private temporary directory, which is where the
     * script runs from: a script finds its shared module next to itself. The caller removes the directory.
     */
    internal fun prepareScriptDirectory(script: String): Path {
        val workDir = createTempDirectory("moinex-script-")

        return runCatching {
            listOf(script, Scripts.RESILIENCE_MODULE).forEach { name ->
                val stream =
                    APIUtils::class.java.getResourceAsStream("${FilesConstants.SCRIPT_PATH}$name")
                        ?: throw MoinexException.ScriptNotFoundException("Python script '$name' not found")

                val target = workDir.resolve(name)
                stream.use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) }
                setSecurePermissions(target)
            }
            workDir
        }.onFailure {
            runCatching { workDir.toFile().deleteRecursively() }
        }.getOrThrow()
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
                .apply {
                    environment()["PYTHONUTF8"] = "1"
                    environment()["PYTHONIOENCODING"] = "utf-8"
                    environment()["PYTHONWARNINGS"] = "ignore"
                }.start()

        registerProcess(process)

        return runCatching {
            val stdoutFuture = CompletableFuture<String>()
            val stderrFuture = CompletableFuture<String>()
            drainAsync(process.inputStream, stdoutFuture)
            drainAsync(process.errorStream, stderrFuture)

            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)

            if (!completed) {
                process.destroyForcibly()
                throw MoinexException.APIFetchException(
                    "Script '$scriptName' timed out after $timeoutSeconds seconds",
                )
            }

            val output = stdoutFuture.get()
            val errorOutput = stderrFuture.get()

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

    /**
     * Reads the stream on its own thread so the child process never blocks on a full pipe buffer.
     */
    private fun drainAsync(
        stream: InputStream,
        target: CompletableFuture<String>,
    ) {
        thread(isDaemon = true) {
            runCatching { stream.bufferedReader().use { it.readText() } }
                .onSuccess { target.complete(it) }
                .onFailure { target.completeExceptionally(it) }
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
