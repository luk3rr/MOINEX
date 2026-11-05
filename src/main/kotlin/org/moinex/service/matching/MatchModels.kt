/*
 * Filename: MatchModels.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.matching

import java.math.BigDecimal
import java.time.LocalDate

enum class MatchTargetType {
    WALLET_TRANSACTION,
    CREDIT_CARD_DEBT,
    TRANSFER,
}

enum class MatchStatus {
    PENDING,
    CONFIRMED,
}

/**
 * One statement line (or a typed entry) to look up among the records already stored.
 * [amount] is always positive; for an installment purchase it is the value of one installment.
 * [isIncome] tells the direction of a wallet line (null ignores the direction).
 * [description] never decides a match, it only orders equally good candidates.
 */
data class MatchQuery(
    val amount: BigDecimal,
    val date: LocalDate,
    val walletId: Int? = null,
    val creditCardId: Int? = null,
    val isIncome: Boolean? = null,
    val description: String? = null,
)

/**
 * A stored record that [MatchQuery] plausibly refers to. [score] is in 0.0..1.0 and [reasons] are already
 * translated for the user's language.
 */
data class Match(
    val targetType: MatchTargetType,
    val targetId: Int,
    val status: MatchStatus,
    val score: Double,
    val reasons: List<String>,
    val existingDescription: String,
    val date: LocalDate,
    val amount: BigDecimal,
)
