package org.moinex.factory

import org.moinex.model.Category
import org.moinex.model.wallettransaction.Transfer
import org.moinex.model.wallettransaction.Wallet
import java.math.BigDecimal
import java.time.LocalDateTime

object TransferFactory {
    fun create(
        id: Int? = null,
        senderWallet: Wallet = WalletFactory.create(id = 1),
        receiverWallet: Wallet = WalletFactory.create(id = 2),
        date: LocalDateTime = LocalDateTime.now(),
        amount: BigDecimal = BigDecimal("100.00"),
        description: String? = null,
        category: Category = CategoryFactory.create(),
    ): Transfer =
        Transfer(
            id = id,
            senderWallet = senderWallet,
            receiverWallet = receiverWallet,
            date = date,
            amount = amount,
            description = description,
            category = category,
        )
}
