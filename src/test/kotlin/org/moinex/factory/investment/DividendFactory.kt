package org.moinex.factory.investment

import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.investment.Dividend
import org.moinex.model.investment.Ticker
import org.moinex.model.wallettransaction.WalletTransaction
import java.time.LocalDateTime

object DividendFactory {
    fun create(
        id: Int? = null,
        ticker: Ticker = TickerFactory.create(id = 1),
        walletTransaction: WalletTransaction =
            WalletTransactionFactory.create(
                date = LocalDateTime.now(),
            ),
    ): Dividend =
        Dividend(
            id = id,
            ticker = ticker,
            walletTransaction = walletTransaction,
        )
}
