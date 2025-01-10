/*
 * Filename: WalletTransactionService.java
 * Created on: October 16, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import org.moinex.entities.Category;
import org.moinex.entities.Transfer;
import org.moinex.entities.Wallet;
import org.moinex.entities.WalletTransaction;
import org.moinex.repositories.TransferRepository;
import org.moinex.repositories.WalletRepository;
import org.moinex.repositories.WalletTransactionRepository;
import org.moinex.util.Constants;
import org.moinex.util.LoggerConfig;
import org.moinex.util.TransactionStatus;
import org.moinex.util.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for the business logic of the wallet transactions
 *
 * Each method to get transactions has a version that returns only transactions
 * that have a category that is not archived
 */
@Service
public class WalletTransactionService
{
    @Autowired
    private WalletRepository m_walletRepository;

    @Autowired
    private TransferRepository m_transferRepository;

    @Autowired
    private WalletTransactionRepository m_walletTransactionRepository;

    private static final Logger m_logger = LoggerConfig.GetLogger();

    public WalletTransactionService() { }

    /**
     * Transfer money between two wallets
     * @param senderId The id of the wallet that sends the money
     * @param receiverId The id of the wallet that receives the money
     * @param amount The amount of money to be transferred
     * @param description A description of the transfer
     * @return The id of the created transfer
     * @throws RuntimeException If the sender and receiver wallets are the same
     * @throws RuntimeException If the amount to transfer is less than or equal to zero
     * @throws RuntimeException If the sender wallet does not exist
     * @throws RuntimeException If the receiver wallet does not exist
     * @throws RuntimeException If the sender wallet does not have enough balance
     * to transfer
     */
    @Transactional
    public Long TransferMoney(Long          senderId,
                              Long          receiverId,
                              LocalDateTime date,
                              BigDecimal    amount,
                              String        description)
    {
        if (senderId.equals(receiverId))
        {
            throw new RuntimeException("Sender and receiver wallets must be different");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Amount to transfer must be greater than zero");
        }

        // Round the amount to two decimal places
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        Wallet senderWallet = m_walletRepository.findById(senderId).orElseThrow(
            ()
                -> new RuntimeException(
                    "Sender wallet not found and cannot transfer money"));

        Wallet receiverWallet =
            m_walletRepository.findById(receiverId)
                .orElseThrow(
                    ()
                        -> new RuntimeException(
                            "Receiver wallet not found and cannot transfer money"));

        if (senderWallet.GetBalance().compareTo(amount) < 0)
        {
            throw new RuntimeException(
                "Sender wallet does not have enough balance to transfer");
        }

        Transfer transfer = m_transferRepository.save(
            new Transfer(senderWallet, receiverWallet, date, amount, description));

        senderWallet.SetBalance(senderWallet.GetBalance().subtract(amount));
        receiverWallet.SetBalance(receiverWallet.GetBalance().add(amount));

        m_walletRepository.save(senderWallet);
        m_walletRepository.save(receiverWallet);

        m_logger.info("Transfer from wallet with id " + senderId +
                      " to wallet with id " + receiverId + " of " + amount +
                      " was successful");

        return transfer.GetId();
    }

    /**
     * Add an income to a wallet
     * @param walletId The id of the wallet that receives the income
     * @param category The category of the income
     * @param date The date of the income
     * @param amount The amount of the income
     * @param description A description of the income
     * @param status The status of the transaction
     * @return The id of the created transaction
     * @throws RuntimeException If the wallet does not exist
     * @throws RuntimeException If the amount is less than or equal to zero
     */
    @Transactional
    public Long AddIncome(Long              walletId,
                          Category          category,
                          LocalDateTime     date,
                          BigDecimal        amount,
                          String            description,
                          TransactionStatus status)
    {
        Wallet wallet = m_walletRepository.findById(walletId).orElseThrow(
            () -> new RuntimeException("Wallet with id " + walletId + " not found"));

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Amount must be greater than zero");
        }

        // Round the amount to two decimal places
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        WalletTransaction wt = new WalletTransaction(wallet,
                                                     category,
                                                     TransactionType.INCOME,
                                                     status,
                                                     date,
                                                     amount,
                                                     description);

        m_walletTransactionRepository.save(wt);

        if (status == TransactionStatus.CONFIRMED)
        {
            wallet.SetBalance(wallet.GetBalance().add(amount));
            m_walletRepository.save(wallet);
        }

