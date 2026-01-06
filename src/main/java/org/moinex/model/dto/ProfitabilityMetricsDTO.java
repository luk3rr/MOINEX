/*
 * Filename: ProfitabilityMetricsDTO.java
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto;

import java.math.BigDecimal;

/**
 * DTO for profitability metrics
 */
public record ProfitabilityMetricsDTO(
        BigDecimal totalInvested,
        BigDecimal currentValue,
        BigDecimal profitLoss,
        BigDecimal returnPercentage,
        BigDecimal dividendYield,
        BigDecimal totalDividends) {

    public boolean isProfitable() {
        return profitLoss.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNeutral() {
        return profitLoss.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isLoss() {
        return profitLoss.compareTo(BigDecimal.ZERO) < 0;
    }
}
