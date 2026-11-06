package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
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
import org.moinex.exception.MoinexException
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.investment.TickerPriceHistoryFactory
import org.moinex.model.dto.StockHistoryRequest
import org.moinex.model.dto.TickerMetadata
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPriceHistory
import org.moinex.repository.investment.TickerPriceHistoryRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class TickerPriceHistoryServiceDailySyncTest :
    BehaviorSpec({
        val priceHistoryRepository = mockk<TickerPriceHistoryRepository>()
        val tickerRepository = mockk<TickerRepository>()
        val marketDataRepository = mockk<TickerRepository>()
        val clockProvider = ClockProvider()
        val service =
            TickerPriceHistoryService(
                priceHistoryRepository,
                tickerRepository,
                mockk<TickerPurchaseRepository>(),
                mockk<TickerSaleRepository>(),
                clockProvider,
                TickerMarketDataService(marketDataRepository),
            )

        val today = clockProvider.today()
        val windowStart = today.minusMonths(12)
        val ticker = TickerFactory.create(id = 1, symbol = "PETR4.SA")

        fun response(vararg rows: Triple<LocalDate, Double, Boolean>): JSONObject =
            JSONObject().put(
                "prices",
                JSONArray(
                    rows.map {
                        JSONObject()
                            .put("date", it.first.toString())
                            .put("price", it.second)
                            .put("is_month_end", it.third)
                    },
                ),
            )

        beforeSpec { mockkObject(APIUtils) }
        afterSpec { unmockkAll() }
        afterContainer { clearAllMocks(answers = true) }

        fun stubMarketData() {
            every { marketDataRepository.save(any<Ticker>()) } answers { firstArg() }
        }

        fun batchResponse(vararg entries: Pair<String, JSONObject>): JSONObject =
            JSONObject().also { json -> entries.forEach { (symbol, result) -> json.put(symbol, result) } }

        fun mockSaved(): io.mockk.CapturingSlot<Iterable<TickerPriceHistory>> {
            val saved = slot<Iterable<TickerPriceHistory>>()
            every { priceHistoryRepository.saveAll(capture(saved)) } answers { saved.captured.toList() }
            return saved
        }

        Given("a ticker with no stored price history") {
            val pastMonthEnd = YearMonth.from(today).minusMonths(2).atEndOfMonth()

            When("syncing the daily history") {
                stubMarketData()
                every { priceHistoryRepository.findLatestPriceByTicker(1) } returns null
                every { priceHistoryRepository.findByTickerIdAndDateRange(1, any(), any()) } returns emptyList()
                coEvery { APIUtils.fetchStockPriceHistory("PETR4.SA", any(), any(), any()) } returns
                    response(
                        Triple(pastMonthEnd.minusDays(1), 10.0, false),
                        Triple(pastMonthEnd, 11.0, true),
                        Triple(today, 12.0, true),
                    )
                val saved = mockSaved()

                val count = service.syncDaily(ticker)

                Then("it fetches the whole window up to tomorrow, so that today is included") {
                    coVerify {
                        APIUtils.fetchStockPriceHistory(
                            "PETR4.SA",
                            windowStart.toString(),
                            today.plusDays(1).toString(),
                            any(),
                        )
                    }
                }

                Then("it stores every day without asking for specific dates") {
                    count shouldBe 3
                    saved.captured.map { it.priceDate } shouldContainExactly
                        listOf(pastMonthEnd.minusDays(1), pastMonthEnd, today)
                    coVerify { APIUtils.fetchStockPriceHistory(any(), any(), any(), null) }
                }

                Then("only the last day of a past month is flagged as month end, never one of the current month") {
                    saved.captured.map { it.isMonthEnd } shouldContainExactly listOf(false, true, false)
                }
            }
        }

        Given("a ticker whose stored series is dense up to its last day") {
            val last = today.minusDays(3)

            When("syncing the daily history") {
                stubMarketData()
                every { priceHistoryRepository.findLatestPriceByTicker(1) } returns
                    TickerPriceHistoryFactory.create(ticker = ticker, priceDate = last)
                every { priceHistoryRepository.countByTickerIdAndDateRange(1, windowStart, last) } returns 250
                every { priceHistoryRepository.findByTickerIdAndDateRange(1, any(), any()) } returns emptyList()
                coEvery { APIUtils.fetchStockPriceHistory(any(), any(), any(), any()) } returns response()
                mockSaved()

                service.syncDaily(ticker)

                Then("it fetches only from the last stored day") {
                    coVerify {
                        APIUtils.fetchStockPriceHistory(
                            "PETR4.SA",
                            last.toString(),
                            today.plusDays(1).toString(),
                            any(),
                        )
                    }
                }
            }
        }

        Given("a ticker with only a few stored prices inside the window") {
            val last = today.minusDays(3)

            When("syncing the daily history") {
                stubMarketData()
                every { priceHistoryRepository.findLatestPriceByTicker(1) } returns
                    TickerPriceHistoryFactory.create(ticker = ticker, priceDate = last)
                every { priceHistoryRepository.countByTickerIdAndDateRange(1, windowStart, last) } returns 12
                every { priceHistoryRepository.findByTickerIdAndDateRange(1, any(), any()) } returns emptyList()
                coEvery { APIUtils.fetchStockPriceHistory(any(), any(), any(), any()) } returns response()
                mockSaved()

                service.syncDaily(ticker)

                Then("it fetches the whole window again") {
                    coVerify {
                        APIUtils.fetchStockPriceHistory(
                            "PETR4.SA",
                            windowStart.toString(),
                            today.plusDays(1).toString(),
                            any(),
                        )
                    }
                }
            }
        }

        Given("a stored price older than the window") {
            When("syncing the daily history") {
                stubMarketData()
                every { priceHistoryRepository.findLatestPriceByTicker(1) } returns
                    TickerPriceHistoryFactory.create(ticker = ticker, priceDate = windowStart.minusMonths(3))
                every { priceHistoryRepository.findByTickerIdAndDateRange(1, any(), any()) } returns emptyList()
                coEvery { APIUtils.fetchStockPriceHistory(any(), any(), any(), any()) } returns response()
                mockSaved()

                service.syncDaily(ticker)

                Then("it does not count rows and fetches the whole window") {
                    verify(exactly = 0) { priceHistoryRepository.countByTickerIdAndDateRange(any(), any(), any()) }
                    coVerify {
                        APIUtils.fetchStockPriceHistory(
                            "PETR4.SA",
                            windowStart.toString(),
                            today.plusDays(1).toString(),
                            any(),
                        )
                    }
                }
            }
        }

        Given("a day that is already stored") {
            val day = today.minusDays(40)

            When("syncing brings a new price for it") {
                stubMarketData()
                val existing =
                    TickerPriceHistoryFactory.create(
                        id = 9,
                        ticker = ticker,
                        priceDate = day,
                        closingPrice = BigDecimal("10.00"),
                        isMonthEnd = true,
                    )
                every { priceHistoryRepository.findLatestPriceByTicker(1) } returns null
                every { priceHistoryRepository.findByTickerIdAndDateRange(1, any(), any()) } returns listOf(existing)
                coEvery { APIUtils.fetchStockPriceHistory(any(), any(), any(), any()) } returns
                    response(Triple(day, 10.5, false))
                val saved = mockSaved()

                service.syncDaily(ticker)

                Then("the row is updated in place and keeps its month end flag") {
                    val row = saved.captured.single()
                    row.id shouldBe 9
                    row.closingPrice.compareTo(BigDecimal("10.5")) shouldBe 0
                    row.isMonthEnd shouldBe true
                }
            }
        }

        Given("a response without prices") {
            When("syncing the daily history") {
                stubMarketData()
                every { priceHistoryRepository.findLatestPriceByTicker(1) } returns null
                coEvery { APIUtils.fetchStockPriceHistory(any(), any(), any(), any()) } returns response()

                val count = service.syncDaily(ticker)

                Then("nothing is written") {
                    count shouldBe 0
                    verify(exactly = 0) { priceHistoryRepository.saveAll(any<Iterable<TickerPriceHistory>>()) }
                }
            }
        }

        Given("two tickers where the first one fails") {
            val first = TickerFactory.create(id = 1, symbol = "BAD1.SA")
            val second = TickerFactory.create(id = 2, symbol = "OK2.SA")

            When("syncing all of them") {
                stubMarketData()
                every { priceHistoryRepository.findLatestPriceByTicker(any()) } returns null
                every { priceHistoryRepository.findByTickerIdAndDateRange(any(), any(), any()) } returns emptyList()
                coEvery { APIUtils.fetchStockPriceHistoryBatch(any()) } returns
                    batchResponse(
                        "BAD1.SA" to JSONObject().put("error", "No data found").put("error_kind", "invalid_symbol"),
                        "OK2.SA" to response(Triple(today, 5.0, false)).put("currency", "BRL"),
                    )
                val saved = mockSaved()

                service.syncDailyPriceHistory(listOf(first, second))

                Then("the failure is isolated and the second ticker is still stored") {
                    saved.captured.map { it.ticker.symbol } shouldContainExactly listOf("OK2.SA")
                    verify(exactly = 0) { priceHistoryRepository.delete(any()) }
                }

                Then("only the ticker that worked is stamped as synced, with the currency the script reported") {
                    first.metadata shouldBe null
                    val data = TickerMetadata.fromJson(second.metadata)
                    (data.historyUpdatedAt != null) shouldBe true
                    data.currency shouldBe "BRL"
                }

                Then("one script run served both tickers") {
                    coVerify(exactly = 1) { APIUtils.fetchStockPriceHistoryBatch(any()) }
                }
            }
        }

        Given("no explicit tickers") {
            val active = TickerFactory.create(id = 1, symbol = "PETR4.SA")

            When("syncing all of them") {
                stubMarketData()
                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns listOf(active)
                every { priceHistoryRepository.findLatestPriceByTicker(1) } returns null
                every { priceHistoryRepository.findByTickerIdAndDateRange(any(), any(), any()) } returns emptyList()
                coEvery { APIUtils.fetchStockPriceHistoryBatch(any()) } returns
                    batchResponse("PETR4.SA" to response())
                mockSaved()

                service.syncDailyPriceHistory()

                Then("the active tickers are used") {
                    verify { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() }
                    coVerify {
                        APIUtils.fetchStockPriceHistoryBatch(
                            match { requests ->
                                requests.map { it.symbol } ==
                                    listOf("PETR4.SA")
                            },
                        )
                    }
                }
            }
        }

        Given("tickers whose history was synced recently") {
            val fresh = TickerFactory.create(id = 1, symbol = "FRESH.SA")
            val stale = TickerFactory.create(id = 2, symbol = "STALE.SA")

            When("syncing without force") {
                stubMarketData()
                fresh.metadata = TickerMetadata(historyUpdatedAt = LocalDateTime.now().minusMinutes(10)).toJson()
                stale.metadata = TickerMetadata(historyUpdatedAt = LocalDateTime.now().minusMinutes(90)).toJson()
                every { priceHistoryRepository.findLatestPriceByTicker(any()) } returns null
                every { priceHistoryRepository.findByTickerIdAndDateRange(any(), any(), any()) } returns emptyList()
                coEvery { APIUtils.fetchStockPriceHistoryBatch(any()) } returns
                    batchResponse("STALE.SA" to response())
                mockSaved()

                service.syncDailyPriceHistory(listOf(fresh, stale))

                Then("only the stale ticker is requested") {
                    coVerify(exactly = 1) {
                        APIUtils.fetchStockPriceHistoryBatch(
                            match { requests ->
                                requests.map { it.symbol } ==
                                    listOf("STALE.SA")
                            },
                        )
                    }
                }
            }

            When("syncing with force") {
                stubMarketData()
                every { priceHistoryRepository.findLatestPriceByTicker(any()) } returns null
                every { priceHistoryRepository.findByTickerIdAndDateRange(any(), any(), any()) } returns emptyList()
                coEvery { APIUtils.fetchStockPriceHistoryBatch(any()) } returns
                    batchResponse("FRESH.SA" to response(), "STALE.SA" to response())
                mockSaved()

                service.syncDailyPriceHistory(listOf(fresh, stale), force = true)

                Then("both are requested") {
                    coVerify(exactly = 1) {
                        APIUtils.fetchStockPriceHistoryBatch(
                            match { requests -> requests.map { it.symbol } == listOf("FRESH.SA", "STALE.SA") },
                        )
                    }
                }
            }

            When("every ticker is fresh") {
                stubMarketData()
                val onlyFresh = listOf(fresh)

                service.syncDailyPriceHistory(onlyFresh)

                Then("no script runs at all") {
                    coVerify(exactly = 0) { APIUtils.fetchStockPriceHistoryBatch(any()) }
                }
            }
        }

        Given("a batch request") {
            val known = TickerFactory.create(id = 1, symbol = "KNOWN.SA")
            val unknown = TickerFactory.create(id = 2, symbol = "NEW.SA")

            When("one ticker has a stored currency and a series dense up to three days ago") {
                stubMarketData()
                known.metadata = TickerMetadata(currency = "USD").toJson()
                val last = today.minusDays(3)
                every { priceHistoryRepository.findLatestPriceByTicker(1) } returns
                    TickerPriceHistoryFactory.create(ticker = known, priceDate = last)
                every { priceHistoryRepository.countByTickerIdAndDateRange(1, windowStart, last) } returns 250
                every { priceHistoryRepository.findLatestPriceByTicker(2) } returns null
                every { priceHistoryRepository.findByTickerIdAndDateRange(any(), any(), any()) } returns emptyList()
                coEvery { APIUtils.fetchStockPriceHistoryBatch(any()) } returns
                    batchResponse("KNOWN.SA" to response(), "NEW.SA" to response())
                mockSaved()

                service.syncDailyPriceHistory(listOf(known, unknown), force = true)

                Then("each symbol asks for its own start and carries the currency it already knows") {
                    coVerify {
                        APIUtils.fetchStockPriceHistoryBatch(
                            match { requests ->
                                requests ==
                                    listOf(
                                        StockHistoryRequest("KNOWN.SA", last, today.plusDays(1), "USD"),
                                        StockHistoryRequest("NEW.SA", windowStart, today.plusDays(1), null),
                                    )
                            },
                        )
                    }
                }
            }
        }

        Given("a batch that cannot run, as when Yahoo is cooling down") {
            val any = TickerFactory.create(id = 1, symbol = "ANY.SA")

            When("syncing") {
                stubMarketData()
                every { priceHistoryRepository.findLatestPriceByTicker(any()) } returns null
                coEvery { APIUtils.fetchStockPriceHistoryBatch(any()) } throws
                    MoinexException.YahooCooldownException(LocalDateTime.now().plusMinutes(5), "paused")

                service.syncDailyPriceHistory(listOf(any), force = true)

                Then("nothing is thrown, stored data is untouched and the ticker is not stamped") {
                    verify(exactly = 0) { priceHistoryRepository.saveAll(any<Iterable<TickerPriceHistory>>()) }
                    any.metadata shouldBe null
                }
            }
        }
    })
