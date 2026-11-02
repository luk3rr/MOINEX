/*
 * Filename: TickerDetailController.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment.view

import atlantafx.base.controls.SegmentedControl
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.SegmentedFilter
import org.moinex.common.ui.TitledValue
import org.moinex.common.util.FxUtils
import org.moinex.common.util.UIUtils
import org.moinex.model.dto.TickerDetailDTO
import org.moinex.model.dto.TradeMarkerDTO
import org.moinex.model.dto.TradeSide
import org.moinex.model.investment.Ticker
import org.moinex.service.PreferencesService
import org.moinex.service.investment.PortfolioAnalyticsService
import org.moinex.ui.common.DividendChart
import org.moinex.ui.common.TickerPriceChart
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/**
 * Dialog with the price of one ticker over the last months, its average price and the user's buys and sells.
 */
@Controller
@Scope("prototype")
class TickerDetailController(
    private val portfolioAnalyticsService: PortfolioAnalyticsService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var nameLabel: Label

    @FXML
    private lateinit var priceLabel: Label

    @FXML
    private lateinit var dayChangeLabel: Label

    @FXML
    private lateinit var quantityLabel: TitledValue

    @FXML
    private lateinit var averagePriceLabel: TitledValue

    @FXML
    private lateinit var returnLabel: TitledValue

    @FXML
    private lateinit var periodSegment: SegmentedControl

    @FXML
    private lateinit var chartHolder: StackPane

    @FXML
    private lateinit var positionBox: HBox

    @FXML
    private lateinit var incomeSummaryBox: HBox

    @FXML
    private lateinit var incomeChartHolder: StackPane

    @FXML
    private lateinit var tradesTable: TableView<TradeMarkerDTO>

    @FXML
    private lateinit var fundamentalButton: Button

    private lateinit var ticker: Ticker
    private lateinit var periodFilter: SegmentedFilter<Int>
    private var onOpenFundamentalAnalysis: (() -> Unit)? = null
    private var loadSequence = 0

    companion object {
        private val logger = LoggerFactory.getLogger(TickerDetailController::class.java)
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private const val MONTHS_3 = 3
        private const val MONTHS_6 = 6
        private const val MONTHS_12 = 12
    }

    @FXML
    fun initialize() {
        periodFilter =
            SegmentedFilter(
                periodSegment,
                listOf("3M" to MONTHS_3, "6M" to MONTHS_6, "12M" to MONTHS_12),
            )
        periodFilter.value = MONTHS_12
        periodFilter.onChange { if (::ticker.isInitialized) load() }

        configureTradesTable()
    }

    /**
     * Shows [ticker]. [openFundamentalAnalysis] is run by the fundamental analysis button; without it the button is
     * hidden.
     */
    fun setTicker(
        ticker: Ticker,
        openFundamentalAnalysis: (() -> Unit)?,
    ) {
        this.ticker = ticker
        onOpenFundamentalAnalysis = openFundamentalAnalysis
        fundamentalButton.isVisible = openFundamentalAnalysis != null
        fundamentalButton.isManaged = openFundamentalAnalysis != null
        nameLabel.text = "${ticker.name} · ${ticker.symbol}"
        load()
    }

    @FXML
    fun handleFundamentalAnalysis() {
        onOpenFundamentalAnalysis?.invoke()
    }

    @FXML
    fun handleClose() {
        (nameLabel.scene.window as Stage).close()
    }

    private fun load() {
        val months = periodFilter.value
        val request = ++loadSequence

        FxUtils.launchBackgroundThenUI(
            background = { runCatching { portfolioAnalyticsService.tickerDetail(ticker.id!!, months) } },
            onUI = { result ->
                if (request != loadSequence) return@launchBackgroundThenUI

                result
                    .onSuccess { it?.let(::show) }
                    .onFailure { logger.error("Could not load the detail of {}", ticker, it) }
            },
        )
    }

    private fun show(detail: TickerDetailDTO) {
        priceLabel.text = UIUtils.formatCurrencyDynamic(detail.ticker.currentUnitValue)
        quantityLabel.title = preferencesService.translate(TranslationKeys.TICKER_DETAIL_QUANTITY)
        quantityLabel.value = detail.quantity.stripTrailingZeros().toPlainString()
        averagePriceLabel.title = preferencesService.translate(TranslationKeys.TICKER_DETAIL_AVERAGE_PRICE)
        averagePriceLabel.value = UIUtils.formatCurrencyDynamic(detail.averagePrice)

        showSignedPercent(
            dayChangeLabel,
            detail.dayChangePct,
            " ${preferencesService.translate(TranslationKeys.TICKER_DETAIL_TODAY)}",
        )
        showSignedPercent(
            returnLabel.valueLabel,
            detail.returnPct,
            suffix = " (${UIUtils.formatCurrency(detail.profitLoss)})",
        )
        returnLabel.title = preferencesService.translate(TranslationKeys.TICKER_DETAIL_RETURN)

        chartHolder.children.clear()
        if (detail.prices.size < 2) {
            chartHolder.children.add(
                Label(preferencesService.translate(TranslationKeys.TICKER_DETAIL_NO_HISTORY)).apply {
                    isWrapText = true
                    alignment = Pos.CENTER
                },
            )
        } else {
            chartHolder.children.add(TickerPriceChart.create(detail, preferencesService::translate))
        }

        tradesTable.items.setAll(detail.trades.sortedByDescending { it.date })
        showPosition(detail)
        showIncome(detail)
    }

    private fun showPosition(detail: TickerDetailDTO) {
        val position = detail.position
        positionBox.children.clear()
        positionBox.children.add(
            titled(
                TranslationKeys.TICKER_DETAIL_POSITION_VALUE,
                "${UIUtils.formatCurrency(position.value)} " +
                    "(${UIUtils.formatPercentageForFundamentalAnalysis(position.weightPct)} " +
                    "${preferencesService.translate(TranslationKeys.TICKER_DETAIL_POSITION_WEIGHT)})",
            ),
        )

        if (position.highest12m != null && position.lowest12m != null) {
            positionBox.children.add(
                titled(
                    TranslationKeys.TICKER_DETAIL_POSITION_RANGE,
                    "${UIUtils.formatCurrencyDynamic(position.lowest12m)} – " +
                        UIUtils.formatCurrencyDynamic(position.highest12m),
                ),
            )
        }
        position.distanceFromHighestPct?.let {
            positionBox.children.add(
                Label(
                    "${UIUtils.formatPercentageForFundamentalAnalysis(it)} " +
                        preferencesService.translate(TranslationKeys.TICKER_DETAIL_POSITION_FROM_HIGH),
                ),
            )
        }
    }

    private fun titled(
        titleKey: String,
        value: String,
    ): TitledValue =
        TitledValue().apply {
            title = preferencesService.translate(titleKey)
            this.value = value
        }

    private fun showIncome(detail: TickerDetailDTO) {
        val income = detail.income
        incomeSummaryBox.children.clear()
        incomeChartHolder.children.clear()

        if (!income.hasDividends()) {
            incomeChartHolder.children.add(
                Label(preferencesService.translate(TranslationKeys.TICKER_DETAIL_INCOME_EMPTY)),
            )
            return
        }

        incomeSummaryBox.children.addAll(
            incomeStat(TranslationKeys.TICKER_DETAIL_INCOME_TOTAL12M, UIUtils.formatCurrency(income.total12m)),
            incomeStat(
                TranslationKeys.TICKER_DETAIL_INCOME_TOTAL_RECEIVED,
                UIUtils.formatCurrency(income.totalReceived),
            ),
            incomeStat(
                TranslationKeys.TICKER_DETAIL_INCOME_LAST,
                income.lastAmount?.let { UIUtils.formatCurrency(it) + " · " + income.lastDate?.format(DATE_FORMAT) }
                    ?: Constants.NA_DATA,
            ),
            incomeStat(
                TranslationKeys.TICKER_DETAIL_INCOME_DY_ON_COST,
                income.dyOnCostPct?.let { UIUtils.formatPercentageForFundamentalAnalysis(it) } ?: Constants.NA_DATA,
            ),
        )
        incomeChartHolder.children.add(DividendChart.create(income.panel, preferencesService::translate))
    }

    private fun incomeStat(
        labelKey: String,
        value: String,
    ): VBox =
        VBox(
            2.0,
            Label(preferencesService.translate(labelKey)).apply { styleClass.add("ticker-stat-label") },
            Label(value).apply { styleClass.add("ticker-stat-value") },
        )

    private fun showSignedPercent(
        label: Label,
        value: BigDecimal?,
        suffix: String = "",
        prefix: String = "",
    ) {
        label.styleClass.removeAll(Styles.POSITIVE_BALANCE_STYLE, Styles.NEGATIVE_BALANCE_STYLE)

        if (value == null) {
            label.text = prefix + Constants.NA_DATA
            return
        }

        val sign =
            when {
                value.signum() > 0 -> "+ "
                value.signum() < 0 -> "− "
                else -> ""
            }
        label.text = prefix + sign + UIUtils.formatPercentageForFundamentalAnalysis(value.abs()) + suffix
        if (value.signum() != 0) {
            label.styleClass.add(
                if (value.signum() >
                    0
                ) {
                    Styles.POSITIVE_BALANCE_STYLE
                } else {
                    Styles.NEGATIVE_BALANCE_STYLE
                },
            )
        }
    }

    private fun configureTradesTable() {
        tradesTable.columns.addAll(
            textColumn(TranslationKeys.TICKER_DETAIL_TRADES_DATE) { it.date.format(DATE_FORMAT) },
            textColumn(TranslationKeys.TICKER_DETAIL_TRADES_TYPE) {
                preferencesService.translate(
                    if (it.side ==
                        TradeSide.BUY
                    ) {
                        TranslationKeys.TICKER_DETAIL_BUY
                    } else {
                        TranslationKeys.TICKER_DETAIL_SELL
                    },
                )
            },
            textColumn(
                TranslationKeys.TICKER_DETAIL_TRADES_QUANTITY,
            ) { it.quantity.stripTrailingZeros().toPlainString() },
            textColumn(TranslationKeys.TICKER_DETAIL_TRADES_PRICE) { UIUtils.formatCurrencyDynamic(it.price) },
        )
    }

    private fun textColumn(
        headerKey: String,
        extractor: (TradeMarkerDTO) -> String,
    ): TableColumn<TradeMarkerDTO, String> =
        TableColumn<TradeMarkerDTO, String>(preferencesService.translate(headerKey)).apply {
            setCellValueFactory { SimpleStringProperty(extractor(it.value)) }
            cellFactory = UIUtils.createCellFactory(Pos.CENTER_LEFT)
        }
}
