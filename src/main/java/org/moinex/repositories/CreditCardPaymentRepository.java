/*
 * Filename: CreditCardPaymentRepository.java
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repositories;

import java.math.BigDecimal;
import java.util.List;
import org.moinex.entities.CreditCardPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditCardPaymentRepository
    extends JpaRepository<CreditCardPayment, Long> {

    /**
     * Get all credit card payments in a month and year
     * @param month The month
     * @param year The year
     * @return A list with all credit card payments in a month and year
     */
    @Query("SELECT ccp "
           + "FROM CreditCardPayment ccp "
           + "WHERE strftime('%m', ccp.date) = printf('%02d', :month) "
           + "AND strftime('%Y', ccp.date) = printf('%04d', :year)"
           + "AND ccp.wallet IS NOT NULL")
    List<CreditCardPayment>
    GetAllPaidPaymentsByMonth(@Param("month") Integer month,
                              @Param("year") Integer  year);

    /**
     * Get credit card payments in a month and year
     * @param month The month
     * @param year The year
     * @return A list with all credit card payments in a month and year
     */
    @Query("SELECT ccp "
           + "FROM CreditCardPayment ccp "
           + "WHERE strftime('%m', ccp.date) = printf('%02d', :month) "
           + "AND strftime('%Y', ccp.date) = printf('%04d', :year)")
    List<CreditCardPayment>
    GetCreditCardPayments(@Param("month") Integer month, @Param("year") Integer year);

    /**
     * Get credit card payments in a month and year by credit card
     * @param crcId The credit card id
     * @param month The month
     * @param year The year
     * @return A list with all credit card payments in a month and year by credit card
     */
    @Query("SELECT ccp "
           + "FROM CreditCardPayment ccp "
           + "WHERE strftime('%m', ccp.date) = printf('%02d', :month) "
           + "AND strftime('%Y', ccp.date) = printf('%04d', :year)"
           + "AND ccp.creditCardDebt.creditCard.id = :crcId")
    List<CreditCardPayment>
    GetCreditCardPayments(@Param("crcId") Long    crcId,
                          @Param("month") Integer month,
                          @Param("year") Integer  year);

    /**
     * Get credit card pending payments in a month and year by credit card
     * @param crcId The credit card id
     * @param month The month
     * @param year The year
     * @return A list with all credit card pending payments in a month and year by
     *     credit card
     */
    @Query("SELECT ccp "
           + "FROM CreditCardPayment ccp "
           + "WHERE strftime('%m', ccp.date) = printf('%02d', :month) "
           + "AND strftime('%Y', ccp.date) = printf('%04d', :year) "
           + "AND ccp.creditCardDebt.creditCard.id = :crcId "
           + "AND ccp.wallet IS NULL")
    List<CreditCardPayment>
    GetPendingCreditCardPayments(@Param("crcId") Long    crcId,
                                 @Param("month") Integer month,
                                 @Param("year") Integer  year);

    /**
     * Get all pending credit card payments
     * @param crcId The credit card id
     * @return A list with all pending credit card payments
     */
    @Query("SELECT ccp "
           + "FROM CreditCardPayment ccp "
           + "WHERE ccp.creditCardDebt.creditCard.id = :crcId "
           + "AND ccp.wallet IS NULL")
    List<CreditCardPayment>
    GetAllPendingCreditCardPayments(@Param("crcId") Long crcId);

    /**
     * Get payments by debt id
     * @param debtId The debt id
     * @return A list with all credit card payments by debt id
     */
    @Query("SELECT ccp "
           + "FROM CreditCardPayment ccp "
           + "WHERE ccp.creditCardDebt.id = :debtId")
    List<CreditCardPayment>
    GetPaymentsByDebtId(@Param("debtId") Long debtId);

    /**
     * Get the total paid amount of a credit card
     * @param creditCardId The credit card id
     * @return The total paid amount of the credit card
     */
    @Query("SELECT COALESCE(SUM(ccp.amount), 0) "
           + "FROM CreditCardPayment ccp "
           + "WHERE ccp.creditCardDebt.creditCard.id = :creditCardId "
           + "AND ccp.wallet IS NOT NULL")
    BigDecimal
    GetTotalPaidAmount(@Param("creditCardId") Long creditCardId);

    /**
     * TODO: Create tests
     * Get the total debt amount of all credit cards in a month and year
     * @param month The month
     * @param year The year
     * @return The total debt amount of all credit cards in a month and year
     */
    @Query("SELECT COALESCE(SUM(ccp.amount), 0) "
           + "FROM CreditCardPayment ccp "
           + "WHERE strftime('%m', ccp.date) = printf('%02d', :month) "
           + "AND strftime('%Y', ccp.date) = printf('%04d', :year)")
    BigDecimal
    GetTotalDebtAmount(@Param("month") Integer month, @Param("year") Integer year);

    /**
     * Get the total debt amount of all credit cards in a year
     * @param year The year
     * @return The total debt amount of all credit cards in a year
     */
    @Query("SELECT COALESCE(SUM(ccp.amount), 0) "
           + "FROM CreditCardPayment ccp "
           + "WHERE strftime('%Y', ccp.date) = printf('%04d', :year)")
    BigDecimal
    GetTotalDebtAmount(@Param("year") Integer year);

    /**
     * TODO: Create tests
     * Get the total of all pending payments of all credit cards from a specified month
     * and year onward, including future months and the current month
     * @param month The starting month (inclusive)
     * @param year The starting year (inclusive)
     * @return The total of all pending payments of all credit cards from the specified
     *     month and year onward
     */
    @Query("SELECT COALESCE(SUM(ccp.amount), 0) "
           + "FROM CreditCardPayment ccp "
           + "WHERE strftime('%m', ccp.date) >= printf('%02d', :month) "
           + "AND strftime('%Y', ccp.date) >= printf('%04d', :year) "
           + "AND ccp.wallet IS NULL")
    BigDecimal
    GetTotalPendingPayments(@Param("month") Integer month, @Param("year") Integer year);

    /**
     * Get the total of all paid payments of all credit cards from a specified month
     * and year
     * @param month The month
     * @param year The year
     * @return The total of all paid payments of all credit cards from the specified
     *   month and year
     */
    @Query("SELECT COALESCE(SUM(ccp.amount), 0) "
           + "FROM CreditCardPayment ccp "
           + "WHERE strftime('%m', ccp.date) = printf('%02d', :month) "
           + "AND strftime('%Y', ccp.date) = printf('%04d', :year) "
           + "AND ccp.wallet IS NOT NULL")
    BigDecimal
    GetPaidPaymentsByMonth(@Param("month") Integer month, @Param("year") Integer year);

    /**
     * Get the total of all paid payments of all credit cards from a specified month
     * and year by wallet id
     * @param month The month
     * @param year The year
     * @return The total of all paid payments of all credit cards from the specified
     *   month and year by wallet id
     */
    @Query("SELECT COALESCE(SUM(ccp.amount), 0) "
           + "FROM CreditCardPayment ccp "
           + "WHERE strftime('%m', ccp.date) = printf('%02d', :month) "
           + "AND strftime('%Y', ccp.date) = printf('%04d', :year) "
           + "AND ccp.wallet.id = :walletId")
    BigDecimal
    GetPaidPaymentsByMonth(@Param("walletId") Long walletId,
                           @Param("month") Integer month,
                           @Param("year") Integer  year);

    /**
     * Get the total of all pending payments of all credit cards from a specified month
     * and year
     * @param month The month
     * @param year The year
     * @return The total of all pending payments of all credit cards from the specified
     *   month and year
     */
    @Query("SELECT COALESCE(SUM(ccp.amount), 0) "
           + "FROM CreditCardPayment ccp "
           + "WHERE strftime('%m', ccp.date) = printf('%02d', :month) "
           + "AND strftime('%Y', ccp.date) = printf('%04d', :year) "
           + "AND ccp.wallet IS NULL")
    BigDecimal
    GetPendingPaymentsByMonth(@Param("month") Integer month,
                              @Param("year") Integer  year);

    /**
     * Get the total of all pending payments of all credit cards from a specified year
     * onward, including future years and the current year
     * @param year The starting year (inclusive)
     * @return The total of all pending payments of all credit cards from the specified
     *    year onward
     */
    @Query("SELECT COALESCE(SUM(ccp.amount), 0) "
           + "FROM CreditCardPayment ccp "
           + "WHERE strftime('%Y', ccp.date) >= printf('%04d', :year) "
           + "AND ccp.wallet IS NULL")
    BigDecimal
    GetTotalPendingPayments(@Param("year") Integer year);

    /**
     * Get the total of all paid payments of all credit cards from a specified year
     * @param year The year
     * @return The total of all paid payments of all credit cards from the specified
     *     year
     */
    @Query("SELECT COALESCE(SUM(ccp.amount), 0) "
           + "FROM CreditCardPayment ccp "
           + "WHERE strftime('%Y', ccp.date) = printf('%04d', :year) "
           + "AND ccp.wallet IS NOT NULL")
    BigDecimal
    GetPaidPaymentsByYear(@Param("year") Integer year);

    /**
     * Get the total of all pending payments of all credit cards from a specified year
     * @param year The year
     * @return The total of all pending payments of all credit cards from the specified
     *     year
     */
    @Query("SELECT COALESCE(SUM(ccp.amount), 0) "
           + "FROM CreditCardPayment ccp "
           + "WHERE strftime('%Y', ccp.date) = printf('%04d', :year) "
           + "AND ccp.wallet IS NULL")
    BigDecimal
    GetPendingPaymentsByYear(@Param("year") Integer year);

    /**
     * Get the total of all pending payments of a credit card
     * @param creditCardId The credit card id
     * @return The total of all pending payments of a credit card
     */
    @Query("SELECT COALESCE(SUM(ccp.amount), 0) "
           + "FROM CreditCardPayment ccp "
           + "JOIN ccp.creditCardDebt ccd "
           + "WHERE ccd.creditCard.id = :creditCardId "
           + "AND ccp.wallet IS NULL")
    BigDecimal
    GetTotalPendingPayments(@Param("creditCardId") Long creditCardId);

    /**
     * Get the total of all pending payments of all credit cards
     * @return The total of all pending payments of all credit cards
     */
    @Query("SELECT COALESCE(SUM(ccp.amount), 0) "
           + "FROM CreditCardPayment ccp "
           + "WHERE ccp.wallet IS NULL")
    BigDecimal
    GetTotalPendingPayments();

    /**
     * Get the remaining debt of a purchase
     * @param debtId The id of the debt
     * @return The remaining debt of the purchase
     */
    @Query("SELECT COALESCE(SUM(ccp.amount), 0) "
           + "FROM CreditCardPayment ccp "
           + "WHERE ccp.creditCardDebt.id = :debtId "
           + "AND ccp.wallet IS NULL")
    BigDecimal
    GetRemainingDebt(@Param("debtId") Long debtId);

    /**
     * Get the invoice amount of a credit card in a specified month and year
     * @param creditCardId The credit card id
     * @param month The month
     * @param year The year
     * @return The invoice amount of the credit card in the specified month and year
     */
    @Query("SELECT COALESCE(SUM(ccp.amount), 0) "
           + "FROM CreditCardPayment ccp "
           + "JOIN ccp.creditCardDebt ccd "
           + "WHERE ccd.creditCard.id = :creditCardId "
           + "AND strftime('%m', ccp.date) = printf('%02d', :month) "
           + "AND strftime('%Y', ccp.date) = printf('%04d', :year)")
    BigDecimal
    GetInvoiceAmount(@Param("creditCardId") Long creditCardId,
                     @Param("month") Integer     month,
                     @Param("year") Integer      year);

    /**
     * Get next invoice date of a credit card
     * @param creditCardId The credit card id
     * @return The next invoice date of the credit card
     */
    @Query("SELECT MIN(ccp.date) "
           + "FROM CreditCardPayment ccp "
           + "JOIN ccp.creditCardDebt ccd "
           + "WHERE ccd.creditCard.id = :creditCardId "
           + "AND ccp.wallet IS NULL")
    String
    GetNextInvoiceDate(@Param("creditCardId") Long creditCardId);
}
