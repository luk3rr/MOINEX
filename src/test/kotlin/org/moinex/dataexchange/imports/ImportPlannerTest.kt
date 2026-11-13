/*
 * Filename: ImportPlannerTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.imports

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.moinex.dataexchange.FakeTransactionSpec
import org.moinex.dataexchange.FakeWalletSpec
import org.moinex.dataexchange.spec.EntityCatalog
import org.moinex.dataexchange.spec.ProblemCodes
import org.moinex.dataexchange.tabular.ExportFormat
import java.math.BigDecimal
import java.nio.file.Files

class ImportPlannerTest :
    BehaviorSpec({
        fun wallet(
            name: String,
            balance: String = "10.00",
        ) = mapOf("name" to name, "balance" to BigDecimal(balance), "archived" to false)

        fun env(
            existingWallets: List<String> = emptyList(),
        ): Triple<FakeWalletSpec, FakeTransactionSpec, ImportPlanner> {
            val wallets = FakeWalletSpec(existingWallets.map { wallet(it) }.toMutableList())
            val transactions = FakeTransactionSpec(wallets)
            return Triple(wallets, transactions, ImportPlanner(EntityCatalog(listOf(wallets, transactions))))
        }

        val walletCols = FakeWalletSpec().columns
        val txCols = FakeTransactionSpec(FakeWalletSpec()).columns

        listOf(ExportFormat.CSV_ZIP, ExportFormat.XLSX).forEach { format ->
            Given("a valid $format file into an empty database") {
                val (_, _, planner) = env()
                val file =
                    ImportTestSupport.write(
                        format,
                        listOf(
                            ImportTestSupport.Sheet("wallets", walletCols, listOf(wallet("Main"), wallet("Savings"))),
                            ImportTestSupport.Sheet(
                                "transactions",
                                txCols,
                                listOf(FakeTransactionSpec.sample("Main"), FakeTransactionSpec.sample("Savings", "5")),
                            ),
                        ),
                    )

                When("planning") {
                    val plan = runBlocking { planner.plan(file) }

                    Then("every row is new, nothing is written and references inside the file resolve") {
                        plan.hasErrors shouldBe false
                        plan.entities.map { it.id } shouldBe listOf("wallets", "transactions")
                        plan.entities.map { it.toAdd } shouldBe listOf(2, 2)
                        plan.entities.map { it.duplicates } shouldBe listOf(0, 0)
                    }
                }
            }
        }

        Given("a database that already has a wallet and a transaction") {
            val wallets = FakeWalletSpec(mutableListOf(wallet("Main")))
            val transactions = FakeTransactionSpec(wallets, mutableListOf(FakeTransactionSpec.sample("Main")))
            val planner = ImportPlanner(EntityCatalog(listOf(wallets, transactions)))
            val file =
                ImportTestSupport.write(
                    ExportFormat.CSV_ZIP,
                    listOf(
                        ImportTestSupport.Sheet("wallets", walletCols, listOf(wallet("Main"), wallet("New"))),
                        ImportTestSupport.Sheet(
                            "transactions",
                            txCols,
                            listOf(
                                FakeTransactionSpec.sample("Main"),
                                FakeTransactionSpec.sample("Main"),
                                FakeTransactionSpec.sample("New"),
                            ),
                        ),
                    ),
                )

            When("planning") {
                val plan = runBlocking { planner.plan(file) }

                Then("existing keys are duplicates, and identical rows count by occurrence") {
                    val byId = plan.entities.associateBy { it.id }
                    byId.getValue("wallets").duplicates shouldBe 1
                    byId.getValue("wallets").toAdd shouldBe 1
                    byId.getValue("transactions").duplicates shouldBe 1
                    byId.getValue("transactions").toAdd shouldBe 2
                    plan.hasErrors shouldBe false
                }
            }
        }

        Given("a transaction that points to an unknown wallet") {
            val (_, _, planner) = env(listOf("Main"))
            val file =
                ImportTestSupport.write(
                    ExportFormat.CSV_ZIP,
                    listOf(
                        ImportTestSupport.Sheet("transactions", txCols, listOf(FakeTransactionSpec.sample("Ghost"))),
                    ),
                )

            When("planning") {
                val plan = runBlocking { planner.plan(file) }

                Then("it is E06 on that row and column, and the plan has errors") {
                    val problem =
                        plan.entities
                            .single()
                            .problems
                            .single()
                    problem.code shouldBe ProblemCodes.UNRESOLVED_REFERENCE
                    problem.row shouldBe 2
                    problem.column shouldBe "wallet"
                    plan.hasErrors shouldBe true
                    plan.entities.single().toAdd shouldBe 0
                }
            }
        }

        Given("invalid and missing values") {
            val (_, _, planner) = env()
            val bad = mapOf("name" to "Main", "balance" to BigDecimal("1"), "archived" to false)
            val file =
                ImportTestSupport.write(
                    ExportFormat.CSV_ZIP,
                    listOf(ImportTestSupport.Sheet("wallets", walletCols, listOf(bad))),
                )
            val dir = Files.createTempDirectory("edit")
            val edited = dir.resolve("edited.zip")
            java.util.zip.ZipFile(file.toFile()).use { zip ->
                java.util.zip.ZipOutputStream(Files.newOutputStream(edited)).use { out ->
                    zip.entries().asSequence().forEach { e ->
                        out.putNextEntry(java.util.zip.ZipEntry(e.name))
                        var text = zip.getInputStream(e).readBytes().toString(Charsets.UTF_8)
                        if (e.name == "wallets.csv") text = "name,balance,archived\nMain,\"12,5\",maybe\n,3,true\n"
                        out.write(text.toByteArray())
                        out.closeEntry()
                    }
                }
            }

            When("planning") {
                val plan = runBlocking { planner.plan(edited) }

                Then("each problem has entity, row, column and an E05 code") {
                    val problems = plan.entities.single().problems
                    problems.map { Triple(it.row, it.column, it.code) } shouldBe
                        listOf(
                            Triple(2, "balance", ProblemCodes.INVALID_VALUE),
                            Triple(2, "archived", ProblemCodes.INVALID_VALUE),
                            Triple(3, "name", ProblemCodes.INVALID_VALUE),
                        )
                    plan.hasErrors shouldBe true
                }
            }
        }

        Given("files that are not valid exports") {
            val (_, _, planner) = env()

            Then("a missing _meta is E01") {
                val file = ImportTestSupport.write(ExportFormat.CSV_ZIP, emptyList(), meta = mapOf("x" to "y"))
                runBlocking { planner.plan(file) }.fileProblems.single().code shouldBe ProblemCodes.NOT_MOINEX_FILE
            }
            Then("an unknown layout version is E02") {
                val file =
                    ImportTestSupport.write(
                        ExportFormat.XLSX,
                        emptyList(),
                        meta =
                            mapOf(
                                "app" to "moinex",
                                "layout_version" to "2",
                            ),
                    )
                runBlocking { planner.plan(file) }.fileProblems.single().code shouldBe ProblemCodes.UNKNOWN_LAYOUT
            }
            Then("a declared sheet that is missing is E03") {
                val file =
                    ImportTestSupport.write(
                        ExportFormat.CSV_ZIP,
                        emptyList(),
                        meta = mapOf("app" to "moinex", "layout_version" to "1", "entities" to "wallets"),
                    )
                runBlocking { planner.plan(file) }.fileProblems.single().code shouldBe
                    ProblemCodes.MISSING_SHEET_OR_COLUMN
            }
            Then("a required column that is missing is E03 and an extra column is W02") {
                val cols =
                    listOf(
                        org.moinex.dataexchange.spec
                            .Column("name", org.moinex.dataexchange.spec.CellType.TEXT),
                        org.moinex.dataexchange.spec
                            .Column("extra", org.moinex.dataexchange.spec.CellType.TEXT),
                    )
                val file =
                    ImportTestSupport.write(
                        ExportFormat.CSV_ZIP,
                        listOf(
                            ImportTestSupport.Sheet(
                                "wallets",
                                cols,
                                listOf(
                                    mapOf(
                                        "name" to "A",
                                        "extra" to "z",
                                    ),
                                ),
                            ),
                        ),
                    )
                val plan = runBlocking { planner.plan(file) }
                plan.entities
                    .single()
                    .problems
                    .map { it.code }
                    .toSet() shouldBe
                    setOf(ProblemCodes.MISSING_SHEET_OR_COLUMN, ProblemCodes.UNKNOWN_COLUMN)
                plan.hasErrors shouldBe true
            }
            Then("a formula cell in XLSX is E07 and never evaluated") {
                val plan = runBlocking { planner.plan(ImportTestSupport.writeXlsxWithFormula()) }
                plan.entities
                    .single()
                    .problems
                    .single()
                    .code shouldBe ProblemCodes.FORMULA_CELL
                plan.hasErrors shouldBe true
            }
            Then("a text file is E01") {
                val txt = Files.createTempFile("x", ".txt")
                runBlocking { planner.plan(txt) }.fileProblems.single().code shouldBe ProblemCodes.NOT_MOINEX_FILE
            }
        }
    })
