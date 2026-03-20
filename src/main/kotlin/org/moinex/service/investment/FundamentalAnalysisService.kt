/*
 * Filename: FundamentalAnalysisService.kt (original filename: FundamentalAnalysisService.java)
 * Created on: January 9, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

 * Migrate to Kotlin on 09/03/2026
 */

package org.moinex.service.investment

import org.json.JSONObject
import org.moinex.common.extension.findByIdOrThrow
import org.moinex.common.extension.isCacheExpired
import org.moinex.common.extension.isValidForFundamentalAnalysis
import org.moinex.common.retry.retry
import org.moinex.common.util.APIUtils
import org.moinex.config.RetryConfig
import org.moinex.exception.MoinexException
import org.moinex.model.enums.AssetType
import org.moinex.model.enums.PeriodType
import org.moinex.model.investment.FundamentalAnalysis
import org.moinex.model.investment.Ticker
import org.moinex.repository.investment.FundamentalAnalysisRepository
import org.moinex.repository.investment.TickerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class FundamentalAnalysisService(
    private val fundamentalAnalysisRepository: FundamentalAnalysisRepository,
    private val tickerRepository: TickerRepository,
) {
    private val logger = LoggerFactory.getLogger(FundamentalAnalysisService::class.java)

    companion object {
        const val CACHE_VALIDITY_HOURS = 24 * 7
        const val RECOMMENDED_UPDATE_HOURS = 24

        val VALID_TICKER_TYPES = setOf(AssetType.STOCK, AssetType.REIT)
    }

    @Transactional
    suspend fun getAnalysis(
        tickerId: Int,
        periodType: PeriodType,
        forceRefresh: Boolean = false,
    ): FundamentalAnalysis {
        val tickerFromDatabase = tickerRepository.findByIdOrThrow(tickerId)

        check(!tickerFromDatabase.isArchived) {
            "Cannot fetch analysis for archived ticker: ${tickerFromDatabase.symbol}"
        }

        check(tickerFromDatabase.isValidForFundamentalAnalysis()) {
            "Ticker type must be STOCK or REIT"
        }

        if (!forceRefresh) {
            fundamentalAnalysisRepository
                .findByTickerAndPeriodType(tickerFromDatabase, periodType)
                .takeIf { !it.isCacheExpired() }
                ?.let { analysis ->
                    logger.info("Returning cached analysis for {} ({})", tickerFromDatabase.symbol, periodType)
                    return analysis
                }

            logger.info("Cache expired for {} ({}), fetching new data", tickerFromDatabase.symbol, periodType)
        }

        return fetchAndSaveAnalysis(tickerFromDatabase, periodType)
    }

    @Transactional
    suspend fun fetchAndSaveAnalysis(
        ticker: Ticker,
        periodType: PeriodType,
    ): FundamentalAnalysis {
        val symbol = ticker.symbol

        val tickerData =
            retry(
                config = RetryConfig.FUNDAMENTAL_ANALYSIS,
                logger = logger,
                operationName = "Fetch fundamental analysis for $symbol ($periodType)",
            ) {
                val response = APIUtils.fetchFundamentalAnalysis(symbol, periodType)

                val responseData = response.getJSONObject(symbol)

                handleApiResponse(responseData, symbol)

                responseData
            }

        val analysis =
            fundamentalAnalysisRepository
                .findByTickerAndPeriodType(ticker, periodType)
                ?: createNewAnalysis(ticker, periodType)

        analysis.apply {
            companyName = tickerData.optString("company_name", "")
            sector = tickerData.optString("sector", "")
            industry = tickerData.optString("industry", "")
            currency = tickerData.optString("currency", "BRL")
            this.periodType = periodType
            dataJson = tickerData.toString()
            lastUpdate = LocalDateTime.now()
        }

        return fundamentalAnalysisRepository.save(analysis)
    }

    private fun handleApiResponse(
        tickerData: JSONObject,
        symbol: String,
    ) {
        if (!tickerData.has("exception")) return

        val errorMsg = tickerData.getString("exception")
        logger.warn("API returned exception for {}: {}", symbol, errorMsg)

        when {
            errorMsg.contains("Financial data not available") -> {
                logger.info(
                    "Financial data not available for {}, but saving partial data (e.g., price performance)",
                    symbol,
                )
            }
            else -> {
                throw MoinexException.APIFetchException("API exception: $errorMsg")
            }
        }
    }

    fun getAllAnalysesForTicker(tickerId: Int): List<FundamentalAnalysis> =
        fundamentalAnalysisRepository.findByTickerId(tickerId)

    private fun createNewAnalysis(
        ticker: Ticker,
        periodType: PeriodType,
    ) = FundamentalAnalysis(
        id = null,
        ticker = ticker,
        companyName = null,
        sector = null,
        industry = null,
        currency = "BRL",
        periodType = periodType,
        dataJson = "{}",
        createdAt = LocalDateTime.now(),
        lastUpdate = LocalDateTime.now(),
    )
}
