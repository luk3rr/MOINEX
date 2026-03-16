package org.moinex.model.investment

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.moinex.common.converter.LocalDateTimeStringConverter
import org.moinex.common.converter.YearMonthStringConverter
import org.moinex.common.extension.toRounded
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

@Entity
@Table(name = "investment_performance_snapshot")
class InvestmentPerformanceSnapshot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Convert(converter = YearMonthStringConverter::class)
    @Column(name = "reference_month", nullable = false)
    var referenceMonth: YearMonth,
    @Column(name = "invested_value", nullable = false)
    var investedValue: BigDecimal,
    @Column(name = "portfolio_value", nullable = false)
    var portfolioValue: BigDecimal,
    @Column(name = "accumulated_capital_gains", nullable = false)
    var accumulatedCapitalGains: BigDecimal,
    @Column(name = "monthly_capital_gains", nullable = false)
    var monthlyCapitalGains: BigDecimal,
    @Convert(converter = LocalDateTimeStringConverter::class)
    @Column(name = "calculated_at", nullable = false)
    var calculatedAt: LocalDateTime,
) {
    init {
        investedValue = investedValue.toRounded()
        portfolioValue = portfolioValue.toRounded()
        accumulatedCapitalGains = accumulatedCapitalGains.toRounded()
        monthlyCapitalGains = monthlyCapitalGains.toRounded()
    }

    override fun toString(): String = "InvestmentPerformanceSnapshot [referenceMonth=$referenceMonth, calculatedAt=$calculatedAt]"
}
