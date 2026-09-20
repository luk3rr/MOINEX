/*
 * Filename: ExtractedTransactionDTO.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto.ai

import org.moinex.model.enums.WalletTransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

enum class TransactionKind {
    WALLET,
    CREDIT_CARD,
    CREDIT_CARD_PAYMENT,
}

/**
 * [amount] is always positive; direction is conveyed by [movementType] (only meaningful when
 * [kind] is [TransactionKind.WALLET] - a credit card entry is always a purchase/debt).
 * [invoiceMonth] is only meaningful when [kind] is [TransactionKind.CREDIT_CARD_PAYMENT]; falls
 * back to [date]'s month when the model can't determine the billing period.
 */
data class ExtractedTransactionDTO(
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val kind: TransactionKind,
    val movementType: WalletTransactionType = WalletTransactionType.EXPENSE,
    val invoiceMonth: YearMonth? = null,
    val suggestedWalletId: Int?,
    val suggestedCreditCardId: Int?,
    val suggestedCategoryId: Int?,
    val suggestedCategoryName: String,
    val confidence: Double,
    val possibleDuplicate: Boolean = false,
)
