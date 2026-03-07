/*
 * Filename: FundamentalAnalysis.kt (original filename: FundamentalAnalysis.java)
 * Created on: January  9, 2026
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
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.moinex.common.converter.LocalDateTimeStringConverter
import org.moinex.model.enums.PeriodType
import java.time.LocalDateTime

@Entity
@Table(name = "fundamental_analysis")
class FundamentalAnalysis(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    var ticker: Ticker,
    @Column(name = "company_name")
    var companyName: String? = null,
    @Column(name = "sector")
    var sector: String? = null,
    @Column(name = "industry")
    var industry: String? = null,
    @Column(name = "currency", nullable = false)
    var currency: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    var periodType: PeriodType,
    @Column(name = "data_json", columnDefinition = "TEXT", nullable = false)
    var dataJson: String,
    @Column(name = "last_update", nullable = false)
    @Convert(converter = LocalDateTimeStringConverter::class)
    var lastUpdate: LocalDateTime,
    @Column(name = "created_at", nullable = false)
    @Convert(converter = LocalDateTimeStringConverter::class)
    var createdAt: LocalDateTime,
) {
    init {
        currency = currency.trim().uppercase()

        require(currency.isNotEmpty()) {
            "Currency cannot be empty"
        }
        require(dataJson.isNotEmpty()) {
            "Data JSON cannot be empty"
        }
    }

    override fun toString(): String = "FundamentalAnalysis [id=$id, ticker=${ticker.symbol}, period=$periodType]"
}
