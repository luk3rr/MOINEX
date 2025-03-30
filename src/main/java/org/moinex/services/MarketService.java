/*
 * Filename: MarketService.java
 * Created on: January 17, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;
import org.moinex.entities.investment.BrazilianMarketIndicators;
import org.moinex.entities.investment.MarketQuotesAndCommodities;
import org.moinex.exceptions.APIFetchException;
import org.moinex.exceptions.ResourceAlreadyUpdatingException;
import org.moinex.repositories.investment.BrazilianMarketIndicatorsRepository;
import org.moinex.repositories.investment.MarketQuotesAndCommoditiesRepository;
import org.moinex.util.APIUtils;
import org.moinex.util.Constants;
import org.springframework.stereotype.Service;

/**
 * Service that provides market information
 */
@Service
public class MarketService
{
    private final BrazilianMarketIndicatorsRepository
        brazilianMarketIndicatorsRepository;

    private final MarketQuotesAndCommoditiesRepository
        marketQuotesAndCommoditiesRepository;

    private boolean isBrazilianMarketIndicatorsUpdating = false;

    private boolean isMarketQuotesAndCommoditiesUpdating = false;

    public MarketService(
        BrazilianMarketIndicatorsRepository  brazilianMarketIndicatorsRepository,
        MarketQuotesAndCommoditiesRepository marketQuotesAndCommoditiesRepository)
    {
        this.brazilianMarketIndicatorsRepository = brazilianMarketIndicatorsRepository;
        this.marketQuotesAndCommoditiesRepository =
            marketQuotesAndCommoditiesRepository;
    }

    /**
     * Get the Brazilian market indicators
     * @return The Brazilian market indicators
     * @throws EntityNotFoundException If no Brazilian market indicators are found
     */
    @Transactional
    public BrazilianMarketIndicators getBrazilianMarketIndicators()
    {
        List<BrazilianMarketIndicators> indicatorsList =
            brazilianMarketIndicatorsRepository.findAll();

        if (indicatorsList.isEmpty())
        {
            throw new EntityNotFoundException("No Brazilian market indicators found");
        }
        else
        {
            if (indicatorsList.size() > 1)
            {
                // If there are more than one indicator, delete the others
                for (int i = 1; i < indicatorsList.size(); i++)
                {
                    brazilianMarketIndicatorsRepository.delete(indicatorsList.get(i));
                }
            }

            // Return the first indicator
            return indicatorsList.getFirst();
        }
    }

