/*
 * Filename: GoalService.java
 * Created on: December  6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.NoArgsConstructor;
import org.moinex.error.MoinexException;
import org.moinex.model.goal.Goal;
import org.moinex.model.wallettransaction.WalletType;
import org.moinex.repository.goal.GoalRepository;
import org.moinex.repository.wallettransaction.TransferRepository;
import org.moinex.repository.wallettransaction.WalletRepository;
import org.moinex.repository.wallettransaction.WalletTransactionRepository;
import org.moinex.repository.wallettransaction.WalletTypeRepository;
import org.moinex.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing goals
 */
@Service
@NoArgsConstructor
public class GoalService {
    private static final Logger logger = LoggerFactory.getLogger(GoalService.class);
    private GoalRepository goalRepository;
    private WalletRepository walletRepository;
    private TransferRepository transfersRepository;
    private WalletTransactionRepository walletTransactionRepository;
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    public GoalService(
            GoalRepository goalRepository,
            WalletRepository walletRepository,
            TransferRepository transfersRepository,
            WalletTransactionRepository walletTransactionRepository,
            WalletTypeRepository walletTypeRepository) {
        this.goalRepository = goalRepository;
        this.walletRepository = walletRepository;
        this.transfersRepository = transfersRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletTypeRepository = walletTypeRepository;
    }

