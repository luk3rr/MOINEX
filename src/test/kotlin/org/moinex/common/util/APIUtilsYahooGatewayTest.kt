/*
 * Filename: APIUtilsYahooGatewayTest.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.moinex.common.ClockProvider
import org.moinex.common.constant.Scripts
import org.moinex.exception.MoinexException
import org.moinex.model.enums.PeriodType

class APIUtilsYahooGatewayTest :
    BehaviorSpec({
        val original = APIUtils.yahooGateway

        beforeSpec { mockkObject(APIUtils) }

        afterSpec {
            APIUtils.yahooGateway = original
            unmockkAll()
        }

        Given("a rate limit reported by the price script") {
            APIUtils.yahooGateway = YahooGateway(ClockProvider())
            coEvery { APIUtils.runPythonScript(any(), any(), any()) } returns
                JSONObject("""{"AAA":{"error":"x","error_kind":"rate_limit"}}""")

            runBlocking { APIUtils.fetchStockPrices(listOf("AAA")) }

            When("the history and the fundamentals are requested afterwards") {
                Then("both are refused by the gateway without running a script") {
                    shouldThrow<MoinexException.YahooCooldownException> {
                        runBlocking { APIUtils.fetchStockPriceHistory("AAA", "2026-01-01", "2026-01-31") }
                    }
                    shouldThrow<MoinexException.YahooCooldownException> {
                        runBlocking { APIUtils.fetchFundamentalAnalysis("AAA", PeriodType.ANNUAL) }
                    }
                    shouldThrow<MoinexException.YahooCooldownException> {
                        runBlocking { APIUtils.fetchStockPrices(listOf("AAA")) }
                    }

                    coVerify(exactly = 1) { APIUtils.runPythonScript(Scripts.GET_STOCK_PRICE_SCRIPT, any(), any()) }
                    coVerify(exactly = 0) {
                        APIUtils.runPythonScript(Scripts.GET_STOCK_PRICE_HISTORY_SCRIPT, any(), any())
                    }
                    coVerify(exactly = 0) {
                        APIUtils.runPythonScript(Scripts.GET_FUNDAMENTAL_DATA_SCRIPT, any(), any())
                    }
                }
            }
        }

        Given("scripts that do not reach Yahoo") {
            APIUtils.yahooGateway = YahooGateway(ClockProvider())
            coEvery { APIUtils.runPythonScript(any(), any(), any()) } returns
                JSONObject("""{"error":"x","error_kind":"rate_limit"}""")
            runBlocking { APIUtils.fetchStockPrices(listOf("AAA")) }

            When("the Brazilian indicators are requested during a cooldown") {
                coEvery {
                    APIUtils.runPythonScript(
                        Scripts.GET_BRAZILIAN_MARKET_INDICATORS_SCRIPT,
                        any(),
                        any(),
                    )
                } returns
                    JSONObject("""{"ok":true}""")

                Then("they still run") {
                    runBlocking { APIUtils.fetchBrazilianMarketIndicators() }.getBoolean("ok") shouldBe true
                }
            }
        }
    })
