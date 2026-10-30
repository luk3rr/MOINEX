package org.moinex.common.ui

import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.control.ComboBox
import javafx.scene.control.Control
import javafx.scene.control.DatePicker
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import java.math.BigDecimal

/**
 * Inline validation of a dialog form: each failing field gets the `field-invalid` class and an error label
 * under it, and the first invalid field takes the focus. Errors that come from services stay in an Alert.
 */
class FieldValidation(
    private val translate: (String) -> String,
) {
    private class Rule(
        val control: Control,
        val check: () -> String?,
    )

    private val rules = mutableListOf<Rule>()
    private val errorLabels = mutableMapOf<Control, Label>()
    private val watched = mutableSetOf<Control>()

    fun required(control: Control) {
        rules += Rule(control) { requiredError(isEmpty(control)) }
    }

    fun number(control: TextField) {
        rules += Rule(control) { numberError(control.text, positive = false) }
    }

    fun positiveNumber(control: TextField) {
        rules += Rule(control) { numberError(control.text, positive = true) }
    }

    fun validate(): Boolean {
        clearErrors()
        var firstInvalid: Control? = null
        rules.forEach { rule ->
            if (rule.control in errorLabels) return@forEach
            val messageKey = rule.check() ?: return@forEach
            showError(rule.control, translate(messageKey))
            if (firstInvalid == null) firstInvalid = rule.control
        }
        firstInvalid?.requestFocus()
        return firstInvalid == null
    }

    fun clearErrors() {
        errorLabels.keys.toList().forEach(::clearError)
    }

    private fun showError(
        control: Control,
        message: String,
    ) {
        control.styleClass.add(Styles.FIELD_INVALID)
        val label = Label(message).apply { styleClass.add(Styles.FIELD_ERROR) }
        errorLabels[control] = label
        rowOf(control)?.let { row ->
            val box = row.parent as VBox
            box.children.add(box.children.indexOf(row) + 1, label)
        }
        resizeWindow(control)
        if (watched.add(control)) {
            when (control) {
                is TextField -> control.textProperty().addListener { _, _, _ -> clearError(control) }
                is ComboBox<*> -> control.valueProperty().addListener { _, _, _ -> clearError(control) }
                is DatePicker -> control.valueProperty().addListener { _, _, _ -> clearError(control) }
            }
        }
    }

    private fun clearError(control: Control) {
        control.styleClass.remove(Styles.FIELD_INVALID)
        errorLabels.remove(control)?.let { label -> (label.parent as? VBox)?.children?.remove(label) }
        resizeWindow(control)
    }

    private fun resizeWindow(control: Control) {
        Platform.runLater { control.scene?.window?.sizeToScene() }
    }

    private fun rowOf(control: Control): Node? {
        var node: Node? = control
        while (node != null && node.parent !is VBox) node = node.parent
        return node
    }

    private fun isEmpty(control: Control): Boolean =
        when (control) {
            is TextField -> control.text.isNullOrBlank()
            is ComboBox<*> -> control.value == null
            is DatePicker -> control.value == null
            else -> false
        }

    companion object {
        fun requiredError(isEmpty: Boolean): String? = if (isEmpty) TranslationKeys.DIALOG_VALIDATION_REQUIRED else null

        fun numberError(
            text: String?,
            positive: Boolean,
        ): String? {
            if (text.isNullOrBlank()) return null
            val value = text.trim().toBigDecimalOrNull() ?: return TranslationKeys.DIALOG_VALIDATION_INVALID_NUMBER
            return if (positive && value <= BigDecimal.ZERO) TranslationKeys.DIALOG_VALIDATION_POSITIVE else null
        }
    }
}
