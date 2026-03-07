/*
 * Filename: TickerSale.kt (original filename: TickerSale.java)
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model.investment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.moinex.model.wallettransaction.WalletTransaction
import java.math.BigDecimal

@Entity
@Table(name = "ticker_sale")
class TickerSale(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @ManyToOne
    @JoinColumn(name = "ticker_id", referencedColumnName = "id")
    var ticker: Ticker,
    @Column(name = "average_cost", nullable = false)
    var averageCost: BigDecimal,
    quantity: BigDecimal,
    unitPrice: BigDecimal,
    walletTransaction: WalletTransaction,
) : Transaction(
        quantity = quantity,
        unitPrice = unitPrice,
        walletTransaction = walletTransaction,
    ) {
    init {
        require(averageCost >= BigDecimal.ZERO) {
            "Average cost must be non-negative"
        }
    }

    override fun toString(): String = "TickerSale [id=$id, ticker=${ticker.symbol}, quantity=$quantity]"
}
