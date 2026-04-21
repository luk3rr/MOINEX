/*
 * Filename: CreditCardDebtRowDTO.kt
 * Created on: April 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.moinex.model.creditcard.CreditCardPayment

sealed class CreditCardDebtRowDTO {
    data class Materialized(
        val payment: CreditCardPayment,
    ) : CreditCardDebtRowDTO()

    data class Projected(
        val occurrence: RecurringCreditCardDebtOccurrenceDTO,
    ) : CreditCardDebtRowDTO()
}
