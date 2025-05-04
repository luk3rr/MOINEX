package org.moinex.app;

import org.moinex.service.MarketService;
import org.moinex.service.RecurringTransactionService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppStartupRunner implements ApplicationRunner {

    private final MarketService marketService;
    private final RecurringTransactionService recurringTransactionService;

    @Autowired
    public AppStartupRunner(
            MarketService marketService, RecurringTransactionService recurringTransactionService) {
        this.marketService = marketService;
        this.recurringTransactionService = recurringTransactionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        recurringTransactionService.processRecurringTransactions();

        marketService
                .updateBrazilianMarketIndicatorsFromApiAsync()
                .exceptionally(
                        ex -> {
                            LoggerFactory.getLogger(AppStartupRunner.class).error(ex.getMessage());
                            return null;
                        });

        marketService
                .updateMarketQuotesAndCommoditiesFromApiAsync()
                .exceptionally(
                        ex -> {
                            LoggerFactory.getLogger(AppStartupRunner.class).error(ex.getMessage());
                            return null;
                        });
    }
}
