package org.moinex.factory.creditcard

import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.model.creditcard.CreditCardPayment
import org.moinex.model.wallettransaction.Wallet
import java.math.BigDecimal
import java.time.LocalDateTime

object CreditCardPaymentFactory {
    fun create(
        id: Int? = null,
        wallet: Wallet? = null,
        creditCardDebt: CreditCardDebt? = null,
        amount: BigDecimal = BigDecimal("100.00"),
        rebateUsed: BigDecimal = BigDecimal.ZERO,
        installment: Int = 1,
        refunded: Boolean = false,
        date: LocalDateTime = LocalDateTime.now(),
    ): CreditCardPayment =
        CreditCardPayment(
            id = id,
            wallet = wallet,
            creditCardDebt = creditCardDebt ?: CreditCardDebtFactory.create(),
            amount = amount,
            rebateUsed = rebateUsed,
            installment = installment,
            refunded = refunded,
            date = date,
        )
}
