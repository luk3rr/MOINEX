/*
 * Filename: CreditCardPayment.kt (original filename: CreditCardPayment.java)
 * Created on: August 26, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 28/02/2026
 */

package org.moinex.model.creditcard

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.moinex.common.converter.LocalDateTimeStringConverter
import org.moinex.common.extension.toRounded
import org.moinex.model.wallettransaction.Wallet
import org.moinex.util.Constants
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "credit_card_payment")
class CreditCardPayment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @ManyToOne
    @JoinColumn(name = "wallet_id", referencedColumnName = "id")
    var wallet: Wallet? = null,
    @ManyToOne
    @JoinColumn(name = "debt_id", referencedColumnName = "id", nullable = false)
    var creditCardDebt: CreditCardDebt,
    @Column(name = "amount", nullable = false, scale = 2)
    var amount: BigDecimal,
    @Column(name = "rebateUsed", nullable = false, scale = 2)
    var rebateUsed: BigDecimal = BigDecimal.ZERO,
    @Column(name = "installment", nullable = false)
    var installment: Int,
    @Column(name = "refunded", nullable = false)
    var refunded: Boolean = false,
    @Column(name = "date", nullable = false)
    @Convert(converter = LocalDateTimeStringConverter::class)
    var date: LocalDateTime,
) {
    init {
        amount = amount.toRounded()
        rebateUsed = rebateUsed.toRounded()

        require(amount > BigDecimal.ZERO) { "Amount must be greater than zero" }
        require(rebateUsed >= BigDecimal.ZERO) { "Rebate used must be greater than or equal to zero" }
        require(installment in 1..Constants.MAX_INSTALLMENTS) { "Installment must be between 1 and ${Constants.MAX_INSTALLMENTS}" }
    }

    fun isRefunded(): Boolean = refunded

    fun isPaid(): Boolean = wallet != null

    override fun toString(): String =
        "Credit Card Payment [id=$id, installment=$installment, amount=$amount, date=$date, debtId=${creditCardDebt.id}] "
}
