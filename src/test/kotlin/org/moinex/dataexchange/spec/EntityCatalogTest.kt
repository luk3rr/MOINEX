/*
 * Filename: EntityCatalogTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.spec

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.dataexchange.FakeTransactionSpec
import org.moinex.dataexchange.FakeWalletSpec

class EntityCatalogTest :
    BehaviorSpec({
        Given("specs registered out of dependency order") {
            val wallets = FakeWalletSpec()
            val catalog = EntityCatalog(listOf(FakeTransactionSpec(wallets), wallets))

            Then("the order follows dependsOn") {
                catalog.ordered().map { it.id } shouldBe listOf("wallets", "transactions")
                catalog.groups().keys shouldBe setOf("wallets")
                catalog.find("wallets") shouldBe wallets
            }
        }

        Given("specs that depend on each other") {
            fun cyclic(
                name: String,
                other: String,
            ) = object : EntitySpec {
                override val id = name
                override val group = "g"
                override val columns = emptyList<Column>()
                override val dependsOn = listOf(other)

                override fun count() = 0L

                override fun export() = emptySequence<Map<String, Any?>>()

                override fun naturalKey(row: ParsedRow) = ""

                override fun existingKeys() = emptyMap<String, Int>()

                override fun apply(
                    row: ParsedRow,
                    ctx: ApplyContext,
                ) = Unit
            }

            Then("a cycle fails") {
                shouldThrow<IllegalArgumentException> { EntityCatalog(listOf(cyclic("a", "b"), cyclic("b", "a"))) }
            }
        }
    })
