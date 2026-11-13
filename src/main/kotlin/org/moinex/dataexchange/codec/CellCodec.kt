/*
 * Filename: CellCodec.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.codec

import org.moinex.dataexchange.spec.CellType
import org.moinex.dataexchange.spec.Column
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

object CellCodec {
    private val DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    private val INT_PATTERN = Regex("-?\\d+")
    private val DECIMAL_PATTERN = Regex("-?\\d+(\\.\\d+)?")
    private const val FORMULA_TRIGGERS = "=+-@\t\r"

    sealed interface Decoded {
        data class Value(
            val value: Any?,
        ) : Decoded

        data class Invalid(
            val reason: String,
        ) : Decoded
    }

    fun encode(
        value: Any?,
        column: Column,
    ): String? {
        if (value == null) return null
        return when (column.type) {
            CellType.TEXT -> value.toString()
            CellType.INT ->
                when (value) {
                    is Int, is Long, is Short, is Byte -> value.toString()
                    else -> throw IllegalArgumentException("Column ${column.name} expects an integer")
                }
            CellType.BOOLEAN -> (value as Boolean).toString()
            CellType.DECIMAL ->
                when (value) {
                    is BigDecimal -> value.toPlainString()
                    is Int, is Long -> BigDecimal.valueOf((value as Number).toLong()).toPlainString()
                    else -> throw IllegalArgumentException("Column ${column.name} expects a BigDecimal")
                }
            CellType.DATE -> (value as LocalDate).toString()
            CellType.DATETIME -> (value as LocalDateTime).truncatedTo(ChronoUnit.SECONDS).format(DATETIME_FORMAT)
            CellType.ENUM -> (value as Enum<*>).name
        }
    }

    fun decode(
        raw: String?,
        column: Column,
    ): Decoded {
        if (raw == null || raw.isEmpty()) return Decoded.Value(null)
        return try {
            when (column.type) {
                CellType.TEXT -> Decoded.Value(raw)
                CellType.INT ->
                    if (INT_PATTERN.matches(raw)) Decoded.Value(raw.toInt()) else Decoded.Invalid(raw)
                CellType.BOOLEAN ->
                    when (raw.lowercase()) {
                        "true", "1" -> Decoded.Value(true)
                        "false", "0" -> Decoded.Value(false)
                        else -> Decoded.Invalid(raw)
                    }
                CellType.DECIMAL ->
                    if (DECIMAL_PATTERN.matches(raw)) Decoded.Value(BigDecimal(raw)) else Decoded.Invalid(raw)
                CellType.DATE -> Decoded.Value(LocalDate.parse(raw))
                CellType.DATETIME -> Decoded.Value(LocalDateTime.parse(raw))
                CellType.ENUM -> decodeEnum(raw, column)
            }
        } catch (_: DateTimeParseException) {
            Decoded.Invalid(raw)
        } catch (_: NumberFormatException) {
            Decoded.Invalid(raw)
        }
    }

    private fun decodeEnum(
        raw: String,
        column: Column,
    ): Decoded {
        val constants = column.enumClass?.enumConstants ?: return Decoded.Invalid(raw)
        val match = constants.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        return if (match != null) Decoded.Value(match) else Decoded.Invalid(raw)
    }

    fun escapeFormula(text: String): String = if (needsEscape(text)) "'$text" else text

    fun unescapeFormula(text: String): String =
        if (text.startsWith("'") && text.length > 1 && (FORMULA_TRIGGERS + "'").contains(text[1])) {
            text.substring(1)
        } else {
            text
        }

    private fun needsEscape(text: String): Boolean {
        if (text.isEmpty()) return false
        val first = text[0]
        if (FORMULA_TRIGGERS.contains(first)) return true
        return first == '\'' && text.length > 1 && (FORMULA_TRIGGERS + "'").contains(text[1])
    }
}
