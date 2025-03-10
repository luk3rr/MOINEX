/*
 * Filename: WalletTransactionService.java
 * Created on: October 16, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.NoArgsConstructor;
import org.moinex.entities.Category;
import org.moinex.entities.wallettransaction.Transfer;
import org.moinex.entities.wallettransaction.Wallet;
import org.moinex.entities.wallettransaction.WalletTransaction;
import org.moinex.exceptions.AttributeAlreadySetException;
import org.moinex.exceptions.InsufficientResourcesException;
import org.moinex.exceptions.SameSourceDestionationException;
import org.moinex.repositories.wallettransaction.TransferRepository;
import org.moinex.repositories.wallettransaction.WalletRepository;
import org.moinex.repositories.wallettransaction.WalletTransactionRepository;
import org.moinex.util.Constants;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@NoArgsConstructor
public class WalletTransactionService
{
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    private static final Logger logger =
        LoggerFactory.getLogger(WalletTransactionService.class);

    /**
     * Transfer money between two wallets
     * @param senderId The id of the wallet that sends the money
     * @param receiverId The id of the wallet that receives the money
     * @param amount The amount of money to be transferred
     * @param description A description of the transfer
     * @return The id of the created transfer
     * @throws SameSourceDestionationException If the sender and receiver wallets are
     *     the
     *    same
     * @throws IllegalArgumentException If the amount is less than or equal to zero
     * @throws EntityNotFoundException If the sender or receiver wallet does not exist
     * @throws InsufficientResourcesException If the sender wallet does not have enough
     */
    @Transactional
    public Long transferMoney(Long          senderId,
                              Long          receiverId,
                              LocalDateTime date,
                              BigDecimal    amount,
                              String        description)
    {
        if (senderId.equals(receiverId))
        {
            throw new SameSourceDestionationException(
                "Sender and receiver wallets must be different");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new IllegalArgumentException(
                "Amount to transfer must be greater than zero");
        }

        // Round the amount to two decimal places
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        Wallet senderWallet = walletRepository.findById(senderId).orElseThrow(
            ()
                -> new EntityNotFoundException(
                    "Sender wallet not found and cannot transfer money"));

        Wallet receiverWallet =
            walletRepository.findById(receiverId)
                .orElseThrow(
                    ()
                        -> new EntityNotFoundException(
                            "Receiver wallet not found and cannot transfer money"));

        if (senderWallet.getBalance().compareTo(amount) < 0)
        {
            throw new InsufficientResourcesException(
                "Sender wallet does not have enough balance to transfer");
        }

        Transfer transfer = transferRepository.save(Transfer.builder()
                                                        .senderWallet(senderWallet)
                                                        .receiverWallet(receiverWallet)
                                                        .date(date)
                                                        .amount(amount)
                                                        .description(description)
                                                        .build());

        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        logger.info("Transfer from wallet with id " + senderId + " to wallet with id " +
                    receiverId + " of " + amount + " was successful");

        return transfer.getId();
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
     * @throws EntityNotFoundException If the wallet does not exist
     * @throws IllegalArgumentException If the amount is less than or equal to zero
     */
    @Transactional
    public Long addIncome(Long              walletId,
                          Category          category,
                          LocalDateTime     date,
                          BigDecimal        amount,
                          String            description,
                          TransactionStatus status)
    {
        Wallet wallet = walletRepository.findById(walletId).orElseThrow(
            ()
                -> new EntityNotFoundException("Wallet with id " + walletId +
                                               " not found"));

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // Round the amount to two decimal places
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        WalletTransaction wt = WalletTransaction.builder()
                                   .wallet(wallet)
                                   .category(category)
                                   .type(TransactionType.INCOME)
                                   .status(status)
                                   .date(date)
                                   .amount(amount)
                                   .description(description)
                                   .build();

        walletTransactionRepository.save(wt);

        if (status == TransactionStatus.CONFIRMED)
        {
            wallet.setBalance(wallet.getBalance().add(amount));
            walletRepository.save(wallet);
        }

        logger.info("Income with status " + status.toString() + " of " + amount +
                    " added to wallet with id " + walletId);

        return wt.getId();
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
     * @throws EntityNotFoundException If the wallet does not exist
     * @throws IllegalArgumentException If the amount is less than or equal to zero
     */
    @Transactional
    public Long addExpense(Long              walletId,
                           Category          category,
                           LocalDateTime     date,
                           BigDecimal        amount,
                           String            description,
                           TransactionStatus status)
    {
        Wallet wallet = walletRepository.findById(walletId).orElseThrow(
            ()
                -> new EntityNotFoundException("Wallet with id " + walletId +
                                               " not found"));

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // Round the amount to two decimal places
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        WalletTransaction wt = WalletTransaction.builder()
                                   .wallet(wallet)
                                   .category(category)
                                   .type(TransactionType.EXPENSE)
                                   .status(status)
                                   .date(date)
                                   .amount(amount)
                                   .description(description)
                                   .build();

        walletTransactionRepository.save(wt);

        if (status.equals(TransactionStatus.CONFIRMED))
        {
            wallet.setBalance(wallet.getBalance().subtract(amount));
            walletRepository.save(wallet);
        }

        logger.info("Expense with status " + status.toString() + " of " + amount +
                    " added to wallet with id " + walletId);

        return wt.getId();
    }

    /**
     * Update a transaction
     * @param transaction The transaction to be updated
     * @throws EntityNotFoundException If the transaction does not exist
     * @throws EntityNotFoundException If the wallet does not exist
     * @throws IllegalArgumentException If the amount is less than or equal to zero
     * @throws IllegalStateException If the transaction type does not exist
     * @throws IllegalStateException If the transaction status does not exist
     */
    @Transactional
    public void updateTransaction(WalletTransaction transaction)
    {
        // Check if the transaction exists
        WalletTransaction oldTransaction =
            walletTransactionRepository.findById(transaction.getId())
                .orElseThrow(()
                                 -> new EntityNotFoundException("Transaction with id " +
                                                                transaction.getId() +
                                                                " not found"));

        // Check if the wallet exists
        if (!walletTransactionRepository.existsWalletByTransactionId(
                transaction.getId()))
        {
            throw new EntityNotFoundException(
                "Wallet with name " + transaction.getWallet().getName() + " not found");
        }

        // Check if the amount is greater than zero
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new IllegalArgumentException(
                "Amount must be greater than or equal to zero");
        }

        // Round the amount to two decimal places
        transaction.setAmount(
            transaction.getAmount().setScale(2, RoundingMode.HALF_UP));

        // Complex update of the transaction
        changeTransactionWallet(oldTransaction, transaction.getWallet());
        changeTransactionType(oldTransaction, transaction.getType());
        changeTransactionAmount(oldTransaction, transaction.getAmount());
        changeTransactionStatus(oldTransaction, transaction.getStatus());

        // Trivial update of the transaction
        oldTransaction.setDate(transaction.getDate());
        oldTransaction.setDescription(transaction.getDescription());
        oldTransaction.setCategory(transaction.getCategory());

        walletTransactionRepository.save(oldTransaction);

        logger.info("Transaction with id " + transaction.getId() +
                    " updated successfully");
    }

    /**
     * Change the type of a transaction
     * @param transaction The transaction to be updated
     * @param newType The new type of the transaction
     * @throws IllegalStateException If the transaction type does not exist
     * @note This method persists the changes in the wallet balances
     * and the transaction in the database
     */
    private void changeTransactionType(WalletTransaction oldTransaction,
                                       TransactionType   newType)
    {
        if (oldTransaction.getType().equals(newType))
        {
            logger.info("Transaction with id " + oldTransaction.getId() +
                        " has the same type as before");

            return;
        }

        Wallet wallet = oldTransaction.getWallet();

        TransactionType oldType = oldTransaction.getType();

        if (oldTransaction.getStatus().equals(TransactionStatus.CONFIRMED))
        {
            // Revert the old transaction
            if (oldType.equals(TransactionType.EXPENSE))
            {
                wallet.setBalance(wallet.getBalance().add(oldTransaction.getAmount()));
            }
            else if (oldType.equals(TransactionType.INCOME))
            {
                wallet.setBalance(
                    wallet.getBalance().subtract(oldTransaction.getAmount()));
            }
            else
            {
                // WARNING for the case of new types being added to the enum
                // and not being handled here
                throw new IllegalStateException("Transaction type not recognized");
            }

            // Apply the new transaction
            if (newType.equals(TransactionType.EXPENSE))
            {
                wallet.setBalance(
                    wallet.getBalance().subtract(oldTransaction.getAmount()));
            }
            else if (newType.equals(TransactionType.INCOME))
            {
                wallet.setBalance(wallet.getBalance().add(oldTransaction.getAmount()));
            }
            else
            {
                // WARNING for the case of new types being added to the enum
                // and not being handled here
                throw new IllegalStateException("Transaction type not recognized");
            }

            walletRepository.save(wallet);
        }

        oldTransaction.setType(newType);
        walletTransactionRepository.save(oldTransaction);

        logger.info("Transaction with id " + oldTransaction.getId() +
                    " type changed to " + newType.toString());
    }

    /**
     * Change the wallet of a transaction
     * @param transaction The transaction to be updated
     * @param newWallet The new wallet of the transaction
     * @throws IllegalStateException If the transaction type does not exist
     * @note This method persists the changes in the wallet balances
     * and the transaction in the database
     */
    private void changeTransactionWallet(WalletTransaction oldTransaction,
                                         Wallet            newWallet)
    {
        if (oldTransaction.getWallet().getId().equals(newWallet.getId()))
        {
            logger.info("Transaction with id " + oldTransaction.getId() +
                        " has the same wallet as before");

            return;
        }

        Wallet oldWallet = oldTransaction.getWallet();

        if (oldTransaction.getStatus().equals(TransactionStatus.CONFIRMED))
        {
            if (oldTransaction.getType().equals(TransactionType.EXPENSE))
            {
                // Revert expense from old wallet
                oldWallet.setBalance(
                    oldWallet.getBalance().add(oldTransaction.getAmount()));

                // Apply expense to new wallet
                newWallet.setBalance(
                    newWallet.getBalance().subtract(oldTransaction.getAmount()));
            }
            else if (oldTransaction.getType().equals(TransactionType.INCOME))
            {
                // Revert income from old wallet
                oldWallet.setBalance(
                    oldWallet.getBalance().subtract(oldTransaction.getAmount()));

                // Apply income to new wallet
                newWallet.setBalance(
                    newWallet.getBalance().add(oldTransaction.getAmount()));
            }
            else
            {
                // WARNING for the case of new types being added to the enum
                // and not being handled here
                throw new IllegalStateException("Transaction type not recognized");
            }

            walletRepository.save(oldWallet);
            walletRepository.save(newWallet);
        }

        oldTransaction.setWallet(newWallet);
        walletTransactionRepository.save(oldTransaction);

        logger.info("Transaction with id " + oldTransaction.getId() +
                    " wallet changed to " + newWallet.getName());
    }

    /**
     * Change the amount of a transaction
     * @param oldTransaction The transaction to be updated
     * @param newAmount The new amount of the transaction
     * @throws IllegalStateException If the transaction type does not exist
     * @note This method persists the changes in the wallet balances
     * and the transaction in the database
     */
    private void changeTransactionAmount(WalletTransaction oldTransaction,
                                         BigDecimal        newAmount)
    {
        BigDecimal oldAmount = oldTransaction.getAmount();

        BigDecimal diff = oldAmount.subtract(newAmount).abs();

        // Check if the new amount is the same as the old amount
        if (diff.compareTo(BigDecimal.ZERO) == 0)
        {
            logger.info("Transaction with id " + oldTransaction.getId() +
                        " has the same amount as before");

            return;
        }

        Wallet wallet = oldTransaction.getWallet();

        // Apply the difference to the wallet balance
        if (oldTransaction.getStatus().equals(TransactionStatus.CONFIRMED))
        {
            if (oldTransaction.getType().equals(TransactionType.EXPENSE))
            {
                BigDecimal balance = wallet.getBalance();

                if (oldAmount.compareTo(newAmount) > 0)
                {
                    wallet.setBalance(balance.add(diff));
                }
                else
                {
                    wallet.setBalance(balance.subtract(diff));
                }
            }
            else if (oldTransaction.getType().equals(TransactionType.INCOME))
            {
                if (oldAmount.compareTo(newAmount) > 0)
                {
                    wallet.setBalance(wallet.getBalance().subtract(diff));
                }
                else
                {
                    wallet.setBalance(wallet.getBalance().add(diff));
                }
            }
            else
            {
                // WARNING for the case of new types being added to the enum
                // and not being handled here
                throw new IllegalStateException("Transaction type not recognized");
            }

            logger.info("Wallet with id " + wallet.getId() + " balance changed to " +
                        wallet.getBalance());

            walletRepository.save(wallet);
        }

        oldTransaction.setAmount(newAmount);
        walletTransactionRepository.save(oldTransaction);

        logger.info("Transaction with id " + oldTransaction.getId() +
                    " amount changed to " + newAmount);
    }

    /**
     * Change the status of a transaction
     * @param transaction The transaction to be updated
     * @param newStatus The new status of the transaction
     * @throws IllegalStateException If the transaction status does not exist
     * @throws IllegalStateException If the transaction type does not exist
     * @note This method persists the changes in the wallet balances
     * and the transaction in the database
     */
    private void changeTransactionStatus(WalletTransaction transaction,
                                         TransactionStatus newStatus)
    {
        if (transaction.getStatus().equals(newStatus))
        {
            logger.info("Transaction with id " + transaction.getId() +
                        " has the same status as before");

            return;
        }

        Wallet            wallet    = transaction.getWallet();
        TransactionStatus oldStatus = transaction.getStatus();

        if (transaction.getType().equals(TransactionType.EXPENSE))
        {
            if (oldStatus.equals(TransactionStatus.CONFIRMED))
            {
                if (newStatus.equals(TransactionStatus.PENDING))
                {
                    // Revert the expense
                    wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
                }
            }
            else if (oldStatus.equals(TransactionStatus.PENDING))
            {
                if (newStatus.equals(TransactionStatus.CONFIRMED))
                {
                    // Apply the expense
                    wallet.setBalance(
                        wallet.getBalance().subtract(transaction.getAmount()));
                }
            }
            else
            {
                // WARNING for the case of new status being added to the enum
                // and not being handled here
                throw new IllegalStateException("Transaction status not recognized");
            }
        }
        else if (transaction.getType().equals(TransactionType.INCOME))
        {
            if (oldStatus.equals(TransactionStatus.CONFIRMED))
            {
                if (newStatus.equals(TransactionStatus.PENDING))
                {
                    wallet.setBalance(
                        wallet.getBalance().subtract(transaction.getAmount()));
                }
            }
            else if (oldStatus.equals(TransactionStatus.PENDING))
            {
                if (newStatus.equals(TransactionStatus.CONFIRMED))
                {
                    wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
                }
            }
            else
            {
                // WARNING for the case of new status being added to the enum
                // and not being handled here
                throw new IllegalStateException("Transaction status not recognized");
            }
        }
        else
        {
            // WARNING for the case of new types being added to the enum
            // and not being handled here
            throw new IllegalStateException("Transaction type not recognized");
        }

        transaction.setStatus(newStatus);
        walletRepository.save(wallet);

        logger.info("Wallet with id " + wallet.getId() + " balance changed to " +
                    wallet.getBalance());

        walletTransactionRepository.save(transaction);

        logger.info("Transaction with id " + transaction.getId() +
                    " status changed to " + newStatus.toString());
    }

    /**
     * Delete a transaction from a wallet
     * @param transactionId The id of the transaction to be removed
     * @throws EntityNotFoundException If the transaction does not exist
     */
    @Transactional
    public void deleteTransaction(Long transactionId)
    {
        WalletTransaction transaction =
            walletTransactionRepository.findById(transactionId)
                .orElseThrow(()
                                 -> new EntityNotFoundException("Transaction with id " +
                                                                transactionId +
                                                                " not found"));

        Wallet wallet = transaction.getWallet();

        // Update the wallet balance if the transaction is confirmed
        if (transaction.getStatus().equals(TransactionStatus.CONFIRMED))
        {
            BigDecimal amount = transaction.getAmount();
            if (transaction.getType().equals(TransactionType.INCOME))
            {
                wallet.setBalance(wallet.getBalance().subtract(amount));
            }
            else
            {
                wallet.setBalance(wallet.getBalance().add(amount));
            }

            walletRepository.save(wallet);
        }

        walletTransactionRepository.delete(transaction);

        logger.info("Transaction " + transactionId + " deleted from wallet " +
                    wallet.getName());
    }

    /**
     * Confirm a pending transaction
     * @param transactionId The id of the transaction to be confirmed
     * @throws EntityNotFoundException If the transaction does not exist
     * @throws AttributeAlreadySetException If the transaction is already confirmed
     */
    @Transactional
    public void confirmTransaction(Long transactionId)
    {
        WalletTransaction transaction =
            walletTransactionRepository.findById(transactionId)
                .orElseThrow(()
                                 -> new EntityNotFoundException("Transaction with id " +
                                                                transactionId +
                                                                " not found"));

        if (transaction.getStatus().equals(TransactionStatus.CONFIRMED))
        {
            throw new AttributeAlreadySetException(
                "Transaction with id " + transactionId + " is already confirmed");
        }

        Wallet wallet = transaction.getWallet();

        if (transaction.getType().equals(TransactionType.EXPENSE))
        {
            wallet.setBalance(wallet.getBalance().subtract(transaction.getAmount()));
        }
        else
        {
            wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
        }

        transaction.setStatus(TransactionStatus.CONFIRMED);

        walletRepository.save(wallet);
        walletTransactionRepository.save(transaction);
    }

    /**
     * Get all transactions
     * @return A list with all transactions
     */
    public List<WalletTransaction> getAllTransactions()
    {
        return walletTransactionRepository.findAll();
    }

    /**
     * Get transaction by id
     * @param id The id of the transaction
     * @return The transaction with the provided id
     * @throws EntityNotFoundException If the transaction does not exist
     */
    public WalletTransaction getTransactionById(Long id)
    {
        return walletTransactionRepository.findById(id).orElseThrow(
            ()
                -> new EntityNotFoundException("Transaction with id " + id +
                                               " not found"));
    }

    /**
     * Get all transactions where both wallet and category are not archived
     * @return A list with all transactions
     */
    public List<WalletTransaction> getNonArchivedTransactions()
    {
        return walletTransactionRepository.findNonArchivedTransactions();
    }

    /**
     * Get all income transactions
     * @return A list with all income transactions
     */
    public List<WalletTransaction> getIncomes()
    {
        return walletTransactionRepository.findIncomeTransactions();
    }

    /**
     * Get all income transactions where both wallet and category are not archived
     * @return A list with all income transactions
     */
    public List<WalletTransaction> getNonArchivedIncomes()
    {
        return walletTransactionRepository.findNonArchivedIncomeTransactions();
    }

    /**
     * Get all expense transactions
     * @return A list with all expense transactions
     */
    public List<WalletTransaction> getExpenses()
    {
        return walletTransactionRepository.findExpenseTransactions();
    }

    /**
     * Get all expense transactions where both wallet and category are not archived
     * @return A list with all expense transactions
     */
    public List<WalletTransaction> getNonArchivedExpenses()
    {
        return walletTransactionRepository.findNonArchivedExpenseTransactions();
    }

    /**
     * Get all transactions by month
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction> getTransactionsByMonth(Integer month, Integer year)
    {
        return walletTransactionRepository.findTransactionsByMonth(month, year);
    }

    /**
     * Get all transactions by month where both wallet and category are not archived
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction> getNonArchivedTransactionsByMonth(Integer month,
                                                                     Integer year)
    {
        return walletTransactionRepository.findNonArchivedTransactionsByMonth(month,
                                                                              year);
    }

    /**
     * Get all transactions by year
     * @param year The year of the transactions
     * @return A list with all transactions of the year
     */
    public List<WalletTransaction> getTransactionsByYear(Integer year)
    {
        return walletTransactionRepository.findTransactionsByYear(year);
    }

    /**
     * Get all transactions by year where both wallet and category are not archived
     * @param year The year of the transactions
     * @return A list with all transactions of the year
     */
    public List<WalletTransaction> getNonArchivedTransactionsByYear(Integer year)
    {
        return walletTransactionRepository.findNonArchivedTransactionsByYear(year);
    }

    /**
     * Get all transactions by wallet and month
     * @param walletId The id of the wallet
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction>
    getTransactionsByWalletAndMonth(Long walletId, Integer month, Integer year)
    {
        return walletTransactionRepository.findTransactionsByWalletAndMonth(walletId,
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
    getNonArchivedTransactionsByWalletAndMonth(Long    walletId,
                                               Integer month,
                                               Integer year)
    {
        return walletTransactionRepository.findNonArchivedTransactionsByWalletAndMonth(
            walletId,
            month,
            year);
    }

    /**
     * Get all transactions between two dates
     * @param startDate The start date
     * @param endDate The end date
     * @return A list with all transactions between the two dates
     */
    public List<WalletTransaction> getTransactionsBetweenDates(LocalDateTime startDate,
                                                               LocalDateTime endDate)
    {
        String startDateStr = startDate.format(Constants.DB_DATE_FORMATTER);
        String endDateStr   = endDate.format(Constants.DB_DATE_FORMATTER);

        return walletTransactionRepository.findTransactionsBetweenDates(startDateStr,
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
    getNonArchivedTransactionsBetweenDates(LocalDateTime startDate,
                                           LocalDateTime endDate)
    {
        String startDateStr = startDate.format(Constants.DB_DATE_FORMATTER);
        String endDateStr   = endDate.format(Constants.DB_DATE_FORMATTER);

        return walletTransactionRepository.findNonArchivedTransactionsBetweenDates(
            startDateStr,
            endDateStr);
    }

    /**
     * Get all confirmed transactions by month
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction> getConfirmedTransactionsByMonth(Integer month,
                                                                   Integer year)
    {
        return walletTransactionRepository.findConfirmedTransactionsByMonth(month,
                                                                            year);
    }

    /**
     * Get all confirmed transactions by month where both wallet and category are not
     * archived
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction>
    getNonArchivedConfirmedTransactionsByMonth(Integer month, Integer year)
    {
        return walletTransactionRepository.findNonArchivedConfirmedTransactionsByMonth(
            month,
            year);
    }

    /**
     * Get all pending transactions by month
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction> getPendingTransactionsByMonth(Integer month,
                                                                 Integer year)
    {
        return walletTransactionRepository.findPendingTransactionsByMonth(month, year);
    }

    /**
     * Get all pending transactions by month where both wallet and category are not
     * archived
     * @param month The month of the transactions
     * @param year The year of the transactions
     */
    public List<WalletTransaction>
    getNonArchivedPendingTransactionsByMonth(Integer month, Integer year)
    {
        return walletTransactionRepository.findNonArchivedPendingTransactionsByMonth(
            month,
            year);
    }

    /**
     * Get the last n transactions of all wallets
     * @param n The number of transactions to get
     * @return A list with the last n transactions of all wallets
     */
    public List<WalletTransaction> getLastTransactions(Integer n)
    {
        return walletTransactionRepository.findLastTransactions(PageRequest.ofSize(n));
    }

    /**
     * Get the last n transactions of all wallets both wallet and category are not
     * archived
     * @param n The number of transactions to get
     * @return A list with the last n transactions of all wallets
     */
    public List<WalletTransaction> getNonArchivedLastTransactions(Integer n)
    {
        return walletTransactionRepository.findNonArchivedLastTransactions(
            PageRequest.ofSize(n));
    }

    /**
     * Get the last n transactions of a wallet
     * @param walletId The id of the wallet
     * @param n The number of transactions to get
     * @return A list with the last n transactions of the wallet
     */
    public List<WalletTransaction> getLastTransactionsByWallet(Long walletId, Integer n)
    {
        return walletTransactionRepository.findLastTransactionsByWallet(
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
    public List<WalletTransaction> getNonArchivedLastTransactionsByWallet(Long walletId,
                                                                          Integer n)
    {
        return walletTransactionRepository.findNonArchivedLastTransactionsByWallet(
            walletId,
            PageRequest.ofSize(n));
    }

    /**
     * Get the date of the oldest transaction
     * @return The date of the oldest transaction or the current date if there are no
     *     transactions
     */
    public LocalDateTime getOldestTransactionDate()
    {
        String date = walletTransactionRepository.findOldestTransactionDate();

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
    public LocalDateTime getNonArchivedOldestTransactionDate()
    {
        String date =
            walletTransactionRepository.findNonArchivedOldestTransactionDate();

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
    public LocalDateTime getNewestTransactionDate()
    {
        String date = walletTransactionRepository.findNewestTransactionDate();

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
    public LocalDateTime getNonArchivedNewestTransactionDate()
    {
        String date =
            walletTransactionRepository.findNonArchivedNewestTransactionDate();

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
    public Long getTransactionCountByWallet(Long walletId)
    {
        return walletTransactionRepository.getTransactionCountByWallet(walletId) +
            transferRepository.getTransferCountByWallet(walletId);
    }

    /**
     * Get count of transactions by wallet where both wallet and category are not
     * archived
     * @param walletId The id of the wallet
     * @return The count of transactions in the wallet
     */
    public Long getNonArchivedTransactionCountByWallet(Long walletId)
    {
        return walletTransactionRepository.getCountNonArchivedTransactionsByWallet(
            walletId);
    }

    /**
     * Get the transfers by wallet
     * @param walletId The id of the wallet
     * @return A list with the transfers in the wallet
     */
    public List<Transfer> getTransfersByWallet(Long walletId)
    {
        return transferRepository.findTransfersByWallet(walletId);
    }

    /**
     * Get the transfers by month and year
     * @param month The month
     * @param year The year
     * @return A list with the transfers by month and year
     */
    public List<Transfer> getTransfersByMonthAndYear(Integer month, Integer year)
    {
        return transferRepository.findTransferByMonthAndYear(month, year);
    }

    /**
     * Get the transfers by wallet and month
     * @param walletId The id of the wallet
     * @param month The month
     * @param year The year
     * @return A list with the transfers in the wallet by month
     */
    public List<Transfer>
    getTransfersByWalletAndMonth(Long walletId, Integer month, Integer year)
    {
        return transferRepository.findTransfersByWalletAndMonth(walletId, month, year);
    }

    /**
     * Get income suggestions. Suggestions are transactions with distinct descriptions
     * and most recent date
     * @return A list with the suggestions
     */
    public List<WalletTransaction> getIncomeSuggestions()
    {
        return walletTransactionRepository.findSuggestions(TransactionType.INCOME);
    }

    /**
     * Get expense suggestions. Suggestions are transactions with distinct descriptions
     * and most recent date
     * @return A list with the suggestions
     */
    public List<WalletTransaction> getExpenseSuggestions()
    {
        return walletTransactionRepository.findSuggestions(TransactionType.EXPENSE);
    }

    /**
     * Get transfer suggestions. Suggestions are transactions with distinct descriptions
     * and most recent date
     * @return A list with the suggestions
     */
    public List<Transfer> getTransferSuggestions()
    {
        return transferRepository.findSuggestions();
    }
}
