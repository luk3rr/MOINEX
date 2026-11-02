/*
 * Filename: WalletTransactionContextDTO.kt
 * Created on: March 11, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.moinex.model.Category
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.wallettransaction.Wallet
import java.time.LocalDateTime

data class WalletTransactionContextDTO(
    val wallet: Wallet,
    val date: LocalDateTime,
    val category: Category,
    val description: String?,
    val status: WalletTransactionStatus,
    val includeInAnalysis: Boolean,
)
