package org.moinex.service;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.moinex.model.Category;
import org.moinex.model.enums.BudgetGroupTransactionFilter;
import org.moinex.model.enums.TransactionType;
import org.moinex.model.financialplanning.BudgetGroup;
import org.moinex.model.financialplanning.FinancialPlan;
import org.moinex.repository.CategoryRepository;
import org.moinex.repository.creditcard.CreditCardPaymentRepository;
import org.moinex.repository.financialplanning.BudgetGroupRepository;
import org.moinex.repository.financialplanning.FinancialPlanRepository;
import org.moinex.repository.wallettransaction.TransferRepository;
import org.moinex.repository.wallettransaction.WalletTransactionRepository;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing financial plans and budget groups
 */
@Service
@NoArgsConstructor
public class FinancialPlanningService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialPlanningService.class);
    private FinancialPlanRepository financialPlanRepository;
    private BudgetGroupRepository budgetGroupRepository;
    private WalletTransactionRepository walletTransactionRepository;
    private CreditCardPaymentRepository creditCardPaymentRepository;
    private CategoryRepository categoryRepository;
    private TransferRepository transferRepository;

    @Autowired
    public FinancialPlanningService(
            FinancialPlanRepository financialPlanRepository,
            BudgetGroupRepository budgetGroupRepository,
            WalletTransactionRepository walletTransactionRepository,
            CreditCardPaymentRepository creditCardPaymentRepository,
            CategoryRepository categoryRepository,
            TransferRepository transferRepository) {
        this.financialPlanRepository = financialPlanRepository;
        this.budgetGroupRepository = budgetGroupRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.creditCardPaymentRepository = creditCardPaymentRepository;
        this.categoryRepository = categoryRepository;
        this.transferRepository = transferRepository;
    }

    /**
     * Creates a new financial plan along with its budget groups
     *
     * @param name   The name of the plan
     * @param income The base monthly income for the plan
     * @param groups The list of budget groups to create for this plan
     * @return The ID of the newly created financial plan
     */
    @Transactional
    public Integer createPlan(
            @NonNull String name, @NonNull BigDecimal income, List<BudgetGroup> groups) {
        name = name.trim();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("The name of the financial plan cannot be empty.");
        }

        if (financialPlanRepository.existsByName(name)) {
            throw new IllegalArgumentException("A financial plan with this name already exists.");
        }

        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The base income must be greater than zero.");
        }

        validateBudgetGroups(groups);

        FinancialPlan plan = FinancialPlan.builder().name(name).baseIncome(income).build();

        groups.forEach(group -> group.setPlan(plan));
        plan.setBudgetGroups(groups);

        try {
            FinancialPlan oldPlan = getActivePlan();
            oldPlan.setArchived(true);
            financialPlanRepository.save(oldPlan);
            logger.info("Archived previous financial plan: {}", oldPlan.getName());
        } catch (EntityNotFoundException e) {
            logger.info("No active financial plan found, creating a new one.");
        }

        logger.info(
                "Financial plan '{}' created with base income {}",
                name,
                UIUtils.formatCurrency(income));

        return financialPlanRepository.save(plan).getId();
    }

    /**
     * Edits an existing financial plan and its budget groups
     *
     * @param plan The financial plan with updated details
     */
    @Transactional
    public void updatePlan(@NonNull FinancialPlan plan) {
        FinancialPlan originalPlan =
                financialPlanRepository
                        .findById(plan.getId())
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Financial plan with ID "
                                                        + plan.getId()
                                                        + " not found"));

        if (plan.getName() == null || plan.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("The name of the financial plan cannot be empty.");
        }

        if (plan.getBaseIncome() == null || plan.getBaseIncome().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The base income must be greater than zero.");
        }

        // Check if the name is being changed to one that already exists
        if (!originalPlan.getName().equals(plan.getName())
                && financialPlanRepository.existsByName(plan.getName())) {
            throw new IllegalStateException("A financial plan with this name already exists.");
        }

        validateBudgetGroups(plan.getBudgetGroups());

        originalPlan.setName(plan.getName());
        originalPlan.setBaseIncome(plan.getBaseIncome());

        originalPlan.getBudgetGroups().clear();
        plan.getBudgetGroups().forEach(group -> group.setPlan(originalPlan));
        originalPlan.getBudgetGroups().addAll(plan.getBudgetGroups());

        financialPlanRepository.save(originalPlan);

        logger.info("Financial plan '{}' updated", plan.getName());
    }

    private void validateBudgetGroups(List<BudgetGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            throw new IllegalArgumentException("At least one budget group must be provided.");
        }

        BigDecimal totalPercentage = BigDecimal.ZERO;

        for (BudgetGroup group : groups) {
            if (group.getName() == null || group.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Budget group name cannot be empty.");
            }
            if (group.getTargetPercentage() == null
                    || group.getTargetPercentage().compareTo(BigDecimal.ZERO) <= 0
                    || group.getTargetPercentage().compareTo(new BigDecimal("100.00")) > 0) {
                throw new IllegalArgumentException(
                        "Budget group percentage must be between 0.00 and 100.00");
            }
            if (group.getCategories() == null || group.getCategories().isEmpty()) {
                throw new IllegalArgumentException("Budget group must have at least one category.");
            }
            group.getCategories()
                    .forEach(
                            category -> {
                                if (!categoryRepository.existsById(category.getId())) {
                                    throw new EntityNotFoundException(
                                            "Category with ID "
                                                    + category.getId()
                                                    + " does not exist.");
                                }

                                if (category.isArchived()) {
                                    throw new IllegalArgumentException(
                                            "Cannot add archived category "
                                                    + category.getName()
                                                    + " to budget group.");
                                }
                            });

            totalPercentage = totalPercentage.add(group.getTargetPercentage());
        }

        if (totalPercentage.compareTo(new BigDecimal("100.00")) != 0) {
            throw new IllegalArgumentException(
                    "Total target percentage of budget groups must equal 100.00");
        }
    }

    /**
     * Calculates the current status of a financial plan for a given period.
     * It determines the amount spent in each budget group by summing up the
     * transactions of the associated categories within the specified month and year.
     *
     * @param planId The ID of the financial plan.
     * @param period The month and year for which to calculate the status.
     * @return A list of DTOs, each containing a budget group and the total amount spent for it.
     */
    @Transactional(readOnly = true)
    public List<PlanStatusDTO> getPlanStatus(Integer planId, YearMonth period) {
        FinancialPlan plan =
                financialPlanRepository
                        .findById(planId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Financial plan with ID " + planId + " not found"));

        String startDate =
                period.atDay(1).atStartOfDay().format(Constants.DATE_FORMATTER_WITH_TIME);
        String endDate =
                period.atEndOfMonth().atTime(23, 59, 59).format(Constants.DATE_FORMATTER_WITH_TIME);

        return plan.getBudgetGroups().stream()
                .map(
                        group -> {
                            if (group.getCategories() == null || group.getCategories().isEmpty()) {
                                return new PlanStatusDTO(group, BigDecimal.ZERO);
                            }

                            List<Integer> categoryIds =
                                    group.getCategories().stream()
                                            .map(Category::getId)
                                            .collect(Collectors.toList());

                            BigDecimal totalAmount = BigDecimal.ZERO;

                            BudgetGroupTransactionFilter filter = group.getTransactionTypeFilter();

                            if (filter == BudgetGroupTransactionFilter.EXPENSE
                                    || filter == BudgetGroupTransactionFilter.BOTH) {
                                BigDecimal walletExpenses =
                                        walletTransactionRepository
                                                .getSumAmountByCategoriesAndDateBetween(
                                                        categoryIds,
                                                        TransactionType.EXPENSE,
                                                        startDate,
                                                        endDate);

                                BigDecimal creditCardTransactionsAmount =
                                        creditCardPaymentRepository
                                                .getSumAmountByCategoriesAndDateBetween(
                                                        categoryIds, startDate, endDate);

                                totalAmount =
                                        totalAmount
                                                .add(walletExpenses)
                                                .add(creditCardTransactionsAmount);
                            }

                            if (filter == BudgetGroupTransactionFilter.INCOME
                                    || filter == BudgetGroupTransactionFilter.BOTH) {
                                BigDecimal walletIncome =
                                        walletTransactionRepository
                                                .getSumAmountByCategoriesAndDateBetween(
                                                        categoryIds,
                                                        TransactionType.INCOME,
                                                        startDate,
                                                        endDate);

                                totalAmount = totalAmount.add(walletIncome);
                            }

                            return new PlanStatusDTO(group, totalAmount);
                        })
                .collect(Collectors.toList());
    }

    public FinancialPlan getActivePlan() {
        return financialPlanRepository
                .findByArchivedFalse()
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "No active financial plan found. Please create one."));
    }

    /**
     * Gets historical spending data for all budget groups in a plan over multiple months
     *
     * @param planId The ID of the financial plan
     * @param startPeriod The starting month (inclusive)
     * @param endPeriod The ending month (inclusive)
     * @return List of historical data points for each budget group and month
     */
    @Transactional(readOnly = true)
    public List<BudgetGroupHistoricalDataDTO> getHistoricalData(
            Integer planId, YearMonth startPeriod, YearMonth endPeriod) {
        FinancialPlan plan =
                financialPlanRepository
                        .findById(planId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Financial plan with ID " + planId + " not found"));

        List<BudgetGroupHistoricalDataDTO> historicalData = new java.util.ArrayList<>();

        for (YearMonth period = startPeriod;
                !period.isAfter(endPeriod);
                period = period.plusMonths(1)) {
            List<PlanStatusDTO> statusList = getPlanStatus(planId, period);

            for (PlanStatusDTO status : statusList) {
                BigDecimal targetAmount =
                        plan.getBaseIncome()
                                .multiply(status.group().getTargetPercentage())
                                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

                historicalData.add(
                        new BudgetGroupHistoricalDataDTO(
                                status.group().getName(),
                                period,
                                status.spentAmount(),
                                targetAmount));
            }
        }

        return historicalData;
    }

    public record PlanStatusDTO(BudgetGroup group, BigDecimal spentAmount) {}

    public record BudgetGroupHistoricalDataDTO(
            String groupName, YearMonth period, BigDecimal spentAmount, BigDecimal targetAmount) {}
}
