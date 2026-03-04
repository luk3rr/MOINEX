package org.moinex.factory

import org.moinex.model.Category
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import java.math.BigDecimal
import java.time.LocalDateTime

object WalletTransactionFactory {
    fun create(
        id: Int? = null,
        date: LocalDateTime = LocalDateTime.now(),
        status: WalletTransactionStatus = WalletTransactionStatus.PENDING,
        description: String? = null,
        includeInAnalysis: Boolean = true,
        wallet: Wallet = WalletFactory.create(),
        category: Category = CategoryFactory.create(),
        type: WalletTransactionType = WalletTransactionType.EXPENSE,
        amount: BigDecimal = BigDecimal("100.00"),
    ): WalletTransaction =
        WalletTransaction(
            id = id,
            date = date,
            status = status,
            description = description,
            includeInAnalysis = includeInAnalysis,
            wallet = wallet,
            category = category,
            type = type,
            amount = amount,
        )
}
