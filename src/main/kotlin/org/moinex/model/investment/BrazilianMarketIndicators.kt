/*
 * Filename: BrazilianMarketIndicators.kt (original filename: BrazilianMarketIndicators.java)
 * Created on: January 17, 2025
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
import jakarta.persistence.Table
import org.moinex.common.converter.LocalDateTimeStringConverter
import org.moinex.common.converter.YearMonthStringConverter
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

@Entity
@Table(name = "brazilian_market_indicators")
class BrazilianMarketIndicators(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "selic_target")
    var selicTarget: BigDecimal? = null,
    @Column(name = "ipca_last_month_rate")
    var ipcaLastMonth: BigDecimal? = null,
    @Column(name = "ipca_last_month_reference")
    @Convert(converter = YearMonthStringConverter::class)
    var ipcaLastMonthReference: YearMonth? = null,
    @Column(name = "ipca_12_months")
    var ipca12Months: BigDecimal? = null,
    @Column(name = "last_update")
    @Convert(converter = LocalDateTimeStringConverter::class)
    var lastUpdate: LocalDateTime? = null,
) {
    override fun toString(): String = "BrazilianMarketIndicators [id=$id, lastUpdate=$lastUpdate]"
}
