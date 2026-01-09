/*
 * Filename: FundamentalAnalysisRepository.java
 * Created on: January  9, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment;

import java.util.List;
import java.util.Optional;
import org.moinex.model.investment.FundamentalAnalysis;
import org.moinex.model.investment.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for FundamentalAnalysis entity
 */
@Repository
public interface FundamentalAnalysisRepository
        extends JpaRepository<FundamentalAnalysis, Long> {

    /**
     * Find fundamental analysis by ticker
     * @param ticker The ticker entity
     * @return Optional containing the analysis if found
     */
    Optional<FundamentalAnalysis> findByTicker(Ticker ticker);

    /**
     * Find fundamental analysis by ticker id
     * @param tickerId The ticker id
     * @return Optional containing the analysis if found
     */
    Optional<FundamentalAnalysis> findByTickerId(Integer tickerId);

    /**
     * Check if fundamental analysis exists for a ticker
     * @param ticker The ticker entity
     * @return true if exists, false otherwise
     */
    boolean existsByTicker(Ticker ticker);

    /**
     * Delete fundamental analysis by ticker
     * @param ticker The ticker entity
     */
    void deleteByTicker(Ticker ticker);

    /**
     * Find all analyses for active (non-archived) tickers
     * @return List of analyses for active tickers
     */
    @Query("SELECT fa FROM FundamentalAnalysis fa WHERE fa.ticker.isArchived = false")
    List<FundamentalAnalysis> findAllForActiveTickers();
}
