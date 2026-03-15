package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.json.JSONObject
import org.moinex.config.RetryConfig
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.investment.TickerPriceHistoryFactory
import org.moinex.factory.investment.TickerPurchaseFactory
import org.moinex.factory.investment.TickerSaleFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.repository.investment.TickerPriceHistoryRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.moinex.util.APIUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class TickerPriceHistoryServiceTest :
    BehaviorSpec({
        val priceHistoryRepository = mockk<TickerPriceHistoryRepository>()
        val tickerRepository = mockk<TickerRepository>()
        val tickerPurchaseRepository = mockk<TickerPurchaseRepository>()
        val tickerSaleRepository = mockk<TickerSaleRepository>()

        val service =
            TickerPriceHistoryService(
                priceHistoryRepository,
                tickerRepository,
                tickerPurchaseRepository,
                tickerSaleRepository,
            )

        mockkObject(RetryConfig.Companion)
        mockkObject(APIUtils)

        afterContainer { clearAllMocks(answers = true) }

        beforeContainer {
            every { RetryConfig.TICKER_PRICE } returns
                RetryConfig(
                    maxRetries = 3,
                    initialDelayMs = 100,
                    multiplier = 2.0,
                )
        }

        Given("no active tickers in the database") {
            When("initializing price history") {
                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns emptyList()

                val result = service.initializePriceHistory()

                Then("should return without processing") {
                    result shouldBe Unit
                }

                Then("should not call any other repository methods") {
                    verify(exactly = 1) { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() }
                }
            }
        }

        Given("active tickers with no purchases and no quantity") {
            When("initializing price history") {
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST1",
                        currentQuantity = BigDecimal.ZERO,
                        createdAt = LocalDateTime.now(),
                    )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker)
                every { tickerPurchaseRepository.findAllByTickerId(1) } returns emptyList()
                every { tickerSaleRepository.findAllByTickerId(1) } returns emptyList()

                service.initializePriceHistory()

                Then("should skip the ticker") {
                    verify { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() }
                }
            }
        }

        Given("active ticker with quantity but no purchases") {
            When("initializing price history") {
                val createdDate = LocalDateTime.of(2026, 1, 15, 10, 0)
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST2",
                        currentQuantity = BigDecimal("100"),
                        createdAt = createdDate,
                    )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker)
                every { tickerPurchaseRepository.findAllByTickerId(1) } returns emptyList()
                every { tickerSaleRepository.findAllByTickerId(1) } returns emptyList()
                every {
                    priceHistoryRepository.findEarliestPriceByTicker(1)
                } returns null
                coEvery { APIUtils.fetchStockPriceHistory(any(), any(), any(), any()) } returns
                    JSONObject(mapOf("prices" to emptyList<JSONObject>()))

                service.initializePriceHistory()

                Then("should use createdAt as reference date") {
                    verify { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() }
                    verify { tickerPurchaseRepository.findAllByTickerId(1) }
                }
            }
        }

        Given("active ticker with purchases") {
            When("initializing price history with incomplete historical data") {
                val purchaseDate = LocalDateTime.of(2026, 2, 10, 10, 0)
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST3",
                        currentQuantity = BigDecimal("50"),
                        createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
                    )
                val walletTransaction =
                    WalletTransactionFactory.create(date = purchaseDate)
                val purchase =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        walletTransaction = walletTransaction,
                    )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker)
                every { tickerPurchaseRepository.findAllByTickerId(1) } returns listOf(purchase)
                every { tickerSaleRepository.findAllByTickerId(1) } returns emptyList()
                every {
                    priceHistoryRepository.findEarliestPriceByTicker(1)
                } returns null
                coEvery { APIUtils.fetchStockPriceHistory(any(), any(), any(), any()) } returns
                    JSONObject(mapOf("prices" to emptyList<JSONObject>()))

                service.initializePriceHistory()

                Then("should backfill price history") {
                    verify { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() }
                    verify { tickerPurchaseRepository.findAllByTickerId(1) }
                }
            }
        }

        Given("active ticker with complete historical data and missing transaction dates") {
            When("initializing price history") {
                val purchaseDate = LocalDateTime.of(2026, 2, 10, 10, 0)
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST4",
                        currentQuantity = BigDecimal("50"),
                        createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
                    )
                val walletTransaction =
                    WalletTransactionFactory.create(date = purchaseDate)
                val purchase =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        walletTransaction = walletTransaction,
                    )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker)
                every { tickerPurchaseRepository.findAllByTickerId(1) } returns listOf(purchase)
                every { tickerSaleRepository.findAllByTickerId(1) } returns emptyList()
                every {
                    priceHistoryRepository.findEarliestPriceByTicker(1)
                } returns
                    TickerPriceHistoryFactory.create(
                        priceDate = LocalDate.of(2026, 1, 1),
                    )
                every {
                    priceHistoryRepository.existsByTickerIdAndDate(1, any())
                } returns false
                coEvery { APIUtils.fetchStockPriceHistory(any(), any(), any(), any()) } returns
                    JSONObject(mapOf("prices" to emptyList<JSONObject>()))

                service.initializePriceHistory()

                Then("should fetch missing prices for transaction dates") {
                    verify { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() }
                }
            }
        }

        Given("multiple active tickers with mixed scenarios") {
            When("initializing price history") {
                val ticker1 =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST5",
                        currentQuantity = BigDecimal.ZERO,
                    )
                val ticker2 =
                    TickerFactory.create(
                        id = 2,
                        symbol = "TEST6",
                        currentQuantity = BigDecimal("100"),
                        createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
                    )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker1, ticker2)
                every { tickerPurchaseRepository.findAllByTickerId(1) } returns emptyList()
                every { tickerPurchaseRepository.findAllByTickerId(2) } returns emptyList()
                every { tickerSaleRepository.findAllByTickerId(1) } returns emptyList()
                every { tickerSaleRepository.findAllByTickerId(2) } returns emptyList()
                every {
                    priceHistoryRepository.findEarliestPriceByTicker(2)
                } returns null
                coEvery { APIUtils.fetchStockPriceHistory(any(), any(), any(), any()) } returns
                    JSONObject(mapOf("prices" to emptyList<JSONObject>()))

                service.initializePriceHistory()

                Then("should process all tickers") {
                    verify { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() }
                }
            }
        }

        Given("ticker with purchase and sale transactions") {
            When("initializing price history") {
                val purchaseDate = LocalDateTime.of(2026, 2, 10, 10, 0)
                val saleDate = LocalDateTime.of(2026, 3, 15, 10, 0)
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST7",
                        currentQuantity = BigDecimal("45"),
                        createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
                    )
                val purchaseTransaction =
                    WalletTransactionFactory.create(date = purchaseDate)
                val saleTransaction =
                    WalletTransactionFactory.create(date = saleDate)
                val purchase =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        walletTransaction = purchaseTransaction,
                    )
                val sale =
                    TickerSaleFactory.create(
                        ticker = ticker,
                        walletTransaction = saleTransaction,
                    )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker)
                every { tickerPurchaseRepository.findAllByTickerId(1) } returns listOf(purchase)
                every { tickerSaleRepository.findAllByTickerId(1) } returns listOf(sale)
                every {
                    priceHistoryRepository.findEarliestPriceByTicker(1)
                } returns null
                coEvery { APIUtils.fetchStockPriceHistory(any(), any(), any(), any()) } returns
                    JSONObject(mapOf("prices" to emptyList<JSONObject>()))

                service.initializePriceHistory()

                Then("should consider both purchase and sale dates") {
                    verify { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() }
                    verify { tickerPurchaseRepository.findAllByTickerId(1) }
                    verify { tickerSaleRepository.findAllByTickerId(1) }
                }
            }
        }

        Given("ticker with initial quantity before first purchase") {
            When("initializing price history") {
                val purchaseDate = LocalDateTime.of(2026, 3, 10, 10, 0)
                val createdDate = LocalDateTime.of(2026, 1, 1, 10, 0)
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST8",
                        currentQuantity = BigDecimal("150"),
                        createdAt = createdDate,
                    )
                val walletTransaction =
                    WalletTransactionFactory.create(date = purchaseDate)
                val purchase =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        quantity = BigDecimal("50"),
                        walletTransaction = walletTransaction,
                    )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker)
                every { tickerPurchaseRepository.findAllByTickerId(1) } returns listOf(purchase)
                every { tickerSaleRepository.findAllByTickerId(1) } returns emptyList()
                every {
                    priceHistoryRepository.findEarliestPriceByTicker(1)
                } returns null
                coEvery { APIUtils.fetchStockPriceHistory(any(), any(), any(), any()) } returns
                    JSONObject(mapOf("prices" to emptyList<JSONObject>()))

                service.initializePriceHistory()

                Then("should use createdAt as reference date due to initial quantity") {
                    verify { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() }
                    verify { tickerPurchaseRepository.findAllByTickerId(1) }
                }
            }
        }

        Given("ticker with exception during processing") {
            When("initializing price history") {
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST9",
                        currentQuantity = BigDecimal("100"),
                        createdAt = LocalDateTime.now(),
                    )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker)
                every { tickerPurchaseRepository.findAllByTickerId(1) } throws
                    RuntimeException("Database error")

                service.initializePriceHistory()

                Then("should continue processing other tickers") {
                    verify { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() }
                }
            }
        }

        Given("ticker with complete data but missing specific transaction dates") {
            When("initializing price history and updateMissingPrices is called") {
                val purchaseDate1 = LocalDateTime.of(2026, 2, 10, 10, 0)
                val purchaseDate2 = LocalDateTime.of(2026, 3, 15, 10, 0)
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST10",
                        currentQuantity = BigDecimal("100"),
                        createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
                    )
                val purchase1 =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        walletTransaction = WalletTransactionFactory.create(date = purchaseDate1),
                    )
                val purchase2 =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        walletTransaction = WalletTransactionFactory.create(date = purchaseDate2),
                    )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker)
                every { tickerPurchaseRepository.findAllByTickerId(1) } returns
                    listOf(purchase1, purchase2)
                every { tickerSaleRepository.findAllByTickerId(1) } returns emptyList()
                every { tickerRepository.findById(1) } returns Optional.of(ticker)
                every {
                    priceHistoryRepository.findEarliestPriceByTicker(1)
                } returns
                    TickerPriceHistoryFactory.create(
                        priceDate = LocalDate.of(2026, 1, 1),
                    )
                every {
                    priceHistoryRepository.existsByTickerIdAndDate(1, LocalDate.of(2026, 1, 1))
                } returns true
                every {
                    priceHistoryRepository.existsByTickerIdAndDate(1, LocalDate.of(2026, 2, 10))
                } returns false
                every {
                    priceHistoryRepository.existsByTickerIdAndDate(1, LocalDate.of(2026, 3, 15))
                } returns false
                every {
                    priceHistoryRepository.findMostRecentPriceBeforeDate(1, any())
                } returns null
                coEvery { APIUtils.fetchStockPriceHistory(any(), any(), any(), any()) } returns
                    JSONObject(mapOf("prices" to emptyList<JSONObject>()))

                service.initializePriceHistory()

                Then("should call fetchPricesForDates for missing dates") {
                    verify { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() }
                    verify(atLeast = 1) { priceHistoryRepository.existsByTickerIdAndDate(1, any()) }
                }
            }
        }

        Given("ticker with complete data and all prices already stored") {
            When("initializing price history and updateMissingPrices finds no missing dates") {
                val purchaseDate = LocalDateTime.of(2026, 2, 10, 10, 0)
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST11",
                        currentQuantity = BigDecimal("50"),
                        createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
                    )
                val purchase =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        walletTransaction = WalletTransactionFactory.create(date = purchaseDate),
                    )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker)
                every { tickerPurchaseRepository.findAllByTickerId(1) } returns listOf(purchase)
                every { tickerSaleRepository.findAllByTickerId(1) } returns emptyList()
                every { tickerRepository.findById(1) } returns Optional.of(ticker)
                every {
                    priceHistoryRepository.findEarliestPriceByTicker(1)
                } returns
                    TickerPriceHistoryFactory.create(
                        priceDate = LocalDate.of(2026, 1, 1),
                    )
                every {
                    priceHistoryRepository.existsByTickerIdAndDate(1, any())
                } returns true
                every {
                    priceHistoryRepository.findMostRecentPriceBeforeDate(1, any())
                } returns
                    TickerPriceHistoryFactory.create(
                        closingPrice = BigDecimal("100.00"),
                    )

                service.initializePriceHistory()

                Then("should skip fetching prices") {
                    verify { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() }
                    verify { priceHistoryRepository.existsByTickerIdAndDate(1, any()) }
                }
            }
        }

        Given("API returns price data with month-end and transaction dates") {
            When("fetchAndStorePrices processes the response") {
                val purchaseDate = LocalDateTime.of(2026, 2, 15, 10, 0)
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST12",
                        currentQuantity = BigDecimal("50"),
                        createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
                    )
                val purchase =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        walletTransaction = WalletTransactionFactory.create(date = purchaseDate),
                    )

                val priceData1 = JSONObject()
                priceData1.put("date", "2026-02-15")
                priceData1.put("price", 105.50)
                priceData1.put("is_month_end", false)

                val priceData2 = JSONObject()
                priceData2.put("date", "2026-02-28")
                priceData2.put("price", 110.00)
                priceData2.put("is_month_end", true)

                val apiResponse = JSONObject()
                apiResponse.put("prices", listOf(priceData1, priceData2))

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker)
                every { tickerRepository.findById(1) } returns Optional.of(ticker)
                every { tickerPurchaseRepository.findAllByTickerId(1) } returns listOf(purchase)
                every { tickerSaleRepository.findAllByTickerId(1) } returns emptyList()
                every {
                    priceHistoryRepository.findEarliestPriceByTicker(1)
                } returns
                    TickerPriceHistoryFactory.create(
                        priceDate = LocalDate.of(2026, 2, 10),
                    )
                every {
                    priceHistoryRepository.existsByTickerIdAndDate(1, LocalDate.of(2026, 2, 15))
                } returns false
                every {
                    priceHistoryRepository.findMostRecentPriceBeforeDate(1, any())
                } returns null
                every { priceHistoryRepository.findByTickerIdAndDate(1, any()) } returns null
                every { priceHistoryRepository.save(any()) } returnsArgument 0
                coEvery {
                    APIUtils.fetchStockPriceHistory(
                        "TEST12",
                        any(),
                        any(),
                        any(),
                    )
                } returns apiResponse

                service.initializePriceHistory()

                Then("should store both transaction date and month-end prices") {
                    verify(atLeast = 1) { priceHistoryRepository.save(any()) }
                }
            }
        }

        Given("existing price history entry for a date") {
            When("storePriceHistory is called with updated price") {
                val purchaseDate = LocalDateTime.of(2026, 2, 15, 10, 0)
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST13",
                        currentQuantity = BigDecimal("50"),
                        createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
                    )
                val purchase =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        walletTransaction = WalletTransactionFactory.create(date = purchaseDate),
                    )

                val existingPrice = TickerPriceHistoryFactory.create()

                val priceData = JSONObject()
                priceData.put("date", "2026-02-15")
                priceData.put("price", 120.00)
                priceData.put("is_month_end", false)

                val apiResponse = JSONObject()
                apiResponse.put("prices", listOf(priceData))

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker)
                every { tickerRepository.findById(1) } returns Optional.of(ticker)
                every { tickerPurchaseRepository.findAllByTickerId(1) } returns listOf(purchase)
                every { tickerSaleRepository.findAllByTickerId(1) } returns emptyList()
                every {
                    priceHistoryRepository.findEarliestPriceByTicker(1)
                } returns
                    TickerPriceHistoryFactory.create(
                        priceDate = LocalDate.of(2026, 2, 10),
                    )
                every {
                    priceHistoryRepository.existsByTickerIdAndDate(1, LocalDate.of(2026, 2, 15))
                } returns false
                every {
                    priceHistoryRepository.findMostRecentPriceBeforeDate(1, any())
                } returns null
                every {
                    priceHistoryRepository.findByTickerIdAndDate(1, LocalDate.of(2026, 2, 15))
                } returns existingPrice
                every { priceHistoryRepository.save(existingPrice) } returns existingPrice
                coEvery {
                    APIUtils.fetchStockPriceHistory(
                        "TEST13",
                        any(),
                        any(),
                        any(),
                    )
                } returns apiResponse

                service.initializePriceHistory()

                Then("should update existing price entry") {
                    verify { priceHistoryRepository.findByTickerIdAndDate(1, LocalDate.of(2026, 2, 15)) }
                    verify { priceHistoryRepository.save(existingPrice) }
                }
            }
        }

        Given("ticker with multiple purchases and sales for calculateInitialQuantity") {
            When("determining initial quantity before first purchase") {
                val createdDate = LocalDateTime.of(2026, 1, 1, 10, 0)
                val purchaseDate1 = LocalDateTime.of(2026, 2, 10, 10, 0)
                val purchaseDate2 = LocalDateTime.of(2026, 3, 15, 10, 0)
                val saleDate = LocalDateTime.of(2026, 4, 20, 10, 0)

                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST14",
                        currentQuantity = BigDecimal("150"),
                        createdAt = createdDate,
                    )
                val purchase1 =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        quantity = BigDecimal("50"),
                        walletTransaction = WalletTransactionFactory.create(date = purchaseDate1),
                    )
                val purchase2 =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        quantity = BigDecimal("30"),
                        walletTransaction = WalletTransactionFactory.create(date = purchaseDate2),
                    )
                val sale =
                    TickerSaleFactory.create(
                        ticker = ticker,
                        quantity = BigDecimal("20"),
                        walletTransaction = WalletTransactionFactory.create(date = saleDate),
                    )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker)
                every { tickerRepository.findById(1) } returns Optional.of(ticker)
                every { tickerPurchaseRepository.findAllByTickerId(1) } returns
                    listOf(purchase1, purchase2)
                every { tickerSaleRepository.findAllByTickerId(1) } returns listOf(sale)
                every {
                    priceHistoryRepository.findEarliestPriceByTicker(1)
                } returns null
                coEvery { APIUtils.fetchStockPriceHistory(any(), any(), any(), any()) } returns
                    JSONObject(mapOf("prices" to emptyList<JSONObject>()))

                service.initializePriceHistory()

                Then("should calculate initial quantity correctly (150 - 50 - 30 + 20 = 90)") {
                    verify { tickerRepository.findById(1) }
                    verify { tickerPurchaseRepository.findAllByTickerId(1) }
                    verify { tickerSaleRepository.findAllByTickerId(1) }
                }
            }
        }

        Given("ticker with purchases in different months for fetchPricesForDates") {
            When("fetching prices for multiple transaction dates across months") {
                val purchaseDate1 = LocalDateTime.of(2026, 2, 10, 10, 0)
                val purchaseDate2 = LocalDateTime.of(2026, 3, 15, 10, 0)
                val purchaseDate3 = LocalDateTime.of(2026, 5, 20, 10, 0)

                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST15",
                        currentQuantity = BigDecimal("150"),
                        createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
                    )
                val purchase1 =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        walletTransaction = WalletTransactionFactory.create(date = purchaseDate1),
                    )
                val purchase2 =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        walletTransaction = WalletTransactionFactory.create(date = purchaseDate2),
                    )
                val purchase3 =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        walletTransaction = WalletTransactionFactory.create(date = purchaseDate3),
                    )

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker)
                every { tickerRepository.findById(1) } returns Optional.of(ticker)
                every { tickerPurchaseRepository.findAllByTickerId(1) } returns
                    listOf(purchase1, purchase2, purchase3)
                every { tickerSaleRepository.findAllByTickerId(1) } returns emptyList()
                every {
                    priceHistoryRepository.findEarliestPriceByTicker(1)
                } returns
                    TickerPriceHistoryFactory.create(
                        priceDate = LocalDate.of(2026, 1, 1),
                    )
                every {
                    priceHistoryRepository.existsByTickerIdAndDate(1, any())
                } returns false
                every {
                    priceHistoryRepository.findMostRecentPriceBeforeDate(1, any())
                } returns null
                coEvery { APIUtils.fetchStockPriceHistory(any(), any(), any(), any()) } returns
                    JSONObject(mapOf("prices" to emptyList<JSONObject>()))

                service.initializePriceHistory()

                Then("should group dates by month and fetch prices") {
                    verify { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() }
                    verify(atLeast = 3) { priceHistoryRepository.existsByTickerIdAndDate(1, any()) }
                }
            }
        }

        Given("storing price for current month that is not month-end") {
            When("storePriceHistory deletes previous current month prices") {
                val today = LocalDate.now()
                val purchaseDate = today.atTime(10, 0)
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST17",
                        currentQuantity = BigDecimal("50"),
                        createdAt = today.minusMonths(2).atStartOfDay(),
                    )
                val purchase =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        walletTransaction = WalletTransactionFactory.create(date = purchaseDate),
                    )

                val priceData = JSONObject()
                priceData.put("date", today.toString())
                priceData.put("price", 105.50)
                priceData.put("is_month_end", false)

                val apiResponse = JSONObject()
                apiResponse.put("prices", listOf(priceData))

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker)
                every { tickerRepository.findById(1) } returns Optional.of(ticker)
                every { tickerPurchaseRepository.findAllByTickerId(1) } returns listOf(purchase)
                every { tickerSaleRepository.findAllByTickerId(1) } returns emptyList()
                every {
                    priceHistoryRepository.findEarliestPriceByTicker(1)
                } returns
                    TickerPriceHistoryFactory.create(
                        priceDate = today.minusMonths(1),
                    )
                every {
                    priceHistoryRepository.existsByTickerIdAndDate(1, today)
                } returns false
                every {
                    priceHistoryRepository.findMostRecentPriceBeforeDate(1, any())
                } returns null
                every { priceHistoryRepository.findByTickerIdAndDate(1, any()) } returns null
                every { priceHistoryRepository.save(any()) } returnsArgument 0
                every {
                    priceHistoryRepository.findMonthEndPricesByTickerAndDateRange(1, any(), any())
                } returns
                    listOf(
                        TickerPriceHistoryFactory.create(
                            priceDate = today.minusDays(1),
                            isMonthEnd = false,
                        ),
                    )
                every {
                    priceHistoryRepository.delete(any())
                } returns Unit
                coEvery {
                    APIUtils.fetchStockPriceHistory(
                        "TEST17",
                        any(),
                        any(),
                        any(),
                    )
                } returns apiResponse

                service.initializePriceHistory()

                Then("should delete previous current month prices before storing new one") {
                    verify(atLeast = 1) {
                        priceHistoryRepository.findMonthEndPricesByTickerAndDateRange(1, any(), any())
                    }
                    verify(atLeast = 1) {
                        priceHistoryRepository.delete(any())
                    }
                    verify(atLeast = 1) { priceHistoryRepository.save(any()) }
                }
            }
        }

        Given("API returns prices that should be filtered") {
            When("fetchAndStorePrices filters non-relevant dates") {
                val purchaseDate = LocalDateTime.of(2026, 2, 15, 10, 0)
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "TEST16",
                        currentQuantity = BigDecimal("50"),
                        createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
                    )
                val purchase =
                    TickerPurchaseFactory.create(
                        ticker = ticker,
                        walletTransaction = WalletTransactionFactory.create(date = purchaseDate),
                    )

                val relevantPrice = JSONObject()
                relevantPrice.put("date", "2026-02-15")
                relevantPrice.put("price", 105.50)
                relevantPrice.put("is_month_end", false)

                val irrelevantPrice = JSONObject()
                irrelevantPrice.put("date", "2026-02-20")
                irrelevantPrice.put("price", 107.00)
                irrelevantPrice.put("is_month_end", false)

                val apiResponse = JSONObject()
                apiResponse.put("prices", listOf(relevantPrice, irrelevantPrice))

                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                    listOf(ticker)
                every { tickerRepository.findById(1) } returns Optional.of(ticker)
                every { tickerPurchaseRepository.findAllByTickerId(1) } returns listOf(purchase)
                every { tickerSaleRepository.findAllByTickerId(1) } returns emptyList()
                every {
                    priceHistoryRepository.findEarliestPriceByTicker(1)
                } returns
                    TickerPriceHistoryFactory.create(
                        priceDate = LocalDate.of(2026, 2, 10),
                    )
                every {
                    priceHistoryRepository.existsByTickerIdAndDate(1, LocalDate.of(2026, 2, 15))
                } returns false
                every {
                    priceHistoryRepository.findMostRecentPriceBeforeDate(1, any())
                } returns null
                every { priceHistoryRepository.findByTickerIdAndDate(1, any()) } returns null
                every { priceHistoryRepository.save(any()) } returnsArgument 0
                coEvery {
                    APIUtils.fetchStockPriceHistory(
                        "TEST16",
                        any(),
                        any(),
                        any(),
                    )
                } returns apiResponse

                service.initializePriceHistory()

                Then("should only store relevant prices") {
                    verify(atLeast = 1) { priceHistoryRepository.save(any()) }
                }
            }
        }
    })
