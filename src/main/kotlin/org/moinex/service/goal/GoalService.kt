package org.moinex.service.goal

import org.moinex.common.extension.findByIdOrThrow
import org.moinex.common.extension.isNotZero
import org.moinex.common.extension.isZero
import org.moinex.model.enums.GoalFundingStrategy
import org.moinex.model.goal.Goal
import org.moinex.repository.goal.GoalRepository
import org.moinex.service.wallet.WalletService
import org.moinex.util.Constants
import org.moinex.util.UIUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class GoalService(
    private val goalRepository: GoalRepository,
    private val walletService: WalletService,
) {
    private val logger = LoggerFactory.getLogger(GoalService::class.java)

    @Transactional
    fun createGoal(
        goal: Goal,
        strategy: GoalFundingStrategy? = null,
    ): Int {
        check(!goalRepository.existsByName(goal.name)) {
            "A goal with name '${goal.name} already exists"
        }

        check(!walletService.existsByName(goal.name)) {
            "A wallet with name '${goal.name}' already exists"
        }

        check(walletService.existsWalletTypeByName(Constants.GOAL_DEFAULT_WALLET_TYPE_NAME)) {
            "${Constants.GOAL_DEFAULT_WALLET_TYPE_NAME} wallet type not found"
        }

        goal.masterWallet?.let {
            check(it.isMaster()) {
                "The master wallet must be a master wallet, not a virtual wallet"
            }
        }

        strategy?.let { handleFundingStrategy(goal, it, goal.balance) }

        val newGoal = goalRepository.save(goal)

        logger.info("$newGoal was created")

        return newGoal.id!!
    }

    @Transactional
    fun updateGoal(updatedGoal: Goal) {
        val goalFromDatabase = goalRepository.findByIdOrThrow(updatedGoal.id!!)

        check(!goalRepository.existsByNameAndIdNot(updatedGoal.name, updatedGoal.id!!)) {
            "A goal with name '${updatedGoal.name}' already exists"
        }

        check(!walletService.existsByNameAndIdNot(updatedGoal.name, updatedGoal.id!!)) {
            "A wallet with name '${updatedGoal.name}' already exists"
        }

        if (goalFromDatabase.isArchived != updatedGoal.isArchived) {
            when (updatedGoal.isArchived) {
                true -> archiveGoal(goalFromDatabase.id!!)
                false -> unarchiveGoal(goalFromDatabase.id!!)
            }
        }

        updateMasterWallet(goalFromDatabase, updatedGoal)
        updateBalance(goalFromDatabase, updatedGoal.balance)

        goalFromDatabase.apply {
            name = updatedGoal.name
            targetBalance = updatedGoal.targetBalance
            targetDate = updatedGoal.targetDate
            motivation = updatedGoal.motivation
        }

        if (goalFromDatabase.isCompleted() != updatedGoal.isCompleted()) {
            when (updatedGoal.isCompleted()) {
                true -> completeGoal(goalFromDatabase.id!!)
                false -> reopenGoal(goalFromDatabase.id!!)
            }
        }

        logger.info("$goalFromDatabase updated successfully")
    }

    @Transactional
    fun deleteGoal(id: Int) {
        val goalFromDatabase = goalRepository.findByIdOrThrow(id)

        check(walletService.getWalletTransactionAndTransferCountByWallet(id) == 0) {
            "Goal wallet with id $id has transactions and cannot be deleted. " +
                "Remove the transactions first or archive the goal"
        }

        if (goalFromDatabase.isMaster()) {
            walletService.removeAllVirtualWalletsFromMasterWallet(goalFromDatabase.id!!)
        }

        goalRepository.delete(goalFromDatabase)

        logger.info("$goalFromDatabase deleted successfully")
    }

    @Transactional
    fun archiveGoal(id: Int) {
        val goalFromDatabase = goalRepository.findByIdOrThrow(id)

        goalFromDatabase.isArchived = true

        when {
            goalFromDatabase.isMaster() -> walletService.removeAllVirtualWalletsFromMasterWallet(goalFromDatabase.id!!)
            goalFromDatabase.isVirtual() -> goalFromDatabase.masterWallet = null
        }

        logger.info("$goalFromDatabase archived successfully")
    }

    @Transactional
    fun unarchiveGoal(id: Int) {
        val goalFromDatabase = goalRepository.findByIdOrThrow(id)

        goalFromDatabase.isArchived = false

        logger.info("$goalFromDatabase unarchived successfully")
    }

    @Transactional
    fun completeGoal(id: Int) {
        val goalFromDatabase = goalRepository.findByIdOrThrow(id)

        check(goalFromDatabase.balance >= goalFromDatabase.targetBalance) {
            "The goal has not been completed yet. The balance is less than the target balance. " +
                "Deposit more money to complete the goal or change the target balance"
        }

        goalFromDatabase.apply {
            completionDate = LocalDate.now()
            targetBalance = balance
            masterWallet = null
        }

        logger.info("$goalFromDatabase completed successfully")
    }

    @Transactional
    fun reopenGoal(id: Int) {
        val goalFromDatabase = goalRepository.findByIdOrThrow(id)

        goalFromDatabase.completionDate = null

        logger.info("$goalFromDatabase reopened successfully")
    }

    fun getAllGoals(): List<Goal> = goalRepository.findAll()

    fun getGoalById(id: Int): Goal = goalRepository.findByIdOrThrow(id)

    private fun handleFundingStrategy(
        goal: Goal,
        strategy: GoalFundingStrategy,
        value: BigDecimal,
    ) {
        if (goal.isMaster() || value.isZero()) return

        val masterWallet = walletService.getWalletById(goal.masterWallet!!.id!!)

        when (strategy) {
            GoalFundingStrategy.NEW_DEPOSIT -> {
                masterWallet.balance = masterWallet.balance.add(value)

                logger.info(
                    "Current {} balance updated to {} after funding {}",
                    masterWallet,
                    masterWallet.balance,
                    goal,
                )
            }
            GoalFundingStrategy.ALLOCATE_FROM_EXISTING -> {
                val freeBalance = walletService.getUnallocatedBalance(masterWallet)

                check(freeBalance >= value) {
                    "Master wallet has insufficient unallocated balance. Free balance: " +
                        UIUtils.formatCurrency(freeBalance)
                }

                logger.info(
                    "Allocating {} from master {} to {}",
                    UIUtils.formatCurrency(value),
                    masterWallet,
                    goal,
                )
            }
        }
    }

    private fun updateMasterWallet(
        oldGoal: Goal,
        newGoal: Goal,
    ) {
        val newMasterWallet = newGoal.masterWallet

        newMasterWallet?.let {
            check(newMasterWallet.isMaster()) {
                "The master wallet must be a master wallet, not a virtual wallet"
            }
        }

        val currentMasterWallet = oldGoal.masterWallet

        if (newMasterWallet == null && currentMasterWallet == null) {
            logger.info("$oldGoal has no master wallet to update")
            return
        }

        currentMasterWallet?.let {
            if (currentMasterWallet == newMasterWallet) {
                val balanceDifference = newGoal.balance.subtract(oldGoal.balance)

                if (balanceDifference.isNotZero()) {
                    currentMasterWallet.balance += balanceDifference

                    logger.info(
                        "Balance of master {} {} by {} due to goal update",
                        currentMasterWallet,
                        if (balanceDifference > BigDecimal.ZERO) "increased" else "decreased",
                        balanceDifference.abs(),
                    )
                }
                return
            }

            currentMasterWallet.balance -= oldGoal.balance

            logger.info(
                "Balance of {} for {} was removed from old master {}",
                oldGoal.balance,
                oldGoal,
                currentMasterWallet,
            )
        }

        oldGoal.masterWallet = newMasterWallet

        newMasterWallet?.let {
            val depositValue =
                when {
                    newMasterWallet == currentMasterWallet -> newGoal.balance.subtract(oldGoal.balance)
                    else -> newGoal.balance
                }

            handleFundingStrategy(oldGoal, GoalFundingStrategy.NEW_DEPOSIT, depositValue)

            logger.info(
                "{} master wallet updated to {}",
                oldGoal,
                newMasterWallet,
            )
        }
    }

    private fun updateBalance(
        goal: Goal,
        newBalance: BigDecimal,
    ) {
        val balanceDifference = newBalance.subtract(goal.balance)

        if (balanceDifference.isNotZero()) {
            val updatedInitial = goal.initialBalance.add(balanceDifference)
            goal.initialBalance = updatedInitial.max(BigDecimal.ZERO)

            logger.info(
                "Goal with id {} initial balance updated to {}",
                goal.id,
                goal.initialBalance,
            )
        }

        goal.balance = newBalance

        logger.info("{} balance updated to {}", goal, newBalance)
    }
}
