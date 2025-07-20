package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.error.MoinexException;
import org.moinex.model.investment.BrazilianMarketIndicators;
import org.moinex.model.investment.MarketQuotesAndCommodities;
import org.moinex.repository.investment.BrazilianMarketIndicatorsRepository;
import org.moinex.repository.investment.MarketQuotesAndCommoditiesRepository;
import org.moinex.util.APIUtils;
import org.moinex.util.Constants;

@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

    @Mock private BrazilianMarketIndicatorsRepository brazilianMarketIndicatorsRepository;

    @Mock private MarketQuotesAndCommoditiesRepository marketQuotesAndCommoditiesRepository;

    @InjectMocks private MarketService marketService;

    @Nested
    @DisplayName("Get Brazilian Market Indicators Tests")
    class GetBrazilianMarketIndicatorsTests {
        @Test
        @DisplayName("Should return single indicator when only one exists")
        void getBrazilianMarketIndicators_SingleIndicator_Success() {
            BrazilianMarketIndicators indicator = BrazilianMarketIndicators.builder().id(1).build();
            when(brazilianMarketIndicatorsRepository.findAll())
                    .thenReturn(Collections.singletonList(indicator));

            BrazilianMarketIndicators result = marketService.getBrazilianMarketIndicators();

            assertNotNull(result);
            assertEquals(indicator.getId(), result.getId());
            verify(brazilianMarketIndicatorsRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should return first indicator and delete others when multiple exist")
        void getBrazilianMarketIndicators_MultipleIndicators_DeletesExtras() {
            BrazilianMarketIndicators indicator1 =
                    BrazilianMarketIndicators.builder().id(1).build();
            BrazilianMarketIndicators indicator2 =
                    BrazilianMarketIndicators.builder().id(2).build();
            List<BrazilianMarketIndicators> indicators =
                    new ArrayList<>(List.of(indicator1, indicator2));
            when(brazilianMarketIndicatorsRepository.findAll()).thenReturn(indicators);

            BrazilianMarketIndicators result = marketService.getBrazilianMarketIndicators();

            assertNotNull(result);
            assertEquals(indicator1.getId(), result.getId());
            verify(brazilianMarketIndicatorsRepository, times(1)).delete(indicator2);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when no indicators exist")
        void getBrazilianMarketIndicators_NoIndicators_ThrowsException() {
            when(brazilianMarketIndicatorsRepository.findAll()).thenReturn(Collections.emptyList());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> marketService.getBrazilianMarketIndicators());
        }
    }

    @Nested
    @DisplayName("Update Brazilian Market Indicators Async Tests")
    class UpdateBrazilianMarketIndicatorsAsyncTests {
        @Test
        @DisplayName("Should update indicators from API successfully")
        void updateBrazilianMarketIndicatorsFromApiAsync_Success() throws JSONException {
            JSONObject selic = new JSONObject().put("valor", new BigDecimal("10.50"));
            JSONObject ipca12 = new JSONObject().put("valor", new BigDecimal("4.65"));
            JSONObject ipcaMonth =
                    new JSONObject().put("valor", new BigDecimal("0.38")).put("data", "30/06/2025");
            JSONObject apiResponse =
                    new JSONObject()
                            .put("selic_target", selic)
                            .put("ipca_12_months", ipca12)
                            .put("ipca_last_month", ipcaMonth);

            CompletableFuture<JSONObject> futureResponse =
                    CompletableFuture.completedFuture(apiResponse);

            try (MockedStatic<APIUtils> mockedApi = Mockito.mockStatic(APIUtils.class)) {
                mockedApi
                        .when(APIUtils::fetchBrazilianMarketIndicatorsAsync)
                        .thenReturn(futureResponse);
                when(brazilianMarketIndicatorsRepository.findAll())
                        .thenReturn(Collections.emptyList());

                CompletableFuture<BrazilianMarketIndicators> resultFuture =
                        marketService.updateBrazilianMarketIndicatorsFromApiAsync();
                BrazilianMarketIndicators result = resultFuture.join();

                assertNotNull(result);
                assertEquals(new BigDecimal("10.50"), result.getSelicTarget());
                assertEquals(new BigDecimal("4.65"), result.getIpca12Months());
                assertEquals(new BigDecimal("0.38"), result.getIpcaLastMonth());
                verify(brazilianMarketIndicatorsRepository)
                        .save(any(BrazilianMarketIndicators.class));
            }
        }

        @Test
        @DisplayName("Should throw ResourceAlreadyUpdatingException if already updating")
        void updateBrazilianMarketIndicatorsFromApiAsync_AlreadyUpdating_ThrowsException() {
            marketService.updateBrazilianMarketIndicatorsFromApiAsync();

            CompletableFuture<BrazilianMarketIndicators> secondCallFuture =
                    marketService.updateBrazilianMarketIndicatorsFromApiAsync();

            Exception exception = assertThrows(ExecutionException.class, secondCallFuture::get);
            assertTrue(
                    exception.getCause()
                            instanceof MoinexException.ResourceAlreadyUpdatingException);
        }

        @Test
        @DisplayName("Should throw APIFetchException when API response is null")
        void updateBrazilianMarketIndicatorsFromApiAsync_NullApiResponse_ThrowsException() {
            try (MockedStatic<APIUtils> mockedApi = Mockito.mockStatic(APIUtils.class)) {
                mockedApi
                        .when(APIUtils::fetchBrazilianMarketIndicatorsAsync)
                        .thenReturn(CompletableFuture.completedFuture(null));
                CompletableFuture<BrazilianMarketIndicators> resultFuture =
                        marketService.updateBrazilianMarketIndicatorsFromApiAsync();
                Exception exception = assertThrows(ExecutionException.class, resultFuture::get);
                assertTrue(exception.getCause() instanceof MoinexException.APIFetchException);
            }
        }

        @Test
        @DisplayName("Should throw APIFetchException when API fetch fails with exception")
        void updateBrazilianMarketIndicatorsFromApiAsync_ApiFetchFails_ThrowsException() {
            CompletableFuture<JSONObject> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("API error"));
            try (MockedStatic<APIUtils> mockedApi = Mockito.mockStatic(APIUtils.class)) {
                mockedApi
                        .when(APIUtils::fetchBrazilianMarketIndicatorsAsync)
                        .thenReturn(failedFuture);
                CompletableFuture<BrazilianMarketIndicators> resultFuture =
                        marketService.updateBrazilianMarketIndicatorsFromApiAsync();
                Exception exception = assertThrows(ExecutionException.class, resultFuture::get);
                assertTrue(exception.getCause() instanceof MoinexException.APIFetchException);
            }
        }
    }

    @Nested
    @DisplayName("Get Market Quotes and Commodities Tests")
    class GetMarketQuotesAndCommoditiesTests {
        @Test
        @DisplayName("Should return quotes and commodities when they exist")
        void getMarketQuotesAndCommodities_Exists_ReturnsData() {
            MarketQuotesAndCommodities quotes =
                    MarketQuotesAndCommodities.builder()
                            .dollar(new BigDecimal("5.40"))
                            .oilBrent(new BigDecimal("85"))
                            .build();
            when(marketQuotesAndCommoditiesRepository.findAll())
                    .thenReturn(Collections.singletonList(quotes));

            MarketQuotesAndCommodities result = marketService.getMarketQuotesAndCommodities();

            assertNotNull(result);
            assertEquals(new BigDecimal("5.40"), result.getDollar());
            assertEquals(new BigDecimal("85"), result.getOilBrent());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when no quotes exist")
        void getMarketQuotesAndCommodities_NoData_ThrowsException() {
            when(marketQuotesAndCommoditiesRepository.findAll())
                    .thenReturn(Collections.emptyList());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> marketService.getMarketQuotesAndCommodities());
        }

        @Test
        @DisplayName("Should return first quote and delete others when multiple exist")
        void getMarketQuotesAndCommodities_MultipleIndicators_DeletesExtras() {
            MarketQuotesAndCommodities mq1 = MarketQuotesAndCommodities.builder().id(1).build();
            MarketQuotesAndCommodities mq2 = MarketQuotesAndCommodities.builder().id(2).build();
            List<MarketQuotesAndCommodities> list = new ArrayList<>(List.of(mq1, mq2));
            when(marketQuotesAndCommoditiesRepository.findAll()).thenReturn(list);

            MarketQuotesAndCommodities result = marketService.getMarketQuotesAndCommodities();

            assertNotNull(result);
            assertEquals(mq1.getId(), result.getId());
            verify(marketQuotesAndCommoditiesRepository, times(1)).delete(mq2);
        }
    }

    @Nested
    @DisplayName("Update Market Quotes and Commodities Async Tests")
    class UpdateMarketQuotesAndCommoditiesAsyncTests {
        private JSONObject createPriceObject(BigDecimal price) throws JSONException {
            return new JSONObject().put("price", price);
        }

        @Test
        @DisplayName("Should update quotes from API successfully when no previous data exists")
        void updateMarketQuotesAndCommoditiesFromApiAsync_Success_NoExistingData()
                throws JSONException {
            JSONObject apiResponse =
                    new JSONObject()
                            .put(Constants.DOLLAR_TICKER, createPriceObject(new BigDecimal("5.40")))
                            .put(Constants.EURO_TICKER, createPriceObject(new BigDecimal("5.80")))
                            .put(
                                    Constants.IBOVESPA_TICKER,
                                    createPriceObject(new BigDecimal("120000")))
                            .put(
                                    Constants.BITCOIN_TICKER,
                                    createPriceObject(new BigDecimal("60000")))
                            .put(
                                    Constants.ETHEREUM_TICKER,
                                    createPriceObject(new BigDecimal("3000")))
                            .put(Constants.GOLD_TICKER, createPriceObject(new BigDecimal("2300")))
                            .put(
                                    Constants.SOYBEAN_TICKER,
                                    createPriceObject(new BigDecimal("1350")))
                            .put(
                                    Constants.COFFEE_ARABICA_TICKER,
                                    createPriceObject(new BigDecimal("220")))
                            .put(Constants.WHEAT_TICKER, createPriceObject(new BigDecimal("580")))
                            .put(
                                    Constants.OIL_BRENT_TICKER,
                                    createPriceObject(new BigDecimal("85")));

            CompletableFuture<JSONObject> futureResponse =
                    CompletableFuture.completedFuture(apiResponse);

            try (MockedStatic<APIUtils> mockedApi = Mockito.mockStatic(APIUtils.class)) {
                mockedApi
                        .when(() -> APIUtils.fetchStockPricesAsync(any()))
                        .thenReturn(futureResponse);
                when(marketQuotesAndCommoditiesRepository.findAll())
                        .thenReturn(Collections.emptyList());

                CompletableFuture<MarketQuotesAndCommodities> resultFuture =
                        marketService.updateMarketQuotesAndCommoditiesFromApiAsync();
                MarketQuotesAndCommodities result = resultFuture.join();

                assertNotNull(result);
                assertEquals(new BigDecimal("5.40"), result.getDollar());
                assertEquals(new BigDecimal("85"), result.getOilBrent());
                verify(marketQuotesAndCommoditiesRepository)
                        .save(any(MarketQuotesAndCommodities.class));
            }
        }

        @Test
        @DisplayName(
                "Should throw ResourceAlreadyUpdatingException if an update is already in progress")
        void updateMarketQuotesAndCommoditiesFromApiAsync_AlreadyUpdating_ThrowsException() {
            marketService.updateMarketQuotesAndCommoditiesFromApiAsync();

            CompletableFuture<MarketQuotesAndCommodities> secondCallFuture =
                    marketService.updateMarketQuotesAndCommoditiesFromApiAsync();

            Exception exception = assertThrows(ExecutionException.class, secondCallFuture::get);
            assertTrue(
                    exception.getCause()
                            instanceof MoinexException.ResourceAlreadyUpdatingException);
        }

        @Test
        @DisplayName("Should handle API fetch failure gracefully")
        void updateMarketQuotesAndCommoditiesFromApiAsync_ApiFetchFails_ThrowsException() {
            CompletableFuture<JSONObject> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("API is down"));

            try (MockedStatic<APIUtils> mockedApi = Mockito.mockStatic(APIUtils.class)) {
                mockedApi
                        .when(() -> APIUtils.fetchStockPricesAsync(any()))
                        .thenReturn(failedFuture);

                CompletableFuture<MarketQuotesAndCommodities> resultFuture =
                        marketService.updateMarketQuotesAndCommoditiesFromApiAsync();

                Exception exception = assertThrows(ExecutionException.class, resultFuture::get);
                assertTrue(exception.getCause() instanceof MoinexException.APIFetchException);
            }
        }

        @Test
        @DisplayName("Should throw APIFetchException when API response is null")
        void updateMarketQuotesAndCommoditiesFromApiAsync_NullApiResponse_ThrowsException() {
            try (MockedStatic<APIUtils> mockedApi = Mockito.mockStatic(APIUtils.class)) {
                mockedApi
                        .when(() -> APIUtils.fetchStockPricesAsync(any()))
                        .thenReturn(CompletableFuture.completedFuture(null));
                CompletableFuture<MarketQuotesAndCommodities> resultFuture =
                        marketService.updateMarketQuotesAndCommoditiesFromApiAsync();
                Exception exception = assertThrows(ExecutionException.class, resultFuture::get);
                assertTrue(exception.getCause() instanceof MoinexException.APIFetchException);
            }
        }
    }
}
