/*
 * Filename: TickerRepository.kt (original filename: TickerRepository.java)
 * Created on: January  6, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository.investment

import org.moinex.model.enums.AssetType
import org.moinex.model.investment.Ticker
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TickerRepository : JpaRepository<Ticker, Int> {
    /**
     * Check if a ticker with the given name exists
     * @param name The name of the ticker
     * @return True if a ticker with the given name exists, false otherwise
     */
    fun existsByName(name: String): Boolean

    /**
     * Check if a ticker with the given symbol exists
     * @param symbol The symbol of the ticker
     * @return True if a ticker with the given symbol exists, false otherwise
     */
    fun existsBySymbol(symbol: String): Boolean

    /**
     * Get count of purchases associated with the ticker
     * @param tickerId The id of the ticker
     * @return The count of purchases associated with the ticker
     */
    @Query(
        "SELECT count(p) " +
            "FROM TickerPurchase p " +
            "WHERE p.ticker.id = :tickerId",
    )
    fun getPurchaseCountByTicker(
        @Param("tickerId") tickerId: Int,
    ): Int

    /**
     * Get count of sales associated with the ticker
     * @param tickerId The id of the ticker
     * @return The count of sales associated with the ticker
     */
    @Query(
        "SELECT count(s) " +
            "FROM TickerSale s " +
            "WHERE s.ticker.id = :tickerId",
    )
    fun getSaleCountByTicker(
        @Param("tickerId") tickerId: Int,
    ): Int

    /**
     * Get count of dividends associated with the ticker
     * @param tickerId The id of the ticker
     * @return The count of dividends associated with the ticker
     */
    @Query(
        "SELECT count(d) " +
            "FROM Dividend d " +
            "WHERE d.ticker.id = :tickerId",
    )
    fun getDividendCountByTicker(
        @Param("tickerId") tickerId: Int,
    ): Int

    /**
     * Get count of crypto exchanges associated with the ticker
     * @param tickerId The id of the ticker
     * @return The count of crypto exchanges associated with the ticker
     */
    @Query(
        "SELECT count(e) " +
            "FROM CryptoExchange e " +
            "WHERE e.soldCrypto.id = :tickerId OR e.receivedCrypto.id = :tickerId",
    )
    fun getCryptoExchangeCountByTicker(
        @Param("tickerId") tickerId: Int,
    ): Int

    fun findBySymbol(symbol: String): Optional<Ticker>

    /**
     * Findall tickers and order them by symbol in ascending order
     * @return A list of tickers ordered by symbol in ascending order
     */
    fun findAllByOrderBySymbolAsc(): List<Ticker>

    /**
     * Find all tickers that are not archived and order them by symbol in ascending order
     * @return A list of tickers that are not archived and ordered by symbol in ascending order
     */
    fun findAllByIsArchivedFalseOrderBySymbolAsc(): List<Ticker>

    /**
     * Find all tickers that are archived and order them by symbol in ascending order
     * @return A list of tickers that are archived and ordered by symbol in ascending order
     */
    fun findAllByIsArchivedTrueOrderBySymbolAsc(): List<Ticker>

    /**
     * Get all non-archived tickers of a specific type
     * @param type The type of the tickers
     */
    fun findAllByTypeAndIsArchivedFalseOrderBySymbolAsc(type: AssetType): List<Ticker>
}
