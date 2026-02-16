/*
 * Filename: TickerService.java
 * Created on: January  6, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.NoArgsConstructor;
import org.json.JSONObject;
import org.moinex.error.MoinexException;
import org.moinex.model.Category;
import org.moinex.model.enums.TickerType;
import org.moinex.model.enums.TransactionStatus;
import org.moinex.model.investment.*;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.repository.investment.*;
import org.moinex.util.APIUtils;
import org.moinex.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing tickers
 */
@Service
@NoArgsConstructor
public class TickerService {
    private static final Logger logger = LoggerFactory.getLogger(TickerService.class);
    private TickerRepository tickerRepository;
    private TickerPurchaseRepository tickerPurchaseRepository;
    private TickerSaleRepository tickerSaleRepository;
    private DividendRepository dividendRepository;
    private CryptoExchangeRepository cryptoExchangeRepository;
    private WalletTransactionService walletTransactionService;

    public Integer MAX_RETRIES = 5;
    public static final Integer RETRY_DELAY_MS = 2000;
    public static final Double RETRY_MULTIPLIER = 1.5;

    @Autowired
    public TickerService(
            TickerRepository tickerRepository,
            TickerPurchaseRepository tickerPurchaseRepository,
            TickerSaleRepository tickerSaleRepository,
            DividendRepository dividendRepository,
            CryptoExchangeRepository cryptoExchangeRepository,
            WalletTransactionService walletTransactionService) {
        this.tickerRepository = tickerRepository;
        this.tickerPurchaseRepository = tickerPurchaseRepository;
        this.tickerSaleRepository = tickerSaleRepository;
        this.dividendRepository = dividendRepository;
        this.cryptoExchangeRepository = cryptoExchangeRepository;
        this.walletTransactionService = walletTransactionService;
    }

    /**
     * Registers a new ticker
     *
     * @param name         The name of the ticker
     * @param symbol       The symbol of the ticker
     * @param type         The type of the ticker
     * @param price        The price of the ticker
     * @param avgUnitPrice The average unit price of the ticker
     * @param quantity     The quantity of the ticker
     * @return The id of the registered ticker
     * @throws IllegalArgumentException If the name or symbol is empty
     * @throws EntityExistsException    If the ticker already exists
     * @throws IllegalArgumentException If the price is less than or equal to zero
     * @throws IllegalArgumentException If the quantity is less than zero
     * @throws IllegalArgumentException If the average unit price is lower than zero
     */
    @Transactional
    public Integer addTicker(
            String name,
            String symbol,
            TickerType type,
            BigDecimal price,
            BigDecimal avgUnitPrice,
            BigDecimal quantity) {
        // Remove leading and trailing whitespaces
        name = name.strip();
        symbol = symbol.strip();

        if (name.isBlank() || symbol.isBlank()) {
            throw new IllegalArgumentException("Name and symbol must not be empty");
        }

        if (tickerRepository.existsBySymbol(symbol)) {
            logger.warn("Ticker with symbol {} already exists", symbol);

            throw new EntityExistsException("Ticker with symbol " + symbol + " already exists");
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }

        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity must be greater than or equal to zero");
        }

        if (avgUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Average unit price must be greater than or equal to zero");
        }

        price = Constants.roundPrice(price, type);
        avgUnitPrice = Constants.roundPrice(avgUnitPrice, type);

        Ticker ticker =
                Ticker.builder()
                        .name(name)
                        .symbol(symbol)
                        .type(type)
                        .currentUnitValue(price)
                        .averageUnitValue(avgUnitPrice)
                        .averageUnitValueCount(BigDecimal.ONE)
                        .currentQuantity(quantity)
                        .lastUpdate(LocalDateTime.now())
                        .build();

        tickerRepository.save(ticker);

        logger.info("Ticker {} registered successfully", symbol);

