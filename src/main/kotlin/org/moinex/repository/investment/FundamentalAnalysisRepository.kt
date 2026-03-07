/*
 * Filename: FundamentalAnalysisRepository.kt (original filename: FundamentalAnalysisRepository.java)
 * Created on: January  9, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository.investment

import org.moinex.model.enums.PeriodType
import org.moinex.model.investment.FundamentalAnalysis
import org.moinex.model.investment.Ticker
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface FundamentalAnalysisRepository : JpaRepository<FundamentalAnalysis, Int> {
    /**
     * Find fundamental analysis by ticker and period type
     * @param ticker The ticker entity
     * @param periodType The period type (ANNUAL, QUARTERLY, etc.)
     * @return Optional containing the analysis if found
     */
    fun findByTickerAndPeriodType(
        ticker: Ticker,
        periodType: PeriodType,
    ): Optional<FundamentalAnalysis>

    /**
     * Find fundamental analysis by ticker id and period type
     * @param tickerId The ticker id
     * @param periodType The period type (ANNUAL, QUARTERLY, etc.)
     * @return Optional containing the analysis if found
     */
    fun findByTickerIdAndPeriodType(
        tickerId: Int,
        periodType: PeriodType,
    ): Optional<FundamentalAnalysis>

    /**
     * Find all fundamental analyses for a ticker (all period types)
     * @param ticker The ticker entity
     * @return List of all analyses for the ticker
     */
    fun findByTicker(ticker: Ticker): List<FundamentalAnalysis>

    /**
     * Find all fundamental analyses for a ticker by id (all period types)
     * @param tickerId The ticker id
     * @return List of all analyses for the ticker
     */
    fun findByTickerId(tickerId: Int): List<FundamentalAnalysis>

    /**
     * Check if fundamental analysis exists for a ticker and period type
     * @param ticker The ticker entity
     * @param periodType The period type
     * @return true if exists, false otherwise
     */
    fun existsByTickerAndPeriodType(
        ticker: Ticker,
        periodType: PeriodType,
    ): Boolean

    /**
     * Delete all fundamental analyses for a ticker
     * @param ticker The ticker entity
     */
    fun deleteByTicker(ticker: Ticker)

    /**
     * Delete fundamental analysis by ticker and period type
     * @param ticker The ticker entity
     * @param periodType The period type
     */
    fun deleteByTickerAndPeriodType(
        ticker: Ticker,
        periodType: PeriodType,
    )

    /**
     * Find all analyses for active (non-archived) tickers
     * @return List of analyses for active tickers
     */
    @Query(
        "SELECT fa " +
            "FROM FundamentalAnalysis fa " +
            "WHERE fa.ticker.isArchived = false",
    )
    fun findAllForActiveTickers(): List<FundamentalAnalysis>
}
