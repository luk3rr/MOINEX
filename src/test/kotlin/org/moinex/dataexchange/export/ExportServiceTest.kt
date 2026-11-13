/*
 * Filename: ExportServiceTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.moinex.dataexchange.FakeTransactionSpec
import org.moinex.dataexchange.FakeWalletSpec
import org.moinex.dataexchange.spec.EntityCatalog
import org.moinex.dataexchange.spec.ProgressSink
import org.moinex.dataexchange.tabular.ExportFormat
import org.moinex.dataexchange.tabular.TabularFiles
import java.math.BigDecimal
import java.nio.file.Files

class ExportServiceTest :
    BehaviorSpec({
        val wallets =
            FakeWalletSpec(
                mutableListOf(
                    mapOf("name" to "Main", "balance" to BigDecimal("100.00"), "archived" to false),
                    mapOf("name" to "Savings", "balance" to BigDecimal("5.5"), "archived" to true),
                ),
            )
        val transactions = FakeTransactionSpec(wallets, mutableListOf(FakeTransactionSpec.sample()))
        val service = ExportService(EntityCatalog(listOf(transactions, wallets)))

        Given("a catalog with two entities") {
            Then("counts are listed per entity") {
                service.counts() shouldBe mapOf("wallets" to 2L, "transactions" to 1L)
            }

            When("only wallets are selected") {
                val target = Files.createTempFile("export", ".xlsx")
                val reports = mutableListOf<Triple<String, Long, Long>>()
                val result =
                    runBlocking {
                        service.export(setOf("wallets"), ExportFormat.XLSX, target) { id, done, total ->
                            reports.add(Triple(id, done, total))
                        }
                    }

                Then("the file has the meta and only that sheet, and progress ends at the total") {
                    result.rows shouldBe mapOf("wallets" to 2L)
                    TabularFiles.open(target).use { reader ->
                        reader.sheetNames() shouldBe setOf("wallets")
                        reader.meta()!!["app"] shouldBe "moinex"
                        reader.meta()!!["layout_version"] shouldBe "1"
                        reader.meta()!!["entities"] shouldBe "wallets"
                    }
                    reports.last() shouldBe Triple("wallets", 2L, 2L)
                }
            }

            When("nothing is selected") {
                Then("it is refused") {
                    shouldThrow<IllegalArgumentException> {
                        runBlocking {
                            service.export(
                                emptySet(),
                                ExportFormat.CSV_ZIP,
                                Files.createTempFile("e", ".zip"),
                            )
                        }
                    }
                }
            }

            When("the export fails midway") {
                val failing =
                    object : org.moinex.dataexchange.spec.EntitySpec by wallets {
                        override fun export(): Sequence<Map<String, Any?>> = sequence { error("read failed") }
                    }
                val failService = ExportService(EntityCatalog(listOf(failing)))
                val target = Files.createTempFile("export", ".zip")

                Then("the partial file is removed") {
                    shouldThrow<IllegalStateException> {
                        runBlocking {
                            failService.export(
                                setOf("wallets"),
                                ExportFormat.CSV_ZIP,
                                target,
                                ProgressSink.NONE,
                            )
                        }
                    }
                    Files.exists(target) shouldBe false
                }
            }
        }
    })
