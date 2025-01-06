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
}
