/*
 * Filename: NetWorthSnapshotRepository.kt (original filename: NetWorthSnapshotRepository.java)
 * Created on: January 22, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository

import org.moinex.model.NetWorthSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Repository
interface NetWorthSnapshotRepository : JpaRepository<NetWorthSnapshot, Int> {
    /**
     * Find snapshot by month and year
     * @param month The month
     * @param year The year
     * @return Optional snapshot
     */
    fun findByMonthAndYear(
        month: Int,
        year: Int,
    ): Optional<NetWorthSnapshot>

    /**
     * Get all snapshots ordered by year and month
     * @return List of snapshots
     */
    @Query(
        "SELECT nws " +
            "FROM NetWorthSnapshot nws " +
            "ORDER BY nws.year ASC, nws.month ASC",
    )
    fun findAllOrderedByDate(): List<NetWorthSnapshot>

    @Modifying
    @Transactional
    @Query("DELETE FROM NetWorthSnapshot")
    override fun deleteAll()

    /**
     * Check if snapshot exists for month and year
     * @param month The month
     * @param year The year
     * @return True if exists
     */
    fun existsByMonthAndYear(
        month: Int,
        year: Int,
    ): Boolean
}
