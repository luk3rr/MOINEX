/*
 * Filename: BondInterestCalculationRepository.kt (original filename: BondInterestCalculationRepository.java)
 * Created on: February 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository.investment

import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondInterestCalculation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Repository
interface BondInterestCalculationRepository : JpaRepository<BondInterestCalculation, Int> {
    @Modifying
    @Transactional
    @Query("DELETE FROM BondInterestCalculation b WHERE b.bond = :bond")
    fun deleteByBond(
        @Param("bond") bond: Bond,
    )

    @Query(
        "SELECT b " +
            "FROM BondInterestCalculation b " +
            "WHERE b.bond = :bond " +
            "AND b.referenceMonth = :referenceMonth",
    )
    fun findByBondAndReferenceMonth(
        @Param("bond") bond: Bond,
        @Param("referenceMonth") referenceMonth: String,
    ): Optional<BondInterestCalculation>

    @Query(
        "SELECT b " +
            "FROM BondInterestCalculation b " +
            "WHERE b.bond = :bond " +
            "ORDER BY b.referenceMonth ASC",
    )
    fun findByBondOrderByReferenceMonthAsc(
        @Param("bond") bond: Bond,
    ): List<BondInterestCalculation>

    @Query(
        "SELECT b " +
            "FROM BondInterestCalculation b " +
            "WHERE b.bond = :bond " +
            "ORDER BY b.referenceMonth DESC LIMIT 1",
    )
    fun findLastCalculatedMonth(
        @Param("bond") bond: Bond,
    ): Optional<BondInterestCalculation>
}
