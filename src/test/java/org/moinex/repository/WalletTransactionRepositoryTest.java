/*
 * Filename: WalletTransactionRepositoryTest.java
 * Created on: September 14, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.moinex.app.config.AppConfig;
import org.moinex.model.Category;
import org.moinex.model.enums.TransactionStatus;
import org.moinex.model.enums.TransactionType;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.repository.wallettransaction.WalletRepository;
import org.moinex.repository.wallettransaction.WalletTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Tests for the WalletTransactionRepository
 */
@DataJpaTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AppConfig.class})
@ActiveProfiles("test")
class WalletTransactionRepositoryTest {
    @Autowired private WalletTransactionRepository walletTransactionRepository;

    @Autowired private WalletRepository walletRepository;

    @Autowired private CategoryRepository categoryRepository;

    private Wallet wallet1;

    private Wallet wallet2;

    private Category createCategory(String name) {
        Category category = Category.builder().name(name).build();
        categoryRepository.save(category);
        categoryRepository.flush();
        return category;
    }

    private WalletTransaction createWalletTransaction(
            Wallet wallet, BigDecimal amount, LocalDateTime date) {
        WalletTransaction walletTransaction =
                WalletTransaction.builder()
                        .wallet(wallet)
                        .amount(amount)
                        .date(date)
                        .status(TransactionStatus.CONFIRMED)
                        .type(TransactionType.EXPENSE)
                        .category(createCategory("Category"))
                        .includeInAnalysis(true)
                        .build();

        walletTransactionRepository.save(walletTransaction);
        walletTransactionRepository.flush();
        return walletTransaction;
    }

    private Wallet createWallet(String name, BigDecimal balance) {
        Wallet wallet = Wallet.builder().name(name).balance(balance).build();
        walletRepository.save(wallet);
        walletRepository.flush();
        return wallet;
    }

    @BeforeEach
    void setUp() {
        wallet1 = createWallet("Wallet1", new BigDecimal("1000.0"));
        wallet2 = createWallet("Wallet2", new BigDecimal("2000.0"));
    }

    @Test
    @DisplayName("Test if the last n transactions in a wallet are returned correctly")
    void testGetLastTransactions() {
        // Create the wallet transactions
        WalletTransaction walletTransaction1 =
                createWalletTransaction(wallet1, new BigDecimal("140.0"), LocalDateTime.now());
        WalletTransaction walletTransaction2 =
                createWalletTransaction(
                        wallet1, new BigDecimal("210.0"), LocalDateTime.now().minusDays(1));

        WalletTransaction walletTransaction3 =
                createWalletTransaction(
                        wallet1, new BigDecimal("300.0"), LocalDateTime.now().minusDays(2));

        createWalletTransaction(wallet1, new BigDecimal("300.0"), LocalDateTime.now().minusDays(3));

        // Request the last 3 transactions
        Pageable request = PageRequest.ofSize(3);

        // Get the last transactions in the wallet by date
        List<WalletTransaction> lastTransactions =
                walletTransactionRepository.findLastTransactionsByWallet(wallet1.getId(), request);

        // Check if the last transactions are correct
        assertEquals(3, lastTransactions.size());

        // Check if the last transactions are in the correct order
        assertEquals(walletTransaction1, lastTransactions.get(0));
        assertEquals(walletTransaction2, lastTransactions.get(1));
        assertEquals(walletTransaction3, lastTransactions.get(2));
    }

    @Test
    @DisplayName(
            "Test if the last n transactions in a wallet are returned correctly "
                    + "when there are no transactions")
    void testGetLastTransactionsNoTransactions() {
        // Request the last 3 transactions
        Pageable request = PageRequest.ofSize(3);

        // Get the last transactions in the wallet by date
        List<WalletTransaction> lastTransactions =
                walletTransactionRepository.findLastTransactionsByWallet(wallet1.getId(), request);

        // Check if the last transactions are correct
        assertEquals(0, lastTransactions.size());
    }

    @Test
    @DisplayName("Test if the last n transactions of all wallets are returned correctly")
    void testGetLastTransactionsAllWallets() {
        // Create the wallet transactions
        WalletTransaction walletTransaction1 =
                createWalletTransaction(wallet1, new BigDecimal("140.0"), LocalDateTime.now());
        WalletTransaction walletTransaction2 =
                createWalletTransaction(
                        wallet1, new BigDecimal("210.0"), LocalDateTime.now().minusDays(1));

        WalletTransaction walletTransaction3 =
                createWalletTransaction(
                        wallet2, new BigDecimal("300.0"), LocalDateTime.now().minusDays(2));

        createWalletTransaction(wallet2, new BigDecimal("300.0"), LocalDateTime.now().minusDays(3));

        // Request the last 3 transactions
        Pageable request = PageRequest.ofSize(3);

        // Get the last transactions in the wallet by date
        List<WalletTransaction> lastTransactions =
                walletTransactionRepository.findLastTransactions(request);

        // Check if the last transactions are correct
        assertEquals(3, lastTransactions.size());

        // Check if the last transactions are in the correct order
        assertEquals(walletTransaction1, lastTransactions.get(0));
        assertEquals(walletTransaction2, lastTransactions.get(1));
        assertEquals(walletTransaction3, lastTransactions.get(2));
    }
}
