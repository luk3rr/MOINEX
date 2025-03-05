/*
 * Filename: LoggerConfig.java
 * Created on: August 28, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Configures the logger for the application
 */
public final class LoggerConfig
{
    // Singleton logger instance
    private static final Logger m_logger =
        Logger.getLogger(LoggerConfig.class.getName());

    // Prevent instantiation
    private LoggerConfig() { }

    // Static initializer block to configure the logger
    static
    {
        try
        {
            // Set the logger level
            m_logger.setLevel(Level.ALL);

            // Create a console handler
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            m_logger.addHandler(consoleHandler);

            // Create a file handler with append mode
            FileHandler fileHandler = new FileHandler(Constants.LOG_FILE, true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new SimpleFormatter());
            m_logger.addHandler(fileHandler);
        }
        catch (Exception e)
        {
            m_logger.log(Level.SEVERE, "Error configuring logger", e);
        }
    }

    /**
     * Get the logger instance
     * @return The logger instance
     */
    public static Logger getLogger()
    {
        return m_logger;
    }
}
