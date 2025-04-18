/*
 * Filename: CreditCardDebtRepository.java
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.creditcard;

import java.math.BigDecimal;
import java.util.List;
import org.moinex.model.creditcard.CreditCardDebt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditCardDebtRepository extends JpaRepository<CreditCardDebt, Long> {

    /**
     * Get the total debt of a credit card
     * @param creditCardId The name of the credit card
     * @return The total debt of the credit card
     */
    @Query("SELECT coalesce(sum(ccd.amount), 0) FROM CreditCardDebt ccd "
           + "WHERE ccd.creditCard.id = :creditCardId")
    BigDecimal
    getTotalDebt(@Param("creditCardId") Long creditCardId);

    /**
     * Get the date of the earliest payment
     * @return The date of the earliest payment
     */
    @Query("SELECT min(ccp.date) FROM CreditCardPayment ccp")
    String findEarliestPaymentDate();

    /**
     * Get the date of the latest payment
     * @return The date of the latest payment
     */
    @Query("SELECT max(ccp.date) FROM CreditCardPayment ccp")
    String findLatestPaymentDate();

    /**
     * Get count of debts by credit card
     * @param creditCardId The id of the credit card
     * @return The count of debts by credit card
     */
    @Query("SELECT count(ccd) FROM CreditCardDebt ccd "
           + "WHERE ccd.creditCard.id = :creditCardId")
    Long
    getDebtCountByCreditCard(@Param("creditCardId") Long creditCardId);

    /**
     * Get the number of associated transactions for a category
     * @param categoryId Category ID
     * @return Number of transactions
     */
    @Query(
        "SELECT count(ccd) FROM CreditCardDebt ccd WHERE ccd.category.id = :categoryId")
    Long
    getCountTransactions(@Param("categoryId") Long categoryId);

    /**
     * Get suggestions. Suggestions are debts with distinct descriptions
     * and most recent date
     * @return A list with the suggestions
     */
    @Query("SELECT ccd "
           + "FROM CreditCardDebt ccd "
           + "WHERE ccd.creditCard.isArchived = false AND "
           + "ccd.date = (SELECT max(ccd2.date) "
           + "                 FROM CreditCardDebt ccd2 "
           + "                 WHERE ccd2.creditCard.isArchived = false AND "
           + "                 ccd2.description = ccd.description) "
           + "ORDER BY ccd.date DESC")
    List<CreditCardDebt>
    findSuggestions();
}
