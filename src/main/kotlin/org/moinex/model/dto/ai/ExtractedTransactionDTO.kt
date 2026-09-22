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
 * [existingTransactionId], when set (only meaningful for [TransactionKind.WALLET]), identifies an
 * already-recorded PENDING wallet transaction that this proposal confirms/updates instead of
 * creating a new one - the model sets it after spotting a match via `get_transactions_in_period`.
 * [installments]/[installmentNumber] are only meaningful for [TransactionKind.CREDIT_CARD].
 * [installments] is how many installments will actually be scheduled from here on - already
 * adjusted by [org.moinex.ai.tools.ProposeTransactionTool] to the *remaining* count when
 * [installmentNumber] is greater than 1 (a statement line like "3/10" continuing a plan that
 * predates the app), so the review screen and persistence never need to redo that math.
 * [installmentNumber] is the original position in the plan as read off the statement (1 for a
 * purchase seen for the first time) - kept for the review screen's notes/duplicate detection, not
 * used for persistence itself.
 */
data class ExtractedTransactionDTO(
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val kind: TransactionKind,
    val movementType: WalletTransactionType = WalletTransactionType.EXPENSE,
    val invoiceMonth: YearMonth? = null,
    val existingTransactionId: Int? = null,
    val installments: Int = 1,
    val installmentNumber: Int = 1,
    val suggestedWalletId: Int?,
    val suggestedCreditCardId: Int?,
    val suggestedCategoryId: Int?,
    val suggestedCategoryName: String,
    val confidence: Double,
    val possibleDuplicate: Boolean = false,
)
