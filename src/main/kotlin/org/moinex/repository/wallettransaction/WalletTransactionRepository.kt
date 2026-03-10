/*
 * Filename: WalletTransactionRepository.kt (original filename: WalletTransactionRepository.java)
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.repository.wallettransaction

import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.WalletTransaction
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface WalletTransactionRepository : JpaRepository<WalletTransaction, Int> {
    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE wt.category.isArchived = false " +
            "AND wt.wallet.isArchived = false " +
            "ORDER BY wt.date DESC",
    )
    fun findNonArchivedTransactions(): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE wt.type = 'INCOME' " +
            "ORDER BY wt.date DESC",
    )
    fun findIncomeTransactions(): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE wt.type = 'EXPENSE' " +
            "ORDER BY wt.date DESC",
    )
    fun findExpenseTransactions(): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE wt.type = :type " +
            "AND wt.category.isArchived = false " +
            "AND wt.wallet.isArchived = false " +
            "ORDER BY wt.date DESC",
    )
    fun findAllNonArchivedTransactionsByType(
        @Param("type") type: WalletTransactionType,
    ): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE strftime('%m', wt.date) = printf('%02d', :month) " +
            "AND strftime('%Y', wt.date) = printf('%04d', :year) " +
            "ORDER BY wt.date DESC",
    )
    fun findTransactionsByMonth(
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE strftime('%m', wt.date) = printf('%02d', :month) " +
            "AND strftime('%Y', wt.date) = printf('%04d', :year) " +
            "AND wt.category.isArchived = false " +
            "AND wt.wallet.isArchived = false " +
            "ORDER BY wt.date DESC",
    )
    fun findNonArchivedTransactionsByMonth(
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE strftime('%Y', wt.date) = printf('%04d', :year) " +
            "ORDER BY wt.date DESC",
    )
    fun findTransactionsByYear(
        @Param("year") year: Int,
    ): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE strftime('%Y', wt.date) = printf('%04d', :year) " +
            "AND wt.category.isArchived = false " +
            "AND wt.wallet.isArchived = false " +
            "ORDER BY wt.date DESC",
    )
    fun findNonArchivedTransactionsByYear(
        @Param("year") year: Int,
    ): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE wt.wallet.id = :walletId " +
            "AND strftime('%m', wt.date) = printf('%02d', :month) " +
            "AND strftime('%Y', wt.date) = printf('%04d', :year) " +
            "ORDER BY wt.date DESC",
    )
    fun findTransactionsByWalletAndMonth(
        @Param("walletId") walletId: Int,
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE wt.wallet.id = :walletId " +
            "AND strftime('%m', wt.date) = printf('%02d', :month) " +
            "AND strftime('%Y', wt.date) = printf('%04d', :year) " +
            "AND wt.category.isArchived = false " +
            "AND wt.wallet.isArchived = false " +
            "ORDER BY wt.date DESC",
    )
    fun findNonArchivedTransactionsByWalletAndMonth(
        @Param("walletId") walletId: Int,
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE wt.date >= :startDate " +
            "AND wt.date <= :endDate " +
            "ORDER BY wt.date DESC",
    )
    fun findTransactionsBetweenDates(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
    ): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE wt.date >= :startDate " +
            "AND wt.date <= :endDate " +
            "AND wt.category.isArchived = false " +
            "AND wt.wallet.isArchived = false " +
            "ORDER BY wt.date DESC",
    )
    fun findNonArchivedTransactionsBetweenDates(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
    ): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE strftime('%m', wt.date) = printf('%02d', :month) " +
            "AND strftime('%Y', wt.date) = printf('%04d', :year) " +
            "AND wt.status = 'CONFIRMED' " +
            "ORDER BY wt.date DESC",
    )
    fun findConfirmedTransactionsByMonth(
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE strftime('%m', wt.date) = printf('%02d', :month) " +
            "AND strftime('%Y', wt.date) = printf('%04d', :year) " +
            "AND wt.status = 'CONFIRMED' " +
            "AND wt.category.isArchived = false " +
            "AND wt.wallet.isArchived = false " +
            "ORDER BY wt.date DESC",
    )
    fun findNonArchivedConfirmedTransactionsByMonth(
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE strftime('%m', wt.date) = printf('%02d', :month) " +
            "AND strftime('%Y', wt.date) = printf('%04d', :year) " +
            "AND wt.status = 'PENDING' " +
            "ORDER BY wt.date DESC",
    )
    fun findPendingTransactionsByMonth(
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE strftime('%m', wt.date) = printf('%02d', :month) " +
            "AND strftime('%Y', wt.date) = printf('%04d', :year) " +
            "AND wt.status = 'PENDING' " +
            "AND wt.category.isArchived = false " +
            "AND wt.wallet.isArchived = false " +
            "ORDER BY wt.date DESC",
    )
    fun findNonArchivedPendingTransactionsByMonth(
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "ORDER BY wt.date DESC",
    )
    fun findLastTransactions(pageable: Pageable): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE wt.category.isArchived = false " +
            "AND wt.wallet.isArchived = false " +
            "ORDER BY wt.date DESC",
    )
    fun findNonArchivedLastTransactions(pageable: Pageable): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE wt.wallet.id = :walletId " +
            "ORDER BY wt.date DESC",
    )
    fun findLastTransactionsByWallet(
        @Param("walletId") walletId: Int,
        pageable: Pageable,
    ): List<WalletTransaction>

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE wt.wallet.id = :walletId " +
            "AND wt.category.isArchived = false " +
            "AND wt.wallet.isArchived = false " +
            "ORDER BY wt.date DESC",
    )
    fun findNonArchivedLastTransactionsByWallet(
        @Param("walletId") walletId: Int,
        pageable: Pageable,
    ): List<WalletTransaction>

    @Query(
        "SELECT min(wt.date) " +
            "FROM WalletTransaction wt",
    )
    fun findOldestTransactionDate(): LocalDateTime?

    @Query(
        "SELECT min(wt.date) " +
            "FROM WalletTransaction wt " +
            "WHERE wt.category.isArchived = false " +
            "AND wt.wallet.isArchived = false",
    )
    fun findNonArchivedOldestTransactionDate(): LocalDateTime?

    @Query(
        "SELECT max(wt.date) " +
            "FROM WalletTransaction wt",
    )
    fun findNewestTransactionDate(): LocalDateTime?

    @Query(
        "SELECT max(wt.date) " +
            "FROM WalletTransaction wt " +
            "WHERE wt.category.isArchived = false " +
            "AND wt.wallet.isArchived = false",
    )
    fun findNonArchivedNewestTransactionDate(): LocalDateTime?

    @Query(
        "SELECT count(wt) " +
            "FROM WalletTransaction wt " +
            "WHERE wt.wallet.id = :walletId",
    )
    fun getTransactionCountByWallet(
        @Param("walletId") walletId: Int,
    ): Int

    @Query(
        "SELECT count(wt) " +
            "FROM WalletTransaction wt " +
            "WHERE wt.wallet.id = :walletId " +
            "AND wt.category.isArchived = false " +
            "AND wt.wallet.isArchived = false",
    )
    fun getCountNonArchivedTransactionsByWallet(
        @Param("walletId") walletId: Int,
    ): Int

    @Query(
        "SELECT COUNT(wt) > 0 " +
            "FROM WalletTransaction wt " +
            "WHERE wt.id = :transactionId",
    )
    fun existsWalletByTransactionId(
        @Param("transactionId") transactionId: Int,
    ): Boolean

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE wt.type = :walletTransactionType " +
            "AND wt.wallet.isArchived = false " +
            "AND wt.category.isArchived = false " +
            "AND wt.date = (SELECT max(wt2.date) " +
            "               FROM WalletTransaction wt2 " +
            "               WHERE wt2.wallet.isArchived = false " +
            "               AND wt2.category.isArchived = false " +
            "               AND wt2.description = wt.description) " +
            "ORDER BY wt.date DESC",
    )
    fun findSuggestions(
        @Param("walletTransactionType") walletTransactionType: WalletTransactionType,
    ): List<WalletTransaction>

    @Query(
        "SELECT COALESCE(SUM(wt.amount), 0) " +
            "FROM WalletTransaction wt " +
            "WHERE wt.category.id IN :categoryIds " +
            "AND wt.type = :walletTransactionType " +
            "AND wt.date BETWEEN :startDate AND :endDate",
    )
    fun sumAmountByCategoriesAndDateRange(
        @Param("categoryIds") categoryIds: List<Int>,
        @Param("walletTransactionType") walletTransactionType: WalletTransactionType,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
    ): BigDecimal

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE strftime('%m', wt.date) = printf('%02d', :month) " +
            "AND strftime('%Y', wt.date) = printf('%04d', :year) " +
            "AND wt.includeInAnalysis = true " +
            "AND wt.category.isArchived = false " +
            "AND wt.wallet.isArchived = false " +
            "ORDER BY wt.date DESC",
    )
    fun findNonArchivedTransactionsByMonthForAnalysis(
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<WalletTransaction>

    @Query(
        "SELECT COALESCE(SUM(wt.amount), 0) " +
            "FROM WalletTransaction wt " +
            "WHERE wt.category.id IN :categoryIds " +
            "AND wt.type = :walletTransactionType " +
            "AND wt.includeInAnalysis = true " +
            "AND wt.date BETWEEN :startDate AND :endDate",
    )
    fun getSumAmountByCategoriesAndDateBetweenForAnalysis(
        @Param("categoryIds") categoryIds: List<Int>,
        @Param("walletTransactionType") walletTransactionType: WalletTransactionType,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
    ): BigDecimal

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE wt.wallet.id = :walletId " +
            "AND wt.date >= :date " +
            "ORDER BY wt.date ASC",
    )
    fun findTransactionsByWalletAfterDate(
        @Param("walletId") walletId: Int,
        @Param("date") date: LocalDateTime,
    ): List<WalletTransaction>

    @Query(
        "SELECT MIN(wt.date) " +
            "FROM WalletTransaction wt " +
            "WHERE wt.wallet.id = :walletId " +
            "AND wt.status = 'CONFIRMED'",
    )
    fun findFirstTransactionDate(
        @Param("walletId") walletId: Int,
    ): LocalDateTime?

    @Query(
        "SELECT wt " +
            "FROM WalletTransaction wt " +
            "WHERE wt.wallet.id = :walletId " +
            "AND wt.date <= :endDate " +
            "AND wt.status = 'CONFIRMED' " +
            "ORDER BY wt.date ASC",
    )
    fun findAllConfirmedTransactionsUpToDate(
        @Param("walletId") walletId: Int,
        @Param("endDate") endDate: LocalDateTime,
    ): List<WalletTransaction>

    @Query(
        "SELECT count(t) " +
            "FROM WalletTransaction t " +
            "WHERE t.category.id = :categoryId",
    )
    fun getTransactionCountByCategory(
        @Param("categoryId") categoryId: Int,
    ): Int
}
