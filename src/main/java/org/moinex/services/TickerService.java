/*
 * Filename: TickerService.java
 * Created on: January  6, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import org.moinex.entities.Category;
import org.moinex.entities.Wallet;
import org.moinex.entities.WalletTransaction;
import org.moinex.entities.WalletType;
import org.moinex.entities.investment.Dividend;
import org.moinex.entities.investment.Purchase;
import org.moinex.entities.investment.Sale;
import org.moinex.entities.investment.Ticker;
import org.moinex.repositories.DividendRepository;
import org.moinex.repositories.PurchaseRepository;
import org.moinex.repositories.SaleRepository;
import org.moinex.repositories.TickerRepository;
import org.moinex.repositories.TransferRepository;
import org.moinex.repositories.WalletRepository;
import org.moinex.repositories.WalletTransactionRepository;
import org.moinex.repositories.WalletTypeRepository;
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
    private PurchaseRepository m_purchaseRepository;

    @Autowired
    private SaleRepository m_saleRepository;

    @Autowired
    private DividendRepository m_dividendRepository;

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

        Purchase purchase =
            new Purchase(ticker, quantity, unitPrice, walletTransaction);

        m_purchaseRepository.save(purchase);

        // Update holdings quantity
        ticker.SetCurrentQuantity(ticker.GetCurrentQuantity().add(quantity));

        // Update average price
        BigDecimal updatedTotalValue =
            ticker.GetAveragePrice().multiply(ticker.GetAveragePriceCount());

        updatedTotalValue = updatedTotalValue.add(unitPrice.multiply(quantity));

        ticker.SetAveragePrice(
            updatedTotalValue.divide(ticker.GetAveragePriceCount().add(quantity),
                                     RoundingMode.HALF_UP));

        ticker.SetAveragePriceCount(ticker.GetAveragePriceCount().add(quantity));

        logger.info("Purchase with id " + purchase.GetId() + " added to ticker " +
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

        Sale sale = new Sale(ticker, quantity, unitPrice, walletTransaction);

        m_saleRepository.save(sale);

        // Update holdings quantity
        ticker.SetCurrentQuantity(ticker.GetCurrentQuantity().subtract(quantity));

        // If sold all holdings, reset average price
        if (ticker.GetCurrentQuantity().compareTo(BigDecimal.ZERO) == 0)
        {
            ResetAveragePrice(tickerId);
        }

        m_tickerRepository.save(ticker);

        logger.info("Sale with id " + sale.GetId() + " added to ticker " +
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
     * Get count of transactions associated with the ticker
     * @param tickerId The id of the ticker
     * @return The count of transactions associated with the ticker
     */
    public Long GetTransactionCountByTicker(Long tickerId)
    {
        return GetPurchaseCountByTicker(tickerId) + GetSaleCountByTicker(tickerId) +
            GetDividendCountByTicker(tickerId);
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
}
