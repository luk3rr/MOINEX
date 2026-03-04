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
import org.moinex.common.LocalDateTimeStringConverter
import org.moinex.model.Category
import org.moinex.model.enums.RecurringTransactionFrequency
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Inheritance
@Table(name = "recurring_transaction")
class RecurringTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "start_date", nullable = false)
    @Convert(converter = LocalDateTimeStringConverter::class)
    var startDate: LocalDateTime,
    @Column(name = "end_date", nullable = false)
    @Convert(converter = LocalDateTimeStringConverter::class)
    var endDate: LocalDateTime,
    @Column(name = "next_due_date", nullable = false)
    @Convert(converter = LocalDateTimeStringConverter::class)
    var nextDueDate: LocalDateTime,
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
        require(startDate.isBefore(endDate) || startDate.isEqual(endDate)) {
            "Start date must be before or equal to end date"
        }

        require(nextDueDate.isAfter(startDate) || nextDueDate.isEqual(startDate)) {
            "Next due date must be after or equal to start date"
        }
    }

    override fun toString(): String = "Recurring Transaction [id=$id, type=$type, amount=$amount]"
}
