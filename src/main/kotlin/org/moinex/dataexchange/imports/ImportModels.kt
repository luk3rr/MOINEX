/*
 * Filename: ImportModels.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.imports

import org.moinex.dataexchange.spec.ParsedRow
import org.moinex.dataexchange.spec.Problem
import org.moinex.dataexchange.spec.Severity

data class EntityPlan(
    val id: String,
    val total: Int,
    val toAdd: Int,
    val duplicates: Int,
    val problems: List<Problem>,
    val rows: List<ParsedRow>,
    val existingSnapshot: Int,
    val newRows: List<Int> = emptyList(),
    val duplicateRows: List<Int> = emptyList(),
    val labels: Map<Int, String> = emptyMap(),
) {
    val errors: Int get() = problems.count { it.severity == Severity.ERROR }
    val warnings: Int get() = problems.count { it.severity == Severity.WARNING }
}

data class ImportPlan(
    val layoutVersion: Int,
    val fileProblems: List<Problem>,
    val entities: List<EntityPlan>,
) {
    val hasErrors: Boolean
        get() =
            fileProblems.any { it.severity == Severity.ERROR } || entities.any { it.errors > 0 }

    val totalToAdd: Int get() = entities.sumOf { it.toAdd }
}

data class ImportResult(
    val added: Map<String, Int>,
    val ignored: Map<String, Int>,
)

class ImportExecutionException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
