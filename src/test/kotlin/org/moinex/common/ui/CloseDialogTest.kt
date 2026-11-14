/*
 * Filename: CloseDialogTest.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import javafx.scene.layout.Pane
import javafx.scene.layout.Region

class CloseDialogTest :
    BehaviorSpec({
        Given("a field nested inside a dialog card that publishes its close action") {
            var closed = 0
            val field = Region()
            val body = Pane(field)
            val card = Pane(body)
            card.properties[DIALOG_CLOSE_KEY] = Runnable { closed++ }

            When("the field asks to close its dialog") {
                field.closeDialog()

                Then("the close action of the card runs once") {
                    closed shouldBe 1
                }
            }
        }

        Given("two nested dialogs") {
            val order = mutableListOf<String>()
            val inner = Pane()
            val innerCard = Pane(inner)
            innerCard.properties[DIALOG_CLOSE_KEY] = Runnable { order.add("inner") }
            val outerCard = Pane()
            outerCard.properties[DIALOG_CLOSE_KEY] = Runnable { order.add("outer") }

            When("a node of the inner one closes its dialog") {
                inner.closeDialog()

                Then("only the inner close action runs") {
                    order shouldBe listOf("inner")
                }
            }
        }

        Given("a node that is in no dialog and in no window") {
            val orphan = Region()

            Then("closing does nothing and does not fail") {
                orphan.closeDialog()
            }
        }
    })
