/*
 * Filename: TickerPriceHistoryService.java
 * Created on: February 17, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.NoArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.moinex.model.investment.Ticker;
import org.moinex.model.investment.TickerPriceHistory;
import org.moinex.repository.investment.TickerPriceHistoryRepository;
import org.moinex.repository.investment.TickerPurchaseRepository;
import org.moinex.repository.investment.TickerRepository;
import org.moinex.repository.investment.TickerSaleRepository;
import org.moinex.util.APIUtils;
import org.moinex.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing ticker price history
 */
@Service
@NoArgsConstructor
public class TickerPriceHistoryService {
    private static final Logger logger = LoggerFactory.getLogger(TickerPriceHistoryService.class);

    private TickerPriceHistoryRepository priceHistoryRepository;
    private TickerRepository tickerRepository;
    private TickerPurchaseRepository tickerPurchaseRepository;
    private TickerSaleRepository tickerSaleRepository;

    public static final Integer MAX_RETRIES = 7;
    public static final Integer RETRY_DELAY_MS = 2000;
    public static final Double RETRY_MULTIPLIER = 1.5;

    @Autowired
    public TickerPriceHistoryService(
            TickerPriceHistoryRepository priceHistoryRepository,
            TickerRepository tickerRepository,
            TickerPurchaseRepository tickerPurchaseRepository,
            TickerSaleRepository tickerSaleRepository) {
        this.priceHistoryRepository = priceHistoryRepository;
        this.tickerRepository = tickerRepository;
        this.tickerPurchaseRepository = tickerPurchaseRepository;
        this.tickerSaleRepository = tickerSaleRepository;
    }

    /**
     * Store a price history entry
     * @param ticker The ticker
     * @param priceDate The price date
     * @param closingPrice The closing price
     * @param isMonthEnd Whether this is a month-end price
     */
    @Transactional
    public void storePriceHistory(
            Ticker ticker, LocalDate priceDate, BigDecimal closingPrice, boolean isMonthEnd) {
        String dateStr = priceDate.format(Constants.DATE_FORMATTER_NO_TIME);
        YearMonth priceMonth = YearMonth.from(priceDate);
        YearMonth currentMonth = YearMonth.now();

        Optional<TickerPriceHistory> existing =
                priceHistoryRepository.findByTickerIdAndDate(ticker.getId(), dateStr);

        if (existing.isPresent()) {
            TickerPriceHistory history = existing.get();
            history.setClosingPrice(closingPrice);
            history.setMonthEnd(isMonthEnd);
            priceHistoryRepository.save(history);
            logger.debug("Updated price for {} on {}", ticker.getSymbol(), priceDate);
        } else {
            // For current month: delete any existing record and create new one
            // This ensures we only have 1 record per month (the most recent)
            if (priceMonth.equals(currentMonth) && !isMonthEnd) {
                deleteCurrentMonthPrice(ticker.getId(), priceMonth);
            }

            TickerPriceHistory history =
                    TickerPriceHistory.builder()
                            .ticker(ticker)
                            .priceDate(dateStr)
                            .closingPrice(closingPrice)
                            .isMonthEnd(isMonthEnd)
                            .build();

            priceHistoryRepository.save(history);
            logger.debug("Stored new price for {} on {}", ticker.getSymbol(), priceDate);
        }
    }

    /**
     * Delete any existing price record for the current month
     * This ensures we only keep the most recent price for the current month
     * @param tickerId The ticker ID
     * @param month The month to clean up
     */
    private void deleteCurrentMonthPrice(Integer tickerId, YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        String startDateStr = monthStart.format(Constants.DATE_FORMATTER_NO_TIME);
        String endDateStr = monthEnd.format(Constants.DATE_FORMATTER_NO_TIME);

        List<TickerPriceHistory> currentMonthPrices =
                priceHistoryRepository.findMonthEndPricesByTickerAndDateRange(
                        tickerId, startDateStr, endDateStr);

        for (TickerPriceHistory price : currentMonthPrices) {
            if (!price.isMonthEnd()) {
                priceHistoryRepository.delete(price);
                logger.debug(
                        "Deleted old current month price for ticker {} on {}",
                        tickerId,
                        price.getPriceDate());
            }
        }
    }

