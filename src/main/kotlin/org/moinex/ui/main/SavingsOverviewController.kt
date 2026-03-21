/*
 * Filename: SavingsOverviewController.kt (original filename: SavingsOverviewController.java)
 * Created on: February 18, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 17/03/2026
 */

package org.moinex.ui.main

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.PieChart
import javafx.scene.chart.StackedBarChart
import javafx.scene.chart.XYChart
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.moinex.common.chart.ChartFactory
import org.moinex.common.chart.DoughnutChart
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.extension.setAnchorPaneConstraints
import org.moinex.common.util.AnimationUtils
import org.moinex.common.util.FxUtils
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.dto.AllocationDTO
import org.moinex.model.dto.ProfitabilityMetricsDTO
import org.moinex.model.dto.TickerPerformanceDTO
import org.moinex.model.enums.AssetType
import org.moinex.model.investment.BrazilianMarketIndicators
import org.moinex.model.investment.Dividend
import org.moinex.model.investment.MarketQuotesAndCommodities
import org.moinex.model.investment.Ticker
import org.moinex.service.PreferencesService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.InvestmentPerformanceService
import org.moinex.service.investment.InvestmentTargetService
import org.moinex.service.investment.MarketService
import org.moinex.service.investment.TickerService
import org.moinex.ui.common.ProfitabilityMetricsPaneController
import org.moinex.ui.dialog.investment.update.EditInvestmentTargetController
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.util.concurrent.atomic.AtomicInteger

