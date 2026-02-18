package org.moinex.app;

import org.moinex.repository.investment.TickerPurchaseRepository;
import org.moinex.repository.investment.TickerSaleRepository;
import org.moinex.service.MarketService;
import org.moinex.service.RecurringTransactionService;
import org.moinex.service.TickerPriceHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppStartupRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AppStartupRunner.class);

    private final MarketService marketService;
    private final RecurringTransactionService recurringTransactionService;
    private final TickerPriceHistoryService priceHistoryService;
    private final TickerPurchaseRepository tickerPurchaseRepository;
    private final TickerSaleRepository tickerSaleRepository;

    @Autowired
    public AppStartupRunner(
            MarketService marketService,
            RecurringTransactionService recurringTransactionService,
            TickerPriceHistoryService priceHistoryService,
            TickerPurchaseRepository tickerPurchaseRepository,
            TickerSaleRepository tickerSaleRepository) {
        this.marketService = marketService;
        this.recurringTransactionService = recurringTransactionService;
        this.priceHistoryService = priceHistoryService;
        this.tickerPurchaseRepository = tickerPurchaseRepository;
        this.tickerSaleRepository = tickerSaleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        recurringTransactionService.processRecurringTransactions();

        // Execute API operations sequentially to avoid rate limiting
        // Each operation waits for the previous one to complete before starting
        marketService
                .updateBrazilianMarketIndicatorsFromApiAsync()
                .exceptionally(
                        ex -> {
                            logger.error(
                                    "Failed to update Brazilian market indicators: {}",
                                    ex.getMessage());
                            return null;
                        })
                .thenCompose(
                        result ->
                                priceHistoryService
                                        .initializePriceHistory(
                                                tickerPurchaseRepository, tickerSaleRepository)
                                        .exceptionally(
                                                ex -> {
                                                    logger.error(
                                                            "Failed to initialize price history:"
                                                                    + " {}",
                                                            ex.getMessage());
                                                    return null;
                                                }))
                .thenCompose(
                        result ->
                                marketService
                                        .updateMarketQuotesAndCommoditiesFromApiAsync()
                                        .exceptionally(
                                                ex -> {
                                                    logger.error(
                                                            "Failed to update market quotes and"
                                                                    + " commodities: {}",
                                                            ex.getMessage());
                                                    return null;
                                                }));
    }
}
