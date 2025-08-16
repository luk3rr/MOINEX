/*
 * Filename: WalletTransactionService.java
 * Created on: October 16, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.NoArgsConstructor;
import org.moinex.error.MoinexException;
import org.moinex.model.Category;
import org.moinex.model.wallettransaction.Transfer;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.repository.wallettransaction.TransferRepository;
import org.moinex.repository.wallettransaction.WalletRepository;
import org.moinex.repository.wallettransaction.WalletTransactionRepository;
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
 * <p>
 * Each method to get transactions has a version that returns only transactions
 * that have a category that is not archived
 */
@Service
@NoArgsConstructor
public class WalletTransactionService {
    private static final Logger logger = LoggerFactory.getLogger(WalletTransactionService.class);
    private WalletRepository walletRepository;
    private TransferRepository transferRepository;
    private WalletTransactionRepository walletTransactionRepository;
    private WalletService walletService;

    @Autowired
    public WalletTransactionService(
            WalletRepository walletRepository,
            TransferRepository transferRepository,
            WalletTransactionRepository walletTransactionRepository,
            WalletService walletService) {
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletService = walletService;
    }

    /**
     * Transfer money between two wallets
     *
     * @param senderId    The id of the wallet that sends the money
     * @param receiverId  The id of the wallet that receives the money
     * @param amount      The amount of money to be transferred
     * @param description A description of the transfer
     * @return The id of the created transfer
     * @throws MoinexException.SameSourceDestinationException If the sender and receiver wallets are
     *                                                        the
     *                                                        same
     * @throws IllegalArgumentException                       If the amount is less than or equal to zero
     * @throws EntityNotFoundException                        If the sender or receiver wallet does not exist
     * @throws MoinexException.InsufficientResourcesException If the sender wallet does not have enough
     */
    @Transactional
    public Integer transferMoney(
            Integer senderId,
            Integer receiverId,
            Category category,
            LocalDateTime date,
            BigDecimal amount,
            String description) {
        if (senderId.equals(receiverId)) {
            throw new MoinexException.SameSourceDestinationException(
                    "Sender and receiver wallets must be different");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount to transfer must be greater than zero");
        }

        // Round the amount to two decimal places
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        Wallet senderWallet =
                walletRepository
                        .findById(senderId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Sender wallet not found and cannot transfer"
                                                        + " money"));

