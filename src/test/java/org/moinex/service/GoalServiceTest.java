/*
 * Filename: GoalServiceTest.java
 * Created on: December  7, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.moinex.model.goal.Goal;
import org.moinex.model.wallettransaction.WalletType;
import org.moinex.repository.goal.GoalRepository;
import org.moinex.repository.wallettransaction.WalletRepository;
import org.moinex.repository.wallettransaction.WalletTypeRepository;
import org.moinex.util.Constants;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock private WalletTypeRepository walletTypeRepository;

    @Mock private WalletRepository walletRepository;

    @Mock private GoalRepository goalRepository;

    @InjectMocks private GoalService goalService;

    private Goal goal;
    private WalletType walletType;

    @BeforeAll
    static void setUp() {
        MockitoAnnotations.openMocks(WalletServiceTest.class);
    }

    private Goal createGoal(
            Long id,
            String name,
            BigDecimal initialBalance,
            BigDecimal targetBalance,
            LocalDateTime targetDate,
            String motivation) {
        return new Goal(
                id, name, initialBalance, targetBalance, targetDate, motivation, walletType);
    }

    private WalletType createWalletType(Long id, String name) {
        return new WalletType(id, name);
    }

    @BeforeEach
    void beforeEach() {
        goal =
                createGoal(
                        1L,
                        "Goal1",
                        BigDecimal.valueOf(100.0),
                        BigDecimal.valueOf(200.0),
                        LocalDateTime.now().plusDays(30),
                        "Motivation1");

        walletType = createWalletType(1L, Constants.GOAL_DEFAULT_WALLET_TYPE_NAME);
    }

    @Test
    @DisplayName("Test if the goal is created successfully")
    void testCreateGoal() {
        when(goalRepository.existsByName(goal.getName())).thenReturn(false);

        when(walletTypeRepository.findByName(Constants.GOAL_DEFAULT_WALLET_TYPE_NAME))
                .thenReturn(Optional.of(walletType));

        goalService.addGoal(
                goal.getName(),
                goal.getInitialBalance(),
                goal.getTargetBalance(),
                goal.getTargetDate().toLocalDate(),
                goal.getMotivation());

        // Capture the wallet object that was saved and check if the values are correct
        ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);

        verify(goalRepository).save(goalCaptor.capture());

        assertEquals(goal.getName(), goalCaptor.getValue().getName());

        assertEquals(goal.getInitialBalance(), goalCaptor.getValue().getInitialBalance());

        assertEquals(goal.getTargetBalance(), goalCaptor.getValue().getTargetBalance());
    }

    @Test
    @DisplayName("Test if the goal is not created when the name already exists")
    void testCreateGoalAlreadyExists() {
        when(goalRepository.existsByName(goal.getName())).thenReturn(true);

        String goalName = goal.getName();
        BigDecimal initialBalance = goal.getInitialBalance();
        BigDecimal targetBalance = goal.getTargetBalance();
        LocalDate targetDate = goal.getTargetDate().toLocalDate();
        String motivation = goal.getMotivation();

        assertThrows(
                EntityExistsException.class,
                () ->
                        goalService.addGoal(
                                goalName, initialBalance, targetBalance, targetDate, motivation));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is not created when the initial balance is negative")
    void testCreateGoalNegativeInitialBalance() {
        when(goalRepository.existsByName(goal.getName())).thenReturn(false);
        when(walletRepository.existsByName(goal.getName())).thenReturn(false);

        String goalName = goal.getName();
        BigDecimal initialBalance = BigDecimal.valueOf(-1.0);
        BigDecimal targetBalance = goal.getTargetBalance();
        LocalDate targetDate = goal.getTargetDate().toLocalDate();
        String motivation = goal.getMotivation();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        goalService.addGoal(
                                goalName, initialBalance, targetBalance, targetDate, motivation));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is not created when the target balance is negative")
    void testCreateGoalNegativeTargetBalance() {
        when(goalRepository.existsByName(goal.getName())).thenReturn(false);

        String goalName = goal.getName();
        BigDecimal initialBalance = goal.getInitialBalance();
        BigDecimal targetBalance = BigDecimal.valueOf(-1.0);
        LocalDate targetDate = goal.getTargetDate().toLocalDate();
        String motivation = goal.getMotivation();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        goalService.addGoal(
                                goalName, initialBalance, targetBalance, targetDate, motivation));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is not created when target balance zero")
    void testCreateGoalZeroBalance() {
        when(goalRepository.existsByName(goal.getName())).thenReturn(false);

        String goalName = goal.getName();
        BigDecimal initialBalance = BigDecimal.valueOf(0.0);
        BigDecimal targetBalance = BigDecimal.valueOf(0.0);
        LocalDate targetDate = goal.getTargetDate().toLocalDate();
        String motivation = goal.getMotivation();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        goalService.addGoal(
                                goalName, initialBalance, targetBalance, targetDate, motivation));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is not created when the target date is in the past")
    void testCreateGoalTargetDateInPast() {
        when(goalRepository.existsByName(goal.getName())).thenReturn(false);

        String goalName = goal.getName();
        BigDecimal initialBalance = goal.getInitialBalance();
        BigDecimal targetBalance = goal.getTargetBalance();
        LocalDate targetDate = LocalDate.now().minusDays(1);
        String motivation = goal.getMotivation();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        goalService.addGoal(
                                goalName, initialBalance, targetBalance, targetDate, motivation));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "Test if the goal is not created when the target balance is less "
                    + "than the initial balance")
    void testCreateGoalTargetBalanceLessThanInitialBalance() {
        when(goalRepository.existsByName(goal.getName())).thenReturn(false);

        String goalName = goal.getName();
        BigDecimal initialBalance = goal.getInitialBalance();
        BigDecimal targetBalance = initialBalance.subtract(BigDecimal.valueOf(1.0));
        LocalDate targetDate = goal.getTargetDate().toLocalDate();
        String motivation = goal.getMotivation();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        goalService.addGoal(
                                goalName, initialBalance, targetBalance, targetDate, motivation));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is not created when the wallet type does not exist")
    void testCreateGoalWalletTypeDoesNotExist() {
        when(goalRepository.existsByName(goal.getName())).thenReturn(false);

        when(walletTypeRepository.findByName(Constants.GOAL_DEFAULT_WALLET_TYPE_NAME))
                .thenReturn(Optional.empty());

        String goalName = goal.getName();
        BigDecimal initialBalance = goal.getInitialBalance();
        BigDecimal targetBalance = goal.getTargetBalance();
        LocalDate targetDate = goal.getTargetDate().toLocalDate();
        String motivation = goal.getMotivation();

        assertThrows(
                EntityNotFoundException.class,
                () ->
                        goalService.addGoal(
                                goalName, initialBalance, targetBalance, targetDate, motivation));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is archived successfully")
    void testArchiveGoal() {
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

        goalService.archiveGoal(goal.getId());

        verify(goalRepository).save(goal);
        assertTrue(goal.isArchived());
    }

    @Test
    @DisplayName("Test if the goal is unarchived successfully")
    void testUnarchiveGoal() {
        goal.setArchived(true);

        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

        goalService.unarchiveGoal(goal.getId());

        verify(goalRepository).save(goal);
        assertFalse(goal.isArchived());
    }

    @Test
    @DisplayName("Test if the goal is not archived when it does not exist")
    void testArchiveGoalDoesNotExist() {
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.empty());

        Long goalId = goal.getId();

        assertThrows(EntityNotFoundException.class, () -> goalService.archiveGoal(goalId));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is not unarchived when it does not exist")
    void testUnarchiveGoalDoesNotExist() {
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.empty());

        Long goalId = goal.getId();

        assertThrows(EntityNotFoundException.class, () -> goalService.unarchiveGoal(goalId));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is renamed successfully")
    void testRenameGoal() {
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

        String newName = goal.getName() + " Renamed";
        goalService.renameGoal(goal.getId(), newName);

        verify(goalRepository).save(goal);
        assertEquals(newName, goal.getName());
    }

    @Test
    @DisplayName("Test if the goal is not renamed when it does not exist")
    void testRenameGoalDoesNotExist() {
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.empty());

        Long goalId = goal.getId();

        assertThrows(
                EntityNotFoundException.class, () -> goalService.renameGoal(goalId, "New Name"));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is not renamed when the new name already exists")
    void testRenameGoalAlreadyExists() {
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

        when(goalRepository.existsByName(goal.getName() + " Renamed")).thenReturn(true);

        Long goalId = goal.getId();
        String goalName = goal.getName();

        assertThrows(
                EntityExistsException.class,
                () -> goalService.renameGoal(goalId, goalName + " Renamed"));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the initial balance is updated successfully")
    void testUpdateInitialBalance() {
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

        BigDecimal newInitialBalance = goal.getInitialBalance().add(BigDecimal.valueOf(100.0));

        goalService.changeInitialBalance(goal.getId(), newInitialBalance);

        verify(goalRepository).save(goal);

        assertEquals(newInitialBalance, goal.getInitialBalance());
    }

    @Test
    @DisplayName("Test if the initial balance is not updated when the new balance is negative")
    void testUpdateInitialBalanceNegative() {
        Long goalId = goal.getId();
        BigDecimal newInitialBalance = BigDecimal.valueOf(-1.0);

        assertThrows(
                IllegalArgumentException.class,
                () -> goalService.changeInitialBalance(goalId, newInitialBalance));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the initial balance is not updated when the goal does not exist")
    void testUpdateInitialBalanceDoesNotExist() {
        Long goalId = goal.getId();
        BigDecimal newInitialBalance = BigDecimal.valueOf(100.0);

        assertThrows(
                EntityNotFoundException.class,
                () -> goalService.changeInitialBalance(goalId, newInitialBalance));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the target balance is updated successfully")
    void testUpdateTargetBalance() {
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

        BigDecimal newTargetBalance = goal.getTargetBalance().add(BigDecimal.valueOf(100.0));
        goalService.changeTargetBalance(goal.getId(), newTargetBalance);

        verify(goalRepository).save(goal);
        assertEquals(newTargetBalance, goal.getTargetBalance());
    }

    @Test
    @DisplayName("Test if the target balance is not updated when the new balance is negative")
    void testUpdateTargetBalanceNegative() {
        Long goalId = goal.getId();
        BigDecimal newTargetBalance = BigDecimal.valueOf(-1.0);

        assertThrows(
                IllegalArgumentException.class,
                () -> goalService.changeTargetBalance(goalId, newTargetBalance));

        verify(goalRepository, never()).save(any());
        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the target balance is not updated when the goal does not exist")
    void testUpdateTargetBalanceDoesNotExist() {
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.empty());

        Long goalId = goal.getId();
        BigDecimal newTargetBalance = BigDecimal.valueOf(100.0);

        assertThrows(
                EntityNotFoundException.class,
                () -> goalService.changeTargetBalance(goalId, newTargetBalance));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the target date is updated successfully")
    void testUpdateTargetDate() {
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

        LocalDateTime newTargetDate = goal.getTargetDate().plusDays(30);
        goalService.changeTargetDate(goal.getId(), newTargetDate);

        verify(goalRepository).save(goal);
        assertEquals(newTargetDate, goal.getTargetDate());
    }

    @Test
    @DisplayName("Test if the target date is not updated when the goal does not exist")
    void testUpdateTargetDateDoesNotExist() {
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.empty());

        Long goalId = goal.getId();
        LocalDateTime currentDateTime = LocalDateTime.now();

        assertThrows(
                EntityNotFoundException.class,
                () -> goalService.changeTargetDate(goalId, currentDateTime));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the target date is not updated when the new date is in the past")
    void testUpdateTargetDateInPast() {
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

        Long goalId = goal.getId();
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);

        assertThrows(
                IllegalArgumentException.class,
                () -> goalService.changeTargetDate(goalId, pastDate));

        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the motivation is updated successfully")
    void testUpdateMotivation() {
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));

        String newMotivation = goal.getMotivation() + " Updated";
        goalService.changeMotivation(goal.getId(), newMotivation);

        verify(goalRepository).save(goal);
        assertEquals(newMotivation, goal.getMotivation());
    }

    @Test
    @DisplayName("Test if the motivation is not updated when the goal does not exist")
    void testUpdateMotivationDoesNotExist() {
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.empty());

        Long goalId = goal.getId();

        assertThrows(
                EntityNotFoundException.class,
                () -> goalService.changeMotivation(goalId, "New Motivation"));

        verify(goalRepository, never()).save(any());
    }
}
