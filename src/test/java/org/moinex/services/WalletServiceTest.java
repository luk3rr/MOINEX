/*
 * Filename: WalletServiceTest.java
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.moinex.entities.Wallet;
import org.moinex.entities.WalletType;
import org.moinex.repositories.CategoryRepository;
import org.moinex.repositories.TransferRepository;
import org.moinex.repositories.WalletRepository;
import org.moinex.repositories.WalletTransactionRepository;
import org.moinex.repositories.WalletTypeRepository;
import org.moinex.util.Constants;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest
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
    private WalletService m_walletService;

    private Wallet     m_wallet1;
    private Wallet     m_wallet2;
    private WalletType m_walletType1;
    private WalletType m_walletType2;

    private Wallet CreateWallet(Long id, String name, BigDecimal balance)
    {
        Wallet wallet = new Wallet(id, name, balance);
        return wallet;
    }

    private WalletType createWalletType(Long id, String name)
    {
        WalletType walletType = new WalletType(id, name);
        return walletType;
    }

    @BeforeAll
    public static void setUp()
    {
        MockitoAnnotations.openMocks(WalletServiceTest.class);
    }

    @BeforeEach
    public void beforeEach()
    {
        m_wallet1 = CreateWallet(1L, "Wallet1", new BigDecimal("1000"));
        m_wallet2 = CreateWallet(2L, "Wallet2", new BigDecimal("2000"));

        m_walletType1 = createWalletType(1L, "Type1");
        m_walletType2 = createWalletType(2L, "Type2");
    }

    @Test
    @DisplayName("Test if the wallet is created successfully")
    public void testCreateWallet()
    {
        when(m_walletRepository.existsByName(m_wallet1.getName())).thenReturn(false);

        m_walletService.addWallet(m_wallet1.getName(), m_wallet1.getBalance());

        // Capture the wallet object that was saved and check if the values are correct
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);

        verify(m_walletRepository).save(walletCaptor.capture());

        assertEquals(m_wallet1.getName(), walletCaptor.getValue().getName());

        assertEquals(m_wallet1.getBalance().doubleValue(),
                     walletCaptor.getValue().getBalance().doubleValue(),
                     Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if the wallet is not created when the name is already in use")
    public void testCreateWalletAlreadyExists()
    {
        when(m_walletRepository.existsByName(m_wallet1.getName())).thenReturn(true);

        assertThrows(RuntimeException.class,
                     ()
                         -> m_walletService.addWallet(m_wallet1.getName(),
                                                      m_wallet1.getBalance()));

        // Verify that the wallet was not saved
        verify(m_walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if the wallet is deleted successfully")
    public void testDeleteWallet()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.of(m_wallet1));

        m_walletService.deleteWallet(m_wallet1.getId());

        // Verify that the wallet was deleted
        verify(m_walletRepository).delete(m_wallet1);
    }

    @Test
    @DisplayName("Test if the wallet is not deleted when it does not exist")
    public void testDeleteWalletDoesNotExist()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> m_walletService.deleteWallet(m_wallet1.getId()));

        // Verify that the wallet was not deleted
        verify(m_walletRepository, never()).delete(any(Wallet.class));
    }

    @DisplayName("Test if the wallet is archived successfully")
    @Test
    public void testArchiveWallet()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.of(m_wallet1));

        when(m_walletRepository.save(any(Wallet.class))).thenReturn(m_wallet1);

        m_walletService.archiveWallet(m_wallet1.getId());

        // Check if the wallet was archived
        verify(m_walletRepository).save(m_wallet1);
        assertTrue(m_wallet1.getIsArchived());
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when trying to archive a non-existent wallet")
    public void
    testArchiveWalletDoesNotExist()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> m_walletService.archiveWallet(m_wallet1.getId()));

        // Verify that the wallet was not archived
        verify(m_walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if the wallet has been renamed successfully")
    public void testRenameWallet()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.of(m_wallet1));

        when(m_walletRepository.save(any(Wallet.class))).thenReturn(m_wallet1);

        String newName = "NewWalletName";

        m_walletService.renameWallet(m_wallet1.getId(), newName);

        // Check if the wallet was renamed
        verify(m_walletRepository).save(m_wallet1);
        assertEquals(newName, m_wallet1.getName());
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when trying to rename a non-existent wallet")
    public void
    testRenameWalletDoesNotExist()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.empty());

        assertThrows(
            RuntimeException.class,
            () -> m_walletService.renameWallet(m_wallet1.getId(), "NewWalletName"));

        // Verify that the wallet was not renamed
        verify(m_walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when trying to rename a wallet to an "
                 + "already existing name")
    public void
    testRenameWalletAlreadyExists()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.of(m_wallet1));

        when(m_walletRepository.existsByName(m_wallet2.getName())).thenReturn(true);

        assertThrows(
            RuntimeException.class,
            () -> m_walletService.renameWallet(m_wallet1.getId(), m_wallet2.getName()));

        // Verify that the wallet was not renamed
        verify(m_walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if the wallet type is changed successfully")
    public void testChangeWalletType()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.of(m_wallet1));

        when(m_walletTypeRepository.existsById(m_walletType2.getId())).thenReturn(true);

        when(m_walletRepository.save(any(Wallet.class))).thenReturn(m_wallet1);

        // Define the wallet type of the wallet to be changed
        m_wallet1.setType(m_walletType1);

        m_walletService.changeWalletType(m_wallet1.getId(), m_walletType2);

        // Check if the wallet type was updated
        verify(m_walletRepository).save(m_wallet1);
        assertEquals(m_walletType2.getId(), m_wallet1.getType().getId());
    }

    @Test
    @DisplayName("Test if exception is thrown when trying to change the wallet type of "
                 + "a non-existent wallet")
    public void
    testChangeWalletTypeWalletDoesNotExist()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.empty());

        assertThrows(
            RuntimeException.class,
            () -> m_walletService.changeWalletType(m_wallet1.getId(), m_walletType2));

        // Verify that the wallet type was not updated
        verify(m_walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when trying to change the wallet type to "
                 + "a non-existent type")
    public void
    testChangeWalletTypeDoesNotExist()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.of(m_wallet1));

        when(m_walletTypeRepository.existsById(m_walletType2.getId()))
            .thenReturn(false);

        assertThrows(
            RuntimeException.class,
            () -> m_walletService.changeWalletType(m_wallet1.getId(), m_walletType2));

        assertThrows(RuntimeException.class,
                     () -> m_walletService.changeWalletType(m_wallet1.getId(), null));

        // Verify that the wallet type was not updated
        verify(m_walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when trying to change the wallet type of "
                 + "a wallet to the same type")
    public void
    testChangeWalletTypeSameType()
    {
        // Define the wallet type of the wallet to be changed
        m_wallet1.setType(m_walletType1);

        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.of(m_wallet1));

        when(m_walletTypeRepository.existsById(m_wallet1.getType().getId()))
            .thenReturn(true);

        assertThrows(RuntimeException.class,
                     ()
                         -> m_walletService.changeWalletType(m_wallet1.getId(),
                                                             m_wallet1.getType()));

        // Verify that the wallet type was not updated
        verify(m_walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Test if the wallet balance is updated successfully")
    public void testUpdateWalletBalance()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.of(m_wallet1));
        when(m_walletRepository.save(any(Wallet.class))).thenReturn(m_wallet1);

        BigDecimal newBalance = new BigDecimal("2000.0");

        m_walletService.updateWalletBalance(m_wallet1.getId(), newBalance);

        // Check if the wallet balance was updated
        verify(m_walletRepository).save(m_wallet1);
        assertEquals(newBalance.doubleValue(),
                     m_wallet1.getBalance().doubleValue(),
                     Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if exception is thrown when trying to update the balance of a "
                 + "non-existent wallet")
    public void
    testUpdateWalletBalanceDoesNotExist()
    {
        when(m_walletRepository.findById(m_wallet1.getId()))
            .thenReturn(Optional.empty());

        assertThrows(
            RuntimeException.class,
            ()
                -> m_walletService.updateWalletBalance(m_wallet1.getId(),
                                                       new BigDecimal("1000.0")));

        // Verify that the wallet balance was not updated
        verify(m_walletRepository, never()).save(any(Wallet.class));
    }
}
