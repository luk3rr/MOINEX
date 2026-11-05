/*
 * Filename: DraftSpec.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation

enum class DraftFieldType {
    TEXT,
    DECIMAL,
    DATE,
    INTEGER,
    CHOICE,
}

/** Which list a [DraftFieldType.CHOICE] field picks from. */
enum class DraftChoices {
    WALLETS,
    CREDIT_CARDS,
    CATEGORIES,
    TICKERS,
    BONDS,
    MOVEMENT_TYPES,
}

/**
 * One editable field of a proposal in the fallback editor. A choice over wallets, cards, tickers and bonds holds the
 * record id; a choice over categories holds the name; movement types hold `income` or `expense`.
 */
data class DraftField(
    val key: String,
    val labelKey: String,
    val type: DraftFieldType,
    val choices: DraftChoices? = null,
    val required: Boolean = true,
)
