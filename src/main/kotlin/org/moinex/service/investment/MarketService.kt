/*
 * Filename: MarketService.kt (original filename: MarketService.java)
 * Created on: January 17, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on March 14, 2026
 */

package org.moinex.service.investment

import jakarta.persistence.EntityNotFoundException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.moinex.common.ClockProvider
import org.moinex.common.constant.Constants
import org.moinex.common.extension.bacenDate
import org.moinex.common.extension.decimal
import org.moinex.common.extension.decimalOrNull
import org.moinex.common.util.APIUtils
import org.moinex.model.investment.BrazilianMarketIndicators
import org.moinex.model.investment.MarketQuotesAndCommodities
import org.moinex.repository.investment.BrazilianMarketIndicatorsRepository
import org.moinex.repository.investment.MarketQuotesAndCommoditiesRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class MarketService(
    private val brazilianMarketIndicatorsRepository: BrazilianMarketIndicatorsRepository,
    private val marketQuotesAndCommoditiesRepository: MarketQuotesAndCommoditiesRepository,
    private val clockProvider: ClockProvider,
) {
    private val logger = LoggerFactory.getLogger(MarketService::class.java)

    private val brazilianMarketIndicatorsMutex = Mutex()
    private val marketQuotesAndCommoditiesMutex = Mutex()

    companion object {
        /**
         * The quote currency of each market symbol, as Yahoo reports it. Passing it to the script saves one `info`
         * call per symbol on every refresh. Soybean, coffee and wheat are quoted in cents of dollar (USX).
         */
        private val MARKET_SYMBOL_CURRENCIES =
            linkedMapOf(
                Constants.IBOVESPA_TICKER to "BRL",
                Constants.DOLLAR_TICKER to "BRL",
                Constants.EURO_TICKER to "BRL",
                Constants.GOLD_TICKER to "USD",
                Constants.SOYBEAN_TICKER to "USX",
                Constants.COFFEE_ARABICA_TICKER to "USX",
                Constants.WHEAT_TICKER to "USX",
                Constants.OIL_BRENT_TICKER to "USD",
                Constants.BITCOIN_TICKER to "USD",
                Constants.ETHEREUM_TICKER to "USD",
            )

        private val MARKET_SYMBOLS = MARKET_SYMBOL_CURRENCIES.map { (symbol, currency) -> "$symbol:$currency" }

        private const val VALUE_FIELD = "valor"
        private const val PRICE_FIELD = "price"
        private const val DATA_FIELD = "data"
        private const val SELIC_TARGET_FIELD = "selic_target"
        private const val IPCA_12_MONTHS_FIELD = "ipca_12_months"
        private const val IPCA_LAST_MONTH_FIELD = "ipca_last_month"
    }

    @Transactional
    suspend fun updateBrazilianMarketIndicatorsFromApi(): BrazilianMarketIndicators =
        brazilianMarketIndicatorsMutex.withLock {
            updateBrazilianMarketIndicators()
        }

    @Transactional
    suspend fun updateMarketQuotesAndCommoditiesFromApi(): MarketQuotesAndCommodities =
        marketQuotesAndCommoditiesMutex.withLock {
            updateMarketQuotesAndCommodities()
        }

    /**
     * Like [updateBrazilianMarketIndicatorsFromApi], but leaves the stored indicators alone when they were updated
     * less than [MarketDataFreshness.TTL] ago.
     */
    @Transactional
    suspend fun updateBrazilianMarketIndicatorsIfStale(): BrazilianMarketIndicators? =
        brazilianMarketIndicatorsMutex.withLock {
            val stored = runCatching { getBrazilianMarketIndicatorsAndCleanup() }.getOrNull()

            if (stored != null && MarketDataFreshness.isFresh(stored.lastUpdate, clockProvider.now())) {
                logger.info("Brazilian market indicators are fresh, skipping the update")
                null
            } else {
                updateBrazilianMarketIndicators()
            }
        }

    /**
     * Like [updateMarketQuotesAndCommoditiesFromApi], but leaves the stored quotes alone when they were updated less
     * than [MarketDataFreshness.TTL] ago.
     */
    @Transactional
    suspend fun updateMarketQuotesAndCommoditiesIfStale(): MarketQuotesAndCommodities? =
        marketQuotesAndCommoditiesMutex.withLock {
            val stored = runCatching { getMarketQuotesAndCommoditiesAndCleanup() }.getOrNull()

            if (stored != null && MarketDataFreshness.isFresh(stored.lastUpdate, clockProvider.now())) {
                logger.info("Market quotes and commodities are fresh, skipping the update")
                null
            } else {
                updateMarketQuotesAndCommodities()
            }
        }

    @Transactional
    suspend fun getBrazilianMarketIndicatorsOrFetch(): BrazilianMarketIndicators =
        runCatching { getBrazilianMarketIndicatorsAndCleanup() }
            .getOrElse {
                logger.info("Brazilian market indicators not found in database, fetching from API")
                updateBrazilianMarketIndicatorsFromApi()
            }

    @Transactional
    suspend fun getMarketQuotesAndCommoditiesOrFetch(): MarketQuotesAndCommodities =
        runCatching { getMarketQuotesAndCommoditiesAndCleanup() }
            .getOrElse {
                logger.info("Market quotes and commodities not found in database, fetching from API")
                updateMarketQuotesAndCommoditiesFromApi()
            }

    private fun getBrazilianMarketIndicatorsAndCleanup(): BrazilianMarketIndicators =
        brazilianMarketIndicatorsRepository
            .findAll()
            .also { cleanupDuplicates(it, brazilianMarketIndicatorsRepository::delete) }
            .firstOrNull() ?: throw EntityNotFoundException("No Brazilian market indicators found")

    private fun getMarketQuotesAndCommoditiesAndCleanup(): MarketQuotesAndCommodities =
        marketQuotesAndCommoditiesRepository
            .findAll()
            .also { cleanupDuplicates(it, marketQuotesAndCommoditiesRepository::delete) }
            .firstOrNull() ?: throw EntityNotFoundException("No market quotes and commodities found")

    private suspend fun updateBrazilianMarketIndicators(): BrazilianMarketIndicators {
        val jsonObject = APIUtils.fetchBrazilianMarketIndicators()

        val indicators =
            runCatching { getBrazilianMarketIndicatorsAndCleanup() }
                .getOrElse { BrazilianMarketIndicators() }

        return jsonObject
            .parseBrazilianMarketIndicators(indicators)
            .also { brazilianMarketIndicatorsRepository.save(it) }
    }

    private suspend fun updateMarketQuotesAndCommodities(): MarketQuotesAndCommodities {
        val jsonObject = APIUtils.fetchStockPrices(MARKET_SYMBOLS)

        val quotesAndCommodities =
            runCatching { getMarketQuotesAndCommoditiesAndCleanup() }
                .getOrElse { MarketQuotesAndCommodities() }

        return jsonObject
            .parseMarketQuotesAndCommodities(quotesAndCommodities)
            .also { marketQuotesAndCommoditiesRepository.save(it) }
    }

    private fun <T> cleanupDuplicates(
        items: List<T>,
        deleteAction: (T) -> Unit,
    ) = items.drop(1).forEach(deleteAction)

    private fun JSONObject.parseBrazilianMarketIndicators(
        indicators: BrazilianMarketIndicators,
    ): BrazilianMarketIndicators =
        indicators.apply {
            selicTarget = decimal(SELIC_TARGET_FIELD, VALUE_FIELD)
            ipca12Months = decimal(IPCA_12_MONTHS_FIELD, VALUE_FIELD)
            ipcaLastMonth = decimal(IPCA_LAST_MONTH_FIELD, VALUE_FIELD)

            val localDate = bacenDate(IPCA_LAST_MONTH_FIELD, DATA_FIELD)

            ipcaLastMonthReference = YearMonth.from(localDate)
            lastUpdate = clockProvider.now()
        }

    private fun JSONObject.parseMarketQuotesAndCommodities(
        marketQuotesAndCommodities: MarketQuotesAndCommodities,
    ): MarketQuotesAndCommodities =
        marketQuotesAndCommodities.apply {
            val successfulSymbols = mutableListOf<String>()
            val failedSymbols = mutableListOf<String>()

            decimalOrNull(Constants.DOLLAR_TICKER, PRICE_FIELD)?.let {
                dollar = it
                successfulSymbols.add(Constants.DOLLAR_TICKER)
            } ?: failedSymbols.add(Constants.DOLLAR_TICKER)

            decimalOrNull(Constants.EURO_TICKER, PRICE_FIELD)?.let {
                euro = it
                successfulSymbols.add(Constants.EURO_TICKER)
            } ?: failedSymbols.add(Constants.EURO_TICKER)

            decimalOrNull(Constants.IBOVESPA_TICKER, PRICE_FIELD)?.let {
                ibovespa = it
                successfulSymbols.add(Constants.IBOVESPA_TICKER)
            } ?: failedSymbols.add(Constants.IBOVESPA_TICKER)

            decimalOrNull(Constants.BITCOIN_TICKER, PRICE_FIELD)?.let {
                bitcoin = it
                successfulSymbols.add(Constants.BITCOIN_TICKER)
            } ?: failedSymbols.add(Constants.BITCOIN_TICKER)

            decimalOrNull(Constants.ETHEREUM_TICKER, PRICE_FIELD)?.let {
                ethereum = it
                successfulSymbols.add(Constants.ETHEREUM_TICKER)
            } ?: failedSymbols.add(Constants.ETHEREUM_TICKER)

            decimalOrNull(Constants.GOLD_TICKER, PRICE_FIELD)?.let {
                gold = it
                successfulSymbols.add(Constants.GOLD_TICKER)
            } ?: failedSymbols.add(Constants.GOLD_TICKER)

            decimalOrNull(Constants.SOYBEAN_TICKER, PRICE_FIELD)?.let {
                soybean = it
                successfulSymbols.add(Constants.SOYBEAN_TICKER)
            } ?: failedSymbols.add(Constants.SOYBEAN_TICKER)

            decimalOrNull(Constants.COFFEE_ARABICA_TICKER, PRICE_FIELD)?.let {
                coffee = it
                successfulSymbols.add(Constants.COFFEE_ARABICA_TICKER)
            } ?: failedSymbols.add(Constants.COFFEE_ARABICA_TICKER)

            decimalOrNull(Constants.WHEAT_TICKER, PRICE_FIELD)?.let {
                wheat = it
                successfulSymbols.add(Constants.WHEAT_TICKER)
            } ?: failedSymbols.add(Constants.WHEAT_TICKER)

            decimalOrNull(Constants.OIL_BRENT_TICKER, PRICE_FIELD)?.let {
                oilBrent = it
                successfulSymbols.add(Constants.OIL_BRENT_TICKER)
            } ?: failedSymbols.add(Constants.OIL_BRENT_TICKER)

            logger.info("Market prices updated: ${successfulSymbols.size} successful, ${failedSymbols.size} failed")

            if (successfulSymbols.isNotEmpty()) {
                logger.debug("Successfully updated: ${successfulSymbols.joinToString(", ")}")
            }

            if (failedSymbols.isNotEmpty()) {
                logger.warn("Failed to fetch prices for: ${failedSymbols.joinToString(", ")}")
            }

            lastUpdate = clockProvider.now()
        }
}
