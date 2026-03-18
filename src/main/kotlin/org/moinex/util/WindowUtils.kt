/*
 * Filename: WindowUtils.kt (original filename: WindowUtils.java)
 * Created on: October 12, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.util

import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonType
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.stage.Modality
import javafx.stage.Stage
import org.moinex.app.JavaFXApp
import org.moinex.service.PreferencesService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import java.util.ResourceBundle
import java.util.function.Consumer

object WindowUtils {
    private val logger = LoggerFactory.getLogger(WindowUtils::class.java)

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
                AlertType.CONFIRMATION -> Constants.TranslationKeys.DIALOG_CONFIRMATION_TITLE
                AlertType.INFORMATION -> Constants.TranslationKeys.DIALOG_INFO_TITLE
                AlertType.ERROR -> Constants.TranslationKeys.DIALOG_ERROR_TITLE
                else -> return ""
            },
        ) ?: when (type) {
            AlertType.CONFIRMATION -> "Confirmation"
            AlertType.INFORMATION -> "Info"
            AlertType.ERROR -> "Error"
            else -> ""
        }
    }

    @JvmStatic
    @JvmOverloads
    fun showConfirmationDialog(
        header: String,
        message: String,
        resources: ResourceBundle? = null,
    ): Boolean {
        val alert = Alert(AlertType.CONFIRMATION)

        val (yesButton, noButton) =
            if (resources != null) {
                ButtonType(resources.getString(Constants.TranslationKeys.DIALOG_BUTTON_YES)) to
                    ButtonType(resources.getString(Constants.TranslationKeys.DIALOG_BUTTON_NO))
            } else {
                ButtonType.YES to ButtonType.NO
            }

        alert.buttonTypes.setAll(noButton, yesButton)
        alert.setAttributes(getAlertTitle(AlertType.CONFIRMATION, resources), header, message)

        return alert.showAndWait().orElse(noButton) == yesButton
    }

    @JvmStatic
    @JvmOverloads
    fun showInformationDialog(
        header: String,
        message: String,
        resources: ResourceBundle? = null,
    ) {
        Alert(AlertType.INFORMATION).apply {
            setAttributes(getAlertTitle(AlertType.INFORMATION, resources), header, message)
            showAndWait()
        }
    }

    @JvmStatic
    @JvmOverloads
    fun showErrorDialog(
        header: String,
        message: String,
        resources: ResourceBundle? = null,
    ) {
        Alert(AlertType.ERROR).apply {
            setAttributes(getAlertTitle(AlertType.ERROR, resources), header, message)
            showAndWait()
        }
    }

    @JvmStatic
    @JvmOverloads
    fun showSuccessDialog(
        header: String,
        message: String,
        resources: ResourceBundle? = null,
    ) {
        Alert(AlertType.INFORMATION).apply {
            graphic =
                ImageView(
                    Image(
                        WindowUtils::class.java.getResource(Constants.SUCCESS_ICON)?.toString()
                            ?: throw IllegalStateException("Success icon not found"),
                    ),
                )

            val title =
                resources?.getString(Constants.TranslationKeys.DIALOG_SUCCESS_TITLE) ?: "Success"
            setAttributes(title, header, message)
            showAndWait()
        }
    }

    @JvmStatic
    fun centerWindowOnScreen(stage: Stage) {
        stage.centerOnScreen()
    }

    @JvmStatic
    @JvmOverloads
    fun <T> openModalWindow(
        fxmlFileName: String,
        title: String,
        springContext: ApplicationContext,
        controllerSetup: Consumer<T>, // TODO: Após migração apra kotlin, utilizar controllerSetup: (T) -> Unit
        onHiddenActions: List<Runnable> = emptyList(),
        resources: ResourceBundle? = null,
    ) {
        val bundle = resources ?: springContext.getBean<PreferencesService>().getBundle()

        runCatching {
            val loader =
                FXMLLoader(WindowUtils::class.java.getResource(fxmlFileName), bundle).apply {
                    setControllerFactory { springContext.getBean(it) }
                }

            val root = loader.load<javafx.scene.Parent>()

            Stage().apply {
                initModality(Modality.APPLICATION_MODAL)
                scene =
                    Scene(root).apply {
                        stylesheets.add(
                            WindowUtils::class.java
                                .getResource(Constants.COMMON_STYLE_SHEET)
                                ?.toExternalForm()
                                ?: throw IllegalStateException("Common stylesheet not found"),
                        )
                    }

                val controller: T = loader.getController()
                controllerSetup.accept(controller)

                this.title = title
                setOnHidden { onHiddenActions.forEach(Runnable::run) }
                showAndWait()
            }
        }.onFailure { e ->
            logger.error("Error loading FXML file: '{}'", fxmlFileName, e)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun <T> openPopupWindow(
        fxmlFileName: String,
        title: String,
        springContext: ApplicationContext,
        controllerSetup: Consumer<T>, // TODO: Após migração apra kotlin, utilizar controllerSetup: (T) -> Unit
        onHiddenActions: List<Runnable> = emptyList(),
        resources: ResourceBundle? = null,
    ) {
        val bundle = resources ?: springContext.getBean<PreferencesService>().getBundle()

        runCatching {
            val loader =
                FXMLLoader(WindowUtils::class.java.getResource(fxmlFileName), bundle).apply {
                    setControllerFactory { springContext.getBean(it) }
                }

            val root = loader.load<javafx.scene.Parent>()

            Stage().apply {
                scene =
                    Scene(root).apply {
                        stylesheets.add(
                            WindowUtils::class.java
                                .getResource(Constants.COMMON_STYLE_SHEET)
                                ?.toExternalForm()
                                ?: throw IllegalStateException("Common stylesheet not found"),
                        )
                    }

                val controller: T = loader.getController()
                controllerSetup.accept(controller)

                this.title = title
                setOnHidden { onHiddenActions.forEach(Runnable::run) }
                showAndWait()
            }
        }.onFailure { e ->
            logger.error("Error loading FXML file: '{}'", fxmlFileName, e)
        }
    }

    @JvmStatic
    fun openUrl(url: String) {
        runCatching {
            JavaFXApp.getHostServicesInstance().showDocument(url)
        }.onFailure { e ->
            logger.error("Error opening URL: {}", url, e)
            showErrorDialog("Error", "Could not open URL: $url")
        }
    }
}
