package org.moinex.factory.investment

import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPriceHistory
import java.math.BigDecimal
import java.time.LocalDate

object TickerPriceHistoryFactory {
    fun create(
        id: Int? = null,
        ticker: Ticker = TickerFactory.create(id = 1),
        priceDate: LocalDate = LocalDate.now(),
        closingPrice: BigDecimal = BigDecimal("100.00"),
        isMonthEnd: Boolean = false,
    ): TickerPriceHistory =
        TickerPriceHistory(
            id = id,
            ticker = ticker,
            priceDate = priceDate,
            closingPrice = closingPrice,
            isMonthEnd = isMonthEnd,
        )
}
