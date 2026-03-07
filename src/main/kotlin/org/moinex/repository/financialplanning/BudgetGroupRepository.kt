/*
 * Filename: BudgetGroupRepository.kt (original filename: BudgetGroupRepository.java)
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository.financialplanning

import org.moinex.model.financialplanning.BudgetGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BudgetGroupRepository : JpaRepository<BudgetGroup, Int>
