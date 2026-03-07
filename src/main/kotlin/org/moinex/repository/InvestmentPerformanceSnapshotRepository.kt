/*
 * Filename: InvestmentPerformanceSnapshotRepository.kt (original filename: InvestmentPerformanceSnapshotRepository.java)
 * Created on: February 17, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository

import org.moinex.model.InvestmentPerformanceSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Repository
interface InvestmentPerformanceSnapshotRepository : JpaRepository<InvestmentPerformanceSnapshot, Int> {
    fun findByMonthAndYear(
        month: Int,
        year: Int,
    ): Optional<InvestmentPerformanceSnapshot>

    @Query(
        "SELECT ips " +
            "FROM InvestmentPerformanceSnapshot ips " +
            "ORDER BY ips.year ASC, ips.month ASC",
    )
    fun findAllOrderedByDate(): List<InvestmentPerformanceSnapshot>

    @Modifying
    @Transactional
    @Query("DELETE FROM InvestmentPerformanceSnapshot")
    override fun deleteAll()

    fun existsByMonthAndYear(
        month: Int,
        year: Int,
    ): Boolean
}
