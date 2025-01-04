/*
 * Filename: CreditCardDebtRepository.java
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repositories;

import java.math.BigDecimal;
import org.moinex.entities.CreditCardDebt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditCardDebtRepository extends JpaRepository<CreditCardDebt, Long> {

    /**
     * Get the total debt of a credit card
     * @param creditCardName The name of the credit card
     * @return The total debt of the credit card
     */
    @Query("SELECT COALESCE(SUM(ccd.totalAmount), 0) FROM CreditCardDebt ccd "
           + "WHERE ccd.creditCard.id = :creditCardId")
    BigDecimal
    GetTotalDebt(@Param("creditCardId") Long creditCardId);

    /**
     * Get the date of the earliest payment
     * @return The date of the earliest payment
     */
    @Query("SELECT MIN(ccp.date) FROM CreditCardPayment ccp")
    String FindEarliestPaymentDate();

    /**
     * Get the date of the latest payment
     * @return The date of the latest payment
     */
    @Query("SELECT MAX(ccp.date) FROM CreditCardPayment ccp")
    String FindLatestPaymentDate();

    /**
     * Get count of debts by credit card
     * @param creditCardId The id of the credit card
     * @return The count of debts by credit card
     */
    @Query("SELECT COUNT(ccd) FROM CreditCardDebt ccd "
           + "WHERE ccd.creditCard.id = :creditCardId")
    Long
    GetDebtCountByCreditCard(@Param("creditCardId") Long creditCardId);

    /**
     * Get the number of associated transactions for a category
     * @param categoryId Category ID
     * @return Number of transactions
     */
    @Query(
        "SELECT COUNT(ccd) FROM CreditCardDebt ccd WHERE ccd.category.id = :categoryId")
    Long
    CountTransactions(@Param("categoryId") Long categoryId);
}
