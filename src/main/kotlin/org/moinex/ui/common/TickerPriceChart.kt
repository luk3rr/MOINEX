/*
 * Filename: TickerPriceChart.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common

import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.layout.StackPane
import javafx.util.StringConverter
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.ChartCrosshair
import org.moinex.common.ui.ChartMarker
import org.moinex.common.ui.CrosshairSeries
import org.moinex.common.util.UIUtils
import org.moinex.model.dto.TickerDetailDTO
import org.moinex.model.dto.TradeSide
import java.math.BigDecimal
import java.text.MessageFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Line chart of a ticker: closing prices, a dashed line at the average price, and a triangle for each buy (up) and
 * sell (down) of the user. The chart styling is in `ticker-price-chart.css`.
 */
object TickerPriceChart {
    private const val STYLE_CLASS = "ticker-price-chart"
    private const val BUY_CLASS = "trade-buy"
    private const val SELL_CLASS = "trade-sell"
    private const val TICK_COUNT = 6
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM")
    private val TOOLTIP_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun create(
        detail: TickerDetailDTO,
        translate: (String) -> String,
    ): StackPane {
        val firstDay =
            detail.prices
                .first()
                .date
                .toEpochDay()
                .toDouble()
        val lastDay =
            detail.prices
                .last()
                .date
                .toEpochDay()
                .toDouble()
        val span = (lastDay - firstDay).coerceAtLeast(1.0)

        val xAxis =
            NumberAxis(firstDay, lastDay, (span / TICK_COUNT).coerceAtLeast(1.0)).apply {
                isMinorTickVisible = false
                tickLabelFormatter =
                    object : StringConverter<Number>() {
                        override fun toString(value: Number): String =
                            LocalDate.ofEpochDay(value.toLong()).format(DATE_FORMAT)

                        override fun fromString(string: String): Number = 0
                    }
            }
        val yAxis = NumberAxis().apply { isForceZeroInRange = false }

        val prices = XYChart.Series<Number, Number>()
        detail.prices.forEach { prices.data.add(XYChart.Data(it.date.toEpochDay(), it.price)) }

        val average = XYChart.Series<Number, Number>()
        average.data.add(XYChart.Data(firstDay, detail.averagePrice))
        average.data.add(XYChart.Data(lastDay, detail.averagePrice))

        val chart =
            LineChart(xAxis, yAxis).apply {
                data.addAll(prices, average)
                isLegendVisible = false
                animated = false
                createSymbols = false
                UIUtils.applyDefaultChartStyle(this, STYLE_CLASS)
                stylesheets.add(
                    TickerPriceChart::class.java.getResource(Files.TICKER_PRICE_CHART_STYLE_SHEET)?.toExternalForm()
                        ?: error("Ticker price chart stylesheet not found"),
                )
            }

        val markers =
            detail.trades.map { trade ->
                ChartMarker(
                    date = trade.date,
                    value = trade.price.toDouble(),
                    styleClass = if (trade.side == TradeSide.BUY) BUY_CLASS else SELL_CLASS,
                    pointsUp = trade.side == TradeSide.BUY,
                    tooltip = tooltipText(trade.date, trade.side, trade.quantity, trade.price, translate),
                )
            }
        val notes =
            detail.trades.map {
                it.date to tooltipText(it.date, it.side, it.quantity, it.price, translate)
            }

        return ChartCrosshair.wrap(
            chart,
            STYLE_CLASS,
            listOf(
                CrosshairSeries(
                    translate(TranslationKeys.TICKER_DETAIL_PRICE),
                    "crosshair-series-price",
                    detail.prices.map { it.date to it.price.toDouble() },
                ) { UIUtils.formatCurrencyDynamic(BigDecimal.valueOf(it)) },
                CrosshairSeries(
                    translate(TranslationKeys.TICKER_DETAIL_AVERAGE_PRICE),
                    "crosshair-series-average",
                    detail.prices.map { it.date to detail.averagePrice.toDouble() },
                ) { UIUtils.formatCurrencyDynamic(BigDecimal.valueOf(it)) },
            ),
            markers,
            notes,
        )
    }

    private fun tooltipText(
        date: LocalDate,
        side: TradeSide,
        quantity: BigDecimal,
        price: BigDecimal,
        translate: (String) -> String,
    ): String {
        val label =
            translate(
                if (side == TradeSide.BUY) {
                    TranslationKeys.TICKER_DETAIL_BUY
                } else {
                    TranslationKeys.TICKER_DETAIL_SELL
                },
            )

        return MessageFormat.format(
            "{0} {1}: {2} @ {3}",
            label,
            date.format(TOOLTIP_DATE_FORMAT),
            quantity.stripTrailingZeros().toPlainString(),
            UIUtils.formatCurrencyDynamic(price),
        )
    }
}
