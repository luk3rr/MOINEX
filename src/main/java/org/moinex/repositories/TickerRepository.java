/*
 * Filename: TickerRepository.java
 * Created on: January  6, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repositories;

import java.util.List;
import org.moinex.entities.investment.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TickerRepository extends JpaRepository<Ticker, Long> {

    /**
     * Check if a ticker with the given name exists
     * @param name The name of the ticker
     * @return True if a ticker with the given name exists, false otherwise
     */
    Boolean existsByName(String name);

    /**
     * Check if a ticker with the given symbol exists
     * @param symbol The symbol of the ticker
     * @return True if a ticker with the given symbol exists, false otherwise
     */
    Boolean existsBySymbol(String symbol);

    /**
     * Get count of purchases associated with the ticker
     * @param tickerId The id of the ticker
     * @return The count of purchases associated with the ticker
     */
    @Query("SELECT COUNT(p) "
           + "FROM TickerPurchase p "
           + "WHERE p.ticker.id = :tickerId")

    Long
    GetPurchaseCountByTicker(@Param("tickerId") Long tickerId);

    /**
     * Get count of sales associated with the ticker
     * @param tickerId The id of the ticker
     * @return The count of sales associated with the ticker
     */
    @Query("SELECT COUNT(s) "
           + "FROM TickerSale s "
           + "WHERE s.ticker.id = :tickerId")
    Long
    GetSaleCountByTicker(@Param("tickerId") Long tickerId);

    /**
     * Get count of dividends associated with the ticker
     * @param tickerId The id of the ticker
     * @return The count of dividends associated with the ticker
     */
    @Query("SELECT COUNT(d) "
           + "FROM Dividend d "
           + "WHERE d.ticker.id = :tickerId")
    Long
    GetDividendCountByTicker(@Param("tickerId") Long tickerId);

    /**
     * Findall tickers and order them by symbol in ascending order
     * @return A list of tickers ordered by symbol in ascending order
     */
    public List<Ticker> findAllByOrderBySymbolAsc();

    /**
     * Find all tickers that are not archived and order them by symbol in ascending
     * order
     * @return A list of tickers that are not archived and ordered by symbol in
     *     ascending order
     */
    public List<Ticker> findAllByArchivedFalseOrderBySymbolAsc();

    /**
     * Find all tickers that are archived and order them by symbol in ascending order
     * @return A list of tickers that are archived and ordered by symbol in ascending
     *     order
     */
    public List<Ticker> findAllByArchivedTrueOrderBySymbolAsc();
}
