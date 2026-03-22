/*
 * Filename: FinancialPlanRepository.kt (original filename: FinancialPlanRepository.java)
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository.financialplanning

import org.moinex.model.financialplanning.FinancialPlan
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface FinancialPlanRepository : JpaRepository<FinancialPlan, Int> {
    fun existsByName(name: String): Boolean

    fun findByArchivedFalse(): FinancialPlan?

    fun existsByNameAndIdNot(
        name: String,
        id: Int,
    ): Boolean

    @Query(
        "SELECT p FROM FinancialPlan p WHERE p.startDate <= :date " +
            "AND (p.endDate IS NULL OR p.endDate >= :date)",
    )
    fun findPlanForDate(
        @Param("date") date: LocalDate,
    ): FinancialPlan?
}
