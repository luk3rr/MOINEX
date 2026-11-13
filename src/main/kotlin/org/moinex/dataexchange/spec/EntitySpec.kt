/*
 * Filename: EntitySpec.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.spec

interface EntitySpec {
    val id: String
    val group: String
    val columns: List<Column>
    val dependsOn: List<String> get() = emptyList()
    val refColumn: String? get() = null

    fun count(): Long

    fun export(): Sequence<Map<String, Any?>>

    fun naturalKey(row: ParsedRow): String

    fun existingKeys(): Map<String, Int>

    fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> = emptyList()

    fun applyOrder(rows: List<ParsedRow>): List<ParsedRow> = rows

    fun prepare(
        rowsByEntity: Map<String, List<ParsedRow>>,
        ctx: ApplyContext,
    ) = Unit

    fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    )
}

class ResolveContext(
    private val fileKeys: Map<String, Set<String>>,
    private val fileRefs: Map<String, Set<String>>,
    private val dbKeys: (String) -> Set<String>,
) {
    private val dbCache = mutableMapOf<String, Set<String>>()

    fun existsInFile(
        entityId: String,
        key: String,
    ): Boolean = fileKeys[entityId]?.contains(key) == true

    fun existsInDatabase(
        entityId: String,
        key: String,
    ): Boolean = dbCache.getOrPut(entityId) { dbKeys(entityId) }.contains(key)

    fun exists(
        entityId: String,
        key: String,
    ): Boolean = existsInFile(entityId, key) || existsInDatabase(entityId, key)

    fun hasRef(
        entityId: String,
        ref: String,
    ): Boolean = fileRefs[entityId]?.contains(ref) == true
}

class ApplyContext {
    private val created = mutableMapOf<Pair<String, String>, Any>()
    private val memos = mutableMapOf<String, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> memo(
        name: String,
        init: () -> T,
    ): T = memos.getOrPut(name, init) as T

    fun remember(
        entityId: String,
        key: String,
        value: Any,
    ) {
        created[entityId to key] = value
    }

    fun recall(
        entityId: String,
        key: String,
    ): Any? = created[entityId to key]
}
