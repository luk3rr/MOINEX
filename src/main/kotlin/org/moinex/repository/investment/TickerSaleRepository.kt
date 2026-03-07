/*
 * Filename: TickerSaleRepository.kt (original filename: TickerSaleRepository.java)
 * Created on: January  7, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository.investment

import org.moinex.model.investment.TickerSale
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TickerSaleRepository : JpaRepository<TickerSale, Int> {
    @Query(
        "SELECT ts " +
            "FROM TickerSale ts " +
            "LEFT JOIN ts.walletTransaction wt " +
            "WHERE wt.date <= :date " +
            "ORDER BY wt.date ASC",
    )
    fun findAllByDateBefore(
        @Param("date") date: String,
    ): List<TickerSale>

    @Query(
        "SELECT ts " +
            "FROM TickerSale ts " +
            "WHERE ts.ticker.isArchived = false",
    )
    fun findAllNonArchived(): List<TickerSale>
}
