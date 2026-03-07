/*
 * Filename: Dividend.kt (original filename: Dividend.java)
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

@Entity
@Table(name = "dividend")
class Dividend(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @ManyToOne
    @JoinColumn(name = "ticker_id", referencedColumnName = "id", nullable = false)
    var ticker: Ticker,
    @ManyToOne
    @JoinColumn(name = "wallet_transaction_id", referencedColumnName = "id", nullable = false)
    var walletTransaction: WalletTransaction,
) {
    override fun toString(): String = "Dividend [id=$id, ticker=${ticker.symbol}]"
}
