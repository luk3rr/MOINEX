/*
 * Filename: CreditCardTransaction.kt (original filename: CreditCardTransaction.java)
 * Created on: March  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 28/02/2026
 */

package org.moinex.model.creditcard

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import org.moinex.common.LocalDateTimeStringConverter
import java.math.BigDecimal
import java.time.LocalDateTime

@MappedSuperclass
class CreditCardTransaction(
    @ManyToOne
    @JoinColumn(name = "crc_id", referencedColumnName = "id", nullable = false)
    open var creditCard: CreditCard,
    @Column(name = "date", nullable = false)
    @Convert(converter = LocalDateTimeStringConverter::class)
    open var date: LocalDateTime,
    @Column(name = "amount", nullable = false, scale = 2)
    open var amount: BigDecimal,
    @Column(name = "description")
    open var description: String?,
) {
    init {
        require(amount > BigDecimal.ZERO) { "Amount must be positive" }
    }
}
