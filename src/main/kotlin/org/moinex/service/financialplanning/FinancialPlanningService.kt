package org.moinex.service.financialplanning

import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.atEndOfDay
import org.moinex.common.extension.findByIdOrThrow
import org.moinex.common.extension.isBeforeOrEqual
import org.moinex.common.extension.toRounded
import org.moinex.model.dto.BudgetGroupHistoricalDataDTO
import org.moinex.model.dto.PlanStatusDTO
import org.moinex.model.enums.NotificationType
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.financialplanning.BudgetGroup
import org.moinex.model.financialplanning.FinancialPlan
import org.moinex.repository.financialplanning.FinancialPlanRepository
import org.moinex.service.CategoryService
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class FinancialPlanningService(
    private val financialPlanRepository: FinancialPlanRepository,
    private val creditCardService: CreditCardService,
    private val categoryService: CategoryService,
    private val walletService: WalletService,
    private val notificationService: NotificationService,
    private val preferencesService: PreferencesService,
) {
    private val logger = LoggerFactory.getLogger(FinancialPlanningService::class.java)

    @Transactional
    fun createPlan(plan: FinancialPlan): Int {
        check(!financialPlanRepository.existsByName(plan.name)) {
            "A financial plan '${plan.name}' already exists."
        }

        validateCategories(plan.budgetGroups)

        plan.budgetGroups.forEach { group -> group.plan = plan }

        getNonArchivedPlan()?.let { previousPlan ->
            previousPlan.endDate = plan.startDate.minusDays(1)
            previousPlan.archived = true
            logger.info("Archived previous $previousPlan with endDate=${previousPlan.endDate}")
        } ?: logger.info("No active financial plan found, creating a new one")

        val newPlan = financialPlanRepository.save(plan)

        logger.info("$newPlan created successfully with startDate=${newPlan.startDate}")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_PLAN_CREATED_TITLE),
            message =
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_PLAN_CREATED_MESSAGE),
            relatedEntityId = newPlan.id!!,
        )

        return newPlan.id!!
    }

    @Transactional
    fun updatePlan(updatedPlan: FinancialPlan) {
        val planFromDatabase = financialPlanRepository.findByIdOrThrow(updatedPlan.id!!)

        check(!financialPlanRepository.existsByNameAndIdNot(updatedPlan.name, updatedPlan.id!!)) {
            "A financial plan name ${updatedPlan.name} already exists"
        }

        validateCategories(updatedPlan.budgetGroups)

        planFromDatabase.apply {
            name = updatedPlan.name
            baseIncome = updatedPlan.baseIncome
        }

        val updatedGroupsById = updatedPlan.budgetGroups.filter { it.id != null }.associateBy { it.id!! }

        planFromDatabase.budgetGroups.removeIf { it.id !in updatedGroupsById.keys }

        planFromDatabase.budgetGroups.forEach { existingGroup ->
            updatedGroupsById[existingGroup.id]?.let { updatedGroup ->
                existingGroup.name = updatedGroup.name
                existingGroup.targetPercentage = updatedGroup.targetPercentage
                existingGroup.transactionTypeFilter = updatedGroup.transactionTypeFilter
                existingGroup.categories.clear()
                existingGroup.categories.addAll(updatedGroup.categories)
            }
        }

        updatedPlan.budgetGroups.filter { it.id == null }.forEach { newGroup ->
            newGroup.plan = planFromDatabase
            planFromDatabase.budgetGroups.add(newGroup)
        }

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_PLAN_UPDATED_TITLE),
            message =
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_PLAN_UPDATED_MESSAGE),
            relatedEntityId = planFromDatabase.id!!,
        )

        logger.info("$planFromDatabase updated successfully")
    }

    fun getPlanStatus(
        planId: Int,
        period: YearMonth,
    ): List<PlanStatusDTO> {
        val planFromDatabase = financialPlanRepository.findByIdOrThrow(planId)

        val startDate = period.atDay(1).atStartOfDay()
        val endDate = period.atEndOfMonth().atEndOfDay()

        return planFromDatabase.budgetGroups.map { group ->
            PlanStatusDTO(group, calculateGroupAmount(group, startDate, endDate))
        }
    }

    fun getHistoricalData(
        planId: Int,
        startPeriod: YearMonth,
        endPeriod: YearMonth,
    ): List<BudgetGroupHistoricalDataDTO> {
        val planFromDatabase = financialPlanRepository.findByIdOrThrow(planId)

        return generateSequence(startPeriod) { it.plusMonths(1) }
            .takeWhile { it.isBeforeOrEqual(endPeriod) }
            .flatMap { period ->
                getPlanStatus(planId, period).map { status ->
                    BudgetGroupHistoricalDataDTO(
                        status.group.name,
                        period,
                        status.spentAmount,
                        calculateTargetAmount(planFromDatabase.baseIncome, status.group.targetPercentage),
                    )
                }
            }.toList()
    }

    fun getHistoricalDataAcrossPlans(
        startPeriod: YearMonth,
        endPeriod: YearMonth,
    ): List<BudgetGroupHistoricalDataDTO> =
        generateSequence(startPeriod) { it.plusMonths(1) }
            .takeWhile { it.isBeforeOrEqual(endPeriod) }
            .flatMap { period ->
                val plan = getPlanForPeriod(period)
                if (plan != null) {
                    getPlanStatus(plan.id!!, period)
                        .map { status ->
                            BudgetGroupHistoricalDataDTO(
                                status.group.name,
                                period,
                                status.spentAmount,
                                calculateTargetAmount(plan.baseIncome, status.group.targetPercentage),
                            )
                        }.asSequence()
                } else {
                    emptySequence()
                }
            }.toList()

    fun getPlanForPeriod(period: YearMonth): FinancialPlan? {
        val date = period.atDay(15)
        return financialPlanRepository.findPlanForDate(date)
    }

    fun getNonArchivedPlan(): FinancialPlan? = financialPlanRepository.findByArchivedFalse()

    fun getActivePlan(): FinancialPlan? = financialPlanRepository.findByArchivedFalse()

    fun getAllPlans(): List<FinancialPlan> = financialPlanRepository.findAll()

    private fun calculateTargetAmount(
        baseIncome: BigDecimal,
        targetPercentage: BigDecimal,
    ): BigDecimal =
        baseIncome
            .multiply(targetPercentage)
            .divide(BigDecimal(100))
            .toRounded()

    private fun validateCategories(groups: List<BudgetGroup>) {
        groups.forEach { group ->
            check(group.categories.isNotEmpty()) {
                "$group must have at least one category."
            }
        }

        groups
            .asSequence()
            .flatMap { it.categories }
            .forEach { category ->
                check(categoryService.existsById(category.id!!)) {
                    "$category does not exist."
                }

                check(!category.isArchived) {
                    "Cannot add archived $category to budget group."
                }
            }
    }

    private fun calculateGroupAmount(
        group: BudgetGroup,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): BigDecimal {
        if (group.categories.isEmpty()) return BigDecimal.ZERO

        val categoryIds = group.categories.map { it.id!! }

        var totalAmount = BigDecimal.ZERO

        val filter = group.transactionTypeFilter

        if (filter.includeExpenses()) {
            val walletExpenses =
                walletService.getTotalWalletTransactionAmountByCategoriesAndTypeAndDateTimeBetween(
                    categoryIds,
                    WalletTransactionType.EXPENSE,
                    startDate,
                    endDate,
                )

            val creditCardTransactionsAmount =
                creditCardService
                    .getTotalPaymentsByCategoriesAndDateTimeBetween(
                        categoryIds,
                        startDate,
                        endDate,
                    )

            totalAmount += walletExpenses.add(creditCardTransactionsAmount)
        }

        if (filter.includeIncomes()) {
            val walletIncome =
                walletService
                    .getTotalWalletTransactionAmountByCategoriesAndTypeAndDateTimeBetween(
                        categoryIds,
                        WalletTransactionType.INCOME,
                        startDate,
                        endDate,
                    )

            totalAmount += walletIncome
        }

        return totalAmount
    }
}
