/*
 * Filename: InvestmentPerformanceCalculationService.java
 * Created on: February 17, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moinex.model.dto.InvestmentPerformanceDTO;
import org.moinex.model.enums.OperationType;
import org.moinex.model.investment.Bond;
import org.moinex.model.investment.BondOperation;
import org.moinex.model.investment.Dividend;
import org.moinex.model.investment.Ticker;
import org.moinex.model.investment.TickerPurchase;
import org.moinex.model.investment.TickerSale;
import org.moinex.util.Constants;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentPerformanceCalculationService {

    private final InvestmentPerformanceSnapshotService snapshotService;
    private final TickerService tickerService;
    private final BondService bondService;
    private final TickerPriceHistoryService tickerPriceHistoryService;

    @Getter private volatile boolean isCalculating = false;

    /**
     * Get investment performance data for display
     * Uses cache when available, calculates and saves missing months automatically
     *
     * @return InvestmentPerformanceDTO with all metrics for the last N months
     */
    public InvestmentPerformanceDTO getPerformanceData() {
        YearMonth currentMonth = YearMonth.now();
        List<YearMonth> allMonths = new ArrayList<>();
        for (int i = Constants.XYBAR_CHART_MONTHS; i >= 0; i--) {
            allMonths.add(currentMonth.minusMonths(i));
        }

        Map<YearMonth, BigDecimal> monthlyInvested = new TreeMap<>();
        Map<YearMonth, BigDecimal> accumulatedGains = new TreeMap<>();
        Map<YearMonth, BigDecimal> monthlyGains = new TreeMap<>();
        Map<YearMonth, BigDecimal> portfolioValues = new TreeMap<>();

        boolean hasCache = snapshotService.hasSnapshots();

        if (hasCache) {
            // Try to load from cache
            List<YearMonth> missingMonths = new ArrayList<>();

            for (YearMonth month : allMonths) {
                var snapshot = snapshotService.getSnapshot(month.getMonthValue(), month.getYear());
                if (snapshot.isPresent()) {
                    monthlyInvested.put(month, snapshot.get().getInvestedValue());
                    portfolioValues.put(month, snapshot.get().getPortfolioValue());
                    accumulatedGains.put(month, snapshot.get().getAccumulatedCapitalGains());
                    monthlyGains.put(month, snapshot.get().getMonthlyCapitalGains());
                } else {
                    missingMonths.add(month);
                }
            }

            // If there are missing months (e.g., new month started), calculate and save them
            if (!missingMonths.isEmpty()) {
                log.info("Found {} missing months in cache, calculating...", missingMonths.size());

                Map<YearMonth, BigDecimal> calculatedInvested = calculateMonthlyInvestedValue();
                Map<YearMonth, BigDecimal> calculatedAccGains = calculateAccumulatedCapitalGains();
                Map<YearMonth, BigDecimal> calculatedMonthlyGains = calculateMonthlyCapitalGains();
                Map<YearMonth, BigDecimal> calculatedPortfolio = calculateMonthlyPortfolioValue();

                for (YearMonth month : missingMonths) {
                    BigDecimal invested = calculatedInvested.getOrDefault(month, BigDecimal.ZERO);
                    BigDecimal portfolio = calculatedPortfolio.getOrDefault(month, BigDecimal.ZERO);
                    BigDecimal accGains = calculatedAccGains.getOrDefault(month, BigDecimal.ZERO);
                    BigDecimal monthGains =
                            calculatedMonthlyGains.getOrDefault(month, BigDecimal.ZERO);

                    monthlyInvested.put(month, invested);
                    portfolioValues.put(month, portfolio);
                    accumulatedGains.put(month, accGains);
                    monthlyGains.put(month, monthGains);

                    snapshotService.saveSnapshot(
                            month.getMonthValue(),
                            month.getYear(),
                            invested,
                            portfolio,
                            accGains,
                            monthGains);
                }
                log.info("Missing months calculated and saved to cache");
            }
        } else {
            // No cache at all, calculate everything
            log.info("No cache found, calculating all data...");
            monthlyInvested = calculateMonthlyInvestedValue();
            accumulatedGains = calculateAccumulatedCapitalGains();
            monthlyGains = calculateMonthlyCapitalGains();
            portfolioValues = calculateMonthlyPortfolioValue();

            // Save calculated data to cache for future use
            for (YearMonth month : allMonths) {
                BigDecimal invested = monthlyInvested.getOrDefault(month, BigDecimal.ZERO);
                BigDecimal portfolio = portfolioValues.getOrDefault(month, BigDecimal.ZERO);
                BigDecimal accGains = accumulatedGains.getOrDefault(month, BigDecimal.ZERO);
                BigDecimal monthGains = monthlyGains.getOrDefault(month, BigDecimal.ZERO);

                snapshotService.saveSnapshot(
                        month.getMonthValue(),
                        month.getYear(),
                        invested,
                        portfolio,
                        accGains,
                        monthGains);
            }
            log.info("Investment performance data calculated and saved to cache");
        }

        return new InvestmentPerformanceDTO(
                monthlyInvested, portfolioValues, accumulatedGains, monthlyGains);
    }

    @Async
    public CompletableFuture<Void> recalculateAllSnapshots() {
        if (isCalculating) {
            log.warn("Investment performance calculation already in progress, skipping");
            return CompletableFuture.completedFuture(null);
        }

        try {
            isCalculating = true;
            log.info("Starting investment performance recalculation...");

            snapshotService.deleteAllSnapshots();

            Map<YearMonth, BigDecimal> monthlyInvested = calculateMonthlyInvestedValue();
            Map<YearMonth, BigDecimal> accumulatedGains = calculateAccumulatedCapitalGains();
            Map<YearMonth, BigDecimal> monthlyGains = calculateMonthlyCapitalGains();
            Map<YearMonth, BigDecimal> portfolioValues = calculateMonthlyPortfolioValue();

            YearMonth currentMonth = YearMonth.now();
            List<YearMonth> allMonths = new ArrayList<>();
            for (int i = Constants.XYBAR_CHART_MONTHS; i >= 0; i--) {
                allMonths.add(currentMonth.minusMonths(i));
            }

            for (YearMonth month : allMonths) {
                BigDecimal invested = monthlyInvested.getOrDefault(month, BigDecimal.ZERO);
                BigDecimal portfolio = portfolioValues.getOrDefault(month, BigDecimal.ZERO);
                BigDecimal accGains = accumulatedGains.getOrDefault(month, BigDecimal.ZERO);
                BigDecimal monthGains = monthlyGains.getOrDefault(month, BigDecimal.ZERO);

                snapshotService.saveSnapshot(
                        month.getMonthValue(),
                        month.getYear(),
                        invested,
                        portfolio,
                        accGains,
                        monthGains);

                log.debug(
                        "Saved snapshot for {}/{}: invested={}, portfolio={}, accGains={},"
                                + " monthGains={}",
                        month.getMonthValue(),
                        month.getYear(),
                        invested,
                        portfolio,
                        accGains,
                        monthGains);
            }

            log.info("Investment performance recalculation completed successfully");
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Error during investment performance recalculation", e);
            return CompletableFuture.failedFuture(e);
        } finally {
            isCalculating = false;
        }
    }

    private Map<YearMonth, BigDecimal> calculateMonthlyInvestedValue() {
        Map<YearMonth, BigDecimal> investedByMonth = new TreeMap<>();

        List<Ticker> allTickers = tickerService.getAllNonArchivedTickers();

        for (Ticker ticker : allTickers) {
            List<TickerPurchase> purchases = tickerService.getAllPurchasesByTicker(ticker.getId());
            List<TickerSale> sales = tickerService.getAllSalesByTicker(ticker.getId());

            if (purchases.isEmpty()
                    && ticker.getCurrentQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            LocalDate referenceDate;
            if (!purchases.isEmpty()) {
                referenceDate =
                        purchases.stream()
                                .map(p -> p.getWalletTransaction().getDate().toLocalDate())
                                .min(LocalDate::compareTo)
                                .orElse(null);
            } else {
                referenceDate =
                        LocalDateTime.parse(ticker.getCreatedAt(), Constants.DB_DATE_FORMATTER)
                                .toLocalDate();
            }

            if (referenceDate == null) {
                continue;
            }

            YearMonth firstMonth = YearMonth.from(referenceDate);

            Map<YearMonth, BigDecimal> quantityByMonth =
                    calculateQuantityAtMonthEnd(ticker, purchases, sales, firstMonth);

            for (Map.Entry<YearMonth, BigDecimal> entry : quantityByMonth.entrySet()) {
                YearMonth month = entry.getKey();
                BigDecimal quantity = entry.getValue();

                if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal investedValue = ticker.getAverageUnitValue().multiply(quantity);
                investedByMonth.merge(month, investedValue, BigDecimal::add);
            }
        }

        List<Bond> allBonds = bondService.getAllNonArchivedBonds();
        for (Bond bond : allBonds) {
            List<BondOperation> operations = bondService.getOperationsByBond(bond);

            if (operations.isEmpty()) {
                continue;
            }

            LocalDate firstOperationDate =
                    operations.stream()
                            .map(op -> op.getWalletTransaction().getDate().toLocalDate())
                            .min(LocalDate::compareTo)
                            .orElse(null);

            if (firstOperationDate == null) {
                continue;
            }

            YearMonth firstMonth = YearMonth.from(firstOperationDate);
            YearMonth currentMonth = YearMonth.now();

            BigDecimal cumulativeValue = BigDecimal.ZERO;
            YearMonth month = firstMonth;

            while (!month.isAfter(currentMonth)) {
                for (BondOperation op : operations) {
                    YearMonth opMonth = YearMonth.from(op.getWalletTransaction().getDate());
                    if (opMonth.equals(month)) {
                        BigDecimal opValue = op.getUnitPrice().multiply(op.getQuantity());
                        if (op.getOperationType() == OperationType.BUY) {
                            cumulativeValue = cumulativeValue.add(opValue);
                        } else if (op.getOperationType() == OperationType.SELL) {
                            cumulativeValue = cumulativeValue.subtract(opValue);
                        }
                    }
                }

                if (cumulativeValue.compareTo(BigDecimal.ZERO) > 0) {
                    investedByMonth.merge(month, cumulativeValue, BigDecimal::add);
                }

                month = month.plusMonths(1);
            }
        }

        return investedByMonth;
    }

    private Map<YearMonth, BigDecimal> calculateMonthlyCapitalGains() {
        Map<YearMonth, BigDecimal> monthlyGains = new TreeMap<>();

        List<Dividend> allDividends = tickerService.getAllDividends();
        for (Dividend dividend : allDividends) {
            YearMonth month = YearMonth.from(dividend.getWalletTransaction().getDate());
            BigDecimal dividendValue = dividend.getWalletTransaction().getAmount();
            monthlyGains.merge(month, dividendValue, BigDecimal::add);
        }

        List<BondOperation> allOperations = bondService.getAllOperations();
        for (BondOperation operation : allOperations) {
            if (operation.getOperationType() == OperationType.SELL
                    && operation.getNetProfit() != null) {
                YearMonth month = YearMonth.from(operation.getWalletTransaction().getDate());
                monthlyGains.merge(month, operation.getNetProfit(), BigDecimal::add);
            }
        }

        List<TickerSale> tickerSales = tickerService.getAllNonArchivedSales();
        for (TickerSale sale : tickerSales) {
            YearMonth month = YearMonth.from(sale.getWalletTransaction().getDate());
            BigDecimal saleValue = sale.getUnitPrice().multiply(sale.getQuantity());
            BigDecimal costBasis = sale.getAverageCost().multiply(sale.getQuantity());
            BigDecimal profitLoss = saleValue.subtract(costBasis);
            monthlyGains.merge(month, profitLoss, BigDecimal::add);
        }

        Map<YearMonth, BigDecimal> tickerGainsByMonth = calculateTickerAppreciationByMonth();

        for (Map.Entry<YearMonth, BigDecimal> entry : tickerGainsByMonth.entrySet()) {
            monthlyGains.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
        }

        return monthlyGains;
    }

    private Map<YearMonth, BigDecimal> calculateAccumulatedCapitalGains() {
        Map<YearMonth, BigDecimal> accumulatedGains = new TreeMap<>();

        List<Ticker> allTickers = tickerService.getAllNonArchivedTickers();
        List<Dividend> allDividends = tickerService.getAllDividends();

        YearMonth firstMonth = YearMonth.of(2021, 2);
        YearMonth currentMonth = YearMonth.now();

        YearMonth month = firstMonth;
        while (!month.isAfter(currentMonth)) {
            BigDecimal monthAccumulatedGain = BigDecimal.ZERO;

            for (Ticker ticker : allTickers) {
                List<TickerPurchase> purchases =
                        tickerService.getAllPurchasesByTicker(ticker.getId());
                List<TickerSale> sales = tickerService.getAllSalesByTicker(ticker.getId());

                LocalDate monthEnd = month.atEndOfMonth();
                BigDecimal quantityAtMonthEnd =
                        calculateQuantityAtDate(ticker, purchases, sales, monthEnd);

                if (quantityAtMonthEnd.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal costBasis = ticker.getAverageUnitValue().multiply(quantityAtMonthEnd);

                Optional<BigDecimal> priceAtMonthEnd =
                        tickerPriceHistoryService.getClosestPriceBeforeDate(
                                ticker.getId(), monthEnd);

                if (priceAtMonthEnd.isEmpty()) {
                    continue;
                }

                BigDecimal currentValue = priceAtMonthEnd.get().multiply(quantityAtMonthEnd);
                BigDecimal unrealizedGain = currentValue.subtract(costBasis);

                monthAccumulatedGain = monthAccumulatedGain.add(unrealizedGain);
            }

            final YearMonth finalMonth = month;
            BigDecimal accumulatedDividends =
                    allDividends.stream()
                            .filter(
                                    d ->
                                            !d.getWalletTransaction()
                                                    .getDate()
                                                    .toLocalDate()
                                                    .isAfter(finalMonth.atEndOfMonth()))
                            .map(d -> d.getWalletTransaction().getAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthAccumulatedGain = monthAccumulatedGain.add(accumulatedDividends);

            accumulatedGains.put(month, monthAccumulatedGain);
            month = month.plusMonths(1);
        }

        return accumulatedGains;
    }

    private Map<YearMonth, BigDecimal> calculateMonthlyPortfolioValue() {
        Map<YearMonth, BigDecimal> portfolioByMonth = new TreeMap<>();

        List<Ticker> allTickers = tickerService.getAllNonArchivedTickers();

        for (Ticker ticker : allTickers) {
            List<TickerPurchase> purchases = tickerService.getAllPurchasesByTicker(ticker.getId());
            List<TickerSale> sales = tickerService.getAllSalesByTicker(ticker.getId());

            if (purchases.isEmpty()
                    && ticker.getCurrentQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            LocalDate referenceDate;
            if (!purchases.isEmpty()) {
                referenceDate =
                        purchases.stream()
                                .map(p -> p.getWalletTransaction().getDate().toLocalDate())
                                .min(LocalDate::compareTo)
                                .orElse(null);
            } else {
                referenceDate =
                        LocalDateTime.parse(ticker.getCreatedAt(), Constants.DB_DATE_FORMATTER)
                                .toLocalDate();
            }

            if (referenceDate == null) {
                continue;
            }

            YearMonth firstMonth = YearMonth.from(referenceDate);
            Map<YearMonth, BigDecimal> quantityAtMonthEnd =
                    calculateQuantityAtMonthEnd(ticker, purchases, sales, firstMonth);

            for (Map.Entry<YearMonth, BigDecimal> entry : quantityAtMonthEnd.entrySet()) {
                YearMonth month = entry.getKey();
                BigDecimal quantity = entry.getValue();

                if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                LocalDate endDate;
                YearMonth currentMonth = YearMonth.now();
                if (month.equals(currentMonth)) {
                    endDate = LocalDate.now();
                } else {
                    endDate = month.atEndOfMonth();
                }

                Optional<BigDecimal> priceOpt =
                        tickerPriceHistoryService.getClosestPriceBeforeDate(
                                ticker.getId(), endDate);

                if (priceOpt.isEmpty()) {
                    continue;
                }

                BigDecimal price = priceOpt.get();
                BigDecimal value = quantity.multiply(price);

                portfolioByMonth.merge(month, value, BigDecimal::add);
            }
        }

        List<Bond> allBonds = bondService.getAllNonArchivedBonds();
        for (Bond bond : allBonds) {
            List<BondOperation> operations = bondService.getOperationsByBond(bond);

            if (operations.isEmpty()) {
                continue;
            }

            LocalDate firstOperationDate =
                    operations.stream()
                            .map(op -> op.getWalletTransaction().getDate().toLocalDate())
                            .min(LocalDate::compareTo)
                            .orElse(null);

            if (firstOperationDate == null) {
                continue;
            }

            YearMonth firstMonth = YearMonth.from(firstOperationDate);
            YearMonth currentMonth = YearMonth.now();

            BigDecimal cumulativeValue = BigDecimal.ZERO;
            YearMonth month = firstMonth;

            while (!month.isAfter(currentMonth)) {
                for (BondOperation op : operations) {
                    YearMonth opMonth = YearMonth.from(op.getWalletTransaction().getDate());
                    if (opMonth.equals(month)) {
                        BigDecimal opValue = op.getUnitPrice().multiply(op.getQuantity());
                        if (op.getOperationType() == OperationType.BUY) {
                            cumulativeValue = cumulativeValue.add(opValue);
                        } else if (op.getOperationType() == OperationType.SELL) {
                            cumulativeValue = cumulativeValue.subtract(opValue);
                        }
                    }
                }

                if (cumulativeValue.compareTo(BigDecimal.ZERO) > 0) {
                    portfolioByMonth.merge(month, cumulativeValue, BigDecimal::add);
                }

                month = month.plusMonths(1);
            }
        }

        return portfolioByMonth;
    }

    private Map<YearMonth, BigDecimal> calculateTickerAppreciationByMonth() {
        Map<YearMonth, BigDecimal> appreciationByMonth = new TreeMap<>();

        List<Ticker> allTickers = tickerService.getAllNonArchivedTickers();

        for (Ticker ticker : allTickers) {
            List<TickerPurchase> purchases = tickerService.getAllPurchasesByTicker(ticker.getId());
            List<TickerSale> sales = tickerService.getAllSalesByTicker(ticker.getId());

            if (purchases.isEmpty()
                    && ticker.getCurrentQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            LocalDate referenceDate;
            if (!purchases.isEmpty()) {
                referenceDate =
                        purchases.stream()
                                .map(p -> p.getWalletTransaction().getDate().toLocalDate())
                                .min(LocalDate::compareTo)
                                .orElse(null);
            } else {
                referenceDate =
                        LocalDateTime.parse(ticker.getCreatedAt(), Constants.DB_DATE_FORMATTER)
                                .toLocalDate();
            }

            if (referenceDate == null) {
                continue;
            }

            YearMonth firstMonth = YearMonth.from(referenceDate);
            YearMonth currentMonth = YearMonth.now();

            YearMonth month = firstMonth;
            while (!month.isAfter(currentMonth)) {
                BigDecimal monthAppreciation =
                        calculateMonthAppreciationWithIntraMonthTransactions(
                                ticker, purchases, sales, month, referenceDate, firstMonth);

                if (monthAppreciation.compareTo(BigDecimal.ZERO) != 0) {
                    appreciationByMonth.merge(month, monthAppreciation, BigDecimal::add);
                }

                month = month.plusMonths(1);
            }
        }

        return appreciationByMonth;
    }

    private BigDecimal calculateMonthAppreciationWithIntraMonthTransactions(
            Ticker ticker,
            List<TickerPurchase> purchases,
            List<TickerSale> sales,
            YearMonth month,
            LocalDate referenceDate,
            YearMonth firstMonth) {

        List<LocalDate> transactionDatesInMonth = new ArrayList<>();
        transactionDatesInMonth.add(month.atDay(1));

        for (TickerPurchase purchase : purchases) {
            LocalDate purchaseDate = purchase.getWalletTransaction().getDate().toLocalDate();
            if (YearMonth.from(purchaseDate).equals(month)) {
                transactionDatesInMonth.add(purchaseDate);
            }
        }

        for (TickerSale sale : sales) {
            LocalDate saleDate = sale.getWalletTransaction().getDate().toLocalDate();
            if (YearMonth.from(saleDate).equals(month)) {
                transactionDatesInMonth.add(saleDate);
            }
        }

        transactionDatesInMonth.add(month.atEndOfMonth());
        transactionDatesInMonth = transactionDatesInMonth.stream().distinct().sorted().toList();

        BigDecimal quantityAtMonthStart =
                calculateQuantityAtDate(ticker, purchases, sales, month.atDay(1));

        if (quantityAtMonthStart.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalAppreciation = BigDecimal.ZERO;

        for (int i = 0; i < transactionDatesInMonth.size() - 1; i++) {
            LocalDate periodStart = transactionDatesInMonth.get(i);
            LocalDate periodEnd = transactionDatesInMonth.get(i + 1);

            BigDecimal periodQuantity =
                    calculateQuantityAtDate(ticker, purchases, sales, periodStart);

            if (periodQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Optional<BigDecimal> startPriceOpt =
                    tickerPriceHistoryService.getClosestPriceBeforeDate(
                            ticker.getId(), periodStart);
            Optional<BigDecimal> endPriceOpt =
                    tickerPriceHistoryService.getClosestPriceBeforeDate(ticker.getId(), periodEnd);

            if (startPriceOpt.isEmpty() || endPriceOpt.isEmpty()) {
                continue;
            }

            BigDecimal startPrice = startPriceOpt.get();
            BigDecimal endPrice = endPriceOpt.get();
            BigDecimal periodAppreciation = endPrice.subtract(startPrice).multiply(periodQuantity);

            totalAppreciation = totalAppreciation.add(periodAppreciation);
        }

        return totalAppreciation;
    }

    private BigDecimal calculateQuantityAtDate(
            Ticker ticker, List<TickerPurchase> purchases, List<TickerSale> sales, LocalDate date) {

        BigDecimal initialQuantity = ticker.getCurrentQuantity();
        for (TickerPurchase purchase : purchases) {
            initialQuantity = initialQuantity.subtract(purchase.getQuantity());
        }
        for (TickerSale sale : sales) {
            initialQuantity = initialQuantity.add(sale.getQuantity());
        }

        BigDecimal quantity = initialQuantity;

        for (TickerPurchase purchase : purchases) {
            LocalDate purchaseDate = purchase.getWalletTransaction().getDate().toLocalDate();
            if (!purchaseDate.isAfter(date)) {
                quantity = quantity.add(purchase.getQuantity());
            }
        }

        for (TickerSale sale : sales) {
            LocalDate saleDate = sale.getWalletTransaction().getDate().toLocalDate();
            if (!saleDate.isAfter(date)) {
                quantity = quantity.subtract(sale.getQuantity());
            }
        }

        return quantity;
    }

    private Map<YearMonth, BigDecimal> calculateQuantityAtMonthStart(
            Ticker ticker,
            List<TickerPurchase> purchases,
            List<TickerSale> sales,
            YearMonth firstMonth) {
        Map<YearMonth, BigDecimal> quantityAtStart = new TreeMap<>();

        Map<YearMonth, BigDecimal> quantityAtEnd = new TreeMap<>();

        BigDecimal initialQuantity = ticker.getCurrentQuantity();
        for (TickerPurchase purchase : purchases) {
            initialQuantity = initialQuantity.subtract(purchase.getQuantity());
        }
        for (TickerSale sale : sales) {
            initialQuantity = initialQuantity.add(sale.getQuantity());
        }

        BigDecimal cumulativeQuantity = initialQuantity;

        for (TickerPurchase purchase : purchases) {
            YearMonth month = YearMonth.from(purchase.getWalletTransaction().getDate());
            cumulativeQuantity = cumulativeQuantity.add(purchase.getQuantity());
            quantityAtEnd.put(month, cumulativeQuantity);
        }

        for (TickerSale sale : sales) {
            YearMonth month = YearMonth.from(sale.getWalletTransaction().getDate());
            cumulativeQuantity = cumulativeQuantity.subtract(sale.getQuantity());
            quantityAtEnd.put(month, cumulativeQuantity);
        }

        YearMonth currentMonth = YearMonth.now();
        YearMonth month = firstMonth;
        BigDecimal lastQuantity = initialQuantity;

        while (!month.isAfter(currentMonth)) {
            if (quantityAtEnd.containsKey(month)) {
                lastQuantity = quantityAtEnd.get(month);
            } else {
                quantityAtEnd.put(month, lastQuantity);
            }
            month = month.plusMonths(1);
        }

        month = firstMonth;
        BigDecimal previousMonthEnd = initialQuantity;
        while (!month.isAfter(currentMonth)) {
            if (month.equals(firstMonth)) {
                quantityAtStart.put(month, initialQuantity);
            } else {
                quantityAtStart.put(month, previousMonthEnd);
            }
            previousMonthEnd = quantityAtEnd.getOrDefault(month, previousMonthEnd);
            month = month.plusMonths(1);
        }

        return quantityAtStart;
    }

    private Map<YearMonth, BigDecimal> calculateQuantityAtMonthEnd(
            Ticker ticker,
            List<TickerPurchase> purchases,
            List<TickerSale> sales,
            YearMonth firstMonth) {
        Map<YearMonth, BigDecimal> quantityAtEnd = new TreeMap<>();

        BigDecimal initialQuantity = ticker.getCurrentQuantity();
        for (TickerPurchase purchase : purchases) {
            initialQuantity = initialQuantity.subtract(purchase.getQuantity());
        }
        for (TickerSale sale : sales) {
            initialQuantity = initialQuantity.add(sale.getQuantity());
        }

        BigDecimal cumulativeQuantity = initialQuantity;

        List<Object> allTransactions = new ArrayList<>();
        for (TickerPurchase purchase : purchases) {
            allTransactions.add(purchase);
        }
        for (TickerSale sale : sales) {
            allTransactions.add(sale);
        }

        allTransactions.sort(
                (a, b) -> {
                    LocalDateTime dateA =
                            a instanceof TickerPurchase
                                    ? ((TickerPurchase) a).getWalletTransaction().getDate()
                                    : ((TickerSale) a).getWalletTransaction().getDate();
                    LocalDateTime dateB =
                            b instanceof TickerPurchase
                                    ? ((TickerPurchase) b).getWalletTransaction().getDate()
                                    : ((TickerSale) b).getWalletTransaction().getDate();
                    return dateA.compareTo(dateB);
                });

        for (Object transaction : allTransactions) {
            if (transaction instanceof TickerPurchase) {
                TickerPurchase purchase = (TickerPurchase) transaction;
                YearMonth month = YearMonth.from(purchase.getWalletTransaction().getDate());
                cumulativeQuantity = cumulativeQuantity.add(purchase.getQuantity());
                quantityAtEnd.put(month, cumulativeQuantity);
            } else if (transaction instanceof TickerSale) {
                TickerSale sale = (TickerSale) transaction;
                YearMonth month = YearMonth.from(sale.getWalletTransaction().getDate());
                cumulativeQuantity = cumulativeQuantity.subtract(sale.getQuantity());
                quantityAtEnd.put(month, cumulativeQuantity);
            }
        }

        YearMonth currentMonth = YearMonth.now();
        YearMonth month = firstMonth;
        BigDecimal lastQuantity = initialQuantity;

        while (!month.isAfter(currentMonth)) {
            if (!quantityAtEnd.containsKey(month)) {
                quantityAtEnd.put(month, lastQuantity);
            } else {
                lastQuantity = quantityAtEnd.get(month);
            }
            month = month.plusMonths(1);
        }

        return quantityAtEnd;
    }
}
