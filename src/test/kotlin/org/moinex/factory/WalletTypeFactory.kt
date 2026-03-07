package org.moinex.factory

import org.moinex.model.wallettransaction.WalletType

object WalletTypeFactory {
    fun create(
        id: Int? = null,
        name: String = "Wallet Type",
        icon: String? = null,
    ): WalletType = WalletType(id, name, icon)
}
