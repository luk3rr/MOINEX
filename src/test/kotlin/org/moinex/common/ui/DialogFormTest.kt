package org.moinex.common.ui

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class DialogFormTest :
    BehaviorSpec({
        Given("the focus when Enter is pressed") {
            Then("Enter saves from a plain field") {
                DialogForm.enterTriggersSave(false, false, false) shouldBe true
            }

            Then("Enter does not save from a text area, a button or an open list") {
                DialogForm.enterTriggersSave(true, false, false) shouldBe false
                DialogForm.enterTriggersSave(false, true, false) shouldBe false
                DialogForm.enterTriggersSave(false, false, true) shouldBe false
            }
        }
    })
