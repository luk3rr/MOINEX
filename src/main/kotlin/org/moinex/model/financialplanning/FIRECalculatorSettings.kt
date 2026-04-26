package org.moinex.model.financialplanning

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.moinex.common.extension.toRounded
import java.math.BigDecimal

@Entity
@Table(name = "fire_calculator_settings")
class FIRECalculatorSettings(
    @Id
    @Column(name = "id")
    val id: Int = 1,
    @Column(name = "current_net_worth", nullable = false)
    var currentNetWorth: BigDecimal,
    @Column(name = "monthly_contribution", nullable = false)
    var monthlyContribution: BigDecimal,
    @Column(name = "annual_return_rate", nullable = false)
    var annualReturnRate: BigDecimal,
    @Column(name = "monthly_expense", nullable = false)
    var monthlyExpense: BigDecimal,
    @Column(name = "withdrawal_rate", nullable = false)
    var withdrawalRate: BigDecimal,
    @Column(name = "current_age", nullable = false)
    var currentAge: Int,
) {
    init {
        currentNetWorth = currentNetWorth.toRounded()
        monthlyContribution = monthlyContribution.toRounded()
        annualReturnRate = annualReturnRate.toRounded()
        monthlyExpense = monthlyExpense.toRounded()
        withdrawalRate = withdrawalRate.toRounded()
    }

    override fun toString(): String = "FIRECalculatorSettings [id=$id]"
}
