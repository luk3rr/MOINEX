package org.moinex.factory.investment

import org.moinex.model.investment.BrazilianMarketIndicators
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

object BrazilianMarketIndicatorsFactory {
    fun create(
        id: Int? = null,
        selicTarget: BigDecimal? = BigDecimal("13.75"),
        ipcaLastMonth: BigDecimal? = BigDecimal("0.42"),
        ipcaLastMonthReference: YearMonth? = YearMonth.now(),
        ipca12Months: BigDecimal? = BigDecimal("4.50"),
        lastUpdate: LocalDateTime? = LocalDateTime.now(),
    ): BrazilianMarketIndicators =
        BrazilianMarketIndicators(
            id = id,
            selicTarget = selicTarget,
            ipcaLastMonth = ipcaLastMonth,
            ipcaLastMonthReference = ipcaLastMonthReference,
            ipca12Months = ipca12Months,
            lastUpdate = lastUpdate,
        )
}
