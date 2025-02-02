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
    public void SetUp()
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

        m_category = new Category("Dividend Payment");

        m_purchaseTransaction = new WalletTransaction(m_wallet,
                                                      m_category,
                                                      TransactionType.EXPENSE,
                                                      TransactionStatus.CONFIRMED,
                                                      LocalDateTime.now(),
                                                      new BigDecimal("50"),
                                                      "TickerPurchase");
        m_purchase            = new TickerPurchase(1L,
                                        m_ticker1,
                                        new BigDecimal("1"),
                                        new BigDecimal("50"),
                                        m_purchaseTransaction);

        m_saleTransaction = new WalletTransaction(m_wallet,
                                                  m_category,
                                                  TransactionType.INCOME,
                                                  TransactionStatus.CONFIRMED,
                                                  LocalDateTime.now(),
                                                  new BigDecimal("50"),
                                                  "TickerSale");

        m_sale = new TickerSale(1L,
                                m_ticker1,
                                new BigDecimal("1"),
                                new BigDecimal("50"),
                                m_saleTransaction,
                                new BigDecimal("50"));

        m_dividendTransaction = new WalletTransaction(m_wallet,
                                                      m_category,
                                                      TransactionType.INCOME,
                                                      TransactionStatus.CONFIRMED,
                                                      LocalDateTime.now(),
                                                      new BigDecimal("50"),
                                                      "Dividend Payment");

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
    public void TestRegisterTicker()
    {
        when(m_tickerRepository.existsBySymbol(m_ticker1.GetSymbol()))
            .thenReturn(false);

        m_tickerService.RegisterTicker(m_ticker1.GetName(),
                                       m_ticker1.GetSymbol(),
                                       m_ticker1.GetType(),
                                       m_ticker1.GetCurrentUnitValue(),
                                       m_ticker1.GetAveragePrice(),
                                       m_ticker1.GetCurrentQuantity());

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository).save(tickerCaptor.capture());

        Ticker savedTicker = tickerCaptor.getValue();
        assertEquals(m_ticker1.GetName(), savedTicker.GetName());
        assertEquals(m_ticker1.GetSymbol(), savedTicker.GetSymbol());
        assertEquals(m_ticker1.GetType(), savedTicker.GetType());
        assertEquals(0,
                     m_ticker1.GetCurrentUnitValue().compareTo(
                         savedTicker.GetCurrentUnitValue()));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when registering a ticker with an existing symbol")
    public void
    TestRegisterTickerAlreadyExists()
    {
        when(m_tickerRepository.existsBySymbol(m_ticker1.GetSymbol())).thenReturn(true);

        assertThrows(
            RuntimeException.class,
            ()
                -> m_tickerService.RegisterTicker(m_ticker1.GetName(),
                                                  m_ticker1.GetSymbol(),
                                                  m_ticker1.GetType(),
                                                  m_ticker1.GetCurrentUnitValue(),
                                                  m_ticker1.GetAveragePrice(),
                                                  m_ticker1.GetCurrentQuantity()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when registering a ticker with an empty name")
    public void
    TestRegisterTickerEmptyName()
    {
        // Test with empty name
        assertThrows(
            RuntimeException.class,
            ()
                -> m_tickerService.RegisterTicker("",
                                                  m_ticker1.GetSymbol(),
                                                  m_ticker1.GetType(),
                                                  m_ticker1.GetCurrentUnitValue(),
                                                  m_ticker1.GetAveragePrice(),
                                                  m_ticker1.GetCurrentQuantity()));
        // Test with blank name
        assertThrows(
            RuntimeException.class,
            ()
                -> m_tickerService.RegisterTicker(" ",
                                                  m_ticker1.GetSymbol(),
                                                  m_ticker1.GetType(),
                                                  m_ticker1.GetCurrentUnitValue(),
                                                  m_ticker1.GetAveragePrice(),
                                                  m_ticker1.GetCurrentQuantity()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when registering a ticker with an empty symbol")
    public void
    TestRegisterTickerEmptySymbol()
    {
        // Test with empty symbol
        assertThrows(
            RuntimeException.class,
            ()
                -> m_tickerService.RegisterTicker(m_ticker1.GetName(),
                                                  "",
                                                  m_ticker1.GetType(),
                                                  m_ticker1.GetCurrentUnitValue(),
                                                  m_ticker1.GetAveragePrice(),
                                                  m_ticker1.GetCurrentQuantity()));

        // Test with blank symbol
        assertThrows(
            RuntimeException.class,
            ()
                -> m_tickerService.RegisterTicker(m_ticker1.GetName(),
                                                  " ",
                                                  m_ticker1.GetType(),
                                                  m_ticker1.GetCurrentUnitValue(),
                                                  m_ticker1.GetAveragePrice(),
                                                  m_ticker1.GetCurrentQuantity()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when registering a ticker with price "
                 + "less than or equal to zero")
    public void
    TestRegisterTickerInvalidPrice()
    {
        when(m_tickerRepository.existsBySymbol(m_ticker1.GetSymbol()))
            .thenReturn(false);

        // Test with price less than zero
        assertThrows(
            RuntimeException.class,
            ()
                -> m_tickerService.RegisterTicker(m_ticker1.GetName(),
                                                  m_ticker1.GetSymbol(),
                                                  m_ticker1.GetType(),
                                                  new BigDecimal("-0.05"),
                                                  m_ticker1.GetAveragePrice(),
                                                  m_ticker1.GetCurrentQuantity()));

        // Test with price equal to zero
        assertThrows(
            RuntimeException.class,
            ()
                -> m_tickerService.RegisterTicker(m_ticker1.GetName(),
                                                  m_ticker1.GetSymbol(),
                                                  m_ticker1.GetType(),
                                                  BigDecimal.ZERO,
                                                  m_ticker1.GetAveragePrice(),
                                                  m_ticker1.GetCurrentQuantity()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when registering a ticker with average unit price "
        + "less than zero")
    public void
    TestRegisterTickerInvalidAverageUnitPrice()
    {
        when(m_tickerRepository.existsBySymbol(m_ticker1.GetSymbol()))
            .thenReturn(false);

        // Test with average unit price less than zero
        assertThrows(
            RuntimeException.class,
            ()
                -> m_tickerService.RegisterTicker(m_ticker1.GetName(),
                                                  m_ticker1.GetSymbol(),
                                                  m_ticker1.GetType(),
                                                  m_ticker1.GetCurrentUnitValue(),
                                                  new BigDecimal("-0.05"),
                                                  m_ticker1.GetCurrentQuantity()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if a ticker is deleted successfully")
    public void TestDeleteTicker()
    {
        when(m_tickerRepository.findById(m_ticker1.GetId()))
            .thenReturn(Optional.of(m_ticker1));

        when(m_tickerRepository.GetPurchaseCountByTicker(m_ticker1.GetId()))
            .thenReturn(0L);
        when(m_tickerRepository.GetSaleCountByTicker(m_ticker1.GetId())).thenReturn(0L);
        when(m_tickerRepository.GetDividendCountByTicker(m_ticker1.GetId()))
            .thenReturn(0L);
        when(m_tickerRepository.GetCryptoExchangeCountByTicker(m_ticker1.GetId()))
            .thenReturn(0L);

        m_tickerService.DeleteTicker(m_ticker1.GetId());

        verify(m_tickerRepository).delete(m_ticker1);
    }

    @Test
    @DisplayName("Test if exception is thrown when deleting a non-existent ticker")
    public void TestDeleteTickerNotFound()
    {
        when(m_tickerRepository.findById(m_ticker1.GetId()))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.DeleteTicker(m_ticker1.GetId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when deleting a ticker with purchases")
    public void TestDeleteTickerWithPurchases()
    {
        when(m_tickerRepository.findById(m_ticker1.GetId()))
            .thenReturn(Optional.of(m_ticker1));

        when(m_tickerRepository.GetPurchaseCountByTicker(m_ticker1.GetId()))
            .thenReturn(1L);
        when(m_tickerRepository.GetSaleCountByTicker(m_ticker1.GetId())).thenReturn(0L);
        when(m_tickerRepository.GetDividendCountByTicker(m_ticker1.GetId()))
            .thenReturn(0L);
        when(m_tickerRepository.GetCryptoExchangeCountByTicker(m_ticker1.GetId()))
            .thenReturn(0L);

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.DeleteTicker(m_ticker1.GetId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when deleting a ticker with sales")
    public void TestDeleteTickerWithSales()
    {
        when(m_tickerRepository.findById(m_ticker1.GetId()))
            .thenReturn(Optional.of(m_ticker1));

        when(m_tickerRepository.GetPurchaseCountByTicker(m_ticker1.GetId()))
            .thenReturn(0L);
        when(m_tickerRepository.GetSaleCountByTicker(m_ticker1.GetId())).thenReturn(1L);
        when(m_tickerRepository.GetDividendCountByTicker(m_ticker1.GetId()))
            .thenReturn(0L);
        when(m_tickerRepository.GetCryptoExchangeCountByTicker(m_ticker1.GetId()))
            .thenReturn(0L);

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.DeleteTicker(m_ticker1.GetId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when deleting a ticker with dividends")
    public void TestDeleteTickerWithDividends()
    {
        when(m_tickerRepository.findById(m_ticker1.GetId()))
            .thenReturn(Optional.of(m_ticker1));

        when(m_tickerRepository.GetPurchaseCountByTicker(m_ticker1.GetId()))
            .thenReturn(0L);
        when(m_tickerRepository.GetSaleCountByTicker(m_ticker1.GetId())).thenReturn(0L);
        when(m_tickerRepository.GetDividendCountByTicker(m_ticker1.GetId()))
            .thenReturn(1L);
        when(m_tickerRepository.GetCryptoExchangeCountByTicker(m_ticker1.GetId()))
            .thenReturn(0L);

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.DeleteTicker(m_ticker1.GetId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when deleting a ticker with crypto exchanges")
    public void
    TestDeleteTickerWithCryptoExchanges()
    {
        when(m_tickerRepository.findById(m_ticker1.GetId()))
            .thenReturn(Optional.of(m_ticker1));

        when(m_tickerRepository.GetPurchaseCountByTicker(m_ticker1.GetId()))
            .thenReturn(0L);
        when(m_tickerRepository.GetSaleCountByTicker(m_ticker1.GetId())).thenReturn(0L);
        when(m_tickerRepository.GetDividendCountByTicker(m_ticker1.GetId()))
            .thenReturn(0L);
        when(m_tickerRepository.GetCryptoExchangeCountByTicker(m_ticker1.GetId()))
            .thenReturn(1L);

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.DeleteTicker(m_ticker1.GetId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when deleting a ticker with transactions")
    public void TestDeleteTickerWithTransactions()
    {
        when(m_tickerRepository.findById(m_ticker1.GetId()))
            .thenReturn(Optional.of(m_ticker1));

        when(m_tickerRepository.GetPurchaseCountByTicker(m_ticker1.GetId()))
            .thenReturn(1L);
        when(m_tickerRepository.GetSaleCountByTicker(m_ticker1.GetId())).thenReturn(2L);
        when(m_tickerRepository.GetDividendCountByTicker(m_ticker1.GetId()))
            .thenReturn(3L);
        when(m_tickerRepository.GetCryptoExchangeCountByTicker(m_ticker1.GetId()))
            .thenReturn(4L);

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.DeleteTicker(m_ticker1.GetId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if a ticker is archived successfully")
    public void TestArchiveTicker()
    {
        when(m_tickerRepository.findById(m_ticker1.GetId()))
            .thenReturn(Optional.of(m_ticker1));

        m_tickerService.ArchiveTicker(m_ticker1.GetId());

        verify(m_tickerRepository).save(m_ticker1);
        assertTrue(m_ticker1.IsArchived());
    }

    @Test
    @DisplayName("Test if exception is thrown when archiving a non-existent ticker")
    public void TestArchiveTickerNotFound()
    {
        when(m_tickerRepository.findById(m_ticker1.GetId()))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.ArchiveTicker(m_ticker1.GetId()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if a ticker is unarchived successfully")
    public void TestUnarchiveTicker()
    {
        when(m_tickerRepository.findById(m_ticker1.GetId()))
            .thenReturn(Optional.of(m_ticker1));

        m_tickerService.UnarchiveTicker(m_ticker1.GetId());

        verify(m_tickerRepository).save(m_ticker1);
        assertFalse(m_ticker1.IsArchived());
    }

    @Test
    @DisplayName("Test if exception is thrown when unarchiving a non-existent ticker")
    public void TestUnarchiveTickerNotFound()
    {
        when(m_tickerRepository.findById(m_ticker1.GetId()))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.UnarchiveTicker(m_ticker1.GetId()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if a purchase is added successfully to a ticker")
    public void TestAddPurchase()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        when(m_walletTransactionService
                 .AddExpense(anyLong(), any(), any(), any(), any(), any()))
            .thenReturn(100L);

        when(m_walletTransactionService.GetTransactionById(100L))
            .thenReturn(new WalletTransaction(m_wallet,
                                              m_category,
                                              TransactionType.EXPENSE,
                                              TransactionStatus.CONFIRMED,
                                              LocalDateTime.now(),
                                              new BigDecimal("50"),
                                              "TickerPurchase"));

        m_tickerService.AddPurchase(1L,
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

        assertEquals(m_ticker1, purchaseCaptor.getValue().GetTicker());
        assertEquals(new BigDecimal("10"), purchaseCaptor.getValue().GetQuantity());
    }

    @Test
    @DisplayName(
        "Test if adding a purchase to a non-existent ticker throws an exception")
    public void
    TestAddPurchaseTickerNotFound()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddPurchase(1L,
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
    TestAddPurchaseInvalidQuantity()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddPurchase(1L,
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
    TestAddPurchaseInvalidUnitPrice()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddPurchase(1L,
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
    public void TestAddSale()
    {
        m_ticker1.SetCurrentQuantity(new BigDecimal("20"));

        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        when(m_walletTransactionService
                 .AddIncome(anyLong(), any(), any(), any(), any(), any()))
            .thenReturn(100L);

        when(m_walletTransactionService.GetTransactionById(100L))
            .thenReturn(new WalletTransaction(m_wallet,
                                              m_category,
                                              TransactionType.INCOME,
                                              TransactionStatus.CONFIRMED,
                                              LocalDateTime.now(),
                                              new BigDecimal("50"),
                                              "TickerSale"));

        m_tickerService.AddSale(1L,
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

        assertEquals(m_ticker1, saleCapptor.getValue().GetTicker());
        assertEquals(new BigDecimal("10"), saleCapptor.getValue().GetQuantity());
    }

    @Test
    @DisplayName("Test if adding a sale to a non-existent ticker throws an exception")
    public void TestAddSaleTickerNotFound()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddSale(1L,
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
    TestAddSaleExceedsQuantity()
    {
        m_ticker1.SetCurrentQuantity(new BigDecimal("5"));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddSale(1L,
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
    TestAddSaleInvalidUnitPrice()
    {
        m_ticker1.SetCurrentQuantity(new BigDecimal("10"));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddSale(1L,
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
    public void TestAddDividend()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        when(m_walletTransactionService
                 .AddIncome(anyLong(), any(), any(), any(), any(), any()))
            .thenReturn(100L);

        when(m_walletTransactionService.GetTransactionById(100L))
            .thenReturn(new WalletTransaction(m_wallet,
                                              m_category,
                                              TransactionType.INCOME,
                                              TransactionStatus.CONFIRMED,
                                              LocalDateTime.now(),
                                              new BigDecimal("50"),
                                              "Dividend Payment"));

        m_tickerService.AddDividend(1L,
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
    TestAddDividendTickerNotFound()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddDividend(1L,
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
            .AddIncome(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Test if adding a dividend with amount less than or equal to zero "
                 + "throws an exception")
    public void
    TestAddDividendInvalidAmount()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddDividend(1L,
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
            .AddIncome(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Test if a crypto exchange is added successfully to a ticker")
    public void TestAddCryptoExchange()
    {
        m_ticker1.SetType(TickerType.CRYPTOCURRENCY);
        m_ticker2.SetType(TickerType.CRYPTOCURRENCY);

        m_ticker1.SetCurrentQuantity(new BigDecimal("10"));
        m_ticker2.SetCurrentQuantity(new BigDecimal("0"));

        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));
        when(m_tickerRepository.findById(2L)).thenReturn(Optional.of(m_ticker2));

        when(m_cryptoExchangeRepository.save(any(CryptoExchange.class)))
            .thenReturn(m_exchange);

        BigDecimal sourcePreviousQuantity = m_ticker1.GetCurrentQuantity();
        BigDecimal targetPreviousQuantity = m_ticker2.GetCurrentQuantity();

        BigDecimal sourceQuantity = new BigDecimal("1");
        BigDecimal targetQuantity = new BigDecimal("1");

        m_tickerService.AddCryptoExchange(1L,
                                          2L,
                                          sourceQuantity,
                                          targetQuantity,
                                          LocalDateTime.now(),
                                          "");

        // Capture the exchange object that was saved and check its values
        ArgumentCaptor<CryptoExchange> exchangeCaptor =
            ArgumentCaptor.forClass(CryptoExchange.class);

        verify(m_cryptoExchangeRepository).save(exchangeCaptor.capture());

        assertEquals(m_ticker1, exchangeCaptor.getValue().GetSoldCrypto());

        assertEquals(m_ticker2, exchangeCaptor.getValue().GetReceivedCrypto());

        // Capture the source and target tickers after the exchange
        BigDecimal sourceCurrentQuantity = m_ticker1.GetCurrentQuantity();
        BigDecimal targetCurrentQuantity = m_ticker2.GetCurrentQuantity();

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
    TestAddCryptoExchangeSameTicker()
    {
        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddCryptoExchange(1L,
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
    TestAddCryptoExchangeSourceNotFound()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddCryptoExchange(1L,
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
    TestAddCryptoExchangeTargetNotFound()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));
        when(m_tickerRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddCryptoExchange(1L,
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
    TestAddCryptoExchangeInvalidTickerType()
    {
        m_ticker1.SetType(TickerType.STOCK);
        m_ticker2.SetType(TickerType.STOCK);

        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));
        when(m_tickerRepository.findById(2L)).thenReturn(Optional.of(m_ticker2));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddCryptoExchange(1L,
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
    TestAddCryptoExchangeInvalidSourceQuantity()
    {
        m_ticker1.SetType(TickerType.CRYPTOCURRENCY);
        m_ticker2.SetType(TickerType.CRYPTOCURRENCY);

        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));
        when(m_tickerRepository.findById(2L)).thenReturn(Optional.of(m_ticker2));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddCryptoExchange(1L,
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
    TestAddCryptoExchangeInvalidTargetQuantity()
    {
        m_ticker1.SetType(TickerType.CRYPTOCURRENCY);
        m_ticker2.SetType(TickerType.CRYPTOCURRENCY);

        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));
        when(m_tickerRepository.findById(2L)).thenReturn(Optional.of(m_ticker2));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddCryptoExchange(1L,
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
    TestAddCryptoExchangeExceedsSourceQuantity()
    {
        m_ticker1.SetType(TickerType.CRYPTOCURRENCY);
        m_ticker2.SetType(TickerType.CRYPTOCURRENCY);

        m_ticker1.SetCurrentQuantity(new BigDecimal("5"));
        m_ticker2.SetCurrentQuantity(new BigDecimal("0"));

        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));
        when(m_tickerRepository.findById(2L)).thenReturn(Optional.of(m_ticker2));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddCryptoExchange(1L,
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
    public void TestChangeTickerName()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        String oldName = m_ticker1.GetName();
        String newName = oldName + " Changed";
        m_ticker1.SetName(newName);

        m_tickerService.UpdateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

        assertEquals(newName, tickerCaptor.getValue().GetName());
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a non-existent ticker")
    public void TestChangeTickerNameNotFound()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when updating a ticker with an empty name")
    public void
    TestChangeTickerEmptyName()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.SetName("");

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a ticker with a blank name")
    public void TestChangeTickerBlankName()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.SetName(" ");

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a ticker with null name")
    public void TestChangeTickerNullName()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.SetName(null);

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if ticker symbol is changed successfully")
    public void TestChangeTickerSymbol()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        String oldSymbol = m_ticker1.GetSymbol();
        String newSymbol = oldSymbol + " Changed";
        m_ticker1.SetSymbol(newSymbol);

        m_tickerService.UpdateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

        assertEquals(newSymbol, tickerCaptor.getValue().GetSymbol());
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when updating a ticker with an empty symbol")
    public void
    TestChangeTickerEmptySymbol()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.SetSymbol("");

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when updating a ticker with a blank symbol")
    public void
    TestChangeTickerBlankSymbol()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.SetSymbol(" ");

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a ticker with null symbol")
    public void TestChangeTickerNullSymbol()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.SetSymbol(null);

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if ticker type is changed successfully")
    public void TestChangeTickerType()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        TickerType oldType = m_ticker1.GetType();

        TickerType newType;

        if (oldType == TickerType.STOCK)
        {
            newType = TickerType.FUND;
        }
        else
        {
            newType = TickerType.STOCK;
        }

        m_ticker1.SetType(newType);

        m_tickerService.UpdateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

        assertEquals(newType, tickerCaptor.getValue().GetType());
    }

    @Test
    @DisplayName("Test if ticker price is changed successfully")
    public void TestChangeTickerPrice()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        BigDecimal oldPrice = m_ticker1.GetCurrentUnitValue();

        BigDecimal newPrice;

        if (oldPrice.compareTo(BigDecimal.ONE) == 0)
        {
            newPrice = BigDecimal.TEN;
        }
        else
        {
            newPrice = BigDecimal.ONE;
        }

        m_ticker1.SetCurrentUnitValue(newPrice);

        m_tickerService.UpdateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

        assertEquals(0,
                     newPrice.compareTo(tickerCaptor.getValue().GetCurrentUnitValue()));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a ticker with price "
                 + "less than zero")
    public void
    TestChangeTickerPriceLessThanZero()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.SetCurrentUnitValue(new BigDecimal("-0.05"));

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a ticker with price "
                 + "equal to zero")
    public void
    TestChangeTickerPriceEqualToZero()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.SetCurrentUnitValue(BigDecimal.ZERO);

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if ticker current quantity is changed successfully")
    public void TestChangeTickerCurrentQuantity()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        BigDecimal oldQuantity = m_ticker1.GetCurrentQuantity();

        BigDecimal newQuantity;

        if (oldQuantity.compareTo(BigDecimal.ONE) == 0)
        {
            newQuantity = BigDecimal.TEN;
        }
        else
        {
            newQuantity = BigDecimal.ONE;
        }

        m_ticker1.SetCurrentQuantity(newQuantity);

        m_tickerService.UpdateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository).save(tickerCaptor.capture());

        assertEquals(newQuantity, tickerCaptor.getValue().GetCurrentQuantity());
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when updating a ticker with current quantity "
        + "less than zero")
    public void
    TestChangeTickerCurrentQuantityLessThanZero()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.SetCurrentQuantity(new BigDecimal("-0.05"));

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if ticker average price is changed successfully")
    public void TestChangeTickerAveragePrice()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        BigDecimal oldPrice = m_ticker1.GetAveragePrice();

        // Quantitycannot be zero, because in this case the average price is reset at
        // the end of the function
        m_ticker1.SetCurrentQuantity(new BigDecimal("1"));

        BigDecimal newPrice;

        if (oldPrice.compareTo(BigDecimal.ONE) == 0)
        {
            newPrice = BigDecimal.TEN;
        }
        else
        {
            newPrice = BigDecimal.ONE;
        }

        m_ticker1.SetAveragePrice(newPrice);

        m_tickerService.UpdateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

        assertEquals(0, newPrice.compareTo(tickerCaptor.getValue().GetAveragePrice()));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when updating a ticker with average price "
        + "less than zero")
    public void
    TestChangeTickerAveragePriceLessThanZero()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.SetAveragePrice(new BigDecimal("-0.05"));

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdateTicker(m_ticker1); });

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if ticker is set to archived successfully")
    public void TestChangeTickerArchived()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.SetArchived(true);

        m_tickerService.UpdateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

        assertTrue(tickerCaptor.getValue().IsArchived());
    }

    @Test
    @DisplayName("Test if ticker is set to unarchived successfully")
    public void TestChangeTickerUnarchived()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_ticker1.SetArchived(false);

        m_tickerService.UpdateTicker(m_ticker1);

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

        assertFalse(tickerCaptor.getValue().IsArchived());
    }

    @Test
    @DisplayName("Test if purchase quantity is updated successfully")
    public void TestUpdatePurchaseQuantity()
    {
        when(m_tickerPurchaseRepository.findById(1L))
            .thenReturn(Optional.of(m_purchase));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        BigDecimal oldQuantity = m_purchase.GetQuantity();

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
            newQuantity.multiply(m_purchase.GetUnitPrice());

        m_purchase.SetQuantity(newQuantity);

        m_tickerService.UpdatePurchase(m_purchase);

        ArgumentCaptor<TickerPurchase> purchaseCaptor =
            ArgumentCaptor.forClass(TickerPurchase.class);
        verify(m_tickerPurchaseRepository).save(purchaseCaptor.capture());

        assertEquals(newQuantity, purchaseCaptor.getValue().GetQuantity());
        assertEquals(expectedAmountAfterUpdate,
                     purchaseCaptor.getValue().GetWalletTransaction().GetAmount());
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a non-existent purchase")
    public void TestUpdatePurchaseNotFound()
    {
        when(m_tickerPurchaseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdatePurchase(m_purchase); });

        verify(m_tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a purchase with quantity "
                 + "less than zero")
    public void
    TestUpdatePurchaseQuantityLessThanZero()
    {
        when(m_tickerPurchaseRepository.findById(1L))
            .thenReturn(Optional.of(m_purchase));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_purchase.SetQuantity(new BigDecimal("-0.05"));

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdatePurchase(m_purchase); });

        verify(m_tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a purchase with quantity "
                 + "equal to zero")
    public void
    TestUpdatePurchaseQuantityEqualToZero()
    {
        when(m_tickerPurchaseRepository.findById(1L))
            .thenReturn(Optional.of(m_purchase));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_purchase.SetQuantity(BigDecimal.ZERO);

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdatePurchase(m_purchase); });

        verify(m_tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
    }

    @Test
    @DisplayName("Test if purchase unit price is updated successfully")
    public void TestUpdatePurchaseUnitPrice()
    {
        when(m_tickerPurchaseRepository.findById(1L))
            .thenReturn(Optional.of(m_purchase));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        BigDecimal oldUnitPrice = m_purchase.GetUnitPrice();

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
            m_purchase.GetQuantity().multiply(newUnitPrice);

        m_purchase.SetUnitPrice(newUnitPrice);

        m_tickerService.UpdatePurchase(m_purchase);

        verify(m_tickerPurchaseRepository).save(m_purchase);

        ArgumentCaptor<TickerPurchase> purchaseCaptor =
            ArgumentCaptor.forClass(TickerPurchase.class);
        verify(m_tickerPurchaseRepository).save(purchaseCaptor.capture());

        assertEquals(newUnitPrice, purchaseCaptor.getValue().GetUnitPrice());
        assertEquals(expectedAmountAfterUpdate,
                     purchaseCaptor.getValue().GetWalletTransaction().GetAmount());
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a purchase with unit price "
                 + "less than zero")
    public void
    TestUpdatePurchaseUnitPriceLessThanZero()
    {
        when(m_tickerPurchaseRepository.findById(1L))
            .thenReturn(Optional.of(m_purchase));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_purchase.SetUnitPrice(new BigDecimal("-0.05"));

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdatePurchase(m_purchase); });

        verify(m_tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating a purchase with unit price "
                 + "equal to zero")
    public void
    TestUpdatePurchaseUnitPriceEqualToZero()
    {
        when(m_tickerPurchaseRepository.findById(1L))
            .thenReturn(Optional.of(m_purchase));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker1));

        m_purchase.SetUnitPrice(BigDecimal.ZERO);

        assertThrows(RuntimeException.class,
                     () -> { m_tickerService.UpdatePurchase(m_purchase); });

        verify(m_tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
    }

    @Test
    @DisplayName("Test if crypto exchange sold ticker is updated successfully")
    public void TestUpdateCryptoExchangeSoldTicker()
    {
        // Currently: crypto1 -> crypto2
        // After: crypto3 -> crypto2
        BigDecimal exchangeSoldQuantity = m_exchangeCrypto1ToCrypto2.GetSoldQuantity();
        BigDecimal crypto1OldQuantity   = m_crypto1.GetCurrentQuantity();
        BigDecimal crypto2OldQuantity   = m_crypto2.GetCurrentQuantity();
        BigDecimal crypto3OldQuantity   = m_crypto3.GetCurrentQuantity();

        CryptoExchange updatedCryptoExchange =
            new CryptoExchange(m_exchangeCrypto1ToCrypto2.GetId(),
                               m_crypto3,
                               m_exchangeCrypto1ToCrypto2.GetReceivedCrypto(),
                               m_exchangeCrypto1ToCrypto2.GetSoldQuantity(),
                               m_exchangeCrypto1ToCrypto2.GetReceivedQuantity(),
                               m_exchangeCrypto1ToCrypto2.GetDate(),
                               m_exchangeCrypto1ToCrypto2.GetDescription());

        when(m_cryptoExchangeRepository.findById(m_exchangeCrypto1ToCrypto2.GetId()))
            .thenReturn(Optional.of(m_exchangeCrypto1ToCrypto2));

        when(m_tickerRepository.findById(m_crypto2.GetId()))
            .thenReturn(Optional.of(m_crypto2));
        when(m_tickerRepository.findById(m_crypto3.GetId()))
            .thenReturn(Optional.of(m_crypto3));

        m_tickerService.UpdateCryptoExchange(updatedCryptoExchange);

        verify(m_cryptoExchangeRepository).save(m_exchangeCrypto1ToCrypto2);

        assertEquals(0,
                     m_crypto1.GetCurrentQuantity().compareTo(
                         crypto1OldQuantity.add(exchangeSoldQuantity)));
        assertEquals(0, m_crypto2.GetCurrentQuantity().compareTo(crypto2OldQuantity));
        assertEquals(0,
                     m_crypto3.GetCurrentQuantity().compareTo(
                         crypto3OldQuantity.subtract(exchangeSoldQuantity)));
    }

    @Test
    @DisplayName("Test if crypto exchange received ticker is updated successfully")
    public void TestUpdateCryptoExchangeReceivedTicker()
    {
        // Currently: crypto1 -> crypto2
        // After: crypto1 -> crypto3
        BigDecimal exchangeQuantity = m_exchangeCrypto1ToCrypto2.GetReceivedQuantity();
        BigDecimal crypto1OldQuantity = m_crypto1.GetCurrentQuantity();
        BigDecimal crypto2OldQuantity = m_crypto2.GetCurrentQuantity();
        BigDecimal crypto3OldQuantity = m_crypto3.GetCurrentQuantity();

        CryptoExchange updatedCryptoExchange =
            new CryptoExchange(m_exchangeCrypto1ToCrypto2.GetId(),
                               m_exchangeCrypto1ToCrypto2.GetSoldCrypto(),
                               m_crypto3,
                               m_exchangeCrypto1ToCrypto2.GetSoldQuantity(),
                               m_exchangeCrypto1ToCrypto2.GetReceivedQuantity(),
                               m_exchangeCrypto1ToCrypto2.GetDate(),
                               m_exchangeCrypto1ToCrypto2.GetDescription());

        when(m_cryptoExchangeRepository.findById(m_exchangeCrypto1ToCrypto2.GetId()))
            .thenReturn(Optional.of(m_exchangeCrypto1ToCrypto2));

        when(m_tickerRepository.findById(m_crypto1.GetId()))
            .thenReturn(Optional.of(m_crypto1));
        when(m_tickerRepository.findById(m_crypto3.GetId()))
            .thenReturn(Optional.of(m_crypto3));

        m_tickerService.UpdateCryptoExchange(updatedCryptoExchange);

        verify(m_cryptoExchangeRepository).save(m_exchangeCrypto1ToCrypto2);

        assertEquals(0, m_crypto1.GetCurrentQuantity().compareTo(crypto1OldQuantity));
        assertEquals(0,
                     m_crypto2.GetCurrentQuantity().compareTo(
                         crypto2OldQuantity.subtract(exchangeQuantity)));
        assertEquals(0,
                     m_crypto3.GetCurrentQuantity().compareTo(
                         crypto3OldQuantity.add(exchangeQuantity)));
    }

    @Test
    @DisplayName("Test if all tickers are retrieved")
    public void TestGetAllTickers()
    {
        when(m_tickerRepository.findAllByOrderBySymbolAsc())
            .thenReturn(Collections.singletonList(m_ticker1));

        List<Ticker> tickers = m_tickerService.GetAllTickers();

        assertEquals(1, tickers.size());
        assertEquals(m_ticker1, tickers.get(0));
    }
}
