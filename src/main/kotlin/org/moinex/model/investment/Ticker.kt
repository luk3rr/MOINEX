/*
 * Filename: Ticker.kt (original filename: Ticker.java)
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
import org.moinex.common.converter.LocalDateTimeStringConverter
import org.moinex.model.enums.TickerType
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "ticker")
class Ticker(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: TickerType,
    @Column(name = "last_update", nullable = false)
    @Convert(converter = LocalDateTimeStringConverter::class)
    var lastUpdate: LocalDateTime,
    @Column(name = "created_at", nullable = false)
    @Convert(converter = LocalDateTimeStringConverter::class)
    var createdAt: LocalDateTime,
    @Column(name = "archived", nullable = false)
    var isArchived: Boolean = false,
    @Column(name = "domain")
    var domain: String? = null,
    name: String,
    symbol: String,
    currentQuantity: BigDecimal,
    currentUnitValue: BigDecimal,
    averageUnitValue: BigDecimal,
    averageUnitValueCount: BigDecimal = BigDecimal.ONE,
) : Asset(
        name = name,
        symbol = symbol,
        currentQuantity = currentQuantity,
        currentUnitValue = currentUnitValue,
        averageUnitValue = averageUnitValue,
        averageUnitValueCount = averageUnitValueCount,
    ) {
    override fun toString(): String = "Ticker [id=$id, symbol='$symbol', type=$type]"
}
