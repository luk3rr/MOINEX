/*
 * Filename: CalendarEventType.kt (original filename: CalendarEventType.java)
 * Created on: March  1, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 28/02/2026
 */

package org.moinex.model.enums

enum class CalendarEventType(
    val description: String,
    val colorHex: String,
) {
    CREDIT_CARD_STATEMENT_CLOSING("Credit Card Statement Closing", "#FF9800"),
    CREDIT_CARD_DUE_DATE("Credit Card Due Date", "#F44336"),
    DEBT_PAYMENT_DUE_DATE("Debt Payment Due Date", "#3F51B5"),
    INCOME_RECEIPT_DATE("Income Receipt Date", "#4CAF50"),
}
