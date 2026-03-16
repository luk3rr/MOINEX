/*
 * Filename: NetWorthDataPointDTO.kt
 * Created on: March 16, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import java.math.BigDecimal
import java.time.YearMonth

data class NetWorthDataPointDTO(
    val period: YearMonth,
    val assets: BigDecimal,
    val liabilities: BigDecimal,
    val netWorth: BigDecimal,
)
