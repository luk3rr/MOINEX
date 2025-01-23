/*
 * Filename: MarketService.java
 * Created on: January 17, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;
import org.moinex.entities.investment.BrazilianMarketIndicators;
import org.moinex.entities.investment.MarketQuotesAndCommodities;
import org.moinex.repositories.BrazilianMarketIndicatorsRepository;
import org.moinex.repositories.MarketQuotesAndCommoditiesRepository;
import org.moinex.util.APIUtils;
import org.moinex.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that provides market information
 */
@Service
public class MarketService
{
    @Autowired
    private BrazilianMarketIndicatorsRepository m_brazilianMarketIndicatorsRepository;

    @Autowired
    private MarketQuotesAndCommoditiesRepository m_marketQuotesAndCommoditiesRepository;

    private Boolean isBrazilianMarketIndicatorsUpdating = false;

    private Boolean isMarketQuotesAndCommoditiesUpdating = false;

    /**
     * Get the Brazilian market indicators
     * @return The Brazilian market indicators
     * @throws RuntimeException If no Brazilian market indicators are found
     */
    @Transactional
    public BrazilianMarketIndicators GetBrazilianMarketIndicators()
    {
        List<BrazilianMarketIndicators> indicatorsList =
            m_brazilianMarketIndicatorsRepository.findAll();

        if (indicatorsList.isEmpty())
        {
            throw new RuntimeException("No Brazilian market indicators found");
        }
        else
        {
            if (indicatorsList.size() > 1)
            {
                // If there are more than one indicator, delete the others
                for (Integer i = 1; i < indicatorsList.size(); i++)
                {
                    m_brazilianMarketIndicatorsRepository.delete(indicatorsList.get(i));
                }
            }

            // Return the first indicator
            return indicatorsList.get(0);
        }
    }

    /**
     * Get the market quotes and commodities
     * @return The market quotes and commodities
     * @throws RuntimeException If no market quotes and commodities are found
     */
    @Transactional
    public MarketQuotesAndCommodities GetMarketQuotesAndCommodities()
    {
        List<MarketQuotesAndCommodities> marketQuotesAndCommoditiesList =
            m_marketQuotesAndCommoditiesRepository.findAll();

        if (marketQuotesAndCommoditiesList.isEmpty())
        {
            throw new RuntimeException("No market quotes and commodities found");
        }
        else
        {
            if (marketQuotesAndCommoditiesList.size() > 1)
            {
                // If there are more than one indicator, delete the others
                for (Integer i = 1; i < marketQuotesAndCommoditiesList.size(); i++)
                {
                    m_marketQuotesAndCommoditiesRepository.delete(
                        marketQuotesAndCommoditiesList.get(i));
                }
            }

            // Return the first indicator
            return marketQuotesAndCommoditiesList.get(0);
        }
    }

    /**
     * Update the Brazilian market indicators from the API asynchronously
     *
     * @return A CompletableFuture with the Brazilian market indicators
     */
    @Transactional
    public CompletableFuture<BrazilianMarketIndicators>
    UpdateBrazilianMarketIndicatorsFromAPIAsync()
    {
        if (isBrazilianMarketIndicatorsUpdating)
        {
            CompletableFuture<BrazilianMarketIndicators> failedFuture =
                new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException(
                "Brazilian market indicators are already being updated"));
            return failedFuture;
        }

        isBrazilianMarketIndicatorsUpdating = true;

