package org.moinex.factory.investment

import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPurchase
import org.moinex.model.wallettransaction.WalletTransaction
import java.math.BigDecimal
import java.time.LocalDateTime

object TickerPurchaseFactory {
    fun create(
        id: Int? = null,
        ticker: Ticker = TickerFactory.create(id = 1),
        quantity: BigDecimal = BigDecimal("10"),
        unitPrice: BigDecimal = BigDecimal("100.00"),
        walletTransaction: WalletTransaction =
            WalletTransactionFactory.create(
                date = LocalDateTime.now(),
            ),
    ): TickerPurchase =
        TickerPurchase(
            id = id,
            ticker = ticker,
            quantity = quantity,
            unitPrice = unitPrice,
            walletTransaction = walletTransaction,
        )
}
