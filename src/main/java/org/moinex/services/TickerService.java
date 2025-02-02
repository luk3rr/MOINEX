/*
 * Filename: TickerService.java
 * Created on: January  6, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.moinex.entities.Category;
import org.moinex.entities.WalletTransaction;
import org.moinex.entities.investment.CryptoExchange;
import org.moinex.entities.investment.Dividend;
import org.moinex.entities.investment.Ticker;
import org.moinex.entities.investment.TickerPurchase;
import org.moinex.entities.investment.TickerSale;
import org.moinex.repositories.CryptoExchangeRepository;
import org.moinex.repositories.DividendRepository;
import org.moinex.repositories.TickerPurchaseRepository;
import org.moinex.repositories.TickerRepository;
import org.moinex.repositories.TickerSaleRepository;
import org.moinex.repositories.WalletRepository;
import org.moinex.util.APIUtils;
import org.moinex.util.Constants;
import org.moinex.util.LoggerConfig;
import org.moinex.util.TickerType;
import org.moinex.util.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing tickers
 */
@Service
public class TickerService
{
    @Autowired
    private TickerRepository m_tickerRepository;

    @Autowired
    private TickerPurchaseRepository m_tickerPurchaseRepository;

    @Autowired
    private TickerSaleRepository m_tickerSaleRepository;

    @Autowired
    private DividendRepository m_dividendRepository;

    @Autowired
    private CryptoExchangeRepository m_cryptoExchangeRepository;

    @Autowired
    private WalletRepository m_walletRepository;

    @Autowired
    private WalletTransactionService m_walletTransactionService;

    private static final Logger logger = LoggerConfig.GetLogger();

    public TickerService() { }

    /**
     * Registers a new ticker
     * @param name The name of the ticker
     * @param symbol The symbol of the ticker
     * @param type The type of the ticker
     * @param price The price of the ticker
     * @param avgUnitPrice The average unit price of the ticker
     * @param quantity The quantity of the ticker
     * @throws RuntimeException If the ticker already exists
     * @return The id of the registered ticker
     */
    @Transactional
    public Long RegisterTicker(String     name,
                               String     symbol,
                               TickerType type,
                               BigDecimal price,
                               BigDecimal avgUnitPrice,
                               BigDecimal quantity)
    {
        // Remove leading and trailing whitespaces
        name   = name.strip();
        symbol = symbol.strip();

        if (name.isBlank() || symbol.isBlank())
        {
            throw new RuntimeException("Name and symbol must not be empty");
        }

        if (m_tickerRepository.existsBySymbol(symbol))
        {
            logger.warning("Ticker with symbol " + symbol + " already exists");

            throw new RuntimeException("Ticker with symbol " + symbol +
                                       " already exists");
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Price must be greater than zero");
        }

        if (quantity.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new RuntimeException(
                "Quantity must be greater than or equal to zero");
        }

        if (avgUnitPrice.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new RuntimeException(
                "Average unit price must be greater than or equal to zero");
        }

        price        = Constants.RoundPrice(price, type);
        avgUnitPrice = Constants.RoundPrice(avgUnitPrice, type);

        Ticker ticker = new Ticker(name,
                                   symbol,
                                   type,
                                   quantity,
                                   price,
                                   avgUnitPrice,
                                   LocalDateTime.now());

        m_tickerRepository.save(ticker);

        logger.info("Ticker " + symbol + " registered successfully");

        return ticker.GetId();
    }

    /**
     * Deletes a ticker
     * @param id The id of the ticker
     * @throws RuntimeException If the ticker does not exist
     * @throws RuntimeException If the ticker has transactions associated with it
     */
    @Transactional
    public void DeleteTicker(Long id)
    {
        Ticker ticker = m_tickerRepository.findById(id).orElseThrow(
            ()
                -> new RuntimeException("Ticker with id " + id +
                                        " not found and cannot be deleted"));

        // Check if the ticker has transactions associated with it
        if (GetTransactionCountByTicker(id) > 0)
        {
            throw new RuntimeException(
                "Ticker with id " + id +
                " has transactions associated with it and cannot be deleted. Remove "
                + "the transactions first or archive the ticker");
        }

        m_tickerRepository.delete(ticker);

        logger.info("Ticker with id " + id + " was permanently deleted");
    }

