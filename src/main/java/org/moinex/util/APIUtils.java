/*
 * Filename: APIUtils.java
 * Created on: January 17, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.moinex.services.InicializationService;

public class APIUtils
{
    private static final ExecutorService executorService =
        Executors.newCachedThreadPool();

    private static final List<Process> runningProcesses = new ArrayList<>();

    private static Boolean shuttingDown = false;

    private static final Logger logger = LoggerConfig.GetLogger();

    // Prevent instantiation
    private APIUtils() { }

    /**
     * Shutdown the executor service
     */
    public static synchronized void ShutdownExecutor()
    {
        if (shuttingDown)
        {
            return;
        }

        shuttingDown = true;

        logger.info("Shutting down executor service");

        ShutdownProcesses();

        executorService.shutdown();

        try
        {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS))
            {
                logger.warning("Forcing shutdown of executor service...");
                executorService.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            logger.warning("Shutdown interrupted. Forcing shutdown...");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Shutdown all running processes
     */
    private static synchronized void ShutdownProcesses()
    {
        logger.info("Shutting down running processes");

        for (Process process : runningProcesses)
        {
            try
            {
                // Primeiro, tentamos destruir o processo de forma normal
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS))
                {
                    // Se o processo não terminar dentro do limite de tempo, forçamos a
                    // destruição
                    logger.warning(
                        "Process did not terminate in time. Forcing shutdown...");
                    process.destroyForcibly();
                }
                logger.info("Process terminated: " + process.toString());
            }
            catch (InterruptedException e)
            {
                // Se a espera pelo processo for interrompida, forçamos imediatamente
                logger.warning("Process interrupted. Forcing shutdown...");
                process.destroyForcibly();
            }
            catch (Exception e)
            {
                logger.warning("Error during process shutdown: " + e.getMessage());
            }
        }

        logger.info("All processes terminated");

        runningProcesses.clear();
    }

    /**
     * Register a process to be managed by the APIUtils class
     * @param process The process to register
     */
    private static synchronized void RegisterProcess(Process process)
    {
        if (shuttingDown)
        {
            throw new RuntimeException("Application is shutting down");
        }

        runningProcesses.add(process);
    }

    /**
     * Remove a process from the list of running processes
     * @param process The process to remove
     */
    private static synchronized void RemoveProcess(Process process)
    {
        if (shuttingDown)
        {
            throw new RuntimeException("Application is shutting down");
        }

        runningProcesses.remove(process);
    }

    /**
     * Fetch stock prices asynchronously from an external API
     *
     * @param symbols Array of stock symbols
     * @return A CompletableFuture containing the JSON response
     */
    public static CompletableFuture<JSONObject> FetchStockPricesAsync(String[] symbols)
    {
        return CompletableFuture.supplyAsync(() -> {
            return RunPythonScript(Constants.GET_STOCK_PRICE_SCRIPT, symbols);
        }, executorService);
    }

    /**
     * Fetch brazilian market indicators asynchronously from an external API
     *
     * @return A CompletableFuture containing the JSON response
     */
    public static CompletableFuture<JSONObject> FetchBrazilianMarketIndicatorsAsync()
    {
        return CompletableFuture.supplyAsync(() -> {
            return RunPythonScript(Constants.GET_BRAZILIAN_MARKET_INDICATORS_SCRIPT,
                                   new String[0]);
        }, executorService);
    }

    /**
     * Execute a Python script with arguments
     * @param script The script to run
     * @param args The arguments to pass to the script
     */
    public static JSONObject RunPythonScript(String script, String[] args)
    {
        try (InputStream scriptInputStream =
                 InicializationService.class.getResourceAsStream(Constants.SCRIPT_PATH +
                                                                 script))
        {
            if (scriptInputStream == null)
            {
                throw new RuntimeException("Python " + script + " script not found");
            }

            if (shuttingDown)
            {
                throw new RuntimeException("Application is shutting down");
            }

            // Script name without extension
            String scriptName = script.substring(0, script.lastIndexOf('.'));
            Path   tempFile   = Files.createTempFile(scriptName, ".py");

            Files.copy(scriptInputStream,
                       tempFile,
                       StandardCopyOption.REPLACE_EXISTING);

            List<String> commandList = new ArrayList<>();
            commandList.add(Constants.PYTHON_INTERPRETER);
            commandList.add(tempFile.toString());
            commandList.addAll(Arrays.asList(args));

            String[] command = commandList.toArray(new String[0]);

            logger.info("Running Python script as: " + String.join(" ", command));

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process        process        = processBuilder.start();
            RegisterProcess(process);

            try (BufferedReader reader = new BufferedReader(
                     new InputStreamReader(process.getInputStream())))
            {
                String output   = reader.lines().collect(Collectors.joining());
                int    exitCode = process.waitFor();

                if (exitCode != 0)
                {
                    throw new RuntimeException(
                        "Error executing Python script. Exit code: " + exitCode);
                }

                JSONObject jsonObject = new JSONObject(output);

                logger.info("Script " + script + " run successfully");
                logger.info("Output: " + jsonObject.toString());

                return jsonObject;
            }
            finally
            {
                synchronized (runningProcesses)
                {
                    RemoveProcess(process);
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error running Python script: " + e.getMessage(),
                                       e);
        }
    }
}
