/*
 * Filename: ImportViewModel.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.ui

import org.moinex.dataexchange.imports.EntityPlan
import org.moinex.dataexchange.imports.ImportPlan
import org.moinex.dataexchange.imports.ImportResult
import org.moinex.dataexchange.spec.ProgressSink
import java.nio.file.Path

enum class ImportStep { FILE, PREVIEW, RESULT }

enum class PreviewTab { PROBLEMS, NEW, DUPLICATES }

class ImportViewModel(
    private val planner: suspend (Path, ProgressSink) -> ImportPlan,
    private val executor: (ImportPlan, ProgressSink) -> ImportResult,
) {
    var step: ImportStep = ImportStep.FILE
        private set

    var file: Path? = null
        private set

    var plan: ImportPlan? = null
        private set

    var result: ImportResult? = null
        private set

    var failure: String? = null
        private set

    var selectedEntity: String? = null

    var tab: PreviewTab = PreviewTab.PROBLEMS

    val canImport: Boolean
        get() = step == ImportStep.PREVIEW && plan?.let { !it.hasErrors && it.totalToAdd > 0 } == true

    val selectedPlan: EntityPlan? get() = plan?.entities?.firstOrNull { it.id == selectedEntity }

    suspend fun choose(
        path: Path,
        progress: ProgressSink = ProgressSink.NONE,
    ) {
        failure = null
        file = path
        val planned = planner(path, progress)
        plan = planned
        if (planned.fileProblems.any { it.severity == org.moinex.dataexchange.spec.Severity.ERROR }) {
            step = ImportStep.FILE
            return
        }
        selectedEntity = planned.entities.firstOrNull { it.errors > 0 }?.id ?: planned.entities.firstOrNull()?.id
        tab = if ((selectedPlan?.problems?.size ?: 0) > 0) PreviewTab.PROBLEMS else PreviewTab.NEW
        step = ImportStep.PREVIEW
    }

    fun back() {
        plan = null
        failure = null
        step = ImportStep.FILE
    }

    fun confirm(progress: ProgressSink = ProgressSink.NONE): Boolean {
        val current = plan ?: return false
        if (!canImport) return false
        return try {
            result = executor(current, progress)
            step = ImportStep.RESULT
            true
        } catch (e: Exception) {
            failure = e.message ?: e.javaClass.simpleName
            false
        }
    }
}
