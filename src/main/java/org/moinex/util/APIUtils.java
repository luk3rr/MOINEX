/*
 * Filename: APIUtils.java
 * Created on: January 17, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.moinex.error.MoinexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for managing API-related operations, like fetching stock prices and
 * market indicators
 */
public class APIUtils {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private static final List<Process> runningProcesses = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(APIUtils.class);
    private static boolean shuttingDown = false;

    // Prevent instantiation
    private APIUtils() {}

    /**
     * Shutdown the executor service
     */
    public static synchronized void shutdownExecutor() {
        if (shuttingDown) {
            return;
        }

        shuttingDown = true;

        logger.info("Shutting down executor service");

        shutdownProcesses();

        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("Forcing shutdown of executor service...");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Shutdown interrupted. Forcing shutdown...");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Shutdown all running processes
     */
    private static synchronized void shutdownProcesses() {
        logger.info("Shutting down running processes");

        for (Process process : runningProcesses) {
            try {
                // Primeiro, tentamos destruir o processo de forma normal
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    // Se o processo não terminar dentro do limite de tempo, forçamos a
                    // destruição
                    logger.warn("Process did not terminate in time. Forcing shutdown...");
                    process.destroyForcibly();
                }
                logger.info("Process terminated: {}", process);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Se a espera pelo processo for interrompida, forçamos imediatamente
                logger.warn("Process interrupted. Forcing shutdown...");
                process.destroyForcibly();
            } catch (Exception e) {
                logger.warn("Error during process shutdown: {}", e.getMessage());
            }
        }

        logger.info("All processes terminated");

        runningProcesses.clear();
    }

    /**
     * Register a process to be managed by the APIUtils class
     *
     * @param process The process to register
     */
    private static synchronized void registerProcess(Process process) {
        if (shuttingDown) {
            throw new MoinexException.ApplicationShuttingDownException(
                    "Application is shutting down");
        }

        runningProcesses.add(process);
    }

    /**
     * Remove a process from the list of running processes
     *
     * @param process The process to remove
     */
    private static synchronized void removeProcess(Process process) {
        if (shuttingDown) {
            throw new MoinexException.ApplicationShuttingDownException(
                    "Application is shutting down");
        }

        runningProcesses.remove(process);
    }

    /**
     * Fetch stock prices asynchronously from an external API
     *
     * @param symbols Array of stock symbols
     * @return A CompletableFuture containing the JSON response
     */
    public static CompletableFuture<JSONObject> fetchStockPricesAsync(String[] symbols) {
        return CompletableFuture.supplyAsync(
                () -> runPythonScript(Constants.GET_STOCK_PRICE_SCRIPT, symbols), executorService);
    }

    /**
     * Fetch brazilian market indicators asynchronously from an external API
     *
     * @return A CompletableFuture containing the JSON response
     */
    public static CompletableFuture<JSONObject> fetchBrazilianMarketIndicatorsAsync() {
        return CompletableFuture.supplyAsync(
                () ->
                        runPythonScript(
                                Constants.GET_BRAZILIAN_MARKET_INDICATORS_SCRIPT, new String[0]),
                executorService);
    }

    /**
     * Execute a Python script with arguments
     *
     * @param script The script to run
     * @param args   The arguments to pass to the script
     */
    public static JSONObject runPythonScript(String script, String[] args) {
        try (InputStream scriptInputStream =
                APIUtils.class.getResourceAsStream(Constants.SCRIPT_PATH + script)) {
            if (scriptInputStream == null) {
                throw new MoinexException.ScriptNotFoundException(
                        "Python " + script + " script not found");
            }

            if (shuttingDown) {
                throw new MoinexException.ApplicationShuttingDownException(
                        "Application is shutting down");
            }

            // Script name without extension
            String scriptName = script.substring(0, script.lastIndexOf('.'));
            Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
            Path tempFile = Files.createTempFile(tempDirectory, scriptName, ".py");

            setSecurePermissions(tempFile);

            Files.copy(scriptInputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            List<String> commandList = new ArrayList<>();
            commandList.add(Constants.PYTHON_INTERPRETER);
            commandList.add(tempFile.toString());
            commandList.addAll(Arrays.asList(args));

            String[] command = commandList.toArray(new String[0]);

            logger.info("Running Python script as: {}", String.join(" ", command));

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            registerProcess(process);

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.lines().collect(Collectors.joining());
                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    throw new MoinexException.APIFetchException(
                            "Error executing Python script. Exit code: " + exitCode);
                }

                JSONObject jsonObject = new JSONObject(output);

                logger.info("Script {} run successfully", script);
                logger.info("Output: {}", jsonObject);

                return jsonObject;
            } finally {
                synchronized (runningProcesses) {
                    removeProcess(process);
                }
            }
        } catch (InterruptedException e) {
            // Handle the case where the thread is interrupted
            Thread.currentThread().interrupt();
            throw new MoinexException.APIFetchException(
                    "Python script execution was interrupted: " + e);
        } catch (Exception e) {
            // Handle general errors and exceptions
            throw new MoinexException.APIFetchException(
                    "Error running Python script: " + e.getMessage());
        }
    }

    /**
     * Set secure file permissions to restrict access to the owner only
     *
     * @param file The file to set permissions for
     */
    private static void setSecurePermissions(Path file) throws IOException {
        // Set file permissions to be readable and writable only by the owner
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
    }
}
