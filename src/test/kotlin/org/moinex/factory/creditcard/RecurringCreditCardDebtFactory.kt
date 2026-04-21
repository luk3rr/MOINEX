package org.moinex.factory.creditcard

import org.moinex.common.constant.Constants
import org.moinex.factory.CategoryFactory
import org.moinex.model.Category
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.RecurringCreditCardDebt
import org.moinex.model.enums.CreditCardRecurringFrequency
import org.moinex.model.enums.RecurringTransactionStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

object RecurringCreditCardDebtFactory {
    fun create(
        id: Int? = 1,
        creditCard: CreditCard = CreditCardFactory.create(id = 1),
        category: Category = CategoryFactory.create(id = 1),
        amount: BigDecimal = BigDecimal("50.00"),
        description: String? = "Test Recurring",
        dayOfMonth: Int = 10,
        frequency: CreditCardRecurringFrequency = CreditCardRecurringFrequency.MONTHLY,
        status: RecurringTransactionStatus = RecurringTransactionStatus.ACTIVE,
        startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
        endDate: LocalDate = Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE,
        nextInvoiceMonth: YearMonth = YearMonth.now(),
    ): RecurringCreditCardDebt =
        RecurringCreditCardDebt(
            id = id,
            creditCard = creditCard,
            category = category,
            amount = amount,
            description = description,
            dayOfMonth = dayOfMonth,
            frequency = frequency,
            status = status,
            startDate = startDate,
            endDate = endDate,
            nextInvoiceMonth = nextInvoiceMonth,
        )
}
