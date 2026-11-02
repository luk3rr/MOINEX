/*
 * Filename: BalanceDataDTO.kt
 * Created on: March 16, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import java.math.BigDecimal

data class BalanceDataDTO(
    val balance: BigDecimal,
    val pendingExpenses: BigDecimal,
    val pendingIncomes: BigDecimal,
    val totalWallets: Long,
)