    /**
     * Fetch and store historical prices for a ticker from a start date
     * @param ticker The ticker
     * @param startDate The start date (usually first purchase date)
     * @return CompletableFuture that completes when prices are stored
     */
    @Transactional
    public CompletableFuture<Void> fetchAndStoreHistoricalPrices(
            Ticker ticker, LocalDate startDate) {
        return fetchAndStoreHistoricalPrices(ticker, startDate, LocalDate.now());
    }

    /**
     * Fetch and store historical prices for a ticker within a date range
     * @param ticker The ticker
     * @param startDate The start date
     * @param endDate The end date
     * @return CompletableFuture that completes when prices are stored
     */
    @Transactional
    public CompletableFuture<Void> fetchAndStoreHistoricalPrices(
            Ticker ticker, LocalDate startDate, LocalDate endDate) {
        return fetchAndStoreHistoricalPricesWithRetry(
                ticker, startDate, endDate, 1, RETRY_DELAY_MS);
    }

    /**
     * Fetch and store historical prices with retry and exponential backoff
     * @param ticker The ticker
     * @param startDate The start date
     * @param endDate The end date
     * @param attempt Current attempt number
     * @param retryDelayMs Delay before retry in milliseconds
     * @return CompletableFuture that completes when prices are stored
     */
    private CompletableFuture<Void> fetchAndStoreHistoricalPricesWithRetry(
            Ticker ticker, LocalDate startDate, LocalDate endDate, int attempt, int retryDelayMs) {

        if (attempt == 1) {
            logger.info(
                    "Fetching historical prices for {} from {} to {}",
                    ticker.getSymbol(),
                    startDate,
                    endDate);
        } else {
            logger.info(
                    "Retrying fetch for {} - Attempt {}/{}",
                    ticker.getSymbol(),
                    attempt,
                    MAX_RETRIES);
        }

        return APIUtils.fetchStockPriceHistoryAsync(
                        ticker.getSymbol(),
                        startDate.format(Constants.DATE_FORMATTER_NO_TIME),
                        endDate.format(Constants.DATE_FORMATTER_NO_TIME))
                .thenAccept(
                        jsonObject -> {
                            if (jsonObject.has("error")) {
                                String error = jsonObject.getString("error");
                                logger.error(
                                        "Error fetching price history for {}: {}",
                                        ticker.getSymbol(),
                                        error);
                                throw new RuntimeException(error);
                            }

                            JSONArray prices = jsonObject.getJSONArray("prices");
                            int storedCount = 0;

                            // Get all transaction dates for this ticker
                            List<LocalDate> transactionDates = new java.util.ArrayList<>();

                            // Collect purchase dates
                            tickerPurchaseRepository.findAll().stream()
                                    .filter(
                                            purchase ->
                                                    purchase.getTicker()
                                                            .getId()
                                                            .equals(ticker.getId()))
                                    .map(
                                            purchase ->
                                                    purchase.getWalletTransaction()
                                                            .getDate()
                                                            .toLocalDate())
                                    .forEach(transactionDates::add);

                            // Collect sale dates
                            tickerSaleRepository.findAll().stream()
                                    .filter(sale -> sale.getTicker().getId().equals(ticker.getId()))
                                    .map(
                                            sale ->
                                                    sale.getWalletTransaction()
                                                            .getDate()
                                                            .toLocalDate())
                                    .forEach(transactionDates::add);

                            // Convert to Set for fast lookup
                            java.util.Set<LocalDate> transactionDatesSet =
                                    new java.util.HashSet<>(transactionDates);
                            YearMonth currentMonth = YearMonth.now();

                            // Find the most recent date in the response
                            LocalDate mostRecentDate = null;
                            for (int i = 0; i < prices.length(); i++) {
                                JSONObject priceData = prices.getJSONObject(i);
                                LocalDate date =
                                        LocalDate.parse(
                                                priceData.getString("date"),
                                                Constants.DATE_FORMATTER_NO_TIME);
                                if (mostRecentDate == null || date.isAfter(mostRecentDate)) {
                                    mostRecentDate = date;
                                }
                            }

                            for (int i = 0; i < prices.length(); i++) {
                                JSONObject priceData = prices.getJSONObject(i);
                                LocalDate date =
                                        LocalDate.parse(
                                                priceData.getString("date"),
                                                Constants.DATE_FORMATTER_NO_TIME);
                                BigDecimal price = BigDecimal.valueOf(priceData.getDouble("price"));
                                boolean isMonthEnd = priceData.getBoolean("is_month_end");

                                // Determine if this price should be stored
                                boolean shouldStore = false;

                                // 1. Store if it's a transaction date
                                if (transactionDatesSet.contains(date)) {
                                    shouldStore = true;
                                    logger.debug(
                                            "Storing {} for {}: transaction date",
                                            date,
                                            ticker.getSymbol());
                                }

                                // 2. Store if it's month-end of a past month
                                else if (isMonthEnd
                                        && YearMonth.from(date).isBefore(currentMonth)) {
                                    shouldStore = true;
                                    logger.debug(
                                            "Storing {} for {}: past month-end",
                                            date,
                                            ticker.getSymbol());
                                }

                                // 3. Store if it's the most recent price (current month's latest)
                                else if (date.equals(mostRecentDate)
                                        && YearMonth.from(date).equals(currentMonth)) {
                                    shouldStore = true;
                                    logger.debug(
                                            "Storing {} for {}: most recent price",
                                            date,
                                            ticker.getSymbol());
                                }

                                if (shouldStore) {
                                    storePriceHistory(ticker, date, price, isMonthEnd);
                                    storedCount++;
                                } else {
                                    logger.debug(
                                            "Skipping {} for {}: not relevant",
                                            date,
                                            ticker.getSymbol());
                                }
                            }

                            logger.info(
                                    "Stored {} price entries for {} (filtered from {} returned)",
                                    storedCount,
                                    ticker.getSymbol(),
                                    prices.length());
                        })
                .exceptionally(
                        e -> {
                            if (attempt >= MAX_RETRIES) {
                                logger.error(
                                        "Failed to fetch price history for {} after {} attempts:"
                                                + " {}",
                                        ticker.getSymbol(),
                                        MAX_RETRIES,
                                        e.getMessage());
                                return null;
                            }

                            logger.warn(
                                    "Failed to fetch price history for {} (attempt {}/{}): {}."
                                            + " Retrying in {} ms...",
                                    ticker.getSymbol(),
                                    attempt,
                                    MAX_RETRIES,
                                    e.getMessage(),
                                    retryDelayMs);

                            try {
                                Thread.sleep(retryDelayMs);
                                fetchAndStoreHistoricalPricesWithRetry(
                                                ticker,
                                                startDate,
                                                endDate,
                                                attempt + 1,
                                                (int) (retryDelayMs * RETRY_MULTIPLIER))
                                        .join();
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                logger.error("Retry interrupted for {}", ticker.getSymbol());
                            }
                            return null;
                        });
    }

