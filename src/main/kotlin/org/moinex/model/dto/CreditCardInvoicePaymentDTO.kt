package org.moinex.model.dto

import java.math.BigDecimal
import java.time.YearMonth

data class CreditCardInvoicePaymentDTO(
    val creditCardId: Int,
    val billingWalletId: Int,
    val invoiceDate: YearMonth,
    val amount: BigDecimal,
    val rebate: BigDecimal = BigDecimal.ZERO,
) {
    init {
        require(amount > BigDecimal.ZERO) { "Payment amount must be positive" }
        require(rebate >= BigDecimal.ZERO) { "Rebate cannot be negative" }
    }
}
