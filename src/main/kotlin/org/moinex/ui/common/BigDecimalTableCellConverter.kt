/*
 * Filename: BigDecimalTableCellConverter.kt
 * Created on: September 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common

import javafx.util.StringConverter
import java.math.BigDecimal

/** Converts a table cell's free text to [BigDecimal], falling back to zero on invalid input. */
fun bigDecimalTableConverter(): StringConverter<BigDecimal> =
    object : StringConverter<BigDecimal>() {
        override fun toString(value: BigDecimal?): String = value?.toPlainString().orEmpty()

        override fun fromString(text: String?): BigDecimal =
            runCatching { BigDecimal(text) }.getOrDefault(BigDecimal.ZERO)
    }
