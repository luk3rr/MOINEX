package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.moinex.common.ClockProvider
import org.moinex.common.util.APIUtils
import org.moinex.model.investment.BenchmarkPrice
import org.moinex.repository.investment.BenchmarkPriceRepository
import java.math.BigDecimal

class BenchmarkServiceTest :
    BehaviorSpec({
        val repository = mockk<BenchmarkPriceRepository>()
        val clock = ClockProvider()
        val service = BenchmarkService(repository, clock)
        val today = clock.today()

        beforeSpec { mockkObject(APIUtils) }
        afterSpec { unmockkAll() }
        afterContainer { clearAllMocks(answers = true) }

        fun response(vararg rows: Pair<String, Double>) =
            JSONObject().put(
                "prices",
                JSONArray(
                    rows.map { JSONObject().put("date", it.first).put("price", it.second).put("is_month_end", false) },
                ),
            )

        fun stored(date: java.time.LocalDate) =
            BenchmarkPrice(symbol = "^BVSP", priceDate = date, closingPrice = BigDecimal("100"))

        Given("no Ibovespa stored") {
            Then("the last 12 months are downloaded and saved") {
                val saved = slot<Iterable<BenchmarkPrice>>()
                every { repository.findLatestBySymbol("^BVSP") } returns null
                every { repository.findBySymbolAndDateRange("^BVSP", any(), any()) } returns emptyList()
                every { repository.saveAll(capture(saved)) } answers { saved.captured.toList() }
                coEvery { APIUtils.fetchStockPriceHistory("^BVSP", any(), any(), any()) } returns
                    response(today.minusDays(2).toString() to 150000.0, today.minusDays(1).toString() to 151000.0)

                service.syncIbovespa() shouldBe 2

                coVerify {
                    APIUtils.fetchStockPriceHistory(
                        "^BVSP",
                        today.minusMonths(12).toString(),
                        today.plusDays(1).toString(),
                        any(),
                    )
                }
                saved.captured.map { it.closingPrice.toInt() } shouldBe listOf(150000, 151000)
            }
        }

        Given("an Ibovespa stored up to yesterday") {
            Then("nothing is downloaded") {
                every { repository.findLatestBySymbol("^BVSP") } returns stored(today.minusDays(1))

                service.syncIbovespa() shouldBe 0

                coVerify(exactly = 0) { APIUtils.fetchStockPriceHistory(any(), any(), any(), any()) }
            }
        }

        Given("an Ibovespa stored up to two weeks ago") {
            Then("only the days since then are downloaded and the last stored day is updated in place") {
                val last = today.minusDays(14)
                val existing = stored(last)
                val saved = slot<Iterable<BenchmarkPrice>>()
                every { repository.findLatestBySymbol("^BVSP") } returns existing
                every { repository.findBySymbolAndDateRange("^BVSP", any(), any()) } returns listOf(existing)
                every { repository.saveAll(capture(saved)) } answers { saved.captured.toList() }
                coEvery { APIUtils.fetchStockPriceHistory("^BVSP", any(), any(), any()) } returns
                    response(last.toString() to 120.0)

                service.syncIbovespa() shouldBe 1

                coVerify {
                    APIUtils.fetchStockPriceHistory(
                        "^BVSP",
                        last.toString(),
                        today.plusDays(1).toString(),
                        any(),
                    )
                }
                saved.captured.single() shouldBe existing
                existing.closingPrice.toInt() shouldBe 120
            }
        }

        Given("a download that fails") {
            Then("nothing is written and the failure is not thrown") {
                every { repository.findLatestBySymbol("^BVSP") } returns null
                coEvery { APIUtils.fetchStockPriceHistory("^BVSP", any(), any(), any()) } returns
                    JSONObject().put("error", "No data found")

                service.syncIbovespa() shouldBe 0

                verify(exactly = 0) { repository.saveAll(any<Iterable<BenchmarkPrice>>()) }
            }
        }
    })
