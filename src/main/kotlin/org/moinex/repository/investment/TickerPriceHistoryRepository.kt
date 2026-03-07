/*
 * Filename: TickerPriceHistoryRepository.kt (original filename: TickerPriceHistoryRepository.java)
 * Created on: February 17, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository.investment

import org.moinex.model.investment.TickerPriceHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TickerPriceHistoryRepository : JpaRepository<TickerPriceHistory, Int> {
    /**
     * Find price history for a ticker on a specific date
     * @param tickerId The ticker ID
     * @param priceDate The date in string format
     * @return Optional containing the price history if found
     */
    @Query(
        "SELECT ph " +
            "FROM TickerPriceHistory ph " +
            "WHERE ph.ticker.id = :tickerId " +
            "AND ph.priceDate = :priceDate",
    )
    fun findByTickerIdAndDate(
        @Param("tickerId") tickerId: Int,
        @Param("priceDate") priceDate: String,
    ): Optional<TickerPriceHistory>

    /**
     * Find the most recent price for a ticker on or before a specific date
     * @param tickerId The ticker ID
     * @param priceDate The date in string format
     * @return Optional containing the most recent price history
     */
    @Query(
        "SELECT ph " +
            "FROM TickerPriceHistory ph " +
            "WHERE ph.ticker.id = :tickerId " +
            "AND ph.priceDate <= :priceDate " +
            "ORDER BY ph.priceDate DESC LIMIT 1",
    )
    fun findMostRecentPriceBeforeDate(
        @Param("tickerId") tickerId: Int,
        @Param("priceDate") priceDate: String,
    ): Optional<TickerPriceHistory>

    /**
     * Find all month-end prices for a ticker within a date range
     * @param tickerId The ticker ID
     * @param startDate Start date in string format
     * @param endDate End date in string format
     * @return List of month-end price histories
     */
    @Query(
        "SELECT ph " +
            "FROM TickerPriceHistory ph " +
            "WHERE ph.ticker.id = :tickerId " +
            "AND ph.isMonthEnd = true " +
            "AND ph.priceDate >= :startDate " +
            "AND ph.priceDate <= :endDate " +
            "ORDER BY ph.priceDate ASC",
    )
    fun findMonthEndPricesByTickerAndDateRange(
        @Param("tickerId") tickerId: Int,
        @Param("startDate") startDate: String,
        @Param("endDate") endDate: String,
    ): List<TickerPriceHistory>

    /**
     * Get the earliest price date for a ticker
     * @param tickerId The ticker ID
     * @return Optional containing the earliest price history
     */
    @Query(
        "SELECT ph " +
            "FROM TickerPriceHistory ph " +
            "WHERE ph.ticker.id = :tickerId " +
            "ORDER BY ph.priceDate ASC LIMIT 1",
    )
    fun findEarliestPriceByTicker(
        @Param("tickerId") tickerId: Int,
    ): Optional<TickerPriceHistory>

    /**
     * Check if price history exists for a ticker on a specific date
     * @param tickerId The ticker ID
     * @param priceDate The date in string format
     * @return True if exists, false otherwise
     */
    @Query(
        "SELECT COUNT(ph) > 0 " +
            "FROM TickerPriceHistory ph " +
            "WHERE ph.ticker.id = :tickerId " +
            "AND ph.priceDate = :priceDate",
    )
    fun existsByTickerIdAndDate(
        @Param("tickerId") tickerId: Int,
        @Param("priceDate") priceDate: String,
    ): Boolean
}
