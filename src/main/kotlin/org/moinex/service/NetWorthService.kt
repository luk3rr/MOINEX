/*
 * Filename: NetWorthService.kt (original filenames: NetWorthCalculationService.java, NetWorthSnapshotService.java)
 * Created on: January 22, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 15/03/2026
 */

package org.moinex.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.moinex.common.extension.atEndOfDay
import org.moinex.common.extension.isAfterOrEqual
import org.moinex.common.extension.isBeforeOrEqual
import org.moinex.common.extension.isIncome
import org.moinex.common.extension.isNotZero
import org.moinex.common.extension.isOpenEnded
import org.moinex.common.extension.isPending
import org.moinex.common.extension.nextFrom
import org.moinex.config.BalanceCalculationConfig
import org.moinex.model.NetWorthSnapshot
import org.moinex.model.creditcard.CreditCardPayment
import org.moinex.model.dto.BondOperationCalculationDTO
import org.moinex.model.enums.OperationType
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.RecurringTransaction
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.repository.NetWorthSnapshotRepository
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.investment.BondInterestCalculationService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import org.moinex.util.Constants
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class NetWorthService(
    private val netWorthSnapshotRepository: NetWorthSnapshotRepository,
    private val walletService: WalletService,
    private val recurringTransactionService: RecurringTransactionService,
    private val creditCardService: CreditCardService,
    private val tickerService: TickerService,
    private val bondService: BondService,
    private val bondInterestCalculationService: BondInterestCalculationService,
) {
    private val logger = LoggerFactory.getLogger(NetWorthService::class.java)

    private val calculationMutex = Mutex()

    val isCalculating: Boolean
        get() = calculationMutex.isLocked

    @Transactional
    suspend fun recalculateAllSnapshots() =
        calculationMutex.withLock {
            logger.info("Starting net worth recalculation...")

            val wallets = walletService.getAllWalletsOrderedByName()

            val earliestDate = findEarliestTransactionDate(wallets) ?: LocalDateTime.now()
            val startMonth = YearMonth.from(earliestDate)
            val currentMonth = YearMonth.now()
            val endMonth = currentMonth.plusMonths(Constants.PL_CHART_FUTURE_MONTHS.toLong())

            logger.info(
                "Calculating net worth from {} to {} (based on earliest transaction)",
                startMonth,
                endMonth,
            )

            deleteSnapshotsOutsideRange(startMonth, endMonth)

            var targetMonth = startMonth
            var monthCount = 0

            while (targetMonth.isBeforeOrEqual(endMonth)) {
                monthCount++

                logger.info("\n========================================")
                logger.info("CALCULATING NET WORTH FOR {}/{}", targetMonth.month, targetMonth.year)
                logger.info("========================================")

                val walletBalances = calculateWalletBalancesForMonth(targetMonth, wallets, BalanceCalculationConfig.POSITIVE)
                val negativeWalletBalances = calculateWalletBalancesForMonth(targetMonth, wallets, BalanceCalculationConfig.NEGATIVE)
                val investments = calculateInvestmentValueForMonth(targetMonth)
                val recurringIncome = calculateRecurringTransactionsIncome(targetMonth)
                val assets = walletBalances.add(investments).add(recurringIncome)

                val creditCardDebt = calculateCreditCardDebt(targetMonth)
                val recurringTransactionsDebt = calculateRecurringTransactionsDebt(targetMonth)
                val liabilities = creditCardDebt.add(negativeWalletBalances).add(recurringTransactionsDebt)

                val netWorth = assets.minus(liabilities)

                logger.info("--- SUMMARY FOR {}/{} ---", targetMonth.month, targetMonth.year)
                logger.info(
                    "Assets: {} (Wallets: {} + Investments: {} + Recurring Income: {})",
                    assets,
                    walletBalances,
                    investments,
                    recurringIncome,
                )
                logger.info(
                    "Liabilities: {} (Credit Card: {} + Negative Wallets: {} + Recurring Expenses: {})",
                    liabilities,
                    creditCardDebt,
                    negativeWalletBalances,
                    recurringTransactionsDebt,
                )
                logger.info("Net Worth: {}", netWorth)
                logger.info("========================================\n")

                saveSnapshot(
                    NetWorthSnapshot(
                        referenceMonth = targetMonth,
                        assets = assets,
                        liabilities = liabilities,
                        netWorth = netWorth,
                        walletBalances = walletBalances,
                        investments = investments,
                        creditCardDebt = creditCardDebt,
                        negativeWalletBalances = negativeWalletBalances,
                        calculatedAt = LocalDateTime.now(),
                    ),
                )

                targetMonth = targetMonth.plusMonths(1)
            }

            logger.info("Calculated {} months of net worth data", monthCount)
        }

    fun getSnapshot(referenceMonth: YearMonth): NetWorthSnapshot? = netWorthSnapshotRepository.findByReferenceMonth(referenceMonth)

    private fun deleteSnapshotsOutsideRange(
        startMonth: YearMonth,
        endMonth: YearMonth,
    ) {
        logger.info("Deleting snapshots outside range {} to {}", startMonth, endMonth)
        netWorthSnapshotRepository.deleteSnapshotsOutsideRange(startMonth, endMonth)
    }

    private fun saveSnapshot(snapshot: NetWorthSnapshot): NetWorthSnapshot {
        val existing = netWorthSnapshotRepository.findByReferenceMonth(snapshot.referenceMonth)

        val entity =
            existing?.apply {
                referenceMonth = snapshot.referenceMonth
                assets = snapshot.assets
                liabilities = snapshot.liabilities
                netWorth = snapshot.netWorth
                walletBalances = snapshot.walletBalances
                investments = snapshot.investments
                creditCardDebt = snapshot.creditCardDebt
                negativeWalletBalances = snapshot.negativeWalletBalances
                calculatedAt = LocalDateTime.now()
            } ?: snapshot

        return netWorthSnapshotRepository.save(entity)
    }

    private fun findEarliestTransactionDate(wallets: List<Wallet>): LocalDateTime? {
        val walletIds = wallets.mapNotNull { it.id }
        val earliestDate = walletService.getEarliestTransactionDateByWallets(walletIds)
        return earliestDate
    }

    private fun calculateWalletBalancesForMonth(
        targetMonth: YearMonth,
        wallets: List<Wallet>,
        config: BalanceCalculationConfig,
    ): BigDecimal {
        val currentMonth = YearMonth.now()

        logger.debug("=== Calculating {} for {}/{} ===", config.logPrefix, targetMonth.month, targetMonth.year)
        logger.debug("Target month: {} | Current month: {}", targetMonth, currentMonth)

        val filteredWallets = wallets.filter(config.walletFilter)

        val totalBalance =
            when {
                targetMonth.isAfter(currentMonth) -> calculateFutureWalletBalances(targetMonth, currentMonth, filteredWallets, config)
                targetMonth == currentMonth -> calculateCurrentMonthWalletBalances(targetMonth, filteredWallets, config)
                else -> calculateHistoricalWalletBalances(targetMonth, filteredWallets, config)
            }

        val result = config.resultTransform(totalBalance)
        logger.debug("Total {}: {}", config.logPrefix.lowercase(), result)
        logger.debug("=== End {} calculation ===\n", config.logPrefix)
        return result
    }

    private fun calculateFutureWalletBalances(
        targetMonth: YearMonth,
        currentMonth: YearMonth,
        wallets: List<Wallet>,
        config: BalanceCalculationConfig,
    ): BigDecimal {
        logger.info("Future month - projecting from current balance")
        var totalBalance = wallets.sumOf { it.balance.abs() }

        val futureTransactions =
            recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(
                currentMonth.plusMonths(1),
                targetMonth,
            )

        totalBalance = applyTransactionsImpact(totalBalance, futureTransactions)

        logger.debug("Total future {}: {}", config.logPrefix.lowercase(), totalBalance)
        return totalBalance
    }

    private fun calculateCurrentMonthWalletBalances(
        targetMonth: YearMonth,
        wallets: List<Wallet>,
        config: BalanceCalculationConfig,
    ): BigDecimal {
        logger.debug("Current month - including pending and scheduled transactions")
        var totalBalance = wallets.sumOf { it.balance.abs() }

        val scheduledTransactions =
            recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(
                targetMonth,
                targetMonth,
            )

        totalBalance = applyTransactionsImpact(totalBalance, scheduledTransactions)

        val currentMonthTransactions = walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(targetMonth)
        val pendingTransactions = currentMonthTransactions.filter { it.isPending() }

        totalBalance = applyTransactionsImpact(totalBalance, pendingTransactions)

        logger.debug("Total current month {}: {}", config.logPrefix.lowercase(), totalBalance)
        return totalBalance
    }

    private fun applyTransactionsImpact(
        baseBalance: BigDecimal,
        transactions: List<WalletTransaction>,
    ): BigDecimal {
        val (incomes, expenses) = transactions.partition { it.isIncome() }

        val incomesTotal = incomes.sumOf { it.amount }
        val expensesTotal = expenses.sumOf { it.amount }

        return baseBalance.add(incomesTotal).subtract(expensesTotal)
    }

    private fun calculateHistoricalWalletBalances(
        targetMonth: YearMonth,
        wallets: List<Wallet>,
        config: BalanceCalculationConfig,
    ): BigDecimal {
        logger.debug("Historical month - calculating retroactively")
        var totalBalance = BigDecimal.ZERO

        val startOfNextMonth = targetMonth.plusMonths(1).atDay(1).atStartOfDay()
        val startOfTargetMonth = targetMonth.atDay(1).atStartOfDay()

        val allTransactionsByWallet =
            walletService
                .getAllWalletTransactionsByWalletsAfterDate(
                    wallets.map {
                        it.id!!
                    },
                    startOfNextMonth,
                ).groupBy { it.wallet.id!! }
        val allPaymentsByWallet =
            creditCardService
                .getAllPaidPaymentsByWalletsFromDateOnward(
                    wallets.map {
                        it.id!!
                    },
                    startOfTargetMonth,
                ).groupBy { it.wallet!!.id!! }

        wallets.forEach { wallet ->
            var walletBalance = wallet.balance
            logger.debug("  '{}': current balance = {}", wallet, walletBalance)

            val transactions = allTransactionsByWallet[wallet.id!!] ?: emptyList()
            val payments = allPaymentsByWallet[wallet.id!!] ?: emptyList()

            walletBalance = revertWalletTransactionsAfterMonth(wallet, transactions)
            walletBalance = revertCreditCardPaymentsAfterMonth(walletBalance, payments, startOfNextMonth)

            logger.debug("    Final balance for '{}': {}", wallet, walletBalance)

            if (walletBalance.isNotZero()) {
                totalBalance += walletBalance.abs()
                logger.debug("      -> Added to {}", config.logPrefix.lowercase())
            }
        }

        return totalBalance
    }

    private fun calculateCreditCardDebt(targetMonth: YearMonth): BigDecimal {
        logger.debug("=== Calculating CREDIT CARD DEBT for {}/{} ===", targetMonth.month, targetMonth.year)
        val dateAtEndOfMonth = targetMonth.atEndOfMonth().atEndOfDay()

        val creditCardDebt = creditCardService.getDebtAtDate(dateAtEndOfMonth)

        logger.debug("Total credit card debt: {}", creditCardDebt)
        logger.debug("=== End CREDIT CARD DEBT calculation ===\n")
        return creditCardDebt
    }

    private fun calculateRecurringTransactionsAmount(
        targetMonth: YearMonth,
        walletTransactionType: WalletTransactionType,
        debugLabel: String,
    ): BigDecimal {
        logger.debug("=== Calculating RECURRING TRANSACTIONS {} for {}/{} ===", debugLabel, targetMonth.month, targetMonth.year)

        val startOfMonth = targetMonth.atDay(1)
        val endOfMonth = targetMonth.atEndOfMonth()

        val allRecurringTransactions = recurringTransactionService.getAllRecurringTransactionsByType(walletTransactionType)

        val totalAmount =
            allRecurringTransactions
                .filter { it.includeInNetWorth }
                .filterNot { it.endDate.isOpenEnded() }
                .filter { endOfMonth.isAfterOrEqual(it.startDate) }
                .sumOf { calculateRecurringTransactionAmountInMonth(it, startOfMonth, endOfMonth) }

        logger.debug("Total Recurring Transactions {}: {}", debugLabel, totalAmount)
        logger.debug("=== End RECURRING TRANSACTIONS {} calculation ===\n", debugLabel)
        return totalAmount
    }

    private fun calculateRecurringTransactionAmountInMonth(
        recurring: RecurringTransaction,
        startOfMonth: LocalDate,
        endOfMonth: LocalDate,
    ): BigDecimal {
        var currentDate = advanceDateToMonthStart(recurring.startDate, startOfMonth, recurring)
        var amount = BigDecimal.ZERO

        while (currentDate.isBeforeOrEqual(endOfMonth) && currentDate.isBeforeOrEqual(recurring.endDate)) {
            amount += recurring.amount
            logger.debug(
                "  {} - installment due: {}",
                recurring,
                currentDate,
            )
            currentDate = recurring.nextFrom(currentDate)
        }

        if (amount > BigDecimal.ZERO) {
            logger.debug(
                "  Total remaining amount for {}: {} (start: {}, end: {})",
                recurring,
                amount,
                recurring.startDate,
                recurring.endDate,
            )
        }

        return amount
    }

    private fun advanceDateToMonthStart(
        startDate: LocalDate,
        targetMonthStart: LocalDate,
        recurring: RecurringTransaction,
    ): LocalDate {
        var date = startDate
        while (date.isBefore(targetMonthStart)) {
            date = recurring.nextFrom(date)
        }
        return date
    }

    private fun calculateRecurringTransactionsDebt(targetMonth: YearMonth): BigDecimal =
        calculateRecurringTransactionsAmount(targetMonth, WalletTransactionType.EXPENSE, "DEBT")

    private fun calculateRecurringTransactionsIncome(targetMonth: YearMonth): BigDecimal =
        calculateRecurringTransactionsAmount(targetMonth, WalletTransactionType.INCOME, "INCOME")

    private fun calculateInvestmentValueForMonth(targetMonth: YearMonth): BigDecimal {
        val endOfMonth = targetMonth.atEndOfMonth().atEndOfDay()

        val tickerValue = calculateTickerValueAtDate(endOfMonth)
        val bondValue = calculateBondValueAtDate(endOfMonth)

        return tickerValue.add(bondValue)
    }

    private fun calculateTickerValueAtDate(date: LocalDateTime): BigDecimal {
        val quantityChangesAfter =
            (
                tickerService
                    .getAllPurchases()
                    .asSequence()
                    .filter { it.walletTransaction!!.date.isAfter(date) }
                    .map { it.ticker.id!! to it.quantity } +
                    tickerService
                        .getAllSales()
                        .asSequence()
                        .filter { it.walletTransaction!!.date.isAfter(date) }
                        .map { it.ticker.id!! to it.quantity.negate() }
            ).groupingBy { it.first }
                .fold(BigDecimal.ZERO) { acc, (_, quantity) -> acc.add(quantity) }

        val tickerMap = tickerService.getAllTickers().associateBy { it.id!! }

        return tickerMap.values
            .asSequence()
            .mapNotNull { ticker ->
                val changesAfter = quantityChangesAfter[ticker.id!!] ?: BigDecimal.ZERO
                val historicalQuantity = ticker.currentQuantity - changesAfter

                if (historicalQuantity > BigDecimal.ZERO) {
                    historicalQuantity.multiply(ticker.currentUnitValue)
                } else {
                    null
                }
            }.fold(BigDecimal.ZERO, BigDecimal::add)
    }

    private fun calculateBondValueAtDate(date: LocalDateTime): BigDecimal {
        val operations = bondService.getOperationsByDateBefore(date)

        if (operations.isEmpty()) {
            return BigDecimal.ZERO
        }

        val targetMonth = YearMonth.from(date)
        val bondMap =
            operations.groupingBy { it.bond.id!! }.fold(BondOperationCalculationDTO()) { acc, bondOperation ->
                when (bondOperation.operationType) {
                    OperationType.BUY ->
                        acc.copy(
                            quantity = acc.quantity.add(bondOperation.quantity),
                            price = bondOperation.unitPrice,
                            bond = bondOperation.bond,
                        )
                    OperationType.SELL ->
                        acc.copy(
                            quantity = acc.quantity.minus(bondOperation.quantity),
                            bond = bondOperation.bond,
                        )
                }
            }

        return bondMap.values
            .filter { it.quantity > BigDecimal.ZERO }
            .sumOf { data ->
                val interestCalculation =
                    bondInterestCalculationService
                        .getMonthlyInterestHistory(data.bond!!)
                        .firstOrNull { it.referenceMonth == targetMonth }

                interestCalculation?.finalValue ?: (data.quantity.multiply(data.price))
            }
    }

    private fun revertWalletTransactionsAfterMonth(
        wallet: Wallet,
        transactionsAfter: List<WalletTransaction>,
    ): BigDecimal {
        logger.debug("    Reverting {} transactions", transactionsAfter.size)

        return transactionsAfter.fold(wallet.balance) { acc, tx ->
            if (tx.isPending()) return@fold acc

            when (tx.type) {
                WalletTransactionType.INCOME -> acc.minus(tx.amount)
                WalletTransactionType.EXPENSE -> acc.add(tx.amount)
            }
        }
    }

    private fun revertCreditCardPaymentsAfterMonth(
        walletBalance: BigDecimal,
        allPayments: List<CreditCardPayment>,
        startOfNextMonth: LocalDateTime,
    ): BigDecimal {
        val paymentsInMonth =
            allPayments
                .asSequence()
                .filter { it.date.isBefore(startOfNextMonth) }
                .sumOf { it.amount.minus(it.rebateUsed) }

        val futurePayments =
            allPayments
                .asSequence()
                .filter { it.date.isAfterOrEqual(startOfNextMonth) }
                .sumOf { it.amount.minus(it.rebateUsed) }

        return walletBalance.add(futurePayments).minus(paymentsInMonth)
    }
}
