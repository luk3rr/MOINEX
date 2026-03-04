/*
 * Filename: CategoryRepository.kt (original filename: CategoryRepository.java)
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.repository

import org.moinex.model.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository : JpaRepository<Category, Int> {
    fun findAllByIsArchivedFalseOrderByNameAsc(): List<Category>

    fun existsByName(name: String): Boolean

    fun findByName(name: String): Category?
}
