/*
 * Filename: SemanticVersionTest.kt
 * Created on: November 2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SemanticVersionTest :
    BehaviorSpec({
        Given("version texts") {
            When("parsing valid values") {
                Then("the optional v prefix is accepted") {
                    SemanticVersion.parse("v2.22.0") shouldBe SemanticVersion(2, 22, 0)
                    SemanticVersion.parse("2.21.1") shouldBe SemanticVersion(2, 21, 1)
                }
            }
            When("parsing invalid values") {
                Then("null is returned") {
                    listOf("", "v2.22", "2.22.0-rc1", "abc", "1.2.3.4", null).forEach {
                        SemanticVersion.parse(it) shouldBe null
                    }
                }
            }
        }

        Given("two versions") {
            When("comparing") {
                Then("numeric order is used, not text order") {
                    (SemanticVersion(2, 10, 0) > SemanticVersion(2, 9, 9)) shouldBe true
                    (SemanticVersion(3, 0, 0) > SemanticVersion(2, 99, 99)) shouldBe true
                    (SemanticVersion(2, 21, 0) > SemanticVersion(2, 21, 0)) shouldBe false
                    (SemanticVersion(2, 20, 9) < SemanticVersion(2, 21, 0)) shouldBe true
                }
            }
        }
    })
