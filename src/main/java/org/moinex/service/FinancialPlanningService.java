package org.moinex.service;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.moinex.model.Category;
import org.moinex.model.financialplanning.BudgetGroup;
import org.moinex.model.financialplanning.FinancialPlan;
import org.moinex.repository.CategoryRepository;
import org.moinex.repository.financialplanning.BudgetGroupRepository;
import org.moinex.repository.financialplanning.FinancialPlanRepository;
import org.moinex.repository.wallettransaction.WalletTransactionRepository;
import org.moinex.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing financial plans and budget groups
 */
@Service
@NoArgsConstructor
public class FinancialPlanningService {

    private FinancialPlanRepository financialPlanRepository;
    private BudgetGroupRepository budgetGroupRepository;
    private WalletTransactionRepository walletTransactionRepository;
    private CategoryRepository categoryRepository;

    @Autowired
    public FinancialPlanningService(
            FinancialPlanRepository financialPlanRepository,
            BudgetGroupRepository budgetGroupRepository,
            WalletTransactionRepository walletTransactionRepository,
            CategoryRepository categoryRepository) {
        this.financialPlanRepository = financialPlanRepository;
        this.budgetGroupRepository = budgetGroupRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.categoryRepository = categoryRepository;
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

        return financialPlanRepository.save(plan).getId();
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

                            BigDecimal spentAmount =
                                    walletTransactionRepository
                                            .getSumAmountByCategoriesAndDateBetween(
                                                    categoryIds, startDate, endDate);

                            return new PlanStatusDTO(group, spentAmount);
                        })
                .collect(Collectors.toList());
    }

    public static record PlanStatusDTO(BudgetGroup group, BigDecimal spentAmount) {}
}
