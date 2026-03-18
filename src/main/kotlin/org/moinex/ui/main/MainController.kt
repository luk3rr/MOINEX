/*
 * Filename: MainController.kt (original filename: MainController.java)
 * Created on: September 19, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 16/03/2026
 */

package org.moinex.ui.main

import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.util.Duration
import javafx.util.Pair
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.service.PreferencesService
import org.moinex.ui.common.CalculatorController
import org.moinex.ui.common.CalendarController
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.io.IOException

@Controller
class MainController(
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var sidebar: VBox

    @FXML
    private lateinit var rootPane: AnchorPane

    @FXML
    private lateinit var footbarArea: HBox

    @FXML
    private lateinit var menuButton: Button

    @FXML
    private lateinit var homeButton: Button

    @FXML
    private lateinit var walletButton: Button

    @FXML
    private lateinit var creditCardButton: Button

    @FXML
    private lateinit var transactionButton: Button

    @FXML
    private lateinit var goalsAndPlanButton: Button

    @FXML
    private lateinit var savingsButton: Button

    @FXML
    private lateinit var settingsButton: Button

    @FXML
    private lateinit var contentArea: AnchorPane

    @FXML
    private lateinit var toggleMonetaryValuesIcon: ImageView

    private var currentContent: Pair<String, String>? = null
    private var isMenuExpanded = false
    private lateinit var sidebarButtons: Array<Button>

    companion object {
        private val logger = LoggerFactory.getLogger(MainController::class.java)
    }

    @FXML
    fun initialize() {
        sidebarButtons =
            arrayOf(
                menuButton,
                homeButton,
                walletButton,
                creditCardButton,
                transactionButton,
                goalsAndPlanButton,
                savingsButton,
                settingsButton,
            )

        rootPane.stylesheets.add(
            javaClass.getResource(Constants.MAIN_STYLE_SHEET)!!.toExternalForm(),
        )

        footbarArea.stylesheets.add(
            javaClass.getResource(Constants.MAIN_STYLE_SHEET)!!.toExternalForm(),
        )

        menuButton.setOnAction { toggleMenu() }

        homeButton.setOnAction {
            loadContent(Constants.HOME_FXML, Constants.HOME_STYLE_SHEET)
            updateSelectedButton(homeButton)
        }

        walletButton.setOnAction {
            loadContent(Constants.WALLET_FXML, Constants.WALLET_STYLE_SHEET)
            updateSelectedButton(walletButton)
        }

        creditCardButton.setOnAction {
            loadContent(Constants.CREDIT_CARD_FXML, Constants.CREDIT_CARD_STYLE_SHEET)
            updateSelectedButton(creditCardButton)
        }

        transactionButton.setOnAction {
            loadContent(Constants.TRANSACTION_FXML, Constants.TRANSACTION_STYLE_SHEET)
            updateSelectedButton(transactionButton)
        }

        goalsAndPlanButton.setOnAction {
            loadContent(Constants.GOALS_AND_PLANS_FXML, Constants.GOALS_AND_PLANS_STYLE_SHEET)
            updateSelectedButton(goalsAndPlanButton)
        }

        savingsButton.setOnAction {
            loadContent(Constants.SAVINGS_FXML, Constants.SAVINGS_STYLE_SHEET)
            updateSelectedButton(savingsButton)
        }

        settingsButton.setOnAction {
            loadContent(Constants.SETTINGS_FXML, Constants.SETTINGS_STYLE_SHEET)
            updateSelectedButton(settingsButton)
        }

        loadContent(Constants.HOME_FXML, Constants.HOME_STYLE_SHEET)
        updateSelectedButton(homeButton)
    }

    @FXML
    private fun handleOpenCalculator() {
        WindowUtils.openPopupWindow(
            Constants.CALCULATOR_FXML,
            preferencesService.translate(TranslationKeys.MAIN_CALCULATOR),
            springContext,
            { _: CalculatorController -> },
            listOf(Runnable { }),
            preferencesService.getBundle(),
        )
    }

    @FXML
    private fun handleOpenCalendar() {
        WindowUtils.openPopupWindow(
            Constants.CALENDAR_FXML,
            preferencesService.translate(TranslationKeys.MAIN_CALENDAR),
            springContext,
            { _: CalendarController -> },
            listOf(Runnable { }),
            preferencesService.getBundle(),
        )
    }

    @FXML
    private fun handleOpenSettings() {
        loadContent(Constants.SETTINGS_FXML, Constants.SETTINGS_STYLE_SHEET)
        updateSelectedButton(settingsButton)
    }

    @FXML
    private fun handleToggleMonetaryValues() {
        preferencesService.toggleHideMonetaryValues()

        toggleMonetaryValuesIcon.image =
            Image(
                if (preferencesService.showMonetaryValues()) {
                    Constants.SHOW_ICON
                } else {
                    Constants.HIDE_ICON
                },
            )

        currentContent?.let { loadContent(it.key, it.value) }
    }

    fun loadContent(
        fxmlFile: String,
        styleSheet: String,
    ) {
        runCatching {
            val loader =
                FXMLLoader(
                    javaClass.getResource(fxmlFile),
                    preferencesService.getBundle(),
                )
            loader.setControllerFactory { springContext.getBean(it) }
            val newContent = loader.load<Parent>()

            newContent.stylesheets.add(
                javaClass.getResource(styleSheet)!!.toExternalForm(),
            )

            newContent.stylesheets.add(
                javaClass.getResource(Constants.COMMON_STYLE_SHEET)!!.toExternalForm(),
            )

            AnchorPane.setTopAnchor(newContent, 0.0)
            AnchorPane.setRightAnchor(newContent, 0.0)
            AnchorPane.setBottomAnchor(newContent, 0.0)
            AnchorPane.setLeftAnchor(newContent, 0.0)

            contentArea.children.clear()
            contentArea.children.add(newContent)

            currentContent = Pair(fxmlFile, styleSheet)
        }.onFailure { e ->
            when (e) {
                is IOException -> {
                    logger.error("Error loading FXML file: '{}'", fxmlFile, e)
                    logger.error(
                        "Failed to load content. FXML: '{}', StyleSheet: '{}', Error: '{}'",
                        fxmlFile,
                        styleSheet,
                        e.message,
                    )
                    e.cause?.let {
                        logger.error("Root cause: {}", it.message, it)
                    }
                }
                else -> {
                    logger.error(
                        "Unexpected exception loading content. FXML: '{}', StyleSheet: '{}'",
                        fxmlFile,
                        styleSheet,
                        e,
                    )
                }
            }
        }
    }

    private fun updateSelectedButton(selectedButton: Button) {
        sidebarButtons.forEach { button ->
            button.styleClass.remove(Constants.SIDEBAR_SELECTED_BUTTON_STYLE)
        }
        selectedButton.styleClass.add(Constants.SIDEBAR_SELECTED_BUTTON_STYLE)
    }

    private fun toggleMenu() {
        val targetWidth =
            if (isMenuExpanded) {
                Constants.MENU_COLLAPSED_WIDTH
            } else {
                Constants.MENU_EXPANDED_WIDTH
            }

        val timeline = Timeline()
        val keyValueSidebarWidth = KeyValue(sidebar.prefWidthProperty(), targetWidth)
        val keyFrame =
            KeyFrame(
                Duration.millis(Constants.MENU_ANIMATION_DURATION.toDouble()),
                keyValueSidebarWidth,
            )

        timeline.keyFrames.add(keyFrame)

        if (isMenuExpanded) {
            setButtonTextVisibility(false)
        }

        timeline.setOnFinished {
            setButtonTextVisibility(isMenuExpanded)
            setButtonWidth(targetWidth)
        }

        timeline.play()
        isMenuExpanded = !isMenuExpanded
    }

    private fun setButtonTextVisibility(visible: Boolean) {
        sidebarButtons.forEach { button ->
            button.contentDisplay =
                if (visible) {
                    ContentDisplay.LEFT
                } else {
                    ContentDisplay.GRAPHIC_ONLY
                }
        }
    }

    private fun setButtonWidth(width: Double) {
        sidebarButtons.forEach { button ->
            button.prefWidth = width
        }
    }
}
