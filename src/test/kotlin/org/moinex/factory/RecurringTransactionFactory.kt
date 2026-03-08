package org.moinex.factory

import org.moinex.model.Category
import org.moinex.model.enums.RecurringTransactionFrequency
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.RecurringTransaction
import org.moinex.model.wallettransaction.Wallet
import java.math.BigDecimal
import java.time.LocalDate

object RecurringTransactionFactory {
    fun create(
        id: Int? = null,
        startDate: LocalDate = LocalDate.now(),
        endDate: LocalDate = LocalDate.now().plusMonths(12),
        nextDueDate: LocalDate = LocalDate.now(),
        frequency: RecurringTransactionFrequency = RecurringTransactionFrequency.MONTHLY,
        status: RecurringTransactionStatus = RecurringTransactionStatus.ACTIVE,
        includeInNetWorth: Boolean = false,
        description: String? = null,
        wallet: Wallet = WalletFactory.create(),
        category: Category = CategoryFactory.create(),
        type: WalletTransactionType = WalletTransactionType.EXPENSE,
        amount: BigDecimal = BigDecimal("100.00"),
        includeInAnalysis: Boolean = true,
    ): RecurringTransaction =
        RecurringTransaction(
            id = id,
            startDate = startDate,
            endDate = endDate,
            nextDueDate = nextDueDate,
            frequency = frequency,
            status = status,
            includeInNetWorth = includeInNetWorth,
            description = description,
            wallet = wallet,
            category = category,
            type = type,
            amount = amount,
            includeInAnalysis = includeInAnalysis,
        )
}
