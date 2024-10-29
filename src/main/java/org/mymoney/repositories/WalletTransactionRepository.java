/*
 * Filename: WalletTransactionRepository.java
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.mymoney.repositories;

import java.util.List;
import java.util.Optional;
import org.mymoney.entities.Wallet;
import org.mymoney.entities.WalletTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for the WalletTransaction entity
 *
 * This repository provides methods to query the database for WalletTransaction
 *
 * Each method to get transactions has a version that returns only transactions
 * that have a category that is not archived
 */
@Repository
public interface WalletTransactionRepository
    extends JpaRepository<WalletTransaction, Long> {

    /**
     * Get all transactions where both the category and wallet are not archived
     * @return A list with all transactions
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE wt.category.archived = false "
           + "AND wt.wallet.archived = false "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindNonArchivedTransactions();

    /**
     * Get all income transactions
     * @return A list with all income transactions
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE wt.type = 'INCOME' "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindIncomeTransactions();

    /**
     * Get all income transactions where both the category and wallet are not archived
     * @return A list with all income transactions
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE wt.type = 'INCOME' "
           + "AND wt.category.archived = false "
           + "AND wt.wallet.archived = false "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindNonArchivedIncomeTransactions();

    /**
     * Get all expense transactions
     * @return A list with all expense transactions
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE wt.type = 'EXPENSE' "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindExpenseTransactions();

    /**
     * Get all expense transactions where both the category and wallet are not archived
     * @return A list with all expense transactions
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE wt.type = 'EXPENSE' "
           + "AND wt.category.archived = false "
           + "AND wt.wallet.archived = false "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindNonArchivedExpenseTransactions();

    /**
     * Get the all transactions by month and year
     * @param month The month
     * @param year The year
     * @return A list with the transactions by month and year
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE strftime('%m', wt.date) = printf('%02d', :month) "
           + "AND strftime('%Y', wt.date) = printf('%04d', :year) "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindTransactionsByMonth(@Param("month") Integer month, @Param("year") Integer year);

    /**
     * Get the all transactions by month and year where both the category and wallet are
     * not archived
     * @param month The month
     * @param year The year
     * @return A list with the transactions by month and year
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE strftime('%m', wt.date) = printf('%02d', :month) "
           + "AND strftime('%Y', wt.date) = printf('%04d', :year) "
           + "AND wt.category.archived = false "
           + "AND wt.wallet.archived = false "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindNonArchivedTransactionsByMonth(@Param("month") Integer month,
                                       @Param("year") Integer  year);

    /**
     * Get the all transactions by year
     * @param year The year
     * @return A list with the transactions by year
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE strftime('%Y', wt.date) = printf('%04d', :year) "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindTransactionsByYear(@Param("year") Integer year);

    /**
     * Get the all transactions by year where both the category and wallet are not
     * archived
     * @param year The year
     * @return A list with the transactions by year
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE strftime('%Y', wt.date) = printf('%04d', :year) "
           + "AND wt.category.archived = false "
           + "AND wt.wallet.archived = false "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindNonArchivedTransactionsByYear(@Param("year") Integer year);

    /**
     * Get the transactions by wallet and month
     * @param walletId The id of the wallet
     * @param month The month
     * @param year The year
     * @return A list with the transactions in the wallet by month
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE wt.wallet.id = :walletId "
           + "AND strftime('%m', wt.date) = printf('%02d', :month) "
           + "AND strftime('%Y', wt.date) = printf('%04d', :year) "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindTransactionsByWalletAndMonth(@Param("walletId") Long walletId,
                                     @Param("month") Integer month,
                                     @Param("year") Integer  year);

    /**
     * Get the transactions by wallet and month where both the category and wallet are
     * not
     * @param walletId The id of the wallet
     * @param month The month
     * @param year The year
     * @return A list with the transactions in the wallet by month
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE wt.wallet.id = :walletId "
           + "AND strftime('%m', wt.date) = printf('%02d', :month) "
           + "AND strftime('%Y', wt.date) = printf('%04d', :year) "
           + "AND wt.category.archived = false "
           + "AND wt.wallet.archived = false "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindNonArchivedTransactionsByWalletAndMonth(@Param("walletId") Long walletId,
                                                @Param("month") Integer month,
                                                @Param("year") Integer  year);

    /**
     * Get all transactions between two dates
     * @param startDate The start date
     * @param endDate The end date
     * @return A list with the transactions between the two dates
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE wt.date >= :startDate "
           + "AND wt.date <= :endDate "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindTransactionsBetweenDates(@Param("startDate") String startDate,
                                 @Param("endDate") String   endDate);

    /**
     * Get all transactions between two dates where both the category and wallet are not
     * archived
     * @param startDate The start date
     * @param endDate The end date
     * @return A list with the transactions between the two dates
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE wt.date >= :startDate "
           + "AND wt.date <= :endDate "
           + "AND wt.category.archived = false "
           + "AND wt.wallet.archived = false "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindNonArchivedTransactionsBetweenDates(@Param("startDate") String startDate,
                                            @Param("endDate") String   endDate);

    /**
     * Get the confirmed transactions by month and year
     * @param month The month
     * @param year The year
     * @return A list with the transactions in the wallet by month and year
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE strftime('%m', wt.date) = printf('%02d', :month) "
           + "AND strftime('%Y', wt.date) = printf('%04d', :year) "
           + "AND wt.status = 'CONFIRMED' "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindConfirmedTransactionsByMonth(@Param("month") Integer month,
                                     @Param("year") Integer  year);

    /**
     * Get the confirmed transactions by month and year where both the category and
     * wallet are not archived
     * @param month The month
     * @param year The year
     * @return A list with the transactions in the wallet by month and year
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE strftime('%m', wt.date) = printf('%02d', :month) "
           + "AND strftime('%Y', wt.date) = printf('%04d', :year) "
           + "AND wt.status = 'CONFIRMED' "
           + "AND wt.category.archived = false "
           + "AND wt.wallet.archived = false "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindNonArchivedConfirmedTransactionsByMonth(@Param("month") Integer month,
                                                @Param("year") Integer  year);

    /**
     * Get the pending transactions by month and year
     * @param month The month
     * @param year The year
     * @return A list with the transactions in the wallet by month and year
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE strftime('%m', wt.date) = printf('%02d', :month) "
           + "AND strftime('%Y', wt.date) = printf('%04d', :year) "
           + "AND wt.status = 'PENDING' "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindPendingTransactionsByMonth(@Param("month") Integer month,
                                   @Param("year") Integer  year);

    /**
     * Get the pending transactions by month and year where both the category and wallet
     * are not archived
     * @param month The month
     * @param year The year
     * @return A list with the transactions in the wallet by month and year
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE strftime('%m', wt.date) = printf('%02d', :month) "
           + "AND strftime('%Y', wt.date) = printf('%04d', :year) "
           + "AND wt.status = 'PENDING' "
           + "AND wt.category.archived = false "
           + "AND wt.wallet.archived = false "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindNonArchivedPendingTransactionsByMonth(@Param("month") Integer month,
                                              @Param("year") Integer  year);

    /**
     * Get the last n transactions of all wallets
     * @param pageable The pageable object
     * @return A list with the last n transactions of all wallets
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindLastTransactions(Pageable pageable);

    /**
     * Get the last n transactions of all wallets where both the category and wallet are
     * not archived
     * @param pageable The pageable object
     * @return A list with the last n transactions of all wallets
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE wt.category.archived = false "
           + "AND wt.wallet.archived = false "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindNonArchivedLastTransactions(Pageable pageable);

    /**
     * Get the last n transactions in a wallet
     * @param walletId The id of the wallet
     * @param pageable The pageable object
     * @return A list with the last n transactions in the wallet
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE wt.wallet.id = :walletId "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindLastTransactionsByWallet(@Param("walletId") Long walletId, Pageable pageable);

    /**
     * Get the last n transactions in a wallet where both the category and wallet are
     * not archived
     * @param walletId The id of the wallet
     * @param pageable The pageable object
     * @return A list with the last n transactions in the wallet
     */
    @Query("SELECT wt "
           + "FROM WalletTransaction wt "
           + "WHERE wt.wallet.id = :walletId "
           + "AND wt.category.archived = false "
           + "AND wt.wallet.archived = false "
           + "ORDER BY wt.date DESC")
    List<WalletTransaction>
    FindNonArchivedLastTransactionsByWallet(@Param("walletId") Long walletId,
                                            Pageable                pageable);

    /**
     * Get the date of the oldest transaction
     * @return The date of the oldest transaction
     */
    @Query("SELECT MIN(wt.date) "
           + "FROM WalletTransaction wt")
    String
    FindOldestTransactionDate();

    /**
     * Get the date of the oldest transaction where both the category and wallet are not
     * archived
     * @return The date of the oldest transaction
     */
    @Query("SELECT MIN(wt.date) "
           + "FROM WalletTransaction wt "
           + "WHERE wt.category.archived = false "
           + "AND wt.wallet.archived = false")
    String
    FindNonArchivedOldestTransactionDate();

    /**
     * Get the date of the newest transaction
     * @return The date of the newest transaction
     */
    @Query("SELECT MAX(wt.date) "
           + "FROM WalletTransaction wt")
    String
    FindNewestTransactionDate();

    /**
     * Get the date of the newest transaction and the category is not archived
     * @return The date of the newest transaction
     */
    @Query("SELECT MAX(wt.date) "
           + "FROM WalletTransaction wt "
           + "WHERE wt.category.archived = false "
           + "AND wt.wallet.archived = false")
    String
    FindNonArchivedNewestTransactionDate();

    /**
     * Get count of transactions by wallet
     * @param walletId The id of the wallet
     * @return The count of transactions in the wallet
     */
    @Query("SELECT COUNT(wt) "
           + "FROM WalletTransaction wt "
           + "WHERE wt.wallet.id = :walletId")
    Long
    GetTransactionCountByWallet(@Param("walletId") Long walletId);

    /**
     * Get count of transactions by wallet where both the category and wallet are not
     * archived
     * @param walletId The id of the wallet
     * @return The count of transactions in the wallet
     */
    @Query("SELECT COUNT(wt) "
           + "FROM WalletTransaction wt "
           + "WHERE wt.wallet.id = :walletId "
           + "AND wt.category.archived = false "
           + "AND wt.wallet.archived = false")
    Long
    CountNonArchivedTransactionsByWallet(@Param("walletId") Long walletId);

    /**
     * Get the wallet by transaction id
     * @param transactionId The id of the transaction
     */
    @Query("SELECT wt.wallet "
           + "FROM WalletTransaction wt "
           + "WHERE wt.id = :transactionId")
    Optional<Wallet>
    FindWalletByTransactionId(@Param("transactionId") Long transactionId);
}
