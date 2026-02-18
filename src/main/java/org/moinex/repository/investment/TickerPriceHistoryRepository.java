/*
 * Filename: TickerPriceHistoryRepository.java
 * Created on: February 17, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment;

import java.util.List;
import java.util.Optional;
import org.moinex.model.investment.TickerPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TickerPriceHistoryRepository extends JpaRepository<TickerPriceHistory, Integer> {

    /**
     * Find price history for a ticker on a specific date
     * @param tickerId The ticker ID
     * @param priceDate The date in string format
     * @return Optional containing the price history if found
     */
    @Query(
            "SELECT ph "
                    + "FROM TickerPriceHistory ph "
                    + "WHERE ph.ticker.id = :tickerId "
                    + "AND ph.priceDate = :priceDate")
    Optional<TickerPriceHistory> findByTickerIdAndDate(
            @Param("tickerId") Integer tickerId, @Param("priceDate") String priceDate);

    /**
     * Find the most recent price for a ticker on or before a specific date
     * @param tickerId The ticker ID
     * @param priceDate The date in string format
     * @return Optional containing the most recent price history
     */
    @Query(
            "SELECT ph "
                    + "FROM TickerPriceHistory ph "
                    + "WHERE ph.ticker.id = :tickerId "
                    + "AND ph.priceDate <= :priceDate "
                    + "ORDER BY ph.priceDate DESC LIMIT 1")
    Optional<TickerPriceHistory> findMostRecentPriceBeforeDate(
            @Param("tickerId") Integer tickerId, @Param("priceDate") String priceDate);

    /**
     * Find all month-end prices for a ticker
     * @param tickerId The ticker ID
     * @return List of month-end price histories
     */
    @Query(
            "SELECT ph "
                    + "FROM TickerPriceHistory ph "
                    + "WHERE ph.ticker.id = :tickerId "
                    + "AND ph.isMonthEnd = true "
                    + "ORDER BY ph.priceDate ASC")
    List<TickerPriceHistory> findMonthEndPricesByTicker(@Param("tickerId") Integer tickerId);

    /**
     * Find all month-end prices for a ticker within a date range
     * @param tickerId The ticker ID
     * @param startDate Start date in string format
     * @param endDate End date in string format
     * @return List of month-end price histories
     */
    @Query(
            "SELECT ph "
                    + "FROM TickerPriceHistory ph "
                    + "WHERE ph.ticker.id = :tickerId "
                    + "AND ph.isMonthEnd = true "
                    + "AND ph.priceDate >= :startDate "
                    + "AND ph.priceDate <= :endDate "
                    + "ORDER BY ph.priceDate ASC")
    List<TickerPriceHistory> findMonthEndPricesByTickerAndDateRange(
            @Param("tickerId") Integer tickerId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * Get the earliest price date for a ticker
     * @param tickerId The ticker ID
     * @return Optional containing the earliest price history
     */
    @Query(
            "SELECT ph "
                    + "FROM TickerPriceHistory ph "
                    + "WHERE ph.ticker.id = :tickerId "
                    + "ORDER BY ph.priceDate ASC LIMIT 1")
    Optional<TickerPriceHistory> findEarliestPriceByTicker(@Param("tickerId") Integer tickerId);

    /**
     * Check if price history exists for a ticker on a specific date
     * @param tickerId The ticker ID
     * @param priceDate The date in string format
     * @return True if exists, false otherwise
     */
    @Query(
            "SELECT COUNT(ph) > 0 "
                    + "FROM TickerPriceHistory ph "
                    + "WHERE ph.ticker.id = :tickerId "
                    + "AND ph.priceDate = :priceDate")
    boolean existsByTickerIdAndDate(
            @Param("tickerId") Integer tickerId, @Param("priceDate") String priceDate);
}
