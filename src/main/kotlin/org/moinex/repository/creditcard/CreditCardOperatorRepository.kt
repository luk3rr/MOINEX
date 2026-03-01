/*
 * Filename: CreditCardOperatorRepository.kt (original filename: CreditCardOperatorRepository.java)
 * Created on: September 17, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 28/02/2026
 */

package org.moinex.repository.creditcard

import org.moinex.model.creditcard.CreditCardOperator
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CreditCardOperatorRepository : JpaRepository<CreditCardOperator, Int> {
    /**
     * Get all credit card operators ordered by name
     * @return List of credit card operators
     */
    fun findAllByOrderByNameAsc(): List<CreditCardOperator>
}
