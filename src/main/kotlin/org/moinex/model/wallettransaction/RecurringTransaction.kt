/*
 * Filename: RecurringTransaction.kt (original filename: RecurringTransaction.java)
 * Created on: November 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.model.wallettransaction

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.Table
import org.moinex.common.converter.LocalDateStringConverter
import org.moinex.common.isAfterOrEqual
import org.moinex.common.isBeforeOrEqual
import org.moinex.model.Category
import org.moinex.model.enums.RecurringTransactionFrequency
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.util.Constants
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Inheritance
@Table(name = "recurring_transaction")
class RecurringTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "start_date", nullable = false)
    @Convert(converter = LocalDateStringConverter::class)
    var startDate: LocalDate,
    @Column(name = "end_date", nullable = false)
    @Convert(converter = LocalDateStringConverter::class)
    var endDate: LocalDate = Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE,
    @Column(name = "next_due_date", nullable = false)
    @Convert(converter = LocalDateStringConverter::class)
    var nextDueDate: LocalDate,
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    var frequency: RecurringTransactionFrequency,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar default 'ACTIVE'")
    var status: RecurringTransactionStatus = RecurringTransactionStatus.ACTIVE,
    @Column(name = "include_in_net_worth", nullable = false)
    var includeInNetWorth: Boolean = false,
    description: String? = null,
    wallet: Wallet,
    category: Category,
    type: WalletTransactionType,
    amount: BigDecimal,
    includeInAnalysis: Boolean = true,
) : BaseTransaction(wallet, category, type, amount, description, includeInAnalysis) {
    init {
        require(startDate.isBefore(endDate)) {
            "Start date must be before to end date"
        }

        require(nextDueDate.isAfterOrEqual(startDate)) {
            "Next due date must be after or equal to start date"
        }

        val minimumEndDate = startDate.plus(1, frequency.chronoUnit)
        require(minimumEndDate.isBeforeOrEqual(endDate)) {
            "End date must be at least one ${frequency.name} after the start date"
        }
    }

    fun isActive(): Boolean = status == RecurringTransactionStatus.ACTIVE

    override fun toString(): String = "Recurring Transaction [id=$id, type=$type, frequency=$frequency, amount=$amount]"
}
