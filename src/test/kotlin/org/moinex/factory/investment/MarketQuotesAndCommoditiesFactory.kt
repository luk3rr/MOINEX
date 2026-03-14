package org.moinex.factory.investment

import org.moinex.model.investment.MarketQuotesAndCommodities
import java.math.BigDecimal
import java.time.LocalDateTime

object MarketQuotesAndCommoditiesFactory {
    fun create(
        id: Int? = null,
        dollar: BigDecimal? = BigDecimal("5.25"),
        euro: BigDecimal? = BigDecimal("5.85"),
        ibovespa: BigDecimal? = BigDecimal("125000.50"),
        bitcoin: BigDecimal? = BigDecimal("65000.00"),
        ethereum: BigDecimal? = BigDecimal("3500.00"),
        gold: BigDecimal? = BigDecimal("2050.75"),
        soybean: BigDecimal? = BigDecimal("1450.25"),
        coffee: BigDecimal? = BigDecimal("220.50"),
        wheat: BigDecimal? = BigDecimal("650.00"),
        oilBrent: BigDecimal? = BigDecimal("85.50"),
        lastUpdate: LocalDateTime? = LocalDateTime.now(),
    ): MarketQuotesAndCommodities =
        MarketQuotesAndCommodities(
            id = id,
            dollar = dollar,
            euro = euro,
            ibovespa = ibovespa,
            bitcoin = bitcoin,
            ethereum = ethereum,
            gold = gold,
            soybean = soybean,
            coffee = coffee,
            wheat = wheat,
            oilBrent = oilBrent,
            lastUpdate = lastUpdate,
        )
}
