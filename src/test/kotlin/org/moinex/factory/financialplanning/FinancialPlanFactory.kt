package org.moinex.factory.financialplanning

import org.moinex.model.financialplanning.BudgetGroup
import org.moinex.model.financialplanning.FinancialPlan
import java.math.BigDecimal

object FinancialPlanFactory {
    fun create(
        id: Int? = null,
        name: String = "Monthly Budget",
        baseIncome: BigDecimal = BigDecimal("5000.00"),
        budgetGroups: MutableList<BudgetGroup> = mutableListOf(),
        archived: Boolean = false,
    ): FinancialPlan =
        FinancialPlan(
            id = id,
            name = name,
            baseIncome = baseIncome,
            budgetGroups = budgetGroups,
            archived = archived,
        )
}
