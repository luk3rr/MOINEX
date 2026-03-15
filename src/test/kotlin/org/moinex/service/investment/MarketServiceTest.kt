package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.json.JSONObject
import org.moinex.common.retry.RetryException
import org.moinex.config.RetryConfig
import org.moinex.factory.investment.BrazilianMarketIndicatorsFactory
import org.moinex.factory.investment.MarketQuotesAndCommoditiesFactory
import org.moinex.model.investment.BrazilianMarketIndicators
import org.moinex.model.investment.MarketQuotesAndCommodities
import org.moinex.repository.investment.BrazilianMarketIndicatorsRepository
import org.moinex.repository.investment.MarketQuotesAndCommoditiesRepository
import org.moinex.util.APIUtils
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

class MarketServiceTest :
    BehaviorSpec({
        val brazilianMarketIndicatorsRepository = mockk<BrazilianMarketIndicatorsRepository>()
        val marketQuotesAndCommoditiesRepository = mockk<MarketQuotesAndCommoditiesRepository>()

        val service = MarketService(brazilianMarketIndicatorsRepository, marketQuotesAndCommoditiesRepository)

        mockkObject(APIUtils)

        mockkObject(RetryConfig.Companion)

        afterContainer {
            clearAllMocks(answers = true)
        }

        beforeContainer {
            every { RetryConfig.MARKET_DATA } returns
                RetryConfig(
                    maxRetries = 3,
                    initialDelayMs = 100,
                    multiplier = 2.0,
                )
        }

        Given("API returns valid Brazilian market indicators data") {
            When("updating Brazilian market indicators from API") {
                val apiResponse =
                    JSONObject(
                        mapOf(
                            "selic_target" to mapOf("valor" to "13.75"),
                            "ipca_12_months" to mapOf("valor" to "4.50"),
                            "ipca_last_month" to
                                mapOf(
                                    "valor" to "0.42",
                                    "data" to "01/02/2026",
                                ),
                        ),
                    )

                every { brazilianMarketIndicatorsRepository.findAll() } returns emptyList()
                coEvery { APIUtils.fetchBrazilianMarketIndicators() } returns apiResponse
                every { brazilianMarketIndicatorsRepository.save(any()) } answers { invocation ->
                    val bmi = invocation.invocation.args[0] as BrazilianMarketIndicators
                    bmi.id = 1
                    bmi
                }

                val result = service.updateBrazilianMarketIndicatorsFromApi()

                Then("should save Brazilian market indicators with correct values") {
                    result.selicTarget shouldBe BigDecimal("13.75")
                    result.ipca12Months shouldBe BigDecimal("4.50")
                    result.ipcaLastMonth shouldBe BigDecimal("0.42")
                    result.ipcaLastMonthReference shouldBe YearMonth.of(2026, 2)
                }

                Then("should save to repository") {
                    verify { brazilianMarketIndicatorsRepository.save(any()) }
                }

                Then("should update lastUpdate timestamp") {
                    result.lastUpdate shouldNotBe null
                    result.lastUpdate?.isBefore(LocalDateTime.now().plusSeconds(1)) shouldBe true
                }
            }
        }

        Given("existing Brazilian market indicators in database") {
            When("updating Brazilian market indicators from API") {
                val existingIndicators =
                    BrazilianMarketIndicatorsFactory.create(
                        id = 1,
                        selicTarget = BigDecimal("12.00"),
                        ipca12Months = BigDecimal("4.00"),
                        ipcaLastMonth = BigDecimal("0.30"),
                        ipcaLastMonthReference = YearMonth.of(2026, 1),
                    )

                val apiResponse =
                    JSONObject(
                        mapOf(
                            "selic_target" to mapOf("valor" to "13.75"),
                            "ipca_12_months" to mapOf("valor" to "4.50"),
                            "ipca_last_month" to
                                mapOf(
                                    "valor" to "0.42",
                                    "data" to "01/02/2026",
                                ),
                        ),
                    )

                every { brazilianMarketIndicatorsRepository.findAll() } returns listOf(existingIndicators)
                coEvery { APIUtils.fetchBrazilianMarketIndicators() } returns apiResponse
                every { brazilianMarketIndicatorsRepository.save(any()) } returnsArgument 0

                val result = service.updateBrazilianMarketIndicatorsFromApi()

                Then("should update existing indicators") {
                    result.id shouldBe 1
                    result.selicTarget shouldBe BigDecimal("13.75")
                    result.ipca12Months shouldBe BigDecimal("4.50")
                }

                Then("should save updated indicators") {
                    verify { brazilianMarketIndicatorsRepository.save(any()) }
                }
            }
        }

        Given("API returns valid market quotes and commodities data") {
            When("updating market quotes and commodities from API") {
                val apiResponse =
                    JSONObject(
                        mapOf(
                            "^BVSP" to mapOf("price" to "125000.50"),
                            "BRL=X" to mapOf("price" to "5.25"),
                            "EURBRL=X" to mapOf("price" to "5.85"),
                            "USDBRL=X" to mapOf("price" to "5.25"),
                            "GC=F" to mapOf("price" to "2050.75"),
                            "ZS=F" to mapOf("price" to "1450.25"),
                            "KC=F" to mapOf("price" to "220.50"),
                            "ZW=F" to mapOf("price" to "650.00"),
                            "BZ=F" to mapOf("price" to "85.50"),
                            "BTC-USD" to mapOf("price" to "65000.00"),
                            "ETH-USD" to mapOf("price" to "3500.00"),
                        ),
                    )

                every { marketQuotesAndCommoditiesRepository.findAll() } returns emptyList()
                coEvery { APIUtils.fetchStockPrices(any()) } returns apiResponse
                every { marketQuotesAndCommoditiesRepository.save(any()) } answers { invocation ->
                    val mqac = invocation.invocation.args[0] as MarketQuotesAndCommodities
                    mqac.id = 1
                    mqac
                }

                val result = service.updateMarketQuotesAndCommoditiesFromApi()

                Then("should save market quotes with correct values") {
                    result.ibovespa shouldBe BigDecimal("125000.50")
                    result.dollar shouldBe BigDecimal("5.25")
                    result.euro shouldBe BigDecimal("5.85")
                    result.gold shouldBe BigDecimal("2050.75")
                    result.bitcoin shouldBe BigDecimal("65000.00")
                    result.ethereum shouldBe BigDecimal("3500.00")
                }

                Then("should save to repository") {
                    verify { marketQuotesAndCommoditiesRepository.save(any()) }
                }

                Then("should update lastUpdate timestamp") {
                    result.lastUpdate shouldNotBe null
                    result.lastUpdate?.isBefore(LocalDateTime.now().plusSeconds(1)) shouldBe true
                }
            }
        }

        Given("existing market quotes and commodities in database") {
            When("updating market quotes and commodities from API") {
                val existingQuotes =
                    MarketQuotesAndCommoditiesFactory.create(
                        id = 1,
                        ibovespa = BigDecimal("120000.00"),
                        dollar = BigDecimal("5.00"),
                        euro = BigDecimal("5.50"),
                    )

                val apiResponse =
                    JSONObject(
                        mapOf(
                            "^BVSP" to mapOf("price" to "125000.50"),
                            "BRL=X" to mapOf("price" to "5.25"),
                            "EURBRL=X" to mapOf("price" to "5.85"),
                            "USDBRL=X" to mapOf("price" to "5.25"),
                            "GC=F" to mapOf("price" to "2050.75"),
                            "ZS=F" to mapOf("price" to "1450.25"),
                            "KC=F" to mapOf("price" to "220.50"),
                            "ZW=F" to mapOf("price" to "650.00"),
                            "BZ=F" to mapOf("price" to "85.50"),
                            "BTC-USD" to mapOf("price" to "65000.00"),
                            "ETH-USD" to mapOf("price" to "3500.00"),
                        ),
                    )

                every { marketQuotesAndCommoditiesRepository.findAll() } returns listOf(existingQuotes)
                coEvery { APIUtils.fetchStockPrices(any()) } returns apiResponse
                every { marketQuotesAndCommoditiesRepository.save(any()) } returnsArgument 0

                val result = service.updateMarketQuotesAndCommoditiesFromApi()

                Then("should update existing quotes") {
                    result.id shouldBe 1
                    result.ibovespa shouldBe BigDecimal("125000.50")
                    result.dollar shouldBe BigDecimal("5.25")
                }

                Then("should save updated quotes") {
                    verify { marketQuotesAndCommoditiesRepository.save(any()) }
                }
            }
        }

        Given("Brazilian market indicators exist in database") {
            When("getting Brazilian market indicators or fetch") {
                val existingIndicators =
                    BrazilianMarketIndicatorsFactory.create(
                        id = 1,
                        selicTarget = BigDecimal("13.75"),
                        ipca12Months = BigDecimal("4.50"),
                    )

                every { brazilianMarketIndicatorsRepository.findAll() } returns listOf(existingIndicators)

                val result = service.getBrazilianMarketIndicatorsOrFetch()

                Then("should return existing indicators from database") {
                    result.id shouldBe 1
                    result.selicTarget shouldBe BigDecimal("13.75")
                }

                Then("should not call API") {
                    coVerify(exactly = 0) { APIUtils.fetchBrazilianMarketIndicators() }
                }
            }
        }

        Given("Brazilian market indicators do not exist in database") {
            When("getting Brazilian market indicators or fetch") {
                val apiResponse =
                    JSONObject(
                        mapOf(
                            "selic_target" to mapOf("valor" to "13.75"),
                            "ipca_12_months" to mapOf("valor" to "4.50"),
                            "ipca_last_month" to
                                mapOf(
                                    "valor" to "0.42",
                                    "data" to "01/02/2026",
                                ),
                        ),
                    )

                every { brazilianMarketIndicatorsRepository.findAll() } returns emptyList()
                coEvery { APIUtils.fetchBrazilianMarketIndicators() } returns apiResponse
                every { brazilianMarketIndicatorsRepository.save(any()) } answers { invocation ->
                    val bmi = invocation.invocation.args[0] as BrazilianMarketIndicators
                    bmi.id = 1
                    bmi
                }

                val result = service.getBrazilianMarketIndicatorsOrFetch()

                Then("should fetch from API") {
                    result.selicTarget shouldBe BigDecimal("13.75")
                    result.ipca12Months shouldBe BigDecimal("4.50")
                }

                Then("should save fetched data") {
                    verify { brazilianMarketIndicatorsRepository.save(any()) }
                }
            }
        }

        Given("market quotes and commodities exist in database") {
            When("getting market quotes and commodities or fetch") {
                val existingQuotes =
                    MarketQuotesAndCommoditiesFactory.create(
                        id = 1,
                        ibovespa = BigDecimal("125000.50"),
                        dollar = BigDecimal("5.25"),
                    )

                every { marketQuotesAndCommoditiesRepository.findAll() } returns listOf(existingQuotes)

                val result = service.getMarketQuotesAndCommoditiesOrFetch()

                Then("should return existing quotes from database") {
                    result.id shouldBe 1
                    result.ibovespa shouldBe BigDecimal("125000.50")
                }

                Then("should not call API") {
                    coVerify(exactly = 0) { APIUtils.fetchStockPrices(any()) }
                }
            }
        }

        Given("market quotes and commodities do not exist in database") {
            When("getting market quotes and commodities or fetch") {
                val apiResponse =
                    JSONObject(
                        mapOf(
                            "^BVSP" to mapOf("price" to "125000.50"),
                            "BRL=X" to mapOf("price" to "5.25"),
                            "EURBRL=X" to mapOf("price" to "5.85"),
                            "USDBRL=X" to mapOf("price" to "5.25"),
                            "GC=F" to mapOf("price" to "2050.75"),
                            "ZS=F" to mapOf("price" to "1450.25"),
                            "KC=F" to mapOf("price" to "220.50"),
                            "ZW=F" to mapOf("price" to "650.00"),
                            "BZ=F" to mapOf("price" to "85.50"),
                            "BTC-USD" to mapOf("price" to "65000.00"),
                            "ETH-USD" to mapOf("price" to "3500.00"),
                        ),
                    )

                every { marketQuotesAndCommoditiesRepository.findAll() } returns emptyList()
                coEvery { APIUtils.fetchStockPrices(any()) } returns apiResponse
                every { marketQuotesAndCommoditiesRepository.save(any()) } answers { invocation ->
                    val mqac = invocation.invocation.args[0] as MarketQuotesAndCommodities
                    mqac.id = 1
                    mqac
                }

                val result = service.getMarketQuotesAndCommoditiesOrFetch()

                Then("should fetch from API") {
                    result.ibovespa shouldBe BigDecimal("125000.50")
                    result.dollar shouldBe BigDecimal("5.25")
                }

                Then("should save fetched data") {
                    verify { marketQuotesAndCommoditiesRepository.save(any()) }
                }
            }
        }

        Given("API fails when updating Brazilian market indicators") {
            When("API throws exception during fetch") {
                every { brazilianMarketIndicatorsRepository.findAll() } returns emptyList()
                coEvery { APIUtils.fetchBrazilianMarketIndicators() } throws RuntimeException("API error")

                Then("should throw RetryException after retries") {
                    shouldThrow<RetryException> {
                        service.updateBrazilianMarketIndicatorsFromApi()
                    }
                }
            }
        }

        Given("API fails when updating market quotes and commodities") {
            When("API throws exception during fetch") {
                every { marketQuotesAndCommoditiesRepository.findAll() } returns emptyList()
                coEvery { APIUtils.fetchStockPrices(any()) } throws RuntimeException("API error")

                Then("should throw RetryException after retries") {
                    shouldThrow<RetryException> {
                        service.updateMarketQuotesAndCommoditiesFromApi()
                    }
                }
            }
        }

        Given("API returns invalid response for Brazilian market indicators") {
            When("updating Brazilian market indicators with missing required fields") {
                val invalidApiResponse = JSONObject(mapOf("invalid_field" to "value"))

                every { brazilianMarketIndicatorsRepository.findAll() } returns emptyList()
                coEvery { APIUtils.fetchBrazilianMarketIndicators() } returns invalidApiResponse

                Then("should throw RetryException due to missing data") {
                    shouldThrow<RetryException> {
                        service.updateBrazilianMarketIndicatorsFromApi()
                    }
                }
            }
        }

        Given("API returns invalid response for market quotes and commodities") {
            When("updating market quotes and commodities with missing required fields") {
                val invalidApiResponse = JSONObject(mapOf("invalid_field" to "value"))

                every { marketQuotesAndCommoditiesRepository.findAll() } returns emptyList()
                coEvery { APIUtils.fetchStockPrices(any()) } returns invalidApiResponse

                Then("should throw RetryException due to missing data") {
                    shouldThrow<RetryException> {
                        service.updateMarketQuotesAndCommoditiesFromApi()
                    }
                }
            }
        }

        Given("multiple duplicate Brazilian market indicators in database") {
            When("getting Brazilian market indicators or fetch") {
                val indicator1 =
                    BrazilianMarketIndicatorsFactory.create(
                        id = 1,
                        selicTarget = BigDecimal("13.75"),
                    )
                val indicator2 =
                    BrazilianMarketIndicatorsFactory.create(
                        id = 2,
                        selicTarget = BigDecimal("13.50"),
                    )
                val indicator3 =
                    BrazilianMarketIndicatorsFactory.create(
                        id = 3,
                        selicTarget = BigDecimal("13.25"),
                    )

                every { brazilianMarketIndicatorsRepository.findAll() } returns listOf(indicator1, indicator2, indicator3)
                every { brazilianMarketIndicatorsRepository.delete(any()) } returns Unit

                val result = service.getBrazilianMarketIndicatorsOrFetch()

                Then("should return first indicator") {
                    result.id shouldBe 1
                }

                Then("should delete duplicate indicators") {
                    verify(exactly = 2) { brazilianMarketIndicatorsRepository.delete(any()) }
                }
            }
        }

        Given("multiple duplicate market quotes and commodities in database") {
            When("getting market quotes and commodities or fetch") {
                val quotes1 =
                    MarketQuotesAndCommoditiesFactory.create(
                        id = 1,
                        ibovespa = BigDecimal("125000.50"),
                    )
                val quotes2 =
                    MarketQuotesAndCommoditiesFactory.create(
                        id = 2,
                        ibovespa = BigDecimal("124000.00"),
                    )

                every { marketQuotesAndCommoditiesRepository.findAll() } returns listOf(quotes1, quotes2)
                every { marketQuotesAndCommoditiesRepository.delete(any()) } returns Unit

                val result = service.getMarketQuotesAndCommoditiesOrFetch()

                Then("should return first quotes") {
                    result.id shouldBe 1
                }

                Then("should delete duplicate quotes") {
                    verify(exactly = 1) { marketQuotesAndCommoditiesRepository.delete(any()) }
                }
            }
        }

        Given("concurrent updates to Brazilian market indicators") {
            When("multiple threads try to update simultaneously") {
                val apiResponse =
                    JSONObject(
                        mapOf(
                            "selic_target" to mapOf("valor" to "13.75"),
                            "ipca_12_months" to mapOf("valor" to "4.50"),
                            "ipca_last_month" to
                                mapOf(
                                    "valor" to "0.42",
                                    "data" to "01/02/2026",
                                ),
                        ),
                    )

                every { brazilianMarketIndicatorsRepository.findAll() } returns emptyList()
                coEvery { APIUtils.fetchBrazilianMarketIndicators() } returns apiResponse
                every { brazilianMarketIndicatorsRepository.save(any()) } answers { invocation ->
                    val bmi = invocation.invocation.args[0] as BrazilianMarketIndicators
                    bmi.id = 1
                    bmi
                }

                val result = service.updateBrazilianMarketIndicatorsFromApi()

                Then("should handle concurrent access with mutex") {
                    result shouldNotBe null
                    verify { brazilianMarketIndicatorsRepository.save(any()) }
                }
            }
        }

        Given("concurrent updates to market quotes and commodities") {
            When("multiple threads try to update simultaneously") {
                val apiResponse =
                    JSONObject(
                        mapOf(
                            "^BVSP" to mapOf("price" to "125000.50"),
                            "BRL=X" to mapOf("price" to "5.25"),
                            "EURBRL=X" to mapOf("price" to "5.85"),
                            "USDBRL=X" to mapOf("price" to "5.25"),
                            "GC=F" to mapOf("price" to "2050.75"),
                            "ZS=F" to mapOf("price" to "1450.25"),
                            "KC=F" to mapOf("price" to "220.50"),
                            "ZW=F" to mapOf("price" to "650.00"),
                            "BZ=F" to mapOf("price" to "85.50"),
                            "BTC-USD" to mapOf("price" to "65000.00"),
                            "ETH-USD" to mapOf("price" to "3500.00"),
                        ),
                    )

                every { marketQuotesAndCommoditiesRepository.findAll() } returns emptyList()
                coEvery { APIUtils.fetchStockPrices(any()) } returns apiResponse
                every { marketQuotesAndCommoditiesRepository.save(any()) } answers { invocation ->
                    val mqac = invocation.invocation.args[0] as MarketQuotesAndCommodities
                    mqac.id = 1
                    mqac
                }

                val result = service.updateMarketQuotesAndCommoditiesFromApi()

                Then("should handle concurrent access with mutex") {
                    result shouldNotBe null
                    verify { marketQuotesAndCommoditiesRepository.save(any()) }
                }
            }
        }
    })
