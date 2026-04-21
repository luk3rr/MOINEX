/*
 * Filename: RecurringCreditCardDebtRepository.kt
 * Created on: April 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.creditcard

import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.model.creditcard.RecurringCreditCardDebt
import org.moinex.model.enums.RecurringTransactionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface RecurringCreditCardDebtRepository : JpaRepository<RecurringCreditCardDebt, Int> {
    fun findAllByStatus(status: RecurringTransactionStatus): List<RecurringCreditCardDebt>

    fun findAllByCreditCard(creditCard: CreditCard): List<RecurringCreditCardDebt>

    fun findAllByCreditCardAndStatus(
        creditCard: CreditCard,
        status: RecurringTransactionStatus,
    ): List<RecurringCreditCardDebt>

    @Query(
        "SELECT ccd FROM CreditCardDebt ccd " +
            "WHERE ccd.recurringSource.id = :recurringId " +
            "AND ccd.date >= :monthStart " +
            "AND ccd.date <= :monthEnd",
    )
    fun findMaterializedDebtForMonth(
        @Param("recurringId") recurringId: Int,
        @Param("monthStart") monthStart: LocalDateTime,
        @Param("monthEnd") monthEnd: LocalDateTime,
    ): List<CreditCardDebt>
}
