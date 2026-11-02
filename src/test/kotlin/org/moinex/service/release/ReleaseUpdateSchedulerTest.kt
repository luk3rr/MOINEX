/*
 * Filename: ReleaseUpdateSchedulerTest.kt
 * Created on: November 2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.release

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class ReleaseUpdateSchedulerTest :
    BehaviorSpec({
        val service = mockk<ReleaseUpdateService>()
        val scheduler = ReleaseUpdateScheduler(service)

        Given("a check that throws") {
            When("running it safely") {
                every { service.checkForUpdate() } throws IllegalStateException("boom")

                Then("the error is swallowed so the loop keeps going") {
                    scheduler.checkSafely() shouldBe false
                }
            }
        }

        Given("a check that notifies") {
            When("running it safely") {
                every { service.checkForUpdate() } returns true

                Then("the result is passed through") {
                    scheduler.checkSafely() shouldBe true
                }
            }
        }

        Given("the scheduling interval") {
            Then("it is 24 hours") {
                ReleaseUpdateScheduler.CHECK_INTERVAL_MS shouldBe 86_400_000L
            }
        }
    })