    /**
     * Validates the date and balances of a goal
     *
     * @param initialBalance The initial balance of the goal
     * @param targetBalance  The target balance of the goal
     * @param targetDateTime The target date of the goal
     * @throws IllegalArgumentException If the target date is in the past
     * @throws IllegalArgumentException If the initial balance is negative
     * @throws IllegalArgumentException If the target balance is negative or zero
     * @throws IllegalArgumentException If the initial balance is greater than the
     *                                  target balance
     */
    @Transactional
    public void validateDateAndBalances(
            BigDecimal initialBalance, BigDecimal targetBalance, LocalDateTime targetDateTime) {
        if (targetDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("The target date of the goal cannot be in the past");
        }

        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "The initial balance of the goal cannot be negative");
        }

        if (targetBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "The target balance of the goal must be greater than zero");
        }

        if (initialBalance.compareTo(targetBalance) > 0) {
            throw new IllegalArgumentException(
                    "The initial balance of the goal cannot be "
                            + "greater than the target balance");
        }
    }

    /**
     * Creates a new goal
     *
     * @param name           The name of the goal
     * @param initialBalance The initial balance of the goal
     * @param targetBalance  The targetBalance balance of the goal
     * @param targetDate     The targetBalance date of the goal
     * @param motivation     The motivation for the goal
     * @return The id of the created goal
     * @throws IllegalArgumentException If the name of the goal is empty
     * @throws EntityExistsException    If a goal with the same name already exists
     * @throws EntityExistsException    If a wallet with the same name already exists
     * @throws IllegalArgumentException If the target date is in the past
     * @throws IllegalArgumentException If the initial balance is negative
     * @throws IllegalArgumentException If the target balance is negative or zero
     * @throws IllegalArgumentException If the initial balance is greater than the
     *                                  target balance
     * @throws EntityNotFoundException  If the goal wallet type is not found
     */
    @Transactional
    public Integer addGoal(
            String name,
            BigDecimal initialBalance,
            BigDecimal targetBalance,
            LocalDate targetDate,
            String motivation) {
        // Remove leading and trailing whitespaces
        name = name.strip();

        if (name.isBlank()) {
            throw new IllegalArgumentException("The name of the goal cannot be empty");
        }

        if (goalRepository.existsByName(name)) {
            throw new EntityExistsException("A goal with name " + name + " already exists");
        }

        if (walletRepository.existsByName(name)) {
            throw new EntityExistsException("A wallet with name " + name + " already exists");
        }

        LocalDateTime targetDateTime = targetDate.atStartOfDay();

        validateDateAndBalances(initialBalance, targetBalance, targetDateTime);

        // All goals have the same wallet type
        WalletType walletType =
                walletTypeRepository
                        .findByName(Constants.GOAL_DEFAULT_WALLET_TYPE_NAME)
                        .orElseThrow(
                                () -> new EntityNotFoundException("Goal wallet type not found"));

        Goal goal =
                Goal.builder()
                        .name(name)
                        .initialBalance(initialBalance)
                        .balance(initialBalance)
                        .targetBalance(targetBalance)
                        .targetDate(targetDateTime)
                        .motivation(motivation)
                        .type(walletType)
                        .build();

        goalRepository.save(goal);

        logger.info("Goal {} created with initial balance {}", name, initialBalance);

        return goal.getId();
    }

    /**
     * Delete a goal
     *
     * @param idGoal The id of the goal to be deleted
     * @throws EntityNotFoundException If the goal does not exist
     * @throws IllegalStateException   If the goal has transactions
     */
    @Transactional
    public void deleteGoal(Integer idGoal) {
        Goal goal =
                goalRepository
                        .findById(idGoal)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Goal with id %d not found", idGoal)));

        if (walletTransactionRepository.getTransactionCountByWallet(idGoal) > 0
                || transfersRepository.getTransferCountByWallet(idGoal) > 0) {
            throw new IllegalStateException(
                    "Goal wallet with id "
                            + idGoal
                            + " has transactions and cannot be deleted. Remove "
                            + "the transactions first or archive the goal");
        }

        goalRepository.delete(goal);

        logger.info("Goal {} was permanently deleted", goal.getName());
    }

    /**
     * Updates a goal
     *
     * @param goal The goal to be updated
     * @throws EntityNotFoundException  If the goal does not exist
     * @throws IllegalArgumentException If the name of the goal is empty
     * @throws EntityExistsException    If a goal with the same name already exists
     * @throws EntityExistsException    If a wallet with the same name already exists
     * @throws IllegalArgumentException If the target date is in the past
     * @throws IllegalArgumentException If the initial balance is negative
     * @throws IllegalArgumentException If the target balance is negative or zero
     * @throws IllegalArgumentException If the initial balance is greater than the
     *                                  target balance
     */
    @Transactional
    public void updateGoal(Goal goal) {
        Goal oldGoal =
                goalRepository
                        .findById(goal.getId())
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Goal with id %d not found",
                                                        goal.getId())));

        // Remove leading and trailing whitespaces
        goal.setName(goal.getName().strip());

        if (goal.getName().isBlank()) {
            throw new IllegalArgumentException("The name of the goal cannot be empty");
        }

        if (!goal.getName().equals(oldGoal.getName())) {
            if (goalRepository.existsByName(goal.getName())) {
                throw new EntityExistsException(
                        "A goal with name " + goal.getName() + " already exists");
            } else if (walletRepository.existsByName(goal.getName())) {
                throw new EntityExistsException(
                        "A wallet with name " + goal.getName() + " already exists");
            }
        }

        validateDateAndBalances(
                goal.getInitialBalance(), goal.getTargetBalance(), goal.getTargetDate());

        oldGoal.setName(goal.getName());
        oldGoal.setInitialBalance(goal.getInitialBalance());
        oldGoal.setBalance(goal.getBalance());
        oldGoal.setTargetBalance(goal.getTargetBalance());
        oldGoal.setTargetDate(goal.getTargetDate());
        oldGoal.setMotivation(goal.getMotivation());
        oldGoal.setArchived(goal.isArchived());

        // Check if the goal was completed or reopened, and update it
        if (goal.isCompleted() != oldGoal.isCompleted()) {
            if (goal.isCompleted()) {
                completeGoal(goal.getId());
            } else {
                reopenGoal(goal.getId());
            }
        }

        goalRepository.save(goal);

        logger.info("Goal with id {} updated successfully", goal.getId());
    }

    /**
     * Archive a goal
     *
     * @param idGoal The id of the goal to be archived
     * @throws EntityNotFoundException If the goal does not exist
     * @note This method is used to archive a goal, which means that the goal
     * will not be deleted from the database, but it will not be used in the
     * application anymore
     */
    @Transactional
    public void archiveGoal(Integer idGoal) {
        Goal goal =
                goalRepository
                        .findById(idGoal)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Goal with id %d not found and cannot be"
                                                                + " archived",
                                                        idGoal)));
        goal.setArchived(true);

        goalRepository.save(goal);

        logger.info("Goal with id {} archived", idGoal);
    }

    /**
     * Unarchive a goal
     *
     * @param idGoal The id of the goal to be unarchived
     * @throws EntityNotFoundException If the goal does not exist
     * @note This method is used to unarchive a goal, which means that the goal
     * will be used in the application again
     */
    @Transactional
    public void unarchiveGoal(Integer idGoal) {
        Goal goal =
                goalRepository
                        .findById(idGoal)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Goal with id %d not found and cannot be"
                                                                + " unarchived",
                                                        idGoal)));
        goal.setArchived(false);

        goalRepository.save(goal);

        logger.info("Goal with id {} unarchived", idGoal);
    }

    /**
     * Complete a goal
     *
     * @param idGoal The id of the goal to be completed
     * @throws EntityNotFoundException If the goal does not exist
     * @throws MoinexException.IncompleteGoalException If the goal has not been completed yet
     */
    @Transactional
    public void completeGoal(Integer idGoal) {
        Goal goal =
                goalRepository
                        .findById(idGoal)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Goal with id %d not found", idGoal)));

        if (goal.getBalance().compareTo(goal.getTargetBalance()) < 0) {
            throw new MoinexException.IncompleteGoalException(
                    "The goal has not been completed yet. The "
                            + "balance is less than the target balance. "
                            + "Deposit more money to complete the "
                            + "goal or change the target balance");
        }

        goal.setCompletionDate(LocalDateTime.now());
        goal.setTargetBalance(goal.getBalance());

        goalRepository.save(goal);

        logger.info("Goal with id {} completed", idGoal);
    }

    /**
     * Reopen a goal
     *
     * @param idGoal The id of the goal to be reopened
     * @throws EntityNotFoundException If the goal does not exist
     */
    @Transactional
    public void reopenGoal(Integer idGoal) {
        Goal goal =
                goalRepository
                        .findById(idGoal)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Goal with id %d not found", idGoal)));

        goal.setCompletionDate(null);
        goalRepository.save(goal);

        logger.info("Goal with id {} reopened", idGoal);
    }

    /**
     * Rename a goal
     *
     * @param idGoal  The id of the goal to be renamed
     * @param newName The new name of the goal
     * @throws EntityNotFoundException If the goal does not exist
     * @throws EntityExistsException   If a goal with the same name already exists
     * @throws IllegalStateException   If the name of the goal is empty
     */
    @Transactional
    public void renameGoal(Integer idGoal, String newName) {
        newName = newName.strip();

        if (newName.isBlank()) {
            throw new IllegalStateException("The name of the goal cannot be empty");
        }

        Goal goal =
                goalRepository
                        .findById(idGoal)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Goal with id %d not found", idGoal)));

        if (goalRepository.existsByName(newName)) {
            throw new EntityExistsException("A goal with name " + newName + " already exists");
        }

        goal.setName(newName);
        goalRepository.save(goal);

        logger.info("Goal with id {} renamed to {}", idGoal, newName);
    }

    /**
     * Change the initial balance of a goal
     *
     * @param idGoal            The id of the goal to have the initial balance changed
     * @param newInitialBalance The new initial balance of the goal
     * @throws IllegalArgumentException If the goal does not exist or if the new initial
     *                                  balance is negative
     * @throws EntityNotFoundException  If the goal does not exist
     */
    @Transactional
    public void changeInitialBalance(Integer idGoal, BigDecimal newInitialBalance) {
        if (newInitialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "The initial balance of the goal cannot be negative");
        }

        Goal goal =
                goalRepository
                        .findById(idGoal)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Goal with id %d not found", idGoal)));

        goal.setInitialBalance(newInitialBalance);
        goalRepository.save(goal);

        logger.info("Goal with id {} initial balance changed to {}", idGoal, newInitialBalance);
    }

    /**
     * Change the target balance of a goal
     *
     * @param idGoal           The id of the goal to have the target balance changed
     * @param newTargetBalance The new target balance of the goal
     * @throws IllegalArgumentException if the new target balance is negative
     * @throws EntityNotFoundException  If the goal does not exist
     */
    @Transactional
    public void changeTargetBalance(Integer idGoal, BigDecimal newTargetBalance) {
        if (newTargetBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("The target balance of the goal cannot be negative");
        }

        Goal goal =
                goalRepository
                        .findById(idGoal)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Goal with id %d not found", idGoal)));

        goal.setTargetBalance(newTargetBalance);
        goalRepository.save(goal);

        logger.info("Goal with id {} target balance changed to {}", idGoal, newTargetBalance);
    }

    /**
     * Change the target date of a goal
     *
     * @param idGoal        The id of the goal to have the target date changed
     * @param newTargetDate The new target date of the goal
     * @throws EntityNotFoundException  If the goal does not exist
     * @throws IllegalArgumentException If the target date is in the past
     */
    @Transactional
    public void changeTargetDate(Integer idGoal, LocalDateTime newTargetDate) {
        Goal goal =
                goalRepository
                        .findById(idGoal)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Goal with id %d not found", idGoal)));

        if (newTargetDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("The target date of the goal cannot be in the past");
        }

        goal.setTargetDate(newTargetDate);
        goalRepository.save(goal);

        logger.info("Goal with id {} target date changed to {}", idGoal, newTargetDate);
    }

    /**
     * Change the motivation of a goal
     *
     * @param idGoal        The id of the goal to have the motivation changed
     * @param newMotivation The new motivation of the goal
     * @throws EntityNotFoundException If the goal does not exist
     */
    @Transactional
    public void changeMotivation(Integer idGoal, String newMotivation) {
        Goal goal =
                goalRepository
                        .findById(idGoal)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Goal with id %d not found", idGoal)));

        goal.setMotivation(newMotivation);
        goalRepository.save(goal);

        logger.info("Goal with id {} motivation changed to {}", idGoal, newMotivation);
    }

    /**
     * Get all goals
     */
    public List<Goal> getGoals() {
        return goalRepository.findAll();
    }

    /**
     * Get goal by id
     *
     * @param idGoal The id of the goal to be retrieved
     * @return The goal with the given id
     * @throws EntityNotFoundException If the goal does not exist
     */
    public Goal getGoalById(Integer idGoal) {
        return goalRepository
                .findById(idGoal)
                .orElseThrow(
                        () -> new EntityNotFoundException("Goal with id " + idGoal + " not found"));
    }
}
