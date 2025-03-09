/*
 * Filename: RecurringTransactionServiceTest.java
 * Created on: November 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.entities.Category;
import org.moinex.entities.wallettransaction.RecurringTransaction;
import org.moinex.entities.wallettransaction.Wallet;
import org.moinex.entities.wallettransaction.WalletTransaction;
import org.moinex.exceptions.AttributeAlreadySetException;
import org.moinex.repositories.wallettransaction.RecurringTransactionRepository;
import org.moinex.repositories.wallettransaction.WalletRepository;
import org.moinex.util.Constants;
import org.moinex.util.enums.RecurringTransactionFrequency;
import org.moinex.util.enums.RecurringTransactionStatus;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceTest
{
    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private WalletTransactionService walletTransactionService;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private RecurringTransactionService recurringTransactionService;

    private Wallet        wallet;
    private Wallet        wallet2;
    private Category      category;
    private Category      category2;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime nextDueDate;

    private RecurringTransaction dailyRT;
    private RecurringTransaction weeklyRecurringTransaction;
    private RecurringTransaction monthlyRecurringTransaction;
    private RecurringTransaction yearlyRecurringTransaction;

    private RecurringTransaction
    createRecurringTransaction(Long                          id,
                               Wallet                        wallet,
                               Category                      category,
                               TransactionType               type,
                               BigDecimal                    amount,
                               LocalDateTime                 startDate,
                               LocalDateTime                 endDate,
                               LocalDateTime                 nextDueDate,
                               RecurringTransactionFrequency frequency,
                               String                        description)
    {
        RecurringTransaction recurringTransaction =
            new RecurringTransaction(id,
                                     wallet,
                                     category,
                                     type,
                                     amount,
                                     startDate,
                                     endDate,
                                     nextDueDate,
                                     frequency,
                                     description);

        return recurringTransaction;
    }

    @BeforeEach
    void beforeEach()
    {
        wallet  = new Wallet(1L, "Wallet", BigDecimal.valueOf(1000.0));
        wallet2 = new Wallet(2L, "Wallet 2", BigDecimal.valueOf(500.0));

        category  = Category.builder().name("c1").build();
        category2 = Category.builder().name("c2").build();

        startDate =
            LocalDateTime.now().with(Constants.RECURRING_TRANSACTION_DEFAULT_TIME);
        endDate = LocalDateTime.now().plusMonths(1).with(
            Constants.RECURRING_TRANSACTION_DEFAULT_TIME);
        nextDueDate = LocalDateTime.now().with(
            Constants.RECURRING_TRANSACTION_DUE_DATE_DEFAULT_TIME);

        dailyRT = createRecurringTransaction(1L,
                                             wallet,
                                             category,
                                             TransactionType.EXPENSE,
                                             BigDecimal.valueOf(100.0),
                                             startDate,
                                             endDate,
                                             nextDueDate,
                                             RecurringTransactionFrequency.DAILY,
                                             "Daily transaction");

        weeklyRecurringTransaction =
            createRecurringTransaction(2L,
                                       wallet,
                                       category,
                                       TransactionType.EXPENSE,
                                       BigDecimal.valueOf(100.0),
                                       startDate,
                                       endDate,
                                       nextDueDate,
                                       RecurringTransactionFrequency.WEEKLY,
                                       "Weekly transaction");

        monthlyRecurringTransaction =
            createRecurringTransaction(3L,
                                       wallet,
                                       category,
                                       TransactionType.EXPENSE,
                                       BigDecimal.valueOf(100.0),
                                       startDate,
                                       endDate,
                                       nextDueDate,
                                       RecurringTransactionFrequency.MONTHLY,
                                       "Monthly transaction");

        yearlyRecurringTransaction =
            createRecurringTransaction(4L,
                                       wallet,
                                       category,
                                       TransactionType.EXPENSE,
                                       BigDecimal.valueOf(100.0),
                                       startDate,
                                       endDate,
                                       nextDueDate,
                                       RecurringTransactionFrequency.YEARLY,
                                       "Yearly transaction");
    }

    @Test
    @DisplayName("Test if the recurring transactions are created successfully")
    void testCreateRecurringTransaction()
    {
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        recurringTransactionService.addRecurringTransaction(
            dailyRT.getWallet().getId(),
            dailyRT.getCategory(),
            dailyRT.getType(),
            dailyRT.getAmount(),
            dailyRT.getStartDate().toLocalDate(),
            dailyRT.getEndDate().toLocalDate(),
            dailyRT.getDescription(),
            dailyRT.getFrequency());

        // Capture the recurring transaction that was saved
        ArgumentCaptor<RecurringTransaction> recurringTransactionCaptor =
            ArgumentCaptor.forClass(RecurringTransaction.class);

        verify(recurringTransactionRepository)
            .save(recurringTransactionCaptor.capture());

        assertEquals(dailyRT.getWallet(),
                     recurringTransactionCaptor.getValue().getWallet());
        assertEquals(dailyRT.getCategory(),
                     recurringTransactionCaptor.getValue().getCategory());
        assertEquals(dailyRT.getType(),
                     recurringTransactionCaptor.getValue().getType());
        assertEquals(dailyRT.getAmount(),
                     recurringTransactionCaptor.getValue().getAmount());
        assertEquals(dailyRT.getStartDate(),
                     recurringTransactionCaptor.getValue().getStartDate());
        assertEquals(dailyRT.getEndDate(),
                     recurringTransactionCaptor.getValue().getEndDate());
        assertEquals(dailyRT.getDescription(),
                     recurringTransactionCaptor.getValue().getDescription());
        assertEquals(dailyRT.getStatus(),
                     recurringTransactionCaptor.getValue().getStatus());
        assertEquals(dailyRT.getFrequency(),
                     recurringTransactionCaptor.getValue().getFrequency());
    }

    @Test
    @DisplayName("Test if the recurring transactions is not created when the wallet "
                 + "is not found")
    void
    testCreateRecurringTransactionWalletNotFound()
    {
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                     ()
                         -> recurringTransactionService.addRecurringTransaction(
                             dailyRT.getWallet().getId(),
                             dailyRT.getCategory(),
                             dailyRT.getType(),
                             dailyRT.getAmount(),
                             dailyRT.getStartDate().toLocalDate(),
                             dailyRT.getEndDate().toLocalDate(),
                             dailyRT.getDescription(),
                             dailyRT.getFrequency()));

        verify(recurringTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the recurring transactions are stopped successfully")
    void testStopRecurringTransaction()
    {
        when(recurringTransactionRepository.findById(dailyRT.getId()))
            .thenReturn(Optional.of(dailyRT));

        // Change the end date to a date in the future
        dailyRT.setEndDate(LocalDateTime.now().plusDays(40));

        recurringTransactionService.stopRecurringTransaction(dailyRT.getId());

        // Capture the recurring transaction that was saved
        ArgumentCaptor<RecurringTransaction> recurringTransactionCaptor =
            ArgumentCaptor.forClass(RecurringTransaction.class);

        verify(recurringTransactionRepository)
            .save(recurringTransactionCaptor.capture());

        // Check if the status of the recurring transaction is INACTIVE
        assertEquals(recurringTransactionCaptor.getValue().getStatus(),
                     RecurringTransactionStatus.INACTIVE);
    }

    @Test
    @DisplayName("Test if the recurring transactions is not stopped when the "
                 + "recurring transaction is not found")
    void
    testStopRecurringTransactionNotFound()
    {
        when(recurringTransactionRepository.findById(dailyRT.getId()))
            .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                     ()
                         -> recurringTransactionService.stopRecurringTransaction(
                             dailyRT.getId()));

        verify(recurringTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the recurring transactions is not stopped when the "
                 + "recurring transaction has already ended")
    void
    testStopRecurringTransactionAlreadyEnded()
    {
        when(recurringTransactionRepository.findById(dailyRT.getId()))
            .thenReturn(Optional.of(dailyRT));

        // Change the end date to a date in the past
        dailyRT.setStatus(RecurringTransactionStatus.INACTIVE);

        assertThrows(AttributeAlreadySetException.class,
                     ()
                         -> recurringTransactionService.stopRecurringTransaction(
                             dailyRT.getId()));

        verify(recurringTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the recurring transactions are deleted successfully")
    void testDeleteRecurringTransaction()
    {
        when(recurringTransactionRepository.findById(dailyRT.getId()))
            .thenReturn(Optional.of(dailyRT));

        recurringTransactionService.deleteRecurringTransaction(dailyRT.getId());

        verify(recurringTransactionRepository).delete(dailyRT);
    }

    @Test
    @DisplayName("Test if the recurring transactions is not deleted when the "
                 + "recurring transaction is not found")
    void
    testDeleteRecurringTransactionNotFound()
    {
        when(recurringTransactionRepository.findById(dailyRT.getId()))
            .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                     ()
                         -> recurringTransactionService.deleteRecurringTransaction(
                             dailyRT.getId()));

        verify(recurringTransactionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Test if the recurring transactions are updated successfully")
    void testUpdateRecurringTransaction()
    {
        RecurringTransaction updatedRT =
            new RecurringTransaction(dailyRT.getId(),
                                     dailyRT.getWallet(),
                                     dailyRT.getCategory(),
                                     dailyRT.getType(),
                                     dailyRT.getAmount(),
                                     dailyRT.getStartDate(),
                                     dailyRT.getEndDate(),
                                     dailyRT.getNextDueDate(),
                                     dailyRT.getFrequency(),
                                     dailyRT.getDescription());

        when(recurringTransactionRepository.findById(updatedRT.getId()))
            .thenReturn(Optional.of(dailyRT));

        // Update the recurring transaction
        updatedRT.setWallet(dailyRT.getWallet().getId() == wallet.getId() ? wallet2
                                                                          : wallet);

        updatedRT.setCategory(
            updatedRT.getCategory().getName().equals(category.getName()) ? category2
                                                                         : category);

        updatedRT.setType(updatedRT.getType() == TransactionType.EXPENSE
                              ? TransactionType.INCOME
                              : TransactionType.EXPENSE);

        updatedRT.setAmount(BigDecimal.valueOf(200.0));
        updatedRT.setEndDate(updatedRT.getEndDate().plusDays(10));
        updatedRT.setNextDueDate(updatedRT.getNextDueDate().plusDays(10));
        updatedRT.setFrequency(updatedRT.getFrequency() ==
                                       RecurringTransactionFrequency.DAILY
                                   ? RecurringTransactionFrequency.WEEKLY
                                   : RecurringTransactionFrequency.DAILY);
        updatedRT.setDescription("Updated description");

        recurringTransactionService.updateRecurringTransaction(updatedRT);

        // Capture the recurring transaction that was saved
        ArgumentCaptor<RecurringTransaction> recurringTransactionCaptor =
            ArgumentCaptor.forClass(RecurringTransaction.class);

        verify(recurringTransactionRepository)
            .save(recurringTransactionCaptor.capture());

        assertEquals(updatedRT.getWallet(),
                     recurringTransactionCaptor.getValue().getWallet());
        assertEquals(updatedRT.getCategory(),
                     recurringTransactionCaptor.getValue().getCategory());
        assertEquals(updatedRT.getType(),
                     recurringTransactionCaptor.getValue().getType());
        assertEquals(updatedRT.getAmount(),
                     recurringTransactionCaptor.getValue().getAmount());
        assertEquals(updatedRT.getStartDate(),
                     recurringTransactionCaptor.getValue().getStartDate());
        assertEquals(updatedRT.getEndDate(),
                     recurringTransactionCaptor.getValue().getEndDate());
        assertEquals(updatedRT.getDescription(),
                     recurringTransactionCaptor.getValue().getDescription());
        assertEquals(updatedRT.getStatus(),
                     recurringTransactionCaptor.getValue().getStatus());
        assertEquals(updatedRT.getFrequency(),
                     recurringTransactionCaptor.getValue().getFrequency());
    }

    @Test
    @DisplayName("Test if the daily recurring transactions are processed correctly")
    void testProcessDailyRecurringTransaction()
    {
        LocalDateTime today =
            LocalDateTime.now().with(Constants.RECURRING_TRANSACTION_DEFAULT_TIME);

        dailyRT.setNextDueDate(today.minusDays(10));

        when(recurringTransactionRepository.findByStatus(
                 RecurringTransactionStatus.ACTIVE))
            .thenReturn(Collections.singletonList(dailyRT));

        recurringTransactionService.processRecurringTransactions();

        // Capture the dates of the transactions
        ArgumentCaptor<LocalDateTime> dateCaptor =
            ArgumentCaptor.forClass(LocalDateTime.class);

        verify(walletTransactionService, times(10))
            .addExpense(eq(dailyRT.getWallet().getId()),
                        eq(dailyRT.getCategory()),
                        dateCaptor.capture(),
                        eq(dailyRT.getAmount()),
                        eq(dailyRT.getDescription()),
                        eq(TransactionStatus.PENDING));

        // Get the captured dates
        List<LocalDateTime> capturedDates = dateCaptor.getAllValues();

        // Check if the captured dates correspond to the expected dates for each of the
        // 10 days
        for (int i = 0; i < 10; i++)
        {
            LocalDate expectedDate = today.minusDays(10 - i).toLocalDate();

            assertEquals(expectedDate,
                         capturedDates.get(i).toLocalDate(),
                         "The date of the transaction is not the expected one");
        }

        verify(recurringTransactionRepository, atLeastOnce()).save(dailyRT);
    }

    @Test
    @DisplayName("Test if the weekly recurring transactions are processed correctly")
    void testProcessWeeklyRecurringTransaction()
    {
        LocalDateTime today =
            LocalDateTime.now().with(Constants.RECURRING_TRANSACTION_DEFAULT_TIME);

        weeklyRecurringTransaction.setNextDueDate(today.minusWeeks(5));

        when(recurringTransactionRepository.findByStatus(
                 RecurringTransactionStatus.ACTIVE))
            .thenReturn(Collections.singletonList(weeklyRecurringTransaction));

        recurringTransactionService.processRecurringTransactions();

        // Capture the dates of the transactions
        ArgumentCaptor<LocalDateTime> dateCaptor =
            ArgumentCaptor.forClass(LocalDateTime.class);

        verify(walletTransactionService, times(5))
            .addExpense(eq(weeklyRecurringTransaction.getWallet().getId()),
                        eq(weeklyRecurringTransaction.getCategory()),
                        dateCaptor.capture(),
                        eq(weeklyRecurringTransaction.getAmount()),
                        eq(weeklyRecurringTransaction.getDescription()),
                        eq(TransactionStatus.PENDING));

        // Get the captured dates
        List<LocalDateTime> capturedDates = dateCaptor.getAllValues();

        // Check if the captured dates correspond to the expected dates for each of the
        // 2 weeks
        for (int i = 0; i < 5; i++)
        {
            LocalDate expectedDate = today.minusWeeks(5 - i).toLocalDate();

            assertEquals(expectedDate,
                         capturedDates.get(i).toLocalDate(),
                         "The date of the transaction is not the expected one");
        }

        verify(recurringTransactionRepository, atLeastOnce())
            .save(weeklyRecurringTransaction);
    }

    @Test
    @DisplayName("Test if the monthly recurring transactions are processed correctly")
    void testProcessMonthlyRecurringTransaction()
    {
        LocalDateTime today =
            LocalDateTime.now().with(Constants.RECURRING_TRANSACTION_DEFAULT_TIME);

        monthlyRecurringTransaction.setNextDueDate(today.minusMonths(12));

        when(recurringTransactionRepository.findByStatus(
                 RecurringTransactionStatus.ACTIVE))
            .thenReturn(Collections.singletonList(monthlyRecurringTransaction));

        recurringTransactionService.processRecurringTransactions();

        // Capture the dates of the transactions
        ArgumentCaptor<LocalDateTime> dateCaptor =
            ArgumentCaptor.forClass(LocalDateTime.class);

        verify(walletTransactionService, times(12))
            .addExpense(eq(monthlyRecurringTransaction.getWallet().getId()),
                        eq(monthlyRecurringTransaction.getCategory()),
                        dateCaptor.capture(),
                        eq(monthlyRecurringTransaction.getAmount()),
                        eq(monthlyRecurringTransaction.getDescription()),
                        eq(TransactionStatus.PENDING));

        // Get the captured dates
        List<LocalDateTime> capturedDates = dateCaptor.getAllValues();

        // Check if the captured dates correspond to the expected dates for each of the
        // 1 month
        for (int i = 0; i < 12; i++)
        {
            LocalDate expectedDate = today.minusMonths(12 - i).toLocalDate();

            assertEquals(expectedDate,
                         capturedDates.get(i).toLocalDate(),
                         "The date of the transaction is not the expected one");
        }

        verify(recurringTransactionRepository, atLeastOnce())
            .save(monthlyRecurringTransaction);
    }

    @Test
    @DisplayName("Test if the yearly recurring transactions are processed correctly")
    void testProcessYearlyRecurringTransaction()
    {
        LocalDateTime today =
            LocalDateTime.now().with(Constants.RECURRING_TRANSACTION_DEFAULT_TIME);

        yearlyRecurringTransaction.setNextDueDate(today.minusYears(5));

        when(recurringTransactionRepository.findByStatus(
                 RecurringTransactionStatus.ACTIVE))
            .thenReturn(Collections.singletonList(yearlyRecurringTransaction));

        recurringTransactionService.processRecurringTransactions();

        // Capture the dates of the transactions
        ArgumentCaptor<LocalDateTime> dateCaptor =
            ArgumentCaptor.forClass(LocalDateTime.class);

        verify(walletTransactionService, times(5))
            .addExpense(eq(yearlyRecurringTransaction.getWallet().getId()),
                        eq(yearlyRecurringTransaction.getCategory()),
                        dateCaptor.capture(),
                        eq(yearlyRecurringTransaction.getAmount()),
                        eq(yearlyRecurringTransaction.getDescription()),
                        eq(TransactionStatus.PENDING));

        // Get the captured dates
        List<LocalDateTime> capturedDates = dateCaptor.getAllValues();

        // Check if the captured dates correspond to the expected dates for each of the
        // 1 year
        for (int i = 0; i < 5; i++)
        {
            LocalDate expectedDate = today.minusYears(5 - i).toLocalDate();

            assertEquals(expectedDate,
                         capturedDates.get(i).toLocalDate(),
                         "The date of the transaction is not the expected one");
        }

        verify(recurringTransactionRepository, atLeastOnce())
            .save(yearlyRecurringTransaction);
    }

    @Test
    @DisplayName(
        "Test if the active recurring transactions with end date in the past are "
        + "stopped")
    void
    testProcessRecurringTransactionEnds()
    {
        LocalDateTime today =
            LocalDateTime.now().with(Constants.RECURRING_TRANSACTION_DEFAULT_TIME);

        dailyRT.setNextDueDate(today.minusDays(10));
        dailyRT.setEndDate(today.minusDays(5));
        dailyRT.setStatus(RecurringTransactionStatus.ACTIVE);

        when(recurringTransactionRepository.findByStatus(
                 RecurringTransactionStatus.ACTIVE))
            .thenReturn(Collections.singletonList(dailyRT));

        recurringTransactionService.processRecurringTransactions();

        // Captures and check if the recurring transaction was saved with the status as
        // INACTIVE
        ArgumentCaptor<RecurringTransaction> captor =
            ArgumentCaptor.forClass(RecurringTransaction.class);
        verify(recurringTransactionRepository, times(1)).save(captor.capture());

        RecurringTransaction capturedTransaction = captor.getValue();

        assertEquals(RecurringTransactionStatus.INACTIVE,
                     capturedTransaction.getStatus());
    }

    @Test
    @DisplayName(
        "Test if get future recurring transactions by month returns the correct "
        + "transactions")
    void
    testGetFutureRecurringTransactionsByMonth()
    {
        YearMonth     november2011YearMonth = YearMonth.of(2011, 11);
        LocalDateTime november2011DateTime =
            LocalDate.of(2011, 11, 1)
                .atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME);

        Integer expectedTransactions = 0;

        dailyRT.setNextDueDate(november2011DateTime);
        expectedTransactions += november2011YearMonth.lengthOfMonth();

        weeklyRecurringTransaction.setNextDueDate(november2011DateTime);
        expectedTransactions += 5; // In 4 weeks there are 5 transactions, because it
                                   // includes the transaction of November 1st

        monthlyRecurringTransaction.setNextDueDate(november2011DateTime);
        expectedTransactions += 1;

        yearlyRecurringTransaction.setNextDueDate(november2011DateTime);
        expectedTransactions += 1;

        when(recurringTransactionRepository.findByStatus(
                 RecurringTransactionStatus.ACTIVE))
            .thenReturn(List.of(dailyRT,
                                weeklyRecurringTransaction,
                                monthlyRecurringTransaction,
                                yearlyRecurringTransaction));

        List<WalletTransaction> futureRecurringTransactions =
            recurringTransactionService.getFutureTransactionsByMonth(
                november2011YearMonth,
                november2011YearMonth);

        assertEquals(expectedTransactions, futureRecurringTransactions.size());
    }

    @Test
    @DisplayName(
        "Test if get future recurring transactions by year returns the correct "
        + "transactions")
    void
    testGetFutureRecurringTransactionsByYear()
    {
        Year          year2011 = Year.of(2011);
        LocalDateTime january2011DateTime =
            LocalDate.of(2011, 1, 1)
                .atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME);

        Integer expectedTransactions = 0;

        dailyRT.setNextDueDate(january2011DateTime);
        expectedTransactions += 365;

        weeklyRecurringTransaction.setNextDueDate(january2011DateTime);
        expectedTransactions += 53; // In 52 weeks there are 53 transactions, because it
                                    // includes the transaction of January 1st

        monthlyRecurringTransaction.setNextDueDate(january2011DateTime);
        expectedTransactions += 12;

        yearlyRecurringTransaction.setNextDueDate(january2011DateTime);
        expectedTransactions += 1;

        when(recurringTransactionRepository.findByStatus(
                 RecurringTransactionStatus.ACTIVE))
            .thenReturn(List.of(dailyRT,
                                weeklyRecurringTransaction,
                                monthlyRecurringTransaction,
                                yearlyRecurringTransaction));

        List<WalletTransaction> futureRecurringTransactions =
            recurringTransactionService.getFutureTransactionsByYear(year2011, year2011);

        assertEquals(expectedTransactions, futureRecurringTransactions.size());
    }
}