    /**
     * Get the market quotes and commodities
     * @return The market quotes and commodities
     * @throws EntityNotFoundException If no market quotes and commodities are found
     */
    @Transactional
    public MarketQuotesAndCommodities getMarketQuotesAndCommodities()
    {
        List<MarketQuotesAndCommodities> marketQuotesAndCommoditiesList =
            marketQuotesAndCommoditiesRepository.findAll();

        if (marketQuotesAndCommoditiesList.isEmpty())
        {
            throw new EntityNotFoundException("No market quotes and commodities found");
        }
        else
        {
            if (marketQuotesAndCommoditiesList.size() > 1)
            {
                // If there are more than one indicator, delete the others
                for (int i = 1; i < marketQuotesAndCommoditiesList.size(); i++)
                {
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
     * @return A CompletableFuture with the Brazilian market indicators
     * @throws ResourceAlreadyUpdatingException If the Brazilian market indicators are
     *     already being updated
     * @throws APIFetchException If the API fetch fails
     */
    @Transactional
    public CompletableFuture<BrazilianMarketIndicators>
    updateBrazilianMarketIndicatorsFromApiAsync()
    {
        if (isBrazilianMarketIndicatorsUpdating)
        {
            CompletableFuture<BrazilianMarketIndicators> failedFuture =
                new CompletableFuture<>();

            failedFuture.completeExceptionally(new ResourceAlreadyUpdatingException(
                "Brazilian market indicators are already being updated"));

            return failedFuture;
        }

        isBrazilianMarketIndicatorsUpdating = true;

        return APIUtils.fetchBrazilianMarketIndicatorsAsync()
            .thenApply(jsonObject -> {
                if (jsonObject != null)
                {
                    BrazilianMarketIndicators bmi;

                    try
                    {
                        bmi = getBrazilianMarketIndicators();
                    }
                    catch (EntityNotFoundException e)
                    {
                        // Create a new indicator if none is found
                        bmi = BrazilianMarketIndicators.builder().build();
                    }

                    bmi.setSelicTarget(jsonObject.getJSONObject("selic_target")
                                           .getBigDecimal("valor"));

                    bmi.setIpca12Months(jsonObject.getJSONObject("ipca_12_months")
                                            .getBigDecimal("valor"));

                    JSONObject ipcaLastMonth =
                        jsonObject.getJSONObject("ipca_last_month");

                    bmi.setIpcaLastMonth(ipcaLastMonth.getBigDecimal("valor"));

                    // Extract the date in the format DD/MM/YYYY
                    // and convert it to a YearMonth object
                    String date = ipcaLastMonth.getString("data");

                    DateTimeFormatter inputFormatter =
                        DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    LocalDate localDate    = LocalDate.parse(date, inputFormatter);
                    YearMonth dateDateTime = YearMonth.from(localDate);

                    bmi.setIpcaLastMonthReference(dateDateTime);

                    bmi.setLastUpdate(LocalDateTime.now());

                    brazilianMarketIndicatorsRepository.save(bmi);
                    return bmi;
                }
                else
                {
                    throw new APIFetchException(
                        "Failed to fetch Brazilian market indicators");
                }
            })
            .whenComplete((result, e) -> isBrazilianMarketIndicatorsUpdating = false)
            .exceptionally(e -> {
                isBrazilianMarketIndicatorsUpdating = false;
                throw new APIFetchException(
                    "Failed to fetch Brazilian market indicators: " + e);
            });
    }

    /**
     * Update the market quotes and commodities from the API asynchronously
     * @return A CompletableFuture with the market quotes and commodities
     * @throws ResourceAlreadyUpdatingException If the market quotes and commodities are
     *     already being updated
     * @throws APIFetchException If the API fetch fails
     */
    public CompletableFuture<MarketQuotesAndCommodities>
    updateMarketQuotesAndCommoditiesFromApiAsync()
    {
        if (isMarketQuotesAndCommoditiesUpdating)
        {
            CompletableFuture<MarketQuotesAndCommodities> failedFuture =
                new CompletableFuture<>();

            failedFuture.completeExceptionally(new ResourceAlreadyUpdatingException(
                "Market quotes and commodities are already being updated"));

            return failedFuture;
        }

        isMarketQuotesAndCommoditiesUpdating = true;

        String[] symbols = { Constants.IBOVESPA_TICKER, Constants.DOLLAR_TICKER,
                             Constants.EURO_TICKER,     Constants.GOLD_TICKER,
                             Constants.SOYBEAN_TICKER,  Constants.COFFEE_ARABICA_TICKER,
                             Constants.WHEAT_TICKER,    Constants.OIL_BRENT_TICKER,
                             Constants.BITCOIN_TICKER,  Constants.ETHEREUM_TICKER };

        return APIUtils.fetchStockPricesAsync(symbols)
            .thenApply(jsonObject -> {
                if (jsonObject != null)
                {
                    MarketQuotesAndCommodities mqac;

                    try
                    {
                        mqac = getMarketQuotesAndCommodities();
                    }
                    catch (EntityNotFoundException e)
                    {
                        // Create a new indicator if none is found
                        mqac = MarketQuotesAndCommodities.builder().build();
                    }

                    mqac.setDollar(jsonObject.getJSONObject(Constants.DOLLAR_TICKER)
                                       .getBigDecimal("price"));

                    mqac.setEuro(jsonObject.getJSONObject(Constants.EURO_TICKER)
                                     .getBigDecimal("price"));

                    mqac.setIbovespa(jsonObject.getJSONObject(Constants.IBOVESPA_TICKER)
                                         .getBigDecimal("price"));

                    mqac.setBitcoin(jsonObject.getJSONObject(Constants.BITCOIN_TICKER)
                                        .getBigDecimal("price"));

                    mqac.setEthereum(jsonObject.getJSONObject(Constants.ETHEREUM_TICKER)
                                         .getBigDecimal("price"));

                    mqac.setGold(jsonObject.getJSONObject(Constants.GOLD_TICKER)
                                     .getBigDecimal("price"));

                    mqac.setSoybean(jsonObject.getJSONObject(Constants.SOYBEAN_TICKER)
                                        .getBigDecimal("price"));

                    mqac.setCoffee(
                        jsonObject.getJSONObject(Constants.COFFEE_ARABICA_TICKER)
                            .getBigDecimal("price"));

                    mqac.setWheat(jsonObject.getJSONObject(Constants.WHEAT_TICKER)
                                      .getBigDecimal("price"));

                    mqac.setOilBrent(
                        jsonObject.getJSONObject(Constants.OIL_BRENT_TICKER)
                            .getBigDecimal("price"));

                    mqac.setLastUpdate(LocalDateTime.now());

                    marketQuotesAndCommoditiesRepository.save(mqac);
                    return mqac;
                }
                else
                {
                    throw new APIFetchException(
                        "Failed to fetch market quotes and commodities");
                }
            })
            .whenComplete((result, e) -> isMarketQuotesAndCommoditiesUpdating = false)
            .exceptionally(e -> {
                isMarketQuotesAndCommoditiesUpdating = false;
                throw new APIFetchException(
                    "Failed to fetch market quotes and commodities" + e);
            });
    }
}
