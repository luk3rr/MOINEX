package org.moinex.model.dto

import org.moinex.model.enums.InterestIndex

data class SyncMarketIndicatorResultDTO(
    val indicator: InterestIndex,
    val synced: Boolean,
)
