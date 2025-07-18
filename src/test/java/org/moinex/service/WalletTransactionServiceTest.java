/*
 * Filename: WalletTransactionServiceTest.java
 * Created on: October 16, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
class WalletTransactionServiceTest {
    @Mock private WalletRepository walletRepository;

    @Mock private TransferRepository transferRepository;

    @Mock private WalletTransactionRepository walletTransactionRepository;

    @InjectMocks private WalletTransactionService walletTransactionService;

    private Wallet wallet1;
    private Wallet wallet2;
    private Transfer transfer;
    private WalletTransaction wallet1IncomeTransaction;
    private WalletTransaction wallet1ExpenseTransaction;
    private Category category;
    private LocalDateTime date;
    private BigDecimal incomeAmount;
    private BigDecimal expenseAmount;
    private BigDecimal transferAmount;
    private final String description = "";

    private Wallet createWallet(Integer id, String name, BigDecimal balance) {
        return new Wallet(id, name, balance);
    }

    private Transfer createTransfer(
            Integer id, Wallet sender, Wallet receiver, LocalDateTime date, BigDecimal amount) {
        return new Transfer(id, sender, receiver, date, amount, "");
    }

    private WalletTransaction createWalletTransaction(
            Wallet wallet,
            Category category,
            TransactionType type,
            TransactionStatus status,
            LocalDateTime date,
            BigDecimal amount) {
        return WalletTransaction.builder()
                .wallet(wallet)
                .category(category)
                .type(type)
                .status(status)
                .date(date)
                .amount(amount)
                .description("")
                .build();
    }

    @BeforeAll
    static void setUp() {
        MockitoAnnotations.openMocks(WalletTransactionServiceTest.class);
    }

    @BeforeEach
    void beforeEach() {
        incomeAmount = new BigDecimal("500");
        expenseAmount = new BigDecimal("200");
        transferAmount = new BigDecimal("125.5");

        date = LocalDateTime.now();
        category = Category.builder().name("Category").build();

        wallet1 = createWallet(1, "Wallet1", new BigDecimal("1000"));
        wallet2 = createWallet(2, "Wallet2", new BigDecimal("2000"));

        transfer = createTransfer(1, wallet1, wallet2, date, transferAmount);

        wallet1IncomeTransaction =
                createWalletTransaction(
                        wallet1,
                        category,
                        TransactionType.INCOME,
                        TransactionStatus.CONFIRMED,
                        date,
                        incomeAmount);

        wallet1ExpenseTransaction =
                createWalletTransaction(
                        wallet1,
                        category,
                        TransactionType.EXPENSE,
                        TransactionStatus.CONFIRMED,
                        date,
                        expenseAmount);
    }

    @Test
    @DisplayName("Test if the money transfer is successful")
    void testTransferMoneySuccess() {
        BigDecimal senderPreviousBalance = wallet1.getBalance();
        BigDecimal receiverPreviousBalance = wallet2.getBalance();

        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));
        when(walletRepository.findById(wallet2.getId())).thenReturn(Optional.of(wallet2));

        when(walletRepository.save(wallet1)).thenReturn(wallet1);
        when(walletRepository.save(wallet2)).thenReturn(wallet2);

        when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);

        walletTransactionService.transferMoney(
                wallet1.getId(),
                wallet2.getId(),
                transfer.getDate(),
                transfer.getAmount(),
                transfer.getDescription());

        // Check if the sender and receiver balances were updated
        verify(walletRepository).findById(wallet1.getId());
        verify(walletRepository).findById(wallet2.getId());
        verify(walletRepository).save(wallet1);
        verify(walletRepository).save(wallet2);

        assertEquals(
                senderPreviousBalance.subtract(transferAmount).doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);

        assertEquals(
                receiverPreviousBalance.add(transferAmount).doubleValue(),
                wallet2.getBalance().doubleValue(),
                Constants.EPSILON);

        // Check if the transfer was saved
        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);

        verify(transferRepository).save(transferCaptor.capture());

        assertEquals(wallet1, transferCaptor.getValue().getSenderWallet());
        assertEquals(wallet2, transferCaptor.getValue().getReceiverWallet());
        assertEquals(
                transferAmount.doubleValue(),
                transferCaptor.getValue().getAmount().doubleValue(),
                Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if exception is thrown when the sender wallet does not exist")
    void testTransferMoneySenderDoesNotExist() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.empty());

        Integer senderWalletId = wallet1.getId();
        Integer receiverWalletId = wallet2.getId();
        BigDecimal amountToTransfer = transferAmount;

        assertThrows(
                EntityNotFoundException.class,
                () ->
                        walletTransactionService.transferMoney(
                                senderWalletId,
                                receiverWalletId,
                                date,
                                amountToTransfer,
                                description));

        // Verify that the transfer was not saved
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when the receiver wallet does not exist")
    void testTransferMoneyReceiverDoesNotExist() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

        when(walletRepository.findById(wallet2.getId())).thenReturn(Optional.empty());

        Integer senderWalletId = wallet1.getId();
        Integer receiverWalletId = wallet2.getId();

        assertThrows(
                EntityNotFoundException.class,
                () ->
                        walletTransactionService.transferMoney(
                                senderWalletId,
                                receiverWalletId,
                                date,
                                transferAmount,
                                description),
                "Receiver wallet does not exist");

        // Verify that the transfer was not saved
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when the sender and receiver wallets are the same")
    void testTransferMoneySameWallet() {
        assertThrows(
                MoinexException.SameSourceDestinationException.class,
                () ->
                        walletTransactionService.transferMoney(
                                wallet1.getId(),
                                wallet1.getId(),
                                date,
                                transferAmount,
                                description));

        // Verify that the transfer was not saved
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName(
            "Test if exception is thrown when the amount to transfer is less "
                    + "than or equal to zero")
    void testTransferMoneyAmountZero() {
        Integer senderWalletId = wallet1.getId();
        Integer receiverWalletId = wallet2.getId();
        BigDecimal zeroAmount = new BigDecimal("0.0");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        walletTransactionService.transferMoney(
                                senderWalletId, receiverWalletId, date, zeroAmount, description));

        // Verify that the transfer was not saved
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Test if the confirmed income is added successfully")
    void testAddConfirmedIncome() {
        BigDecimal previousBalance = wallet1.getBalance();

        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));
        when(walletRepository.save(wallet1)).thenReturn(wallet1);

        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(
                        WalletTransaction.builder()
                                .wallet(wallet1)
                                .category(category)
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.CONFIRMED)
                                .date(date)
                                .amount(incomeAmount)
                                .description(description)
                                .build());

        walletTransactionService.addIncome(
                wallet1.getId(),
                category,
                date,
                incomeAmount,
                description,
                TransactionStatus.CONFIRMED);

        // Check if the wallet balance was updated
        verify(walletRepository).save(wallet1);
        assertEquals(
                previousBalance.add(incomeAmount).doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if the pending income is added successfully")
    void testAddPendingIncome() {
        BigDecimal previousBalance = wallet1.getBalance();

        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(
                        WalletTransaction.builder()
                                .wallet(wallet1)
                                .category(category)
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.PENDING)
                                .date(date)
                                .amount(incomeAmount)
                                .description(description)
                                .build());

        walletTransactionService.addIncome(
                wallet1.getId(),
                category,
                date,
                incomeAmount,
                description,
                TransactionStatus.PENDING);

        // Check if the wallet balance is the same
        verify(walletRepository, never()).save(any(Wallet.class));
        assertEquals(
                previousBalance.doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);
    }

    @Test
    @DisplayName(
            "Test if exception is thrown when the wallet to receive the income " + "does not exist")
    void testAddIncomeWalletDoesNotExist() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.empty());

        Integer walletId = wallet1.getId();

        // Check for confirmed income
        assertThrows(
                EntityNotFoundException.class,
                () ->
                        walletTransactionService.addIncome(
                                walletId,
                                category,
                                date,
                                incomeAmount,
                                description,
                                TransactionStatus.CONFIRMED));

        // Check for pending income
        assertThrows(
                EntityNotFoundException.class,
                () ->
                        walletTransactionService.addIncome(
                                walletId,
                                category,
                                date,
                                incomeAmount,
                                description,
                                TransactionStatus.PENDING));

        // Verify that the income was not added
        verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Test if the confirmed expense is added successfully")
    void testAddConfirmedExpense() {
        BigDecimal previousBalance = wallet1.getBalance();

        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));
        when(walletRepository.save(wallet1)).thenReturn(wallet1);

        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(
                        WalletTransaction.builder()
                                .wallet(wallet1)
                                .category(category)
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.CONFIRMED)
                                .date(date)
                                .amount(expenseAmount)
                                .description(description)
                                .build());

        walletTransactionService.addExpense(
                wallet1.getId(),
                category,
                date,
                expenseAmount,
                description,
                TransactionStatus.CONFIRMED);

        // Check if the wallet balance was updated
        verify(walletRepository).save(wallet1);
        assertEquals(
                previousBalance.subtract(expenseAmount).doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if the pending expense is added successfully")
    void testAddPendingExpense() {
        BigDecimal previousBalance = wallet1.getBalance();

        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(
                        WalletTransaction.builder()
                                .wallet(wallet1)
                                .category(category)
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.PENDING)
                                .date(date)
                                .amount(expenseAmount)
                                .description(description)
                                .build());

        walletTransactionService.addExpense(
                wallet1.getId(),
                category,
                date,
                expenseAmount,
                description,
                TransactionStatus.PENDING);

        // Check if the wallet balance is the same
        verify(walletRepository, never()).save(wallet1);
        assertEquals(
                previousBalance.doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);
    }

    @Test
    @DisplayName(
            "Test if exception is thrown when the wallet to receive the expense "
                    + "does not exist")
    void testAddExpenseWalletDoesNotExist() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.empty());

        Integer walletId = wallet1.getId();

        // Check for confirmed expense
        assertThrows(
                EntityNotFoundException.class,
                () ->
                        walletTransactionService.addExpense(
                                walletId,
                                category,
                                date,
                                expenseAmount,
                                description,
                                TransactionStatus.CONFIRMED));

        // Check for pending expense
        assertThrows(
                EntityNotFoundException.class,
                () ->
                        walletTransactionService.addExpense(
                                walletId,
                                category,
                                date,
                                expenseAmount,
                                description,
                                TransactionStatus.PENDING));

        // Verify that the expense was not added
        verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName(
            "Test if transaction type is changed from EXPENSE to INCOME and "
                    + "wallet balance is updated correctly")
    void testChangeTransactionTypeFromExpenseToIncome() {
        // Setup previous state
        BigDecimal oldBalance = wallet1.getBalance();
        BigDecimal expenseTransactionAmount = wallet1ExpenseTransaction.getAmount();
        BigDecimal newIncomeAmount = new BigDecimal("300.0");

        WalletTransaction updatedTransaction =
                createWalletTransaction(
                        wallet1,
                        category,
                        TransactionType.INCOME,
                        TransactionStatus.CONFIRMED,
                        date,
                        newIncomeAmount);

        when(walletTransactionRepository.findById(wallet1ExpenseTransaction.getId()))
                .thenReturn(Optional.of(wallet1ExpenseTransaction));

        when(walletTransactionRepository.existsWalletByTransactionId(
                        wallet1ExpenseTransaction.getId()))
                .thenReturn(true);

        when(walletRepository.save(wallet1)).thenReturn(wallet1);

        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(updatedTransaction);

        walletTransactionService.updateTransaction(updatedTransaction);

        // Verify that the old expense was reverted and new income was applied
        assertEquals(
                oldBalance.add(expenseTransactionAmount).add(newIncomeAmount).doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);

        // Verify repository interactions
        verify(walletTransactionRepository).findById(wallet1ExpenseTransaction.getId());
        verify(walletRepository, times(2)).save(wallet1);
        verify(walletTransactionRepository, times(3)).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName(
            "Test if transaction status is changed from CONFIRMED to PENDING "
                    + "and balance is reverted")
    void testChangeTransactionStatusFromConfirmedToPending() {
        BigDecimal oldBalance = wallet1.getBalance();
        WalletTransaction updatedTransaction =
                createWalletTransaction(
                        wallet1,
                        category,
                        TransactionType.EXPENSE,
                        TransactionStatus.PENDING,
                        date,
                        expenseAmount);

        when(walletTransactionRepository.findById(wallet1ExpenseTransaction.getId()))
                .thenReturn(Optional.of(wallet1ExpenseTransaction));

        when(walletTransactionRepository.existsWalletByTransactionId(
                        wallet1ExpenseTransaction.getId()))
                .thenReturn(true);

        when(walletRepository.save(wallet1)).thenReturn(wallet1);

        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(updatedTransaction);

        walletTransactionService.updateTransaction(updatedTransaction);

        // Verify that the balance was reverted for the expense
        assertEquals(
                oldBalance.add(expenseAmount).doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);

        // Verify repository interactions
        verify(walletTransactionRepository).findById(wallet1ExpenseTransaction.getId());
        verify(walletRepository).save(wallet1);
        verify(walletTransactionRepository, times(2)).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Test if the wallet is changed and transaction is applied to the new wallet")
    void testChangeTransactionWallet() {
        BigDecimal oldWallet1Balance = wallet1.getBalance();
        BigDecimal oldWallet2Balance = wallet2.getBalance();
        BigDecimal amount = wallet1IncomeTransaction.getAmount();

        WalletTransaction updatedTransaction =
                createWalletTransaction(
                        wallet2,
                        category,
                        TransactionType.INCOME,
                        TransactionStatus.CONFIRMED,
                        date,
                        amount);

        when(walletTransactionRepository.findById(wallet1IncomeTransaction.getId()))
                .thenReturn(Optional.of(wallet1IncomeTransaction));

        when(walletTransactionRepository.existsWalletByTransactionId(
                        wallet1ExpenseTransaction.getId()))
                .thenReturn(true);

        when(walletRepository.save(wallet1)).thenReturn(wallet1);

        when(walletRepository.save(wallet2)).thenReturn(wallet2);

        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(updatedTransaction);

        walletTransactionService.updateTransaction(updatedTransaction);

        // Verify that the amount was reverted from the old wallet and added to the new
        // wallet
        assertEquals(
                oldWallet1Balance.subtract(amount).doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);

        assertEquals(
                oldWallet2Balance.add(amount).doubleValue(),
                wallet2.getBalance().doubleValue(),
                Constants.EPSILON);

        // Verify repository interactions
        verify(walletTransactionRepository).findById(wallet1IncomeTransaction.getId());
        verify(walletRepository).save(wallet1);
        verify(walletRepository).save(wallet2);
        verify(walletTransactionRepository, times(2)).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName(
            "Test if transaction amount is increased and wallet balance is " + "updated correctly")
    void testIncreaseTransactionAmount() {
        // Setup previous state
        BigDecimal oldBalance = wallet1.getBalance();
        BigDecimal oldAmount = wallet1ExpenseTransaction.getAmount();
        BigDecimal newAmount = oldAmount.add(new BigDecimal("100.0"));

        WalletTransaction updatedTransaction =
                createWalletTransaction(
                        wallet1,
                        category,
                        TransactionType.EXPENSE,
                        TransactionStatus.CONFIRMED,
                        date,
                        newAmount);

        when(walletTransactionRepository.findById(wallet1ExpenseTransaction.getId()))
                .thenReturn(Optional.of(wallet1ExpenseTransaction));

        when(walletTransactionRepository.existsWalletByTransactionId(
                        wallet1ExpenseTransaction.getId()))
                .thenReturn(true);

        when(walletRepository.save(wallet1)).thenReturn(wallet1);

        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(updatedTransaction);

        walletTransactionService.updateTransaction(updatedTransaction);

        // Verify that the old amount was reverted and new amount was applied
        BigDecimal expectedBalance = oldBalance.add(oldAmount).subtract(newAmount);
        assertEquals(
                expectedBalance.doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);

        // Verify repository interactions
        verify(walletTransactionRepository).findById(wallet1ExpenseTransaction.getId());
        verify(walletRepository, times(1)).save(wallet1);
        verify(walletTransactionRepository, times(2)).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName(
            "Test if transaction amount is decreased and wallet balance is " + "updated correctly")
    void testDecreaseTransactionAmount() {
        // Setup previous state
        BigDecimal oldBalance = wallet1.getBalance();
        BigDecimal oldAmount = wallet1ExpenseTransaction.getAmount();
        BigDecimal newAmount = oldAmount.subtract(new BigDecimal("100.0"));

        WalletTransaction updatedTransaction =
                createWalletTransaction(
                        wallet1,
                        category,
                        TransactionType.EXPENSE,
                        TransactionStatus.CONFIRMED,
                        date,
                        newAmount);

        when(walletTransactionRepository.findById(wallet1ExpenseTransaction.getId()))
                .thenReturn(Optional.of(wallet1ExpenseTransaction));

        when(walletTransactionRepository.existsWalletByTransactionId(
                        wallet1ExpenseTransaction.getId()))
                .thenReturn(true);

        when(walletRepository.save(wallet1)).thenReturn(wallet1);

        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(updatedTransaction);

        walletTransactionService.updateTransaction(updatedTransaction);

        // Verify that the old amount was reverted and new amount was applied
        BigDecimal expectedBalance = oldBalance.add(oldAmount).subtract(newAmount);
        assertEquals(
                expectedBalance.doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);

        // Verify repository interactions
        verify(walletTransactionRepository).findById(wallet1ExpenseTransaction.getId());
        verify(walletRepository, times(1)).save(wallet1);
        verify(walletTransactionRepository, times(2)).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Test if transaction amount remains the same and wallet balance is unaffected")
    void testChangeTransactionAmountWithNoChange() {
        // Setup previous state
        BigDecimal oldBalance = wallet1.getBalance();
        BigDecimal oldAmount = wallet1ExpenseTransaction.getAmount();

        WalletTransaction updatedTransaction =
                createWalletTransaction(
                        wallet1,
                        category,
                        TransactionType.EXPENSE,
                        TransactionStatus.CONFIRMED,
                        date,
                        oldAmount // Same amount as before
                        );

        when(walletTransactionRepository.findById(wallet1ExpenseTransaction.getId()))
                .thenReturn(Optional.of(wallet1ExpenseTransaction));

        when(walletTransactionRepository.existsWalletByTransactionId(
                        wallet1ExpenseTransaction.getId()))
                .thenReturn(true);

        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenReturn(updatedTransaction);

        walletTransactionService.updateTransaction(updatedTransaction);

        // Verify that the balance is unaffected
        assertEquals(
                oldBalance.doubleValue(), wallet1.getBalance().doubleValue(), Constants.EPSILON);

        // Verify repository interactions
        verify(walletTransactionRepository).findById(wallet1ExpenseTransaction.getId());
        verify(walletRepository, never()).save(wallet1); // No save on wallet since no change
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when updating transaction with amount <= 0")
    void testChangeTransactionAmountInvalidAmount() {
        BigDecimal invalidAmount = BigDecimal.ZERO;

        WalletTransaction updatedTransaction =
                createWalletTransaction(
                        wallet1,
                        category,
                        TransactionType.EXPENSE,
                        TransactionStatus.CONFIRMED,
                        date,
                        invalidAmount);

        when(walletTransactionRepository.findById(wallet1ExpenseTransaction.getId()))
                .thenReturn(Optional.of(wallet1ExpenseTransaction));

        when(walletTransactionRepository.existsWalletByTransactionId(
                        wallet1ExpenseTransaction.getId()))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> walletTransactionService.updateTransaction(updatedTransaction));

        // Verify repository interactions
        verify(walletTransactionRepository).findById(wallet1ExpenseTransaction.getId());
        verify(walletRepository, never()).save(wallet1);
        verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Test if an exception is thrown when transaction does not exist")
    void testUpdateNonExistentTransaction() {
        WalletTransaction nonExistentTransaction =
                createWalletTransaction(
                        wallet1,
                        category,
                        TransactionType.INCOME,
                        TransactionStatus.CONFIRMED,
                        date,
                        incomeAmount);

        when(walletTransactionRepository.findById(nonExistentTransaction.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> walletTransactionService.updateTransaction(nonExistentTransaction),
                "Transaction with id " + nonExistentTransaction.getId() + " not found");

        verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Test if the confirmed expense is deleted successfully")
    void testDeleteConfirmedExpense() {
        BigDecimal previousBalance = wallet1.getBalance();

        when(walletTransactionRepository.findById(wallet1ExpenseTransaction.getId()))
                .thenReturn(Optional.of(wallet1ExpenseTransaction));

        walletTransactionService.deleteTransaction(wallet1ExpenseTransaction.getId());

        // Check if the transaction was deleted
        verify(walletTransactionRepository).delete(wallet1ExpenseTransaction);

        // Check if the wallet balance was updated
        verify(walletRepository).save(wallet1);

        assertEquals(
                previousBalance.add(expenseAmount).doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if the pending expense is deleted successfully")
    void testDeletePendingExpense() {
        BigDecimal previousBalance = wallet1.getBalance();
        wallet1ExpenseTransaction.setStatus(TransactionStatus.PENDING);

        when(walletTransactionRepository.findById(wallet1ExpenseTransaction.getId()))
                .thenReturn(Optional.of(wallet1ExpenseTransaction));

        walletTransactionService.deleteTransaction(wallet1ExpenseTransaction.getId());

        // Check if the transaction was deleted
        verify(walletTransactionRepository).delete(wallet1ExpenseTransaction);

        // Check if the wallet balance was not updated
        verify(walletRepository, never()).save(any(Wallet.class));

        assertEquals(
                previousBalance.doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if the confirmed income transaction is deleted successfully")
    void testDeleteConfirmedIncome() {
        BigDecimal previousBalance = wallet1.getBalance();

        when(walletTransactionRepository.findById(wallet1IncomeTransaction.getId()))
                .thenReturn(Optional.of(wallet1IncomeTransaction));

        walletTransactionService.deleteTransaction(wallet1IncomeTransaction.getId());

        // Check if the transaction was deleted
        verify(walletTransactionRepository).delete(wallet1IncomeTransaction);

        // Check if the wallet balance was updated
        verify(walletRepository).save(wallet1);

        assertEquals(
                previousBalance.subtract(incomeAmount).doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if the pending income transaction is deleted successfully")
    void testDeletePendingIncome() {
        BigDecimal previousBalance = wallet1.getBalance();
        wallet1IncomeTransaction.setStatus(TransactionStatus.PENDING);

        when(walletTransactionRepository.findById(wallet1IncomeTransaction.getId()))
                .thenReturn(Optional.of(wallet1IncomeTransaction));

        walletTransactionService.deleteTransaction(wallet1IncomeTransaction.getId());

        // Check if the transaction was deleted
        verify(walletTransactionRepository).delete(wallet1IncomeTransaction);

        // Check if the wallet balance was not updated
        verify(walletRepository, never()).save(any(Wallet.class));

        assertEquals(
                previousBalance.doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if exception is thrown when the transaction to delete does not " + "exist")
    void testDeleteTransactionDoesNotExist() {
        when(walletTransactionRepository.findById(wallet1IncomeTransaction.getId()))
                .thenReturn(Optional.empty());

        Integer transactionId = wallet1IncomeTransaction.getId();

        assertThrows(
                EntityNotFoundException.class,
                () -> walletTransactionService.deleteTransaction(transactionId));
    }

    @Test
    @DisplayName("Test if the income transaction is confirmed successfully")
    void testConfirmIncomeTransaction() {
        wallet1IncomeTransaction.setStatus(TransactionStatus.PENDING);
        BigDecimal previousBalance = wallet1.getBalance();

        when(walletTransactionRepository.findById(wallet1IncomeTransaction.getId()))
                .thenReturn(Optional.of(wallet1IncomeTransaction));

        walletTransactionService.confirmTransaction(wallet1IncomeTransaction.getId());

        // Check if the transaction was confirmed
        verify(walletTransactionRepository).save(wallet1IncomeTransaction);
        assertEquals(TransactionStatus.CONFIRMED, wallet1IncomeTransaction.getStatus());

        // Check if the wallet balance was updated
        verify(walletRepository).save(wallet1);
        assertEquals(
                previousBalance.add(incomeAmount).doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if the expense transaction is confirmed successfully")
    void testConfirmExpenseTransaction() {
        wallet1ExpenseTransaction.setStatus(TransactionStatus.PENDING);
        BigDecimal previousBalance = wallet1.getBalance();

        when(walletTransactionRepository.findById(wallet1ExpenseTransaction.getId()))
                .thenReturn(Optional.of(wallet1ExpenseTransaction));

        walletTransactionService.confirmTransaction(wallet1ExpenseTransaction.getId());

        // Check if the transaction was confirmed
        verify(walletTransactionRepository).save(wallet1ExpenseTransaction);
        assertEquals(TransactionStatus.CONFIRMED, wallet1ExpenseTransaction.getStatus());

        // Check if the wallet balance was updated
        verify(walletRepository).save(wallet1);
        assertEquals(
                previousBalance.subtract(expenseAmount).doubleValue(),
                wallet1.getBalance().doubleValue(),
                Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if exception is thrown when the transaction to confirm does not " + "exist")
    void testConfirmTransactionDoesNotExist() {
        when(walletTransactionRepository.findById(wallet1IncomeTransaction.getId()))
                .thenReturn(Optional.empty());

        Integer transactionId = wallet1IncomeTransaction.getId();

        assertThrows(
                EntityNotFoundException.class,
                () -> walletTransactionService.confirmTransaction(transactionId));
    }

    @Test
    @DisplayName("Test if the transaction already confirmed is not confirmed again")
    void testConfirmTransactionAlreadyConfirmed() {
        when(walletTransactionRepository.findById(wallet1IncomeTransaction.getId()))
                .thenReturn(Optional.of(wallet1IncomeTransaction));

        assertThrows(
                MoinexException.AttributeAlreadySetException.class,
                () ->
                        walletTransactionService.confirmTransaction(
                                wallet1IncomeTransaction.getId()));

        // Check if the transaction was not confirmed
        verify(walletTransactionRepository, never()).save(wallet1IncomeTransaction);

        // Check if the wallet balance was not updated
        verify(walletRepository, never()).save(wallet1);
    }
}
