/*
 * Filename: FundamentalAnalysisService.java
 * Created on: January  9, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.NoArgsConstructor;
import org.json.JSONObject;
import org.moinex.error.MoinexException;
import org.moinex.model.enums.PeriodType;
import org.moinex.model.investment.FundamentalAnalysis;
import org.moinex.model.investment.Ticker;
import org.moinex.repository.investment.FundamentalAnalysisRepository;
import org.moinex.repository.investment.TickerRepository;
import org.moinex.util.APIUtils;
import org.moinex.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing fundamental analysis data
 */
@Service
@NoArgsConstructor
public class FundamentalAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(FundamentalAnalysisService.class);

    public static final Integer CACHE_VALIDITY_HOURS = 24;
    public static final Integer MAX_RETRIES = 5;
    public static final Integer RETRY_DELAY_MS = 2000; // Start with 2 seconds
    public static final Double RETRY_MULTIPLIER = 1.5;

    private FundamentalAnalysisRepository fundamentalAnalysisRepository;
    private TickerRepository tickerRepository;

    @Autowired
    public FundamentalAnalysisService(
            FundamentalAnalysisRepository fundamentalAnalysisRepository,
            TickerRepository tickerRepository) {
        this.fundamentalAnalysisRepository = fundamentalAnalysisRepository;
        this.tickerRepository = tickerRepository;
    }

    /**
     * Get fundamental analysis for a ticker and period type
     * Checks cache first, fetches new data if cache is invalid or forceRefresh is true
     * Only works for non-archived tickers
     *
     * @param tickerId Ticker ID
     * @param periodType Period type (ANNUAL, QUARTERLY, etc.)
     * @param forceRefresh If true, ignores cache and fetches new data
     * @return cached data if available and not expired, otherwise fetches new data
     * @throws MoinexException if data cannot be fetched or ticker is archived
     */
    @Transactional
    public FundamentalAnalysis getAnalysis(
            Integer tickerId, PeriodType periodType, boolean forceRefresh) throws MoinexException {
        // Get ticker and validate
        Ticker ticker =
                tickerRepository
                        .findById(tickerId)
                        .orElseThrow(
                                () -> new EntityNotFoundException("Ticker not found: " + tickerId));

        if (ticker.isArchived()) {
            throw new MoinexException(
                    "Cannot fetch analysis for archived ticker: " + ticker.getSymbol());
        }

        // Check cache first for this specific period type
        if (!forceRefresh) {
            Optional<FundamentalAnalysis> cached =
                    fundamentalAnalysisRepository.findByTickerAndPeriodType(ticker, periodType);

            if (cached.isPresent()) {
                FundamentalAnalysis analysis = cached.get();

                // Check if cache is still valid (24 hours)
                if (!isCacheExpired(analysis)) {
                    logger.info(
                            "Returning cached analysis for {} ({})",
                            ticker.getSymbol(),
                            periodType);
                    return analysis;
                }

                logger.info(
                        "Cache expired for {} ({}), fetching new data",
                        ticker.getSymbol(),
                        periodType);
            }
        }

        return fetchAndSaveAnalysis(ticker, periodType);
    }

    /**
     * Get fundamental analysis by ticker symbol (convenience method)
     *
     * @param symbol Ticker symbol (e.g., "PETR4.SA")
     * @param periodType Period type ("annual" or "quarterly")
     * @param forceRefresh If true, ignores cache and fetches new data
     * @return FundamentalAnalysis entity
     * @throws MoinexException if data cannot be fetched
     */
    @Transactional
    public FundamentalAnalysis getAnalysisBySymbol(
            String symbol, PeriodType periodType, boolean forceRefresh) throws MoinexException {
        Ticker ticker =
                tickerRepository
                        .findBySymbol(symbol)
                        .orElseThrow(
                                () -> new EntityNotFoundException("Ticker not found: " + symbol));

        return getAnalysis(ticker.getId(), periodType, forceRefresh);
    }

    /**
     * Fetch fundamental analysis from Python script and save to database
     * Implements retry mechanism with exponential backoff
     *
     * @param ticker Ticker entity
     * @param periodType Period type ("annual" or "quarterly")
     * @return FundamentalAnalysis entity
     * @throws MoinexException if script execution fails after all retries
     */
    @Transactional
    public FundamentalAnalysis fetchAndSaveAnalysis(Ticker ticker, PeriodType periodType)
            throws MoinexException {
        String symbol = ticker.getSymbol();
        int retryDelayMs = RETRY_DELAY_MS;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.info(
                        "Fetching fundamental analysis for {} ({}) - Attempt {}/{}",
                        symbol,
                        periodType,
                        attempt,
                        MAX_RETRIES);

                JSONObject jsonData =
                        APIUtils.fetchFundamentalAnalysisAsync(symbol, periodType).get();

                JSONObject tickerData = jsonData.getJSONObject(symbol);

                // Check if response contains error
                if (tickerData.has("error")) {
                    String errorMsg = tickerData.getString("error");
                    logger.warn("API returned error for {}: {}", symbol, errorMsg);

                    // If this is not the last attempt, retry
                    if (attempt < MAX_RETRIES) {
                        logger.info("Retrying in {} ms...", retryDelayMs);
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= RETRY_MULTIPLIER; // Exponential backoff
                        continue;
                    } else {
                        throw new MoinexException(
                                "API error after " + MAX_RETRIES + " attempts: " + errorMsg);
                    }
                }

                // Create or update entity for this specific period type
                FundamentalAnalysis analysis =
                        fundamentalAnalysisRepository
                                .findByTickerAndPeriodType(ticker, periodType)
                                .orElse(
                                        FundamentalAnalysis.builder()
                                                .ticker(ticker)
                                                .createdAt(
                                                        LocalDateTime.now()
                                                                .format(
                                                                        Constants
                                                                                .DB_DATE_FORMATTER))
                                                .build());

                // Update fields
                analysis.setCompanyName(tickerData.optString("company_name", ""));
                analysis.setSector(tickerData.optString("sector", ""));
                analysis.setIndustry(tickerData.optString("industry", ""));
                analysis.setCurrency(tickerData.optString("currency", "BRL"));
                analysis.setPeriodType(periodType);
                analysis.setDataJson(tickerData.toString()); // Store full JSON
                analysis.setLastUpdate(LocalDateTime.now());

                analysis = fundamentalAnalysisRepository.save(analysis);

                logger.info("Successfully saved analysis for {} on attempt {}", symbol, attempt);
                return analysis;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Retry interrupted for {}", symbol);
                throw new MoinexException("Fetch interrupted: " + e.getMessage());
            } catch (MoinexException e) {
                throw e;
            } catch (Exception e) {
                logger.error(
                        "Error fetching analysis for {} on attempt {}: {}",
                        symbol,
                        attempt,
                        e.getMessage());

                // If this is not the last attempt, retry
                if (attempt < MAX_RETRIES) {
                    try {
                        logger.info("Retrying in {} ms...", retryDelayMs);
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2; // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new MoinexException("Retry interrupted: " + ie.getMessage());
                    }
                } else {
                    throw new MoinexException(
                            "Failed to fetch fundamental analysis after "
                                    + MAX_RETRIES
                                    + " attempts: "
                                    + e.getMessage());
                }
            }
        }

        // Should never reach here
        throw new MoinexException("Failed to fetch fundamental analysis for " + symbol);
    }

    /**
     * Get all cached analyses (all tickers, including archived)
     *
     * @return List of all fundamental analyses
     */
    public List<FundamentalAnalysis> getAllAnalyses() {
        return fundamentalAnalysisRepository.findAll();
    }

    /**
     * Get analyses only for active (non-archived) tickers
     *
     * @return List of analyses for active tickers
     */
    public List<FundamentalAnalysis> getAnalysesForActiveTickers() {
        return fundamentalAnalysisRepository.findAllForActiveTickers();
    }

    /**
     * Delete analysis by ticker ID
     *
     * @param tickerId Ticker ID
     */
    @Transactional
    public void deleteAnalysis(Integer tickerId) {
        Ticker ticker =
                tickerRepository
                        .findById(tickerId)
                        .orElseThrow(
                                () -> new EntityNotFoundException("Ticker not found: " + tickerId));

        fundamentalAnalysisRepository.deleteByTicker(ticker);
        logger.info("Deleted analysis for {}", ticker.getSymbol());
    }

    /**
     * Check if cache is expired (older than 24 hours)
     *
     * @param analysis FundamentalAnalysis entity
     * @return true if cache is expired
     */
    public boolean isCacheExpired(FundamentalAnalysis analysis) {
        if (analysis == null) {
            return true;
        }

        LocalDateTime lastUpdate = analysis.getLastUpdate();
        LocalDateTime now = LocalDateTime.now();

        return lastUpdate.plusHours(CACHE_VALIDITY_HOURS).isBefore(now);
    }

    /**
     * Check if cache is expired for a specific ticker and period type
     *
     * @param tickerId Ticker ID
     * @param periodType Period type
     * @return true if cache is expired or doesn't exist
     */
    public boolean isCacheExpired(Integer tickerId, PeriodType periodType) {
        Optional<FundamentalAnalysis> analysis =
                fundamentalAnalysisRepository.findByTickerIdAndPeriodType(tickerId, periodType);

        return analysis.map(this::isCacheExpired).orElse(true);
    }

    /**
     * Get all cached analyses for a ticker (all period types)
     *
     * @param tickerId Ticker ID
     * @return List of all analyses for the ticker
     */
    public List<FundamentalAnalysis> getAllAnalysesForTicker(Integer tickerId) {
        return fundamentalAnalysisRepository.findByTickerId(tickerId);
    }
}
