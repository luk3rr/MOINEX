/*
 * Filename: CreditCardDebt.kt (original filename: CreditCardDebt.java)
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
import org.moinex.model.Category
import org.moinex.util.Constants
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "credit_card_debt")
class CreditCardDebt(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id", nullable = false)
    var category: Category,
    @Column(name = "installments", nullable = false)
    var installments: Int = 1,
    creditCard: CreditCard,
    date: LocalDateTime,
    amount: BigDecimal,
    description: String? = null,
) : CreditCardTransaction(
        creditCard = creditCard,
        date = date,
        amount = amount,
        description = description,
    ) {
    init {
        require(installments in 1..Constants.MAX_INSTALLMENTS) {
            "Installment must be in the range [1, ${Constants.MAX_INSTALLMENTS}]"
        }
    }

    override fun toString(): String = "CreditCardDebt [id=$id, description='$description']"
}
