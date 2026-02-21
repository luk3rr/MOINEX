package org.moinex.app;

import java.util.concurrent.CompletableFuture;
import org.moinex.repository.investment.TickerPurchaseRepository;
import org.moinex.repository.investment.TickerSaleRepository;
import org.moinex.service.*;
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
    private final TickerService tickerService;
    private final TickerPurchaseRepository tickerPurchaseRepository;
    private final TickerSaleRepository tickerSaleRepository;
    private final InvestmentPerformanceCalculationService investmentPerformanceCalculationService;
    private final MarketIndicatorService marketIndicatorService;

    @Autowired
    public AppStartupRunner(
            MarketService marketService,
            RecurringTransactionService recurringTransactionService,
            TickerPriceHistoryService priceHistoryService,
            TickerService tickerService,
            TickerPurchaseRepository tickerPurchaseRepository,
            TickerSaleRepository tickerSaleRepository,
            InvestmentPerformanceCalculationService investmentPerformanceCalculationService,
            MarketIndicatorService marketIndicatorService) {
        this.marketService = marketService;
        this.recurringTransactionService = recurringTransactionService;
        this.priceHistoryService = priceHistoryService;
        this.tickerService = tickerService;
        this.tickerPurchaseRepository = tickerPurchaseRepository;
        this.tickerSaleRepository = tickerSaleRepository;
        this.investmentPerformanceCalculationService = investmentPerformanceCalculationService;
        this.marketIndicatorService = marketIndicatorService;
    }

    @Override
    public void run(ApplicationArguments args) {
        recurringTransactionService.processRecurringTransactions();

        // Update market indicators synchronously before other operations
        try {
            logger.info("Updating market indicator history...");
            marketIndicatorService.updateAllIndicators();
            logger.info("Market indicator history updated successfully");
        } catch (Exception ex) {
            logger.warn("Failed to update market indicator history: {}", ex.getMessage());
        }

        // Execute API operations sequentially to avoid rate limiting
        // Each operation waits for the previous one to complete before starting
        marketService
                .updateBrazilianMarketIndicatorsFromApiAsync()
                .exceptionally(
                        ex -> {
                            logger.error("Failed to update Brazilian market indicators", ex);
                            return null;
                        })
                .thenCompose(
                        result ->
                                marketService
                                        .updateMarketQuotesAndCommoditiesFromApiAsync()
                                        .exceptionally(
                                                ex -> {
                                                    logger.error(
                                                            "Failed to update market quotes and"
                                                                    + " commodities",
                                                            ex);
                                                    return null;
                                                }))
                .thenCompose(
                        result ->
                                priceHistoryService
                                        .initializePriceHistory(
                                                tickerPurchaseRepository, tickerSaleRepository)
                                        .handle(
                                                (res, ex) -> {
                                                    if (ex != null) {
                                                        logger.error(
                                                                "Failed to initialize price"
                                                                        + " history",
                                                                ex);
                                                        return false;
                                                    }
                                                    return true;
                                                }))
                .thenCompose(
                        previousStepSuccess ->
                                tickerService
                                        .updateAllNonArchivedTickersPricesAsync()
                                        .handle(
                                                (res, ex) -> {
                                                    if (ex != null) {
                                                        logger.error(
                                                                "Failed to update ticker prices",
                                                                ex);
                                                        return false;
                                                    }
                                                    return previousStepSuccess;
                                                }))
                .thenCompose(
                        bothSucceeded -> {
                            if (Boolean.TRUE.equals(bothSucceeded)) {
                                return investmentPerformanceCalculationService
                                        .recalculateAllSnapshots();
                            }
                            return CompletableFuture.completedFuture(null);
                        })
                .exceptionally(
                        ex -> {
                            logger.error("Unexpected error in startup pipeline", ex);
                            return null;
                        });
    }
}
