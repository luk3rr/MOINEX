/*
 * Filename: FundamentalAnalysisServiceTest.java
 * Created on: January 10, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.error.MoinexException;
import org.moinex.model.enums.PeriodType;
import org.moinex.model.enums.TickerType;
import org.moinex.model.investment.FundamentalAnalysis;
import org.moinex.model.investment.Ticker;
import org.moinex.repository.investment.FundamentalAnalysisRepository;
import org.moinex.repository.investment.TickerRepository;
import org.moinex.util.APIUtils;
import org.moinex.util.Constants;

@ExtendWith(MockitoExtension.class)
class FundamentalAnalysisServiceTest {

    @Mock private FundamentalAnalysisRepository fundamentalAnalysisRepository;

    @Mock private TickerRepository tickerRepository;

    @InjectMocks private FundamentalAnalysisService fundamentalAnalysisService;

    private Ticker stockTicker;
    private Ticker reitTicker;
    private Ticker cryptoTicker;
    private FundamentalAnalysis analysis;
    private JSONObject validApiResponse;
    private JSONObject partialDataResponse;
    private JSONObject errorResponse;

    @BeforeEach
    void setUp() throws Exception {
        stockTicker =
                Ticker.builder()
                        .id(1)
                        .symbol("PETR4.SA")
                        .name("Petrobras")
                        .type(TickerType.STOCK)
                        .isArchived(false)
                        .build();

        reitTicker =
                Ticker.builder()
                        .id(2)
                        .symbol("BPFF11.SA")
                        .name("Brasil Plural FII")
                        .type(TickerType.REIT)
                        .isArchived(false)
                        .build();

        cryptoTicker =
                Ticker.builder()
                        .id(3)
                        .symbol("BTC-USD")
                        .name("Bitcoin")
                        .type(TickerType.CRYPTOCURRENCY)
                        .isArchived(false)
                        .build();

        analysis =
                FundamentalAnalysis.builder()
                        .id(1)
                        .ticker(stockTicker)
                        .periodType(PeriodType.ANNUAL)
                        .companyName("Petrobras")
                        .sector("Energy")
                        .industry("Oil & Gas")
                        .currency("BRL")
                        .dataJson("{}")
                        .lastUpdate(LocalDateTime.now().format(Constants.DB_DATE_FORMATTER))
                        .createdAt(LocalDateTime.now().format(Constants.DB_DATE_FORMATTER))
                        .build();

        // Valid API response with all data
        validApiResponse = new JSONObject();
        JSONObject tickerData = new JSONObject();
        tickerData.put("symbol", "PETR4.SA");
        tickerData.put("company_name", "Petrobras");
        tickerData.put("sector", "Energy");
        tickerData.put("industry", "Oil & Gas");
        tickerData.put("currency", "BRL");
        tickerData.put("period_type", "annual");
        tickerData.put("last_updated", LocalDateTime.now().toString());
        tickerData.put("profitability", new JSONObject());
        tickerData.put("valuation", new JSONObject());
        validApiResponse.put("PETR4.SA", tickerData);

        // Partial data response (REIT with error but price_performance available)
        partialDataResponse = new JSONObject();
        JSONObject reitData = new JSONObject();
        reitData.put("symbol", "BPFF11.SA");
        reitData.put("company_name", "Brasil Plural FII");
        reitData.put("sector", "Real Estate");
        reitData.put("industry", "REIT - Diversified");
        reitData.put("currency", "BRL");
        reitData.put("period_type", "annual");
        reitData.put("last_updated", LocalDateTime.now().toString());
        reitData.put("error", "Financial data not available");
        reitData.put("price_performance", new JSONObject().put("current_price", 54.52));
        partialDataResponse.put("BPFF11.SA", reitData);

        // Error response
        errorResponse = new JSONObject();
        JSONObject errorData = new JSONObject();
        errorData.put("error", "Symbol not found");
        errorResponse.put("INVALID.SA", errorData);
    }

    @Nested
    @DisplayName("Get Analysis Tests")
    class GetAnalysisTests {

        @Test
        @DisplayName("Should return cached analysis when cache is valid")
        void getAnalysis_ValidCache_ReturnsCached() throws MoinexException {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(stockTicker));
            when(fundamentalAnalysisRepository.findByTickerAndPeriodType(
                            stockTicker, PeriodType.ANNUAL))
                    .thenReturn(Optional.of(analysis));

            FundamentalAnalysis result =
                    fundamentalAnalysisService.getAnalysis(1, PeriodType.ANNUAL, false);

            assertNotNull(result);
            assertEquals(analysis.getId(), result.getId());
            verify(fundamentalAnalysisRepository, times(1))
                    .findByTickerAndPeriodType(stockTicker, PeriodType.ANNUAL);
            verify(fundamentalAnalysisRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should fetch new data when cache is expired")
        void getAnalysis_ExpiredCache_FetchesNew() throws Exception {
            FundamentalAnalysis expiredAnalysis =
                    FundamentalAnalysis.builder()
                            .id(1)
                            .ticker(stockTicker)
                            .periodType(PeriodType.ANNUAL)
                            .lastUpdate(
                                    LocalDateTime.now()
                                            .minusHours(25)
                                            .format(Constants.DB_DATE_FORMATTER))
                            .dataJson("{}")
                            .createdAt(LocalDateTime.now().format(Constants.DB_DATE_FORMATTER))
                            .build();

            when(tickerRepository.findById(1)).thenReturn(Optional.of(stockTicker));
            when(fundamentalAnalysisRepository.findByTickerAndPeriodType(
                            stockTicker, PeriodType.ANNUAL))
                    .thenReturn(Optional.of(expiredAnalysis));

            try (MockedStatic<APIUtils> apiUtilsMock = mockStatic(APIUtils.class)) {
                apiUtilsMock
                        .when(
                                () ->
                                        APIUtils.fetchFundamentalAnalysisAsync(
                                                "PETR4.SA", PeriodType.ANNUAL))
                        .thenReturn(CompletableFuture.completedFuture(validApiResponse));

                when(fundamentalAnalysisRepository.save(any(FundamentalAnalysis.class)))
                        .thenReturn(expiredAnalysis);

                FundamentalAnalysis result =
                        fundamentalAnalysisService.getAnalysis(1, PeriodType.ANNUAL, false);

                assertNotNull(result);
                verify(fundamentalAnalysisRepository, times(1))
                        .save(any(FundamentalAnalysis.class));
            }
        }

        @Test
        @DisplayName("Should fetch new data when forceRefresh is true")
        void getAnalysis_ForceRefresh_FetchesNew() throws Exception {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(stockTicker));
            when(fundamentalAnalysisRepository.findByTickerAndPeriodType(
                            stockTicker, PeriodType.ANNUAL))
                    .thenReturn(Optional.empty());

            try (MockedStatic<APIUtils> apiUtilsMock = mockStatic(APIUtils.class)) {
                apiUtilsMock
                        .when(
                                () ->
                                        APIUtils.fetchFundamentalAnalysisAsync(
                                                "PETR4.SA", PeriodType.ANNUAL))
                        .thenReturn(CompletableFuture.completedFuture(validApiResponse));

                when(fundamentalAnalysisRepository.save(any(FundamentalAnalysis.class)))
                        .thenReturn(analysis);

                FundamentalAnalysis result =
                        fundamentalAnalysisService.getAnalysis(1, PeriodType.ANNUAL, true);

                assertNotNull(result);
                verify(fundamentalAnalysisRepository, times(1))
                        .save(any(FundamentalAnalysis.class));
            }
        }

        @Test
        @DisplayName("Should throw exception when ticker not found")
        void getAnalysis_TickerNotFound_ThrowsException() {
            when(tickerRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> fundamentalAnalysisService.getAnalysis(999, PeriodType.ANNUAL, false));
        }

        @Test
        @DisplayName("Should throw exception when ticker is archived")
        void getAnalysis_ArchivedTicker_ThrowsException() {
            Ticker archivedTicker =
                    Ticker.builder()
                            .id(1)
                            .symbol("PETR4.SA")
                            .type(TickerType.STOCK)
                            .isArchived(true)
                            .build();

            when(tickerRepository.findById(1)).thenReturn(Optional.of(archivedTicker));

            assertThrows(
                    MoinexException.class,
                    () -> fundamentalAnalysisService.getAnalysis(1, PeriodType.ANNUAL, false));
        }

        @Test
        @DisplayName("Should throw exception when ticker type is invalid")
        void getAnalysis_InvalidTickerType_ThrowsException() {
            when(tickerRepository.findById(3)).thenReturn(Optional.of(cryptoTicker));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> fundamentalAnalysisService.getAnalysis(3, PeriodType.ANNUAL, false));
        }
    }

    @Nested
    @DisplayName("Fetch and Save Analysis Tests")
    class FetchAndSaveAnalysisTests {

        @Test
        @DisplayName("Should save analysis successfully with valid API response")
        void fetchAndSaveAnalysis_ValidResponse_Success() throws Exception {
            when(fundamentalAnalysisRepository.findByTickerAndPeriodType(
                            stockTicker, PeriodType.ANNUAL))
                    .thenReturn(Optional.empty());

            try (MockedStatic<APIUtils> apiUtilsMock = mockStatic(APIUtils.class)) {
                apiUtilsMock
                        .when(
                                () ->
                                        APIUtils.fetchFundamentalAnalysisAsync(
                                                "PETR4.SA", PeriodType.ANNUAL))
                        .thenReturn(CompletableFuture.completedFuture(validApiResponse));

                when(fundamentalAnalysisRepository.save(any(FundamentalAnalysis.class)))
                        .thenReturn(analysis);

                FundamentalAnalysis result =
                        fundamentalAnalysisService.fetchAndSaveAnalysis(
                                stockTicker, PeriodType.ANNUAL);

                assertNotNull(result);
                verify(fundamentalAnalysisRepository, times(1))
                        .save(any(FundamentalAnalysis.class));
            }
        }

        @Test
        @DisplayName("Should save partial data when financial data not available")
        void fetchAndSaveAnalysis_PartialData_Success() throws Exception {
            when(fundamentalAnalysisRepository.findByTickerAndPeriodType(
                            reitTicker, PeriodType.ANNUAL))
                    .thenReturn(Optional.empty());

            try (MockedStatic<APIUtils> apiUtilsMock = mockStatic(APIUtils.class)) {
                apiUtilsMock
                        .when(
                                () ->
                                        APIUtils.fetchFundamentalAnalysisAsync(
                                                "BPFF11.SA", PeriodType.ANNUAL))
                        .thenReturn(CompletableFuture.completedFuture(partialDataResponse));

                FundamentalAnalysis reitAnalysis =
                        FundamentalAnalysis.builder()
                                .id(2)
                                .ticker(reitTicker)
                                .periodType(PeriodType.ANNUAL)
                                .companyName("Brasil Plural FII")
                                .dataJson(partialDataResponse.getJSONObject("BPFF11.SA").toString())
                                .build();

                when(fundamentalAnalysisRepository.save(any(FundamentalAnalysis.class)))
                        .thenReturn(reitAnalysis);

                FundamentalAnalysis result =
                        fundamentalAnalysisService.fetchAndSaveAnalysis(
                                reitTicker, PeriodType.ANNUAL);

                assertNotNull(result);
                verify(fundamentalAnalysisRepository, times(1))
                        .save(any(FundamentalAnalysis.class));
            }
        }

        @Test
        @DisplayName("Should throw exception for permanent errors like symbol not found")
        void fetchAndSaveAnalysis_SymbolNotFound_ThrowsException() throws Exception {
            Ticker invalidTicker =
                    Ticker.builder()
                            .id(4)
                            .symbol("INVALID.SA")
                            .type(TickerType.STOCK)
                            .isArchived(false)
                            .build();

            try (MockedStatic<APIUtils> apiUtilsMock = mockStatic(APIUtils.class)) {
                apiUtilsMock
                        .when(
                                () ->
                                        APIUtils.fetchFundamentalAnalysisAsync(
                                                "INVALID.SA", PeriodType.ANNUAL))
                        .thenReturn(CompletableFuture.completedFuture(errorResponse));

                assertThrows(
                        MoinexException.class,
                        () ->
                                fundamentalAnalysisService.fetchAndSaveAnalysis(
                                        invalidTicker, PeriodType.ANNUAL));
            }
        }

        @Test
        @DisplayName("Should retry on transient errors and eventually succeed")
        void fetchAndSaveAnalysis_TransientError_RetriesAndSucceeds() throws Exception {
            when(fundamentalAnalysisRepository.findByTickerAndPeriodType(
                            stockTicker, PeriodType.ANNUAL))
                    .thenReturn(Optional.empty());

            try (MockedStatic<APIUtils> apiUtilsMock = mockStatic(APIUtils.class)) {
                // First call fails, second succeeds
                apiUtilsMock
                        .when(
                                () ->
                                        APIUtils.fetchFundamentalAnalysisAsync(
                                                "PETR4.SA", PeriodType.ANNUAL))
                        .thenReturn(
                                CompletableFuture.failedFuture(new RuntimeException("Timeout")),
                                CompletableFuture.completedFuture(validApiResponse));

                when(fundamentalAnalysisRepository.save(any(FundamentalAnalysis.class)))
                        .thenReturn(analysis);

                FundamentalAnalysis result =
                        fundamentalAnalysisService.fetchAndSaveAnalysis(
                                stockTicker, PeriodType.ANNUAL);

                assertNotNull(result);
                verify(fundamentalAnalysisRepository, times(1))
                        .save(any(FundamentalAnalysis.class));
            }
        }

        @Test
        @DisplayName("Should throw exception after max retries exceeded")
        void fetchAndSaveAnalysis_MaxRetriesExceeded_ThrowsException() throws Exception {
            try (MockedStatic<APIUtils> apiUtilsMock = mockStatic(APIUtils.class)) {
                apiUtilsMock
                        .when(
                                () ->
                                        APIUtils.fetchFundamentalAnalysisAsync(
                                                "PETR4.SA", PeriodType.ANNUAL))
                        .thenReturn(
                                CompletableFuture.failedFuture(new RuntimeException("Timeout")));

                assertThrows(
                        MoinexException.class,
                        () ->
                                fundamentalAnalysisService.fetchAndSaveAnalysis(
                                        stockTicker, PeriodType.ANNUAL));
            }
        }
    }

    @Nested
    @DisplayName("Cache Management Tests")
    class CacheManagementTests {

        @Test
        @DisplayName("Should identify expired cache correctly")
        void isCacheExpired_ExpiredCache_ReturnsTrue() {
            FundamentalAnalysis expiredAnalysis =
                    FundamentalAnalysis.builder()
                            .lastUpdate(
                                    LocalDateTime.now()
                                            .minusHours(25)
                                            .format(Constants.DB_DATE_FORMATTER))
                            .build();

            assertTrue(fundamentalAnalysisService.isCacheExpired(expiredAnalysis));
        }

        @Test
        @DisplayName("Should identify valid cache correctly")
        void isCacheExpired_ValidCache_ReturnsFalse() {
            FundamentalAnalysis validAnalysis =
                    FundamentalAnalysis.builder()
                            .lastUpdate(LocalDateTime.now().format(Constants.DB_DATE_FORMATTER))
                            .build();

            assertFalse(fundamentalAnalysisService.isCacheExpired(validAnalysis));
        }
    }

    @Nested
    @DisplayName("Get All Analyses Tests")
    class GetAllAnalysesTests {

        @Test
        @DisplayName("Should return all analyses including archived tickers")
        void getAllAnalyses_ReturnsAll() {
            List<FundamentalAnalysis> analyses = List.of(analysis);
            when(fundamentalAnalysisRepository.findAll()).thenReturn(analyses);

            List<FundamentalAnalysis> result = fundamentalAnalysisService.getAllAnalyses();

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(fundamentalAnalysisRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return only analyses for active tickers")
        void getAnalysesForActiveTickers_ReturnsOnlyActive() {
            List<FundamentalAnalysis> activeAnalyses = List.of(analysis);
            when(fundamentalAnalysisRepository.findAllForActiveTickers())
                    .thenReturn(activeAnalyses);

            List<FundamentalAnalysis> result =
                    fundamentalAnalysisService.getAnalysesForActiveTickers();

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(fundamentalAnalysisRepository, times(1)).findAllForActiveTickers();
        }

        @Test
        @DisplayName("Should return empty list when no analyses exist")
        void getAllAnalyses_NoAnalyses_ReturnsEmptyList() {
            when(fundamentalAnalysisRepository.findAll()).thenReturn(Collections.emptyList());

            List<FundamentalAnalysis> result = fundamentalAnalysisService.getAllAnalyses();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Get Analyses for Ticker Tests")
    class GetAnalysesForTickerTests {

        @Test
        @DisplayName("Should return all analyses for a specific ticker")
        void getAllAnalysesForTicker_ReturnsAllPeriods() {
            FundamentalAnalysis annualAnalysis =
                    FundamentalAnalysis.builder()
                            .ticker(stockTicker)
                            .periodType(PeriodType.ANNUAL)
                            .build();
            FundamentalAnalysis quarterlyAnalysis =
                    FundamentalAnalysis.builder()
                            .ticker(stockTicker)
                            .periodType(PeriodType.QUARTERLY)
                            .build();

            List<FundamentalAnalysis> analyses = List.of(annualAnalysis, quarterlyAnalysis);
            when(fundamentalAnalysisRepository.findByTickerId(1)).thenReturn(analyses);

            List<FundamentalAnalysis> result =
                    fundamentalAnalysisService.getAllAnalysesForTicker(1);

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(fundamentalAnalysisRepository, times(1)).findByTickerId(1);
        }
    }

    @Nested
    @DisplayName("Delete Analysis Tests")
    class DeleteAnalysisTests {

        @Test
        @DisplayName("Should delete analysis successfully")
        void deleteAnalysis_Success() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(stockTicker));
            doNothing().when(fundamentalAnalysisRepository).deleteByTicker(any());

            assertDoesNotThrow(() -> fundamentalAnalysisService.deleteAnalysis(1));

            verify(fundamentalAnalysisRepository, times(1)).deleteByTicker(any());
        }

        @Test
        @DisplayName("Should throw exception when ticker not found for deletion")
        void deleteAnalysis_TickerNotFound_ThrowsException() {
            when(tickerRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> fundamentalAnalysisService.deleteAnalysis(999));

            verify(fundamentalAnalysisRepository, never()).deleteByTicker(any());
        }
    }

    @Nested
    @DisplayName("Ticker Validation Tests")
    class TickerValidationTests {

        @Test
        @DisplayName("Should validate STOCK ticker as valid")
        void tickerIsValidForFundamentalAnalysis_Stock_ReturnsTrue() {
            assertTrue(FundamentalAnalysisService.tickerIsValidForFundamentalAnalysis(stockTicker));
        }

        @Test
        @DisplayName("Should validate REIT ticker as valid")
        void tickerIsValidForFundamentalAnalysis_REIT_ReturnsTrue() {
            assertTrue(FundamentalAnalysisService.tickerIsValidForFundamentalAnalysis(reitTicker));
        }

        @Test
        @DisplayName("Should validate CRYPTOCURRENCY ticker as invalid")
        void tickerIsValidForFundamentalAnalysis_Crypto_ReturnsFalse() {
            assertFalse(
                    FundamentalAnalysisService.tickerIsValidForFundamentalAnalysis(cryptoTicker));
        }

        @Test
        @DisplayName("Should validate ETF ticker as invalid")
        void tickerIsValidForFundamentalAnalysis_ETF_ReturnsFalse() {
            Ticker etfTicker =
                    Ticker.builder()
                            .id(5)
                            .symbol("SPY")
                            .type(TickerType.ETF)
                            .isArchived(false)
                            .build();

            assertFalse(FundamentalAnalysisService.tickerIsValidForFundamentalAnalysis(etfTicker));
        }
    }
}
