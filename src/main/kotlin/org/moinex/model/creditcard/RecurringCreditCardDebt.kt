/*
 * Filename: RecurringCreditCardDebt.kt
 * Created on: April 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.creditcard

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.moinex.common.constant.Constants
import org.moinex.common.converter.LocalDateStringConverter
import org.moinex.common.converter.YearMonthStringConverter
import org.moinex.common.extension.isBeforeOrEqual
import org.moinex.common.extension.toRounded
import org.moinex.model.Category
import org.moinex.model.enums.CreditCardRecurringFrequency
import org.moinex.model.enums.RecurringTransactionStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Entity
@Table(name = "recurring_credit_card_debt")
class RecurringCreditCardDebt(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @ManyToOne
    @JoinColumn(name = "crc_id", referencedColumnName = "id", nullable = false)
    var creditCard: CreditCard,
    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id", nullable = false)
    var category: Category,
    @Column(name = "amount", nullable = false, scale = 2)
    var amount: BigDecimal,
    @Column(name = "description")
    var description: String?,
    @Column(name = "day_of_month", nullable = false)
    var dayOfMonth: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    var frequency: CreditCardRecurringFrequency,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar default 'ACTIVE'")
    var status: RecurringTransactionStatus = RecurringTransactionStatus.ACTIVE,
    @Column(name = "start_date", nullable = false)
    @Convert(converter = LocalDateStringConverter::class)
    var startDate: LocalDate,
    @Column(name = "end_date", nullable = false)
    @Convert(converter = LocalDateStringConverter::class)
    var endDate: LocalDate = Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE,
    @Column(name = "next_invoice_month", nullable = false)
    @Convert(converter = YearMonthStringConverter::class)
    var nextInvoiceMonth: YearMonth,
) {
    init {
        amount = amount.toRounded()

        require(amount > BigDecimal.ZERO) { "Amount must be positive" }

        require(dayOfMonth in 1..Constants.MAX_BILLING_DUE_DAY) {
            "Day of month must be in range [1, ${Constants.MAX_BILLING_DUE_DAY}]"
        }

        val minimumEndDate = startDate.plus(1, frequency.chronoUnit)
        require(minimumEndDate.isBeforeOrEqual(endDate)) {
            "End date must be at least one ${frequency.name} after the start date"
        }
    }

    fun isActive(): Boolean = status == RecurringTransactionStatus.ACTIVE

    fun isInactive(): Boolean = status == RecurringTransactionStatus.INACTIVE

    override fun toString(): String =
        "RecurringCreditCardDebt [id=$id, creditCard=${creditCard.name}, amount=$amount, frequency=$frequency]"
}
