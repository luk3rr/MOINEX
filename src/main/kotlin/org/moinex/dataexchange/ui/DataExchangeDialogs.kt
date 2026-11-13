/*
 * Filename: DataExchangeDialogs.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.ui

import javafx.application.Platform
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
        lateinit var stage: Stage
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
        stage = newStage(text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_TITLE), dialog.root, owner)
        stage.setOnHidden { scope.coroutineContext[Job]?.cancel() }
        stage.show()
    }

    fun openImport(owner: Window? = null) {
        val model =
            ImportViewModel(
                planner = { path, progress -> importPlanner.plan(path, progress) },
                executor = { plan, progress -> importExecutor.execute(plan, progress) },
            )
        val scope = CoroutineScope(SupervisorJob() + AppDispatchers.IO)
        var job: Job? = null
        lateinit var stage: Stage
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
        stage = newStage(text.t(TranslationKeys.DATA_EXCHANGE_IMPORT_TITLE), dialog.root, owner)
        stage.setOnHidden { scope.coroutineContext[Job]?.cancel() }
        stage.show()
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

    private fun newStage(
        title: String,
        root: javafx.scene.Parent,
        owner: Window?,
    ): Stage =
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
                    stylesheets.add(
                        DataExchangeDialogs::class.java.getResource(Files.COMMON_STYLE_SHEET)?.toExternalForm()
                            ?: throw IllegalStateException("Common stylesheet not found"),
                    )
                }
        }

    private fun chooseTarget(
        stage: Stage,
        format: ExportFormat,
    ): Path? {
        val extension = if (format == ExportFormat.XLSX) "xlsx" else "zip"
        val chooser =
            FileChooser().apply {
                title = text.t(TranslationKeys.DATA_EXCHANGE_EXPORT_CHOOSER)
                initialFileName = "moinex-export-${LocalDate.now()}.$extension"
                extensionFilters.add(FileChooser.ExtensionFilter(extension.uppercase(), "*.$extension"))
            }
        val chosen = chooser.showSaveDialog(stage)?.toPath() ?: return null
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

    private fun chooseSource(stage: Stage): Path? =
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
            }.showOpenDialog(stage)
            ?.toPath()
}