        return ticker.getId();
    }

    /**
     * Deletes a ticker
     *
     * @param id The id of the ticker
     * @throws EntityNotFoundException If the ticker does not exist
     * @throws IllegalStateException   If the ticker has transactions associated with it
     */
    @Transactional
    public void deleteTicker(Integer id) {
        Ticker ticker =
                tickerRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Ticker with id %d not found and cannot be"
                                                                + " deleted",
                                                        id)));

        // Check if the ticker has transactions associated with it
        if (getTransactionCountByTicker(id) > 0) {
            throw new IllegalStateException(
                    String.format(
                            "Ticker with id %d has transactions associated with it and cannot be"
                                + " deleted. Remove the transactions first or archive the ticker",
                            id));
        }

        tickerRepository.delete(ticker);

        logger.info("Ticker with id {} was permanently deleted", id);
    }

    /**
     * Archives a ticker
     *
     * @param id The id of the ticker
     * @throws EntityNotFoundException If the ticker does not exist
     * @note This method is used to archive a ticker, which means that the ticker
     * will not be deleted from the database, but it will not use in the application
     * anymore
     */
    @Transactional
    public void archiveTicker(Integer id) {
        Ticker ticker =
                tickerRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Ticker with id %d not found and cannot be"
                                                                + " archived",
                                                        id)));
        ticker.setArchived(true);
        tickerRepository.save(ticker);

        logger.info("Ticker with id {} was archived", id);
    }

    /**
     * Unarchives a ticker
     *
     * @param id The id of the ticker
     * @throws EntityNotFoundException If the ticker does not exist
     * @note This method is used to unarchive a ticker, which means that the ticker
     * will be used in the application again
     */
    @Transactional
    public void unarchiveTicker(Integer id) {
        Ticker ticker =
                tickerRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Ticker with id %d not found and cannot be"
                                                                + " unarchived",
                                                        id)));

        ticker.setArchived(false);
        tickerRepository.save(ticker);

        logger.info("Ticker with id {} was unarchived", id);
    }

    /**
     * Update a ticker
     *
     * @param tk The ticker to be updated
     * @throws EntityNotFoundException  If the ticker does not exist
     * @throws IllegalArgumentException If the name or symbol is empty
     * @throws IllegalArgumentException If the price is less than or equal to zero
     * @throws IllegalArgumentException If the quantity is less than zero
     * @throws IllegalArgumentException If the average unit price is lower than zero
     */
    @Transactional
    public void updateTicker(Ticker tk) {
        Ticker oldTicker =
                tickerRepository
                        .findById(tk.getId())
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Ticker with id %d not found and cannot be"
                                                                + " updated",
                                                        tk.getId())));

        // Remove leading and trailing whitespaces
        tk.setName(tk.getName() != null ? tk.getName().strip() : null);
        tk.setSymbol(tk.getSymbol() != null ? tk.getSymbol().strip() : null);

        if (tk.getName() == null
                || tk.getName().isBlank()
                || tk.getSymbol() == null
                || tk.getSymbol().isBlank()) {
            throw new IllegalArgumentException("Name and symbol must not be empty");
        }

        if (tk.getCurrentUnitValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }

        if (tk.getCurrentQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity must be greater than or equal to zero");
        }

        if (tk.getAverageUnitValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Average unit price must be greater than or equal to zero");
        }

        tk.setCurrentUnitValue(Constants.roundPrice(tk.getCurrentUnitValue(), tk.getType()));
        tk.setAverageUnitValue(Constants.roundPrice(tk.getAverageUnitValue(), tk.getType()));

        oldTicker.setName(tk.getName());
        oldTicker.setSymbol(tk.getSymbol());
        oldTicker.setType(tk.getType());
        oldTicker.setCurrentUnitValue(tk.getCurrentUnitValue());
        oldTicker.setCurrentQuantity(tk.getCurrentQuantity());
        oldTicker.setAverageUnitValue(tk.getAverageUnitValue());
        oldTicker.setArchived(tk.isArchived());

        // If sold all holdings, reset the average price
        if (oldTicker.getCurrentQuantity().compareTo(BigDecimal.ZERO) == 0) {
            resetAveragePrice(oldTicker.getId());
        }

        tickerRepository.save(oldTicker);

        logger.info("Ticker with id {} was updated", tk.getId());
    }

    /**
     * Check if logo file exists for a given domain
     *
     * @param domain Company domain (e.g., "apple.com")
     * @return true if logo file exists, false otherwise
     */
    private boolean logoExists(String domain) {
        if (domain == null || domain.isEmpty()) {
            return false;
        }

        try {
            String filename =
                    domain.replace("https://", "")
                                    .replace("http://", "")
                                    .replace("www.", "")
                                    .split("/")[0]
                            + ".png";
            Path logoPath = Paths.get(Constants.LOGOS_DIR, filename);
            return Files.exists(logoPath);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Download logos for tickers that don't have them yet
     *
     * @param tickers List of tickers to check and download logos for
     */
    private void downloadMissingLogos(List<Ticker> tickers) {
        List<String> websitesToDownload =
                tickers.stream()
                        .filter(
                                ticker ->
                                        ticker.getDomain() != null && !ticker.getDomain().isEmpty())
                        .map(Ticker::getDomain)
                        .filter(domain -> !logoExists(domain))
                        .distinct()
                        .toList();

        if (websitesToDownload.isEmpty()) {
            logger.info("All logos already cached, skipping download");
            return;
        }

        logger.info("Downloading {} missing logos", websitesToDownload.size());

        try {
            APIUtils.fetchStockLogosAsync(websitesToDownload.toArray(new String[0]))
                    .thenAccept(
                            jsonObject -> {
                                int successCount = 0;
                                int failCount = 0;

                                for (String website : websitesToDownload) {
                                    if (jsonObject.has(website)) {
                                        JSONObject logoData = jsonObject.getJSONObject(website);
                                        if (logoData.has("logo_path")) {
                                            successCount++;
                                            logger.debug("Logo downloaded for {}", website);
                                        } else if (logoData.has("error")) {
                                            failCount++;
                                            logger.debug(
                                                    "Failed to download logo for {}: {}",
                                                    website,
                                                    logoData.getString("error"));
                                        }
                                    }
                                }

                                logger.info(
                                        "Logo download complete: {} successful, {} failed",
                                        successCount,
                                        failCount);
                            })
                    .exceptionally(
                            e -> {
                                logger.warn("Error downloading logos: {}", e.getMessage());
                                return null;
                            });
        } catch (Exception e) {
            logger.warn("Failed to initiate logo download: {}", e.getMessage());
        }
    }

    @Transactional
    public CompletableFuture<List<Ticker>> updateTickersPriceFromApiAsync(List<Ticker> tickers) {
        if (tickers.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("No tickers to update"));
        }

        return updateTickersPriceWithRetry(tickers, 1, RETRY_DELAY_MS);
    }

    private CompletableFuture<List<Ticker>> updateTickersPriceWithRetry(
            List<Ticker> tickers, int attempt, int retryDelayMs) {
        String[] symbols = tickers.stream().map(Ticker::getSymbol).toArray(String[]::new);

        logger.info("Updating ticker prices - Attempt {}/{}", attempt, MAX_RETRIES);

        return APIUtils.fetchStockPricesAsync(symbols)
                .thenApply(
                        jsonObject -> {
                            List<Ticker> failed = new ArrayList<>();

                            for (Ticker ticker : tickers) {
                                try {
                                    if (!jsonObject.has(ticker.getSymbol())) {
                                        failed.add(ticker);
                                        continue;
                                    }

                                    JSONObject tickerData =
                                            jsonObject.getJSONObject(ticker.getSymbol());

                                    if (tickerData.has("error")) {
                                        throw new RuntimeException(tickerData.getString("error"));
                                    }

                                    BigDecimal price =
                                            new BigDecimal(tickerData.get("price").toString());

                                    price = Constants.roundPrice(price, ticker.getType());

                                    ticker.setCurrentUnitValue(price);
                                    ticker.setLastUpdate(LocalDateTime.now());

                                    if (tickerData.has("website")) {
                                        ticker.setDomain(tickerData.getString("website"));
                                    }

                                    tickerRepository.save(ticker);

                                } catch (Exception e) {
                                    logger.warn(
                                            "Failed to update ticker {}: {}",
                                            ticker.getSymbol(),
                                            e.getMessage());
                                    failed.add(ticker);
                                }
                            }

                            // Download missing logos after updating prices
                            downloadMissingLogos(tickers);

                            return failed;
                        })
                .handle(
                        (failed, throwable) -> {
                            if (throwable == null) {
                                return CompletableFuture.completedFuture(failed);
                            }

                            if (attempt >= MAX_RETRIES) {
                                CompletableFuture<List<Ticker>> failedFuture =
                                        new CompletableFuture<>();

                                failedFuture.completeExceptionally(throwable);
                                return failedFuture;
                            }

                            logger.info("Retrying in {} ms...", retryDelayMs);

                            return CompletableFuture.runAsync(
                                            () -> {},
                                            CompletableFuture.delayedExecutor(
                                                    retryDelayMs, TimeUnit.MILLISECONDS))
                                    .thenCompose(
                                            v ->
                                                    updateTickersPriceWithRetry(
                                                            tickers,
                                                            attempt + 1,
                                                            (int)
                                                                    (retryDelayMs
                                                                            * RETRY_MULTIPLIER)));
                        })
                .thenCompose(Function.identity());
    }

    /**
     * Add a purchase to a ticker
     *
     * @param tickerId  The id of the ticker
     * @param quantity  The quantity of the purchase
     * @param unitPrice The unit price of the purchase
     * @param date      The purchase date
     * @throws EntityNotFoundException  If the ticker does not exist
     * @throws IllegalArgumentException If the quantity is less than or equal to zero
     * @throws IllegalArgumentException If the unit price is less than or equal to zero
     */
    @Transactional
    public void addPurchase(
            Integer tickerId,
            Integer walletId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            Category category,
            LocalDateTime date,
            String description,
            TransactionStatus status,
            Boolean includeInAnalysis) {
        Ticker ticker =
                tickerRepository
                        .findById(tickerId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Ticker with id %d not found and cannot add"
                                                                + " purchase",
                                                        tickerId)));

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero");
        }

        BigDecimal amount = unitPrice.multiply(quantity);

        // Create a wallet transaction for the purchase
        Integer id =
                walletTransactionService.addExpense(
                        walletId, category, date, amount, description, status, includeInAnalysis);

        WalletTransaction walletTransaction = walletTransactionService.getTransactionById(id);

        TickerPurchase purchase =
                TickerPurchase.builder()
                        .ticker(ticker)
                        .quantity(quantity)
                        .unitPrice(unitPrice)
                        .walletTransaction(walletTransaction)
                        .build();

        tickerPurchaseRepository.save(purchase);

        // Update holding quantity
        ticker.setCurrentQuantity(ticker.getCurrentQuantity().add(quantity));

        // Update average price
        if (ticker.getAverageUnitValueCount().compareTo(BigDecimal.ZERO) == 0) {
            ticker.setAverageUnitValue(unitPrice);
        } else {
            BigDecimal currentTotalValue =
                    ticker.getAverageUnitValue().multiply(ticker.getAverageUnitValueCount());
            BigDecimal newTotalValue = unitPrice.multiply(quantity);
            BigDecimal totalQuantity = ticker.getAverageUnitValueCount().add(quantity);

            ticker.setAverageUnitValue(
                    currentTotalValue
                            .add(newTotalValue)
                            .divide(totalQuantity, 2, RoundingMode.HALF_UP));
        }

        ticker.setAverageUnitValueCount(ticker.getAverageUnitValueCount().add(quantity));

        logger.info(
                "TickerPurchase with id {} added to ticker {}. Wallet transaction with id {}"
                        + " created for the purchase and added to wallet with id {}",
                purchase.getId(),
                ticker.getSymbol(),
                id,
                walletId);
    }

    /**
     * Add a sale to a ticker
     *
     * @param tickerId  The id of the ticker
     * @param quantity  The quantity of the sale
     * @param unitPrice The unit price of the sale
     * @param date      The sale date
     * @throws EntityNotFoundException                        If the ticker does not exist
     * @throws IllegalArgumentException                       If the quantity is less than or equal to zero
     * @throws IllegalArgumentException                       If the unit price is less than or equal to zero
     * @throws MoinexException.InsufficientResourcesException If the quantity is greater than the
     *                                                        current
     *                                                        quantity
     */
    @Transactional
    public void addSale(
            Integer tickerId,
            Integer walletId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            Category category,
            LocalDateTime date,
            String description,
            TransactionStatus status,
            Boolean includeInAnalysis) {
        Ticker ticker =
                tickerRepository
                        .findById(tickerId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Ticker with id %d not found and cannot add"
                                                                + " sale",
                                                        tickerId)));

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero");
        }

        // Check if the quantity is greater than the current quantity
        if (quantity.compareTo(ticker.getCurrentQuantity()) > 0) {
            throw new MoinexException.InsufficientResourcesException(
                    "Quantity must be less than or equal to the current " + "quantity");
        }

        BigDecimal amount = unitPrice.multiply(quantity);

        // Create a wallet transaction for the sale
        Integer id =
                walletTransactionService.addIncome(
                        walletId, category, date, amount, description, status, includeInAnalysis);

        WalletTransaction walletTransaction = walletTransactionService.getTransactionById(id);

        TickerSale sale =
                TickerSale.builder()
                        .ticker(ticker)
                        .quantity(quantity)
                        .unitPrice(unitPrice)
                        .walletTransaction(walletTransaction)
                        .averageCost(ticker.getAverageUnitValue())
                        .build();

        tickerSaleRepository.save(sale);

        // Update holding quantity
        ticker.setCurrentQuantity(ticker.getCurrentQuantity().subtract(quantity));

        // If sold all holdings, reset the average price
        if (ticker.getCurrentQuantity().compareTo(BigDecimal.ZERO) == 0) {
            resetAveragePrice(tickerId);
        }

        tickerRepository.save(ticker);

        logger.info(
                "TickerSale with id {} added to ticker {}. Wallet transaction with id {} created"
                        + " for the sale and added to wallet with id {}",
                sale.getId(),
                ticker.getSymbol(),
                id,
                walletId);
    }

    /**
     * Add a dividend to a ticker
     *
     * @param tickerId    The id of the ticker
     * @param walletId    The id of the deposit wallet
     * @param category    The category of the dividend
     * @param amount      The amount of the dividend
     * @param date        The date of the dividend
     * @param description The description of the dividend
     * @param status      The status of the dividend
     * @throws EntityNotFoundException  If the ticker does not exist
     * @throws IllegalArgumentException If the amount is less than or equal to zero
     */
    @Transactional
    public void addDividend(
            Integer tickerId,
            Integer walletId,
            Category category,
            BigDecimal amount,
            LocalDateTime date,
            String description,
            TransactionStatus status,
            Boolean includeInAnalysis) {
        Ticker ticker =
                tickerRepository
                        .findById(tickerId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Ticker with id %d not found and cannot add"
                                                                + " dividend",
                                                        tickerId)));

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // Create a wallet transaction for the dividend
        Integer id =
                walletTransactionService.addIncome(
                        walletId, category, date, amount, description, status, includeInAnalysis);

        WalletTransaction walletTransaction = walletTransactionService.getTransactionById(id);

        // Create a dividend
        Dividend dividend =
                Dividend.builder().ticker(ticker).walletTransaction(walletTransaction).build();

        dividendRepository.save(dividend);

        logger.info(
                "Dividend with id {} added to ticker {}. Wallet transaction with id {} created for"
                        + " the dividend and added to wallet with id {}",
                dividend.getId(),
                ticker.getSymbol(),
                id,
                walletId);
    }

    /**
     * Exchange crypto between two tickers
     *
     * @param sourceTickerId   The id of the sold ticker
     * @param targetTickerId   The id of the target ticker
     * @param soldQuantity     The quantity of the sold ticker
     * @param receivedQuantity The quantity of the target ticker
     * @param date             The date of the exchange
     * @param description      The description of the exchange
     * @throws MoinexException.SameSourceDestinationException If the source and target tickers are the
     *                                                        same
     * @throws EntityNotFoundException                        If the source ticker does not exist
     * @throws EntityNotFoundException                        If the target ticker does not exist
     * @throws MoinexException.InvalidTickerTypeException     If the tickers are not of type CRYPTO
     * @throws IllegalArgumentException                       If the quantity is less than or equal to zero
     * @throws MoinexException.InsufficientResourcesException If the quantity is greater than the
     *                                                        current
     *                                                        quantity
     */
    @Transactional
    public void addCryptoExchange(
            Integer sourceTickerId,
            Integer targetTickerId,
            BigDecimal soldQuantity,
            BigDecimal receivedQuantity,
            LocalDateTime date,
            String description) {
        if (sourceTickerId.equals(targetTickerId)) {
            throw new MoinexException.SameSourceDestinationException(
                    "Source and target tickers must be different");
        }

        Ticker soldCrypto =
                tickerRepository
                        .findById(sourceTickerId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Ticker with id %d not found and cannot"
                                                                + " exchange crypto",
                                                        sourceTickerId)));

        Ticker receivedCrypto =
                tickerRepository
                        .findById(targetTickerId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Ticker with id %d not found and cannot"
                                                                + " exchange crypto",
                                                        targetTickerId)));

        if (soldCrypto.getType() != TickerType.CRYPTOCURRENCY
                || receivedCrypto.getType() != TickerType.CRYPTOCURRENCY) {
            throw new MoinexException.InvalidTickerTypeException(
                    "Both tickers must be of type CRYPTO to exchange crypto");
        }

        if (soldQuantity.compareTo(BigDecimal.ZERO) <= 0
                || receivedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        // Check if the quantity is greater than the current quantity
        if (soldQuantity.compareTo(soldCrypto.getCurrentQuantity()) > 0) {
            throw new MoinexException.InsufficientResourcesException(
                    "Source quantity must be less than or equal to the current " + "quantity");
        }

        CryptoExchange exchange =
                CryptoExchange.builder()
                        .soldCrypto(soldCrypto)
                        .receivedCrypto(receivedCrypto)
                        .soldQuantity(soldQuantity)
                        .receivedQuantity(receivedQuantity)
                        .date(date)
                        .description(description)
                        .build();

        cryptoExchangeRepository.save(exchange);

        // Update holding quantity
        soldCrypto.setCurrentQuantity(soldCrypto.getCurrentQuantity().subtract(soldQuantity));
        receivedCrypto.setCurrentQuantity(
                receivedCrypto.getCurrentQuantity().add(receivedQuantity));

        // If sold all holdings, reset the average price
        if (soldCrypto.getCurrentQuantity().compareTo(BigDecimal.ZERO) == 0) {
            resetAveragePrice(sourceTickerId);
        }

        tickerRepository.save(soldCrypto);
        tickerRepository.save(receivedCrypto);

        logger.info(
                "CryptoExchange with id {} added to tickers {} and {}",
                exchange.getId(),
                soldCrypto.getSymbol(),
                receivedCrypto.getSymbol());
    }

    /**
     * Delete a purchase
     *
     * @param purchaseId The id of the purchase
     * @throws EntityNotFoundException If the purchase does not exist
     */
    @Transactional
    public void deletePurchase(Integer purchaseId) {
        TickerPurchase purchase =
                tickerPurchaseRepository
                        .findById(purchaseId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "TickerPurchase with id %d not found and"
                                                                + " cannot be deleted",
                                                        purchaseId)));

        // Delete purchase before deleting wallet transaction to avoid
        // foreign key constraint violation
        tickerPurchaseRepository.delete(purchase);

        walletTransactionService.deleteTransaction(purchase.getWalletTransaction().getId());

        logger.info("TickerPurchase with id {} was deleted", purchaseId);
    }

    /**
     * Delete a sale
     *
     * @param saleId The id of the sale
     * @throws EntityNotFoundException If the sale does not exist
     */
    @Transactional
    public void deleteSale(Integer saleId) {
        TickerSale sale =
                tickerSaleRepository
                        .findById(saleId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "TickerSale with id %d not found and cannot"
                                                                + " be deleted",
                                                        saleId)));

        // Delete sale before deleting wallet transaction to avoid
        // foreign key constraint violation
        tickerSaleRepository.delete(sale);

        walletTransactionService.deleteTransaction(sale.getWalletTransaction().getId());

        logger.info("TickerSale with id {} was deleted", saleId);
    }

    /**
     * Delete a dividend
     *
     * @param dividendId The id of the dividend
     * @throws EntityNotFoundException If the dividend does not exist
     */
    @Transactional
    public void deleteDividend(Integer dividendId) {
        Dividend dividend =
                dividendRepository
                        .findById(dividendId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Dividend with id %d not found and cannot"
                                                                + " be deleted",
                                                        dividendId)));

        // Delete dividend before deleting wallet transaction to avoid
        // foreign key constraint violation
        dividendRepository.delete(dividend);

        walletTransactionService.deleteTransaction(dividend.getWalletTransaction().getId());

        logger.info("Dividend with id {} was deleted", dividendId);
    }

    /**
     * Delete a crypto exchange
     *
     * @param exchangeId The id of the crypto exchange
     * @throws EntityNotFoundException If the crypto exchange does not exist
     */
    @Transactional
    public void deleteCryptoExchange(Integer exchangeId) {
        CryptoExchange exchange =
                cryptoExchangeRepository
                        .findById(exchangeId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "CryptoExchange with id %d not found and"
                                                                + " cannot be deleted",
                                                        exchangeId)));

        // Adjust holding quantity
        exchange.getSoldCrypto()
                .setCurrentQuantity(
                        exchange.getSoldCrypto()
                                .getCurrentQuantity()
                                .add(exchange.getSoldQuantity()));

        exchange.getReceivedCrypto()
                .setCurrentQuantity(
                        exchange.getReceivedCrypto()
                                .getCurrentQuantity()
                                .subtract(exchange.getReceivedQuantity()));

        tickerRepository.save(exchange.getSoldCrypto());
        tickerRepository.save(exchange.getReceivedCrypto());

        cryptoExchangeRepository.delete(exchange);

        logger.info("CryptoExchange with id {} was deleted", exchangeId);
    }

    /**
     * Update a purchase
     *
     * @param purchase The purchase to be updated
     * @throws EntityNotFoundException  If the purchase does not exist
     * @throws EntityNotFoundException  If the ticker does not exist
     * @throws IllegalArgumentException If the quantity is less than or equal to zero
     * @throws IllegalArgumentException If the unit price is less than or equal to zero
     */
    @Transactional
    public void updatePurchase(TickerPurchase purchase) {
        TickerPurchase oldPurchase =
                tickerPurchaseRepository
                        .findById(purchase.getId())
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "TickerPurchase with id %d not found and"
                                                                + " cannot be updated",
                                                        purchase.getId())));

        if (!tickerRepository.existsById(purchase.getTicker().getId())) {
            throw new EntityNotFoundException(
                    "Ticker with id "
                            + purchase.getTicker().getId()
                            + " not found and cannot update purchase");
        }

        if (purchase.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (purchase.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero");
        }

        oldPurchase.setQuantity(purchase.getQuantity());
        oldPurchase.setUnitPrice(purchase.getUnitPrice());

        purchase.getWalletTransaction()
                .setAmount(purchase.getUnitPrice().multiply(purchase.getQuantity()));

        walletTransactionService.updateTransaction(purchase.getWalletTransaction());

        tickerPurchaseRepository.save(oldPurchase);

        logger.info("TickerPurchase with id {} was updated", purchase.getId());
    }

    /**
     * Update a sale
     *
     * @param sale The sale to be updated
     * @throws EntityNotFoundException  If the sale does not exist
     * @throws EntityNotFoundException  If the ticker does not exist
     * @throws IllegalArgumentException If the quantity is less than or equal to zero
     * @throws IllegalArgumentException If the unit price is less than or equal to zero
     */
    @Transactional
    public void updateSale(TickerSale sale) {
        TickerSale oldSale =
                tickerSaleRepository
                        .findById(sale.getId())
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "TickerSale with id %d not found and cannot"
                                                                + " be updated",
                                                        sale.getId())));

        if (!tickerRepository.existsById(sale.getTicker().getId())) {
            throw new EntityNotFoundException(
                    "Ticker with id "
                            + sale.getTicker().getId()
                            + " not found and cannot update sale");
        }

        if (sale.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (sale.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero");
        }

        oldSale.setQuantity(sale.getQuantity());
        oldSale.setUnitPrice(sale.getUnitPrice());

        sale.getWalletTransaction().setAmount(sale.getUnitPrice().multiply(sale.getQuantity()));

        walletTransactionService.updateTransaction(sale.getWalletTransaction());

        tickerSaleRepository.save(oldSale);

        logger.info("TickerSale with id {} was updated", sale.getId());
    }

    /**
     * Update a dividend
     *
     * @param dividend The dividend to be updated
     * @throws EntityNotFoundException If the dividend does not exist
     * @throws EntityNotFoundException If the ticker does not exist
     */
    @Transactional
    public void updateDividend(Dividend dividend) {
        Dividend oldDividend =
                dividendRepository
                        .findById(dividend.getId())
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Dividend with id "
                                                        + dividend.getId()
                                                        + " not found and cannot be updated"));

        if (!tickerRepository.existsById(dividend.getTicker().getId())) {
            throw new EntityNotFoundException(
                    "Ticker with id "
                            + dividend.getTicker().getId()
                            + " not found and cannot update dividend");
        }

        walletTransactionService.updateTransaction(dividend.getWalletTransaction());

        dividendRepository.save(oldDividend);

        logger.info("Dividend with id {} was updated", dividend.getId());
    }

    /**
     * Update a crypto exchange
     *
     * @param exchange The crypto exchange to be updated
     * @throws EntityNotFoundException        If the crypto exchange does not exist
     * @throws EntityNotFoundException        If the source ticker does not exist
     * @throws EntityNotFoundException        If the target ticker does not exist
     * @throws MoinexException.SameSourceDestinationException If the source and target tickers are the
     *                                        same
     * @throws IllegalArgumentException       If the quantity is less than or equal to zero
     */
    @Transactional
    public void updateCryptoExchange(CryptoExchange exchange) {
        CryptoExchange oldExchange =
                cryptoExchangeRepository
                        .findById(exchange.getId())
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "CryptoExchange with id "
                                                        + exchange.getId()
                                                        + " not found and cannot be updated"));

        tickerRepository
                .findById(exchange.getSoldCrypto().getId())
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "Source ticker with id "
                                                + exchange.getSoldCrypto().getId()
                                                + " not found and cannot update crypto exchange"));

        tickerRepository
                .findById(exchange.getReceivedCrypto().getId())
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "Target ticker with id "
                                                + exchange.getReceivedCrypto().getId()
                                                + " not found and cannot update crypto exchange"));

        if (exchange.getSoldCrypto().getId().equals(exchange.getReceivedCrypto().getId())) {
            throw new MoinexException.SameSourceDestinationException(
                    "Source and target tickers must be different");
        }

        if (exchange.getSoldQuantity().compareTo(BigDecimal.ZERO) <= 0
                || exchange.getReceivedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        // Complex update operations
        changeSoldCrypto(oldExchange, exchange.getSoldCrypto());
        changeReceivedCrypto(oldExchange, exchange.getReceivedCrypto());
        changeSoldQuantity(oldExchange, exchange.getSoldQuantity());
        changeReceivedQuantity(oldExchange, exchange.getReceivedQuantity());

        // Trivial update operations
        oldExchange.setDate(exchange.getDate());
        oldExchange.setDescription(exchange.getDescription());

        cryptoExchangeRepository.save(oldExchange);

        logger.info("CryptoExchange with id {} was updated", exchange.getId());
    }

    /**
     * Change the quantity of sold ticker of a crypto exchange
     *
     * @param oldExchange  The crypto exchange to be updated
     * @param soldQuantity The new sold quantity
     */
    public void changeSoldQuantity(CryptoExchange oldExchange, BigDecimal soldQuantity) {
        // Adjust holding quantity
        oldExchange
                .getSoldCrypto()
                .setCurrentQuantity(
                        oldExchange
                                .getSoldCrypto()
                                .getCurrentQuantity()
                                .add(oldExchange.getSoldQuantity().subtract(soldQuantity)));

        tickerRepository.save(oldExchange.getSoldCrypto());

        oldExchange.setSoldQuantity(soldQuantity);
    }

    /**
     * Change the quantity of received crypto of a crypto exchange
     *
     * @param oldExchange      The crypto exchange to be updated
     * @param receivedQuantity The new target quantity
     */
    public void changeReceivedQuantity(CryptoExchange oldExchange, BigDecimal receivedQuantity) {
        // Adjust holding quantity
        oldExchange
                .getReceivedCrypto()
                .setCurrentQuantity(
                        oldExchange
                                .getReceivedCrypto()
                                .getCurrentQuantity()
                                .subtract(
                                        oldExchange
                                                .getReceivedQuantity()
                                                .subtract(receivedQuantity)));

        tickerRepository.save(oldExchange.getReceivedCrypto());

        oldExchange.setReceivedQuantity(receivedQuantity);
    }

    /**
     * Change the sold ticker of a crypto exchange
     *
     * @param oldExchange The crypto exchange
     * @param soldCrypto  The new sold ticker
     */
    private void changeSoldCrypto(CryptoExchange oldExchange, Ticker soldCrypto) {
        if (oldExchange.getSoldCrypto().getId().equals(soldCrypto.getId())) {
            return;
        }

        // Adjust holding quantity
        oldExchange
                .getSoldCrypto()
                .setCurrentQuantity(
                        oldExchange
                                .getSoldCrypto()
                                .getCurrentQuantity()
                                .add(oldExchange.getSoldQuantity()));

        soldCrypto.setCurrentQuantity(
                soldCrypto.getCurrentQuantity().subtract(oldExchange.getSoldQuantity()));

        tickerRepository.save(oldExchange.getSoldCrypto());
        tickerRepository.save(soldCrypto);

        oldExchange.setSoldCrypto(soldCrypto);
    }

    /**
     * Change the received ticker of a crypto exchange
     *
     * @param oldExchange    The crypto exchange
     * @param receivedCrypto The new target ticker
     */
    private void changeReceivedCrypto(CryptoExchange oldExchange, Ticker receivedCrypto) {
        if (oldExchange.getReceivedCrypto().getId().equals(receivedCrypto.getId())) {
            return;
        }

        // Adjust holding quantity
        oldExchange
                .getReceivedCrypto()
                .setCurrentQuantity(
                        oldExchange
                                .getReceivedCrypto()
                                .getCurrentQuantity()
                                .subtract(oldExchange.getReceivedQuantity()));

        receivedCrypto.setCurrentQuantity(
                receivedCrypto.getCurrentQuantity().add(oldExchange.getReceivedQuantity()));

        tickerRepository.save(oldExchange.getReceivedCrypto());
        tickerRepository.save(receivedCrypto);

        oldExchange.setReceivedCrypto(receivedCrypto);
    }

    /**
     * Reset the average price of a ticker to zero
     *
     * @param tickerId The id of the ticker
     * @throws EntityNotFoundException If the ticker does not exist
     */
    public void resetAveragePrice(Integer tickerId) {
        Ticker ticker =
                tickerRepository
                        .findById(tickerId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Ticker with id "
                                                        + tickerId
                                                        + " not found and cannot reset average"
                                                        + " price"));

        ticker.setAverageUnitValue(BigDecimal.ZERO);
        ticker.setAverageUnitValueCount(BigDecimal.ZERO);

        tickerRepository.save(ticker);

        logger.info("Average price of ticker {} was reset to zero", ticker.getSymbol());
    }

    /**
     * Get all tickers
     *
     * @return A list with all tickers
     */
    public List<Ticker> getAllTickers() {
        return tickerRepository.findAllByOrderBySymbolAsc();
    }

    /**
     * Get all non-archived tickers
     *
     * @return A list with all non-archived tickers
     */
    public List<Ticker> getAllNonArchivedTickers() {
        return tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc();
    }

    /**
     * Get all archived tickers
     *
     * @return A list with all archived tickers
     */
    public List<Ticker> getAllArchivedTickers() {
        return tickerRepository.findAllByIsArchivedTrueOrderBySymbolAsc();
    }

    /**
     * Get all non-archived tickers of a specific type
     *
     * @param type The type of the tickers
     */
    public List<Ticker> getAllNonArchivedTickersByType(TickerType type) {
        return tickerRepository.findAllByTypeAndIsArchivedFalseOrderBySymbolAsc(type);
    }

    /**
     * Get count of transactions associated with the ticker
     *
     * @param tickerId The id of the ticker
     * @return The count of transactions associated with the ticker
     */
    public Integer getTransactionCountByTicker(Integer tickerId) {
        return getPurchaseCountByTicker(tickerId)
                + getSaleCountByTicker(tickerId)
                + getDividendCountByTicker(tickerId)
                + getCryptoExchangeCountByTicker(tickerId);
    }

    /**
     * Get count of purchases associated with the ticker
     *
     * @param tickerId The id of the ticker
     * @return The count of purchases associated with the ticker
     */
    public Integer getPurchaseCountByTicker(Integer tickerId) {
        return tickerRepository.getPurchaseCountByTicker(tickerId);
    }

    /**
     * Get count of sales associated with the ticker
     *
     * @param tickerId The id of the ticker
     * @return The count of sales associated with the ticker
     */
    public Integer getSaleCountByTicker(Integer tickerId) {
        return tickerRepository.getSaleCountByTicker(tickerId);
    }

    /**
     * Get count of dividends associated with the ticker
     *
     * @param tickerId The id of the ticker
     * @return The count of dividends associated with the ticker
     */
    public Integer getDividendCountByTicker(Integer tickerId) {
        return tickerRepository.getDividendCountByTicker(tickerId);
    }

    /**
     * Get count of crypto exchanges associated with the ticker
     *
     * @param tickerId The id of the ticker
     * @return The count of crypto exchanges associated with the ticker
     */
    public Integer getCryptoExchangeCountByTicker(Integer tickerId) {
        return tickerRepository.getCryptoExchangeCountByTicker(tickerId);
    }

    /**
     * Get all purchases
     *
     * @return A list with all purchases
     */
    public List<TickerPurchase> getAllPurchases() {
        return tickerPurchaseRepository.findAll();
    }

    /**
     * Get all sales
     *
     * @return A list with all sales
     */
    public List<TickerSale> getAllSales() {
        return tickerSaleRepository.findAll();
    }

    public List<TickerSale> getAllNonArchivedSales() {
        return tickerSaleRepository.findAllNonArchived();
    }

    /**
     * Get all dividends
     *
     * @return A list with all dividends
     */
    public List<Dividend> getAllDividends() {
        return dividendRepository.findAll();
    }

    /**
     * Get all crypto exchanges
     *
     * @return A list with all crypto exchanges
     */
    public List<CryptoExchange> getAllCryptoExchanges() {
        return cryptoExchangeRepository.findAll();
    }

    /**
     * Get all purchases up to a specific date
     *
     * @param date The date threshold
     * @return A list with all purchases before or on the date
     */
    public List<TickerPurchase> getPurchasesByDateBefore(LocalDateTime date) {
        String dateStr = date.format(Constants.DB_DATE_FORMATTER);
        return tickerPurchaseRepository.findAllByDateBefore(dateStr);
    }

    /**
     * Get all sales up to a specific date
     *
     * @param date The date threshold
     * @return A list with all sales before or on the date
     */
    public List<TickerSale> getSalesByDateBefore(LocalDateTime date) {
        String dateStr = date.format(Constants.DB_DATE_FORMATTER);
        return tickerSaleRepository.findAllByDateBefore(dateStr);
    }
}