    /**
     * Archives a ticker
     * @param id The id of the ticker
     * @throws RuntimeException If the ticker does not exist
     * @note This method is used to archive a ticker, which means that the ticker
     * will not be deleted from the database, but it will not used in the application
     * anymore
     */
    @Transactional
    public void ArchiveTicker(Long id)
    {
        Ticker ticker = m_tickerRepository.findById(id).orElseThrow(
            ()
                -> new RuntimeException("Ticker with id " + id +
                                        " not found and cannot be archived"));

        ticker.SetArchived(true);
        m_tickerRepository.save(ticker);

        logger.info("Ticker with id " + id + " was archived");
    }

    /**
     * Unarchives a ticker
     * @param id The id of the ticker
     * @throws RuntimeException If the ticker does not exist
     * @note This method is used to unarchive a ticker, which means that the ticker
     * will be used in the application again
     */
    @Transactional
    public void UnarchiveTicker(Long id)
    {
        Ticker ticker = m_tickerRepository.findById(id).orElseThrow(
            ()
                -> new RuntimeException("Ticker with id " + id +
                                        " not found and cannot be unarchived"));

        ticker.SetArchived(false);
        m_tickerRepository.save(ticker);

        logger.info("Ticker with id " + id + " was unarchived");
    }

    /**
     * Update a ticker
     * @param tk The ticker to be updated
     * @throws RuntimeException If the ticker does not exist
     * @throws RuntimeException If the name or symbol is empty
     * @throws RuntimeException If the price is less than or equal to zero
     * @throws RuntimeException If the quantity is less than zero
     * @throws RuntimeException If the average unit price is less than zero
     */
    @Transactional
    public void UpdateTicker(Ticker tk)
    {
        Ticker oldTicker =
            m_tickerRepository.findById(tk.GetId())
                .orElseThrow(
                    ()
                        -> new RuntimeException("Ticker with id " + tk.GetId() +
                                                " not found and cannot be updated"));

        // Remove leading and trailing whitespaces
        tk.SetName(tk.GetName().strip());
        tk.SetSymbol(tk.GetSymbol().strip());

        if (tk.GetName().isBlank() || tk.GetSymbol().isBlank())
        {
            throw new RuntimeException("Name and symbol must not be empty");
        }

        if (tk.GetCurrentUnitValue().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Price must be greater than zero");
        }

        if (tk.GetCurrentQuantity().compareTo(BigDecimal.ZERO) < 0)
        {
            throw new RuntimeException(
                "Quantity must be greater than or equal to zero");
        }

        if (tk.GetAveragePrice().compareTo(BigDecimal.ZERO) < 0)
        {
            throw new RuntimeException(
                "Average unit price must be greater than or equal to zero");
        }

        tk.SetCurrentUnitValue(
            Constants.RoundPrice(tk.GetCurrentUnitValue(), tk.GetType()));
        tk.SetAveragePrice(Constants.RoundPrice(tk.GetAveragePrice(), tk.GetType()));

        oldTicker.SetName(tk.GetName());
        oldTicker.SetSymbol(tk.GetSymbol());
        oldTicker.SetType(tk.GetType());
        oldTicker.SetCurrentUnitValue(tk.GetCurrentUnitValue());
        oldTicker.SetCurrentQuantity(tk.GetCurrentQuantity());
        oldTicker.SetAveragePrice(tk.GetAveragePrice());
        oldTicker.SetArchived(tk.IsArchived());

        // If sold all holdings, reset average price
        if (oldTicker.GetCurrentQuantity().compareTo(BigDecimal.ZERO) == 0)
        {
            ResetAveragePrice(oldTicker.GetId());
        }

        m_tickerRepository.save(oldTicker);

        logger.info("Ticker with id " + tk.GetId() + " was updated");
    }

    /**
     * Update tickers price from API asynchronously
     * @param tickers The list of tickers to update
     * @return A completable future with a list with tickers that failed to update
     */
    @Transactional
    public CompletableFuture<List<Ticker>>
    UpdateTickersPriceFromAPIAsync(List<Ticker> tickers)
    {
        if (tickers.isEmpty())
        {
            CompletableFuture<List<Ticker>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(
                new RuntimeException("No tickers to update"));
            return failedFuture;
        }

        String[] symbols =
            tickers.stream().map(Ticker::GetSymbol).toArray(String[] ::new);

        return APIUtils.FetchStockPricesAsync(symbols).thenApply(jsonObject -> {
            List<Ticker> failed = new ArrayList<>();
            tickers.forEach(ticker -> {
                try
                {
                    JSONObject tickerData =
                        jsonObject.getJSONObject(ticker.GetSymbol());
                    BigDecimal price = tickerData.getBigDecimal("price");

                    price = Constants.RoundPrice(price, ticker.GetType());

                    ticker.SetCurrentUnitValue(price);
                    ticker.SetLastUpdate(LocalDateTime.now());
                    m_tickerRepository.save(ticker);
                }
                catch (Exception e)
                {
                    logger.warning("Failed to update ticker " + ticker.GetSymbol() +
                                   ": " + e.getMessage());
                    failed.add(ticker);
                }
            });
            return failed;
        });
    }

