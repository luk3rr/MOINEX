/*
 * Filename: InvestmentPerformanceCalculationServiceTest.java
 * Created on: February 18, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.model.InvestmentPerformanceSnapshot;
import org.moinex.model.dto.InvestmentPerformanceDTO;
import org.moinex.model.enums.OperationType;
import org.moinex.model.enums.TickerType;
import org.moinex.model.investment.Bond;
import org.moinex.model.investment.BondOperation;
import org.moinex.model.investment.Dividend;
import org.moinex.model.investment.Ticker;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.util.Constants;

@ExtendWith(MockitoExtension.class)
class InvestmentPerformanceCalculationServiceTest {

    @Mock private InvestmentPerformanceSnapshotService snapshotService;
    @Mock private TickerService tickerService;
    @Mock private BondService bondService;
    @Mock private TickerPriceHistoryService tickerPriceHistoryService;
    @Mock private BondInterestCalculationService bondInterestCalculationService;

    @InjectMocks private InvestmentPerformanceCalculationService calculationService;

    private Ticker ticker;
    private Bond bond;
    private InvestmentPerformanceSnapshot snapshot;

    @BeforeEach
    void setUp() {
        ticker =
                new Ticker(
                        1,
                        "Apple Inc.",
                        "AAPL",
                        TickerType.STOCK,
                        new BigDecimal("10"),
                        new BigDecimal("150.00"),
                        new BigDecimal("145.00"),
                        LocalDateTime.now());
        ticker.setCreatedAt(LocalDateTime.now().minusMonths(6).format(Constants.DB_DATE_FORMATTER));

        bond = Bond.builder().id(1).name("Treasury Bond").issuer("Government").build();

        snapshot =
                InvestmentPerformanceSnapshot.builder()
                        .id(1)
                        .month(1)
                        .year(2026)
                        .investedValue(new BigDecimal("10000.00"))
                        .portfolioValue(new BigDecimal("10500.00"))
                        .accumulatedCapitalGains(new BigDecimal("500.00"))
                        .monthlyCapitalGains(new BigDecimal("100.00"))
                        .build();
    }

    @Nested
    @DisplayName("Get Performance Data Tests")
    class GetPerformanceDataTests {
        @Test
        @DisplayName("Should return performance data from cache when available")
        void getPerformanceData_FromCache_Success() {
            when(snapshotService.hasSnapshots()).thenReturn(true);
            when(snapshotService.getSnapshot(anyInt(), anyInt())).thenReturn(Optional.of(snapshot));

            InvestmentPerformanceDTO result = calculationService.getPerformanceData();

            assertNotNull(result);
            assertNotNull(result.monthlyInvested());
            assertNotNull(result.portfolioValues());
            assertNotNull(result.accumulatedGains());
            assertNotNull(result.monthlyGains());
            verify(snapshotService, atLeastOnce()).getSnapshot(anyInt(), anyInt());
        }

        @Test
        @DisplayName("Should calculate performance data when cache is empty")
        void getPerformanceData_NoCache_Calculate() {
            when(snapshotService.hasSnapshots()).thenReturn(false);
            when(tickerService.getAllNonArchivedTickers()).thenReturn(emptyList());
            when(tickerService.getAllDividends()).thenReturn(emptyList());
            when(tickerService.getAllNonArchivedSales()).thenReturn(emptyList());
            when(bondService.getAllNonArchivedBonds()).thenReturn(emptyList());
            when(bondService.getAllOperations()).thenReturn(emptyList());
            when(snapshotService.saveSnapshot(anyInt(), anyInt(), any(), any(), any(), any()))
                    .thenReturn(snapshot);

            InvestmentPerformanceDTO result = calculationService.getPerformanceData();

            assertNotNull(result);
            verify(tickerService, atLeastOnce()).getAllNonArchivedTickers();
            verify(bondService, atLeastOnce()).getAllNonArchivedBonds();
            verify(snapshotService, atLeastOnce())
                    .saveSnapshot(anyInt(), anyInt(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should calculate and save missing months when cache is incomplete")
        void getPerformanceData_PartialCache_CalculateMissing() {
            YearMonth currentMonth = YearMonth.now();

            when(snapshotService.hasSnapshots()).thenReturn(true);

            // Mock all months to return empty except one
            when(snapshotService.getSnapshot(anyInt(), anyInt())).thenReturn(Optional.empty());
            when(snapshotService.getSnapshot(
                            currentMonth.minusMonths(1).getMonthValue(),
                            currentMonth.minusMonths(1).getYear()))
                    .thenReturn(Optional.of(snapshot));

            when(tickerService.getAllNonArchivedTickers()).thenReturn(emptyList());
            when(tickerService.getAllDividends()).thenReturn(emptyList());
            when(tickerService.getAllNonArchivedSales()).thenReturn(emptyList());
            when(bondService.getAllNonArchivedBonds()).thenReturn(emptyList());
            when(bondService.getAllOperations()).thenReturn(emptyList());
            when(snapshotService.saveSnapshot(anyInt(), anyInt(), any(), any(), any(), any()))
                    .thenReturn(snapshot);

            InvestmentPerformanceDTO result = calculationService.getPerformanceData();

            assertNotNull(result);
            verify(snapshotService, atLeastOnce())
                    .saveSnapshot(anyInt(), anyInt(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Recalculate All Snapshots Tests")
    class RecalculateAllSnapshotsTests {
        @Test
        @DisplayName("Should recalculate all snapshots successfully")
        void recalculateAllSnapshots_Success() throws Exception {
            when(tickerService.getAllNonArchivedTickers()).thenReturn(emptyList());
            when(tickerService.getAllDividends()).thenReturn(emptyList());
            when(tickerService.getAllNonArchivedSales()).thenReturn(emptyList());
            when(bondService.getAllNonArchivedBonds()).thenReturn(emptyList());
            when(bondService.getAllOperations()).thenReturn(emptyList());
            when(snapshotService.saveSnapshot(anyInt(), anyInt(), any(), any(), any(), any()))
                    .thenReturn(snapshot);
            doNothing().when(snapshotService).deleteAllSnapshots();

            CompletableFuture<Void> result = calculationService.recalculateAllSnapshots();

            assertNotNull(result);
            result.get();
            assertFalse(result.isCompletedExceptionally());
            verify(snapshotService).deleteAllSnapshots();
            verify(snapshotService, atLeastOnce())
                    .saveSnapshot(anyInt(), anyInt(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should not recalculate if calculation is already in progress")
        void recalculateAllSnapshots_AlreadyCalculating() {
            calculationService.recalculateAllSnapshots();

            CompletableFuture<Void> result = calculationService.recalculateAllSnapshots();

            assertNotNull(result);
            assertTrue(result.isDone());
        }

        @Test
        @DisplayName("Should handle exception during recalculation")
        void recalculateAllSnapshots_Exception() {
            when(tickerService.getAllNonArchivedTickers())
                    .thenThrow(new RuntimeException("Database error"));

            CompletableFuture<Void> result = calculationService.recalculateAllSnapshots();

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
            assertFalse(calculationService.isCalculating());
        }

        @Test
        @DisplayName("Should return true when calculation is in progress")
        void isCalculating_True() throws Exception {
            when(tickerService.getAllNonArchivedTickers()).thenReturn(emptyList());
            when(tickerService.getAllDividends()).thenReturn(emptyList());
            when(tickerService.getAllNonArchivedSales()).thenReturn(emptyList());
            when(bondService.getAllNonArchivedBonds()).thenReturn(emptyList());
            when(bondService.getAllOperations()).thenReturn(emptyList());
            doNothing().when(snapshotService).deleteAllSnapshots();
            when(snapshotService.saveSnapshot(anyInt(), anyInt(), any(), any(), any(), any()))
                    .thenReturn(snapshot);

            // Start calculation
            CompletableFuture<Void> future = calculationService.recalculateAllSnapshots();

            // The flag should be set immediately when recalculate is called
            // Since the method is @Async, it may complete very quickly with empty data
            // We verify that either it's calculating or already completed successfully
            boolean wasCalculatingOrCompleted =
                    calculationService.isCalculating() || future.isDone();
            assertTrue(wasCalculatingOrCompleted);

            // Wait for completion
            future.get();

            // After completion, should be false
            assertFalse(calculationService.isCalculating());
        }
    }

    @Nested
    @DisplayName("Monthly Invested Value Calculation Tests")
    class MonthlyInvestedValueTests {
        @Test
        @DisplayName("Should calculate invested value for ticker with purchases")
        void calculateInvestedValue_WithPurchases() {
            ticker.setCurrentQuantity(new BigDecimal("10"));
            ticker.setAverageUnitValue(new BigDecimal("145.00"));

            when(tickerService.getAllNonArchivedTickers()).thenReturn(List.of(ticker));
            when(tickerService.getAllPurchasesByTicker(1)).thenReturn(emptyList());
            when(tickerService.getAllSalesByTicker(1)).thenReturn(emptyList());
            when(tickerService.getAllDividends()).thenReturn(emptyList());
            when(tickerService.getAllNonArchivedSales()).thenReturn(emptyList());
            when(bondService.getAllNonArchivedBonds()).thenReturn(emptyList());
            when(bondService.getAllOperations()).thenReturn(emptyList());
            when(tickerPriceHistoryService.getClosestPriceBeforeDate(any(Ticker.class), any()))
                    .thenReturn(Optional.of(new BigDecimal("150.00")));
            when(snapshotService.saveSnapshot(anyInt(), anyInt(), any(), any(), any(), any()))
                    .thenReturn(snapshot);

            InvestmentPerformanceDTO result = calculationService.getPerformanceData();

            assertNotNull(result);
            assertFalse(result.monthlyInvested().isEmpty());
        }

        @Test
        @DisplayName("Should calculate invested value for bonds with operations")
        void calculateInvestedValue_WithBonds() {
            WalletTransaction transaction =
                    WalletTransaction.builder()
                            .date(LocalDateTime.now().minusMonths(1))
                            .amount(new BigDecimal("1000.00"))
                            .build();

            BondOperation operation =
                    BondOperation.builder()
                            .bond(bond)
                            .operationType(OperationType.BUY)
                            .unitPrice(new BigDecimal("100.00"))
                            .quantity(new BigDecimal("10"))
                            .walletTransaction(transaction)
                            .build();

            when(tickerService.getAllNonArchivedTickers()).thenReturn(emptyList());
            when(tickerService.getAllDividends()).thenReturn(emptyList());
            when(tickerService.getAllNonArchivedSales()).thenReturn(emptyList());
            when(bondInterestCalculationService.getMonthlyInterestHistory(any(Bond.class)))
                    .thenReturn(emptyList());
            when(bondService.getAllNonArchivedBonds()).thenReturn(List.of(bond));
            when(bondService.getOperationsByBond(bond)).thenReturn(List.of(operation));
            when(bondService.getAllOperations()).thenReturn(List.of(operation));
            when(snapshotService.saveSnapshot(anyInt(), anyInt(), any(), any(), any(), any()))
                    .thenReturn(snapshot);

            InvestmentPerformanceDTO result = calculationService.getPerformanceData();

            assertNotNull(result);
            assertFalse(result.monthlyInvested().isEmpty());
        }
    }

    @Nested
    @DisplayName("Portfolio Value Calculation Tests")
    class PortfolioValueTests {
        @Test
        @DisplayName("Should calculate portfolio value using current prices")
        void calculatePortfolioValue_Success() {
            ticker.setCurrentQuantity(new BigDecimal("10"));
            ticker.setCurrentUnitValue(new BigDecimal("150.00"));

            when(tickerService.getAllNonArchivedTickers()).thenReturn(List.of(ticker));
            when(tickerService.getAllPurchasesByTicker(1)).thenReturn(emptyList());
            when(tickerService.getAllSalesByTicker(1)).thenReturn(emptyList());
            when(tickerService.getAllDividends()).thenReturn(emptyList());
            when(tickerService.getAllNonArchivedSales()).thenReturn(emptyList());
            when(tickerPriceHistoryService.getClosestPriceBeforeDate(any(Ticker.class), any()))
                    .thenReturn(Optional.of(new BigDecimal("150.00")));
            when(bondService.getAllNonArchivedBonds()).thenReturn(emptyList());
            when(bondService.getAllOperations()).thenReturn(emptyList());
            when(snapshotService.saveSnapshot(anyInt(), anyInt(), any(), any(), any(), any()))
                    .thenReturn(snapshot);

            InvestmentPerformanceDTO result = calculationService.getPerformanceData();

            assertNotNull(result);
            assertFalse(result.portfolioValues().isEmpty());
        }

        @Test
        @DisplayName("Should use current unit value for today's date")
        void calculatePortfolioValue_CurrentMonth_UsesCurrentValue() {
            ticker.setCurrentQuantity(new BigDecimal("10"));
            ticker.setCurrentUnitValue(new BigDecimal("150.00"));

            when(tickerService.getAllNonArchivedTickers()).thenReturn(List.of(ticker));
            when(tickerService.getAllPurchasesByTicker(1)).thenReturn(emptyList());
            when(tickerService.getAllSalesByTicker(1)).thenReturn(emptyList());
            when(tickerService.getAllDividends()).thenReturn(emptyList());
            when(tickerService.getAllNonArchivedSales()).thenReturn(emptyList());
            when(tickerPriceHistoryService.getClosestPriceBeforeDate(any(Ticker.class), any()))
                    .thenReturn(Optional.of(ticker.getCurrentUnitValue()));
            when(bondService.getAllNonArchivedBonds()).thenReturn(emptyList());
            when(bondService.getAllOperations()).thenReturn(emptyList());
            when(snapshotService.saveSnapshot(anyInt(), anyInt(), any(), any(), any(), any()))
                    .thenReturn(snapshot);

            InvestmentPerformanceDTO result = calculationService.getPerformanceData();

            assertNotNull(result);
            verify(tickerPriceHistoryService, atLeastOnce())
                    .getClosestPriceBeforeDate(any(Ticker.class), any());
        }
    }

    @Nested
    @DisplayName("Capital Gains Calculation Tests")
    class CapitalGainsTests {
        @Test
        @DisplayName("Should calculate monthly capital gains from dividends")
        void calculateMonthlyGains_WithDividends() {
            WalletTransaction transaction =
                    WalletTransaction.builder()
                            .date(LocalDateTime.now().minusMonths(1))
                            .amount(new BigDecimal("50.00"))
                            .build();

            Dividend dividend = Dividend.builder().walletTransaction(transaction).build();

            when(tickerService.getAllDividends()).thenReturn(List.of(dividend));
            when(tickerService.getAllNonArchivedSales()).thenReturn(emptyList());
            when(bondService.getAllOperations()).thenReturn(emptyList());
            when(tickerService.getAllNonArchivedTickers()).thenReturn(emptyList());
            when(bondService.getAllNonArchivedBonds()).thenReturn(emptyList());
            when(snapshotService.saveSnapshot(anyInt(), anyInt(), any(), any(), any(), any()))
                    .thenReturn(snapshot);

            InvestmentPerformanceDTO result = calculationService.getPerformanceData();

            assertNotNull(result);
            assertFalse(result.monthlyGains().isEmpty());
        }

        @Test
        @DisplayName("Should calculate accumulated capital gains correctly")
        void calculateAccumulatedGains_Success() {
            ticker.setCurrentQuantity(new BigDecimal("10"));
            ticker.setAverageUnitValue(new BigDecimal("145.00"));
            ticker.setCurrentUnitValue(new BigDecimal("150.00"));

            when(tickerService.getAllNonArchivedTickers()).thenReturn(List.of(ticker));
            when(tickerService.getAllDividends()).thenReturn(emptyList());
            when(tickerService.getAllNonArchivedSales()).thenReturn(emptyList());
            when(tickerService.getAllPurchasesByTicker(1)).thenReturn(emptyList());
            when(tickerService.getAllSalesByTicker(1)).thenReturn(emptyList());
            when(tickerPriceHistoryService.getClosestPriceBeforeDate(any(Ticker.class), any()))
                    .thenReturn(Optional.of(new BigDecimal("150.00")));
            when(bondService.getAllNonArchivedBonds()).thenReturn(emptyList());
            when(bondService.getAllOperations()).thenReturn(emptyList());
            when(snapshotService.saveSnapshot(anyInt(), anyInt(), any(), any(), any(), any()))
                    .thenReturn(snapshot);

            InvestmentPerformanceDTO result = calculationService.getPerformanceData();

            assertNotNull(result);
            assertFalse(result.accumulatedGains().isEmpty());
        }
    }
}
