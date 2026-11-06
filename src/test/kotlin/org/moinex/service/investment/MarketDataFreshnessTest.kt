/*
 * Filename: MarketDataFreshnessTest.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.LocalDateTime

class MarketDataFreshnessTest :
    BehaviorSpec({
        val now = LocalDateTime.of(2026, 11, 6, 12, 0)

        Given("the default time to live of 60 minutes") {
            Then("the TTL is one hour") {
                MarketDataFreshness.TTL shouldBe Duration.ofMinutes(60)
            }

            When("the data is 59 minutes old") {
                Then("it is fresh") {
                    MarketDataFreshness.isFresh(now.minusMinutes(59), now) shouldBe true
                }
            }

            When("the data is exactly 60 minutes old") {
                Then("it is stale") {
                    MarketDataFreshness.isFresh(now.minusMinutes(60), now) shouldBe false
                }
            }

            When("there is no date") {
                Then("it is stale") {
                    MarketDataFreshness.isFresh(null, now) shouldBe false
                }
            }

            When("the date is in the future because the clock moved back") {
                Then("it is stale, so it is fetched again") {
                    MarketDataFreshness.isFresh(now.plusMinutes(5), now) shouldBe false
                }
            }

            When("a different TTL is given") {
                Then("that one is used") {
                    MarketDataFreshness.isFresh(now.minusMinutes(10), now, Duration.ofMinutes(5)) shouldBe false
                    MarketDataFreshness.isFresh(now.minusMinutes(3), now, Duration.ofMinutes(5)) shouldBe true
                }
            }
        }
    })
