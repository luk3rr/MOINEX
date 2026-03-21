/*
 * Filename: SpringApp.kt (original filename: SpringApp.java)
 * Created on: [Original date]
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.app

import org.moinex.common.util.FileUtils
import org.moinex.config.AppConfig
import org.slf4j.LoggerFactory
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import java.nio.file.Paths

object SpringApp {
    private val logger = LoggerFactory.getLogger(SpringApp::class.java)

    fun start(args: Array<String>): ConfigurableApplicationContext {
        createApplicationDirectories()
        return SpringApplicationBuilder().sources(AppConfig::class.java).run(*args)
    }

    private fun createApplicationDirectories() {
        val userHome = System.getProperty("user.home")
        val moinexDir = Paths.get(userHome, ".moinex")
        val dataDir = Paths.get(userHome, ".moinex", "data")

        FileUtils
            .createDirectoriesIfNotExists(moinexDir)
            .onFailure { e ->
                logger.error("Failed to create moinex directory: {}", moinexDir, e)
                throw RuntimeException("Failed to create application directories", e)
            }

        FileUtils
            .createDirectoriesIfNotExists(dataDir)
            .onFailure { e ->
                logger.error("Failed to create data directory: {}", dataDir, e)
                throw RuntimeException("Failed to create application directories", e)
            }
    }
}
