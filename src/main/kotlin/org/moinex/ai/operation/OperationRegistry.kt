/*
 * Filename: OperationRegistry.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation

import org.springframework.stereotype.Component

/**
 * All operations the assistant can propose. Names are unique, and a name that suggests deleting is refused so that
 * the assistant can never be given a way to remove data.
 */
@Component
class OperationRegistry(
    operations: List<AppOperation<*>>,
) {
    companion object {
        private val FORBIDDEN_PREFIXES = listOf("delete", "remove", "drop", "purge")
    }

    private val byName: Map<String, AppOperation<*>>

    init {
        operations.forEach { operation ->
            require(FORBIDDEN_PREFIXES.none { operation.name.startsWith(it) }) {
                "Operation '${operation.name}' looks like a deletion, which the assistant must not have"
            }
        }
        val duplicated = operations.groupBy { it.name }.filterValues { it.size > 1 }.keys
        require(duplicated.isEmpty()) { "Duplicated operation names: ${duplicated.joinToString()}" }

        byName = operations.associateBy { it.name }
    }

    fun all(): List<AppOperation<*>> = byName.values.toList()

    fun byDomain(domain: OperationDomain): List<AppOperation<*>> = byName.values.filter { it.domain == domain }

    fun find(name: String): AppOperation<*>? = byName[name]

    @Suppress("UNCHECKED_CAST")
    fun <P : Any> get(name: String): AppOperation<P> =
        (byName[name] ?: error("Unknown operation: $name")) as AppOperation<P>
}
