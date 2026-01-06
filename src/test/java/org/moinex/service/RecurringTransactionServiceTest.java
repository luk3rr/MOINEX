package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.error.MoinexException;
import org.moinex.model.Category;
import org.moinex.model.enums.RecurringTransactionFrequency;
import org.moinex.model.enums.RecurringTransactionStatus;
import org.moinex.model.enums.TransactionType;
import org.moinex.model.wallettransaction.RecurringTransaction;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.repository.wallettransaction.RecurringTransactionRepository;
import org.moinex.repository.wallettransaction.WalletRepository;
import org.moinex.util.Constants;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceTest {

    @Mock private RecurringTransactionRepository recurringTransactionRepository;
    @Mock private WalletTransactionService walletTransactionService;
    @Mock private WalletRepository walletRepository;

    @InjectMocks private RecurringTransactionService recurringTransactionService;

    private Wallet wallet;
    private Category category;
    private RecurringTransaction recurringTransaction;

    @BeforeEach
    void setUp() {
        wallet = new Wallet(1, "Test Wallet", BigDecimal.valueOf(1000.0));
        category = Category.builder().id(1).name("Test Category").build();
        recurringTransaction =
                RecurringTransaction.builder()
                        .id(1)
                        .wallet(wallet)
                        .category(category)
                        .type(TransactionType.EXPENSE)
                        .amount(new BigDecimal("100.00"))
                        .startDate(
                                LocalDate.now()
                                        .plusDays(1)
                                        .atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME))
                        .endDate(
                                LocalDate.now()
                                        .plusMonths(2)
                                        .atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME))
                        .nextDueDate(
                                LocalDate.now()
                                        .plusDays(1)
                                        .atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME))
                        .frequency(RecurringTransactionFrequency.MONTHLY)
                        .description("Monthly Rent")
                        .status(RecurringTransactionStatus.ACTIVE)
                        .build();
    }

    @Nested
    @DisplayName("Add Recurring Transaction Tests")
    class AddTests {
        @Test
        @DisplayName("Should add recurring transaction successfully")
        void addRecurringTransaction_Success() {
            when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

            recurringTransactionService.addRecurringTransaction(
                    wallet.getId(),
                    category,
                    TransactionType.EXPENSE,
                    new BigDecimal("150.00"),
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusMonths(3),
                    "New recurring expense",
                    RecurringTransactionFrequency.MONTHLY);

            ArgumentCaptor<RecurringTransaction> captor =
                    ArgumentCaptor.forClass(RecurringTransaction.class);
            verify(recurringTransactionRepository).save(captor.capture());
            assertEquals("New recurring expense", captor.getValue().getDescription());
            assertEquals(new BigDecimal("150.00"), captor.getValue().getAmount());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for start date in the past")
        void addRecurringTransaction_StartDateInPast_ThrowsException() {
            when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            recurringTransactionService.addRecurringTransaction(
                                    wallet.getId(),
                                    category,
                                    TransactionType.EXPENSE,
                                    BigDecimal.TEN,
                                    LocalDate.now().minusDays(1),
                                    LocalDate.now().plusMonths(1),
                                    "Past transaction",
                                    RecurringTransactionFrequency.MONTHLY));
        }

        @DisplayName("Throws EntityNotFoundException when wallet does not exist")
        @Test
        void throwsException_WhenWalletDoesNotExist() {
            when(walletRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            recurringTransactionService.addRecurringTransaction(
                                    999,
                                    category,
                                    TransactionType.EXPENSE,
                                    BigDecimal.TEN,
                                    LocalDate.now(),
                                    LocalDate.now().plusMonths(1),
                                    "Non-existent wallet transaction",
                                    RecurringTransactionFrequency.MONTHLY));
        }

        @DisplayName("Throws IllegalArgumentException when start or end date is null")
        @Test
        void throwsException_WhenStartOrEndDateIsNull() {
            when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            recurringTransactionService.addRecurringTransaction(
                                    wallet.getId(),
                                    category,
                                    TransactionType.EXPENSE,
                                    BigDecimal.TEN,
                                    null,
                                    LocalDate.now().plusMonths(1),
                                    "Null start date transaction",
                                    RecurringTransactionFrequency.MONTHLY));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            recurringTransactionService.addRecurringTransaction(
                                    wallet.getId(),
                                    category,
                                    TransactionType.EXPENSE,
                                    BigDecimal.TEN,
                                    LocalDate.now(),
                                    null,
                                    "Null end date transaction",
                                    RecurringTransactionFrequency.MONTHLY));
        }

        @DisplayName("Throws IllegalArgumentException when amount is less than or equal to zero")
        @Test
        void throwsException_WhenAmountIsZeroOrNegative() {
            when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            recurringTransactionService.addRecurringTransaction(
                                    wallet.getId(),
                                    category,
                                    TransactionType.EXPENSE,
                                    BigDecimal.ZERO,
                                    LocalDate.now(),
                                    LocalDate.now().plusMonths(1),
                                    "Zero amount transaction",
                                    RecurringTransactionFrequency.MONTHLY));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            recurringTransactionService.addRecurringTransaction(
                                    wallet.getId(),
                                    category,
                                    TransactionType.EXPENSE,
                                    BigDecimal.valueOf(-10),
                                    LocalDate.now(),
                                    LocalDate.now().plusMonths(1),
                                    "Negative amount transaction",
                                    RecurringTransactionFrequency.MONTHLY));
        }
    }

    @Nested
    @DisplayName("Process Recurring Transactions Tests")
    class ProcessTests {
        @Test
        @DisplayName("Should create expense transaction when nextDueDate is in the past")
        void processRecurringExpenseTransactions_CreatesTransaction_WhenDueDateIsPassed() {
            recurringTransaction.setType(TransactionType.EXPENSE);
            recurringTransaction.setNextDueDate(LocalDateTime.now().minusDays(1));
            when(recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE))
                    .thenReturn(Collections.singletonList(recurringTransaction));

            recurringTransactionService.processRecurringTransactions();

            verify(walletTransactionService, times(1))
                    .addExpense(any(), any(), any(), any(), any(), any());
            verify(recurringTransactionRepository).save(recurringTransaction);
            assertTrue(recurringTransaction.getNextDueDate().isAfter(LocalDateTime.now()));
        }

        @Test
        @DisplayName("Should create income transaction when nextDueDate is in the past")
        void processRecurringIncomeTransactions_CreatesTransaction_WhenDueDateIsPassed() {
            recurringTransaction.setType(TransactionType.INCOME);
            recurringTransaction.setNextDueDate(LocalDateTime.now().minusDays(1));
            when(recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE))
                    .thenReturn(Collections.singletonList(recurringTransaction));

            recurringTransactionService.processRecurringTransactions();

            verify(walletTransactionService, times(1))
                    .addIncome(any(), any(), any(), any(), any(), any());
            verify(recurringTransactionRepository).save(recurringTransaction);
            assertTrue(recurringTransaction.getNextDueDate().isAfter(LocalDateTime.now()));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when transaction type is not recognized")
        void processRecurringTransactions_ThrowsException_WhenTypeIsNotRecognized() {
            TransactionType invalidType = mock(TransactionType.class);

            recurringTransaction.setType(invalidType);
            recurringTransaction.setNextDueDate(LocalDateTime.now().minusDays(1));
            when(recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE))
                    .thenReturn(Collections.singletonList(recurringTransaction));

            assertThrows(
                    IllegalStateException.class,
                    () -> recurringTransactionService.processRecurringTransactions());
        }

        @Test
        @DisplayName("Should not create transaction when nextDueDate is in the future")
        void processRecurringTransactions_DoesNotCreateTransaction_WhenDueDateIsInFuture() {
            recurringTransaction.setNextDueDate(LocalDateTime.now().plusDays(1));
            when(recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE))
                    .thenReturn(Collections.singletonList(recurringTransaction));

            recurringTransactionService.processRecurringTransactions();

            verify(walletTransactionService, never())
                    .addExpense(any(), any(), any(), any(), any(), any());
            verify(recurringTransactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should deactivate recurring transaction when it expires")
        void processRecurringTransactions_DeactivatesExpiredTransaction() {
            recurringTransaction.setNextDueDate(LocalDateTime.now().minusMonths(1));
            recurringTransaction.setEndDate(LocalDateTime.now().minusDays(1));
            when(recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE))
                    .thenReturn(Collections.singletonList(recurringTransaction));

            recurringTransactionService.processRecurringTransactions();

            verify(walletTransactionService, never())
                    .addExpense(any(), any(), any(), any(), any(), any());

            ArgumentCaptor<RecurringTransaction> captor =
                    ArgumentCaptor.forClass(RecurringTransaction.class);
            verify(recurringTransactionRepository).save(captor.capture());
            assertEquals(RecurringTransactionStatus.INACTIVE, captor.getValue().getStatus());
        }
    }

    @Nested
    @DisplayName("Get Future Transactions Tests")
    class GetFutureTransactionsTests {
        @Test
        @DisplayName("Should generate correct future transactions for a given month")
        void getFutureTransactionsByMonth_Success() {
            recurringTransaction.setFrequency(RecurringTransactionFrequency.WEEKLY);
            recurringTransaction.setNextDueDate(
                    LocalDate.now()
                            .withDayOfMonth(1)
                            .atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME));
            when(recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE))
                    .thenReturn(Collections.singletonList(recurringTransaction));

            List<WalletTransaction> futureTransactions =
                    recurringTransactionService.getFutureTransactionsByMonth(
                            YearMonth.now(), YearMonth.now());

            assertTrue(futureTransactions.size() >= 4 && futureTransactions.size() <= 5);
            assertEquals(recurringTransaction.getAmount(), futureTransactions.get(0).getAmount());
        }
    }

    @Nested
    @DisplayName("Update Recurring Transaction Tests")
    class UpdateTests {
        @Test
        @DisplayName("Should update recurring transaction successfully")
        void updateRecurringTransaction_Success() {
            RecurringTransaction updatedData =
                    RecurringTransaction.builder()
                            .id(recurringTransaction.getId())
                            .amount(new BigDecimal("200.00"))
                            .description("Updated Description")
                            .startDate(recurringTransaction.getStartDate())
                            .endDate(recurringTransaction.getEndDate().plusMonths(1))
                            .nextDueDate(recurringTransaction.getNextDueDate())
                            .frequency(RecurringTransactionFrequency.WEEKLY)
                            .build();

            when(recurringTransactionRepository.findById(recurringTransaction.getId()))
                    .thenReturn(Optional.of(recurringTransaction));

            recurringTransactionService.updateRecurringTransaction(updatedData);

            ArgumentCaptor<RecurringTransaction> captor =
                    ArgumentCaptor.forClass(RecurringTransaction.class);
            verify(recurringTransactionRepository).save(captor.capture());
            RecurringTransaction saved = captor.getValue();

            assertEquals("Updated Description", saved.getDescription());
            assertEquals(new BigDecimal("200.00"), saved.getAmount());
            assertEquals(RecurringTransactionFrequency.WEEKLY, saved.getFrequency());
        }

        @Test
        @DisplayName(
                "Should throw EntityNotFoundException when updating a non-existent transaction")
        void updateRecurringTransaction_NotFound_ThrowsException() {
            when(recurringTransactionRepository.findById(anyInt())).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            recurringTransactionService.updateRecurringTransaction(
                                    recurringTransaction));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when amount is zero or negative")
        void updateRecurringTransaction_ZeroOrNegativeAmount_ThrowsException() {
            RecurringTransaction invalidTransaction =
                    RecurringTransaction.builder()
                            .id(recurringTransaction.getId())
                            .amount(BigDecimal.ZERO)
                            .description("Invalid Amount")
                            .startDate(recurringTransaction.getStartDate())
                            .endDate(recurringTransaction.getEndDate())
                            .nextDueDate(recurringTransaction.getNextDueDate())
                            .frequency(RecurringTransactionFrequency.MONTHLY)
                            .build();

            when(recurringTransactionRepository.findById(recurringTransaction.getId()))
                    .thenReturn(Optional.of(recurringTransaction));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            recurringTransactionService.updateRecurringTransaction(
                                    invalidTransaction));

            invalidTransaction.setAmount(BigDecimal.valueOf(-100));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            recurringTransactionService.updateRecurringTransaction(
                                    invalidTransaction));
        }

        @Test
        @DisplayName(
                "Should throw IllegalArgumentException when start end date is before start date")
        void updateRecurringTransaction_StartDateInPast_ThrowsException() {
            RecurringTransaction invalidTransaction =
                    RecurringTransaction.builder()
                            .id(recurringTransaction.getId())
                            .amount(new BigDecimal("150.00"))
                            .description("Invalid Start Date")
                            .startDate(recurringTransaction.getStartDate())
                            .endDate(recurringTransaction.getStartDate().minusDays(10))
                            .nextDueDate(recurringTransaction.getNextDueDate())
                            .frequency(RecurringTransactionFrequency.MONTHLY)
                            .build();

            when(recurringTransactionRepository.findById(recurringTransaction.getId()))
                    .thenReturn(Optional.of(recurringTransaction));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            recurringTransactionService.updateRecurringTransaction(
                                    invalidTransaction));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when frequency is invalid")
        void updateRecurringTransaction_InvalidFrequency_ThrowsException() {
            RecurringTransactionFrequency invalidFrequency =
                    mock(RecurringTransactionFrequency.class);

            RecurringTransaction invalidTransaction =
                    RecurringTransaction.builder()
                            .id(recurringTransaction.getId())
                            .amount(new BigDecimal("150.00"))
                            .description("Invalid Frequency")
                            .startDate(recurringTransaction.getStartDate())
                            .endDate(recurringTransaction.getEndDate())
                            .nextDueDate(recurringTransaction.getNextDueDate())
                            .frequency(invalidFrequency)
                            .build();

            when(recurringTransactionRepository.findById(recurringTransaction.getId()))
                    .thenReturn(Optional.of(recurringTransaction));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            recurringTransactionService.updateRecurringTransaction(
                                    invalidTransaction));
        }

        @Test
        @DisplayName(
                "Should throw IllegalArgumentException when end date is not at least one interval"
                        + " after start date")
        void testEndDateNotAtLeastOneIntervalAfterStartDate() {
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusDays(1);
            RecurringTransactionFrequency frequency = RecurringTransactionFrequency.DAILY;

            RecurringTransaction invalidTransaction =
                    RecurringTransaction.builder()
                            .id(recurringTransaction.getId())
                            .amount(new BigDecimal("150.00"))
                            .description("Invalid End Date")
                            .startDate(
                                    startDate.atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME))
                            .endDate(endDate.atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME))
                            .nextDueDate(
                                    startDate.atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME))
                            .frequency(frequency)
                            .build();

            when(recurringTransactionRepository.findById(recurringTransaction.getId()))
                    .thenReturn(Optional.of(recurringTransaction));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            recurringTransactionService.updateRecurringTransaction(
                                    invalidTransaction));
        }
    }

    @Nested
    @DisplayName("Lifecycle Management Tests")
    class LifecycleTests {
        @Test
        @DisplayName("Should stop a recurring transaction")
        void stopRecurringTransaction_Success() {
            when(recurringTransactionRepository.findById(recurringTransaction.getId()))
                    .thenReturn(Optional.of(recurringTransaction));

            recurringTransactionService.stopRecurringTransaction(recurringTransaction.getId());

            ArgumentCaptor<RecurringTransaction> captor =
                    ArgumentCaptor.forClass(RecurringTransaction.class);
            verify(recurringTransactionRepository).save(captor.capture());
            assertEquals(RecurringTransactionStatus.INACTIVE, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("Should delete a recurring transaction")
        void deleteRecurringTransaction_Success() {
            when(recurringTransactionRepository.findById(recurringTransaction.getId()))
                    .thenReturn(Optional.of(recurringTransaction));

            recurringTransactionService.deleteRecurringTransaction(recurringTransaction.getId());

            verify(recurringTransactionRepository).delete(recurringTransaction);
        }

        @DisplayName(
                "Throws EntityNotFoundException when deleting a recurring transaction ID does not"
                        + " exist")
        @Test
        void throwsException_WhenRecurringTransactionIdDoesNotExist() {
            when(recurringTransactionRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> recurringTransactionService.deleteRecurringTransaction(999));
        }

        @DisplayName(
                "Throws EntityNotFoundException when stopping recurring transaction does not exist")
        @Test
        void throwsException_WhenRecurringTransactionDoesNotExist() {
            when(recurringTransactionRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> recurringTransactionService.stopRecurringTransaction(999));
        }

        @DisplayName(
                "Throws AttributeAlreadySetException when recurring transaction is already"
                        + " inactive")
        @Test
        void throwsException_WhenRecurringTransactionAlreadyInactive() {
            RecurringTransaction inactiveTransaction =
                    RecurringTransaction.builder()
                            .id(1)
                            .status(RecurringTransactionStatus.INACTIVE)
                            .build();

            when(recurringTransactionRepository.findById(1))
                    .thenReturn(Optional.of(inactiveTransaction));

            assertThrows(
                    MoinexException.AttributeAlreadySetException.class,
                    () -> recurringTransactionService.stopRecurringTransaction(1));
        }

        @DisplayName("Stops recurring transaction successfully when it is active")
        @Test
        void stopsRecurringTransaction_Successfully_WhenActive() {
            RecurringTransaction activeTransaction =
                    RecurringTransaction.builder()
                            .id(1)
                            .status(RecurringTransactionStatus.ACTIVE)
                            .build();

            when(recurringTransactionRepository.findById(1))
                    .thenReturn(Optional.of(activeTransaction));

            recurringTransactionService.stopRecurringTransaction(1);

            ArgumentCaptor<RecurringTransaction> captor =
                    ArgumentCaptor.forClass(RecurringTransaction.class);
            verify(recurringTransactionRepository).save(captor.capture());
            assertEquals(RecurringTransactionStatus.INACTIVE, captor.getValue().getStatus());
        }
    }

    @Nested
    @DisplayName("Get Last Transaction Date Tests")
    class GetLastTransactionDateTests {

        @Test
        @DisplayName("Should calculate last date correctly for DAILY frequency")
        void getLastTransactionDate_Daily() {
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(10);

            LocalDate lastDate =
                    recurringTransactionService.getLastTransactionDate(
                            startDate, endDate, RecurringTransactionFrequency.DAILY);

            assertEquals(LocalDate.now().plusDays(10), lastDate);
        }

        @Test
        @DisplayName("Should calculate last date correctly for MONTHLY frequency")
        void getLastTransactionDate_Monthly() {
            LocalDate startDate = LocalDate.now().plusDays(1);
            LocalDate endDate = startDate.plusMonths(5).plusDays(10);
            LocalDate expectedDate = startDate.plusMonths(5);

            LocalDate lastDate =
                    recurringTransactionService.getLastTransactionDate(
                            startDate, endDate, RecurringTransactionFrequency.MONTHLY);

            assertEquals(expectedDate, lastDate);
        }

        @Test
        @DisplayName("Should calculate last date correctly for WEEKLY frequency")
        void getLastTransactionDate_Weekly() {
            LocalDate startDate = LocalDate.now().plusDays(1);
            LocalDate endDate = startDate.plusWeeks(3).plusDays(3);
            LocalDate expectedDate = startDate.plusWeeks(3);

            LocalDate lastDate =
                    recurringTransactionService.getLastTransactionDate(
                            startDate, endDate, RecurringTransactionFrequency.WEEKLY);

            assertEquals(expectedDate, lastDate);
        }

        @Test
        @DisplayName("Should calculate last date correctly for YEARLY frequency")
        void getLastTransactionDate_Yearly() {
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusYears(1);

            LocalDate lastDate =
                    recurringTransactionService.getLastTransactionDate(
                            startDate, endDate, RecurringTransactionFrequency.YEARLY);

            assertEquals(LocalDate.now().plusYears(1), lastDate);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when start date is in the past")
        void getLastTransactionDate_StartDateInPast_ThrowsException() {
            LocalDate startDate = LocalDate.now().minusDays(1);
            LocalDate endDate = LocalDate.now().plusMonths(1);

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            recurringTransactionService.getLastTransactionDate(
                                    startDate, endDate, RecurringTransactionFrequency.MONTHLY));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid date interval")
        void getLastTransactionDate_InvalidInterval_ThrowsException() {
            LocalDate startDate = LocalDate.now().plusDays(1);
            LocalDate endDate = startDate.plusDays(20);

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            recurringTransactionService.getLastTransactionDate(
                                    startDate, endDate, RecurringTransactionFrequency.MONTHLY));
        }
    }

    @Nested
    @DisplayName("Get Future Transactions By Year Tests")
    class GetFutureTransactionsByYearTests {

        @Test
        @DisplayName("Should generate correct monthly transactions for the rest of the year")
        void getFutureTransactionsByYear_Monthly() {
            recurringTransaction.setNextDueDate(LocalDateTime.of(2025, 7, 15, 23, 59));
            recurringTransaction.setEndDate(LocalDateTime.of(2025, 12, 31, 23, 59));
            recurringTransaction.setFrequency(RecurringTransactionFrequency.MONTHLY);
            when(recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE))
                    .thenReturn(Collections.singletonList(recurringTransaction));

            List<WalletTransaction> futureTransactions =
                    recurringTransactionService.getFutureTransactionsByYear(
                            Year.of(2025), Year.of(2025));

            assertEquals(6, futureTransactions.size());
            assertEquals(LocalDateTime.of(2025, 7, 15, 0, 0), futureTransactions.get(0).getDate());
            assertEquals(
                    LocalDateTime.of(2025, 12, 15, 23, 59, 59),
                    futureTransactions.get(5).getDate());
        }

        @Test
        @DisplayName("Should not generate transactions if next due date is after the end year")
        void getFutureTransactionsByYear_StartsAfterEndYear() {
            recurringTransaction.setNextDueDate(LocalDateTime.of(2026, 1, 15, 23, 59));
            when(recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE))
                    .thenReturn(Collections.singletonList(recurringTransaction));

            List<WalletTransaction> futureTransactions =
                    recurringTransactionService.getFutureTransactionsByYear(
                            Year.of(2025), Year.of(2025));

            assertTrue(futureTransactions.isEmpty());
        }

        @Test
        @DisplayName("Should not generate transactions if end date has already passed")
        void getFutureTransactionsByYear_EndDatePassed() {
            recurringTransaction.setNextDueDate(LocalDateTime.of(2025, 1, 15, 23, 59));
            recurringTransaction.setEndDate(LocalDateTime.of(2024, 12, 31, 23, 59));
            when(recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE))
                    .thenReturn(Collections.singletonList(recurringTransaction));

            List<WalletTransaction> futureTransactions =
                    recurringTransactionService.getFutureTransactionsByYear(
                            Year.of(2025), Year.of(2025));

            assertTrue(futureTransactions.isEmpty());
        }

        @Test
        @DisplayName("Should handle multiple recurring transactions with different frequencies")
        void getFutureTransactionsByYear_MultipleTransactions() {
            RecurringTransaction weekly =
                    RecurringTransaction.builder()
                            .nextDueDate(LocalDateTime.of(2025, 12, 20, 23, 59))
                            .endDate(LocalDateTime.of(2026, 1, 10, 23, 59))
                            .frequency(RecurringTransactionFrequency.WEEKLY)
                            .build();

            RecurringTransaction monthly =
                    RecurringTransaction.builder()
                            .nextDueDate(LocalDateTime.of(2025, 11, 15, 23, 59))
                            .endDate(LocalDateTime.of(2026, 2, 1, 23, 59))
                            .frequency(RecurringTransactionFrequency.MONTHLY)
                            .build();

            when(recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE))
                    .thenReturn(List.of(weekly, monthly));

            List<WalletTransaction> futureTransactions =
                    recurringTransactionService.getFutureTransactionsByYear(
                            Year.of(2025), Year.of(2025));

            assertEquals(4, futureTransactions.size());
        }
    }

    @Nested
    @DisplayName("Calculate Expected Remaining Amount Tests")
    class CalculateExpectedRemainingAmountTests {

        @Test
        @DisplayName(
                "Should calculate the correct remaining amount for a finite recurring transaction")
        void calculateExpectedRemainingAmount_Success() {
            recurringTransaction.setNextDueDate(LocalDateTime.now().minusMonths(1));
            recurringTransaction.setEndDate(LocalDateTime.now().plusMonths(2).withDayOfMonth(28));
            when(recurringTransactionRepository.findById(recurringTransaction.getId()))
                    .thenReturn(Optional.of(recurringTransaction));

            Double remainingAmount =
                    recurringTransactionService.calculateExpectedRemainingAmount(
                            recurringTransaction.getId());

            assertEquals(400.0, remainingAmount);
        }

        @Test
        @DisplayName(
                "Should return Double.POSITIVE_INFINITY when the end date is the default constant")
        void calculateExpectedRemainingAmount_Infinite_WhenDefaultEndDate() {
            recurringTransaction.setEndDate(
                    Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE.atTime(
                            Constants.RECURRING_TRANSACTION_DEFAULT_TIME));
            when(recurringTransactionRepository.findById(recurringTransaction.getId()))
                    .thenReturn(Optional.of(recurringTransaction));

            Double remainingAmount =
                    recurringTransactionService.calculateExpectedRemainingAmount(
                            recurringTransaction.getId());

            assertEquals(Double.POSITIVE_INFINITY, remainingAmount);
        }

        @Test
        @DisplayName("Should return 0.0 when the end date is in the past")
        void calculateExpectedRemainingAmount_ReturnsZero_WhenEndDateIsInPast() {
            recurringTransaction.setEndDate(LocalDateTime.now().minusDays(1));
            when(recurringTransactionRepository.findById(recurringTransaction.getId()))
                    .thenReturn(Optional.of(recurringTransaction));

            Double remainingAmount =
                    recurringTransactionService.calculateExpectedRemainingAmount(
                            recurringTransaction.getId());

            assertEquals(0.0, remainingAmount);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when the transaction does not exist")
        void calculateExpectedRemainingAmount_NotFound_ThrowsException() {
            when(recurringTransactionRepository.findById(anyInt())).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> recurringTransactionService.calculateExpectedRemainingAmount(999));
        }
    }
}
