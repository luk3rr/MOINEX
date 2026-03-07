/*
 * Filename: TickerPriceHistory.kt (original filename: TickerPriceHistory.java)
 * Created on: February 17, 2026
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
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Entity that stores historical price data for tickers
 * Used to calculate capital appreciation/depreciation over time
 */
@Entity
@Table(name = "ticker_price_history")
class TickerPriceHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @ManyToOne
    @JoinColumn(name = "ticker_id", nullable = false)
    var ticker: Ticker,
    @Column(name = "price_date", nullable = false)
    @Convert(converter = LocalDateStringConverter::class)
    var priceDate: LocalDate,
    @Column(name = "closing_price", nullable = false)
    var closingPrice: BigDecimal,
    @Column(name = "is_month_end", nullable = false)
    var isMonthEnd: Boolean = false,
) {
    init {
        require(closingPrice >= BigDecimal.ZERO) {
            "Closing price must be non-negative"
        }
    }

    override fun toString(): String = "TickerPriceHistory [id=$id, ticker=${ticker.symbol}, date=$priceDate]"
}
