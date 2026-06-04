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
import org.moinex.service.PreferencesService
import org.slf4j.LoggerFactory
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import java.nio.file.Paths
import java.util.prefs.Preferences

object SpringApp {
    private val logger = LoggerFactory.getLogger(SpringApp::class.java)

    fun start(args: Array<String>): ConfigurableApplicationContext {
        val dbPath = resolveDbPath()
        System.setProperty("moinex.db.path", dbPath)
        createApplicationDirectories(dbPath)
        return SpringApplicationBuilder().sources(AppConfig::class.java).run(*args)
    }

    fun resolveDbPath(): String {
        val prefs = Preferences.userRoot().node("org.moinex")
        val storedDir = prefs[PreferencesService.PREF_KEY_DB_DIR, null]
        val dir = storedDir ?: "${System.getProperty("user.home")}/.moinex/data"
        return "$dir/moinex.db"
    }

    private fun createApplicationDirectories(dbPath: String) {
        val userHome = System.getProperty("user.home")
        val moinexDir = Paths.get(userHome, ".moinex")

        FileUtils
            .createDirectoriesIfNotExists(moinexDir)
            .onFailure { e ->
                logger.error("Failed to create moinex directory: {}", moinexDir, e)
                throw RuntimeException("Failed to create application directories", e)
            }

        val dbDir = Paths.get(dbPath).parent
        FileUtils
            .createDirectoriesIfNotExists(dbDir)
            .onFailure { e ->
                logger.error("Failed to create database directory: {}", dbDir, e)
                throw RuntimeException("Failed to create application directories", e)
            }
    }
}
