package org.moinex.model.creditcard

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "credit_card_operator")
class CreditCardOperator(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "name", nullable = false, length = 50, unique = true)
    var name: String,
    @Column(name = "icon", length = 30)
    var icon: String? = null,
) {
    init {
        name = name.trim()
        require(name.isNotBlank()) { "Credit card operator name cannot be empty" }
    }

    override fun toString(): String = "Credit Card Operator [id=$id, name='$name']"
}
