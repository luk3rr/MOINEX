/*
 * Filename: TickerServiceTest.java
 * Created on: January  7, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.entities.Category;
import org.moinex.entities.Wallet;
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
import org.moinex.util.TickerType;
import org.moinex.util.TransactionStatus;
import org.moinex.util.TransactionType;

@ExtendWith(MockitoExtension.class)
public class TickerServiceTest
{
    @Mock
    private TickerRepository m_tickerRepository;

    @Mock
    private TickerPurchaseRepository m_tickerPurchaseRepository;

    @Mock
    private TickerSaleRepository m_tickerSaleRepository;

    @Mock
    private DividendRepository m_dividendRepository;

    @Mock
    private CryptoExchangeRepository m_cryptoExchangeRepository;

    @Mock
    private WalletRepository m_walletRepository;

    @Mock
    private WalletTransactionService m_walletTransactionService;

    @InjectMocks
    private TickerService m_tickerService;

    private Ticker            m_ticker1;
    private Ticker            m_ticker2;
    private Ticker            m_crypto1;
    private Ticker            m_crypto2;
    private Ticker            m_crypto3;
    private Wallet            m_wallet;
    private WalletTransaction m_purchaseTransaction;
    private WalletTransaction m_saleTransaction;
    private WalletTransaction m_dividendTransaction;
    private TickerPurchase    m_purchase;
    private TickerSale        m_sale;
    private Dividend          m_dividend;
    private CryptoExchange    m_exchange;
    private CryptoExchange    m_exchangeCrypto1ToCrypto2;
    private Category          m_category;

    @BeforeEach
    public void setUp()
    {
        m_ticker1 = new Ticker(1L,
                               "Ticker1",
                               "T1",
                               TickerType.STOCK,
                               BigDecimal.ZERO,
                               new BigDecimal("100"),
                               new BigDecimal("100"),
                               LocalDateTime.now());

        m_ticker2 = new Ticker(2L,
                               "Ticker2",
                               "T2",
                               TickerType.STOCK,
                               BigDecimal.ZERO,
                               new BigDecimal("100"),
                               new BigDecimal("100"),
                               LocalDateTime.now());

        m_crypto1 = new Ticker(3L,
                               "crypto1",
                               "T3",
                               TickerType.CRYPTOCURRENCY,
                               BigDecimal.ZERO,
                               new BigDecimal("10"),
                               new BigDecimal("10"),
                               LocalDateTime.now());

        m_crypto2 = new Ticker(4L,
                               "crypto2",
                               "T4",
                               TickerType.CRYPTOCURRENCY,
                               BigDecimal.ZERO,
                               new BigDecimal("10"),
                               new BigDecimal("10"),
                               LocalDateTime.now());

        m_crypto3 = new Ticker(5L,
                               "crypto3",
                               "T5",
                               TickerType.CRYPTOCURRENCY,
                               BigDecimal.ZERO,
                               new BigDecimal("10"),
                               new BigDecimal("10"),
                               LocalDateTime.now());

        m_wallet = new Wallet(1L, "Main Wallet", BigDecimal.ZERO);

        m_category = Category.builder().name("Category").build();

        m_purchaseTransaction = WalletTransaction.builder()
                                    .wallet(m_wallet)
                                    .category(m_category)
                                    .type(TransactionType.EXPENSE)
                                    .status(TransactionStatus.CONFIRMED)
                                    .date(LocalDateTime.now())
                                    .amount(new BigDecimal("50"))
                                    .description("TickerPurchase")
                                    .build();

        m_purchase = new TickerPurchase(1L,
                                        m_ticker1,
                                        new BigDecimal("1"),
                                        new BigDecimal("50"),
                                        m_purchaseTransaction);

        m_saleTransaction = WalletTransaction.builder()
                                .wallet(m_wallet)
                                .category(m_category)
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.CONFIRMED)
                                .date(LocalDateTime.now())
                                .amount(new BigDecimal("50"))
                                .description("TickerSale")
                                .build();

        m_sale = new TickerSale(1L,
                                m_ticker1,
                                new BigDecimal("1"),
                                new BigDecimal("50"),
                                m_saleTransaction,
                                new BigDecimal("50"));

        m_dividendTransaction = WalletTransaction.builder()
                                    .wallet(m_wallet)
                                    .category(m_category)
                                    .type(TransactionType.INCOME)
                                    .status(TransactionStatus.CONFIRMED)
                                    .date(LocalDateTime.now())
                                    .amount(new BigDecimal("50"))
                                    .description("Dividend Payment")
                                    .build();

        m_dividend = new Dividend(1L, m_ticker1, m_dividendTransaction);

        m_exchange = new CryptoExchange(1L,
                                        m_ticker1,
                                        m_ticker1,
                                        new BigDecimal("1"),
                                        new BigDecimal("1"),
                                        LocalDateTime.now(),
                                        "");

        m_exchangeCrypto1ToCrypto2 = new CryptoExchange(2L,
                                                        m_crypto1,
                                                        m_crypto2,
                                                        new BigDecimal("1"),
                                                        new BigDecimal("1"),
                                                        LocalDateTime.now(),
                                                        "");
    }

    @Test
    @DisplayName("Test if a ticker is registered successfully")
    public void testRegisterTicker()
    {
        when(m_tickerRepository.existsBySymbol(m_ticker1.getSymbol()))
            .thenReturn(false);

        m_tickerService.addTicker(m_ticker1.getName(),
                                  m_ticker1.getSymbol(),
                                  m_ticker1.getType(),
                                  m_ticker1.getCurrentUnitValue(),
                                  m_ticker1.getAverageUnitValue(),
                                  m_ticker1.getCurrentQuantity());

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository).save(tickerCaptor.capture());

        Ticker savedTicker = tickerCaptor.getValue();
        assertEquals(m_ticker1.getName(), savedTicker.getName());
        assertEquals(m_ticker1.getSymbol(), savedTicker.getSymbol());
        assertEquals(m_ticker1.getType(), savedTicker.getType());
        assertEquals(0,
                     m_ticker1.getCurrentUnitValue().compareTo(
                         savedTicker.getCurrentUnitValue()));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when registering a ticker with an existing symbol")
    public void
    testRegisterTickerAlreadyExists()
    {
        when(m_tickerRepository.existsBySymbol(m_ticker1.getSymbol())).thenReturn(true);

        assertThrows(RuntimeException.class,
                     ()
                         -> m_tickerService.addTicker(m_ticker1.getName(),
                                                      m_ticker1.getSymbol(),
                                                      m_ticker1.getType(),
                                                      m_ticker1.getCurrentUnitValue(),
                                                      m_ticker1.getAverageUnitValue(),
                                                      m_ticker1.getCurrentQuantity()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when registering a ticker with an empty name")
    public void
    testRegisterTickerEmptyName()
    {
        // Test with empty name
        assertThrows(RuntimeException.class,
                     ()
                         -> m_tickerService.addTicker("",
                                                      m_ticker1.getSymbol(),
                                                      m_ticker1.getType(),
                                                      m_ticker1.getCurrentUnitValue(),
                                                      m_ticker1.getAverageUnitValue(),
                                                      m_ticker1.getCurrentQuantity()));
        // Test with blank name
        assertThrows(RuntimeException.class,
                     ()
                         -> m_tickerService.addTicker(" ",
                                                      m_ticker1.getSymbol(),
                                                      m_ticker1.getType(),
                                                      m_ticker1.getCurrentUnitValue(),
                                                      m_ticker1.getAverageUnitValue(),
                                                      m_ticker1.getCurrentQuantity()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when registering a ticker with an empty symbol")
    public void
    testRegisterTickerEmptySymbol()
    {
        // Test with empty symbol
        assertThrows(RuntimeException.class,
                     ()
                         -> m_tickerService.addTicker(m_ticker1.getName(),
                                                      "",
                                                      m_ticker1.getType(),
                                                      m_ticker1.getCurrentUnitValue(),
                                                      m_ticker1.getAverageUnitValue(),
                                                      m_ticker1.getCurrentQuantity()));

        // Test with blank symbol
        assertThrows(RuntimeException.class,
                     ()
                         -> m_tickerService.addTicker(m_ticker1.getName(),
                                                      " ",
                                                      m_ticker1.getType(),
                                                      m_ticker1.getCurrentUnitValue(),
                                                      m_ticker1.getAverageUnitValue(),
                                                      m_ticker1.getCurrentQuantity()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when registering a ticker with price "
                 + "less than or equal to zero")
    public void
    testRegisterTickerInvalidPrice()
    {
        when(m_tickerRepository.existsBySymbol(m_ticker1.getSymbol()))
            .thenReturn(false);

        // Test with price less than zero
        assertThrows(RuntimeException.class,
                     ()
                         -> m_tickerService.addTicker(m_ticker1.getName(),
                                                      m_ticker1.getSymbol(),
                                                      m_ticker1.getType(),
                                                      new BigDecimal("-0.05"),
                                                      m_ticker1.getAverageUnitValue(),
                                                      m_ticker1.getCurrentQuantity()));

        // Test with price equal to zero
        assertThrows(RuntimeException.class,
                     ()
                         -> m_tickerService.addTicker(m_ticker1.getName(),
                                                      m_ticker1.getSymbol(),
                                                      m_ticker1.getType(),
                                                      BigDecimal.ZERO,
                                                      m_ticker1.getAverageUnitValue(),
                                                      m_ticker1.getCurrentQuantity()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when registering a ticker with average unit price "
        + "less than zero")
    public void
    testRegisterTickerInvalidAverageUnitPrice()
    {
        when(m_tickerRepository.existsBySymbol(m_ticker1.getSymbol()))
            .thenReturn(false);

        // Test with average unit price less than zero
        assertThrows(RuntimeException.class,
                     ()
                         -> m_tickerService.addTicker(m_ticker1.getName(),
                                                      m_ticker1.getSymbol(),
                                                      m_ticker1.getType(),
                                                      m_ticker1.getCurrentUnitValue(),
                                                      new BigDecimal("-0.05"),
                                                      m_ticker1.getCurrentQuantity()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if a ticker is deleted successfully")
    public void testDeleteTicker()
    {
        when(m_tickerRepository.findById(m_ticker1.getId()))
            .thenReturn(Optional.of(m_ticker1));

        when(m_tickerRepository.getPurchaseCountByTicker(m_ticker1.getId()))
            .thenReturn(0L);
        when(m_tickerRepository.getSaleCountByTicker(m_ticker1.getId())).thenReturn(0L);
        when(m_tickerRepository.getDividendCountByTicker(m_ticker1.getId()))
            .thenReturn(0L);
        when(m_tickerRepository.getCryptoExchangeCountByTicker(m_ticker1.getId()))
            .thenReturn(0L);

        m_tickerService.deleteTicker(m_ticker1.getId());

        verify(m_tickerRepository).delete(m_ticker1);
    }

    @Test
    @DisplayName("Test if exception is thrown when deleting a non-existent ticker")
    public void testDeleteTickerNotFound()
    {
        when(m_tickerRepository.findById(m_ticker1.getId()))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.deleteTicker(m_ticker1.getId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when deleting a ticker with purchases")
    public void testDeleteTickerWithPurchases()
    {
        when(m_tickerRepository.findById(m_ticker1.getId()))
            .thenReturn(Optional.of(m_ticker1));

        when(m_tickerRepository.getPurchaseCountByTicker(m_ticker1.getId()))
            .thenReturn(1L);
        when(m_tickerRepository.getSaleCountByTicker(m_ticker1.getId())).thenReturn(0L);
        when(m_tickerRepository.getDividendCountByTicker(m_ticker1.getId()))
            .thenReturn(0L);
        when(m_tickerRepository.getCryptoExchangeCountByTicker(m_ticker1.getId()))
            .thenReturn(0L);

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.deleteTicker(m_ticker1.getId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when deleting a ticker with sales")
    public void testDeleteTickerWithSales()
    {
        when(m_tickerRepository.findById(m_ticker1.getId()))
            .thenReturn(Optional.of(m_ticker1));

        when(m_tickerRepository.getPurchaseCountByTicker(m_ticker1.getId()))
            .thenReturn(0L);
        when(m_tickerRepository.getSaleCountByTicker(m_ticker1.getId())).thenReturn(1L);
        when(m_tickerRepository.getDividendCountByTicker(m_ticker1.getId()))
            .thenReturn(0L);
        when(m_tickerRepository.getCryptoExchangeCountByTicker(m_ticker1.getId()))
            .thenReturn(0L);

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.deleteTicker(m_ticker1.getId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when deleting a ticker with dividends")
    public void testDeleteTickerWithDividends()
    {
        when(m_tickerRepository.findById(m_ticker1.getId()))
            .thenReturn(Optional.of(m_ticker1));

        when(m_tickerRepository.getPurchaseCountByTicker(m_ticker1.getId()))
            .thenReturn(0L);
        when(m_tickerRepository.getSaleCountByTicker(m_ticker1.getId())).thenReturn(0L);
        when(m_tickerRepository.getDividendCountByTicker(m_ticker1.getId()))
            .thenReturn(1L);
        when(m_tickerRepository.getCryptoExchangeCountByTicker(m_ticker1.getId()))
            .thenReturn(0L);

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.deleteTicker(m_ticker1.getId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when deleting a ticker with crypto exchanges")
    public void
    testDeleteTickerWithCryptoExchanges()
    {
        when(m_tickerRepository.findById(m_ticker1.getId()))
            .thenReturn(Optional.of(m_ticker1));

        when(m_tickerRepository.getPurchaseCountByTicker(m_ticker1.getId()))
            .thenReturn(0L);
        when(m_tickerRepository.getSaleCountByTicker(m_ticker1.getId())).thenReturn(0L);
        when(m_tickerRepository.getDividendCountByTicker(m_ticker1.getId()))
            .thenReturn(0L);
        when(m_tickerRepository.getCryptoExchangeCountByTicker(m_ticker1.getId()))
            .thenReturn(1L);

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.deleteTicker(m_ticker1.getId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when deleting a ticker with transactions")
    public void testDeleteTickerWithTransactions()
    {
        when(m_tickerRepository.findById(m_ticker1.getId()))
            .thenReturn(Optional.of(m_ticker1));

        when(m_tickerRepository.getPurchaseCountByTicker(m_ticker1.getId()))
            .thenReturn(1L);
        when(m_tickerRepository.getSaleCountByTicker(m_ticker1.getId())).thenReturn(2L);
        when(m_tickerRepository.getDividendCountByTicker(m_ticker1.getId()))
            .thenReturn(3L);
        when(m_tickerRepository.getCryptoExchangeCountByTicker(m_ticker1.getId()))
            .thenReturn(4L);

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.deleteTicker(m_ticker1.getId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if a ticker is archived successfully")
    public void testArchiveTicker()
    {
        when(m_tickerRepository.findById(m_ticker1.getId()))
            .thenReturn(Optional.of(m_ticker1));

        m_tickerService.archiveTicker(m_ticker1.getId());

        verify(m_tickerRepository).save(m_ticker1);
        assertTrue(m_ticker1.getIsArchived());
    }

    @Test
    @DisplayName("Test if exception is thrown when archiving a non-existent ticker")
    public void testArchiveTickerNotFound()
    {
        when(m_tickerRepository.findById(m_ticker1.getId()))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.archiveTicker(m_ticker1.getId()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if a ticker is unarchived successfully")
    public void testUnarchiveTicker()
    {
        when(m_tickerRepository.findById(m_ticker1.getId()))
            .thenReturn(Optional.of(m_ticker1));

        m_tickerService.unarchiveTicker(m_ticker1.getId());

        verify(m_tickerRepository).save(m_ticker1);
        assertFalse(m_ticker1.getIsArchived());
    }

    @Test
    @DisplayName("Test if exception is thrown when unarchiving a non-existent ticker")
    public void testUnarchiveTickerNotFound()
    {
        when(m_tickerRepository.findById(m_ticker1.getId()))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.unarchiveTicker(m_ticker1.getId()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if a purchase is added successfully to a ticker")
    public void testAddPurchase()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        when(m_walletTransactionService
                 .addExpense(anyLong(), any(), any(), any(), any(), any()))
            .thenReturn(100L);

        when(m_walletTransactionService.getTransactionById(100L))
            .thenReturn(WalletTransaction.builder()
                            .wallet(m_wallet)
                            .category(m_category)
                            .type(TransactionType.EXPENSE)
                            .status(TransactionStatus.CONFIRMED)
                            .date(LocalDateTime.now())
                            .amount(new BigDecimal("50"))
                            .description("TickerPurchase")
                            .build());

        m_tickerService.addPurchase(1L,
                                    1L,
                                    new BigDecimal("10"),
                                    new BigDecimal("150"),
                                    m_category,
                                    LocalDateTime.now(),
                                    "TickerPurchase",
                                    TransactionStatus.CONFIRMED);

        // Capture the purchase object that was saved and check its values
        ArgumentCaptor<TickerPurchase> purchaseCaptor =
            ArgumentCaptor.forClass(TickerPurchase.class);

        verify(m_tickerPurchaseRepository).save(purchaseCaptor.capture());

        assertEquals(m_ticker1, purchaseCaptor.getValue().getTicker());
        assertEquals(new BigDecimal("10"), purchaseCaptor.getValue().getQuantity());
    }

    @Test
    @DisplayName(
        "Test if adding a purchase to a non-existent ticker throws an exception")
    public void
    testAddPurchaseTickerNotFound()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.addPurchase(1L,
                                        1L,
                                        new BigDecimal("10"),
                                        new BigDecimal("150"),
                                        m_category,
                                        LocalDateTime.now(),
                                        "TickerPurchase",
                                        TransactionStatus.CONFIRMED);
        });

        // Verify that the purchase was not saved
        verify(m_tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
    }

    @Test
    @DisplayName("Test if adding a purchase with quantity less than or equal to zero "
                 + "throws an exception")
    public void
    testAddPurchaseInvalidQuantity()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.addPurchase(1L,
                                        1L,
                                        BigDecimal.ZERO,
                                        new BigDecimal("150"),
                                        m_category,
                                        LocalDateTime.now(),
                                        "TickerPurchase",
                                        TransactionStatus.CONFIRMED);
        });

        // Verify that the purchase was not saved
        verify(m_tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
    }

    @Test
    @DisplayName("Test if adding a purchase with unit price less than or equal to "
                 + "zero throws an exception")
    public void
    testAddPurchaseInvalidUnitPrice()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.addPurchase(1L,
                                        1L,
                                        new BigDecimal("10"),
                                        BigDecimal.ZERO,
                                        m_category,
                                        LocalDateTime.now(),
                                        "TickerPurchase",
                                        TransactionStatus.CONFIRMED);
        });

        // Verify that the purchase was not saved
        verify(m_tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
    }

    @Test
    @DisplayName("Test if a sale is added successfully to a ticker")
    public void testAddSale()
    {
        m_ticker1.setCurrentQuantity(new BigDecimal("20"));

        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        when(m_walletTransactionService
                 .addIncome(anyLong(), any(), any(), any(), any(), any()))
            .thenReturn(100L);

        when(m_walletTransactionService.getTransactionById(100L))
            .thenReturn(WalletTransaction.builder()
                            .wallet(m_wallet)
                            .category(m_category)
                            .type(TransactionType.INCOME)
                            .status(TransactionStatus.CONFIRMED)
                            .date(LocalDateTime.now())
                            .amount(new BigDecimal("50"))
                            .description("TickerSale")
                            .build());

        m_tickerService.addSale(1L,
                                1L,
                                new BigDecimal("10"),
                                new BigDecimal("200"),
                                m_category,
                                LocalDateTime.now(),
                                "TickerSale",
                                TransactionStatus.CONFIRMED);

        // Capture the sale object that was saved and check its values
        ArgumentCaptor<TickerSale> saleCapptor =
            ArgumentCaptor.forClass(TickerSale.class);

        verify(m_tickerSaleRepository).save(saleCapptor.capture());

        assertEquals(m_ticker1, saleCapptor.getValue().getTicker());
        assertEquals(new BigDecimal("10"), saleCapptor.getValue().getQuantity());
    }

    @Test
    @DisplayName("Test if adding a sale to a non-existent ticker throws an exception")
    public void testAddSaleTickerNotFound()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.addSale(1L,
                                    1L,
                                    new BigDecimal("10"),
                                    new BigDecimal("200"),
                                    m_category,
                                    LocalDateTime.now(),
                                    "TickerSale",
                                    TransactionStatus.CONFIRMED);
        });

        // Verify that the sale was not saved
        verify(m_tickerSaleRepository, never()).save(any(TickerSale.class));
    }

    @Test
    @DisplayName("Test if adding a sale with quantity greater than current quantity "
                 + "throws an exception")
    public void
    testAddSaleExceedsQuantity()
    {
        m_ticker1.setCurrentQuantity(new BigDecimal("5"));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.addSale(1L,
                                    1L,
                                    new BigDecimal("10"),
                                    new BigDecimal("200"),
                                    m_category,
                                    LocalDateTime.now(),
                                    "TickerSale",
                                    TransactionStatus.CONFIRMED);
        });

        // Verify that the sale was not saved
        verify(m_tickerSaleRepository, never()).save(any(TickerSale.class));
    }

    @Test
    @DisplayName("Test if adding a sale with unit price less than or equal to zero "
                 + "throws an exception")
    public void
    testAddSaleInvalidUnitPrice()
    {
        m_ticker1.setCurrentQuantity(new BigDecimal("10"));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.addSale(1L,
                                    1L,
                                    new BigDecimal("5"),
                                    BigDecimal.ZERO,
                                    m_category,
                                    LocalDateTime.now(),
                                    "TickerSale",
                                    TransactionStatus.CONFIRMED);
        });

        // Verify that the sale was not saved
        verify(m_tickerSaleRepository, never()).save(any(TickerSale.class));
    }

    @Test
    @DisplayName("Test if a dividend is added successfully to a ticker")
    public void testAddDividend()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        when(m_walletTransactionService
                 .addIncome(anyLong(), any(), any(), any(), any(), any()))
            .thenReturn(100L);

        when(m_walletTransactionService.getTransactionById(100L))
            .thenReturn(WalletTransaction.builder()
                            .wallet(m_wallet)
                            .category(m_category)
                            .type(TransactionType.INCOME)
                            .status(TransactionStatus.CONFIRMED)
                            .date(LocalDateTime.now())
                            .amount(new BigDecimal("50"))
                            .description("Dividend Payment")
                            .build());

        m_tickerService.addDividend(1L,
                                    1L,
                                    m_category,
                                    new BigDecimal("50"),
                                    LocalDateTime.now(),
                                    "Dividend Payment",
                                    TransactionStatus.CONFIRMED);

        verify(m_dividendRepository).save(any(Dividend.class));
    }

    @Test
    @DisplayName(
        "Test if adding a dividend to a non-existent ticker throws an exception")
    public void
    testAddDividendTickerNotFound()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.addDividend(1L,
                                        1L,
                                        m_category,
                                        new BigDecimal("50"),
                                        LocalDateTime.now(),
                                        "Dividend Payment",
                                        TransactionStatus.CONFIRMED);
        });

        // Verify that the dividend was not saved
        verify(m_dividendRepository, never()).save(any(Dividend.class));

        // Verify that the wallet transaction was not created
        verify(m_walletTransactionService, never())
            .addIncome(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Test if adding a dividend with amount less than or equal to zero "
                 + "throws an exception")
    public void
    testAddDividendInvalidAmount()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.addDividend(1L,
                                        1L,
                                        m_category,
                                        BigDecimal.ZERO,
                                        LocalDateTime.now(),
                                        "Dividend Payment",
                                        TransactionStatus.CONFIRMED);
        });

        // Verify that the dividend was not saved
        verify(m_dividendRepository, never()).save(any(Dividend.class));

        // Verify that the wallet transaction was not created
        verify(m_walletTransactionService, never())
            .addIncome(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Test if a crypto exchange is added successfully to a ticker")
    public void testAddCryptoExchange()
    {
        m_ticker1.setType(TickerType.CRYPTOCURRENCY);
        m_ticker2.setType(TickerType.CRYPTOCURRENCY);

        m_ticker1.setCurrentQuantity(new BigDecimal("10"));
        m_ticker2.setCurrentQuantity(new BigDecimal("0"));

        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));
        when(m_tickerRepository.findById(2L)).thenReturn(Optional.of(m_ticker2));

        when(m_cryptoExchangeRepository.save(any(CryptoExchange.class)))
            .thenReturn(m_exchange);

        BigDecimal sourcePreviousQuantity = m_ticker1.getCurrentQuantity();
        BigDecimal targetPreviousQuantity = m_ticker2.getCurrentQuantity();

        BigDecimal sourceQuantity = new BigDecimal("1");
        BigDecimal targetQuantity = new BigDecimal("1");

        m_tickerService.addCryptoExchange(1L,
                                          2L,
                                          sourceQuantity,
                                          targetQuantity,
                                          LocalDateTime.now(),
                                          "");

        // Capture the exchange object that was saved and check its values
        ArgumentCaptor<CryptoExchange> exchangeCaptor =
            ArgumentCaptor.forClass(CryptoExchange.class);

        verify(m_cryptoExchangeRepository).save(exchangeCaptor.capture());

        assertEquals(m_ticker1, exchangeCaptor.getValue().getSoldCrypto());

        assertEquals(m_ticker2, exchangeCaptor.getValue().getReceivedCrypto());

        // Capture the source and target tickers after the exchange
        BigDecimal sourceCurrentQuantity = m_ticker1.getCurrentQuantity();
        BigDecimal targetCurrentQuantity = m_ticker2.getCurrentQuantity();

        // Check if the quantities were updated correctly
        assertEquals(0,
                     sourceCurrentQuantity.compareTo(
                         sourcePreviousQuantity.subtract(sourceQuantity)));

        assertEquals(0,
                     targetCurrentQuantity.compareTo(
                         targetPreviousQuantity.add(targetQuantity)));
    }

    @Test
    @DisplayName("Test if adding a crypto exchange with source and target tickers "
                 + "being the same throws an exception")
    public void
    testAddCryptoExchangeSameTicker()
    {
        assertThrows(RuntimeException.class, () -> {
            m_tickerService.addCryptoExchange(1L,
                                              1L,
                                              new BigDecimal("1"),
                                              new BigDecimal("1"),
                                              LocalDateTime.now(),
                                              "");
        });

        // Verify that the exchange was not saved
        verify(m_cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
    }

    @Test
    @DisplayName("Test if adding a crypto exchange with source ticker not found "
                 + "throws an exception")
    public void
    testAddCryptoExchangeSourceNotFound()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.addCryptoExchange(1L,
                                              2L,
                                              new BigDecimal("1"),
                                              new BigDecimal("1"),
                                              LocalDateTime.now(),
                                              "");
        });

        // Verify that the exchange was not saved
        verify(m_cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
    }

    @Test
    @DisplayName("Test if adding a crypto exchange with target ticker not found "
                 + "throws an exception")
    public void
    testAddCryptoExchangeTargetNotFound()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));
        when(m_tickerRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.addCryptoExchange(1L,
                                              2L,
                                              new BigDecimal("1"),
                                              new BigDecimal("1"),
                                              LocalDateTime.now(),
                                              "");
        });

        // Verify that the exchange was not saved
        verify(m_cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
    }

    @Test
    @DisplayName(
        "Test if adding a crypto exchange with source or target ticker type is not "
        + " cryptocurrency throws an exception")
    public void
    testAddCryptoExchangeInvalidTickerType()
    {
        m_ticker1.setType(TickerType.STOCK);
        m_ticker2.setType(TickerType.STOCK);

        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));
        when(m_tickerRepository.findById(2L)).thenReturn(Optional.of(m_ticker2));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.addCryptoExchange(1L,
                                              2L,
                                              new BigDecimal("1"),
                                              new BigDecimal("1"),
                                              LocalDateTime.now(),
                                              "");
        });

        // Verify that the exchange was not saved
        verify(m_cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
    }

    @Test
    @DisplayName("Test if adding a crypto exchange with source quantity less than or "
                 + "equal to zero throws an exception")
    public void
    testAddCryptoExchangeInvalidSourceQuantity()
    {
        m_ticker1.setType(TickerType.CRYPTOCURRENCY);
        m_ticker2.setType(TickerType.CRYPTOCURRENCY);

        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));
        when(m_tickerRepository.findById(2L)).thenReturn(Optional.of(m_ticker2));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.addCryptoExchange(1L,
                                              2L,
                                              BigDecimal.ZERO,
                                              new BigDecimal("1"),
                                              LocalDateTime.now(),
                                              "");
        });

        // Verify that the exchange was not saved
        verify(m_cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
    }

    @Test
    @DisplayName("Test if adding a crypto exchange with target quantity less than or "
                 + "equal to zero throws an exception")
    public void
    testAddCryptoExchangeInvalidTargetQuantity()
    {
        m_ticker1.setType(TickerType.CRYPTOCURRENCY);
        m_ticker2.setType(TickerType.CRYPTOCURRENCY);

        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));
        when(m_tickerRepository.findById(2L)).thenReturn(Optional.of(m_ticker2));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.addCryptoExchange(1L,
                                              2L,
                                              new BigDecimal("1"),
                                              BigDecimal.ZERO,
                                              LocalDateTime.now(),
                                              "");
        });

        // Verify that the exchange was not saved
        verify(m_cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
    }

    @Test
    @DisplayName("Test if adding a crypto exchange with source quantity greater than "
                 + "current quantity throws an exception")
    public void
    testAddCryptoExchangeExceedsSourceQuantity()
    {
        m_ticker1.setType(TickerType.CRYPTOCURRENCY);
        m_ticker2.setType(TickerType.CRYPTOCURRENCY);

        m_ticker1.setCurrentQuantity(new BigDecimal("5"));
        m_ticker2.setCurrentQuantity(new BigDecimal("0"));

        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));
        when(m_tickerRepository.findById(2L)).thenReturn(Optional.of(m_ticker2));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.addCryptoExchange(1L,
                                              2L,
                                              new BigDecimal("10"),
                                              new BigDecimal("1"),
                                              LocalDateTime.now(),
                                              "");
        });

        // Verify that the exchange was not saved
        verify(m_cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
    }

    @Test
    @DisplayName("Test if ticker name is changed successfully")
    public void testChangeTickerName()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        String oldName = m_ticker1.getName();
        String newName = oldName + " Changed";
        m_ticker1.setName(newName);

        m_tickerService.updateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

        assertEquals(newName, tickerCaptor.getValue().getName());
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a non-existent ticker")
    public void testChangeTickerNameNotFound()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when updating a ticker with an empty name")
    public void
    testChangeTickerEmptyName()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.setName("");

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a ticker with a blank name")
    public void testChangeTickerBlankName()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.setName(" ");

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a ticker with null name")
    public void testChangeTickerNullName()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.setName(null);

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if ticker symbol is changed successfully")
    public void testChangeTickerSymbol()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        String oldSymbol = m_ticker1.getSymbol();
        String newSymbol = oldSymbol + " Changed";
        m_ticker1.setSymbol(newSymbol);

        m_tickerService.updateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

        assertEquals(newSymbol, tickerCaptor.getValue().getSymbol());
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when updating a ticker with an empty symbol")
    public void
    testChangeTickerEmptySymbol()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.setSymbol("");

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when updating a ticker with a blank symbol")
    public void
    testChangeTickerBlankSymbol()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.setSymbol(" ");

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a ticker with null symbol")
    public void testChangeTickerNullSymbol()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.setSymbol(null);

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if ticker type is changed successfully")
    public void testChangeTickerType()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        TickerType oldType = m_ticker1.getType();

        TickerType newType;

        if (oldType == TickerType.STOCK)
        {
            newType = TickerType.FUND;
        }
        else
        {
            newType = TickerType.STOCK;
        }

        m_ticker1.setType(newType);

        m_tickerService.updateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

        assertEquals(newType, tickerCaptor.getValue().getType());
    }

    @Test
    @DisplayName("Test if ticker price is changed successfully")
    public void testChangeTickerPrice()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        BigDecimal oldPrice = m_ticker1.getCurrentUnitValue();

        BigDecimal newPrice;

        if (oldPrice.compareTo(BigDecimal.ONE) == 0)
        {
            newPrice = BigDecimal.TEN;
        }
        else
        {
            newPrice = BigDecimal.ONE;
        }

        m_ticker1.setCurrentUnitValue(newPrice);

        m_tickerService.updateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

        assertEquals(0,
                     newPrice.compareTo(tickerCaptor.getValue().getCurrentUnitValue()));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a ticker with price "
                 + "less than zero")
    public void
    testChangeTickerPriceLessThanZero()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.setCurrentUnitValue(new BigDecimal("-0.05"));

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a ticker with price "
                 + "equal to zero")
    public void
    testChangeTickerPriceEqualToZero()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.setCurrentUnitValue(BigDecimal.ZERO);

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if ticker current quantity is changed successfully")
    public void testChangeTickerCurrentQuantity()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        BigDecimal oldQuantity = m_ticker1.getCurrentQuantity();

        BigDecimal newQuantity;

        if (oldQuantity.compareTo(BigDecimal.ONE) == 0)
        {
            newQuantity = BigDecimal.TEN;
        }
        else
        {
            newQuantity = BigDecimal.ONE;
        }

        m_ticker1.setCurrentQuantity(newQuantity);

        m_tickerService.updateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository).save(tickerCaptor.capture());

        assertEquals(newQuantity, tickerCaptor.getValue().getCurrentQuantity());
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when updating a ticker with current quantity "
        + "less than zero")
    public void
    testChangeTickerCurrentQuantityLessThanZero()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.setCurrentQuantity(new BigDecimal("-0.05"));

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if ticker average price is changed successfully")
    public void testChangeTickerAveragePrice()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        BigDecimal oldPrice = m_ticker1.getAverageUnitValue();

        // Quantitycannot be zero, because in this case the average price is reset at
        // the end of the function
        m_ticker1.setCurrentQuantity(new BigDecimal("1"));

        BigDecimal newPrice;

        if (oldPrice.compareTo(BigDecimal.ONE) == 0)
        {
            newPrice = BigDecimal.TEN;
        }
        else
        {
            newPrice = BigDecimal.ONE;
        }

        m_ticker1.setAverageUnitValue(newPrice);

        m_tickerService.updateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

        assertEquals(0,
                     newPrice.compareTo(tickerCaptor.getValue().getAverageUnitValue()));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when updating a ticker with average price "
        + "less than zero")
    public void
    testChangeTickerAveragePriceLessThanZero()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.setAverageUnitValue(new BigDecimal("-0.05"));

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if ticker is set to archived successfully")
    public void testChangeTickerArchived()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.setIsArchived(true);

        m_tickerService.updateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

        assertTrue(tickerCaptor.getValue().getIsArchived());
    }

    @Test
    @DisplayName("Test if ticker is set to unarchived successfully")
    public void testChangeTickerUnarchived()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.setIsArchived(false);

        m_tickerService.updateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

        assertFalse(tickerCaptor.getValue().getIsArchived());
    }

    @Test
    @DisplayName("Test if purchase quantity is updated successfully")
    public void testUpdatePurchaseQuantity()
    {
        when(m_tickerPurchaseRepository.findById(1L))
            .thenReturn(Optional.of(m_purchase));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        BigDecimal oldQuantity = m_purchase.getQuantity();

        BigDecimal newQuantity;

        if (oldQuantity.compareTo(BigDecimal.ONE) == 0)
        {
            newQuantity = BigDecimal.TEN;
        }
        else
        {
            newQuantity = BigDecimal.ONE;
        }

        BigDecimal expectedAmountAfterUpdate =
            newQuantity.multiply(m_purchase.getUnitPrice());

        m_purchase.setQuantity(newQuantity);

        m_tickerService.updatePurchase(m_purchase);

        ArgumentCaptor<TickerPurchase> purchaseCaptor =
            ArgumentCaptor.forClass(TickerPurchase.class);
        verify(m_tickerPurchaseRepository).save(purchaseCaptor.capture());

        assertEquals(newQuantity, purchaseCaptor.getValue().getQuantity());
        assertEquals(expectedAmountAfterUpdate,
                     purchaseCaptor.getValue().getWalletTransaction().getAmount());
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a non-existent purchase")
    public void testUpdatePurchaseNotFound()
    {
        when(m_tickerPurchaseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updatePurchase(m_purchase); });

        verify(m_tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a purchase with quantity "
                 + "less than zero")
    public void
    testUpdatePurchaseQuantityLessThanZero()
    {
        when(m_tickerPurchaseRepository.findById(1L))
            .thenReturn(Optional.of(m_purchase));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_purchase.setQuantity(new BigDecimal("-0.05"));

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updatePurchase(m_purchase); });

        verify(m_tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a purchase with quantity "
                 + "equal to zero")
    public void
    testUpdatePurchaseQuantityEqualToZero()
    {
        when(m_tickerPurchaseRepository.findById(1L))
            .thenReturn(Optional.of(m_purchase));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_purchase.setQuantity(BigDecimal.ZERO);

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updatePurchase(m_purchase); });

        verify(m_tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
    }

    @Test
    @DisplayName("Test if purchase unit price is updated successfully")
    public void testUpdatePurchaseUnitPrice()
    {
        when(m_tickerPurchaseRepository.findById(1L))
            .thenReturn(Optional.of(m_purchase));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        BigDecimal oldUnitPrice = m_purchase.getUnitPrice();

        BigDecimal newUnitPrice;

        if (oldUnitPrice.compareTo(BigDecimal.ONE) == 0)
        {
            newUnitPrice = BigDecimal.TEN;
        }
        else
        {
            newUnitPrice = BigDecimal.ONE;
        }

        BigDecimal expectedAmountAfterUpdate =
            m_purchase.getQuantity().multiply(newUnitPrice);

        m_purchase.setUnitPrice(newUnitPrice);

        m_tickerService.updatePurchase(m_purchase);

        verify(m_tickerPurchaseRepository).save(m_purchase);

        ArgumentCaptor<TickerPurchase> purchaseCaptor =
            ArgumentCaptor.forClass(TickerPurchase.class);
        verify(m_tickerPurchaseRepository).save(purchaseCaptor.capture());

        assertEquals(newUnitPrice, purchaseCaptor.getValue().getUnitPrice());
        assertEquals(expectedAmountAfterUpdate,
                     purchaseCaptor.getValue().getWalletTransaction().getAmount());
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a purchase with unit price "
                 + "less than zero")
    public void
    testUpdatePurchaseUnitPriceLessThanZero()
    {
        when(m_tickerPurchaseRepository.findById(1L))
            .thenReturn(Optional.of(m_purchase));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_purchase.setUnitPrice(new BigDecimal("-0.05"));

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updatePurchase(m_purchase); });

        verify(m_tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a purchase with unit price "
                 + "equal to zero")
    public void
    testUpdatePurchaseUnitPriceEqualToZero()
    {
        when(m_tickerPurchaseRepository.findById(1L))
            .thenReturn(Optional.of(m_purchase));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_purchase.setUnitPrice(BigDecimal.ZERO);

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.updatePurchase(m_purchase); });

        verify(m_tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
    }

    @Test
    @DisplayName("Test if crypto exchange sold ticker is updated successfully")
    public void testUpdateCryptoExchangeSoldTicker()
    {
        // Currently: crypto1 -> crypto2
        // After: crypto3 -> crypto2
        BigDecimal exchangeSoldQuantity = m_exchangeCrypto1ToCrypto2.getSoldQuantity();
        BigDecimal crypto1OldQuantity   = m_crypto1.getCurrentQuantity();
        BigDecimal crypto2OldQuantity   = m_crypto2.getCurrentQuantity();
        BigDecimal crypto3OldQuantity   = m_crypto3.getCurrentQuantity();

        CryptoExchange updatedCryptoExchange =
            new CryptoExchange(m_exchangeCrypto1ToCrypto2.getId(),
                               m_crypto3,
                               m_exchangeCrypto1ToCrypto2.getReceivedCrypto(),
                               m_exchangeCrypto1ToCrypto2.getSoldQuantity(),
                               m_exchangeCrypto1ToCrypto2.getReceivedQuantity(),
                               m_exchangeCrypto1ToCrypto2.getDate(),
                               m_exchangeCrypto1ToCrypto2.getDescription());

        when(m_cryptoExchangeRepository.findById(m_exchangeCrypto1ToCrypto2.getId()))
            .thenReturn(Optional.of(m_exchangeCrypto1ToCrypto2));

        when(m_tickerRepository.findById(m_crypto2.getId()))
            .thenReturn(Optional.of(m_crypto2));
        when(m_tickerRepository.findById(m_crypto3.getId()))
            .thenReturn(Optional.of(m_crypto3));

        m_tickerService.updateCryptoExchange(updatedCryptoExchange);

        verify(m_cryptoExchangeRepository).save(m_exchangeCrypto1ToCrypto2);

        assertEquals(0,
                     m_crypto1.getCurrentQuantity().compareTo(
                         crypto1OldQuantity.add(exchangeSoldQuantity)));
        assertEquals(0, m_crypto2.getCurrentQuantity().compareTo(crypto2OldQuantity));
        assertEquals(0,
                     m_crypto3.getCurrentQuantity().compareTo(
                         crypto3OldQuantity.subtract(exchangeSoldQuantity)));
    }

    @Test
    @DisplayName("Test if crypto exchange received ticker is updated successfully")
    public void testUpdateCryptoExchangeReceivedTicker()
    {
        // Currently: crypto1 -> crypto2
        // After: crypto1 -> crypto3
        BigDecimal exchangeQuantity = m_exchangeCrypto1ToCrypto2.getReceivedQuantity();
        BigDecimal crypto1OldQuantity = m_crypto1.getCurrentQuantity();
        BigDecimal crypto2OldQuantity = m_crypto2.getCurrentQuantity();
        BigDecimal crypto3OldQuantity = m_crypto3.getCurrentQuantity();

        CryptoExchange updatedCryptoExchange =
            new CryptoExchange(m_exchangeCrypto1ToCrypto2.getId(),
                               m_exchangeCrypto1ToCrypto2.getSoldCrypto(),
                               m_crypto3,
                               m_exchangeCrypto1ToCrypto2.getSoldQuantity(),
                               m_exchangeCrypto1ToCrypto2.getReceivedQuantity(),
                               m_exchangeCrypto1ToCrypto2.getDate(),
                               m_exchangeCrypto1ToCrypto2.getDescription());

        when(m_cryptoExchangeRepository.findById(m_exchangeCrypto1ToCrypto2.getId()))
            .thenReturn(Optional.of(m_exchangeCrypto1ToCrypto2));

        when(m_tickerRepository.findById(m_crypto1.getId()))
            .thenReturn(Optional.of(m_crypto1));
        when(m_tickerRepository.findById(m_crypto3.getId()))
            .thenReturn(Optional.of(m_crypto3));

        m_tickerService.updateCryptoExchange(updatedCryptoExchange);

        verify(m_cryptoExchangeRepository).save(m_exchangeCrypto1ToCrypto2);

        assertEquals(0, m_crypto1.getCurrentQuantity().compareTo(crypto1OldQuantity));
        assertEquals(0,
                     m_crypto2.getCurrentQuantity().compareTo(
                         crypto2OldQuantity.subtract(exchangeQuantity)));
        assertEquals(0,
                     m_crypto3.getCurrentQuantity().compareTo(
                         crypto3OldQuantity.add(exchangeQuantity)));
    }

    @Test
    @DisplayName("Test if all tickers are retrieved")
    public void testGetAllTickers()
    {
        when(m_tickerRepository.findAllByOrderBySymbolAsc())
            .thenReturn(Collections.singletonList(m_ticker1));

        List<Ticker> tickers = m_tickerService.getAllTickers();

        assertEquals(1, tickers.size());
        assertEquals(m_ticker1, tickers.get(0));
    }
}
