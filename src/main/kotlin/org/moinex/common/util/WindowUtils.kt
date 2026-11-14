/*
 * Filename: WindowUtils.kt (original filename: WindowUtils.java)
 * Created on: October 12, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.common.util

import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.ContextMenu
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.stage.Modality
import javafx.stage.Stage
import org.moinex.app.JavaFXApp
import org.moinex.common.constant.Files
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.DialogCard
import org.moinex.common.ui.DialogHost
import org.moinex.common.ui.DialogSize
import org.moinex.common.ui.DialogSizeStore
import org.moinex.common.ui.Icons
import org.moinex.common.ui.PreferencesDialogSizeStore
import org.moinex.common.ui.closeDialog
import org.moinex.common.ui.icon
import org.moinex.model.enums.NotificationType
import org.moinex.service.PreferencesService
import org.moinex.ui.common.ToastHub
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import java.util.ResourceBundle

object WindowUtils {
    private val logger = LoggerFactory.getLogger(WindowUtils::class.java)
    private const val SUCCESS_ICON_SIZE = 48
    private const val CONFIRMATION_ICON_SIZE = 32
    private const val CONFIRMATION_SPACING = 12.0
    private const val CONFIRMATION_BUTTON_SPACING = 8.0
    private const val CONFIRMATION_PADDING = 16.0

    /**
     * Overlay host of the main window; null until the main window exists, when dialogs fall back to windows of their own.
     */
    @Volatile
    var dialogHost: DialogHost? = null

    /**
     * Where the size the user gave to each dialog is remembered.
     */
    var dialogSizes: DialogSizeStore = PreferencesDialogSizeStore(PreferencesService.preferences)

    /**
     * Name that identifies the dialog of [fxmlFileName] in [dialogSizes].
     */
    fun sizeKeyOf(fxmlFileName: String): String = fxmlFileName.substringAfterLast('/').removeSuffix(".fxml")

    /**
     * Toast hub of the main window; null until the main window exists, when messages fall back to alerts.
     */
    @Volatile
    var toastHub: ToastHub? = null

    private fun Alert.setAttributes(
        title: String,
        header: String,
        message: String,
    ) {
        this.title = title
        headerText = header
        contentText = message
    }

    private fun getAlertTitle(
        type: AlertType,
        resources: ResourceBundle?,
    ): String {
        return resources?.getString(
            when (type) {
                AlertType.CONFIRMATION -> TranslationKeys.DIALOG_CONFIRMATION_TITLE
                AlertType.INFORMATION -> TranslationKeys.DIALOG_INFO_TITLE
                AlertType.ERROR -> TranslationKeys.DIALOG_ERROR_TITLE
                else -> return ""
            },
        ) ?: when (type) {
            AlertType.CONFIRMATION -> "Confirmation"
            AlertType.INFORMATION -> "Info"
            AlertType.ERROR -> "Error"
            else -> ""
        }
    }

    @JvmOverloads
    fun showConfirmationDialog(
        header: String,
        message: String,
        resources: ResourceBundle? = null,
        danger: Boolean = false,
    ): Boolean {
        val host = dialogHost ?: return showConfirmationAlert(header, message, resources)

        val yesText = resources?.getString(TranslationKeys.DIALOG_BUTTON_YES) ?: "Yes"
        val noText = resources?.getString(TranslationKeys.DIALOG_BUTTON_NO) ?: "No"
        var confirmed = false
        lateinit var card: DialogCard

        val no = Button(noText).apply { setOnAction { card.closeDialog() } }
        val yes =
            Button(yesText).apply {
                styleClass.add(if (danger) Styles.BUTTON_DANGER else Styles.BUTTON_PRIMARY)
                setOnAction {
                    confirmed = true
                    card.closeDialog()
                }
            }
        val messageIcon =
            icon(if (danger) Icons.WARNING else Icons.QUESTION, CONFIRMATION_ICON_SIZE).apply {
                styleClass.add(if (danger) Styles.ICON_NEGATIVE else Styles.ICON_ACCENT)
            }
        val messageLabel =
            Label(message).apply {
                isWrapText = true
                HBox.setHgrow(this, Priority.ALWAYS)
            }
        val body =
            HBox().apply {
                spacing = CONFIRMATION_SPACING
                alignment = Pos.CENTER_LEFT
                padding = Insets(CONFIRMATION_PADDING)
                children.addAll(messageIcon, messageLabel)
            }

        card =
            DialogCard(
                header,
                body,
                size = DialogSize.SMALL,
                closable = false,
                sizeKey = "confirmation",
                sizeStore = dialogSizes,
                footer =
                    HBox().apply {
                        spacing = CONFIRMATION_BUTTON_SPACING
                        alignment = Pos.CENTER
                        children.addAll(no, yes)
                    },
            )
        card.initialFocus = no
        host.show(card)

        return confirmed
    }

    private fun showConfirmationAlert(
        header: String,
        message: String,
        resources: ResourceBundle?,
    ): Boolean {
        val alert = Alert(AlertType.CONFIRMATION)

        val (yesButton, noButton) =
            if (resources != null) {
                ButtonType(resources.getString(TranslationKeys.DIALOG_BUTTON_YES)) to
                    ButtonType(resources.getString(TranslationKeys.DIALOG_BUTTON_NO))
            } else {
                ButtonType.YES to ButtonType.NO
            }

        alert.buttonTypes.setAll(noButton, yesButton)
        alert.setAttributes(getAlertTitle(AlertType.CONFIRMATION, resources), header, message)

        return alert.showAndWait().orElse(noButton) == yesButton
    }

    @JvmOverloads
    fun showInformationDialog(
        header: String,
        message: String,
        resources: ResourceBundle? = null,
    ) {
        if (showToast(NotificationType.WARNING, header, message, sticky = false)) return

        Alert(AlertType.INFORMATION).apply {
            setAttributes(getAlertTitle(AlertType.INFORMATION, resources), header, message)
            showAndWait()
        }
    }

    @JvmOverloads
    fun showErrorDialog(
        header: String,
        message: String,
        resources: ResourceBundle? = null,
    ) {
        if (showToast(NotificationType.ERROR, header, message, sticky = true)) return

        Alert(AlertType.ERROR).apply {
            setAttributes(getAlertTitle(AlertType.ERROR, resources), header, message)
            showAndWait()
        }
    }

    @JvmOverloads
    fun showSuccessDialog(
        header: String,
        message: String,
        resources: ResourceBundle? = null,
    ) {
        if (showToast(NotificationType.SUCCESS, header, message, sticky = false)) return

        Alert(AlertType.INFORMATION).apply {
            graphic =
                icon(Icons.SUCCESS, SUCCESS_ICON_SIZE).apply {
                    styleClass.add(Styles.ICON_POSITIVE)
                }

            val title =
                resources?.getString(TranslationKeys.DIALOG_SUCCESS_TITLE) ?: "Success"
            setAttributes(title, header, message)
            showAndWait()
        }
    }

    /**
     * Shows a toast when the main window has a [toastHub], and tells whether it did.
     */
    private fun showToast(
        type: NotificationType,
        header: String,
        message: String,
        sticky: Boolean,
    ): Boolean {
        val hub = toastHub ?: return false

        if (Platform.isFxApplicationThread()) {
            hub.show(type, header, message, sticky)
        } else {
            Platform.runLater { hub.show(type, header, message, sticky) }
        }
        return true
    }

    @JvmOverloads
    fun showDetailDialog(
        header: String,
        content: Node,
        resources: ResourceBundle? = null,
    ) {
        val host = dialogHost
        if (host == null) {
            Alert(AlertType.INFORMATION).apply {
                title = getAlertTitle(AlertType.INFORMATION, resources)
                headerText = header
                dialogPane.content = content
                dialogPane.minWidth = 500.0
                isResizable = true
                showAndWait()
            }
            return
        }

        lateinit var card: DialogCard
        val closeText = resources?.getString(TranslationKeys.DIALOG_BUTTON_CLOSE) ?: "Close"
        card =
            DialogCard(
                header,
                content,
                size = DialogSize.WIDE,
                closeTooltip = closeText,
                sizeKey = "detail",
                sizeStore = dialogSizes,
                footer = Button(closeText).apply { setOnAction { card.closeDialog() } },
            )
        host.show(card)
    }

    fun centerWindowOnScreen(stage: Stage) {
        stage.centerOnScreen()
    }

    /**
     * Loads [fxmlFileName] with its Spring-managed controller to be placed inside another window, and returns the root
     * node and the controller.
     */
    fun <T> loadEmbedded(
        fxmlFileName: String,
        springContext: ApplicationContext,
    ): Pair<Parent, T> {
        val bundle = springContext.getBean<PreferencesService>().bundle
        val loader =
            FXMLLoader(WindowUtils::class.java.getResource(fxmlFileName), bundle).apply {
                setControllerFactory { springContext.getBean(it) }
            }

        return loader.load<Parent>() to loader.getController<T>()
    }

    @JvmOverloads
    fun <T> openModalWindow(
        fxmlFileName: String,
        title: String,
        springContext: ApplicationContext,
        controllerSetup: (T) -> Unit,
        onHiddenActions: List<Runnable> = emptyList(),
        resources: ResourceBundle? = null,
    ) {
        val host = dialogHost
        if (host == null) {
            openModalStage(fxmlFileName, title, springContext, controllerSetup, onHiddenActions, resources)
            return
        }

        val bundle = resources ?: springContext.getBean<PreferencesService>().bundle

        runCatching {
            val loader =
                FXMLLoader(WindowUtils::class.java.getResource(fxmlFileName), bundle).apply {
                    setControllerFactory { springContext.getBean(it) }
                }

            val root = loader.load<Parent>()
            val controller: T = loader.getController()
            controllerSetup(controller)

            val card =
                DialogCard(
                    title,
                    root,
                    closeTooltip = bundle.getString(TranslationKeys.DIALOG_BUTTON_CLOSE),
                    width = maxOf(DialogSize.DEFAULT.width, root.prefWidth(-1.0) + DialogCard.SCROLLBAR_ALLOWANCE),
                    sizeKey = sizeKeyOf(fxmlFileName),
                    sizeStore = dialogSizes,
                )
            card.stylesheets.add(
                WindowUtils::class.java.getResource(Files.COMMON_STYLE_SHEET)?.toExternalForm()
                    ?: throw IllegalStateException("Common stylesheet not found"),
            )
            host.show(card, onHiddenActions)
        }.onFailure { e ->
            logger.error("Error loading FXML file: '{}'", fxmlFileName, e)
        }
    }

    private fun <T> openModalStage(
        fxmlFileName: String,
        title: String,
        springContext: ApplicationContext,
        controllerSetup: (T) -> Unit,
        onHiddenActions: List<Runnable>,
        resources: ResourceBundle?,
    ) {
        val bundle = resources ?: springContext.getBean<PreferencesService>().bundle

        runCatching {
            val loader =
                FXMLLoader(WindowUtils::class.java.getResource(fxmlFileName), bundle).apply {
                    setControllerFactory { springContext.getBean(it) }
                }

            val root = loader.load<Parent>()

            Stage().apply {
                initModality(Modality.APPLICATION_MODAL)
                scene =
                    Scene(root).apply {
                        addEventHandler(KeyEvent.KEY_PRESSED) { event ->
                            if (event.code == KeyCode.ESCAPE) {
                                event.consume()
                                window?.hide()
                            }
                        }
                        stylesheets.add(
                            WindowUtils::class.java
                                .getResource(Files.COMMON_STYLE_SHEET)
                                ?.toExternalForm()
                                ?: throw IllegalStateException("Common stylesheet not found"),
                        )
                    }

                val controller: T = loader.getController()
                controllerSetup(controller)

                this.title = title
                setOnHidden { onHiddenActions.forEach(Runnable::run) }
                showAndWait()
            }
        }.onFailure { e ->
            logger.error("Error loading FXML file: '{}'", fxmlFileName, e)
        }
    }

    @JvmOverloads
    fun <T> openPopupWindow(
        fxmlFileName: String,
        title: String,
        springContext: ApplicationContext,
        controllerSetup: (T) -> Unit,
        onHiddenActions: List<Runnable> = emptyList(),
        resources: ResourceBundle? = null,
    ) {
        val bundle = resources ?: springContext.getBean<PreferencesService>().bundle

        runCatching {
            val loader =
                FXMLLoader(WindowUtils::class.java.getResource(fxmlFileName), bundle).apply {
                    setControllerFactory { springContext.getBean(it) }
                }

            val root = loader.load<Parent>()

            Stage().apply {
                scene =
                    Scene(root).apply {
                        stylesheets.add(
                            WindowUtils::class.java
                                .getResource(Files.COMMON_STYLE_SHEET)
                                ?.toExternalForm()
                                ?: throw IllegalStateException("Common stylesheet not found"),
                        )
                    }

                val controller: T = loader.getController()
                controllerSetup(controller)

                this.title = title
                setOnHidden { onHiddenActions.forEach(Runnable::run) }
                showAndWait()
            }
        }.onFailure { e ->
            logger.error("Error loading FXML file: '{}'", fxmlFileName, e)
        }
    }

    fun openUrl(url: String) {
        val normalizedUrl =
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }

        runCatching {
            val hostServices = JavaFXApp.getHostServicesInstance()
            hostServices.showDocument(normalizedUrl)
        }.onFailure { e ->
            logger.error("Error opening URL: {}", normalizedUrl, e)
            showErrorDialog("Error", "Could not open URL: $normalizedUrl\n\nError: ${e.message}")
        }
    }

    /**
     * Adds a right-click menu to [control] that copies [url] to the clipboard, so the user can still reach the page
     * when the system has no working default browser.
     */
    fun addCopyLinkMenu(
        control: Control,
        url: String,
        copyLabel: String,
    ) {
        control.contextMenu =
            ContextMenu(
                MenuItem(copyLabel).apply {
                    setOnAction {
                        Clipboard.getSystemClipboard().setContent(ClipboardContent().apply { putString(url) })
                    }
                },
            )
    }
}
