/*
 * Filename: PreferencesServiceLimitTest.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class PreferencesServiceLimitTest :
    BehaviorSpec({
        Given("the text of a limit field") {
            Then("blank means no limit") {
                PreferencesService.interpretLimit("", 20) shouldBe null
                PreferencesService.interpretLimit("   ", 20) shouldBe null
                PreferencesService.interpretLimit(null, 20) shouldBe null
            }

            Then("a whole number above zero is the new limit") {
                PreferencesService.interpretLimit("30", 20) shouldBe 30
                PreferencesService.interpretLimit(" 7 ", null) shouldBe 7
            }

            Then("zero, a negative number and text that is not a number keep the current limit") {
                PreferencesService.interpretLimit("0", 20) shouldBe 20
                PreferencesService.interpretLimit("-5", 20) shouldBe 20
                PreferencesService.interpretLimit("abc", 20) shouldBe 20
                PreferencesService.interpretLimit("2.5", null) shouldBe null
            }
        }
    })
