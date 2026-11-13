/*
 * Filename: NaturalKeys.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.spec

import org.moinex.common.extension.toRounded
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object NaturalKeys {
    private const val SEPARATOR = "\u001F"

    private const val CELL_SEPARATOR = "\u241F"

    /**
     * A key as text that can travel in a spreadsheet cell, where control characters are not allowed.
     */
    fun toCell(key: String): String = key.replace(SEPARATOR, CELL_SEPARATOR)

    fun fromCell(cell: String): String = cell.replace(CELL_SEPARATOR, SEPARATOR)

    fun of(vararg parts: Any?): String = parts.joinToString(SEPARATOR) { normalize(it) }

    private fun normalize(part: Any?): String =
        when (part) {
            null -> ""
            is BigDecimal -> part.toRounded().toPlainString()
            is LocalDateTime -> part.truncatedTo(ChronoUnit.SECONDS).toString()
            is Enum<*> -> part.name
            else -> part.toString()
        }
}