    /**
     * Get the price of a ticker on a specific date (or the most recent price before that date)
     * @param tickerId The ticker ID
     * @param date The date
     * @return Optional containing the price if found
     */
    public Optional<BigDecimal> getPriceOnDate(Integer tickerId, LocalDate date) {
        String dateStr = date.format(Constants.DATE_FORMATTER_NO_TIME);
        return priceHistoryRepository
                .findMostRecentPriceBeforeDate(tickerId, dateStr)
                .map(TickerPriceHistory::getClosingPrice);
    }

    /**
     * Get the closest price before or on a specific date
     * Alias for getPriceOnDate for better readability
     * @param tickerId The ticker ID
     * @param date The date
     * @return Optional containing the price if found
     */
    public Optional<BigDecimal> getClosestPriceBeforeDate(Integer tickerId, LocalDate date) {
        return getPriceOnDate(tickerId, date);
    }

    /**
     * Check if historical data is complete for a ticker
     * Verifies if data exists from first purchase date (or before) until now
     * @param tickerId The ticker ID
     * @param firstPurchaseDate The date of first purchase or created_at
     * @return True if historical data is complete
     */
    public boolean hasCompleteHistoricalData(Integer tickerId, LocalDate firstPurchaseDate) {
        if (firstPurchaseDate == null) {
            return true; // No purchases, so no data needed
        }

        Optional<LocalDate> earliestDate = getEarliestPriceDate(tickerId);
        if (earliestDate.isEmpty()) {
            logger.debug("Ticker {} has no historical data", tickerId);
            return false; // No data at all
        }

        // With the new optimization strategy, we store:
        // 1. Transaction dates
        // 2. Month-end dates
        // 3. Most recent price
        // So we consider data complete if we have data from the same month or earlier
        YearMonth firstPurchaseMonth = YearMonth.from(firstPurchaseDate);
        YearMonth earliestDataMonth = YearMonth.from(earliestDate.get());

        boolean isComplete = !earliestDataMonth.isAfter(firstPurchaseMonth);

        logger.debug(
                "Ticker {}: firstPurchaseDate={}, earliestDataDate={}, isComplete={}",
                tickerId,
                firstPurchaseDate,
                earliestDate.get(),
                isComplete);

        return isComplete;
    }

