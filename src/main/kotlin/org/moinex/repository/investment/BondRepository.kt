/*
 * Filename: BondRepository.kt (original filename: BondRepository.java)
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository.investment

import org.moinex.model.investment.Bond
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BondRepository : JpaRepository<Bond, Int> {
    fun findByArchivedFalseOrderByNameAsc(): List<Bond>

    fun findByArchivedTrueOrderByNameAsc(): List<Bond>

    fun existsBySymbol(symbol: String): Boolean

    fun existsBySymbolAndIdNot(
        symbol: String,
        int: Int,
    ): Boolean
}
