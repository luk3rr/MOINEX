/*
 * Filename: RecurringTransactionService.java
 * Created on: November 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import org.moinex.entities.Category;
import org.moinex.entities.RecurringTransaction;
import org.moinex.entities.Wallet;
import org.moinex.repositories.RecurringTransactionRepository;
import org.moinex.repositories.WalletRepository;
import org.moinex.util.LoggerConfig;
import org.moinex.util.RecurringTransactionFrequency;
import org.moinex.util.TransactionStatus;
import org.moinex.util.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing the recurring transactions
 */
@Service
public class RecurringTransactionService
{
    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private WalletTransactionService walletTransactionService;

    @Autowired
    private WalletRepository walletRepository;

    private static final Logger m_logger = LoggerConfig.GetLogger();

    public RecurringTransactionService() { }

    @Transactional
    public Long CreateRecurringTransaction(Wallet                        wallet,
                                           Category                      category,
                                           TransactionType               type,
                                           BigDecimal                    amount,
                                           LocalDateTime                 startDate,
                                           LocalDateTime                 endDate,
                                           String                        description,
                                           RecurringTransactionFrequency frequency)
    {
        walletRepository.findById(wallet.GetId())
            .orElseThrow(()
                             -> new RuntimeException("Wallet with id " +
                                                     wallet.GetId() + " not found"));

        RecurringTransaction recurringTransaction =
            new RecurringTransaction(wallet,
                                     category,
                                     type,
                                     amount,
                                     startDate,
                                     endDate,
                                     startDate,
                                     frequency,
                                     description);

        recurringTransactionRepository.save(recurringTransaction);

        m_logger.info("Created recurring transaction " + recurringTransaction.GetId());

        return recurringTransaction.GetId();
    }

    @Transactional
    public void StopRecurringTransaction(Long recurringTransactionId)
    {
        RecurringTransaction recurringTransaction =
            recurringTransactionRepository.findById(recurringTransactionId)
                .orElseThrow(
                    () -> new RuntimeException("Recurring transaction not found"));

        // Check if the recurring transaction has already ended
        if (recurringTransaction.GetEndDate().isBefore(LocalDateTime.now()))
        {
            throw new RuntimeException("Recurring transaction has already ended");
        }

        recurringTransaction.SetEndDate(LocalDateTime.now());
        recurringTransactionRepository.save(recurringTransaction);

        m_logger.info("Stopped recurring transaction " + recurringTransaction.GetId());
    }

    /**
     * Process the recurring transactions
     * This method checks if the next due date of the recurring transactions has
     * already passed and generates the missing transactions
     */
    @Transactional
    public void ProcessRecurringTransactions()
    {
        List<RecurringTransaction> recurringTransactions =
            recurringTransactionRepository.findAll();

        LocalDateTime today = LocalDateTime.now();

        for (RecurringTransaction recurring : recurringTransactions)
        {
            LocalDateTime nextDueDate = recurring.GetNextDueDate();

            // Check if the next due date has already passed and generate the missing
            // transactions
            if (!nextDueDate.isAfter(today) && !recurring.GetEndDate().isBefore(today))
            {
                while (!nextDueDate.isAfter(today))
                {
                    CreateTransactionForDate(recurring, nextDueDate);

                    nextDueDate =
                        CalculateNextDueDate(nextDueDate, recurring.GetFrequency());
                }

                // Update the next due date in the recurring transaction
                recurring.SetNextDueDate(nextDueDate);
                recurringTransactionRepository.save(recurring);
            }
        }
    }

    /**
     * Create a wallet transaction for a recurring transaction
     * @param recurring The recurring transaction
     * @param dueDate The due date of the transaction
     */
    private void CreateTransactionForDate(RecurringTransaction recurring,
                                          LocalDateTime        dueDate)
    {
        try
        {
            if (recurring.GetType().equals(TransactionType.INCOME))
            {
                walletTransactionService.AddIncome(recurring.GetWallet().GetId(),
                                                   recurring.GetCategory(),
                                                   dueDate,
                                                   recurring.GetAmount(),
                                                   recurring.GetDescription(),
                                                   TransactionStatus.PENDING);
            }
            else if (recurring.GetType().equals(TransactionType.EXPENSE))
            {
                walletTransactionService.AddExpense(recurring.GetWallet().GetId(),
                                                    recurring.GetCategory(),
                                                    dueDate,
                                                    recurring.GetAmount(),
                                                    recurring.GetDescription(),
                                                    TransactionStatus.PENDING);
            }
            else
            {
                throw new RuntimeException("Invalid transaction type");
            }
        }
        catch (RuntimeException e)
        {
            m_logger.warning("Failed to create transaction for recurring transaction " +
                             recurring.GetId() + ": " + e.getMessage());
        }
    }

    /**
     * Calculate the next due date of a recurring transaction
     * @param currentDueDate The current due date
     * @param frequency The frequency of the recurring transaction
     * @return The next due date with the time set to 23:59
     */
    private LocalDateTime CalculateNextDueDate(LocalDateTime currentDueDate,
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

        // Set the time to 23:59
        return nextDueDate.withHour(23).withMinute(59).withSecond(0).withNano(0);
    }
}