    /**
     * Get the earliest price date for a ticker
     * @param tickerId The ticker ID
     * @return Optional containing the earliest date
     */
    public Optional<LocalDate> getEarliestPriceDate(Integer tickerId) {
        return priceHistoryRepository
                .findEarliestPriceByTicker(tickerId)
                .map(TickerPriceHistory::getPriceDate);
    }

    /**
     * Smart initialization: Update price history for all tickers
     * - For tickers without history: fetch from first purchase date
     * - For tickers with history: update only current month if needed
     * This method is designed to run asynchronously on application startup
     *
     * @param tickerPurchaseRepository Repository to get first purchase dates
     * @return CompletableFuture that completes when all updates are done
     */
    @Transactional
    public CompletableFuture<Void> initializePriceHistory(
            TickerPurchaseRepository tickerPurchaseRepository,
            TickerSaleRepository tickerSaleRepository) {
        return CompletableFuture.runAsync(
                () -> {
                    logger.info("Starting smart price history initialization");

                    List<Ticker> activeTickers =
                            tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc();

                    if (activeTickers.isEmpty()) {
                        logger.info(
                                "No active tickers found, skipping price history initialization");
                        return;
                    }

                    logger.info("Found {} active tickers to process", activeTickers.size());
                    logger.debug(
                            "Active tickers: {}",
                            activeTickers.stream()
                                    .map(t -> t.getSymbol() + " (ID=" + t.getId() + ")")
                                    .collect(java.util.stream.Collectors.joining(", ")));

                    int backfillCount = 0;
                    int updateCount = 0;
                    int skipCount = 0;

                    for (Ticker ticker : activeTickers) {
                        try {
                            LocalDate firstPurchaseDate =
                                    getFirstPurchaseDate(ticker.getId(), tickerPurchaseRepository);
                            LocalDateTime createdAt =
                                    LocalDateTime.parse(
                                            ticker.getCreatedAt(), Constants.DB_DATE_FORMATTER);
                            LocalDate createdDate = createdAt.toLocalDate();

                            LocalDate referenceDate;

                            if (firstPurchaseDate == null) {
                                if (ticker.getCurrentQuantity().compareTo(BigDecimal.ZERO) > 0) {
                                    // Ticker has quantity but no purchases - use createdAt as start
                                    // date
                                    referenceDate = createdDate;
                                    logger.info(
                                            "No purchases for {} (ID={}) but has quantity {}, using"
                                                    + " createdAt {} as start date",
                                            ticker.getSymbol(),
                                            ticker.getId(),
                                            ticker.getCurrentQuantity(),
                                            referenceDate);
                                } else {
                                    logger.info(
                                            "No purchases and no quantity for {} (ID={}), skipping",
                                            ticker.getSymbol(),
                                            ticker.getId());
                                    skipCount++;
                                    continue;
                                }
                            } else {
                                // Check if ticker has initial quantity before first purchase
                                BigDecimal initialQuantity = ticker.getCurrentQuantity();
                                List<org.moinex.model.investment.TickerPurchase> allPurchases =
                                        tickerPurchaseRepository.findAll().stream()
                                                .filter(
                                                        p ->
                                                                p.getTicker()
                                                                        .getId()
                                                                        .equals(ticker.getId()))
                                                .toList();
                                List<org.moinex.model.investment.TickerSale> allSales =
                                        tickerSaleRepository.findAll().stream()
                                                .filter(
                                                        s ->
                                                                s.getTicker()
                                                                        .getId()
                                                                        .equals(ticker.getId()))
                                                .toList();

                                for (org.moinex.model.investment.TickerPurchase purchase :
                                        allPurchases) {
                                    initialQuantity =
                                            initialQuantity.subtract(purchase.getQuantity());
                                }

                                for (org.moinex.model.investment.TickerSale sale : allSales) {
                                    initialQuantity = initialQuantity.add(sale.getQuantity());
                                }

                                if (initialQuantity.compareTo(BigDecimal.ZERO) > 0) {
                                    // Has initial quantity - start from creation date
                                    referenceDate = createdDate;
                                    logger.info(
                                            "Ticker {} (ID={}) has initial quantity {} before first"
                                                    + " purchase, using createdAt {} as start date",
                                            ticker.getSymbol(),
                                            ticker.getId(),
                                            initialQuantity,
                                            referenceDate);
                                } else {
                                    // No initial quantity - start from first purchase
                                    referenceDate = firstPurchaseDate;
                                }
                            }

                            logger.debug(
                                    "Checking {} (ID={}): referenceDate={}",
                                    ticker.getSymbol(),
                                    ticker.getId(),
                                    referenceDate);

                            if (!hasCompleteHistoricalData(ticker.getId(), referenceDate)) {
                                // Ticker has no data or incomplete data - fetch from reference date
                                logger.info(
                                        "Backfilling price history for {} (ID={}) from {}",
                                        ticker.getSymbol(),
                                        ticker.getId(),
                                        referenceDate);

                                fetchAndStoreHistoricalPrices(ticker, referenceDate).join();
                                backfillCount++;
                            } else {
                                // Ticker has complete historical data - check for missing
                                // transaction dates
                                List<LocalDate> transactionDates =
                                        getAllTransactionDates(
                                                ticker.getId(),
                                                tickerPurchaseRepository,
                                                tickerSaleRepository);
                                List<LocalDate> missingDates =
                                        getMissingPriceDates(ticker.getId(), transactionDates);

                                if (!missingDates.isEmpty()) {
                                    logger.info(
                                            "Fetching missing prices for {} on {} transaction"
                                                    + " dates: {}",
                                            ticker.getSymbol(),
                                            missingDates.size(),
                                            missingDates);

                                    fetchPricesForDates(ticker, missingDates).join();
                                    updateCount++;
                                }

                                // Check if current month needs update
                                if (needsCurrentMonthUpdate(ticker.getId())) {
                                    logger.info(
                                            "Updating current month price for {}",
                                            ticker.getSymbol());

                                    updateCurrentMonthPrice(ticker);
                                    updateCount++;
                                } else if (missingDates.isEmpty()) {
                                    logger.debug(
                                            "Price history for {} is up to date",
                                            ticker.getSymbol());
                                    skipCount++;
                                }
                            }
                        } catch (Exception e) {
                            logger.error(
                                    "Failed to update price history for {}: {}",
                                    ticker.getSymbol(),
                                    e.getMessage());
                        }
                    }

                    logger.info(
                            "Price history initialization complete: {} backfilled, {} updated, {}"
                                    + " skipped",
                            backfillCount,
                            updateCount,
                            skipCount);
                });
    }

