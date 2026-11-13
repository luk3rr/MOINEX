/*
 * Filename: ExportDialog.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.RadioButton
import javafx.scene.control.ScrollPane
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.TitledValue
import org.moinex.dataexchange.tabular.ExportFormat

class ExportDialog(
    private val model: ExportViewModel,
    private val text: DataExchangeText,
    private val onExport: () -> Unit,
    private val onCancel: () -> Unit,
) {
    private val exportButton = Button(text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_BUTTON))
    private val summary = TitledValue()
    private val progressLabel = Label()
    private val progressBar = ProgressBar(0.0)
    private val form = VBox(SPACING)
    private val running = VBox(SPACING)
    private val groupBoxes = mutableMapOf<String, CheckBox>()
    private val entityBoxes = mutableMapOf<String, CheckBox>()
    private var updating = false

    val root: Parent =
        ScrollPane(
            VBox(SPACING, form, running).apply {
                padding = Insets(PADDING)
                style = "-fx-background-color: theme-bg-base;"
            },
        ).apply {
            style = "-fx-background: theme-bg-base; -fx-background-color: theme-bg-base;"
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            prefWidth = WIDTH
            prefHeight = HEIGHT
        }

    init {
        summary.title = text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_TITLE)
        buildForm()
        buildRunning()
        showForm()
        refresh()
    }

    fun showForm() {
        form.isVisible = true
        form.isManaged = true
        running.isVisible = false
        running.isManaged = false
    }

    fun showRunning() {
        form.isVisible = false
        form.isManaged = false
        running.isVisible = true
        running.isManaged = true
    }

    fun progress(
        entityId: String,
        done: Long,
        total: Long,
    ) {
        progressLabel.text = "${text.entity(entityId)} ($done / $total)"
        progressBar.progress = if (total > 0) done.toDouble() / total else ProgressBar.INDETERMINATE_PROGRESS
    }

    private fun buildForm() {
        val formatGroup = ToggleGroup()
        val xlsx =
            RadioButton(text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_FORMAT_XLSX)).apply {
                toggleGroup = formatGroup
                isSelected =
                    true
            }
        val csv =
            RadioButton(
                text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_FORMAT_CSV),
            ).apply { toggleGroup = formatGroup }
        xlsx.setOnAction { model.format = ExportFormat.XLSX }
        csv.setOnAction { model.format = ExportFormat.CSV_ZIP }

        val title =
            Label(text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_TITLE)).apply {
                style =
                    "-fx-font-weight: bold; -fx-font-size: 15px;"
            }
        form.children.addAll(
            title,
            HBox(
                SPACING,
                Label(text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_FORMAT) + ":").apply {
                    style =
                        "-fx-font-weight: bold;"
                },
                xlsx,
                csv,
            ).apply { alignment = Pos.CENTER_LEFT },
        )

        model.groups.forEach { group -> form.children.add(groupBox(group)) }

        val notice =
            Label(text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_NOTICE)).apply {
                isWrapText = true
                style = "-fx-text-fill: theme-text-secondary;"
            }
        val cancel = Button(text.t("dialog.button.cancel")).apply { setOnAction { onCancel() } }
        exportButton.setOnAction { onExport() }
        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }
        form.children.addAll(
            notice,
            HBox(SPACING, summary, spacer, cancel, exportButton).apply {
                alignment =
                    Pos.CENTER_LEFT
            },
        )
    }

    private fun groupBox(group: ExportGroup): VBox {
        val header =
            CheckBox("${text.group(group.id)}  (${group.rows})").apply {
                style = "-fx-font-weight: bold;"
                isAllowIndeterminate = false
                setOnAction {
                    if (!updating) {
                        model.setGroup(group.id, isSelected)
                        refresh()
                    }
                }
            }
        groupBoxes[group.id] = header
        val body = VBox(SMALL)
        group.entities.forEach { entity ->
            val box =
                CheckBox("${text.entity(entity.id)}  (${entity.count})").apply {
                    setOnAction {
                        if (!updating) {
                            model.setEntity(entity.id, isSelected)
                            refresh()
                        }
                    }
                }
            entityBoxes[entity.id] = box
            body.children.add(box)
        }
        body.padding = Insets(0.0, 0.0, 0.0, INDENT)
        return VBox(SMALL, header, body).apply {
            styleClass.add("card")
            padding = Insets(SPACING)
        }
    }

    private fun buildRunning() {
        running.alignment = Pos.CENTER_LEFT
        progressBar.maxWidth = Double.MAX_VALUE
        val cancel = Button(text.t("dialog.button.cancel")).apply { setOnAction { onCancel() } }
        running.children.addAll(
            Label(text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_RUNNING)).apply { style = "-fx-font-weight: bold;" },
            progressLabel,
            progressBar,
            HBox(cancel).apply { alignment = Pos.CENTER_RIGHT },
        )
    }

    private fun refresh() {
        updating = true
        model.groups.forEach { group ->
            val state = model.groupState(group.id)
            groupBoxes.getValue(group.id).isSelected = state != SelectionState.NONE
            groupBoxes.getValue(group.id).isIndeterminate = state == SelectionState.SOME
            group.entities.forEach { entityBoxes.getValue(it.id).isSelected = model.isSelected(it.id) }
        }
        val s = model.summary()
        summary.title = text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_SELECTED, s.groups, s.rows).substringBefore(":")
        summary.value = text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_SELECTED, s.groups, s.rows).substringAfter(": ")
        exportButton.isDisable = !model.canExport
        updating = false
    }

    private companion object {
        const val SPACING = 10.0
        const val SMALL = 4.0
        const val PADDING = 16.0
        const val INDENT = 20.0
        const val WIDTH = 640.0
        const val HEIGHT = 640.0
    }
}
