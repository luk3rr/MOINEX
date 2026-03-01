/*
 * Filename: CreditCardCreditRepository.kt
 * Created on: March  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.creditcard

import org.moinex.model.creditcard.CreditCardCredit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface CreditCardCreditRepository : JpaRepository<CreditCardCredit, Int> {
    /**
     * Get credits card credits by credit card ID and month
     * @param crcId The ID of the credit card
     * @param month The month
     * @param year The year
     * @return List of credit card credits
     */
    @Query(
        "SELECT ccc FROM CreditCardCredit ccc " +
            "WHERE ccc.creditCard.id = :crcId " +
            "AND strftime('%m', ccc.date) = printf('%02d', :month) " +
            "AND strftime('%Y', ccc.date) = printf('%04d', :year)",
    )
    fun findCreditCardCreditsByMonth(
        @Param("crcId") crcId: Int,
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<CreditCardCredit>

    /**
     * Get the total credits of a credit card by month
     * @param crcId The ID of the credit card
     * @param month The month
     * @param year The year
     * @return The total credits of a credit card by month
     */
    @Query(
        "SELECT coalesce(sum(ccc.amount), 0) FROM CreditCardCredit ccc " +
            "WHERE ccc.creditCard.id = :crcId " +
            "AND strftime('%m', ccc.date) = printf('%02d', :month) " +
            "AND strftime('%Y', ccc.date) = printf('%04d', :year)",
    )
    fun getTotalCreditCardCreditsByMonth(
        @Param("crcId") crcId: Int,
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): BigDecimal

    /**
     * Get suggestions. Suggestions are credits with distinct descriptions
     * and most recent date
     * @return A list with the suggestions
     */
    @Query(
        "SELECT ccc " +
            "FROM CreditCardCredit ccc " +
            "WHERE ccc.creditCard.isArchived = false AND " +
            "ccc.date = (SELECT max(ccc2.date) " +
            "             FROM CreditCardCredit ccc2 " +
            "             WHERE ccc2.creditCard.isArchived = false AND " +
            "             ccc2.description = ccc.description) " +
            "ORDER BY ccc.date DESC",
    )
    fun findSuggestions(): List<CreditCardCredit>
}
