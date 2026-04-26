/*
 * Filename: MainController.kt (original filename: MainController.java)
 * Created on: September 19, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 16/03/2026
 */

package org.moinex.ui.main

import javafx.animation.FadeTransition
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.ParallelTransition
import javafx.animation.PauseTransition
import javafx.animation.Timeline
import javafx.animation.TranslateTransition
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.util.Duration
import javafx.util.Pair
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Files
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.setAnchorPaneConstraints
import org.moinex.common.util.WindowUtils
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.moinex.ui.common.CalculatorController
import org.moinex.ui.common.CalendarController
import org.moinex.ui.common.NotificationToastOverlay
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.io.IOException

@Controller
class MainController(
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
    private val notificationService: NotificationService,
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

    @FXML
    private lateinit var bellIcon: ImageView

    @FXML
    private lateinit var unreadBadge: Label

    private var currentContent: Pair<String, String>? = null
    private var isMenuExpanded = false
    private var isNotificationPanelOpen = false
    private lateinit var sidebarButtons: Array<Button>

    private lateinit var notificationScrim: Pane
    private lateinit var notificationPanel: VBox
    private lateinit var notificationCenterController: NotificationCenterController
    private lateinit var toastOverlay: NotificationToastOverlay

    companion object {
        private val logger = LoggerFactory.getLogger(MainController::class.java)
        private const val NOTIFICATION_SCRIM_STYLE = "-fx-background-color: rgba(0,0,0,0.45);"
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
            javaClass.getResource(Files.MAIN_STYLE_SHEET)!!.toExternalForm(),
        )

        rootPane.stylesheets.add(
            javaClass.getResource(Files.NOTIFICATION_CSS)!!.toExternalForm(),
        )

        footbarArea.stylesheets.add(
            javaClass.getResource(Files.MAIN_STYLE_SHEET)!!.toExternalForm(),
        )

        menuButton.setOnAction { toggleMenu() }

        homeButton.setOnAction {
            loadContent(Files.HOME_FXML, Files.HOME_STYLE_SHEET)
            updateSelectedButton(homeButton)
        }

        walletButton.setOnAction {
            loadContent(Files.WALLET_FXML, Files.WALLET_STYLE_SHEET)
            updateSelectedButton(walletButton)
        }

        creditCardButton.setOnAction {
            loadContent(Files.CREDIT_CARD_FXML, Files.CREDIT_CARD_STYLE_SHEET)
            updateSelectedButton(creditCardButton)
        }

        transactionButton.setOnAction {
            loadContent(Files.TRANSACTION_FXML, Files.TRANSACTION_STYLE_SHEET)
            updateSelectedButton(transactionButton)
        }

        goalsAndPlanButton.setOnAction {
            loadContent(Files.GOALS_AND_PLANS_FXML, Files.GOALS_AND_PLANS_STYLE_SHEET)
            updateSelectedButton(goalsAndPlanButton)
        }

        savingsButton.setOnAction {
            loadContent(Files.SAVINGS_FXML, Files.SAVINGS_STYLE_SHEET)
            updateSelectedButton(savingsButton)
        }

        settingsButton.setOnAction {
            loadContent(Files.SETTINGS_FXML, Files.SETTINGS_STYLE_SHEET)
            updateSelectedButton(settingsButton)
        }

        setupNotificationDrawer()

        // Toast overlay must be added last to stay on top of everything (highest Z-order)
        toastOverlay = NotificationToastOverlay(rootPane)

        notificationService.registerUiListener { notification ->
            toastOverlay.show(notification)
            animateBellIcon()
            updateUnreadBadge()
        }

        updateUnreadBadge()

        loadContent(Files.HOME_FXML, Files.HOME_STYLE_SHEET)
        updateSelectedButton(homeButton)
    }

    @FXML
    private fun handleOpenCalculator() {
        WindowUtils.openPopupWindow(
            Files.CALCULATOR_FXML,
            preferencesService.translate(TranslationKeys.MAIN_CALCULATOR),
            springContext,
            { _: CalculatorController -> },
            listOf(Runnable { }),
            preferencesService.bundle,
        )
    }

    @FXML
    private fun handleOpenCalendar() {
        WindowUtils.openPopupWindow(
            Files.CALENDAR_FXML,
            preferencesService.translate(TranslationKeys.MAIN_CALENDAR),
            springContext,
            { _: CalendarController -> },
            listOf(Runnable { }),
            preferencesService.bundle,
        )
    }

    @FXML
    private fun handleOpenSettings() {
        loadContent(Files.SETTINGS_FXML, Files.SETTINGS_STYLE_SHEET)
        updateSelectedButton(settingsButton)
    }

    @FXML
    private fun handleToggleMonetaryValues() {
        preferencesService.toggleHideMonetaryValues()

        toggleMonetaryValuesIcon.image =
            Image(
                if (preferencesService.showMonetaryValues()) {
                    Files.SHOW_ICON
                } else {
                    Files.HIDE_ICON
                },
            )

        currentContent?.let { loadContent(it.key, it.value) }
    }

    @FXML
    private fun handleToggleNotifications() {
        if (isNotificationPanelOpen) closeNotificationPanel() else openNotificationPanel()
    }

    fun loadContent(
        fxmlFile: String,
        styleSheet: String,
    ) {
        runCatching {
            val loader =
                FXMLLoader(
                    javaClass.getResource(fxmlFile),
                    preferencesService.bundle,
                )
            loader.setControllerFactory { springContext.getBean(it) }
            val newContent = loader.load<Parent>()

            newContent.stylesheets.add(
                javaClass.getResource(styleSheet)!!.toExternalForm(),
            )

            newContent.stylesheets.add(
                javaClass.getResource(Files.COMMON_STYLE_SHEET)!!.toExternalForm(),
            )

            newContent.setAnchorPaneConstraints()

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

    private fun setupNotificationDrawer() {
        notificationScrim =
            Pane().apply {
                style = NOTIFICATION_SCRIM_STYLE
                isVisible = false
                opacity = 0.0
                setOnMouseClicked { closeNotificationPanel() }
            }
        rootPane.children.add(notificationScrim)
        notificationScrim.setAnchorPaneConstraints()

        val loader =
            FXMLLoader(
                javaClass.getResource(Files.NOTIFICATION_CENTER_FXML),
                preferencesService.bundle,
            )
        loader.setControllerFactory { springContext.getBean(it) }
        notificationPanel = loader.load()
        notificationCenterController = loader.getController()

        notificationPanel.isVisible = false
        notificationPanel.translateX = Constants.NOTIFICATION_PANEL_WIDTH

        rootPane.children.add(notificationPanel)
        AnchorPane.setTopAnchor(notificationPanel, 0.0)
        AnchorPane.setBottomAnchor(notificationPanel, 0.0)
        AnchorPane.setRightAnchor(notificationPanel, 0.0)
    }

    private fun openNotificationPanel() {
        notificationCenterController.refresh()
        notificationService.markAllAsRead()

        notificationPanel.isVisible = true
        notificationScrim.isVisible = true

        val slideIn =
            TranslateTransition(Duration.millis(Constants.NOTIFICATION_DRAWER_ANIMATION_MS), notificationPanel).apply {
                fromX = Constants.NOTIFICATION_PANEL_WIDTH
                toX = 0.0
            }
        val fadeIn =
            FadeTransition(Duration.millis(Constants.NOTIFICATION_DRAWER_ANIMATION_MS), notificationScrim).apply {
                fromValue = 0.0
                toValue = 1.0
            }

        ParallelTransition(slideIn, fadeIn).play()
        isNotificationPanelOpen = true
        updateUnreadBadge()
    }

    private fun closeNotificationPanel() {
        val slideOut =
            TranslateTransition(Duration.millis(Constants.NOTIFICATION_DRAWER_ANIMATION_MS), notificationPanel).apply {
                fromX = 0.0
                toX = Constants.NOTIFICATION_PANEL_WIDTH
            }
        val fadeOut =
            FadeTransition(Duration.millis(Constants.NOTIFICATION_DRAWER_ANIMATION_MS), notificationScrim).apply {
                fromValue = 1.0
                toValue = 0.0
            }

        ParallelTransition(slideOut, fadeOut)
            .apply {
                setOnFinished {
                    notificationPanel.isVisible = false
                    notificationScrim.isVisible = false
                }
            }.play()

        isNotificationPanelOpen = false
    }

    private fun updateUnreadBadge() {
        val count = notificationService.countUnread()
        unreadBadge.text = if (count > 9) "9+" else count.toString()
        unreadBadge.isVisible = count > 0
    }

    private fun animateBellIcon() {
        bellIcon.image = Image(Files.BELL_GIF)

        val restore =
            PauseTransition(Duration.millis(Constants.NOTIFICATION_BELL_GIF_DURATION_MS))
        restore.setOnFinished {
            bellIcon.image = Image(Files.BELL_ICON)
        }
        restore.play()
    }

    private fun updateSelectedButton(selectedButton: Button) {
        sidebarButtons.forEach { button ->
            button.styleClass.remove(Styles.SIDEBAR_SELECTED_BUTTON_STYLE)
        }
        selectedButton.styleClass.add(Styles.SIDEBAR_SELECTED_BUTTON_STYLE)
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
                Duration.millis(Constants.MENU_ANIMATION_DURATION),
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
