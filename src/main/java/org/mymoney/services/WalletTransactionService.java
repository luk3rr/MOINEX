/*
 * Filename: WalletTransactionService.java
 * Created on: October 16, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.mymoney.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import org.mymoney.entities.Category;
import org.mymoney.entities.Transfer;
import org.mymoney.entities.Wallet;
import org.mymoney.entities.WalletTransaction;
import org.mymoney.repositories.TransferRepository;
import org.mymoney.repositories.WalletRepository;
import org.mymoney.repositories.WalletTransactionRepository;
import org.mymoney.util.Constants;
import org.mymoney.util.LoggerConfig;
import org.mymoney.util.TransactionStatus;
import org.mymoney.util.TransactionType;
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
                              Double        amount,
                              String        description)
    {
        if (senderId.equals(receiverId))
        {
            throw new RuntimeException("Sender and receiver wallets must be different");
        }

        if (amount <= 0)
        {
            throw new RuntimeException("Amount to transfer must be greater than zero");
        }

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

        if (senderWallet.GetBalance() < amount)
        {
            throw new RuntimeException(
                "Sender wallet does not have enough balance to transfer");
        }

        Transfer transfer = m_transferRepository.save(
            new Transfer(senderWallet, receiverWallet, date, amount, description));

        senderWallet.SetBalance(senderWallet.GetBalance() - amount);
        receiverWallet.SetBalance(receiverWallet.GetBalance() + amount);

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
     * @throws RuntimeException If the amount to transfer is less than or equal to zero
     */
    @Transactional
    public Long AddIncome(Long              walletId,
                          Category          category,
                          LocalDateTime     date,
                          Double            amount,
                          String            description,
                          TransactionStatus status)
    {
        Wallet wallet = m_walletRepository.findById(walletId).orElseThrow(
            () -> new RuntimeException("Wallet with id " + walletId + " not found"));

        if (amount <= 0)
        {
            throw new RuntimeException("Amount to transfer must be greater than zero");
        }

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
            wallet.SetBalance(wallet.GetBalance() + amount);
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
     * @throws RuntimeException If the amount to transfer is less than or equal to zero
     * @throws RuntimeException If the wallet does not have enough balance to confirm
     */
    @Transactional
    public Long AddExpense(Long              walletId,
                           Category          category,
                           LocalDateTime     date,
                           Double            amount,
                           String            description,
                           TransactionStatus status)
    {
        Wallet wallet = m_walletRepository.findById(walletId).orElseThrow(
            () -> new RuntimeException("Wallet with id " + walletId + " not found"));

        if (amount <= 0)
        {
            throw new RuntimeException("Amount to transfer must be greater than zero");
        }

        if (wallet.GetBalance() < amount)
        {
            throw new RuntimeException(
                "Wallet " + wallet.GetName() +
                " does not have enough balance to confirm expense");
        }

        WalletTransaction wt = new WalletTransaction(wallet,
                                                     category,
                                                     TransactionType.EXPENSE,
                                                     status,
                                                     date,
                                                     amount,
                                                     description);

        m_walletTransactionRepository.save(wt);

        if (status == TransactionStatus.CONFIRMED)
        {
            wallet.SetBalance(wallet.GetBalance() - amount);
            m_walletRepository.save(wallet);
        }

        m_logger.info("Expense with status " + status.toString() + " of " + amount +
                      " added to wallet with id " + walletId);

        return wt.GetId();
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
            Double amount = transaction.GetAmount();
            if (transaction.GetType() == TransactionType.INCOME)
            {
                wallet.SetBalance(wallet.GetBalance() - amount);
            }
            else
            {
                wallet.SetBalance(wallet.GetBalance() + amount);
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
     * @throws RuntimeException If wallet does not have enough balance to confirm
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
            if (wallet.GetBalance() < transaction.GetAmount())
            {
                throw new RuntimeException(
                    "Wallet " + wallet.GetName() +
                    " does not have enough balance to confirm transaction");
            }

            wallet.SetBalance(wallet.GetBalance() - transaction.GetAmount());
        }
        else
        {
            wallet.SetBalance(wallet.GetBalance() + transaction.GetAmount());
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
     */
    public WalletTransaction GetTransactionById(Long id)
    {
        return m_walletTransactionRepository.findById(id).orElseThrow(
            () -> new RuntimeException("Transaction with id " + id + " not found"));
    }

    /**
     * Get all transactions and category is not archived
     * @return A list with all transactions
     */
    public List<WalletTransaction> GetAllTransactionsAndCategoryNotArchived()
    {
        return m_walletTransactionRepository.GetAllTransactionsAndCategoryNotArchived();
    }

    /**
     * Get all income transactions
     * @return A list with all income transactions
     */
    public List<WalletTransaction> GetAllIncomes()
    {
        return m_walletTransactionRepository.GetAllIncomeTransactions();
    }

    /**
     * Get all income transactions and category is not archived
     * @return A list with all income transactions
     */
    public List<WalletTransaction> GetAllIncomesAndCategoryNotArchived()
    {
        return m_walletTransactionRepository
            .GetAllIncomeTransactionsAndCategoryNotArchived();
    }

    /**
     * Get all expense transactions
     * @return A list with all expense transactions
     */
    public List<WalletTransaction> GetAllExpenses()
    {
        return m_walletTransactionRepository.GetAllExpenseTransactions();
    }

    /**
     * Get all expense transactions and category is not archived
     * @return A list with all expense transactions
     */
    public List<WalletTransaction> GetAllExpensesAndCategoryNotArchived()
    {
        return m_walletTransactionRepository
            .GetAllExpenseTransactionsAndCategoryNotArchived();
    }

    /**
     * Get all transactions by month
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction> GetAllTransactionsByMonth(Integer month,
                                                             Integer year)
    {
        return m_walletTransactionRepository.GetAllTransactionsByMonth(month, year);
    }

    /**
     * Get all transactions by month and category is not archived
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction>
    GetAllTransactionsByMonthAndCategoryNotArchived(Integer month, Integer year)
    {
        return m_walletTransactionRepository
            .GetAllTransactionsByMonthAndCategoryNotArchived(month, year);
    }

    /**
     * Get all transactions by year
     * @param year The year of the transactions
     * @return A list with all transactions of the year
     */
    public List<WalletTransaction> GetAllTransactionsByYear(Integer year)
    {
        return m_walletTransactionRepository.GetAllTransactionsByYear(year);
    }

    /**
     * Get all transactions by year and category is not archived
     * @param year The year of the transactions
     * @return A list with all transactions of the year
     */
    public List<WalletTransaction>
    GetAllTransactionsByYearAndCategoryNotArchived(Integer year)
    {
        return m_walletTransactionRepository
            .GetAllTransactionsByYearAndCategoryNotArchived(year);
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
        return m_walletTransactionRepository.GetTransactionsByWalletAndMonth(walletId,
                                                                             month,
                                                                             year);
    }

    /**
     * Get all transactions by wallet and month and category is not archived
     * @param walletId The id of the wallet
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction>
    GetTransactionsByWalletAndMonthAndCategoryNotArchived(Long    walletId,
                                                          Integer month,
                                                          Integer year)
    {
        return m_walletTransactionRepository
            .GetTransactionsByWalletAndMonthAndCategoryNotArchived(walletId,
                                                                   month,
                                                                   year);
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

        return m_walletTransactionRepository.GetTransactionsBetweenDates(startDateStr,
                                                                         endDateStr);
    }

    /**
     * Get all transactions between two dates and category is not archived
     * @param startDate The start date
     * @param endDate The end date
     * @return A list with all transactions between the two dates
     */
    public List<WalletTransaction>
    GetTransactionsBetweenDatesAndCategoryNotArchived(LocalDateTime startDate,
                                                      LocalDateTime endDate)
    {
        String startDateStr = startDate.format(Constants.DB_DATE_FORMATTER);
        String endDateStr   = endDate.format(Constants.DB_DATE_FORMATTER);

        return m_walletTransactionRepository
            .GetTransactionsBetweenDatesAndCategoryNotArchived(startDateStr,
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
        return m_walletTransactionRepository.GetConfirmedTransactionsByMonth(month,
                                                                             year);
    }

    /**
     * Get all confirmed transactions by month and category is not archived
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction>
    GetConfirmedTransactionsByMonthAndCategoryNotArchived(Integer month, Integer year)
    {
        return m_walletTransactionRepository
            .GetConfirmedTransactionsByMonthAndCategoryNotArchived(month, year);
    }

    /**
     * Get all pending transactions by month
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction> GetPendingTransactionsByMonth(Integer month,
                                                                 Integer year)
    {
        return m_walletTransactionRepository.GetPendingTransactionsByMonth(month, year);
    }

    /**
     * Get all pending transactions by month and category is not archived
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction>
    GetPendingTransactionsByMonthAndCategoryNotArchived(Integer month, Integer year)
    {
        return m_walletTransactionRepository
            .GetPendingTransactionsByMonthAndCategoryNotArchived(month, year);
    }

    /**
     * Get the last n transactions of all wallets
     * @param n The number of transactions to get
     * @return A list with the last n transactions of all wallets
     */
    public List<WalletTransaction> GetLastTransactions(Integer n)
    {
        return m_walletTransactionRepository.GetLastTransactions(PageRequest.ofSize(n));
    }

    /**
     * Get the last n transactions of all wallets and category is not archived
     * @param n The number of transactions to get
     * @return A list with the last n transactions of all wallets
     */
    public List<WalletTransaction> GetLastTransactionsAndCategoryNotArchived(Integer n)
    {
        return m_walletTransactionRepository.GetLastTransactionsAndCategoryNotArchived(
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
        return m_walletTransactionRepository.GetLastTransactionsByWallet(
            walletId,
            PageRequest.ofSize(n));
    }

    /**
     * Get the last n transactions of a wallet and category is not archived
     * @param walletId The id of the wallet
     * @param n The number of transactions to get
     * @return A list with the last n transactions of the wallet
     */
    public List<WalletTransaction>
    GetLastTransactionsByWalletAndCategoryNotArchived(Long walletId, Integer n)
    {
        return m_walletTransactionRepository
            .GetLastTransactionsByWalletAndCategoryNotArchived(walletId,
                                                               PageRequest.ofSize(n));
    }

    /**
     * Get the date of the oldest transaction
     * @return The date of the oldest transaction or the current date if there are no
     *     transactions
     */
    public LocalDateTime GetOldestTransactionDate()
    {
        String date = m_walletTransactionRepository.GetOldestTransactionDate();

        if (date == null)
        {
            return LocalDateTime.now();
        }

        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    /**
     * Get the date of the oldest transaction and category is not archived
     * @return The date of the oldest transaction or the current date if there are no
     *    transactions
     */
    public LocalDateTime GetOldestTransactionDateAndCategoryNotArchived()
    {
        String date = m_walletTransactionRepository
                          .GetOldestTransactionDateAndCategoryNotArchived();

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
        String date = m_walletTransactionRepository.GetNewestTransactionDate();

        if (date == null)
        {
            return LocalDateTime.now();
        }

        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    /**
     * Get the date of the newest transaction and category is not archived
     * @return The date of the newest transaction or the current date if there are no
     *     transactions
     */
    public LocalDateTime GetNewestTransactionDateAndCategoryNotArchived()
    {
        String date = m_walletTransactionRepository
                          .GetNewestTransactionDateAndCategoryNotArchived();

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
        return m_walletTransactionRepository.CountTransactionsByWallet(walletId);
    }

    /**
     * Get count of transactions by wallet and category is not archived
     * @param walletId The id of the wallet
     * @return The count of transactions in the wallet
     */
    public Long GetTransactionCountByWalletAndCategoryNotArchived(Long walletId)
    {
        return m_walletTransactionRepository
            .CountTransactionsByWalletAndCategoryNotArchived(walletId);
    }

    /**
     * Get the transfers by wallet
     * @param walletId The id of the wallet
     * @return A list with the transfers in the wallet
     */
    public List<Transfer> GetTransfersByWallet(Long walletId)
    {
        return m_transferRepository.GetTransfersByWallet(walletId);
    }

    /**
     * Get the transfers by month and year
     * @param month The month
     * @param year The year
     * @return A list with the transfers by month and year
     */
    public List<Transfer> GetTransfersByMonthAndYear(Integer month, Integer year)
    {
        return m_transferRepository.GetTransferByMonthAndYear(month, year);
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
        return m_transferRepository.GetTransfersByWalletAndMonth(walletId, month, year);
    }
}
