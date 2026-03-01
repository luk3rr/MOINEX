package org.moinex.factory

import org.moinex.model.creditcard.CreditCardOperator

object CreditCardOperatorFactory {
    fun create(
        id: Int? = null,
        name: String = "Visa",
        icon: String? = null,
    ): CreditCardOperator =
        CreditCardOperator(
            id = id,
            name = name,
            icon = icon,
        )
}
