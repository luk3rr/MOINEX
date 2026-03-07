package org.moinex.factory

import org.moinex.model.goal.Goal
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletType
import java.math.BigDecimal
import java.time.LocalDate

object GoalFactory {
    fun create(
        id: Int? = null,
        name: String = "Emergency Fund",
        initialBalance: BigDecimal = BigDecimal("1000.00"),
        targetBalance: BigDecimal = BigDecimal("10000.00"),
        targetDate: LocalDate = LocalDate.now().plusMonths(12),
        completionDate: LocalDate? = null,
        motivation: String? = null,
        type: WalletType = WalletTypeFactory.create(1, "Goal"),
        isArchived: Boolean = false,
        masterWallet: Wallet? = null,
    ): Goal =
        Goal(
            id = id,
            name = name,
            initialBalance = initialBalance,
            targetBalance = targetBalance,
            targetDate = targetDate,
            completionDate = completionDate,
            motivation = motivation,
            type = type,
            isArchived = isArchived,
            masterWallet = masterWallet,
        )
}
