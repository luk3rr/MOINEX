package org.moinex.config

import org.moinex.model.enums.BalanceType
import org.moinex.model.wallettransaction.Wallet
import java.math.BigDecimal

data class BalanceCalculationConfig(
    val balanceType: BalanceType,
    val logPrefix: String,
    val walletFilter: (Wallet) -> Boolean,
    val resultTransform: (BigDecimal) -> BigDecimal,
) {
    companion object {
        val POSITIVE =
            BalanceCalculationConfig(
                balanceType = BalanceType.POSITIVE,
                logPrefix = "WALLET BALANCES",
                walletFilter = { it.balance > BigDecimal.ZERO },
                resultTransform = { it },
            )

        val NEGATIVE =
            BalanceCalculationConfig(
                balanceType = BalanceType.NEGATIVE,
                logPrefix = "NEGATIVE WALLET BALANCES",
                walletFilter = { it.balance < BigDecimal.ZERO },
                resultTransform = { it.abs() },
            )
    }
}
