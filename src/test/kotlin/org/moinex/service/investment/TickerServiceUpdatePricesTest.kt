package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONObject
import org.moinex.common.ClockProvider
import org.moinex.common.extension.toRounded
import org.moinex.common.util.APIUtils
import org.moinex.common.util.FileUtils
import org.moinex.factory.investment.TickerFactory
import org.moinex.model.dto.TickerMetadata
import org.moinex.model.enums.AssetType
import org.moinex.model.investment.Ticker
import org.moinex.repository.investment.CryptoExchangeRepository
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.nio.file.Path
import java.time.LocalDateTime

class TickerServiceUpdatePricesTest :
    BehaviorSpec({
        val tickerRepository = mockk<TickerRepository>()
        val tickerPurchaseRepository = mockk<TickerPurchaseRepository>()
        val tickerSaleRepository = mockk<TickerSaleRepository>()
        val dividendRepository = mockk<DividendRepository>()
        val cryptoExchangeRepository = mockk<CryptoExchangeRepository>()
        val walletService = mockk<WalletService>(relaxed = true)
        val notificationService = mockk<NotificationService>(relaxed = true)
        val preferencesService = mockk<PreferencesService>(relaxed = true)
        val clockProvider = ClockProvider()

        val service =
            TickerService(
                tickerRepository,
                tickerPurchaseRepository,
                tickerSaleRepository,
                dividendRepository,
                cryptoExchangeRepository,
                walletService,
                notificationService,
                preferencesService,
                clockProvider,
                TickerMarketDataService(tickerRepository),
            )

        beforeSpec {
            mockkObject(APIUtils)
            mockkObject(FileUtils)
        }

        afterContainer {
            clearAllMocks(answers = true)
        }

        afterSpec {
            unmockkAll()
        }

        Given("no active tickers in the database") {
            When("updating all non-archived ticker prices") {
                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns emptyList()

                service.updateAllNonArchivedTickersPrices(force = true)

                Then("should skip price update") {
                    verify(exactly = 1) { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() }
                    verify(exactly = 0) { tickerRepository.save(any<Ticker>()) }
                }
            }
        }

        Given("active tickers in the database") {
            And("API returns successful response for all tickers") {
                When("updating all non-archived ticker prices") {
                    val ticker1 =
                        TickerFactory.create(
                            id = 1,
                            symbol = "AAPL",
                            type = AssetType.STOCK,
                            currentUnitValue = BigDecimal("100.00"),
                        )
                    val ticker2 =
                        TickerFactory.create(
                            id = 2,
                            symbol = "GOOGL",
                            type = AssetType.STOCK,
                            currentUnitValue = BigDecimal("200.00"),
                        )

                    val apiResponse =
                        JSONObject()
                            .put(
                                "AAPL",
                                JSONObject()
                                    .put("price", 150.00)
                                    .put("website", "apple.com"),
                            ).put(
                                "GOOGL",
                                JSONObject()
                                    .put("price", 250.00)
                                    .put("website", "google.com"),
                            )

                    every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                        listOf(ticker1, ticker2)
                    coEvery { APIUtils.fetchStockPrices(any()) } returns
                        apiResponse
                    coEvery { APIUtils.fetchStockLogos(any()) } returns
                        JSONObject()
                    every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }

                    service.updateAllNonArchivedTickersPrices(force = true)

                    Then("should update ticker prices") {
                        ticker1.currentUnitValue shouldBe BigDecimal("150.00")
                        ticker2.currentUnitValue shouldBe BigDecimal("250.00")
                    }

                    Then("should update ticker domains") {
                        ticker1.domain shouldBe "apple.com"
                        ticker2.domain shouldBe "google.com"
                    }

                    Then("should save all updated tickers") {
                        verify { tickerRepository.save(any<Ticker>()) }
                    }
                }
            }

            And("API returns exception for some tickers") {
                When("updating ticker prices with partial failures") {
                    val ticker1 =
                        TickerFactory.create(
                            id = 1,
                            symbol = "AAPL",
                            currentUnitValue = BigDecimal("100.00"),
                        )
                    val ticker2 =
                        TickerFactory.create(
                            id = 2,
                            symbol = "INVALID",
                            currentUnitValue = BigDecimal("200.00"),
                        )

                    val apiResponse =
                        JSONObject()
                            .put(
                                "AAPL",
                                JSONObject()
                                    .put("price", 150.00)
                                    .put("website", "apple.com"),
                            ).put(
                                "INVALID",
                                JSONObject()
                                    .put("exception", "Ticker not found"),
                            )

                    every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                        listOf(ticker1, ticker2)
                    coEvery { APIUtils.fetchStockPrices(any()) } returns
                        apiResponse
                    coEvery { APIUtils.fetchStockLogos(any()) } returns
                        JSONObject()
                    every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }

                    val failed =
                        service.updateTickersPriceFromApi(listOf(ticker1, ticker2))

                    Then("should update successful ticker") {
                        ticker1.currentUnitValue shouldBe BigDecimal("150.00")
                    }

                    Then("should not update failed ticker") {
                        ticker2.currentUnitValue shouldBe BigDecimal("200.00")
                    }

                    Then("should return list of failed tickers") {
                        failed.size shouldBe 1
                        failed[0].symbol shouldBe "INVALID"
                    }

                    Then("should save only successful tickers") {
                        verify(exactly = 1) { tickerRepository.save(any<Ticker>()) }
                    }
                }
            }

            And("API response does not contain ticker data") {
                When("updating ticker prices with missing data") {
                    val ticker =
                        TickerFactory.create(
                            id = 1,
                            symbol = "AAPL",
                            currentUnitValue = BigDecimal("100.00"),
                        )

                    val apiResponse = JSONObject()

                    every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns listOf(ticker)
                    coEvery { APIUtils.fetchStockPrices(any()) } returns
                        apiResponse
                    every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }

                    val failed =
                        service.updateTickersPriceFromApi(listOf(ticker))

                    Then("should not update ticker price") {
                        ticker.currentUnitValue shouldBe BigDecimal("100.00")
                    }

                    Then("should return ticker as failed") {
                        failed.size shouldBe 1
                        failed[0].symbol shouldBe "AAPL"
                    }

                    Then("should not save any tickers") {
                        verify(exactly = 0) { tickerRepository.save(any<Ticker>()) }
                    }
                }
            }
        }

        Given("multiple tickers with different types") {
            When("updating prices for stocks and cryptocurrencies") {
                val stock =
                    TickerFactory.create(
                        id = 1,
                        symbol = "AAPL",
                        type = AssetType.STOCK,
                        currentUnitValue = BigDecimal("100.00"),
                    )
                val crypto =
                    TickerFactory.create(
                        id = 2,
                        symbol = "BTC",
                        type = AssetType.CRYPTOCURRENCY,
                        currentUnitValue = BigDecimal("50000.00"),
                    )

                val apiResponse =
                    JSONObject()
                        .put(
                            "AAPL",
                            JSONObject()
                                .put("price", 150.00)
                                .put("website", "apple.com"),
                        ).put(
                            "BTC",
                            JSONObject()
                                .put("price", 55000.00),
                        )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(stock, crypto)
                coEvery { APIUtils.fetchStockPrices(any()) } returns
                    apiResponse
                coEvery { APIUtils.fetchStockLogos(any()) } returns
                    JSONObject()
                every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }

                service.updateAllNonArchivedTickersPrices(force = true)

                Then("should update stock price") {
                    stock.currentUnitValue shouldBe BigDecimal("150.00")
                }

                Then("should update crypto price") {
                    crypto.currentUnitValue.toRounded() shouldBe BigDecimal("55000.00")
                }

                Then("should update stock domain") {
                    stock.domain shouldBe "apple.com"
                }

                Then("should save all tickers") {
                    verify(exactly = 2) { tickerRepository.save(any<Ticker>()) }
                }
            }
        }

        Given("ticker with existing domain") {
            When("updating price and API returns new domain") {
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "AAPL",
                        domain = "old-apple.com",
                        currentUnitValue = BigDecimal("100.00"),
                    )

                val apiResponse =
                    JSONObject()
                        .put(
                            "AAPL",
                            JSONObject()
                                .put("price", 150.00)
                                .put("website", "apple.com"),
                        )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns listOf(ticker)
                coEvery { APIUtils.fetchStockPrices(any()) } returns apiResponse
                coEvery { APIUtils.fetchStockLogos(any()) } returns
                    JSONObject()
                every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }

                service.updateAllNonArchivedTickersPrices(force = true)

                Then("should update domain to new value") {
                    ticker.domain shouldBe "apple.com"
                }

                Then("should update price") {
                    ticker.currentUnitValue shouldBe BigDecimal("150.00")
                }
            }
        }

        Given("ticker without domain") {
            When("updating price and API does not return domain") {
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "AAPL",
                        domain = null,
                        currentUnitValue = BigDecimal("100.00"),
                    )

                val apiResponse =
                    JSONObject()
                        .put(
                            "AAPL",
                            JSONObject()
                                .put("price", 150.00),
                        )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns listOf(ticker)
                coEvery { APIUtils.fetchStockPrices(any()) } returns apiResponse
                every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }

                service.updateAllNonArchivedTickersPrices(force = true)

                Then("should keep domain as null") {
                    ticker.domain shouldBe null
                }

                Then("should update price") {
                    ticker.currentUnitValue shouldBe BigDecimal("150.00")
                }
            }
        }

        Given("empty ticker list") {
            When("updating prices for empty list") {
                val failed =
                    service.updateTickersPriceFromApi(emptyList())

                Then("should return empty failed list") {
                    failed.size shouldBe 0
                }

                Then("should not call API") {
                    coVerify(exactly = 0) { APIUtils.fetchStockPrices(any()) }
                }
            }
        }

        Given("ticker with last update timestamp") {
            When("updating price successfully") {
                val oldTimestamp = LocalDateTime.of(2026, 1, 1, 10, 0)
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "AAPL",
                        currentUnitValue = BigDecimal("100.00"),
                        lastUpdate = oldTimestamp,
                    )

                val apiResponse =
                    JSONObject()
                        .put(
                            "AAPL",
                            JSONObject()
                                .put("price", 150.00),
                        )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns listOf(ticker)
                coEvery { APIUtils.fetchStockPrices(any()) } returns apiResponse
                every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }

                service.updateAllNonArchivedTickersPrices(force = true)

                Then("should update last update timestamp") {
                    ticker.lastUpdate.isAfter(oldTimestamp) shouldBe true
                }

                Then("should update price") {
                    ticker.currentUnitValue shouldBe BigDecimal("150.00")
                }
            }
        }

        Given("all tickers fail to update") {
            When("updating prices and all tickers have errors") {
                val ticker1 =
                    TickerFactory.create(
                        id = 1,
                        symbol = "INVALID1",
                        currentUnitValue = BigDecimal("100.00"),
                    )
                val ticker2 =
                    TickerFactory.create(
                        id = 2,
                        symbol = "INVALID2",
                        currentUnitValue = BigDecimal("200.00"),
                    )

                val apiResponse =
                    JSONObject()
                        .put(
                            "INVALID1",
                            JSONObject()
                                .put("exception", "Not found"),
                        ).put(
                            "INVALID2",
                            JSONObject()
                                .put("exception", "Not found"),
                        )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker1, ticker2)
                coEvery { APIUtils.fetchStockPrices(any()) } returns
                    apiResponse
                every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }

                val failed =
                    service.updateTickersPriceFromApi(listOf(ticker1, ticker2))

                Then("should return all tickers as failed") {
                    failed.size shouldBe 2
                }

                Then("should not update any ticker prices") {
                    ticker1.currentUnitValue shouldBe BigDecimal("100.00")
                    ticker2.currentUnitValue shouldBe BigDecimal("200.00")
                }

                Then("should not save any tickers") {
                    verify(exactly = 0) { tickerRepository.save(any<Ticker>()) }
                }
            }
        }

        Given("active tickers with some failures") {
            When("updating all non-archived ticker prices with partial failures") {
                val ticker1 =
                    TickerFactory.create(
                        id = 1,
                        symbol = "AAPL",
                        currentUnitValue = BigDecimal("100.00"),
                    )
                val ticker2 =
                    TickerFactory.create(
                        id = 2,
                        symbol = "INVALID",
                        currentUnitValue = BigDecimal("200.00"),
                    )

                val apiResponse =
                    JSONObject()
                        .put(
                            "AAPL",
                            JSONObject()
                                .put("price", 150.00),
                        ).put(
                            "INVALID",
                            JSONObject()
                                .put("exception", "Ticker not found"),
                        )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker1, ticker2)
                coEvery { APIUtils.fetchStockPrices(any()) } returns
                    apiResponse
                every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }

                service.updateAllNonArchivedTickersPrices(force = true)

                Then("should log warning about failed tickers") {
                    verify(exactly = 1) { tickerRepository.save(any<Ticker>()) }
                }

                Then("should update successful ticker") {
                    ticker1.currentUnitValue shouldBe BigDecimal("150.00")
                }

                Then("should not update failed ticker") {
                    ticker2.currentUnitValue shouldBe BigDecimal("200.00")
                }
            }
        }

        Given("tickers with domains for logo download") {
            And("API returns successful logo download") {
                When("updating ticker prices with logo download") {
                    every { FileUtils.getPath(any(), any()) } returns mockk<Path>(relaxed = true)
                    every { FileUtils.exists(any()) } returns false
                    val ticker =
                        TickerFactory.create(
                            id = 1,
                            symbol = "AAPL",
                            domain = "apple.com",
                            currentUnitValue = BigDecimal("100.00"),
                        )

                    val priceResponse =
                        JSONObject()
                            .put(
                                "AAPL",
                                JSONObject()
                                    .put("price", 150.00)
                                    .put("website", "apple.com"),
                            )

                    val logoResponse =
                        JSONObject()
                            .put(
                                "apple.com",
                                JSONObject()
                                    .put("logo_path", "/path/to/logo.png"),
                            )

                    every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns listOf(ticker)
                    coEvery { APIUtils.fetchStockPrices(any()) } returns
                        priceResponse
                    coEvery { APIUtils.fetchStockLogos(any()) } returns
                        logoResponse
                    every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }

                    service.updateAllNonArchivedTickersPrices(force = true)

                    Then("should update ticker price") {
                        ticker.currentUnitValue shouldBe BigDecimal("150.00")
                    }

                    Then("should call logo download API") {
                        coVerify { APIUtils.fetchStockLogos(listOf("apple.com")) }
                    }
                }
            }

            And("API returns exception for logo download") {
                When("updating ticker prices with logo download exception") {
                    every { FileUtils.getPath(any(), any()) } returns mockk<Path>(relaxed = true)
                    every { FileUtils.exists(any()) } returns false
                    val ticker =
                        TickerFactory.create(
                            id = 1,
                            symbol = "AAPL",
                            domain = "apple.com",
                            currentUnitValue = BigDecimal("100.00"),
                        )

                    val priceResponse =
                        JSONObject()
                            .put(
                                "AAPL",
                                JSONObject()
                                    .put("price", 150.00)
                                    .put("website", "apple.com"),
                            )

                    val logoResponse =
                        JSONObject()
                            .put(
                                "apple.com",
                                JSONObject()
                                    .put("exception", "Logo not found"),
                            )

                    every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns listOf(ticker)
                    coEvery { APIUtils.fetchStockPrices(any()) } returns
                        priceResponse
                    coEvery { APIUtils.fetchStockLogos(any()) } returns
                        logoResponse
                    every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }

                    service.updateAllNonArchivedTickersPrices(force = true)

                    Then("should update ticker price despite logo exception") {
                        ticker.currentUnitValue shouldBe BigDecimal("150.00")
                    }

                    Then("should call logo download API") {
                        coVerify { APIUtils.fetchStockLogos(listOf("apple.com")) }
                    }
                }
            }

            And("logo download returns malformed JSON") {
                When("updating ticker prices with logo processing exception") {
                    every { FileUtils.getPath(any(), any()) } returns mockk<Path>(relaxed = true)
                    every { FileUtils.exists(any()) } returns false
                    val ticker =
                        TickerFactory.create(
                            id = 1,
                            symbol = "AAPL",
                            domain = "apple.com",
                            currentUnitValue = BigDecimal("100.00"),
                        )

                    val priceResponse =
                        JSONObject()
                            .put(
                                "AAPL",
                                JSONObject()
                                    .put("price", 150.00)
                                    .put("website", "apple.com"),
                            )

                    val logoResponse =
                        JSONObject()
                            .put(
                                "apple.com",
                                JSONObject(),
                            )

                    every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns listOf(ticker)
                    coEvery { APIUtils.fetchStockPrices(any()) } returns
                        priceResponse
                    coEvery { APIUtils.fetchStockLogos(any()) } returns
                        logoResponse
                    every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }

                    service.updateAllNonArchivedTickersPrices(force = true)

                    Then("should update ticker price despite logo processing issue") {
                        ticker.currentUnitValue shouldBe BigDecimal("150.00")
                    }

                    Then("should handle gracefully") {
                        verify { tickerRepository.save(any<Ticker>()) }
                    }
                }
            }
        }

        Given("tickers updated recently and tickers updated long ago") {
            val now = LocalDateTime.now()
            val fresh =
                TickerFactory.create(id = 1, symbol = "FRESH", lastUpdate = now.minusMinutes(10))
            val stale =
                TickerFactory.create(id = 2, symbol = "STALE", lastUpdate = now.minusHours(3))

            When("the automatic update runs") {
                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns listOf(fresh, stale)
                every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }
                coEvery { APIUtils.fetchStockPrices(any()) } returns
                    JSONObject().put("STALE", JSONObject().put("price", 12.0).put("currency", "BRL"))
                coEvery { APIUtils.fetchStockLogos(any()) } returns JSONObject()

                service.updateAllNonArchivedTickersPrices()

                Then("only the stale ticker is requested") {
                    coVerify(exactly = 1) { APIUtils.fetchStockPrices(listOf("STALE")) }
                }
            }

            When("the update is forced") {
                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns listOf(fresh, stale)
                every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }
                coEvery { APIUtils.fetchStockPrices(any()) } returns
                    JSONObject()
                        .put("FRESH", JSONObject().put("price", 11.0))
                        .put("STALE", JSONObject().put("price", 12.0))
                coEvery { APIUtils.fetchStockLogos(any()) } returns JSONObject()

                service.updateAllNonArchivedTickersPrices(force = true)

                Then("both are requested") {
                    coVerify(exactly = 1) {
                        APIUtils.fetchStockPrices(
                            match { args ->
                                args.map { it.substringBefore(":") } ==
                                    listOf("FRESH", "STALE")
                            },
                        )
                    }
                }
            }
        }

        Given("every ticker is fresh") {
            val fresh = TickerFactory.create(id = 1, symbol = "FRESH", lastUpdate = LocalDateTime.now().minusMinutes(5))

            When("the automatic update runs") {
                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns listOf(fresh)

                service.updateAllNonArchivedTickersPrices()

                Then("Yahoo is not asked at all") {
                    coVerify(exactly = 0) { APIUtils.fetchStockPrices(any()) }
                    verify(exactly = 0) { tickerRepository.save(any<Ticker>()) }
                }
            }
        }

        Given("a ticker whose currency is known and one whose currency is not") {
            val known = TickerFactory.create(id = 1, symbol = "KNOWN", currentUnitValue = BigDecimal("10.00"))
            known.metadata = TickerMetadata(currency = "USD").toJson()
            val unknown = TickerFactory.create(id = 2, symbol = "NEW", currentUnitValue = BigDecimal("20.00"))

            When("prices are fetched") {
                every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }
                coEvery { APIUtils.fetchStockPrices(any()) } returns
                    JSONObject()
                        .put("KNOWN", JSONObject().put("price", 55.0).put("currency", "USD"))
                        .put(
                            "NEW",
                            JSONObject()
                                .put(
                                    "price",
                                    21.0,
                                ).put("currency", "BRL")
                                .put("website", "https://new.example"),
                        )
                coEvery { APIUtils.fetchStockLogos(any()) } returns JSONObject()

                val failed = service.updateTickersPriceFromApi(listOf(known, unknown))

                Then("the known currency goes to the script and the unknown one does not") {
                    coVerify { APIUtils.fetchStockPrices(listOf("KNOWN:USD", "NEW")) }
                    failed.size shouldBe 0
                }

                Then("the currency and the site the script reported are stored with the price and the moment") {
                    TickerMetadata.fromJson(unknown.metadata).currency shouldBe "BRL"
                    unknown.domain shouldBe "https://new.example"
                    unknown.currentUnitValue shouldBe BigDecimal("21.00")
                    unknown.lastUpdate.isAfter(LocalDateTime.now().minusMinutes(1)) shouldBe true
                }
            }
        }

        Given("a script answer that carries an error and no price") {
            val unknownCurrency =
                TickerFactory.create(
                    id = 1,
                    symbol = "NOCUR",
                    currentUnitValue = BigDecimal("30.00"),
                    lastUpdate = LocalDateTime.now().minusDays(1),
                )
            val limited =
                TickerFactory.create(
                    id = 2,
                    symbol = "LIMIT",
                    currentUnitValue = BigDecimal("40.00"),
                    lastUpdate = LocalDateTime.now().minusDays(1),
                )
            val before = unknownCurrency.lastUpdate

            When("prices are fetched") {
                every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }
                coEvery { APIUtils.fetchStockPrices(any()) } returns
                    JSONObject()
                        .put(
                            "NOCUR",
                            JSONObject()
                                .put(
                                    "error",
                                    "Currency of NOCUR is unknown",
                                ).put("error_kind", "currency_unknown"),
                        ).put("LIMIT", JSONObject().put("error", "429").put("error_kind", "rate_limit"))
                coEvery { APIUtils.fetchStockLogos(any()) } returns JSONObject()

                val failed = service.updateTickersPriceFromApi(listOf(unknownCurrency, limited))

                Then("both are reported as failed and keep their old price, moment and record") {
                    failed.map { it.symbol } shouldBe listOf("NOCUR", "LIMIT")
                    unknownCurrency.currentUnitValue shouldBe BigDecimal("30.00")
                    limited.currentUnitValue shouldBe BigDecimal("40.00")
                    unknownCurrency.lastUpdate shouldBe before
                    unknownCurrency.metadata shouldBe null
                    verify(exactly = 0) { tickerRepository.save(any<Ticker>()) }
                }
            }
        }
    })
