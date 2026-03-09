package org.moinex.service.investment

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
import org.moinex.model.investment.FundamentalAnalysis
import org.moinex.repository.investment.FundamentalAnalysisRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.service.FundamentalAnalysisService
import org.moinex.util.APIUtils
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class FundamentalAnalysisServiceFetchAndSaveTest :
    BehaviorSpec({
        val fundamentalAnalysisRepository = mockk<FundamentalAnalysisRepository>()
        val tickerRepository = mockk<TickerRepository>()

        val service = FundamentalAnalysisService(fundamentalAnalysisRepository, tickerRepository)

        mockkStatic(APIUtils::class)

        afterContainer { clearAllMocks(answers = true) }

        Given("a valid ticker and period type for fetching analysis") {
            And("the analysis does not exist in the database") {
                When("API returns complete valid data") {
                    val ticker =
                        TickerFactory.create(
                            id = 1,
                            type = AssetType.STOCK,
                            name = "Apple Inc.",
                            symbol = "AAPL",
                            domain = "apple.com",
                        )

                    val apiResponse =
                        JSONObject(
                            mapOf(
                                "AAPL" to
                                    mapOf(
                                        "company_name" to "Apple Inc.",
                                        "sector" to "Technology",
                                        "industry" to "Consumer Electronics",
                                        "currency" to "USD",
                                    ),
                            ),
                        )

                    every { fundamentalAnalysisRepository.findByTickerAndPeriodType(ticker, PeriodType.ANNUAL) } returns null
                    every { APIUtils.fetchFundamentalAnalysisAsync("AAPL", PeriodType.ANNUAL) } returns
                        CompletableFuture.completedFuture(apiResponse)
                    every { fundamentalAnalysisRepository.save(any()) } answers { invocation ->
                        val analysis = invocation.invocation.args[0] as FundamentalAnalysis
                        analysis.id = 1
                        analysis
                    }

                    val result = service.fetchAndSaveAnalysis(ticker, PeriodType.ANNUAL)

                    Then("should create new analysis with API data") {
                        result.companyName shouldBe "Apple Inc."
                        result.sector shouldBe "Technology"
                        result.industry shouldBe "Consumer Electronics"
                        result.currency shouldBe "USD"
                        result.periodType shouldBe PeriodType.ANNUAL
                    }

                    Then("should save analysis to repository") {
                        verify { fundamentalAnalysisRepository.save(any()) }
                    }

                    Then("should update lastUpdate timestamp") {
                        result.lastUpdate.isBefore(LocalDateTime.now().plusSeconds(1)) shouldBe true
                    }
                }
            }

            And("the analysis already exists in the database") {
                When("API returns updated data") {
                    val ticker =
                        TickerFactory.create(
                            id = 2,
                            type = AssetType.STOCK,
                            name = "Microsoft Corporation",
                            symbol = "MSFT",
                            domain = "microsoft.com",
                        )

                    val existingAnalysis =
                        FundamentalAnalysisFactory.create(
                            id = 2,
                            ticker = ticker,
                            companyName = "Microsoft",
                            sector = "Technology",
                            industry = "Software",
                            currency = "USD",
                            periodType = PeriodType.ANNUAL,
                            lastUpdate = LocalDateTime.now().minusDays(1),
                            createdAt = LocalDateTime.now().minusDays(1),
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

                    every { fundamentalAnalysisRepository.findByTickerAndPeriodType(ticker, PeriodType.ANNUAL) } returns
                        existingAnalysis
                    every { APIUtils.fetchFundamentalAnalysisAsync("MSFT", PeriodType.ANNUAL) } returns
                        CompletableFuture.completedFuture(apiResponse)
                    every { fundamentalAnalysisRepository.save(any()) } returnsArgument 0

                    val result = service.fetchAndSaveAnalysis(ticker, PeriodType.ANNUAL)

                    Then("should update existing analysis") {
                        result.id shouldBe 2
                        result.companyName shouldBe "Microsoft Corporation"
                    }

                    Then("should save updated analysis") {
                        verify { fundamentalAnalysisRepository.save(any()) }
                    }
                }
            }
        }

        Given("API returns error for financial data not available") {
            When("fetching analysis with partial data error") {
                val ticker =
                    TickerFactory.create(
                        id = 3,
                        type = AssetType.STOCK,
                        name = "New Company",
                        symbol = "NEWCO",
                    )

                val apiResponse =
                    JSONObject(
                        mapOf(
                            "NEWCO" to
                                mapOf(
                                    "error" to "Financial data not available",
                                    "company_name" to "",
                                ),
                        ),
                    )

                every { fundamentalAnalysisRepository.findByTickerAndPeriodType(ticker, PeriodType.QUARTERLY) } returns null
                every { APIUtils.fetchFundamentalAnalysisAsync("NEWCO", PeriodType.QUARTERLY) } returns
                    CompletableFuture.completedFuture(apiResponse)
                every { fundamentalAnalysisRepository.save(any()) } answers { invocation ->
                    val analysis = invocation.invocation.args[0] as FundamentalAnalysis
                    analysis.id = 3
                    analysis
                }

                val result = service.fetchAndSaveAnalysis(ticker, PeriodType.QUARTERLY)

                Then("should still save partial data") {
                    result.periodType shouldBe PeriodType.QUARTERLY
                    verify { fundamentalAnalysisRepository.save(any()) }
                }
            }
        }

        Given("API response with missing optional fields") {
            When("API returns response without optional fields") {
                val ticker =
                    TickerFactory.create(
                        id = 5,
                        type = AssetType.STOCK,
                        name = "Minimal Company",
                        symbol = "MINI",
                    )

                val apiResponse =
                    JSONObject(
                        mapOf(
                            "MINI" to
                                mapOf(
                                    "currency" to "BRL",
                                ),
                        ),
                    )

                every { fundamentalAnalysisRepository.findByTickerAndPeriodType(ticker, PeriodType.ANNUAL) } returns null
                every { APIUtils.fetchFundamentalAnalysisAsync("MINI", PeriodType.ANNUAL) } returns
                    CompletableFuture.completedFuture(apiResponse)
                every { fundamentalAnalysisRepository.save(any()) } answers { invocation ->
                    val analysis = invocation.invocation.args[0] as FundamentalAnalysis
                    analysis.id = 5
                    analysis
                }

                val result = service.fetchAndSaveAnalysis(ticker, PeriodType.ANNUAL)

                Then("should use default values for missing fields") {
                    result.companyName shouldBe ""
                    result.sector shouldBe ""
                    result.industry shouldBe ""
                    result.currency shouldBe "BRL"
                }

                Then("should save analysis with defaults") {
                    verify { fundamentalAnalysisRepository.save(any()) }
                }
            }
        }

        Given("API response with different period types") {
            When("fetching analysis for quarterly period") {
                val ticker =
                    TickerFactory.create(
                        id = 6,
                        type = AssetType.STOCK,
                        name = "Test Company",
                        symbol = "TEST",
                    )

                val apiResponse =
                    JSONObject(
                        mapOf(
                            "TEST" to
                                mapOf(
                                    "company_name" to "Test Company",
                                    "sector" to "Technology",
                                    "industry" to "Software",
                                    "currency" to "USD",
                                ),
                        ),
                    )

                every { fundamentalAnalysisRepository.findByTickerAndPeriodType(ticker, PeriodType.QUARTERLY) } returns null
                every { APIUtils.fetchFundamentalAnalysisAsync("TEST", PeriodType.QUARTERLY) } returns
                    CompletableFuture.completedFuture(apiResponse)
                every { fundamentalAnalysisRepository.save(any()) } answers { invocation ->
                    val analysis = invocation.invocation.args[0] as FundamentalAnalysis
                    analysis.id = 6
                    analysis
                }

                val result = service.fetchAndSaveAnalysis(ticker, PeriodType.QUARTERLY)

                Then("should save analysis with correct period type") {
                    result.periodType shouldBe PeriodType.QUARTERLY
                }
            }
        }

        Given("API response with currency conversion") {
            When("API returns data with different currency") {
                val ticker =
                    TickerFactory.create(
                        id = 8,
                        type = AssetType.STOCK,
                        name = "International Company",
                        symbol = "INTL",
                    )

                val apiResponse =
                    JSONObject(
                        mapOf(
                            "INTL" to
                                mapOf(
                                    "company_name" to "International Company",
                                    "sector" to "Technology",
                                    "industry" to "Software",
                                    "currency" to "EUR",
                                ),
                        ),
                    )

                every { fundamentalAnalysisRepository.findByTickerAndPeriodType(ticker, PeriodType.ANNUAL) } returns null
                every { APIUtils.fetchFundamentalAnalysisAsync("INTL", PeriodType.ANNUAL) } returns
                    CompletableFuture.completedFuture(apiResponse)
                every { fundamentalAnalysisRepository.save(any()) } answers { invocation ->
                    val analysis = invocation.invocation.args[0] as FundamentalAnalysis
                    analysis.id = 8
                    analysis
                }

                val result = service.fetchAndSaveAnalysis(ticker, PeriodType.ANNUAL)

                Then("should preserve currency from API response") {
                    result.currency shouldBe "EUR"
                }
            }
        }

        Given("API response with JSON data preservation") {
            When("fetching analysis with complex JSON data") {
                val ticker =
                    TickerFactory.create(
                        id = 9,
                        type = AssetType.STOCK,
                        name = "Complex Company",
                        symbol = "CMPLX",
                    )

                val apiResponse =
                    JSONObject(
                        mapOf(
                            "CMPLX" to
                                mapOf(
                                    "company_name" to "Complex Company",
                                    "sector" to "Technology",
                                    "industry" to "Software",
                                    "currency" to "USD",
                                    "extra_field" to "extra_value",
                                ),
                        ),
                    )

                every { fundamentalAnalysisRepository.findByTickerAndPeriodType(ticker, PeriodType.ANNUAL) } returns null
                every { APIUtils.fetchFundamentalAnalysisAsync("CMPLX", PeriodType.ANNUAL) } returns
                    CompletableFuture.completedFuture(apiResponse)
                every { fundamentalAnalysisRepository.save(any()) } answers { invocation ->
                    val analysis = invocation.invocation.args[0] as FundamentalAnalysis
                    analysis.id = 9
                    analysis
                }

                val result = service.fetchAndSaveAnalysis(ticker, PeriodType.ANNUAL)

                Then("should preserve complete JSON data") {
                    result.dataJson.contains("extra_field") shouldBe true
                    result.dataJson.contains("extra_value") shouldBe true
                }
            }
        }
    })
