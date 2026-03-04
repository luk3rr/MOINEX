/*
 * Filename: BaseTransaction.kt (original filename: BaseTransaction.java)
 * Created on: November 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.model.wallettransaction

import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import org.moinex.common.toRounded
import org.moinex.model.Category
import org.moinex.model.enums.WalletTransactionType
import java.math.BigDecimal

@MappedSuperclass
class BaseTransaction(
    @ManyToOne
    @JoinColumn(name = "wallet_id", referencedColumnName = "id", nullable = false)
    var wallet: Wallet,
    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id", nullable = false)
    var category: Category,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: WalletTransactionType,
    @Column(name = "amount", nullable = false, scale = 2)
    var amount: BigDecimal,
    @Column(name = "description")
    var description: String? = null,
    @Column(name = "include_in_analysis", nullable = false)
    var includeInAnalysis: Boolean,
) {
    init {
        amount = amount.toRounded()
        require(amount > BigDecimal.ZERO) { "Amount must be positive" }
    }
}
