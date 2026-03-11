/*
 * Filename: APIUtils.kt (original filename: APIUtils.java)
 * Created on: January 17, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 10/03/2026
 */

package org.moinex.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.moinex.common.extension.toBACENFormat
import org.moinex.error.MoinexException
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.PeriodType
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.time.LocalDate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.pathString

object APIUtils {
    private val executorService = Executors.newCachedThreadPool()
    private val runningProcesses = mutableListOf<Process>()
    private val logger = LoggerFactory.getLogger(APIUtils::class.java)

    private const val DEFAULT_SCRIPT_TIMEOUT_SECONDS = 10L

    @Volatile
    private var shuttingDown = false

    @Synchronized
    fun shutdownExecutor() {
        if (shuttingDown) return

        shuttingDown = true
        logger.info("Shutting down executor service")

        shutdownProcesses()

        executorService.shutdown()

        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("Forcing shutdown of executor service...")
                executorService.shutdownNow()
            }
        } catch (_: InterruptedException) {
            logger.warn("Shutdown interrupted. Forcing shutdown...")
            executorService.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    @Synchronized
    private fun shutdownProcesses() {
        logger.info("Shutting down running processes")

        runningProcesses.forEach { process ->
            runCatching {
                process.destroy()
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    logger.warn("Process did not terminate in time. Forcing shutdown...")
                    process.destroyForcibly()
                }
                logger.info("Process terminated: $process")
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

    @Synchronized
    private fun registerProcess(process: Process) {
        if (shuttingDown) {
            throw MoinexException.ApplicationShuttingDownException("Application is shutting down")
        }
        runningProcesses.add(process)
    }

    @Synchronized
    private fun removeProcess(process: Process) {
        if (shuttingDown) {
            throw MoinexException.ApplicationShuttingDownException("Application is shutting down")
        }
        runningProcesses.remove(process)
    }

    suspend fun fetchStockPrices(
        symbols: Array<String>,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): JSONObject =
        withContext(dispatcher) {
            runPythonScript(Constants.GET_STOCK_PRICE_SCRIPT, symbols)
        }

    suspend fun fetchBrazilianMarketIndicators(dispatcher: CoroutineDispatcher = Dispatchers.IO): JSONObject =
        withContext(dispatcher) {
            runPythonScript(Constants.GET_BRAZILIAN_MARKET_INDICATORS_SCRIPT, emptyArray())
        }

    suspend fun fetchFundamentalAnalysis(
        symbol: String,
        period: PeriodType,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): JSONObject =
        withContext(dispatcher) {
            runPythonScript(
                Constants.GET_FUNDAMENTAL_DATA_SCRIPT,
                arrayOf(symbol, "--period", period.name.lowercase(), "--format", "json"),
            )
        }

    suspend fun fetchMarketIndicatorHistory(
        indicatorType: InterestIndex,
        startDate: LocalDate,
        endDate: LocalDate,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): JSONObject =
        withContext(dispatcher) {
            runPythonScript(
                Constants.GET_MARKET_INDICATOR_HISTORY_SCRIPT,
                arrayOf(
                    indicatorType.name,
                    startDate.toBACENFormat(),
                    endDate.toBACENFormat(),
                ),
            )
        }

    suspend fun fetchStockLogos(
        websites: Array<String>,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): JSONObject =
        withContext(dispatcher) {
            runPythonScript(Constants.GET_STOCK_LOGO_SCRIPT, websites)
        }

    suspend fun fetchStockPriceHistory(
        symbol: String,
        startDate: String,
        endDate: String,
        specificDates: List<String>? = null,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): JSONObject =
        withContext(dispatcher) {
            val args =
                if (specificDates.isNullOrEmpty()) {
                    arrayOf(symbol, startDate, endDate)
                } else {
                    arrayOf(symbol, startDate, endDate, JSONArray(specificDates).toString())
                }
            runPythonScript("get_stock_price_history.py", args)
        }

    @Deprecated("Use suspend function fetchStockPrices instead", ReplaceWith("fetchStockPrices(symbols)"))
    fun fetchStockPricesAsync(symbols: Array<String>): CompletableFuture<JSONObject> =
        CompletableFuture.supplyAsync(
            { runPythonScript(Constants.GET_STOCK_PRICE_SCRIPT, symbols) },
            executorService,
        )

    @Deprecated("Use suspend function fetchBrazilianMarketIndicators instead", ReplaceWith("fetchBrazilianMarketIndicators()"))
    fun fetchBrazilianMarketIndicatorsAsync(): CompletableFuture<JSONObject> =
        CompletableFuture.supplyAsync(
            { runPythonScript(Constants.GET_BRAZILIAN_MARKET_INDICATORS_SCRIPT, emptyArray()) },
            executorService,
        )

    @Deprecated("Use suspend function fetchFundamentalAnalysis instead", ReplaceWith("fetchFundamentalAnalysis(symbol, period)"))
    fun fetchFundamentalAnalysisAsync(
        symbol: String,
        period: PeriodType,
    ): CompletableFuture<JSONObject> =
        CompletableFuture.supplyAsync(
            {
                runPythonScript(
                    Constants.GET_FUNDAMENTAL_DATA_SCRIPT,
                    arrayOf(symbol, "--period", period.name.lowercase(), "--format", "json"),
                )
            },
            executorService,
        )

    @Deprecated("Use suspend function fetchStockLogos instead", ReplaceWith("fetchStockLogos(websites)"))
    fun fetchStockLogosAsync(websites: Array<String>): CompletableFuture<JSONObject> =
        CompletableFuture.supplyAsync(
            { runPythonScript(Constants.GET_STOCK_LOGO_SCRIPT, websites) },
            executorService,
        )

    @Deprecated(
        "Use suspend function fetchStockPriceHistory instead",
        ReplaceWith("fetchStockPriceHistory(symbol, startDate, endDate, specificDates)"),
    )
    fun fetchStockPriceHistoryAsync(
        symbol: String,
        startDate: String,
        endDate: String,
        specificDates: List<String>? = null,
    ): CompletableFuture<JSONObject> =
        CompletableFuture.supplyAsync(
            {
                val args =
                    if (specificDates.isNullOrEmpty()) {
                        arrayOf(symbol, startDate, endDate)
                    } else {
                        arrayOf(symbol, startDate, endDate, JSONArray(specificDates).toString())
                    }
                runPythonScript("get_stock_price_history.py", args)
            },
            executorService,
        )

    @Deprecated("Use suspend function with runPythonScript directly", ReplaceWith("runPythonScript(script, args)"))
    fun runPythonScriptAsync(
        script: String,
        args: Array<String>,
    ): CompletableFuture<JSONObject> =
        CompletableFuture.supplyAsync(
            { runPythonScript(script, args) },
            executorService,
        )

    fun runPythonScript(
        script: String,
        args: Array<String>,
        timeoutSeconds: Long = DEFAULT_SCRIPT_TIMEOUT_SECONDS,
    ): JSONObject {
        val scriptInputStream: InputStream =
            APIUtils::class.java.getResourceAsStream(Constants.SCRIPT_PATH + script)
                ?: throw MoinexException.ScriptNotFoundException("Python $script script not found")

        if (shuttingDown) {
            throw MoinexException.ApplicationShuttingDownException("Application is shutting down")
        }

        return runCatching {
            val scriptName = script.substringBeforeLast('.')
            val tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"))
            val tempFile = Files.createTempFile(tempDirectory, scriptName, ".py")

            setSecurePermissions(tempFile)

            Files.copy(scriptInputStream, tempFile, StandardCopyOption.REPLACE_EXISTING)

            val command =
                buildList {
                    add(Constants.PYTHON_INTERPRETER)
                    add(tempFile.pathString)
                    addAll(args)
                }.toTypedArray()

            logger.info("Running Python script as: ${command.joinToString(" ")}")

            val process = ProcessBuilder(*command).start()
            registerProcess(process)

            try {
                process.inputStream.bufferedReader().use { reader ->
                    process.errorStream.bufferedReader().use { errorReader ->
                        val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)

                        if (!completed) {
                            logger.error("Python script '$script' timed out after $timeoutSeconds seconds")
                            process.destroyForcibly()
                            throw MoinexException.APIFetchException(
                                "Python script '$script' execution timed out after $timeoutSeconds seconds",
                            )
                        }

                        val output = reader.readText()
                        val errorOutput = errorReader.readText()
                        val exitCode = process.exitValue()

                        if (exitCode != 0) {
                            logger.error("Python script '$script' failed with exit code: $exitCode")

                            if (errorOutput.isNotEmpty()) {
                                logger.debug("Error output: $errorOutput")
                            }

                            throw MoinexException.APIFetchException(
                                "Error executing '$script'. Exit code: $exitCode",
                            )
                        }

                        val jsonObject = JSONObject(output)

                        logger.info("Script $script run successfully")
                        logger.info("Output: $jsonObject")

                        jsonObject
                    }
                }
            } finally {
                synchronized(runningProcesses) {
                    removeProcess(process)
                }
            }
        }.getOrElse { e ->
            when (e) {
                is InterruptedException -> {
                    Thread.currentThread().interrupt()
                    throw MoinexException.APIFetchException("Python script execution was interrupted: $e")
                }
                else -> throw MoinexException.APIFetchException("Error running Python script: ${e.message}")
            }
        }
    }

    private fun setSecurePermissions(file: Path) {
        val os = System.getProperty("os.name").lowercase()

        if (!os.contains("win")) {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
        }
    }
}
