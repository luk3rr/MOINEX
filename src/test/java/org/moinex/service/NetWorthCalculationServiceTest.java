/*
 * Filename: NetWorthCalculationServiceTest.java
 * Created on: January 24, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.model.enums.OperationType;
import org.moinex.model.enums.TransactionType;
import org.moinex.model.investment.Bond;
import org.moinex.model.investment.BondOperation;
import org.moinex.model.investment.Ticker;
import org.moinex.model.investment.TickerPurchase;
import org.moinex.model.investment.TickerSale;
import org.moinex.model.wallettransaction.RecurringTransaction;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletTransaction;

@ExtendWith(MockitoExtension.class)
class NetWorthCalculationServiceTest {

    @Mock private NetWorthSnapshotService netWorthSnapshotService;
    @Mock private WalletService walletService;
    @Mock private WalletTransactionService walletTransactionService;
    @Mock private RecurringTransactionService recurringTransactionService;
    @Mock private CreditCardService creditCardService;
    @Mock private TickerService tickerService;
    @Mock private BondService bondService;

    @InjectMocks private NetWorthCalculationService netWorthCalculationService;

    private Wallet wallet1;
    private Wallet wallet2;

    @BeforeEach
    void setUp() {
        wallet1 = new Wallet(1, "Wallet 1", new BigDecimal("1000.00"));
        wallet2 = new Wallet(2, "Wallet 2", new BigDecimal("2000.00"));
    }

    @Nested
    @DisplayName("Recalculate All Snapshots Tests")
    class RecalculateAllSnapshotsTests {
        @Test
        @DisplayName("Should recalculate all snapshots successfully")
        void recalculateAllSnapshots_Success() throws Exception {
            when(walletService.getAllWalletsOrderedByName()).thenReturn(List.of(wallet1, wallet2));
            when(walletTransactionService.getFirstTransactionDate(1))
                    .thenReturn(LocalDateTime.now().minusMonths(3));
            when(walletTransactionService.getFirstTransactionDate(2))
                    .thenReturn(LocalDateTime.now().minusMonths(2));
            when(recurringTransactionService.getFutureTransactionsByMonthForAnalysis(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(creditCardService.getDebtAtDate(any())).thenReturn(BigDecimal.ZERO);
            when(creditCardService.getEffectivePaidPaymentsByMonth(anyInt(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(tickerService.getAllPurchases()).thenReturn(Collections.emptyList());
            when(tickerService.getAllSales()).thenReturn(Collections.emptyList());
            when(tickerService.getAllTickers()).thenReturn(Collections.emptyList());
            when(bondService.getOperationsByDateBefore(any())).thenReturn(Collections.emptyList());
            when(walletTransactionService.getTransactionsByWalletAfterDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(any()))
                    .thenReturn(Collections.emptyList());

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();

            assertNotNull(result);
            result.get();
            assertFalse(result.isCompletedExceptionally());
            verify(netWorthSnapshotService).deleteAllSnapshots();
            verify(netWorthSnapshotService, atLeastOnce())
                    .saveSnapshot(
                            anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should not recalculate if calculation is already in progress")
        void recalculateAllSnapshots_AlreadyCalculating() {
            netWorthCalculationService.recalculateAllSnapshots();

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();

            assertNotNull(result);
            assertTrue(result.isDone());
            verify(walletService, times(2)).getAllWalletsOrderedByName();
        }

        @Test
        @DisplayName("Should handle exception during recalculation")
        void recalculateAllSnapshots_Exception() {
            when(walletService.getAllWalletsOrderedByName())
                    .thenThrow(new RuntimeException("Database error"));

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();

            assertNotNull(result);
            assertTrue(result.isDone());
            assertTrue(result.isCompletedExceptionally());
        }
    }

    @Nested
    @DisplayName("Calculation State Tests")
    class CalculationStateTests {
        @Test
        @DisplayName("Should return false when not calculating")
        void isCalculating_False() {
            assertFalse(netWorthCalculationService.isCalculating());
        }

        @Test
        @DisplayName("Should return true when calculation is in progress")
        void isCalculating_True() throws Exception {
            when(walletService.getAllWalletsOrderedByName()).thenReturn(List.of(wallet1, wallet2));
            when(walletTransactionService.getFirstTransactionDate(anyInt()))
                    .thenReturn(LocalDateTime.now().minusMonths(1));
            when(recurringTransactionService.getFutureTransactionsByMonthForAnalysis(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(creditCardService.getDebtAtDate(any())).thenReturn(BigDecimal.ZERO);
            when(creditCardService.getEffectivePaidPaymentsByMonth(anyInt(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(tickerService.getAllPurchases()).thenReturn(Collections.emptyList());
            when(tickerService.getAllSales()).thenReturn(Collections.emptyList());
            when(tickerService.getAllTickers()).thenReturn(Collections.emptyList());
            when(bondService.getOperationsByDateBefore(any())).thenReturn(Collections.emptyList());
            when(walletTransactionService.getTransactionsByWalletAfterDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(any()))
                    .thenReturn(Collections.emptyList());

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();
            result.get();

            assertFalse(netWorthCalculationService.isCalculating());
        }
    }

    @Nested
    @DisplayName("Wallet Balances Calculation Tests")
    class WalletBalancesCalculationTests {
        @Test
        @DisplayName("Should calculate wallet balances for current month")
        void calculateWalletBalancesForMonth_CurrentMonth() throws Exception {
            when(walletService.getAllWalletsOrderedByName()).thenReturn(List.of(wallet1, wallet2));
            when(walletTransactionService.getFirstTransactionDate(anyInt()))
                    .thenReturn(LocalDateTime.now().minusMonths(1));
            when(recurringTransactionService.getFutureTransactionsByMonthForAnalysis(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(creditCardService.getDebtAtDate(any())).thenReturn(BigDecimal.ZERO);
            when(creditCardService.getEffectivePaidPaymentsByMonth(anyInt(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(tickerService.getAllPurchases()).thenReturn(Collections.emptyList());
            when(tickerService.getAllSales()).thenReturn(Collections.emptyList());
            when(tickerService.getAllTickers()).thenReturn(Collections.emptyList());
            when(bondService.getOperationsByDateBefore(any())).thenReturn(Collections.emptyList());
            when(walletTransactionService.getTransactionsByWalletAfterDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(any()))
                    .thenReturn(Collections.emptyList());

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();
            result.get();

            verify(netWorthSnapshotService, atLeastOnce())
                    .saveSnapshot(
                            anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should calculate wallet balances for future month")
        void calculateWalletBalancesForMonth_FutureMonth() throws Exception {
            when(walletService.getAllWalletsOrderedByName()).thenReturn(List.of(wallet1, wallet2));
            when(walletTransactionService.getFirstTransactionDate(anyInt()))
                    .thenReturn(LocalDateTime.now().minusMonths(1));
            when(recurringTransactionService.getFutureTransactionsByMonthForAnalysis(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(creditCardService.getDebtAtDate(any())).thenReturn(BigDecimal.ZERO);
            when(creditCardService.getEffectivePaidPaymentsByMonth(anyInt(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(tickerService.getAllPurchases()).thenReturn(Collections.emptyList());
            when(tickerService.getAllSales()).thenReturn(Collections.emptyList());
            when(tickerService.getAllTickers()).thenReturn(Collections.emptyList());
            when(bondService.getOperationsByDateBefore(any())).thenReturn(Collections.emptyList());
            when(walletTransactionService.getTransactionsByWalletAfterDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(any()))
                    .thenReturn(Collections.emptyList());

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();
            result.get();

            verify(netWorthSnapshotService, atLeastOnce())
                    .saveSnapshot(
                            anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Investment Value Calculation Tests")
    class InvestmentValueCalculationTests {
        @Test
        @DisplayName("Should calculate investment value with tickers and bonds")
        void calculateInvestmentValueForMonth_WithTickers() throws Exception {
            when(walletService.getAllWalletsOrderedByName()).thenReturn(List.of(wallet1, wallet2));
            when(walletTransactionService.getFirstTransactionDate(anyInt()))
                    .thenReturn(LocalDateTime.now().minusMonths(1));
            when(recurringTransactionService.getFutureTransactionsByMonthForAnalysis(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(creditCardService.getDebtAtDate(any())).thenReturn(BigDecimal.ZERO);
            when(creditCardService.getEffectivePaidPaymentsByMonth(anyInt(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(tickerService.getAllPurchases()).thenReturn(Collections.emptyList());
            when(tickerService.getAllSales()).thenReturn(Collections.emptyList());
            when(tickerService.getAllTickers()).thenReturn(Collections.emptyList());
            when(bondService.getOperationsByDateBefore(any())).thenReturn(Collections.emptyList());
            when(walletTransactionService.getTransactionsByWalletAfterDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(any()))
                    .thenReturn(Collections.emptyList());

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();
            result.get();

            verify(tickerService, atLeastOnce()).getAllPurchases();
            verify(tickerService, atLeastOnce()).getAllSales();
            verify(bondService, atLeastOnce()).getOperationsByDateBefore(any());
        }

        @Test
        @DisplayName("Should calculate investment value without tickers")
        void calculateInvestmentValueForMonth_NoTickers() throws Exception {
            when(walletService.getAllWalletsOrderedByName()).thenReturn(List.of(wallet1));
            when(walletTransactionService.getFirstTransactionDate(anyInt()))
                    .thenReturn(LocalDateTime.now().minusMonths(1));
            when(recurringTransactionService.getFutureTransactionsByMonthForAnalysis(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(creditCardService.getDebtAtDate(any())).thenReturn(BigDecimal.ZERO);
            when(creditCardService.getEffectivePaidPaymentsByMonth(anyInt(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(tickerService.getAllPurchases()).thenReturn(Collections.emptyList());
            when(tickerService.getAllSales()).thenReturn(Collections.emptyList());
            when(tickerService.getAllTickers()).thenReturn(Collections.emptyList());
            when(bondService.getOperationsByDateBefore(any())).thenReturn(Collections.emptyList());
            when(walletTransactionService.getTransactionsByWalletAfterDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(any()))
                    .thenReturn(Collections.emptyList());

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();
            result.get();

            verify(netWorthSnapshotService, atLeastOnce())
                    .saveSnapshot(
                            anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Credit Card Debt Calculation Tests")
    class CreditCardDebtCalculationTests {
        @Test
        @DisplayName("Should calculate credit card debt for current month")
        void calculateCreditCardDebt_CurrentMonth() throws Exception {
            when(walletService.getAllWalletsOrderedByName()).thenReturn(List.of(wallet1));
            when(walletTransactionService.getFirstTransactionDate(anyInt()))
                    .thenReturn(LocalDateTime.now().minusMonths(1));
            when(recurringTransactionService.getFutureTransactionsByMonthForAnalysis(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(creditCardService.getDebtAtDate(any())).thenReturn(new BigDecimal("500.00"));
            when(creditCardService.getEffectivePaidPaymentsByMonth(anyInt(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(tickerService.getAllPurchases()).thenReturn(Collections.emptyList());
            when(tickerService.getAllSales()).thenReturn(Collections.emptyList());
            when(tickerService.getAllTickers()).thenReturn(Collections.emptyList());
            when(bondService.getOperationsByDateBefore(any())).thenReturn(Collections.emptyList());
            when(walletTransactionService.getTransactionsByWalletAfterDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(any()))
                    .thenReturn(Collections.emptyList());

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();
            result.get();

            verify(creditCardService, atLeastOnce()).getDebtAtDate(any());
        }

        @Test
        @DisplayName("Should calculate credit card debt for future month")
        void calculateCreditCardDebt_FutureMonth() throws Exception {
            when(walletService.getAllWalletsOrderedByName()).thenReturn(List.of(wallet1));
            when(walletTransactionService.getFirstTransactionDate(anyInt()))
                    .thenReturn(LocalDateTime.now().minusMonths(1));
            when(recurringTransactionService.getFutureTransactionsByMonthForAnalysis(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(creditCardService.getDebtAtDate(any())).thenReturn(new BigDecimal("500.00"));
            when(creditCardService.getEffectivePaidPaymentsByMonth(anyInt(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(tickerService.getAllPurchases()).thenReturn(Collections.emptyList());
            when(tickerService.getAllSales()).thenReturn(Collections.emptyList());
            when(tickerService.getAllTickers()).thenReturn(Collections.emptyList());
            when(bondService.getOperationsByDateBefore(any())).thenReturn(Collections.emptyList());
            when(walletTransactionService.getTransactionsByWalletAfterDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(any()))
                    .thenReturn(Collections.emptyList());

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();
            result.get();

            verify(creditCardService, atLeastOnce()).getDebtAtDate(any());
        }
    }

    @Nested
    @DisplayName("Recurring Transactions Calculation Tests")
    class RecurringTransactionsCalculationTests {
        @Test
        @DisplayName(
                "Should calculate recurring transactions income with valid recurring transaction")
        void calculateRecurringTransactionsIncome_WithValidTransaction() throws Exception {
            RecurringTransaction incomeTransaction =
                    RecurringTransaction.builder()
                            .id(1)
                            .amount(new BigDecimal("500.00"))
                            .startDate(LocalDateTime.now().minusMonths(2))
                            .endDate(LocalDateTime.now().plusMonths(2))
                            .includeInNetWorth(true)
                            .frequency(org.moinex.model.enums.RecurringTransactionFrequency.MONTHLY)
                            .build();

            when(walletService.getAllWalletsOrderedByName()).thenReturn(List.of(wallet1));
            when(walletTransactionService.getFirstTransactionDate(anyInt()))
                    .thenReturn(LocalDateTime.now().minusMonths(3));
            when(recurringTransactionService.getFutureTransactionsByMonthForAnalysis(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(creditCardService.getDebtAtDate(any())).thenReturn(BigDecimal.ZERO);
            when(creditCardService.getEffectivePaidPaymentsByMonth(anyInt(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(tickerService.getAllPurchases()).thenReturn(Collections.emptyList());
            when(tickerService.getAllSales()).thenReturn(Collections.emptyList());
            when(tickerService.getAllTickers()).thenReturn(Collections.emptyList());
            when(bondService.getOperationsByDateBefore(any())).thenReturn(Collections.emptyList());
            when(walletTransactionService.getTransactionsByWalletAfterDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(TransactionType.INCOME))
                    .thenReturn(List.of(incomeTransaction));
            when(recurringTransactionService.getAllByType(TransactionType.EXPENSE))
                    .thenReturn(Collections.emptyList());

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();
            result.get();

            verify(recurringTransactionService, atLeastOnce()).getAllByType(TransactionType.INCOME);
            verify(netWorthSnapshotService, atLeastOnce())
                    .saveSnapshot(
                            anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName(
                "Should calculate recurring transactions debt with valid recurring transaction")
        void calculateRecurringTransactionsDebt_WithValidTransaction() throws Exception {
            RecurringTransaction expenseTransaction =
                    RecurringTransaction.builder()
                            .id(1)
                            .amount(new BigDecimal("300.00"))
                            .startDate(LocalDateTime.now().minusMonths(2))
                            .endDate(LocalDateTime.now().plusMonths(2))
                            .includeInNetWorth(true)
                            .frequency(org.moinex.model.enums.RecurringTransactionFrequency.MONTHLY)
                            .build();

            when(walletService.getAllWalletsOrderedByName()).thenReturn(List.of(wallet1));
            when(walletTransactionService.getFirstTransactionDate(anyInt()))
                    .thenReturn(LocalDateTime.now().minusMonths(3));
            when(recurringTransactionService.getFutureTransactionsByMonthForAnalysis(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(creditCardService.getDebtAtDate(any())).thenReturn(BigDecimal.ZERO);
            when(creditCardService.getEffectivePaidPaymentsByMonth(anyInt(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(tickerService.getAllPurchases()).thenReturn(Collections.emptyList());
            when(tickerService.getAllSales()).thenReturn(Collections.emptyList());
            when(tickerService.getAllTickers()).thenReturn(Collections.emptyList());
            when(bondService.getOperationsByDateBefore(any())).thenReturn(Collections.emptyList());
            when(walletTransactionService.getTransactionsByWalletAfterDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(TransactionType.INCOME))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(TransactionType.EXPENSE))
                    .thenReturn(List.of(expenseTransaction));

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();
            result.get();

            verify(recurringTransactionService, atLeastOnce())
                    .getAllByType(TransactionType.EXPENSE);
            verify(netWorthSnapshotService, atLeastOnce())
                    .saveSnapshot(
                            anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should skip recurring transaction not included in net worth")
        void calculateRecurringTransactions_NotIncludedInNetWorth() throws Exception {
            RecurringTransaction excludedTransaction =
                    RecurringTransaction.builder()
                            .id(1)
                            .amount(new BigDecimal("500.00"))
                            .startDate(LocalDateTime.now().minusMonths(2))
                            .endDate(LocalDateTime.now().plusMonths(2))
                            .includeInNetWorth(false)
                            .frequency(org.moinex.model.enums.RecurringTransactionFrequency.MONTHLY)
                            .build();

            when(walletService.getAllWalletsOrderedByName()).thenReturn(List.of(wallet1));
            when(walletTransactionService.getFirstTransactionDate(anyInt()))
                    .thenReturn(LocalDateTime.now().minusMonths(3));
            when(recurringTransactionService.getFutureTransactionsByMonthForAnalysis(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(creditCardService.getDebtAtDate(any())).thenReturn(BigDecimal.ZERO);
            when(creditCardService.getEffectivePaidPaymentsByMonth(anyInt(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(tickerService.getAllPurchases()).thenReturn(Collections.emptyList());
            when(tickerService.getAllSales()).thenReturn(Collections.emptyList());
            when(tickerService.getAllTickers()).thenReturn(Collections.emptyList());
            when(bondService.getOperationsByDateBefore(any())).thenReturn(Collections.emptyList());
            when(walletTransactionService.getTransactionsByWalletAfterDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(TransactionType.INCOME))
                    .thenReturn(List.of(excludedTransaction));
            when(recurringTransactionService.getAllByType(TransactionType.EXPENSE))
                    .thenReturn(Collections.emptyList());

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();
            result.get();

            verify(netWorthSnapshotService, atLeastOnce())
                    .saveSnapshot(
                            anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Ticker Value Calculation Tests")
    class TickerValueCalculationTests {
        @Test
        @DisplayName("Should calculate ticker value with purchases and sales")
        void calculateTickerValueAtDate_WithPurchasesAndSales() throws Exception {
            Ticker ticker =
                    Ticker.builder()
                            .id(1)
                            .currentQuantity(new BigDecimal("10.00"))
                            .currentUnitValue(new BigDecimal("100.00"))
                            .build();

            WalletTransaction purchaseTx =
                    WalletTransaction.builder()
                            .id(1)
                            .date(LocalDateTime.now().minusMonths(2))
                            .type(TransactionType.EXPENSE)
                            .amount(new BigDecimal("1000.00"))
                            .build();

            TickerPurchase purchase =
                    TickerPurchase.builder()
                            .id(1)
                            .ticker(ticker)
                            .quantity(new BigDecimal("5.00"))
                            .walletTransaction(purchaseTx)
                            .build();

            WalletTransaction saleTx =
                    WalletTransaction.builder()
                            .id(2)
                            .date(LocalDateTime.now().minusMonths(1))
                            .type(TransactionType.INCOME)
                            .amount(new BigDecimal("500.00"))
                            .build();

            TickerSale sale =
                    TickerSale.builder()
                            .id(1)
                            .ticker(ticker)
                            .quantity(new BigDecimal("2.00"))
                            .walletTransaction(saleTx)
                            .build();

            when(walletService.getAllWalletsOrderedByName()).thenReturn(List.of(wallet1));
            when(walletTransactionService.getFirstTransactionDate(anyInt()))
                    .thenReturn(LocalDateTime.now().minusMonths(3));
            when(recurringTransactionService.getFutureTransactionsByMonthForAnalysis(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(creditCardService.getDebtAtDate(any())).thenReturn(BigDecimal.ZERO);
            when(creditCardService.getEffectivePaidPaymentsByMonth(anyInt(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(tickerService.getAllPurchases()).thenReturn(List.of(purchase));
            when(tickerService.getAllSales()).thenReturn(List.of(sale));
            when(tickerService.getAllTickers()).thenReturn(List.of(ticker));
            when(bondService.getOperationsByDateBefore(any())).thenReturn(Collections.emptyList());
            when(walletTransactionService.getTransactionsByWalletAfterDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(any()))
                    .thenReturn(Collections.emptyList());

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();
            result.get();

            verify(tickerService, atLeastOnce()).getAllPurchases();
            verify(tickerService, atLeastOnce()).getAllSales();
            verify(tickerService, atLeastOnce()).getAllTickers();
            verify(netWorthSnapshotService, atLeastOnce())
                    .saveSnapshot(
                            anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should calculate ticker value with only purchases")
        void calculateTickerValueAtDate_OnlyPurchases() throws Exception {
            Ticker ticker =
                    Ticker.builder()
                            .id(1)
                            .currentQuantity(new BigDecimal("10.00"))
                            .currentUnitValue(new BigDecimal("100.00"))
                            .build();

            WalletTransaction purchaseTx =
                    WalletTransaction.builder()
                            .id(1)
                            .date(LocalDateTime.now().minusMonths(2))
                            .type(TransactionType.EXPENSE)
                            .amount(new BigDecimal("1000.00"))
                            .build();

            TickerPurchase purchase =
                    TickerPurchase.builder()
                            .id(1)
                            .ticker(ticker)
                            .quantity(new BigDecimal("10.00"))
                            .walletTransaction(purchaseTx)
                            .build();

            when(walletService.getAllWalletsOrderedByName()).thenReturn(List.of(wallet1));
            when(walletTransactionService.getFirstTransactionDate(anyInt()))
                    .thenReturn(LocalDateTime.now().minusMonths(3));
            when(recurringTransactionService.getFutureTransactionsByMonthForAnalysis(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(creditCardService.getDebtAtDate(any())).thenReturn(BigDecimal.ZERO);
            when(creditCardService.getEffectivePaidPaymentsByMonth(anyInt(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(tickerService.getAllPurchases()).thenReturn(List.of(purchase));
            when(tickerService.getAllSales()).thenReturn(Collections.emptyList());
            when(tickerService.getAllTickers()).thenReturn(List.of(ticker));
            when(bondService.getOperationsByDateBefore(any())).thenReturn(Collections.emptyList());
            when(walletTransactionService.getTransactionsByWalletAfterDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(any()))
                    .thenReturn(Collections.emptyList());

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();
            result.get();

            verify(netWorthSnapshotService, atLeastOnce())
                    .saveSnapshot(
                            anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Bond Value Calculation Tests")
    class BondValueCalculationTests {
        @Test
        @DisplayName("Should calculate bond value with buy and sell operations")
        void calculateBondValueAtDate_WithBuyAndSell() throws Exception {
            Bond bond = Bond.builder().id(1).name("Test Bond").build();

            WalletTransaction buyTx =
                    WalletTransaction.builder()
                            .id(1)
                            .date(LocalDateTime.now().minusMonths(2))
                            .type(TransactionType.EXPENSE)
                            .amount(new BigDecimal("5000.00"))
                            .build();

            BondOperation buyOperation =
                    BondOperation.builder()
                            .id(1)
                            .bond(bond)
                            .operationType(OperationType.BUY)
                            .quantity(new BigDecimal("100.00"))
                            .unitPrice(new BigDecimal("50.00"))
                            .walletTransaction(buyTx)
                            .build();

            WalletTransaction sellTx =
                    WalletTransaction.builder()
                            .id(2)
                            .date(LocalDateTime.now().minusMonths(1))
                            .type(TransactionType.INCOME)
                            .amount(new BigDecimal("1650.00"))
                            .build();

            BondOperation sellOperation =
                    BondOperation.builder()
                            .id(2)
                            .bond(bond)
                            .operationType(OperationType.SELL)
                            .quantity(new BigDecimal("30.00"))
                            .unitPrice(new BigDecimal("55.00"))
                            .walletTransaction(sellTx)
                            .build();

            when(walletService.getAllWalletsOrderedByName()).thenReturn(List.of(wallet1));
            when(walletTransactionService.getFirstTransactionDate(anyInt()))
                    .thenReturn(LocalDateTime.now().minusMonths(3));
            when(recurringTransactionService.getFutureTransactionsByMonthForAnalysis(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(creditCardService.getDebtAtDate(any())).thenReturn(BigDecimal.ZERO);
            when(creditCardService.getEffectivePaidPaymentsByMonth(anyInt(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(tickerService.getAllPurchases()).thenReturn(Collections.emptyList());
            when(tickerService.getAllSales()).thenReturn(Collections.emptyList());
            when(tickerService.getAllTickers()).thenReturn(Collections.emptyList());
            when(bondService.getOperationsByDateBefore(any()))
                    .thenReturn(List.of(buyOperation, sellOperation));
            when(walletTransactionService.getTransactionsByWalletAfterDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(any()))
                    .thenReturn(Collections.emptyList());

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();
            result.get();

            verify(bondService, atLeastOnce()).getOperationsByDateBefore(any());
            verify(netWorthSnapshotService, atLeastOnce())
                    .saveSnapshot(
                            anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should calculate bond value with only buy operations")
        void calculateBondValueAtDate_OnlyBuy() throws Exception {
            Bond bond = Bond.builder().id(1).name("Test Bond").build();

            WalletTransaction buyTx =
                    WalletTransaction.builder()
                            .id(1)
                            .date(LocalDateTime.now().minusMonths(2))
                            .type(TransactionType.EXPENSE)
                            .amount(new BigDecimal("5000.00"))
                            .build();

            BondOperation buyOperation =
                    BondOperation.builder()
                            .id(1)
                            .bond(bond)
                            .operationType(OperationType.BUY)
                            .quantity(new BigDecimal("100.00"))
                            .unitPrice(new BigDecimal("50.00"))
                            .walletTransaction(buyTx)
                            .build();

            when(walletService.getAllWalletsOrderedByName()).thenReturn(List.of(wallet1));
            when(walletTransactionService.getFirstTransactionDate(anyInt()))
                    .thenReturn(LocalDateTime.now().minusMonths(3));
            when(recurringTransactionService.getFutureTransactionsByMonthForAnalysis(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(creditCardService.getDebtAtDate(any())).thenReturn(BigDecimal.ZERO);
            when(creditCardService.getEffectivePaidPaymentsByMonth(anyInt(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(tickerService.getAllPurchases()).thenReturn(Collections.emptyList());
            when(tickerService.getAllSales()).thenReturn(Collections.emptyList());
            when(tickerService.getAllTickers()).thenReturn(Collections.emptyList());
            when(bondService.getOperationsByDateBefore(any())).thenReturn(List.of(buyOperation));
            when(walletTransactionService.getTransactionsByWalletAfterDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());
            when(recurringTransactionService.getAllByType(any()))
                    .thenReturn(Collections.emptyList());

            CompletableFuture<Void> result = netWorthCalculationService.recalculateAllSnapshots();
            result.get();

            verify(netWorthSnapshotService, atLeastOnce())
                    .saveSnapshot(
                            anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
        }
    }
}
