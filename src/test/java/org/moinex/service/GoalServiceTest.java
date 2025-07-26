package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
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
import org.moinex.model.goal.Goal;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletType;
import org.moinex.repository.goal.GoalRepository;
import org.moinex.repository.wallettransaction.TransferRepository;
import org.moinex.repository.wallettransaction.WalletRepository;
import org.moinex.repository.wallettransaction.WalletTransactionRepository;
import org.moinex.repository.wallettransaction.WalletTypeRepository;
import org.moinex.util.Constants;
import org.moinex.util.enums.GoalFundingStrategy;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock private GoalRepository goalRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransferRepository transfersRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private WalletTypeRepository walletTypeRepository;
    @Mock private WalletService walletService;

    @InjectMocks private GoalService goalService;

    private Goal goal;
    private WalletType walletType;

    @BeforeEach
    void setUp() {
        walletType = new WalletType(1, Constants.GOAL_DEFAULT_WALLET_TYPE_NAME);
        goal =
                Goal.builder()
                        .id(1)
                        .name("Test Goal")
                        .initialBalance(new BigDecimal("100.00"))
                        .balance(new BigDecimal("150.00"))
                        .targetBalance(new BigDecimal("1000.00"))
                        .targetDate(LocalDateTime.now().plusMonths(6))
                        .motivation("Test Motivation")
                        .type(walletType)
                        .build();
    }

    @Test
    @DisplayName("Should get all goals")
    void getGoals_Success() {
        when(goalRepository.findAll()).thenReturn(Collections.singletonList(goal));

        List<Goal> goals = goalService.getGoals();

        assertNotNull(goals);
        assertEquals(1, goals.size());
        verify(goalRepository).findAll();
    }

    @Nested
    @DisplayName("Add Goal Tests")
    class AddGoalTests {
        @Test
        @DisplayName("Should add goal successfully")
        void addGoal_Success() {
            when(goalRepository.existsByName(anyString())).thenReturn(false);
            when(walletRepository.existsByName(anyString())).thenReturn(false);
            when(walletTypeRepository.findByName(anyString())).thenReturn(Optional.of(walletType));

            goalService.addGoal(
                    goal.getName(),
                    goal.getInitialBalance(),
                    goal.getTargetBalance(),
                    goal.getTargetDate().toLocalDate(),
                    goal.getMotivation());

            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepository).save(goalCaptor.capture());
            assertEquals(goal.getName(), goalCaptor.getValue().getName());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if goal name is empty")
        void addGoal_EmptyName_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            goalService.addGoal(
                                    "  ",
                                    goal.getInitialBalance(),
                                    goal.getTargetBalance(),
                                    goal.getTargetDate().toLocalDate(),
                                    goal.getMotivation()));
        }

        @Test
        @DisplayName("Should throw EntityExistsException if goal name already exists")
        void addGoal_GoalNameExists_ThrowsException() {
            when(goalRepository.existsByName(anyString())).thenReturn(true);

            assertThrows(
                    EntityExistsException.class,
                    () ->
                            goalService.addGoal(
                                    goal.getName(),
                                    goal.getInitialBalance(),
                                    goal.getTargetBalance(),
                                    goal.getTargetDate().toLocalDate(),
                                    goal.getMotivation()));
        }

        @Test
        @DisplayName(
                "Should throw EntityExistsException if already exists a wallet with the same name")
        void addGoal_WalletNameExists_ThrowsException() {
            when(walletRepository.existsByName(anyString())).thenReturn(true);

            assertThrows(
                    EntityExistsException.class,
                    () ->
                            goalService.addGoal(
                                    goal.getName(),
                                    goal.getInitialBalance(),
                                    goal.getTargetBalance(),
                                    goal.getTargetDate().toLocalDate(),
                                    goal.getMotivation()));
        }

        @Test
        @DisplayName(
                "Should throw EntityNotFoundException if default wallet type for goal not found")
        void addGoal_WalletTypeNotFound_ThrowsException() {
            when(walletTypeRepository.findByName(Constants.GOAL_DEFAULT_WALLET_TYPE_NAME))
                    .thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            goalService.addGoal(
                                    goal.getName(),
                                    goal.getInitialBalance(),
                                    goal.getTargetBalance(),
                                    goal.getTargetDate().toLocalDate(),
                                    goal.getMotivation()));
        }

        @Test
        @DisplayName(
                "Should throw IllegalArgumentException if master wallet is actually a virtual"
                        + " wallet")
        void addGoal_MasterWalletIsVirtual_ThrowsException() {
            Wallet masterWallet = new Wallet(1, "Virtual Wallet", BigDecimal.ZERO);
            masterWallet.setMasterWallet(new Wallet(2, "Master Wallet", BigDecimal.ZERO));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            goalService.addGoal(
                                    goal.getName(),
                                    goal.getInitialBalance(),
                                    goal.getTargetBalance(),
                                    goal.getTargetDate().toLocalDate(),
                                    goal.getMotivation(),
                                    masterWallet,
                                    any()));

            verify(goalRepository, never()).save(any(Goal.class));
        }
    }

    @Nested
    @DisplayName("Delete Goal Tests")
    class DeleteGoalTests {
        @Test
        @DisplayName("Should delete goal successfully if it has no transactions")
        void deleteGoal_Success() {
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
            when(walletTransactionRepository.getTransactionCountByWallet(goal.getId()))
                    .thenReturn(0);
            when(transfersRepository.getTransferCountByWallet(goal.getId())).thenReturn(0);

            goalService.deleteGoal(goal.getId());

            verify(goalRepository).delete(goal);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when deleting a non-existent goal")
        void deleteGoal_NotFound_ThrowsException() {
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> goalService.deleteGoal(goal.getId()));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when deleting a goal with transactions")
        void deleteGoal_WithTransactions_ThrowsException() {
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
            when(walletTransactionRepository.getTransactionCountByWallet(goal.getId()))
                    .thenReturn(1);

            assertThrows(IllegalStateException.class, () -> goalService.deleteGoal(goal.getId()));
        }

        @Test
        @DisplayName("Should unlink virtual wallets before deleting master wallet")
        void deleteMasterWallet_UnlinkVirtualWallets() {
            Goal virtualGoal =
                    Goal.builder()
                            .id(4)
                            .name("Test Goal")
                            .initialBalance(new BigDecimal("100.00"))
                            .balance(new BigDecimal("150.00"))
                            .targetBalance(new BigDecimal("1000.00"))
                            .targetDate(LocalDateTime.now().plusMonths(6))
                            .motivation("Test Motivation")
                            .type(walletType)
                            .build();

            virtualGoal.setMasterWallet(goal);

            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
            when(walletTransactionRepository.getTransactionCountByWallet(goal.getId()))
                    .thenReturn(0);
            when(transfersRepository.getTransferCountByWallet(goal.getId())).thenReturn(0);

            goalService.deleteGoal(goal.getId());

            verify(walletService).removeAllVirtualWalletsFromMasterWallet(goal.getId());
            verify(goalRepository).delete(goal);
        }
    }

    @Nested
    @DisplayName("Update Goal Tests")
    class UpdateGoalTests {
        @Test
        @DisplayName("Should update goal successfully")
        void updateGoal_Success() {
            goal.setArchived(true);

            Goal updatedGoal =
                    Goal.builder()
                            .id(goal.getId())
                            .name("Updated Goal Name")
                            .initialBalance(goal.getInitialBalance())
                            .balance(goal.getBalance())
                            .targetBalance(new BigDecimal("1200.00"))
                            .targetDate(goal.getTargetDate().plusMonths(1))
                            .motivation("Updated Motivation")
                            .isArchived(false)
                            .build();

            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

            goalService.updateGoal(updatedGoal);

            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepository, atLeastOnce()).save(goalCaptor.capture());
            assertEquals("Updated Goal Name", goalCaptor.getValue().getName());
            assertEquals(new BigDecimal("1200.00"), goalCaptor.getValue().getTargetBalance());
            assertFalse(goalCaptor.getValue().isArchived());
        }

        @Test
        @DisplayName(
                "Should successfully update the initial balance when current balance is increased")
        void updateGoal_InitialBalanceUpdated() {
            Goal updatedGoal =
                    Goal.builder()
                            .id(goal.getId())
                            .name("Updated Goal Name")
                            .initialBalance(goal.getInitialBalance())
                            .balance(goal.getBalance().add(new BigDecimal("50.00")))
                            .targetBalance(goal.getTargetBalance())
                            .targetDate(goal.getTargetDate().plusMonths(1))
                            .isArchived(true)
                            .motivation("Updated Motivation")
                            .build();

            BigDecimal expectedInitialBalance =
                    goal.getInitialBalance().add(new BigDecimal("50.00"));

            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

            goalService.updateGoal(updatedGoal);

            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepository, atLeastOnce()).save(goalCaptor.capture());
            assertEquals(expectedInitialBalance, goalCaptor.getValue().getInitialBalance());
            assertTrue(goalCaptor.getValue().isArchived());
        }

        @Test
        @DisplayName(
                "Should successfully update the initial balance when current balance is decreased")
        void updateGoal_InitialBalanceDecreased() {
            Goal updatedGoal =
                    Goal.builder()
                            .id(goal.getId())
                            .name("Updated Goal Name")
                            .initialBalance(goal.getInitialBalance())
                            .balance(goal.getBalance().subtract(new BigDecimal("50.00")))
                            .targetBalance(goal.getTargetBalance())
                            .targetDate(goal.getTargetDate().plusMonths(1))
                            .motivation("Updated Motivation")
                            .build();

            BigDecimal expectedInitialBalance =
                    goal.getInitialBalance().subtract(new BigDecimal("50.00"));

            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

            goalService.updateGoal(updatedGoal);

            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepository, atLeastOnce()).save(goalCaptor.capture());
            assertEquals(expectedInitialBalance, goalCaptor.getValue().getInitialBalance());
        }

        @Test
        @DisplayName(
                "Should reset initial balance to zero if new balance implies a negative initial"
                        + " value")
        void updateGoal_InitialBalanceResetToZero() {
            Goal updatedGoal =
                    Goal.builder()
                            .id(goal.getId())
                            .name("Updated Goal Name")
                            .initialBalance(goal.getInitialBalance())
                            .balance(
                                    goal.getBalance()
                                            .subtract(goal.getInitialBalance().add(BigDecimal.ONE)))
                            .targetBalance(goal.getTargetBalance())
                            .targetDate(goal.getTargetDate().plusMonths(1))
                            .motivation("Updated Motivation")
                            .build();

            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

            goalService.updateGoal(updatedGoal);

            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepository, atLeastOnce()).save(goalCaptor.capture());
            assertEquals(BigDecimal.ZERO, goalCaptor.getValue().getInitialBalance());
        }

        @Test
        @DisplayName("Successfully complete goal when updating with completion date")
        void toggleGoalStatus_Completed() {
            Goal updatedGoal =
                    Goal.builder()
                            .id(goal.getId())
                            .name("Updated Goal Name")
                            .initialBalance(goal.getInitialBalance())
                            .balance(goal.getBalance())
                            .targetBalance(goal.getBalance())
                            .targetDate(goal.getTargetDate().plusMonths(1))
                            .motivation("Updated Motivation")
                            .completionDate(LocalDateTime.now())
                            .build();

            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

            goalService.updateGoal(updatedGoal);

            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepository, atLeastOnce()).save(goalCaptor.capture());
            assertNotNull(goalCaptor.getValue().getCompletionDate());
            assertTrue(goalCaptor.getValue().isCompleted());
        }

        @Test
        @DisplayName("Successfully reopen goal when updating with null completion date")
        void toggleGoalStatus_Reopened() {
            Goal updatedGoal =
                    Goal.builder()
                            .id(goal.getId())
                            .name("Updated Goal Name")
                            .initialBalance(goal.getInitialBalance())
                            .balance(goal.getBalance())
                            .targetBalance(goal.getTargetBalance())
                            .targetDate(goal.getTargetDate().plusMonths(1))
                            .motivation("Updated Motivation")
                            .build();

            // Set old goal as completed
            goal.setBalance(goal.getTargetBalance());
            goal.setCompletionDate(LocalDateTime.now());

            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

            goalService.updateGoal(updatedGoal);

            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepository, atLeastOnce()).save(goalCaptor.capture());
            assertNull(goalCaptor.getValue().getCompletionDate());
            assertFalse(goalCaptor.getValue().isCompleted());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when updating with empty name")
        void updateGoal_EmptyName_ThrowsException() {
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

            Goal updatedGoal =
                    Goal.builder()
                            .id(goal.getId())
                            .name("  ")
                            .initialBalance(goal.getInitialBalance())
                            .balance(goal.getBalance())
                            .targetBalance(goal.getTargetBalance())
                            .targetDate(goal.getTargetDate())
                            .motivation(goal.getMotivation())
                            .build();

            assertThrows(IllegalArgumentException.class, () -> goalService.updateGoal(updatedGoal));
        }

        @Test
        @DisplayName("Should throw EntityExistsException when updating to an existing goal name")
        void updateGoal_ExistingName_ThrowsException() {
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
            when(goalRepository.existsByName("Existing Goal Name")).thenReturn(true);

            Goal updatedGoal =
                    Goal.builder()
                            .id(goal.getId())
                            .name("Existing Goal Name")
                            .initialBalance(goal.getInitialBalance())
                            .balance(goal.getBalance())
                            .targetBalance(goal.getTargetBalance())
                            .targetDate(goal.getTargetDate())
                            .motivation(goal.getMotivation())
                            .build();

            assertThrows(EntityExistsException.class, () -> goalService.updateGoal(updatedGoal));
        }

        @Test
        @DisplayName("Should throw EntityExistsException when updating to an existing wallet name")
        void updateGoal_WalletNameExists_ThrowsException() {
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
            when(goalRepository.existsByName(anyString())).thenReturn(false);
            when(walletRepository.existsByName("Existing Wallet Name")).thenReturn(true);

            Goal updatedGoal =
                    Goal.builder()
                            .id(goal.getId())
                            .name("Existing Wallet Name")
                            .initialBalance(goal.getInitialBalance())
                            .balance(goal.getBalance())
                            .targetBalance(goal.getTargetBalance())
                            .targetDate(goal.getTargetDate())
                            .motivation(goal.getMotivation())
                            .build();

            assertThrows(EntityExistsException.class, () -> goalService.updateGoal(updatedGoal));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when updating a non-existent goal")
        void updateGoal_NotFound_ThrowsException() {
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> goalService.updateGoal(goal));
        }

        @Nested
        @DisplayName("Update Goal Master Wallet Tests")
        class UpdateGoalMasterWalletTests {
            private Wallet masterWalletA;
            private Wallet masterWalletB;

            @BeforeEach
            void setup() {
                masterWalletA = new Wallet(10, "Master A", new BigDecimal("5000.00"));
                masterWalletB = new Wallet(11, "Master B", new BigDecimal("3000.00"));
            }

            @Test
            @DisplayName("Should associate a master wallet with a standalone goal")
            void updateGoal_SetMasterWalletForStandaloneGoal() {
                goal.setMasterWallet(null);
                Goal updatedGoalData =
                        Goal.builder()
                                .id(goal.getId())
                                .name(goal.getName())
                                .masterWallet(masterWalletA)
                                .balance(goal.getBalance())
                                .initialBalance(goal.getInitialBalance())
                                .targetBalance(goal.getTargetBalance())
                                .targetDate(goal.getTargetDate())
                                .build();

                when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

                goalService.updateGoal(updatedGoalData);

                ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
                verify(goalRepository, atLeastOnce()).save(captor.capture());

                assertNotNull(captor.getValue().getMasterWallet());
                assertEquals(masterWalletA.getId(), captor.getValue().getMasterWallet().getId());

                assertEquals(new BigDecimal("5150.00"), masterWalletA.getBalance());
            }

            @Test
            @DisplayName("Should remove master wallet from a virtual goal")
            void updateGoal_RemoveMasterWalletFromVirtualGoal() {
                goal.setMasterWallet(masterWalletA);
                goal.setBalance(new BigDecimal("200.00"));
                masterWalletA.setBalance(new BigDecimal("5200.00"));

                Goal updatedGoalData =
                        Goal.builder()
                                .id(goal.getId())
                                .name(goal.getName())
                                .masterWallet(null)
                                .balance(goal.getBalance())
                                .initialBalance(goal.getInitialBalance())
                                .targetBalance(goal.getTargetBalance())
                                .targetDate(goal.getTargetDate())
                                .build();

                when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

                goalService.updateGoal(updatedGoalData);

                ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
                verify(goalRepository, atLeastOnce()).save(captor.capture());

                assertNull(captor.getValue().getMasterWallet());

                assertEquals(new BigDecimal("5000.00"), masterWalletA.getBalance());
            }

            @Test
            @DisplayName("Should update master wallet from A to B correctly")
            void updateGoal_SetMasterWalletFromBToA() {
                goal.setMasterWallet(masterWalletA);
                goal.setBalance(new BigDecimal("100.00"));
                masterWalletA.setBalance(new BigDecimal("5100.00"));

                Goal updatedGoalData =
                        Goal.builder()
                                .id(goal.getId())
                                .name(goal.getName())
                                .masterWallet(masterWalletB)
                                .balance(goal.getBalance())
                                .initialBalance(goal.getInitialBalance())
                                .targetBalance(goal.getTargetBalance())
                                .targetDate(goal.getTargetDate())
                                .build();

                when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

                goalService.updateGoal(updatedGoalData);

                ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
                verify(goalRepository, atLeastOnce()).save(goalCaptor.capture());

                assertEquals(masterWalletB, goalCaptor.getValue().getMasterWallet());

                // Check if the balance of the old master wallet (A) was decreased
                assertEquals(new BigDecimal("5000.00"), masterWalletA.getBalance());
                verify(walletRepository).save(masterWalletA);

                // Check if the balance of the new master wallet (B) was changed
                assertEquals(new BigDecimal("3100.00"), masterWalletB.getBalance());
                verify(walletRepository).save(masterWalletB);
            }

            @Test
            @DisplayName(
                    "Should update master wallet correctly when goal current balance is bigger than"
                            + " current balance of the master wallet")
            void updateGoal_SetMasterWalletFromAtoBWithHigherBalance() {
                goal.setMasterWallet(masterWalletA);
                goal.setBalance(new BigDecimal("100.00"));
                masterWalletA.setBalance(new BigDecimal("5000.00"));

                Goal updatedGoalData =
                        Goal.builder()
                                .id(goal.getId())
                                .name(goal.getName())
                                .masterWallet(masterWalletA)
                                .balance(new BigDecimal("5500.00"))
                                .initialBalance(goal.getInitialBalance())
                                .targetBalance(goal.getTargetBalance())
                                .targetDate(goal.getTargetDate())
                                .build();

                when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

                goalService.updateGoal(updatedGoalData);

                ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
                verify(goalRepository, atLeastOnce()).save(goalCaptor.capture());

                assertEquals(masterWalletA, goalCaptor.getValue().getMasterWallet());

                assertEquals(new BigDecimal("10400.00"), masterWalletA.getBalance());
                verify(walletRepository).save(masterWalletA);
            }

            @Test
            @DisplayName("Should throw exception when trying to set a virtual wallet as master")
            void updateGoal_SetVirtualWalletAsMaster_ThrowsException() {
                Wallet newMasterWhoIsVirtual = new Wallet(12, "I am virtual", BigDecimal.TEN);
                newMasterWhoIsVirtual.setMasterWallet(masterWalletA);

                Goal updatedGoalData =
                        Goal.builder()
                                .id(goal.getId())
                                .name(goal.getName())
                                .masterWallet(newMasterWhoIsVirtual)
                                .balance(goal.getBalance())
                                .initialBalance(goal.getInitialBalance())
                                .targetBalance(goal.getTargetBalance())
                                .targetDate(goal.getTargetDate())
                                .build();

                when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

                assertThrows(
                        IllegalArgumentException.class,
                        () -> goalService.updateGoal(updatedGoalData));
            }

            @Test
            @DisplayName("Should not change master wallet if it is the same")
            void updateGoal_SameMasterWallet_NoChange() {
                goal.setMasterWallet(masterWalletA);
                Goal updatedGoalData =
                        Goal.builder()
                                .id(goal.getId())
                                .name(goal.getName())
                                .masterWallet(masterWalletA)
                                .balance(goal.getBalance())
                                .initialBalance(goal.getInitialBalance())
                                .targetBalance(goal.getTargetBalance())
                                .targetDate(goal.getTargetDate())
                                .build();

                when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

                goalService.updateGoal(updatedGoalData);

                ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
                verify(goalRepository, atLeastOnce()).save(captor.capture());

                assertEquals(masterWalletA, captor.getValue().getMasterWallet());
            }
        }
    }

    @Nested
    @DisplayName("Complete and Reopen Goal Tests")
    class CompleteAndReopenGoalTests {
        @Test
        @DisplayName("Should complete goal successfully when balance is sufficient")
        void completeGoal_Success() {
            goal.setBalance(goal.getTargetBalance());
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

            goalService.completeGoal(goal.getId());

            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepository).save(goalCaptor.capture());
            assertNotNull(goalCaptor.getValue().getCompletionDate());
        }

        @Test
        @DisplayName("Should throw IncompleteGoalException when balance is insufficient")
        void completeGoal_InsufficientBalance_ThrowsException() {
            goal.setBalance(goal.getTargetBalance().subtract(BigDecimal.ONE));
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

            assertThrows(
                    MoinexException.IncompleteGoalException.class,
                    () -> goalService.completeGoal(goal.getId()));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when completing a non-existent goal")
        void completeGoal_NotFound_ThrowsException() {
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class, () -> goalService.completeGoal(goal.getId()));
        }

        @Test
        @DisplayName("Should reopen a completed goal")
        void reopenGoal_Success() {
            goal.setCompletionDate(LocalDateTime.now());
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

            goalService.reopenGoal(goal.getId());

            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepository).save(goalCaptor.capture());
            assertNull(goalCaptor.getValue().getCompletionDate());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when reopening a non-existent goal")
        void reopenGoal_NotFound_ThrowsException() {
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> goalService.reopenGoal(goal.getId()));
        }
    }

    @Nested
    @DisplayName("Archive and Unarchive Goal Tests")
    class ArchiveAndUnarchiveGoalTests {
        @Test
        @DisplayName("Should archive goal successfully when its a master wallet")
        void archiveMasterGoal_Success() {
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

            goalService.archiveGoal(goal.getId());

            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepository).save(goalCaptor.capture());
            assertTrue(goalCaptor.getValue().isArchived());
        }

        @Test
        @DisplayName("Should archive goal successfully when its a virtual wallet")
        void archiveVirtualGoal_Success() {
            goal.setMasterWallet(new Wallet(1, "Master Wallet", BigDecimal.ZERO));

            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

            goalService.archiveGoal(goal.getId());

            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepository).save(goalCaptor.capture());
            assertTrue(goalCaptor.getValue().isArchived());

            // Ensure the goal is now a master wallet, meaning it is no longer linked to one
            assertTrue(goalCaptor.getValue().isMaster());

            // Ensure the master wallet is not updated
            // The balance of the goal should still stay in the master wallet
            verify(walletRepository, never()).save(any(Wallet.class));
        }

        @Test
        @DisplayName("Should unarchive goal successfully")
        void unarchiveGoal_Success() {
            goal.setArchived(true);
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

            goalService.unarchiveGoal(goal.getId());

            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepository).save(goalCaptor.capture());
            assertFalse(goalCaptor.getValue().isArchived());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when archiving a non-existent goal")
        void archiveGoal_NotFound_ThrowsException() {
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class, () -> goalService.archiveGoal(goal.getId()));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when unarchiving a non-existent goal")
        void unarchiveGoal_NotFound_ThrowsException() {
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class, () -> goalService.unarchiveGoal(goal.getId()));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {
        @Test
        @DisplayName("Should throw IllegalArgumentException for target date in the past")
        void validateDateAndBalances_PastDate_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            goalService.validateDateAndBalances(
                                    BigDecimal.TEN,
                                    new BigDecimal("100"),
                                    LocalDateTime.now().minusDays(1)));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for negative initial balance")
        void validateDateAndBalances_NegativeInitialBalance_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            goalService.validateDateAndBalances(
                                    new BigDecimal("-1"),
                                    new BigDecimal("100"),
                                    LocalDateTime.now().plusDays(1)));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for zero target balance")
        void validateDateAndBalances_ZeroTargetBalance_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            goalService.validateDateAndBalances(
                                    BigDecimal.ZERO,
                                    BigDecimal.ZERO,
                                    LocalDateTime.now().plusDays(1)));
        }

        @Test
        @DisplayName(
                "Should throw IllegalArgumentException for initial balance greater than target")
        void validateDateAndBalances_InitialGreaterThanTarget_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            goalService.validateDateAndBalances(
                                    new BigDecimal("101"),
                                    new BigDecimal("100"),
                                    LocalDateTime.now().plusDays(1)));
        }
    }

    @Nested
    @DisplayName("Add Goal With Master Wallet Tests")
    class AddGoalWithMasterWalletTests {
        private Wallet masterWallet;

        @BeforeEach
        void setup() {
            masterWallet = new Wallet(10, "Master Wallet", new BigDecimal("1000.00"));
            when(walletTypeRepository.findByName(anyString())).thenReturn(Optional.of(walletType));
            when(goalRepository.existsByName(anyString())).thenReturn(false);
            when(walletRepository.existsByName(anyString())).thenReturn(false);
        }

        @Test
        @DisplayName(
                "Should create goal with NEW_DEPOSIT strategy and update master wallet balance")
        void testAddGoalWithMasterWallet_NewDepositStrategy_Success() {
            BigDecimal initialBalance = new BigDecimal("200.00");

            goalService.addGoal(
                    "New Goal",
                    initialBalance,
                    new BigDecimal("500.00"),
                    LocalDate.now().plusMonths(6),
                    "Motivation",
                    masterWallet,
                    GoalFundingStrategy.NEW_DEPOSIT);

            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository).save(walletCaptor.capture());
            assertEquals(new BigDecimal("1200.00"), walletCaptor.getValue().getBalance());

            verify(goalRepository).save(any(Goal.class));
        }

        @Test
        @DisplayName(
                "Should create goal with ALLOCATE_FROM_EXISTING strategy without changing master"
                        + " wallet balance")
        void testAddGoalWithMasterWallet_AllocateStrategy_Success() {
            BigDecimal initialBalance = new BigDecimal("300.00");
            when(walletRepository.getAllocatedBalanceByMasterWallet(masterWallet.getId()))
                    .thenReturn(new BigDecimal("500.00"));

            goalService.addGoal(
                    "New Goal",
                    initialBalance,
                    new BigDecimal("500.00"),
                    LocalDate.now().plusMonths(6),
                    "Motivation",
                    masterWallet,
                    GoalFundingStrategy.ALLOCATE_FROM_EXISTING);

            verify(walletRepository, never()).save(any(Wallet.class));

            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepository).save(goalCaptor.capture());
            assertEquals(initialBalance, goalCaptor.getValue().getBalance());
            assertEquals(masterWallet, goalCaptor.getValue().getMasterWallet());
        }

        @Test
        @DisplayName(
                "Should throw InsufficientResourcesException when allocating more than free"
                        + " balance")
        void testAddGoalWithMasterWallet_AllocateStrategy_InsufficientFunds() {
            BigDecimal initialBalance = new BigDecimal("300.00");
            when(walletRepository.getAllocatedBalanceByMasterWallet(masterWallet.getId()))
                    .thenReturn(new BigDecimal("800.00"));

            assertThrows(
                    MoinexException.InsufficientResourcesException.class,
                    () ->
                            goalService.addGoal(
                                    "New Goal",
                                    initialBalance,
                                    new BigDecimal("500.00"),
                                    LocalDate.now().plusMonths(6),
                                    "Motivation",
                                    masterWallet,
                                    GoalFundingStrategy.ALLOCATE_FROM_EXISTING));

            verify(goalRepository, never()).save(any());
            verify(walletRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if strategy is null when required")
        void testAddGoalWithMasterWallet_MissingStrategy_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            goalService.addGoal(
                                    "New Goal",
                                    new BigDecimal("100.00"),
                                    new BigDecimal("500.00"),
                                    LocalDate.now().plusMonths(6),
                                    "Motivation",
                                    masterWallet,
                                    null));
        }
    }
}
