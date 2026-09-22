/*
 * Filename: ExtractedInvestmentOperationDTO.kt
 * Created on: September 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto.ai

import java.math.BigDecimal
import java.time.LocalDate

enum class InvestmentOperationKind {
    STOCK_PURCHASE,
    STOCK_SALE,
    DIVIDEND,
    BOND_APPLICATION,
    BOND_REDEMPTION,
}

/**
 * A candidate investment operation extracted/proposed by the assistant. Unlike a regular
 * income/expense, this is never persisted as a plain [org.moinex.model.wallettransaction.WalletTransaction]
 * by itself - it always goes through the dedicated ticker/bond domain services (which create their
 * own wallet transaction internally), so the app's invested quantity/average cost/portfolio
 * tracking stays consistent.
 *
 * [quantity]/[unitPrice] are only meaningful for [InvestmentOperationKind.STOCK_PURCHASE],
 * [InvestmentOperationKind.STOCK_SALE], [InvestmentOperationKind.BOND_APPLICATION] and
 * [InvestmentOperationKind.BOND_REDEMPTION] - zero otherwise.
 * [fees]/[taxes] are only meaningful for [InvestmentOperationKind.BOND_APPLICATION] and
 * [InvestmentOperationKind.BOND_REDEMPTION] - a ticker purchase/sale has no such fields in this app.
 * [dividendAmount] is only meaningful for [InvestmentOperationKind.DIVIDEND] - the total amount
 * received, since a dividend has no quantity/unit price breakdown.
 * [suggestedTickerId] is only set for ticker-based kinds (purchase/sale/dividend);
 * [suggestedBondId] only for bond-based kinds (application/redemption) - the two asset universes
 * are matched independently and never mixed.
 */
data class ExtractedInvestmentOperationDTO(
    val date: LocalDate,
    val kind: InvestmentOperationKind,
    val description: String,
    val quantity: BigDecimal = BigDecimal.ZERO,
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val fees: BigDecimal = BigDecimal.ZERO,
    val taxes: BigDecimal = BigDecimal.ZERO,
    val dividendAmount: BigDecimal = BigDecimal.ZERO,
    val suggestedTickerId: Int? = null,
    val suggestedBondId: Int? = null,
    val suggestedAssetLabel: String,
    val suggestedWalletId: Int?,
    val suggestedCategoryId: Int?,
    val suggestedCategoryName: String,
    val confidence: Double,
)
