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
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.moinex.services.InicializationService;
import org.moinex.services.TickerService;

public class APIUtils
{
    private static final ExecutorService executorService =
        Executors.newCachedThreadPool();

    private static final Logger logger = LoggerConfig.GetLogger();

    // Prevent instantiation
    private APIUtils() { }

    /**
     * Fetch stock prices asynchronously from an external API
     *
     * @param symbols Array of stock symbols
     * @return A CompletableFuture containing the JSON response
     */
    public static CompletableFuture<JSONObject> FetchStockPricesAsync(String[] symbols)
    {
        return CompletableFuture.supplyAsync(() -> {
            try
            {
                return RunPythonScript(Constants.GET_STOCK_PRICE_SCRIPT, symbols);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error fetching stock prices: " +
                                               e.getMessage(),
                                           e);
            }
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
            try
            {
                return RunPythonScript(Constants.GET_BRAZILIAN_MARKET_INDICATORS_SCRIPT,
                                       new String[0]);
            }
            catch (Exception e)
            {
                throw new RuntimeException(
                    "Error fetching brazilian market indicators: " + e.getMessage(),
                    e);
            }
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
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error running Python script: " + e.getMessage(),
                                       e);
        }
    }
}
