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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.error.MoinexException;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletType;
import org.moinex.repository.wallettransaction.TransferRepository;
import org.moinex.repository.wallettransaction.WalletRepository;
import org.moinex.repository.wallettransaction.WalletTransactionRepository;
import org.moinex.repository.wallettransaction.WalletTypeRepository;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private WalletTypeRepository walletTypeRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private TransferRepository transferRepository;

    @InjectMocks private WalletService walletService;

    private Wallet wallet1;
    private WalletType walletType1;

    @BeforeEach
    void setUp() {
        walletType1 = new WalletType(1, "Type1");
        wallet1 = new Wallet(1, "Wallet1", new BigDecimal("1000.00"));
        wallet1.setType(walletType1);
    }

    @Nested
    @DisplayName("Add Wallet Tests")
    class AddWalletTests {
        @Test
        @DisplayName("Should add wallet successfully")
        void addWallet_Success() {
            when(walletRepository.existsByName(wallet1.getName())).thenReturn(false);

            walletService.addWallet(wallet1.getName(), wallet1.getBalance(), walletType1);

            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository).save(walletCaptor.capture());
            assertEquals(wallet1.getName(), walletCaptor.getValue().getName());
            assertEquals(0, wallet1.getBalance().compareTo(walletCaptor.getValue().getBalance()));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for an empty name")
        void addWallet_EmptyName_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> walletService.addWallet("  ", BigDecimal.TEN, walletType1));
            verify(walletRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw EntityExistsException for a duplicate name")
        void addWallet_DuplicateName_ThrowsException() {
            when(walletRepository.existsByName(wallet1.getName())).thenReturn(true);

            assertThrows(
                    EntityExistsException.class,
                    () ->
                            walletService.addWallet(
                                    wallet1.getName(), wallet1.getBalance(), walletType1));
            verify(walletRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Wallet Tests")
    class DeleteWalletTests {
        @Test
        @DisplayName("Should delete wallet successfully if it has no transactions")
        void deleteWallet_Success() {
            when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));
            when(walletTransactionRepository.getTransactionCountByWallet(wallet1.getId()))
                    .thenReturn(0);
            when(transferRepository.getTransferCountByWallet(wallet1.getId())).thenReturn(0);

            walletService.deleteWallet(wallet1.getId());

            verify(walletRepository).delete(wallet1);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException for a non-existent wallet")
        void deleteWallet_NotFound_ThrowsException() {
            when(walletRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> walletService.deleteWallet(999));
        }

        @Test
        @DisplayName("Should throw IllegalStateException if wallet has transactions")
        void deleteWallet_WithTransactions_ThrowsException() {
            when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));
            when(walletTransactionRepository.getTransactionCountByWallet(wallet1.getId()))
                    .thenReturn(1);

            assertThrows(
                    IllegalStateException.class, () -> walletService.deleteWallet(wallet1.getId()));
        }

        @Test
        @DisplayName("Should unlink virtual wallets before deleting master wallet")
        void deleteMasterWallet_UnlinkVirtualWallets() {
            Wallet masterWallet = new Wallet(1, "Master Wallet", new BigDecimal("100.00"));
            Wallet virtualWallet1 = new Wallet(2, "Virtual Wallet1", new BigDecimal("50.00"));
            Wallet virtualWallet2 = new Wallet(3, "Virtual Wallet2", new BigDecimal("30.00"));
            virtualWallet1.setMasterWallet(masterWallet);
            virtualWallet2.setMasterWallet(masterWallet);

            when(walletRepository.findById(masterWallet.getId()))
                    .thenReturn(Optional.of(masterWallet));
            when(walletRepository.findVirtualWalletsByMasterWallet(masterWallet.getId()))
                    .thenReturn(List.of(virtualWallet1, virtualWallet2));
            when(walletTransactionRepository.getTransactionCountByWallet(masterWallet.getId()))
                    .thenReturn(0);
            when(transferRepository.getTransferCountByWallet(masterWallet.getId())).thenReturn(0);

            walletService.deleteWallet(masterWallet.getId());

            ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository, times(2)).save(captor.capture());
            List<Wallet> savedWallets = captor.getAllValues();
            assertTrue(savedWallets.stream().allMatch(w -> w.getMasterWallet() == null));
            assertTrue(
                    savedWallets.stream().anyMatch(w -> w.getId().equals(virtualWallet1.getId())));
            assertTrue(
                    savedWallets.stream().anyMatch(w -> w.getId().equals(virtualWallet2.getId())));
            verify(walletRepository).delete(masterWallet);
        }
    }

    @Nested
    @DisplayName("Update Wallet Balance Tests")
    class UpdateWalletBalanceTests {

        private Wallet masterWallet;
        private Wallet virtualWallet;

        @BeforeEach
        void setup() {
            masterWallet = new Wallet(1, "Master Wallet", new BigDecimal("1000.00"));
            virtualWallet = new Wallet(2, "Virtual Wallet", new BigDecimal("200.00"));
            virtualWallet.setMasterWallet(masterWallet);
        }

        @Test
        @DisplayName("Should increment balance of a normal (master) wallet")
        void incrementWalletBalance_NormalWallet_Success() {
            BigDecimal amount = new BigDecimal("150.00");
            when(walletRepository.findById(masterWallet.getId()))
                    .thenReturn(Optional.of(masterWallet));

            walletService.incrementWalletBalance(masterWallet.getId(), amount);

            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository).save(walletCaptor.capture());
            assertEquals(new BigDecimal("1150.00"), walletCaptor.getValue().getBalance());
        }

        @Test
        @DisplayName("Should decrement balance of a normal (master) wallet")
        void decrementWalletBalance_NormalWallet_Success() {
            BigDecimal amount = new BigDecimal("150.00");
            when(walletRepository.findById(masterWallet.getId()))
                    .thenReturn(Optional.of(masterWallet));
            when(walletRepository.getSumOfBalancesByMasterWallet(masterWallet.getId()))
                    .thenReturn(BigDecimal.ZERO);

            walletService.decrementWalletBalance(masterWallet.getId(), amount);

            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository).save(walletCaptor.capture());
            assertEquals(new BigDecimal("850.00"), walletCaptor.getValue().getBalance());
        }

        @Test
        @DisplayName("Should increment balance of a virtual wallet and its master wallet")
        void incrementWalletBalance_VirtualWallet_UpdatesBoth() {
            BigDecimal amount = new BigDecimal("50.00");
            when(walletRepository.findById(virtualWallet.getId()))
                    .thenReturn(Optional.of(virtualWallet));

            walletService.incrementWalletBalance(virtualWallet.getId(), amount);

            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository, times(2)).save(walletCaptor.capture());

            List<Wallet> savedWallets = walletCaptor.getAllValues();
            Wallet savedMaster =
                    savedWallets.stream()
                            .filter(w -> w.getId().equals(masterWallet.getId()))
                            .findFirst()
                            .get();
            Wallet savedVirtual =
                    savedWallets.stream()
                            .filter(w -> w.getId().equals(virtualWallet.getId()))
                            .findFirst()
                            .get();

            assertEquals(new BigDecimal("1050.00"), savedMaster.getBalance());
            assertEquals(new BigDecimal("250.00"), savedVirtual.getBalance());
        }

        @Test
        @DisplayName("Should decrement balance of a virtual wallet and its master wallet")
        void decrementWalletBalance_VirtualWallet_UpdatesBoth() {
            BigDecimal amount = new BigDecimal("50.00");
            when(walletRepository.findById(virtualWallet.getId()))
                    .thenReturn(Optional.of(virtualWallet));

            walletService.decrementWalletBalance(virtualWallet.getId(), amount);

            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository, times(2)).save(walletCaptor.capture());

            List<Wallet> savedWallets = walletCaptor.getAllValues();
            Wallet savedMaster =
                    savedWallets.stream()
                            .filter(w -> w.getId().equals(masterWallet.getId()))
                            .findFirst()
                            .get();
            Wallet savedVirtual =
                    savedWallets.stream()
                            .filter(w -> w.getId().equals(virtualWallet.getId()))
                            .findFirst()
                            .get();

            assertEquals(new BigDecimal("950.00"), savedMaster.getBalance());
            assertEquals(new BigDecimal("150.00"), savedVirtual.getBalance());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for a zero amount")
        void updateWalletBalance_ZeroAmount_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> walletService.incrementWalletBalance(1, BigDecimal.ZERO));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> walletService.decrementWalletBalance(1, BigDecimal.ZERO));
        }

        @Test
        @DisplayName(
                "Should throw IllegalStateException for a negative balance after decrement in a"
                        + " virtual wallet")
        void decrementWalletBalance_VirtualWallet_NegativeBalance_ThrowsException() {
            BigDecimal amount = virtualWallet.getBalance().add(BigDecimal.ONE);
            when(walletRepository.findById(virtualWallet.getId()))
                    .thenReturn(Optional.of(virtualWallet));

            assertThrows(
                    IllegalStateException.class,
                    () -> walletService.decrementWalletBalance(virtualWallet.getId(), amount));
        }

        @Test
        @DisplayName(
                "Should throw IllegalStateException for decrement greater than unallocated balance"
                        + " in a master wallet")
        void decrementWalletBalance_MasterWallet_NegativeBalance_ThrowsException() {
            BigDecimal amount = masterWallet.getBalance();
            when(walletRepository.findById(masterWallet.getId()))
                    .thenReturn(Optional.of(masterWallet));
            when(walletRepository.getSumOfBalancesByMasterWallet(masterWallet.getId()))
                    .thenReturn(BigDecimal.ONE);

            assertThrows(
                    IllegalStateException.class,
                    () -> walletService.decrementWalletBalance(masterWallet.getId(), amount));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for a negative amount")
        void updateWalletBalance_NegativeAmount_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> walletService.incrementWalletBalance(1, new BigDecimal("-100")));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> walletService.decrementWalletBalance(1, new BigDecimal("-100")));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException for a non-existent wallet")
        void updateWalletBalance_WalletNotFound_ThrowsException() {
            when(walletRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> walletService.incrementWalletBalance(999, BigDecimal.TEN));
        }
    }

    @Nested
    @DisplayName("Archive and Unarchive Wallet Tests")
    class ArchiveUnarchiveTests {
        @Test
        @DisplayName("Should archive wallet successfully")
        void archiveWallet_Success() {
            when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

            walletService.archiveWallet(wallet1.getId());

            ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository).save(captor.capture());
            assertTrue(captor.getValue().isArchived());
        }

        @Test
        @DisplayName("Should unarchive wallet successfully")
        void unarchiveWallet_Success() {
            wallet1.setArchived(true);
            when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));

            walletService.unarchiveWallet(wallet1.getId());

            ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository).save(captor.capture());
            assertFalse(captor.getValue().isArchived());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when archiving a non-existent wallet")
        void archiveWallet_NotFound_ThrowsException() {
            when(walletRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> walletService.archiveWallet(999));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when unarchiving a non-existent wallet")
        void unarchiveWallet_NotFound_ThrowsException() {
            when(walletRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> walletService.unarchiveWallet(999));
        }
    }

    @Nested
    @DisplayName("Rename Wallet Tests")
    class RenameTests {
        @Test
        @DisplayName("Should rename wallet successfully")
        void renameWallet_Success() {
            String newName = "New Wallet Name";
            when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));
            when(walletRepository.existsByName(newName)).thenReturn(false);

            walletService.renameWallet(wallet1.getId(), newName);

            ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository).save(captor.capture());
            assertEquals(newName, captor.getValue().getName());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for an empty name")
        void renameWallet_EmptyName_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> walletService.renameWallet(wallet1.getId(), "  "));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when renaming a non-existent wallet")
        void renameWallet_NotFound_ThrowsException() {
            when(walletRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> walletService.renameWallet(999, "New Name"));
        }

        @Test
        @DisplayName("Should throw EntityExistsException when renaming to an existing name")
        void renameWallet_NameExists_ThrowsException() {
            String newName = "Existing Name";
            when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));
            when(walletRepository.existsByName(newName)).thenReturn(true);

            assertThrows(
                    EntityExistsException.class,
                    () -> walletService.renameWallet(wallet1.getId(), newName));
        }
    }

    @Nested
    @DisplayName("Change wallet type Tests")
    class ChangeWalletTypeTests {

        @DisplayName("Changes wallet type successfully")
        @Test
        void changeWalletType_Success() {
            WalletType oldType = new WalletType(1, "OldType");
            Wallet wallet = new Wallet(1, "Wallet1", BigDecimal.TEN, oldType);
            WalletType newType = new WalletType(2, "NewType");

            when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
            when(walletTypeRepository.existsById(newType.getId())).thenReturn(true);

            walletService.changeWalletType(wallet.getId(), newType);

            verify(walletRepository).save(wallet);
            assertEquals(newType, wallet.getType());
        }

        @DisplayName("Throws EntityNotFoundException when wallet does not exist")
        @Test
        void changeWalletType_WalletNotFound_ThrowsException() {
            when(walletRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> walletService.changeWalletType(999, new WalletType(1, "Type")));
        }

        @DisplayName("Throws EntityNotFoundException when wallet type does not exist")
        @Test
        void changeWalletType_WalletTypeNotFound_ThrowsException() {
            Wallet wallet = new Wallet(1, "Wallet1", BigDecimal.TEN);
            WalletType newType = new WalletType(2, "NewType");
            when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
            when(walletTypeRepository.existsById(newType.getId())).thenReturn(false);

            assertThrows(
                    EntityNotFoundException.class,
                    () -> walletService.changeWalletType(wallet.getId(), newType));
        }

        @DisplayName("Throws AttributeAlreadySetException when wallet already has the new type")
        @Test
        void changeWalletType_AlreadyHasType_ThrowsException() {
            WalletType currentType = new WalletType(1, "CurrentType");
            Wallet wallet = new Wallet(1, "Wallet1", BigDecimal.TEN);
            wallet.setType(currentType);
            when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
            when(walletTypeRepository.existsById(currentType.getId())).thenReturn(true);

            assertThrows(
                    MoinexException.AttributeAlreadySetException.class,
                    () -> walletService.changeWalletType(wallet.getId(), currentType));
        }
    }
}
