/*
 * Filename: FilePasswordController.kt
 * Created on: October 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.ai

import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.stage.Stage
import org.springframework.stereotype.Controller

/**
 * Dialog that asks for the password of a protected file attached to the AI chat.
 * [password] holds the typed password after the dialog closes, or null when it was cancelled.
 */
@Controller
class FilePasswordController {
    @FXML
    private lateinit var messageLabel: Label

    @FXML
    private lateinit var passwordField: PasswordField

    private var confirmedPassword: String? = null

    val password: String?
        get() = confirmedPassword

    @FXML
    private fun initialize() {
        confirmedPassword = null
    }

    fun setMessage(message: String) {
        messageLabel.text = message
    }

    @FXML
    private fun handleConfirm() {
        if (passwordField.text.isEmpty()) return

        confirmedPassword = passwordField.text
        (passwordField.scene.window as Stage).close()
    }

    @FXML
    private fun handleCancel() {
        (passwordField.scene.window as Stage).close()
    }
}
