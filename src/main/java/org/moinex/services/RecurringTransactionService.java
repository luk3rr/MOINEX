/*
 * Filename: RecurringTransactionService.java
 * Created on: November 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.NoArgsConstructor;
import org.moinex.entities.Category;
import org.moinex.entities.RecurringTransaction;
import org.moinex.entities.Wallet;
import org.moinex.entities.WalletTransaction;
import org.moinex.repositories.RecurringTransactionRepository;
import org.moinex.repositories.WalletRepository;
import org.moinex.util.Constants;
import org.moinex.util.RecurringTransactionFrequency;
import org.moinex.util.RecurringTransactionStatus;
import org.moinex.util.TransactionStatus;
import org.moinex.util.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing the recurring transactions
 */
@Service
@NoArgsConstructor
public class RecurringTransactionService
{
    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private WalletTransactionService walletTransactionService;

    @Autowired
    private WalletRepository walletRepository;

    private static final Logger m_logger =
        LoggerFactory.getLogger(RecurringTransactionService.class);

    /**
     * Validate start and end dates for editing a recurring transaction
     *
     * @param startDate The start date
     * @param endDate The end date
     * @param frequency The frequency of the recurring transaction
     * @throws RuntimeException If dates or intervals are invalid
     */
    private void validateDateAndIntervalForEdit(LocalDate                     startDate,
                                                LocalDate                     endDate,
                                                RecurringTransactionFrequency frequency)
    {
        if (endDate.isBefore(startDate))
        {
            throw new RuntimeException("End date cannot be before start date.");
        }

        Map<RecurringTransactionFrequency, TemporalUnit> frequencyUnits =
            Map.of(RecurringTransactionFrequency.DAILY,
                   ChronoUnit.DAYS,
                   RecurringTransactionFrequency.WEEKLY,
                   ChronoUnit.WEEKS,
                   RecurringTransactionFrequency.MONTHLY,
                   ChronoUnit.MONTHS,
                   RecurringTransactionFrequency.YEARLY,
                   ChronoUnit.YEARS);

        TemporalUnit unit = frequencyUnits.get(frequency);
        if (unit == null)
        {
            throw new RuntimeException("Invalid frequency type.");
        }

        LocalDate minimumEndDate = startDate.plus(1, unit);
        if (!minimumEndDate.isBefore(endDate) && !minimumEndDate.equals(endDate))
        {
            throw new RuntimeException(
                String.format("End date must be at least one %s after the start date.",
                              frequency.name().toLowerCase()));
        }
    }

    /**
     * Validate start and end dates for creating a recurring transaction.
     * Ensures start date is not in the past and reuses edit validation.
     *
     * @param startDate The start date
     * @param endDate The end date
     * @param frequency The frequency of the recurring transaction
     * @throws RuntimeException If dates or intervals are invalid
     */
    private void
    validateDateAndIntervalForCreate(LocalDate                     startDate,
                                     LocalDate                     endDate,
                                     RecurringTransactionFrequency frequency)
    {
        if (startDate.isBefore(LocalDate.now()))
        {
            throw new RuntimeException("Start date cannot be before today.");
        }

        validateDateAndIntervalForEdit(startDate, endDate, frequency);
    }

    /**
     * Create a recurring transaction
     * @param walletId The id of the wallet
     * @param category The category of the transaction
     * @param type The type of the transaction
     * @param amount The amount of the transaction
     * @param startDate The start date of the recurring transaction
     * @param description The description of the transaction
     * @param frequency The frequency of the recurring transaction
     * @return The id of the recurring transaction
     * @throws RuntimeException If the wallet is not found
     * @throws RuntimeException If the start date or end date is null
     * @throws RuntimeException If the amount is less than or equal to zero
     * @throws RuntimeException If the date and interval between start and end date is
     *     invalid
     * @throws RuntimeException If the frequency is invalid
     */
    @Transactional
    public Long addRecurringTransaction(Long                          walletId,
                                        Category                      category,
                                        TransactionType               type,
                                        BigDecimal                    amount,
                                        LocalDate                     startDate,
                                        String                        description,
                                        RecurringTransactionFrequency frequency)
    {
        LocalDate defaultEndDate = Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE;

        return addRecurringTransaction(walletId,
                                       category,
                                       type,
                                       amount,
                                       startDate,
                                       defaultEndDate,
                                       description,
                                       frequency);
    }