        return APIUtils.FetchBrazilianMarketIndicatorsAsync()
            .thenApply(jsonObject -> {
                if (jsonObject != null)
                {
                    BrazilianMarketIndicators bmi;

                    try
                    {
                        bmi = GetBrazilianMarketIndicators();
                    }
                    catch (RuntimeException e)
                    {
                        // Create a new indicator if none is found
                        bmi = new BrazilianMarketIndicators();
                    }

                    bmi.SetSelicTarget(jsonObject.getJSONObject("selic_target")
                                           .getBigDecimal("valor"));

                    bmi.SetIpca12Months(jsonObject.getJSONObject("ipca_12_months")
                                            .getBigDecimal("valor"));

                    JSONObject ipcaLastMonth =
                        jsonObject.getJSONObject("ipca_last_month");

                    bmi.SetIpcaLastMonth(ipcaLastMonth.getBigDecimal("valor"));

                    // Extract the date in the format DD/MM/YYYY
                    // and convert it to MM/YYYY
                    String   date      = ipcaLastMonth.getString("data");
                    String[] dateParts = date.split("/");

                    if (dateParts.length == 3)
                    {
                        String formattedDate =
                            String.format("%s/%s", dateParts[1], dateParts[2]);

                        bmi.SetIpcaLastMonthReference(formattedDate);
                    }
                    else
                    {
                        // If the date is not in the expected format
                        bmi.SetIpcaLastMonthReference("N/A");
                    }

                    bmi.SetLastUpdate(LocalDateTime.now());

                    m_brazilianMarketIndicatorsRepository.save(bmi);
                    return bmi;
                }
                else
                {
                    throw new RuntimeException(
                        "Failed to fetch Brazilian market indicators");
                }
            })
            .whenComplete(
                (result, e) -> { isBrazilianMarketIndicatorsUpdating = false; })
            .exceptionally(e -> {
                isBrazilianMarketIndicatorsUpdating = false;
                throw new RuntimeException(
                    "Failed to fetch Brazilian market indicators",
                    e);
            });
    }

    /**
     * Update the market quotes and commodities from the API asynchronously
     *
     * @return A CompletableFuture with the market quotes and commodities
     */
    public CompletableFuture<MarketQuotesAndCommodities>
    UpdateMarketQuotesAndCommoditiesFromAPIAsync()
    {
        if (isMarketQuotesAndCommoditiesUpdating)
        {
            CompletableFuture<MarketQuotesAndCommodities> failedFuture =
                new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException(
                "Market quotes and commodities are already being updated"));
            return failedFuture;
        }

        isMarketQuotesAndCommoditiesUpdating = true;

        String[] symbols = { Constants.IBOVESPA_TICKER, Constants.DOLLAR_TICKER,
                             Constants.EURO_TICKER,     Constants.GOLD_TICKER,
                             Constants.SOYBEAN_TICKER,  Constants.COFFEE_ARABICA_TICKER,
                             Constants.WHEAT_TICKER,    Constants.OIL_BRENT_TICKER,
                             Constants.BITCOIN_TICKER,  Constants.ETHEREUM_TICKER };

        return APIUtils.FetchStockPricesAsync(symbols)
            .thenApply(jsonObject -> {
                if (jsonObject != null)
                {
                    MarketQuotesAndCommodities mqac;

                    try
                    {
                        mqac = GetMarketQuotesAndCommodities();
                    }
                    catch (RuntimeException e)
                    {
                        // Create a new indicator if none is found
                        mqac = new MarketQuotesAndCommodities();
                    }

                    mqac.SetDollar(jsonObject.getJSONObject(Constants.DOLLAR_TICKER)
                                       .getBigDecimal("price"));

                    mqac.SetEuro(jsonObject.getJSONObject(Constants.EURO_TICKER)
                                     .getBigDecimal("price"));

                    mqac.SetIbovespa(jsonObject.getJSONObject(Constants.IBOVESPA_TICKER)
                                         .getBigDecimal("price"));

                    mqac.SetBitcoin(jsonObject.getJSONObject(Constants.BITCOIN_TICKER)
                                        .getBigDecimal("price"));

                    mqac.SetEthereum(jsonObject.getJSONObject(Constants.ETHEREUM_TICKER)
                                         .getBigDecimal("price"));

                    mqac.SetGold(jsonObject.getJSONObject(Constants.GOLD_TICKER)
                                     .getBigDecimal("price"));

                    mqac.SetSoybean(jsonObject.getJSONObject(Constants.SOYBEAN_TICKER)
                                        .getBigDecimal("price"));

                    mqac.SetCoffee(
                        jsonObject.getJSONObject(Constants.COFFEE_ARABICA_TICKER)
                            .getBigDecimal("price"));

                    mqac.SetWheat(jsonObject.getJSONObject(Constants.WHEAT_TICKER)
                                      .getBigDecimal("price"));

                    mqac.SetOilBrent(
                        jsonObject.getJSONObject(Constants.OIL_BRENT_TICKER)
                            .getBigDecimal("price"));

                    mqac.SetLastUpdate(LocalDateTime.now());

                    m_marketQuotesAndCommoditiesRepository.save(mqac);
                    return mqac;
                }
                else
                {
                    throw new RuntimeException(
                        "Failed to fetch market quotes and commodities");
                }
            })
            .whenComplete(
                (result, e) -> { isMarketQuotesAndCommoditiesUpdating = false; })
            .exceptionally(e -> {
                isMarketQuotesAndCommoditiesUpdating = false;
                throw new RuntimeException(
                    "Failed to fetch market quotes and commodities",
                    e);
            });
    }
}