    /**
     * Add a purchase to a ticker
     * @param tickerId The id of the ticker
     * @param quantity The quantity of the purchase
     * @param unitPrice The unit price of the purchase
     * @param purchaseDate The purchase date
     * @throws RuntimeException If the ticker does not exist
     * @throws RuntimeException If the quantity is less than or equal to zero
     * @throws RuntimeException If the unit price is less than or equal to zero
     */
    @Transactional
    public void AddPurchase(Long              tickerId,
                            Long              walletId,
                            BigDecimal        quantity,
                            BigDecimal        unitPrice,
                            Category          category,
                            LocalDateTime     date,
                            String            description,
                            TransactionStatus status)
    {
        Ticker ticker = m_tickerRepository.findById(tickerId).orElseThrow(
            ()
                -> new RuntimeException("Ticker with id " + tickerId +
                                        " not found and cannot add purchase"));

        if (quantity.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Quantity must be greater than zero");
        }

        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Unit price must be greater than zero");
        }

        BigDecimal amount = unitPrice.multiply(quantity);

        // Create a wallet transaction for the dividend
        Long id = m_walletTransactionService.AddExpense(walletId,
                                                        category,
                                                        date,
                                                        amount,
                                                        description,
                                                        status);

        WalletTransaction walletTransaction =
            m_walletTransactionService.GetTransactionById(id);

        TickerPurchase purchase =
            new TickerPurchase(ticker, quantity, unitPrice, walletTransaction);

        m_tickerPurchaseRepository.save(purchase);

        // Update holdings quantity
        ticker.SetCurrentQuantity(ticker.GetCurrentQuantity().add(quantity));

        // Update average price
        BigDecimal updatedTotalValue =
            ticker.GetAveragePrice().multiply(ticker.GetAveragePriceCount());

        updatedTotalValue = updatedTotalValue.add(unitPrice.multiply(quantity));

        ticker.SetAveragePrice(
            updatedTotalValue.divide(ticker.GetAveragePriceCount().add(quantity),
                                     2,
                                     RoundingMode.HALF_UP));

        ticker.SetAveragePriceCount(ticker.GetAveragePriceCount().add(quantity));

