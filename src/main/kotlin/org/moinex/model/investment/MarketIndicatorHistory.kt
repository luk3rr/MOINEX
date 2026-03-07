/*
 * Filename: MarketIndicatorHistory.kt (original filename: MarketIndicatorHistory.java)
 * Created on: February 20, 2026
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
import org.moinex.common.converter.LocalDateTimeStringConverter
import org.moinex.model.enums.InterestIndex
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "market_indicator_history")
class MarketIndicatorHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_type", nullable = false)
    var indicatorType: InterestIndex,
    @Column(name = "reference_date", nullable = false)
    @Convert(converter = LocalDateStringConverter::class)
    var referenceDate: LocalDate,
    @Column(name = "rate_value", nullable = false)
    var rateValue: BigDecimal,
    @Column(name = "created_at", nullable = false)
    @Convert(converter = LocalDateTimeStringConverter::class)
    var createdAt: LocalDateTime,
) {
    init {
        require(rateValue >= BigDecimal.ZERO) {
            "Rate value must be non-negative"
        }
    }

    override fun toString(): String = "MarketIndicatorHistory [id=$id, type=$indicatorType, date=$referenceDate]"
}
