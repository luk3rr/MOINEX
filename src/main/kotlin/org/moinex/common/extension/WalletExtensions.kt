package org.moinex.common.extension

import org.moinex.config.BalanceCalculationConfig
import org.moinex.model.enums.BalanceType
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.RecurringTransaction
import org.moinex.model.wallettransaction.WalletTransaction
import java.time.LocalDate

fun WalletTransaction.isIncome(): Boolean = this.type == WalletTransactionType.INCOME

fun WalletTransaction.isExpense(): Boolean = this.type == WalletTransactionType.EXPENSE

fun WalletTransaction.isPending(): Boolean = this.status == WalletTransactionStatus.PENDING

fun WalletTransaction.isConfirmed(): Boolean = this.status == WalletTransactionStatus.CONFIRMED

fun RecurringTransaction.nextFrom(date: LocalDate): LocalDate = date.plus(1, this.frequency.chronoUnit)

fun BalanceCalculationConfig.isPositive(): Boolean = this.balanceType == BalanceType.POSITIVE

fun BalanceCalculationConfig.isNegative(): Boolean = this.balanceType == BalanceType.NEGATIVE
