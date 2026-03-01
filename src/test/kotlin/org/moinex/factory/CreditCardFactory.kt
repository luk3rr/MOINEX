package org.moinex.factory

import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.CreditCardOperator
import org.moinex.model.wallettransaction.Wallet
import java.math.BigDecimal

object CreditCardFactory {
    fun create(
        id: Int? = null,
        name: String = "Test Card",
        billingDueDay: Int = 15,
        closingDay: Int = 5,
        maxDebt: BigDecimal = BigDecimal("10000.00"),
        lastFourDigits: String? = "1234",
        operator: CreditCardOperator = CreditCardOperatorFactory.create(),
        defaultBillingWallet: Any? = null,
        availableRebate: BigDecimal = BigDecimal.ZERO,
        isArchived: Boolean = false,
    ): CreditCard {
        val creditCard =
            CreditCard(
                id = id,
                name = name,
                billingDueDay = billingDueDay,
                closingDay = closingDay,
                maxDebt = maxDebt,
                lastFourDigits = lastFourDigits,
                operator = operator,
                defaultBillingWallet = defaultBillingWallet as? Wallet,
                availableRebate = availableRebate,
                isArchived = isArchived,
            )
        return creditCard
    }
}
