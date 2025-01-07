/*
 * Filename: TickerService.java
 * Created on: January  6, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import org.moinex.entities.WalletType;
import org.moinex.entities.investment.Ticker;
import org.moinex.repositories.TickerRepository;
import org.moinex.repositories.TransferRepository;
import org.moinex.repositories.WalletRepository;
import org.moinex.repositories.WalletTransactionRepository;
import org.moinex.repositories.WalletTypeRepository;
import org.moinex.util.Constants;
import org.moinex.util.LoggerConfig;
import org.moinex.util.TickerType;
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

    private static final Logger logger = LoggerConfig.GetLogger();

    public TickerService() { }

    /**
     * Registers a new ticker
     * @param name The name of the ticker
     * @param symbol The symbol of the ticker
     * @param type The type of the ticker
     * @param price The price of the ticker
     * @throws RuntimeException If the ticker already exists
     * @return The id of the registered ticker
     */
    @Transactional
    public Long
    RegisterTicker(String name, String symbol, TickerType type, BigDecimal price)
    {
        if (m_tickerRepository.existsBySymbol(symbol))
        {
            logger.warning("Ticker with symbol " + symbol + " already exists");

            throw new RuntimeException("Ticker with symbol " + symbol +
                                       " already exists");
        }

        Ticker ticker = new Ticker(name,
                                   symbol,
                                   type,
                                   BigDecimal.valueOf(0.0),
                                   price,
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
