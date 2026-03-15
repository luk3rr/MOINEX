package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.moinex.common.retry.RetryException
import org.moinex.config.RetryConfig
import org.moinex.factory.investment.MarketIndicatorHistoryFactory
import org.moinex.model.enums.InterestIndex
import org.moinex.model.investment.MarketIndicatorHistory
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.MarketIndicatorHistoryRepository
import org.moinex.util.APIUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class MarketIndicatorServiceTest :
    BehaviorSpec({
        val marketIndicatorHistoryRepository = mockk<MarketIndicatorHistoryRepository>()
        val bondOperationRepository = mockk<BondOperationRepository>()

        val service = MarketIndicatorService(marketIndicatorHistoryRepository, bondOperationRepository)

        mockkObject(APIUtils)

        mockkObject(RetryConfig.Companion)

        afterContainer {
            clearAllMocks(answers = true)
        }

        beforeContainer {
            every { RetryConfig.BACEN_API } returns
                RetryConfig(
                    maxRetries = 3,
                    initialDelayMs = 100,
                    multiplier = 2.0,
                )
        }

        Given("no bonds with interest indices") {
            When("updating all indicators") {
                every { bondOperationRepository.findAllUsedInterestIndices() } returns emptyList()

                service.updateAllIndicators()

                Then("should skip synchronization") {
                    verify(exactly = 0) { marketIndicatorHistoryRepository.findEarliestByIndicatorType(any()) }
                }
            }
        }

        Given("bonds with interest indices but no purchase dates") {
            When("updating all indicators") {
                every { bondOperationRepository.findAllUsedInterestIndices() } returns listOf(InterestIndex.CDI)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(InterestIndex.CDI) } returns null

                service.updateAllIndicators()

                Then("should skip synchronization for indicators without purchase dates") {
                    verify(exactly = 0) { marketIndicatorHistoryRepository.findEarliestByIndicatorType(InterestIndex.CDI) }
                }
            }
        }

        Given("multiple indicators used by bonds") {
            When("updating all indicators") {
                val indicators = listOf(InterestIndex.CDI, InterestIndex.SELIC, InterestIndex.IPCA)
                every { bondOperationRepository.findAllUsedInterestIndices() } returns indicators
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(any()) } returns null

                service.updateAllIndicators()

                Then("should process all indicators") {
                    verify(exactly = 3) { bondOperationRepository.findEarliestBuyDateByInterestIndex(any()) }
                }
            }
        }

        Given("indicator history between two dates") {
            When("getting indicator history") {
                val startDate = LocalDate.of(2026, 1, 1)
                val endDate = LocalDate.of(2026, 3, 11)
                val histories =
                    listOf(
                        MarketIndicatorHistoryFactory.create(
                            id = 1,
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, 15),
                            rateValue = BigDecimal("10.50"),
                        ),
                        MarketIndicatorHistoryFactory.create(
                            id = 2,
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 2, 15),
                            rateValue = BigDecimal("10.75"),
                        ),
                    )
                every {
                    marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDateBetween(
                        InterestIndex.CDI,
                        startDate,
                        endDate,
                    )
                } returns histories

                val result = service.getIndicatorHistoryBetween(InterestIndex.CDI, startDate, endDate)

                Then("should return all indicator history records") {
                    result.size shouldBe 2
                }

                Then("should return records with correct indicator type") {
                    result.all { it.indicatorType == InterestIndex.CDI } shouldBe true
                }

                Then("should return records within date range") {
                    result.all {
                        it.referenceDate.isAfter(
                            startDate.minusDays(1),
                        ) &&
                            it.referenceDate.isBefore(endDate.plusDays(1))
                    } shouldBe
                        true
                }

                Then("should call repository method") {
                    verify {
                        marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDateBetween(
                            InterestIndex.CDI,
                            startDate,
                            endDate,
                        )
                    }
                }
            }
        }

        Given("no indicator history between two dates") {
            When("getting indicator history") {
                val startDate = LocalDate.of(2026, 1, 1)
                val endDate = LocalDate.of(2026, 3, 11)
                every {
                    marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDateBetween(
                        InterestIndex.SELIC,
                        startDate,
                        endDate,
                    )
                } returns emptyList()

                val result = service.getIndicatorHistoryBetween(InterestIndex.SELIC, startDate, endDate)

                Then("should return empty list") {
                    result.size shouldBe 0
                }
            }
        }

        Given("multiple indicators to synchronize") {
            When("updating all indicators with mixed results") {
                val indicators = listOf(InterestIndex.CDI, InterestIndex.SELIC)
                every { bondOperationRepository.findAllUsedInterestIndices() } returns indicators
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(InterestIndex.CDI) } returns null
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(InterestIndex.SELIC) } returns null

                service.updateAllIndicators()

                Then("should process all indicators") {
                    verify(exactly = 2) { bondOperationRepository.findEarliestBuyDateByInterestIndex(any()) }
                }
            }
        }

        Given("different interest index types") {
            When("getting history for each type") {
                val cdiHistory =
                    listOf(
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, 1),
                        ),
                    )
                val selicHistory =
                    listOf(
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.SELIC,
                            referenceDate = LocalDate.of(2026, 1, 1),
                        ),
                    )
                val ipcaHistory =
                    listOf(
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.IPCA,
                            referenceDate = LocalDate.of(2026, 1, 1),
                        ),
                    )

                every {
                    marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDateBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns cdiHistory
                every {
                    marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDateBetween(
                        InterestIndex.SELIC,
                        any(),
                        any(),
                    )
                } returns selicHistory
                every {
                    marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDateBetween(
                        InterestIndex.IPCA,
                        any(),
                        any(),
                    )
                } returns ipcaHistory

                val startDate = LocalDate.of(2026, 1, 1)
                val endDate = LocalDate.of(2026, 3, 11)

                val cdiResult = service.getIndicatorHistoryBetween(InterestIndex.CDI, startDate, endDate)
                val selicResult = service.getIndicatorHistoryBetween(InterestIndex.SELIC, startDate, endDate)
                val ipcaResult = service.getIndicatorHistoryBetween(InterestIndex.IPCA, startDate, endDate)

                Then("should return correct history for CDI") {
                    cdiResult.all { it.indicatorType == InterestIndex.CDI } shouldBe true
                }

                Then("should return correct history for SELIC") {
                    selicResult.all { it.indicatorType == InterestIndex.SELIC } shouldBe true
                }

                Then("should return correct history for IPCA") {
                    ipcaResult.all { it.indicatorType == InterestIndex.IPCA } shouldBe true
                }
            }
        }

        Given("rate values with different precisions") {
            When("creating indicators with various rate values") {
                val smallRate = BigDecimal("0.01")
                val mediumRate = BigDecimal("10.50")
                val largeRate = BigDecimal("99.99")

                val indicator1 = MarketIndicatorHistoryFactory.create(rateValue = smallRate)
                val indicator2 = MarketIndicatorHistoryFactory.create(rateValue = mediumRate)
                val indicator3 = MarketIndicatorHistoryFactory.create(rateValue = largeRate)

                Then("should handle small rate values correctly") {
                    indicator1.rateValue shouldBe smallRate
                }

                Then("should handle medium rate values correctly") {
                    indicator2.rateValue shouldBe mediumRate
                }

                Then("should handle large rate values correctly") {
                    indicator3.rateValue shouldBe largeRate
                }
            }
        }

        Given("zero rate value") {
            When("creating indicator with zero rate") {
                val indicator = MarketIndicatorHistoryFactory.create(rateValue = BigDecimal.ZERO)

                Then("should accept zero rate value") {
                    indicator.rateValue shouldBe BigDecimal.ZERO
                }
            }
        }

        Given("negative rate value") {
            When("creating indicator with negative rate") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        MarketIndicatorHistoryFactory.create(rateValue = BigDecimal("-5.00"))
                    }
                }
            }
        }

        Given("large rate values") {
            When("saving indicators with large rate values") {
                val largeRate = BigDecimal("999.99")
                val indicator = MarketIndicatorHistoryFactory.create(rateValue = largeRate)

                Then("should handle large rate values") {
                    indicator.rateValue shouldBe largeRate
                }
            }
        }

        Given("historical dates spanning multiple years") {
            When("getting history across years") {
                val startDate = LocalDate.of(2024, 1, 1)
                val endDate = LocalDate.of(2026, 3, 11)
                val histories =
                    listOf(
                        MarketIndicatorHistoryFactory.create(
                            id = 1,
                            referenceDate = LocalDate.of(2024, 6, 1),
                        ),
                        MarketIndicatorHistoryFactory.create(
                            id = 2,
                            referenceDate = LocalDate.of(2025, 6, 1),
                        ),
                        MarketIndicatorHistoryFactory.create(
                            id = 3,
                            referenceDate = LocalDate.of(2026, 3, 1),
                        ),
                    )
                every {
                    marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDateBetween(
                        any(),
                        startDate,
                        endDate,
                    )
                } returns histories

                val result = service.getIndicatorHistoryBetween(InterestIndex.CDI, startDate, endDate)

                Then("should return all records spanning multiple years") {
                    result.size shouldBe 3
                }

                Then("should maintain chronological order") {
                    result[0].referenceDate.isBefore(result[1].referenceDate) shouldBe true
                    result[1].referenceDate.isBefore(result[2].referenceDate) shouldBe true
                }
            }
        }

        Given("same reference date with different indicators") {
            When("getting history for different indicators on same date") {
                val referenceDate = LocalDate.of(2026, 3, 1)
                val cdiHistory =
                    listOf(
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = referenceDate,
                            rateValue = BigDecimal("10.50"),
                        ),
                    )
                val selicHistory =
                    listOf(
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.SELIC,
                            referenceDate = referenceDate,
                            rateValue = BigDecimal("11.25"),
                        ),
                    )

                every {
                    marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDateBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns cdiHistory
                every {
                    marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDateBetween(
                        InterestIndex.SELIC,
                        any(),
                        any(),
                    )
                } returns selicHistory

                val cdiResult = service.getIndicatorHistoryBetween(InterestIndex.CDI, referenceDate, referenceDate)
                val selicResult = service.getIndicatorHistoryBetween(InterestIndex.SELIC, referenceDate, referenceDate)

                Then("should return different rates for different indicators") {
                    cdiResult[0].rateValue shouldBe BigDecimal("10.50")
                    selicResult[0].rateValue shouldBe BigDecimal("11.25")
                }
            }
        }

        Given("indicator with existing data and no gap") {
            When("determining sync start date with complete data") {
                val indicator = InterestIndex.CDI
                val earliestPurchaseDate = LocalDate.of(2025, 1, 1)
                val endDate = LocalDate.of(2026, 3, 11)
                val earliestData =
                    MarketIndicatorHistoryFactory.create(
                        id = 1,
                        indicatorType = indicator,
                        referenceDate = LocalDate.of(2025, 1, 1),
                    )
                val latestData =
                    MarketIndicatorHistoryFactory.create(
                        id = 100,
                        indicatorType = indicator,
                        referenceDate = LocalDate.of(2026, 3, 10),
                    )

                every { bondOperationRepository.findAllUsedInterestIndices() } returns listOf(indicator)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator) } returns
                    earliestPurchaseDate.atStartOfDay()
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) } returns earliestData
                every { marketIndicatorHistoryRepository.findLatestByIndicatorType(indicator) } returns latestData
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } returns JSONObject().put("data", JSONArray())

                service.updateAllIndicators()

                Then("should use latest data date as sync start") {
                    verify { marketIndicatorHistoryRepository.findLatestByIndicatorType(indicator) }
                }
            }
        }

        Given("indicator with data gap") {
            When("determining sync start date with gap in data") {
                val indicator = InterestIndex.SELIC
                val earliestPurchaseDate = LocalDate.of(2024, 1, 1)
                val endDate = LocalDate.of(2026, 3, 11)
                val earliestData =
                    MarketIndicatorHistoryFactory.create(
                        id = 1,
                        indicatorType = indicator,
                        referenceDate = LocalDate.of(2025, 1, 1),
                    )

                every { bondOperationRepository.findAllUsedInterestIndices() } returns listOf(indicator)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator) } returns
                    earliestPurchaseDate.atStartOfDay()
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) } returns earliestData
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } returns JSONObject().put("data", JSONArray())

                service.updateAllIndicators()

                Then("should identify gap and attempt backfill") {
                    verify { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) }
                }
            }
        }

        Given("indicator with no existing data") {
            When("determining sync start date with no historical data") {
                val indicator = InterestIndex.IPCA
                val earliestPurchaseDate = LocalDate.of(2025, 6, 1)

                every { bondOperationRepository.findAllUsedInterestIndices() } returns listOf(indicator)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator) } returns
                    earliestPurchaseDate.atStartOfDay()
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) } returns null
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } returns JSONObject().put("data", JSONArray())

                service.updateAllIndicators()

                Then("should attempt to fetch from earliest purchase date") {
                    verify { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) }
                }
            }
        }

        Given("API response with multiple indicator records") {
            When("saving indicators from API response") {
                val indicator = InterestIndex.CDI
                val date1 = LocalDate.of(2026, 3, 1)
                val date2 = LocalDate.of(2026, 3, 2)

                every {
                    marketIndicatorHistoryRepository.existsByIndicatorTypeAndReferenceDate(
                        indicator,
                        date1,
                    )
                } returns false
                every {
                    marketIndicatorHistoryRepository.existsByIndicatorTypeAndReferenceDate(
                        indicator,
                        date2,
                    )
                } returns false
                every { marketIndicatorHistoryRepository.save(any()) } returnsArgument 0

                Then("should save new indicators") {
                    verify(exactly = 0) { marketIndicatorHistoryRepository.save(any()) }
                }
            }
        }

        Given("duplicate indicator in API response") {
            When("saving indicators when some already exist") {
                val indicator = InterestIndex.CDI
                val existingDate = LocalDate.of(2026, 3, 1)
                val newDate = LocalDate.of(2026, 3, 2)

                every {
                    marketIndicatorHistoryRepository.existsByIndicatorTypeAndReferenceDate(
                        indicator,
                        existingDate,
                    )
                } returns true
                every {
                    marketIndicatorHistoryRepository.existsByIndicatorTypeAndReferenceDate(
                        indicator,
                        newDate,
                    )
                } returns false
                every { marketIndicatorHistoryRepository.save(any()) } returnsArgument 0

                Then("should skip existing indicators") {
                    verify(exactly = 0) { marketIndicatorHistoryRepository.existsByIndicatorTypeAndReferenceDate(any(), any()) }
                }
            }
        }

        Given("multiple indicators with different sync requirements") {
            When("updating multiple indicators with mixed scenarios") {
                val cdiIndicator = InterestIndex.CDI
                val selicIndicator = InterestIndex.SELIC
                val cdiPurchaseDate = LocalDate.of(2025, 1, 1)
                val selicPurchaseDate = LocalDate.of(2024, 6, 1)

                every { bondOperationRepository.findAllUsedInterestIndices() } returns
                    listOf(cdiIndicator, selicIndicator)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(cdiIndicator) } returns
                    cdiPurchaseDate.atStartOfDay()
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(selicIndicator) } returns
                    selicPurchaseDate.atStartOfDay()
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(cdiIndicator) } returns null
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(selicIndicator) } returns
                    MarketIndicatorHistoryFactory.create(
                        indicatorType = selicIndicator,
                        referenceDate = LocalDate.of(2024, 6, 1),
                    )

                every { marketIndicatorHistoryRepository.findLatestByIndicatorType(any()) } returns null
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } returns JSONObject().put("data", JSONArray())

                service.updateAllIndicators()

                Then("should process all indicators") {
                    verify(exactly = 2) { bondOperationRepository.findEarliestBuyDateByInterestIndex(any()) }
                }

                Then("should check existing data for all indicators") {
                    verify(exactly = 2) { marketIndicatorHistoryRepository.findEarliestByIndicatorType(any()) }
                }
            }
        }

        Given("indicator with valid rate values") {
            When("saving indicator with valid rate") {
                val indicator = InterestIndex.CDI
                val referenceDate = LocalDate.of(2026, 3, 1)
                val rateValue = BigDecimal("10.50")

                every {
                    marketIndicatorHistoryRepository.existsByIndicatorTypeAndReferenceDate(
                        indicator,
                        referenceDate,
                    )
                } returns false
                every { marketIndicatorHistoryRepository.save(any()) } returnsArgument 0

                Then("should accept valid rate value") {
                    val history =
                        MarketIndicatorHistory(
                            indicatorType = indicator,
                            referenceDate = referenceDate,
                            rateValue = rateValue,
                            createdAt = LocalDateTime.now(),
                        )
                    history.rateValue shouldBe rateValue
                }
            }
        }

        Given("indicator with zero rate value") {
            When("saving indicator with zero rate") {
                val indicator = InterestIndex.SELIC
                val referenceDate = LocalDate.of(2026, 3, 1)
                val rateValue = BigDecimal.ZERO

                Then("should accept zero rate value") {
                    val history =
                        MarketIndicatorHistory(
                            indicatorType = indicator,
                            referenceDate = referenceDate,
                            rateValue = rateValue,
                            createdAt = LocalDateTime.now(),
                        )
                    history.rateValue shouldBe BigDecimal.ZERO
                }
            }
        }

        Given("indicator with high precision rate value") {
            When("saving indicator with precise rate") {
                val indicator = InterestIndex.IPCA
                val referenceDate = LocalDate.of(2026, 3, 1)
                val rateValue = BigDecimal("10.123456")

                Then("should preserve rate precision") {
                    val history =
                        MarketIndicatorHistory(
                            indicatorType = indicator,
                            referenceDate = referenceDate,
                            rateValue = rateValue,
                            createdAt = LocalDateTime.now(),
                        )
                    history.rateValue shouldBe rateValue
                }
            }
        }

        Given("multiple indicators across different date ranges") {
            When("getting history for indicators with different date ranges") {
                val startDate = LocalDate.of(2024, 1, 1)
                val endDate = LocalDate.of(2026, 3, 11)
                val cdiHistory =
                    listOf(
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2025, 1, 1),
                        ),
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, 1),
                        ),
                    )
                val selicHistory =
                    listOf(
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.SELIC,
                            referenceDate = LocalDate.of(2024, 6, 1),
                        ),
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.SELIC,
                            referenceDate = LocalDate.of(2025, 6, 1),
                        ),
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.SELIC,
                            referenceDate = LocalDate.of(2026, 1, 1),
                        ),
                    )

                every {
                    marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDateBetween(
                        InterestIndex.CDI,
                        startDate,
                        endDate,
                    )
                } returns cdiHistory
                every {
                    marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDateBetween(
                        InterestIndex.SELIC,
                        startDate,
                        endDate,
                    )
                } returns selicHistory

                val cdiResult = service.getIndicatorHistoryBetween(InterestIndex.CDI, startDate, endDate)
                val selicResult = service.getIndicatorHistoryBetween(InterestIndex.SELIC, startDate, endDate)

                Then("should return correct count for CDI") {
                    cdiResult.size shouldBe 2
                }

                Then("should return correct count for SELIC") {
                    selicResult.size shouldBe 3
                }

                Then("should maintain chronological order for CDI") {
                    cdiResult[0].referenceDate.isBefore(cdiResult[1].referenceDate) shouldBe true
                }

                Then("should maintain chronological order for SELIC") {
                    selicResult[0].referenceDate.isBefore(selicResult[1].referenceDate) shouldBe true
                    selicResult[1].referenceDate.isBefore(selicResult[2].referenceDate) shouldBe true
                }
            }
        }

        Given("API response with new indicator data") {
            When("saving new indicator that doesn't exist") {
                val indicator = InterestIndex.CDI
                val referenceDate = LocalDate.of(2026, 3, 1)
                val rateValue = BigDecimal("10.50")

                every {
                    marketIndicatorHistoryRepository.existsByIndicatorTypeAndReferenceDate(
                        indicator,
                        referenceDate,
                    )
                } returns false
                every { marketIndicatorHistoryRepository.save(any()) } returnsArgument 0

                every { bondOperationRepository.findAllUsedInterestIndices() } returns listOf(indicator)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator) } returns
                    LocalDate.of(2025, 1, 1).atStartOfDay()
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) } returns null
                every { marketIndicatorHistoryRepository.findLatestByIndicatorType(indicator) } returns null

                val apiResponse =
                    JSONObject().put(
                        "data",
                        JSONArray().put(
                            JSONObject()
                                .put("data", "01/03/2026")
                                .put("valor", "10.50"),
                        ),
                    )
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } returns apiResponse

                service.updateAllIndicators()

                Then("should save new indicator") {
                    verify { marketIndicatorHistoryRepository.save(any()) }
                }

                Then("should check if indicator exists before saving") {
                    verify {
                        marketIndicatorHistoryRepository.existsByIndicatorTypeAndReferenceDate(
                            indicator,
                            referenceDate,
                        )
                    }
                }
            }
        }

        Given("API response with duplicate indicator data") {
            When("saving indicator that already exists") {
                val indicator = InterestIndex.SELIC
                val referenceDate = LocalDate.of(2026, 3, 1)

                every {
                    marketIndicatorHistoryRepository.existsByIndicatorTypeAndReferenceDate(
                        indicator,
                        referenceDate,
                    )
                } returns true

                every { bondOperationRepository.findAllUsedInterestIndices() } returns listOf(indicator)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator) } returns
                    LocalDate.of(2025, 1, 1).atStartOfDay()
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) } returns null
                every { marketIndicatorHistoryRepository.findLatestByIndicatorType(indicator) } returns null

                val apiResponse =
                    JSONObject().put(
                        "data",
                        JSONArray().put(
                            JSONObject()
                                .put("data", "01/03/2026")
                                .put("valor", "11.25"),
                        ),
                    )
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } returns apiResponse

                service.updateAllIndicators()

                Then("should not save duplicate indicator") {
                    verify(exactly = 0) { marketIndicatorHistoryRepository.save(any()) }
                }
            }
        }

        Given("API response with multiple records, some new and some duplicates") {
            When("saving mixed indicators") {
                val indicator = InterestIndex.IPCA
                val date1 = LocalDate.of(2026, 3, 1)
                val date2 = LocalDate.of(2026, 3, 2)

                every {
                    marketIndicatorHistoryRepository.existsByIndicatorTypeAndReferenceDate(
                        indicator,
                        date1,
                    )
                } returns true
                every {
                    marketIndicatorHistoryRepository.existsByIndicatorTypeAndReferenceDate(
                        indicator,
                        date2,
                    )
                } returns false
                every { marketIndicatorHistoryRepository.save(any()) } returnsArgument 0

                every { bondOperationRepository.findAllUsedInterestIndices() } returns listOf(indicator)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator) } returns
                    LocalDate.of(2025, 1, 1).atStartOfDay()
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) } returns null
                every { marketIndicatorHistoryRepository.findLatestByIndicatorType(indicator) } returns null

                val apiResponse =
                    JSONObject().put(
                        "data",
                        JSONArray()
                            .put(
                                JSONObject()
                                    .put("data", "01/03/2026")
                                    .put("valor", "5.50"),
                            ).put(
                                JSONObject()
                                    .put("data", "02/03/2026")
                                    .put("valor", "5.55"),
                            ),
                    )
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } returns apiResponse

                service.updateAllIndicators()

                Then("should save only new indicators") {
                    verify(exactly = 1) { marketIndicatorHistoryRepository.save(any()) }
                }
            }
        }

        Given("API call fails with retry exception") {
            When("synchronization fails after retries") {
                val indicator = InterestIndex.CDI

                every { bondOperationRepository.findAllUsedInterestIndices() } returns listOf(indicator)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator) } returns
                    LocalDate.of(2025, 1, 1).atStartOfDay()
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) } returns null
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } throws
                    RetryException("Failed after retries", Exception("Network error"))

                service.updateAllIndicators()

                Then("should handle retry exception gracefully") {
                    verify(exactly = 0) { marketIndicatorHistoryRepository.save(any()) }
                }
            }
        }

        Given("API call fails with unexpected exception") {
            When("synchronization fails with unexpected error") {
                val indicator = InterestIndex.SELIC

                every { bondOperationRepository.findAllUsedInterestIndices() } returns listOf(indicator)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator) } returns
                    LocalDate.of(2025, 1, 1).atStartOfDay()
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) } returns null
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } throws
                    RuntimeException("Unexpected error")

                service.updateAllIndicators()

                Then("should handle unexpected exception gracefully") {
                    verify(exactly = 0) { marketIndicatorHistoryRepository.save(any()) }
                }
            }
        }

        Given("empty API response") {
            When("API returns empty data array") {
                val indicator = InterestIndex.IPCA

                every { bondOperationRepository.findAllUsedInterestIndices() } returns listOf(indicator)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator) } returns
                    LocalDate.of(2025, 1, 1).atStartOfDay()
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) } returns null
                every { marketIndicatorHistoryRepository.findLatestByIndicatorType(indicator) } returns null

                val emptyResponse = JSONObject().put("data", JSONArray())
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } returns emptyResponse

                service.updateAllIndicators()

                Then("should handle empty response without errors") {
                    verify(exactly = 0) { marketIndicatorHistoryRepository.save(any()) }
                }
            }
        }

        Given("API call fails with unexpected exception type") {
            When("synchronization fails with non-RetryException error") {
                val indicator = InterestIndex.CDI

                every { bondOperationRepository.findAllUsedInterestIndices() } returns listOf(indicator)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator) } returns
                    LocalDate.of(2025, 1, 1).atStartOfDay()
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) } returns null
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } throws
                    IllegalArgumentException("Invalid argument in API call")

                service.updateAllIndicators()

                Then("should handle unexpected exception gracefully") {
                    verify(exactly = 0) { marketIndicatorHistoryRepository.save(any()) }
                }

                Then("should not attempt to save any data") {
                    verify(exactly = 0) { marketIndicatorHistoryRepository.existsByIndicatorTypeAndReferenceDate(any(), any()) }
                }
            }
        }

        Given("API response with invalid JSON structure") {
            When("API returns malformed data") {
                val indicator = InterestIndex.SELIC

                every { bondOperationRepository.findAllUsedInterestIndices() } returns listOf(indicator)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator) } returns
                    LocalDate.of(2025, 1, 1).atStartOfDay()
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) } returns null
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } throws
                    Exception("JSON parsing error")

                service.updateAllIndicators()

                Then("should handle JSON parsing error gracefully") {
                    verify(exactly = 0) { marketIndicatorHistoryRepository.save(any()) }
                }
            }
        }

        Given("API response with missing required fields") {
            When("API returns data with missing 'data' field") {
                val indicator = InterestIndex.IPCA

                every { bondOperationRepository.findAllUsedInterestIndices() } returns listOf(indicator)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator) } returns
                    LocalDate.of(2025, 1, 1).atStartOfDay()
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) } returns null
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } throws
                    Exception("Missing 'data' field in response")

                service.updateAllIndicators()

                Then("should handle missing field error gracefully") {
                    verify(exactly = 0) { marketIndicatorHistoryRepository.save(any()) }
                }
            }
        }

        Given("API response with invalid date format") {
            When("API returns data with unparseable date") {
                val indicator = InterestIndex.CDI

                every { bondOperationRepository.findAllUsedInterestIndices() } returns listOf(indicator)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator) } returns
                    LocalDate.of(2025, 1, 1).atStartOfDay()
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) } returns null

                val invalidResponse =
                    JSONObject().put(
                        "data",
                        JSONArray().put(
                            JSONObject()
                                .put("data", "invalid-date")
                                .put("valor", "10.50"),
                        ),
                    )
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } returns invalidResponse

                service.updateAllIndicators()

                Then("should handle date parsing error gracefully") {
                    verify(exactly = 0) { marketIndicatorHistoryRepository.save(any()) }
                }
            }
        }

        Given("API response with invalid rate value") {
            When("API returns data with non-numeric rate") {
                val indicator = InterestIndex.SELIC

                every { bondOperationRepository.findAllUsedInterestIndices() } returns listOf(indicator)
                every { bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator) } returns
                    LocalDate.of(2025, 1, 1).atStartOfDay()
                every { marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator) } returns null

                val invalidResponse =
                    JSONObject().put(
                        "data",
                        JSONArray().put(
                            JSONObject()
                                .put("data", "01/03/2026")
                                .put("valor", "invalid-number"),
                        ),
                    )
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } returns invalidResponse

                service.updateAllIndicators()

                Then("should handle invalid rate value error gracefully") {
                    verify(exactly = 0) { marketIndicatorHistoryRepository.save(any()) }
                }
            }
        }
    })
