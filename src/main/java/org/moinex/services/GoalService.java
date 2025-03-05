/*
 * Filename: GoalService.java
 * Created on: December  6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import lombok.NoArgsConstructor;
import org.moinex.entities.Goal;
import org.moinex.entities.WalletType;
import org.moinex.repositories.GoalRepository;
import org.moinex.repositories.TransferRepository;
import org.moinex.repositories.WalletRepository;
import org.moinex.repositories.WalletTransactionRepository;
import org.moinex.repositories.WalletTypeRepository;
import org.moinex.util.Constants;
import org.moinex.util.LoggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing goals
 */
@Service
@NoArgsConstructor
public class GoalService
{
    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransferRepository transfersRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    private static final Logger logger = LoggerConfig.getLogger();

    /**
     * Validates the date and balances of a goal
     * @param initialBalance The initial balance of the goal
     * @param targetBalance The target balance of the goal
     * @param targetDateTime The target date of the goal
     * @throws RuntimeException If the target date is in the past, if the initial
     *   balance is negative, if the target balance is negative or zero or if the
     *   initial balance is greater than the target balance
     */
    @Transactional
    public void validateDateAndBalances(BigDecimal    initialBalance,
                                        BigDecimal    targetBalance,
                                        LocalDateTime targetDateTime)
    {
        if (targetDateTime.isBefore(LocalDateTime.now()))
        {
            throw new RuntimeException(
                "The target date of the goal cannot be in the past");
        }

        if (initialBalance.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new RuntimeException(
                "The initial balance of the goal cannot be negative");
        }

        if (targetBalance.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException(
                "The target balance of the goal must be greater than zero");
        }

        if (initialBalance.compareTo(targetBalance) > 0)
        {
            throw new RuntimeException("The initial balance of the goal cannot be "
                                       + "greater than the target balance");
        }
    }

    /**
     * Creates a new goal
     * @param name The name of the goal
     * @param initialBalance The initial balance of the goal
     * @param targetBalance The targetBalance balance of the goal
     * @param targetDate The targetBalance date of the goal
     * @param motivation The motivation for the goal
     * @return The id of the created goal
     * @throws RuntimeException If the name of the goal is empty, if a goal with the
     *     same name already exists or if the initial balance is negative
     */
    @Transactional
    public Long addGoal(String     name,
                        BigDecimal initialBalance,
                        BigDecimal targetBalance,
                        LocalDate  targetDate,
                        String     motivation)
    {
        // Remove leading and trailing whitespaces
        name = name.strip();

        if (name.isBlank())
        {
            throw new RuntimeException("The name of the goal cannot be empty");
        }

        if (goalRepository.existsByName(name))
        {
            throw new RuntimeException("A goal with name " + name + " already exists");
        }

        if (walletRepository.existsByName(name))
        {
            throw new RuntimeException("A wallet with name " + name +
                                       " already exists");
        }

        LocalDateTime targetDateTime = targetDate.atStartOfDay();

        validateDateAndBalances(initialBalance, targetBalance, targetDateTime);

        // All goals has the same wallet type
        WalletType walletType =
            walletTypeRepository.findByName(Constants.GOAL_DEFAULT_WALLET_TYPE_NAME)
                .orElseThrow(() -> new RuntimeException("Goal wallet type not found"));

        Goal goal = Goal.builder()
                        .name(name)
                        .initialBalance(initialBalance)
                        .balance(initialBalance)
                        .targetBalance(targetBalance)
                        .targetDate(targetDateTime)
                        .motivation(motivation)
                        .type(walletType)
                        .build();

        goalRepository.save(goal);

        logger.info("Goal " + name + " created with initial balance " + initialBalance);

        return goal.getId();
    }

    /**
     * Delete a goal
     * @param idGoal The id of the goal to be deleted
     * @throws RuntimeException If the goal does not exist or if the goal has
     *     transactions
     */
    @Transactional
    public void deleteGoal(Long idGoal)
    {
        Goal goal = goalRepository.findById(idGoal).orElseThrow(
            () -> new RuntimeException("Goal with id " + idGoal + " not found"));

        if (walletTransactionRepository.getTransactionCountByWallet(idGoal) > 0 ||
            transfersRepository.getTransferCountByWallet(idGoal) > 0)
        {
            throw new RuntimeException(
                "Goal wallet with id " + idGoal +
                " has transactions and cannot be deleted. Remove "
                + "the transactions first or archive the goal");
        }

        goalRepository.delete(goal);

        logger.info("Goal " + goal.getName() + " was permanently deleted");
    }

