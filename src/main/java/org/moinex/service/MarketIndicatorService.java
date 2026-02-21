/*
 * Filename: MarketIndicatorService.java
 * Created on: February 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.moinex.error.MoinexException;
import org.moinex.model.enums.InterestIndex;
import org.moinex.model.investment.MarketIndicatorHistory;
import org.moinex.repository.investment.BondOperationRepository;
import org.moinex.repository.investment.MarketIndicatorHistoryRepository;
import org.moinex.util.APIUtils;
import org.moinex.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MarketIndicatorService {

    private final MarketIndicatorHistoryRepository marketIndicatorHistoryRepository;
    private final BondOperationRepository bondOperationRepository;

    @Autowired
    public MarketIndicatorService(
            MarketIndicatorHistoryRepository marketIndicatorHistoryRepository,
            BondOperationRepository bondOperationRepository) {
        this.marketIndicatorHistoryRepository = marketIndicatorHistoryRepository;
        this.bondOperationRepository = bondOperationRepository;
    }

    /**
     * Synchronize market indicator history from BACEN API
     *
     * @param indicatorType The type of indicator (CDI, SELIC, IPCA)
     * @param startDate The start date for the historical data
     * @param endDate The end date for the historical data
     */
    @Transactional
    public void syncIndicatorHistory(
            InterestIndex indicatorType, LocalDate startDate, LocalDate endDate) {
        try {
            log.info(
                    "Starting synchronization of {} from {} to {}",
                    indicatorType,
                    startDate,
                    endDate);

            JSONObject result =
                    APIUtils.runPythonScript(
                            Constants.GET_MARKET_INDICATOR_HISTORY_SCRIPT,
                            new String[] {
                                indicatorType.name(),
                                convertToDateFormat(startDate),
                                convertToDateFormat(endDate)
                            });

            JSONArray dataArray = result.getJSONArray("data");

            int savedCount = 0;
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject entry = dataArray.getJSONObject(i);
                String dateStr = entry.getString("data");
                String valueStr = entry.getString("valor");

                // Parse date from BACEN format (DD/MM/YYYY) to LocalDate
                LocalDate referenceDate = LocalDate.parse(dateStr, Constants.BACEN_DATE_FORMATTER);
                BigDecimal rateValue = new BigDecimal(valueStr);

                // Check if already exists
                String refDateStr = referenceDate.format(Constants.DATE_FORMATTER_NO_TIME);
                if (!marketIndicatorHistoryRepository.existsByIndicatorTypeAndReferenceDate(
                        indicatorType, refDateStr)) {
                    MarketIndicatorHistory history =
                            MarketIndicatorHistory.builder()
                                    .indicatorType(indicatorType)
                                    .referenceDate(referenceDate)
                                    .rateValue(rateValue)
                                    .createdAt(LocalDateTime.now())
                                    .build();

                    marketIndicatorHistoryRepository.save(history);
                    savedCount++;
                }
            }

            log.info(
                    "Successfully synchronized {} {} records from {} to {}",
                    savedCount,
                    indicatorType,
                    startDate,
                    endDate);

        } catch (MoinexException e) {
            log.error("Error synchronizing indicator {}: {}", indicatorType, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error(
                    "Unexpected error synchronizing indicator {}: {}",
                    indicatorType,
                    e.getMessage());
            throw new MoinexException.APIFetchException(
                    "Error synchronizing indicator " + indicatorType + ": " + e.getMessage());
        }
    }

    /**
     * Get the indicator rate for a specific date
     *
     * @param indicatorType The type of indicator
     * @param date The date to query
     * @return The rate value for that date
     */
    @Transactional(readOnly = true)
    public BigDecimal getIndicatorRate(InterestIndex indicatorType, LocalDate date) {
        String dateStr = date.format(Constants.DB_DATE_FORMATTER);

        Optional<MarketIndicatorHistory> history =
                marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDate(
                        indicatorType, dateStr);

        if (history.isEmpty()) {
            throw new EntityNotFoundException(
                    String.format("No rate found for %s on %s", indicatorType, date));
        }

        return history.get().getRateValue();
    }

    /**
     * Get the indicator rate for a specific date, or the closest previous date if not found
     *
     * @param indicatorType The type of indicator
     * @param date The date to query
     * @return The rate value for that date or closest previous date
     */
    @Transactional(readOnly = true)
    public BigDecimal getIndicatorRateOrPrevious(InterestIndex indicatorType, LocalDate date) {
        String dateStr = date.format(Constants.DB_DATE_FORMATTER);

        Optional<MarketIndicatorHistory> history =
                marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDate(
                        indicatorType, dateStr);

        if (history.isPresent()) {
            return history.get().getRateValue();
        }

        // If not found, try to find the closest previous date
        List<MarketIndicatorHistory> previousRecords =
                marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDateBetween(
                        indicatorType, "1900-01-01", dateStr);

        if (previousRecords.isEmpty()) {
            throw new EntityNotFoundException(
                    String.format("No rate found for %s on or before %s", indicatorType, date));
        }

        // Return the last (most recent) record
        return previousRecords.getLast().getRateValue();
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
    public List<MarketIndicatorHistory> getIndicatorHistoryBetween(
            InterestIndex indicatorType, LocalDate startDate, LocalDate endDate) {
        String startDateStr = startDate.format(Constants.DB_DATE_FORMATTER);
        String endDateStr = endDate.format(Constants.DB_DATE_FORMATTER);

        return marketIndicatorHistoryRepository.findByIndicatorTypeAndReferenceDateBetween(
                indicatorType, startDateStr, endDateStr);
    }

    /**
     * Get the latest indicator rate
     *
     * @param indicatorType The type of indicator
     * @return The latest rate value
     */
    @Transactional(readOnly = true)
    public BigDecimal getLatestIndicatorRate(InterestIndex indicatorType) {
        Optional<MarketIndicatorHistory> latest =
                marketIndicatorHistoryRepository.findLatestByIndicatorType(indicatorType);

        if (latest.isEmpty()) {
            throw new EntityNotFoundException(String.format("No rate found for %s", indicatorType));
        }

        return latest.get().getRateValue();
    }

    /**
     * Get the earliest indicator date in the database
     *
     * @param indicatorType The type of indicator
     * @return The earliest date
     */
    @Transactional(readOnly = true)
    public LocalDate getEarliestIndicatorDate(InterestIndex indicatorType) {
        Optional<MarketIndicatorHistory> earliest =
                marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicatorType);

        if (earliest.isEmpty()) {
            throw new EntityNotFoundException(String.format("No data found for %s", indicatorType));
        }

        return earliest.get().getReferenceDate();
    }

    /**
     * Smart update: Synchronize only necessary market indicator data
     * - Identifies which indicators are actually used by bonds
     * - For each indicator, fetches data from earliest bond purchase date
     * - Only syncs data that is missing from the database
     * This method is designed to run on application startup
     */
    @Transactional
    public void updateAllIndicators() {
        log.info("Starting smart market indicator synchronization");

        // Get all indicators used by bonds
        List<InterestIndex> usedIndicators = bondOperationRepository.findAllUsedInterestIndices();

        if (usedIndicators.isEmpty()) {
            log.info("No bonds with interest indices found, skipping indicator synchronization");
            return;
        }

        log.info("Found {} indicators used by bonds: {}", usedIndicators.size(), usedIndicators);

        LocalDate endDate = LocalDate.now();
        int syncCount = 0;
        int skipCount = 0;

        for (InterestIndex indicator : usedIndicators) {
            try {
                // Get earliest purchase date for this indicator
                String earliestDateStr =
                        bondOperationRepository.findEarliestBuyDateByInterestIndex(indicator);

                if (earliestDateStr == null) {
                    log.info("No purchase date found for indicator {}, skipping", indicator);
                    skipCount++;
                    continue;
                }

                LocalDate earliestPurchaseDate =
                        LocalDate.parse(earliestDateStr, Constants.DB_DATE_FORMATTER);
                log.info(
                        "Indicator {} earliest purchase date: {}", indicator, earliestPurchaseDate);

                // Check if we already have data from this date
                Optional<MarketIndicatorHistory> earliestData =
                        marketIndicatorHistoryRepository.findEarliestByIndicatorType(indicator);

                LocalDate syncStartDate;
                if (earliestData.isPresent()) {
                    // We have data, check if we need to fill gaps or extend
                    LocalDate existingEarliestDate = earliestData.get().getReferenceDate();

                    if (existingEarliestDate.isAfter(earliestPurchaseDate)) {
                        // We're missing data from before the earliest existing record
                        syncStartDate = earliestPurchaseDate;
                        log.info(
                                "Indicator {} has gap: existing data from {}, need to backfill to"
                                        + " {}",
                                indicator,
                                existingEarliestDate,
                                syncStartDate);
                    } else {
                        // We have data from before or at the purchase date
                        // Just update from yesterday to today
                        syncStartDate = endDate.minusDays(1);
                        log.debug(
                                "Indicator {} has complete data from {}, updating recent data",
                                indicator,
                                existingEarliestDate);
                    }
                } else {
                    // No data at all, fetch from earliest purchase date
                    syncStartDate = earliestPurchaseDate;
                    log.info(
                            "No historical data for indicator {}, fetching from {}",
                            indicator,
                            syncStartDate);
                }

                // Sync the data
                syncIndicatorHistory(indicator, syncStartDate, endDate);
                syncCount++;

            } catch (Exception e) {
                log.warn("Failed to update indicator {}: {}", indicator, e.getMessage());
            }
        }

        log.info(
                "Market indicator synchronization completed: {} synced, {} skipped",
                syncCount,
                skipCount);
    }

    /**
     * Force update all indicators with specified date range
     * Use this for manual updates or backfilling
     *
     * @param startDate The start date for the historical data
     * @param endDate The end date for the historical data
     */
    @Transactional
    public void updateAllIndicatorsForDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Force updating all market indicators from {} to {}", startDate, endDate);

        List<InterestIndex> usedIndicators = bondOperationRepository.findAllUsedInterestIndices();

        if (usedIndicators.isEmpty()) {
            log.info("No bonds with interest indices found, skipping indicator synchronization");
            return;
        }

        for (InterestIndex indicator : usedIndicators) {
            try {
                syncIndicatorHistory(indicator, startDate, endDate);
            } catch (Exception e) {
                log.warn("Failed to update indicator {}: {}", indicator, e.getMessage());
            }
        }

        log.info("Market indicators force update completed");
    }

    /**
     * Check if indicator data exists for a specific date
     *
     * @param indicatorType The type of indicator
     * @param date The date to check
     * @return true if data exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasIndicatorData(InterestIndex indicatorType, LocalDate date) {
        String dateStr = date.format(Constants.DB_DATE_FORMATTER);
        return marketIndicatorHistoryRepository.existsByIndicatorTypeAndReferenceDate(
                indicatorType, dateStr);
    }

    /**
     * Convert LocalDate to DD/MM/YYYY format for Python script
     *
     * @param date The date to convert
     * @return Date string in DD/MM/YYYY format
     */
    private String convertToDateFormat(LocalDate date) {
        return String.format(
                "%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }
}
