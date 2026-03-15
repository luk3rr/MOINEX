package org.moinex.factory.investment

import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerSale
import org.moinex.model.wallettransaction.WalletTransaction
import java.math.BigDecimal
import java.time.LocalDateTime

object TickerSaleFactory {
    fun create(
        id: Int? = null,
        ticker: Ticker = TickerFactory.create(id = 1),
        quantity: BigDecimal = BigDecimal("5"),
        unitPrice: BigDecimal = BigDecimal("110.00"),
        averageCost: BigDecimal = BigDecimal("100.00"),
        walletTransaction: WalletTransaction =
            WalletTransactionFactory.create(
                date = LocalDateTime.now(),
            ),
    ): TickerSale =
        TickerSale(
            id = id,
            ticker = ticker,
            averageCost = averageCost,
            quantity = quantity,
            unitPrice = unitPrice,
            walletTransaction = walletTransaction,
        )
}
