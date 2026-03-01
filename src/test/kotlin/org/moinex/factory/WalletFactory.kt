package org.moinex.factory

import org.moinex.model.wallettransaction.Wallet
import java.math.BigDecimal

object WalletFactory {
    fun create(
        id: Int? = null,
        name: String = "Main Wallet",
        balance: BigDecimal = BigDecimal("5000.00"),
    ): Wallet = Wallet(id, name, balance)
}