        Wallet receiverWallet =
                walletRepository
                        .findById(receiverId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Receiver wallet not found and cannot transfer"
                                                        + " money"));

        if (senderWallet.isMaster()
                && receiverWallet.isVirtual()
                && receiverWallet.getMasterWallet().equals(senderWallet)) {
            throw new MoinexException.TransferFromMasterToVirtualWalletException(
                    "You cannot transfer money from a master wallet to its virtual wallet.");
        }

        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new MoinexException.InsufficientResourcesException(
                    "Sender wallet does not have enough balance to transfer");
        }

        Transfer transfer =
                transferRepository.save(
                        Transfer.builder()
                                .senderWallet(senderWallet)
                                .receiverWallet(receiverWallet)
                                .category(category)
                                .date(date)
                                .amount(amount)
                                .description(description)
                                .build());

        walletService.decrementWalletBalance(senderWallet.getId(), amount);
        walletService.incrementWalletBalance(receiverWallet.getId(), amount);

        logger.info(
                "Transfer from wallet with id {} to wallet with id {} of {} was successful",
                senderId,
                receiverId,
                amount);

        return transfer.getId();
    }

    /**
     * Add an income to a wallet
     *
     * @param walletId    The id of the wallet that receives the income
     * @param category    The category of the income
     * @param date        The date of the income
     * @param amount      The amount of the income
     * @param description A description of the income
     * @param status      The status of the transaction
     * @return The id of the created transaction
     * @throws EntityNotFoundException  If the wallet does not exist
     * @throws IllegalArgumentException If the amount is less than or equal to zero
     */
    @Transactional
    public Integer addIncome(
            Integer walletId,
            Category category,
            LocalDateTime date,
            BigDecimal amount,
            String description,
            TransactionStatus status) {
        Wallet wallet =
                walletRepository
                        .findById(walletId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Wallet with id %d not found", walletId)));

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // Round the amount to two decimal places
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        WalletTransaction wt =
                WalletTransaction.builder()
                        .wallet(wallet)
                        .category(category)
                        .type(TransactionType.INCOME)
                        .status(status)
                        .date(date)
                        .amount(amount)
                        .description(description)
                        .build();

        walletTransactionRepository.save(wt);

        if (status == TransactionStatus.CONFIRMED) {
            walletService.incrementWalletBalance(walletId, amount);
        }

        logger.info(
                "Income with status {} of {} added to wallet with id {}", status, amount, walletId);

        return wt.getId();
    }

    /**
     * Add an expense to a wallet
     *
     * @param walletId    The id of the wallet that receives the expense
     * @param category    The category of the expense
     * @param date        The date of the expense
     * @param amount      The amount of the expense
     * @param description A description of the expense
     * @param status      The status of the transaction
     * @return The id of the created transaction
     * @throws EntityNotFoundException  If the wallet does not exist
     * @throws IllegalArgumentException If the amount is less than or equal to zero
     */
    @Transactional
    public Integer addExpense(
            Integer walletId,
            Category category,
            LocalDateTime date,
            BigDecimal amount,
            String description,
            TransactionStatus status) {
        Wallet wallet =
                walletRepository
                        .findById(walletId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Wallet with id %d not found", walletId)));

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // Round the amount to two decimal places
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        WalletTransaction wt =
                WalletTransaction.builder()
                        .wallet(wallet)
                        .category(category)
                        .type(TransactionType.EXPENSE)
                        .status(status)
                        .date(date)
                        .amount(amount)
                        .description(description)
                        .build();

        walletTransactionRepository.save(wt);

        if (status.equals(TransactionStatus.CONFIRMED)) {
            walletService.decrementWalletBalance(walletId, amount);
        }

        logger.info(
                "Expense with status {} of {} added to wallet with id {}",
                status,
                amount,
                walletId);

        return wt.getId();
    }

    /**
     * Update a transaction
     *
     * @param transaction The transaction to be updated
     * @throws EntityNotFoundException  If the transaction does not exist
     * @throws EntityNotFoundException  If the wallet does not exist
     * @throws IllegalArgumentException If the amount is less than or equal to zero
     * @throws IllegalStateException    If the transaction type does not exist
     * @throws IllegalStateException    If the transaction status does not exist
     */
    @Transactional
    public void updateTransaction(WalletTransaction transaction) {
        WalletTransaction oldTransaction =
                walletTransactionRepository
                        .findById(transaction.getId())
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Transaction with id %d not found",
                                                        transaction.getId())));

        // Check if the wallet exists
        if (!walletTransactionRepository.existsWalletByTransactionId(transaction.getId())) {
            throw new EntityNotFoundException(
                    String.format(
                            "Wallet with name %s not found", transaction.getWallet().getName()));
        }

        // Check if the amount is greater than zero
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than or equal to zero");
        }

        // Round the amount to two decimal places
        transaction.setAmount(transaction.getAmount().setScale(2, RoundingMode.HALF_UP));

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

        logger.info("Transaction with id {} updated successfully", transaction.getId());
    }

    /**
     * Updates an existing transfer, adjusting the balances of all affected wallets.
     *
     * @param updatedTransfer The Transfer object with the updated information.
     * @throws EntityNotFoundException if the transfer or any involved wallet is not found.
     * @throws MoinexException.SameSourceDestinationException if the new sender and receiver are the same.
     * @throws IllegalArgumentException if the new amount is zero or negative.
     * @throws MoinexException.InsufficientResourcesException if the new sender wallet has insufficient funds.
     */
    @Transactional
    public void updateTransfer(Transfer updatedTransfer) {
        Transfer oldTransfer =
                transferRepository
                        .findById(updatedTransfer.getId())
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Transfer with id "
                                                        + updatedTransfer.getId()
                                                        + " not found."));

        Wallet newSender = updatedTransfer.getSenderWallet();
        Wallet newReceiver = updatedTransfer.getReceiverWallet();

        BigDecimal newAmount = updatedTransfer.getAmount().setScale(2, RoundingMode.HALF_UP);

        if (newSender.getId().equals(newReceiver.getId())) {
            throw new MoinexException.SameSourceDestinationException(
                    "Sender and receiver wallets must be different");
        }
        if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount to transfer must be greater than zero");
        }

        revertTransferWalletBalances(oldTransfer, updatedTransfer);

        oldTransfer.setSenderWallet(newSender);
        oldTransfer.setReceiverWallet(newReceiver);
        oldTransfer.setAmount(newAmount);
        oldTransfer.setDate(updatedTransfer.getDate());
        oldTransfer.setDescription(updatedTransfer.getDescription());
        oldTransfer.setCategory(updatedTransfer.getCategory());

        transferRepository.save(oldTransfer);
        logger.info("Transfer with id {} updated successfully.", oldTransfer.getId());
    }

    /**
     * Deletes a transfer and reverts the balance changes in the respective wallets.
     *
     * @param transferId The ID of the transfer to be deleted.
     * @throws EntityNotFoundException if the transfer is not found.
     */
    @Transactional
    public void deleteTransfer(Integer transferId) {
        Transfer transfer =
                transferRepository
                        .findById(transferId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Transfer with id " + transferId + " not found."));

        Wallet senderWallet = transfer.getSenderWallet();
        Wallet receiverWallet = transfer.getReceiverWallet();
        BigDecimal amount = transfer.getAmount();

        logger.info(
                "Reverting transfer {}: returning {} to sender {} and debiting from receiver {}",
                transferId,
                amount,
                senderWallet.getId(),
                receiverWallet.getId());
        walletService.incrementWalletBalance(senderWallet.getId(), amount);
        walletService.decrementWalletBalance(receiverWallet.getId(), amount);

        transferRepository.delete(transfer);

        logger.info("Transfer with id {} deleted successfully.", transferId);
    }

    /**
     * Reverts the wallet balances based on the old and new transfer details.
     *
     * @param oldTransfer The original transfer before the update.
     * @param newTransfer The updated transfer with new details.
     */
    private void revertTransferWalletBalances(Transfer oldTransfer, Transfer newTransfer) {
        Wallet oldSender = oldTransfer.getSenderWallet();
        Wallet oldReceiver = oldTransfer.getReceiverWallet();
        BigDecimal oldAmount = oldTransfer.getAmount();

        Wallet newSender = newTransfer.getSenderWallet();
        Wallet newReceiver = newTransfer.getReceiverWallet();
        BigDecimal newAmount = newTransfer.getAmount().setScale(2, RoundingMode.HALF_UP);

        boolean senderChanged = !oldSender.getId().equals(newSender.getId());
        boolean receiverChanged = !oldReceiver.getId().equals(newReceiver.getId());

        if (!senderChanged && !receiverChanged) {
            // Case 1: Only the amount possibly changed
            BigDecimal difference = newAmount.subtract(oldAmount);
            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                // Amount increased -> debit difference from sender and credit to receiver
                walletService.decrementWalletBalance(newSender.getId(), difference);
                walletService.incrementWalletBalance(newReceiver.getId(), difference);
            } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
                // Amount decreased -> credit difference to sender and debit from receiver
                BigDecimal absDiff = difference.abs();
                walletService.incrementWalletBalance(newSender.getId(), absDiff);
                walletService.decrementWalletBalance(newReceiver.getId(), absDiff);
            }
        } else if (senderChanged && !receiverChanged) {
            // Case 2a: Sender changed

            walletService.incrementWalletBalance(oldSender.getId(), oldAmount);
            walletService.decrementWalletBalance(newSender.getId(), newAmount);

            if (!oldAmount.equals(newAmount)) {
                BigDecimal difference = newAmount.subtract(oldAmount);

                if (difference.compareTo(BigDecimal.ZERO) > 0) {
                    walletService.incrementWalletBalance(newReceiver.getId(), difference);
                } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
                    walletService.decrementWalletBalance(newReceiver.getId(), difference.abs());
                }
            }
        } else if (!senderChanged && receiverChanged) {
            // Case 2b: Receiver changed

            walletService.decrementWalletBalance(oldReceiver.getId(), oldAmount);
            walletService.incrementWalletBalance(newReceiver.getId(), newAmount);

            if (!oldAmount.equals(newAmount)) {
                BigDecimal difference = newAmount.subtract(oldAmount);
                if (difference.compareTo(BigDecimal.ZERO) > 0) {
                    walletService.decrementWalletBalance(newSender.getId(), difference);
                } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
                    walletService.incrementWalletBalance(newSender.getId(), difference.abs());
                }
            }
        } else {
            // Case 3: Both sender and receiver changed
            walletService.incrementWalletBalance(oldSender.getId(), oldAmount);
            walletService.decrementWalletBalance(oldReceiver.getId(), oldAmount);

            walletService.decrementWalletBalance(newSender.getId(), newAmount);
            walletService.incrementWalletBalance(newReceiver.getId(), newAmount);
        }
    }

    /**
     * Change the type of transaction
     *
     * @param oldTransaction The transaction to be updated
     * @param newType        The new type of the transaction
     * @throws IllegalStateException If the transaction type does not exist
     * @note This method persists the changes in the wallet balances
     * and the transaction in the database
     */
    private void changeTransactionType(WalletTransaction oldTransaction, TransactionType newType) {
        if (oldTransaction.getType().equals(newType)) {
            logger.info(
                    "Transaction with id {} has the same type as before", oldTransaction.getId());

            return;
        }

        Wallet wallet = oldTransaction.getWallet();

        TransactionType oldType = oldTransaction.getType();

        if (oldTransaction.getStatus().equals(TransactionStatus.CONFIRMED)) {
            // Revert the old transaction
            if (oldType.equals(TransactionType.EXPENSE)) {
                walletService.incrementWalletBalance(wallet.getId(), oldTransaction.getAmount());

            } else if (oldType.equals(TransactionType.INCOME)) {
                walletService.decrementWalletBalance(wallet.getId(), oldTransaction.getAmount());

            } else {
                // WARNING for the case of new types being added to the enum
                // and not being handled here
                throw new IllegalStateException("Transaction type not recognized");
            }

            // Apply the new transaction
            if (newType.equals(TransactionType.EXPENSE)) {
                walletService.decrementWalletBalance(wallet.getId(), oldTransaction.getAmount());

            } else if (newType.equals(TransactionType.INCOME)) {
                walletService.incrementWalletBalance(wallet.getId(), oldTransaction.getAmount());

            } else {
                // WARNING for the case of new types being added to the enum
                // and not being handled here
                throw new IllegalStateException("Transaction type not recognized");
            }
        }

        oldTransaction.setType(newType);
        walletTransactionRepository.save(oldTransaction);
        logger.info("Transaction with id {} type changed to {}", oldTransaction.getId(), newType);
    }

    /**
     * Change the wallet of a transaction
     *
     * @param oldTransaction The transaction to be updated
     * @param newWallet      The new wallet of the transaction
     * @throws IllegalStateException If the transaction type does not exist
     * @note This method persists the changes in the wallet balances
     * and the transaction in the database
     */
    private void changeTransactionWallet(WalletTransaction oldTransaction, Wallet newWallet) {
        if (oldTransaction.getWallet().getId().equals(newWallet.getId())) {
            logger.info(
                    "Transaction with id {} has the same wallet as before", oldTransaction.getId());

            return;
        }

        Wallet oldWallet = oldTransaction.getWallet();

        if (oldTransaction.getStatus().equals(TransactionStatus.CONFIRMED)) {
            if (oldTransaction.getType().equals(TransactionType.EXPENSE)) {
                // Revert expense from old wallet
                walletService.incrementWalletBalance(oldWallet.getId(), oldTransaction.getAmount());

                // Apply expense to new wallet
                walletService.decrementWalletBalance(newWallet.getId(), oldTransaction.getAmount());

            } else if (oldTransaction.getType().equals(TransactionType.INCOME)) {
                // Revert income from old wallet
                walletService.decrementWalletBalance(oldWallet.getId(), oldTransaction.getAmount());

                // Apply income to new wallet
                walletService.incrementWalletBalance(newWallet.getId(), oldTransaction.getAmount());
            } else {
                // WARNING for the case of new types being added to the enum
                // and not being handled here
                throw new IllegalStateException("Transaction type not recognized");
            }
        }

        oldTransaction.setWallet(newWallet);
        walletTransactionRepository.save(oldTransaction);

        logger.info(
                "Transaction with id {} wallet changed to {}",
                oldTransaction.getId(),
                newWallet.getName());
    }

    /**
     * Change the amount of a transaction
     *
     * @param oldTransaction The transaction to be updated
     * @param newAmount      The new amount of the transaction
     * @throws IllegalStateException If the transaction type does not exist
     * @note This method persists the changes in the wallet balances
     * and the transaction in the database
     */
    private void changeTransactionAmount(WalletTransaction oldTransaction, BigDecimal newAmount) {
        BigDecimal oldAmount = oldTransaction.getAmount();

        BigDecimal diff = oldAmount.subtract(newAmount).abs();

        // Check if the new amount is the same as the old amount
        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            logger.info(
                    "Transaction with id {} has the same amount as before", oldTransaction.getId());

            return;
        }

        Wallet wallet = oldTransaction.getWallet();

        // Apply the difference to the wallet balance
        if (oldTransaction.getStatus().equals(TransactionStatus.CONFIRMED)) {
            if (oldTransaction.getType().equals(TransactionType.EXPENSE)) {

                if (oldAmount.compareTo(newAmount) > 0) {
                    walletService.incrementWalletBalance(wallet.getId(), diff);

                } else {
                    walletService.decrementWalletBalance(wallet.getId(), diff);
                }
            } else if (oldTransaction.getType().equals(TransactionType.INCOME)) {
                if (oldAmount.compareTo(newAmount) > 0) {
                    walletService.decrementWalletBalance(wallet.getId(), diff);

                } else {
                    walletService.incrementWalletBalance(wallet.getId(), diff);
                }
            } else {
                // WARNING for the case of new types being added to the enum
                // and not being handled here
                throw new IllegalStateException("Transaction type not recognized");
            }
        }

        oldTransaction.setAmount(newAmount);
        walletTransactionRepository.save(oldTransaction);

        logger.info(
                "Transaction with id {} amount changed to {}", oldTransaction.getId(), newAmount);
    }

    /**
     * Change the status of a transaction
     *
     * @param transaction The transaction to be updated
     * @param newStatus   The new status of the transaction
     * @throws IllegalStateException If the transaction status does not exist
     * @throws IllegalStateException If the transaction type does not exist
     * @note This method persists the changes in the wallet balances
     * and the transaction in the database
     */
    private void changeTransactionStatus(
            WalletTransaction transaction, TransactionStatus newStatus) {
        if (transaction.getStatus().equals(newStatus)) {
            logger.info(
                    "Transaction with id {} has the same status as before", transaction.getId());

            return;
        }

        Wallet wallet = transaction.getWallet();
        TransactionStatus oldStatus = transaction.getStatus();

        if (transaction.getType().equals(TransactionType.EXPENSE)) {
            if (oldStatus.equals(TransactionStatus.CONFIRMED)) {
                if (newStatus.equals(TransactionStatus.PENDING)) {
                    // Revert the expense
                    walletService.incrementWalletBalance(wallet.getId(), transaction.getAmount());
                }
            } else if (oldStatus.equals(TransactionStatus.PENDING)) {
                if (newStatus.equals(TransactionStatus.CONFIRMED)) {
                    // Apply the expense
                    walletService.decrementWalletBalance(wallet.getId(), transaction.getAmount());
                }
            } else {
                // WARNING for the case of new status being added to the enum
                // and not being handled here
                throw new IllegalStateException("Transaction status not recognized");
            }
        } else if (transaction.getType().equals(TransactionType.INCOME)) {
            if (oldStatus.equals(TransactionStatus.CONFIRMED)) {
                if (newStatus.equals(TransactionStatus.PENDING)) {
                    // Revert the income
                    walletService.decrementWalletBalance(wallet.getId(), transaction.getAmount());
                }
            } else if (oldStatus.equals(TransactionStatus.PENDING)) {
                if (newStatus.equals(TransactionStatus.CONFIRMED)) {
                    // Apply the income
                    walletService.incrementWalletBalance(wallet.getId(), transaction.getAmount());
                }
            } else {
                // WARNING for the case of new status being added to the enum
                // and not being handled here
                throw new IllegalStateException("Transaction status not recognized");
            }
        } else {
            // WARNING for the case of new types being added to the enum
            // and not being handled here
            throw new IllegalStateException("Transaction type not recognized");
        }

        transaction.setStatus(newStatus);

        walletTransactionRepository.save(transaction);

        logger.info("Transaction with id {} status changed to {}", transaction.getId(), newStatus);
    }

    /**
     * Delete a transaction from a wallet
     *
     * @param transactionId The id of the transaction to be removed
     * @throws EntityNotFoundException If the transaction does not exist
     */
    @Transactional
    public void deleteTransaction(Integer transactionId) {
        WalletTransaction transaction =
                walletTransactionRepository
                        .findById(transactionId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Transaction with id %d not found",
                                                        transactionId)));

        Wallet wallet = transaction.getWallet();

        // Update the wallet balance if the transaction is confirmed
        if (transaction.getStatus().equals(TransactionStatus.CONFIRMED)) {
            BigDecimal amount = transaction.getAmount();
            if (transaction.getType().equals(TransactionType.INCOME)) {
                walletService.decrementWalletBalance(wallet.getId(), amount);
            } else {
                walletService.incrementWalletBalance(wallet.getId(), amount);
            }
        }

        walletTransactionRepository.delete(transaction);

        logger.info("Transaction {} deleted from wallet {}", transactionId, wallet.getName());
    }

    /**
     * Get transaction by id
     *
     * @param id The id of the transaction
     * @return The transaction with the provided id
     * @throws EntityNotFoundException If the transaction does not exist
     */
    public WalletTransaction getTransactionById(Integer id) {
        return walletTransactionRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "Transaction with id " + id + " not found"));
    }

    /**
     * Get all income transactions where both wallet and category are not archived
     *
     * @return A list with all income transactions
     */
    public List<WalletTransaction> getNonArchivedIncomes() {
        return walletTransactionRepository.findNonArchivedIncomeTransactions();
    }

    /**
     * Get all expense transactions where both wallet and category are not archived
     *
     * @return A list with all expense transactions
     */
    public List<WalletTransaction> getNonArchivedExpenses() {
        return walletTransactionRepository.findNonArchivedExpenseTransactions();
    }

    /**
     * Get all transactions by month
     *
     * @param month The month of the transactions
     * @param year  The year of the transactions
     */
    public List<WalletTransaction> getTransactionsByMonth(Integer month, Integer year) {
        return walletTransactionRepository.findTransactionsByMonth(month, year);
    }

    /**
     * Get all transactions by month when both wallet and category are not archived
     *
     * @param month The month of the transactions
     * @param year  The year of the transactions
     */
    public List<WalletTransaction> getNonArchivedTransactionsByMonth(Integer month, Integer year) {
        return walletTransactionRepository.findNonArchivedTransactionsByMonth(month, year);
    }

    /**
     * Get all transactions by year when both wallet and category are not archived
     *
     * @param year The year of the transactions
     * @return A list with all transactions of the year
     */
    public List<WalletTransaction> getNonArchivedTransactionsByYear(Integer year) {
        return walletTransactionRepository.findNonArchivedTransactionsByYear(year);
    }

    /**
     * Get all transactions by wallet where both wallet and category are not archived
     *
     * @param walletId The id of the wallet
     * @param month    The month of the transactions
     * @param year     The year of the transactions
     */
    public List<WalletTransaction> getNonArchivedTransactionsByWalletAndMonth(
            Integer walletId, Integer month, Integer year) {
        return walletTransactionRepository.findNonArchivedTransactionsByWalletAndMonth(
                walletId, month, year);
    }

    /**
     * Get all transactions between two dates where both wallet and category are not
     * archived
     *
     * @param startDate The start date
     * @param endDate   The end date
     * @return A list with all transactions between the two dates
     */
    public List<WalletTransaction> getNonArchivedTransactionsBetweenDates(
            LocalDateTime startDate, LocalDateTime endDate) {
        String startDateStr = startDate.format(Constants.DB_DATE_FORMATTER);
        String endDateStr = endDate.format(Constants.DB_DATE_FORMATTER);

        return walletTransactionRepository.findNonArchivedTransactionsBetweenDates(
                startDateStr, endDateStr);
    }

    /**
     * Get all confirmed transactions by month when both wallet and category are not
     * archived
     *
     * @param month The month of the transactions
     * @param year  The year of the transactions
     */
    public List<WalletTransaction> getNonArchivedConfirmedTransactionsByMonth(
            Integer month, Integer year) {
        return walletTransactionRepository.findNonArchivedConfirmedTransactionsByMonth(month, year);
    }

    /**
     * Get the last n transactions of all wallets both wallet and category are not
     * archived
     *
     * @param n The number of transactions to get
     * @return A list with the last n transactions of all wallets
     */
    public List<WalletTransaction> getNonArchivedLastTransactions(Integer n) {
        return walletTransactionRepository.findNonArchivedLastTransactions(PageRequest.ofSize(n));
    }

    /**
     * Get the date of the oldest transaction
     *
     * @return The date of the oldest transaction or the current date if there are no
     * transactions
     */
    public LocalDateTime getOldestTransactionDate() {
        String date = walletTransactionRepository.findOldestTransactionDate();

        if (date == null) {
            return LocalDateTime.now();
        }

        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    /**
     * Get count of transactions by wallet
     *
     * @param walletId The id of the wallet
     * @return The count of transactions in the wallet
     */
    public Integer getTransactionCountByWallet(Integer walletId) {
        return walletTransactionRepository.getTransactionCountByWallet(walletId)
                + transferRepository.getTransferCountByWallet(walletId);
    }

    /**
     * Get the transfers by wallet and month
     *
     * @param walletId The id of the wallet
     * @param month    The month
     * @param year     The year
     * @return A list with the transfers in the wallet by month
     */
    public List<Transfer> getTransfersByWalletAndMonth(
            Integer walletId, Integer month, Integer year) {
        return transferRepository.findTransfersByWalletAndMonth(walletId, month, year);
    }

    /**
     * Get income suggestions. Suggestions are transactions with distinct descriptions
     * and the most recent date
     *
     * @return A list with the suggestions
     */
    public List<WalletTransaction> getIncomeSuggestions() {
        return walletTransactionRepository.findSuggestions(TransactionType.INCOME);
    }

    /**
     * Get expense suggestions. Suggestions are transactions with distinct descriptions
     * and the most recent date
     *
     * @return A list with the suggestions
     */
    public List<WalletTransaction> getExpenseSuggestions() {
        return walletTransactionRepository.findSuggestions(TransactionType.EXPENSE);
    }

    /**
     * Get transfer suggestions. Suggestions are transactions with distinct descriptions
     * and the most recent date
     *
     * @return A list with the suggestions
     */
    public List<Transfer> getTransferSuggestions() {
        return transferRepository.findSuggestions();
    }

    public List<Transfer> getAllTransfers() {
        return transferRepository.findAll();
    }
}
