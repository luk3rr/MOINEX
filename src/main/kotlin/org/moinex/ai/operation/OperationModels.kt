/*
 * Filename: OperationModels.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation

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

/** Short lines shown on a review card about what the operation will change (balance after, invoice total...). */
data class Preview(
    val lines: List<String>,
)

/** Values handed to and returned by the app's own dialog when a proposal is edited in draft mode. */
data class DraftValues(
    val operationName: String,
    val fields: Map<String, Any?>,
)
