package org.moinex.factory

import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletType
import java.math.BigDecimal

object WalletFactory {
    fun create(
        id: Int? = null,
        name: String = "Main Wallet",
        balance: BigDecimal = BigDecimal("5000.00"),
        type: WalletType = WalletTypeFactory.create(1, "type"),
        masterWallet: Wallet? = null,
        isArchived: Boolean = false,
    ): Wallet = Wallet(id, type, name, balance, isArchived, masterWallet)
}