    /**
     * Updates a goal
     * @param goal The goal to be updated
     * @throws RuntimeException If the goal does not exist, if the name of the goal is
     *   empty, if a goal with the same name already exists, if the initial balance
     *   is negative, if the target balance is negative or zero, if the initial balance
     *   is greater than the target balance or if the target date is in the past
     */
    @Transactional
    public void updateGoal(Goal goal)
    {
        Goal oldGoal =
            goalRepository.findById(goal.getId())
                .orElseThrow(()
                                 -> new RuntimeException("Goal with id " +
                                                         goal.getId() + " not found"));

        // Remove leading and trailing whitespaces
        goal.setName(goal.getName().strip());

        if (goal.getName().isBlank())
        {
            throw new RuntimeException("The name of the goal cannot be empty");
        }

        if (!goal.getName().equals(oldGoal.getName()))
        {
            if (goalRepository.existsByName(goal.getName()))
            {
                throw new RuntimeException("A goal with name " + goal.getName() +
                                           " already exists");
            }
            else if (walletRepository.existsByName(goal.getName()))
            {
                throw new RuntimeException("A wallet with name " + goal.getName() +
                                           " already exists");
            }
        }

        validateDateAndBalances(goal.getInitialBalance(),
                                goal.getTargetBalance(),
                                goal.getTargetDate());

        oldGoal.setName(goal.getName());
        oldGoal.setInitialBalance(goal.getInitialBalance());
        oldGoal.setBalance(goal.getBalance());
        oldGoal.setTargetBalance(goal.getTargetBalance());
        oldGoal.setTargetDate(goal.getTargetDate());
        oldGoal.setMotivation(goal.getMotivation());
        oldGoal.setIsArchived(goal.getIsArchived());

        // Check if the goal was completed or reopened, and update it
        if (goal.isCompleted() != oldGoal.isCompleted())
        {
            if (goal.isCompleted())
            {
                completeGoal(goal.getId());
            }
            else
            {
                reopenGoal(goal.getId());
            }
        }

        goalRepository.save(goal);

        logger.info("Goal with id " + goal.getId() + " updated successfully");
    }

    /**
     * Archive a goal
     * @param idGoal The id of the goal to be archived
     * @throws RuntimeException If the goal does not exist
     * @note This method is used to archive a goal, which means that the goal
     * will not be deleted from the database, but it will not be used in the
     * application anymore
     */
    @Transactional
    public void archiveGoal(Long idGoal)
    {
        Goal goal = goalRepository.findById(idGoal).orElseThrow(
            ()
                -> new RuntimeException("Goal with id " + idGoal +
                                        " not found and cannot be archived"));

        goal.setIsArchived(true);

        goalRepository.save(goal);

        logger.info("Goal with id " + idGoal + " archived");
    }

    /**
     * Unarchive a goal
     * @param idGoal The id of the goal to be unarchived
     * @throws RuntimeException If the goal does not exist
     * @note This method is used to unarchive a goal, which means that the goal
     * will be used in the application again
     */
    @Transactional
    public void unarchiveGoal(Long idGoal)
    {
        Goal goal = goalRepository.findById(idGoal).orElseThrow(
            ()
                -> new RuntimeException("Goal with id " + idGoal +
                                        " not found and cannot be unarchived"));

        goal.setIsArchived(false);

        goalRepository.save(goal);

        logger.info("Goal with id " + idGoal + " unarchived");
    }

    /**
     * Complete a goal
     * @param idGoal The id of the goal to be completed
     * @throws RuntimeException If the goal does not exist or if the balance is less
     *    than the target balance
     */
    @Transactional
    public void completeGoal(Long idGoal)
    {
        Goal goal = goalRepository.findById(idGoal).orElseThrow(
            () -> new RuntimeException("Goal with id " + idGoal + " not found"));

        if (goal.getBalance().compareTo(goal.getTargetBalance()) < 0)
        {
            throw new RuntimeException("The goal has not been completed yet. The "
                                       + "balance is less than the target balance. "
                                       + "Deposit more money to complete the "
                                       + "goal or change the target balance");
        }

        goal.setCompletionDate(LocalDateTime.now());
        goal.setTargetBalance(goal.getBalance());

        goalRepository.save(goal);

        logger.info("Goal with id " + idGoal + " completed");
    }

    /**
     * Reopen a goal
     * @param idGoal The id of the goal to be reopened
     * @throws RuntimeException If the goal does not exist
     */
    @Transactional
    public void reopenGoal(Long idGoal)
    {
        Goal goal = goalRepository.findById(idGoal).orElseThrow(
            () -> new RuntimeException("Goal with id " + idGoal + " not found"));

        goal.setCompletionDate(null);
        goalRepository.save(goal);

        logger.info("Goal with id " + idGoal + " reopened");
    }

