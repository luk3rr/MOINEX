/*
 * Filename: BondOperation.kt (original filename: BondOperation.java)
 * Created on: January  3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model.investment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.moinex.common.extension.toRounded
import org.moinex.model.enums.OperationType
import org.moinex.model.wallettransaction.WalletTransaction
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "bond_operation")
class BondOperation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @ManyToOne(optional = false)
    @JoinColumn(name = "bond_id", referencedColumnName = "id", nullable = false)
    var bond: Bond,
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    var operationType: OperationType,
    @Column(name = "quantity", nullable = false)
    var quantity: BigDecimal,
    @Column(name = "unit_price", nullable = false)
    var unitPrice: BigDecimal,
    @Column(name = "fees", nullable = false)
    var fees: BigDecimal = BigDecimal.ZERO,
    @Column(name = "taxes", nullable = false)
    var taxes: BigDecimal = BigDecimal.ZERO,
    @Column(name = "net_profit", nullable = false)
    var netProfit: BigDecimal = BigDecimal.ZERO,
    @Column(name = "spread")
    var spread: BigDecimal? = null,
    @ManyToOne
    @JoinColumn(name = "wallet_transaction_id", referencedColumnName = "id", nullable = false)
    var walletTransaction: WalletTransaction? = null,
) {
    init {
        quantity = quantity.toRounded()
        unitPrice = unitPrice.toRounded()
        fees = fees.toRounded()
        taxes = taxes.toRounded()
        netProfit = netProfit.toRounded()
        spread = spread?.toRounded()

        require(quantity > BigDecimal.ZERO) {
            "Quantity must be positive"
        }
        require(unitPrice >= BigDecimal.ZERO) {
            "Unit price must be non-negative"
        }
        require(fees >= BigDecimal.ZERO) {
            "Fees must be non-negative"
        }
        require(taxes >= BigDecimal.ZERO) {
            "Taxes must be non-negative"
        }
    }

    val localDate: LocalDate
        get() = walletTransaction!!.date.toLocalDate()

    val totalValue: BigDecimal
        get() = quantity.multiply(unitPrice)

    override fun toString(): String = "BondOperation [id=$id, bond=${bond.name}, type=$operationType]"

    fun isPurchase(): Boolean = operationType == OperationType.BUY

    fun isSale(): Boolean = operationType == OperationType.SELL
}
