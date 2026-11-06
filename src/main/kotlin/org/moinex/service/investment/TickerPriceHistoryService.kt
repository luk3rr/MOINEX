/*
 * Filename: TickerPriceHistoryService.kt (original filename: TickerPriceHistoryService.java)
 * Created on: February 17, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 14/03/2026
 */

package org.moinex.service.investment

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import org.moinex.common.ClockProvider
import org.moinex.common.extension.throwIfError
import org.moinex.common.extension.toNoTimeFormat
import org.moinex.common.util.APIUtils
import org.moinex.model.dto.DailyPricePointDTO
import org.moinex.model.dto.StockHistoryRequest
import org.moinex.model.dto.TickerPriceInitializationStatsDTO
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPriceHistory
import org.moinex.repository.investment.TickerPriceHistoryRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Service
class TickerPriceHistoryService(
    private val priceHistoryRepository: TickerPriceHistoryRepository,
    private val tickerRepository: TickerRepository,
    private val tickerPurchaseRepository: TickerPurchaseRepository,
    private val tickerSaleRepository: TickerSaleRepository,
    private val clockProvider: ClockProvider,
    private val tickerMarketDataService: TickerMarketDataService,
) {
    companion object {
        const val DAILY_WINDOW_MONTHS = 12
        const val DAILY_DENSITY_THRESHOLD = 0.6
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    suspend fun initializePriceHistory() {
        logger.info("Starting smart price history initialization")

        val activeTickers = tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc()

        if (activeTickers.isEmpty()) {
            logger.info("No active tickers found, skipping price history initialization")
            return
        }

        logger.info("Found {} active tickers to process", activeTickers.size)
        logger.debug(
            "Active tickers: {}",
            activeTickers.joinToString(", ") { "$it" },
        )

        val stats = TickerPriceInitializationStatsDTO()

        activeTickers.forEach { ticker ->
            runCatching {
                processTicker(ticker, stats)
            }.onFailure { e ->
                logger.error("Failed to update price history for {}: {}", ticker, e.message)
            }
        }

        logger.info(
            "Price history initialization complete: {} backfilled, {} updated, {} skipped",
            stats.backfillCount,
            stats.updateCount,
            stats.skipCount,
        )
    }

    /**
     * Keeps one closing price per trading day for the last [DAILY_WINDOW_MONTHS] months, for every active ticker (or
     * just [tickers]), with a single script run for all of them. Unless [force] is set, tickers synced less than
     * [MarketDataFreshness.TTL] ago are left alone. A failure on one ticker is logged and never removes data already
     * stored; when Yahoo is cooling down after a rate limit nothing is requested.
     */
    suspend fun syncDailyPriceHistory(
        tickers: List<Ticker>? = null,
        force: Boolean = false,
    ) {
        val candidates = tickers ?: tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc()
        val now = clockProvider.now()
        val targets =
            if (force) {
                candidates
            } else {
                candidates.filterNot {
                    MarketDataFreshness.isFresh(tickerMarketDataService.read(it).historyUpdatedAt, now)
                }
            }

        if (targets.isEmpty()) {
            logger.info("Daily price history is fresh for all {} tickers, nothing to sync", candidates.size)
            return
        }

        val today = clockProvider.today()
        val windowStart = today.minusMonths(DAILY_WINDOW_MONTHS.toLong())

        val requests =
            targets.map { ticker ->
                StockHistoryRequest(
                    symbol = ticker.symbol,
                    start = dailyFetchStart(ticker.id!!, windowStart),
                    end = today.plusDays(1),
                    currency = tickerMarketDataService.read(ticker).currency,
                )
            }

        val results =
            runCatching { APIUtils.fetchStockPriceHistoryBatch(requests) }
                .getOrElse {
                    logger.error("Failed to sync the daily price history of {} tickers: {}", targets.size, it.message)
                    return
                }

        targets.zip(requests).forEach { (ticker, request) ->
            runCatching {
                val result =
                    results.optJSONObject(ticker.symbol)
                        ?: error("API response does not contain data for ${ticker.symbol}")

                check(!result.has("error")) { "${result.getString("error")} [${result.optString("error_kind")}]" }

                val written = storeDailyPrices(ticker, request.start, result.getJSONArray("prices"))
                tickerMarketDataService.recordHistorySync(
                    ticker,
                    clockProvider.now(),
                    result.optString("currency").takeIf { it.isNotBlank() },
                )

                logger.info("Synced {} daily prices for {} starting {}", written, ticker, request.start)
            }.onFailure {
                logger.error("Failed to sync the daily price history of {}: {}", ticker, it.message)
            }
        }
    }

    /**
     * Throws away the stored price history of [ticker] and builds it again from the API: the backfill from the first
     * transaction (transaction dates and month ends) plus the daily window. When anything fails, the history stored
     * before the call is put back, so a failing API never leaves the ticker with less data than it had.
     */
    suspend fun rebuildPriceHistory(ticker: Ticker) {
        val tickerId = ticker.id!!
        val previous =
            priceHistoryRepository.findAllByTickerId(tickerId).map {
                TickerPriceHistory(
                    ticker = it.ticker,
                    priceDate = it.priceDate,
                    closingPrice = it.closingPrice,
                    isMonthEnd = it.isMonthEnd,
                )
            }

        logger.info("Rebuilding the price history of {}, discarding {} stored rows", ticker, previous.size)
        priceHistoryRepository.deleteAllByTickerId(tickerId)

        try {
            processTicker(ticker, TickerPriceInitializationStatsDTO())
            syncDaily(ticker)
        } catch (e: Exception) {
            logger.error(
                "Failed to rebuild the price history of {}, restoring {} rows: {}",
                ticker,
                previous.size,
                e.message,
            )
            priceHistoryRepository.deleteAllByTickerId(tickerId)
            priceHistoryRepository.saveAll(previous)
            throw e
        }
    }

    /**
     * Fetches the trading days missing for [ticker] and upserts them. When the stored series is dense up to its last
     * day only the days since then are fetched, otherwise the whole window is.
     * @return how many rows were written
     */
    suspend fun syncDaily(ticker: Ticker): Int {
        val today = clockProvider.today()
        val windowStart = today.minusMonths(DAILY_WINDOW_MONTHS.toLong())
        val fetchStart = dailyFetchStart(ticker.id!!, windowStart)

        val jsonObject =
            APIUtils
                .fetchStockPriceHistory(ticker.symbol, fetchStart.toNoTimeFormat(), today.plusDays(1).toNoTimeFormat())
                .throwIfError("Fetch daily price history for $ticker")

        val written = storeDailyPrices(ticker, fetchStart, jsonObject.getJSONArray("prices"))
        tickerMarketDataService.recordHistorySync(
            ticker,
            clockProvider.now(),
            jsonObject.optString("currency").takeIf { it.isNotBlank() },
        )

        logger.info("Synced {} daily prices for {} starting {}", written, ticker, fetchStart)

        return written
    }

    /**
     * Upserts the [prices] the script returned for [ticker], read from [fetchStart] on.
     * @return how many rows were written
     */
    private fun storeDailyPrices(
        ticker: Ticker,
        fetchStart: LocalDate,
        prices: JSONArray,
    ): Int {
        if (prices.length() == 0) return 0

        val today = clockProvider.today()
        val existingByDate =
            priceHistoryRepository
                .findByTickerIdAndDateRange(ticker.id!!, fetchStart, today.plusDays(1))
                .associateBy { it.priceDate }
        val currentMonth = clockProvider.currentMonth()

        val rows =
            prices
                .asSequence()
                .map { it as JSONObject }
                .map { toDailyRow(ticker, it, existingByDate, currentMonth) }
                .toList()

        priceHistoryRepository.saveAll(rows)

        return rows.size
    }

    fun getDailyPrices(
        tickerId: Int,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<TickerPriceHistory> = priceHistoryRepository.findByTickerIdAndDateRange(tickerId, startDate, endDate)

    /**
     * Daily closing prices of several tickers in one query, oldest first, keyed by ticker id.
     */
    fun getPricePoints(
        tickerIds: Collection<Int>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Map<Int, List<DailyPricePointDTO>> =
        if (tickerIds.isEmpty()) {
            emptyMap()
        } else {
            priceHistoryRepository
                .findPricePointsByTickerIdsAndDateRange(tickerIds, startDate, endDate)
                .groupBy { it.tickerId }
        }

    private fun toDailyRow(
        ticker: Ticker,
        priceData: JSONObject,
        existingByDate: Map<LocalDate, TickerPriceHistory>,
        currentMonth: YearMonth,
    ): TickerPriceHistory {
        val date = priceData.getString("date").toNoTimeFormat()
        val price = BigDecimal(priceData.getDouble("price"))
        val monthEnd = priceData.getBoolean("is_month_end") && YearMonth.from(date).isBefore(currentMonth)
        val existing = existingByDate[date]

        return if (existing != null) {
            existing.apply {
                closingPrice = price
                isMonthEnd = isMonthEnd || monthEnd
            }
        } else {
            TickerPriceHistory(ticker = ticker, priceDate = date, closingPrice = price, isMonthEnd = monthEnd)
        }
    }

    private fun dailyFetchStart(
        tickerId: Int,
        windowStart: LocalDate,
    ): LocalDate {
        val latest = priceHistoryRepository.findLatestPriceByTicker(tickerId)?.priceDate ?: return windowStart
        if (latest.isBefore(windowStart)) return windowStart

        val storedDays = priceHistoryRepository.countByTickerIdAndDateRange(tickerId, windowStart, latest)
        val spanDays = ChronoUnit.DAYS.between(windowStart, latest) + 1
        val isDense = storedDays >= spanDays * DAILY_DENSITY_THRESHOLD

        return if (isDense) latest else windowStart
    }

    fun getClosestPriceBeforeDate(
        ticker: Ticker,
        date: LocalDate,
    ): BigDecimal? =
        when {
            date == clockProvider.today() -> ticker.currentUnitValue
            else -> getPriceOnDate(ticker.id!!, date)
        }

    private suspend fun processTicker(
        ticker: Ticker,
        stats: TickerPriceInitializationStatsDTO,
    ) {
        val referenceDate =
            determineReferenceDate(ticker) ?: run {
                stats.skipCount++
                return
            }

        logger.debug("Checking {}: referenceDate={}", ticker, referenceDate)

        if (!hasCompleteHistoricalData(ticker.id!!, referenceDate)) {
            backfillPriceHistory(ticker, referenceDate)
            stats.backfillCount++
        } else {
            updateMissingPrices(ticker, stats)
        }
    }

    private fun determineReferenceDate(ticker: Ticker): LocalDate? {
        val firstPurchaseDate = getFirstPurchaseDate(ticker.id!!)
        val createdDate = ticker.createdAt.toLocalDate()

        return when {
            firstPurchaseDate == null -> {
                if (ticker.currentQuantity > BigDecimal.ZERO) {
                    logger.info(
                        "No purchases for {} but has quantity {}, using createdAt {} as start date",
                        ticker,
                        ticker.currentQuantity,
                        createdDate,
                    )
                    createdDate
                } else {
                    logger.info("No purchases and no quantity for {}, skipping", ticker)
                    null
                }
            }
            else -> {
                val initialQuantity = calculateInitialQuantity(ticker.id!!)
                if (initialQuantity > BigDecimal.ZERO) {
                    logger.info(
                        "{} has initial quantity {} before first purchase, using createdAt {} as start date",
                        ticker,
                        initialQuantity,
                        createdDate,
                    )
                    createdDate
                } else {
                    firstPurchaseDate
                }
            }
        }
    }

    private suspend fun backfillPriceHistory(
        ticker: Ticker,
        referenceDate: LocalDate,
    ) {
        logger.info("Backfilling price history for {} from {}", ticker, referenceDate)

        fetchAndStorePrices(ticker, referenceDate, clockProvider.today())
    }

    private suspend fun updateMissingPrices(
        ticker: Ticker,
        stats: TickerPriceInitializationStatsDTO,
    ) {
        val transactionDates = getAllTransactionDates(ticker.id!!)
        val missingDates = getMissingPriceDates(ticker, transactionDates)

        if (missingDates.isNotEmpty()) {
            logger.info(
                "Fetching missing prices for {} on {} transaction dates: {}",
                ticker,
                missingDates.size,
                missingDates,
            )
            fetchPricesForDates(ticker, missingDates)
            stats.updateCount++
        } else {
            logger.debug("Price history for {} is up to date", ticker)
            stats.skipCount++
        }
    }

    private suspend fun fetchPricesForDates(
        ticker: Ticker,
        dates: List<LocalDate>,
    ) {
        if (dates.isEmpty()) return

        val datesByMonth = dates.groupBy { YearMonth.from(it) }

        coroutineScope {
            datesByMonth
                .map { (month, monthDates) ->
                    async {
                        val startDate = month.atDay(1)
                        val endDate = month.atEndOfMonth()

                        fetchAndStorePrices(ticker, startDate, endDate, monthDates.toSet())
                    }
                }.awaitAll()
        }
    }

    private suspend fun fetchAndStorePrices(
        ticker: Ticker,
        startDate: LocalDate,
        endDate: LocalDate,
        specificDates: Set<LocalDate>? = null,
    ) {
        val filterDates = specificDates ?: getTransactionDatesSet(ticker.id!!)

        logger.info(
            "Fetching prices for {} from {} to {}{}",
            ticker,
            startDate,
            endDate,
            if (specificDates != null) " (specific dates: $specificDates)" else "",
        )

        val jsonObject =
            APIUtils
                .fetchStockPriceHistory(
                    ticker.symbol,
                    startDate.toNoTimeFormat(),
                    endDate.toNoTimeFormat(),
                    filterDates,
                ).throwIfError("Fetch price history for $ticker")

        val prices = jsonObject.getJSONArray("prices")
        val currentMonth = clockProvider.currentMonth()

        val storedCount =
            prices
                .asSequence()
                .map { it as JSONObject }
                .filter { priceData ->
                    val date = priceData.getString("date").toNoTimeFormat()
                    val isMonthEnd = priceData.getBoolean("is_month_end")

                    when {
                        filterDates.contains(date) -> {
                            logger.debug("Storing {} for {}: matches filter", date, ticker)
                            true
                        }
                        isMonthEnd && YearMonth.from(date).isBefore(currentMonth) -> {
                            logger.debug("Storing {} for {}: past month-end", date, ticker)
                            true
                        }
                        else -> {
                            logger.debug("Skipping {} for {}: not relevant", date, ticker)
                            false
                        }
                    }
                }.onEach { priceData ->
                    val date = priceData.getString("date").toNoTimeFormat()
                    val price = BigDecimal(priceData.getDouble("price"))
                    val isMonthEnd = priceData.getBoolean("is_month_end")
                    storePriceHistory(ticker, date, price, isMonthEnd)
                }.count()

        logger.info("Stored {} price entries for {} (filtered from {} returned)", storedCount, ticker, prices.length())
    }

    private fun storePriceHistory(
        ticker: Ticker,
        priceDate: LocalDate,
        closingPrice: BigDecimal,
        isMonthEnd: Boolean,
    ) {
        val priceMonth = YearMonth.from(priceDate)
        val currentMonth = clockProvider.currentMonth()

        val existing = priceHistoryRepository.findByTickerIdAndDate(ticker.id!!, priceDate)

        if (existing != null) {
            existing
                .apply {
                    this.closingPrice = closingPrice
                    this.isMonthEnd = isMonthEnd
                }.let(priceHistoryRepository::save)

            logger.debug("Updated price for {} on {}", ticker, priceDate)
        } else {
            if (priceMonth == currentMonth && !isMonthEnd) {
                deleteCurrentMonthPrice(ticker.id!!, priceMonth)
            }

            TickerPriceHistory(
                ticker = ticker,
                priceDate = priceDate,
                closingPrice = closingPrice,
                isMonthEnd = isMonthEnd,
            ).let(priceHistoryRepository::save)

            logger.debug("Stored new price for {} on {}", ticker, priceDate)
        }
    }

    private fun hasCompleteHistoricalData(
        tickerId: Int,
        firstPurchaseDate: LocalDate?,
    ): Boolean {
        if (firstPurchaseDate == null) return true

        val earliestDate = getEarliestPriceDate(tickerId) ?: return false

        val firstPurchaseMonth = YearMonth.from(firstPurchaseDate)
        val earliestDataMonth = YearMonth.from(earliestDate)

        val isComplete = !earliestDataMonth.isAfter(firstPurchaseMonth)

        logger.debug(
            "Ticker {}: firstPurchaseDate={}, earliestDataDate={}, isComplete={}",
            tickerId,
            firstPurchaseDate,
            earliestDate,
            isComplete,
        )

        return isComplete
    }

    private fun calculateInitialQuantity(tickerId: Int): BigDecimal {
        val ticker = tickerRepository.findById(tickerId).orElse(null) ?: return BigDecimal.ZERO
        var initialQuantity = ticker.currentQuantity

        tickerPurchaseRepository
            .findAllByTickerId(tickerId)
            .forEach { initialQuantity -= it.quantity }

        tickerSaleRepository
            .findAllByTickerId(tickerId)
            .forEach { initialQuantity += it.quantity }

        return initialQuantity
    }

    private fun getTransactionDatesSet(tickerId: Int): Set<LocalDate> = getTransactionDates(tickerId).toSet()

    private fun getFirstPurchaseDate(tickerId: Int): LocalDate? = getTransactionDates(tickerId).minOrNull()

    private fun getPriceOnDate(
        tickerId: Int,
        date: LocalDate,
    ): BigDecimal? =
        priceHistoryRepository
            .findMostRecentPriceBeforeDate(tickerId, date)
            ?.closingPrice

    private fun getEarliestPriceDate(tickerId: Int): LocalDate? =
        priceHistoryRepository
            .findEarliestPriceByTicker(tickerId)
            ?.priceDate

    private fun getAllTransactionDates(tickerId: Int): List<LocalDate> =
        buildList {
            val ticker = tickerRepository.findById(tickerId).orElse(null) ?: return@buildList

            addAll(getTransactionDates(tickerId))

            val initialQuantity = calculateInitialQuantity(tickerId)
            if (initialQuantity > BigDecimal.ZERO) {
                val createdDate = ticker.createdAt.toLocalDate()
                add(createdDate)
                logger.debug(
                    "{} has initial quantity {}, adding createdAt {} as transaction date",
                    ticker,
                    initialQuantity,
                    createdDate,
                )
            }
        }

    private fun getTransactionDates(tickerId: Int): List<LocalDate> =
        buildList {
            addAll(
                tickerPurchaseRepository
                    .findAllByTickerId(tickerId)
                    .map { it.walletTransaction!!.date.toLocalDate() },
            )

            addAll(
                tickerSaleRepository
                    .findAllByTickerId(tickerId)
                    .map { it.walletTransaction!!.date.toLocalDate() },
            )
        }

    private fun getMissingPriceDates(
        ticker: Ticker,
        transactionDates: List<LocalDate>,
    ): List<LocalDate> =
        transactionDates.filter { date ->
            !priceHistoryRepository.existsByTickerIdAndDate(ticker.id!!, date) &&
                getClosestPriceBeforeDate(ticker, date) == null
        }

    private fun deleteCurrentMonthPrice(
        tickerId: Int,
        month: YearMonth,
    ) {
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()

        priceHistoryRepository
            .findMonthEndPricesByTickerAndDateRange(tickerId, monthStart, monthEnd)
            .filterNot { it.isMonthEnd }
            .forEach { price ->
                priceHistoryRepository.delete(price)
                logger.debug("Deleted old current month price for ticker {} on {}", tickerId, price.priceDate)
            }
    }
}
