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
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.getEffectiveEndDate
import org.moinex.common.extension.isBeforeOrEqual
import org.moinex.common.extension.isNotZero
import org.moinex.model.dto.InvestmentPerformanceDTO
import org.moinex.model.enums.NotificationType
import org.moinex.model.enums.OperationType
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondOperation
import org.moinex.model.investment.Dividend
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
import java.time.LocalDateTime
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
) {
    private val logger = LoggerFactory.getLogger(InvestmentPerformanceService::class.java)

    private val calculationMutex = Mutex()

    @Transactional
    suspend fun recalculateAllSnapshots() =
        calculationMutex.withLock {
            logger.info("Starting investment performance recalculation...")

            snapshotRepository.deleteAll()

            val investmentPerformanceDTO =
                InvestmentPerformanceDTO(
                    monthlyInvested = calculateMonthlyInvestedValue(),
                    portfolioValues = calculateMonthlyPortfolioValue(),
                    accumulatedGains = calculateAccumulatedCapitalGains(),
                    monthlyGains = calculateMonthlyCapitalGains(),
                )

            val currentMonth = YearMonth.now()
            val allMonths = (Constants.XYBAR_CHART_MONTHS downTo 0).map { currentMonth.minusMonths(it.toLong()) }

            savePerformanceSnapshots(allMonths, investmentPerformanceDTO)

            logger.info("Investment performance recalculation completed successfully")

            notificationService.send(
                type = NotificationType.INFO,
                title =
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_PERFORMANCE_CALCULATION_TITLE),
                message =
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_PERFORMANCE_CALCULATION_MESSAGE),
            )
        }

    @Transactional
    fun getPerformanceData(): InvestmentPerformanceDTO {
        val currentMonth = YearMonth.now()
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

                val calculatedInvested = calculateMonthlyInvestedValue()
                val calculatedAccGains = calculateAccumulatedCapitalGains()
                val calculatedMonthlyGains = calculateMonthlyCapitalGains()
                val calculatedPortfolio = calculateMonthlyPortfolioValue()

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
                            calculatedAt = LocalDateTime.now(),
                        ),
                    )
                }
                logger.info("Missing months calculated and saved to cache")
            }
        } else {
            logger.info("No cache found, calculating all data...")
            monthlyInvested.putAll(calculateMonthlyInvestedValue())
            accumulatedGains.putAll(calculateAccumulatedCapitalGains())
            monthlyGains.putAll(calculateMonthlyCapitalGains())
            portfolioValues.putAll(calculateMonthlyPortfolioValue())

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

    private fun calculateMonthlyInvestedValue(): Map<YearMonth, BigDecimal> {
        val result = mutableMapOf<YearMonth, BigDecimal>()

        processTickersWithTransactions { ticker, purchases, sales ->
            val referenceDate = determineTickerReferenceDate(ticker, purchases) ?: return@processTickersWithTransactions
            val firstMonth = YearMonth.from(referenceDate)
            val quantityByMonth = calculateQuantityAtMonthEnd(ticker, purchases, sales, firstMonth)

            quantityByMonth.forEach { (month, quantity) ->
                if (quantity > BigDecimal.ZERO) {
                    val investedValue = ticker.averageUnitValue.multiply(quantity)
                    result.merge(month, investedValue, BigDecimal::add)
                }
            }
        }

        processBondsWithOperations { operations ->
            val bondInvestedByMonth = calculateBondCumulativeValueByMonth(operations)
            bondInvestedByMonth.forEach { (month, value) ->
                result.merge(month, value, BigDecimal::add)
            }
        }

        return result
    }

    private fun calculateMonthlyCapitalGains(): Map<YearMonth, BigDecimal> {
        val monthlyGains = mutableMapOf<YearMonth, BigDecimal>()
        val allBonds = bondService.getAllNonArchivedBonds()

        tickerService
            .getAllDividends()
            .groupBy { YearMonth.from(it.walletTransaction!!.date) }
            .forEach { (month, dividends) ->
                monthlyGains.merge(month, dividends.sumOf { it.walletTransaction!!.amount }, BigDecimal::add)
            }

        bondService
            .getAllOperations()
            .filter { it.isSale() }
            .groupBy { YearMonth.from(it.walletTransaction!!.date) }
            .forEach { (month, operations) ->
                monthlyGains.merge(month, operations.sumOf { it.netProfit }, BigDecimal::add)
            }

        tickerService.getAllNonArchivedSales().forEach { tickerSale ->
            val month = YearMonth.from(tickerSale.walletTransaction!!.date)
            val saleValue = tickerSale.unitPrice.multiply(tickerSale.quantity)
            val costBasis = tickerSale.averageCost.multiply(tickerSale.quantity)
            val profitLoss = saleValue.subtract(costBasis)
            monthlyGains.merge(month, profitLoss, BigDecimal::add)
        }

        calculateTickerAppreciationByMonth().forEach { (month, gains) ->
            monthlyGains.merge(month, gains, BigDecimal::add)
        }

        val interestHistoryByBond =
            allBonds.associate { bond ->
                bond.id!! to bondService.getMonthlyInterestHistory(bond.id!!)
            }

        interestHistoryByBond.values.flatten().forEach { calculation ->
            calculation.referenceMonth.let { month ->
                monthlyGains.merge(month, calculation.monthlyInterest, BigDecimal::add)
            }
        }

        return monthlyGains
    }

    private fun calculateAccumulatedCapitalGains(): Map<YearMonth, BigDecimal> {
        val accumulatedGains = mutableMapOf<YearMonth, BigDecimal>()

        val allTickers = tickerService.getAllNonArchivedTickers()
        val allDividends = tickerService.getAllDividends()
        val allBonds = bondService.getAllNonArchivedBonds()

        val purchasesByTicker = tickerService.getAllPurchases().groupBy { it.ticker.id!! }
        val salesByTicker = tickerService.getAllSales().groupBy { it.ticker.id!! }
        val interestHistoryByBond =
            allBonds.associate { bond ->
                bond.id!! to bondService.getMonthlyInterestHistory(bond.id!!)
            }

        val currentMonth = YearMonth.now()
        var month = determineFirstMonth(allTickers, allDividends, allBonds)

        while (month.isBeforeOrEqual(currentMonth)) {
            var monthAccumulatedGain = BigDecimal.ZERO

            allTickers.forEach { ticker ->
                val purchases = purchasesByTicker[ticker.id!!] ?: emptyList()
                val sales = salesByTicker[ticker.id!!] ?: emptyList()

                val monthEnd = month.getEffectiveEndDate()
                val quantityAtMonthEnd = calculateQuantityAtDate(ticker, purchases, sales, monthEnd)

                if (quantityAtMonthEnd > BigDecimal.ZERO) {
                    val costBasis = ticker.averageUnitValue.multiply(quantityAtMonthEnd)
                    val priceAtMonthEnd = tickerPriceHistoryService.getClosestPriceBeforeDate(ticker, monthEnd)

                    if (priceAtMonthEnd != null) {
                        val currentValue = priceAtMonthEnd.multiply(quantityAtMonthEnd)
                        val unrealizedGain = currentValue.subtract(costBasis)
                        monthAccumulatedGain = monthAccumulatedGain.add(unrealizedGain)
                    }
                }
            }

            val accumulatedDividends =
                allDividends
                    .asSequence()
                    .filter {
                        it.walletTransaction!!
                            .date
                            .toLocalDate()
                            .isBeforeOrEqual(month.atEndOfMonth())
                    }.sumOf { it.walletTransaction!!.amount }

            monthAccumulatedGain = monthAccumulatedGain.add(accumulatedDividends)

            val bondAccumulated =
                interestHistoryByBond.values
                    .flatten()
                    .filter { it.referenceMonth.isBeforeOrEqual(month) }
                    .maxByOrNull { it.referenceMonth }
                    ?.accumulatedInterest ?: BigDecimal.ZERO

            monthAccumulatedGain = monthAccumulatedGain.add(bondAccumulated)

            accumulatedGains[month] = monthAccumulatedGain
            month = month.plusMonths(1)
        }

        return accumulatedGains
    }

    private fun calculateMonthlyPortfolioValue(): Map<YearMonth, BigDecimal> {
        val result = mutableMapOf<YearMonth, BigDecimal>()

        processTickersWithTransactions { ticker, purchases, sales ->
            val referenceDate = determineTickerReferenceDate(ticker, purchases) ?: return@processTickersWithTransactions
            val firstMonth = YearMonth.from(referenceDate)
            val quantityAtMonthEnd = calculateQuantityAtMonthEnd(ticker, purchases, sales, firstMonth)

            quantityAtMonthEnd.forEach { (month, quantity) ->
                if (quantity > BigDecimal.ZERO) {
                    val endDate = month.getEffectiveEndDate()
                    val price = tickerPriceHistoryService.getClosestPriceBeforeDate(ticker, endDate)

                    if (price != null) {
                        val value = quantity.multiply(price)
                        result.merge(month, value, BigDecimal::add)
                    }
                }
            }
        }

        processBondsWithOperations { operations ->
            val bondPortfolioByMonth = calculateBondCumulativeValueByMonth(operations)
            bondPortfolioByMonth.forEach { (month, value) ->
                result.merge(month, value, BigDecimal::add)
            }
        }

        bondService.getAllNonArchivedBonds().forEach { bond ->
            val interestHistory = bondInterestCalculationService.getMonthlyInterestHistory(bond)
            interestHistory.forEach { interest ->
                interest.referenceMonth.let { month ->
                    result.merge(month, interest.accumulatedInterest, BigDecimal::add)
                }
            }
        }

        return result
    }

    private fun calculateTickerAppreciationByMonth(): Map<YearMonth, BigDecimal> {
        val result = mutableMapOf<YearMonth, BigDecimal>()

        processTickersWithTransactions { ticker, purchases, sales ->
            val referenceDate = determineTickerReferenceDate(ticker, purchases) ?: return@processTickersWithTransactions
            val firstMonth = YearMonth.from(referenceDate)
            val currentMonth = YearMonth.now()

            var month = firstMonth
            while (month.isBeforeOrEqual(currentMonth)) {
                val monthAppreciation =
                    calculateMonthAppreciationWithIntraMonthTransactions(ticker, purchases, sales, month)

                if (monthAppreciation.isNotZero()) {
                    result.merge(month, monthAppreciation, BigDecimal::add)
                }

                month = month.plusMonths(1)
            }
        }

        return result
    }

    private fun calculateMonthAppreciationWithIntraMonthTransactions(
        ticker: Ticker,
        purchases: List<TickerPurchase>,
        sales: List<TickerSale>,
        month: YearMonth,
    ): BigDecimal {
        val transactionDatesInMonth =
            buildList {
                add(month.atDay(1))
                addAll(
                    purchases
                        .map { it.walletTransaction!!.date.toLocalDate() }
                        .filter { YearMonth.from(it) == month },
                )
                addAll(
                    sales
                        .map { it.walletTransaction!!.date.toLocalDate() }
                        .filter { YearMonth.from(it) == month },
                )
                add(month.getEffectiveEndDate())
            }

        val sortedDates = transactionDatesInMonth.distinct().sorted()

        return sortedDates
            .zipWithNext()
            .mapNotNull { (periodStart, periodEnd) ->
                val periodQuantity = calculateQuantityAtDate(ticker, purchases, sales, periodEnd)

                if (periodQuantity <= BigDecimal.ZERO) return@mapNotNull null

                val startPrice = tickerPriceHistoryService.getClosestPriceBeforeDate(ticker, periodStart)
                val endPrice = tickerPriceHistoryService.getClosestPriceBeforeDate(ticker, periodEnd)

                if (startPrice != null && endPrice != null) {
                    endPrice.subtract(startPrice).multiply(periodQuantity)
                } else {
                    null
                }
            }.fold(BigDecimal.ZERO, BigDecimal::add)
    }

    private fun calculateQuantityAtDate(
        ticker: Ticker,
        purchases: List<TickerPurchase>,
        sales: List<TickerSale>,
        date: LocalDate,
    ): BigDecimal {
        val initialQuantity = calculateInitialQuantity(ticker, purchases, sales)

        val purchasesBeforeDate =
            purchases
                .filter {
                    it.walletTransaction!!
                        .date
                        .toLocalDate()
                        .isBeforeOrEqual(date)
                }.sumOf { it.quantity }

        val salesBeforeDate =
            sales
                .filter {
                    it.walletTransaction!!
                        .date
                        .toLocalDate()
                        .isBeforeOrEqual(date)
                }.sumOf { it.quantity }

        return initialQuantity.add(purchasesBeforeDate).subtract(salesBeforeDate)
    }

    private fun calculateQuantityAtMonthEnd(
        ticker: Ticker,
        purchases: List<TickerPurchase>,
        sales: List<TickerSale>,
        firstMonth: YearMonth,
    ): Map<YearMonth, BigDecimal> {
        val quantityAtEnd = mutableMapOf<YearMonth, BigDecimal>()
        val initialQuantity = calculateInitialQuantity(ticker, purchases, sales)
        var cumulativeQuantity = initialQuantity

        val allTransactions =
            (purchases + sales).sortedBy { transaction ->
                when (transaction) {
                    is TickerPurchase -> transaction.walletTransaction!!.date
                    is TickerSale -> transaction.walletTransaction!!.date
                    else -> LocalDateTime.MIN
                }
            }

        allTransactions.forEach { transaction ->
            when (transaction) {
                is TickerPurchase -> {
                    val month = YearMonth.from(transaction.walletTransaction!!.date)
                    cumulativeQuantity = cumulativeQuantity.add(transaction.quantity)
                    quantityAtEnd[month] = cumulativeQuantity
                }
                is TickerSale -> {
                    val month = YearMonth.from(transaction.walletTransaction!!.date)
                    cumulativeQuantity = cumulativeQuantity.subtract(transaction.quantity)
                    quantityAtEnd[month] = cumulativeQuantity
                }
            }
        }

        val currentMonth = YearMonth.now()
        var month = firstMonth
        var lastQuantity = initialQuantity

        while (month.isBeforeOrEqual(currentMonth)) {
            quantityAtEnd[month]?.let {
                lastQuantity = it
            } ?: run {
                quantityAtEnd[month] = lastQuantity
            }
            month = month.plusMonths(1)
        }

        return quantityAtEnd
    }

    private fun determineFirstMonth(
        allTickers: List<Ticker>,
        allDividends: List<Dividend>,
        allBonds: List<Bond>,
    ): YearMonth {
        val tickerMonths =
            allTickers.flatMap { ticker ->
                tickerService.getAllPurchasesByTicker(ticker.id!!).map {
                    YearMonth.from(it.walletTransaction!!.date)
                }
            }

        val dividendMonths =
            allDividends.map {
                YearMonth.from(it.walletTransaction!!.date)
            }

        val bondMonths =
            allBonds.flatMap { bond ->
                bondService.getOperationsByBond(bond).map {
                    YearMonth.from(it.walletTransaction!!.date)
                }
            }

        return (tickerMonths + dividendMonths + bondMonths).minOrNull() ?: YearMonth.now()
    }

    private fun calculateBondCumulativeValueByMonth(operations: List<BondOperation>): Map<YearMonth, BigDecimal> {
        val result = mutableMapOf<YearMonth, BigDecimal>()

        val firstOperationDate =
            operations
                .minOfOrNull { it.walletTransaction!!.date.toLocalDate() }
                ?: return result

        val firstMonth = YearMonth.from(firstOperationDate)
        val currentMonth = YearMonth.now()

        var cumulativeValue = BigDecimal.ZERO
        var month = firstMonth

        while (month.isBeforeOrEqual(currentMonth)) {
            operations.forEach { bondOperation ->
                val opMonth = YearMonth.from(bondOperation.walletTransaction!!.date)
                if (opMonth == month) {
                    val opValue = bondOperation.unitPrice.multiply(bondOperation.quantity)
                    cumulativeValue =
                        when (bondOperation.operationType) {
                            OperationType.BUY -> cumulativeValue.add(opValue)
                            OperationType.SELL -> cumulativeValue.subtract(opValue)
                        }
                }
            }

            if (cumulativeValue > BigDecimal.ZERO) {
                result[month] = cumulativeValue
            }

            month = month.plusMonths(1)
        }

        return result
    }

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
                    calculatedAt = LocalDateTime.now(),
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
                calculatedAt = LocalDateTime.now()
            } ?: snapshot

        return snapshotRepository.save(entity)
    }

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

    private inline fun processTickersWithTransactions(
        action: (Ticker, List<TickerPurchase>, List<TickerSale>) -> Unit,
    ) {
        val allTickers = tickerService.getAllNonArchivedTickers()
        val purchasesByTicker = tickerService.getAllPurchases().groupBy { it.ticker.id!! }
        val salesByTicker = tickerService.getAllSales().groupBy { it.ticker.id!! }

        allTickers.forEach { ticker ->
            val purchases = purchasesByTicker[ticker.id!!] ?: emptyList()
            val sales = salesByTicker[ticker.id!!] ?: emptyList()

            if (purchases.isEmpty() && ticker.currentQuantity <= BigDecimal.ZERO) {
                return@forEach
            }

            action(ticker, purchases, sales)
        }
    }

    private inline fun processBondsWithOperations(action: (List<BondOperation>) -> Unit) {
        bondService.getAllNonArchivedBonds().forEach { bond ->
            val operations = bondService.getOperationsByBond(bond)
            if (operations.isEmpty()) return@forEach
            action(operations)
        }
    }
}
