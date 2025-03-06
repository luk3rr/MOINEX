/*
 * Filename: LoggerConfig.java
 * Created on: August 28, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
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
            m_logger.setLevel(Level.INFO);

            // Create a console handler
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.WARNING);
            m_logger.addHandler(consoleHandler);

            // Create a file handler with append mode
            FileHandler fileHandler = new FileHandler(Constants.LOG_FILE, true);
            fileHandler.setLevel(Level.INFO);
            fileHandler.setFormatter(new SimpleFormatter());
            m_logger.addHandler(fileHandler);

            setFilePermissions(Paths.get(Constants.LOG_FILE));
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

    /**
     * Set secure file permissions for log files (optional but recommended).
     * @param logFile The log file to set permissions for
     */
    private static void setFilePermissions(Path logFile) throws IOException
    {
        // Set file permissions to be readable and writable only by the owner
        if (Files.exists(logFile))
        {
            // Define the permissions: owner read and write, others no permissions
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);

            // Apply the permissions
            Files.setPosixFilePermissions(logFile, perms);
        }
    }
}
