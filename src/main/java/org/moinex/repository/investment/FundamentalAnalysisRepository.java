/*
 * Filename: FundamentalAnalysisRepository.java
 * Created on: January  9, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment;

import java.util.List;
import java.util.Optional;
import org.moinex.model.enums.PeriodType;
import org.moinex.model.investment.FundamentalAnalysis;
import org.moinex.model.investment.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for FundamentalAnalysis entity
 */
@Repository
public interface FundamentalAnalysisRepository extends JpaRepository<FundamentalAnalysis, Integer> {

    /**
     * Find fundamental analysis by ticker and period type
     * @param ticker The ticker entity
     * @param periodType The period type (ANNUAL, QUARTERLY, etc.)
     * @return Optional containing the analysis if found
     */
    Optional<FundamentalAnalysis> findByTickerAndPeriodType(Ticker ticker, PeriodType periodType);

    /**
     * Find fundamental analysis by ticker id and period type
     * @param tickerId The ticker id
     * @param periodType The period type (ANNUAL, QUARTERLY, etc.)
     * @return Optional containing the analysis if found
     */
    Optional<FundamentalAnalysis> findByTickerIdAndPeriodType(
            Integer tickerId, PeriodType periodType);

    /**
     * Find all fundamental analyses for a ticker (all period types)
     * @param ticker The ticker entity
     * @return List of all analyses for the ticker
     */
    List<FundamentalAnalysis> findByTicker(Ticker ticker);

    /**
     * Find all fundamental analyses for a ticker by id (all period types)
     * @param tickerId The ticker id
     * @return List of all analyses for the ticker
     */
    List<FundamentalAnalysis> findByTickerId(Integer tickerId);

    /**
     * Check if fundamental analysis exists for a ticker and period type
     * @param ticker The ticker entity
     * @param periodType The period type
     * @return true if exists, false otherwise
     */
    boolean existsByTickerAndPeriodType(Ticker ticker, PeriodType periodType);

    /**
     * Delete all fundamental analyses for a ticker
     * @param ticker The ticker entity
     */
    void deleteByTicker(Ticker ticker);

    /**
     * Delete fundamental analysis by ticker and period type
     * @param ticker The ticker entity
     * @param periodType The period type
     */
    void deleteByTickerAndPeriodType(Ticker ticker, PeriodType periodType);

    /**
     * Find all analyses for active (non-archived) tickers
     * @return List of analyses for active tickers
     */
    @Query("SELECT fa FROM FundamentalAnalysis fa WHERE fa.ticker.isArchived = false")
    List<FundamentalAnalysis> findAllForActiveTickers();
}
