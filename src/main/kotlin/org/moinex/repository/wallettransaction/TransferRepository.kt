/*
 * Filename: TransferRepository.kt (original filename: TransferRepository.java)
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.repository.wallettransaction

import org.moinex.model.wallettransaction.Transfer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface TransferRepository : JpaRepository<Transfer, Int> {
    @Query(
        "SELECT t " +
            "FROM Transfer t " +
            "WHERE t.senderWallet.id = :walletId " +
            "OR t.receiverWallet.id = :walletId " +
            "ORDER BY t.date DESC",
    )
    fun findTransfersByWallet(
        @Param("walletId") walletId: Int,
    ): List<Transfer>

    @Query(
        "SELECT t " +
            "FROM Transfer t " +
            "WHERE strftime('%m', t.date) = printf('%02d', :month) " +
            "AND strftime('%Y', t.date) = printf('%04d', :year) " +
            "ORDER BY t.date DESC",
    )
    fun findTransferByMonthAndYear(
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<Transfer>

    @Query(
        "SELECT t " +
            "FROM Transfer t " +
            "WHERE (t.senderWallet.id = :walletId " +
            "       OR t.receiverWallet.id = :walletId) " +
            "AND strftime('%m', t.date) = printf('%02d', :month) " +
            "AND strftime('%Y', t.date) = printf('%04d', :year) " +
            "ORDER BY t.date DESC",
    )
    fun findTransfersByWalletAndMonth(
        @Param("walletId") walletId: Int,
        @Param("month") month: Int,
        @Param("year") year: Int,
    ): List<Transfer>

    @Query(
        "SELECT count(t) " +
            "FROM Transfer t " +
            "WHERE t.senderWallet.id = :walletId " +
            "OR t.receiverWallet.id = :walletId",
    )
    fun getTransferCountByWallet(
        @Param("walletId") walletId: Int,
    ): Int

    @Query(
        "SELECT count(t) " +
            "FROM Transfer t " +
            "WHERE t.category.id = :categoryId",
    )
    fun getTransferCountByCategory(
        @Param("categoryId") categoryId: Int,
    ): Int

    @Query(
        "SELECT t " +
            "FROM Transfer t " +
            "WHERE t.senderWallet.isArchived = false " +
            "AND t.receiverWallet.isArchived = false " +
            "AND t.date = (SELECT max(t2.date) " +
            "               FROM Transfer t2 " +
            "               WHERE t2.senderWallet.isArchived = false " +
            "               AND t2.receiverWallet.isArchived = false " +
            "               AND t2.description = t.description) " +
            "ORDER BY t.date DESC",
    )
    fun findSuggestions(): List<Transfer>

    @Query(
        "SELECT COALESCE(SUM(tf.amount), 0) " +
            "FROM Transfer tf " +
            "WHERE tf.category.id IN :categoryIds " +
            "AND tf.date BETWEEN :startDate AND :endDate",
    )
    fun getSumAmountByCategoriesAndDateBetween(
        @Param("categoryIds") categoryIds: List<Int>,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
    ): BigDecimal

    @Query(
        "SELECT t " +
            "FROM Transfer t " +
            "WHERE (t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId) " +
            "AND t.date > :date " +
            "ORDER BY t.date ASC",
    )
    fun findTransfersByWalletAfterDate(
        @Param("walletId") walletId: Int,
        @Param("date") date: LocalDateTime,
    ): List<Transfer>
}
