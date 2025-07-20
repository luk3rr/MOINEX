/*
 * Filename: WalletTransactionServiceTest.java
 * Created on: October 16, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.*;
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
    private final String description = "";
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

    @BeforeAll
    static void setUp() {
        MockitoAnnotations.openMocks(WalletTransactionServiceTest.class);
    }

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

    @Nested
    @DisplayName("Transfer Money Tests")
    class TransferMoneyTests {

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

            verify(transferRepository, never()).save(any(Transfer.class));
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when the sender and receiver wallets are the same")
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
                                    senderWalletId,
                                    receiverWalletId,
                                    date,
                                    zeroAmount,
                                    description));

            verify(transferRepository, never()).save(any(Transfer.class));
        }

        @Test
        @DisplayName("Should throw exception when transfer amount is greater than sender balance")
        void testTransferMoneyInsufficientBalance() {
            BigDecimal amountToTransfer = wallet1.getBalance().add(BigDecimal.ONE);

            when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));
            when(walletRepository.findById(wallet2.getId())).thenReturn(Optional.of(wallet2));

            Integer senderWalletId = wallet1.getId();
            Integer receiverWalletId = wallet2.getId();

            assertThrows(
                    MoinexException.InsufficientResourcesException.class,
                    () ->
                            walletTransactionService.transferMoney(
                                    senderWalletId,
                                    receiverWalletId,
                                    date,
                                    amountToTransfer,
                                    description));

            verify(transferRepository, never()).save(any(Transfer.class));
        }
    }

    @Nested
    @DisplayName("Add Income Tests")
    class AddIncomeTests {

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

            verify(walletRepository, never()).save(any(Wallet.class));
            assertEquals(
                    previousBalance.doubleValue(),
                    wallet1.getBalance().doubleValue(),
                    Constants.EPSILON);
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when the wallet to receive the income "
                        + "does not exist")
        void testAddIncomeWalletDoesNotExist() {
            when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.empty());

            Integer walletId = wallet1.getId();

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

            verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when the income amount is less than or equal to zero")
        void testAddIncomeAmountZero() {
            BigDecimal zeroAmount = BigDecimal.ZERO;

            when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            walletTransactionService.addIncome(
                                    wallet1.getId(),
                                    category,
                                    date,
                                    zeroAmount,
                                    description,
                                    TransactionStatus.CONFIRMED));

            verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
        }
    }

    @Nested
    @DisplayName("Add Expense Tests")
    class AddExpenseTests {

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

            verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when the expense amount is less than or equal to zero")
        void testAddExpenseAmountZero() {
            BigDecimal zeroAmount = BigDecimal.ZERO;

            when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            walletTransactionService.addExpense(
                                    wallet1.getId(),
                                    category,
                                    date,
                                    zeroAmount,
                                    description,
                                    TransactionStatus.CONFIRMED));

            verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
        }
    }

    @Nested
    @DisplayName("Update Transaction Tests")
    class UpdateTransactionTests {

        @Nested
        @DisplayName("Basic checks before updating a transaction")
        class BasicChecksBeforeUpdate {

            private WalletTransaction transaction;

            @BeforeEach
            void setup() {
                transaction =
                        WalletTransaction.builder()
                                .id(1)
                                .wallet(wallet1)
                                .amount(new BigDecimal("100.00"))
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.CONFIRMED)
                                .build();
            }

            @Test
            @DisplayName("Should throw exception if transaction does not exist")
            void updateTransaction_TransactionDoesNotExist_ThrowsException() {
                when(walletTransactionRepository.findById(transaction.getId()))
                        .thenReturn(Optional.empty());

                assertThrows(
                        EntityNotFoundException.class,
                        () -> walletTransactionService.updateTransaction(transaction));

                verify(walletRepository, never()).save(any(Wallet.class));
            }

            @Test
            @DisplayName("Should throw exception if wallet does not exist")
            void updateTransaction_WalletDoesNotExist_ThrowsException() {
                when(walletTransactionRepository.findById(transaction.getId()))
                        .thenReturn(Optional.of(transaction));
                when(walletTransactionRepository.existsWalletByTransactionId(transaction.getId()))
                        .thenReturn(false);

                assertThrows(
                        EntityNotFoundException.class,
                        () -> walletTransactionService.updateTransaction(transaction));

                verify(walletRepository, never()).save(any(Wallet.class));
            }

            @Test
            @DisplayName(
                    "Should throw exception if transaction amount is less than or equal to zero")
            void updateTransaction_AmountLessThanOrEqualToZero_ThrowsException() {
                transaction.setAmount(BigDecimal.ZERO);

                when(walletTransactionRepository.findById(transaction.getId()))
                        .thenReturn(Optional.of(transaction));
                when(walletTransactionRepository.existsWalletByTransactionId(transaction.getId()))
                        .thenReturn(true);

                assertThrows(
                        IllegalArgumentException.class,
                        () -> walletTransactionService.updateTransaction(transaction));

                verify(walletRepository, never()).save(any(Wallet.class));
            }
        }

        @Nested
        @DisplayName("Update Transaction Type Tests")
        class UpdateTransactionTypeTests {

            private Wallet testWallet;
            private WalletTransaction confirmedExpense;
            private WalletTransaction confirmedIncome;
            private WalletTransaction pendingExpense;

            @BeforeEach
            void setup() {
                testWallet = new Wallet(10, "Test Wallet", new BigDecimal("1000.00"));

                confirmedExpense =
                        WalletTransaction.builder()
                                .id(1)
                                .wallet(testWallet)
                                .amount(new BigDecimal("100.00"))
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.CONFIRMED)
                                .build();

                confirmedIncome =
                        WalletTransaction.builder()
                                .id(2)
                                .wallet(testWallet)
                                .amount(new BigDecimal("200.00"))
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.CONFIRMED)
                                .build();

                pendingExpense =
                        WalletTransaction.builder()
                                .id(3)
                                .wallet(testWallet)
                                .amount(new BigDecimal("50.00"))
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.PENDING)
                                .build();
            }

            @Test
            @DisplayName("Should update balance correctly when changing from EXPENSE to INCOME")
            void updateTransaction_FromExpenseToIncome_UpdatesBalance() {
                WalletTransaction updatedData =
                        WalletTransaction.builder()
                                .id(confirmedExpense.getId())
                                .wallet(testWallet)
                                .date(LocalDateTime.now())
                                .amount(confirmedExpense.getAmount())
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.CONFIRMED)
                                .build();

                when(walletTransactionRepository.findById(confirmedExpense.getId()))
                        .thenReturn(Optional.of(confirmedExpense));
                when(walletTransactionRepository.existsWalletByTransactionId(anyInt()))
                        .thenReturn(true);

                BigDecimal initialBalance = testWallet.getBalance();
                BigDecimal transactionAmount = confirmedExpense.getAmount();
                // Saldo esperado: 1000 (inicial) + 100 (reverte despesa) + 100 (aplica receita) =
                // 1200.00
                BigDecimal expectedBalance =
                        initialBalance.add(transactionAmount).add(transactionAmount);

                walletTransactionService.updateTransaction(updatedData);

                assertEquals(0, expectedBalance.compareTo(testWallet.getBalance()));
                verify(walletRepository).save(testWallet);
            }

            @Test
            @DisplayName("Should update balance correctly when changing from INCOME to EXPENSE")
            void updateTransaction_FromIncomeToExpense_UpdatesBalance() {
                WalletTransaction updatedData =
                        WalletTransaction.builder()
                                .id(confirmedIncome.getId())
                                .wallet(testWallet)
                                .date(LocalDateTime.now())
                                .amount(confirmedIncome.getAmount())
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.CONFIRMED)
                                .build();

                when(walletTransactionRepository.findById(confirmedIncome.getId()))
                        .thenReturn(Optional.of(confirmedIncome));
                when(walletTransactionRepository.existsWalletByTransactionId(anyInt()))
                        .thenReturn(true);

                BigDecimal initialBalance = testWallet.getBalance();
                BigDecimal transactionAmount = confirmedIncome.getAmount();
                // Saldo esperado: 1000 (inicial) - 200 (reverte receita) - 200 (aplica despesa) =
                // 600.00
                BigDecimal expectedBalance =
                        initialBalance.subtract(transactionAmount).subtract(transactionAmount);

                walletTransactionService.updateTransaction(updatedData);

                assertEquals(0, expectedBalance.compareTo(testWallet.getBalance()));
                verify(walletRepository).save(testWallet);
            }

            @Test
            @DisplayName("Should not change balance when updating a PENDING transaction's type")
            void updateTransaction_ForPendingTransaction_DoesNotChangeBalance() {
                WalletTransaction updatedData =
                        WalletTransaction.builder()
                                .id(pendingExpense.getId())
                                .wallet(testWallet)
                                .date(LocalDateTime.now())
                                .amount(pendingExpense.getAmount())
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.PENDING)
                                .build();

                when(walletTransactionRepository.findById(pendingExpense.getId()))
                        .thenReturn(Optional.of(pendingExpense));
                when(walletTransactionRepository.existsWalletByTransactionId(anyInt()))
                        .thenReturn(true);

                BigDecimal initialBalance = testWallet.getBalance();

                walletTransactionService.updateTransaction(updatedData);

                assertEquals(0, initialBalance.compareTo(testWallet.getBalance()));
                verify(walletRepository, never()).save(any(Wallet.class));
            }

            @Test
            @DisplayName("Should not change balance if the new type is the same as the old type")
            void updateTransaction_SameType_DoesNothingToBalance() {
                WalletTransaction updatedData =
                        WalletTransaction.builder()
                                .id(confirmedExpense.getId())
                                .wallet(testWallet)
                                .date(LocalDateTime.now())
                                .amount(confirmedExpense.getAmount())
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.CONFIRMED)
                                .build();

                when(walletTransactionRepository.findById(confirmedExpense.getId()))
                        .thenReturn(Optional.of(confirmedExpense));
                when(walletTransactionRepository.existsWalletByTransactionId(anyInt()))
                        .thenReturn(true);

                BigDecimal initialBalance = testWallet.getBalance();

                walletTransactionService.updateTransaction(updatedData);

                assertEquals(0, initialBalance.compareTo(testWallet.getBalance()));
                verify(walletRepository, never()).save(any(Wallet.class));
            }
        }

        @Nested
        @DisplayName("Update Transaction Wallet Tests")
        class UpdateTransactionWalletTests {

            private Wallet wallet1;
            private Wallet wallet2;
            private WalletTransaction confirmedExpenseOnWallet1;
            private WalletTransaction confirmedIncomeOnWallet1;

            @BeforeEach
            void setup() {
                wallet1 = new Wallet(1, "Wallet One", new BigDecimal("1000.00"));
                wallet2 = new Wallet(2, "Wallet Two", new BigDecimal("500.00"));

                confirmedExpenseOnWallet1 =
                        WalletTransaction.builder()
                                .id(1)
                                .wallet(wallet1)
                                .amount(new BigDecimal("100.00"))
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.CONFIRMED)
                                .build();

                confirmedIncomeOnWallet1 =
                        WalletTransaction.builder()
                                .id(2)
                                .wallet(wallet1)
                                .amount(new BigDecimal("200.00"))
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.CONFIRMED)
                                .build();
            }

            @Test
            @DisplayName(
                    "Should update balances correctly when changing wallet for an EXPENSE"
                            + " transaction")
            void updateTransaction_ChangeWalletForExpense_UpdatesBothBalances() {
                WalletTransaction updatedData =
                        WalletTransaction.builder()
                                .id(confirmedExpenseOnWallet1.getId())
                                .wallet(wallet2)
                                .date(LocalDateTime.now())
                                .amount(confirmedExpenseOnWallet1.getAmount())
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.CONFIRMED)
                                .build();

                when(walletTransactionRepository.findById(confirmedExpenseOnWallet1.getId()))
                        .thenReturn(Optional.of(confirmedExpenseOnWallet1));
                when(walletTransactionRepository.existsWalletByTransactionId(anyInt()))
                        .thenReturn(true);

                BigDecimal wallet1InitialBalance = wallet1.getBalance();
                BigDecimal wallet2InitialBalance = wallet2.getBalance();
                BigDecimal transactionAmount = confirmedExpenseOnWallet1.getAmount();

                // Saldo esperado wallet1: 1000 + 100 (reverte despesa) = 1100
                BigDecimal expectedWallet1Balance = wallet1InitialBalance.add(transactionAmount);
                // Saldo esperado wallet2: 500 - 100 (aplica despesa) = 400
                BigDecimal expectedWallet2Balance =
                        wallet2InitialBalance.subtract(transactionAmount);

                walletTransactionService.updateTransaction(updatedData);

                assertEquals(0, expectedWallet1Balance.compareTo(wallet1.getBalance()));
                assertEquals(0, expectedWallet2Balance.compareTo(wallet2.getBalance()));
                verify(walletRepository).save(wallet1);
                verify(walletRepository).save(wallet2);
            }

            @Test
            @DisplayName(
                    "Should update balances correctly when changing wallet for an INCOME"
                            + " transaction")
            void updateTransaction_ChangeWalletForIncome_UpdatesBothBalances() {
                WalletTransaction updatedData =
                        WalletTransaction.builder()
                                .id(confirmedIncomeOnWallet1.getId())
                                .wallet(wallet2)
                                .date(LocalDateTime.now())
                                .amount(confirmedIncomeOnWallet1.getAmount())
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.CONFIRMED)
                                .build();

                when(walletTransactionRepository.findById(confirmedIncomeOnWallet1.getId()))
                        .thenReturn(Optional.of(confirmedIncomeOnWallet1));
                when(walletTransactionRepository.existsWalletByTransactionId(anyInt()))
                        .thenReturn(true);

                BigDecimal wallet1InitialBalance = wallet1.getBalance();
                BigDecimal wallet2InitialBalance = wallet2.getBalance();
                BigDecimal transactionAmount = confirmedIncomeOnWallet1.getAmount();

                // Saldo esperado wallet1: 1000 - 200 (reverte receita) = 800
                BigDecimal expectedWallet1Balance =
                        wallet1InitialBalance.subtract(transactionAmount);
                // Saldo esperado wallet2: 500 + 200 (aplica receita) = 700
                BigDecimal expectedWallet2Balance = wallet2InitialBalance.add(transactionAmount);

                walletTransactionService.updateTransaction(updatedData);

                assertEquals(0, expectedWallet1Balance.compareTo(wallet1.getBalance()));
                assertEquals(0, expectedWallet2Balance.compareTo(wallet2.getBalance()));
                verify(walletRepository).save(wallet1);
                verify(walletRepository).save(wallet2);
            }
        }

        @Nested
        @DisplayName("Update Transaction Amount Tests")
        class UpdateTransactionAmountTests {

            private Wallet testWallet;
            private WalletTransaction confirmedExpense;
            private WalletTransaction confirmedIncome;

            @BeforeEach
            void setup() {
                testWallet = new Wallet(10, "Test Wallet", new BigDecimal("1000.00"));

                confirmedExpense =
                        WalletTransaction.builder()
                                .id(1)
                                .wallet(testWallet)
                                .amount(new BigDecimal("100.00"))
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.CONFIRMED)
                                .build();

                confirmedIncome =
                        WalletTransaction.builder()
                                .id(2)
                                .wallet(testWallet)
                                .amount(new BigDecimal("200.00"))
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.CONFIRMED)
                                .build();
            }

            @Test
            @DisplayName("Should decrease wallet balance when increasing EXPENSE amount")
            void updateTransaction_IncreaseExpenseAmount_UpdatesBalance() {
                BigDecimal newAmount = new BigDecimal("150.00");
                WalletTransaction updatedData =
                        WalletTransaction.builder()
                                .id(1)
                                .wallet(testWallet)
                                .amount(newAmount)
                                .date(LocalDateTime.now())
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.CONFIRMED)
                                .build();
                when(walletTransactionRepository.findById(1))
                        .thenReturn(Optional.of(confirmedExpense));
                when(walletTransactionRepository.existsWalletByTransactionId(1)).thenReturn(true);

                BigDecimal expectedBalance = new BigDecimal("950.00");

                walletTransactionService.updateTransaction(updatedData);

                assertEquals(0, expectedBalance.compareTo(testWallet.getBalance()));
                verify(walletRepository).save(testWallet);
            }

            @Test
            @DisplayName("Should increase wallet balance when decreasing EXPENSE amount")
            void updateTransaction_DecreaseExpenseAmount_UpdatesBalance() {
                BigDecimal newAmount = new BigDecimal("80.00");
                WalletTransaction updatedData =
                        WalletTransaction.builder()
                                .id(1)
                                .wallet(testWallet)
                                .amount(newAmount)
                                .date(LocalDateTime.now())
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.CONFIRMED)
                                .build();
                when(walletTransactionRepository.findById(1))
                        .thenReturn(Optional.of(confirmedExpense));
                when(walletTransactionRepository.existsWalletByTransactionId(1)).thenReturn(true);

                BigDecimal expectedBalance = new BigDecimal("1020.00");

                walletTransactionService.updateTransaction(updatedData);

                assertEquals(0, expectedBalance.compareTo(testWallet.getBalance()));
                verify(walletRepository).save(testWallet);
            }

            @Test
            @DisplayName("Should increase wallet balance when increasing INCOME amount")
            void updateTransaction_IncreaseIncomeAmount_UpdatesBalance() {
                BigDecimal newAmount = new BigDecimal("250.00");
                WalletTransaction updatedData =
                        WalletTransaction.builder()
                                .id(2)
                                .wallet(testWallet)
                                .date(LocalDateTime.now())
                                .amount(newAmount)
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.CONFIRMED)
                                .build();
                when(walletTransactionRepository.findById(2))
                        .thenReturn(Optional.of(confirmedIncome));
                when(walletTransactionRepository.existsWalletByTransactionId(2)).thenReturn(true);

                BigDecimal expectedBalance = new BigDecimal("1050.00");

                walletTransactionService.updateTransaction(updatedData);

                assertEquals(0, expectedBalance.compareTo(testWallet.getBalance()));
                verify(walletRepository).save(testWallet);
            }

            @Test
            @DisplayName("Should decrease wallet balance when decreasing INCOME amount")
            void updateTransaction_DecreaseIncomeAmount_UpdatesBalance() {
                BigDecimal newAmount = new BigDecimal("150.00");
                WalletTransaction updatedData =
                        WalletTransaction.builder()
                                .id(2)
                                .wallet(testWallet)
                                .amount(newAmount)
                                .date(LocalDateTime.now())
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.CONFIRMED)
                                .build();
                when(walletTransactionRepository.findById(2))
                        .thenReturn(Optional.of(confirmedIncome));
                when(walletTransactionRepository.existsWalletByTransactionId(2)).thenReturn(true);

                BigDecimal expectedBalance = new BigDecimal("950.00");

                walletTransactionService.updateTransaction(updatedData);

                assertEquals(0, expectedBalance.compareTo(testWallet.getBalance()));
                verify(walletRepository).save(testWallet);
            }
        }

        @Nested
        @DisplayName("Update Transaction Status Tests")
        class UpdateTransactionStatusTests {

            private Wallet testWallet;
            private WalletTransaction confirmedExpense;
            private WalletTransaction pendingExpense;
            private WalletTransaction confirmedIncome;
            private WalletTransaction pendingIncome;

            @BeforeEach
            void setup() {
                testWallet = new Wallet(10, "Test Wallet", new BigDecimal("1000.00"));

                confirmedExpense =
                        WalletTransaction.builder()
                                .id(1)
                                .wallet(testWallet)
                                .amount(new BigDecimal("100.00"))
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.CONFIRMED)
                                .date(LocalDateTime.now())
                                .build();
                pendingExpense =
                        WalletTransaction.builder()
                                .id(2)
                                .wallet(testWallet)
                                .amount(new BigDecimal("50.00"))
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.PENDING)
                                .date(LocalDateTime.now())
                                .build();
                confirmedIncome =
                        WalletTransaction.builder()
                                .id(3)
                                .wallet(testWallet)
                                .amount(new BigDecimal("200.00"))
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.CONFIRMED)
                                .date(LocalDateTime.now())
                                .build();
                pendingIncome =
                        WalletTransaction.builder()
                                .id(4)
                                .wallet(testWallet)
                                .amount(new BigDecimal("150.00"))
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.PENDING)
                                .date(LocalDateTime.now())
                                .build();
            }

            @Test
            @DisplayName("Should revert balance when changing EXPENSE from CONFIRMED to PENDING")
            void updateTransaction_ExpenseFromConfirmedToPending_RevertsBalance() {
                WalletTransaction updatedData =
                        WalletTransaction.builder()
                                .id(1)
                                .wallet(testWallet)
                                .amount(confirmedExpense.getAmount())
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.PENDING)
                                .date(LocalDateTime.now())
                                .build();
                when(walletTransactionRepository.findById(1))
                        .thenReturn(Optional.of(confirmedExpense));
                when(walletTransactionRepository.existsWalletByTransactionId(1)).thenReturn(true);
                BigDecimal expectedBalance = new BigDecimal("1100.00");

                walletTransactionService.updateTransaction(updatedData);

                assertEquals(0, expectedBalance.compareTo(testWallet.getBalance()));
                verify(walletRepository).save(testWallet);
            }

            @Test
            @DisplayName("Should apply balance when changing EXPENSE from PENDING to CONFIRMED")
            void updateTransaction_ExpenseFromPendingToConfirmed_AppliesBalance() {
                WalletTransaction updatedData =
                        WalletTransaction.builder()
                                .id(2)
                                .wallet(testWallet)
                                .amount(pendingExpense.getAmount())
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.CONFIRMED)
                                .date(LocalDateTime.now())
                                .build();
                when(walletTransactionRepository.findById(2))
                        .thenReturn(Optional.of(pendingExpense));
                when(walletTransactionRepository.existsWalletByTransactionId(2)).thenReturn(true);
                BigDecimal expectedBalance = new BigDecimal("950.00");

                walletTransactionService.updateTransaction(updatedData);

                assertEquals(0, expectedBalance.compareTo(testWallet.getBalance()));
                verify(walletRepository).save(testWallet);
            }

            @Test
            @DisplayName("Should revert balance when changing INCOME from CONFIRMED to PENDING")
            void updateTransaction_IncomeFromConfirmedToPending_RevertsBalance() {
                WalletTransaction updatedData =
                        WalletTransaction.builder()
                                .id(3)
                                .wallet(testWallet)
                                .amount(confirmedIncome.getAmount())
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.PENDING)
                                .date(LocalDateTime.now())
                                .build();
                when(walletTransactionRepository.findById(3))
                        .thenReturn(Optional.of(confirmedIncome));
                when(walletTransactionRepository.existsWalletByTransactionId(3)).thenReturn(true);
                BigDecimal expectedBalance = new BigDecimal("800.00");

                walletTransactionService.updateTransaction(updatedData);

                assertEquals(0, expectedBalance.compareTo(testWallet.getBalance()));
                verify(walletRepository).save(testWallet);
            }

            @Test
            @DisplayName("Should apply balance when changing INCOME from PENDING to CONFIRMED")
            void updateTransaction_IncomeFromPendingToConfirmed_AppliesBalance() {
                WalletTransaction updatedData =
                        WalletTransaction.builder()
                                .id(4)
                                .wallet(testWallet)
                                .amount(pendingIncome.getAmount())
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.CONFIRMED)
                                .date(LocalDateTime.now())
                                .build();
                when(walletTransactionRepository.findById(4))
                        .thenReturn(Optional.of(pendingIncome));
                when(walletTransactionRepository.existsWalletByTransactionId(4)).thenReturn(true);
                BigDecimal expectedBalance = new BigDecimal("1150.00");

                walletTransactionService.updateTransaction(updatedData);

                assertEquals(0, expectedBalance.compareTo(testWallet.getBalance()));
                verify(walletRepository).save(testWallet);
            }
        }
    }

    @Nested
    @DisplayName("Delete Transaction Tests")
    class DeleteTransactionTests {

        @Test
        @DisplayName("Test if the confirmed expense is deleted successfully")
        void testDeleteConfirmedExpense() {
            BigDecimal previousBalance = wallet1.getBalance();

            when(walletTransactionRepository.findById(wallet1ExpenseTransaction.getId()))
                    .thenReturn(Optional.of(wallet1ExpenseTransaction));

            walletTransactionService.deleteTransaction(wallet1ExpenseTransaction.getId());

            verify(walletTransactionRepository).delete(wallet1ExpenseTransaction);

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

            verify(walletTransactionRepository).delete(wallet1ExpenseTransaction);

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

            verify(walletTransactionRepository).delete(wallet1IncomeTransaction);

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

            verify(walletTransactionRepository).delete(wallet1IncomeTransaction);

            verify(walletRepository, never()).save(any(Wallet.class));

            assertEquals(
                    previousBalance.doubleValue(),
                    wallet1.getBalance().doubleValue(),
                    Constants.EPSILON);
        }

        @Test
        @DisplayName(
                "Test if exception is thrown when the transaction to delete does not " + "exist")
        void testDeleteTransactionDoesNotExist() {
            when(walletTransactionRepository.findById(wallet1IncomeTransaction.getId()))
                    .thenReturn(Optional.empty());

            Integer transactionId = wallet1IncomeTransaction.getId();

            assertThrows(
                    EntityNotFoundException.class,
                    () -> walletTransactionService.deleteTransaction(transactionId));
        }
    }
}
