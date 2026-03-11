package org.moinex.factory.investment

import org.moinex.model.enums.InterestIndex
import org.moinex.model.investment.MarketIndicatorHistory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

object MarketIndicatorHistoryFactory {
    fun create(
        id: Int? = null,
        indicatorType: InterestIndex = InterestIndex.CDI,
        referenceDate: LocalDate = LocalDate.now(),
        rateValue: BigDecimal = BigDecimal("10.50"),
        createdAt: LocalDateTime = LocalDateTime.now(),
    ): MarketIndicatorHistory =
        MarketIndicatorHistory(
            id,
            indicatorType,
            referenceDate,
            rateValue,
            createdAt,
        )
}
