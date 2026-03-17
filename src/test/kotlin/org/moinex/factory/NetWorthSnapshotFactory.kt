package org.moinex.factory

import org.moinex.model.NetWorthSnapshot
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

object NetWorthSnapshotFactory {
    fun create(
        referenceMonth: YearMonth = YearMonth.now(),
        assets: BigDecimal = BigDecimal("5000.00"),
        liabilities: BigDecimal = BigDecimal("1500.00"),
        netWorth: BigDecimal = BigDecimal("3500.00"),
        walletBalances: BigDecimal = BigDecimal("5000.00"),
        investments: BigDecimal = BigDecimal.ZERO,
        creditCardDebt: BigDecimal = BigDecimal("1500.00"),
        negativeWalletBalances: BigDecimal = BigDecimal.ZERO,
        calculatedAt: LocalDateTime = LocalDateTime.now(),
    ): NetWorthSnapshot =
        NetWorthSnapshot(
            referenceMonth = referenceMonth,
            assets = assets,
            liabilities = liabilities,
            netWorth = netWorth,
            walletBalances = walletBalances,
            investments = investments,
            creditCardDebt = creditCardDebt,
            negativeWalletBalances = negativeWalletBalances,
            calculatedAt = calculatedAt,
        )
}
