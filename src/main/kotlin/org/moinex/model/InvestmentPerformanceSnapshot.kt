/*
 * Filename: InvestmentPerformanceSnapshot.kt (original filename: InvestmentPerformanceSnapshot.java)
 * Created on: February 17, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.moinex.common.extension.toRounded
import java.math.BigDecimal

@Entity
@Table(name = "investment_performance_snapshot")
class InvestmentPerformanceSnapshot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "month", nullable = false)
    var month: Int,
    @Column(name = "year", nullable = false)
    var year: Int,
    @Column(name = "invested_value", nullable = false)
    var investedValue: BigDecimal,
    @Column(name = "portfolio_value", nullable = false)
    var portfolioValue: BigDecimal,
    @Column(name = "accumulated_capital_gains", nullable = false)
    var accumulatedCapitalGains: BigDecimal,
    @Column(name = "monthly_capital_gains", nullable = false)
    var monthlyCapitalGains: BigDecimal,
    @Column(name = "calculated_at", nullable = false)
    var calculatedAt: String,
) {
    init {
        investedValue = investedValue.toRounded()
        portfolioValue = portfolioValue.toRounded()
        accumulatedCapitalGains = accumulatedCapitalGains.toRounded()
        monthlyCapitalGains = monthlyCapitalGains.toRounded()

        require(month in 1..12) {
            "Month must be between 1 and 12"
        }
        require(year > 0) {
            "Year must be positive"
        }
    }

    override fun toString(): String = "InvestmentPerformanceSnapshot [month=$month, year=$year]"
}
