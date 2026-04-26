package org.moinex.factory.financialplanning

import org.moinex.model.financialplanning.FIRECalculatorSettings
import java.math.BigDecimal

object FIRECalculatorSettingsFactory {
    fun create(
        currentNetWorth: BigDecimal = BigDecimal("100000.00"),
        monthlyContribution: BigDecimal = BigDecimal("2000.00"),
        annualReturnRate: BigDecimal = BigDecimal("10.00"),
        monthlyExpense: BigDecimal = BigDecimal("5000.00"),
        withdrawalRate: BigDecimal = BigDecimal("4.00"),
        currentAge: Int = 30,
    ): FIRECalculatorSettings =
        FIRECalculatorSettings(
            currentNetWorth = currentNetWorth,
            monthlyContribution = monthlyContribution,
            annualReturnRate = annualReturnRate,
            monthlyExpense = monthlyExpense,
            withdrawalRate = withdrawalRate,
            currentAge = currentAge,
        )
}
