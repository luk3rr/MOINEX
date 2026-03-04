/*
 * Filename: GoalRepository.kt (original filename: GoalRepository.java)
 * Created on: December 6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.repository.goal

import org.moinex.model.goal.Goal
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GoalRepository : JpaRepository<Goal, Int> {
    fun existsByName(name: String): Boolean
}
