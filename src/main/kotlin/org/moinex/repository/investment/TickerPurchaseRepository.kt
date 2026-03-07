/*
 * Filename: TickerPurchaseRepository.kt (original filename: TickerPurchaseRepository.java)
 * Created on: January  7, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository.investment

import org.moinex.model.investment.TickerPurchase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TickerPurchaseRepository : JpaRepository<TickerPurchase, Int> {
    @Query(
        "SELECT tp " +
            "FROM TickerPurchase tp " +
            "LEFT JOIN tp.walletTransaction wt " +
            "WHERE wt.date <= :date " +
            "ORDER BY wt.date ASC",
    )
    fun findAllByDateBefore(
        @Param("date") date: String,
    ): List<TickerPurchase>
}
