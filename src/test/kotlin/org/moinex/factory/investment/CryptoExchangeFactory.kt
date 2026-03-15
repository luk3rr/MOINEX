package org.moinex.factory.investment

import org.moinex.model.enums.AssetType
import org.moinex.model.investment.CryptoExchange
import org.moinex.model.investment.Ticker
import java.math.BigDecimal
import java.time.LocalDateTime

object CryptoExchangeFactory {
    fun create(
        id: Int? = null,
        soldCrypto: Ticker =
            TickerFactory.create(
                id = 1,
                symbol = "BTC",
                type = AssetType.CRYPTOCURRENCY,
            ),
        receivedCrypto: Ticker =
            TickerFactory.create(
                id = 2,
                symbol = "ETH",
                type = AssetType.CRYPTOCURRENCY,
            ),
        soldQuantity: BigDecimal = BigDecimal("1.0"),
        receivedQuantity: BigDecimal = BigDecimal("15.0"),
        date: LocalDateTime = LocalDateTime.now(),
        description: String? = null,
    ): CryptoExchange =
        CryptoExchange(
            id = id,
            soldCrypto = soldCrypto,
            receivedCrypto = receivedCrypto,
            soldQuantity = soldQuantity,
            receivedQuantity = receivedQuantity,
            date = date,
            description = description,
        )
}
