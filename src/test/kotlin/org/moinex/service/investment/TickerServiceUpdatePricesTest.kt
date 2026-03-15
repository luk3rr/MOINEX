package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.json.JSONObject
import org.moinex.common.extension.toRounded
import org.moinex.config.RetryConfig
import org.moinex.factory.investment.TickerFactory
import org.moinex.model.enums.AssetType
import org.moinex.model.investment.Ticker
import org.moinex.repository.investment.CryptoExchangeRepository
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.moinex.service.wallet.WalletService
import org.moinex.util.APIUtils
import org.moinex.util.FileUtils
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

        val service =
            TickerService(
                tickerRepository,
                tickerPurchaseRepository,
                tickerSaleRepository,
                dividendRepository,
                cryptoExchangeRepository,
                walletService,
            )

        mockkObject(APIUtils)
        mockkObject(RetryConfig.Companion)
        mockkObject(FileUtils)

        afterContainer {
            clearAllMocks(answers = true)
        }

        beforeContainer {
            every { RetryConfig.TICKER_PRICE } returns
                RetryConfig(
                    maxRetries = 3,
                    initialDelayMs = 100,
                    multiplier = 2.0,
                )
            every { RetryConfig.TICKER_LOGO } returns
                RetryConfig(
                    maxRetries = 3,
                    initialDelayMs = 100,
                    multiplier = 2.0,
                )
        }

        Given("no active tickers in the database") {
            When("updating all non-archived ticker prices") {
                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns emptyList()

                service.updateAllNonArchivedTickersPrices()

                Then("should skip price update") {
                    verify(exactly = 1) { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() }
                    verify(exactly = 0) { tickerRepository.saveAll(any<List<Ticker>>()) }
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
                    coEvery { APIUtils.fetchStockPrices(listOf("AAPL", "GOOGL")) } returns apiResponse
                    every { tickerRepository.saveAll(any<List<Ticker>>()) } returns
                        listOf(ticker1, ticker2)

                    service.updateAllNonArchivedTickersPrices()

                    Then("should update ticker prices") {
                        ticker1.currentUnitValue shouldBe BigDecimal("150.00")
                        ticker2.currentUnitValue shouldBe BigDecimal("250.00")
                    }

                    Then("should update ticker domains") {
                        ticker1.domain shouldBe "apple.com"
                        ticker2.domain shouldBe "google.com"
                    }

                    Then("should save all updated tickers") {
                        verify { tickerRepository.saveAll(any<List<Ticker>>()) }
                    }
                }
            }

            And("API returns error for some tickers") {
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
                                    .put("error", "Ticker not found"),
                            )

                    every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                        listOf(ticker1, ticker2)
                    coEvery { APIUtils.fetchStockPrices(listOf("AAPL", "INVALID")) } returns apiResponse
                    every { tickerRepository.saveAll(any<List<Ticker>>()) } returns
                        listOf(ticker1)

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
                        verify { tickerRepository.saveAll(match<List<Ticker>> { it.size == 1 }) }
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
                    coEvery { APIUtils.fetchStockPrices(listOf("AAPL")) } returns apiResponse
                    every { tickerRepository.saveAll(any<List<Ticker>>()) } returns emptyList()

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
                        verify(exactly = 0) { tickerRepository.saveAll(any<List<Ticker>>()) }
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
                coEvery { APIUtils.fetchStockPrices(listOf("AAPL", "BTC")) } returns apiResponse
                every { tickerRepository.saveAll(any<List<Ticker>>()) } returns
                    listOf(stock, crypto)

                service.updateAllNonArchivedTickersPrices()

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
                    verify { tickerRepository.saveAll(match<List<Ticker>> { it.size == 2 }) }
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
                coEvery { APIUtils.fetchStockPrices(listOf("AAPL")) } returns apiResponse
                every { tickerRepository.saveAll(any<List<Ticker>>()) } returns
                    listOf(ticker)

                service.updateAllNonArchivedTickersPrices()

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
                coEvery { APIUtils.fetchStockPrices(listOf("AAPL")) } returns apiResponse
                every { tickerRepository.saveAll(any<List<Ticker>>()) } returns
                    listOf(ticker)

                service.updateAllNonArchivedTickersPrices()

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
                coEvery { APIUtils.fetchStockPrices(listOf("AAPL")) } returns apiResponse
                every { tickerRepository.saveAll(any<List<Ticker>>()) } returns
                    listOf(ticker)

                service.updateAllNonArchivedTickersPrices()

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
                                .put("error", "Not found"),
                        ).put(
                            "INVALID2",
                            JSONObject()
                                .put("error", "Not found"),
                        )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker1, ticker2)
                coEvery { APIUtils.fetchStockPrices(listOf("INVALID1", "INVALID2")) } returns apiResponse
                every { tickerRepository.saveAll(any<List<Ticker>>()) } returns emptyList()

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
                    verify(exactly = 0) { tickerRepository.saveAll(any<List<Ticker>>()) }
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
                                .put("error", "Ticker not found"),
                        )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker1, ticker2)
                coEvery { APIUtils.fetchStockPrices(listOf("AAPL", "INVALID")) } returns apiResponse
                every { tickerRepository.saveAll(any<List<Ticker>>()) } returns listOf(ticker1)

                service.updateAllNonArchivedTickersPrices()

                Then("should log warning about failed tickers") {
                    verify { tickerRepository.saveAll(match<List<Ticker>> { it.size == 1 }) }
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
                    coEvery { APIUtils.fetchStockPrices(listOf("AAPL")) } returns priceResponse
                    coEvery { APIUtils.fetchStockLogos(listOf("apple.com")) } returns logoResponse
                    every { tickerRepository.saveAll(any<List<Ticker>>()) } returns listOf(ticker)

                    service.updateAllNonArchivedTickersPrices()

                    Then("should update ticker price") {
                        ticker.currentUnitValue shouldBe BigDecimal("150.00")
                    }

                    Then("should call logo download API") {
                        coVerify { APIUtils.fetchStockLogos(listOf("apple.com")) }
                    }
                }
            }

            And("API returns error for logo download") {
                When("updating ticker prices with logo download error") {
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
                                    .put("error", "Logo not found"),
                            )

                    every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns listOf(ticker)
                    coEvery { APIUtils.fetchStockPrices(listOf("AAPL")) } returns priceResponse
                    coEvery { APIUtils.fetchStockLogos(listOf("apple.com")) } returns logoResponse
                    every { tickerRepository.saveAll(any<List<Ticker>>()) } returns listOf(ticker)

                    service.updateAllNonArchivedTickersPrices()

                    Then("should update ticker price despite logo error") {
                        ticker.currentUnitValue shouldBe BigDecimal("150.00")
                    }

                    Then("should call logo download API") {
                        coVerify { APIUtils.fetchStockLogos(listOf("apple.com")) }
                    }
                }
            }

            And("logo download returns malformed JSON") {
                When("updating ticker prices with logo processing error") {
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
                    coEvery { APIUtils.fetchStockPrices(listOf("AAPL")) } returns priceResponse
                    coEvery { APIUtils.fetchStockLogos(listOf("apple.com")) } returns logoResponse
                    every { tickerRepository.saveAll(any<List<Ticker>>()) } returns listOf(ticker)

                    service.updateAllNonArchivedTickersPrices()

                    Then("should update ticker price despite logo processing issue") {
                        ticker.currentUnitValue shouldBe BigDecimal("150.00")
                    }

                    Then("should handle gracefully") {
                        verify { tickerRepository.saveAll(any<List<Ticker>>()) }
                    }
                }
            }
        }
    })
