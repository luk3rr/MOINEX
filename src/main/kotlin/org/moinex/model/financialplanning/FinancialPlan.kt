/*
 * Filename: FinancialPlan.kt (original filename: FinancialPlan.java)
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model.financialplanning

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.moinex.common.extension.isEqual
import org.moinex.common.extension.toRounded
import java.math.BigDecimal

@Entity
@Table(name = "financial_plan")
class FinancialPlan(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    @Column(nullable = false, length = 50)
    var name: String,
    @Column(name = "base_income", nullable = false)
    var baseIncome: BigDecimal,
    @OneToMany(
        mappedBy = "plan",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER,
    )
    var budgetGroups: MutableList<BudgetGroup> = mutableListOf(),
    @Column(name = "archived", nullable = false)
    var archived: Boolean = false,
) {
    init {
        name = name.trim()
        baseIncome = baseIncome.toRounded()

        require(name.isNotEmpty()) {
            "Financial plan name cannot be empty"
        }
        require(baseIncome > BigDecimal.ZERO) {
            "Base income must be positive"
        }
        require(budgetGroups.isNotEmpty()) {
            "Financial plan must have at least one budget group"
        }
        require(budgetGroups.sumOf { it.targetPercentage }.isEqual(100)) {
            "Total target percentage of budget groups must equal 100"
        }
    }

    override fun toString(): String = "FinancialPlan [id=$id, name='$name']"
}
