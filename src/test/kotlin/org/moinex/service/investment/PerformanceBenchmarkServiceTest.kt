package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.moinex.common.ClockProvider
import org.moinex.factory.investment.TickerFactory
import org.moinex.model.dto.DailyPricePointDTO
import org.moinex.model.dto.PerformancePeriod
import org.moinex.model.enums.InterestIndex
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import java.math.BigDecimal

class PerformanceBenchmarkServiceTest :
    BehaviorSpec({
        val tickerRepository = mockk<TickerRepository>()
        val purchases = mockk<TickerPurchaseRepository>()
        val sales = mockk<TickerSaleRepository>()
        val dividends = mockk<DividendRepository>()
        val priceHistory = mockk<TickerPriceHistoryService>()
        val indicators = mockk<MarketIndicatorService>()
        val benchmarks = mockk<BenchmarkService>()
        val clock = ClockProvider()
        val service =
            PerformanceBenchmarkService(
                tickerRepository,
                purchases,
                dividends,
                sales,
                priceHistory,
                indicators,
                benchmarks,
                mockk<org.moinex.repository.investment.BondRepository>(),
                mockk<BondDailySeries>(),
                clock,
            )
        val today = clock.today()

        Given("no benchmark data yet") {
            Then("refreshing asks for the Ibovespa, the CDI and the IPCA of the last 12 months") {
                coEvery { benchmarks.syncIbovespa() } returns 0
                coEvery { indicators.ensureHistory(any(), any()) } returns Unit

                service.refreshBenchmarks()

                coVerify { benchmarks.syncIbovespa() }
                coVerify { indicators.ensureHistory(InterestIndex.CDI, today.minusMonths(12)) }
                coVerify { indicators.ensureHistory(InterestIndex.IPCA, today.minusMonths(12)) }
            }
        }

        Given("a ticker with two days of prices") {
            val ticker = TickerFactory.create(id = 1, currentQuantity = BigDecimal("10"))
            val day1 = today.minusDays(10)
            val day2 = today.minusDays(9)

            every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns listOf(ticker)
            every { purchases.findAllByTickerId(1) } returns emptyList()
            every { sales.findAllByTickerId(1) } returns emptyList()
            every { priceHistory.getPricePoints(listOf(1), any(), any()) } returns
                mapOf(
                    1 to
                        listOf(
                            DailyPricePointDTO(1, day1, BigDecimal("10")),
                            DailyPricePointDTO(1, day2, BigDecimal("12")),
                        ),
                )
            every { indicators.getIndicatorHistoryBetween(InterestIndex.CDI, any(), any()) } returns emptyList()
            every { indicators.getIndicatorHistoryBetween(InterestIndex.IPCA, any(), any()) } returns emptyList()
            every { benchmarks.getIbovespa(any(), any()) } returns emptyList()

            Then("the portfolio line is built and the benchmarks without data stay empty") {
                val series = service.benchmarkSeries(PerformancePeriod.M3)

                series.portfolio.map { it.pct } shouldBe listOf(BigDecimal("0.00"), BigDecimal("20.00"))
                series.cdi shouldBe emptyList()
                series.ibovespa shouldBe emptyList()
                series.ipca shouldBe emptyList()
                series.isEmpty() shouldBe false
            }
        }

        Given("a portfolio without price history") {
            every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns
                listOf(TickerFactory.create(id = 2))
            every { purchases.findAllByTickerId(2) } returns emptyList()
            every { sales.findAllByTickerId(2) } returns emptyList()
            every { priceHistory.getPricePoints(listOf(2), any(), any()) } returns emptyMap()

            Then("the series is empty and the benchmarks are not even read") {
                val series = service.benchmarkSeries(PerformancePeriod.M12)

                series.isEmpty() shouldBe true
                series.cdi shouldBe emptyList()
            }
        }
    })
