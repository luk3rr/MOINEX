/*
 * Filename: BondOperationRepository.kt (original filename: BondOperationRepository.java)
 * Created on: January  3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository.investment

import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.OperationType
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondOperation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface BondOperationRepository : JpaRepository<BondOperation, Int> {
    @Query(
        "SELECT bo " +
            "FROM BondOperation bo " +
            "LEFT JOIN bo.walletTransaction wt " +
            "ORDER BY wt.date DESC",
    )
    fun findAllByOrderByOperationDateDesc(): List<BondOperation>

    @Query(
        "SELECT bo " +
            "FROM BondOperation bo " +
            "LEFT JOIN bo.walletTransaction wt " +
            "WHERE bo.bond = :bond " +
            "ORDER BY wt.date ASC",
    )
    fun findByBondOrderByOperationDateAsc(
        @Param("bond") bond: Bond,
    ): List<BondOperation>

    @Query(
        "SELECT bo " +
            "FROM BondOperation bo " +
            "LEFT JOIN bo.walletTransaction wt " +
            "WHERE bo.bond = :bond " +
            "AND bo.operationType = :operationType " +
            "ORDER BY wt.date ASC",
    )
    fun findByBondAndOperationTypeOrderByOperationDateAsc(
        @Param("bond") bond: Bond,
        @Param("operationType") operationType: OperationType,
    ): List<BondOperation>

    @Query(
        "SELECT bo " +
            "FROM BondOperation bo " +
            "LEFT JOIN bo.walletTransaction wt " +
            "WHERE wt.date <= :date " +
            "ORDER BY wt.date ASC",
    )
    fun findAllByDateBefore(
        @Param("date") date: LocalDateTime,
    ): List<BondOperation>

    @Query(
        "SELECT MIN(wt.date) " +
            "FROM BondOperation bo " +
            "LEFT JOIN bo.walletTransaction wt " +
            "WHERE bo.bond.interestIndex = :interestIndex " +
            "AND bo.operationType = 'BUY'",
    )
    fun findEarliestBuyDateByInterestIndex(
        @Param("interestIndex") interestIndex: InterestIndex,
    ): String?

    @Query(
        "SELECT DISTINCT bo.bond.interestIndex " +
            "FROM BondOperation bo " +
            "WHERE bo.bond.interestIndex IS NOT NULL " +
            "AND bo.operationType = 'BUY'",
    )
    fun findAllUsedInterestIndices(): List<InterestIndex>
}
