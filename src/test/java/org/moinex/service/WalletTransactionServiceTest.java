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
import java.math.RoundingMode;
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
    @Mock private WalletService walletService;
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
        return new Transfer(id, sender, receiver, date, amount, "", null);
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
            when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));
            when(walletRepository.findById(wallet2.getId())).thenReturn(Optional.of(wallet2));

            when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);

            walletTransactionService.transferMoney(
                    wallet1.getId(),
                    wallet2.getId(),
                    category,
                    transfer.getDate(),
                    transfer.getAmount(),
                    transfer.getDescription());

            verify(walletRepository).findById(wallet1.getId());
            verify(walletRepository).findById(wallet2.getId());

            verify(walletService)
                    .decrementWalletBalance(
                            wallet1.getId(), transferAmount.setScale(2, RoundingMode.HALF_UP));
            verify(walletService)
                    .incrementWalletBalance(
                            wallet2.getId(), transferAmount.setScale(2, RoundingMode.HALF_UP));

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
                                    category,
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
                                    category,
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
                                    category,
                                    date,
                                    transferAmount,
                                    description));

            verify(transferRepository, never()).save(any(Transfer.class));
        }

        @Test
        @DisplayName(
                "Should throw exception when user tries to transfer money from a master wallet to"
                        + " its virtual wallet")
        void testTransferMoneyMasterToVirtualWallet() {
            Wallet masterWallet = createWallet(1, "Master Wallet", new BigDecimal("1000"));
            Wallet virtualWallet = createWallet(2, "Virtual Wallet", new BigDecimal("500"));
            masterWallet.setMasterWallet(null);
            virtualWallet.setMasterWallet(masterWallet);

            when(walletRepository.findById(masterWallet.getId()))
                    .thenReturn(Optional.of(masterWallet));
            when(walletRepository.findById(virtualWallet.getId()))
                    .thenReturn(Optional.of(virtualWallet));

            assertThrows(
                    MoinexException.TransferFromMasterToVirtualWalletException.class,
                    () ->
                            walletTransactionService.transferMoney(
                                    masterWallet.getId(),
                                    virtualWallet.getId(),
                                    category,
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
                                    category,
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
                                    category,
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
            when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

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

            verify(walletService)
                    .incrementWalletBalance(
                            wallet1.getId(), incomeAmount.setScale(2, RoundingMode.HALF_UP));
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
            when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

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

            verify(walletService)
                    .decrementWalletBalance(
                            wallet1.getId(), expenseAmount.setScale(2, RoundingMode.HALF_UP));
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
                                .date(LocalDateTime.now())
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

            private WalletTransaction confirmedExpense;
            private WalletTransaction confirmedIncome;
            private WalletTransaction pendingExpense;

            @BeforeEach
            void setup() {
                Wallet testWallet = new Wallet(10, "Test Wallet", new BigDecimal("1000.00"));

                confirmedExpense =
                        WalletTransaction.builder()
                                .id(1)
                                .wallet(testWallet)
                                .amount(new BigDecimal("100.00"))
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.CONFIRMED)
                                .date(LocalDateTime.now())
                                .build();

                confirmedIncome =
                        WalletTransaction.builder()
                                .id(2)
                                .wallet(testWallet)
                                .amount(new BigDecimal("200.00"))
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.CONFIRMED)
                                .date(LocalDateTime.now())
                                .build();

                pendingExpense =
                        WalletTransaction.builder()
                                .id(3)
                                .wallet(testWallet)
                                .amount(new BigDecimal("50.00"))
                                .type(TransactionType.EXPENSE)
                                .status(TransactionStatus.PENDING)
                                .date(LocalDateTime.now())
                                .build();
            }

            @Test
            @DisplayName("Should update balance correctly when changing from EXPENSE to INCOME")
            void updateTransaction_FromExpenseToIncome_UpdatesBalance() {
                WalletTransaction updatedData =
                        confirmedExpense.toBuilder().type(TransactionType.INCOME).build();

                when(walletTransactionRepository.findById(confirmedExpense.getId()))
                        .thenReturn(Optional.of(confirmedExpense));
                when(walletTransactionRepository.existsWalletByTransactionId(anyInt()))
                        .thenReturn(true);

                walletTransactionService.updateTransaction(updatedData);

                // Increment twice because:
                // 1. 100 (expense is reverted)
                // 2. 100 (income is applied)
                verify(walletService, times(2))
                        .incrementWalletBalance(
                                updatedData.getWallet().getId(), updatedData.getAmount());

                ArgumentCaptor<WalletTransaction> captor =
                        ArgumentCaptor.forClass(WalletTransaction.class);

                verify(walletTransactionRepository, atLeastOnce()).save(captor.capture());

                assertEquals(TransactionType.INCOME, captor.getValue().getType());
            }

            @Test
            @DisplayName("Should update balance correctly when changing from INCOME to EXPENSE")
            void updateTransaction_FromIncomeToExpense_UpdatesBalance() {
                WalletTransaction updatedData =
                        confirmedIncome.toBuilder().type(TransactionType.EXPENSE).build();

                when(walletTransactionRepository.findById(confirmedIncome.getId()))
                        .thenReturn(Optional.of(confirmedIncome));
                when(walletTransactionRepository.existsWalletByTransactionId(anyInt()))
                        .thenReturn(true);

                walletTransactionService.updateTransaction(updatedData);

                // Decrement twice because:
                // 1. 200 (income is reverted)
                // 2. 200 (expense is applied)
                verify(walletService, times(2))
                        .decrementWalletBalance(
                                updatedData.getWallet().getId(), updatedData.getAmount());

                ArgumentCaptor<WalletTransaction> captor =
                        ArgumentCaptor.forClass(WalletTransaction.class);

                verify(walletTransactionRepository, atLeastOnce()).save(captor.capture());

                assertEquals(TransactionType.EXPENSE, captor.getValue().getType());
            }

            @Test
            @DisplayName("Should not change balance when updating a PENDING transaction's type")
            void updateTransaction_ForPendingTransaction_DoesNotChangeBalance() {
                WalletTransaction updatedData =
                        pendingExpense.toBuilder().type(TransactionType.INCOME).build();

                when(walletTransactionRepository.findById(pendingExpense.getId()))
                        .thenReturn(Optional.of(pendingExpense));
                when(walletTransactionRepository.existsWalletByTransactionId(anyInt()))
                        .thenReturn(true);

                walletTransactionService.updateTransaction(updatedData);

                verify(walletService, never()).incrementWalletBalance(any(), any());
                verify(walletService, never()).decrementWalletBalance(any(), any());

                ArgumentCaptor<WalletTransaction> captor =
                        ArgumentCaptor.forClass(WalletTransaction.class);

                verify(walletTransactionRepository, atLeastOnce()).save(captor.capture());

                assertEquals(TransactionType.INCOME, captor.getValue().getType());
            }

            @Test
            @DisplayName("Should not change balance if the new type is the same as the old type")
            void updateTransaction_SameType_DoesNothingToBalance() {
                WalletTransaction updatedData =
                        confirmedExpense.toBuilder().type(TransactionType.EXPENSE).build();

                when(walletTransactionRepository.findById(confirmedExpense.getId()))
                        .thenReturn(Optional.of(confirmedExpense));
                when(walletTransactionRepository.existsWalletByTransactionId(anyInt()))
                        .thenReturn(true);

                walletTransactionService.updateTransaction(updatedData);

                verify(walletService, never()).incrementWalletBalance(any(), any());
                verify(walletService, never()).decrementWalletBalance(any(), any());

                ArgumentCaptor<WalletTransaction> captor =
                        ArgumentCaptor.forClass(WalletTransaction.class);

                verify(walletTransactionRepository, atLeastOnce()).save(captor.capture());

                assertEquals(TransactionType.EXPENSE, captor.getValue().getType());
                assertEquals(confirmedExpense.getAmount(), captor.getValue().getAmount());
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
                                .date(LocalDateTime.now())
                                .build();

                confirmedIncomeOnWallet1 =
                        WalletTransaction.builder()
                                .id(2)
                                .wallet(wallet1)
                                .amount(new BigDecimal("200.00"))
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.CONFIRMED)
                                .date(LocalDateTime.now())
                                .build();
            }

            @Test
            @DisplayName(
                    "Should update balances correctly when changing wallet for an EXPENSE"
                            + " transaction")
            void updateTransaction_ChangeWalletForExpense_UpdatesBothBalances() {
                WalletTransaction updatedData =
                        confirmedExpenseOnWallet1.toBuilder().wallet(wallet2).build();

                when(walletTransactionRepository.findById(confirmedExpenseOnWallet1.getId()))
                        .thenReturn(Optional.of(confirmedExpenseOnWallet1));
                when(walletTransactionRepository.existsWalletByTransactionId(
                                confirmedExpenseOnWallet1.getId()))
                        .thenReturn(true);

                walletTransactionService.updateTransaction(updatedData);

                // Revert expense from old wallet
                verify(walletService, times(1))
                        .incrementWalletBalance(
                                wallet1.getId(), confirmedExpenseOnWallet1.getAmount());

                // Apply expense to new wallet
                verify(walletService, times(1))
                        .decrementWalletBalance(wallet2.getId(), updatedData.getAmount());

                ArgumentCaptor<WalletTransaction> captor =
                        ArgumentCaptor.forClass(WalletTransaction.class);

                verify(walletTransactionRepository, atLeastOnce()).save(captor.capture());

                assertEquals(wallet2.getId(), captor.getValue().getWallet().getId());
            }

            @Test
            @DisplayName(
                    "Should update balances correctly when changing wallet for an INCOME"
                            + " transaction")
            void updateTransaction_ChangeWalletForIncome_UpdatesBothBalances() {
                WalletTransaction updatedData =
                        confirmedIncomeOnWallet1.toBuilder().wallet(wallet2).build();

                when(walletTransactionRepository.findById(confirmedIncomeOnWallet1.getId()))
                        .thenReturn(Optional.of(confirmedIncomeOnWallet1));
                when(walletTransactionRepository.existsWalletByTransactionId(anyInt()))
                        .thenReturn(true);

                walletTransactionService.updateTransaction(updatedData);

                // Revert income from old wallet
                verify(walletService, times(1))
                        .decrementWalletBalance(
                                wallet1.getId(), confirmedIncomeOnWallet1.getAmount());

                // Apply income to new wallet
                verify(walletService, times(1))
                        .incrementWalletBalance(wallet2.getId(), updatedData.getAmount());

                ArgumentCaptor<WalletTransaction> captor =
                        ArgumentCaptor.forClass(WalletTransaction.class);

                verify(walletTransactionRepository, atLeastOnce()).save(captor.capture());

                assertEquals(wallet2.getId(), captor.getValue().getWallet().getId());
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
                                .date(LocalDateTime.now())
                                .build();

                confirmedIncome =
                        WalletTransaction.builder()
                                .id(2)
                                .wallet(testWallet)
                                .amount(new BigDecimal("200.00"))
                                .type(TransactionType.INCOME)
                                .status(TransactionStatus.CONFIRMED)
                                .date(LocalDateTime.now())
                                .build();
            }

            @Test
            @DisplayName("Should decrease wallet balance when increasing EXPENSE amount")
            void updateTransaction_IncreaseExpenseAmount_UpdatesBalance() {
                BigDecimal increment = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
                BigDecimal newAmount = confirmedExpense.getAmount().add(increment);

                WalletTransaction updatedData =
                        confirmedExpense.toBuilder().amount(newAmount).build();

                when(walletTransactionRepository.findById(confirmedExpense.getId()))
                        .thenReturn(Optional.of(confirmedExpense));
                when(walletTransactionRepository.existsWalletByTransactionId(
                                confirmedExpense.getId()))
                        .thenReturn(true);

                walletTransactionService.updateTransaction(updatedData);

                verify(walletService, times(1))
                        .decrementWalletBalance(confirmedExpense.getWallet().getId(), increment);

                ArgumentCaptor<WalletTransaction> captor =
                        ArgumentCaptor.forClass(WalletTransaction.class);

                verify(walletTransactionRepository, atLeastOnce()).save(captor.capture());

                assertEquals(newAmount, captor.getValue().getAmount());
            }

            @Test
            @DisplayName("Should increase wallet balance when decreasing EXPENSE amount")
            void updateTransaction_DecreaseExpenseAmount_UpdatesBalance() {
                BigDecimal decrement = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
                BigDecimal newAmount = confirmedExpense.getAmount().subtract(decrement);

                WalletTransaction updatedData =
                        confirmedExpense.toBuilder().amount(newAmount).build();

                when(walletTransactionRepository.findById(confirmedExpense.getId()))
                        .thenReturn(Optional.of(confirmedExpense));
                when(walletTransactionRepository.existsWalletByTransactionId(
                                confirmedExpense.getId()))
                        .thenReturn(true);

                walletTransactionService.updateTransaction(updatedData);

                verify(walletService, times(1))
                        .incrementWalletBalance(confirmedExpense.getWallet().getId(), decrement);

                ArgumentCaptor<WalletTransaction> captor =
                        ArgumentCaptor.forClass(WalletTransaction.class);

                verify(walletTransactionRepository, atLeastOnce()).save(captor.capture());

                assertEquals(newAmount, captor.getValue().getAmount());
            }

            @Test
            @DisplayName("Should increase wallet balance when increasing INCOME amount")
            void updateTransaction_IncreaseIncomeAmount_UpdatesBalance() {
                BigDecimal increment = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);

                BigDecimal newAmount = confirmedIncome.getAmount().add(increment);

                WalletTransaction updatedData =
                        confirmedIncome.toBuilder().amount(newAmount).build();

                when(walletTransactionRepository.findById(confirmedIncome.getId()))
                        .thenReturn(Optional.of(confirmedIncome));
                when(walletTransactionRepository.existsWalletByTransactionId(
                                confirmedIncome.getId()))
                        .thenReturn(true);

                walletTransactionService.updateTransaction(updatedData);

                verify(walletService)
                        .incrementWalletBalance(updatedData.getWallet().getId(), increment);

                ArgumentCaptor<WalletTransaction> captor =
                        ArgumentCaptor.forClass(WalletTransaction.class);

                verify(walletTransactionRepository, atLeastOnce()).save(captor.capture());

                assertEquals(newAmount, captor.getValue().getAmount());
            }

            @Test
            @DisplayName("Should decrease wallet balance when decreasing INCOME amount")
            void updateTransaction_DecreaseIncomeAmount_UpdatesBalance() {
                BigDecimal decrement = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);

                BigDecimal newAmount = confirmedIncome.getAmount().subtract(decrement);

                WalletTransaction updatedData =
                        confirmedIncome.toBuilder().amount(newAmount).build();

                when(walletTransactionRepository.findById(confirmedIncome.getId()))
                        .thenReturn(Optional.of(confirmedIncome));
                when(walletTransactionRepository.existsWalletByTransactionId(
                                confirmedIncome.getId()))
                        .thenReturn(true);

                walletTransactionService.updateTransaction(updatedData);

                verify(walletService)
                        .decrementWalletBalance(updatedData.getWallet().getId(), decrement);

                ArgumentCaptor<WalletTransaction> captor =
                        ArgumentCaptor.forClass(WalletTransaction.class);

                verify(walletTransactionRepository, atLeastOnce()).save(captor.capture());

                assertEquals(newAmount, captor.getValue().getAmount());
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
                        confirmedExpense.toBuilder().status(TransactionStatus.PENDING).build();

                when(walletTransactionRepository.findById(confirmedExpense.getId()))
                        .thenReturn(Optional.of(confirmedExpense));
                when(walletTransactionRepository.existsWalletByTransactionId(
                                confirmedExpense.getId()))
                        .thenReturn(true);

                walletTransactionService.updateTransaction(updatedData);

                verify(walletService)
                        .incrementWalletBalance(
                                confirmedExpense.getWallet().getId(), updatedData.getAmount());

                ArgumentCaptor<WalletTransaction> captor =
                        ArgumentCaptor.forClass(WalletTransaction.class);

                verify(walletTransactionRepository, atLeastOnce()).save(captor.capture());

                assertEquals(TransactionStatus.PENDING, captor.getValue().getStatus());
            }

            @Test
            @DisplayName("Should apply balance when changing EXPENSE from PENDING to CONFIRMED")
            void updateTransaction_ExpenseFromPendingToConfirmed_AppliesBalance() {
                WalletTransaction updatedData =
                        pendingExpense.toBuilder().status(TransactionStatus.CONFIRMED).build();

                when(walletTransactionRepository.findById(pendingExpense.getId()))
                        .thenReturn(Optional.of(pendingExpense));
                when(walletTransactionRepository.existsWalletByTransactionId(
                                pendingExpense.getId()))
                        .thenReturn(true);

                walletTransactionService.updateTransaction(updatedData);

                verify(walletService)
                        .decrementWalletBalance(
                                updatedData.getWallet().getId(), updatedData.getAmount());

                ArgumentCaptor<WalletTransaction> captor =
                        ArgumentCaptor.forClass(WalletTransaction.class);

                verify(walletTransactionRepository, atLeastOnce()).save(captor.capture());

                assertEquals(TransactionStatus.CONFIRMED, captor.getValue().getStatus());
            }

            @Test
            @DisplayName("Should revert balance when changing INCOME from CONFIRMED to PENDING")
            void updateTransaction_IncomeFromConfirmedToPending_RevertsBalance() {
                WalletTransaction updatedData =
                        confirmedIncome.toBuilder().status(TransactionStatus.PENDING).build();

                when(walletTransactionRepository.findById(confirmedIncome.getId()))
                        .thenReturn(Optional.of(confirmedIncome));
                when(walletTransactionRepository.existsWalletByTransactionId(
                                confirmedIncome.getId()))
                        .thenReturn(true);

                walletTransactionService.updateTransaction(updatedData);

                verify(walletService)
                        .decrementWalletBalance(
                                confirmedIncome.getWallet().getId(), updatedData.getAmount());

                ArgumentCaptor<WalletTransaction> captor =
                        ArgumentCaptor.forClass(WalletTransaction.class);

                verify(walletTransactionRepository, atLeastOnce()).save(captor.capture());

                assertEquals(TransactionStatus.PENDING, captor.getValue().getStatus());
            }

            @Test
            @DisplayName("Should apply balance when changing INCOME from PENDING to CONFIRMED")
            void updateTransaction_IncomeFromPendingToConfirmed_AppliesBalance() {
                WalletTransaction updatedData =
                        pendingIncome.toBuilder().status(TransactionStatus.CONFIRMED).build();

                when(walletTransactionRepository.findById(pendingIncome.getId()))
                        .thenReturn(Optional.of(pendingIncome));
                when(walletTransactionRepository.existsWalletByTransactionId(pendingIncome.getId()))
                        .thenReturn(true);

                walletTransactionService.updateTransaction(updatedData);

                verify(walletService)
                        .incrementWalletBalance(
                                pendingIncome.getWallet().getId(), updatedData.getAmount());

                ArgumentCaptor<WalletTransaction> captor =
                        ArgumentCaptor.forClass(WalletTransaction.class);

                verify(walletTransactionRepository, atLeastOnce()).save(captor.capture());

                assertEquals(TransactionStatus.CONFIRMED, captor.getValue().getStatus());
            }
        }
    }

    @Nested
    @DisplayName("Delete Transaction Tests")
    class DeleteTransactionTests {

        @Test
        @DisplayName("Test if the confirmed expense is deleted successfully")
        void testDeleteConfirmedExpense() {
            when(walletTransactionRepository.findById(wallet1ExpenseTransaction.getId()))
                    .thenReturn(Optional.of(wallet1ExpenseTransaction));

            walletTransactionService.deleteTransaction(wallet1ExpenseTransaction.getId());

            verify(walletTransactionRepository).delete(wallet1ExpenseTransaction);
            verify(walletService).incrementWalletBalance(wallet1.getId(), expenseAmount);
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
            when(walletTransactionRepository.findById(wallet1IncomeTransaction.getId()))
                    .thenReturn(Optional.of(wallet1IncomeTransaction));

            walletTransactionService.deleteTransaction(wallet1IncomeTransaction.getId());

            verify(walletTransactionRepository).delete(wallet1IncomeTransaction);
            verify(walletService).decrementWalletBalance(wallet1.getId(), incomeAmount);
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
