/*
 * Filename: MarketService.java
 * Created on: January 17, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.json.JSONObject;
import org.moinex.error.MoinexException;
import org.moinex.model.investment.BrazilianMarketIndicators;
import org.moinex.model.investment.MarketQuotesAndCommodities;
import org.moinex.repository.investment.BrazilianMarketIndicatorsRepository;
import org.moinex.repository.investment.MarketQuotesAndCommoditiesRepository;
import org.moinex.util.APIUtils;
import org.moinex.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service that provides market information
 */
@Service
public class MarketService {
    private final BrazilianMarketIndicatorsRepository brazilianMarketIndicatorsRepository;

    private final MarketQuotesAndCommoditiesRepository marketQuotesAndCommoditiesRepository;

    private boolean isBrazilianMarketIndicatorsUpdating = false;

    private boolean isMarketQuotesAndCommoditiesUpdating = false;

    public Integer MAX_RETRIES = 7;
    public static final Integer RETRY_DELAY_MS = 2000;
    public static final Double RETRY_MULTIPLIER = 1.5;

    private static final Logger logger = LoggerFactory.getLogger(MarketService.class);

    public MarketService(
            BrazilianMarketIndicatorsRepository brazilianMarketIndicatorsRepository,
            MarketQuotesAndCommoditiesRepository marketQuotesAndCommoditiesRepository) {
        this.brazilianMarketIndicatorsRepository = brazilianMarketIndicatorsRepository;
        this.marketQuotesAndCommoditiesRepository = marketQuotesAndCommoditiesRepository;
    }

    /**
     * Get the Brazilian market indicators
     *
     * @return The Brazilian market indicators
     * @throws EntityNotFoundException If no Brazilian market indicators are found
     */
    @Transactional
    public BrazilianMarketIndicators getBrazilianMarketIndicators() {
        List<BrazilianMarketIndicators> indicatorsList =
                brazilianMarketIndicatorsRepository.findAll();

        if (indicatorsList.isEmpty()) {
            throw new EntityNotFoundException("No Brazilian market indicators found");
        } else {
            if (indicatorsList.size() > 1) {
                // If there are more than one indicator, delete the others
                for (int i = 1; i < indicatorsList.size(); i++) {
                    brazilianMarketIndicatorsRepository.delete(indicatorsList.get(i));
                }
            }

            // Return the first indicator
            return indicatorsList.getFirst();
        }
    }

    /**
     * Get the market quotes and commodities
     *
     * @return The market quotes and commodities
     * @throws EntityNotFoundException If no market quotes and commodities are found
     */
    @Transactional
    public MarketQuotesAndCommodities getMarketQuotesAndCommodities() {
        List<MarketQuotesAndCommodities> marketQuotesAndCommoditiesList =
                marketQuotesAndCommoditiesRepository.findAll();

        if (marketQuotesAndCommoditiesList.isEmpty()) {
            throw new EntityNotFoundException("No market quotes and commodities found");
        } else {
            if (marketQuotesAndCommoditiesList.size() > 1) {
                // If there are more than one indicator, delete the others
                for (int i = 1; i < marketQuotesAndCommoditiesList.size(); i++) {
                    marketQuotesAndCommoditiesRepository.delete(
                            marketQuotesAndCommoditiesList.get(i));
                }
            }

            // Return the first indicator
            return marketQuotesAndCommoditiesList.getFirst();
        }
    }

    /**
     * Update the Brazilian market indicators from the API asynchronously
     *
     * @return A CompletableFuture with the Brazilian market indicators
     * @throws MoinexException.ResourceAlreadyUpdatingException If the Brazilian market indicators are
     *                                                          already being updated
     * @throws MoinexException.APIFetchException                If the API fetch fails
     */
    @Transactional
    public CompletableFuture<BrazilianMarketIndicators>
            updateBrazilianMarketIndicatorsFromApiAsync() {

        if (isBrazilianMarketIndicatorsUpdating) {
            return CompletableFuture.failedFuture(
                    new MoinexException.ResourceAlreadyUpdatingException(
                            "Brazilian market indicators are already being updated"));
        }

        isBrazilianMarketIndicatorsUpdating = true;

        return updateBrazilianMarketIndicatorsWithRetry(1, RETRY_DELAY_MS)
                .whenComplete((r, e) -> isBrazilianMarketIndicatorsUpdating = false);
    }

