package org.moinex.factory.investment

import org.moinex.factory.CategoryFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.model.Category
import org.moinex.model.dto.BondOperationWalletTransactionDTO
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.wallettransaction.Wallet
import java.time.LocalDateTime

object BondOperationWalletTransactionDTOFactory {
    fun create(
        wallet: Wallet = WalletFactory.create(),
        date: LocalDateTime = LocalDateTime.now(),
        category: Category = CategoryFactory.create(),
        description: String? = "Bond operation",
        status: WalletTransactionStatus = WalletTransactionStatus.PENDING,
        includeInAnalysis: Boolean = true,
    ): BondOperationWalletTransactionDTO =
        BondOperationWalletTransactionDTO(
            wallet = wallet,
            date = date,
            category = category,
            description = description,
            status = status,
            includeInAnalysis = includeInAnalysis,
        )
}