    /**
     * Get the first purchase date for a ticker
     * @param tickerId The ticker ID
     * @param tickerPurchaseRepository Repository to query purchases
     * @return First purchase date or null if no purchases
     */
    private LocalDate getFirstPurchaseDate(
            Integer tickerId,
            org.moinex.repository.investment.TickerPurchaseRepository tickerPurchaseRepository) {
        return tickerPurchaseRepository.findAll().stream()
                .filter(purchase -> purchase.getTicker().getId().equals(tickerId))
                .map(purchase -> purchase.getWalletTransaction().getDate().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    /**
     * Get all transaction dates (purchases and sales) for a ticker
     * Also includes createdAt date if ticker has initial quantity before first purchase
     * @param tickerId The ticker ID
     * @param tickerPurchaseRepository Repository to query purchases
     * @param tickerSaleRepository Repository to query sales
     * @return List of all transaction dates
     */
    private List<LocalDate> getAllTransactionDates(
            Integer tickerId,
            org.moinex.repository.investment.TickerPurchaseRepository tickerPurchaseRepository,
            org.moinex.repository.investment.TickerSaleRepository tickerSaleRepository) {
        List<LocalDate> dates = new java.util.ArrayList<>();

        // Get ticker to check for initial quantity
        Optional<Ticker> tickerOpt = tickerRepository.findById(tickerId);
        if (tickerOpt.isEmpty()) {
            return dates;
        }

        Ticker ticker = tickerOpt.get();

        // Add purchase dates
        List<LocalDate> purchaseDates =
                tickerPurchaseRepository.findAll().stream()
                        .filter(purchase -> purchase.getTicker().getId().equals(tickerId))
                        .map(purchase -> purchase.getWalletTransaction().getDate().toLocalDate())
                        .toList();
        dates.addAll(purchaseDates);

        // Add sale dates
        tickerSaleRepository.findAll().stream()
                .filter(sale -> sale.getTicker().getId().equals(tickerId))
                .map(sale -> sale.getWalletTransaction().getDate().toLocalDate())
                .forEach(dates::add);

        // Check if ticker has initial quantity (before any purchases)
        // This includes two cases:
        // 1. Ticker with NO purchases but HAS current quantity (e.g., AMER3)
        // 2. Ticker WITH purchases but HAD initial quantity before first purchase (e.g., ITSA4)
        BigDecimal initialQuantity = ticker.getCurrentQuantity();

        // Calculate quantity from purchases
        for (org.moinex.model.investment.TickerPurchase purchase :
                tickerPurchaseRepository.findAll().stream()
                        .filter(p -> p.getTicker().getId().equals(tickerId))
                        .toList()) {
            initialQuantity = initialQuantity.subtract(purchase.getQuantity());
        }

        // Add back sales
        for (org.moinex.model.investment.TickerSale sale :
                tickerSaleRepository.findAll().stream()
                        .filter(s -> s.getTicker().getId().equals(tickerId))
                        .toList()) {
            initialQuantity = initialQuantity.add(sale.getQuantity());
        }

        // If there was initial quantity (before any purchases), add createdAt as a transaction date
        // This is the reference date for calculating appreciation of the initial quantity
        if (initialQuantity.compareTo(BigDecimal.ZERO) > 0) {
            LocalDateTime createdAt =
                    LocalDateTime.parse(ticker.getCreatedAt(), Constants.DB_DATE_FORMATTER);
            LocalDate createdDate = createdAt.toLocalDate();
            dates.add(createdDate);
            logger.debug(
                    "Ticker {} has initial quantity {}, adding createdAt {} as transaction date",
                    ticker.getSymbol(),
                    initialQuantity,
                    createdDate);
        }

        return dates;
    }

    /**
     * Check if price history exists for all transaction dates
     * @param tickerId The ticker ID
     * @param transactionDates List of transaction dates to check
     * @return List of dates missing price history
     */
    private List<LocalDate> getMissingPriceDates(
            Integer tickerId, List<LocalDate> transactionDates) {
        List<LocalDate> missingDates = new java.util.ArrayList<>();

        for (LocalDate date : transactionDates) {
            String dateStr = date.format(Constants.DATE_FORMATTER_NO_TIME);
            if (!priceHistoryRepository.existsByTickerIdAndDate(tickerId, dateStr)) {
                Optional<BigDecimal> priceBeforeDate = getClosestPriceBeforeDate(tickerId, date);

                if (priceBeforeDate.isEmpty()) {
                    missingDates.add(date);
                } else {
                    // Check if the price we found is recent enough (within 7 days)
                    Optional<LocalDate> priceDate =
                            priceHistoryRepository
                                    .findMostRecentPriceBeforeDate(tickerId, dateStr)
                                    .map(TickerPriceHistory::getPriceDate);

                    if (priceDate.isPresent()) {
                        missingDates.add(date);
                    }
                }
            }
        }

        return missingDates;
    }

    /**
     * Fetch and store prices for specific dates
     * @param ticker The ticker
     * @param dates List of dates to fetch prices for
     * @return CompletableFuture that completes when prices are stored
     */
    private CompletableFuture<Void> fetchPricesForDates(Ticker ticker, List<LocalDate> dates) {
        if (dates.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // Group dates by month to minimize API calls
        java.util.Map<YearMonth, List<LocalDate>> datesByMonth =
                dates.stream().collect(java.util.stream.Collectors.groupingBy(YearMonth::from));

        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        for (java.util.Map.Entry<YearMonth, List<LocalDate>> entry : datesByMonth.entrySet()) {
            YearMonth month = entry.getKey();
            LocalDate startDate = month.atDay(1);
            LocalDate endDate = month.atEndOfMonth();
            List<LocalDate> monthDates = entry.getValue();

            // Convert dates to string format for API
            List<String> dateStrings =
                    monthDates.stream()
                            .map(d -> d.format(Constants.DATE_FORMATTER_NO_TIME))
                            .toList();

            // Fetch only specific dates + month-end
            CompletableFuture<Void> future =
                    fetchAndStoreHistoricalPricesWithSpecificDates(
                            ticker, startDate, endDate, dateStrings);
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Fetch and store historical prices for specific dates within a range
     * @param ticker The ticker
     * @param startDate The start date
     * @param endDate The end date
     * @param specificDates List of specific dates to fetch (in YYYY-MM-DD format)
     * @return CompletableFuture that completes when prices are stored
     */
    private CompletableFuture<Void> fetchAndStoreHistoricalPricesWithSpecificDates(
            Ticker ticker, LocalDate startDate, LocalDate endDate, List<String> specificDates) {
        return fetchAndStoreHistoricalPricesWithSpecificDatesRetry(
                ticker, startDate, endDate, specificDates, 1, RETRY_DELAY_MS);
    }

    /**
     * Fetch and store historical prices for specific dates within a range with retry logic
     * @param ticker The ticker
     * @param startDate The start date
     * @param endDate The end date
     * @param specificDates List of specific dates to fetch (in YYYY-MM-DD format)
     * @param attempt Current attempt number
     * @param retryDelayMs Delay before retry in milliseconds
     * @return CompletableFuture that completes when prices are stored
     */
    private CompletableFuture<Void> fetchAndStoreHistoricalPricesWithSpecificDatesRetry(
            Ticker ticker,
            LocalDate startDate,
            LocalDate endDate,
            List<String> specificDates,
            int attempt,
            int retryDelayMs) {

        if (attempt == 1) {
            logger.info(
                    "Fetching prices for {} from {} to {} (specific dates: {})",
                    ticker.getSymbol(),
                    startDate,
                    endDate,
                    specificDates);
        } else {
            logger.info(
                    "Retrying fetch for {} - Attempt {}/{}",
                    ticker.getSymbol(),
                    attempt,
                    MAX_RETRIES);
        }

        return APIUtils.fetchStockPriceHistoryAsync(
                        ticker.getSymbol(),
                        startDate.format(Constants.DATE_FORMATTER_NO_TIME),
                        endDate.format(Constants.DATE_FORMATTER_NO_TIME),
                        specificDates)
                .thenAccept(
                        jsonObject -> {
                            if (jsonObject.has("error")) {
                                String error = jsonObject.getString("error");
                                logger.error(
                                        "Error fetching price history for {}: {}",
                                        ticker.getSymbol(),
                                        error);
                                throw new RuntimeException(error);
                            }

                            JSONArray prices = jsonObject.getJSONArray("prices");
                            int storedCount = 0;

                            // Convert specific dates to Set for fast lookup
                            java.util.Set<String> specificDatesSet =
                                    new java.util.HashSet<>(specificDates);
                            YearMonth currentMonth = YearMonth.now();

                            // Find the most recent date in the response
                            LocalDate mostRecentDate = null;
                            for (int i = 0; i < prices.length(); i++) {
                                JSONObject priceData = prices.getJSONObject(i);
                                LocalDate date =
                                        LocalDate.parse(
                                                priceData.getString("date"),
                                                Constants.DATE_FORMATTER_NO_TIME);
                                if (mostRecentDate == null || date.isAfter(mostRecentDate)) {
                                    mostRecentDate = date;
                                }
                            }

                            for (int i = 0; i < prices.length(); i++) {
                                JSONObject priceData = prices.getJSONObject(i);
                                LocalDate date =
                                        LocalDate.parse(
                                                priceData.getString("date"),
                                                Constants.DATE_FORMATTER_NO_TIME);
                                BigDecimal price = BigDecimal.valueOf(priceData.getDouble("price"));
                                boolean isMonthEnd = priceData.getBoolean("is_month_end");
                                String dateStr = date.format(Constants.DATE_FORMATTER_NO_TIME);

                                // Determine if this price should be stored
                                boolean shouldStore = false;

                                // 1. Store if it's a transaction date (specific date requested)
                                if (specificDatesSet.contains(dateStr)) {
                                    shouldStore = true;
                                    logger.debug(
                                            "Storing {} for {}: transaction date",
                                            dateStr,
                                            ticker.getSymbol());
                                }

                                // 2. Store if it's month-end of a past month
                                else if (isMonthEnd
                                        && YearMonth.from(date).isBefore(currentMonth)) {
                                    shouldStore = true;
                                    logger.debug(
                                            "Storing {} for {}: past month-end",
                                            dateStr,
                                            ticker.getSymbol());
                                }

                                // 3. Store if it's the most recent price (current month's latest)
                                else if (date.equals(mostRecentDate)
                                        && YearMonth.from(date).equals(currentMonth)) {
                                    shouldStore = true;
                                    logger.debug(
                                            "Storing {} for {}: most recent price",
                                            dateStr,
                                            ticker.getSymbol());
                                }

                                if (shouldStore) {
                                    storePriceHistory(ticker, date, price, isMonthEnd);
                                    storedCount++;
                                } else {
                                    logger.debug(
                                            "Skipping {} for {}: not relevant",
                                            dateStr,
                                            ticker.getSymbol());
                                }
                            }

                            logger.info(
                                    "Stored {} price entries for {} (filtered from {} returned,"
                                            + " requested {} specific dates)",
                                    storedCount,
                                    ticker.getSymbol(),
                                    prices.length(),
                                    specificDates.size());
                        })
                .exceptionally(
                        e -> {
                            if (attempt >= MAX_RETRIES) {
                                logger.error(
                                        "Failed to fetch specific dates for {} after {} attempts:"
                                                + " {}",
                                        ticker.getSymbol(),
                                        MAX_RETRIES,
                                        e.getMessage());
                                return null;
                            }

                            logger.warn(
                                    "Failed to fetch specific dates for {} (attempt {}/{}): {}."
                                            + " Retrying in {} ms...",
                                    ticker.getSymbol(),
                                    attempt,
                                    MAX_RETRIES,
                                    e.getMessage(),
                                    retryDelayMs);

                            try {
                                Thread.sleep(retryDelayMs);
                                fetchAndStoreHistoricalPricesWithSpecificDatesRetry(
                                                ticker,
                                                startDate,
                                                endDate,
                                                specificDates,
                                                attempt + 1,
                                                (int) (retryDelayMs * RETRY_MULTIPLIER))
                                        .join();
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                logger.error("Retry interrupted for {}", ticker.getSymbol());
                            }
                            return null;
                        });
    }

    /**
     * Check if current month price needs to be updated
     * @param tickerId The ticker ID
     * @return True if current month needs update
     */
    private boolean needsCurrentMonthUpdate(Integer tickerId) {
        LocalDate today = LocalDate.now();
        String todayStr = today.format(Constants.DATE_FORMATTER_NO_TIME);

        // Check if we already have a price for today
        return !priceHistoryRepository.existsByTickerIdAndDate(tickerId, todayStr);
    }

    /**
     * Update the current month price for a ticker
     * Replaces any existing current month price with today's price
     * @param ticker The ticker
     */
    @Transactional
    public void updateCurrentMonthPrice(Ticker ticker) {
        LocalDate today = LocalDate.now();
        LocalDate lastDayOfMonth = YearMonth.now().atEndOfMonth();
        boolean isMonthEnd = today.equals(lastDayOfMonth);

        storePriceHistory(ticker, today, ticker.getCurrentUnitValue(), isMonthEnd);

        logger.info(
                "Updated current month price for {} to {} on {} (month-end: {})",
                ticker.getSymbol(),
                ticker.getCurrentUnitValue(),
                today,
                isMonthEnd);
    }
}
