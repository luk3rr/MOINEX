/*
 * Filename: FundamentalAnalysisService.java
 * Created on: January  9, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
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
    private static final Logger logger =
            LoggerFactory.getLogger(FundamentalAnalysisService.class);

    // Cache validity: 24 hours
    private static final Duration CACHE_VALIDITY = Duration.ofHours(24);

    private FundamentalAnalysisRepository fundamentalAnalysisRepository;
    private TickerRepository tickerRepository;
    private FundamentalAnalysisService self;

    @Autowired
    public FundamentalAnalysisService(
            FundamentalAnalysisRepository fundamentalAnalysisRepository,
            TickerRepository tickerRepository,
            FundamentalAnalysisService self) {
        this.fundamentalAnalysisRepository = fundamentalAnalysisRepository;
        this.tickerRepository = tickerRepository;
        this.self = self;
    }

    /**
     * Get fundamental analysis for a ticker
     *
     * @param tickerId Ticker ID
     * @param periodType Period type ("annual" or "quarterly")
     * @param forceRefresh If true, ignores cache and fetches new data
     * @return cached data if available and not expired, otherwise fetches new data
     * @throws MoinexException if data cannot be fetched or ticker is archived
     */
    @Transactional
    public FundamentalAnalysis getAnalysis(
            Integer tickerId, PeriodType periodType, boolean forceRefresh) throws MoinexException {
        // Get ticker and validate
        Ticker ticker = tickerRepository
                .findById(tickerId)
                .orElseThrow(() -> new EntityNotFoundException("Ticker not found: " + tickerId));

        if (ticker.isArchived()) {
            throw new MoinexException(
                    "Cannot fetch analysis for archived ticker: " + ticker.getSymbol());
        }

        // Check cache first
        if (!forceRefresh) {
            Optional<FundamentalAnalysis> cached =
                    fundamentalAnalysisRepository.findByTicker(ticker);

            if (cached.isPresent()) {
                FundamentalAnalysis analysis = cached.get();
                LocalDateTime lastUpdate = analysis.getLastUpdate();
                Duration age = Duration.between(lastUpdate, LocalDateTime.now());

                // Return cached data if still valid and same period type
                if (age.compareTo(CACHE_VALIDITY) < 0 && analysis.getPeriodType().equals(periodType)) {
                    logger.info("Returning cached analysis for {}", ticker.getSymbol());
                    return analysis;
                }

                logger.info("Cache expired for {}, fetching new data", ticker.getSymbol());
            }
        }

        return self.fetchAndSaveAnalysis(ticker, periodType);
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
        Ticker ticker = tickerRepository
                .findBySymbol(symbol)
                .orElseThrow(() -> new EntityNotFoundException("Ticker not found: " + symbol));

        return self.getAnalysis(ticker.getId(), periodType, forceRefresh);
    }

    /**
     * Fetch fundamental analysis from Python script and save to database
     *
     * @param ticker Ticker entity
     * @param periodType Period type ("annual" or "quarterly")
     * @return FundamentalAnalysis entity
     * @throws MoinexException if script execution fails
     */
    @Transactional
    public FundamentalAnalysis fetchAndSaveAnalysis(Ticker ticker, PeriodType periodType)
            throws MoinexException {
        try {
            String symbol = ticker.getSymbol();
            logger.info("Fetching fundamental analysis for {} ({})", symbol, periodType);

            // Use APIUtils to execute Python script asynchronously
            JSONObject jsonData =
                    APIUtils.fetchFundamentalAnalysisAsync(symbol, periodType).get();

            JSONObject tickerData = jsonData.getJSONObject(symbol);

            // Create or update entity
            FundamentalAnalysis analysis =
                    fundamentalAnalysisRepository
                            .findByTicker(ticker)
                            .orElse(FundamentalAnalysis.builder()
                                    .ticker(ticker)
                                    .createdAt(LocalDateTime.now().format(Constants.DB_DATE_FORMATTER))
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

            logger.info("Successfully saved analysis for {}", symbol);
            return analysis;

        } catch (Exception e) {
            logger.error("Error fetching analysis for {}: {}", ticker.getSymbol(), e.getMessage());
            throw new MoinexException("Failed to fetch fundamental analysis: " + e.getMessage());
        }
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
        Ticker ticker = tickerRepository
                .findById(tickerId)
                .orElseThrow(() -> new EntityNotFoundException("Ticker not found: " + tickerId));

        fundamentalAnalysisRepository.deleteByTicker(ticker);
        logger.info("Deleted analysis for {}", ticker.getSymbol());
    }

    /**
     * Check if analysis exists for ticker
     *
     * @param tickerId Ticker ID
     * @return true if exists
     */
    public boolean hasAnalysis(Integer tickerId) {
        return tickerRepository
                .findById(tickerId)
                .map(fundamentalAnalysisRepository::existsByTicker)
                .orElse(false);
    }

    /**
     * Get analysis by ticker ID (from cache only)
     *
     * @param tickerId Ticker ID
     * @return FundamentalAnalysis entity
     * @throws EntityNotFoundException if not found
     */
    public FundamentalAnalysis getAnalysisFromCache(Integer tickerId) {
        return fundamentalAnalysisRepository
                .findByTickerId(tickerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No cached analysis found for ticker ID: " + tickerId));
    }

    /**
     * Check if cached analysis is expired
     *
     * @param tickerId Ticker ID
     * @return true if expired or not found
     */
    public boolean isCacheExpired(Integer tickerId) {
        Optional<FundamentalAnalysis> cached =
                fundamentalAnalysisRepository.findByTickerId(tickerId);

        if (cached.isEmpty()) {
            return true;
        }

        LocalDateTime lastUpdate = cached.get().getLastUpdate();
        Duration age = Duration.between(lastUpdate, LocalDateTime.now());

        return age.compareTo(CACHE_VALIDITY) >= 0;
    }
}
