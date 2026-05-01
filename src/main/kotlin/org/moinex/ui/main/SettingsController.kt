/*
 * Filename: SettingsController.kt (original filename: SettingsController.java)
 * Created on: September 19, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 16/03/2026
 */

package org.moinex.ui.main

import com.jfoenix.controls.JFXButton
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.ComboBox
import javafx.stage.Stage
import javafx.util.StringConverter
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.FxUtils
import org.moinex.service.PreferencesService
import org.moinex.service.ThemeService
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.util.Locale

@Controller
class SettingsController(
    private val preferencesService: PreferencesService,
    private val themeService: ThemeService,
    private val springContext: ConfigurableApplicationContext,
) {
    @FXML
    private lateinit var languageComboBox: ComboBox<Locale>

    @FXML
    private lateinit var themeToggleButton: JFXButton

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

        updateThemeButton()
    }

    @FXML
    private fun handleToggleTheme() {
        themeService.toggleAndApply()
        updateThemeButton()
    }

    private fun updateThemeButton() {
        themeToggleButton.text =
            if (preferencesService.isDarkMode()) {
                preferencesService.translate(TranslationKeys.SETTINGS_THEME_LIGHT)
            } else {
                preferencesService.translate(TranslationKeys.SETTINGS_THEME_DARK)
            }
    }
}
