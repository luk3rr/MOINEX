/*
 * Filename: Models.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.spec

import org.moinex.common.constant.TranslationKeys

enum class CellType { TEXT, INT, BOOLEAN, DECIMAL, DATE, DATETIME, ENUM }

data class Column(
    val name: String,
    val type: CellType,
    val required: Boolean = false,
    val enumClass: Class<out Enum<*>>? = null,
)

data class RawRow(
    val number: Int,
    val cells: Map<String, String?>,
    val formulaColumns: Set<String> = emptySet(),
)

data class ParsedRow(
    val number: Int,
    val values: Map<String, Any?>,
) {
    fun text(column: String): String? = values[column] as String?

    fun decimal(column: String): java.math.BigDecimal? = values[column] as java.math.BigDecimal?

    fun bool(column: String): Boolean? = values[column] as Boolean?

    fun date(column: String): java.time.LocalDate? = values[column] as java.time.LocalDate?

    fun dateTime(column: String): java.time.LocalDateTime? = values[column] as java.time.LocalDateTime?

    fun int(column: String): Int? = values[column] as Int?

    @Suppress("UNCHECKED_CAST")
    fun <E : Enum<E>> enum(column: String): E? = values[column] as E?
}

enum class Severity { ERROR, WARNING }

data class Problem(
    val entity: String,
    val row: Int,
    val column: String?,
    val code: String,
    val severity: Severity,
    val message: String = "",
) {
    val i18nKey: String get() = TranslationKeys.DATA_EXCHANGE_PROBLEM_PREFIX + code
}

object ProblemCodes {
    const val NOT_MOINEX_FILE = "E01"
    const val UNKNOWN_LAYOUT = "E02"
    const val MISSING_SHEET_OR_COLUMN = "E03"
    const val FILE_TOO_LARGE = "E04"
    const val INVALID_VALUE = "E05"
    const val UNRESOLVED_REFERENCE = "E06"
    const val FORMULA_CELL = "E07"
    const val DOMAIN_RULE = "E08"
    const val ADJUSTED_VALUE = "W01"
    const val UNKNOWN_COLUMN = "W02"
}

class DataExchangeException(
    val problem: Problem,
    cause: Throwable? = null,
) : RuntimeException("${problem.code}: ${problem.message}", cause)

fun interface ProgressSink {
    fun report(
        entityId: String,
        done: Long,
        total: Long,
    )

    companion object {
        val NONE = ProgressSink { _, _, _ -> }
    }
}
