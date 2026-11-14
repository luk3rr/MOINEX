/*
 * Filename: DataExchangeDialogs.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.ui

import javafx.application.Platform
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.Window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.DialogCard
import org.moinex.common.ui.DialogSize
import org.moinex.common.ui.closeDialog
import org.moinex.common.util.WindowUtils
import org.moinex.config.AppDispatchers
import org.moinex.dataexchange.export.ExportService
import org.moinex.dataexchange.imports.ImportExecutor
import org.moinex.dataexchange.imports.ImportPlanner
import org.moinex.dataexchange.spec.EntityCatalog
import org.moinex.dataexchange.tabular.ExportFormat
import org.moinex.service.PreferencesService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.time.LocalDate

@Component
class DataExchangeDialogs(
    private val catalog: EntityCatalog,
    private val exportService: ExportService,
    private val importPlanner: ImportPlanner,
    private val importExecutor: ImportExecutor,
    private val preferences: PreferencesService,
) {
    private val logger = LoggerFactory.getLogger(DataExchangeDialogs::class.java)
    private val text get() = DataExchangeText(preferences)

    fun openExport(owner: Window? = null) {
        val model = ExportViewModel.from(catalog, exportService.counts())
        val scope = CoroutineScope(SupervisorJob() + AppDispatchers.IO)
        var job: Job? = null
        lateinit var stage: Presented
        lateinit var dialog: ExportDialog

        dialog =
            ExportDialog(
                model,
                text,
                onExport = {
                    val target = chooseTarget(stage, model.format)
                    if (target != null) {
                        dialog.showRunning()
                        job =
                            scope.launch {
                                try {
                                    val result =
                                        exportService.export(model.selected, model.format, target) { id, done, total ->
                                            Platform.runLater { dialog.progress(id, done, total) }
                                        }
                                    Platform.runLater {
                                        stage.close()
                                        WindowUtils.showSuccessDialog(
                                            text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_DONE_TITLE),
                                            text.t(
                                                TranslationKeys.DATA_EXCHANGE_EXPORT_DONE_MESSAGE,
                                                result.rows.values.sum(),
                                                result.rows.size,
                                                target,
                                            ),
                                        )
                                    }
                                } catch (_: CancellationException) {
                                    Platform.runLater { dialog.showForm() }
                                } catch (e: Exception) {
                                    logger.error("Export failed: {}", e.javaClass.simpleName)
                                    Platform.runLater {
                                        dialog.showForm()
                                        WindowUtils.showErrorDialog(
                                            text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_ERROR),
                                            e.message ?: e.javaClass.simpleName,
                                        )
                                    }
                                }
                            }
                    }
                },
                onCancel = {
                    job?.cancel()
                    if (job == null) stage.close()
                },
            )
        stage =
            present(text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_TITLE), dialog.root, owner, "data-export") {
                scope.coroutineContext[Job]?.cancel()
            }
    }

    fun openImport(owner: Window? = null) {
        val model =
            ImportViewModel(
                planner = { path, progress -> importPlanner.plan(path, progress) },
                executor = { plan, progress -> importExecutor.execute(plan, progress) },
            )
        val scope = CoroutineScope(SupervisorJob() + AppDispatchers.IO)
        var job: Job? = null
        lateinit var stage: Presented
        lateinit var dialog: ImportDialog

        val actions =
            object : ImportDialog.Actions {
                override fun chooseFile() {
                    val file = chooseSource(stage) ?: return
                    dialog.busy(text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_READING))
                    job =
                        scope.launch {
                            try {
                                model.choose(
                                    file,
                                ) { id, done, total -> Platform.runLater { dialog.progress(id, done, total) } }
                            } catch (_: CancellationException) {
                                Unit
                            } catch (e: Exception) {
                                logger.error("Import reading failed: {}", e.javaClass.simpleName)
                                Platform.runLater {
                                    WindowUtils.showErrorDialog(
                                        text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_ERROR_TITLE),
                                        e.message ?: e.javaClass.simpleName,
                                    )
                                }
                            }
                            Platform.runLater { dialog.busy(null) }
                        }
                }

                override fun confirm() {
                    dialog.busy(text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_RUNNING))
                    job =
                        scope.launch {
                            model.confirm { id, done, total -> Platform.runLater { dialog.progress(id, done, total) } }
                            Platform.runLater { dialog.busy(null) }
                        }
                }

                override fun cancel() {
                    job?.cancel()
                    Platform.runLater { dialog.busy(null) }
                }

                override fun close() {
                    stage.close()
                }
            }
        dialog = ImportDialog(model, text, actions)
        stage =
            present(text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_TITLE), dialog.root, owner, "data-import") {
                scope.coroutineContext[Job]?.cancel()
            }
    }

    fun saveTemplate(owner: Window? = null) {
        val chooser =
            FileChooser().apply {
                title = text.t(TranslationKeys.DATA_EXCHANGE_SETTINGS_TEMPLATE)
                initialFileName = "moinex-template.xlsx"
                extensionFilters.addAll(
                    FileChooser.ExtensionFilter("XLSX", "*.xlsx"),
                    FileChooser.ExtensionFilter("CSV (.zip)", "*.zip"),
                )
            }
        val chosen = chooser.showSaveDialog(owner)?.toPath() ?: return
        val format =
            if (chooser.selectedExtensionFilter?.extensions?.contains("*.zip") == true ||
                chosen.fileName
                    .toString()
                    .lowercase()
                    .endsWith(".zip")
            ) {
                ExportFormat.CSV_ZIP
            } else {
                ExportFormat.XLSX
            }
        val extension = if (format == ExportFormat.XLSX) ".xlsx" else ".zip"
        val target =
            if (chosen.fileName
                    .toString()
                    .lowercase()
                    .endsWith(extension)
            ) {
                chosen
            } else {
                chosen.resolveSibling(
                    chosen.fileName.toString() + extension,
                )
            }
        runCatching { exportService.exportTemplate(format, target) }
            .onSuccess {
                WindowUtils.showSuccessDialog(
                    text.t(TranslationKeys.DATA_EXCHANGE_TEMPLATE_TITLE),
                    text.t(TranslationKeys.DATA_EXCHANGE_TEMPLATE_DONE, target),
                )
            }.onFailure {
                WindowUtils.showErrorDialog(
                    text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_ERROR),
                    it.message ?: it.javaClass.simpleName,
                )
            }
    }

    /**
     * A dialog that is open, in the overlay of the main window or, when it is not ready, in a window of its own.
     */
    private class Presented(
        val close: () -> Unit,
        val window: () -> Window?,
    )

    private fun present(
        title: String,
        root: Parent,
        owner: Window?,
        sizeKey: String,
        onHidden: () -> Unit,
    ): Presented {
        val host = WindowUtils.dialogHost
        val commonStyleSheet =
            DataExchangeDialogs::class.java.getResource(Files.COMMON_STYLE_SHEET)?.toExternalForm()
                ?: throw IllegalStateException("Common stylesheet not found")

        if (host != null) {
            val card =
                DialogCard(
                    title,
                    root,
                    closeTooltip = preferences.translate(TranslationKeys.DIALOG_BUTTON_CLOSE),
                    width = maxOf(DialogSize.DEFAULT.width, root.prefWidth(-1.0) + DialogCard.SCROLLBAR_ALLOWANCE),
                    sizeKey = sizeKey,
                    sizeStore = WindowUtils.dialogSizes,
                )
            card.stylesheets.add(commonStyleSheet)
            host.present(card, listOf(Runnable(onHidden)))
            return Presented({ card.closeDialog() }, { card.scene?.window })
        }

        val stage =
            Stage().apply {
                initModality(Modality.APPLICATION_MODAL)
                owner?.let { initOwner(it) }
                this.title = title
                scene =
                    Scene(root).apply {
                        addEventHandler(KeyEvent.KEY_PRESSED) { event ->
                            if (event.code == KeyCode.ESCAPE) {
                                event.consume()
                                window?.hide()
                            }
                        }
                        stylesheets.add(commonStyleSheet)
                    }
                setOnHidden { onHidden() }
            }
        stage.show()
        return Presented({ stage.close() }, { stage })
    }

    private fun chooseTarget(
        stage: Presented,
        format: ExportFormat,
    ): Path? {
        val extension = if (format == ExportFormat.XLSX) "xlsx" else "zip"
        val chooser =
            FileChooser().apply {
                title = text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_CHOOSER)
                initialFileName = "moinex-export-${LocalDate.now()}.$extension"
                extensionFilters.add(FileChooser.ExtensionFilter(extension.uppercase(), "*.$extension"))
            }
        val chosen = chooser.showSaveDialog(stage.window())?.toPath() ?: return null
        return if (chosen.fileName
                .toString()
                .lowercase()
                .endsWith(".$extension")
        ) {
            chosen
        } else {
            chosen.resolveSibling(chosen.fileName.toString() + "." + extension)
        }
    }

    private fun chooseSource(stage: Presented): Path? =
        FileChooser()
            .apply {
                title = text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_CHOOSE_CHOOSER)
                extensionFilters.add(
                    FileChooser.ExtensionFilter(
                        text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_CHOOSE_FILTER),
                        "*.xlsx",
                        "*.zip",
                    ),
                )
            }.showOpenDialog(stage.window())
            ?.toPath()
}
