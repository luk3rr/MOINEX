package org.moinex.model.creditcard

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.moinex.model.enums.CreditCardCreditType
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "credit_card_credit")
class CreditCardCredit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: CreditCardCreditType,
    creditCard: CreditCard,
    date: LocalDateTime,
    amount: BigDecimal,
    description: String? = null,
) : CreditCardTransaction(
        creditCard = creditCard,
        date = date,
        amount = amount,
        description = description,
    ) {
    override fun toString(): String {
        return "Credit Card Credit [id=$id, type=$type, amount=$amount, date=$date, " +
            "description=$description, creditCardId=${creditCard.id}]"
    }
}
