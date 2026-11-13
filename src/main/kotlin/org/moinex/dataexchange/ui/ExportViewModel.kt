/*
 * Filename: ExportViewModel.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.ui

import org.moinex.dataexchange.spec.EntityCatalog
import org.moinex.dataexchange.tabular.ExportFormat

data class ExportEntity(
    val id: String,
    val count: Long,
)

data class ExportGroup(
    val id: String,
    val entities: List<ExportEntity>,
) {
    val rows: Long get() = entities.sumOf { it.count }
}

enum class SelectionState { NONE, SOME, ALL }

data class ExportSummary(
    val groups: Int,
    val rows: Long,
)

class ExportViewModel(
    val groups: List<ExportGroup>,
) {
    private val selectedIds = linkedSetOf<String>()

    var format: ExportFormat = ExportFormat.XLSX

    init {
        groups.flatMap { it.entities }.filter { it.count > 0 }.forEach { selectedIds.add(it.id) }
    }

    val selected: Set<String> get() = selectedIds.toSet()

    val canExport: Boolean get() = selectedIds.isNotEmpty()

    fun isSelected(entityId: String): Boolean = entityId in selectedIds

    fun setEntity(
        entityId: String,
        on: Boolean,
    ) {
        if (on) selectedIds.add(entityId) else selectedIds.remove(entityId)
    }

    fun setGroup(
        groupId: String,
        on: Boolean,
    ) {
        groups.firstOrNull { it.id == groupId }?.entities?.forEach { setEntity(it.id, on) }
    }

    fun groupState(groupId: String): SelectionState {
        val entities = groups.firstOrNull { it.id == groupId }?.entities.orEmpty()
        val count = entities.count { it.id in selectedIds }
        return when {
            count == 0 -> SelectionState.NONE
            count == entities.size -> SelectionState.ALL
            else -> SelectionState.SOME
        }
    }

    fun summary(): ExportSummary {
        val chosen = groups.filter { group -> group.entities.any { it.id in selectedIds } }
        val rows = groups.flatMap { it.entities }.filter { it.id in selectedIds }.sumOf { it.count }
        return ExportSummary(chosen.size, rows)
    }

    companion object {
        fun from(
            catalog: EntityCatalog,
            counts: Map<String, Long>,
        ): ExportViewModel =
            ExportViewModel(
                catalog.groups().map { (group, specs) ->
                    ExportGroup(group, specs.map { ExportEntity(it.id, counts[it.id] ?: 0L) })
                },
            )
    }
}
