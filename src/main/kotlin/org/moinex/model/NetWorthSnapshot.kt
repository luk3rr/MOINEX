/*
 * Filename: NetWorthSnapshot.kt (original filename: NetWorthSnapshot.java)
 * Created on: January 22, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.moinex.common.converter.LocalDateTimeStringConverter
import org.moinex.common.converter.YearMonthStringConverter
import org.moinex.common.extension.toRounded
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

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
    @Convert(converter = YearMonthStringConverter::class)
    @Column(name = "reference_month", nullable = false)
    var referenceMonth: YearMonth,
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
    @Convert(converter = LocalDateTimeStringConverter::class)
    var calculatedAt: LocalDateTime,
) {
    init {
        assets = assets.toRounded()
        liabilities = liabilities.toRounded()
        netWorth = netWorth.toRounded()
        walletBalances = walletBalances.toRounded()
        investments = investments.toRounded()
        creditCardDebt = creditCardDebt.toRounded()
        negativeWalletBalances = negativeWalletBalances.toRounded()
    }

    override fun toString(): String = "NetWorthSnapshot [referenceMonth=$referenceMonth, netWorth=$netWorth, calculatedAt=$calculatedAt]"
}
