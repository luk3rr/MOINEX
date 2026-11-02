/*
 * Filename: BenchmarkPrice.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.investment

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.moinex.common.converter.LocalDateStringConverter
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Closing level of a market index used as a benchmark (for example the Ibovespa, in points), one row per day.
 */
@Entity
@Table(name = "benchmark_price_history")
class BenchmarkPrice(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "symbol", nullable = false)
    var symbol: String,
    @Column(name = "price_date", nullable = false)
    @Convert(converter = LocalDateStringConverter::class)
    var priceDate: LocalDate,
    @Column(name = "closing_price", nullable = false)
    var closingPrice: BigDecimal,
) {
    init {
        require(closingPrice > BigDecimal.ZERO) {
            "Closing price must be positive"
        }
    }

    override fun toString(): String = "BenchmarkPrice [symbol=$symbol, date=$priceDate]"
}
