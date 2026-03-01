/*
 * Filename: CreditCardPaymentRepository.kt
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.creditcard

import org.moinex.model.creditcard.CreditCardPayment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface CreditCardPaymentRepository : JpaRepository<CreditCardPayment, Int> {
    /**
     * Get all credit card payments in a month and year
     * @param month The month
     * @param year The year
     * @return A list with all credit card payments in a month and year
     */
    @Query(
        "SELECT ccp " +
            "FROM CreditCardPayment ccp " +
            "WHERE strftime('%m', ccp.date) = printf('%02d', :month) " +
            "AND strftime('%Y', ccp.date) = printf('%04d', :year) " +
            "AND ccp.wallet IS NOT NULL",
    )
    fun getAllPaidPaymentsByMonth(
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<CreditCardPayment>

    /**
     * Get credit card payments in a month and year
     * @param month The month
     * @param year The year
     * @return A list with all credit card payments in a month and year
     */
    @Query(
        "SELECT ccp " +
            "FROM CreditCardPayment ccp " +
            "WHERE strftime('%m', ccp.date) = printf('%02d', :month) " +
            "AND strftime('%Y', ccp.date) = printf('%04d', :year)",
    )
    fun getCreditCardPayments(
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<CreditCardPayment>

    /**
     * Get credit card payments in a month and year by credit card
     * @param crcId The credit card id
     * @param month The month
     * @param year The year
     * @return A list with all credit card payments in a month and year by credit card
     */
    @Query(
        "SELECT ccp " +
            "FROM CreditCardPayment ccp " +
            "WHERE strftime('%m', ccp.date) = printf('%02d', :month) " +
            "AND strftime('%Y', ccp.date) = printf('%04d', :year) " +
            "AND ccp.creditCardDebt.creditCard.id = :crcId",
    )
    fun getCreditCardPayments(
        @Param("crcId") crcId: Int,
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<CreditCardPayment>

    /**
     * Get credit card pending payments in a month and year by credit card
     * @param crcId The credit card id
     * @param month The month
     * @param year The year
     * @return A list with all credit card pending payments in a month and year by
     *     credit card
     */
    @Query(
        "SELECT ccp " +
            "FROM CreditCardPayment ccp " +
            "WHERE strftime('%m', ccp.date) = printf('%02d', :month) " +
            "AND strftime('%Y', ccp.date) = printf('%04d', :year) " +
            "AND ccp.creditCardDebt.creditCard.id = :crcId " +
            "AND ccp.wallet IS NULL " +
            "AND ccp.refunded = false",
    )
    fun getPendingCreditCardPayments(
        @Param("crcId") crcId: Int,
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<CreditCardPayment>

    /**
     * Get all pending credit card payments
     * @param crcId The credit card id
     * @return A list with all pending credit card payments
     */
    @Query(
        "SELECT ccp " +
            "FROM CreditCardPayment ccp " +
            "WHERE ccp.creditCardDebt.creditCard.id = :crcId " +
            "AND ccp.wallet IS NULL " +
            "AND ccp.refunded = false",
    )
    fun getAllPendingCreditCardPayments(
        @Param("crcId") crcId: Int,
    ): List<CreditCardPayment>

    /**
     * Get payments by debt id
     * @param debtId The debt id
     * @return A list with all credit card payments by debt id
     */
    @Query(
        "SELECT ccp " +
            "FROM CreditCardPayment ccp " +
            "WHERE ccp.creditCardDebt.id = :debtId " +
            "ORDER BY ccp.installment ASC",
    )
    fun getPaymentsByDebtOrderedByInstallment(
        @Param("debtId") debtId: Int,
    ): List<CreditCardPayment>

    /**
     * Get the total paid amount of a credit card
     * @param creditCardId The credit card id
     * @return The total paid amount of the credit card
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount), 0) " +
            "FROM CreditCardPayment ccp " +
            "WHERE ccp.creditCardDebt.creditCard.id = :creditCardId " +
            "AND ccp.wallet IS NOT NULL",
    )
    fun getTotalPaidAmount(
        @Param("creditCardId") creditCardId: Int,
    ): BigDecimal

    /**
     * Get the total debt amount of all credit cards in a month and year
     * @param month The month
     * @param year The year
     * @return The total debt amount of all credit cards in a month and year
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount), 0) " +
            "FROM CreditCardPayment ccp " +
            "WHERE strftime('%m', ccp.date) = printf('%02d', :month) " +
            "AND strftime('%Y', ccp.date) = printf('%04d', :year) " +
            "AND ccp.refunded = false",
    )
    fun getTotalDebtAmountByMonth(
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): BigDecimal

    /**
     * Get the total debt amount of all credit cards in a year
     * @param year The year
     * @return The total debt amount of all credit cards in a year
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount), 0) " +
            "FROM CreditCardPayment ccp " +
            "WHERE strftime('%Y', ccp.date) = printf('%04d', :year) " +
            "AND ccp.refunded = false",
    )
    fun getTotalDebtAmountByYear(
        @Param("year") year: Int,
    ): BigDecimal

    /**
     * Get the total of all pending payments of all credit cards from a specified month
     * and year onward, including future months and the current month
     * @param month The starting month (inclusive)
     * @param year The starting year (inclusive)
     * @return The total of all pending payments of all credit cards from the specified
     *     month and year onward
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount), 0) " +
            "FROM CreditCardPayment ccp " +
            "WHERE strftime('%m', ccp.date) >= printf('%02d', :month) " +
            "AND strftime('%Y', ccp.date) >= printf('%04d', :year) " +
            "AND ccp.wallet IS NULL " +
            "AND ccp.refunded = false",
    )
    fun getTotalPendingPayments(
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): BigDecimal

    /**
     * Get the total of all paid payments of all credit cards from a specified month
     * and year
     * @param month The month
     * @param year The year
     * @return The total of all paid payments of all credit cards from the specified
     *   month and year
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount - ccp.rebateUsed), 0) " +
            "FROM CreditCardPayment ccp " +
            "WHERE strftime('%m', ccp.date) = printf('%02d', :month) " +
            "AND strftime('%Y', ccp.date) = printf('%04d', :year) " +
            "AND ccp.wallet IS NOT NULL",
    )
    fun getEffectivePaidPaymentsByMonth(
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): BigDecimal

    /**
     * Get the total of all paid payments of all credit cards from a specified month
     * and year by wallet id
     * @param month The month
     * @param year The year
     * @return The total of all paid payments of all credit cards from the specified
     *   month and year by wallet id
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount - ccp.rebateUsed), 0) " +
            "FROM CreditCardPayment ccp " +
            "WHERE strftime('%m', ccp.date) = printf('%02d', :month) " +
            "AND strftime('%Y', ccp.date) = printf('%04d', :year) " +
            "AND ccp.wallet.id = :walletId",
    )
    fun getEffectivePaidPaymentsByMonth(
        @Param("walletId") walletId: Int,
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): BigDecimal

    /**
     * Get the total of all pending payments of all credit cards from a specified month
     * and year
     * @param month The month
     * @param year The year
     * @return The total of all pending payments of all credit cards from the specified
     *   month and year
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount), 0) " +
            "FROM CreditCardPayment ccp " +
            "WHERE strftime('%m', ccp.date) = printf('%02d', :month) " +
            "AND strftime('%Y', ccp.date) = printf('%04d', :year) " +
            "AND ccp.wallet IS NULL " +
            "AND ccp.refunded = false",
    )
    fun getPendingPaymentsByMonth(
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): BigDecimal

    /**
     * Get total pending payments amount of a credit card in a specific year and month
     * @param creditCardId The credit card id
     * @param month The month
     * @param year The year
     * @return Total amount of pending payments of the credit card in the specified year and month
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount), 0) " +
            "FROM CreditCardPayment ccp " +
            "WHERE ccp.creditCardDebt.creditCard.id = :creditCardId " +
            "AND strftime('%m', ccp.date) = printf('%02d', :month) " +
            "AND strftime('%Y', ccp.date) = printf('%04d', :year) " +
            "AND ccp.wallet IS NULL " +
            "AND ccp.refunded = false",
    )
    fun getPendingPaymentsByCreditCardAndMonth(
        @Param("creditCardId") creditCardId: Int,
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): BigDecimal

    /**
     * Get the total of all pending payments of all credit cards from a specified year
     * onward, including future years and the current year
     * @param year The starting year (inclusive)
     * @return The total of all pending payments of all credit cards from the specified
     *    year onward
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount), 0) " +
            "FROM CreditCardPayment ccp " +
            "WHERE strftime('%Y', ccp.date) >= printf('%04d', :year) " +
            "AND ccp.wallet IS NULL " +
            "AND ccp.refunded = false",
    )
    fun getTotalPendingPaymentsByYear(
        @Param("year") year: Int,
    ): BigDecimal

    /**
     * Get the total of all paid payments of all credit cards from a specified year
     * @param year The year
     * @return The total of all paid payments of all credit cards from the specified
     *     year
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount), 0) " +
            "FROM CreditCardPayment ccp " +
            "WHERE strftime('%Y', ccp.date) = printf('%04d', :year) " +
            "AND ccp.wallet IS NOT NULL",
    )
    fun getPaidPaymentsByYear(
        @Param("year") year: Int,
    ): BigDecimal

    /**
     * Get the total of all pending payments of all credit cards from a specified year
     * @param year The year
     * @return The total of all pending payments of all credit cards from the specified
     *     year
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount), 0) " +
            "FROM CreditCardPayment ccp " +
            "WHERE strftime('%Y', ccp.date) = printf('%04d', :year) " +
            "AND ccp.wallet IS NULL " +
            "AND ccp.refunded = false",
    )
    fun getPendingPaymentsByYear(
        @Param("year") year: Int,
    ): BigDecimal

    /**
     * Get the total of all pending payments of a credit card
     * @param creditCardId The credit card id
     * @return The total of all pending payments of a credit card
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount), 0) " +
            "FROM CreditCardPayment ccp " +
            "JOIN ccp.creditCardDebt ccd " +
            "WHERE ccd.creditCard.id = :creditCardId " +
            "AND ccp.wallet IS NULL " +
            "AND ccp.refunded = false",
    )
    fun getTotalPendingPaymentsByCreditCard(
        @Param("creditCardId") creditCardId: Int,
    ): BigDecimal

    /**
     * Get the total of all pending payments of all credit cards
     * @return The total of all pending payments of all credit cards
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount), 0) " +
            "FROM CreditCardPayment ccp " +
            "WHERE ccp.wallet IS NULL " +
            "AND ccp.refunded = false",
    )
    fun getTotalPendingPayments(): BigDecimal

    /**
     * Get the remaining debt of a purchase
     * @param debtId The id of the debt
     * @return The remaining debt of the purchase
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount), 0) " +
            "FROM CreditCardPayment ccp " +
            "WHERE ccp.creditCardDebt.id = :debtId " +
            "AND ccp.wallet IS NULL " +
            "AND ccp.refunded = false",
    )
    fun getRemainingDebt(
        @Param("debtId") debtId: Int,
    ): BigDecimal

    /**
     * Get the invoice amount of a credit card in a specified month and year
     * @param creditCardId The credit card id
     * @param month The month
     * @param year The year
     * @return The invoice amount of the credit card in the specified month and year
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount), 0) FROM CreditCardPayment ccp JOIN ccp.creditCardDebt" +
            " ccd WHERE ccd.creditCard.id = :creditCardId AND strftime('%m', ccp.date) =" +
            " printf('%02d', :month) AND strftime('%Y', ccp.date) = printf('%04d', :year) AND" +
            " (ccp.refunded = false OR (ccp.refunded = true AND ccp.wallet IS NOT NULL))",
    )
    fun getInvoiceAmount(
        @Param("creditCardId") creditCardId: Int,
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): BigDecimal

    /**
     * Get next invoice date of a credit card
     * @param creditCardId The credit card id
     * @return The next invoice date of the credit card
     */
    @Query(
        "SELECT min(ccp.date) " +
            "FROM CreditCardPayment ccp " +
            "JOIN ccp.creditCardDebt ccd " +
            "WHERE ccd.creditCard.id = :creditCardId " +
            "AND ccp.wallet IS NULL " +
            "AND ccp.refunded = false ",
    )
    fun getNextInvoiceDate(
        @Param("creditCardId") creditCardId: Int,
    ): LocalDateTime?

    /**
     * Sums the amount of all transactions for a given list of category IDs and a specific date range
     *
     * @param categoryIds The list of category IDs to filter by.
     * @param startDate   The start date of the period (inclusive), formatted as 'YYYY-MM-DD HH:MM:SS'.
     * @param endDate     The end date of the period (inclusive), formatted as 'YYYY-MM-DD HH:MM:SS'.
     * @return The total sum of the transaction amounts. Returns 0 if no transactions are found.
     */
    @Query(
        "SELECT COALESCE(SUM(ccp.amount), 0) " +
            "FROM CreditCardPayment ccp " +
            "JOIN ccp.creditCardDebt ccd " +
            "WHERE ccd.category.id IN :categoryIds " +
            "AND ccp.date BETWEEN :startDate AND :endDate " +
            "AND ccp.refunded = false",
    )
    fun getTotalPaymentsByCategoriesAndDateTimeBetween(
        @Param("categoryIds") categoryIds: List<Int>,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
    ): BigDecimal

    /**
     * Get total amount of paid payments up to a specific date
     * @param endDate The end date (inclusive)
     * @return Total amount of paid payments up to the date
     */
    @Query(
        "SELECT coalesce(sum(ccp.amount), 0) " +
            "FROM CreditCardPayment ccp " +
            "WHERE ccp.date <= :endDate " +
            "AND ccp.wallet IS NOT NULL",
    )
    fun getTotalPaidPaymentsUpToDate(
        @Param("endDate") endDate: LocalDateTime,
    ): BigDecimal

    /**
     * Get all paid payments up to a specific date
     * @param endDate The end date (inclusive)
     * @return List of all paid payments up to the date
     */
    @Query(
        "SELECT ccp " +
            "FROM CreditCardPayment ccp " +
            "WHERE ccp.date <= :endDate " +
            "AND ccp.wallet IS NOT NULL " +
            "ORDER BY ccp.date ASC",
    )
    fun findAllPaidUpToDate(
        @Param("endDate") endDate: LocalDateTime,
    ): List<CreditCardPayment>

    /**
     * Get the date of the earliest payment
     * @return The date of the earliest payment
     */
    @Query("SELECT min(ccp.date) FROM CreditCardPayment ccp")
    fun findEarliestPaymentDate(): LocalDateTime?

    /**
     * Get the date of the latest payment
     * @return The date of the latest payment
     */
    @Query("SELECT max(ccp.date) FROM CreditCardPayment ccp")
    fun findLatestPaymentDate(): LocalDateTime?
}
