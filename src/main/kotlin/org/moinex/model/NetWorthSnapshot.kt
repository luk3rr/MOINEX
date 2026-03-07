/*
 * Filename: NetWorthSnapshot.kt (original filename: NetWorthSnapshot.java)
 * Created on: January 22, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.moinex.common.toRounded
import java.math.BigDecimal

/**
 * Represents a cached snapshot of net worth calculation for a specific month
 * This avoids expensive recalculations on every page load
 */
@Entity
@Table(name = "net_worth_snapshot")
class NetWorthSnapshot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "month", nullable = false)
    var month: Int,
    @Column(name = "year", nullable = false)
    var year: Int,
    @Column(name = "assets", nullable = false, scale = 2)
    var assets: BigDecimal,
    @Column(name = "liabilities", nullable = false, scale = 2)
    var liabilities: BigDecimal,
    @Column(name = "net_worth", nullable = false, scale = 2)
    var netWorth: BigDecimal,
    @Column(name = "wallet_balances", nullable = false, scale = 2)
    var walletBalances: BigDecimal,
    @Column(name = "investments", nullable = false, scale = 2)
    var investments: BigDecimal,
    @Column(name = "credit_card_debt", nullable = false, scale = 2)
    var creditCardDebt: BigDecimal,
    @Column(name = "negative_wallet_balances", nullable = false, scale = 2)
    var negativeWalletBalances: BigDecimal,
    @Column(name = "calculated_at", nullable = false)
    var calculatedAt: String,
) {
    init {
        assets = assets.toRounded()
        liabilities = liabilities.toRounded()
        netWorth = netWorth.toRounded()
        walletBalances = walletBalances.toRounded()
        investments = investments.toRounded()
        creditCardDebt = creditCardDebt.toRounded()
        negativeWalletBalances = negativeWalletBalances.toRounded()

        require(month in 1..12) {
            "Month must be between 1 and 12"
        }
        require(year > 0) {
            "Year must be positive"
        }
    }

    override fun toString(): String = "NetWorthSnapshot [month=$month, year=$year, netWorth=$netWorth]"
}
