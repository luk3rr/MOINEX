/*
 * Filename: WalletTransactionServiceTest.java
 * Created on: October 16, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

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
import org.moinex.entities.Category;
import org.moinex.entities.wallettransaction.Transfer;
import org.moinex.entities.wallettransaction.Wallet;
import org.moinex.entities.wallettransaction.WalletTransaction;
import org.moinex.exceptions.AttributeAlreadySetException;
import org.moinex.exceptions.SameSourceDestionationException;
import org.moinex.repositories.CategoryRepository;
import org.moinex.repositories.wallettransaction.TransferRepository;
import org.moinex.repositories.wallettransaction.WalletRepository;
import org.moinex.repositories.wallettransaction.WalletTransactionRepository;
import org.moinex.repositories.wallettransaction.WalletTypeRepository;
import org.moinex.util.Constants;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;

@ExtendWith(MockitoExtension.class)
class WalletTransactionServiceTest
{
    @Mock
    private WalletRepository m_walletRepository;

    @Mock
    private WalletTypeRepository m_walletTypeRepository;

    @Mock
    private TransferRepository m_transferRepository;

    @Mock
    private CategoryRepository m_categoryRepository;

    @Mock
    private WalletTransactionRepository m_walletTransactionRepository;

    @InjectMocks
    private WalletTransactionService m_walletTransactionService;

    private Wallet            m_wallet1;
    private Wallet            m_wallet2;
    private Transfer          m_transfer;
    private WalletTransaction m_wallet1IncomeTransaction;
    private WalletTransaction m_wallet1ExpenseTransaction;
    private Category          m_category;
    private LocalDateTime     m_date;
    private BigDecimal        m_incomeAmount;
    private BigDecimal        m_expenseAmount;
    private BigDecimal        m_transferAmount;
    private String            m_description = "";

    private Wallet createWallet(Long id, String name, BigDecimal balance)
    {
        Wallet wallet = new Wallet(id, name, balance);
        return wallet;
    }

    private Transfer createTransfer(Long          id,
                                    Wallet        sender,
                                    Wallet        receiver,
                                    LocalDateTime date,
                                    BigDecimal    amount,
                                    String        description)
    {
        Transfer transfer =
            new Transfer(id, sender, receiver, date, amount, description);
        return transfer;
    }

    private WalletTransaction createWalletTransaction(Wallet            wallet,
                                                      Category          category,
                                                      TransactionType   type,
                                                      TransactionStatus status,
                                                      LocalDateTime     date,
                                                      BigDecimal        amount,
                                                      String            description)
    {
        WalletTransaction walletTransaction = WalletTransaction.builder()
                                                  .wallet(wallet)
                                                  .category(category)
                                                  .type(type)
                                                  .status(status)
                                                  .date(date)
                                                  .amount(amount)
                                                  .description(description)
                                                  .build();

        return walletTransaction;
    }

    @BeforeAll
    static void setUp()
    {
        MockitoAnnotations.openMocks(WalletTransactionServiceTest.class);
    }

    @BeforeEach
    void beforeEach()
    {
        m_incomeAmount   = new BigDecimal("500");
        m_expenseAmount  = new BigDecimal("200");
        m_transferAmount = new BigDecimal("125.5");

        m_date     = LocalDateTime.now();
        m_category = Category.builder().name("Category").build();

        m_wallet1 = createWallet(1L, "Wallet1", new BigDecimal("1000"));
        m_wallet2 = createWallet(2L, "Wallet2", new BigDecimal("2000"));

        m_transfer = createTransfer(1L,
                                    m_wallet1,
                                    m_wallet2,
                                    m_date,
                                    m_transferAmount,
                                    m_description);

        m_wallet1IncomeTransaction =
            createWalletTransaction(m_wallet1,
                                    m_category,
                                    TransactionType.INCOME,
                                    TransactionStatus.CONFIRMED,
                                    m_date,
                                    m_incomeAmount,
                                    m_description);

        m_wallet1ExpenseTransaction =
            createWalletTransaction(m_wallet1,
                                    m_category,
                                    TransactionType.EXPENSE,
                                    TransactionStatus.CONFIRMED,
                                    m_date,
                                    m_expenseAmount,
                                    m_description);
    }

    @Test
    @DisplayName("Test if the money transfer is successful")
    void testTransferMoneySuccess()
    {
        BigDecimal m_senderPreviousBalance   = m_wallet1.getBalance();
        BigDecimal m_receiverPreviousBalance = m_wallet2.getBalance();

        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.of(m_wallet1));
        when(m_walletRepository.findById(m_wallet2.getId()))
            .thenReturn(Optional.of(m_wallet2));

        when(m_walletRepository.save(m_wallet1)).thenReturn(m_wallet1);
        when(m_walletRepository.save(m_wallet2)).thenReturn(m_wallet2);

        when(m_transferRepository.save(any(Transfer.class))).thenReturn(m_transfer);

        m_walletTransactionService.transferMoney(m_wallet1.getId(),
                                                 m_wallet2.getId(),
                                                 m_transfer.getDate(),
                                                 m_transfer.getAmount(),
                                                 m_transfer.getDescription());

        // Check if the sender and receiver balances were updated
        verify(m_walletRepository).findById(m_wallet1.getId());
        verify(m_walletRepository).findById(m_wallet2.getId());
        verify(m_walletRepository).save(m_wallet1);
        verify(m_walletRepository).save(m_wallet2);

        assertEquals(m_senderPreviousBalance.subtract(m_transferAmount).doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);

        assertEquals(m_receiverPreviousBalance.add(m_transferAmount).doubleValue(),
                     m_wallet2.getBalance().doubleValue(),
                     Constants.EPSILON);

        // Check if the transfer was saved
        ArgumentCaptor<Transfer> transferCaptor =
            ArgumentCaptor.forClass(Transfer.class);

        verify(m_transferRepository).save(transferCaptor.capture());

        assertEquals(m_wallet1, transferCaptor.getValue().getSenderWallet());
        assertEquals(m_wallet2, transferCaptor.getValue().getReceiverWallet());
        assertEquals(m_transferAmount.doubleValue(),
                     transferCaptor.getValue().getAmount().doubleValue(),
                     Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if exception is thrown when the sender wallet does not exist")
    void testTransferMoneySenderDoesNotExist()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                     ()
                         -> m_walletTransactionService.transferMoney(m_wallet1.getId(),
                                                                     m_wallet2.getId(),
                                                                     m_date,
                                                                     m_transferAmount,
                                                                     m_description));

        // Verify that the transfer was not saved
        verify(m_transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when the receiver wallet does not exist")
    void testTransferMoneyReceiverDoesNotExist()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.of(m_wallet1));

        when(m_walletRepository.findById(m_wallet2.getId()))
            .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                     ()
                         -> m_walletTransactionService.transferMoney(m_wallet1.getId(),
                                                                     m_wallet2.getId(),
                                                                     m_date,
                                                                     m_transferAmount,
                                                                     m_description),
                     "Receiver wallet does not exist");

        // Verify that the transfer was not saved
        verify(m_transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when the sender and receiver wallets are the same")
    void
    testTransferMoneySameWallet()
    {
        assertThrows(SameSourceDestionationException.class,
                     ()
                         -> m_walletTransactionService.transferMoney(m_wallet1.getId(),
                                                                     m_wallet1.getId(),
                                                                     m_date,
                                                                     m_transferAmount,
                                                                     m_description));

        // Verify that the transfer was not saved
        verify(m_transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when the amount to transfer is less "
                 + "than or equal to zero")
    void
    testTransferMoneyAmountZero()
    {
        assertThrows(
            IllegalArgumentException.class,
            ()
                -> m_walletTransactionService.transferMoney(m_wallet1.getId(),
                                                            m_wallet2.getId(),
                                                            m_date,
                                                            new BigDecimal("0.0"),
                                                            m_description));

        // Verify that the transfer was not saved
        verify(m_transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Test if the confirmed income is added successfully")
    void testAddConfirmedIncome()
    {
        BigDecimal previousBalance = m_wallet1.getBalance();

        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.of(m_wallet1));
        when(m_walletRepository.save(m_wallet1)).thenReturn(m_wallet1);

        when(m_walletTransactionRepository.save(any(WalletTransaction.class)))
            .thenReturn(WalletTransaction.builder()
                            .wallet(m_wallet1)
                            .category(m_category)
                            .type(TransactionType.INCOME)
                            .status(TransactionStatus.CONFIRMED)
                            .date(m_date)
                            .amount(m_incomeAmount)
                            .description(m_description)
                            .build());

        m_walletTransactionService.addIncome(m_wallet1.getId(),
                                             m_category,
                                             m_date,
                                             m_incomeAmount,
                                             m_description,
                                             TransactionStatus.CONFIRMED);

        // Check if the wallet balance was updated
        verify(m_walletRepository).save(m_wallet1);
        assertEquals(previousBalance.add(m_incomeAmount).doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if the pending income is added successfully")
    void testAddPendingIncome()
    {
        BigDecimal previousBalance = m_wallet1.getBalance();

        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.of(m_wallet1));

        when(m_walletTransactionRepository.save(any(WalletTransaction.class)))
            .thenReturn(WalletTransaction.builder()
                            .wallet(m_wallet1)
                            .category(m_category)
                            .type(TransactionType.INCOME)
                            .status(TransactionStatus.PENDING)
                            .date(m_date)
                            .amount(m_incomeAmount)
                            .description(m_description)
                            .build());

        m_walletTransactionService.addIncome(m_wallet1.getId(),
                                             m_category,
                                             m_date,
                                             m_incomeAmount,
                                             m_description,
                                             TransactionStatus.PENDING);

        // Check if the wallet balance is the same
        verify(m_walletRepository, never()).save(any(Wallet.class));
        assertEquals(previousBalance.doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if exception is thrown when the wallet to receive the income "
                 + "does not exist")
    void
    testAddIncomeWalletDoesNotExist()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.empty());

        // Check for confirmed income
        assertThrows(
            EntityNotFoundException.class,
            ()
                -> m_walletTransactionService.addIncome(m_wallet1.getId(),
                                                        m_category,
                                                        m_date,
                                                        m_incomeAmount,
                                                        m_description,
                                                        TransactionStatus.CONFIRMED));

        // Check for pending income
        assertThrows(
            EntityNotFoundException.class,
            ()
                -> m_walletTransactionService.addIncome(m_wallet1.getId(),
                                                        m_category,
                                                        m_date,
                                                        m_incomeAmount,
                                                        m_description,
                                                        TransactionStatus.PENDING));

        // Verify that the income was not added
        verify(m_walletTransactionRepository, never())
            .save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Test if the confirmed expense is added successfully")
    void testAddConfirmedExpense()
    {
        BigDecimal previousBalance = m_wallet1.getBalance();

        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.of(m_wallet1));
        when(m_walletRepository.save(m_wallet1)).thenReturn(m_wallet1);

        when(m_walletTransactionRepository.save(any(WalletTransaction.class)))
            .thenReturn(WalletTransaction.builder()
                            .wallet(m_wallet1)
                            .category(m_category)
                            .type(TransactionType.EXPENSE)
                            .status(TransactionStatus.CONFIRMED)
                            .date(m_date)
                            .amount(m_expenseAmount)
                            .description(m_description)
                            .build());

        m_walletTransactionService.addExpense(m_wallet1.getId(),
                                              m_category,
                                              m_date,
                                              m_expenseAmount,
                                              m_description,
                                              TransactionStatus.CONFIRMED);

        // Check if the wallet balance was updated
        verify(m_walletRepository).save(m_wallet1);
        assertEquals(previousBalance.subtract(m_expenseAmount).doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if the pending expense is added successfully")
    void testAddPendingExpense()
    {
        BigDecimal previousBalance = m_wallet1.getBalance();

        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.of(m_wallet1));

        when(m_walletTransactionRepository.save(any(WalletTransaction.class)))
            .thenReturn(WalletTransaction.builder()
                            .wallet(m_wallet1)
                            .category(m_category)
                            .type(TransactionType.EXPENSE)
                            .status(TransactionStatus.PENDING)
                            .date(m_date)
                            .amount(m_expenseAmount)
                            .description(m_description)
                            .build());

        m_walletTransactionService.addExpense(m_wallet1.getId(),
                                              m_category,
                                              m_date,
                                              m_expenseAmount,
                                              m_description,
                                              TransactionStatus.PENDING);

        // Check if the wallet balance is the same
        verify(m_walletRepository, never()).save(m_wallet1);
        assertEquals(previousBalance.doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if exception is thrown when the wallet to receive the expense "
                 + "does not exist")
    void
    testAddExpenseWalletDoesNotExist()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.empty());

        // Check for confirmed expense
        assertThrows(
            EntityNotFoundException.class,
            ()
                -> m_walletTransactionService.addExpense(m_wallet1.getId(),
                                                         m_category,
                                                         m_date,
                                                         m_expenseAmount,
                                                         m_description,
                                                         TransactionStatus.CONFIRMED));

        // Check for pending expense
        assertThrows(
            EntityNotFoundException.class,
            ()
                -> m_walletTransactionService.addExpense(m_wallet1.getId(),
                                                         m_category,
                                                         m_date,
                                                         m_expenseAmount,
                                                         m_description,
                                                         TransactionStatus.PENDING));

        // Verify that the expense was not added
        verify(m_walletTransactionRepository, never())
            .save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Test if transaction type is changed from EXPENSE to INCOME and "
                 + "wallet balance is updated correctly")
    void
    testChangeTransactionTypeFromExpenseToIncome()
    {
        // Setup previous state
        BigDecimal oldBalance      = m_wallet1.getBalance();
        BigDecimal expenseAmount   = m_wallet1ExpenseTransaction.getAmount();
        BigDecimal newIncomeAmount = new BigDecimal("300.0");

        WalletTransaction updatedTransaction =
            createWalletTransaction(m_wallet1,
                                    m_category,
                                    TransactionType.INCOME,
                                    TransactionStatus.CONFIRMED,
                                    m_date,
                                    newIncomeAmount,
                                    m_description);

        when(
            m_walletTransactionRepository.findById(m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1ExpenseTransaction));

        when(m_walletTransactionRepository.findWalletByTransactionId(
                 m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1));

        when(m_walletRepository.save(m_wallet1)).thenReturn(m_wallet1);

        when(m_walletTransactionRepository.save(any(WalletTransaction.class)))
            .thenReturn(updatedTransaction);

        m_walletTransactionService.updateTransaction(updatedTransaction);

        // Verify that the old expense was reverted and new income was applied
        assertEquals(oldBalance.add(expenseAmount).add(newIncomeAmount).doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);

        // Verify repository interactions
        verify(m_walletTransactionRepository)
            .findById(m_wallet1ExpenseTransaction.getId());
        verify(m_walletRepository, times(2)).save(m_wallet1);
        verify(m_walletTransactionRepository, times(3))
            .save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Test if transaction status is changed from CONFIRMED to PENDING "
                 + "and balance is reverted")
    void
    testChangeTransactionStatusFromConfirmedToPending()
    {
        BigDecimal        oldBalance = m_wallet1.getBalance();
        WalletTransaction updatedTransaction =
            createWalletTransaction(m_wallet1,
                                    m_category,
                                    TransactionType.EXPENSE,
                                    TransactionStatus.PENDING,
                                    m_date,
                                    m_expenseAmount,
                                    m_description);

        when(
            m_walletTransactionRepository.findById(m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1ExpenseTransaction));

        when(m_walletTransactionRepository.findWalletByTransactionId(
                 m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1));

        when(m_walletRepository.save(m_wallet1)).thenReturn(m_wallet1);

        when(m_walletTransactionRepository.save(any(WalletTransaction.class)))
            .thenReturn(updatedTransaction);

        m_walletTransactionService.updateTransaction(updatedTransaction);

        // Verify that the balance was reverted for the expense
        assertEquals(oldBalance.add(m_expenseAmount).doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);

        // Verify repository interactions
        verify(m_walletTransactionRepository)
            .findById(m_wallet1ExpenseTransaction.getId());
        verify(m_walletRepository).save(m_wallet1);
        verify(m_walletTransactionRepository, times(2))
            .save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName(
        "Test if the wallet is changed and transaction is applied to the new wallet")
    void
    testChangeTransactionWallet()
    {
        BigDecimal oldWallet1Balance = m_wallet1.getBalance();
        BigDecimal oldWallet2Balance = m_wallet2.getBalance();
        BigDecimal amount            = m_wallet1IncomeTransaction.getAmount();

        WalletTransaction updatedTransaction =
            createWalletTransaction(m_wallet2,
                                    m_category,
                                    TransactionType.INCOME,
                                    TransactionStatus.CONFIRMED,
                                    m_date,
                                    amount,
                                    m_description);

        when(m_walletTransactionRepository.findById(m_wallet1IncomeTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1IncomeTransaction));

        when(m_walletTransactionRepository.findWalletByTransactionId(
                 m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1));

        when(m_walletRepository.save(m_wallet1)).thenReturn(m_wallet1);

        when(m_walletRepository.save(m_wallet2)).thenReturn(m_wallet2);

        when(m_walletTransactionRepository.save(any(WalletTransaction.class)))
            .thenReturn(updatedTransaction);

        m_walletTransactionService.updateTransaction(updatedTransaction);

        // Verify that the amount was reverted from the old wallet and added to the new
        // wallet
        assertEquals(oldWallet1Balance.subtract(amount).doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);

        assertEquals(oldWallet2Balance.add(amount).doubleValue(),
                     m_wallet2.getBalance().doubleValue(),
                     Constants.EPSILON);

        // Verify repository interactions
        verify(m_walletTransactionRepository)
            .findById(m_wallet1IncomeTransaction.getId());
        verify(m_walletRepository).save(m_wallet1);
        verify(m_walletRepository).save(m_wallet2);
        verify(m_walletTransactionRepository, times(2))
            .save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Test if transaction amount is increased and wallet balance is "
                 + "updated correctly")
    void
    testIncreaseTransactionAmount()
    {
        // Setup previous state
        BigDecimal oldBalance = m_wallet1.getBalance();
        BigDecimal oldAmount  = m_wallet1ExpenseTransaction.getAmount();
        BigDecimal newAmount  = oldAmount.add(new BigDecimal("100.0"));

        WalletTransaction updatedTransaction =
            createWalletTransaction(m_wallet1,
                                    m_category,
                                    TransactionType.EXPENSE,
                                    TransactionStatus.CONFIRMED,
                                    m_date,
                                    newAmount,
                                    m_description);

        when(
            m_walletTransactionRepository.findById(m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1ExpenseTransaction));

        when(m_walletTransactionRepository.findWalletByTransactionId(
                 m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1));

        when(m_walletRepository.save(m_wallet1)).thenReturn(m_wallet1);

        when(m_walletTransactionRepository.save(any(WalletTransaction.class)))
            .thenReturn(updatedTransaction);

        m_walletTransactionService.updateTransaction(updatedTransaction);

        // Verify that the old amount was reverted and new amount was applied
        BigDecimal expectedBalance = oldBalance.add(oldAmount).subtract(newAmount);
        assertEquals(expectedBalance.doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);

        // Verify repository interactions
        verify(m_walletTransactionRepository)
            .findById(m_wallet1ExpenseTransaction.getId());
        verify(m_walletRepository, times(1)).save(m_wallet1);
        verify(m_walletTransactionRepository, times(2))
            .save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Test if transaction amount is decreased and wallet balance is "
                 + "updated correctly")
    void
    testDecreaseTransactionAmount()
    {
        // Setup previous state
        BigDecimal oldBalance = m_wallet1.getBalance();
        BigDecimal oldAmount  = m_wallet1ExpenseTransaction.getAmount();
        BigDecimal newAmount  = oldAmount.subtract(new BigDecimal("100.0"));

        WalletTransaction updatedTransaction =
            createWalletTransaction(m_wallet1,
                                    m_category,
                                    TransactionType.EXPENSE,
                                    TransactionStatus.CONFIRMED,
                                    m_date,
                                    newAmount,
                                    m_description);

        when(
            m_walletTransactionRepository.findById(m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1ExpenseTransaction));

        when(m_walletTransactionRepository.findWalletByTransactionId(
                 m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1));

        when(m_walletRepository.save(m_wallet1)).thenReturn(m_wallet1);

        when(m_walletTransactionRepository.save(any(WalletTransaction.class)))
            .thenReturn(updatedTransaction);

        m_walletTransactionService.updateTransaction(updatedTransaction);

        // Verify that the old amount was reverted and new amount was applied
        BigDecimal expectedBalance = oldBalance.add(oldAmount).subtract(newAmount);
        assertEquals(expectedBalance.doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);

        // Verify repository interactions
        verify(m_walletTransactionRepository)
            .findById(m_wallet1ExpenseTransaction.getId());
        verify(m_walletRepository, times(1)).save(m_wallet1);
        verify(m_walletTransactionRepository, times(2))
            .save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName(
        "Test if transaction amount remains the same and wallet balance is unaffected")
    void
    testChangeTransactionAmountWithNoChange()
    {
        // Setup previous state
        BigDecimal oldBalance = m_wallet1.getBalance();
        BigDecimal oldAmount  = m_wallet1ExpenseTransaction.getAmount();

        WalletTransaction updatedTransaction =
            createWalletTransaction(m_wallet1,
                                    m_category,
                                    TransactionType.EXPENSE,
                                    TransactionStatus.CONFIRMED,
                                    m_date,
                                    oldAmount, // Same amount as before
                                    m_description);

        when(
            m_walletTransactionRepository.findById(m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1ExpenseTransaction));

        when(m_walletTransactionRepository.findWalletByTransactionId(
                 m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1));

        when(m_walletTransactionRepository.save(any(WalletTransaction.class)))
            .thenReturn(updatedTransaction);

        m_walletTransactionService.updateTransaction(updatedTransaction);

        // Verify that the balance is unaffected
        assertEquals(oldBalance.doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);

        // Verify repository interactions
        verify(m_walletTransactionRepository)
            .findById(m_wallet1ExpenseTransaction.getId());
        verify(m_walletRepository, never())
            .save(m_wallet1); // No save on wallet since no change
        verify(m_walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when updating transaction with amount <= 0")
    void
    testChangeTransactionAmountInvalidAmount()
    {
        BigDecimal invalidAmount = BigDecimal.ZERO;

        WalletTransaction updatedTransaction =
            createWalletTransaction(m_wallet1,
                                    m_category,
                                    TransactionType.EXPENSE,
                                    TransactionStatus.CONFIRMED,
                                    m_date,
                                    invalidAmount,
                                    m_description);

        when(
            m_walletTransactionRepository.findById(m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1ExpenseTransaction));

        when(m_walletTransactionRepository.findWalletByTransactionId(
                 m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1));

        assertThrows(
            IllegalArgumentException.class,
            () -> m_walletTransactionService.updateTransaction(updatedTransaction));

        // Verify repository interactions
        verify(m_walletTransactionRepository)
            .findById(m_wallet1ExpenseTransaction.getId());
        verify(m_walletRepository, never()).save(m_wallet1);
        verify(m_walletTransactionRepository, never())
            .save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Test if an exception is thrown when transaction does not exist")
    void testUpdateNonExistentTransaction()
    {
        WalletTransaction nonExistentTransaction =
            createWalletTransaction(m_wallet1,
                                    m_category,
                                    TransactionType.INCOME,
                                    TransactionStatus.CONFIRMED,
                                    m_date,
                                    m_incomeAmount,
                                    m_description);

        when(m_walletTransactionRepository.findById(nonExistentTransaction.getId()))
            .thenReturn(Optional.empty());

        assertThrows(
            EntityNotFoundException.class,
            ()
                -> m_walletTransactionService.updateTransaction(nonExistentTransaction),
            "Transaction with id " + nonExistentTransaction.getId() + " not found");

        verify(m_walletTransactionRepository, never())
            .save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Test if the confirmed expense is deleted successfully")
    void testDeleteConfirmedExpense()
    {
        BigDecimal previousBalance = m_wallet1.getBalance();

        when(
            m_walletTransactionRepository.findById(m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1ExpenseTransaction));

        m_walletTransactionService.deleteTransaction(
            m_wallet1ExpenseTransaction.getId());

        // Check if the transaction was deleted
        verify(m_walletTransactionRepository).delete(m_wallet1ExpenseTransaction);

        // Check if the wallet balance was updated
        verify(m_walletRepository).save(m_wallet1);

        assertEquals(previousBalance.add(m_expenseAmount).doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if the pending expense is deleted successfully")
    void testDeletePendingExpense()
    {
        BigDecimal previousBalance = m_wallet1.getBalance();
        m_wallet1ExpenseTransaction.setStatus(TransactionStatus.PENDING);

        when(
            m_walletTransactionRepository.findById(m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1ExpenseTransaction));

        m_walletTransactionService.deleteTransaction(
            m_wallet1ExpenseTransaction.getId());

        // Check if the transaction was deleted
        verify(m_walletTransactionRepository).delete(m_wallet1ExpenseTransaction);

        // Check if the wallet balance was not updated
        verify(m_walletRepository, never()).save(any(Wallet.class));

        assertEquals(previousBalance.doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if the confirmed income transaction is deleted successfully")
    void testDeleteConfirmedIncome()
    {
        BigDecimal previousBalance = m_wallet1.getBalance();

        when(m_walletTransactionRepository.findById(m_wallet1IncomeTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1IncomeTransaction));

        m_walletTransactionService.deleteTransaction(
            m_wallet1IncomeTransaction.getId());

        // Check if the transaction was deleted
        verify(m_walletTransactionRepository).delete(m_wallet1IncomeTransaction);

        // Check if the wallet balance was updated
        verify(m_walletRepository).save(m_wallet1);

        assertEquals(previousBalance.subtract(m_incomeAmount).doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if the pending income transaction is deleted successfully")
    void testDeletePendingIncome()
    {
        BigDecimal previousBalance = m_wallet1.getBalance();
        m_wallet1IncomeTransaction.setStatus(TransactionStatus.PENDING);

        when(m_walletTransactionRepository.findById(m_wallet1IncomeTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1IncomeTransaction));

        m_walletTransactionService.deleteTransaction(
            m_wallet1IncomeTransaction.getId());

        // Check if the transaction was deleted
        verify(m_walletTransactionRepository).delete(m_wallet1IncomeTransaction);

        // Check if the wallet balance was not updated
        verify(m_walletRepository, never()).save(any(Wallet.class));

        assertEquals(previousBalance.doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if exception is thrown when the transaction to delete does not "
                 + "exist")
    void
    testDeleteTransactionDoesNotExist()
    {
        when(m_walletTransactionRepository.findById(m_wallet1IncomeTransaction.getId()))
            .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                     ()
                         -> m_walletTransactionService.deleteTransaction(
                             m_wallet1IncomeTransaction.getId()));
    }

    @Test
    @DisplayName("Test if the income transaction is confirmed successfully")
    void testConfirmIncomeTransaction()
    {
        m_wallet1IncomeTransaction.setStatus(TransactionStatus.PENDING);
        BigDecimal previousBalance = m_wallet1.getBalance();

        when(m_walletTransactionRepository.findById(m_wallet1IncomeTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1IncomeTransaction));

        m_walletTransactionService.confirmTransaction(
            m_wallet1IncomeTransaction.getId());

        // Check if the transaction was confirmed
        verify(m_walletTransactionRepository).save(m_wallet1IncomeTransaction);
        assertEquals(TransactionStatus.CONFIRMED,
                     m_wallet1IncomeTransaction.getStatus());

        // Check if the wallet balance was updated
        verify(m_walletRepository).save(m_wallet1);
        assertEquals(previousBalance.add(m_incomeAmount).doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if the expense transaction is confirmed successfully")
    void testConfirmExpenseTransaction()
    {
        m_wallet1ExpenseTransaction.setStatus(TransactionStatus.PENDING);
        BigDecimal previousBalance = m_wallet1.getBalance();

        when(
            m_walletTransactionRepository.findById(m_wallet1ExpenseTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1ExpenseTransaction));

        m_walletTransactionService.confirmTransaction(
            m_wallet1ExpenseTransaction.getId());

        // Check if the transaction was confirmed
        verify(m_walletTransactionRepository).save(m_wallet1ExpenseTransaction);
        assertEquals(TransactionStatus.CONFIRMED,
                     m_wallet1ExpenseTransaction.getStatus());

        // Check if the wallet balance was updated
        verify(m_walletRepository).save(m_wallet1);
        assertEquals(previousBalance.subtract(m_expenseAmount).doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if exception is thrown when the transaction to confirm does not "
                 + "exist")
    void
    testConfirmTransactionDoesNotExist()
    {
        when(m_walletTransactionRepository.findById(m_wallet1IncomeTransaction.getId()))
            .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                     ()
                         -> m_walletTransactionService.confirmTransaction(
                             m_wallet1IncomeTransaction.getId()));
    }

    @Test
    @DisplayName("Test if the transaction already confirmed is not confirmed again")
    void testConfirmTransactionAlreadyConfirmed()
    {
        when(m_walletTransactionRepository.findById(m_wallet1IncomeTransaction.getId()))
            .thenReturn(Optional.of(m_wallet1IncomeTransaction));

        assertThrows(AttributeAlreadySetException.class,
                     ()
                         -> m_walletTransactionService.confirmTransaction(
                             m_wallet1IncomeTransaction.getId()));

        // Check if the transaction was not confirmed
        verify(m_walletTransactionRepository, never()).save(m_wallet1IncomeTransaction);

        // Check if the wallet balance was not updated
        verify(m_walletRepository, never()).save(m_wallet1);
    }
}
