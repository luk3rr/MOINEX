/*
 * Filename: InicializationService.java
 * Created on: November 19, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import jakarta.annotation.PostConstruct;
import java.util.logging.Logger;
import lombok.NoArgsConstructor;
import org.moinex.util.LoggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for running the initialization tasks
 */
@Component
@NoArgsConstructor
public class InicializationService
{
    @Autowired
    private RecurringTransactionService recurringTransactionService;

    @Autowired
    private MarketService marketService;

    private static final Logger logger = LoggerConfig.getLogger();

    @PostConstruct
    public void initialize()
    {
        recurringTransactionService.processRecurringTransactions();
        marketService.updateBrazilianMarketIndicatorsFromApiAsync().exceptionally(
            ex -> {
                logger.severe(ex.getMessage());
                return null;
            });

        marketService.updateMarketQuotesAndCommoditiesFromApiAsync().exceptionally(
            ex -> {
                logger.severe(ex.getMessage());
                return null;
            });
    }
}
