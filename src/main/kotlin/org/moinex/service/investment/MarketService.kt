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
import org.moinex.common.extension.bacenDate
import org.moinex.common.extension.decimal
import org.moinex.common.retry.retry
import org.moinex.config.RetryConfig
import org.moinex.model.investment.BrazilianMarketIndicators
import org.moinex.model.investment.MarketQuotesAndCommodities
import org.moinex.repository.investment.BrazilianMarketIndicatorsRepository
import org.moinex.repository.investment.MarketQuotesAndCommoditiesRepository
import org.moinex.util.APIUtils
import org.moinex.util.Constants
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class MarketService(
    private val brazilianMarketIndicatorsRepository: BrazilianMarketIndicatorsRepository,
    private val marketQuotesAndCommoditiesRepository: MarketQuotesAndCommoditiesRepository,
) {
    private val logger = LoggerFactory.getLogger(MarketService::class.java)

    private val brazilianMarketIndicatorsMutex = Mutex()
    private val marketQuotesAndCommoditiesMutex = Mutex()

    companion object {
        private val MARKET_SYMBOLS =
            listOf(
                Constants.IBOVESPA_TICKER,
                Constants.DOLLAR_TICKER,
                Constants.EURO_TICKER,
                Constants.GOLD_TICKER,
                Constants.SOYBEAN_TICKER,
                Constants.COFFEE_ARABICA_TICKER,
                Constants.WHEAT_TICKER,
                Constants.OIL_BRENT_TICKER,
                Constants.BITCOIN_TICKER,
                Constants.ETHEREUM_TICKER,
            )

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
            retry(
                config = RetryConfig.MARKET_DATA,
                logger = logger,
                operationName = "Update Brazilian market indicators",
            ) {
                updateBrazilianMarketIndicators()
            }
        }

    @Transactional
    suspend fun updateMarketQuotesAndCommoditiesFromApi(): MarketQuotesAndCommodities =
        marketQuotesAndCommoditiesMutex.withLock {
            retry(
                config = RetryConfig.MARKET_DATA,
                logger = logger,
                operationName = "Update market quotes and commodities",
            ) {
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

    private fun JSONObject.parseBrazilianMarketIndicators(indicators: BrazilianMarketIndicators): BrazilianMarketIndicators =
        indicators.apply {
            selicTarget = decimal(SELIC_TARGET_FIELD, VALUE_FIELD)
            ipca12Months = decimal(IPCA_12_MONTHS_FIELD, VALUE_FIELD)
            ipcaLastMonth = decimal(IPCA_LAST_MONTH_FIELD, VALUE_FIELD)

            val localDate = bacenDate(IPCA_LAST_MONTH_FIELD, DATA_FIELD)

            ipcaLastMonthReference = YearMonth.from(localDate)
            lastUpdate = LocalDateTime.now()
        }

    private fun JSONObject.parseMarketQuotesAndCommodities(
        marketQuotesAndCommodities: MarketQuotesAndCommodities,
    ): MarketQuotesAndCommodities =
        marketQuotesAndCommodities.apply {
            dollar = decimal(Constants.DOLLAR_TICKER, PRICE_FIELD)
            euro = decimal(Constants.EURO_TICKER, PRICE_FIELD)
            ibovespa = decimal(Constants.IBOVESPA_TICKER, PRICE_FIELD)
            bitcoin = decimal(Constants.BITCOIN_TICKER, PRICE_FIELD)
            ethereum = decimal(Constants.ETHEREUM_TICKER, PRICE_FIELD)
            gold = decimal(Constants.GOLD_TICKER, PRICE_FIELD)
            soybean = decimal(Constants.SOYBEAN_TICKER, PRICE_FIELD)
            coffee = decimal(Constants.COFFEE_ARABICA_TICKER, PRICE_FIELD)
            wheat = decimal(Constants.WHEAT_TICKER, PRICE_FIELD)
            oilBrent = decimal(Constants.OIL_BRENT_TICKER, PRICE_FIELD)

            lastUpdate = LocalDateTime.now()
        }
}
