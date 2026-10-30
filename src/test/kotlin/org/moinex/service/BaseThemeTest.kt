package org.moinex.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class BaseThemeTest :
    BehaviorSpec({
        Given("the dark mode preference") {
            Then("the base theme is Dracula") {
                BaseTheme.forMode(true) shouldBe BaseTheme.DRACULA
            }
        }

        Given("the light mode preference") {
            Then("the base theme is Primer Light") {
                BaseTheme.forMode(false) shouldBe BaseTheme.PRIMER_LIGHT
            }
        }
    })
