/*
 * Filename: Transfer.kt (original filename: Transfer.java)
 * Created on: August 25, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.model.wallettransaction

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.moinex.common.LocalDateTimeStringConverter
import org.moinex.common.toRounded
import org.moinex.model.Category
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "transfer")
class Transfer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @ManyToOne
    @JoinColumn(name = "sender_wallet_id", referencedColumnName = "id", nullable = false)
    var senderWallet: Wallet,
    @ManyToOne
    @JoinColumn(name = "receiver_wallet_id", referencedColumnName = "id", nullable = false)
    var receiverWallet: Wallet,
    @Column(name = "date", nullable = false)
    @Convert(converter = LocalDateTimeStringConverter::class)
    var date: LocalDateTime,
    @Column(name = "amount", nullable = false, scale = 2)
    var amount: BigDecimal,
    @Column(name = "description")
    var description: String? = null,
    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id", nullable = false)
    var category: Category,
) {
    init {
        amount = amount.toRounded()
        require(amount > BigDecimal.ZERO) { "Amount must be positive" }
        require(senderWallet != receiverWallet) { "Sender and receiver wallets must be different" }
    }

    override fun toString(): String = "Transfer [id=$id, amount=$amount, from='$senderWallet', to='$receiverWallet']"
}
