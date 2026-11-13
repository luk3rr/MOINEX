/*
 * Filename: ImportViewModelTest.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.ui

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.moinex.dataexchange.imports.EntityPlan
import org.moinex.dataexchange.imports.ImportExecutionException
import org.moinex.dataexchange.imports.ImportPlan
import org.moinex.dataexchange.imports.ImportResult
import org.moinex.dataexchange.spec.Problem
import org.moinex.dataexchange.spec.ProblemCodes
import org.moinex.dataexchange.spec.Severity
import java.nio.file.Path

class ImportViewModelTest :
    BehaviorSpec({
        fun entity(
            id: String,
            toAdd: Int,
            problems: List<Problem> = emptyList(),
        ) = EntityPlan(id, toAdd, toAdd, 0, problems, emptyList(), 0)

        val error = Problem("transactions", 2, "wallet", ProblemCodes.UNRESOLVED_REFERENCE, Severity.ERROR)

        Given("a file that is not an export") {
            val bad =
                ImportPlan(
                    0,
                    listOf(Problem("file", 0, null, ProblemCodes.NOT_MOINEX_FILE, Severity.ERROR)),
                    emptyList(),
                )
            val model = ImportViewModel({ _, _ -> bad }, { _, _ -> error("not called") })

            When("it is chosen") {
                runBlocking { model.choose(Path.of("x.zip")) }

                Then("the wizard stays on the file step and cannot import") {
                    model.step shouldBe ImportStep.FILE
                    model.canImport shouldBe false
                }
            }
        }

        Given("a plan with errors in one entity") {
            val plan =
                ImportPlan(1, emptyList(), listOf(entity("wallets", 2), entity("transactions", 3, listOf(error))))
            val model = ImportViewModel({ _, _ -> plan }, { _, _ -> error("not called") })

            When("it is chosen") {
                runBlocking { model.choose(Path.of("x.xlsx")) }

                Then("it goes to the preview on the entity with errors, and import is blocked") {
                    model.step shouldBe ImportStep.PREVIEW
                    model.selectedEntity shouldBe "transactions"
                    model.tab shouldBe PreviewTab.PROBLEMS
                    model.canImport shouldBe false
                    model.confirm() shouldBe false
                }
            }

            When("going back") {
                runBlocking { model.choose(Path.of("x.xlsx")) }
                model.back()

                Then("it returns to the file step without a plan") {
                    model.step shouldBe ImportStep.FILE
                    model.plan shouldBe null
                }
            }
        }

        Given("a valid plan") {
            val plan = ImportPlan(1, emptyList(), listOf(entity("wallets", 2)))
            val result = ImportResult(mapOf("wallets" to 2), mapOf("wallets" to 0))
            var calls = 0
            val model =
                ImportViewModel({ _, _ -> plan }, { _, _ ->
                    calls++
                    result
                })
            runBlocking { model.choose(Path.of("x.xlsx")) }

            When("confirmed") {
                val ok = model.confirm()

                Then("the executor runs once and the wizard shows the result") {
                    ok shouldBe true
                    calls shouldBe 1
                    model.step shouldBe ImportStep.RESULT
                    model.result shouldBe result
                }
            }
        }

        Given("an executor that fails") {
            val plan = ImportPlan(1, emptyList(), listOf(entity("wallets", 2)))
            val model = ImportViewModel({ _, _ -> plan }, { _, _ -> throw ImportExecutionException("boom") })
            runBlocking { model.choose(Path.of("x.xlsx")) }

            When("confirmed") {
                val ok = model.confirm()

                Then("the failure is kept, the step stays on the preview and nothing is marked done") {
                    ok shouldBe false
                    model.failure shouldBe "boom"
                    model.step shouldBe ImportStep.PREVIEW
                    model.result shouldBe null
                }
            }
        }

        Given("a plan with nothing new") {
            val plan = ImportPlan(1, emptyList(), listOf(entity("wallets", 0)))
            val model = ImportViewModel({ _, _ -> plan }, { _, _ -> error("not called") })
            runBlocking { model.choose(Path.of("x.xlsx")) }

            Then("there is nothing to import") {
                model.canImport shouldBe false
            }
        }
    })
