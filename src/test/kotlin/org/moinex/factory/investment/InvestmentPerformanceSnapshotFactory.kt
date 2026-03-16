package org.moinex.factory.investment

import org.moinex.model.investment.InvestmentPerformanceSnapshot
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

object InvestmentPerformanceSnapshotFactory {
    fun create(
        id: Int? = null,
        referenceMonth: YearMonth = YearMonth.now(),
        investedValue: BigDecimal = BigDecimal("1000.00"),
        portfolioValue: BigDecimal = BigDecimal("1100.00"),
        accumulatedCapitalGains: BigDecimal = BigDecimal("100.00"),
        monthlyCapitalGains: BigDecimal = BigDecimal("50.00"),
        calculatedAt: LocalDateTime = LocalDateTime.now(),
    ): InvestmentPerformanceSnapshot =
        InvestmentPerformanceSnapshot(
            id = id,
            referenceMonth = referenceMonth,
            investedValue = investedValue,
            portfolioValue = portfolioValue,
            accumulatedCapitalGains = accumulatedCapitalGains,
            monthlyCapitalGains = monthlyCapitalGains,
            calculatedAt = calculatedAt,
        )
}
