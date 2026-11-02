/*
 * Filename: BenchmarkPriceRepository.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment

import org.moinex.model.investment.BenchmarkPrice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface BenchmarkPriceRepository : JpaRepository<BenchmarkPrice, Int> {
    @Query(
        "SELECT b " +
            "FROM BenchmarkPrice b " +
            "WHERE b.symbol = :symbol " +
            "AND b.priceDate >= :startDate " +
            "AND b.priceDate <= :endDate " +
            "ORDER BY b.priceDate ASC",
    )
    fun findBySymbolAndDateRange(
        @Param("symbol") symbol: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): List<BenchmarkPrice>

    @Query(
        "SELECT b " +
            "FROM BenchmarkPrice b " +
            "WHERE b.symbol = :symbol " +
            "ORDER BY b.priceDate DESC LIMIT 1",
    )
    fun findLatestBySymbol(
        @Param("symbol") symbol: String,
    ): BenchmarkPrice?
}
