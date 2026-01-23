/*
 * Filename: NetWorthCalculationService.java
 * Created on: January 22, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moinex.model.enums.OperationType;
import org.moinex.model.enums.TransactionStatus;
import org.moinex.model.enums.TransactionType;
import org.moinex.model.investment.BondOperation;
import org.moinex.model.investment.Ticker;
import org.moinex.model.investment.TickerPurchase;
import org.moinex.model.investment.TickerSale;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.util.Constants;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetWorthCalculationService {

    private final NetWorthSnapshotService netWorthSnapshotService;
    private final WalletService walletService;
    private final WalletTransactionService walletTransactionService;
    private final RecurringTransactionService recurringTransactionService;
    private final CreditCardService creditCardService;
    private final TickerService tickerService;
    private final BondService bondService;

    private List<Wallet> wallets;

    @Getter private volatile boolean isCalculating = false;

    /**
     * Recalculate all net worth snapshots asynchronously
     * This is a long-running operation that should not block the UI
     * @return CompletableFuture that completes when calculation is done
     */
    @Async
    public CompletableFuture<Void> recalculateAllSnapshots() {
        if (isCalculating) {
            log.warn("Calculation already in progress, skipping");
            return CompletableFuture.completedFuture(null);
        }

        try {
            isCalculating = true;

            log.info("Starting net worth recalculation...");

            wallets = walletService.getAllWalletsOrderedByName();

            netWorthSnapshotService.deleteAllSnapshots();

            // Find earliest transaction/transfer date to determine starting point
            LocalDateTime earliestDate = null;

            for (Wallet wallet : wallets) {
                LocalDateTime firstTxDate =
                        walletTransactionService.getFirstTransactionDate(wallet.getId());
                if (firstTxDate != null
                        && (earliestDate == null || firstTxDate.isBefore(earliestDate))) {
                    earliestDate = firstTxDate;
                }
            }

            if (earliestDate == null) {
                earliestDate = LocalDateTime.now();
            }

            YearMonth startMonth = YearMonth.from(earliestDate);
            YearMonth currentMonth = YearMonth.now();
            YearMonth endMonth = currentMonth.plusMonths(Constants.PL_CHART_FUTURE_MONTHS);

            log.info(
                    "Calculating net worth from {} to {} (based on earliest transaction/transfer)",
                    startMonth,
                    endMonth);

            YearMonth targetMonth = startMonth;
            int monthCount = 0;

            while (!targetMonth.isAfter(endMonth)) {
                Integer month = targetMonth.getMonthValue();
                Integer year = targetMonth.getYear();
                monthCount++;

                log.info("\n========================================");
                log.info("CALCULATING NET WORTH FOR {}/{}", month, year);
                log.info("========================================");

                BigDecimal walletBalances = calculateWalletBalancesForMonth(month, year);
                BigDecimal investments = calculateInvestmentValueForMonth(month, year);
                BigDecimal assets = walletBalances.add(investments);

                BigDecimal negativeWalletBalances = calculateNegativeWalletBalances(month, year);
                BigDecimal creditCardDebt = calculateCreditCardDebt(month, year);
                BigDecimal liabilities = creditCardDebt.add(negativeWalletBalances);

                BigDecimal netWorth = assets.subtract(liabilities);

                log.info("--- SUMMARY FOR {}/{} ---", month, year);
                log.info(
                        "Assets: {} (Wallets: {} + Investments: {})",
                        assets,
                        walletBalances,
                        investments);
                log.info(
                        "Liabilities: {} (Credit Card: {} + Negative Wallets: {})",
                        liabilities,
                        creditCardDebt,
                        negativeWalletBalances);
                log.info("Net Worth: {}", netWorth);
                log.info("========================================\n");

                netWorthSnapshotService.saveSnapshot(
                        month,
                        year,
                        assets,
                        liabilities,
                        netWorth,
                        walletBalances,
                        investments,
                        creditCardDebt,
                        negativeWalletBalances);

                targetMonth = targetMonth.plusMonths(1);
            }

            log.info("Calculated {} months of net worth data", monthCount);

            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        } finally {
            isCalculating = false;
        }
    }

    /**
     * Calculate wallet balances for a given month
     * @param month The month
     * @param year The year
     * @return Total wallet balances
     */
    private BigDecimal calculateWalletBalancesForMonth(Integer month, Integer year) {
        BigDecimal totalBalance = BigDecimal.ZERO;

        YearMonth targetMonth = YearMonth.of(year, month);
        YearMonth currentMonth = YearMonth.now();

        log.debug("=== Calculating WALLET BALANCES for {}/{} ===", month, year);
        log.debug("Target month: {} | Current month: {}", targetMonth, currentMonth);

        if (targetMonth.isAfter(currentMonth)) { // Future projection
            log.info("Future month - projecting from current balance");
            totalBalance =
                    wallets.stream()
                            .map(Wallet::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<WalletTransaction> futureTransactions =
                    recurringTransactionService.getFutureTransactionsByMonthForAnalysis(
                            currentMonth.plusMonths(1), targetMonth);

            BigDecimal futureIncomesTotal =
                    futureTransactions.stream()
                            .filter(t -> t.getType().equals(TransactionType.INCOME))
                            .map(WalletTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalBalance = totalBalance.add(futureIncomesTotal);

            BigDecimal futureExpensesTotal =
                    futureTransactions.stream()
                            .filter(t -> t.getType().equals(TransactionType.EXPENSE))
                            .map(WalletTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalBalance = totalBalance.subtract(futureExpensesTotal);

            log.debug("Total future wallet balances: {}", totalBalance);
        } else if (targetMonth.equals(
                currentMonth)) { // Current month - include pending and scheduled
            log.debug("Current month - including pending and scheduled transactions");
            totalBalance =
                    wallets.stream()
                            .map(Wallet::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Add scheduled recurring transactions for current month
            List<WalletTransaction> scheduledTransactions =
                    recurringTransactionService.getFutureTransactionsByMonthForAnalysis(
                            currentMonth, currentMonth);

            BigDecimal scheduledIncomes =
                    scheduledTransactions.stream()
                            .filter(t -> t.getType().equals(TransactionType.INCOME))
                            .map(WalletTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalBalance = totalBalance.add(scheduledIncomes);

            BigDecimal scheduledExpenses =
                    scheduledTransactions.stream()
                            .filter(t -> t.getType().equals(TransactionType.EXPENSE))
                            .map(WalletTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalBalance = totalBalance.subtract(scheduledExpenses);

            // Add pending transactions for current month
            List<WalletTransaction> currentMonthTransactions =
                    walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            month, year);

            BigDecimal pendingIncomes =
                    currentMonthTransactions.stream()
                            .filter(
                                    t ->
                                            t.getType().equals(TransactionType.INCOME)
                                                    && t.getStatus()
                                                            .equals(TransactionStatus.PENDING))
                            .map(WalletTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalBalance = totalBalance.add(pendingIncomes);

            BigDecimal pendingExpenses =
                    currentMonthTransactions.stream()
                            .filter(
                                    t ->
                                            t.getType().equals(TransactionType.EXPENSE)
                                                    && t.getStatus()
                                                            .equals(TransactionStatus.PENDING))
                            .map(WalletTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalBalance = totalBalance.subtract(pendingExpenses);

            log.debug("Total current month wallet balances: {}", totalBalance);
        } else { // Historical data - calculate retroactively from current balance
            log.debug("Historical month - calculating retroactively");
            for (Wallet wallet : wallets) {
                BigDecimal walletBalance = wallet.getBalance();
                log.debug("  Wallet '{}': current balance = {}", wallet.getName(), walletBalance);

                walletBalance = revertWalletTransactionsAfterMonth(wallet, targetMonth);
                walletBalance = revertCreditCardPaymentsAfterMonth(wallet, walletBalance, targetMonth, month, year);

                log.debug("    Final balance for '{}': {}", wallet.getName(), walletBalance);

                // Only add positive balances to avoid negative assets
                if (walletBalance.compareTo(BigDecimal.ZERO) > 0) {
                    totalBalance = totalBalance.add(walletBalance);
                    log.debug("      -> Added to assets (positive)");
                } else {
                    log.debug("      -> Skipped (negative or zero, will be counted as liability)");
                }
            }
        }

        log.debug("Total wallet balances (assets only): {}", totalBalance);
        log.debug("=== End WALLET BALANCES calculation ===\n");
        return totalBalance;
    }

    /**
     * Calculate negative wallet balances (overdrafts) for a given month
     * @param month The month
     * @param year The year
     * @return Total negative wallet balances
     */
    private BigDecimal calculateNegativeWalletBalances(Integer month, Integer year) {
        BigDecimal totalNegative = BigDecimal.ZERO;

        YearMonth targetMonth = YearMonth.of(year, month);
        YearMonth currentMonth = YearMonth.now();

        log.debug("=== Calculating NEGATIVE WALLET BALANCES for {}/{} ===", month, year);
        log.debug("Target month: {} | Current month: {}", targetMonth, currentMonth);

        if (targetMonth.isAfter(currentMonth)) { // Future projection
            log.debug("Future month - projecting negative balances");

            totalNegative =
                    wallets.stream()
                            .map(Wallet::getBalance)
                            .filter(b -> b.compareTo(BigDecimal.ZERO) < 0)
                            .map(BigDecimal::abs)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.debug("Total future negative balances: {}", totalNegative);
        } else if (targetMonth.equals(currentMonth)) { // Current month
            log.debug("Current month - using current negative balances");

            totalNegative =
                    wallets.stream()
                            .map(Wallet::getBalance)
                            .filter(b -> b.compareTo(BigDecimal.ZERO) < 0)
                            .map(BigDecimal::abs)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.debug("Total current negative balances: {}", totalNegative);
        } else { // Historical data
            log.debug("Historical month - calculating negative balances retroactively");

            for (Wallet wallet : wallets) {
                BigDecimal walletBalance = wallet.getBalance();
                log.debug("  Wallet '{}': current balance = {}", wallet.getName(), walletBalance);

                walletBalance = revertWalletTransactionsAfterMonth(wallet, targetMonth);
                walletBalance = revertCreditCardPaymentsAfterMonth(wallet, walletBalance, targetMonth, month, year);

                log.debug("    Final balance for '{}': {}", wallet.getName(), walletBalance);

                // Only add negative balances as liabilities
                if (walletBalance.compareTo(BigDecimal.ZERO) < 0) {
                    totalNegative = totalNegative.add(walletBalance.abs());
                    log.debug("      -> Added to liabilities: {}", walletBalance.abs());
                }
            }
        }

        log.debug("Total negative wallet balances: {}", totalNegative);
        log.debug("=== End NEGATIVE WALLET BALANCES calculation ===\n");
        return totalNegative;
    }

    /**
     * Calculate credit card debt for a given month
     * @param month The month
     * @param year The year
     * @return Total credit card debt
     */
    private BigDecimal calculateCreditCardDebt(Integer month, Integer year) {
        log.debug("=== Calculating CREDIT CARD DEBT for {}/{} ===", month, year);
        BigDecimal creditCardDebt = BigDecimal.ZERO;

        YearMonth targetMonth = YearMonth.of(year, month);
        YearMonth currentMonth = YearMonth.now();

        if (targetMonth.isAfter(currentMonth)) {
            // Future: pending invoices (debts not yet paid)
            creditCardDebt = creditCardService.getPendingPaymentsByMonth(month, year);
        } else if (targetMonth.equals(currentMonth)) {
            // Current month: paid invoices + pending invoices
            BigDecimal crcPaidPayments =
                    creditCardService.getEffectivePaidPaymentsByMonth(month, year);
            creditCardDebt = creditCardDebt.add(crcPaidPayments);

            BigDecimal crcPendingPayments =
                    creditCardService.getPendingPaymentsByMonth(month, year);
            creditCardDebt = creditCardDebt.add(crcPendingPayments);
        } else {
            // Historical: paid invoices (debts that existed at that time)
            creditCardDebt = creditCardService.getEffectivePaidPaymentsByMonth(month, year);
        }

        log.debug("Total credit card debt: {}", creditCardDebt);
        log.debug("=== End CREDIT CARD DEBT calculation ===\n");
        return creditCardDebt;
    }

    /**
     * Calculate investment value for a given month
     * @param month The month
     * @param year The year
     * @return Total investment value
     */
    private BigDecimal calculateInvestmentValueForMonth(Integer month, Integer year) {
        YearMonth targetMonth = YearMonth.of(year, month);

        LocalDateTime endOfMonth = targetMonth.atEndOfMonth().atTime(23, 59, 59);

        BigDecimal tickerValue = calculateTickerValueAtDate(endOfMonth);

        BigDecimal bondValue = calculateBondValueAtDate(endOfMonth);

        return tickerValue.add(bondValue);
    }

    /**
     * Calculate total ticker value at a specific date
     * @param date The date to calculate value for
     * @return Total ticker value
     */
    private BigDecimal calculateTickerValueAtDate(LocalDateTime date) {
        // Get all purchases and sales AFTER the target date
        List<TickerPurchase> allPurchases = tickerService.getAllPurchases();
        List<TickerSale> allSales = tickerService.getAllSales();

        // Calculate quantity changes after target date for each ticker
        Map<Integer, BigDecimal> quantityChangesAfter = new HashMap<>();

        for (TickerPurchase purchase : allPurchases) {
            LocalDateTime purchaseDate = purchase.getWalletTransaction().getDate();
            if (purchaseDate.isAfter(date)) {
                Integer tickerId = purchase.getTicker().getId();
                quantityChangesAfter.merge(tickerId, purchase.getQuantity(), BigDecimal::add);
            }
        }

        for (TickerSale sale : allSales) {
            LocalDateTime saleDate = sale.getWalletTransaction().getDate();
            if (saleDate.isAfter(date)) {
                Integer tickerId = sale.getTicker().getId();
                quantityChangesAfter.merge(tickerId, sale.getQuantity(), BigDecimal::add);
            }
        }

        // Calculate historical quantity: current quantity - changes after date
        List<Ticker> allTickers = tickerService.getAllTickers();
        Map<Integer, BigDecimal> tickerQuantities = new HashMap<>();

        for (Ticker ticker : allTickers) {
            BigDecimal currentQty = ticker.getCurrentQuantity();
            BigDecimal changesAfter =
                    quantityChangesAfter.getOrDefault(ticker.getId(), BigDecimal.ZERO);
            BigDecimal historicalQty = currentQty.subtract(changesAfter);

            if (historicalQty.compareTo(BigDecimal.ZERO) > 0) {
                tickerQuantities.put(ticker.getId(), historicalQty);
            }
        }

        Map<Integer, Ticker> tickerMap =
                allTickers.stream().collect(Collectors.toMap(Ticker::getId, ticker -> ticker));

        BigDecimal totalValue = BigDecimal.ZERO;
        for (Map.Entry<Integer, BigDecimal> entry : tickerQuantities.entrySet()) {
            Ticker ticker = tickerMap.get(entry.getKey());
            if (ticker != null) {
                BigDecimal quantity = entry.getValue();
                BigDecimal currentPrice = ticker.getCurrentUnitValue();
                BigDecimal value = quantity.multiply(currentPrice);
                totalValue = totalValue.add(value);
            }
        }

        return totalValue;
    }

    /**
     * Calculate total bond value at a specific date
     * @param date The date to calculate value for
     * @return Total bond value
     */
    private BigDecimal calculateBondValueAtDate(LocalDateTime date) {
        List<BondOperation> operations = bondService.getOperationsByDateBefore(date);

        // Calculate quantity held for each bond
        Map<Integer, BigDecimal> bondQuantities = new HashMap<>();
        Map<Integer, BigDecimal> bondPrices = new HashMap<>();

        for (BondOperation operation : operations) {
            Integer bondId = operation.getBond().getId();

            if (operation.getOperationType().equals(OperationType.BUY)) {
                bondQuantities.merge(bondId, operation.getQuantity(), BigDecimal::add);
                bondPrices.put(bondId, operation.getUnitPrice());
            } else if (operation.getOperationType().equals(OperationType.SELL)) {
                bondQuantities.merge(bondId, operation.getQuantity().negate(), BigDecimal::add);
            }
        }

        // Calculate total value using the last known price for each bond
        BigDecimal totalValue = BigDecimal.ZERO;
        for (java.util.Map.Entry<Integer, BigDecimal> entry : bondQuantities.entrySet()) {
            BigDecimal quantity = entry.getValue();
            BigDecimal price = bondPrices.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            totalValue = totalValue.add(quantity.multiply(price));
        }

        return totalValue;
    }

    /**
     * Revert wallet transactions that occurred after the target month
     * @param wallet The wallet to process
     * @param targetMonth The target month to revert from
     * @return The adjusted wallet balance
     */
    private BigDecimal revertWalletTransactionsAfterMonth(Wallet wallet, YearMonth targetMonth) {
        BigDecimal walletBalance = wallet.getBalance();
        LocalDateTime startOfNextMonth = targetMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<WalletTransaction> transactionsAfter =
                walletTransactionService.getTransactionsByWalletAfterDate(
                        wallet.getId(), startOfNextMonth);

        log.debug(
                "    Reverting {} transactions after {}",
                transactionsAfter.size(),
                startOfNextMonth);

        for (WalletTransaction tx : transactionsAfter) {
            if (tx.getStatus().equals(TransactionStatus.CONFIRMED)) {
                if (tx.getType().equals(TransactionType.INCOME)) {
                    walletBalance = walletBalance.subtract(tx.getAmount());
                } else if (tx.getType().equals(TransactionType.EXPENSE)) {
                    walletBalance = walletBalance.add(tx.getAmount());
                }
            }
        }

        return walletBalance;
    }

    /**
     * Revert credit card payments that occurred after the target month and apply payments in target month
     * @param wallet The wallet to process
     * @param walletBalance The current wallet balance (after transaction reversions)
     * @param targetMonth The target month to revert from
     * @param month The target month value
     * @param year The target year value
     * @return The adjusted wallet balance
     */
    private BigDecimal revertCreditCardPaymentsAfterMonth(
            Wallet wallet, BigDecimal walletBalance, YearMonth targetMonth, Integer month, Integer year) {

        // Revert credit card payments after target month
        for (int futureMonth = targetMonth.getMonthValue() + 1; futureMonth <= 12; futureMonth++) {
            BigDecimal payments =
                    creditCardService.getEffectivePaidPaymentsByMonth(
                            wallet.getId(), futureMonth, targetMonth.getYear());
            walletBalance = walletBalance.add(payments);
        }

        // For months in next years
        int nextYear = targetMonth.getYear() + 1;
        YearMonth now = YearMonth.now();
        while (nextYear <= now.getYear()) {
            int maxMonth = (nextYear == now.getYear()) ? now.getMonthValue() : 12;
            for (int futureMonth = 1; futureMonth <= maxMonth; futureMonth++) {
                BigDecimal payments =
                        creditCardService.getEffectivePaidPaymentsByMonth(
                                wallet.getId(), futureMonth, nextYear);
                walletBalance = walletBalance.add(payments);
            }
            nextYear++;
        }

        // Apply credit card payments in target month
        BigDecimal paymentsInMonth =
                creditCardService.getEffectivePaidPaymentsByMonth(wallet.getId(), month, year);
        walletBalance = walletBalance.subtract(paymentsInMonth);

        return walletBalance;
    }
}
