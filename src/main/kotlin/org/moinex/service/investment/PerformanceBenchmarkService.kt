/*
 * Filename: PerformanceBenchmarkService.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import org.moinex.common.ClockProvider
import org.moinex.model.dto.BenchmarkSeriesDTO
import org.moinex.model.dto.ContributionDTO
import org.moinex.model.dto.DailyPricePointDTO
import org.moinex.model.dto.PerformancePeriod
import org.moinex.model.enums.InterestIndex
import org.moinex.model.investment.Ticker
import org.moinex.repository.investment.BondRepository
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Compares the variable income portfolio with the CDI, the Ibovespa and the IPCA over a period. Bonds are not part of
 * the portfolio line; only tickers with a daily price history are.
 */
@Service
class PerformanceBenchmarkService(
    private val tickerRepository: TickerRepository,
    private val tickerPurchaseRepository: TickerPurchaseRepository,
    private val dividendRepository: DividendRepository,
    private val tickerSaleRepository: TickerSaleRepository,
    private val tickerPriceHistoryService: TickerPriceHistoryService,
    private val marketIndicatorService: MarketIndicatorService,
    private val benchmarkService: BenchmarkService,
    private val bondRepository: BondRepository,
    private val bondDailySeries: BondDailySeries,
    private val clockProvider: ClockProvider,
) {
    /**
     * Downloads what the benchmarks lack for the longest period shown. Failures are logged and keep what is stored.
     */
    suspend fun refreshBenchmarks() {
        val start = clockProvider.today().minusMonths(PerformancePeriod.M12.months.toLong())

        benchmarkService.syncIbovespa()
        marketIndicatorService.ensureHistory(InterestIndex.CDI, start)
        marketIndicatorService.ensureHistory(InterestIndex.IPCA, start)
    }

    fun benchmarkSeries(
        period: PerformancePeriod,
        includeDividends: Boolean = false,
        includeFixedIncome: Boolean = false,
    ): BenchmarkSeriesDTO {
        val today = clockProvider.today()
        val start = today.minusMonths(period.months.toLong())
        val tickers = tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc()
        val points = tickerPriceHistoryService.getPricePoints(tickers.mapNotNull { it.id }, start, today)

        val positions = loadPositions(tickers, points, includeDividends)

        val portfolio =
            PortfolioPerformanceCalculator.portfolioReturn(
                positions,
                start,
                today,
                includeDividends,
                if (includeFixedIncome) loadFixedIncome(today) else emptyList(),
            )
        val days = portfolio.map { it.date }
        if (days.size < 2) return BenchmarkSeriesDTO(portfolio, emptyList(), emptyList(), emptyList())

        val cdi = rates(InterestIndex.CDI, start, today)
        val ipca = rates(InterestIndex.IPCA, start, today)
        val ibovespa =
            benchmarkService
                .getIbovespa(start.minusDays(IBOVESPA_LOOKBACK_DAYS), today)
                .map { it.priceDate to it.closingPrice }

        return BenchmarkSeriesDTO(
            portfolio = portfolio,
            cdi = PortfolioPerformanceCalculator.cumulativeFromDailyRates(cdi, days),
            ibovespa = PortfolioPerformanceCalculator.cumulativeFromLevels(ibovespa, days),
            ipca = PortfolioPerformanceCalculator.cumulativeFromMonthlyRates(ipca, days),
        )
    }

    /**
     * What each ticker added to the portfolio over [period], in money, biggest first.
     */
    fun contributions(period: PerformancePeriod): List<ContributionDTO> {
        val today = clockProvider.today()
        val start = today.minusMonths(period.months.toLong())
        val tickers = tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc()
        val points = tickerPriceHistoryService.getPricePoints(tickers.mapNotNull { it.id }, start, today)

        return PortfolioPerformanceCalculator.contributions(
            loadPositions(tickers, points),
            dividendRepository.findAll(),
            start,
            today,
        )
    }

    private fun loadPositions(
        tickers: List<Ticker>,
        points: Map<Int, List<DailyPricePointDTO>>,
        includeDividends: Boolean = false,
    ): List<PortfolioPerformanceCalculator.Position> {
        val payoutsByTicker =
            if (includeDividends) {
                dividendRepository
                    .findAll()
                    .filter { it.walletTransaction != null }
                    .groupBy({ it.ticker.id }, {
                        PortfolioPerformanceCalculator.Payout(
                            it.walletTransaction!!.date.toLocalDate(),
                            it.walletTransaction!!.amount,
                        )
                    })
            } else {
                emptyMap()
            }

        return tickers.map { ticker ->
            val trades =
                tickerPurchaseRepository.findAllByTickerId(ticker.id!!).mapNotNull {
                    toTrade(it.walletTransaction?.date?.toLocalDate(), it.quantity, it.unitPrice)
                } +
                    tickerSaleRepository.findAllByTickerId(ticker.id!!).mapNotNull {
                        toTrade(it.walletTransaction?.date?.toLocalDate(), it.quantity.negate(), it.unitPrice)
                    }

            PortfolioPerformanceCalculator.Position(
                ticker,
                trades,
                points[ticker.id].orEmpty(),
                payoutsByTicker[ticker.id].orEmpty(),
            )
        }
    }

    private fun loadFixedIncome(today: LocalDate): List<PortfolioPerformanceCalculator.FixedIncomePosition> =
        bondRepository.findByArchivedFalseOrderByNameAsc().mapNotNull { bondDailySeries.position(it, today) }

    private fun rates(
        index: InterestIndex,
        start: LocalDate,
        end: LocalDate,
    ): List<Pair<LocalDate, BigDecimal>> =
        marketIndicatorService.getIndicatorHistoryBetween(index, start, end).map { it.referenceDate to it.rateValue }

    private fun toTrade(
        date: LocalDate?,
        signedQuantity: BigDecimal,
        price: BigDecimal,
    ): PortfolioPerformanceCalculator.Trade? =
        date?.let { PortfolioPerformanceCalculator.Trade(it, signedQuantity, price) }

    companion object {
        private const val IBOVESPA_LOOKBACK_DAYS = 7L
    }
}
