/*
 * Filename: TickerPerformanceDTO.java
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dto;

import java.math.BigDecimal;

/**
 * DTO for ticker performance information
 */
public record TickerPerformanceDTO(
        String name,
        String symbol,
        BigDecimal profitLossPercentage,
        BigDecimal profitLossValue,
        BigDecimal currentValue) {

    public boolean isPositive() {
        return profitLossPercentage.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return profitLossPercentage.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isNeutral() {
        return profitLossPercentage.compareTo(BigDecimal.ZERO) == 0;
    }

    public String getSign() {
        if (isNeutral()) return "";
        return isPositive() ? "+ " : "- ";
    }
}
