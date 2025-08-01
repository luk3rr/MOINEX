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
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletType;
import org.moinex.repository.goal.GoalRepository;
import org.moinex.repository.wallettransaction.TransferRepository;
import org.moinex.repository.wallettransaction.WalletRepository;
import org.moinex.repository.wallettransaction.WalletTransactionRepository;
import org.moinex.repository.wallettransaction.WalletTypeRepository;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.enums.GoalFundingStrategy;
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
    private WalletService walletService;

    @Autowired
    public GoalService(
            GoalRepository goalRepository,
            WalletRepository walletRepository,
            TransferRepository transfersRepository,
            WalletTransactionRepository walletTransactionRepository,
            WalletTypeRepository walletTypeRepository,
            WalletService walletService) {
        this.goalRepository = goalRepository;
        this.walletRepository = walletRepository;
        this.transfersRepository = transfersRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletTypeRepository = walletTypeRepository;
        this.walletService = walletService;
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
     * Creates a new goal without a master wallet
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
        return addGoal(name, initialBalance, targetBalance, targetDate, motivation, null, null);
    }

    /**
     * Creates a new goal
     *
     * @param name           The name of the goal
     * @param initialBalance The initial balance of the goal
     * @param targetBalance  The targetBalance balance of the goal
     * @param targetDate     The targetBalance date of the goal
     * @param motivation     The motivation for the goal
     * @param masterWallet   The master wallet of the goal, can be null if the goal does not
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
            String motivation,
            Wallet masterWallet,
            GoalFundingStrategy strategy) {
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

        if (masterWallet != null && !masterWallet.isMaster()) {
            throw new IllegalArgumentException(
                    "The master wallet must be a master wallet, not a virtual wallet");
        }

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
                        .masterWallet(masterWallet)
                        .build();

        handleFundingStrategy(goal, strategy, goal.getBalance());

        goalRepository.save(goal);

        logger.info("Goal {} created with initial balance {}", name, initialBalance);

        return goal.getId();
    }

    /**
     * Handles the funding strategy for a goal when goal is an virtual wallet and a master wallet is provided.
     *
     * @param goal     The goal to be funded
     * @param strategy The funding strategy to be applied
     * @throws IllegalArgumentException                       If the funding strategy is null when a master wallet is provided
     * @throws MoinexException.InsufficientResourcesException If the master wallet does not have enough unallocated balance
     */
    private void handleFundingStrategy(Goal goal, GoalFundingStrategy strategy, BigDecimal value) {
        if (!goal.isVirtual() || value.compareTo(BigDecimal.ZERO) <= 0) return;

        if (strategy == null) {
            throw new IllegalArgumentException(
                    "A funding strategy is required when a master wallet" + " are provided.");
        }

        Wallet masterWallet = goal.getMasterWallet();

        switch (strategy) {
            case NEW_DEPOSIT:
                masterWallet.setBalance(masterWallet.getBalance().add(value));
                walletRepository.save(masterWallet);
                logger.info(
                        "Current master wallet '{}' balance updated to {} after funding goal"
                                + " '{}'",
                        masterWallet.getName(),
                        masterWallet.getBalance(),
                        goal.getName());
                break;

            case ALLOCATE_FROM_EXISTING:
                BigDecimal allocatedBalance =
                        walletRepository.getAllocatedBalanceByMasterWallet(masterWallet.getId());
                BigDecimal freeBalance = masterWallet.getBalance().subtract(allocatedBalance);

                if (freeBalance.compareTo(value) < 0) {
                    throw new MoinexException.InsufficientResourcesException(
                            "Master wallet has insufficient unallocated balance. Free balance: "
                                    + UIUtils.formatCurrency(freeBalance));
                }

                logger.info(
                        "Allocating {} from master wallet '{}' to goal '{}'",
                        value,
                        masterWallet.getName(),
                        goal.getName());
                break;
        }
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

        if (goal.isMaster()) {
            walletService.removeAllVirtualWalletsFromMasterWallet(goal.getId());
        }

        goalRepository.delete(goal);

        logger.info("Goal {} was permanently deleted", goal.getName());
    }

    /**
     * Updates a goal
     *
     * @param goalUpdated The goal to be updated
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
    public void updateGoal(Goal goalUpdated) {
        Goal oldGoal =
                goalRepository
                        .findById(goalUpdated.getId())
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Goal with id %d not found",
                                                        goalUpdated.getId())));

        // Remove leading and trailing whitespaces
        goalUpdated.setName(goalUpdated.getName().strip());

        if (goalUpdated.getName().isBlank()) {
            throw new IllegalArgumentException("The name of the goal cannot be empty");
        }

        if (!goalUpdated.getName().equals(oldGoal.getName())) {
            if (goalRepository.existsByName(goalUpdated.getName())) {
                throw new EntityExistsException(
                        "A goal with name " + goalUpdated.getName() + " already exists");
            } else if (walletRepository.existsByName(goalUpdated.getName())) {
                throw new EntityExistsException(
                        "A wallet with name " + goalUpdated.getName() + " already exists");
            }
        }

        validateDateAndBalances(
                goalUpdated.getInitialBalance(),
                goalUpdated.getTargetBalance(),
                goalUpdated.getTargetDate());

        oldGoal.setName(goalUpdated.getName());

        if (oldGoal.isArchived() != goalUpdated.isArchived()) {
            if (goalUpdated.isArchived()) {
                archiveGoal(oldGoal.getId());
            } else {
                unarchiveGoal(oldGoal.getId());
            }
        }

        updateMasterWallet(oldGoal, goalUpdated);

        updateBalance(oldGoal, goalUpdated.getBalance());

        oldGoal.setTargetBalance(goalUpdated.getTargetBalance());
        oldGoal.setTargetDate(goalUpdated.getTargetDate());
        oldGoal.setMotivation(goalUpdated.getMotivation());

        // Check if the goal was completed or reopened, and update it
        if (goalUpdated.isCompleted() != oldGoal.isCompleted()) {
            if (goalUpdated.isCompleted()) {
                completeGoal(goalUpdated.getId());
            } else {
                reopenGoal(goalUpdated.getId());
            }
        }

        goalRepository.save(oldGoal);

        logger.info("Goal with id {} updated successfully", oldGoal.getId());
    }

    private void updateMasterWallet(Goal oldGoal, Goal newGoal) {
        Wallet newMasterWallet = newGoal.getMasterWallet();

        if (newMasterWallet != null && !newMasterWallet.isMaster()) {
            throw new IllegalArgumentException(
                    "The master wallet must be a master wallet, not a virtual wallet");
        }

        Wallet currentMasterWallet = oldGoal.getMasterWallet();

        if (newMasterWallet == null && currentMasterWallet == null) {
            logger.info("Goal with id {} has no master wallet to update", oldGoal.getId());
            return;
        }

        if (currentMasterWallet != null && currentMasterWallet.equals(newMasterWallet)) {
            BigDecimal balanceDifference = newGoal.getBalance().subtract(oldGoal.getBalance());

            if (balanceDifference.compareTo(BigDecimal.ZERO) != 0) {
                currentMasterWallet.setBalance(
                        currentMasterWallet.getBalance().add(balanceDifference));
                walletRepository.save(currentMasterWallet);
                logger.info(
                        "Balance of master wallet '{}' {} by {} due to goal update",
                        currentMasterWallet.getName(),
                        (balanceDifference.compareTo(BigDecimal.ZERO) > 0)
                                ? "increased"
                                : "decreased",
                        balanceDifference.abs());
            }
            return;
        }

        if (currentMasterWallet != null) {
            currentMasterWallet.setBalance(
                    currentMasterWallet.getBalance().subtract(oldGoal.getBalance()));
            walletRepository.save(currentMasterWallet);
            logger.info(
                    "Balance of {} for goal '{}' was removed from old master wallet '{}'",
                    oldGoal.getBalance(),
                    oldGoal.getName(),
                    currentMasterWallet.getName());
        }

        oldGoal.setMasterWallet(newMasterWallet);

        if (newMasterWallet != null) {
            BigDecimal depositValue =
                    newMasterWallet.equals(currentMasterWallet)
                            ? newGoal.getBalance().subtract(oldGoal.getBalance())
                            : newGoal.getBalance();

            handleFundingStrategy(oldGoal, GoalFundingStrategy.NEW_DEPOSIT, depositValue);

            logger.info(
                    "Goal with id {} master wallet updated to {}",
                    oldGoal.getId(),
                    newMasterWallet.getName());
        } else {
            logger.info("Goal with id {} master wallet removed", oldGoal.getId());
        }
    }

    private void updateBalance(Goal goal, BigDecimal newBalance) {
        // increment or decrement the initial balance based on the new balance
        BigDecimal balanceDifference = newBalance.subtract(goal.getBalance());

        if (balanceDifference.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal updatedInitial = goal.getInitialBalance().add(balanceDifference);
            goal.setInitialBalance(updatedInitial.max(BigDecimal.ZERO));

            logger.info(
                    "Goal with id {} initial balance updated to {}",
                    goal.getId(),
                    goal.getInitialBalance());
        }

        goal.setBalance(newBalance);

        goalRepository.save(goal);
        logger.info("Goal with id {} balance updated to {}", goal.getId(), newBalance);
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

        if (goal.isMaster()) {
            walletService.removeAllVirtualWalletsFromMasterWallet(goal.getId());
        } else if (goal.isVirtual()) {
            // If the goal is a virtual wallet, leave its balance in the master wallet
            goal.setMasterWallet(null);
        }

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
     * @throws EntityNotFoundException                 If the goal does not exist
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

        // If the goal is a virtual wallet, unlink it from its master
        // This frees up the allocated balance in the master wallet for other transactions
        goal.setMasterWallet(null);

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
