/*
 * Filename: CreditCard.kt (original filename: CreditCard.java)
 * Created on: August 26, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 28/02/2026
 */

package org.moinex.model.creditcard

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.moinex.common.toRounded
import org.moinex.model.wallettransaction.Wallet
import org.moinex.util.Constants
import java.math.BigDecimal

@Entity
@Table(name = "credit_card")
class CreditCard(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "operator_id", referencedColumnName = "id", nullable = false)
    var operator: CreditCardOperator,
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "default_billing_wallet_id", referencedColumnName = "id")
    var defaultBillingWallet: Wallet? = null,
    @Column(name = "name", nullable = false, length = 50, unique = true)
    var name: String,
    @Column(name = "billing_due_day", nullable = false)
    var billingDueDay: Int,
    @Column(name = "closing_day", nullable = false)
    var closingDay: Int,
    @Column(name = "max_debt", nullable = false, scale = 2)
    var maxDebt: BigDecimal,
    @Column(name = "available_rebate", nullable = false, scale = 2)
    var availableRebate: BigDecimal = BigDecimal.ZERO,
    @Column(name = "last_four_digits", length = 4)
    var lastFourDigits: String? = null,
    @Column(name = "archived", nullable = false)
    var isArchived: Boolean = false,
) {
    init {
        name = name.trim()
        maxDebt = maxDebt.toRounded()
        availableRebate = availableRebate.toRounded()
        require(name.isNotBlank()) { "Credit card name cannot be empty" }
        require(billingDueDay in 1..Constants.MAX_BILLING_DUE_DAY) {
            "Billing due day must be in the range [1, ${Constants.MAX_BILLING_DUE_DAY}]"
        }
        require(closingDay in 1..Constants.MAX_BILLING_DUE_DAY) {
            "Closing day must be in the range [1, ${Constants.MAX_BILLING_DUE_DAY}]"
        }
        require(maxDebt > BigDecimal.ZERO) { "Max debt must be positive" }
        lastFourDigits?.let {
            require(it.isNotBlank() && it.length == 4) { "Last four digits must have length 4" }
            require(it.matches(Regex("\\d{4}"))) { "Last four digits must be numeric" }
        }
    }

    override fun toString(): String = "Credit Card [id=$id, name='$name']"
}