@Controller
class SavingsOverviewController(
    private val tickerService: TickerService,
    private val marketService: MarketService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
    private val investmentTargetService: InvestmentTargetService,
    private val bondService: BondService,
    private val investmentPerformanceService: InvestmentPerformanceService,
    private val chartFactory: ChartFactory,
) {
    @FXML
    private lateinit var overviewTotalInvestedField: Text

    @FXML
    private lateinit var overviewTabGainsField: Text

    @FXML
    private lateinit var overviewTabLossesField: Text

    @FXML
    private lateinit var overviewTabTotalValueField: Text

    @FXML
    private lateinit var overviewTabSelicValueField: Label

    @FXML
    private lateinit var overviewTabIPCALastMonthValueField: Label

    @FXML
    private lateinit var overviewTabIPCALastMonthDescriptionField: Label

    @FXML
    private lateinit var overviewTabIPCA12MonthsValueField: Label

    @FXML
    private lateinit var overviewTabDollarValueField: Label

    @FXML
    private lateinit var overviewTabEuroValueField: Label

    @FXML
    private lateinit var overviewTabIbovespaValueField: Label

    @FXML
    private lateinit var overviewTabBitcoinValueField: Label

    @FXML
    private lateinit var overviewTabEthereumValueField: Label

    @FXML
    private lateinit var overviewTabGoldValueField: Label

    @FXML
    private lateinit var overviewTabSoybeanValueField: Label

    @FXML
    private lateinit var overviewTabCoffeeValueField: Label

    @FXML
    private lateinit var overviewTabWheatValueField: Label

    @FXML
    private lateinit var overviewTabOilBrentValueField: Label

    @FXML
    private lateinit var brazilianMarketIndicatorsLastUpdateValue: Label

    @FXML
    private lateinit var marketQuotesLastUpdateValue: Label

    @FXML
    private lateinit var commoditiesLastUpdateValue: Label

    @FXML
    private lateinit var pieChartAnchorPane: AnchorPane

    @FXML
    private lateinit var graphPrevButton: Button

    @FXML
    private lateinit var graphNextButton: Button

    @FXML
    private lateinit var recalculateInvestmentPerformanceButton: Button

    @FXML
    private lateinit var recalculateInvestmentPerformanceButtonIcon: ImageView

    @FXML
    private lateinit var overviewTabBottonPaneTitle: Label

    @FXML
    private lateinit var portfolioP2: HBox

    @FXML
    private lateinit var portfolioP5: HBox

    @FXML
    private lateinit var profitabilityMetricsPaneController: ProfitabilityMetricsPaneController

    private lateinit var tickers: List<Ticker>
    private lateinit var dividends: List<Dividend>
    private var brazilianMarketIndicators: BrazilianMarketIndicators? = null
    private var marketQuotesAndCommodities: MarketQuotesAndCommodities? = null

    private val scheduledUpdatingMarketQuotesRetries = AtomicInteger(0)
    private var scheduledUpdatingMarketQuotesJob: Job? = null
    private val scheduledUpdatingBrazilianIndicatorsRetries = AtomicInteger(0)
    private var scheduledUpdatingBrazilianIndicatorsJob: Job? = null

    private var graphPaneCurrentPage = 0

    companion object {
        private val logger = LoggerFactory.getLogger(SavingsOverviewController::class.java)
        private const val SCHEDULE_DELAY_IN_SECONDS = 30
        private const val MAX_RETRIES = 3
        private const val TOP_PERFORMERS_LIMIT = 5
        private const val ALLOCATION_PANEL_CONTAINER_SPACING = 10
        private const val ALLOCATION_PANEL_COLUMNS_SPACING = 20
        private const val ALLOCATION_PANEL_ITEMS_SPACING = 8
        private const val ALLOCATION_BAR_CONTAINER_SPACING = 3
        private const val ALLOCATION_INFO_BOX_SPACING = 10
        private const val ALLOCATION_PROGRESS_BAR_HEIGHT = 10.0
        private const val ALLOCATION_FILLED_BAR_HEIGHT = 20.0
        private const val HUNDRED = 100.0
    }

    @FXML
    fun initialize() {
        loadBrazilianMarketIndicators()
        loadMarketQuotesAndCommodities()

        updateOverviewTabFields()
        updateBrazilianMarketIndicators()
        updateMarketQuotesAndCommodities()
        updateTopPerformersPanel()
        updateAllocationVsTargetPanel()
        updateProfitabilityMetricsPanel()
        updateDisplayGraphs()

        setGraphButtonsActions()
    }

    @FXML
    fun handleEditInvestmentTarget() {
        WindowUtils.openModalWindow(
            Constants.EDIT_INVESTMENT_TARGET_FXML,
            preferencesService.translate(
                TranslationKeys.INVESTMENT_DIALOG_EDIT_TARGET_TITLE,
            ),
            springContext,
            { _: EditInvestmentTargetController -> },
            listOf(Runnable { updateAllocationVsTargetPanel() }),
        )
    }

    @FXML
    fun handleRecalculateInvestmentPerformance() {
        setOffRecalculateInvestmentPerformanceButton()

        FxUtils.launchBackgroundThenUI(
            background = {
                runCatching {
                    investmentPerformanceService.recalculateAllSnapshots()
                }.onFailure { ex ->
                    logger.error("Error during investment performance recalculation", ex)
                }
            },
            onUI = {
                logger.info("Investment performance recalculation completed, updating chart...")
                updateInvestmentPerformanceChart()
                setOnRecalculateInvestmentPerformanceButton()
            },
        )
    }

    private fun loadBrazilianMarketIndicators() {
        FxUtils.launchBackgroundThenUI(
            background = {
                runCatching {
                    marketService.getBrazilianMarketIndicatorsOrFetch()
                }.onSuccess {
                    logger.info("Loaded Brazilian market indicators")
                }.onFailure { ex ->
                    logger.error("Failed to load Brazilian market indicators: {}", ex.message)
                }
            },
            onUI = { result ->
                result
                    .onSuccess { bmi ->
                        brazilianMarketIndicators = bmi
                        scheduledUpdatingBrazilianIndicatorsRetries.set(0)
                    }.onFailure {
                        schedulerRetryForUpdatingBrazilianIndicators()
                    }
            },
        )
    }

    private fun loadMarketQuotesAndCommodities() {
        FxUtils.launchBackgroundThenUI(
            background = {
                runCatching {
                    marketService.getMarketQuotesAndCommoditiesOrFetch()
                }.onSuccess {
                    logger.info("Loaded market quotes and commodities")
                }.onFailure { ex ->
                    logger.error("Failed to load market quotes and commodities: {}", ex.message)
                }
            },
            onUI = { result ->
                result
                    .onSuccess { mqc ->
                        marketQuotesAndCommodities = mqc
                        scheduledUpdatingMarketQuotesRetries.set(0)
                    }.onFailure {
                        schedulerEntryForUpdatingMarketQuotes()
                    }
            },
        )
    }

    private fun loadTickersFromDatabase() {
        tickers = tickerService.getAllNonArchivedTickers()
    }

    private fun loadDividendsFromDatabase() {
        dividends = tickerService.getAllDividends()
    }

    private fun updateBrazilianMarketIndicators() {
        brazilianMarketIndicators?.let { bmi ->
            overviewTabSelicValueField.text =
                UIUtils.formatPercentage(bmi.selicTarget)

            overviewTabIPCALastMonthValueField.text =
                UIUtils.formatPercentage(bmi.ipcaLastMonth)

            overviewTabIPCALastMonthDescriptionField.text =
                "IPCA ${bmi.ipcaLastMonthReference}"

            overviewTabIPCA12MonthsValueField.text =
                UIUtils.formatPercentage(bmi.ipca12Months)

            brazilianMarketIndicatorsLastUpdateValue.text =
                UIUtils.formatDateTimeForDisplay(bmi.lastUpdate)
        }
    }

    private fun updateMarketQuotesAndCommodities() {
        marketQuotesAndCommodities?.let { mqc ->
            overviewTabDollarValueField.text = UIUtils.formatCurrency(mqc.dollar)
            overviewTabEuroValueField.text = UIUtils.formatCurrency(mqc.euro)
            overviewTabIbovespaValueField.text = UIUtils.formatCurrency(mqc.ibovespa)
            overviewTabBitcoinValueField.text = UIUtils.formatCurrency(mqc.bitcoin)
            overviewTabEthereumValueField.text = UIUtils.formatCurrency(mqc.ethereum)
            overviewTabGoldValueField.text = UIUtils.formatCurrency(mqc.gold)
            overviewTabSoybeanValueField.text = UIUtils.formatCurrency(mqc.soybean)
            overviewTabCoffeeValueField.text = UIUtils.formatCurrency(mqc.coffee)
            overviewTabWheatValueField.text = UIUtils.formatCurrency(mqc.wheat)
            overviewTabOilBrentValueField.text = UIUtils.formatCurrency(mqc.oilBrent)

            marketQuotesLastUpdateValue.text =
                UIUtils.formatDateTimeForDisplay(mqc.lastUpdate)

            commoditiesLastUpdateValue.text =
                UIUtils.formatDateTimeForDisplay(mqc.lastUpdate)
        }
    }

    private fun schedulerEntryForUpdatingMarketQuotes() {
        if (scheduledUpdatingMarketQuotesRetries.get() >= MAX_RETRIES) {
            logger.warn("Max retries reached for updating market quotes")
            return
        }

        if (scheduledUpdatingMarketQuotesJob?.isActive == true) {
            logger.warn("Already scheduled to update market quotes")
            return
        }

        scheduledUpdatingMarketQuotesRetries.incrementAndGet()

        logger.info("Scheduling retry for updating market quotes")

        scheduledUpdatingMarketQuotesJob =
            FxUtils.launchOnBackground {
                delay(SCHEDULE_DELAY_IN_SECONDS * 1000L)
                loadMarketQuotesAndCommodities()
            }
    }

    private fun schedulerRetryForUpdatingBrazilianIndicators() {
        if (scheduledUpdatingBrazilianIndicatorsRetries.get() >= MAX_RETRIES) {
            logger.warn("Max retries reached for updating Brazilian market indicators")
            return
        }

        if (scheduledUpdatingBrazilianIndicatorsJob?.isActive == true) {
            logger.warn("Already scheduled to update Brazilian market indicators")
            return
        }

        scheduledUpdatingBrazilianIndicatorsRetries.incrementAndGet()

        logger.info("Scheduling retry for updating Brazilian market indicators")

        scheduledUpdatingBrazilianIndicatorsJob =
            FxUtils.launchOnBackground {
                delay(SCHEDULE_DELAY_IN_SECONDS * 1000L)
                loadBrazilianMarketIndicators()
            }
    }

    private fun updateInvestmentDistributionChart() {
        pieChartAnchorPane.children.clear()

        val investmentByType = calculateInvestmentDistributionByType()

        if (investmentByType.isEmpty()) {
            return
        }

        val totalInvestment = investmentByType.values.fold(BigDecimal.ZERO, BigDecimal::add)

        val pieChartData =
            FXCollections.observableArrayList(
                investmentByType
                    .filter { it.value > BigDecimal.ZERO }
                    .map { PieChart.Data(it.key, it.value.toDouble()) },
            )

        val doughnutChart = chartFactory.createDoughnutChart(pieChartData)
        doughnutChart.labelsVisible = false

        doughnutChart.data.forEach { data ->
            val value = BigDecimal(data.pieValue)
            val percentage =
                value
                    .divide(totalInvestment, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(HUNDRED))

            val tooltipText =
                "${data.name}\n${UIUtils.formatCurrency(value)} " +
                    "(${UIUtils.formatPercentage(percentage)})"

            UIUtils.addTooltipToNode(data.node, tooltipText)
        }

        UIUtils.applyDefaultChartStyle(doughnutChart)

        pieChartAnchorPane.children.removeIf { it is DoughnutChart }
        pieChartAnchorPane.children.add(doughnutChart)
        doughnutChart.setAnchorPaneConstraints()
    }

    private fun calculateInvestmentDistributionByType(): Map<String, BigDecimal> {
        val investmentByType = mutableMapOf<String, BigDecimal>()

        val allTickers = tickerService.getAllNonArchivedTickers()
        val allBonds = bondService.getAllNonArchivedBonds()

        allTickers.forEach { ticker ->
            val tickerCurrentValue = ticker.currentQuantity.multiply(ticker.currentUnitValue)
            val typeName = UIUtils.translateAssetType(ticker.type)
            investmentByType.merge(typeName, tickerCurrentValue, BigDecimal::add)
        }

        allBonds.forEach { bond ->
            val bondInvestedValue = bondService.getTotalInvestedValue(bond)
            val bondAccumulatedInterest = bondService.getTotalAccumulatedInterestByBondId(bond.id!!)
            val bondTotalValue = bondInvestedValue.add(bondAccumulatedInterest)
            val typeName = UIUtils.translateBondType(bond.type)
            investmentByType.merge(typeName, bondTotalValue, BigDecimal::add)
        }

        return investmentByType
    }

    private fun setGraphButtonsActions() {
        graphPrevButton.setOnAction {
            if (graphPaneCurrentPage > 0) {
                graphPaneCurrentPage--
                updateDisplayGraphs()
            }
        }

        graphNextButton.setOnAction {
            if (graphPaneCurrentPage < 1) {
                graphPaneCurrentPage++
                updateDisplayGraphs()
            }
        }
    }

    private fun updateDisplayGraphs() {
        pieChartAnchorPane.children.clear()

        when (graphPaneCurrentPage) {
            0 -> {
                overviewTabBottonPaneTitle.text =
                    preferencesService.translate(TranslationKeys.SAVINGS_OVERVIEW_PORTFOLIO)
                updateInvestmentDistributionChart()
                recalculateInvestmentPerformanceButton.isVisible = false
                recalculateInvestmentPerformanceButton.isManaged = false
            }

            1 -> {
                overviewTabBottonPaneTitle.text =
                    preferencesService.translate(
                        TranslationKeys.SAVINGS_INVESTMENT_PERFORMANCE,
                    )
                updateInvestmentPerformanceChart()
                recalculateInvestmentPerformanceButton.isVisible = true
                recalculateInvestmentPerformanceButton.isManaged = true
            }
        }

        graphPrevButton.isDisable = graphPaneCurrentPage == 0
        graphNextButton.isDisable = graphPaneCurrentPage >= 1
    }

    private fun setOffRecalculateInvestmentPerformanceButton() {
        recalculateInvestmentPerformanceButtonIcon.image =
            Image(javaClass.getResource(Constants.LOADING_GIF)!!.toExternalForm())
        recalculateInvestmentPerformanceButton.isDisable = true
        recalculateInvestmentPerformanceButton.text =
            preferencesService.translate(TranslationKeys.SAVINGS_BUTTON_RECALCULATING)
    }

    private fun setOnRecalculateInvestmentPerformanceButton() {
        recalculateInvestmentPerformanceButton.isDisable = false
        recalculateInvestmentPerformanceButtonIcon.image =
            Image(javaClass.getResource(Constants.RELOAD_ICON)!!.toExternalForm())
        recalculateInvestmentPerformanceButton.text =
            preferencesService.translate(TranslationKeys.SAVINGS_BUTTON_RECALCULATE)
    }

    private fun updateInvestmentPerformanceChart() {
        val xAxis = CategoryAxis()
        val yAxis = NumberAxis()

        val stackedBarChart = StackedBarChart(xAxis, yAxis)
        stackedBarChart.isLegendVisible = true
        stackedBarChart.verticalGridLinesVisible = false

        pieChartAnchorPane.children.setAll(stackedBarChart)
        stackedBarChart.setAnchorPaneConstraints()

        stackedBarChart.data.clear()

        val investedSeries = XYChart.Series<String, Number>()
        investedSeries.name =
            preferencesService.translate(TranslationKeys.SAVINGS_INVESTED_VALUE)

        val capitalGainsSeries = XYChart.Series<String, Number>()
        capitalGainsSeries.name =
            preferencesService.translate(
                TranslationKeys.SAVINGS_ACCUMULATED_CAPITAL_GAINS,
            )

        val performanceData = investmentPerformanceService.getPerformanceData()

        val monthlyInvested = performanceData.monthlyInvested
        val accumulatedGains = performanceData.accumulatedGains
        val monthlyGains = performanceData.monthlyGains
        val portfolioValues = performanceData.portfolioValues

        val currentMonth = YearMonth.now()
        val allMonths =
            (Constants.XYBAR_CHART_MONTHS downTo 0).map { currentMonth.minusMonths(it.toLong()) }

        val formatter = UIUtils.getShortMonthYearFormatter(preferencesService.locale)

        var lastInvested = BigDecimal.ZERO
        var lastAccumulatedGains = BigDecimal.ZERO

        allMonths.forEach { month ->
            val monthLabel = month.format(formatter)

            val invested = monthlyInvested.getOrDefault(month, lastInvested)
            val gains = accumulatedGains.getOrDefault(month, lastAccumulatedGains)

            if (monthlyInvested.containsKey(month)) {
                lastInvested = invested
            }
            if (accumulatedGains.containsKey(month)) {
                lastAccumulatedGains = gains
            }

            investedSeries.data.add(XYChart.Data(monthLabel, invested.toDouble()))
            capitalGainsSeries.data.add(XYChart.Data(monthLabel, gains.toDouble()))
        }

        stackedBarChart.data.addAll(investedSeries, capitalGainsSeries)

        val maxTotal =
            portfolioValues.values
                .maxOfOrNull { it.toDouble() } ?: 0.0

        AnimationUtils.setDynamicYAxisBounds(yAxis, maxTotal)

        UIUtils.formatCurrencyYAxis(yAxis)
        UIUtils.applyDefaultChartStyle(stackedBarChart)

        stackedBarChart.layout()

        var previousPortfolio = BigDecimal.ZERO

        investedSeries.data.forEachIndexed { i, investedData ->
            val gainsData = capitalGainsSeries.data[i]

            val yearMonth = allMonths[i]
            val portfolio = portfolioValues.getOrDefault(yearMonth, BigDecimal.ZERO)
            val invested = BigDecimal.valueOf(investedData.yValue as Double)
            val accumulatedGain = accumulatedGains.getOrDefault(yearMonth, BigDecimal.ZERO)
            val monthlyGain = monthlyGains.getOrDefault(yearMonth, BigDecimal.ZERO)

            var tooltipText =
                buildString {
                    append(
                        preferencesService.translate(
                            TranslationKeys.SAVINGS_OVERVIEW_PORTFOLIO,
                        ),
                    )
                    append(": ")
                    append(UIUtils.formatCurrency(portfolio))
                    append("\n")
                    append(
                        preferencesService.translate(
                            TranslationKeys.SAVINGS_INVESTED_VALUE,
                        ),
                    )
                    append(": ")
                    append(UIUtils.formatCurrency(invested))
                    append("\n")
                    append(
                        preferencesService.translate(
                            TranslationKeys.SAVINGS_ACCUMULATED_CAPITAL_GAINS,
                        ),
                    )
                    append(": ")
                    append(UIUtils.formatCurrency(accumulatedGain))
                    append("\n")
                    append(
                        preferencesService.translate(
                            TranslationKeys.SAVINGS_MONTHLY_CAPITAL_GAINS,
                        ),
                    )
                    append(": ")
                    append(UIUtils.formatCurrency(monthlyGain))
                }

            if (i > 0 && previousPortfolio > BigDecimal.ZERO) {
                val change = portfolio.subtract(previousPortfolio)
                val percentageChange =
                    change
                        .divide(previousPortfolio, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal(HUNDRED))

                val sign = if (percentageChange >= BigDecimal.ZERO) "+ " else "- "
                tooltipText +=
                    "\n${preferencesService.translate(TranslationKeys.SAVINGS_VARIATION)}: " +
                    "$sign${UIUtils.formatPercentage(percentageChange)}"
            }

            investedData.node?.let { UIUtils.addTooltipToNode(it, tooltipText) }
            gainsData.node?.let { UIUtils.addTooltipToNode(it, tooltipText) }

            previousPortfolio = portfolio
        }
    }

    private fun updateOverviewTabFields() {
        loadTickersFromDatabase()
        loadDividendsFromDatabase()

        logger.info(
            "Overview calculation: tickers={}, dividends={}",
            tickers.size,
            dividends.size,
        )

        var totalInvested =
            tickers
                .map { it.averageUnitValue.multiply(it.currentQuantity) }
                .fold(BigDecimal.ZERO, BigDecimal::add)

        var portfolioCurrentValue =
            tickers
                .map { it.currentQuantity.multiply(it.currentUnitValue) }
                .fold(BigDecimal.ZERO, BigDecimal::add)

        var tickerUnrealizedGains = BigDecimal.ZERO
        var tickerUnrealizedLosses = BigDecimal.ZERO

        tickers.forEach { ticker ->
            val invested = ticker.averageUnitValue.multiply(ticker.currentQuantity)
            val current = ticker.currentQuantity.multiply(ticker.currentUnitValue)
            val profitLoss = current.subtract(invested)

            logger.debug(
                "Overview ticker {} ({}): qty={}, avg={}, current={}, invested={}, value={}, pl={}",
                ticker.symbol,
                ticker.name,
                ticker.currentQuantity,
                ticker.averageUnitValue,
                ticker.currentUnitValue,
                invested,
                current,
                profitLoss,
            )

            if (profitLoss > BigDecimal.ZERO) {
                tickerUnrealizedGains = tickerUnrealizedGains.add(profitLoss)
            } else {
                tickerUnrealizedLosses = tickerUnrealizedLosses.add(profitLoss.abs())
            }
        }

        logger.info(
            "Overview tickers unrealized: gains={}, losses={} (gross)",
            tickerUnrealizedGains,
            tickerUnrealizedLosses,
        )

        val bondsTotalInvested = bondService.getTotalInvestedValue()
        val bondsAccumulatedInterest = bondService.getAllBondsTotalAccumulatedInterest()

        logger.info(
            "Overview bonds: investedValue={}, currentValueAssumed={}, accumulatedInterest={}",
            bondsTotalInvested,
            bondsTotalInvested,
            bondsAccumulatedInterest,
        )

        totalInvested = totalInvested.add(bondsTotalInvested)
        portfolioCurrentValue = portfolioCurrentValue.add(bondsTotalInvested).add(bondsAccumulatedInterest)

        val totalDividends =
            dividends
                .map { it.walletTransaction!!.amount }
                .fold(BigDecimal.ZERO, BigDecimal::add)

        logger.info("Overview dividends: count={}, total={}", dividends.size, totalDividends)

        val tickerSales = tickerService.getAllNonArchivedSales()
        val tickerRealizedProfit =
            tickerSales
                .map { sale ->
                    val saleValue = sale.unitPrice.multiply(sale.quantity)
                    val costBasis = sale.averageCost.multiply(sale.quantity)
                    saleValue.subtract(costBasis)
                }.fold(BigDecimal.ZERO, BigDecimal::add)

        logger.info(
            "Overview ticker sales: count={}, realizedProfitLoss={}",
            tickerSales.size,
            tickerRealizedProfit,
        )

        var totalGains = tickerUnrealizedGains.add(totalDividends).add(bondsAccumulatedInterest)
        var totalLosses = tickerUnrealizedLosses

        if (tickerRealizedProfit > BigDecimal.ZERO) {
            totalGains = totalGains.add(tickerRealizedProfit)
        } else {
            totalLosses = totalLosses.add(tickerRealizedProfit.abs())
        }

        logger.info(
            "Overview result: totalInvested={}, totalValue={}, gains={}, losses={} (gross)",
            totalInvested,
            portfolioCurrentValue,
            totalGains,
            totalLosses,
        )

        overviewTotalInvestedField.text = UIUtils.formatCurrency(totalInvested)
        overviewTabGainsField.text = UIUtils.formatCurrency(totalGains)
        overviewTabLossesField.text = UIUtils.formatCurrency(totalLosses)
        overviewTabTotalValueField.text = UIUtils.formatCurrency(portfolioCurrentValue)
    }

    private fun calculateProfitabilityMetricsByType(): Map<String, ProfitabilityMetricsDTO> {
        loadTickersFromDatabase()
        loadDividendsFromDatabase()

        val variableInvested =
            tickers
                .map { it.averageUnitValue.multiply(it.currentQuantity) }
                .fold(BigDecimal.ZERO, BigDecimal::add)

        val variableCurrentValue =
            tickers
                .map { it.currentQuantity.multiply(it.currentUnitValue) }
                .fold(BigDecimal.ZERO, BigDecimal::add)

        val variableProfitLoss = variableCurrentValue.subtract(variableInvested)

        val variableReturnPercentage =
            if (variableInvested > BigDecimal.ZERO) {
                variableProfitLoss
                    .divide(variableInvested, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(HUNDRED))
            } else {
                BigDecimal.ZERO
            }

        val totalDividends =
            dividends
                .map { it.walletTransaction!!.amount }
                .fold(BigDecimal.ZERO, BigDecimal::add)

        val variableDividendYield =
            if (variableInvested > BigDecimal.ZERO) {
                totalDividends
                    .divide(variableInvested, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(HUNDRED))
            } else {
                BigDecimal.ZERO
            }

        val fixedInvested = bondService.getTotalInvestedValue()
        val fixedAccumulatedInterest = bondService.getAllBondsTotalAccumulatedInterest()
        val fixedCurrentValue = fixedInvested.add(fixedAccumulatedInterest)

        val fixedReturnPercentage =
            if (fixedInvested > BigDecimal.ZERO) {
                fixedAccumulatedInterest
                    .divide(fixedInvested, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(HUNDRED))
            } else {
                BigDecimal.ZERO
            }

        val totalInvested = variableInvested.add(fixedInvested)
        val totalCurrentValue = variableCurrentValue.add(fixedCurrentValue)
        val totalProfitLoss = variableProfitLoss.add(fixedAccumulatedInterest)

        val totalReturnPercentage =
            if (totalInvested > BigDecimal.ZERO) {
                totalProfitLoss
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(HUNDRED))
            } else {
                BigDecimal.ZERO
            }

        return mapOf(
            "variable" to
                ProfitabilityMetricsDTO(
                    variableInvested,
                    variableCurrentValue,
                    variableProfitLoss,
                    variableReturnPercentage,
                    variableDividendYield,
                    totalDividends,
                ),
            "fixed" to
                ProfitabilityMetricsDTO(
                    fixedInvested,
                    fixedCurrentValue,
                    fixedAccumulatedInterest,
                    fixedReturnPercentage,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                ),
            "total" to
                ProfitabilityMetricsDTO(
                    totalInvested,
                    totalCurrentValue,
                    totalProfitLoss,
                    totalReturnPercentage,
                    variableDividendYield,
                    totalDividends,
                ),
        )
    }

    private fun calculateTopPerformers(
        best: Boolean,
        limit: Int = TOP_PERFORMERS_LIMIT,
    ): List<TickerPerformanceDTO> {
        loadTickersFromDatabase()

        return tickers
            .filter { it.currentQuantity > BigDecimal.ZERO }
            .map { t ->
                val invested = t.averageUnitValue.multiply(t.currentQuantity)
                val current = t.currentQuantity.multiply(t.currentUnitValue)
                val profitLoss = current.subtract(invested)
                val percentage =
                    if (invested > BigDecimal.ZERO) {
                        profitLoss
                            .divide(invested, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal(HUNDRED))
                    } else {
                        BigDecimal.ZERO
                    }

                TickerPerformanceDTO(t.name, t.symbol, percentage, profitLoss, current)
            }.sortedWith(
                if (best) {
                    compareByDescending { it.profitLossPercentage }
                } else {
                    compareBy { it.profitLossPercentage }
                },
            ).take(limit)
    }

    private fun updateProfitabilityMetricsPanel() {
        val metrics = calculateProfitabilityMetricsByType()
        val variableMetrics = metrics["variable"]!!
        val fixedMetrics = metrics["fixed"]!!
        val totalMetrics = metrics["total"]!!

        profitabilityMetricsPaneController.setMetrics(variableMetrics, fixedMetrics, totalMetrics)
    }

    private fun updateTopPerformersPanel() {
        portfolioP2.children.clear()

        val container = VBox(30.0)
        container.alignment = Pos.CENTER

        val bestPerformers = calculateTopPerformers(best = true)
        val worstPerformers = calculateTopPerformers(best = false)

        val bestBox = VBox(5.0)
        val bestLabel =
            Label(
                preferencesService.translate(
                    TranslationKeys.SAVINGS_TOP_PERFORMERS_BEST,
                ),
            )
        bestLabel.styleClass.add(Constants.CUSTOM_TABLE_TITLE_STYLE)
        bestLabel.alignment = Pos.CENTER
        bestBox.children.add(bestLabel)
        bestBox.alignment = Pos.CENTER

        bestBox.children.add(createTableHeader())
        bestPerformers.forEach { bestBox.children.add(createPerformerRow(it)) }

        val worstBox = VBox(5.0)
        val worstLabel =
            Label(
                preferencesService.translate(
                    TranslationKeys.SAVINGS_TOP_PERFORMERS_WORST,
                ),
            )
        worstLabel.styleClass.add(Constants.CUSTOM_TABLE_TITLE_STYLE)
        worstLabel.alignment = Pos.CENTER
        worstBox.children.add(worstLabel)
        worstBox.alignment = Pos.CENTER

        worstBox.children.add(createTableHeader())
        worstPerformers.forEach { worstBox.children.add(createPerformerRow(it)) }

        container.children.addAll(bestBox, worstBox)

        portfolioP2.children.add(container)
        HBox.setHgrow(container, Priority.ALWAYS)
    }

    private fun createTableHeader(): HBox {
        val header = HBox(10.0)
        header.alignment = Pos.CENTER_LEFT

        val assetHeader =
            Label(
                preferencesService.translate(
                    TranslationKeys.SAVINGS_TOP_PERFORMERS_HEADER_ASSET,
                ),
            )
        assetHeader.styleClass.add(Constants.CUSTOM_TABLE_HEADER_STYLE)
        configureColumnWidth(assetHeader, Constants.TOP_PERFORMERS_ASSET_COLUMN_WIDTH)
        assetHeader.alignment = Pos.CENTER_LEFT

        val spacerA = Region()
        HBox.setHgrow(spacerA, Priority.ALWAYS)

        val spacerB = Region()
        HBox.setHgrow(spacerB, Priority.ALWAYS)

        val returnHeader =
            Label(
                preferencesService.translate(
                    TranslationKeys.SAVINGS_TOP_PERFORMERS_HEADER_RETURN,
                ),
            )
        returnHeader.styleClass.add(Constants.CUSTOM_TABLE_HEADER_STYLE)
        configureColumnWidth(returnHeader, Constants.TOP_PERFORMERS_RETURN_COLUMN_WIDTH)
        returnHeader.alignment = Pos.CENTER

        val valueHeader =
            Label(
                preferencesService.translate(
                    TranslationKeys.SAVINGS_TOP_PERFORMERS_HEADER_VALUE,
                ),
            )
        valueHeader.styleClass.add(Constants.CUSTOM_TABLE_HEADER_STYLE)
        configureColumnWidth(valueHeader, Constants.TOP_PERFORMERS_VALUE_COLUMN_WIDTH)
        valueHeader.alignment = Pos.CENTER_RIGHT

        header.children.addAll(assetHeader, spacerA, returnHeader, spacerB, valueHeader)

        return header
    }

    private fun configureColumnWidth(
        label: Label,
        width: Double,
    ) {
        label.minWidth = width
        label.maxWidth = width
    }

    private fun createPerformerRow(performer: TickerPerformanceDTO): HBox {
        val row = HBox(10.0)
        row.alignment = Pos.CENTER_LEFT

        val symbolLabel = Label(performer.symbol)
        symbolLabel.styleClass.add(Constants.CUSTOM_TABLE_CELL_STYLE)
        configureColumnWidth(symbolLabel, Constants.TOP_PERFORMERS_ASSET_COLUMN_WIDTH)
        symbolLabel.alignment = Pos.CENTER_LEFT

        val spacerA = Region()
        HBox.setHgrow(spacerA, Priority.ALWAYS)

        val spacerB = Region()
        HBox.setHgrow(spacerB, Priority.ALWAYS)

        val percentageLabel =
            Label(
                performer.getSign() +
                    UIUtils.formatPercentage(performer.profitLossPercentage),
            )
        percentageLabel.styleClass.add(Constants.CUSTOM_TABLE_CELL_STYLE)

        when {
            performer.isPositive() -> percentageLabel.styleClass.add(Constants.INFO_LABEL_GREEN_STYLE)
            performer.isNegative() -> percentageLabel.styleClass.add(Constants.INFO_LABEL_RED_STYLE)
            else -> percentageLabel.styleClass.add(Constants.INFO_LABEL_NEUTRAL_STYLE)
        }

        configureColumnWidth(percentageLabel, Constants.TOP_PERFORMERS_RETURN_COLUMN_WIDTH)
        percentageLabel.alignment = Pos.CENTER

        val valueLabel = Label(UIUtils.formatCurrencyDynamic(performer.currentValue))
        valueLabel.styleClass.add(Constants.CUSTOM_TABLE_CELL_STYLE)
        configureColumnWidth(valueLabel, Constants.TOP_PERFORMERS_VALUE_COLUMN_WIDTH)
        valueLabel.alignment = Pos.CENTER_RIGHT

        row.children.addAll(symbolLabel, spacerA, percentageLabel, spacerB, valueLabel)

        return row
    }

    private fun calculateAllocationVsTarget(): List<AllocationDTO> {
        val currentAllocation = mutableMapOf<AssetType, BigDecimal>()

        val totalBondValue =
            bondService
                .getTotalInvestedValue()
                .add(bondService.getAllBondsTotalAccumulatedInterest())

        var totalTickerValue = BigDecimal.ZERO

        tickers.forEach { ticker ->
            val value = ticker.currentQuantity.multiply(ticker.currentUnitValue)
            totalTickerValue = totalTickerValue.add(value)
            currentAllocation.merge(ticker.type, value, BigDecimal::add)
        }

        val totalValue = totalTickerValue.add(totalBondValue)

        val targets = investmentTargetService.getAllActiveTargets()

        return targets.map { target ->
            val assetType = target.assetType
            val (currentValue, typeName) =
                if (assetType == AssetType.BOND) {
                    totalBondValue to
                        preferencesService.translate(TranslationKeys.ASSET_TYPE_BOND)
                } else {
                    val tickerType = AssetType.valueOf(assetType.name)
                    currentAllocation.getOrDefault(tickerType, BigDecimal.ZERO) to
                        UIUtils.translateAssetType(tickerType)
                }

            val currentPercentage =
                if (totalValue > BigDecimal.ZERO) {
                    currentValue
                        .divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(HUNDRED))
                } else {
                    BigDecimal.ZERO
                }

            val difference = currentPercentage.subtract(target.targetPercentage)

            AllocationDTO(
                assetType,
                typeName,
                currentPercentage,
                target.targetPercentage,
                currentValue,
                difference,
            )
        }
    }

    private fun updateAllocationVsTargetPanel() {
        portfolioP5.children.clear()

        val container = VBox(ALLOCATION_PANEL_CONTAINER_SPACING.toDouble())
        container.alignment = Pos.CENTER
        container.style = "-fx-padding: $ALLOCATION_PANEL_CONTAINER_SPACING;"

        val allocations = calculateAllocationVsTarget()

        val gridPane = GridPane()
        gridPane.hgap = ALLOCATION_PANEL_COLUMNS_SPACING.toDouble()
        gridPane.alignment = Pos.CENTER

        val col1 =
            ColumnConstraints().apply {
                percentWidth = 50.0
                minWidth = 20.0
                hgrow = Priority.ALWAYS
            }

        val col2 =
            ColumnConstraints().apply {
                percentWidth = 50.0
                minWidth = 20.0
                hgrow = Priority.ALWAYS
            }

        gridPane.columnConstraints.addAll(col1, col2)

        val leftColumn = VBox(ALLOCATION_PANEL_ITEMS_SPACING.toDouble())
        leftColumn.alignment = Pos.CENTER_LEFT

        val rightColumn = VBox(ALLOCATION_PANEL_ITEMS_SPACING.toDouble())
        rightColumn.alignment = Pos.CENTER_LEFT

        allocations.forEachIndexed { i, allocation ->
            if (i % 2 == 0) {
                leftColumn.children.add(createAllocationBar(allocation))
            } else {
                rightColumn.children.add(createAllocationBar(allocation))
            }
        }

        gridPane.add(leftColumn, 0, 0)
        gridPane.add(rightColumn, 1, 0)

        container.children.add(gridPane)

        portfolioP5.children.add(container)
        HBox.setHgrow(container, Priority.ALWAYS)
    }

    private fun createAllocationBar(allocation: AllocationDTO): VBox {
        val barContainer = VBox(ALLOCATION_BAR_CONTAINER_SPACING.toDouble())

        val typeLabel = Label(allocation.typeName)
        typeLabel.styleClass.add(Constants.ALLOCATION_TYPE_LABEL_STYLE)

        val progressBar = HBox()
        progressBar.styleClass.add(Constants.ALLOCATION_PROGRESS_BAR_STYLE)
        progressBar.prefHeight = ALLOCATION_PROGRESS_BAR_HEIGHT

        val achievementPercentage = allocation.getAchievementPercentage()
        val fillPercentage =
            if (achievementPercentage >= BigDecimal(HUNDRED)) {
                HUNDRED
            } else {
                achievementPercentage.toDouble()
            }

        val filledBar = HBox()

        when {
            allocation.isCriticalLow() ->
                filledBar.styleClass.add(Constants.ALLOCATION_FILLED_BAR_CRITICAL_LOW_STYLE)

            allocation.isWarningLow() ->
                filledBar.styleClass.add(Constants.ALLOCATION_FILLED_BAR_WARNING_LOW_STYLE)

            allocation.isOnTargetRange() ->
                filledBar.styleClass.add(Constants.ALLOCATION_FILLED_BAR_ON_TARGET_STYLE)

            allocation.isWarningHigh() ->
                filledBar.styleClass.add(Constants.ALLOCATION_FILLED_BAR_WARNING_HIGH_STYLE)

            allocation.isCriticalHigh() ->
                filledBar.styleClass.add(Constants.ALLOCATION_FILLED_BAR_CRITICAL_HIGH_STYLE)
        }

        filledBar.prefHeight = ALLOCATION_FILLED_BAR_HEIGHT
        filledBar
            .prefWidthProperty()
            .bind(progressBar.widthProperty().multiply(fillPercentage / HUNDRED))

        progressBar.children.add(filledBar)

        val infoBox = HBox(ALLOCATION_INFO_BOX_SPACING.toDouble())
        infoBox.alignment = Pos.CENTER_LEFT

        val currentLabel =
            Label(
                "${UIUtils.formatPercentage(allocation.currentPercentage)} / " +
                    UIUtils.formatPercentage(allocation.targetPercentage),
            )
        currentLabel.styleClass.add(Constants.ALLOCATION_INFO_LABEL_STYLE)

        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)

        val statusText = getStatusText(allocation)
        val statusLabel = Label(statusText)
        statusLabel.styleClass.add(Constants.ALLOCATION_DIFF_LABEL_STYLE)

        if (!allocation.isNotInStrategy()) {
            when {
                allocation.isCriticalLow() ->
                    statusLabel.styleClass.add(Constants.ALLOCATION_DIFF_CRITICAL_LOW_STYLE)

                allocation.isWarningLow() ->
                    statusLabel.styleClass.add(Constants.ALLOCATION_DIFF_WARNING_LOW_STYLE)

                allocation.isOnTargetRange() ->
                    statusLabel.styleClass.add(Constants.ALLOCATION_DIFF_ON_TARGET_STYLE)

                allocation.isWarningHigh() ->
                    statusLabel.styleClass.add(Constants.ALLOCATION_DIFF_WARNING_HIGH_STYLE)

                allocation.isCriticalHigh() ->
                    statusLabel.styleClass.add(Constants.ALLOCATION_DIFF_CRITICAL_HIGH_STYLE)
            }
        }

        infoBox.children.addAll(currentLabel, spacer, statusLabel)

        barContainer.children.addAll(typeLabel, progressBar, infoBox)

        return barContainer
    }

    private fun getStatusText(allocation: AllocationDTO): String {
        if (allocation.isNotInStrategy()) {
            return ""
        }

        if (allocation.isOnTargetRange()) {
            return preferencesService.translate(
                TranslationKeys.SAVINGS_ALLOCATION_STATUS_ON_TARGET,
            )
        }

        val absDifference = allocation.difference.abs()
        val formattedDiff = UIUtils.formatPercentage(absDifference)

        return when {
            allocation.isCriticalLow() ->
                "${preferencesService.translate(TranslationKeys.SAVINGS_ALLOCATION_STATUS_CRITICAL_LOW)} $formattedDiff"

            allocation.isWarningLow() ->
                "${preferencesService.translate(TranslationKeys.SAVINGS_ALLOCATION_STATUS_WARNING_LOW)} $formattedDiff"

            allocation.isWarningHigh() ->
                "${preferencesService.translate(TranslationKeys.SAVINGS_ALLOCATION_STATUS_WARNING_HIGH)} $formattedDiff"

            allocation.isCriticalHigh() ->
                "${preferencesService.translate(
                    TranslationKeys.SAVINGS_ALLOCATION_STATUS_CRITICAL_HIGH,
                )} $formattedDiff"

            else -> ""
        }
    }
}
