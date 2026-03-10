package org.moinex.factory.financialplanning

import org.moinex.factory.CategoryFactory
import org.moinex.model.Category
import org.moinex.model.enums.BudgetGroupTransactionFilter
import org.moinex.model.financialplanning.BudgetGroup
import org.moinex.model.financialplanning.FinancialPlan
import java.math.BigDecimal

object BudgetGroupFactory {
    fun create(
        id: Int? = null,
        name: String = "Food",
        targetPercentage: BigDecimal = BigDecimal("30.00"),
        plan: FinancialPlan? = null,
        categories: MutableSet<Category> = mutableSetOf(CategoryFactory.create(id = 1, name = "Groceries")),
        transactionTypeFilter: BudgetGroupTransactionFilter = BudgetGroupTransactionFilter.EXPENSE,
    ): BudgetGroup =
        BudgetGroup(
            id = id,
            name = name,
            targetPercentage = targetPercentage,
            plan = plan,
            categories = categories,
            transactionTypeFilter = transactionTypeFilter,
        )
}