    /**
     * Rename a goal
     * @param idGoal The id of the goal to be renamed
     * @param newName The new name of the goal
     * @throws RuntimeException If the goal does not exist, if the new name is empty
     *    or if a goal with the same name already exists
     */
    @Transactional
    public void renameGoal(Long idGoal, String newName)
    {
        newName = newName.strip();

        if (newName.isBlank())
        {
            throw new RuntimeException("The name of the goal cannot be empty");
        }

        Goal goal = goalRepository.findById(idGoal).orElseThrow(
            () -> new RuntimeException("Goal with id " + idGoal + " not found"));

        if (goalRepository.existsByName(newName))
        {
            throw new RuntimeException("A goal with name " + newName +
                                       " already exists");
        }

        goal.setName(newName);
        goalRepository.save(goal);

        logger.info("Goal with id " + idGoal + " renamed to " + newName);
    }

    /**
     * Change the initial balance of a goal
     * @param idGoal The id of the goal to have the initial balance changed
     * @param newInitialBalance The new initial balance of the goal
     * @throws RuntimeException If the goal does not exist or if the new initial balance
     *    is negative
     */
    @Transactional
    public void changeInitialBalance(Long idGoal, BigDecimal newInitialBalance)
    {
        if (newInitialBalance.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new RuntimeException(
                "The initial balance of the goal cannot be negative");
        }

        Goal goal = goalRepository.findById(idGoal).orElseThrow(
            () -> new RuntimeException("Goal with id " + idGoal + " not found"));

        goal.setInitialBalance(newInitialBalance);
        goalRepository.save(goal);

        logger.info("Goal with id " + idGoal + " initial balance changed to " +
                    newInitialBalance);
    }

    /**
     * Change the target balance of a goal
     * @param idGoal The id of the goal to have the target balance changed
     * @param newTargetBalance The new target balance of the goal
     * @throws RuntimeException If the goal does not exist or if the new target balance
     *    is negative
     */
    @Transactional
    public void changeTargetBalance(Long idGoal, BigDecimal newTargetBalance)
    {
        if (newTargetBalance.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new RuntimeException(
                "The target balance of the goal cannot be negative");
        }

        Goal goal = goalRepository.findById(idGoal).orElseThrow(
            () -> new RuntimeException("Goal with id " + idGoal + " not found"));

        goal.setTargetBalance(newTargetBalance);
        goalRepository.save(goal);

        logger.info("Goal with id " + idGoal + " target balance changed to " +
                    newTargetBalance);
    }

    /**
     * Change the target date of a goal
     * @param idGoal The id of the goal to have the target date changed
     * @param newTargetDate The new target date of the goal
     * @throws RuntimeException If the goal does not exist
     */
    @Transactional
    public void changeTargetDate(Long idGoal, LocalDateTime newTargetDate)
    {
        Goal goal = goalRepository.findById(idGoal).orElseThrow(
            () -> new RuntimeException("Goal with id " + idGoal + " not found"));

        if (newTargetDate.isBefore(LocalDateTime.now()))
        {
            throw new RuntimeException(
                "The target date of the goal cannot be in the past");
        }

        goal.setTargetDate(newTargetDate);
        goalRepository.save(goal);

        logger.info("Goal with id " + idGoal + " target date changed to " +
                    newTargetDate);
    }

    /**
     * Change the motivation of a goal
     * @param idGoal The id of the goal to have the motivation changed
     * @param newMotivation The new motivation of the goal
     * @throws RuntimeException If the goal does not exist
     */
    @Transactional
    public void changeMotivation(Long idGoal, String newMotivation)
    {
        Goal goal = goalRepository.findById(idGoal).orElseThrow(
            () -> new RuntimeException("Goal with id " + idGoal + " not found"));

        goal.setMotivation(newMotivation);
        goalRepository.save(goal);

        logger.info("Goal with id " + idGoal + " motivation changed to " +
                    newMotivation);
    }

    /**
     * Get all goals
     */
    public List<Goal> getGoals()
    {
        return goalRepository.findAll();
    }

    /**
     * Get goal by id
     * @param idGoal The id of the goal to be retrieved
     * @return The goal with the given id
     * @throws RuntimeException If the goal does not exist
     */
    public Goal getGoalById(Long idGoal)
    {
        return goalRepository.findById(idGoal).orElseThrow(
            () -> new RuntimeException("Goal with id " + idGoal + " not found"));
    }
}
