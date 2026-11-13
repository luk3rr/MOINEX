/*
 * Filename: EntityCatalog.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.spec

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class EntityCatalog(
    specs: List<EntitySpec>,
) {
    @Autowired
    constructor(provider: ObjectProvider<EntitySpec>) : this(provider.orderedStream().toList())

    private val byId: Map<String, EntitySpec> = specs.associateBy { it.id }
    private val ordered: List<EntitySpec>

    init {
        require(byId.size == specs.size) { "Duplicate entity spec ids" }
        ordered = topologicalOrder(specs)
    }

    fun ordered(): List<EntitySpec> = ordered

    fun find(id: String): EntitySpec? = byId[id]

    fun groups(): Map<String, List<EntitySpec>> = ordered.groupBy { it.group }

    private fun topologicalOrder(specs: List<EntitySpec>): List<EntitySpec> {
        val result = mutableListOf<EntitySpec>()
        val state = mutableMapOf<String, Int>()

        fun visit(spec: EntitySpec) {
            when (state[spec.id]) {
                2 -> return
                1 -> throw IllegalArgumentException("Cyclic dependency at ${spec.id}")
            }
            state[spec.id] = 1
            spec.dependsOn.mapNotNull { byId[it] }.forEach { visit(it) }
            state[spec.id] = 2
            result.add(spec)
        }

        specs.forEach { visit(it) }
        return result
    }
}
