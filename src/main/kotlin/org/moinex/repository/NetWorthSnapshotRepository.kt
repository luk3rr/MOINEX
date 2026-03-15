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
import java.time.YearMonth

@Repository
interface NetWorthSnapshotRepository : JpaRepository<NetWorthSnapshot, Int> {
    fun findByReferenceMonth(referenceMonth: YearMonth): NetWorthSnapshot?

    fun existsByReferenceMonth(referenceMonth: YearMonth): Boolean

    /**
     * Get all snapshots ordered by reference month
     * @return List of snapshots
     */
    @Query(
        "SELECT nws " +
            "FROM NetWorthSnapshot nws " +
            "ORDER BY nws.referenceMonth ASC",
    )
    fun findAllOrderedByDate(): List<NetWorthSnapshot>

    @Modifying
    @Transactional
    @Query("DELETE FROM NetWorthSnapshot")
    override fun deleteAll()

    @Modifying
    @Transactional
    @Query(
        "DELETE FROM NetWorthSnapshot nws " +
            "WHERE nws.referenceMonth < :startMonth OR nws.referenceMonth > :endMonth",
    )
    fun deleteSnapshotsOutsideRange(
        startMonth: YearMonth,
        endMonth: YearMonth,
    )
}
