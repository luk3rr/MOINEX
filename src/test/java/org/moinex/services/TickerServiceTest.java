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
import org.moinex.entities.investment.Ticker;
import org.moinex.repositories.TickerRepository;
import org.moinex.util.TickerType;

@ExtendWith(MockitoExtension.class)
public class TickerServiceTest
{
    @Mock
    private TickerRepository m_tickerRepository;

    @InjectMocks
    private TickerService m_tickerService;

    private Ticker m_ticker1;

    @BeforeEach
    public void SetUp()
    {
        m_ticker1 = new Ticker(1L,
                               "Ticker1",
                               "T1",
                               TickerType.STOCK,
                               BigDecimal.ZERO,
                               new BigDecimal("100"),
                               LocalDateTime.now());
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
                                       m_ticker1.GetCurrentUnitValue());

        ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
        verify(m_tickerRepository).save(tickerCaptor.capture());

        Ticker savedTicker = tickerCaptor.getValue();
        assertEquals(m_ticker1.GetName(), savedTicker.GetName());
        assertEquals(m_ticker1.GetSymbol(), savedTicker.GetSymbol());
        assertEquals(m_ticker1.GetType(), savedTicker.GetType());
        assertEquals(m_ticker1.GetCurrentUnitValue(),
                     savedTicker.GetCurrentUnitValue());
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
                                                  m_ticker1.GetCurrentUnitValue()));

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
