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
import java.time.LocalDate

@Repository
interface TickerPriceHistoryRepository : JpaRepository<TickerPriceHistory, Int> {
    @Query(
        "SELECT ph " +
            "FROM TickerPriceHistory ph " +
            "WHERE ph.ticker.id = :tickerId " +
            "AND ph.priceDate = :priceDate",
    )
    fun findByTickerIdAndDate(
        @Param("tickerId") tickerId: Int,
        @Param("priceDate") priceDate: LocalDate,
    ): TickerPriceHistory?

    @Query(
        "SELECT ph " +
            "FROM TickerPriceHistory ph " +
            "WHERE ph.ticker.id = :tickerId " +
            "AND ph.priceDate <= :priceDate " +
            "ORDER BY ph.priceDate DESC LIMIT 1",
    )
    fun findMostRecentPriceBeforeDate(
        @Param("tickerId") tickerId: Int,
        @Param("priceDate") priceDate: LocalDate,
    ): TickerPriceHistory?

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
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): List<TickerPriceHistory>

    @Query(
        "SELECT ph " +
            "FROM TickerPriceHistory ph " +
            "WHERE ph.ticker.id = :tickerId " +
            "ORDER BY ph.priceDate ASC LIMIT 1",
    )
    fun findEarliestPriceByTicker(
        @Param("tickerId") tickerId: Int,
    ): TickerPriceHistory?

    @Query(
        "SELECT COUNT(ph) > 0 " +
            "FROM TickerPriceHistory ph " +
            "WHERE ph.ticker.id = :tickerId " +
            "AND ph.priceDate = :priceDate",
    )
    fun existsByTickerIdAndDate(
        @Param("tickerId") tickerId: Int,
        @Param("priceDate") priceDate: LocalDate,
    ): Boolean
}
