/*
 * Filename: ImportDialog.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.ui

import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.TitledValue
import org.moinex.dataexchange.imports.EntityPlan
import org.moinex.dataexchange.spec.Problem
import org.moinex.dataexchange.spec.Severity

class ImportDialog(
    private val model: ImportViewModel,
    private val text: DataExchangeText,
    private val actions: Actions,
) {
    interface Actions {
        fun chooseFile()

        fun confirm()

        fun cancel()

        fun close()
    }

    private val steps = HBox(SPACING)
    private val content = StackPane()
    private val buttons = HBox(SPACING).apply { alignment = Pos.CENTER_RIGHT }
    private var busyText: String? = null
    private val progressLabel = Label()

    val root: Parent =
        VBox(SPACING, steps, content, buttons).apply {
            padding = Insets(PADDING)
            prefWidth = WIDTH
            prefHeight = HEIGHT
            VBox.setVgrow(content, Priority.ALWAYS)
        }

    init {
        render()
    }

    fun busy(message: String?) {
        busyText = message
        render()
    }

    fun progress(
        entityId: String,
        done: Long,
        total: Long,
    ) {
        progressLabel.text = "${text.entity(entityId)} ($done${if (total >= 0) " / $total" else ""})"
    }

    fun render() {
        renderSteps()
        content.children.clear()
        buttons.children.clear()
        val busy = busyText
        if (busy != null) {
            renderBusy(busy)
            return
        }
        when (model.step) {
            ImportStep.FILE -> renderFile()
            ImportStep.PREVIEW -> renderPreview()
            ImportStep.RESULT -> renderResult()
        }
    }

    private fun renderSteps() {
        steps.children.setAll(
            stepLabel(TranslationKeys.DATA_EXCHANGE_IMPORT_STEP_FILE, ImportStep.FILE),
            Label("›"),
            stepLabel(TranslationKeys.DATA_EXCHANGE_IMPORT_STEP_PREVIEW, ImportStep.PREVIEW),
            Label("›"),
            stepLabel(TranslationKeys.DATA_EXCHANGE_IMPORT_STEP_RESULT, ImportStep.RESULT),
        )
    }

    private fun stepLabel(
        key: String,
        step: ImportStep,
    ) = Label(text.t(key)).apply {
        style = if (model.step == step) "-fx-font-weight: bold;" else "-fx-text-fill: theme-text-secondary;"
    }

    private fun renderBusy(message: String) {
        val bar = ProgressBar().apply { maxWidth = Double.MAX_VALUE }
        content.children.add(
            VBox(SPACING, Label(message).apply { style = "-fx-font-weight: bold;" }, progressLabel, bar),
        )
        if (model.step != ImportStep.PREVIEW || model.plan == null) {
            buttons.children.add(button("dialog.button.cancel") { actions.cancel() })
        }
    }

    private fun renderFile() {
        val box = VBox(SPACING)
        box.alignment = Pos.CENTER
        box.padding = Insets(PADDING)
        box.style =
            "-fx-border-color: theme-border; -fx-border-style: dashed; -fx-border-width: 2; -fx-border-radius: 8;"
        box.children.addAll(
            Label(text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_CHOOSE_PROMPT)),
            button(TranslationKeys.DATA_EXCHANGE_IMPORT_CHOOSE_BUTTON) { actions.chooseFile() },
        )
        val column = VBox(SPACING, box, hint())
        model.plan?.fileProblems?.forEach { column.children.add(banner(text.problem(it), NEGATIVE)) }
        model.failure?.let {
            column.children.add(
                banner(text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_FAILED, it), NEGATIVE),
            )
        }
        content.children.add(column)
        buttons.children.add(button("dialog.button.cancel") { actions.close() })
    }

    private fun renderPreview() {
        val plan = model.plan ?: return
        val file =
            TitledValue().apply {
                title = text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_FILE)
                value =
                    model.file
                        ?.fileName
                        ?.toString()
                        .orEmpty()
            }
        val table = entityTable(plan.entities)
        val detail = detailPane()
        val split = HBox(SPACING, table, detail)
        HBox.setHgrow(detail, Priority.ALWAYS)
        VBox.setVgrow(split, Priority.ALWAYS)
        val column = VBox(SPACING, file, split)
        plan.fileProblems.forEach {
            column.children.add(
                banner(
                    text.problem(it),
                    if (it.severity ==
                        Severity.ERROR
                    ) {
                        NEGATIVE
                    } else {
                        WARNING
                    },
                ),
            )
        }
        if (plan.hasErrors) {
            column.children.add(
                banner(text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_ERRORS_BANNER), NEGATIVE),
            )
        }
        if (!plan.hasErrors &&
            plan.totalToAdd == 0
        ) {
            column.children.add(banner(text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_NOTHING), WARNING))
        }
        model.failure?.let {
            column.children.add(
                banner(text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_FAILED, it), NEGATIVE),
            )
        }
        content.children.add(column)

        buttons.children.addAll(
            button(TranslationKeys.DATA_EXCHANGE_IMPORT_BACK) {
                model.back()
                render()
            },
            Button(text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_BUTTON, plan.totalToAdd)).apply {
                isDisable = !model.canImport
                setOnAction { actions.confirm() }
            },
        )
    }

    private fun entityTable(entities: List<EntityPlan>): TableView<EntityPlan> =
        TableView(FXCollections.observableArrayList(entities)).apply {
            prefWidth = TABLE_WIDTH
            minWidth = TABLE_WIDTH
            columns.addAll(
                column(TranslationKeys.DATA_EXCHANGE_IMPORT_TABLE_ENTITY, { text.entity(it.id) }, ENTITY_COLUMN_WIDTH),
                column(TranslationKeys.DATA_EXCHANGE_IMPORT_TABLE_SHEET, { it.id }, SHEET_COLUMN_WIDTH),
                column(TranslationKeys.DATA_EXCHANGE_IMPORT_TABLE_NEW, { it.toAdd.toString() }, NUMBER_COLUMN_WIDTH),
                column(
                    TranslationKeys.DATA_EXCHANGE_IMPORT_TABLE_DUPLICATES,
                    { it.duplicates.toString() },
                    DUPLICATES_COLUMN_WIDTH,
                ),
                column(
                    TranslationKeys.DATA_EXCHANGE_IMPORT_TABLE_STATE,
                    {
                        if (it.errors >
                            0
                        ) {
                            text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_TABLE_ERRORS, it.errors)
                        } else {
                            text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_TABLE_OK)
                        }
                    },
                ),
            )
            selectionModel.select(entities.firstOrNull { it.id == model.selectedEntity })
            selectionModel.selectedItemProperty().addListener { _, _, selected ->
                if (selected != null) {
                    model.selectedEntity = selected.id
                    model.tab = if (selected.problems.isNotEmpty()) PreviewTab.PROBLEMS else PreviewTab.NEW
                    render()
                }
            }
        }

    private fun detailPane(): VBox {
        val selected = model.selectedPlan ?: return VBox()
        val title =
            TitledValue().apply {
                title = "${text.entity(selected.id)} (${selected.id})"
                value =
                    text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_TABLE_NEW) + " " + selected.toAdd + " · " +
                    selected.total
            }
        val group = ToggleGroup()
        val tabs =
            HBox(
                SMALL,
                tab(
                    group,
                    PreviewTab.PROBLEMS,
                    text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_TAB_PROBLEMS, selected.problems.size),
                ),
                tab(group, PreviewTab.NEW, text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_TAB_NEW, selected.newRows.size)),
                tab(
                    group,
                    PreviewTab.DUPLICATES,
                    text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_TAB_DUPLICATES, selected.duplicateRows.size),
                ),
            )
        val body = if (model.tab == PreviewTab.PROBLEMS) problemTable(selected.problems) else rowTable(selected)
        VBox.setVgrow(body, Priority.ALWAYS)
        return VBox(SPACING, title, tabs, body)
    }

    private fun tab(
        group: ToggleGroup,
        tab: PreviewTab,
        label: String,
    ) = ToggleButton(label).apply {
        toggleGroup = group
        isSelected = model.tab == tab
        setOnAction {
            model.tab = tab
            render()
        }
    }

    private fun problemTable(problems: List<Problem>): TableView<Problem> =
        TableView(FXCollections.observableArrayList(problems)).apply {
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN
            prefHeight = BODY_HEIGHT
            columns.addAll(
                column(TranslationKeys.DATA_EXCHANGE_IMPORT_COLUMN_ROW, { it.row.toString() }, NUMBER_COLUMN_WIDTH),
                column(TranslationKeys.DATA_EXCHANGE_IMPORT_COLUMN_COLUMN, { it.column.orEmpty() }, SHEET_COLUMN_WIDTH),
                column(TranslationKeys.DATA_EXCHANGE_IMPORT_COLUMN_PROBLEM, { text.problem(it) }, PROBLEM_WIDTH),
            )
        }

    private fun rowTable(plan: EntityPlan): TableView<Pair<Int, String>> {
        val rows = if (model.tab == PreviewTab.NEW) plan.newRows else plan.duplicateRows
        return TableView(
            FXCollections.observableArrayList(
                rows.take(MAX_LISTED).map {
                    it to plan.labels[it].orEmpty()
                },
            ),
        ).apply {
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN
            prefHeight = BODY_HEIGHT
            columns.addAll(
                column(TranslationKeys.DATA_EXCHANGE_IMPORT_COLUMN_ROW, { it.first.toString() }, NUMBER_COLUMN_WIDTH),
                column(TranslationKeys.DATA_EXCHANGE_IMPORT_COLUMN_RECORD, { it.second }, PROBLEM_WIDTH),
            )
        }
    }

    private fun renderResult() {
        val result = model.result ?: return
        val table =
            TableView(FXCollections.observableArrayList(result.added.keys.toList())).apply {
                columns.addAll(
                    column(TranslationKeys.DATA_EXCHANGE_IMPORT_TABLE_ENTITY, { text.entity(it) }),
                    column(TranslationKeys.DATA_EXCHANGE_IMPORT_DONE_ADDED, { result.added.getValue(it).toString() }),
                    column(
                        TranslationKeys.DATA_EXCHANGE_IMPORT_DONE_IGNORED,
                        { result.ignored[it]?.toString() ?: "0" },
                    ),
                )
            }
        content.children.add(
            VBox(
                SPACING,
                banner(
                    text.t(
                        TranslationKeys.DATA_EXCHANGE_IMPORT_DONE_BANNER,
                        result.added.values.sum(),
                        result.ignored.values.sum(),
                    ),
                    POSITIVE,
                ),
                table,
            ),
        )
        buttons.children.add(button(TranslationKeys.DATA_EXCHANGE_IMPORT_CLOSE) { actions.close() })
    }

    private fun hint() =
        Label(text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_HINT)).apply {
            isWrapText = true
            style = "-fx-text-fill: theme-text-secondary;"
        }

    private fun banner(
        message: String,
        color: String,
    ) = Label(message).apply {
        isWrapText = true
        maxWidth = Double.MAX_VALUE
        padding = Insets(SMALL, SPACING, SMALL, SPACING)
        style = "-fx-text-fill: $color; -fx-border-color: $color; -fx-border-radius: 6; -fx-background-radius: 6;"
    }

    private fun button(
        key: String,
        action: () -> Unit,
    ) = Button(text.t(key)).apply { setOnAction { action() } }

    private fun <T> column(
        key: String,
        value: (T) -> String,
        width: Double = COLUMN_WIDTH,
    ) = TableColumn<T, String>(text.t(key)).apply {
        setCellValueFactory { javafx.beans.property.SimpleStringProperty(value(it.value)) }
        prefWidth = width
    }

    private companion object {
        const val SPACING = 10.0
        const val SMALL = 4.0
        const val PADDING = 16.0
        const val WIDTH = 1200.0
        const val HEIGHT = 760.0
        const val TABLE_WIDTH = 580.0
        const val ENTITY_COLUMN_WIDTH = 140.0
        const val SHEET_COLUMN_WIDTH = 170.0
        const val NUMBER_COLUMN_WIDTH = 60.0
        const val DUPLICATES_COLUMN_WIDTH = 85.0
        const val COLUMN_WIDTH = 90.0
        const val PROBLEM_WIDTH = 400.0
        const val BODY_HEIGHT = 260.0
        const val MAX_LISTED = 2000
        const val NEGATIVE = "theme-negative"
        const val POSITIVE = "theme-positive"
        const val WARNING = "theme-warning"
    }
}
