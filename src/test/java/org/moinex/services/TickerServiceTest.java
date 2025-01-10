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
import org.moinex.entities.investment.Dividend;
import org.moinex.entities.investment.Purchase;
import org.moinex.entities.investment.Sale;
import org.moinex.entities.investment.Ticker;
import org.moinex.repositories.DividendRepository;
import org.moinex.repositories.PurchaseRepository;
import org.moinex.repositories.SaleRepository;
import org.moinex.repositories.TickerRepository;
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
    private PurchaseRepository m_purchaseRepository;

    @Mock
    private SaleRepository m_saleRepository;

    @Mock
    private DividendRepository m_dividendRepository;

    @Mock
    private WalletRepository m_walletRepository;

    @Mock
    private WalletTransactionService m_walletTransactionService;

    @InjectMocks
    private TickerService m_tickerService;

    private Ticker   m_ticker;
    private Wallet   m_wallet;
    private Category m_category;

    @BeforeEach
    public void SetUp()
    {
        m_ticker = new Ticker(1L,
                              "Ticker1",
                              "T1",
                              TickerType.STOCK,
                              BigDecimal.ZERO,
                              new BigDecimal("100"),
                              new BigDecimal("100"),
                              LocalDateTime.now());

        m_wallet = new Wallet(1L, "Main Wallet", BigDecimal.ZERO);

        m_category = new Category("Dividend Payment");
    }

    @Test
    @DisplayName("Test if a ticker is registered successfully")
    public void TestRegisterTicker()
    {
        when(m_tickerRepository.existsBySymbol(m_ticker.GetSymbol())).thenReturn(false);

        m_tickerService.RegisterTicker(m_ticker.GetName(),
                                       m_ticker.GetSymbol(),
                                       m_ticker.GetType(),
                                       m_ticker.GetCurrentUnitValue(),
                                       m_ticker.GetAveragePrice(),
                                       m_ticker.GetCurrentQuantity());

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository).save(tickerCaptor.capture());

        Ticker savedTicker = tickerCaptor.getValue();
        assertEquals(m_ticker.GetName(), savedTicker.GetName());
        assertEquals(m_ticker.GetSymbol(), savedTicker.GetSymbol());
        assertEquals(m_ticker.GetType(), savedTicker.GetType());
        assertEquals(m_ticker.GetCurrentUnitValue(), savedTicker.GetCurrentUnitValue());
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when registering a ticker with an existing symbol")
    public void
    TestRegisterTickerAlreadyExists()
    {
        when(m_tickerRepository.existsBySymbol(m_ticker.GetSymbol())).thenReturn(true);

        assertThrows(
            RuntimeException.class,
            ()
                -> m_tickerService.RegisterTicker(m_ticker.GetName(),
                                                  m_ticker.GetSymbol(),
                                                  m_ticker.GetType(),
                                                  m_ticker.GetCurrentUnitValue(),
                                                  m_ticker.GetAveragePrice(),
                                                  m_ticker.GetCurrentQuantity()));

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
                                                  m_ticker.GetSymbol(),
                                                  m_ticker.GetType(),
                                                  m_ticker.GetCurrentUnitValue(),
                                                  m_ticker.GetAveragePrice(),
                                                  m_ticker.GetCurrentQuantity()));
        // Test with blank name
        assertThrows(
            RuntimeException.class,
            ()
                -> m_tickerService.RegisterTicker(" ",
                                                  m_ticker.GetSymbol(),
                                                  m_ticker.GetType(),
                                                  m_ticker.GetCurrentUnitValue(),
                                                  m_ticker.GetAveragePrice(),
                                                  m_ticker.GetCurrentQuantity()));

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
                -> m_tickerService.RegisterTicker(m_ticker.GetName(),
                                                  "",
                                                  m_ticker.GetType(),
                                                  m_ticker.GetCurrentUnitValue(),
                                                  m_ticker.GetAveragePrice(),
                                                  m_ticker.GetCurrentQuantity()));

        // Test with blank symbol
        assertThrows(
            RuntimeException.class,
            ()
                -> m_tickerService.RegisterTicker(m_ticker.GetName(),
                                                  " ",
                                                  m_ticker.GetType(),
                                                  m_ticker.GetCurrentUnitValue(),
                                                  m_ticker.GetAveragePrice(),
                                                  m_ticker.GetCurrentQuantity()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when registering a ticker with price "
                 + "less than or equal to zero")
    public void
    TestRegisterTickerInvalidPrice()
    {
        when(m_tickerRepository.existsBySymbol(m_ticker.GetSymbol())).thenReturn(false);

        // Test with price less than zero
        assertThrows(
            RuntimeException.class,
            ()
                -> m_tickerService.RegisterTicker(m_ticker.GetName(),
                                                  m_ticker.GetSymbol(),
                                                  m_ticker.GetType(),
                                                  new BigDecimal("-0.05"),
                                                  m_ticker.GetAveragePrice(),
                                                  m_ticker.GetCurrentQuantity()));

        // Test with price equal to zero
        assertThrows(
            RuntimeException.class,
            ()
                -> m_tickerService.RegisterTicker(m_ticker.GetName(),
                                                  m_ticker.GetSymbol(),
                                                  m_ticker.GetType(),
                                                  BigDecimal.ZERO,
                                                  m_ticker.GetAveragePrice(),
                                                  m_ticker.GetCurrentQuantity()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when registering a ticker with average unit price "
        + "less than zero")
    public void
    TestRegisterTickerInvalidAverageUnitPrice()
    {
        when(m_tickerRepository.existsBySymbol(m_ticker.GetSymbol())).thenReturn(false);

        // Test with average unit price less than zero
        assertThrows(
            RuntimeException.class,
            ()
                -> m_tickerService.RegisterTicker(m_ticker.GetName(),
                                                  m_ticker.GetSymbol(),
                                                  m_ticker.GetType(),
                                                  m_ticker.GetCurrentUnitValue(),
                                                  new BigDecimal("-0.05"),
                                                  m_ticker.GetCurrentQuantity()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if a ticker is deleted successfully")
    public void TestDeleteTicker()
    {
        when(m_tickerRepository.findById(m_ticker.GetId()))
            .thenReturn(Optional.of(m_ticker));

        when(m_tickerRepository.GetPurchaseCountByTicker(m_ticker.GetId()))
            .thenReturn(0L);
        when(m_tickerRepository.GetSaleCountByTicker(m_ticker.GetId())).thenReturn(0L);
        when(m_tickerRepository.GetDividendCountByTicker(m_ticker.GetId()))
            .thenReturn(0L);

        m_tickerService.DeleteTicker(m_ticker.GetId());

        verify(m_tickerRepository).delete(m_ticker);
    }

    @Test
    @DisplayName("Test if exception is thrown when deleting a non-existent ticker")
    public void TestDeleteTickerNotFound()
    {
        when(m_tickerRepository.findById(m_ticker.GetId()))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.DeleteTicker(m_ticker.GetId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when deleting a ticker with purchases")
    public void TestDeleteTickerWithPurchases()
    {
        when(m_tickerRepository.findById(m_ticker.GetId()))
            .thenReturn(Optional.of(m_ticker));

        when(m_tickerRepository.GetPurchaseCountByTicker(m_ticker.GetId()))
            .thenReturn(1L);
        when(m_tickerRepository.GetSaleCountByTicker(m_ticker.GetId())).thenReturn(0L);
        when(m_tickerRepository.GetDividendCountByTicker(m_ticker.GetId()))
            .thenReturn(0L);

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.DeleteTicker(m_ticker.GetId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when deleting a ticker with sales")
    public void TestDeleteTickerWithSales()
    {
        when(m_tickerRepository.findById(m_ticker.GetId()))
            .thenReturn(Optional.of(m_ticker));

        when(m_tickerRepository.GetPurchaseCountByTicker(m_ticker.GetId()))
            .thenReturn(0L);
        when(m_tickerRepository.GetSaleCountByTicker(m_ticker.GetId())).thenReturn(1L);
        when(m_tickerRepository.GetDividendCountByTicker(m_ticker.GetId()))
            .thenReturn(0L);

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.DeleteTicker(m_ticker.GetId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when deleting a ticker with dividends")
    public void TestDeleteTickerWithDividends()
    {
        when(m_tickerRepository.findById(m_ticker.GetId()))
            .thenReturn(Optional.of(m_ticker));

        when(m_tickerRepository.GetPurchaseCountByTicker(m_ticker.GetId()))
            .thenReturn(0L);
        when(m_tickerRepository.GetSaleCountByTicker(m_ticker.GetId())).thenReturn(0L);
        when(m_tickerRepository.GetDividendCountByTicker(m_ticker.GetId()))
            .thenReturn(1L);

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.DeleteTicker(m_ticker.GetId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when deleting a ticker with transactions")
    public void TestDeleteTickerWithTransactions()
    {
        when(m_tickerRepository.findById(m_ticker.GetId()))
            .thenReturn(Optional.of(m_ticker));

        when(m_tickerRepository.GetPurchaseCountByTicker(m_ticker.GetId()))
            .thenReturn(1L);
        when(m_tickerRepository.GetSaleCountByTicker(m_ticker.GetId())).thenReturn(2L);
        when(m_tickerRepository.GetDividendCountByTicker(m_ticker.GetId()))
            .thenReturn(3L);

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.DeleteTicker(m_ticker.GetId()));

        verify(m_tickerRepository, never()).delete(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if a ticker is archived successfully")
    public void TestArchiveTicker()
    {
        when(m_tickerRepository.findById(m_ticker.GetId()))
            .thenReturn(Optional.of(m_ticker));

        m_tickerService.ArchiveTicker(m_ticker.GetId());

        verify(m_tickerRepository).save(m_ticker);
        assertTrue(m_ticker.IsArchived());
    }

    @Test
    @DisplayName("Test if exception is thrown when archiving a non-existent ticker")
    public void TestArchiveTickerNotFound()
    {
        when(m_tickerRepository.findById(m_ticker.GetId()))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.ArchiveTicker(m_ticker.GetId()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if a ticker is unarchived successfully")
    public void TestUnarchiveTicker()
    {
        when(m_tickerRepository.findById(m_ticker.GetId()))
            .thenReturn(Optional.of(m_ticker));

        m_tickerService.UnarchiveTicker(m_ticker.GetId());

        verify(m_tickerRepository).save(m_ticker);
        assertFalse(m_ticker.IsArchived());
    }

    @Test
    @DisplayName("Test if exception is thrown when unarchiving a non-existent ticker")
    public void TestUnarchiveTickerNotFound()
    {
        when(m_tickerRepository.findById(m_ticker.GetId()))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> m_tickerService.UnarchiveTicker(m_ticker.GetId()));

        verify(m_tickerRepository, never()).save(any(Ticker.class));
    }

    @Test
    @DisplayName("Test if a purchase is added successfully to a ticker")
    public void TestAddPurchase()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker));

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
                                              "Purchase"));

        m_tickerService.AddPurchase(1L,
                                    1L,
                                    new BigDecimal("10"),
                                    new BigDecimal("150"),
                                    m_category,
                                    LocalDateTime.now(),
                                    "Purchase",
                                    TransactionStatus.CONFIRMED);

        // Capture the purchase object that was saved and check its values
        ArgumentCaptor<Purchase> purchaseCaptor =
            ArgumentCaptor.forClass(Purchase.class);

        verify(m_purchaseRepository).save(purchaseCaptor.capture());

        assertEquals(m_ticker, purchaseCaptor.getValue().GetTicker());
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
                                        "Purchase",
                                        TransactionStatus.CONFIRMED);
        });

        // Verify that the purchase was not saved
        verify(m_purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    @DisplayName("Test if adding a purchase with quantity less than or equal to zero "
                 + "throws an exception")
    public void
    TestAddPurchaseInvalidQuantity()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddPurchase(1L,
                                        1L,
                                        BigDecimal.ZERO,
                                        new BigDecimal("150"),
                                        m_category,
                                        LocalDateTime.now(),
                                        "Purchase",
                                        TransactionStatus.CONFIRMED);
        });

        // Verify that the purchase was not saved
        verify(m_purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    @DisplayName("Test if adding a purchase with unit price less than or equal to "
                 + "zero throws an exception")
    public void
    TestAddPurchaseInvalidUnitPrice()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddPurchase(1L,
                                        1L,
                                        new BigDecimal("10"),
                                        BigDecimal.ZERO,
                                        m_category,
                                        LocalDateTime.now(),
                                        "Purchase",
                                        TransactionStatus.CONFIRMED);
        });

        // Verify that the purchase was not saved
        verify(m_purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    @DisplayName("Test if a sale is added successfully to a ticker")
    public void TestAddSale()
    {
        m_ticker.SetCurrentQuantity(new BigDecimal("20"));

        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker));

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
                                              "Sale"));

        m_tickerService.AddSale(1L,
                                1L,
                                new BigDecimal("10"),
                                new BigDecimal("200"),
                                m_category,
                                LocalDateTime.now(),
                                "Sale",
                                TransactionStatus.CONFIRMED);

        // Capture the sale object that was saved and check its values
        ArgumentCaptor<Sale> saleCapptor = ArgumentCaptor.forClass(Sale.class);

        verify(m_saleRepository).save(saleCapptor.capture());

        assertEquals(m_ticker, saleCapptor.getValue().GetTicker());
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
                                    "Sale",
                                    TransactionStatus.CONFIRMED);
        });

        // Verify that the sale was not saved
        verify(m_saleRepository, never()).save(any(Sale.class));
    }

    @Test
    @DisplayName("Test if adding a sale with quantity greater than current quantity "
                 + "throws an exception")
    public void
    TestAddSaleExceedsQuantity()
    {
        m_ticker.SetCurrentQuantity(new BigDecimal("5"));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddSale(1L,
                                    1L,
                                    new BigDecimal("10"),
                                    new BigDecimal("200"),
                                    m_category,
                                    LocalDateTime.now(),
                                    "Sale",
                                    TransactionStatus.CONFIRMED);
        });

        // Verify that the sale was not saved
        verify(m_saleRepository, never()).save(any(Sale.class));
    }

    @Test
    @DisplayName("Test if adding a sale with unit price less than or equal to zero "
                 + "throws an exception")
    public void
    TestAddSaleInvalidUnitPrice()
    {
        m_ticker.SetCurrentQuantity(new BigDecimal("10"));
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker));

        assertThrows(RuntimeException.class, () -> {
            m_tickerService.AddSale(1L,
                                    1L,
                                    new BigDecimal("5"),
                                    BigDecimal.ZERO,
                                    m_category,
                                    LocalDateTime.now(),
                                    "Sale",
                                    TransactionStatus.CONFIRMED);
        });

        // Verify that the sale was not saved
        verify(m_saleRepository, never()).save(any(Sale.class));
    }

    @Test
    @DisplayName("Test if a dividend is added successfully to a ticker")
    public void TestAddDividend()
    {
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker));

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
        when(m_tickerRepository.findById(1L)).thenReturn(Optional.of(m_ticker));

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
    @DisplayName("Test if all tickers are retrieved")
    public void TestGetAllTickers()
    {
        when(m_tickerRepository.findAllByOrderBySymbolAsc())
            .thenReturn(Collections.singletonList(m_ticker));

        List<Ticker> tickers = m_tickerService.GetAllTickers();

        assertEquals(1, tickers.size());
        assertEquals(m_ticker, tickers.get(0));
    }
}
