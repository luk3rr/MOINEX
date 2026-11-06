/*
 * Filename: StockHistoryRequest.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import java.time.LocalDate

/**
 * One symbol of a batch price history request. A null [currency] makes the script read it from Yahoo.
 */
data class StockHistoryRequest(
    val symbol: String,
    val start: LocalDate,
    val end: LocalDate,
    val currency: String? = null,
    val dates: Set<LocalDate>? = null,
)
