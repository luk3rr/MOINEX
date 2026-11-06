/*
 * Filename: FundamentalPriceOverlayTest.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDateTime

class FundamentalPriceOverlayTest :
    BehaviorSpec({
        val at = LocalDateTime.of(2026, 11, 6, 10, 30, 15, 123_000_000)
        val stored =
            """
            {
              "symbol": "AAA",
              "valuation": {
                "current_price": {"value": 10.0, "type": "currency", "data_temporality": "real_time"},
                "graham_fair_value": {"value": 30.0, "type": "currency"}
              },
              "price_performance": {
                "current_price": {"value": 10.0, "type": "currency"},
                "day_high": {"value": 11.0, "type": "currency"}
              },
              "income": {"dividend_yield": {"value": 5.0, "type": "percent"}}
            }
            """.trimIndent()

        Given("an analysis stored with an old price") {
            When("the ticker's current price is applied") {
                val result = JSONObject(FundamentalPriceOverlay.apply(stored, BigDecimal("12.34"), at))

                Then("the price of the valuation and of the price performance sections is the ticker's") {
                    listOf("valuation", "price_performance").forEach { section ->
                        val price = result.getJSONObject(section).getJSONObject("current_price")
                        price.getBigDecimal("value").compareTo(BigDecimal("12.34")) shouldBe 0
                        price.getString("type") shouldBe "currency"
                        price.getString("data_temporality") shouldBe "real_time"
                        price.getString("reference_date") shouldBe "2026-11-06T10:30:15"
                    }
                }

                Then("everything else is untouched") {
                    result.getJSONObject("valuation").getJSONObject("graham_fair_value").getDouble("value") shouldBe
                        30.0
                    result.getJSONObject("price_performance").getJSONObject("day_high").getDouble("value") shouldBe 11.0
                    result.getJSONObject("income").getJSONObject("dividend_yield").getDouble("value") shouldBe 5.0
                    result.getString("symbol") shouldBe "AAA"
                }
            }
        }

        Given("an analysis without a price performance section") {
            val partial = """{"valuation":{"current_price":{"value":1}}}"""

            When("the price is applied") {
                val result = JSONObject(FundamentalPriceOverlay.apply(partial, BigDecimal("2"), at))

                Then("no section is invented") {
                    result.has("price_performance") shouldBe false
                    result.getJSONObject("valuation").getJSONObject("current_price").getInt("value") shouldBe 2
                }
            }
        }

        Given("a stored analysis that cannot be read") {
            When("the price is applied") {
                Then("it is returned as it was") {
                    FundamentalPriceOverlay.apply("not json", BigDecimal("2"), at) shouldBe "not json"
                }
            }
        }
    })
