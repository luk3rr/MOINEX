package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.json.JSONObject
import org.moinex.factory.investment.FundamentalAnalysisFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.model.enums.AssetType
import org.moinex.model.enums.PeriodType
import org.moinex.repository.investment.FundamentalAnalysisRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.service.FundamentalAnalysisService
import org.moinex.util.APIUtils
import java.time.LocalDateTime
import java.util.Optional
import java.util.concurrent.CompletableFuture

class FundamentalAnalysisServiceGetAnalysisTest :
    BehaviorSpec({
        val fundamentalAnalysisRepository = mockk<FundamentalAnalysisRepository>()
        val tickerRepository = mockk<TickerRepository>()

        val service = FundamentalAnalysisService(fundamentalAnalysisRepository, tickerRepository)

        mockkStatic(APIUtils::class)

        afterContainer { clearAllMocks(answers = true) }

        Given("a valid ticker with cached analysis") {
            When("getting analysis without force refresh and cache is not expired") {
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        type = AssetType.STOCK,
                        name = "Apple Inc.",
                        symbol = "AAPL",
                    )

                val cachedAnalysis =
                    FundamentalAnalysisFactory.create(
                        id = 1,
                        ticker = ticker,
                        periodType = PeriodType.ANNUAL,
                        lastUpdate = LocalDateTime.now().minusHours(12),
                    )

                every { tickerRepository.findById(1) } returns Optional.of(ticker)
                every { fundamentalAnalysisRepository.findByTickerAndPeriodType(ticker, PeriodType.ANNUAL) } returns
                    cachedAnalysis

                val result = service.getAnalysis(1, PeriodType.ANNUAL, forceRefresh = false)

                Then("should return cached analysis") {
                    result.id shouldBe 1
                    result.ticker.symbol shouldBe "AAPL"
                }

                Then("should not fetch new data from API") {
                    verify(exactly = 0) { APIUtils.fetchFundamentalAnalysisAsync(any(), any()) }
                }
            }
        }

        Given("a valid ticker with expired cached analysis") {
            When("getting analysis without force refresh and cache is expired") {
                val ticker =
                    TickerFactory.create(
                        id = 2,
                        type = AssetType.STOCK,
                        name = "Microsoft Corporation",
                        symbol = "MSFT",
                    )

                val expiredAnalysis =
                    FundamentalAnalysisFactory.create(
                        id = 2,
                        ticker = ticker,
                        periodType = PeriodType.ANNUAL,
                        lastUpdate = LocalDateTime.now().minusDays(10),
                    )

                val apiResponse =
                    JSONObject(
                        mapOf(
                            "MSFT" to
                                mapOf(
                                    "company_name" to "Microsoft Corporation",
                                    "sector" to "Technology",
                                    "industry" to "Software",
                                    "currency" to "USD",
                                ),
                        ),
                    )

                val updatedAnalysis =
                    FundamentalAnalysisFactory.create(
                        id = 2,
                        ticker = ticker,
                        companyName = "Microsoft Corporation",
                        periodType = PeriodType.ANNUAL,
                        lastUpdate = LocalDateTime.now(),
                    )

                every { tickerRepository.findById(2) } returns Optional.of(ticker)
                every { fundamentalAnalysisRepository.findByTickerAndPeriodType(ticker, PeriodType.ANNUAL) } returns
                    expiredAnalysis
                every { APIUtils.fetchFundamentalAnalysisAsync("MSFT", PeriodType.ANNUAL) } returns
                    CompletableFuture.completedFuture(apiResponse)
                every { fundamentalAnalysisRepository.save(any()) } returns updatedAnalysis

                val result = service.getAnalysis(2, PeriodType.ANNUAL, forceRefresh = false)

                Then("should fetch new data from API") {
                    result.companyName shouldBe "Microsoft Corporation"
                }

                Then("should call API") {
                    verify { APIUtils.fetchFundamentalAnalysisAsync("MSFT", PeriodType.ANNUAL) }
                }
            }
        }

        Given("a valid ticker with force refresh enabled") {
            When("getting analysis with forceRefresh = true") {
                val ticker =
                    TickerFactory.create(
                        id = 3,
                        type = AssetType.STOCK,
                        name = "Google Inc.",
                        symbol = "GOOGL",
                    )

                val cachedAnalysis =
                    FundamentalAnalysisFactory.create(
                        id = 3,
                        ticker = ticker,
                        periodType = PeriodType.QUARTERLY,
                        lastUpdate = LocalDateTime.now().minusHours(1),
                    )

                val apiResponse =
                    JSONObject(
                        mapOf(
                            "GOOGL" to
                                mapOf(
                                    "company_name" to "Alphabet Inc.",
                                    "sector" to "Technology",
                                    "industry" to "Internet Services",
                                    "currency" to "USD",
                                ),
                        ),
                    )

                val updatedAnalysis =
                    FundamentalAnalysisFactory.create(
                        id = 3,
                        ticker = ticker,
                        companyName = "Alphabet Inc.",
                        periodType = PeriodType.QUARTERLY,
                        lastUpdate = LocalDateTime.now(),
                    )

                every { tickerRepository.findById(3) } returns Optional.of(ticker)
                every { fundamentalAnalysisRepository.findByTickerAndPeriodType(ticker, PeriodType.QUARTERLY) } returns
                    cachedAnalysis
                every { APIUtils.fetchFundamentalAnalysisAsync("GOOGL", PeriodType.QUARTERLY) } returns
                    CompletableFuture.completedFuture(apiResponse)
                every { fundamentalAnalysisRepository.save(any()) } returns updatedAnalysis

                val result = service.getAnalysis(3, PeriodType.QUARTERLY, forceRefresh = true)

                Then("should fetch new data regardless of cache") {
                    result.companyName shouldBe "Alphabet Inc."
                }

                Then("should call API even with fresh cache") {
                    verify { APIUtils.fetchFundamentalAnalysisAsync("GOOGL", PeriodType.QUARTERLY) }
                }
            }
        }

        Given("an archived ticker") {
            When("getting analysis for archived ticker") {
                val ticker =
                    TickerFactory.create(
                        id = 4,
                        type = AssetType.STOCK,
                        name = "Tesla Inc.",
                        symbol = "TSLA",
                        isArchived = true,
                    )

                every { tickerRepository.findById(4) } returns Optional.of(ticker)

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.getAnalysis(4, PeriodType.ANNUAL)
                    }
                }

                Then("should not fetch analysis from repository") {
                    verify(exactly = 0) { fundamentalAnalysisRepository.findByTickerAndPeriodType(any(), any()) }
                }
            }
        }

        Given("a ticker with invalid type for fundamental analysis") {
            When("getting analysis for BOND ticker") {
                val ticker =
                    TickerFactory.create(
                        id = 5,
                        type = AssetType.BOND,
                        name = "Bond 001",
                        symbol = "BOND001",
                    )

                every { tickerRepository.findById(5) } returns Optional.of(ticker)

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.getAnalysis(5, PeriodType.ANNUAL)
                    }
                }
            }

            When("getting analysis for CRYPTOCURRENCY ticker") {
                val ticker =
                    TickerFactory.create(
                        id = 6,
                        type = AssetType.CRYPTOCURRENCY,
                        name = "Bitcoin",
                        symbol = "BTC",
                    )

                every { tickerRepository.findById(6) } returns Optional.of(ticker)

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.getAnalysis(6, PeriodType.ANNUAL)
                    }
                }
            }
        }

        Given("a non-existent ticker") {
            When("getting analysis for non-existent ticker") {
                every { tickerRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<jakarta.persistence.EntityNotFoundException> {
                        service.getAnalysis(999, PeriodType.ANNUAL)
                    }
                }
            }
        }

        Given("a REIT ticker (valid for fundamental analysis)") {
            When("getting analysis for REIT ticker") {
                val ticker =
                    TickerFactory.create(
                        id = 7,
                        type = AssetType.REIT,
                        name = "Real Estate Investment Trust",
                        symbol = "REIT001",
                    )

                val analysis =
                    FundamentalAnalysisFactory.create(
                        id = 7,
                        ticker = ticker,
                        periodType = PeriodType.ANNUAL,
                        lastUpdate = LocalDateTime.now().minusHours(12),
                    )

                every { tickerRepository.findById(7) } returns Optional.of(ticker)
                every { fundamentalAnalysisRepository.findByTickerAndPeriodType(ticker, PeriodType.ANNUAL) } returns
                    analysis

                val result = service.getAnalysis(7, PeriodType.ANNUAL)

                Then("should return analysis for REIT") {
                    result.id shouldBe 7
                    result.ticker.type shouldBe AssetType.REIT
                }
            }
        }

        Given("a ticker with no cached analysis") {
            When("getting analysis for ticker without cache") {
                val ticker =
                    TickerFactory.create(
                        id = 8,
                        type = AssetType.STOCK,
                        name = "New Company",
                        symbol = "NEWCO",
                    )

                val apiResponse =
                    JSONObject(
                        mapOf(
                            "NEWCO" to
                                mapOf(
                                    "company_name" to "New Company Inc.",
                                    "sector" to "Technology",
                                    "industry" to "Software",
                                    "currency" to "USD",
                                ),
                        ),
                    )

                val newAnalysis =
                    FundamentalAnalysisFactory.create(
                        id = 8,
                        ticker = ticker,
                        companyName = "New Company Inc.",
                        periodType = PeriodType.ANNUAL,
                    )

                every { tickerRepository.findById(8) } returns Optional.of(ticker)
                every { fundamentalAnalysisRepository.findByTickerAndPeriodType(ticker, PeriodType.ANNUAL) } returns null
                every { APIUtils.fetchFundamentalAnalysisAsync("NEWCO", PeriodType.ANNUAL) } returns
                    CompletableFuture.completedFuture(apiResponse)
                every { fundamentalAnalysisRepository.save(any()) } returns newAnalysis

                val result = service.getAnalysis(8, PeriodType.ANNUAL)

                Then("should fetch and save new analysis") {
                    result.companyName shouldBe "New Company Inc."
                    verify { fundamentalAnalysisRepository.save(any()) }
                }
            }
        }

        Given("API fails on first attempt but succeeds on retry") {
            When("getting analysis and API recovers after first failure") {
                val ticker =
                    TickerFactory.create(
                        id = 11,
                        type = AssetType.STOCK,
                        name = "Retry Company",
                        symbol = "RETRY",
                    )

                val successResponse =
                    JSONObject(
                        mapOf(
                            "RETRY" to
                                mapOf(
                                    "company_name" to "Retry Company Inc.",
                                    "sector" to "Technology",
                                    "industry" to "Software",
                                    "currency" to "USD",
                                ),
                        ),
                    )

                val savedAnalysis =
                    FundamentalAnalysisFactory.create(
                        id = 11,
                        ticker = ticker,
                        companyName = "Retry Company Inc.",
                        periodType = PeriodType.ANNUAL,
                    )

                every { tickerRepository.findById(11) } returns Optional.of(ticker)
                every { fundamentalAnalysisRepository.findByTickerAndPeriodType(ticker, PeriodType.ANNUAL) } returns null
                every { APIUtils.fetchFundamentalAnalysisAsync("RETRY", PeriodType.ANNUAL) } returnsMany
                    listOf(
                        CompletableFuture.failedFuture(RuntimeException("Temporary failure")),
                        CompletableFuture.completedFuture(successResponse),
                    )
                every { fundamentalAnalysisRepository.save(any()) } returns savedAnalysis

                val result = service.getAnalysis(11, PeriodType.ANNUAL)

                Then("should succeed after retry") {
                    result.companyName shouldBe "Retry Company Inc."
                    verify { fundamentalAnalysisRepository.save(any()) }
                }
            }
        }

        Given("different period types") {
            When("getting analysis for QUARTERLY period") {
                val ticker =
                    TickerFactory.create(
                        id = 9,
                        type = AssetType.STOCK,
                        name = "Test Company",
                        symbol = "TEST",
                    )

                val analysis =
                    FundamentalAnalysisFactory.create(
                        id = 9,
                        ticker = ticker,
                        periodType = PeriodType.QUARTERLY,
                        lastUpdate = LocalDateTime.now().minusHours(12),
                    )

                every { tickerRepository.findById(9) } returns Optional.of(ticker)
                every { fundamentalAnalysisRepository.findByTickerAndPeriodType(ticker, PeriodType.QUARTERLY) } returns
                    analysis

                val result = service.getAnalysis(9, PeriodType.QUARTERLY)

                Then("should return analysis with correct period type") {
                    result.periodType shouldBe PeriodType.QUARTERLY
                }
            }
        }
    })
