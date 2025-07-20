/*
 * Filename: WalletServiceTest.java
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
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
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletType;
import org.moinex.repository.wallettransaction.TransferRepository;
import org.moinex.repository.wallettransaction.WalletRepository;
import org.moinex.repository.wallettransaction.WalletTransactionRepository;
import org.moinex.repository.wallettransaction.WalletTypeRepository;
import org.moinex.util.Constants;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {
    @Mock private WalletRepository walletRepository;

    @Mock private WalletTypeRepository walletTypeRepository;

    @Mock private WalletTransactionRepository walletTransactionRepository;

    @Mock private TransferRepository transferRepository;

    @InjectMocks private WalletService walletService;

    private Wallet wallet1;
    private Wallet wallet2;
    private WalletType walletType1;
    private WalletType walletType2;

    @BeforeAll
    static void setUp() {
        MockitoAnnotations.openMocks(WalletServiceTest.class);
    }

    private Wallet CreateWallet(Integer id, String name, BigDecimal balance) {
        return new Wallet(id, name, balance);
    }

    private WalletType createWalletType(Integer id, String name) {
        return new WalletType(id, name);
    }

    @BeforeEach
    void beforeEach() {
        wallet1 = CreateWallet(1, "Wallet1", new BigDecimal("1000"));
        wallet2 = CreateWallet(2, "Wallet2", new BigDecimal("2000"));

        walletType1 = createWalletType(1, "Type1");
        walletType2 = createWalletType(2, "Type2");
    }

    @Test
    @DisplayName("Test if the wallet is created successfully")
    void testCreateWallet() {
        when(walletRepository.existsByName(wallet1.getName())).thenReturn(false);

        walletService.addWallet(wallet1.getName(), wallet1.getBalance(), walletType1);

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);

        verify(walletRepository).save(walletCaptor.capture());

        assertEquals(wallet1.getName(), walletCaptor.getValue().getName());

        assertEquals(
                wallet1.getBalance().doubleValue(),
                walletCaptor.getValue().getBalance().doubleValue(),
                Constants.EPSILON);
    }

    @Test
    @DisplayName("Should throw exception when trying to create a wallet with empty name")
    void testCreateWalletWithEmptyName() {
        String name = "  ";
        BigDecimal balance = wallet1.getBalance();

        assertThrows(
                IllegalArgumentException.class,
                () -> walletService.addWallet(name, balance, walletType1));

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if the wallet is not created when the name is already in use")
    void testCreateWalletAlreadyExists() {
        when(walletRepository.existsByName(wallet1.getName())).thenReturn(true);

        String name = wallet1.getName();
        BigDecimal balance = wallet1.getBalance();

        assertThrows(
                EntityExistsException.class,
                () -> walletService.addWallet(name, balance, walletType1));

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if the wallet is deleted successfully")
    void testDeleteWallet() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));
        when(walletTransactionRepository.getTransactionCountByWallet(wallet1.getId()))
                .thenReturn(0);
        when(transferRepository.getTransferCountByWallet(wallet1.getId())).thenReturn(0);

        walletService.deleteWallet(wallet1.getId());

        verify(walletRepository).delete(wallet1);
    }

    @Test
    @DisplayName("Test if the wallet is not deleted when it does not exist")
    void testDeleteWalletDoesNotExist() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.empty());

        Integer walletId = wallet1.getId();

        assertThrows(EntityNotFoundException.class, () -> walletService.deleteWallet(walletId));

        verify(walletRepository, never()).delete(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if the wallet is not deleted when it has transactions")
    void testDeleteWalletWithTransactions() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));
        when(walletTransactionRepository.getTransactionCountByWallet(wallet1.getId()))
                .thenReturn(1);

        Integer walletId = wallet1.getId();

        assertThrows(IllegalStateException.class, () -> walletService.deleteWallet(walletId));

        verify(walletRepository, never()).delete(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if the wallet is not deleted when it has transfers")
    void testDeleteWalletWithTransfers() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));
        when(walletTransactionRepository.getTransactionCountByWallet(wallet1.getId()))
                .thenReturn(0);
        when(transferRepository.getTransferCountByWallet(wallet1.getId())).thenReturn(1);

        Integer walletId = wallet1.getId();

        assertThrows(IllegalStateException.class, () -> walletService.deleteWallet(walletId));

        verify(walletRepository, never()).delete(any(Wallet.class));
    }

    @DisplayName("Test if the wallet is archived successfully")
    @Test
    void testArchiveWallet() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet1);

        walletService.archiveWallet(wallet1.getId());

        verify(walletRepository).save(wallet1);
        assertTrue(wallet1.isArchived());
    }

    @Test
    @DisplayName("Test if exception is thrown when trying to archive a non-existent wallet")
    void testArchiveWalletDoesNotExist() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.empty());

        Integer walletId = wallet1.getId();

        assertThrows(EntityNotFoundException.class, () -> walletService.archiveWallet(walletId));

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if the wallet is unarchived successfully")
    void testArchiveWalletUnarchived() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

        wallet1.setArchived(true);

        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet1);

        walletService.unarchiveWallet(wallet1.getId());

        verify(walletRepository).save(wallet1);
        assertFalse(wallet1.isArchived());
    }

    @Test
    @DisplayName("Test if exception is thrown when trying to unarchive a non-existent wallet")
    void testUnarchiveWalletDoesNotExist() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.empty());

        Integer walletId = wallet1.getId();

        assertThrows(EntityNotFoundException.class, () -> walletService.unarchiveWallet(walletId));

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if the wallet has been renamed successfully")
    void testRenameWallet() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet1);

        String newName = "NewWalletName";

        walletService.renameWallet(wallet1.getId(), newName);

        verify(walletRepository).save(wallet1);
        assertEquals(newName, wallet1.getName());
    }

    @Test
    @DisplayName("Test if exception is thrown when trying to rename a wallet with an empty name")
    void testRenameWalletWithEmptyName() {
        String newName = "  ";

        assertThrows(
                IllegalArgumentException.class,
                () -> walletService.renameWallet(wallet1.getId(), newName));

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when trying to rename a non-existent wallet")
    void testRenameWalletDoesNotExist() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.empty());

        Integer walletId = wallet1.getId();
        String newName = "NewWalletName";

        assertThrows(
                EntityNotFoundException.class, () -> walletService.renameWallet(walletId, newName));

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName(
            "Test if exception is thrown when trying to rename a wallet to an "
                    + "already existing name")
    void testRenameWalletAlreadyExists() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

        when(walletRepository.existsByName(wallet2.getName())).thenReturn(true);

        Integer walletId = wallet1.getId();
        String walletName = wallet2.getName();

        assertThrows(
                EntityExistsException.class,
                () -> walletService.renameWallet(walletId, walletName));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if the wallet type is changed successfully")
    void testChangeWalletType() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

        when(walletTypeRepository.existsById(walletType2.getId())).thenReturn(true);

        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet1);

        wallet1.setType(walletType1);

        walletService.changeWalletType(wallet1.getId(), walletType2);

        verify(walletRepository).save(wallet1);
        assertEquals(walletType2.getId(), wallet1.getType().getId());
    }

    @Test
    @DisplayName(
            "Test if exception is thrown when trying to change the wallet type of "
                    + "a non-existent wallet")
    void testChangeWalletTypeWalletDoesNotExist() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.empty());

        Integer walletId = wallet1.getId();

        assertThrows(
                EntityNotFoundException.class,
                () -> walletService.changeWalletType(walletId, walletType2));

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName(
            "Test if exception is thrown when trying to change the wallet type to "
                    + "a non-existent type")
    void testChangeWalletTypeDoesNotExist() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

        when(walletTypeRepository.existsById(walletType2.getId())).thenReturn(false);

        Integer walletId = wallet1.getId();

        assertThrows(
                EntityNotFoundException.class,
                () -> walletService.changeWalletType(walletId, walletType2));

        assertThrows(
                EntityNotFoundException.class,
                () -> walletService.changeWalletType(walletId, null));

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName(
            "Test if exception is thrown when trying to change the wallet type of "
                    + "a wallet to the same type")
    void testChangeWalletTypeSameType() {
        wallet1.setType(walletType1);

        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

        when(walletTypeRepository.existsById(wallet1.getType().getId())).thenReturn(true);

        assertThrows(
                MoinexException.AttributeAlreadySetException.class,
                () -> walletService.changeWalletType(wallet1.getId(), wallet1.getType()));

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if the wallet balance is updated successfully")
    void testUpdateWalletBalance() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet1);

        BigDecimal newBalance = new BigDecimal("2000.0");

        walletService.updateWalletBalance(wallet1.getId(), newBalance);

        verify(walletRepository).save(wallet1);
        assertEquals(
                newBalance.doubleValue(), wallet1.getBalance().doubleValue(), Constants.EPSILON);
    }

    @Test
    @DisplayName(
            "Test if exception is thrown when trying to update the balance of a "
                    + "non-existent wallet")
    void testUpdateWalletBalanceDoesNotExist() {
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.empty());

        Integer walletId = wallet1.getId();
        BigDecimal newBalance = new BigDecimal("1000.0");

        assertThrows(
                EntityNotFoundException.class,
                () -> walletService.updateWalletBalance(walletId, newBalance));

        verify(walletRepository, never()).save(any(Wallet.class));
    }
}
