/*
 * Filename: TickerPriceInitializationStatsDTO.kt
 * Created on: March 14, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

data class TickerPriceInitializationStatsDTO(
    var backfillCount: Int = 0,
    var updateCount: Int = 0,
    var skipCount: Int = 0,
)
