/*
 * Filename: CryptoExchange.kt (original filename: CryptoExchange.java)
 * Created on: January 28, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model.investment

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.moinex.common.converter.LocalDateTimeStringConverter
import org.moinex.common.extension.toRounded
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "crypto_exchange")
class CryptoExchange(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @ManyToOne
    @JoinColumn(name = "sold_crypto_id", referencedColumnName = "id", nullable = false)
    var soldCrypto: Ticker,
    @ManyToOne
    @JoinColumn(name = "received_crypto_id", referencedColumnName = "id", nullable = false)
    var receivedCrypto: Ticker,
    @Column(name = "sold_quantity", nullable = false)
    var soldQuantity: BigDecimal,
    @Column(name = "received_quantity", nullable = false)
    var receivedQuantity: BigDecimal,
    @Column(name = "date", nullable = false)
    @Convert(converter = LocalDateTimeStringConverter::class)
    var date: LocalDateTime,
    @Column(name = "description")
    var description: String? = null,
) {
    init {
        soldQuantity = soldQuantity.toRounded()
        receivedQuantity = receivedQuantity.toRounded()

        require(soldQuantity > BigDecimal.ZERO) {
            "Sold quantity must be positive"
        }
        require(receivedQuantity > BigDecimal.ZERO) {
            "Received quantity must be positive"
        }
    }

    override fun toString(): String = "CryptoExchange [id=$id, ${soldCrypto.symbol} -> ${receivedCrypto.symbol}]"
}
