package org.moinex.factory

import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletType
import java.math.BigDecimal

object WalletTypeFactory {
    fun create(
        id: Int? = null,
        name: String = "Wallet Type",
        icon: String? = null,
    ): WalletType = WalletType(id,  name, icon)
}
