package org.moinex.model.dto.form

import org.moinex.model.creditcard.CreditCardOperator
import org.moinex.model.wallettransaction.Wallet

data class CreditCardFormDTO(
    val name: String,
    val limitStr: String,
    val lastFourDigits: String,
    val closingDayStr: String?,
    val dueDayStr: String?,
    val operator: CreditCardOperator?,
    val defaultBillingWallet: Wallet?,
) {
    fun isValid(): Boolean =
        name.isNotEmpty() &&
            limitStr.isNotEmpty() &&
            lastFourDigits.isNotEmpty() &&
            operator != null &&
            closingDayStr != null &&
            dueDayStr != null
}
