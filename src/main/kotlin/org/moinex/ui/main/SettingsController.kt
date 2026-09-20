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
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import javafx.util.StringConverter
import org.moinex.ai.credentials.AiCredentialStore
import org.moinex.ai.provider.AnthropicProvider
import org.moinex.ai.provider.GeminiProvider
import org.moinex.ai.provider.LlmProviderFactory
import org.moinex.ai.provider.OpenAiProvider
import org.moinex.ai.provider.OpenRouterProvider
import org.moinex.app.SpringApp
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.FxUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.enums.LlmProviderType
import org.moinex.service.PreferencesService
import org.moinex.service.ThemeService
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.StandardCopyOption
import java.text.MessageFormat
import java.util.Locale
import java.nio.file.Files as NioFiles

@Controller
class SettingsController(
    private val preferencesService: PreferencesService,
    private val themeService: ThemeService,
    private val springContext: ConfigurableApplicationContext,
    private val aiCredentialStore: AiCredentialStore,
    private val llmProviderFactory: LlmProviderFactory,
) {
    @FXML
    private lateinit var languageComboBox: ComboBox<Locale>

    @FXML
    private lateinit var themeComboBox: ComboBox<String>

    @FXML
    private lateinit var dbPathField: TextField

    @FXML
    private lateinit var restartRequiredLabel: Label

    @FXML
    private lateinit var savingsRateTargetField: TextField

    @FXML
    private lateinit var aiProviderComboBox: ComboBox<LlmProviderType>

    @FXML
    private lateinit var aiApiKeyField: PasswordField

    @FXML
    private lateinit var aiModelField: TextField

    @FXML
    private lateinit var aiKeyStatusLabel: Label

    @FXML
    private lateinit var aiTestConnectionButton: Button

    @FXML
    private lateinit var aiSaveButton: Button

    @FXML
    private lateinit var aiClearButton: Button

    companion object {
        private val logger = LoggerFactory.getLogger(SettingsController::class.java)
        private val MIN_SAVINGS_RATE_TARGET = BigDecimal.ZERO
        private val MAX_SAVINGS_RATE_TARGET = BigDecimal(100)
        private const val STYLE_KEY_STATUS_CONFIGURED = "ai-key-status-configured"
        private const val STYLE_KEY_STATUS_MISSING = "ai-key-status-missing"
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

        savingsRateTargetField.text = preferencesService.savingsRateTarget.stripTrailingZeros().toPlainString()
        savingsRateTargetField.setOnAction { commitSavingsRateTarget() }
        savingsRateTargetField.focusedProperty().addListener { _, _, focused ->
            if (!focused) commitSavingsRateTarget()
        }

        configureAiSettings()
    }

    private fun configureAiSettings() {
        aiProviderComboBox.items.setAll(LlmProviderType.entries)
        aiProviderComboBox.converter =
            object : StringConverter<LlmProviderType>() {
                override fun toString(type: LlmProviderType?): String = type?.displayName ?: ""

                override fun fromString(string: String?): LlmProviderType? = null
            }

        val initialProvider = aiCredentialStore.selectedProvider ?: LlmProviderType.OPENAI
        aiProviderComboBox.selectionModel.select(initialProvider)
        updateAiKeyStatusLabel(initialProvider)
        updateModelField(initialProvider)

        aiProviderComboBox.valueProperty().addListener { _, _, newProvider ->
            aiApiKeyField.clear()
            newProvider?.let {
                updateAiKeyStatusLabel(it)
                updateModelField(it)
            }
        }
    }

    private fun updateAiKeyStatusLabel(provider: LlmProviderType) {
        val configured = aiCredentialStore.hasApiKey(provider)

        val statusKey =
            if (configured) {
                TranslationKeys.SETTINGS_AI_KEY_CONFIGURED
            } else {
                TranslationKeys.SETTINGS_AI_NO_KEY_CONFIGURED
            }
        aiKeyStatusLabel.text = preferencesService.translate(statusKey)
        aiKeyStatusLabel.styleClass.removeAll(STYLE_KEY_STATUS_CONFIGURED, STYLE_KEY_STATUS_MISSING)
        aiKeyStatusLabel.styleClass.add(if (configured) STYLE_KEY_STATUS_CONFIGURED else STYLE_KEY_STATUS_MISSING)
    }

    private fun updateModelField(provider: LlmProviderType) {
        aiModelField.text = aiCredentialStore.getModel(provider).orEmpty()
        aiModelField.promptText = defaultModelFor(provider)
    }

    private fun defaultModelFor(provider: LlmProviderType): String =
        when (provider) {
            LlmProviderType.OPENAI -> OpenAiProvider.DEFAULT_MODEL
            LlmProviderType.ANTHROPIC -> AnthropicProvider.DEFAULT_MODEL
            LlmProviderType.GEMINI -> GeminiProvider.DEFAULT_MODEL
            LlmProviderType.OPENROUTER -> OpenRouterProvider.DEFAULT_MODEL
        }

    @FXML
    fun handleTestAiConnection() {
        val provider = aiProviderComboBox.value ?: return
        val apiKey =
            aiApiKeyField.text?.trim().takeUnless { it.isNullOrBlank() } ?: aiCredentialStore.getApiKey(provider)
        val model = aiModelField.text?.trim().takeUnless { it.isNullOrBlank() }

        if (apiKey.isNullOrBlank()) {
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.SETTINGS_AI_TEST_CONNECTION),
                preferencesService.translate(TranslationKeys.SETTINGS_AI_NO_KEY_CONFIGURED),
                preferencesService.bundle,
            )
            return
        }

        aiTestConnectionButton.isDisable = true

        FxUtils.launchBackgroundThenUI(
            background = {
                runCatching {
                    llmProviderFactory
                        .buildProvider(provider, apiKey, model)
                        .testConnection()
                        .getOrThrow()
                }
            },
            onUI = { result ->
                aiTestConnectionButton.isDisable = false
                result
                    .onSuccess {
                        WindowUtils.showInformationDialog(
                            preferencesService.translate(TranslationKeys.SETTINGS_AI_TEST_CONNECTION),
                            preferencesService.translate(TranslationKeys.SETTINGS_AI_TEST_CONNECTION_SUCCESS),
                            preferencesService.bundle,
                        )
                    }.onFailure { e ->
                        logger.warn("AI provider connection test failed: {}", e.message)
                        WindowUtils.showErrorDialog(
                            preferencesService.translate(TranslationKeys.SETTINGS_AI_TEST_CONNECTION),
                            MessageFormat.format(
                                preferencesService.translate(TranslationKeys.SETTINGS_AI_TEST_CONNECTION_ERROR),
                                e.message ?: "",
                            ),
                            preferencesService.bundle,
                        )
                    }
            },
        )
    }

    @FXML
    fun handleSaveAiSettings() {
        val provider = aiProviderComboBox.value ?: return
        val typedKey = aiApiKeyField.text?.trim().orEmpty()

        if (typedKey.isNotBlank()) {
            aiCredentialStore.saveApiKey(provider, typedKey)
        }
        aiCredentialStore.saveModel(provider, aiModelField.text?.trim().orEmpty())
        aiCredentialStore.selectedProvider = provider
        aiApiKeyField.clear()
        updateAiKeyStatusLabel(provider)

        WindowUtils.showInformationDialog(
            preferencesService.translate(TranslationKeys.SETTINGS_AI_SECTION_TITLE),
            preferencesService.translate(TranslationKeys.SETTINGS_AI_SAVE_SUCCESS),
            preferencesService.bundle,
        )
    }

    @FXML
    fun handleClearAiKey() {
        val provider = aiProviderComboBox.value ?: return
        aiCredentialStore.clearApiKey(provider)
        aiApiKeyField.clear()
        updateAiKeyStatusLabel(provider)
    }

    private fun commitSavingsRateTarget() {
        val parsed =
            savingsRateTargetField.text
                .trim()
                .replace(',', '.')
                .toBigDecimalOrNull()
        val target =
            parsed
                ?.setScale(2, RoundingMode.HALF_UP)
                ?.coerceIn(MIN_SAVINGS_RATE_TARGET, MAX_SAVINGS_RATE_TARGET)
                ?: preferencesService.savingsRateTarget
        preferencesService.savingsRateTarget = target
        savingsRateTargetField.text = target.stripTrailingZeros().toPlainString()
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
