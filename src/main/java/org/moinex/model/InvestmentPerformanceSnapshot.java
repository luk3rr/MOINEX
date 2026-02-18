/*
 * Filename: InvestmentPerformanceSnapshot.java
 * Created on: February 17, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "investment_performance_snapshot")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentPerformanceSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Integer id;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "invested_value", nullable = false)
    private BigDecimal investedValue;

    @Column(name = "portfolio_value", nullable = false)
    private BigDecimal portfolioValue;

    @Column(name = "accumulated_capital_gains", nullable = false)
    private BigDecimal accumulatedCapitalGains;

    @Column(name = "monthly_capital_gains", nullable = false)
    private BigDecimal monthlyCapitalGains;

    @Column(name = "calculated_at", nullable = false)
    private String calculatedAt;
}
