/*
 * Filename: BondDetailDTO.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.moinex.model.investment.Bond
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Value of a bond at the end of a month.
 */
data class MonthBalanceDTO(
    val month: YearMonth,
    val value: BigDecimal,
)

/**
 * What the detail window of a bond shows: its position, the interest it holds and its estimated net, and its balance
 * month by month.
 */
data class BondDetailDTO(
    val bond: Bond,
    val invested: BigDecimal,
    val currentValue: BigDecimal,
    val accumulatedInterest: BigDecimal,
    val monthInterest: BigDecimal,
    val netInterest: BondRedemptionEstimateDTO,
    val monthlyBalance: List<MonthBalanceDTO>,
)
