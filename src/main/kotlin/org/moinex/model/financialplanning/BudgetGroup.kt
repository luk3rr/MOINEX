/*
 * Filename: BudgetGroup.kt (original filename: BudgetGroup.java)
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model.financialplanning

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.moinex.model.Category
import org.moinex.model.enums.BudgetGroupTransactionFilter
import java.math.BigDecimal

@Entity
@Table(name = "budget_group")
class BudgetGroup(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    @Column(nullable = false, length = 50)
    var name: String,
    @Column(name = "target_percentage", nullable = false)
    var targetPercentage: BigDecimal,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    var plan: FinancialPlan? = null, // TODO: Set plan when creating
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "budget_group_categories",
        joinColumns = [JoinColumn(name = "budget_group_id")],
        inverseJoinColumns = [JoinColumn(name = "category_id")],
    )
    var categories: MutableSet<Category> = mutableSetOf(),
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type_filter", nullable = false)
    var transactionTypeFilter: BudgetGroupTransactionFilter,
) {
    init {
        name = name.trim()

        require(name.isNotEmpty()) {
            "Budget group name cannot be empty"
        }
        require(targetPercentage in BigDecimal.ZERO..BigDecimal(100)) {
            "Budget group target percentage must be between 0 and 100"
        }
        require(categories.isNotEmpty()) {
            "Budget group must have at least one category"
        }
    }

    fun isSame(other: BudgetGroup?): Boolean {
        if (other == null) return false
        return this.name == other.name &&
            this.targetPercentage.compareTo(other.targetPercentage) == 0 &&
            this.categories == other.categories &&
            this.transactionTypeFilter == other.transactionTypeFilter
    }

    override fun toString(): String = "BudgetGroup [id=$id, name='$name']"
}
