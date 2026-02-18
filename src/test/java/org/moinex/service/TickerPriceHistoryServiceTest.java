/*
 * Filename: TickerPriceHistoryServiceTest.java
 * Created on: February 18, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.model.enums.TickerType;
import org.moinex.model.investment.Ticker;
import org.moinex.model.investment.TickerPriceHistory;
import org.moinex.repository.investment.TickerPriceHistoryRepository;
import org.moinex.repository.investment.TickerPurchaseRepository;
import org.moinex.repository.investment.TickerRepository;
import org.moinex.repository.investment.TickerSaleRepository;
import org.moinex.util.Constants;

@ExtendWith(MockitoExtension.class)
class TickerPriceHistoryServiceTest {

    @Mock private TickerPriceHistoryRepository priceHistoryRepository;
    @Mock private TickerRepository tickerRepository;
    @Mock private TickerPurchaseRepository tickerPurchaseRepository;
    @Mock private TickerSaleRepository tickerSaleRepository;

    @InjectMocks private TickerPriceHistoryService tickerPriceHistoryService;

    private Ticker ticker;
    private TickerPriceHistory priceHistory;

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

        priceHistory =
                TickerPriceHistory.builder()
                        .id(1)
                        .ticker(ticker)
                        .priceDate(
                                LocalDate.now()
                                        .minusDays(1)
                                        .format(Constants.DATE_FORMATTER_NO_TIME))
                        .closingPrice(new BigDecimal("145.00"))
                        .isMonthEnd(false)
                        .build();
    }

    @Nested
    @DisplayName("Store Price History Tests")
    class StorePriceHistoryTests {
        @Test
        @DisplayName("Should store new price history entry")
        void storePriceHistory_NewEntry_Success() {
            LocalDate priceDate = LocalDate.now().minusDays(1);
            BigDecimal price = new BigDecimal("150.00");

            when(priceHistoryRepository.findByTickerIdAndDate(anyInt(), anyString()))
                    .thenReturn(Optional.empty());
            when(priceHistoryRepository.save(any(TickerPriceHistory.class)))
                    .thenReturn(priceHistory);

            tickerPriceHistoryService.storePriceHistory(ticker, priceDate, price, false);

            ArgumentCaptor<TickerPriceHistory> captor =
                    ArgumentCaptor.forClass(TickerPriceHistory.class);
            verify(priceHistoryRepository).save(captor.capture());

            TickerPriceHistory saved = captor.getValue();
            assertEquals(ticker, saved.getTicker());
            assertEquals(price, saved.getClosingPrice());
            assertFalse(saved.isMonthEnd());
        }

        @Test
        @DisplayName("Should update existing price history entry")
        void storePriceHistory_ExistingEntry_Update() {
            LocalDate priceDate = LocalDate.now().minusDays(1);
            BigDecimal newPrice = new BigDecimal("155.00");

            when(priceHistoryRepository.findByTickerIdAndDate(anyInt(), anyString()))
                    .thenReturn(Optional.of(priceHistory));
            when(priceHistoryRepository.save(any(TickerPriceHistory.class)))
                    .thenReturn(priceHistory);

            tickerPriceHistoryService.storePriceHistory(ticker, priceDate, newPrice, true);

            verify(priceHistoryRepository).save(priceHistory);
            assertEquals(newPrice, priceHistory.getClosingPrice());
            assertTrue(priceHistory.isMonthEnd());
        }
    }

    @Nested
    @DisplayName("Get Closest Price Before Date Tests")
    class GetClosestPriceBeforeDateTests {
        @Test
        @DisplayName("Should return current unit value for today")
        void getClosestPriceBeforeDate_Today_ReturnsCurrentValue() {
            LocalDate today = LocalDate.now();

            Optional<BigDecimal> result =
                    tickerPriceHistoryService.getClosestPriceBeforeDate(ticker, today);

            assertTrue(result.isPresent());
            assertEquals(ticker.getCurrentUnitValue(), result.get());
            verify(priceHistoryRepository, never())
                    .findMostRecentPriceBeforeDate(anyInt(), anyString());
        }

        @Test
        @DisplayName("Should return historical price for past date")
        void getClosestPriceBeforeDate_PastDate_ReturnsHistoricalPrice() {
            LocalDate pastDate = LocalDate.now().minusDays(5);
            BigDecimal historicalPrice = new BigDecimal("140.00");

            TickerPriceHistory historicalEntry =
                    TickerPriceHistory.builder()
                            .ticker(ticker)
                            .priceDate(pastDate.format(Constants.DATE_FORMATTER_NO_TIME))
                            .closingPrice(historicalPrice)
                            .build();

            when(priceHistoryRepository.findMostRecentPriceBeforeDate(anyInt(), anyString()))
                    .thenReturn(Optional.of(historicalEntry));

            Optional<BigDecimal> result =
                    tickerPriceHistoryService.getClosestPriceBeforeDate(ticker, pastDate);

            assertTrue(result.isPresent());
            assertEquals(historicalPrice, result.get());
        }

        @Test
        @DisplayName("Should return empty when no historical price exists")
        void getClosestPriceBeforeDate_NoHistory_ReturnsEmpty() {
            LocalDate pastDate = LocalDate.now().minusDays(5);

            when(priceHistoryRepository.findMostRecentPriceBeforeDate(anyInt(), anyString()))
                    .thenReturn(Optional.empty());

            Optional<BigDecimal> result =
                    tickerPriceHistoryService.getClosestPriceBeforeDate(ticker, pastDate);

            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("Has Complete Historical Data Tests")
    class HasCompleteHistoricalDataTests {
        @Test
        @DisplayName("Should return true when no purchases exist")
        void hasCompleteHistoricalData_NoPurchases_ReturnsTrue() {
            boolean result = tickerPriceHistoryService.hasCompleteHistoricalData(1, null);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return true when earliest data is before first purchase")
        void hasCompleteHistoricalData_DataBeforePurchase_ReturnsTrue() {
            LocalDate firstPurchaseDate = LocalDate.now().minusMonths(3);
            LocalDate earliestDataDate = LocalDate.now().minusMonths(4);

            TickerPriceHistory earliestEntry =
                    TickerPriceHistory.builder()
                            .priceDate(earliestDataDate.format(Constants.DATE_FORMATTER_NO_TIME))
                            .build();

            when(priceHistoryRepository.findEarliestPriceByTicker(1))
                    .thenReturn(Optional.of(earliestEntry));

            boolean result =
                    tickerPriceHistoryService.hasCompleteHistoricalData(1, firstPurchaseDate);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when no historical data exists")
        void hasCompleteHistoricalData_NoData_ReturnsFalse() {
            LocalDate firstPurchaseDate = LocalDate.now().minusMonths(3);

            when(priceHistoryRepository.findEarliestPriceByTicker(1)).thenReturn(Optional.empty());

            boolean result =
                    tickerPriceHistoryService.hasCompleteHistoricalData(1, firstPurchaseDate);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when earliest data is after first purchase")
        void hasCompleteHistoricalData_DataAfterPurchase_ReturnsFalse() {
            LocalDate firstPurchaseDate = LocalDate.now().minusMonths(4);
            LocalDate earliestDataDate = LocalDate.now().minusMonths(3);

            TickerPriceHistory earliestEntry =
                    TickerPriceHistory.builder()
                            .priceDate(earliestDataDate.format(Constants.DATE_FORMATTER_NO_TIME))
                            .build();

            when(priceHistoryRepository.findEarliestPriceByTicker(1))
                    .thenReturn(Optional.of(earliestEntry));

            boolean result =
                    tickerPriceHistoryService.hasCompleteHistoricalData(1, firstPurchaseDate);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Get Earliest Price Date Tests")
    class GetEarliestPriceDateTests {
        @Test
        @DisplayName("Should return earliest price date when data exists")
        void getEarliestPriceDate_DataExists_ReturnsDate() {
            LocalDate earliestDate = LocalDate.now().minusMonths(6);
            TickerPriceHistory earliestEntry =
                    TickerPriceHistory.builder()
                            .priceDate(earliestDate.format(Constants.DATE_FORMATTER_NO_TIME))
                            .build();

            when(priceHistoryRepository.findEarliestPriceByTicker(1))
                    .thenReturn(Optional.of(earliestEntry));

            Optional<LocalDate> result = tickerPriceHistoryService.getEarliestPriceDate(1);

            assertTrue(result.isPresent());
            assertEquals(earliestDate, result.get());
        }

        @Test
        @DisplayName("Should return empty when no data exists")
        void getEarliestPriceDate_NoData_ReturnsEmpty() {
            when(priceHistoryRepository.findEarliestPriceByTicker(1)).thenReturn(Optional.empty());

            Optional<LocalDate> result = tickerPriceHistoryService.getEarliestPriceDate(1);

            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("Initialize Price History Tests")
    class InitializePriceHistoryTests {
        @Test
        @DisplayName("Should initialize price history for tickers without data")
        void initializePriceHistory_NoData_Success() throws Exception {
            when(tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc())
                    .thenReturn(List.of(ticker));
            when(tickerPurchaseRepository.findAll()).thenReturn(Collections.emptyList());

            var result =
                    tickerPriceHistoryService.initializePriceHistory(
                            tickerPurchaseRepository, tickerSaleRepository);

            assertNotNull(result);
            result.get();
            assertFalse(result.isCompletedExceptionally());
        }

        @Test
        @DisplayName("Should skip archived tickers")
        void initializePriceHistory_ArchivedTicker_Skipped() throws Exception {
            ticker.setArchived(true);

            when(tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc())
                    .thenReturn(List.of(ticker));

            var result =
                    tickerPriceHistoryService.initializePriceHistory(
                            tickerPurchaseRepository, tickerSaleRepository);

            assertNotNull(result);
            result.get();
            verify(priceHistoryRepository, never()).findEarliestPriceByTicker(anyInt());
        }
    }
}
