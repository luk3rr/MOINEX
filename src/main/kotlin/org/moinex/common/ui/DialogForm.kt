package org.moinex.common.ui

import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.TextArea
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

/**
 * Keyboard behavior shared by the create and edit dialogs: initial focus, Enter saves and Esc cancels.
 */
object DialogForm {
    /**
     * Installs the behavior on [root]; the key handler is attached to its scene as soon as it exists.
     */
    fun install(
        root: Node,
        firstFocus: Node?,
        onSave: () -> Unit,
        onCancel: () -> Unit,
    ) {
        root.sceneProperty().addListener { _, _, scene ->
            if (scene == null) return@addListener
            scene.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
                val owner = scene.focusOwner
                val popupShowing = popupShowing(owner)
                val saves = enterTriggersSave(owner is TextArea, owner is Button, popupShowing)
                when {
                    event.code == KeyCode.ENTER &&
                        enterTriggersSave(
                            owner is TextArea,
                            owner is Button,
                            popupShowing,
                        ) -> {
                        event.consume()
                        onSave()
                    }
                    event.code == KeyCode.ESCAPE && !popupShowing -> {
                        event.consume()
                        onCancel()
                    }
                }
            }
            firstFocus?.let { javafx.application.Platform.runLater { it.requestFocus() } }
        }
    }

    /**
     * Enter saves unless it already has a meaning for the focused control.
     */
    fun enterTriggersSave(
        focusInTextArea: Boolean,
        focusOnButton: Boolean,
        popupShowing: Boolean,
    ): Boolean = !focusInTextArea && !focusOnButton && !popupShowing

    private fun popupShowing(owner: Node?): Boolean =
        when (owner) {
            is ComboBox<*> -> owner.isShowing
            is DatePicker -> owner.isShowing
            else -> false
        }
}
