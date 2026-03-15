/*
 * Filename: MarketIndicatorService.kt (original filename: MarketIndicatorService.java)
 * Created on: February 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 10/03/2026
 */

package org.moinex.service.investment

import org.json.JSONObject
import org.moinex.common.retry.RetryException
import org.moinex.common.retry.retry
import org.moinex.config.RetryConfig
import org.moinex.model.dto.SyncMarketIndicatorResultDTO
import org.moinex.model.enums.InterestIndex
import org.moinex.model.investment.MarketIndicatorHistory
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.MarketIndicatorHistoryRepository
import org.moinex.util.APIUtils
import org.moinex.util.Constants
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class MarketIndicatorService(
    private val marketIndicatorHistoryRepository: MarketIndicatorHistoryRepository,
    private val bondOperationRepository: BondOperationRepository,
) {
    private val logger = LoggerFactory.getLogger(MarketIndicatorService::class.java)

    @Transactional
    suspend fun updateAllIndicators() {
        logger.info("Starting smart market indicator synchronization")

        val usedIndicators = bondOperationRepository.findAllUsedInterestIndices()

        if (usedIndicators.isEmpty()) {
            logger.info("No bonds with interest indices found, skipping indicator synchronization")
            return
        }

        logger.info("Found {} indicators used by bonds: {}", usedIndicators.size, usedIndicators)

        val endDate = LocalDate.now()
        val results =
            usedIndicators
                .map { indicator -> processIndicatorSync(indicator, endDate) }
                .partition { it.synced }

        logger.info(
            "Market indicator synchronization completed: {} synced, {} skipped",
            results.first.size,
            results.second.size,
        )
    }

    /**
     * Get all indicator history between two dates
     *
     * @param indicatorType The type of indicator
     * @param startDate The start date
     * @param endDate The end date
     * @return List of indicator history records
     */
    @Transactional(readOnly = true)
    fun getIndicatorHistoryBetween(
        indicatorType: InterestIndex,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<MarketIndicatorHistory> =
        marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDateBetween(
            indicatorType,
            startDate,
            endDate,
        )

    /**
     * Process synchronization for a single indicator
     *
     * @param indicator The indicator type to sync
     * @param endDate The end date for synchronization
     * @return SyncResult with sync status, or null if skipped
     */
    private suspend fun processIndicatorSync(
        indicator: InterestIndex,
        endDate: LocalDate,
    ): SyncMarketIndicatorResultDTO {
        val earliestPurchaseDate = bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator)?.toLocalDate()

        if (earliestPurchaseDate == null) {
            logger.info("No purchase date found for indicator {}, skipping", indicator)
            return SyncMarketIndicatorResultDTO(indicator, synced = false)
        }

        val syncStartDate = determineSyncStartDate(indicator, earliestPurchaseDate, endDate)
        return syncIndicatorHistory(indicator, syncStartDate, endDate)
    }

    /**
     * Determine the start date for synchronization based on existing data
     *
     * @param indicator The indicator type
     * @param earliestPurchaseDate The earliest purchase date for this indicator
     * @param endDate The end date for synchronization
     * @return The start date for synchronization
     */
    private fun determineSyncStartDate(
        indicator: InterestIndex,
        earliestPurchaseDate: LocalDate,
        endDate: LocalDate,
    ): LocalDate {
        val earliestData = marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator)

        return if (earliestData != null) {
            val existingEarliestDate = earliestData.referenceDate

            if (existingEarliestDate.isAfter(earliestPurchaseDate)) {
                logger.info(
                    "Indicator {} has gap: existing data from {}, need to backfill to {}",
                    indicator,
                    existingEarliestDate,
                    earliestPurchaseDate,
                )
                earliestPurchaseDate
            } else {
                val mostRecentAvailableData =
                    marketIndicatorHistoryRepository.findLatestByIndicatorType(indicator)
                val startDate = mostRecentAvailableData?.referenceDate ?: endDate.minusDays(1)
                logger.debug(
                    "Indicator {} has complete data from {}, updating recent data",
                    indicator,
                    existingEarliestDate,
                )
                startDate
            }
        } else {
            logger.info("No historical data for indicator {}, fetching from {}", indicator, earliestPurchaseDate)
            earliestPurchaseDate
        }
    }

    /**
     * Synchronize market indicator history from BACEN API
     *
     * @param indicatorType The type of indicator (CDI, SELIC, IPCA)
     * @param startDate The start date for the historical data
     * @param endDate The end date for the historical data
     */
    private suspend fun syncIndicatorHistory(
        indicatorType: InterestIndex,
        startDate: LocalDate,
        endDate: LocalDate,
    ): SyncMarketIndicatorResultDTO =
        runCatching {
            logger.info(
                "Starting synchronization of {} from {} to {}",
                indicatorType,
                startDate,
                endDate,
            )

            val result =
                retry(
                    config = RetryConfig.BACEN_API,
                    logger = logger,
                    operationName = "Sync $indicatorType from $startDate to $endDate",
                ) {
                    APIUtils.fetchMarketIndicatorHistory(indicatorType, startDate, endDate)
                }

            val dataArray = result.getJSONArray("data")

            val savedCount =
                (0 until dataArray.length())
                    .map { dataArray.getJSONObject(it) }
                    .count { entry -> saveIndicatorIfNotExists(indicatorType, entry) }

            logger.info(
                "Successfully synchronized {} {} records from {} to {}",
                savedCount,
                indicatorType,
                startDate,
                endDate,
            )

            SyncMarketIndicatorResultDTO(indicatorType, synced = true)
        }.fold(
            onSuccess = { it },
            onFailure = { e ->
                when (e) {
                    is RetryException -> {
                        logger.error("Failed to sync {} after retries: {}", indicatorType, e.message)
                    }
                    else -> {
                        logger.error("Unexpected error synchronizing indicator {}: {}", indicatorType, e.message)
                    }
                }
                SyncMarketIndicatorResultDTO(indicatorType, synced = false)
            },
        )

    /**
     * Save indicator history if it doesn't already exist
     *
     * @param indicatorType The type of indicator
     * @param entry JSON entry from BACEN API response
     * @return true if saved, false if already exists
     */
    private fun saveIndicatorIfNotExists(
        indicatorType: InterestIndex,
        entry: JSONObject,
    ): Boolean {
        val referenceDate = LocalDate.parse(entry.getString("data"), Constants.BACEN_DATE_FORMATTER)
        val rateValue = BigDecimal(entry.getString("valor"))

        if (marketIndicatorHistoryRepository.existsByIndicatorTypeAndReferenceDate(indicatorType, referenceDate)) {
            return false
        }

        marketIndicatorHistoryRepository.save(
            MarketIndicatorHistory(
                indicatorType = indicatorType,
                referenceDate = referenceDate,
                rateValue = rateValue,
                createdAt = LocalDateTime.now(),
            ),
        )

        return true
    }
}
