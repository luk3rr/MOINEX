package org.moinex.factory

import org.moinex.model.Category
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.CreditCardDebt
import java.math.BigDecimal
import java.time.LocalDateTime

object CreditCardDebtFactory {
    fun create(
        id: Int? = null,
        category: Category? = null,
        installments: Int = 1,
        creditCard: CreditCard? = null,
        date: LocalDateTime = LocalDateTime.now(),
        amount: BigDecimal = BigDecimal("100.00"),
        description: String? = "Test Debt",
    ): CreditCardDebt =
        CreditCardDebt(
            id = id,
            category = category ?: CategoryFactory.create(),
            installments = installments,
            creditCard = creditCard ?: CreditCardFactory.create(),
            date = date,
            amount = amount,
            description = description,
        )
}
