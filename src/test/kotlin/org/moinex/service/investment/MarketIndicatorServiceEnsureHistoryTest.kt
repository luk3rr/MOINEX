package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.json.JSONArray
import org.json.JSONObject
import org.moinex.common.ClockProvider
import org.moinex.common.util.APIUtils
import org.moinex.factory.investment.MarketIndicatorHistoryFactory
import org.moinex.model.enums.InterestIndex
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.MarketIndicatorHistoryRepository
import java.math.BigDecimal

class MarketIndicatorServiceEnsureHistoryTest :
    BehaviorSpec({
        val repository = mockk<MarketIndicatorHistoryRepository>()
        val clock = ClockProvider()
        val service = MarketIndicatorService(repository, mockk<BondOperationRepository>(), clock)
        val today = clock.today()
        val start = today.minusMonths(12)

        beforeSpec { mockkObject(APIUtils) }
        afterSpec { unmockkAll() }
        afterContainer { clearAllMocks(answers = true) }

        fun entry(
            indicator: InterestIndex,
            date: java.time.LocalDate,
        ) = MarketIndicatorHistoryFactory.create(
            indicatorType = indicator,
            referenceDate = date,
            rateValue = BigDecimal("0.05"),
        )

        fun emptyResponse() = JSONObject().put("data", JSONArray())

        Given("an indicator with no history") {
            Then("the whole window is downloaded") {
                every { repository.findEarliestByIndicatorType(InterestIndex.CDI) } returns null
                every { repository.findLatestByIndicatorType(InterestIndex.CDI) } returns null
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } returns emptyResponse()

                service.ensureHistory(InterestIndex.CDI, start)

                coVerify { APIUtils.fetchMarketIndicatorHistory(InterestIndex.CDI, start, today) }
            }
        }

        Given("a CDI history that covers the window up to a few days ago") {
            Then("nothing is downloaded") {
                every { repository.findEarliestByIndicatorType(InterestIndex.CDI) } returns
                    entry(InterestIndex.CDI, start.minusDays(2))
                every { repository.findLatestByIndicatorType(InterestIndex.CDI) } returns
                    entry(InterestIndex.CDI, today.minusDays(2))

                service.ensureHistory(InterestIndex.CDI, start)

                coVerify(exactly = 0) { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) }
            }
        }

        Given("a CDI history that stopped a week ago") {
            Then("only the days since then are downloaded") {
                val latest = today.minusDays(9)
                every { repository.findEarliestByIndicatorType(InterestIndex.CDI) } returns
                    entry(InterestIndex.CDI, start)
                every { repository.findLatestByIndicatorType(InterestIndex.CDI) } returns
                    entry(InterestIndex.CDI, latest)
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } returns emptyResponse()

                service.ensureHistory(InterestIndex.CDI, start)

                coVerify { APIUtils.fetchMarketIndicatorHistory(InterestIndex.CDI, latest, today) }
            }
        }

        Given("a history that starts well after the window") {
            Then("the missing start is downloaded") {
                every { repository.findEarliestByIndicatorType(InterestIndex.CDI) } returns
                    entry(InterestIndex.CDI, start.plusMonths(4))
                every { repository.findLatestByIndicatorType(InterestIndex.CDI) } returns
                    entry(InterestIndex.CDI, today.minusDays(1))
                coEvery { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) } returns emptyResponse()

                service.ensureHistory(InterestIndex.CDI, start)

                coVerify { APIUtils.fetchMarketIndicatorHistory(InterestIndex.CDI, start, today) }
            }
        }

        Given("a monthly IPCA whose last month is a month old") {
            Then("it is still considered up to date") {
                every { repository.findEarliestByIndicatorType(InterestIndex.IPCA) } returns
                    entry(InterestIndex.IPCA, start.withDayOfMonth(1))
                every { repository.findLatestByIndicatorType(InterestIndex.IPCA) } returns
                    entry(InterestIndex.IPCA, today.minusDays(35))

                service.ensureHistory(InterestIndex.IPCA, start)

                coVerify(exactly = 0) { APIUtils.fetchMarketIndicatorHistory(any(), any(), any()) }
            }
        }
    })
