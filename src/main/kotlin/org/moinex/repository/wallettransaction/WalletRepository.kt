/*
 * Filename: WalletRepository.kt (original filename: WalletRepository.java)
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.repository.wallettransaction

import org.moinex.model.wallettransaction.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.Optional

@Repository
interface WalletRepository : JpaRepository<Wallet, Int> {
    fun existsByName(name: String): Boolean

    fun existsByNameAndIdNot(
        name: String,
        id: Int,
    ): Boolean

    fun findByName(name: String): Optional<Wallet>

    fun findAllByOrderByNameAsc(): List<Wallet>

    fun findAllByIsArchivedTrue(): List<Wallet>

    fun findAllByIsArchivedFalse(): List<Wallet>

    fun findAllByIsArchivedFalseOrderByNameAsc(): List<Wallet>

    @Query(
        "SELECT w " +
            "FROM Wallet w " +
            "WHERE w.masterWallet.id = :masterWalletId",
    )
    fun findVirtualWalletsByMasterWallet(
        @Param("masterWalletId") masterWalletId: Int,
    ): List<Wallet>

    @Query(
        "SELECT COALESCE(COUNT(w), 0) " +
            "FROM Wallet w " +
            "WHERE w.masterWallet.id = :masterWalletId",
    )
    fun getCountOfVirtualWalletsByMasterWalletId(
        @Param("masterWalletId") masterWalletId: Int,
    ): Int

    @Query(
        "SELECT COALESCE(SUM(w.balance), 0) " +
            "FROM Wallet w " +
            "WHERE w.masterWallet.id = :masterWalletId",
    )
    fun getAllocatedBalanceByMasterWallet(
        @Param("masterWalletId") masterWalletId: Int,
    ): BigDecimal
}
