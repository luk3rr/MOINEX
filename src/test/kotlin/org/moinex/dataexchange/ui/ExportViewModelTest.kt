/*
 * Filename: ExportViewModelTest.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.ui

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.dataexchange.tabular.ExportFormat

class ExportViewModelTest :
    BehaviorSpec({
        fun model() =
            ExportViewModel(
                listOf(
                    ExportGroup("wallets", listOf(ExportEntity("wallets", 7), ExportEntity("transactions", 1102))),
                    ExportGroup("investments", listOf(ExportEntity("tickers", 0), ExportEntity("dividends", 0))),
                ),
            )

        Given("groups where only some entities have rows") {
            val model = model()

            Then("entities with rows start selected and the summary counts groups and rows") {
                model.selected shouldBe setOf("wallets", "transactions")
                model.groupState("wallets") shouldBe SelectionState.ALL
                model.groupState("investments") shouldBe SelectionState.NONE
                model.summary() shouldBe ExportSummary(1, 1109)
                model.format shouldBe ExportFormat.XLSX
                model.canExport shouldBe true
            }

            When("a group is toggled and then one entity unchecked") {
                model.setGroup("investments", true)
                model.setEntity("dividends", false)

                Then("the group is partially selected") {
                    model.groupState("investments") shouldBe SelectionState.SOME
                    model.summary().groups shouldBe 2
                }
            }

            When("everything is unchecked") {
                model.setGroup("wallets", false)
                model.setGroup("investments", false)

                Then("export is disabled") {
                    model.canExport shouldBe false
                    model.summary() shouldBe ExportSummary(0, 0)
                }
            }
        }
    })
