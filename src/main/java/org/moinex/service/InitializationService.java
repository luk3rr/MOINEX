/*
 * Filename: InitializationService.java
 * Created on: November 19, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.annotation.PostConstruct;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for running the initialization tasks
 */
@Component
@NoArgsConstructor
public class InitializationService
{
    private RecurringTransactionService recurringTransactionService;

    private MarketService marketService;

    private static final Logger logger =
        LoggerFactory.getLogger(InitializationService.class);

    @Autowired
    public InitializationService(
        RecurringTransactionService recurringTransactionService,
        MarketService               marketService)
    {
        this.recurringTransactionService = recurringTransactionService;
        this.marketService               = marketService;
    }

    @PostConstruct
    public void initialize()
    {
        recurringTransactionService.processRecurringTransactions();
        marketService.updateBrazilianMarketIndicatorsFromApiAsync().exceptionally(
            ex -> {
                logger.error(ex.getMessage());
                return null;
            });

        marketService.updateMarketQuotesAndCommoditiesFromApiAsync().exceptionally(
            ex -> {
                logger.error(ex.getMessage());
                return null;
            });
    }
}