        logger.info("TickerPurchase with id " + purchase.GetId() + " added to ticker " +
                    ticker.GetSymbol() + ". Wallet transaction with id " + id +
                    " created for the purchase and added to wallet with id " +
                    walletId);
    }

    /**
     * Add a sale to a ticker
     * @param tickerId The id of the ticker
     * @param quantity The quantity of the sale
     * @param unitPrice The unit price of the sale
     * @param saleDate The sale date
     * @throws RuntimeException If the ticker does not exist
     * @throws RuntimeException If the quantity is less than or equal to zero
     * @throws RuntimeException If the unit price is less than or equal to zero
     * @throws RuntimeException If the quantity is greater than the current quantity
     */
    @Transactional
    public void AddSale(Long              tickerId,
                        Long              walletId,
                        BigDecimal        quantity,
                        BigDecimal        unitPrice,
                        Category          category,
                        LocalDateTime     date,
                        String            description,
                        TransactionStatus status)
    {
        Ticker ticker = m_tickerRepository.findById(tickerId).orElseThrow(
            ()
                -> new RuntimeException("Ticker with id " + tickerId +
                                        " not found and cannot add sale"));

        if (quantity.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Quantity must be greater than zero");
        }

        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Unit price must be greater than zero");
        }

        // Check if the quantity is greater than the current quantity
        if (quantity.compareTo(ticker.GetCurrentQuantity()) > 0)
        {
            throw new RuntimeException(
                "Quantity must be less than or equal to the current "
                + "quantity");
        }

        BigDecimal amount = unitPrice.multiply(quantity);

        // Create a wallet transaction for the sale
        Long id = m_walletTransactionService
                      .AddIncome(walletId, category, date, amount, description, status);

        WalletTransaction walletTransaction =
            m_walletTransactionService.GetTransactionById(id);

        TickerSale sale = new TickerSale(ticker,
                                         quantity,
                                         unitPrice,
                                         walletTransaction,
                                         ticker.GetAveragePrice());

        m_tickerSaleRepository.save(sale);

        // Update holdings quantity
        ticker.SetCurrentQuantity(ticker.GetCurrentQuantity().subtract(quantity));

        // If sold all holdings, reset average price
        if (ticker.GetCurrentQuantity().compareTo(BigDecimal.ZERO) == 0)
        {
            ResetAveragePrice(tickerId);
        }

        m_tickerRepository.save(ticker);

        logger.info("TickerSale with id " + sale.GetId() + " added to ticker " +
                    ticker.GetSymbol() + ". Wallet transaction with id " + id +
                    " created for the sale and added to wallet with id " + walletId);
    }

    /**
     * Add a dividend to a ticker
     * @param tickerId The id of the ticker
     * @param walletId The id of the deposit wallet
     * @param category The category of the dividend
     * @param amount The amount of the dividend
     * @param date The date of the dividend
     * @throws RuntimeException If the ticker does not exist
     * @throws RuntimeException If the wallet transaction does not exist
     */
    @Transactional
    public void AddDividend(Long              tickerId,
                            Long              walletId,
                            Category          category,
                            BigDecimal        amount,
                            LocalDateTime     date,
                            String            description,
                            TransactionStatus status)
    {
        Ticker ticker = m_tickerRepository.findById(tickerId).orElseThrow(
            ()
                -> new RuntimeException("Ticker with id " + tickerId +
                                        " not found and cannot add dividend"));

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Amount must be greater than zero");
        }

        // Create a wallet transaction for the dividend
        Long id = m_walletTransactionService
                      .AddIncome(walletId, category, date, amount, description, status);

        WalletTransaction walletTransaction =
            m_walletTransactionService.GetTransactionById(id);

        // Create a dividend
        Dividend dividend = new Dividend(ticker, walletTransaction);

        m_dividendRepository.save(dividend);

        logger.info("Dividend with id " + dividend.GetId() + " added to ticker " +
                    ticker.GetSymbol() + ". Wallet transaction with id " + id +
                    " created for the dividend and added to wallet with id " +
                    walletId);
    }

    /**
     * Exchange crypto between two tickers
     * @param sourceTickerId The id of the sold ticker
     * @param targetTickerId The id of the target ticker
     * @param soldQuantity The quantity of the sold ticker
     * @param receivedQuantity The quantity of the target ticker
     * @param date The date of the exchange
     * @param description The description of the exchange
     * @throws RuntimeException If the sold ticker does not exist
     * @throws RuntimeException If the target ticker does not exist
     * @throws RuntimeException If the quantities are less than or equal to zero
     * @throws RuntimeException If the sold quantity is greater than the current
     *    quantity
     */
    @Transactional
    public void AddCryptoExchange(Long          sourceTickerId,
                                  Long          targetTickerId,
                                  BigDecimal    soldQuantity,
                                  BigDecimal    receivedQuantity,
                                  LocalDateTime date,
                                  String        description)
    {
        if (sourceTickerId.equals(targetTickerId))
        {
            throw new RuntimeException("Source and target tickers must be different");
        }

        Ticker soldCrypto =
            m_tickerRepository.findById(sourceTickerId)
                .orElseThrow(()
                                 -> new RuntimeException(
                                     "Ticker with id " + sourceTickerId +
                                     " not found and cannot exchange crypto"));

        Ticker receivedCrypto =
            m_tickerRepository.findById(targetTickerId)
                .orElseThrow(()
                                 -> new RuntimeException(
                                     "Ticker with id " + targetTickerId +
                                     " not found and cannot exchange crypto"));

        if (soldCrypto.GetType() != TickerType.CRYPTOCURRENCY ||
            receivedCrypto.GetType() != TickerType.CRYPTOCURRENCY)
        {
            throw new RuntimeException(
                "Both tickers must be of type CRYPTO to exchange crypto");
        }

        if (soldQuantity.compareTo(BigDecimal.ZERO) <= 0 ||
            receivedQuantity.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Quantity must be greater than zero");
        }

        // Check if the quantity is greater than the current quantity
        if (soldQuantity.compareTo(soldCrypto.GetCurrentQuantity()) > 0)
        {
            throw new RuntimeException(
                "Source quantity must be less than or equal to the current "
                + "quantity");
        }

        CryptoExchange exchange = new CryptoExchange(soldCrypto,
                                                     receivedCrypto,
                                                     soldQuantity,
                                                     receivedQuantity,
                                                     date,
                                                     description);

        m_cryptoExchangeRepository.save(exchange);

        // Update holdings quantity
        soldCrypto.SetCurrentQuantity(
            soldCrypto.GetCurrentQuantity().subtract(soldQuantity));
        receivedCrypto.SetCurrentQuantity(
            receivedCrypto.GetCurrentQuantity().add(receivedQuantity));

        // If sold all holdings, reset average price
        if (soldCrypto.GetCurrentQuantity().compareTo(BigDecimal.ZERO) == 0)
        {
            ResetAveragePrice(sourceTickerId);
        }

        m_tickerRepository.save(soldCrypto);
        m_tickerRepository.save(receivedCrypto);

        logger.info("CryptoExchange with id " + exchange.GetId() +
                    " added to tickers " + soldCrypto.GetSymbol() + " and " +
                    receivedCrypto.GetSymbol());
    }

    /**
     * Delete a purchase
     * @param purchaseId The id of the purchase
     * @throws RuntimeException If the purchase does not exist
     */
    @Transactional
    public void DeletePurchase(Long purchaseId)
    {
        TickerPurchase purchase =
            m_tickerPurchaseRepository.findById(purchaseId)
                .orElseThrow(
                    ()
                        -> new RuntimeException("TickerPurchase with id " + purchaseId +
                                                " not found and cannot be deleted"));

        // Delete purchase before deleting wallet transaction to avoid
        // foreign key constraint violation
        m_tickerPurchaseRepository.delete(purchase);

        m_walletTransactionService.DeleteTransaction(
            purchase.GetWalletTransaction().GetId());

        logger.info("TickerPurchase with id " + purchaseId + " was deleted");
    }

    /**
     * Delete a sale
     * @param saleId The id of the sale
     * @throws RuntimeException If the sale does not exist
     */
    @Transactional
    public void DeleteSale(Long saleId)
    {
        TickerSale sale = m_tickerSaleRepository.findById(saleId).orElseThrow(
            ()
                -> new RuntimeException("TickerSale with id " + saleId +
                                        " not found and cannot be deleted"));

        // Delete sale before deleting wallet transaction to avoid
        // foreign key constraint violation
        m_tickerSaleRepository.delete(sale);

        m_walletTransactionService.DeleteTransaction(
            sale.GetWalletTransaction().GetId());

        logger.info("TickerSale with id " + saleId + " was deleted");
    }

    /**
     * Delete a dividend
     * @param dividendId The id of the dividend
     * @throws RuntimeException If the dividend does not exist
     */
    @Transactional
    public void DeleteDividend(Long dividendId)
    {
        Dividend dividend =
            m_dividendRepository.findById(dividendId)
                .orElseThrow(
                    ()
                        -> new RuntimeException("Dividend with id " + dividendId +
                                                " not found and cannot be deleted"));

        // Delete dividend before deleting wallet transaction to avoid
        // foreign key constraint violation
        m_dividendRepository.delete(dividend);

        m_walletTransactionService.DeleteTransaction(
            dividend.GetWalletTransaction().GetId());

        logger.info("Dividend with id " + dividendId + " was deleted");
    }

    /**
     * Delete a crypto exchange
     * @param exchangeId The id of the crypto exchange
     * @throws RuntimeException If the crypto exchange does not exist
     */
    @Transactional
    public void DeleteCryptoExchange(Long exchangeId)
    {
        CryptoExchange exchange =
            m_cryptoExchangeRepository.findById(exchangeId)
                .orElseThrow(
                    ()
                        -> new RuntimeException("CryptoExchange with id " + exchangeId +
                                                " not found and cannot be deleted"));

        // Adjust holdings quantity
        exchange.GetSoldCrypto().SetCurrentQuantity(
            exchange.GetSoldCrypto().GetCurrentQuantity().add(
                exchange.GetSoldQuantity()));

        exchange.GetReceivedCrypto().SetCurrentQuantity(
            exchange.GetReceivedCrypto().GetCurrentQuantity().subtract(
                exchange.GetReceivedQuantity()));

        m_tickerRepository.save(exchange.GetSoldCrypto());
        m_tickerRepository.save(exchange.GetReceivedCrypto());

        m_cryptoExchangeRepository.delete(exchange);

        logger.info("CryptoExchange with id " + exchangeId + " was deleted");
    }

    /**
     * Update a purchase
     * @param purchase The purchase to be updated
     * @throws RuntimeException If the purchase does not exist
     * @throws RuntimeException If the ticker does not exist
     * @throws RuntimeException If the quantity is less than or equal to zero
     * @throws RuntimeException If the unit price is less than or equal to zero
     */
    @Transactional
    public void UpdatePurchase(TickerPurchase purchase)
    {
        TickerPurchase oldPurchase =
            m_tickerPurchaseRepository.findById(purchase.GetId())
                .orElseThrow(()
                                 -> new RuntimeException(
                                     "TickerPurchase with id " + purchase.GetId() +
                                     " not found and cannot be updated"));

        m_tickerRepository.findById(purchase.GetTicker().GetId())
            .orElseThrow(()
                             -> new RuntimeException(
                                 "Ticker with id " + purchase.GetTicker().GetId() +
                                 " not found and cannot update purchase"));

        if (purchase.GetQuantity().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Quantity must be greater than zero");
        }

        if (purchase.GetUnitPrice().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Unit price must be greater than zero");
        }

        oldPurchase.SetQuantity(purchase.GetQuantity());
        oldPurchase.SetUnitPrice(purchase.GetUnitPrice());

        purchase.GetWalletTransaction().SetAmount(
            purchase.GetUnitPrice().multiply(purchase.GetQuantity()));

        m_walletTransactionService.UpdateTransaction(purchase.GetWalletTransaction());

        m_tickerPurchaseRepository.save(oldPurchase);

        logger.info("TickerPurchase with id " + purchase.GetId() + " was updated");
    }

    /**
     * Update a sale
     * @param sale The sale to be updated
     * @throws RuntimeException If the sale does not exist
     * @throws RuntimeException If the ticker does not exist
     * @throws RuntimeException If the quantity is less than or equal to zero
     * @throws RuntimeException If the unit price is less than or equal to zero
     */
    @Transactional
    public void UpdateSale(TickerSale sale)
    {
        TickerSale oldSale =
            m_tickerSaleRepository.findById(sale.GetId())
                .orElseThrow(
                    ()
                        -> new RuntimeException("TickerSale with id " + sale.GetId() +
                                                " not found and cannot be updated"));

        m_tickerRepository.findById(sale.GetTicker().GetId())
            .orElseThrow(()
                             -> new RuntimeException(
                                 "Ticker with id " + sale.GetTicker().GetId() +
                                 " not found and cannot update sale"));

        if (sale.GetQuantity().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Quantity must be greater than zero");
        }

        if (sale.GetUnitPrice().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Unit price must be greater than zero");
        }

        oldSale.SetQuantity(sale.GetQuantity());
        oldSale.SetUnitPrice(sale.GetUnitPrice());

        sale.GetWalletTransaction().SetAmount(
            sale.GetUnitPrice().multiply(sale.GetQuantity()));

        m_walletTransactionService.UpdateTransaction(sale.GetWalletTransaction());

        m_tickerSaleRepository.save(oldSale);

        logger.info("TickerSale with id " + sale.GetId() + " was updated");
    }

    /**
     * Update a dividend
     * @param dividend The dividend to be updated
     * @throws RuntimeException If the dividend does not exist
     * @throws RuntimeException If the ticker does not exist
     */
    @Transactional
    public void UpdateDividend(Dividend dividend)
    {
        Dividend oldDividend =
            m_dividendRepository.findById(dividend.GetId())
                .orElseThrow(
                    ()
                        -> new RuntimeException("Dividend with id " + dividend.GetId() +
                                                " not found and cannot be updated"));

        m_tickerRepository.findById(dividend.GetTicker().GetId())
            .orElseThrow(()
                             -> new RuntimeException(
                                 "Ticker with id " + dividend.GetTicker().GetId() +
                                 " not found and cannot update dividend"));

        m_walletTransactionService.UpdateTransaction(dividend.GetWalletTransaction());

        m_dividendRepository.save(oldDividend);

        logger.info("Dividend with id " + dividend.GetId() + " was updated");
    }

    /**
     * Update a crypto exchange
     * @param exchange The crypto exchange to be updated
     * @throws RuntimeException If the crypto exchange does not exist
     * @throws RuntimeException If the tickers do not exist
     * @throws RuntimeException If the quantities are less than or equal to zero
     */
    @Transactional
    public void UpdateCryptoExchange(CryptoExchange exchange)
    {
        // Print ids
        System.out.println("Exchange ID: " + exchange.GetId());
        System.out.println("Sold ID: " + exchange.GetSoldCrypto().GetId());
        System.out.println("Received ID: " + exchange.GetReceivedCrypto().GetId());

        CryptoExchange oldExchange =
            m_cryptoExchangeRepository.findById(exchange.GetId())
                .orElseThrow(()
                                 -> new RuntimeException(
                                     "CryptoExchange with id " + exchange.GetId() +
                                     " not found and cannot be updated"));

        m_tickerRepository.findById(exchange.GetSoldCrypto().GetId())
            .orElseThrow(()
                             -> new RuntimeException(
                                 "Source ticker with id " +
                                 exchange.GetSoldCrypto().GetId() +
                                 " not found and cannot update crypto exchange"));

        m_tickerRepository.findById(exchange.GetReceivedCrypto().GetId())
            .orElseThrow(()
                             -> new RuntimeException(
                                 "Target ticker with id " +
                                 exchange.GetReceivedCrypto().GetId() +
                                 " not found and cannot update crypto exchange"));

        if (exchange.GetSoldCrypto().GetId() == exchange.GetReceivedCrypto().GetId())
        {
            throw new RuntimeException("Source and target tickers must be different");
        }

        if (exchange.GetSoldQuantity().compareTo(BigDecimal.ZERO) <= 0 ||
            exchange.GetReceivedQuantity().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Quantity must be greater than zero");
        }

        // Complex update operations
        ChangeSoldCrypto(oldExchange, exchange.GetSoldCrypto());
        ChangeReceivedCrypto(oldExchange, exchange.GetReceivedCrypto());
        ChangeSoldQuantity(oldExchange, exchange.GetSoldQuantity());
        ChangeReceivedQuantity(oldExchange, exchange.GetReceivedQuantity());

        // Trivial update operations
        oldExchange.SetDate(exchange.GetDate());
        oldExchange.SetDescription(exchange.GetDescription());

        m_cryptoExchangeRepository.save(oldExchange);

        logger.info("CryptoExchange with id " + exchange.GetId() + " was updated");
    }

    /**
     * Change the quantity of sold ticker of a crypto exchange
     * @param oldExchange The crypto exchange to be updated
     * @param soldQuantity The new sold quantity
     */
    public void ChangeSoldQuantity(CryptoExchange oldExchange, BigDecimal soldQuantity)
    {
        // Adjust holdings quantity
        oldExchange.GetSoldCrypto().SetCurrentQuantity(
            oldExchange.GetSoldCrypto().GetCurrentQuantity().add(
                oldExchange.GetSoldQuantity().subtract(soldQuantity)));

        m_tickerRepository.save(oldExchange.GetSoldCrypto());

        oldExchange.SetSoldQuantity(soldQuantity);
    }

    /**
     * Change the quantity of received crypto of a crypto exchange
     * @param oldExchange The crypto exchange to be updated
     * @param receivedQuantity The new target quantity
     */
    public void ChangeReceivedQuantity(CryptoExchange oldExchange,
                                       BigDecimal     receivedQuantity)
    {
        // Adjust holdings quantity
        oldExchange.GetReceivedCrypto().SetCurrentQuantity(
            oldExchange.GetReceivedCrypto().GetCurrentQuantity().subtract(
                oldExchange.GetReceivedQuantity().subtract(receivedQuantity)));

        m_tickerRepository.save(oldExchange.GetReceivedCrypto());

        oldExchange.SetReceivedQuantity(receivedQuantity);
    }

    /**
     * Change the sold ticker of a crypto exchange
     * @param exchange The crypto exchange
     * @param soldCrypto The new sold ticker
     */
    private void ChangeSoldCrypto(CryptoExchange oldExchange, Ticker soldCrypto)
    {
        if (oldExchange.GetSoldCrypto().GetId() == soldCrypto.GetId())
        {
            return;
        }

        // Adjust holdings quantity
        oldExchange.GetSoldCrypto().SetCurrentQuantity(
            oldExchange.GetSoldCrypto().GetCurrentQuantity().add(
                oldExchange.GetSoldQuantity()));

        soldCrypto.SetCurrentQuantity(
            soldCrypto.GetCurrentQuantity().subtract(oldExchange.GetSoldQuantity()));

        m_tickerRepository.save(oldExchange.GetSoldCrypto());
        m_tickerRepository.save(soldCrypto);

        oldExchange.SetSoldCrypto(soldCrypto);
    }

    /**
     * Change the received ticker of a crypto exchange
     * @param exchange The crypto exchange
     * @param receivedCrypto The new target ticker
     */
    private void ChangeReceivedCrypto(CryptoExchange oldExchange, Ticker receivedCrypto)
    {
        if (oldExchange.GetReceivedCrypto().GetId() == receivedCrypto.GetId())
        {
            return;
        }

        // Adjust holdings quantity
        oldExchange.GetReceivedCrypto().SetCurrentQuantity(
            oldExchange.GetReceivedCrypto().GetCurrentQuantity().subtract(
                oldExchange.GetReceivedQuantity()));

        receivedCrypto.SetCurrentQuantity(
            receivedCrypto.GetCurrentQuantity().add(oldExchange.GetReceivedQuantity()));

        m_tickerRepository.save(oldExchange.GetReceivedCrypto());
        m_tickerRepository.save(receivedCrypto);

        oldExchange.SetReceivedCrypto(receivedCrypto);
    }

    public void ResetAveragePrice(Long tickerId)
    {
        Ticker ticker = m_tickerRepository.findById(tickerId).orElseThrow(
            ()
                -> new RuntimeException("Ticker with id " + tickerId +
                                        " not found and cannot reset average price"));

        ticker.SetAveragePrice(BigDecimal.ZERO);
        ticker.SetAveragePriceCount(BigDecimal.ZERO);

        m_tickerRepository.save(ticker);

        logger.info("Average price of ticker " + ticker.GetSymbol() +
                    " was reset to zero");
    }

    /**
     * Get all tickers
     * @return A list with all tickers
     */
    public List<Ticker> GetAllTickers()
    {
        return m_tickerRepository.findAllByOrderBySymbolAsc();
    }

    /**
     * Get all non-archived tickers
     * @return A list with all non-archived tickers
     */
    public List<Ticker> GetAllNonArchivedTickers()
    {
        return m_tickerRepository.findAllByArchivedFalseOrderBySymbolAsc();
    }

    /**
     * Get all archived tickers
     * @return A list with all archived tickers
     */
    public List<Ticker> GetAllArchivedTickers()
    {
        return m_tickerRepository.findAllByArchivedTrueOrderBySymbolAsc();
    }

    /**
     * Get all non-archived tickers of a specific type
     * @param type The type of the tickers
     */
    public List<Ticker> GetAllNonArchivedTickersByType(TickerType type)
    {
        return m_tickerRepository.findAllByTypeAndArchivedFalseOrderBySymbolAsc(type);
    }

    /**
     * Get count of transactions associated with the ticker
     * @param tickerId The id of the ticker
     * @return The count of transactions associated with the ticker
     */
    public Long GetTransactionCountByTicker(Long tickerId)
    {
        return GetPurchaseCountByTicker(tickerId) + GetSaleCountByTicker(tickerId) +
            GetDividendCountByTicker(tickerId) +
            GetCryptoExchangeCountByTicker(tickerId);
    }

    /**
     * Get count of purchases associated with the ticker
     * @param tickerId The id of the ticker
     * @return The count of purchases associated with the ticker
     */
    public Long GetPurchaseCountByTicker(Long tickerId)
    {
        return m_tickerRepository.GetPurchaseCountByTicker(tickerId);
    }

    /**
     * Get count of sales associated with the ticker
     * @param tickerId The id of the ticker
     * @return The count of sales associated with the ticker
     */
    public Long GetSaleCountByTicker(Long tickerId)
    {
        return m_tickerRepository.GetSaleCountByTicker(tickerId);
    }

    /**
     * Get count of dividends associated with the ticker
     * @param tickerId The id of the ticker
     * @return The count of dividends associated with the ticker
     */
    public Long GetDividendCountByTicker(Long tickerId)
    {
        return m_tickerRepository.GetDividendCountByTicker(tickerId);
    }

    /**
     * Get count of crypto exchanges associated with the ticker
     * @param tickerId The id of the ticker
     * @return The count of crypto exchanges associated with the ticker
     */
    public Long GetCryptoExchangeCountByTicker(Long tickerId)
    {
        return m_tickerRepository.GetCryptoExchangeCountByTicker(tickerId);
    }

    /**
     * Get all purchases
     * @return A list with all purchases
     */
    public List<TickerPurchase> GetAllPurchases()
    {
        return m_tickerPurchaseRepository.findAll();
    }

    /**
     * Get all sales
     * @return A list with all sales
     */
    public List<TickerSale> GetAllSales()
    {
        return m_tickerSaleRepository.findAll();
    }

    /**
     * Get all dividends
     * @return A list with all dividends
     */
    public List<Dividend> GetAllDividends()
    {
        return m_dividendRepository.findAll();
    }

    /**
     * Get all crypto exchanges
     * @return A list with all crypto exchanges
     */
    public List<CryptoExchange> GetAllCryptoExchanges()
    {
        return m_cryptoExchangeRepository.findAll();
    }
}
