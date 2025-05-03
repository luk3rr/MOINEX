/*
 * Filename: GoalServiceTest.java
 * Created on: December  7, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class GoalServiceTest
{
    @Mock
    private WalletRepository m_walletRepository;

    @Mock
    private WalletTypeRepository m_walletTypeRepository;

    @Mock
    private GoalRepository m_goalRepository;

    @Mock
    private GoalService m_selfRef;

    @InjectMocks
    private GoalService m_goalService;

    private Goal       m_goal;
    private WalletType m_walletType;

    private Goal createGoal(Long          id,
                            String        name,
                            BigDecimal    initialBalance,
                            BigDecimal    targetBalance,
                            LocalDateTime targetDate,
                            String        motivation)
    {
        Goal goal = new Goal(id,
                             name,
                             initialBalance,
                             targetBalance,
                             targetDate,
                             motivation,
                             m_walletType);
        return goal;
    }

    private WalletType createWalletType(Long id, String name)
    {
        WalletType walletType = new WalletType(id, name);
        return walletType;
    }

    @BeforeAll
    static void setUp()
    {
        MockitoAnnotations.openMocks(WalletServiceTest.class);
    }

    @BeforeEach
    void beforeEach()
    {
        m_goal = createGoal(1L,
                            "Goal1",
                            BigDecimal.valueOf(100.0),
                            BigDecimal.valueOf(200.0),
                            LocalDateTime.now().plusDays(30),
                            "Motivation1");

        m_walletType = createWalletType(1L, Constants.GOAL_DEFAULT_WALLET_TYPE_NAME);
    }

    @Test
    @DisplayName("Test if the goal is created successfully")
    void testCreateGoal()
    {
        when(m_goalRepository.existsByName(m_goal.getName())).thenReturn(false);

        when(m_walletTypeRepository.findByName(Constants.GOAL_DEFAULT_WALLET_TYPE_NAME))
            .thenReturn(Optional.of(m_walletType));

        m_goalService.addGoal(m_goal.getName(),
                              m_goal.getInitialBalance(),
                              m_goal.getTargetBalance(),
                              m_goal.getTargetDate().toLocalDate(),
                              m_goal.getMotivation());

        // Capture the wallet object that was saved and check if the values are correct
        ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);

        verify(m_goalRepository).save(goalCaptor.capture());

        assertEquals(m_goal.getName(), goalCaptor.getValue().getName());

        assertEquals(m_goal.getInitialBalance(),
                     goalCaptor.getValue().getInitialBalance());

        assertEquals(m_goal.getTargetBalance(),
                     goalCaptor.getValue().getTargetBalance());
    }

    @Test
    @DisplayName("Test if the goal is not created when the name already exists")
    void testCreateGoalAlreadyExists()
    {
        when(m_goalRepository.existsByName(m_goal.getName())).thenReturn(true);

        assertThrows(EntityExistsException.class, () -> {
            m_goalService.addGoal(m_goal.getName(),
                                  m_goal.getInitialBalance(),
                                  m_goal.getTargetBalance(),
                                  m_goal.getTargetDate().toLocalDate(),
                                  m_goal.getMotivation());
        });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is not created when the initial balance is negative")
    void testCreateGoalNegativeInitialBalance()
    {
        when(m_goalRepository.existsByName(m_goal.getName())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            m_goalService.addGoal(m_goal.getName(),
                                  BigDecimal.valueOf(-1.0),
                                  m_goal.getTargetBalance(),
                                  m_goal.getTargetDate().toLocalDate(),
                                  m_goal.getMotivation());
        });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is not created when the target balance is negative")
    void testCreateGoalNegativeTargetBalance()
    {
        when(m_goalRepository.existsByName(m_goal.getName())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            m_goalService.addGoal(m_goal.getName(),
                                  m_goal.getInitialBalance(),
                                  BigDecimal.valueOf(-1.0),
                                  m_goal.getTargetDate().toLocalDate(),
                                  m_goal.getMotivation());
        });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is not created when target balance zero")
    void testCreateGoalZeroBalance()
    {
        when(m_goalRepository.existsByName(m_goal.getName())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            m_goalService.addGoal(m_goal.getName(),
                                  BigDecimal.valueOf(0.0),
                                  BigDecimal.valueOf(0.0),
                                  m_goal.getTargetDate().toLocalDate(),
                                  m_goal.getMotivation());
        });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is not created when the target date is in the past")
    void testCreateGoalTargetDateInPast()
    {
        when(m_goalRepository.existsByName(m_goal.getName())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            m_goalService.addGoal(m_goal.getName(),
                                  m_goal.getInitialBalance(),
                                  m_goal.getTargetBalance(),
                                  LocalDate.now().minusDays(1),
                                  m_goal.getMotivation());
        });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is not created when the target balance is less "
                 + "than the initial balance")
    void
    testCreateGoalTargetBalanceLessThanInitialBalance()
    {
        when(m_goalRepository.existsByName(m_goal.getName())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            m_goalService.addGoal(
                m_goal.getName(),
                m_goal.getInitialBalance(),
                m_goal.getInitialBalance().subtract(BigDecimal.valueOf(1.0)),
                m_goal.getTargetDate().toLocalDate(),
                m_goal.getMotivation());
        });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is not created when the wallet type does not exist")
    void testCreateGoalWalletTypeDoesNotExist()
    {
        when(m_goalRepository.existsByName(m_goal.getName())).thenReturn(false);

        when(m_walletTypeRepository.findByName(Constants.GOAL_DEFAULT_WALLET_TYPE_NAME))
            .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            m_goalService.addGoal(m_goal.getName(),
                                  m_goal.getInitialBalance(),
                                  m_goal.getTargetBalance(),
                                  m_goal.getTargetDate().toLocalDate(),
                                  m_goal.getMotivation());
        });

        verify(m_goalRepository, never()).save(any());
    }

    // TODO: Delete goal tests

    @Test
    @DisplayName("Test if the goal is archived successfully")
    void testArchiveGoal()
    {
        when(m_goalRepository.findById(m_goal.getId())).thenReturn(Optional.of(m_goal));

        m_goalService.archiveGoal(m_goal.getId());

        verify(m_goalRepository).save(m_goal);
        assertTrue(m_goal.isArchived());
    }

    @Test
    @DisplayName("Test if the goal is unarchived successfully")
    void testUnarchiveGoal()
    {
        m_goal.setArchived(true);

        when(m_goalRepository.findById(m_goal.getId())).thenReturn(Optional.of(m_goal));

        m_goalService.unarchiveGoal(m_goal.getId());

        verify(m_goalRepository).save(m_goal);
        assertTrue(!m_goal.isArchived());
    }

    @Test
    @DisplayName("Test if the goal is not archived when it does not exist")
    void testArchiveGoalDoesNotExist()
    {
        when(m_goalRepository.findById(m_goal.getId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                     () -> { m_goalService.archiveGoal(m_goal.getId()); });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is not unarchived when it does not exist")
    void testUnarchiveGoalDoesNotExist()
    {
        when(m_goalRepository.findById(m_goal.getId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                     () -> { m_goalService.unarchiveGoal(m_goal.getId()); });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is renamed successfully")
    void testRenameGoal()
    {
        when(m_goalRepository.findById(m_goal.getId())).thenReturn(Optional.of(m_goal));

        String newName = m_goal.getName() + " Renamed";
        m_goalService.renameGoal(m_goal.getId(), newName);

        verify(m_goalRepository).save(m_goal);
        assertEquals(newName, m_goal.getName());
    }

    @Test
    @DisplayName("Test if the goal is not renamed when it does not exist")
    void testRenameGoalDoesNotExist()
    {
        when(m_goalRepository.findById(m_goal.getId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                     () -> { m_goalService.renameGoal(m_goal.getId(), "New Name"); });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the goal is not renamed when the new name already exists")
    void testRenameGoalAlreadyExists()
    {
        when(m_goalRepository.findById(m_goal.getId())).thenReturn(Optional.of(m_goal));

        when(m_goalRepository.existsByName(m_goal.getName() + " Renamed"))
            .thenReturn(true);

        assertThrows(EntityExistsException.class, () -> {
            m_goalService.renameGoal(m_goal.getId(), m_goal.getName() + " Renamed");
        });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the initial balance is updated successfully")
    void testUpdateInitialBalance()
    {
        when(m_goalRepository.findById(m_goal.getId())).thenReturn(Optional.of(m_goal));

        BigDecimal newInitialBalance =
            m_goal.getInitialBalance().add(BigDecimal.valueOf(100.0));

        m_goalService.changeInitialBalance(m_goal.getId(), newInitialBalance);

        verify(m_goalRepository).save(m_goal);

        assertEquals(newInitialBalance, m_goal.getInitialBalance());
    }

    @Test
    @DisplayName(
        "Test if the initial balance is not updated when the new balance is negative")
    void
    testUpdateInitialBalanceNegative()
    {
        assertThrows(IllegalArgumentException.class, () -> {
            m_goalService.changeInitialBalance(m_goal.getId(),
                                               BigDecimal.valueOf(-1.0));
        });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName(
        "Test if the initial balance is not updated when the goal does not exist")
    void
    testUpdateInitialBalanceDoesNotExist()
    {
        assertThrows(EntityNotFoundException.class, () -> {
            m_goalService.changeInitialBalance(m_goal.getId(),
                                               BigDecimal.valueOf(100.0));
        });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the target balance is updated successfully")
    void testUpdateTargetBalance()
    {
        when(m_goalRepository.findById(m_goal.getId())).thenReturn(Optional.of(m_goal));

        BigDecimal newTargetBalance =
            m_goal.getTargetBalance().add(BigDecimal.valueOf(100.0));
        m_goalService.changeTargetBalance(m_goal.getId(), newTargetBalance);

        verify(m_goalRepository).save(m_goal);
        assertEquals(newTargetBalance, m_goal.getTargetBalance());
    }

    @Test
    @DisplayName(
        "Test if the target balance is not updated when the new balance is negative")
    void
    testUpdateTargetBalanceNegative()
    {
        assertThrows(IllegalArgumentException.class, () -> {
            m_goalService.changeTargetBalance(m_goal.getId(), BigDecimal.valueOf(-1.0));
        });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName(
        "Test if the target balance is not updated when the goal does not exist")
    void
    testUpdateTargetBalanceDoesNotExist()
    {
        when(m_goalRepository.findById(m_goal.getId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            m_goalService.changeTargetBalance(m_goal.getId(),
                                              BigDecimal.valueOf(100.0));
        });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the target date is updated successfully")
    void testUpdateTargetDate()
    {
        when(m_goalRepository.findById(m_goal.getId())).thenReturn(Optional.of(m_goal));

        LocalDateTime newTargetDate = m_goal.getTargetDate().plusDays(30);
        m_goalService.changeTargetDate(m_goal.getId(), newTargetDate);

        verify(m_goalRepository).save(m_goal);
        assertEquals(newTargetDate, m_goal.getTargetDate());
    }

    @Test
    @DisplayName("Test if the target date is not updated when the goal does not exist")
    void testUpdateTargetDateDoesNotExist()
    {
        when(m_goalRepository.findById(m_goal.getId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            m_goalService.changeTargetDate(m_goal.getId(), LocalDateTime.now());
        });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName(
        "Test if the target date is not updated when the new date is in the past")
    void
    testUpdateTargetDateInPast()
    {
        when(m_goalRepository.findById(m_goal.getId())).thenReturn(Optional.of(m_goal));

        assertThrows(IllegalArgumentException.class, () -> {
            m_goalService.changeTargetDate(m_goal.getId(),
                                           LocalDateTime.now().minusDays(1));
        });

        verify(m_goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the motivation is updated successfully")
    void testUpdateMotivation()
    {
        when(m_goalRepository.findById(m_goal.getId())).thenReturn(Optional.of(m_goal));

        String newMotivation = m_goal.getMotivation() + " Updated";
        m_goalService.changeMotivation(m_goal.getId(), newMotivation);

        verify(m_goalRepository).save(m_goal);
        assertEquals(newMotivation, m_goal.getMotivation());
    }

    @Test
    @DisplayName("Test if the motivation is not updated when the goal does not exist")
    void testUpdateMotivationDoesNotExist()
    {
        when(m_goalRepository.findById(m_goal.getId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            m_goalService.changeMotivation(m_goal.getId(), "New Motivation");
        });

        verify(m_goalRepository, never()).save(any());
    }
}
