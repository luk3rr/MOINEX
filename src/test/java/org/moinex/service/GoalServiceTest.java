package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
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
import org.moinex.model.wallettransaction.WalletType;
import org.moinex.repository.goal.GoalRepository;
import org.moinex.repository.wallettransaction.TransferRepository;
import org.moinex.repository.wallettransaction.WalletRepository;
import org.moinex.repository.wallettransaction.WalletTransactionRepository;
import org.moinex.repository.wallettransaction.WalletTypeRepository;
import org.moinex.util.Constants;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock private GoalRepository goalRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransferRepository transfersRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private WalletTypeRepository walletTypeRepository;

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
    }

    @Nested
    @DisplayName("Update Goal Tests")
    class UpdateGoalTests {
        @Test
        @DisplayName("Should update goal successfully")
        void updateGoal_Success() {
            Goal updatedGoal =
                    Goal.builder()
                            .id(goal.getId())
                            .name("Updated Goal Name")
                            .initialBalance(goal.getInitialBalance())
                            .balance(goal.getBalance())
                            .targetBalance(new BigDecimal("1200.00"))
                            .targetDate(goal.getTargetDate().plusMonths(1))
                            .motivation("Updated Motivation")
                            .build();

            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

            goalService.updateGoal(updatedGoal);

            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepository).save(goalCaptor.capture());
            assertEquals("Updated Goal Name", goalCaptor.getValue().getName());
            assertEquals(new BigDecimal("1200.00"), goalCaptor.getValue().getTargetBalance());
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
        @DisplayName("Should archive goal successfully")
        void archiveGoal_Success() {
            when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

            goalService.archiveGoal(goal.getId());

            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepository).save(goalCaptor.capture());
            assertTrue(goalCaptor.getValue().isArchived());
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
}