        m_logger.info("Income with status " + status.toString() + " of " + amount +
                      " added to wallet with id " + walletId);

        return wt.GetId();
    }

    /**
     * Add an expense to a wallet
     * @param walletId The id of the wallet that receives the expense
     * @param category The category of the expense
     * @param date The date of the expense
     * @param amount The amount of the expense
     * @param description A description of the expense
     * @param status The status of the transaction
     * @return The id of the created transaction
     * @throws RuntimeException If the wallet does not exist
     * @throws RuntimeException If the amount is less than or equal to zero
     */
    @Transactional
    public Long AddExpense(Long              walletId,
                           Category          category,
                           LocalDateTime     date,
                           BigDecimal        amount,
                           String            description,
                           TransactionStatus status)
    {
        Wallet wallet = m_walletRepository.findById(walletId).orElseThrow(
            () -> new RuntimeException("Wallet with id " + walletId + " not found"));

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Amount must be greater than zero");
        }

        // Round the amount to two decimal places
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        WalletTransaction wt = new WalletTransaction(wallet,
                                                     category,
                                                     TransactionType.EXPENSE,
                                                     status,
                                                     date,
                                                     amount,
                                                     description);

        m_walletTransactionRepository.save(wt);

        if (status.equals(TransactionStatus.CONFIRMED))
        {
            wallet.SetBalance(wallet.GetBalance().subtract(amount));
            m_walletRepository.save(wallet);
        }

        m_logger.info("Expense with status " + status.toString() + " of " + amount +
                      " added to wallet with id " + walletId);

        return wt.GetId();
    }

    /**
     * Update a transaction
     * @param transaction The transaction to be updated
     * @throws RuntimeException If the transaction does not exist
     * @throws RuntimeException If the wallet does not exist
     * @throws RuntimeException If the amount is less than or equal to zero
     */
    @Transactional
    public void UpdateTransaction(WalletTransaction transaction)
    {
        // Check if the transaction exists
        WalletTransaction oldTransaction =
            m_walletTransactionRepository.findById(transaction.GetId())
                .orElseThrow(()
                                 -> new RuntimeException("Transaction with id " +
                                                         transaction.GetId() +
                                                         " not found"));

        // Check if the wallet exists
        m_walletTransactionRepository.FindWalletByTransactionId(transaction.GetId())
            .orElseThrow(()
                             -> new RuntimeException("Wallet with name " +
                                                     transaction.GetWallet().GetName() +
                                                     " not found"));

        // Check if the amount is greater than zero
        if (transaction.GetAmount().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Amount must be greater than or equal to zero");
        }

        // Round the amount to two decimal places
        transaction.SetAmount(
            transaction.GetAmount().setScale(2, RoundingMode.HALF_UP));

        // Complex update of the transaction
        ChangeTransactionWallet(oldTransaction, transaction.GetWallet());
        ChangeTransactionType(oldTransaction, transaction.GetType());
        ChangeTransactionAmount(oldTransaction, transaction.GetAmount());
        ChangeTransactionStatus(oldTransaction, transaction.GetStatus());

        // Trivial update of the transaction
        oldTransaction.SetDate(transaction.GetDate());
        oldTransaction.SetDescription(transaction.GetDescription());
        oldTransaction.SetCategory(transaction.GetCategory());

        m_walletTransactionRepository.save(oldTransaction);

        m_logger.info("Transaction with id " + transaction.GetId() +
                      " updated successfully");
    }

    /**
     * Change the type of a transaction
     * @param transaction The transaction to be updated
     * @param newType The new type of the transaction
     * @throws RuntimeException If the transaction type does not exist
     *
     * @note This method persists the changes in the wallet balances
     * and the transaction in the database
     */
    private void ChangeTransactionType(WalletTransaction oldTransaction,
                                       TransactionType   newType)
    {
        if (oldTransaction.GetType().equals(newType))
        {
            m_logger.info("Transaction with id " + oldTransaction.GetId() +
                          " has the same type as before");

            return;
        }

        Wallet wallet = oldTransaction.GetWallet();

        TransactionType oldType = oldTransaction.GetType();

        if (oldTransaction.GetStatus().equals(TransactionStatus.CONFIRMED))
        {
            // Revert the old transaction
            if (oldType.equals(TransactionType.EXPENSE))
            {
                wallet.SetBalance(wallet.GetBalance().add(oldTransaction.GetAmount()));
            }
            else if (oldType.equals(TransactionType.INCOME))
            {
                wallet.SetBalance(
                    wallet.GetBalance().subtract(oldTransaction.GetAmount()));
            }
            else
            {
                // WARNING for the case of new types being added to the enum
                // and not being handled here
                throw new RuntimeException("Transaction type not recognized");
            }

            // Apply the new transaction
            if (newType.equals(TransactionType.EXPENSE))
            {
                wallet.SetBalance(
                    wallet.GetBalance().subtract(oldTransaction.GetAmount()));
            }
            else if (newType.equals(TransactionType.INCOME))
            {
                wallet.SetBalance(wallet.GetBalance().add(oldTransaction.GetAmount()));
            }
            else
            {
                // WARNING for the case of new types being added to the enum
                // and not being handled here
                throw new RuntimeException("Transaction type not recognized");
            }

            m_walletRepository.save(wallet);
        }

        oldTransaction.SetType(newType);
        m_walletTransactionRepository.save(oldTransaction);

        m_logger.info("Transaction with id " + oldTransaction.GetId() +
                      " type changed to " + newType.toString());
    }

    /**
     * Change the wallet of a transaction
     * @param transaction The transaction to be updated
     * @param newWallet The new wallet of the transaction
     * @throws RuntimeException If the transaction type does not exist
     *
     * @note This method persists the changes in the wallet balances
     * and the transaction in the database
     */
    private void ChangeTransactionWallet(WalletTransaction oldTransaction,
                                         Wallet            newWallet)
    {
        if (oldTransaction.GetWallet().GetId() == newWallet.GetId())
        {
            m_logger.info("Transaction with id " + oldTransaction.GetId() +
                          " has the same wallet as before");

            return;
        }

        Wallet oldWallet = oldTransaction.GetWallet();

        if (oldTransaction.GetStatus().equals(TransactionStatus.CONFIRMED))
        {
            if (oldTransaction.GetType().equals(TransactionType.EXPENSE))
            {
                // Revert expense from old wallet
                oldWallet.SetBalance(
                    oldWallet.GetBalance().add(oldTransaction.GetAmount()));

                // Apply expense to new wallet
                newWallet.SetBalance(
                    newWallet.GetBalance().subtract(oldTransaction.GetAmount()));
            }
            else if (oldTransaction.GetType().equals(TransactionType.INCOME))
            {
                // Revert income from old wallet
                oldWallet.SetBalance(
                    oldWallet.GetBalance().subtract(oldTransaction.GetAmount()));

                // Apply income to new wallet
                newWallet.SetBalance(
                    newWallet.GetBalance().add(oldTransaction.GetAmount()));
            }
            else
            {
                // WARNING for the case of new types being added to the enum
                // and not being handled here
                throw new RuntimeException("Transaction type not recognized");
            }

            m_walletRepository.save(oldWallet);
            m_walletRepository.save(newWallet);
        }

        oldTransaction.SetWallet(newWallet);
        m_walletTransactionRepository.save(oldTransaction);

        m_logger.info("Transaction with id " + oldTransaction.GetId() +
                      " wallet changed to " + newWallet.GetName());
    }

    /**
     * Change the amount of a transaction
     * @param transaction The transaction to be updated
     * @param newAmount The new amount of the transaction
     * @throws RuntimeException If the transaction type does not exist
     * @note This method persists the changes in the wallet balances
     * and the transaction in the database
     */
    private void ChangeTransactionAmount(WalletTransaction oldTransaction,
                                         BigDecimal        newAmount)
    {
        BigDecimal oldAmount = oldTransaction.GetAmount();

        BigDecimal diff = oldAmount.subtract(newAmount).abs();

        // Check if the new amount is the same as the old amount
        if (diff.compareTo(BigDecimal.ZERO) == 0)
        {
            m_logger.info("Transaction with id " + oldTransaction.GetId() +
                          " has the same amount as before");

            return;
        }

        Wallet wallet = oldTransaction.GetWallet();

        // Apply the difference to the wallet balance
        if (oldTransaction.GetStatus().equals(TransactionStatus.CONFIRMED))
        {
            if (oldTransaction.GetType().equals(TransactionType.EXPENSE))
            {
                BigDecimal balance = wallet.GetBalance();

                if (oldAmount.compareTo(newAmount) > 0)
                {
                    wallet.SetBalance(balance.add(diff));
                }
                else
                {
                    wallet.SetBalance(balance.subtract(diff));
                }
            }
            else if (oldTransaction.GetType().equals(TransactionType.INCOME))
            {
                if (oldAmount.compareTo(newAmount) > 0)
                {
                    wallet.SetBalance(wallet.GetBalance().subtract(diff));
                }
                else
                {
                    wallet.SetBalance(wallet.GetBalance().add(diff));
                }
            }
            else
            {
                // WARNING for the case of new types being added to the enum
                // and not being handled here
                throw new RuntimeException("Transaction type not recognized");
            }

            m_logger.info("Wallet with id " + wallet.GetId() + " balance changed to " +
                          wallet.GetBalance());

            m_walletRepository.save(wallet);
        }

        oldTransaction.SetAmount(newAmount);
        m_walletTransactionRepository.save(oldTransaction);

        m_logger.info("Transaction with id " + oldTransaction.GetId() +
                      " amount changed to " + newAmount);
    }

    /**
     * Change the status of a transaction
     * @param transaction The transaction to be updated
     * @param newStatus The new status of the transaction
     * @throws RuntimeException If the transaction type does not exist
     *
     * @note This method persists the changes in the wallet balances
     * and the transaction in the database
     */
    private void ChangeTransactionStatus(WalletTransaction transaction,
                                         TransactionStatus newStatus)
    {
        if (transaction.GetStatus().equals(newStatus))
        {
            m_logger.info("Transaction with id " + transaction.GetId() +
                          " has the same status as before");

            return;
        }

        Wallet            wallet    = transaction.GetWallet();
        TransactionStatus oldStatus = transaction.GetStatus();

        if (transaction.GetType().equals(TransactionType.EXPENSE))
        {
            if (oldStatus.equals(TransactionStatus.CONFIRMED))
            {
                if (newStatus.equals(TransactionStatus.PENDING))
                {
                    // Revert the expense
                    wallet.SetBalance(wallet.GetBalance().add(transaction.GetAmount()));
                }
            }
            else if (oldStatus.equals(TransactionStatus.PENDING))
            {
                if (newStatus.equals(TransactionStatus.CONFIRMED))
                {
                    // Apply the expense
                    wallet.SetBalance(
                        wallet.GetBalance().subtract(transaction.GetAmount()));
                }
            }
            else
            {
                // WARNING for the case of new status being added to the enum
                // and not being handled here
                throw new RuntimeException("Transaction status not recognized");
            }
        }
        else if (transaction.GetType().equals(TransactionType.INCOME))
        {
            if (oldStatus.equals(TransactionStatus.CONFIRMED))
            {
                if (newStatus.equals(TransactionStatus.PENDING))
                {
                    wallet.SetBalance(
                        wallet.GetBalance().subtract(transaction.GetAmount()));
                }
            }
            else if (oldStatus.equals(TransactionStatus.PENDING))
            {
                if (newStatus.equals(TransactionStatus.CONFIRMED))
                {
                    wallet.SetBalance(wallet.GetBalance().add(transaction.GetAmount()));
                }
            }
            else
            {
                // WARNING for the case of new status being added to the enum
                // and not being handled here
                throw new RuntimeException("Transaction status not recognized");
            }
        }
        else
        {
            // WARNING for the case of new types being added to the enum
            // and not being handled here
            throw new RuntimeException("Transaction type not recognized");
        }

        transaction.SetStatus(newStatus);
        m_walletRepository.save(wallet);

        m_logger.info("Wallet with id " + wallet.GetId() + " balance changed to " +
                      wallet.GetBalance());

        m_walletTransactionRepository.save(transaction);

        m_logger.info("Transaction with id " + transaction.GetId() +
                      " status changed to " + newStatus.toString());
    }

    /**
     * Delete a transaction from a wallet
     * @param transactionId The id of the transaction to be removed
     * @throws RuntimeException If the transaction does not exist
     */
    @Transactional
    public void DeleteTransaction(Long transactionId)
    {
        WalletTransaction transaction =
            m_walletTransactionRepository.findById(transactionId)
                .orElseThrow(()
                                 -> new RuntimeException("Transaction with id " +
                                                         transactionId + " not found"));

        Wallet wallet = transaction.GetWallet();

        // Update the wallet balance if the transaction is confirmed
        if (transaction.GetStatus() == TransactionStatus.CONFIRMED)
        {
            BigDecimal amount = transaction.GetAmount();
            if (transaction.GetType() == TransactionType.INCOME)
            {
                wallet.SetBalance(wallet.GetBalance().subtract(amount));
            }
            else
            {
                wallet.SetBalance(wallet.GetBalance().add(amount));
            }

            m_walletRepository.save(wallet);
        }

        m_walletTransactionRepository.delete(transaction);

        m_logger.info("Transaction " + transactionId + " deleted from wallet " +
                      wallet.GetName());
    }

    /**
     * Confirm a pending transaction
     * @param transactionId The id of the transaction to be confirmed
     * @throws RuntimeException If the transaction does not exist
     * @throws RuntimeException If the transaction is already confirmed
     */
    @Transactional
    public void ConfirmTransaction(Long transactionId)
    {
        WalletTransaction transaction =
            m_walletTransactionRepository.findById(transactionId)
                .orElseThrow(()
                                 -> new RuntimeException("Transaction with id " +
                                                         transactionId + " not found"));

        if (transaction.GetStatus() == TransactionStatus.CONFIRMED)
        {
            throw new RuntimeException("Transaction with id " + transactionId +
                                       " is already confirmed");
        }

        Wallet wallet = transaction.GetWallet();

        if (transaction.GetType() == TransactionType.EXPENSE)
        {
            wallet.SetBalance(wallet.GetBalance().subtract(transaction.GetAmount()));
        }
        else
        {
            wallet.SetBalance(wallet.GetBalance().add(transaction.GetAmount()));
        }

        transaction.SetStatus(TransactionStatus.CONFIRMED);

        m_walletRepository.save(wallet);
        m_walletTransactionRepository.save(transaction);
    }

    /**
     * Get all transactions
     * @return A list with all transactions
     */
    public List<WalletTransaction> GetAllTransactions()
    {
        return m_walletTransactionRepository.findAll();
    }

    /**
     * Get transaction by id
     * @param id The id of the transaction
     * @return The transaction with the provided id
     * @throws RuntimeException If the transaction does not exist
     */
    public WalletTransaction GetTransactionById(Long id)
    {
        return m_walletTransactionRepository.findById(id).orElseThrow(
            () -> new RuntimeException("Transaction with id " + id + " not found"));
    }

    /**
     * Get all transactions where both wallet and category are not archived
     * @return A list with all transactions
     */
    public List<WalletTransaction> GetNonArchivedTransactions()
    {
        return m_walletTransactionRepository.FindNonArchivedTransactions();
    }

    /**
     * Get all income transactions
     * @return A list with all income transactions
     */
    public List<WalletTransaction> GetIncomes()
    {
        return m_walletTransactionRepository.FindIncomeTransactions();
    }

    /**
     * Get all income transactions where both wallet and category are not archived
     * @return A list with all income transactions
     */
    public List<WalletTransaction> GetNonArchivedIncomes()
    {
        return m_walletTransactionRepository.FindNonArchivedIncomeTransactions();
    }

    /**
     * Get all expense transactions
     * @return A list with all expense transactions
     */
    public List<WalletTransaction> GetExpenses()
    {
        return m_walletTransactionRepository.FindExpenseTransactions();
    }

    /**
     * Get all expense transactions where both wallet and category are not archived
     * @return A list with all expense transactions
     */
    public List<WalletTransaction> GetNonArchivedExpenses()
    {
        return m_walletTransactionRepository.FindNonArchivedExpenseTransactions();
    }

    /**
     * Get all transactions by month
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction> GetTransactionsByMonth(Integer month, Integer year)
    {
        return m_walletTransactionRepository.FindTransactionsByMonth(month, year);
    }

    /**
     * Get all transactions by month where both wallet and category are not archived
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction> GetNonArchivedTransactionsByMonth(Integer month,
                                                                     Integer year)
    {
        return m_walletTransactionRepository.FindNonArchivedTransactionsByMonth(month,
                                                                                year);
    }

    /**
     * Get all transactions by year
     * @param year The year of the transactions
     * @return A list with all transactions of the year
     */
    public List<WalletTransaction> GetTransactionsByYear(Integer year)
    {
        return m_walletTransactionRepository.FindTransactionsByYear(year);
    }

    /**
     * Get all transactions by year where both wallet and category are not archived
     * @param year The year of the transactions
     * @return A list with all transactions of the year
     */
    public List<WalletTransaction> GetNonArchivedTransactionsByYear(Integer year)
    {
        return m_walletTransactionRepository.FindNonArchivedTransactionsByYear(year);
    }

    /**
     * Get all transactions by wallet and month
     * @param walletId The id of the wallet
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction>
    GetTransactionsByWalletAndMonth(Long walletId, Integer month, Integer year)
    {
        return m_walletTransactionRepository.FindTransactionsByWalletAndMonth(walletId,
                                                                              month,
                                                                              year);
    }

    /**
     * Get all transactions by wallet where both wallet and category are not archived
     * @param walletId The id of the wallet
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction>
    GetNonArchivedTransactionsByWalletAndMonth(Long    walletId,
                                               Integer month,
                                               Integer year)
    {
        return m_walletTransactionRepository
            .FindNonArchivedTransactionsByWalletAndMonth(walletId, month, year);
    }

    /**
     * Get all transactions between two dates
     * @param startDate The start date
     * @param endDate The end date
     * @return A list with all transactions between the two dates
     */
    public List<WalletTransaction> GetTransactionsBetweenDates(LocalDateTime startDate,
                                                               LocalDateTime endDate)
    {
        String startDateStr = startDate.format(Constants.DB_DATE_FORMATTER);
        String endDateStr   = endDate.format(Constants.DB_DATE_FORMATTER);

        return m_walletTransactionRepository.FindTransactionsBetweenDates(startDateStr,
                                                                          endDateStr);
    }

    /**
     * Get all transactions between two dates where both wallet and category are not
     * archived
     * @param startDate The start date
     * @param endDate The end date
     * @return A list with all transactions between the two dates
     */
    public List<WalletTransaction>
    GetNonArchivedTransactionsBetweenDates(LocalDateTime startDate,
                                           LocalDateTime endDate)
    {
        String startDateStr = startDate.format(Constants.DB_DATE_FORMATTER);
        String endDateStr   = endDate.format(Constants.DB_DATE_FORMATTER);

        return m_walletTransactionRepository.FindNonArchivedTransactionsBetweenDates(
            startDateStr,
            endDateStr);
    }

    /**
     * Get all confirmed transactions by month
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction> GetConfirmedTransactionsByMonth(Integer month,
                                                                   Integer year)
    {
        return m_walletTransactionRepository.FindConfirmedTransactionsByMonth(month,
                                                                              year);
    }

    /**
     * Get all confirmed transactions by month where both wallet and category are not
     * archived
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction>
    GetNonArchivedConfirmedTransactionsByMonth(Integer month, Integer year)
    {
        return m_walletTransactionRepository
            .FindNonArchivedConfirmedTransactionsByMonth(month, year);
    }

    /**
     * Get all pending transactions by month
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction> GetPendingTransactionsByMonth(Integer month,
                                                                 Integer year)
    {
        return m_walletTransactionRepository.FindPendingTransactionsByMonth(month,
                                                                            year);
    }

    /**
     * Get all pending transactions by month where both wallet and category are not
     * archived
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction>
    GetNonArchivedPendingTransactionsByMonth(Integer month, Integer year)
    {
        return m_walletTransactionRepository.FindNonArchivedPendingTransactionsByMonth(
            month,
            year);
    }

    /**
     * Get the last n transactions of all wallets
     * @param n The number of transactions to get
     * @return A list with the last n transactions of all wallets
     */
    public List<WalletTransaction> GetLastTransactions(Integer n)
    {
        return m_walletTransactionRepository.FindLastTransactions(
            PageRequest.ofSize(n));
    }

    /**
     * Get the last n transactions of all wallets both wallet and category are not
     * archived
     * @param n The number of transactions to get
     * @return A list with the last n transactions of all wallets
     */
    public List<WalletTransaction> GetNonArchivedLastTransactions(Integer n)
    {
        return m_walletTransactionRepository.FindNonArchivedLastTransactions(
            PageRequest.ofSize(n));
    }

    /**
     * Get the last n transactions of a wallet
     * @param walletId The id of the wallet
     * @param n The number of transactions to get
     * @return A list with the last n transactions of the wallet
     */
    public List<WalletTransaction> GetLastTransactionsByWallet(Long walletId, Integer n)
    {
        return m_walletTransactionRepository.FindLastTransactionsByWallet(
            walletId,
            PageRequest.ofSize(n));
    }

    /**
     * Get the last n transactions of a wallet where both wallet and category are not
     * archived
     * @param walletId The id of the wallet
     * @param n The number of transactions to get
     * @return A list with the last n transactions of the wallet
     */
    public List<WalletTransaction> GetNonArchivedLastTransactionsByWallet(Long walletId,
                                                                          Integer n)
    {
        return m_walletTransactionRepository.FindNonArchivedLastTransactionsByWallet(
            walletId,
            PageRequest.ofSize(n));
    }

    /**
     * Get the date of the oldest transaction
     * @return The date of the oldest transaction or the current date if there are no
     *     transactions
     */
    public LocalDateTime GetOldestTransactionDate()
    {
        String date = m_walletTransactionRepository.FindOldestTransactionDate();

        if (date == null)
        {
            return LocalDateTime.now();
        }

        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    /**
     * Get the date of the oldest transaction where both wallet and category are not
     * archived
     * @return The date of the oldest transaction or the current date if there are no
     *    transactions
     */
    public LocalDateTime GetNonArchivedOldestTransactionDate()
    {
        String date =
            m_walletTransactionRepository.FindNonArchivedOldestTransactionDate();

        if (date == null)
        {
            return LocalDateTime.now();
        }

        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    /**
     * Get the date of the newest transaction
     * @return The date of the newest transaction or the current date if there are no
     *     transactions
     */
    public LocalDateTime GetNewestTransactionDate()
    {
        String date = m_walletTransactionRepository.FindNewestTransactionDate();

        if (date == null)
        {
            return LocalDateTime.now();
        }

        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    /**
     * Get the date of the newest transaction where both wallet and category are not
     * archived
     * @return The date of the newest transaction or the current date if there are no
     *     transactions
     */
    public LocalDateTime GetNonArchivedNewestTransactionDate()
    {
        String date =
            m_walletTransactionRepository.FindNonArchivedNewestTransactionDate();

        if (date == null)
        {
            return LocalDateTime.now();
        }

        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    /**
     * Get count of transactions by wallet
     * @param walletId The id of the wallet
     * @return The count of transactions in the wallet
     */
    public Long GetTransactionCountByWallet(Long walletId)
    {
        return m_walletTransactionRepository.GetTransactionCountByWallet(walletId) +
            m_transferRepository.GetTransferCountByWallet(walletId);
    }

    /**
     * Get count of transactions by wallet where both wallet and category are not
     * archived
     * @param walletId The id of the wallet
     * @return The count of transactions in the wallet
     */
    public Long GetNonArchivedTransactionCountByWallet(Long walletId)
    {
        return m_walletTransactionRepository.CountNonArchivedTransactionsByWallet(
            walletId);
    }

    /**
     * Get the transfers by wallet
     * @param walletId The id of the wallet
     * @return A list with the transfers in the wallet
     */
    public List<Transfer> GetTransfersByWallet(Long walletId)
    {
        return m_transferRepository.FindTransfersByWallet(walletId);
    }

    /**
     * Get the transfers by month and year
     * @param month The month
     * @param year The year
     * @return A list with the transfers by month and year
     */
    public List<Transfer> GetTransfersByMonthAndYear(Integer month, Integer year)
    {
        return m_transferRepository.FindTransferByMonthAndYear(month, year);
    }

    /**
     * Get the transfers by wallet and month
     * @param walletId The id of the wallet
     * @param month The month
     * @param year The year
     * @return A list with the transfers in the wallet by month
     */
    public List<Transfer>
    GetTransfersByWalletAndMonth(Long walletId, Integer month, Integer year)
    {
        return m_transferRepository.FindTransfersByWalletAndMonth(walletId,
                                                                  month,
                                                                  year);
    }

    /**
     * Get income suggestions. Suggestions are transactions with distinct descriptions
     * and most recent date
     * @return A list with the suggestions
     */
    public List<WalletTransaction> GetIncomeSuggestions()
    {
        return m_walletTransactionRepository.FindSuggestions(TransactionType.INCOME);
    }

    /**
     * Get expense suggestions. Suggestions are transactions with distinct descriptions
     * and most recent date
     * @return A list with the suggestions
     */
    public List<WalletTransaction> GetExpenseSuggestions()
    {
        return m_walletTransactionRepository.FindSuggestions(TransactionType.EXPENSE);
    }
}