    private CompletableFuture<BrazilianMarketIndicators> updateBrazilianMarketIndicatorsWithRetry(
            int attempt, int retryDelayMs) {

        logger.info("Updating Brazilian market indicators - Attempt {}/{}", attempt, MAX_RETRIES);

        return APIUtils.fetchBrazilianMarketIndicatorsAsync()
                .thenApply(
                        jsonObject -> {
                            if (jsonObject == null) {
                                throw new MoinexException.APIFetchException(
                                        "Null response from API");
                            }

                            BrazilianMarketIndicators bmi;

                            try {
                                bmi = getBrazilianMarketIndicators();
                            } catch (EntityNotFoundException e) {
                                bmi = BrazilianMarketIndicators.builder().build();
                            }

                            String valorField = "valor";

                            bmi.setSelicTarget(
                                    new BigDecimal(
                                            jsonObject
                                                    .getJSONObject("selic_target")
                                                    .get(valorField)
                                                    .toString()));

                            bmi.setIpca12Months(
                                    new BigDecimal(
                                            jsonObject
                                                    .getJSONObject("ipca_12_months")
                                                    .get(valorField)
                                                    .toString()));

                            JSONObject ipcaLastMonth = jsonObject.getJSONObject("ipca_last_month");

                            bmi.setIpcaLastMonth(
                                    new BigDecimal(ipcaLastMonth.get(valorField).toString()));

                            String date = ipcaLastMonth.getString("data");

                            LocalDate localDate =
                                    LocalDate.parse(
                                            date, DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                            bmi.setIpcaLastMonthReference(YearMonth.from(localDate));
                            bmi.setLastUpdate(LocalDateTime.now());

                            brazilianMarketIndicatorsRepository.save(bmi);
                            return bmi;
                        })
                .handle(
                        (result, throwable) -> {
                            if (throwable == null) {
                                return CompletableFuture.completedFuture(result);
                            }

                            if (attempt >= MAX_RETRIES) {
                                CompletableFuture<BrazilianMarketIndicators> failed =
                                        new CompletableFuture<>();

                                failed.completeExceptionally(
                                        new MoinexException.APIFetchException(
                                                "Failed to fetch Brazilian market indicators after "
                                                        + MAX_RETRIES
                                                        + " attempts"));

                                return failed;
                            }

                            logger.warn(
                                    "Error fetching Brazilian market indicators (attempt {}): {}",
                                    attempt,
                                    throwable.getMessage());

                            logger.info("Retrying in {} ms...", retryDelayMs);

                            return CompletableFuture.runAsync(
                                            () -> {},
                                            CompletableFuture.delayedExecutor(
                                                    retryDelayMs, TimeUnit.MILLISECONDS))
                                    .thenCompose(
                                            v ->
                                                    updateBrazilianMarketIndicatorsWithRetry(
                                                            attempt + 1,
                                                            (int)
                                                                    (retryDelayMs
                                                                            * RETRY_MULTIPLIER)));
                        })
                .thenCompose(Function.identity());
    }

    /**
     * Update the market quotes and commodities from the API asynchronously
     *
     * @return A CompletableFuture with the market quotes and commodities
     * @throws MoinexException.ResourceAlreadyUpdatingException If the market quotes and commodities are
     *                                                          already being updated
     * @throws MoinexException.APIFetchException                If the API fetch fails
     */
    public CompletableFuture<MarketQuotesAndCommodities>
            updateMarketQuotesAndCommoditiesFromApiAsync() {

        if (isMarketQuotesAndCommoditiesUpdating) {
            return CompletableFuture.failedFuture(
                    new MoinexException.ResourceAlreadyUpdatingException(
                            "Market quotes and commodities are already being updated"));
        }

        isMarketQuotesAndCommoditiesUpdating = true;

        return updateMarketQuotesAndCommoditiesWithRetry(1, RETRY_DELAY_MS)
                .whenComplete((r, e) -> isMarketQuotesAndCommoditiesUpdating = false);
    }

    private CompletableFuture<MarketQuotesAndCommodities> updateMarketQuotesAndCommoditiesWithRetry(
            int attempt, int retryDelayMs) {

        String[] symbols = {
            Constants.IBOVESPA_TICKER,
            Constants.DOLLAR_TICKER,
            Constants.EURO_TICKER,
            Constants.GOLD_TICKER,
            Constants.SOYBEAN_TICKER,
            Constants.COFFEE_ARABICA_TICKER,
            Constants.WHEAT_TICKER,
            Constants.OIL_BRENT_TICKER,
            Constants.BITCOIN_TICKER,
            Constants.ETHEREUM_TICKER
        };

        logger.info("Updating market quotes and commodities - Attempt {}/{}", attempt, MAX_RETRIES);

        return APIUtils.fetchStockPricesAsync(symbols)
                .thenApply(
                        jsonObject -> {
                            if (jsonObject == null) {
                                throw new MoinexException.APIFetchException(
                                        "Null response from API");
                            }

                            MarketQuotesAndCommodities mqac;

                            try {
                                mqac = getMarketQuotesAndCommodities();
                            } catch (EntityNotFoundException e) {
                                mqac = MarketQuotesAndCommodities.builder().build();
                            }

                            String priceField = "price";

                            mqac.setDollar(
                                    new BigDecimal(
                                            jsonObject
                                                    .getJSONObject(Constants.DOLLAR_TICKER)
                                                    .get(priceField)
                                                    .toString()));

                            mqac.setEuro(
                                    new BigDecimal(
                                            jsonObject
                                                    .getJSONObject(Constants.EURO_TICKER)
                                                    .get(priceField)
                                                    .toString()));

                            mqac.setIbovespa(
                                    new BigDecimal(
                                            jsonObject
                                                    .getJSONObject(Constants.IBOVESPA_TICKER)
                                                    .get(priceField)
                                                    .toString()));

                            mqac.setBitcoin(
                                    new BigDecimal(
                                            jsonObject
                                                    .getJSONObject(Constants.BITCOIN_TICKER)
                                                    .get(priceField)
                                                    .toString()));

                            mqac.setEthereum(
                                    new BigDecimal(
                                            jsonObject
                                                    .getJSONObject(Constants.ETHEREUM_TICKER)
                                                    .get(priceField)
                                                    .toString()));

                            mqac.setGold(
                                    new BigDecimal(
                                            jsonObject
                                                    .getJSONObject(Constants.GOLD_TICKER)
                                                    .get(priceField)
                                                    .toString()));

                            mqac.setSoybean(
                                    new BigDecimal(
                                            jsonObject
                                                    .getJSONObject(Constants.SOYBEAN_TICKER)
                                                    .get(priceField)
                                                    .toString()));

                            mqac.setCoffee(
                                    new BigDecimal(
                                            jsonObject
                                                    .getJSONObject(Constants.COFFEE_ARABICA_TICKER)
                                                    .get(priceField)
                                                    .toString()));

                            mqac.setWheat(
                                    new BigDecimal(
                                            jsonObject
                                                    .getJSONObject(Constants.WHEAT_TICKER)
                                                    .get(priceField)
                                                    .toString()));

                            mqac.setOilBrent(
                                    new BigDecimal(
                                            jsonObject
                                                    .getJSONObject(Constants.OIL_BRENT_TICKER)
                                                    .get(priceField)
                                                    .toString()));

                            mqac.setLastUpdate(LocalDateTime.now());

                            marketQuotesAndCommoditiesRepository.save(mqac);
                            return mqac;
                        })
                .handle(
                        (result, throwable) -> {
                            if (throwable == null) {
                                return CompletableFuture.completedFuture(result);
                            }

                            if (attempt >= MAX_RETRIES) {
                                CompletableFuture<MarketQuotesAndCommodities> failed =
                                        new CompletableFuture<>();

                                failed.completeExceptionally(
                                        new MoinexException.APIFetchException(
                                                "Failed to fetch market quotes and commodities"
                                                        + " after "
                                                        + MAX_RETRIES
                                                        + " attempts"));

                                return failed;
                            }

                            logger.warn(
                                    "Error fetching market quotes (attempt {}): {}",
                                    attempt,
                                    throwable.getMessage());

                            logger.info("Retrying in {} ms...", retryDelayMs);

                            return CompletableFuture.runAsync(
                                            () -> {},
                                            CompletableFuture.delayedExecutor(
                                                    retryDelayMs, TimeUnit.MILLISECONDS))
                                    .thenCompose(
                                            v ->
                                                    updateMarketQuotesAndCommoditiesWithRetry(
                                                            attempt + 1,
                                                            (int)
                                                                    (retryDelayMs
                                                                            * RETRY_MULTIPLIER)));
                        })
                .thenCompose(Function.identity());
    }
}
