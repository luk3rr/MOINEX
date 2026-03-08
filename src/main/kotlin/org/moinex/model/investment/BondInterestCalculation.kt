/*
 * Filename: BondInterestCalculation.kt (original filename: BondInterestCalculation.java)
 * Created on: February 20, 2026
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
import org.moinex.common.converter.LocalDateStringConverter
import org.moinex.common.converter.LocalDateTimeStringConverter
import org.moinex.common.converter.YearMonthStringConverter
import org.moinex.common.extension.toRounded
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Entity
@Table(name = "bond_interest_calculation")
class BondInterestCalculation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @ManyToOne(optional = false)
    @JoinColumn(name = "bond_id", referencedColumnName = "id", nullable = false)
    var bond: Bond,
    @Column(name = "reference_month", nullable = false)
    @Convert(converter = YearMonthStringConverter::class)
    var referenceMonth: YearMonth,
    @Column(name = "calculation_date", nullable = false)
    @Convert(converter = LocalDateStringConverter::class)
    var calculationDate: LocalDate,
    @Column(name = "calculated_until_date")
    @Convert(converter = LocalDateStringConverter::class)
    var calculatedUntilDate: LocalDate? = null,
    @Column(name = "quantity", nullable = false)
    var quantity: BigDecimal,
    @Column(name = "invested_amount", nullable = false)
    var investedAmount: BigDecimal,
    @Column(name = "monthly_interest", nullable = false)
    var monthlyInterest: BigDecimal,
    @Column(name = "accumulated_interest", nullable = false)
    var accumulatedInterest: BigDecimal,
    @Column(name = "final_value", nullable = false)
    var finalValue: BigDecimal,
    @Column(name = "calculation_method")
    var calculationMethod: String? = null,
    @Column(name = "created_at", nullable = false)
    @Convert(converter = LocalDateTimeStringConverter::class)
    var createdAt: LocalDateTime,
    @Column(name = "manually_adjusted", nullable = false)
    var manuallyAdjusted: Boolean = false,
) {
    init {
        quantity = quantity.toRounded()
        investedAmount = investedAmount.toRounded()
        monthlyInterest = monthlyInterest.toRounded()
        accumulatedInterest = accumulatedInterest.toRounded()
        finalValue = finalValue.toRounded()

        require(quantity >= BigDecimal.ZERO) {
            "Quantity must be non-negative"
        }
        require(investedAmount >= BigDecimal.ZERO) {
            "Invested amount must be non-negative"
        }
    }

    fun isManuallyAdjusted(): Boolean = manuallyAdjusted

    override fun toString(): String = "BondInterestCalculation [id=$id, bond=${bond.name}, month=$referenceMonth]"
}
