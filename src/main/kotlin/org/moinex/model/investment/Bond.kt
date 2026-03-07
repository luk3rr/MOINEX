/*
 * Filename: Bond.kt (original filename: Bond.java)
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model.investment

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.moinex.common.converter.LocalDateStringConverter
import org.moinex.model.enums.BondType
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "bond")
class Bond(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "symbol")
    var symbol: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: BondType,
    @Column(name = "issuer")
    var issuer: String? = null,
    @Column(name = "maturity_date")
    @Convert(converter = LocalDateStringConverter::class)
    var maturityDate: LocalDate? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type")
    var interestType: InterestType? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "interest_index")
    var interestIndex: InterestIndex? = null,
    @Column(name = "interest_rate")
    var interestRate: BigDecimal? = null,
    @Column(name = "archived", nullable = false)
    var archived: Boolean = false,
) {
    init {
        name = name.trim()
        symbol = symbol?.trim()?.uppercase()
        issuer = issuer?.trim()

        require(name.isNotEmpty()) {
            "Bond name cannot be empty"
        }
        interestRate?.let {
            require(it >= BigDecimal.ZERO) {
                "Interest rate must be non-negative"
            }
        }
    }

    override fun toString(): String = "Bond [id=$id, name='$name', type=$type]"
}
