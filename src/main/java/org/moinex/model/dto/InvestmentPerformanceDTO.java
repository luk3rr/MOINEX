/*
 * Filename: InvestmentPerformanceDTO.java
 * Created on: February 17, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

/**
 * Data Transfer Object to hold investment performance metrics
 */
public record InvestmentPerformanceDTO(
        Map<YearMonth, BigDecimal> monthlyInvested,
        Map<YearMonth, BigDecimal> portfolioValues,
        Map<YearMonth, BigDecimal> accumulatedGains,
        Map<YearMonth, BigDecimal> monthlyGains) {}
