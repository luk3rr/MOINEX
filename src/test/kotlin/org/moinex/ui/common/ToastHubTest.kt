/*
 * Filename: ToastHubTest.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.model.enums.NotificationType

class ToastHubTest :
    BehaviorSpec({
        class RecordingSurface : ToastSurface {
            val events = mutableListOf<String>()

            override fun addToast(
                id: Int,
                spec: ToastSpec,
            ) {
                events.add("add $id ${spec.type} sticky=${spec.sticky}")
            }

            override fun updateToastCount(
                id: Int,
                count: Int,
            ) {
                events.add("count $id $count")
            }

            override fun removeToast(id: Int) {
                events.add("remove $id")
            }
        }

        Given("a hub over a surface") {
            val surface = RecordingSurface()
            val hub = ToastHub(surface, maxVisible = 2)

            When("a warning is shown twice and an error once") {
                hub.show(NotificationType.WARNING, "Title", "Nothing selected", sticky = false)
                hub.show(NotificationType.WARNING, "Title", "Nothing selected", sticky = false)
                hub.show(NotificationType.ERROR, "Failed", "Could not save", sticky = true)

                Then("the warning is counted, and the error is added as a sticky toast") {
                    surface.events shouldBe
                        listOf(
                            "add 0 WARNING sticky=false",
                            "count 0 2",
                            "add 1 ERROR sticky=true",
                        )
                }
            }

            When("a third kind of toast arrives with the board full") {
                hub.show(NotificationType.SUCCESS, "Saved", "Done", sticky = false)

                Then("the old warning goes away and the error stays") {
                    surface.events.takeLast(2) shouldBe listOf("remove 0", "add 2 SUCCESS sticky=false")
                }
            }
        }
    })
