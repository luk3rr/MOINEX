/*
 * Filename: ImportPlanner.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.imports

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.moinex.dataexchange.codec.CellCodec
import org.moinex.dataexchange.spec.DataExchangeException
import org.moinex.dataexchange.spec.EntityCatalog
import org.moinex.dataexchange.spec.EntitySpec
import org.moinex.dataexchange.spec.ParsedRow
import org.moinex.dataexchange.spec.Problem
import org.moinex.dataexchange.spec.ProblemCodes
import org.moinex.dataexchange.spec.ProgressSink
import org.moinex.dataexchange.spec.ResolveContext
import org.moinex.dataexchange.spec.Severity
import org.moinex.dataexchange.tabular.FileGuard
import org.moinex.dataexchange.tabular.TabularFiles
import org.moinex.dataexchange.tabular.TabularLayout
import org.moinex.dataexchange.tabular.TabularReader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class ImportPlanner(
    private val catalog: EntityCatalog,
) {
    private val logger = LoggerFactory.getLogger(ImportPlanner::class.java)

    suspend fun plan(
        file: Path,
        progress: ProgressSink = ProgressSink.NONE,
        guard: FileGuard = FileGuard(),
    ): ImportPlan {
        val reader =
            try {
                TabularFiles.open(file, guard)
            } catch (e: DataExchangeException) {
                return ImportPlan(0, listOf(e.problem), emptyList())
            }
        return reader.use { planFrom(it, progress) }
    }

    private suspend fun planFrom(
        reader: TabularReader,
        progress: ProgressSink,
    ): ImportPlan {
        val meta = reader.meta()
        if (meta == null || meta[TabularLayout.META_APP] != TabularLayout.META_APP_VALUE) {
            return ImportPlan(0, listOf(fileProblem(ProblemCodes.NOT_MOINEX_FILE, "_meta")), emptyList())
        }
        val version = meta[TabularLayout.META_LAYOUT_VERSION]?.toIntOrNull()
        if (version != TabularLayout.CURRENT_LAYOUT_VERSION) {
            return ImportPlan(
                version ?: 0,
                listOf(fileProblem(ProblemCodes.UNKNOWN_LAYOUT, version.toString())),
                emptyList(),
            )
        }

        val fileProblems = mutableListOf<Problem>()
        val inFile = reader.sheetNames()
        val declared =
            meta[TabularLayout.META_ENTITIES]
                ?.split(
                    ",",
                )?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
        declared
            .filter {
                it !in inFile
            }.forEach { fileProblems.add(entityProblem(it, ProblemCodes.MISSING_SHEET_OR_COLUMN, "sheet")) }
        (inFile - catalog.ordered().map { it.id }.toSet()).forEach {
            fileProblems.add(Problem(it, 0, null, ProblemCodes.UNKNOWN_COLUMN, Severity.WARNING, "unknown entity"))
        }

        val specs = catalog.ordered().filter { it.id in inFile }
        val fileKeys = mutableMapOf<String, MutableSet<String>>()
        val fileRefs = mutableMapOf<String, MutableSet<String>>()
        val dbKeysCache = mutableMapOf<String, Map<String, Int>>()
        val dbKeys: (String) -> Map<String, Int> = { id ->
            dbKeysCache.getOrPut(id) { catalog.find(id)?.existingKeys().orEmpty() }
        }
        val context = ResolveContext(fileKeys, fileRefs) { id -> dbKeys(id).keys }

        val plans = mutableListOf<EntityPlan>()
        for (spec in specs) {
            currentCoroutineContext().ensureActive()
            plans.add(planEntity(spec, reader, context, fileKeys, fileRefs, dbKeys(spec.id), progress))
        }
        logger.info("Import plan: {} entities, {} new rows", plans.size, plans.sumOf { it.toAdd })
        return ImportPlan(version, fileProblems, plans)
    }

    private suspend fun planEntity(
        spec: EntitySpec,
        reader: TabularReader,
        context: ResolveContext,
        fileKeys: MutableMap<String, MutableSet<String>>,
        fileRefs: MutableMap<String, MutableSet<String>>,
        existing: Map<String, Int>,
        progress: ProgressSink,
    ): EntityPlan {
        val problems = mutableListOf<Problem>()
        val header = reader.header(spec.id)
        spec.columns.filter { it.required && it.name !in header }.forEach {
            problems.add(Problem(spec.id, 1, it.name, ProblemCodes.MISSING_SHEET_OR_COLUMN, Severity.ERROR))
        }
        (header - spec.columns.map { it.name }.toSet()).forEach {
            problems.add(Problem(spec.id, 1, it, ProblemCodes.UNKNOWN_COLUMN, Severity.WARNING))
        }
        if (problems.any { it.severity == Severity.ERROR }) {
            return EntityPlan(spec.id, 0, 0, 0, problems, emptyList(), existing.values.sum())
        }

        val keys = fileKeys.getOrPut(spec.id) { mutableSetOf() }
        val refs = fileRefs.getOrPut(spec.id) { mutableSetOf() }
        val seen = mutableMapOf<String, Int>()
        val toAdd = mutableListOf<ParsedRow>()
        val duplicateRows = mutableListOf<Int>()
        val labels = mutableMapOf<Int, String>()
        var total = 0
        var duplicates = 0
        for (raw in reader.rows(spec.id, spec.columns)) {
            currentCoroutineContext().ensureActive()
            total++
            val rowProblems = mutableListOf<Problem>()
            val values = mutableMapOf<String, Any?>()
            for (column in spec.columns) {
                if (column.name in raw.formulaColumns) {
                    rowProblems.add(
                        Problem(spec.id, raw.number, column.name, ProblemCodes.FORMULA_CELL, Severity.ERROR),
                    )
                    continue
                }
                when (val decoded = CellCodec.decode(raw.cells[column.name], column)) {
                    is CellCodec.Decoded.Invalid ->
                        rowProblems.add(
                            Problem(
                                spec.id,
                                raw.number,
                                column.name,
                                ProblemCodes.INVALID_VALUE,
                                Severity.ERROR,
                                decoded.reason,
                            ),
                        )
                    is CellCodec.Decoded.Value -> {
                        if (decoded.value == null && column.required) {
                            rowProblems.add(
                                Problem(
                                    spec.id,
                                    raw.number,
                                    column.name,
                                    ProblemCodes.INVALID_VALUE,
                                    Severity.ERROR,
                                    "required",
                                ),
                            )
                        }
                        values[column.name] = decoded.value
                    }
                }
            }
            val parsed = ParsedRow(raw.number, values)
            if (rowProblems.none { it.severity == Severity.ERROR }) {
                rowProblems.addAll(spec.validate(parsed, context))
            }
            problems.addAll(rowProblems)
            if (rowProblems.any { it.severity == Severity.ERROR }) continue

            val key = spec.naturalKey(parsed)
            keys.add(key)
            spec.refColumn?.let { column -> parsed.text(column)?.let { refs.add(it) } }
            val occurrence = (seen[key] ?: 0) + 1
            seen[key] = occurrence
            labels[raw.number] = key.replace(KEY_SEPARATOR, " · ")
            if (occurrence <= (existing[key] ?: 0)) {
                duplicates++
                duplicateRows.add(raw.number)
            } else {
                toAdd.add(parsed)
            }
            if (total % PROGRESS_STEP == 0) progress.report(spec.id, total.toLong(), -1)
        }
        progress.report(spec.id, total.toLong(), total.toLong())
        return EntityPlan(
            spec.id,
            total,
            toAdd.size,
            duplicates,
            problems,
            toAdd,
            existing.values.sum(),
            toAdd.map { it.number },
            duplicateRows,
            labels,
        )
    }

    private fun fileProblem(
        code: String,
        detail: String,
    ) = Problem("file", 0, null, code, Severity.ERROR, detail)

    private fun entityProblem(
        entity: String,
        code: String,
        detail: String,
    ) = Problem(entity, 0, null, code, Severity.ERROR, detail)

    private companion object {
        const val PROGRESS_STEP = 200
        const val KEY_SEPARATOR = "\u001F"
    }
}
