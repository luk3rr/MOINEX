/*
 * Filename: DraftEditorDialog.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common.ai

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.util.StringConverter
import org.moinex.ai.operation.DraftChoices
import org.moinex.ai.operation.DraftField
import org.moinex.ai.operation.DraftFieldType
import org.moinex.ai.operation.DraftValues
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.service.PreferencesService
import java.math.BigDecimal
import java.time.LocalDate

/** An option of a choice field: [value] is stored in the draft, [label] is what the user reads. */
data class DraftChoice(
    val value: Any?,
    val label: String,
)

/** Where the editor gets the lists behind its choice fields. */
fun interface DraftChoiceSource {
    fun choices(kind: DraftChoices): List<DraftChoice>
}

/**
 * The fallback editor of a proposal: a plain form built from the fields an operation declares. It returns the edited
 * values without saving anything; the review session decides what to do with them.
 */
class DraftEditorDialog(
    private val preferencesService: PreferencesService,
    private val choiceSource: DraftChoiceSource,
) {
    private sealed interface Editor {
        val field: DraftField

        fun node(): Node

        fun read(): Any?

        fun isFilled(): Boolean

        fun onChange(listener: () -> Unit)

        fun write(value: Any?) {}

        fun lock(locked: Boolean) {
            node().isDisable = locked
        }
    }

    private class TextEditor(
        override val field: DraftField,
        initial: Any?,
    ) : Editor {
        private val input = TextField(initial?.toString().orEmpty())

        override fun node(): Node = input

        override fun read(): Any? = input.text.trim()

        override fun isFilled() = input.text.isNotBlank()

        override fun onChange(listener: () -> Unit) {
            input.textProperty().addListener { _, _, _ -> listener() }
        }
    }

    private class DecimalEditor(
        override val field: DraftField,
        initial: Any?,
    ) : Editor {
        private val input = TextField((initial as? BigDecimal)?.toPlainString().orEmpty())

        override fun node(): Node = input

        override fun read(): Any? =
            input.text
                .trim()
                .replace(',', '.')
                .toBigDecimalOrNull()

        override fun isFilled() = read() != null

        override fun onChange(listener: () -> Unit) {
            input.textProperty().addListener { _, _, _ -> listener() }
        }

        override fun write(value: Any?) {
            input.text = (value as? BigDecimal)?.toPlainString().orEmpty()
        }
    }

    private class IntegerEditor(
        override val field: DraftField,
        initial: Any?,
    ) : Editor {
        private val input = TextField((initial as? Number)?.toInt()?.toString().orEmpty())

        override fun node(): Node = input

        override fun read(): Any? = input.text.trim().toIntOrNull()

        override fun isFilled() = read() != null

        override fun onChange(listener: () -> Unit) {
            input.textProperty().addListener { _, _, _ -> listener() }
        }
    }

    private class DateEditor(
        override val field: DraftField,
        initial: Any?,
    ) : Editor {
        private val input = DatePicker(initial as? LocalDate)

        override fun node(): Node = input

        override fun read(): Any? = input.value

        override fun isFilled() = input.value != null

        override fun onChange(listener: () -> Unit) {
            input.valueProperty().addListener { _, _, _ -> listener() }
        }
    }

    private class BooleanEditor(
        override val field: DraftField,
        initial: Any?,
    ) : Editor {
        private val input = CheckBox().apply { isSelected = initial as? Boolean ?: false }

        override fun node(): Node = input

        override fun read(): Any? = input.isSelected

        override fun isFilled() = true

        override fun onChange(listener: () -> Unit) {
            input.selectedProperty().addListener { _, _, _ -> listener() }
        }
    }

    private class ChoiceEditor(
        override val field: DraftField,
        options: List<DraftChoice>,
        initial: Any?,
    ) : Editor {
        private val input =
            ComboBox<DraftChoice>().apply {
                items.setAll(options)
                converter =
                    object : StringConverter<DraftChoice>() {
                        override fun toString(value: DraftChoice?) = value?.label.orEmpty()

                        override fun fromString(string: String?) = options.firstOrNull { it.label == string }
                    }
                value = options.firstOrNull { it.value == initial }
            }

        override fun node(): Node = input

        override fun read(): Any? = input.value?.value

        override fun isFilled() = input.value != null

        override fun onChange(listener: () -> Unit) {
            input.valueProperty().addListener { _, _, _ -> listener() }
        }
    }

    /**
     * Shows the form over [initial] and returns the values the user saved, or null when they cancelled or the saved
     * values are not valid. Fields of [initial] that the form does not show are kept as they were.
     */
    fun show(
        title: String,
        spec: List<DraftField>,
        initial: DraftValues,
        suggest: (DraftValues) -> Map<String, Any?> = { emptyMap() },
    ): DraftValues? = build(title, spec, initial, suggest).showAndWait().orElse(null)

    internal fun build(
        title: String,
        spec: List<DraftField>,
        initial: DraftValues,
        suggest: (DraftValues) -> Map<String, Any?> = { emptyMap() },
    ): Dialog<DraftValues?> {
        val editors = spec.map { buildEditor(it, initial.fields[it.key]) }
        wireAutoCalculate(editors, initial, suggest)
        val grid =
            GridPane().apply {
                hgap = GAP
                vgap = GAP
                columnConstraints.addAll(
                    ColumnConstraints().apply { minWidth = LABEL_WIDTH },
                    ColumnConstraints().apply {
                        hgrow = Priority.ALWAYS
                        isFillWidth = true
                    },
                )
                editors.forEachIndexed { row, editor ->
                    add(
                        Label(preferencesService.translate(editor.field.labelKey)).apply {
                            styleClass.add("ai-draft-label")
                        },
                        0,
                        row,
                    )
                    val node = editor.node()
                    (node as? Region)?.maxWidth = Double.MAX_VALUE
                    GridPane.setHgrow(node, Priority.ALWAYS)
                    add(node, 1, row)
                }
            }
        val banner =
            TextFlow(
                Text(preferencesService.translate(TranslationKeys.AIPANEL_DRAFT_BANNER_TITLE) + ":").apply {
                    styleClass.add("ai-draft-banner-title")
                },
                Text(" " + preferencesService.translate(TranslationKeys.AIPANEL_DRAFT_BANNER)).apply {
                    styleClass.add("ai-draft-banner-text")
                },
            ).apply {
                styleClass.add("ai-draft-banner")
                prefWidth = CONTENT_WIDTH
                maxWidth = CONTENT_WIDTH
            }
        val error =
            Label(preferencesService.translate(TranslationKeys.AIPANEL_DRAFT_INVALID)).apply {
                isVisible = false
                isManaged = false
                styleClass.add("ai-review-error")
            }
        val dialog = Dialog<DraftValues?>()
        val cancelButton =
            Button(preferencesService.translate(TranslationKeys.DIALOG_BUTTON_CANCEL)).apply {
                minWidth = BUTTON_MIN_WIDTH
                setOnAction {
                    dialog.result = null
                    dialog.close()
                }
            }
        val saveButton =
            Button(preferencesService.translate(TranslationKeys.DIALOG_BUTTON_SAVE)).apply {
                minWidth = BUTTON_MIN_WIDTH
                setOnAction {
                    if (editors.any { it.field.required && !it.isFilled() }) {
                        error.isVisible = true
                        error.isManaged = true
                    } else {
                        dialog.result =
                            DraftValues(
                                initial.operationName,
                                initial.fields + editors.associate { it.field.key to it.read() },
                            )
                        dialog.close()
                    }
                }
            }
        val buttons = HBox(GAP, cancelButton, saveButton).apply { alignment = Pos.CENTER }

        dialog.apply {
            this.title = title
            dialogPane.stylesheets.add(
                DraftEditorDialog::class.java.getResource(Files.AI_CHAT_STYLE_SHEET)!!.toExternalForm(),
            )
            dialogPane.prefWidth = CONTENT_WIDTH + GAP * 4
            dialogPane.content = VBox(GAP * 1.5, banner, grid, error, buttons).apply { padding = Insets(GAP) }
            setResultConverter { dialog.result }
            dialogPane.buttonTypes.add(ButtonType.CANCEL)
            dialogPane.lookupButton(ButtonType.CANCEL).apply {
                isVisible = false
                isManaged = false
            }
        }

        return dialog
    }

    /**
     * While the box of the `autoCalculate` field is on, the fields the operation suggests are filled in from what the
     * form holds and cannot be typed, and they follow every change of the other fields, as in the app's own forms.
     */
    private fun wireAutoCalculate(
        editors: List<Editor>,
        initial: DraftValues,
        suggest: (DraftValues) -> Map<String, Any?>,
    ) {
        val toggle = editors.firstOrNull { it.field.key == AUTO_CALCULATE_KEY } ?: return
        val byKey = editors.associateBy { it.field.key }
        var suggested = emptySet<String>()
        var refreshing = false

        fun refresh() {
            val current =
                DraftValues(
                    initial.operationName,
                    initial.fields + editors.associate { it.field.key to it.read() },
                )
            val values = if (toggle.read() == true) suggest(current) else emptyMap()

            refreshing = true
            try {
                suggested.filter { it !in values }.forEach { byKey[it]?.lock(false) }
                values.forEach { (key, value) ->
                    byKey[key]?.apply {
                        write(value)
                        lock(true)
                    }
                }
                suggested = values.keys
            } finally {
                refreshing = false
            }
        }

        editors.forEach { editor -> editor.onChange { if (!refreshing) refresh() } }
        refresh()
    }

    private fun buildEditor(
        field: DraftField,
        initial: Any?,
    ): Editor =
        when (field.type) {
            DraftFieldType.TEXT -> TextEditor(field, initial)
            DraftFieldType.DECIMAL -> DecimalEditor(field, initial)
            DraftFieldType.INTEGER -> IntegerEditor(field, initial)
            DraftFieldType.DATE -> DateEditor(field, initial)
            DraftFieldType.BOOLEAN -> BooleanEditor(field, initial)
            DraftFieldType.CHOICE ->
                ChoiceEditor(field, choiceSource.choices(field.choices ?: DraftChoices.CATEGORIES), initial)
        }

    companion object {
        private const val GAP = 10.0
        private const val CONTENT_WIDTH = 520.0
        private const val LABEL_WIDTH = 200.0
        private const val BUTTON_MIN_WIDTH = 80.0
        private const val AUTO_CALCULATE_KEY = "autoCalculate"
    }
}
