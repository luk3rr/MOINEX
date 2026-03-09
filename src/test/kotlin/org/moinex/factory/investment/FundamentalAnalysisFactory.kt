package org.moinex.factory.investment

import org.moinex.model.enums.PeriodType
import org.moinex.model.investment.FundamentalAnalysis
import org.moinex.model.investment.Ticker
import java.time.LocalDateTime

object FundamentalAnalysisFactory {
    fun create(
        id: Int? = null,
        ticker: Ticker = TickerFactory.create(),
        companyName: String? = "Test Company",
        sector: String? = "Technology",
        industry: String? = "Software",
        currency: String = "BRL",
        periodType: PeriodType = PeriodType.ANNUAL,
        dataJson: String = "{}",
        lastUpdate: LocalDateTime = LocalDateTime.now(),
        createdAt: LocalDateTime = LocalDateTime.now(),
    ): FundamentalAnalysis =
        FundamentalAnalysis(
            id = id,
            ticker = ticker,
            companyName = companyName,
            sector = sector,
            industry = industry,
            currency = currency,
            periodType = periodType,
            dataJson = dataJson,
            lastUpdate = lastUpdate,
            createdAt = createdAt,
        )
}
