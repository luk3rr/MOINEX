/*
 * Filename: TickerServiceTest.java
 * Created on: January  7, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.error.MoinexException;
import org.moinex.model.Category;
import org.moinex.model.investment.*;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.repository.investment.*;
import org.moinex.util.APIUtils;
import org.moinex.util.enums.TickerType;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;

@ExtendWith(MockitoExtension.class)
class TickerServiceTest {
    @Mock private TickerRepository tickerRepository;

    @Mock private TickerPurchaseRepository tickerPurchaseRepository;

    @Mock private TickerSaleRepository tickerSaleRepository;

    @Mock private DividendRepository dividendRepository;

    @Mock private CryptoExchangeRepository cryptoExchangeRepository;

    @Mock private WalletTransactionService walletTransactionService;

    @InjectMocks private TickerService tickerService;

    private Ticker ticker1;
    private Ticker ticker2;
    private Ticker crypto1;
    private Ticker crypto2;
    private Ticker crypto3;
    private Wallet wallet;
    private TickerPurchase tickerPurchase;
    private CryptoExchange cryptoExchange;
    private CryptoExchange exchangeCrypto1ToCrypto2;
    private Category category;

    @BeforeEach
    void beforeEach() {
        ticker1 =
                new Ticker(
                        1,
                        "Ticker1",
                        "T1",
                        TickerType.STOCK,
                        BigDecimal.ZERO,
                        new BigDecimal("100"),
                        new BigDecimal("100"),
                        LocalDateTime.now());

        ticker2 =
                new Ticker(
                        2,
                        "Ticker2",
                        "T2",
                        TickerType.STOCK,
                        BigDecimal.ZERO,
                        new BigDecimal("100"),
                        new BigDecimal("100"),
                        LocalDateTime.now());

        crypto1 =
                new Ticker(
                        3,
                        "crypto1",
                        "T3",
                        TickerType.CRYPTOCURRENCY,
                        BigDecimal.ZERO,
                        new BigDecimal("10"),
                        new BigDecimal("10"),
                        LocalDateTime.now());

        crypto2 =
                new Ticker(
                        4,
                        "crypto2",
                        "T4",
                        TickerType.CRYPTOCURRENCY,
                        BigDecimal.ZERO,
                        new BigDecimal("10"),
                        new BigDecimal("10"),
                        LocalDateTime.now());

        crypto3 =
                new Ticker(
                        5,
                        "crypto3",
                        "T5",
                        TickerType.CRYPTOCURRENCY,
                        BigDecimal.ZERO,
                        new BigDecimal("10"),
                        new BigDecimal("10"),
                        LocalDateTime.now());

        wallet = new Wallet(1, "Main Wallet", BigDecimal.ZERO);

        category = Category.builder().name("Category").build();

        WalletTransaction purchaseTransaction =
                WalletTransaction.builder()
                        .wallet(wallet)
                        .category(category)
                        .type(TransactionType.EXPENSE)
                        .status(TransactionStatus.CONFIRMED)
                        .date(LocalDateTime.now())
                        .amount(new BigDecimal("50"))
                        .description("TickerPurchase")
                        .build();

        tickerPurchase =
                new TickerPurchase(
                        1, ticker1, new BigDecimal("1"), new BigDecimal("50"), purchaseTransaction);

        cryptoExchange =
                new CryptoExchange(
                        1,
                        ticker1,
                        ticker1,
                        new BigDecimal("1"),
                        new BigDecimal("1"),
                        LocalDateTime.now(),
                        "");

        exchangeCrypto1ToCrypto2 =
                new CryptoExchange(
                        2,
                        crypto1,
                        crypto2,
                        new BigDecimal("1"),
                        new BigDecimal("1"),
                        LocalDateTime.now(),
                        "");
    }

    @Test
    @DisplayName("Test if all tickers are retrieved")
    void testGetAllTickers() {
        when(tickerRepository.findAllByOrderBySymbolAsc())
                .thenReturn(Collections.singletonList(ticker1));

        List<Ticker> tickers = tickerService.getAllTickers();

        assertEquals(1, tickers.size());
        assertEquals(ticker1, tickers.getFirst());
    }

    @Nested
    @DisplayName("Add ticker tests")
    class AddTickerTests {

        @Test
        @DisplayName("Test if a ticker is registered successfully")
        void testRegisterTicker() {
            when(tickerRepository.existsBySymbol(ticker1.getSymbol())).thenReturn(false);

            tickerService.addTicker(
                    ticker1.getName(),
                    ticker1.getSymbol(),
                    ticker1.getType(),
                    ticker1.getCurrentUnitValue(),
                    ticker1.getAverageUnitValue(),
                    ticker1.getCurrentQuantity());

            ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
            verify(tickerRepository).save(tickerCaptor.capture());

            Ticker savedTicker = tickerCaptor.getValue();
            assertEquals(ticker1.getName(), savedTicker.getName());
            assertEquals(ticker1.getSymbol(), savedTicker.getSymbol());
            assertEquals(ticker1.getType(), savedTicker.getType());
            assertEquals(
                    0, ticker1.getCurrentUnitValue().compareTo(savedTicker.getCurrentUnitValue()));
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when registering a ticker with an existing symbol")
        void testRegisterTickerAlreadyExists() {
            when(tickerRepository.existsBySymbol(ticker1.getSymbol())).thenReturn(true);

            String name = ticker1.getName();
            String symbol = ticker1.getSymbol();
            TickerType type = ticker1.getType();
            BigDecimal currentUnitValue = ticker1.getCurrentUnitValue();
            BigDecimal averageUnitValue = ticker1.getAverageUnitValue();
            BigDecimal currentQuantity = ticker1.getCurrentQuantity();

            assertThrows(
                    EntityExistsException.class,
                    () ->
                            tickerService.addTicker(
                                    name,
                                    symbol,
                                    type,
                                    currentUnitValue,
                                    averageUnitValue,
                                    currentQuantity));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }

        @Test
        @DisplayName("Test if exception is thrown when registering a ticker with an empty name")
        void testRegisterTickerEmptyName() {
            String symbol = ticker1.getSymbol();
            TickerType type = ticker1.getType();
            BigDecimal currentUnitValue = ticker1.getCurrentUnitValue();
            BigDecimal averageUnitValue = ticker1.getAverageUnitValue();
            BigDecimal currentQuantity = ticker1.getCurrentQuantity();

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            tickerService.addTicker(
                                    "",
                                    symbol,
                                    type,
                                    currentUnitValue,
                                    averageUnitValue,
                                    currentQuantity));
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            tickerService.addTicker(
                                    " ",
                                    symbol,
                                    type,
                                    currentUnitValue,
                                    averageUnitValue,
                                    currentQuantity));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }

        @Test
        @DisplayName("Test if exception is thrown when registering a ticker with an empty symbol")
        void testRegisterTickerEmptySymbol() {
            String name = ticker1.getName();
            TickerType type = ticker1.getType();
            BigDecimal currentUnitValue = ticker1.getCurrentUnitValue();
            BigDecimal averageUnitValue = ticker1.getAverageUnitValue();
            BigDecimal currentQuantity = ticker1.getCurrentQuantity();

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            tickerService.addTicker(
                                    name,
                                    "",
                                    type,
                                    currentUnitValue,
                                    averageUnitValue,
                                    currentQuantity));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            tickerService.addTicker(
                                    name,
                                    " ",
                                    type,
                                    currentUnitValue,
                                    averageUnitValue,
                                    currentQuantity));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when registering a ticker with price "
                        + "less than or equal to zero")
        void testRegisterTickerInvalidPrice() {
            when(tickerRepository.existsBySymbol(ticker1.getSymbol())).thenReturn(false);

            String name = ticker1.getName();
            String symbol = ticker1.getSymbol();
            TickerType type = ticker1.getType();
            BigDecimal currentUnitValueNegative = new BigDecimal("-0.05");
            BigDecimal averageUnitValue = ticker1.getAverageUnitValue();
            BigDecimal currentQuantity = ticker1.getCurrentQuantity();

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            tickerService.addTicker(
                                    name,
                                    symbol,
                                    type,
                                    currentUnitValueNegative,
                                    averageUnitValue,
                                    currentQuantity));

            BigDecimal currentUnitValueZero = BigDecimal.ZERO;

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            tickerService.addTicker(
                                    name,
                                    symbol,
                                    type,
                                    currentUnitValueZero,
                                    averageUnitValue,
                                    currentQuantity));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when quantity is less than zero")
        void testRegisterTickerInvalidQuantity() {
            when(tickerRepository.existsBySymbol(ticker1.getSymbol())).thenReturn(false);

            String name = ticker1.getName();
            String symbol = ticker1.getSymbol();
            TickerType type = ticker1.getType();
            BigDecimal currentUnitValue = ticker1.getCurrentUnitValue();
            BigDecimal averageUnitValue = ticker1.getAverageUnitValue();
            BigDecimal currentQuantityNegative = new BigDecimal("-1");

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            tickerService.addTicker(
                                    name,
                                    symbol,
                                    type,
                                    currentUnitValue,
                                    averageUnitValue,
                                    currentQuantityNegative));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when registering a ticker with average unit price "
                        + "less than zero")
        void testRegisterTickerInvalidAverageUnitPrice() {
            when(tickerRepository.existsBySymbol(ticker1.getSymbol())).thenReturn(false);

            String name = ticker1.getName();
            String symbol = ticker1.getSymbol();
            TickerType type = ticker1.getType();
            BigDecimal currentUnitValue = ticker1.getCurrentUnitValue();
            BigDecimal averageUnitValue = new BigDecimal("-0.05");
            BigDecimal currentQuantity = ticker1.getCurrentQuantity();

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            tickerService.addTicker(
                                    name,
                                    symbol,
                                    type,
                                    currentUnitValue,
                                    averageUnitValue,
                                    currentQuantity));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }
    }

    @Nested
    @DisplayName("Delete ticker tests")
    class DeleteTickerTests {

        @Test
        @DisplayName("Test if a ticker is deleted successfully")
        void testDeleteTicker() {
            when(tickerRepository.findById(ticker1.getId())).thenReturn(Optional.of(ticker1));

            when(tickerRepository.getPurchaseCountByTicker(ticker1.getId())).thenReturn(0);
            when(tickerRepository.getSaleCountByTicker(ticker1.getId())).thenReturn(0);
            when(tickerRepository.getDividendCountByTicker(ticker1.getId())).thenReturn(0);
            when(tickerRepository.getCryptoExchangeCountByTicker(ticker1.getId())).thenReturn(0);

            tickerService.deleteTicker(ticker1.getId());

            verify(tickerRepository).delete(ticker1);
        }

        @Test
        @DisplayName("Test if exception is thrown when deleting a non-existent ticker")
        void testDeleteTickerNotFound() {
            when(tickerRepository.findById(ticker1.getId())).thenReturn(Optional.empty());

            Integer tickerId = ticker1.getId();
            assertThrows(EntityNotFoundException.class, () -> tickerService.deleteTicker(tickerId));

            verify(tickerRepository, never()).delete(any(Ticker.class));
        }

        @ParameterizedTest
        @DisplayName("Test if exception is thrown when deleting a ticker with transactions")
        @CsvSource({
            "1, 0, 0, 0", // Purchases
            "0, 1, 0, 0", // Sales
            "0, 0, 1, 0" // Dividends
        })
        void testDeleteTickerWithTransactions(
                int purchaseCount, int saleCount, int dividendCount, int cryptoExchangeCount) {
            when(tickerRepository.findById(ticker1.getId())).thenReturn(Optional.of(ticker1));

            when(tickerRepository.getPurchaseCountByTicker(ticker1.getId()))
                    .thenReturn(purchaseCount);
            when(tickerRepository.getSaleCountByTicker(ticker1.getId())).thenReturn(saleCount);
            when(tickerRepository.getDividendCountByTicker(ticker1.getId()))
                    .thenReturn(dividendCount);
            when(tickerRepository.getCryptoExchangeCountByTicker(ticker1.getId()))
                    .thenReturn(cryptoExchangeCount);

            Integer tickerId = ticker1.getId();
            assertThrows(IllegalStateException.class, () -> tickerService.deleteTicker(tickerId));

            verify(tickerRepository, never()).delete(any(Ticker.class));
        }

        @Test
        @DisplayName("Test if exception is thrown when deleting a ticker with crypto exchanges")
        void testDeleteTickerWithCryptoExchanges() {
            when(tickerRepository.findById(ticker1.getId())).thenReturn(Optional.of(ticker1));

            when(tickerRepository.getPurchaseCountByTicker(ticker1.getId())).thenReturn(0);
            when(tickerRepository.getSaleCountByTicker(ticker1.getId())).thenReturn(0);
            when(tickerRepository.getDividendCountByTicker(ticker1.getId())).thenReturn(0);
            when(tickerRepository.getCryptoExchangeCountByTicker(ticker1.getId())).thenReturn(1);

            Integer tickerId = ticker1.getId();
            assertThrows(IllegalStateException.class, () -> tickerService.deleteTicker(tickerId));

            verify(tickerRepository, never()).delete(any(Ticker.class));
        }

        @Test
        @DisplayName("Test if exception is thrown when deleting a ticker with transactions")
        void testDeleteTickerWithTransactions() {
            when(tickerRepository.findById(ticker1.getId())).thenReturn(Optional.of(ticker1));

            when(tickerRepository.getPurchaseCountByTicker(ticker1.getId())).thenReturn(1);
            when(tickerRepository.getSaleCountByTicker(ticker1.getId())).thenReturn(2);
            when(tickerRepository.getDividendCountByTicker(ticker1.getId())).thenReturn(3);
            when(tickerRepository.getCryptoExchangeCountByTicker(ticker1.getId())).thenReturn(4);

            Integer tickerId = ticker1.getId();
            assertThrows(IllegalStateException.class, () -> tickerService.deleteTicker(tickerId));

            verify(tickerRepository, never()).delete(any(Ticker.class));
        }
    }

    @Nested
    @DisplayName("Archive and unarchive ticker tests")
    class ArchiveUnarchiveTickerTests {

        @Test
        @DisplayName("Test if a ticker is archived successfully")
        void testArchiveTicker() {
            when(tickerRepository.findById(ticker1.getId())).thenReturn(Optional.of(ticker1));

            tickerService.archiveTicker(ticker1.getId());

            verify(tickerRepository).save(ticker1);
            assertTrue(ticker1.isArchived());
        }

        @Test
        @DisplayName("Test if exception is thrown when archiving a non-existent ticker")
        void testArchiveTickerNotFound() {
            when(tickerRepository.findById(ticker1.getId())).thenReturn(Optional.empty());

            Integer tickerId = ticker1.getId();
            assertThrows(
                    EntityNotFoundException.class, () -> tickerService.archiveTicker(tickerId));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }

        @Test
        @DisplayName("Test if a ticker is unarchived successfully")
        void testUnarchiveTicker() {
            when(tickerRepository.findById(ticker1.getId())).thenReturn(Optional.of(ticker1));

            tickerService.unarchiveTicker(ticker1.getId());

            verify(tickerRepository).save(ticker1);
            assertFalse(ticker1.isArchived());
        }

        @Test
        @DisplayName("Test if exception is thrown when unarchiving a non-existent ticker")
        void testUnarchiveTickerNotFound() {
            when(tickerRepository.findById(ticker1.getId())).thenReturn(Optional.empty());

            Integer tickerId = ticker1.getId();
            assertThrows(
                    EntityNotFoundException.class, () -> tickerService.unarchiveTicker(tickerId));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }
    }

    @Nested
    @DisplayName("Update ticker tests")
    class UpdateTickerTests {

        @Test
        @DisplayName("Test if ticker name is changed successfully")
        void testChangeTickerName() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            String oldName = ticker1.getName();
            String newName = oldName + " Changed";
            ticker1.setName(newName);

            tickerService.updateTicker(ticker1);

            ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
            verify(tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

            assertEquals(newName, tickerCaptor.getValue().getName());
        }

        @Test
        @DisplayName("Test if exception is thrown when updating a non-existent ticker")
        void testChangeTickerNameNotFound() {
            when(tickerRepository.findById(1)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> tickerService.updateTicker(ticker1));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }

        @ParameterizedTest
        @DisplayName("Test if exception is thrown when updating a ticker with an invalid name")
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void testChangeTickerInvalidName(String invalidName) {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            ticker1.setName(invalidName);

            assertThrows(IllegalArgumentException.class, () -> tickerService.updateTicker(ticker1));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }

        @Test
        @DisplayName("Test if ticker symbol is changed successfully")
        void testChangeTickerSymbol() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            String oldSymbol = ticker1.getSymbol();
            String newSymbol = oldSymbol + " Changed";
            ticker1.setSymbol(newSymbol);

            tickerService.updateTicker(ticker1);

            ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
            verify(tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

            assertEquals(newSymbol, tickerCaptor.getValue().getSymbol());
        }

        @ParameterizedTest
        @DisplayName("Test if exception is thrown when updating a ticker with an invalid symbol")
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void testChangeTickerInvalidSymbol(String invalidSymbol) {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            ticker1.setSymbol(invalidSymbol);

            assertThrows(IllegalArgumentException.class, () -> tickerService.updateTicker(ticker1));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }

        @Test
        @DisplayName("Test if ticker type is changed successfully")
        void testChangeTickerType() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            TickerType oldType = ticker1.getType();

            TickerType newType;

            if (oldType == TickerType.STOCK) {
                newType = TickerType.FUND;
            } else {
                newType = TickerType.STOCK;
            }

            ticker1.setType(newType);

            tickerService.updateTicker(ticker1);

            ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
            verify(tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

            assertEquals(newType, tickerCaptor.getValue().getType());
        }

        @Test
        @DisplayName("Test if ticker price is changed successfully")
        void testChangeTickerPrice() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            BigDecimal oldPrice = ticker1.getCurrentUnitValue();

            BigDecimal newPrice;

            if (oldPrice.compareTo(BigDecimal.ONE) == 0) {
                newPrice = BigDecimal.TEN;
            } else {
                newPrice = BigDecimal.ONE;
            }

            ticker1.setCurrentUnitValue(newPrice);

            tickerService.updateTicker(ticker1);

            ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
            verify(tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

            assertEquals(0, newPrice.compareTo(tickerCaptor.getValue().getCurrentUnitValue()));
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when updating a ticker with price " + "less than zero")
        void testChangeTickerPriceLessThanZero() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            ticker1.setCurrentUnitValue(new BigDecimal("-0.05"));

            assertThrows(IllegalArgumentException.class, () -> tickerService.updateTicker(ticker1));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when updating a ticker with price " + "equal to zero")
        void testChangeTickerPriceEqualToZero() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            ticker1.setCurrentUnitValue(BigDecimal.ZERO);

            assertThrows(IllegalArgumentException.class, () -> tickerService.updateTicker(ticker1));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }

        @Test
        @DisplayName("Test if ticker current quantity is changed successfully")
        void testChangeTickerCurrentQuantity() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            BigDecimal oldQuantity = ticker1.getCurrentQuantity();

            BigDecimal newQuantity;

            if (oldQuantity.compareTo(BigDecimal.ONE) == 0) {
                newQuantity = BigDecimal.TEN;
            } else {
                newQuantity = BigDecimal.ONE;
            }

            ticker1.setCurrentQuantity(newQuantity);

            tickerService.updateTicker(ticker1);

            ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
            verify(tickerRepository).save(tickerCaptor.capture());

            assertEquals(newQuantity, tickerCaptor.getValue().getCurrentQuantity());
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when updating a ticker with current quantity "
                        + "less than zero")
        void testChangeTickerCurrentQuantityLessThanZero() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            ticker1.setCurrentQuantity(new BigDecimal("-0.05"));

            assertThrows(IllegalArgumentException.class, () -> tickerService.updateTicker(ticker1));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }

        @Test
        @DisplayName("Test if ticker average price is changed successfully")
        void testChangeTickerAveragePrice() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            BigDecimal oldPrice = ticker1.getAverageUnitValue();

            // Quantity cannot be zero, because in this case the average price is reset at
            // the end of the function
            ticker1.setCurrentQuantity(new BigDecimal("1"));

            BigDecimal newPrice;

            if (oldPrice.compareTo(BigDecimal.ONE) == 0) {
                newPrice = BigDecimal.TEN;
            } else {
                newPrice = BigDecimal.ONE;
            }

            ticker1.setAverageUnitValue(newPrice);

            tickerService.updateTicker(ticker1);

            ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
            verify(tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

            assertEquals(0, newPrice.compareTo(tickerCaptor.getValue().getAverageUnitValue()));
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when updating a ticker with average price "
                        + "less than zero")
        void testChangeTickerAveragePriceLessThanZero() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            ticker1.setAverageUnitValue(new BigDecimal("-0.05"));

            assertThrows(IllegalArgumentException.class, () -> tickerService.updateTicker(ticker1));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }

        @Test
        @DisplayName("Test if ticker is set to archived successfully")
        void testChangeTickerArchived() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            ticker1.setArchived(true);

            tickerService.updateTicker(ticker1);

            ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
            verify(tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

            assertTrue(tickerCaptor.getValue().isArchived());
        }

        @Test
        @DisplayName("Test if ticker is set to unarchived successfully")
        void testChangeTickerUnarchived() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            ticker1.setArchived(false);

            tickerService.updateTicker(ticker1);

            ArgumentCaptor<Ticker> tickerCaptor = ArgumentCaptor.forClass(Ticker.class);
            verify(tickerRepository, atLeastOnce()).save(tickerCaptor.capture());

            assertFalse(tickerCaptor.getValue().isArchived());
        }
    }

    @Nested
    @DisplayName("Update Tickers Price Async Tests")
    class UpdateTickersPriceAsyncTests {

        LocalDateTime lastUpdate;
        private Ticker tickerToUpdate1;
        private Ticker tickerToUpdate2;
        private List<Ticker> tickers;

        private JSONObject createPriceObject(String price) throws JSONException {
            return new JSONObject().put("price", new BigDecimal(price));
        }

        @BeforeEach
        void setUp() {
            lastUpdate = LocalDateTime.now().minusDays(5);

            tickerToUpdate1 =
                    new Ticker(
                            1,
                            "Test Stock 1",
                            "TEST4",
                            TickerType.STOCK,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            lastUpdate);
            tickerToUpdate2 =
                    new Ticker(
                            2,
                            "Test Stock 2",
                            "ANST4",
                            TickerType.STOCK,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            lastUpdate);
            tickers = List.of(tickerToUpdate1, tickerToUpdate2);
        }

        @Test
        @DisplayName("Should update ticker prices successfully")
        void updateTickersPriceFromApiAsync_Success() throws JSONException {

            JSONObject apiResponse =
                    new JSONObject()
                            .put("TEST4", createPriceObject("98.50"))
                            .put("ANST4", createPriceObject("55.25"));

            CompletableFuture<JSONObject> futureResponse =
                    CompletableFuture.completedFuture(apiResponse);

            try (MockedStatic<APIUtils> mockedApi = Mockito.mockStatic(APIUtils.class)) {
                mockedApi
                        .when(() -> APIUtils.fetchStockPricesAsync(any()))
                        .thenReturn(futureResponse);

                CompletableFuture<List<Ticker>> resultFuture =
                        tickerService.updateTickersPriceFromApiAsync(tickers);
                List<Ticker> failedTickers = resultFuture.join();

                assertTrue(failedTickers.isEmpty());
                ArgumentCaptor<Ticker> captor = ArgumentCaptor.forClass(Ticker.class);
                verify(tickerRepository, times(2)).save(captor.capture());

                Ticker savedTicker1 =
                        captor.getAllValues().stream()
                                .filter(t -> t.getSymbol().equals("TEST4"))
                                .findFirst()
                                .get();
                Ticker savedTicker2 =
                        captor.getAllValues().stream()
                                .filter(t -> t.getSymbol().equals("ANST4"))
                                .findFirst()
                                .get();

                assertEquals(new BigDecimal("98.50"), savedTicker1.getCurrentUnitValue());
                assertEquals(new BigDecimal("55.25"), savedTicker2.getCurrentUnitValue());
            }
        }

        @Test
        @DisplayName("Should return a list of tickers that failed to update")
        void updateTickersPriceFromApiAsync_PartialFailure() throws JSONException {
            Ticker failedTicker =
                    new Ticker(
                            2,
                            "Failed Stock",
                            "FAIL4",
                            TickerType.STOCK,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            LocalDateTime.now());
            List<Ticker> tickersWithFailed = List.of(tickerToUpdate1, failedTicker);

            JSONObject apiResponse = new JSONObject().put("TEST4", createPriceObject("99.00"));

            CompletableFuture<JSONObject> futureResponse =
                    CompletableFuture.completedFuture(apiResponse);

            try (MockedStatic<APIUtils> mockedApi = Mockito.mockStatic(APIUtils.class)) {
                mockedApi
                        .when(() -> APIUtils.fetchStockPricesAsync(any()))
                        .thenReturn(futureResponse);

                CompletableFuture<List<Ticker>> resultFuture =
                        tickerService.updateTickersPriceFromApiAsync(tickersWithFailed);
                List<Ticker> failedList = resultFuture.join();

                assertEquals(1, failedList.size());
                assertEquals("FAIL4", failedList.get(0).getSymbol());
                verify(tickerRepository, times(1)).save(any(Ticker.class));
            }
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for an empty list of tickers")
        void updateTickersPriceFromApiAsync_EmptyList_ThrowsException() {
            CompletableFuture<List<Ticker>> resultFuture =
                    tickerService.updateTickersPriceFromApiAsync(Collections.emptyList());

            Exception exception = assertThrows(ExecutionException.class, resultFuture::get);
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
        }
    }

    @Nested
    @DisplayName("Add Purchase Tests")
    class AddPurchaseTests {

        @Test
        @DisplayName("Test if a purchase is added successfully to a ticker")
        void testAddPurchase() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            when(walletTransactionService.addExpense(anyInt(), any(), any(), any(), any(), any()))
                    .thenReturn(100);

            when(walletTransactionService.getTransactionById(100))
                    .thenReturn(
                            WalletTransaction.builder()
                                    .wallet(wallet)
                                    .category(category)
                                    .type(TransactionType.EXPENSE)
                                    .status(TransactionStatus.CONFIRMED)
                                    .date(LocalDateTime.now())
                                    .amount(new BigDecimal("50"))
                                    .description("TickerPurchase")
                                    .build());

            tickerService.addPurchase(
                    1,
                    1,
                    new BigDecimal("10"),
                    new BigDecimal("150"),
                    category,
                    LocalDateTime.now(),
                    "TickerPurchase",
                    TransactionStatus.CONFIRMED);

            ArgumentCaptor<TickerPurchase> purchaseCaptor =
                    ArgumentCaptor.forClass(TickerPurchase.class);

            verify(tickerPurchaseRepository).save(purchaseCaptor.capture());

            assertEquals(ticker1, purchaseCaptor.getValue().getTicker());
            assertEquals(new BigDecimal("10"), purchaseCaptor.getValue().getQuantity());
        }

        @Test
        @DisplayName("Test if adding a purchase to a non-existent ticker throws an exception")
        void testAddPurchaseTickerNotFound() {
            when(tickerRepository.findById(1)).thenReturn(Optional.empty());

            Integer tickerId = 1;
            Integer walletId = 1;
            BigDecimal quantity = new BigDecimal("10");
            BigDecimal unitPrice = new BigDecimal("150");
            LocalDateTime date = LocalDateTime.now();
            String description = "TickerPurchase";
            TransactionStatus status = TransactionStatus.CONFIRMED;

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            tickerService.addPurchase(
                                    tickerId,
                                    walletId,
                                    quantity,
                                    unitPrice,
                                    category,
                                    date,
                                    description,
                                    status));

            verify(tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
        }

        @Test
        @DisplayName(
                "Test if adding a purchase with quantity less than or equal to zero "
                        + "throws an exception")
        void testAddPurchaseInvalidQuantity() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            Integer tickerId = 1;
            Integer walletId = 1;
            BigDecimal quantity = BigDecimal.ZERO;
            BigDecimal unitPrice = new BigDecimal("150");
            LocalDateTime date = LocalDateTime.now();
            String description = "TickerPurchase";
            TransactionStatus status = TransactionStatus.CONFIRMED;

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            tickerService.addPurchase(
                                    tickerId,
                                    walletId,
                                    quantity,
                                    unitPrice,
                                    category,
                                    date,
                                    description,
                                    status));

            verify(tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
        }

        @Test
        @DisplayName(
                "Test if adding a purchase with unit price less than or equal to "
                        + "zero throws an exception")
        void testAddPurchaseInvalidUnitPrice() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            Integer tickerId = 1;
            Integer walletId = 1;
            BigDecimal quantity = new BigDecimal("10");
            BigDecimal unitPrice = BigDecimal.ZERO;
            LocalDateTime date = LocalDateTime.now();
            String description = "TickerPurchase";
            TransactionStatus status = TransactionStatus.CONFIRMED;

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            tickerService.addPurchase(
                                    tickerId,
                                    walletId,
                                    quantity,
                                    unitPrice,
                                    category,
                                    date,
                                    description,
                                    status));

            verify(tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
        }
    }

    @Nested
    @DisplayName("Add Sale Tests")
    class AddSaleTests {

        @Test
        @DisplayName("Test if a sale is added successfully to a ticker")
        void testAddSale() {
            ticker1.setCurrentQuantity(new BigDecimal("20"));

            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            when(walletTransactionService.addIncome(anyInt(), any(), any(), any(), any(), any()))
                    .thenReturn(100);

            when(walletTransactionService.getTransactionById(100))
                    .thenReturn(
                            WalletTransaction.builder()
                                    .wallet(wallet)
                                    .category(category)
                                    .type(TransactionType.INCOME)
                                    .status(TransactionStatus.CONFIRMED)
                                    .date(LocalDateTime.now())
                                    .amount(new BigDecimal("50"))
                                    .description("TickerSale")
                                    .build());

            tickerService.addSale(
                    1,
                    1,
                    new BigDecimal("20"),
                    new BigDecimal("200"),
                    category,
                    LocalDateTime.now(),
                    "TickerSale",
                    TransactionStatus.CONFIRMED);

            ArgumentCaptor<TickerSale> saleCaptor = ArgumentCaptor.forClass(TickerSale.class);

            verify(tickerSaleRepository).save(saleCaptor.capture());

            assertEquals(ticker1, saleCaptor.getValue().getTicker());
            assertEquals(new BigDecimal("20"), saleCaptor.getValue().getQuantity());
        }

        @Test
        @DisplayName("Test if adding a sale to a non-existent ticker throws an exception")
        void testAddSaleTickerNotFound() {
            when(tickerRepository.findById(1)).thenReturn(Optional.empty());

            Integer tickerId = 1;
            Integer walletId = 1;
            BigDecimal quantity = new BigDecimal("10");
            BigDecimal unitPrice = new BigDecimal("200");
            LocalDateTime date = LocalDateTime.now();
            String description = "TickerSale";
            TransactionStatus status = TransactionStatus.CONFIRMED;

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            tickerService.addSale(
                                    tickerId,
                                    walletId,
                                    quantity,
                                    unitPrice,
                                    category,
                                    date,
                                    description,
                                    status));

            verify(tickerSaleRepository, never()).save(any(TickerSale.class));
        }

        @Test
        @DisplayName(
                "Test if adding a sale with quantity less than or equal to zero throws an"
                        + " exception")
        void testAddSaleInvalidQuantity() {
            ticker1.setCurrentQuantity(new BigDecimal("10"));
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            Integer tickerId = 1;
            Integer walletId = 1;
            BigDecimal quantity = BigDecimal.ZERO;
            BigDecimal unitPrice = new BigDecimal("200");
            LocalDateTime date = LocalDateTime.now();
            String description = "TickerSale";
            TransactionStatus status = TransactionStatus.CONFIRMED;

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            tickerService.addSale(
                                    tickerId,
                                    walletId,
                                    quantity,
                                    unitPrice,
                                    category,
                                    date,
                                    description,
                                    status));

            verify(tickerSaleRepository, never()).save(any(TickerSale.class));
        }

        @Test
        @DisplayName(
                "Test if adding a sale with quantity greater than current quantity "
                        + "throws an exception")
        void testAddSaleExceedsQuantity() {
            ticker1.setCurrentQuantity(new BigDecimal("5"));
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            assertThrows(
                    MoinexException.InsufficientResourcesException.class,
                    () ->
                            tickerService.addSale(
                                    1,
                                    1,
                                    new BigDecimal("10"),
                                    new BigDecimal("200"),
                                    category,
                                    LocalDateTime.now(),
                                    "TickerSale",
                                    TransactionStatus.CONFIRMED));

            verify(tickerSaleRepository, never()).save(any(TickerSale.class));
        }

        @Test
        @DisplayName(
                "Test if adding a sale with unit price less than or equal to zero "
                        + "throws an exception")
        void testAddSaleInvalidUnitPrice() {
            ticker1.setCurrentQuantity(new BigDecimal("10"));
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            Integer tickerId = 1;
            Integer walletId = 1;
            BigDecimal quantity = new BigDecimal("5");
            BigDecimal unitPrice = BigDecimal.ZERO;
            LocalDateTime date = LocalDateTime.now();
            String description = "TickerSale";
            TransactionStatus status = TransactionStatus.CONFIRMED;

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            tickerService.addSale(
                                    tickerId,
                                    walletId,
                                    quantity,
                                    unitPrice,
                                    category,
                                    date,
                                    description,
                                    status));

            verify(tickerSaleRepository, never()).save(any(TickerSale.class));
        }
    }

    @Nested
    @DisplayName("Add Dividend Tests")
    class AddDividendTests {

        @Test
        @DisplayName("Test if a dividend is added successfully to a ticker")
        void testAddDividend() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            when(walletTransactionService.addIncome(anyInt(), any(), any(), any(), any(), any()))
                    .thenReturn(100);

            when(walletTransactionService.getTransactionById(100))
                    .thenReturn(
                            WalletTransaction.builder()
                                    .wallet(wallet)
                                    .category(category)
                                    .type(TransactionType.INCOME)
                                    .status(TransactionStatus.CONFIRMED)
                                    .date(LocalDateTime.now())
                                    .amount(new BigDecimal("50"))
                                    .description("Dividend Payment")
                                    .build());

            tickerService.addDividend(
                    1,
                    1,
                    category,
                    new BigDecimal("50"),
                    LocalDateTime.now(),
                    "Dividend Payment",
                    TransactionStatus.CONFIRMED);

            verify(dividendRepository).save(any(Dividend.class));
        }

        @Test
        @DisplayName("Test if adding a dividend to a non-existent ticker throws an exception")
        void testAddDividendTickerNotFound() {
            when(tickerRepository.findById(1)).thenReturn(Optional.empty());

            Integer tickerId = 1;
            Integer walletId = 1;
            BigDecimal amount = new BigDecimal("50");
            LocalDateTime date = LocalDateTime.now();
            String description = "Dividend Payment";
            TransactionStatus status = TransactionStatus.CONFIRMED;

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            tickerService.addDividend(
                                    tickerId,
                                    walletId,
                                    category,
                                    amount,
                                    date,
                                    description,
                                    status));

            verify(dividendRepository, never()).save(any(Dividend.class));

            verify(walletTransactionService, never())
                    .addIncome(anyInt(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName(
                "Test if adding a dividend with amount less than or equal to zero "
                        + "throws an exception")
        void testAddDividendInvalidAmount() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));

            Integer tickerId = 1;
            Integer walletId = 1;
            BigDecimal amount = BigDecimal.ZERO;
            LocalDateTime date = LocalDateTime.now();
            String description = "Dividend Payment";
            TransactionStatus status = TransactionStatus.CONFIRMED;

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            tickerService.addDividend(
                                    tickerId,
                                    walletId,
                                    category,
                                    amount,
                                    date,
                                    description,
                                    status));

            verify(dividendRepository, never()).save(any(Dividend.class));

            verify(walletTransactionService, never())
                    .addIncome(anyInt(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Add Crypto Exchange Tests")
    class AddCryptoExchangeTests {

        @Test
        @DisplayName("Test if a crypto exchange is added successfully to a ticker")
        void testAddCryptoExchange() {
            ticker1.setType(TickerType.CRYPTOCURRENCY);
            ticker2.setType(TickerType.CRYPTOCURRENCY);

            ticker1.setCurrentQuantity(new BigDecimal("10"));
            ticker2.setCurrentQuantity(new BigDecimal("0"));

            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));
            when(tickerRepository.findById(2)).thenReturn(Optional.of(ticker2));

            when(cryptoExchangeRepository.save(any(CryptoExchange.class)))
                    .thenReturn(cryptoExchange);

            BigDecimal sourcePreviousQuantity = ticker1.getCurrentQuantity();
            BigDecimal targetPreviousQuantity = ticker2.getCurrentQuantity();

            BigDecimal sourceQuantity = new BigDecimal("10");
            BigDecimal targetQuantity = new BigDecimal("1");

            tickerService.addCryptoExchange(
                    1, 2, sourceQuantity, targetQuantity, LocalDateTime.now(), "");

            ArgumentCaptor<CryptoExchange> exchangeCaptor =
                    ArgumentCaptor.forClass(CryptoExchange.class);

            verify(cryptoExchangeRepository).save(exchangeCaptor.capture());

            assertEquals(ticker1, exchangeCaptor.getValue().getSoldCrypto());

            assertEquals(ticker2, exchangeCaptor.getValue().getReceivedCrypto());

            BigDecimal sourceCurrentQuantity = ticker1.getCurrentQuantity();
            BigDecimal targetCurrentQuantity = ticker2.getCurrentQuantity();

            assertEquals(
                    0,
                    sourceCurrentQuantity.compareTo(
                            sourcePreviousQuantity.subtract(sourceQuantity)));

            assertEquals(
                    0, targetCurrentQuantity.compareTo(targetPreviousQuantity.add(targetQuantity)));
        }

        @Test
        @DisplayName(
                "Test if adding a crypto exchange with source and target tickers "
                        + "being the same throws an exception")
        void testAddCryptoExchangeSameTicker() {
            assertThrows(
                    MoinexException.SameSourceDestinationException.class,
                    () ->
                            tickerService.addCryptoExchange(
                                    1,
                                    1,
                                    new BigDecimal("1"),
                                    new BigDecimal("1"),
                                    LocalDateTime.now(),
                                    ""));

            verify(cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
        }

        @Test
        @DisplayName(
                "Test if adding a crypto exchange with source ticker not found "
                        + "throws an exception")
        void testAddCryptoExchangeSourceNotFound() {
            when(tickerRepository.findById(1)).thenReturn(Optional.empty());

            Integer sourceId = 1;
            Integer targetId = 2;
            BigDecimal sourceQuantity = new BigDecimal("1");
            BigDecimal targetQuantity = new BigDecimal("1");
            LocalDateTime date = LocalDateTime.now();
            String description = "";

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            tickerService.addCryptoExchange(
                                    sourceId,
                                    targetId,
                                    sourceQuantity,
                                    targetQuantity,
                                    date,
                                    description));

            verify(cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
        }

        @Test
        @DisplayName(
                "Test if adding a crypto exchange with target ticker not found "
                        + "throws an exception")
        void testAddCryptoExchangeTargetNotFound() {
            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));
            when(tickerRepository.findById(2)).thenReturn(Optional.empty());

            Integer sourceId = 1;
            Integer targetId = 2;
            BigDecimal sourceQuantity = new BigDecimal("1");
            BigDecimal targetQuantity = new BigDecimal("1");
            LocalDateTime date = LocalDateTime.now();
            String description = "";

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            tickerService.addCryptoExchange(
                                    sourceId,
                                    targetId,
                                    sourceQuantity,
                                    targetQuantity,
                                    date,
                                    description));

            verify(cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
        }

        @Test
        @DisplayName(
                "Test if adding a crypto exchange with source or target ticker type is not "
                        + " cryptocurrency throws an exception")
        void testAddCryptoExchangeInvalidTickerType() {
            ticker1.setType(TickerType.STOCK);
            ticker2.setType(TickerType.STOCK);

            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));
            when(tickerRepository.findById(2)).thenReturn(Optional.of(ticker2));

            assertThrows(
                    MoinexException.InvalidTickerTypeException.class,
                    () ->
                            tickerService.addCryptoExchange(
                                    1,
                                    2,
                                    new BigDecimal("1"),
                                    new BigDecimal("1"),
                                    LocalDateTime.now(),
                                    ""));

            verify(cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
        }

        @Test
        @DisplayName(
                "Test if adding a crypto exchange with source quantity less than or "
                        + "equal to zero throws an exception")
        void testAddCryptoExchangeInvalidSourceQuantity() {
            ticker1.setType(TickerType.CRYPTOCURRENCY);
            ticker2.setType(TickerType.CRYPTOCURRENCY);

            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));
            when(tickerRepository.findById(2)).thenReturn(Optional.of(ticker2));

            Integer sourceId = 1;
            Integer targetId = 2;
            BigDecimal sourceQuantity = BigDecimal.ZERO;
            BigDecimal targetQuantity = new BigDecimal("1");
            LocalDateTime date = LocalDateTime.now();
            String description = "";

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            tickerService.addCryptoExchange(
                                    sourceId,
                                    targetId,
                                    sourceQuantity,
                                    targetQuantity,
                                    date,
                                    description));

            verify(cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
        }

        @Test
        @DisplayName(
                "Test if adding a crypto exchange with target quantity less than or "
                        + "equal to zero throws an exception")
        void testAddCryptoExchangeInvalidTargetQuantity() {
            ticker1.setType(TickerType.CRYPTOCURRENCY);
            ticker2.setType(TickerType.CRYPTOCURRENCY);

            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));
            when(tickerRepository.findById(2)).thenReturn(Optional.of(ticker2));

            Integer sourceId = 1;
            Integer targetId = 2;
            BigDecimal sourceQuantity = new BigDecimal("1");
            BigDecimal targetQuantity = BigDecimal.ZERO;
            LocalDateTime date = LocalDateTime.now();
            String description = "";

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            tickerService.addCryptoExchange(
                                    sourceId,
                                    targetId,
                                    sourceQuantity,
                                    targetQuantity,
                                    date,
                                    description));

            verify(cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
        }

        @Test
        @DisplayName(
                "Test if adding a crypto exchange with source quantity greater than "
                        + "current quantity throws an exception")
        void testAddCryptoExchangeExceedsSourceQuantity() {
            ticker1.setType(TickerType.CRYPTOCURRENCY);
            ticker2.setType(TickerType.CRYPTOCURRENCY);

            ticker1.setCurrentQuantity(new BigDecimal("5"));
            ticker2.setCurrentQuantity(new BigDecimal("0"));

            when(tickerRepository.findById(1)).thenReturn(Optional.of(ticker1));
            when(tickerRepository.findById(2)).thenReturn(Optional.of(ticker2));

            assertThrows(
                    MoinexException.InsufficientResourcesException.class,
                    () ->
                            tickerService.addCryptoExchange(
                                    1,
                                    2,
                                    new BigDecimal("10"),
                                    new BigDecimal("1"),
                                    LocalDateTime.now(),
                                    ""));

            verify(cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
        }
    }

    @Nested
    @DisplayName("Delete Ticker Tests (Purchases, Sales, Dividends, Exchanges)")
    class DeleteTickerTransactionsTests {

        @Test
        @DisplayName("Should delete a purchase and its wallet transaction")
        void deletePurchase_Success() {
            WalletTransaction wt = WalletTransaction.builder().id(10).build();
            TickerPurchase purchase =
                    new TickerPurchase(1, ticker1, BigDecimal.ONE, BigDecimal.TEN, wt);
            when(tickerPurchaseRepository.findById(purchase.getId()))
                    .thenReturn(Optional.of(purchase));

            tickerService.deletePurchase(purchase.getId());

            verify(tickerPurchaseRepository).delete(purchase);
            verify(walletTransactionService).deleteTransaction(wt.getId());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when deleting a non-existent purchase")
        void deletePurchase_NotFound_ThrowsException() {
            when(tickerPurchaseRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> tickerService.deletePurchase(999));
        }

        @Test
        @DisplayName("Should delete a sale and its wallet transaction")
        void deleteSale_Success() {
            WalletTransaction wt = WalletTransaction.builder().id(11).build();
            TickerSale sale =
                    new TickerSale(1, ticker1, BigDecimal.ONE, BigDecimal.TEN, wt, BigDecimal.ONE);

            when(tickerSaleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));

            tickerService.deleteSale(sale.getId());

            verify(tickerSaleRepository).delete(sale);
            verify(walletTransactionService).deleteTransaction(wt.getId());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when deleting a non-existent sale")
        void deleteSale_NotFound_ThrowsException() {
            when(tickerSaleRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> tickerService.deleteSale(999));
        }

        @Test
        @DisplayName("Should delete a dividend and its wallet transaction")
        void deleteDividend_Success() {
            WalletTransaction wt = WalletTransaction.builder().id(12).build();
            Dividend dividend = new Dividend(1, ticker1, wt);
            when(dividendRepository.findById(dividend.getId())).thenReturn(Optional.of(dividend));

            tickerService.deleteDividend(dividend.getId());

            verify(dividendRepository).delete(dividend);
            verify(walletTransactionService).deleteTransaction(wt.getId());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when deleting a non-existent dividend")
        void deleteDividend_NotFound_ThrowsException() {
            when(dividendRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> tickerService.deleteDividend(999));
        }

        @Test
        @DisplayName("Should delete a crypto exchange and adjust ticker quantities")
        void deleteCryptoExchange_Success() {
            Ticker crypto1 =
                    new Ticker(
                            2,
                            "Crypto A",
                            "CA",
                            TickerType.CRYPTOCURRENCY,
                            new BigDecimal("8.00"),
                            BigDecimal.ZERO,
                            new BigDecimal("10.00"),
                            LocalDateTime.now());

            Ticker crypto2 =
                    new Ticker(
                            3,
                            "Crypto B",
                            "CB",
                            TickerType.CRYPTOCURRENCY,
                            new BigDecimal("9.00"),
                            BigDecimal.ZERO,
                            new BigDecimal("5.00"),
                            LocalDateTime.now());

            CryptoExchange exchange =
                    new CryptoExchange(
                            1,
                            crypto1,
                            crypto2,
                            new BigDecimal("2.00"),
                            new BigDecimal("4.00"),
                            LocalDateTime.now(),
                            "Exchange");

            when(cryptoExchangeRepository.findById(exchange.getId()))
                    .thenReturn(Optional.of(exchange));

            tickerService.deleteCryptoExchange(exchange.getId());

            verify(cryptoExchangeRepository).delete(exchange);
            verify(tickerRepository).save(crypto1);
            verify(tickerRepository).save(crypto2);

            assertEquals(new BigDecimal("10.00"), crypto1.getCurrentQuantity()); // 8 + 2
            assertEquals(new BigDecimal("5.00"), crypto2.getCurrentQuantity()); // 9 - 4
        }

        @Test
        @DisplayName(
                "Should throw EntityNotFoundException when deleting a non-existent crypto exchange")
        void deleteCryptoExchange_NotFound_ThrowsException() {
            when(cryptoExchangeRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class, () -> tickerService.deleteCryptoExchange(999));

            verify(tickerRepository, never()).save(any(Ticker.class));
        }
    }

    @Nested
    @DisplayName("Update Purchase Tests")
    class UpdatePurchaseTests {
        @Test
        @DisplayName("Test if purchase quantity is updated successfully")
        void testUpdatePurchaseQuantity() {
            when(tickerPurchaseRepository.findById(1)).thenReturn(Optional.of(tickerPurchase));
            when(tickerRepository.existsById(1)).thenReturn(true);

            BigDecimal oldQuantity = tickerPurchase.getQuantity();

            BigDecimal newQuantity;

            if (oldQuantity.compareTo(BigDecimal.ONE) == 0) {
                newQuantity = BigDecimal.TEN;
            } else {
                newQuantity = BigDecimal.ONE;
            }

            BigDecimal expectedAmountAfterUpdate =
                    newQuantity.multiply(tickerPurchase.getUnitPrice());

            tickerPurchase.setQuantity(newQuantity);

            tickerService.updatePurchase(tickerPurchase);

            ArgumentCaptor<TickerPurchase> purchaseCaptor =
                    ArgumentCaptor.forClass(TickerPurchase.class);
            verify(tickerPurchaseRepository).save(purchaseCaptor.capture());

            assertEquals(newQuantity, purchaseCaptor.getValue().getQuantity());
            assertEquals(
                    expectedAmountAfterUpdate,
                    purchaseCaptor.getValue().getWalletTransaction().getAmount());
        }

        @Test
        @DisplayName("Test if exception is thrown when updating a non-existent purchase")
        void testUpdatePurchaseNotFound() {
            when(tickerPurchaseRepository.findById(1)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> tickerService.updatePurchase(tickerPurchase));

            verify(tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
        }

        @Test
        @DisplayName("Test if exception is thrown when updating a purchase with ticker not found")
        void testUpdatePurchaseTickerNotFound() {
            when(tickerPurchaseRepository.findById(1)).thenReturn(Optional.of(tickerPurchase));
            when(tickerRepository.existsById(1)).thenReturn(false);

            assertThrows(
                    EntityNotFoundException.class,
                    () -> tickerService.updatePurchase(tickerPurchase));

            verify(tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when updating a purchase with quantity "
                        + "less than zero")
        void testUpdatePurchaseQuantityLessThanZero() {
            when(tickerPurchaseRepository.findById(1)).thenReturn(Optional.of(tickerPurchase));
            when(tickerRepository.existsById(1)).thenReturn(true);

            tickerPurchase.setQuantity(new BigDecimal("-0.05"));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> tickerService.updatePurchase(tickerPurchase));

            verify(tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when updating a purchase with quantity "
                        + "equal to zero")
        void testUpdatePurchaseQuantityEqualToZero() {
            when(tickerPurchaseRepository.findById(1)).thenReturn(Optional.of(tickerPurchase));
            when(tickerRepository.existsById(1)).thenReturn(true);

            tickerPurchase.setQuantity(BigDecimal.ZERO);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> tickerService.updatePurchase(tickerPurchase));

            verify(tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
        }

        @Test
        @DisplayName("Test if purchase unit price is updated successfully")
        void testUpdatePurchaseUnitPrice() {
            when(tickerPurchaseRepository.findById(1)).thenReturn(Optional.of(tickerPurchase));
            when(tickerRepository.existsById(1)).thenReturn(true);

            BigDecimal oldUnitPrice = tickerPurchase.getUnitPrice();

            BigDecimal newUnitPrice;

            if (oldUnitPrice.compareTo(BigDecimal.ONE) == 0) {
                newUnitPrice = BigDecimal.TEN;
            } else {
                newUnitPrice = BigDecimal.ONE;
            }

            BigDecimal expectedAmountAfterUpdate =
                    tickerPurchase.getQuantity().multiply(newUnitPrice);

            tickerPurchase.setUnitPrice(newUnitPrice);

            tickerService.updatePurchase(tickerPurchase);

            verify(tickerPurchaseRepository).save(tickerPurchase);

            ArgumentCaptor<TickerPurchase> purchaseCaptor =
                    ArgumentCaptor.forClass(TickerPurchase.class);
            verify(tickerPurchaseRepository).save(purchaseCaptor.capture());

            assertEquals(newUnitPrice, purchaseCaptor.getValue().getUnitPrice());
            assertEquals(
                    expectedAmountAfterUpdate,
                    purchaseCaptor.getValue().getWalletTransaction().getAmount());
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when updating a purchase with unit price "
                        + "less than zero")
        void testUpdatePurchaseUnitPriceLessThanZero() {
            when(tickerPurchaseRepository.findById(1)).thenReturn(Optional.of(tickerPurchase));
            when(tickerRepository.existsById(1)).thenReturn(true);

            tickerPurchase.setUnitPrice(new BigDecimal("-0.05"));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> tickerService.updatePurchase(tickerPurchase));

            verify(tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when updating a purchase with unit price "
                        + "equal to zero")
        void testUpdatePurchaseUnitPriceEqualToZero() {
            when(tickerPurchaseRepository.findById(1)).thenReturn(Optional.of(tickerPurchase));
            when(tickerRepository.existsById(1)).thenReturn(true);

            tickerPurchase.setUnitPrice(BigDecimal.ZERO);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> tickerService.updatePurchase(tickerPurchase));

            verify(tickerPurchaseRepository, never()).save(any(TickerPurchase.class));
        }
    }

    @Nested
    @DisplayName("Update Sale Tests")
    class UpdateSaleTests {

        private TickerSale tickerSale;

        @BeforeEach
        void setUp() {
            tickerSale =
                    TickerSale.builder()
                            .id(1)
                            .ticker(ticker1)
                            .quantity(ticker1.getCurrentQuantity())
                            .unitPrice(new BigDecimal("100"))
                            .walletTransaction(
                                    WalletTransaction.builder()
                                            .id(1)
                                            .amount(
                                                    ticker1.getCurrentQuantity()
                                                            .multiply(new BigDecimal("100")))
                                            .build())
                            .build();
        }

        @Test
        @DisplayName("Test if sale quantity is updated successfully")
        void testUpdateSaleQuantity() {
            when(tickerSaleRepository.findById(1)).thenReturn(Optional.of(tickerSale));
            when(tickerRepository.existsById(1)).thenReturn(true);

            BigDecimal oldQuantity = tickerSale.getQuantity();

            BigDecimal newQuantity;

            if (oldQuantity.compareTo(BigDecimal.ONE) == 0) {
                newQuantity = BigDecimal.TEN;
            } else {
                newQuantity = BigDecimal.ONE;
            }

            BigDecimal expectedAmountAfterUpdate = newQuantity.multiply(tickerSale.getUnitPrice());

            tickerSale.setQuantity(newQuantity);

            tickerService.updateSale(tickerSale);

            ArgumentCaptor<TickerSale> saleCaptor = ArgumentCaptor.forClass(TickerSale.class);
            verify(tickerSaleRepository).save(saleCaptor.capture());

            assertEquals(newQuantity, saleCaptor.getValue().getQuantity());
            assertEquals(
                    expectedAmountAfterUpdate,
                    saleCaptor.getValue().getWalletTransaction().getAmount());
        }

        @Test
        @DisplayName("Test if exception is thrown when updating a non-existent sale")
        void testUpdateSaleNotFound() {
            when(tickerSaleRepository.findById(1)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> tickerService.updateSale(tickerSale));

            verify(tickerSaleRepository, never()).save(any(TickerSale.class));
        }

        @Test
        @DisplayName("Test if exception is thrown when updating a sale with ticker not found")
        void testUpdateSaleTickerNotFound() {
            when(tickerSaleRepository.findById(1)).thenReturn(Optional.of(tickerSale));
            when(tickerRepository.existsById(1)).thenReturn(false);

            assertThrows(EntityNotFoundException.class, () -> tickerService.updateSale(tickerSale));

            verify(tickerSaleRepository, never()).save(any(TickerSale.class));
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when updating a sale with quantity "
                        + "less than zero")
        void testUpdateSaleQuantityLessThanZero() {
            when(tickerSaleRepository.findById(1)).thenReturn(Optional.of(tickerSale));
            when(tickerRepository.existsById(1)).thenReturn(true);

            tickerSale.setQuantity(new BigDecimal("-0.05"));
            assertThrows(
                    IllegalArgumentException.class, () -> tickerService.updateSale(tickerSale));
            verify(tickerSaleRepository, never()).save(any(TickerSale.class));
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when updating a sale with unit price "
                        + "less than zero")
        void testUpdateSaleUnitPriceLessThanZero() {
            when(tickerSaleRepository.findById(1)).thenReturn(Optional.of(tickerSale));
            when(tickerRepository.existsById(1)).thenReturn(true);

            tickerSale.setQuantity(new BigDecimal("1"));
            tickerSale.setUnitPrice(new BigDecimal("-0.05"));

            assertThrows(
                    IllegalArgumentException.class, () -> tickerService.updateSale(tickerSale));

            verify(tickerSaleRepository, never()).save(any(TickerSale.class));
        }
    }

    @Nested
    @DisplayName("Update Dividend Tests")
    class UpdateDividendTests {

        private Dividend dividend;

        @BeforeEach
        void setUp() {
            dividend =
                    Dividend.builder()
                            .id(1)
                            .ticker(ticker1)
                            .walletTransaction(
                                    WalletTransaction.builder()
                                            .id(1)
                                            .amount(new BigDecimal("50"))
                                            .build())
                            .build();
        }

        @Test
        @DisplayName("Test if dividend amount is updated successfully")
        void testUpdateDividendAmount() {
            when(dividendRepository.findById(1)).thenReturn(Optional.of(dividend));
            when(tickerRepository.existsById(1)).thenReturn(true);

            BigDecimal oldAmount = dividend.getWalletTransaction().getAmount();

            BigDecimal newAmount;

            if (oldAmount.compareTo(BigDecimal.ONE) == 0) {
                newAmount = BigDecimal.TEN;
            } else {
                newAmount = BigDecimal.ONE;
            }

            dividend.getWalletTransaction().setAmount(newAmount);

            tickerService.updateDividend(dividend);

            ArgumentCaptor<Dividend> dividendCaptor = ArgumentCaptor.forClass(Dividend.class);
            verify(dividendRepository).save(dividendCaptor.capture());

            assertEquals(newAmount, dividendCaptor.getValue().getWalletTransaction().getAmount());
        }

        @Test
        @DisplayName("Test if exception is thrown when updating a non-existent dividend")
        void testUpdateDividendNotFound() {
            when(dividendRepository.findById(1)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class, () -> tickerService.updateDividend(dividend));

            verify(dividendRepository, never()).save(any(Dividend.class));
        }

        @Test
        @DisplayName("Test if exception is thrown when updating a dividend with ticker not found")
        void testUpdateDividendTickerNotFound() {
            when(dividendRepository.findById(1)).thenReturn(Optional.of(dividend));
            when(tickerRepository.existsById(1)).thenReturn(false);

            assertThrows(
                    EntityNotFoundException.class, () -> tickerService.updateDividend(dividend));

            verify(dividendRepository, never()).save(any(Dividend.class));
        }
    }

    @Nested
    @DisplayName("Update Crypto Exchange Tests")
    class UpdateCryptoExchangeTests {

        @Test
        @DisplayName("Test if crypto exchange sold ticker is updated successfully")
        void testUpdateCryptoExchangeSoldTicker() {
            // Currently: crypto1 -> crypto2
            // After: crypto3 -> crypto2
            BigDecimal exchangeSoldQuantity = exchangeCrypto1ToCrypto2.getSoldQuantity();
            BigDecimal crypto1OldQuantity = crypto1.getCurrentQuantity();
            BigDecimal crypto2OldQuantity = crypto2.getCurrentQuantity();
            BigDecimal crypto3OldQuantity = crypto3.getCurrentQuantity();

            CryptoExchange updatedCryptoExchange =
                    new CryptoExchange(
                            exchangeCrypto1ToCrypto2.getId(),
                            crypto3,
                            exchangeCrypto1ToCrypto2.getReceivedCrypto(),
                            exchangeCrypto1ToCrypto2.getSoldQuantity(),
                            exchangeCrypto1ToCrypto2.getReceivedQuantity(),
                            exchangeCrypto1ToCrypto2.getDate(),
                            exchangeCrypto1ToCrypto2.getDescription());

            when(cryptoExchangeRepository.findById(exchangeCrypto1ToCrypto2.getId()))
                    .thenReturn(Optional.of(exchangeCrypto1ToCrypto2));

            when(tickerRepository.findById(crypto2.getId())).thenReturn(Optional.of(crypto2));
            when(tickerRepository.findById(crypto3.getId())).thenReturn(Optional.of(crypto3));

            tickerService.updateCryptoExchange(updatedCryptoExchange);

            verify(cryptoExchangeRepository).save(exchangeCrypto1ToCrypto2);

            assertEquals(
                    0,
                    crypto1.getCurrentQuantity()
                            .compareTo(crypto1OldQuantity.add(exchangeSoldQuantity)));
            assertEquals(0, crypto2.getCurrentQuantity().compareTo(crypto2OldQuantity));
            assertEquals(
                    0,
                    crypto3.getCurrentQuantity()
                            .compareTo(crypto3OldQuantity.subtract(exchangeSoldQuantity)));
        }

        @Test
        @DisplayName("Test if crypto exchange received ticker is updated successfully")
        void testUpdateCryptoExchangeReceivedTicker() {
            // Currently: crypto1 -> crypto2
            // After: crypto1 -> crypto3
            BigDecimal exchangeQuantity = exchangeCrypto1ToCrypto2.getReceivedQuantity();
            BigDecimal crypto1OldQuantity = crypto1.getCurrentQuantity();
            BigDecimal crypto2OldQuantity = crypto2.getCurrentQuantity();
            BigDecimal crypto3OldQuantity = crypto3.getCurrentQuantity();

            CryptoExchange updatedCryptoExchange =
                    new CryptoExchange(
                            exchangeCrypto1ToCrypto2.getId(),
                            exchangeCrypto1ToCrypto2.getSoldCrypto(),
                            crypto3,
                            exchangeCrypto1ToCrypto2.getSoldQuantity(),
                            exchangeCrypto1ToCrypto2.getReceivedQuantity(),
                            exchangeCrypto1ToCrypto2.getDate(),
                            exchangeCrypto1ToCrypto2.getDescription());

            when(cryptoExchangeRepository.findById(exchangeCrypto1ToCrypto2.getId()))
                    .thenReturn(Optional.of(exchangeCrypto1ToCrypto2));

            when(tickerRepository.findById(crypto1.getId())).thenReturn(Optional.of(crypto1));
            when(tickerRepository.findById(crypto3.getId())).thenReturn(Optional.of(crypto3));

            tickerService.updateCryptoExchange(updatedCryptoExchange);

            verify(cryptoExchangeRepository).save(exchangeCrypto1ToCrypto2);

            assertEquals(0, crypto1.getCurrentQuantity().compareTo(crypto1OldQuantity));
            assertEquals(
                    0,
                    crypto2.getCurrentQuantity()
                            .compareTo(crypto2OldQuantity.subtract(exchangeQuantity)));
            assertEquals(
                    0,
                    crypto3.getCurrentQuantity()
                            .compareTo(crypto3OldQuantity.add(exchangeQuantity)));
        }

        @Test
        @DisplayName(
                "Should throw EntityNotFoundException when updating a non-existent crypto exchange")
        void testUpdateCryptoExchangeNotFound() {
            CryptoExchange updatedCryptoExchange =
                    new CryptoExchange(
                            999,
                            crypto1,
                            crypto2,
                            BigDecimal.ONE,
                            BigDecimal.ONE,
                            LocalDateTime.now(),
                            "");

            when(cryptoExchangeRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> tickerService.updateCryptoExchange(updatedCryptoExchange));

            verify(cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
        }

        @Test
        @DisplayName(
                "Should throw EntityNotFoundException when updating a crypto exchange with"
                        + " non-existent source ticker")
        void testUpdateCryptoExchangeSourceTickerNotFound() {
            CryptoExchange updatedCryptoExchange =
                    new CryptoExchange(
                            exchangeCrypto1ToCrypto2.getId(),
                            crypto1,
                            crypto2,
                            exchangeCrypto1ToCrypto2.getSoldQuantity(),
                            exchangeCrypto1ToCrypto2.getReceivedQuantity(),
                            exchangeCrypto1ToCrypto2.getDate(),
                            exchangeCrypto1ToCrypto2.getDescription());

            when(cryptoExchangeRepository.findById(exchangeCrypto1ToCrypto2.getId()))
                    .thenReturn(Optional.of(exchangeCrypto1ToCrypto2));
            when(tickerRepository.findById(crypto1.getId())).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> tickerService.updateCryptoExchange(updatedCryptoExchange));

            verify(cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
        }

        @Test
        @DisplayName(
                "Should throw EntityNotFoundException when updating a crypto exchange with"
                        + " non-existent target ticker")
        void testUpdateCryptoExchangeTargetTickerNotFound() {
            CryptoExchange updatedCryptoExchange =
                    new CryptoExchange(
                            exchangeCrypto1ToCrypto2.getId(),
                            crypto1,
                            crypto2,
                            exchangeCrypto1ToCrypto2.getSoldQuantity(),
                            exchangeCrypto1ToCrypto2.getReceivedQuantity(),
                            exchangeCrypto1ToCrypto2.getDate(),
                            exchangeCrypto1ToCrypto2.getDescription());

            when(cryptoExchangeRepository.findById(exchangeCrypto1ToCrypto2.getId()))
                    .thenReturn(Optional.of(exchangeCrypto1ToCrypto2));
            when(tickerRepository.findById(crypto1.getId())).thenReturn(Optional.of(crypto1));
            when(tickerRepository.findById(crypto2.getId())).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> tickerService.updateCryptoExchange(updatedCryptoExchange));

            verify(cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
        }

        @Test
        @DisplayName(
                "Should throw SameSourceDestinationException when updating a crypto exchange with"
                        + " same source and target ticker")
        void testUpdateCryptoExchangeSameSourceAndTargetTicker() {
            CryptoExchange updatedCryptoExchange =
                    new CryptoExchange(
                            exchangeCrypto1ToCrypto2.getId(),
                            crypto1,
                            crypto1,
                            exchangeCrypto1ToCrypto2.getSoldQuantity(),
                            exchangeCrypto1ToCrypto2.getReceivedQuantity(),
                            exchangeCrypto1ToCrypto2.getDate(),
                            exchangeCrypto1ToCrypto2.getDescription());

            when(cryptoExchangeRepository.findById(exchangeCrypto1ToCrypto2.getId()))
                    .thenReturn(Optional.of(exchangeCrypto1ToCrypto2));
            when(tickerRepository.findById(crypto1.getId())).thenReturn(Optional.of(crypto1));

            assertThrows(
                    MoinexException.SameSourceDestinationException.class,
                    () -> tickerService.updateCryptoExchange(updatedCryptoExchange));

            verify(cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
        }

        @Test
        @DisplayName(
                "Should throw IllegalArgumentException when updating a crypto exchange with sold"
                        + " quantity less than or equal to zero")
        void testUpdateCryptoExchangeInvalidSoldQuantity() {
            CryptoExchange updatedCryptoExchange =
                    new CryptoExchange(
                            exchangeCrypto1ToCrypto2.getId(),
                            crypto1,
                            crypto2,
                            BigDecimal.ZERO,
                            exchangeCrypto1ToCrypto2.getReceivedQuantity(),
                            exchangeCrypto1ToCrypto2.getDate(),
                            exchangeCrypto1ToCrypto2.getDescription());

            when(cryptoExchangeRepository.findById(exchangeCrypto1ToCrypto2.getId()))
                    .thenReturn(Optional.of(exchangeCrypto1ToCrypto2));
            when(tickerRepository.findById(crypto1.getId())).thenReturn(Optional.of(crypto1));
            when(tickerRepository.findById(crypto2.getId())).thenReturn(Optional.of(crypto2));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> tickerService.updateCryptoExchange(updatedCryptoExchange));

            verify(cryptoExchangeRepository, never()).save(any(CryptoExchange.class));
        }
    }
}
