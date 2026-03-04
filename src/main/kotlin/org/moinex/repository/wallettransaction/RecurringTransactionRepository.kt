/*
 * Filename: RecurringTransactionRepository.kt (original filename: RecurringTransactionRepository.java)
 * Created on: November 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.repository.wallettransaction

import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.RecurringTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RecurringTransactionRepository : JpaRepository<RecurringTransaction, Int> {
    fun findByStatus(status: RecurringTransactionStatus): List<RecurringTransaction>

    fun findByType(type: WalletTransactionType): List<RecurringTransaction>
}