    /**
     * Create a recurring transaction
     * @param walletId The id of the wallet
     * @param category The category of the transaction
     * @param type The type of the transaction
     * @param amount The amount of the transaction
     * @param startDate The start date of the recurring transaction
     * @param description The description of the transaction
     * @param frequency The frequency of the recurring transaction
     * @return The id of the recurring transaction
     * @throws RuntimeException If the wallet is not found
     * @throws RuntimeException If the start date or end date is null
     * @throws RuntimeException If the amount is less than or equal to zero
     * @throws RuntimeException If the date and interval between start and end date is
     *     invalid
     * @throws RuntimeException If the frequency is invalid
     */
    @Transactional
    public Long addRecurringTransaction(Long                          walletId,
                                        Category                      category,
                                        TransactionType               type,
                                        BigDecimal                    amount,
                                        LocalDate                     startDate,
                                        LocalDate                     endDate,
                                        String                        description,
                                        RecurringTransactionFrequency frequency)
    {
        Wallet wt = walletRepository.findById(walletId).orElseThrow(
            () -> new RuntimeException("Wallet with id " + walletId + " not found"));

        if (startDate == null || endDate == null)
        {
            throw new RuntimeException("Start and end date cannot be null");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Amount must be greater than zero");
        }

        // Define the end date as the last second of the day
        LocalDateTime startDateWithTime =
            startDate.atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME);

