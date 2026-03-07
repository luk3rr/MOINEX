/*
 * Filename: Goal.kt (original filename: Goal.java)
 * Created on: December 6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.model.goal

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import org.moinex.common.toRounded
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletType
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "goal")
@PrimaryKeyJoinColumn(name = "wallet_id", referencedColumnName = "id")
class Goal(
    @Column(name = "initial_balance", nullable = false)
    var initialBalance: BigDecimal,
    @Column(name = "target_balance", nullable = false, scale = 2)
    var targetBalance: BigDecimal,
    @Column(name = "target_date", nullable = false)
    var targetDate: LocalDate,
    @Column(name = "completion_date")
    var completionDate: LocalDate? = null,
    @Column(name = "motivation", length = 500)
    var motivation: String? = null,
    id: Int? = null,
    name: String,
    type: WalletType,
    isArchived: Boolean = false,
    masterWallet: Wallet? = null,
) : Wallet(
        id = id,
        type = type,
        name = name,
        balance = initialBalance,
        isArchived = isArchived,
        masterWallet = masterWallet,
    ) {
    init {
        initialBalance = initialBalance.toRounded()
        targetBalance = targetBalance.toRounded()

        require(targetDate.isAfter(LocalDate.now())) {
            "Target date must be in the future"
        }

        require(initialBalance >= BigDecimal.ZERO) {
            "Initial balance must be greater than or equal to zero"
        }

        require(targetBalance >= BigDecimal.ZERO) {
            "Target balance must be greater than or equal to zero"
        }

        require(initialBalance <= targetBalance) {
            "Initial balance must be less than or equal to target balance"
        }
    }

    fun isCompleted(): Boolean = completionDate != null
}
