/*
 * Filename: BondYieldText.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import java.math.BigDecimal
import java.util.Locale

/**
 * The text of the accumulated yield of a bond in the table: the amount in reais and, when there is something invested,
 * the percentage over it with its sign, for example "+ R$ 150,20 (+3,76%)".
 */
object BondYieldText {
    fun format(
        amount: BigDecimal,
        percentage: BigDecimal?,
        formatAmount: (BigDecimal) -> String,
        locale: Locale,
    ): String {
        val amountText = formatAmount(amount)

        if (percentage == null) return amountText

        val sign = if (percentage.signum() > 0) "+" else ""

        return "$amountText ($sign${String.format(locale, "%.2f", percentage)}%)"
    }
}
