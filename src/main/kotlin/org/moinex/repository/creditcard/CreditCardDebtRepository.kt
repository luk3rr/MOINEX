/*
 * Filename: CreditCardDebtRepository.kt (original filename: CreditCardDebtRepository.java)
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 28/02/2026
 */

package org.moinex.repository.creditcard

import org.moinex.model.creditcard.CreditCardDebt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface CreditCardDebtRepository : JpaRepository<CreditCardDebt, Int> {
    /**
     * Get the total debt of a credit card
     * @param creditCardId The name of the credit card
     * @return The total debt of the credit card
     */
    @Query(
        "SELECT coalesce(sum(ccd.amount), 0) FROM CreditCardDebt ccd " +
            "WHERE ccd.creditCard.id = :creditCardId",
    )
    fun getTotalDebt(
        @Param("creditCardId") creditCardId: Int,
    ): BigDecimal

    /**
     * Get count of debts by credit card
     * @param creditCardId The id of the credit card
     * @return The count of debts by credit card
     */
    @Query("SELECT count(ccd) FROM CreditCardDebt ccd WHERE ccd.creditCard.id = :creditCardId")
    fun getDebtCountByCreditCard(
        @Param("creditCardId") creditCardId: Int,
    ): Int

    /**
     * Get the number of associated transactions for a category
     * @param categoryId Category ID
     * @return Number of transactions
     */
    @Query("SELECT count(ccd) FROM CreditCardDebt ccd WHERE ccd.category.id = :categoryId")
    fun getCountTransactions(
        @Param("categoryId") categoryId: Int,
    ): Int

    /**
     * Get suggestions. Suggestions are debts with distinct descriptions
     * and most recent date
     * @return A list with the suggestions
     */
    @Query(
        "SELECT ccd " +
            "FROM CreditCardDebt ccd " +
            "WHERE ccd.creditCard.isArchived = false AND " +
            "ccd.date = (SELECT max(ccd2.date) " +
            "             FROM CreditCardDebt ccd2 " +
            "             WHERE ccd2.creditCard.isArchived = false AND " +
            "             ccd2.description = ccd.description) " +
            "ORDER BY ccd.date DESC",
    )
    fun findSuggestions(): List<CreditCardDebt>

    /**
     * Get total amount of debts (purchases) up to a specific date
     * @param endDate The end date (inclusive)
     * @return Total amount of debts up to the date
     */
    @Query(
        "SELECT coalesce(sum(ccd.amount), 0) " +
            "FROM CreditCardDebt ccd " +
            "WHERE ccd.date <= :endDate",
    )
    fun getTotalDebtAmountUpToDate(
        @Param("endDate") endDate: LocalDateTime,
    ): BigDecimal

    /**
     * Get all debts created up to a specific date
     * @param endDate The end date (inclusive)
     * @return List of all debts created up to the date
     */
    @Query(
        "SELECT ccd " +
            "FROM CreditCardDebt ccd " +
            "WHERE ccd.date <= :endDate " +
            "ORDER BY ccd.date ASC",
    )
    fun findAllCreatedUpToDate(
        @Param("endDate") endDate: LocalDateTime,
    ): List<CreditCardDebt>
}
