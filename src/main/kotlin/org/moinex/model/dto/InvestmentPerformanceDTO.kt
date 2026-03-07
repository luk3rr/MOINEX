/*
 * Filename: InvestmentPerformanceDTO.kt
 * Created on: February 17, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model.dto

import java.math.BigDecimal
import java.time.YearMonth

data class InvestmentPerformanceDTO(
    val monthlyInvested: Map<YearMonth, BigDecimal>,
    val portfolioValues: Map<YearMonth, BigDecimal>,
    val accumulatedGains: Map<YearMonth, BigDecimal>,
    val monthlyGains: Map<YearMonth, BigDecimal>,
)
