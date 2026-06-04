/*
 * Filename: SettingsController.kt (original filename: SettingsController.java)
 * Created on: September 19, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 16/03/2026
 */

package org.moinex.ui.main

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import javafx.util.StringConverter
import org.moinex.app.SpringApp
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.FxUtils
import org.moinex.common.util.WindowUtils
import org.moinex.service.PreferencesService
import org.moinex.service.ThemeService
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.io.File
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.nio.file.Files as NioFiles

@Controller
class SettingsController(
    private val preferencesService: PreferencesService,
    private val themeService: ThemeService,
    private val springContext: ConfigurableApplicationContext,
) {
    @FXML
    private lateinit var languageComboBox: ComboBox<Locale>

    @FXML
    private lateinit var themeComboBox: ComboBox<String>

    @FXML
    private lateinit var dbPathField: TextField

    @FXML
    private lateinit var restartRequiredLabel: Label

    companion object {
        private val logger = LoggerFactory.getLogger(SettingsController::class.java)
    }

    @FXML
    fun initialize() {
        languageComboBox.items.setAll(preferencesService.getSupportedLocales())

        val localeLabels = preferencesService.getSupportedLocalesWithLabels()

        languageComboBox.converter =
            object : StringConverter<Locale>() {
                override fun toString(locale: Locale?): String {
                    if (locale == null) {
                        return ""
                    }
                    return localeLabels.getOrDefault(locale, locale.toLanguageTag())
                }

                override fun fromString(string: String?): Locale? = null
            }

        languageComboBox.selectionModel.select(preferencesService.locale)

        languageComboBox.valueProperty().addListener { _, oldVal, newVal ->
            if (newVal == null || newVal == oldVal) {
                return@addListener
            }

            preferencesService.locale = newVal

            val scene = languageComboBox.scene
            if (scene == null || scene.window == null) {
                return@addListener
            }

            val stage = scene.window as Stage

            FxUtils.launchOnFxThread {
                runCatching {
                    val loader =
                        FXMLLoader(
                            javaClass.getResource(Files.MAIN_FXML),
                            preferencesService.bundle,
                        )
                    loader.setControllerFactory { springContext.getBean(it) }
                    val mainRoot = loader.load<Parent>()

                    scene.root = mainRoot
                    stage.title = preferencesService.translate(TranslationKeys.APP_TITLE)
                }.onFailure { e ->
                    logger.error("Failed to reload main UI after language change", e)
                }
            }
        }

        val themeLabels =
            mapOf(
                PreferencesService.THEME_LIGHT to preferencesService.translate(TranslationKeys.SETTINGS_THEME_LIGHT),
                PreferencesService.THEME_DARK to preferencesService.translate(TranslationKeys.SETTINGS_THEME_DARK),
            )

        themeComboBox.items.setAll(themeLabels.keys.toList())

        themeComboBox.converter =
            object : StringConverter<String>() {
                override fun toString(theme: String?): String = themeLabels[theme] ?: theme ?: ""

                override fun fromString(string: String?): String? = null
            }

        themeComboBox.selectionModel.select(preferencesService.theme)

        themeComboBox.valueProperty().addListener { _, oldVal, newVal ->
            if (newVal == null || newVal == oldVal) {
                return@addListener
            }
            preferencesService.theme = newVal
            themeService.applyCurrentTheme()
        }

        dbPathField.text = SpringApp.resolveDbPath()
    }

    @FXML
    fun handleChangeDbPath() {
        val currentDbPath = SpringApp.resolveDbPath()
        val currentDbFile = File(currentDbPath)

        val chooser =
            DirectoryChooser().apply {
                title = preferencesService.translate(TranslationKeys.SETTINGS_DATABASE_LOCATION)
                initialDirectory = currentDbFile.parentFile?.takeIf { it.exists() }
            }

        val stage = dbPathField.scene.window as? Stage ?: return
        val selectedDir = chooser.showDialog(stage) ?: return

        if (selectedDir.canonicalPath == currentDbFile.parentFile?.canonicalPath) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.SETTINGS_DATABASE_LOCATION),
                preferencesService.translate(TranslationKeys.SETTINGS_DATABASE_SAME_DIR),
                preferencesService.bundle,
            )
            return
        }

        if (!selectedDir.canWrite()) {
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.SETTINGS_DATABASE_LOCATION),
                preferencesService.translate(TranslationKeys.SETTINGS_DATABASE_NOT_WRITABLE),
                preferencesService.bundle,
            )
            return
        }

        val targetFile = File(selectedDir, "moinex.db")

        if (targetFile.exists()) {
            val overwrite =
                WindowUtils.showConfirmationDialog(
                    preferencesService.translate(TranslationKeys.SETTINGS_DATABASE_LOCATION),
                    preferencesService.translate(TranslationKeys.SETTINGS_DATABASE_COPY_WARN_OVERWRITE),
                    preferencesService.bundle,
                )
            if (!overwrite) return
        } else if (currentDbFile.exists()) {
            val copy =
                WindowUtils.showConfirmationDialog(
                    preferencesService.translate(TranslationKeys.SETTINGS_DATABASE_COPY_TITLE),
                    preferencesService.translate(TranslationKeys.SETTINGS_DATABASE_COPY_MESSAGE),
                    preferencesService.bundle,
                )
            if (copy) {
                runCatching {
                    NioFiles.copy(currentDbFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    listOf("-wal", "-shm").forEach { suffix ->
                        val sidecar = File(currentDbFile.parent, "moinex.db$suffix")
                        if (sidecar.exists()) {
                            NioFiles.copy(
                                sidecar.toPath(),
                                File(selectedDir, "moinex.db$suffix").toPath(),
                                StandardCopyOption.REPLACE_EXISTING,
                            )
                        }
                    }
                }.onFailure { e ->
                    logger.error("Failed to copy database to {}", selectedDir, e)
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.SETTINGS_DATABASE_LOCATION),
                        preferencesService.translate(TranslationKeys.SETTINGS_DATABASE_COPY_FAILED),
                        preferencesService.bundle,
                    )
                    return
                }
            }
        }

        preferencesService.dbDirectory = selectedDir.canonicalPath
        dbPathField.text = "${selectedDir.canonicalPath}/moinex.db"
        restartRequiredLabel.isVisible = true
        restartRequiredLabel.isManaged = true
    }
}
