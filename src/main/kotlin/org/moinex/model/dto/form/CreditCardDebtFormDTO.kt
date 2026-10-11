/*
 * Filename: CreditCardDebtFormDTO.kt
 * Created on: March 20, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto.form

import org.moinex.model.Category
import org.moinex.model.creditcard.CreditCard
import java.time.LocalDate

data class CreditCardDebtFormDTO(
    val creditCard: CreditCard?,
    val category: Category?,
    val invoiceMonth: Int?,
    val invoiceYear: Int?,
    val purchaseDate: LocalDate?,
    val description: String,
    val valueStr: String,
    val installmentsStr: String,
) {
    fun isValid(): Boolean =
        creditCard != null &&
            category != null &&
            description.isNotEmpty() &&
            valueStr.isNotEmpty() &&
            invoiceMonth != null &&
            invoiceYear != null &&
            purchaseDate != null
}
