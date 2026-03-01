/*
 * Filename: CreditCardRepository.kt (original filename: CreditCardRepository.java)
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 28/02/2026
 */

package org.moinex.repository.creditcard

import org.moinex.model.creditcard.CreditCard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CreditCardRepository : JpaRepository<CreditCard, Int> {
    /**
     * Check if a credit card with the given name exists
     * @param name The name of the credit card
     * @return True if a credit card with the given name exists, false otherwise
     */
    fun existsByName(name: String): Boolean

    /**
     * Check if a credit card with the given name exists, excluding a specific ID
     * @param name The name of the credit card
     * @param id The ID to exclude from the search
     * @return True if a credit card with the given name exists (excluding the given ID), false otherwise
     */
    fun existsByNameAndIdNot(
        name: String,
        id: Int,
    ): Boolean

    /**
     * Get all credit cards that are archived
     * @return A list with all credit cards that are archived
     */
    fun findAllByIsArchivedTrue(): List<CreditCard>

    /**
     * Get all credit cards that are not archived
     * @return A list with all credit cards that are not archived
     */
    fun findAllByIsArchivedFalse(): List<CreditCard>

    /**
     * Get all credit cards ordered by name
     * @return A list with all credit cards ordered by name
     */
    fun findAllByOrderByNameAsc(): List<CreditCard>

    /**
     * Get all credit cards are not archived ordered by name
     * @return A list with all credit cards that are not archived ordered by name
     */
    fun findAllByIsArchivedFalseOrderByNameAsc(): List<CreditCard>
}
