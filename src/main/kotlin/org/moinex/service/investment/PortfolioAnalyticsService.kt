/*
 * Filename: PortfolioAnalyticsService.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import org.moinex.common.ClockProvider
import org.moinex.model.dto.AllocationDTO
import org.moinex.model.dto.DailyPricePointDTO
import org.moinex.model.dto.DividendPanelDTO
import org.moinex.model.dto.DividendSeriesDTO
import org.moinex.model.dto.PortfolioSnapshotDTO
import org.moinex.model.dto.RebalanceAction
import org.moinex.model.dto.RebalanceRowDTO
import org.moinex.model.dto.TickerDetailDTO
import org.moinex.model.dto.TickerIncomeDTO
import org.moinex.model.dto.TickerPerformanceDTO
import org.moinex.model.dto.TickerPositionDTO
import org.moinex.model.dto.TickerRowDTO
import org.moinex.model.dto.TickerSort
import org.moinex.model.dto.TradeMarkerDTO
import org.moinex.model.dto.TradeSide
import org.moinex.model.enums.AssetType
import org.moinex.model.investment.Dividend
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPurchase
import org.moinex.model.investment.TickerSale
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Numbers about the user's portfolio that are worth more than a column of prices: return, weight, dividend yield on
 * cost and the day change, ready for the screens to show.
 */
