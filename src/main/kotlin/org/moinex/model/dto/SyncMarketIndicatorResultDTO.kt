/*
 * Filename: SyncMarketIndicatorResultDTO.kt
 * Created on: March 11, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.moinex.model.enums.InterestIndex

data class SyncMarketIndicatorResultDTO(
    val indicator: InterestIndex,
    val synced: Boolean,
)
