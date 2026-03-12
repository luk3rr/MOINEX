package org.moinex.model.dto

import org.moinex.model.Category
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.wallettransaction.Wallet
import java.time.LocalDateTime

data class BondOperationWalletTransactionDTO(
    val wallet: Wallet,
    val date: LocalDateTime,
    val category: Category,
    val description: String?,
    val status: WalletTransactionStatus,
    val includeInAnalysis: Boolean,
)