        LocalDateTime endDateWithTime =
            endDate.atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME);

        // Ensure the date and interval between start and end date is valid
        validateDateAndIntervalForCreate(startDate, endDate, frequency);

        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                                                        .wallet(wt)
                                                        .category(category)
                                                        .type(type)
                                                        .amount(amount)
                                                        .startDate(startDateWithTime)
                                                        .endDate(endDateWithTime)
                                                        .nextDueDate(startDateWithTime)
                                                        .frequency(frequency)
                                                        .description(description)
                                                        .build();

        recurringTransactionRepository.save(recurringTransaction);

        m_logger.info("Created recurring transaction " + recurringTransaction.getId());

        return recurringTransaction.getId();
    }

    /**
     * Stops a recurring transaction
     * @param recurringTransactionId The id of the recurring transaction
     * @throws RuntimeException If the recurring transaction is not found
     */
    @Transactional
    public void stopRecurringTransaction(Long recurringTransactionId)
    {
        RecurringTransaction recurringTransaction =
            recurringTransactionRepository.findById(recurringTransactionId)
                .orElseThrow(
                    () -> new RuntimeException("Recurring transaction not found"));

        // Check if the recurring transaction has already ended
        if (recurringTransaction.getStatus().equals(
                RecurringTransactionStatus.INACTIVE))
        {
            throw new RuntimeException("Recurring transaction has already ended");
        }

        recurringTransaction.setStatus(RecurringTransactionStatus.INACTIVE);
        recurringTransactionRepository.save(recurringTransaction);

        m_logger.info("Stopped recurring transaction " + recurringTransaction.getId());
    }

    /**
     * Delete a recurring transaction
     * @param recurringTransactionId The id of the recurring transaction
     * @throws RuntimeException If the recurring transaction is not found
     */
    @Transactional
    public void deleteRecurringTransaction(Long recurringTransactionId)
    {
        RecurringTransaction recurringTransaction =
            recurringTransactionRepository.findById(recurringTransactionId)
                .orElseThrow(
                    ()
                        -> new RuntimeException("Recurring transaction with id " +
                                                recurringTransactionId + " not found"));

        recurringTransactionRepository.delete(recurringTransaction);

        m_logger.info("Deleted recurring transaction " + recurringTransaction.getId());
    }

    /**
     * Update a recurring transaction
     * @param rt The recurring transaction
     * @throws RuntimeException If the recurring transaction is not found
     * @throws RuntimeException If the start date or end date is null
     * @throws RuntimeException If the amount is less than or equal to zero
     * @throws RuntimeException If the date and interval between start and end date is
     *     invalid
     */
    @Transactional
    public void updateRecurringTransaction(RecurringTransaction rt)
    {
        RecurringTransaction rtToUpdate =
            recurringTransactionRepository.findById(rt.getId())
                .orElseThrow(
                    ()
                        -> new RuntimeException("Recurring transaction with id " +
                                                rt.getId() + " not found"));

        if (rt.getStartDate() == null || rt.getEndDate() == null)
        {
            throw new RuntimeException("Start and end date cannot be null");
        }

        if (rt.getAmount().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Amount must be greater than zero");
        }

        rt.setEndDate(
            rt.getEndDate().with(Constants.RECURRING_TRANSACTION_DEFAULT_TIME));

        // Ensure the date and interval between start and end date is valid
        validateDateAndIntervalForEdit(rtToUpdate.getStartDate().toLocalDate(),
                                       rt.getEndDate().toLocalDate(),
                                       rt.getFrequency());

        rtToUpdate.setWallet(rt.getWallet());
        rtToUpdate.setCategory(rt.getCategory());
        rtToUpdate.setType(rt.getType());
        rtToUpdate.setAmount(rt.getAmount());
        rtToUpdate.setEndDate(rt.getEndDate());
        rtToUpdate.setNextDueDate(rt.getNextDueDate());
        rtToUpdate.setDescription(rt.getDescription());
        rtToUpdate.setFrequency(rt.getFrequency());
        rtToUpdate.setStatus(rt.getStatus());

        recurringTransactionRepository.save(rtToUpdate);
        m_logger.info("Recurring transaction " + rt.getId() + " successfully updated");
    }

    /**
     * Process the recurring transactions
     * This method checks if the next due date of the recurring transactions has
     * already passed and generates the missing transactions
     */
    @Transactional
    public void processRecurringTransactions()
    {
        List<RecurringTransaction> activeRecurringTransactions =
            recurringTransactionRepository.findByStatus(
                RecurringTransactionStatus.ACTIVE);

        LocalDateTime today = LocalDateTime.now();

        for (RecurringTransaction recurring : activeRecurringTransactions)
        {
            LocalDateTime nextDueDate = recurring.getNextDueDate();

            // Check if the next due date has already passed and generate the missing
            // transactions
            if (!nextDueDate.isAfter(today) && !recurring.getEndDate().isBefore(today))
            {
                while (!nextDueDate.isAfter(today))
                {
                    createTransactionForDate(recurring, nextDueDate);

                    nextDueDate =
                        calculateNextDueDate(nextDueDate, recurring.getFrequency());
                }

                // Update the next due date in the recurring transaction
                recurring.setNextDueDate(nextDueDate);
                recurringTransactionRepository.save(recurring);
            }

            // Check if the recurring transaction has ended
            if (recurring.getEndDate().isBefore(today))
            {
                recurring.setStatus(RecurringTransactionStatus.INACTIVE);
                recurringTransactionRepository.save(recurring);
            }
        }
    }

    /**
     * Create a wallet transaction for a recurring transaction
     * @param recurring The recurring transaction
     * @param dueDate The due date of the transaction
     */
    private void createTransactionForDate(RecurringTransaction recurring,
                                          LocalDateTime        dueDate)
    {
        try
        {
            if (recurring.getType().equals(TransactionType.INCOME))
            {
                walletTransactionService.addIncome(recurring.getWallet().getId(),
                                                   recurring.getCategory(),
                                                   dueDate,
                                                   recurring.getAmount(),
                                                   recurring.getDescription(),
                                                   TransactionStatus.PENDING);
            }
            else if (recurring.getType().equals(TransactionType.EXPENSE))
            {
                walletTransactionService.addExpense(recurring.getWallet().getId(),
                                                    recurring.getCategory(),
                                                    dueDate,
                                                    recurring.getAmount(),
                                                    recurring.getDescription(),
                                                    TransactionStatus.PENDING);
            }
            else
            {
                throw new RuntimeException("Invalid transaction type");
            }
        }
        catch (RuntimeException e)
        {
            m_logger.warn("Failed to create transaction for recurring transaction " +
                             recurring.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Calculate the next due date of a recurring transaction
     * @param currentDueDate The current due date
     * @param frequency The frequency of the recurring transaction
     * @return The next due date with the time set to 23:59
     */
    private LocalDateTime calculateNextDueDate(LocalDateTime currentDueDate,
                                               RecurringTransactionFrequency frequency)
    {
        LocalDateTime nextDueDate;

        switch (frequency)
        {
            case DAILY:
                nextDueDate = currentDueDate.plusDays(1);
                break;
            case WEEKLY:
                nextDueDate = currentDueDate.plusWeeks(1);
                break;
            case MONTHLY:
                nextDueDate = currentDueDate.plusMonths(1);
                break;
            case YEARLY:
                nextDueDate = currentDueDate.plusYears(1);
                break;
            default:
                throw new RuntimeException("Invalid frequency");
        }

        return nextDueDate.with(Constants.RECURRING_TRANSACTION_DEFAULT_TIME);
    }

    /**
     * Get the date of the last transaction that will be generated
     * @param startDate The start date
     * @param endDate The end date
     * @param frequency The frequency of the recurring transaction
     * @return The date of the last transaction
     * @throws RuntimeException If the frequency is invalid
     */
    public LocalDate getLastTransactionDate(LocalDate                     startDate,
                                            LocalDate                     endDate,
                                            RecurringTransactionFrequency frequency)
    {
        validateDateAndIntervalForCreate(startDate, endDate, frequency);

        Long interval = 0L;

        switch (frequency)
        {
            case DAILY:
                interval = ChronoUnit.DAYS.between(startDate, endDate);
                break;
            case WEEKLY:
                interval = ChronoUnit.DAYS.between(startDate, endDate) / 7;
                break;
            case MONTHLY:
                interval = ChronoUnit.MONTHS.between(startDate, endDate);
                break;
            case YEARLY:
                interval = ChronoUnit.YEARS.between(startDate, endDate);
                break;
        }

        LocalDate lastTransactionDate =
            addFrequencyToDate(startDate, interval, frequency);

        if (lastTransactionDate.isAfter(endDate))
        {
            interval--;
            lastTransactionDate = addFrequencyToDate(startDate, interval, frequency);
        }

        return lastTransactionDate;
    }

    /**
     * Add the frequency to a date
     * @param date The date
     * @param interval The interval
     * @param frequency The frequency
     * @return The new date
     * @throws RuntimeException If the frequency is invalid
     */
    private LocalDate addFrequencyToDate(LocalDate                     date,
                                         long                          interval,
                                         RecurringTransactionFrequency frequency)
    {
        switch (frequency)
        {
            case DAILY:
                return date.plusDays(interval);
            case WEEKLY:
                return date.plusWeeks(interval);
            case MONTHLY:
                return date.plusMonths(interval);
            case YEARLY:
                return date.plusYears(interval);
            default:
                throw new RuntimeException("Invalid frequency");
        }
    }

    /**
     * Get all recurring transactions
     * @return List of recurring transactions
     */
    public List<RecurringTransaction> getAllRecurringTransactions()
    {
        return recurringTransactionRepository.findAll();
    }

    /**
     * Get future transactions by year
     * @param startYear The start year
     * @param endYear The end year
     */
    public List<WalletTransaction> getFutureTransactionsByYear(Year startYear,
                                                               Year endYear)
    {
        List<RecurringTransaction> recurringTransactions =
            recurringTransactionRepository.findByStatus(
                RecurringTransactionStatus.ACTIVE);

        List<WalletTransaction> futureTransactions = new ArrayList<>();

        // Generate the future transactions for each recurring transaction
        for (RecurringTransaction recurring : recurringTransactions)
        {
            LocalDateTime nextDueDate = recurring.getNextDueDate();

            // If the recurring transaction has ended, stop generating transactions
            if (recurring.getEndDate().isBefore(nextDueDate))
            {
                break;
            }

            while (nextDueDate.isBefore(
                endYear.atMonth(12).atDay(31).atTime(23, 59, 59, 59)))
            {
                // If the recurring transaction has ended, stop generating transactions
                if (recurring.getEndDate().isBefore(nextDueDate))
                {
                    break;
                }

                if (nextDueDate.isAfter(startYear.atDay(1).atTime(0, 0, 0)) ||
                    nextDueDate.equals(startYear.atDay(1).atTime(0, 0, 0)))
                {
                    futureTransactions.add(WalletTransaction.builder()
                                               .wallet(recurring.getWallet())
                                               .category(recurring.getCategory())
                                               .type(recurring.getType())
                                               .status(TransactionStatus.PENDING)
                                               .date(nextDueDate)
                                               .amount(recurring.getAmount())
                                               .description(recurring.getDescription())
                                               .build());
                }

                // Calculate the next due date
                nextDueDate =
                    calculateNextDueDate(nextDueDate, recurring.getFrequency());
                recurring.setNextDueDate(nextDueDate);
            }
        }

        return futureTransactions;
    }

    /**
     * Get future transactions by month
     * @param startMonth The start month
     * @param endMonth The end month
     */
    public List<WalletTransaction> getFutureTransactionsByMonth(YearMonth startMonth,
                                                                YearMonth endMonth)
    {
        List<RecurringTransaction> recurringTransactions =
            recurringTransactionRepository.findByStatus(
                RecurringTransactionStatus.ACTIVE);

        List<WalletTransaction> futureTransactions = new ArrayList<>();

        // Generate the future transactions for each recurring transaction
        for (RecurringTransaction recurring : recurringTransactions)
        {
            LocalDateTime nextDueDate = recurring.getNextDueDate();

            while (nextDueDate.isBefore(endMonth.atEndOfMonth().atTime(23, 59, 59, 59)))
            {
                // If the recurring transaction has ended, stop generating transactions
                if (recurring.getEndDate().isBefore(nextDueDate))
                {
                    break;
                }

                if (nextDueDate.isAfter(startMonth.atDay(1).atTime(0, 0, 0, 0)) ||
                    nextDueDate.equals(startMonth.atDay(1).atTime(0, 0, 0, 0)))
                {
                    futureTransactions.add(WalletTransaction.builder()
                                               .wallet(recurring.getWallet())
                                               .category(recurring.getCategory())
                                               .type(recurring.getType())
                                               .status(TransactionStatus.PENDING)
                                               .date(nextDueDate)
                                               .amount(recurring.getAmount())
                                               .description(recurring.getDescription())
                                               .build());
                }

                // Calculate the next due date
                nextDueDate =
                    calculateNextDueDate(nextDueDate, recurring.getFrequency());
                recurring.setNextDueDate(nextDueDate);
            }
        }

        return futureTransactions;
    }

    public Double calculateExpectedRemainingAmount(Long rtId)
    {
        RecurringTransaction rt =
            recurringTransactionRepository.findById(rtId).orElseThrow(
                () -> new RuntimeException("Recurring transaction not found"));

        if (rt.getEndDate().toLocalDate().equals(
                Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE))
        {
            return Double.POSITIVE_INFINITY;
        }

        LocalDateTime today       = LocalDateTime.now();
        LocalDateTime nextDueDate = rt.getNextDueDate();

        if (rt.getEndDate().isBefore(today))
        {
            return 0.0;
        }

        Double expectedAmount = 0.0;

        while (nextDueDate.isBefore(rt.getEndDate()) ||
               nextDueDate.equals(rt.getEndDate()))
        {
            expectedAmount += rt.getAmount().doubleValue();
            nextDueDate = calculateNextDueDate(nextDueDate, rt.getFrequency());
        }

        return expectedAmount;
    }
}
