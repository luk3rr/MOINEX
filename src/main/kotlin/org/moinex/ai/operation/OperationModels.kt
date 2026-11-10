/*
 * Filename: OperationModels.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation

import java.math.BigDecimal
import java.time.LocalDate

enum class OperationDomain {
    TRANSACTIONS,
    CREDIT_CARD,
    INVESTMENTS,
    PLANNING,
    MAINTENANCE,
}

/** There is deliberately no level for deleting: the assistant cannot remove anything from the app. */
enum class OperationRisk {
    LOW,
    MEDIUM,
}

data class ValidationResult(
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
) {
    val isValid: Boolean get() = errors.isEmpty()

    companion object {
        val OK = ValidationResult()

        fun error(message: String) = ValidationResult(errors = listOf(message))
    }
}

sealed interface ApplyResult {
    data class Success(
        val ids: List<Int>,
        val summary: String,
    ) : ApplyResult

    data class Failure(
        val message: String,
    ) : ApplyResult
}

/** A value of a review card that the screen formats for the user: money with its currency, dates, translated text. */
sealed interface CardValue {
    data class Text(
        val text: String,
    ) : CardValue

    data class Money(
        val amount: BigDecimal,
    ) : CardValue

    data class Day(
        val date: LocalDate,
    ) : CardValue

    data class Translated(
        val key: String,
        val args: List<Any> = emptyList(),
    ) : CardValue
}

data class CardFact(
    val value: CardValue,
    val label: String? = null,
)

fun cardMoney(
    amount: BigDecimal,
    label: String? = null,
) = CardFact(CardValue.Money(amount), label)

fun cardDay(date: LocalDate) = CardFact(CardValue.Day(date))

fun cardText(
    text: String,
    label: String? = null,
) = CardFact(CardValue.Text(text), label)

fun cardTranslated(
    key: String,
    vararg args: Any,
) = CardFact(CardValue.Translated(key, args.toList()))

/** What a review card shows: [title] on the main line and the [facts] (amount, date...) below it in a muted style. */
data class CardInfo(
    val title: String,
    val facts: List<CardFact>,
)

/** Short lines shown on a review card about what the operation will change (balance after, invoice total...). */
data class Preview(
    val lines: List<String>,
    val rows: List<List<CardFact>> = emptyList(),
)

enum class TagTone {
    POSITIVE,
    NEGATIVE,
    NEUTRAL,
}

/** A short classification of a proposal shown as a tag on its review card, such as an expense or an income. */
data class OperationTag(
    val label: String,
    val tone: TagTone,
)

/** Values handed to and returned by the app's own dialog when a proposal is edited in draft mode. */
data class DraftValues(
    val operationName: String,
    val fields: Map<String, Any?>,
)
