/*
 * Filename: Transaction.kt (original filename: Transaction.java)
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model.investment

import jakarta.persistence.Column
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import org.moinex.model.wallettransaction.WalletTransaction
import java.math.BigDecimal

@MappedSuperclass
abstract class Transaction(
    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    var quantity: BigDecimal,
    @Column(name = "unit_price", nullable = false, scale = 2)
    var unitPrice: BigDecimal,
    @ManyToOne
    @JoinColumn(name = "wallet_transaction_id", referencedColumnName = "id", nullable = false)
    var walletTransaction: WalletTransaction,
) {
    init {
        require(quantity > BigDecimal.ZERO) {
            "Transaction quantity must be positive"
        }
        require(unitPrice >= BigDecimal.ZERO) {
            "Unit price must be non-negative"
        }
    }
}