@Service
class PortfolioAnalyticsService(
    private val tickerRepository: TickerRepository,
    private val dividendRepository: DividendRepository,
    private val tickerPurchaseRepository: TickerPurchaseRepository,
    private val tickerSaleRepository: TickerSaleRepository,
    private val tickerPriceHistoryService: TickerPriceHistoryService,
    private val clockProvider: ClockProvider,
) {
    companion object {
        const val SERIES_MONTHS = 12L
        const val MAX_PREVIOUS_CLOSE_AGE_DAYS = 5L
        const val DIVIDEND_MONTHS = 12
        const val DIVIDEND_MAX_SERIES = 6
        private const val MONEY_SCALE = 2
        private const val PERCENT_SCALE = 2
        private val HUNDRED = BigDecimal(100)

        /**
         * Builds the rows from already loaded data. [series] maps a ticker id to its daily closes, oldest first.
         * Weights are taken over all [tickers], whichever of them the screen ends up showing.
         */
        fun buildRows(
            tickers: List<Ticker>,
            dividends: List<Dividend>,
            series: Map<Int, List<DailyPricePointDTO>>,
            today: LocalDate,
        ): List<TickerRowDTO> {
            val values = tickers.associateWith { it.currentQuantity.multiply(it.currentUnitValue) }
            val total = values.values.fold(BigDecimal.ZERO, BigDecimal::add)
            val dividendsSince = today.minusYears(1)
            val dividendsByTicker =
                dividends
                    .filter {
                        it.walletTransaction != null &&
                            !it.walletTransaction!!
                                .date
                                .toLocalDate()
                                .isBefore(dividendsSince)
                    }.groupBy({ it.ticker.id }, { it.walletTransaction!!.amount })

            return tickers.map { ticker ->
                val value = values.getValue(ticker)
                val cost = ticker.currentQuantity.multiply(ticker.averageUnitValue)
                val dividends12m = dividendsByTicker[ticker.id].orEmpty().fold(BigDecimal.ZERO, BigDecimal::add)
                val points = series[ticker.id].orEmpty()

                TickerRowDTO(
                    ticker = ticker,
                    value = value,
                    profitLoss = value.subtract(cost),
                    returnPct = percent(value.subtract(cost), cost),
                    weightPct = percent(value, total) ?: BigDecimal.ZERO.setScale(PERCENT_SCALE),
                    dyOnCostPct = percent(dividends12m, cost),
                    dayChangePct = dayChange(ticker.currentUnitValue, points, today),
                    sparkline = points.map { it.price },
                )
            }
        }

        /**
         * Builds the detail of one ticker for the last [months] months from already loaded data. Prices and trades
         * before the window are left out; the position numbers always describe the whole position.
         */
        fun buildDetail(
            ticker: Ticker,
            allPoints: List<DailyPricePointDTO>,
            purchases: List<TickerPurchase>,
            sales: List<TickerSale>,
            months: Int,
            today: LocalDate,
            dividends: List<Dividend> = emptyList(),
            portfolioValue: BigDecimal = BigDecimal.ZERO,
        ): TickerDetailDTO {
            val start = today.minusMonths(months.toLong())
            val row = buildRows(listOf(ticker), emptyList(), mapOf(ticker.id!! to allPoints), today).first()
            val trades =
                (
                    purchases.mapNotNull {
                        toTrade(
                            it.walletTransaction?.date,
                            TradeSide.BUY,
                            it.quantity,
                            it.unitPrice,
                        )
                    } +
                        sales.mapNotNull {
                            toTrade(
                                it.walletTransaction?.date,
                                TradeSide.SELL,
                                it.quantity,
                                it.unitPrice,
                            )
                        }
                ).filter { !it.date.isBefore(start) && !it.date.isAfter(today) }
                    .sortedBy { it.date }

            return TickerDetailDTO(
                ticker = ticker,
                prices = allPoints.filter { !it.date.isBefore(start) && !it.date.isAfter(today) },
                trades = trades,
                averagePrice = ticker.averageUnitValue,
                quantity = ticker.currentQuantity,
                profitLoss = row.profitLoss,
                returnPct = row.returnPct,
                dayChangePct = row.dayChangePct,
                position = buildPosition(ticker, row, allPoints, portfolioValue),
                income = buildIncome(ticker, dividends, today),
            )
        }

        private fun buildPosition(
            ticker: Ticker,
            row: TickerRowDTO,
            allPoints: List<DailyPricePointDTO>,
            portfolioValue: BigDecimal,
        ): TickerPositionDTO {
            val highest = allPoints.maxOfOrNull { it.price }
            val lowest = allPoints.minOfOrNull { it.price }

            return TickerPositionDTO(
                value = row.value,
                cost = ticker.currentQuantity.multiply(ticker.averageUnitValue),
                weightPct = percent(row.value, portfolioValue) ?: BigDecimal.ZERO.setScale(PERCENT_SCALE),
                highest12m = highest,
                lowest12m = lowest,
                distanceFromHighestPct =
                    highest?.let { percent(ticker.currentUnitValue.subtract(it), it) },
            )
        }

        private fun buildIncome(
            ticker: Ticker,
            dividends: List<Dividend>,
            today: LocalDate,
        ): TickerIncomeDTO {
            val own = dividends.filter { it.ticker.id == ticker.id && it.walletTransaction != null }
            val panel = buildDividendPanel(own, today)
            val last = own.maxByOrNull { it.walletTransaction!!.date }
            val cost = ticker.currentQuantity.multiply(ticker.averageUnitValue)

            return TickerIncomeDTO(
                panel = panel,
                total12m = panel.total12m,
                totalReceived = own.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.walletTransaction!!.amount) },
                lastAmount = last?.walletTransaction?.amount,
                lastDate = last?.walletTransaction?.date?.toLocalDate(),
                dyOnCostPct = percent(panel.total12m, cost),
            )
        }

        private fun toTrade(
            dateTime: java.time.LocalDateTime?,
            side: TradeSide,
            quantity: BigDecimal,
            price: BigDecimal,
        ): TradeMarkerDTO? = dateTime?.let { TradeMarkerDTO(it.toLocalDate(), side, quantity, price) }

        /**
         * Sums the [dividends] received in the [months] calendar months ending in the month of [today], per ticker and
         * month. At most [maxSeries] tickers get their own series, the biggest payers; the rest is added up in one.
         */
        fun buildDividendPanel(
            dividends: List<Dividend>,
            today: LocalDate,
            months: Int = DIVIDEND_MONTHS,
            maxSeries: Int = DIVIDEND_MAX_SERIES,
        ): DividendPanelDTO {
            val currentMonth = YearMonth.from(today)
            val monthList = (months - 1 downTo 0).map { currentMonth.minusMonths(it.toLong()) }
            val bySymbol =
                dividends
                    .mapNotNull { dividend ->
                        dividend.walletTransaction?.let {
                            Triple(
                                dividend.ticker.symbol,
                                YearMonth.from(it.date),
                                it.amount,
                            )
                        }
                    }.filter { it.second in monthList }
                    .groupBy { it.first }
                    .mapValues { (_, items) ->
                        monthList.map { month ->
                            items.filter { it.second == month }.fold(BigDecimal.ZERO) { a, i -> a.add(i.third) }
                        }
                    }

            val ranked =
                bySymbol.entries.sortedByDescending { entry ->
                    entry.value.fold(
                        BigDecimal.ZERO,
                        BigDecimal::add,
                    )
                }
            val series =
                ranked.take(maxSeries).map { DividendSeriesDTO(it.key, it.value) } +
                    ranked
                        .drop(maxSeries)
                        .takeIf { it.isNotEmpty() }
                        ?.let { rest ->
                            listOf(
                                DividendSeriesDTO(
                                    null,
                                    monthList.indices.map { index ->
                                        rest.fold(BigDecimal.ZERO) { acc, entry -> acc.add(entry.value[index]) }
                                    },
                                ),
                            )
                        }.orEmpty()

            val total = series.flatMap { it.values }.fold(BigDecimal.ZERO, BigDecimal::add)

            return DividendPanelDTO(
                months = monthList,
                series = series,
                total12m = total,
                monthlyAverage = total.divide(BigDecimal(months), PERCENT_SCALE, RoundingMode.HALF_UP),
                projection12m = total,
            )
        }

        /**
         * Suggests, for each asset type with a target, how much to buy or sell to reach the targets. Only the types
         * with a target count, so the amounts are relative to the value held in them. Without a [contribution] the
         * excess is sold and the shortfall bought; with one only purchases are suggested, spread over the types in
         * proportion to how far below their target they would be after the contribution. Taxes and costs are ignored.
         */
        fun rebalance(
            allocations: List<AllocationDTO>,
            contribution: BigDecimal?,
        ): List<RebalanceRowDTO> {
            val targets = allocations.filter { it.targetPercentage.signum() > 0 }
            if (targets.isEmpty()) return emptyList()

            val targetSum = targets.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.targetPercentage) }
            val held = targets.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.currentValue) }
            val money = contribution?.takeIf { it.signum() > 0 } ?: BigDecimal.ZERO
            val base = held.add(money)

            val deltas =
                targets.map {
                    base
                        .multiply(
                            it.targetPercentage,
                        ).divide(targetSum, MONEY_SCALE + 4, RoundingMode.HALF_UP)
                        .subtract(it.currentValue)
                }
            val shortfall = deltas.fold(BigDecimal.ZERO) { acc, delta -> acc.add(delta.max(BigDecimal.ZERO)) }

            return targets.mapIndexed { index, allocation ->
                val signed =
                    when {
                        money.signum() == 0 -> deltas[index]
                        shortfall.signum() == 0 -> BigDecimal.ZERO
                        else ->
                            money
                                .multiply(
                                    deltas[index].max(BigDecimal.ZERO),
                                ).divide(shortfall, MONEY_SCALE + 4, RoundingMode.HALF_UP)
                    }.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                val action =
                    when {
                        signed.signum() > 0 -> RebalanceAction.BUY
                        signed.signum() < 0 -> RebalanceAction.SELL
                        else -> RebalanceAction.NONE
                    }

                RebalanceRowDTO(
                    assetType = allocation.assetType,
                    typeName = allocation.typeName,
                    currentPct = allocation.currentPercentage,
                    targetPct = allocation.targetPercentage,
                    deviationPp = allocation.difference,
                    action = action,
                    amount = signed.abs(),
                )
            }
        }

        /**
         * The best (or worst) performing positions, by return since purchase. Only positions that gained count as best
         * and only the ones that lost count as worst, and sold-out positions never count.
         */
        fun topPerformers(
            rows: List<TickerRowDTO>,
            best: Boolean,
            limit: Int,
        ): List<TickerPerformanceDTO> {
            val held = rows.filter { it.ticker.currentQuantity.signum() > 0 && it.returnPct != null }
            val selected =
                if (best) {
                    held.filter { it.returnPct!!.signum() > 0 }.sortedByDescending { it.returnPct }
                } else {
                    held.filter { it.returnPct!!.signum() < 0 }.sortedBy { it.returnPct }
                }

            return selected.take(limit).map {
                TickerPerformanceDTO(it.ticker.name, it.ticker.symbol, it.returnPct!!, it.profitLoss, it.value)
            }
        }

        fun filterAndSort(
            rows: List<TickerRowDTO>,
            type: AssetType?,
            sort: TickerSort,
        ): List<TickerRowDTO> {
            val filtered = rows.filter { type == null || it.ticker.type == type }

            return when (sort) {
                TickerSort.NAME -> filtered.sortedBy { it.ticker.name.lowercase() }
                TickerSort.RETURN -> filtered.sortedWith(compareByDescendingNullsLast { it.returnPct })
                TickerSort.WEIGHT -> filtered.sortedByDescending { it.weightPct }
                TickerSort.DAY_CHANGE -> filtered.sortedWith(compareByDescendingNullsLast { it.dayChangePct })
            }
        }

        private fun <T> compareByDescendingNullsLast(selector: (T) -> BigDecimal?): Comparator<T> =
            Comparator { a, b ->
                val left = selector(a)
                val right = selector(b)
                when {
                    left == null && right == null -> 0
                    left == null -> 1
                    right == null -> -1
                    else -> right.compareTo(left)
                }
            }

        /**
         * Change of the last trading session. When the series already has today, today's price is the current one;
         * otherwise (weekend, holiday, series not yet synced) the last stored close stands for the latest session.
         * Without two closes close enough in time there is no day change.
         */
        private fun dayChange(
            currentPrice: BigDecimal,
            points: List<DailyPricePointDTO>,
            today: LocalDate,
        ): BigDecimal? {
            if (points.size < 2) return null

            val latest = points.last()
            val previous = points[points.size - 2]
            val latestIsFresh = ChronoUnit.DAYS.between(latest.date, today) <= MAX_PREVIOUS_CLOSE_AGE_DAYS
            val sessionsAreClose = ChronoUnit.DAYS.between(previous.date, latest.date) <= MAX_PREVIOUS_CLOSE_AGE_DAYS
            if (!latestIsFresh || !sessionsAreClose) return null

            val latestPrice = if (latest.date == today) currentPrice else latest.price

            return percent(latestPrice.subtract(previous.price), previous.price)
        }

        private fun percent(
            part: BigDecimal,
            whole: BigDecimal,
        ): BigDecimal? =
            if (whole.signum() <= 0) {
                null
            } else {
                part.multiply(HUNDRED).divide(whole, PERCENT_SCALE, RoundingMode.HALF_UP)
            }
    }

    /**
     * The best or worst positions by return, for the overview.
     */
    fun topPerformers(
        best: Boolean,
        limit: Int,
    ): List<TickerPerformanceDTO> =
        topPerformers(
            buildRows(
                tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc(),
                emptyList(),
                emptyMap(),
                clockProvider.today(),
            ),
            best,
            limit,
        )

    /**
     * The dividends of the last twelve months, for the panel of the Performance tab.
     */
    fun dividendPanel(): DividendPanelDTO = buildDividendPanel(dividendRepository.findAll(), clockProvider.today())

    /**
     * The detail of [tickerId] for the last [months] months, or null when the ticker does not exist.
     */
    fun tickerDetail(
        tickerId: Int,
        months: Int,
    ): TickerDetailDTO? {
        val ticker = tickerRepository.findById(tickerId).orElse(null) ?: return null
        val today = clockProvider.today()
        val points =
            tickerPriceHistoryService
                .getPricePoints(listOf(tickerId), today.minusMonths(SERIES_MONTHS), today)[tickerId]
                .orEmpty()

        return buildDetail(
            ticker,
            points,
            tickerPurchaseRepository.findAllByTickerId(tickerId),
            tickerSaleRepository.findAllByTickerId(tickerId),
            months,
            today,
            dividendRepository.findAll().filter { it.ticker.id == tickerId },
            tickerRepository
                .findAllByIsArchivedFalseOrderBySymbolAsc()
                .fold(BigDecimal.ZERO) { acc, it -> acc.add(it.currentQuantity.multiply(it.currentUnitValue)) },
        )
    }

    /**
     * The non-archived tickers with their numbers, keeping only [type] when given and ordered by [sort].
     */
    fun tickerRows(
        type: AssetType? = null,
        sort: TickerSort = TickerSort.NAME,
    ): List<TickerRowDTO> = filterAndSort(portfolioSnapshot().rows, type, sort)

    /**
     * Reads the tickers, their daily prices (in a single query) and the dividends once, for the stocks and funds screen.
     */
    fun portfolioSnapshot(): PortfolioSnapshotDTO {
        val today = clockProvider.today()
        val tickers = tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc()
        val series =
            tickerPriceHistoryService.getPricePoints(
                tickers.mapNotNull { it.id },
                today.minusMonths(SERIES_MONTHS),
                today,
            )
        val dividends = dividendRepository.findAll()

        return PortfolioSnapshotDTO(
            rows = buildRows(tickers, dividends, series, today),
            totalDividends =
                dividends
                    .mapNotNull { it.walletTransaction?.amount }
                    .fold(BigDecimal.ZERO, BigDecimal::add),
        )
    }
}
