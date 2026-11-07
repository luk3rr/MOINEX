/*
 * Filename: YahooCooldownMessageTest.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class YahooCooldownMessageTest :
    BehaviorSpec({
        Given("a translated template and the moment the cooldown ends") {
            When("the text is built") {
                val text =
                    YahooCooldownMessage.text(
                        "O app tentará de novo depois das %s.",
                        LocalDateTime.of(2026, 11, 6, 22, 9, 51, 4_525_631),
                    )

                Then("only hours and minutes are shown, not the seconds or the date") {
                    text shouldBe "O app tentará de novo depois das 22:09."
                }
            }
        }
    })
