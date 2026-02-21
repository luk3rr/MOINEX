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
    private final BondInterestCalculationService bondInterestCalculationService;

    @Autowired
    public AppStartupRunner(
            MarketService marketService,
            RecurringTransactionService recurringTransactionService,
            TickerPriceHistoryService priceHistoryService,
            TickerService tickerService,
            TickerPurchaseRepository tickerPurchaseRepository,
            TickerSaleRepository tickerSaleRepository,
            InvestmentPerformanceCalculationService investmentPerformanceCalculationService,
            MarketIndicatorService marketIndicatorService,
            BondInterestCalculationService bondInterestCalculationService) {
        this.marketService = marketService;
        this.recurringTransactionService = recurringTransactionService;
        this.priceHistoryService = priceHistoryService;
        this.tickerService = tickerService;
        this.tickerPurchaseRepository = tickerPurchaseRepository;
        this.tickerSaleRepository = tickerSaleRepository;
        this.investmentPerformanceCalculationService = investmentPerformanceCalculationService;
        this.marketIndicatorService = marketIndicatorService;
        this.bondInterestCalculationService = bondInterestCalculationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        recurringTransactionService.processRecurringTransactions();

        CompletableFuture.runAsync(
                        () -> {
                            logger.info("Updating market indicator history...");
                            marketIndicatorService.updateAllIndicators();
                            logger.info("Market indicator history updated successfully");
                        })
                .exceptionally(
                        ex -> {
                            logger.warn(
                                    "Failed to update market indicator history: {}",
                                    ex.getMessage());
                            return null;
                        })
                .thenRun(
                        () -> {
                            logger.info("Calculating bond interest...");
                            bondInterestCalculationService.calculateInterestForAllBondsIfNeeded();
                            logger.info("Bond interest calculation completed successfully");
                        })
                .exceptionally(
                        ex -> {
                            logger.warn("Failed to calculate bond interest: {}", ex.getMessage());
                            return null;
                        })
                .thenCompose(
                        ignored ->
                                marketService
                                        .updateBrazilianMarketIndicatorsFromApiAsync()
                                        .exceptionally(
                                                ex -> {
                                                    logger.error(
                                                            "Failed to update Brazilian market"
                                                                    + " indicators",
                                                            ex);
                                                    return null;
                                                })
                                        .thenCompose(
                                                ignored2 ->
                                                        marketService
                                                                .updateMarketQuotesAndCommoditiesFromApiAsync()
                                                                .exceptionally(
                                                                        ex -> {
                                                                            logger.error(
                                                                                    "Failed to"
                                                                                        + " update"
                                                                                        + " market"
                                                                                        + " quotes"
                                                                                        + " and commodities",
                                                                                    ex);
                                                                            return null;
                                                                        }))
                                        .thenCompose(
                                                ignored3 ->
                                                        priceHistoryService
                                                                .initializePriceHistory(
                                                                        tickerPurchaseRepository,
                                                                        tickerSaleRepository)
                                                                .handle(
                                                                        (res, ex) -> {
                                                                            if (ex != null) {
                                                                                logger.error(
                                                                                        "Failed to"
                                                                                            + " initialize"
                                                                                            + " price"
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
                                                                                        "Failed to"
                                                                                            + " update"
                                                                                            + " ticker"
                                                                                            + " prices",
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
                                                    logger.error(
                                                            "Unexpected error in startup pipeline",
                                                            ex);
                                                    return null;
                                                }));
    }
}
