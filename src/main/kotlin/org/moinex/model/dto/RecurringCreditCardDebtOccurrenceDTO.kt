/*
 * Filename: RecurringCreditCardDebtOccurrenceDTO.kt
 * Created on: April 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.moinex.model.creditcard.RecurringCreditCardDebt
import java.math.BigDecimal
import java.time.YearMonth

data class RecurringCreditCardDebtOccurrenceDTO(
    val recurringDebt: RecurringCreditCardDebt,
    val invoiceMonth: YearMonth,
    val amount: BigDecimal,
)
