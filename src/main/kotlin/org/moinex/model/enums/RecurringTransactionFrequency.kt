/*
 * Filename: RecurringTransactionFrequency.kt (original filename: RecurringTransactionFrequency.java)
 * Created on: November 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 28/02/2026
 */

package org.moinex.model.enums

import java.time.temporal.ChronoUnit

enum class RecurringTransactionFrequency(
    val chronoUnit: ChronoUnit,
) {
    DAILY(ChronoUnit.DAYS),
    WEEKLY(ChronoUnit.WEEKS),
    MONTHLY(ChronoUnit.MONTHS),
    YEARLY(ChronoUnit.YEARS),
}
