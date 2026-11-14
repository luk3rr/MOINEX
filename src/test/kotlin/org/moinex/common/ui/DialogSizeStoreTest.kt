/*
 * Filename: DialogSizeStoreTest.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.util.UUID
import java.util.prefs.Preferences

class DialogSizeStoreTest :
    BehaviorSpec({
        Given("a store over a throwaway preferences node") {
            val node = Preferences.userRoot().node("org.moinex.test.${UUID.randomUUID()}")
            val store = PreferencesDialogSizeStore(node)

            afterSpec { node.removeNode() }

            Then("an unknown dialog has no size") {
                store.load("add_expense") shouldBe null
            }

            When("a size is saved") {
                store.save("add_expense", DialogResize.Size(640.0, 480.5))

                Then("it is read back, and another dialog is not affected") {
                    store.load("add_expense") shouldBe DialogResize.Size(640.0, 480.5)
                    store.load("add_income") shouldBe null
                }

                And("when it is cleared") {
                    store.clear("add_expense")

                    Then("the dialog has no size again") {
                        store.load("add_expense") shouldBe null
                    }
                }
            }

            When("the stored value is damaged") {
                node.put("dialog.size.broken", "wide,tall")
                node.put("dialog.size.zero", "0.0,0.0")

                Then("it is ignored") {
                    store.load("broken") shouldBe null
                    store.load("zero") shouldBe null
                }
            }

            When("the key is very long") {
                val key = "x".repeat(200)
                store.save(key, DialogResize.Size(500.0, 400.0))

                Then("it is still saved and read back") {
                    store.load(key) shouldBe DialogResize.Size(500.0, 400.0)
                }
            }
        }
    })
