/*
 * Filename: CreditCardRecurringFrequency.kt
 * Created on: April 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.enums

import java.time.temporal.ChronoUnit

enum class CreditCardRecurringFrequency(
    val chronoUnit: ChronoUnit,
) {
    MONTHLY(ChronoUnit.MONTHS),
    YEARLY(ChronoUnit.YEARS),
}
