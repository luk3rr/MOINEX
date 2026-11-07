/*
 * Filename: InvestmentPerformanceService.kt (original filenames: InvestmentPerformanceSnapshotService.java, InvestmentPerformanceCalculationService.java)
 * Created on: February 17, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 15/03/2026
 */

package org.moinex.service.investment

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.moinex.common.ClockProvider
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.calculateInvestedAmountUntil
import org.moinex.common.extension.realizedInterest
import org.moinex.model.dto.InvestmentPerformanceDTO
import org.moinex.model.enums.NotificationType
import org.moinex.model.investment.InvestmentPerformanceSnapshot
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPurchase
import org.moinex.model.investment.TickerSale
import org.moinex.repository.investment.InvestmentPerformanceSnapshotRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Service
class InvestmentPerformanceService(
    private val snapshotRepository: InvestmentPerformanceSnapshotRepository,
    private val tickerService: TickerService,
    private val bondService: BondService,
    private val tickerPriceHistoryService: TickerPriceHistoryService,
    private val bondInterestCalculationService: BondInterestCalculationService,
    private val notificationService: NotificationService,
    private val preferencesService: PreferencesService,
    private val clockProvider: ClockProvider,
) {
    private val logger = LoggerFactory.getLogger(InvestmentPerformanceService::class.java)

    private val calculationMutex = Mutex()

    @Transactional
    suspend fun recalculateAllSnapshots(publishNotification: Boolean = false) =
        calculationMutex.withLock {
            logger.info("Starting investment performance recalculation...")

            snapshotRepository.deleteAll()

            val investmentPerformanceDTO = computePerformance()

            val currentMonth = clockProvider.currentMonth()
            val allMonths = (Constants.XYBAR_CHART_MONTHS downTo 0).map { currentMonth.minusMonths(it.toLong()) }

            savePerformanceSnapshots(allMonths, investmentPerformanceDTO)

            logger.info("Investment performance recalculation completed successfully")

            if (publishNotification) {
                notificationService.send(
                    type = NotificationType.INFO,
                    title =
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_PERFORMANCE_CALCULATION_TITLE),
                    message =
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_PERFORMANCE_CALCULATION_MESSAGE),
                )
            }
        }

    @Transactional
    fun getPerformanceData(): InvestmentPerformanceDTO {
        val currentMonth = clockProvider.currentMonth()
        val allMonths = (Constants.XYBAR_CHART_MONTHS downTo 0).map { currentMonth.minusMonths(it.toLong()) }

        val monthlyInvested = mutableMapOf<YearMonth, BigDecimal>()
        val accumulatedGains = mutableMapOf<YearMonth, BigDecimal>()
        val monthlyGains = mutableMapOf<YearMonth, BigDecimal>()
        val portfolioValues = mutableMapOf<YearMonth, BigDecimal>()

        val hasCache = hasSnapshots()

        if (hasCache) {
            val missingMonths = mutableListOf<YearMonth>()

            allMonths.forEach { month ->
                val snapshot = getSnapshot(month)
                if (snapshot != null) {
                    monthlyInvested[month] = snapshot.investedValue
                    portfolioValues[month] = snapshot.portfolioValue
                    accumulatedGains[month] = snapshot.accumulatedCapitalGains
                    monthlyGains[month] = snapshot.monthlyCapitalGains
                } else {
                    missingMonths.add(month)
                }
            }

            if (missingMonths.isNotEmpty()) {
                logger.info("Found {} missing months in cache, calculating...", missingMonths.size)

                val computed = computePerformance()
                val calculatedInvested = computed.monthlyInvested
                val calculatedAccGains = computed.accumulatedGains
                val calculatedMonthlyGains = computed.monthlyGains
                val calculatedPortfolio = computed.portfolioValues

                missingMonths.forEach { month ->
                    val invested = calculatedInvested.getOrDefault(month, BigDecimal.ZERO)
                    val portfolio = calculatedPortfolio.getOrDefault(month, BigDecimal.ZERO)
                    val accGains = calculatedAccGains.getOrDefault(month, BigDecimal.ZERO)
                    val monthGains = calculatedMonthlyGains.getOrDefault(month, BigDecimal.ZERO)

                    monthlyInvested[month] = invested
                    portfolioValues[month] = portfolio
                    accumulatedGains[month] = accGains
                    monthlyGains[month] = monthGains

                    saveSnapshot(
                        InvestmentPerformanceSnapshot(
                            referenceMonth = month,
                            investedValue = invested,
                            portfolioValue = portfolio,
                            accumulatedCapitalGains = accGains,
                            monthlyCapitalGains = monthGains,
                            calculatedAt = clockProvider.now(),
                        ),
                    )
                }
                logger.info("Missing months calculated and saved to cache")
            }
        } else {
            logger.info("No cache found, calculating all data...")
            val computed = computePerformance()
            monthlyInvested.putAll(computed.monthlyInvested)
            accumulatedGains.putAll(computed.accumulatedGains)
            monthlyGains.putAll(computed.monthlyGains)
            portfolioValues.putAll(computed.portfolioValues)

            val investmentPerformanceDTO =
                InvestmentPerformanceDTO(
                    monthlyInvested = monthlyInvested,
                    portfolioValues = portfolioValues,
                    accumulatedGains = accumulatedGains,
                    monthlyGains = monthlyGains,
                )

            savePerformanceSnapshots(allMonths, investmentPerformanceDTO)
            logger.info("Investment performance data calculated and saved to cache")
        }

        return InvestmentPerformanceDTO(monthlyInvested, portfolioValues, accumulatedGains, monthlyGains)
    }

    private fun getSnapshot(referenceMonth: YearMonth): InvestmentPerformanceSnapshot? =
        snapshotRepository.findByReferenceMonth(referenceMonth)

    private fun hasSnapshots(): Boolean = snapshotRepository.count() > 0

    private fun savePerformanceSnapshots(
        months: List<YearMonth>,
        investmentPerformanceDTO: InvestmentPerformanceDTO,
    ) {
        months.forEach { month ->
            val invested = investmentPerformanceDTO.monthlyInvested.getOrDefault(month, BigDecimal.ZERO)
            val portfolio = investmentPerformanceDTO.portfolioValues.getOrDefault(month, BigDecimal.ZERO)
            val accGains = investmentPerformanceDTO.accumulatedGains.getOrDefault(month, BigDecimal.ZERO)
            val monthGains = investmentPerformanceDTO.monthlyGains.getOrDefault(month, BigDecimal.ZERO)

            saveSnapshot(
                InvestmentPerformanceSnapshot(
                    referenceMonth = month,
                    investedValue = invested,
                    portfolioValue = portfolio,
                    accumulatedCapitalGains = accGains,
                    monthlyCapitalGains = monthGains,
                    calculatedAt = clockProvider.now(),
                ),
            )

            logger.debug(
                "Saved snapshot for {}/{}: invested={}, portfolio={}, accGains={}, monthGains={}",
                month.month,
                month.year,
                invested,
                portfolio,
                accGains,
                monthGains,
            )
        }
    }

    private fun saveSnapshot(snapshot: InvestmentPerformanceSnapshot): InvestmentPerformanceSnapshot {
        val existing = snapshotRepository.findByReferenceMonth(snapshot.referenceMonth)

        val entity =
            existing?.apply {
                investedValue = snapshot.investedValue
                portfolioValue = snapshot.portfolioValue
                accumulatedCapitalGains = snapshot.accumulatedCapitalGains
                monthlyCapitalGains = snapshot.monthlyCapitalGains
                calculatedAt = clockProvider.now()
            } ?: snapshot

        return snapshotRepository.save(entity)
    }

    private fun computePerformance(): InvestmentPerformanceDTO {
        val today = clockProvider.today()
        val currentMonth = clockProvider.currentMonth()
        val tickers = tickerService.getAllNonArchivedTickers()
        val purchasesByTicker = tickerService.getAllPurchases().groupBy { it.ticker.id!! }
        val salesByTicker = tickerService.getAllSales().groupBy { it.ticker.id!! }
        val dividendsByTicker = tickerService.getAllDividends().groupBy { it.ticker.id!! }
        val bonds = bondService.getAllNonArchivedBonds()
        val bondHistories = bonds.map { bondService.getMonthlyInterestHistory(it.id!!) }
        val bondOperations = bonds.map { bondService.getOperationsByBond(it) }
        val bondFacts =
            bondOperations.zip(bondHistories).map { (operations, history) ->
                InvestmentPerformanceCalculator.BondFacts(
                    investedOn = { date -> operations.calculateInvestedAmountUntil(date) },
                    interestHeldAt = { month ->
                        history
                            .filter { !it.referenceMonth.isAfter(month) }
                            .maxByOrNull { it.referenceMonth }
                            ?.accumulatedInterest ?: BigDecimal.ZERO
                    },
                    flows =
                        operations.map { operation ->
                            val base = operation.quantity.multiply(operation.unitPrice)
                            InvestmentPerformanceCalculator.Payout(
                                operation.walletTransaction!!.date.toLocalDate(),
                                if (operation.isPurchase()) base else base.add(operation.realizedInterest()).negate(),
                            )
                        },
                )
            }

        val tickerFacts =
            tickers
                .filter { (purchasesByTicker[it.id!!].orEmpty().isNotEmpty()) || it.currentQuantity > BigDecimal.ZERO }
                .map { ticker ->
                    val purchases = purchasesByTicker[ticker.id!!].orEmpty()
                    val sales = salesByTicker[ticker.id!!].orEmpty()
                    val facts =
                        InvestmentPerformanceCalculator.TickerFacts(
                            openingQuantity = calculateInitialQuantity(ticker, purchases, sales),
                            openingUnitCost = ticker.averageUnitValue,
                            purchases = purchases.map { it.toTrade() },
                            sales = sales.map { it.toTrade() },
                            payouts =
                                dividendsByTicker[ticker.id!!].orEmpty().map {
                                    InvestmentPerformanceCalculator.Payout(
                                        it.walletTransaction!!.date.toLocalDate(),
                                        it.walletTransaction!!.amount,
                                    )
                                },
                            priceAt = { date -> tickerPriceHistoryService.getClosestPriceBeforeDate(ticker, date) },
                        )
                    facts to firstMonthOf(ticker, purchases)
                }

        val firstMonth =
            (
                tickerFacts.map { it.second } +
                    dividendsByTicker.values.flatten().map { YearMonth.from(it.walletTransaction!!.date) } +
                    bondOperations.flatten().map { YearMonth.from(it.walletTransaction!!.date) }
            ).minOrNull() ?: currentMonth

        val months = generateSequence(firstMonth) { it.plusMonths(1) }.takeWhile { !it.isAfter(currentMonth) }.toList()

        val byMonth = months.associateWith { InvestmentPerformanceCalculator.MonthResult() }.toMutableMap()

        tickerFacts.forEach { (facts, _) ->
            InvestmentPerformanceCalculator.tickerMonths(facts, months, today).forEach { (month, result) ->
                byMonth[month] = byMonth.getValue(month) + result
            }
        }

        bondFacts.forEach { facts ->
            InvestmentPerformanceCalculator.bondMonths(facts, months, today).forEach { (month, result) ->
                byMonth[month] = byMonth.getValue(month) + result
            }
        }

        var accumulated = BigDecimal.ZERO
        val accumulatedGains = mutableMapOf<YearMonth, BigDecimal>()
        months.forEach { month ->
            accumulated = accumulated.add(byMonth.getValue(month).gain)
            accumulatedGains[month] = accumulated
        }

        return InvestmentPerformanceDTO(
            monthlyInvested = byMonth.mapValues { it.value.invested },
            portfolioValues = byMonth.mapValues { it.value.value },
            accumulatedGains = accumulatedGains,
            monthlyGains = byMonth.mapValues { it.value.gain },
        )
    }

    private fun firstMonthOf(
        ticker: Ticker,
        purchases: List<TickerPurchase>,
    ): YearMonth = YearMonth.from(determineTickerReferenceDate(ticker, purchases))

    private fun TickerPurchase.toTrade() =
        InvestmentPerformanceCalculator.Trade(walletTransaction!!.date.toLocalDate(), quantity, unitPrice)

    private fun TickerSale.toTrade() =
        InvestmentPerformanceCalculator.Trade(walletTransaction!!.date.toLocalDate(), quantity, unitPrice)

    private fun determineTickerReferenceDate(
        ticker: Ticker,
        purchases: List<TickerPurchase>,
    ): LocalDate? =
        purchases.minOfOrNull { it.walletTransaction!!.date.toLocalDate() }
            ?: ticker.createdAt.toLocalDate()

    private fun calculateInitialQuantity(
        ticker: Ticker,
        purchases: List<TickerPurchase>,
        sales: List<TickerSale>,
    ): BigDecimal =
        ticker.currentQuantity
            .subtract(purchases.sumOf { it.quantity })
            .add(sales.sumOf { it.quantity })
}
