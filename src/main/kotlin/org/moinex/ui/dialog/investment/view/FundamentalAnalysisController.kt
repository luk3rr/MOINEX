/*
 * Filename: FundamentalAnalysisController.kt
 * Created on: January 9, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment.view

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.ScrollPane
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.image.ImageView
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.json.JSONObject
import org.moinex.common.ClockProvider
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isUpdateRecommended
import org.moinex.common.util.FxUtils
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.config.RetryConfig
import org.moinex.model.dto.AssetKind
import org.moinex.model.dto.FundamentalSnapshot
import org.moinex.model.dto.QualityScore
import org.moinex.model.dto.SectorComparison
import org.moinex.model.dto.ValuationVerdict
import org.moinex.model.enums.NotificationType
import org.moinex.model.enums.PeriodType
import org.moinex.model.investment.FundamentalAnalysis
import org.moinex.model.investment.Ticker
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.moinex.service.ThemeService
import org.moinex.service.investment.FundamentalAnalysisService
import org.moinex.service.investment.FundamentalParser
import org.moinex.service.investment.FundamentalPriceOverlay
import org.moinex.service.investment.FundamentalScoringService
import org.moinex.ui.common.DividendHistoryChart
import org.moinex.ui.common.FundamentalInsightsView
import org.moinex.ui.common.FundamentalMetricPaneController
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.getBean
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.text.MessageFormat
import kotlin.collections.addAll

@Controller
class FundamentalAnalysisController(
    private val fundamentalAnalysisService: FundamentalAnalysisService,
    private val preferencesService: PreferencesService,
    private val springContext: ConfigurableApplicationContext,
    private val notificationService: NotificationService,
    private val clockProvider: ClockProvider,
    private val fundamentalScoringService: FundamentalScoringService,
) {
    @FXML
    private lateinit var companyNameValueLabel: Label

    @FXML
    private lateinit var sectorValueLabel: Label

    @FXML
    private lateinit var industryValueLabel: Label

    @FXML
    private lateinit var currencyValueLabel: Label

    @FXML
    private lateinit var cacheStatusContainer: HBox

    @FXML
    private lateinit var periodComboBox: ComboBox<PeriodType>

    @FXML
    private lateinit var metricsTabPane: TabPane

    @FXML
    private lateinit var insightsBox: HBox

    @FXML
    private lateinit var loadingContainer: VBox

    @FXML
    private lateinit var loadingIndicator: ProgressIndicator

    @FXML
    private lateinit var refreshButton: Button

    @FXML
    private lateinit var companyLogoImageView: ImageView

    private lateinit var ticker: Ticker
    private var currentAnalysis: FundamentalAnalysis? = null
    private var currentInsights: AnalysisInsights? = null

    companion object {
        private val logger = LoggerFactory.getLogger(FundamentalAnalysisController::class.java)

        // CSS Style Classes
        private const val CACHE_STATUS_LABEL_CLASS = "cache-status-label"
        private const val CACHE_STATUS_LABEL_EXPIRED_CLASS = "cache-status-label-expired"
        private const val CACHE_STATUS_LABEL_VALID_CLASS = "cache-status-label-valid"
        private const val CACHE_STATUS_LABEL_UNAVAILABLE_CLASS = "cache-status-label-unavailable"
        private const val TRANSPARENT_SCROLL_PANE_CLASS = "transparent-scroll-pane"

        // JSON Keys - Main Categories
        private const val JSON_KEY_PROFITABILITY = "profitability"
        private const val JSON_KEY_VALUATION = "valuation"
        private const val JSON_KEY_GROWTH = "growth"
        private const val JSON_KEY_DEBT = "debt"
        private const val JSON_KEY_EFFICIENCY = "efficiency"
        private const val JSON_KEY_CASH_GENERATION = "cash_generation"
        private const val JSON_KEY_PRICE_PERFORMANCE = "price_performance"

        // JSON Metrics - Growth (used by createGrowthTab)
        private const val JSON_METRIC_REVENUE_GROWTH = "revenue_growth"
        private const val JSON_METRIC_YOY_GROWTH = "yoy_growth"
        private const val JSON_METRIC_CAGR = "cagr"
        private const val JSON_METRIC_YEARS = "years"
        private const val JSON_METRIC_VALUE = "value"
        private const val JSON_METRIC_TYPE = "type"
        private const val JSON_METRIC_DATA_TEMPORALITY = "data_temporality"
        private const val JSON_VALUE_NUMBER = "number"
        private const val JSON_VALUE_CALCULATED = "calculated"

        private data class MetricDef(
            val labelKey: String,
            val jsonKey: String,
            val tooltipKey: String,
        )

        private val PROFITABILITY_METRICS =
            listOf(
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_ROE,
                    "roe",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_ROE_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_ROIC,
                    "roic",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_ROIC_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_NET_MARGIN,
                    "net_margin",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_NET_MARGIN_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_EBITDA_MARGIN,
                    "ebitda_margin",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_EBITDA_MARGIN_TOOLTIP,
                ),
            )

        private val VALUATION_METRICS =
            listOf(
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_PRICE,
                    "current_price",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_PRICE_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_MARKET_CAP,
                    "market_cap",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_MARKET_CAP_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_ENTERPRISE_VALUE,
                    "enterprise_value",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_ENTERPRISE_VALUE_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_EPS,
                    "eps",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_EPS_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_PE_RATIO,
                    "pe_ratio",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_PE_RATIO_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_PEG_RATIO,
                    "peg_ratio",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_PEG_RATIO_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_EV_EBITDA,
                    "ev_to_ebitda",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_EV_EBITDA_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_EARNINGS_YIELD,
                    "earnings_yield",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_EARNINGS_YIELD_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_FCF_YIELD,
                    "fcf_yield",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_FCF_YIELD_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DIVIDEND_YIELD,
                    "dividend_yield",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DIVIDEND_YIELD_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DIVIDEND_RATE,
                    "dividend_rate",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DIVIDEND_RATE_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_PAYOUT_RATIO,
                    "payout_ratio",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_PAYOUT_RATIO_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_GRAHAM_NUMBER,
                    "graham_number",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_GRAHAM_NUMBER_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_GRAHAM_FAIR_VALUE,
                    "graham_fair_value",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_GRAHAM_FAIR_VALUE_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_MARGIN_OF_SAFETY,
                    "margin_of_safety",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_MARGIN_OF_SAFETY_TOOLTIP,
                ),
            )

        private val DEBT_METRICS =
            listOf(
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_TOTAL_DEBT,
                    "total_debt",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_TOTAL_DEBT_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_NET_DEBT,
                    "net_debt",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_NET_DEBT_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_NET_DEBT_EBITDA,
                    "net_debt_to_ebitda",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_NET_DEBT_EBITDA_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_RATIO,
                    "current_ratio",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_RATIO_TOOLTIP,
                ),
            )

        private val EFFICIENCY_METRICS =
            listOf(
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_ASSET_TURNOVER,
                    "asset_turnover",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_ASSET_TURNOVER_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_EBITDA,
                    "ebitda",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_EBITDA_TOOLTIP,
                ),
            )

        private val CASH_GENERATION_METRICS =
            listOf(
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_FREE_CASH_FLOW,
                    "free_cash_flow",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_FREE_CASH_FLOW_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_OPERATING_CASH_FLOW,
                    "operating_cash_flow",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_OPERATING_CASH_FLOW_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CAPEX,
                    "capex",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CAPEX_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_FCF_NET_INCOME,
                    "fcf_to_net_income",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_FCF_NET_INCOME_TOOLTIP,
                ),
            )

        private val PRICE_PERFORMANCE_METRICS =
            listOf(
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_PRICE,
                    "current_price",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_PRICE_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DAY_HIGH,
                    "day_high",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DAY_HIGH_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DAY_LOW,
                    "day_low",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DAY_LOW_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1D,
                    "change_1d",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1D_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_5D,
                    "change_5d",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_5D_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1M,
                    "change_1m",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1M_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_3M,
                    "change_3m",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_3M_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_6M,
                    "change_6m",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_6M_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1Y,
                    "change_52w",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1Y_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_YTD,
                    "change_ytd",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_YTD_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_52W_HIGH,
                    "week_52_high",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_52W_HIGH_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_52W_LOW,
                    "week_52_low",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_52W_LOW_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DISTANCE_52W_HIGH,
                    "distance_from_52w_high",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DISTANCE_52W_HIGH_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DISTANCE_52W_LOW,
                    "distance_from_52w_low",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DISTANCE_52W_LOW_TOOLTIP,
                ),
            )

        private const val JSON_KEY_INCOME = "income"

        private val FUND_INCOME_METRICS =
            listOf(
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DIVIDEND_YIELD,
                    "dividend_yield",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DIVIDEND_YIELD_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_LAST_DIVIDEND,
                    "last_dividend",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_LAST_DIVIDEND_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DIVIDENDS_12M,
                    "dividends_12m_total",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DIVIDENDS_12M_TOOLTIP,
                ),
            )

        private val FUND_VALUE_METRICS =
            listOf(
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_PRICE,
                    "current_price",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_PRICE_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_MARKET_CAP,
                    "market_cap",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_MARKET_CAP_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_PRICE_TO_BOOK,
                    "price_to_book",
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_PRICE_TO_BOOK_TOOLTIP,
                ),
            )

        private data class AnalysisInsights(
            val snapshot: FundamentalSnapshot,
            val verdict: ValuationVerdict,
            val quality: QualityScore,
            val sector: Map<String, SectorComparison>,
        )

        private data class TabDef(
            val titleKey: String,
            val metrics: List<MetricDef>,
        )

        private val SIMPLE_TABS: Map<String, TabDef> =
            mapOf(
                JSON_KEY_PROFITABILITY to
                    TabDef(TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_PROFITABILITY, PROFITABILITY_METRICS),
                JSON_KEY_VALUATION to TabDef(TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_VALUATION, VALUATION_METRICS),
                JSON_KEY_DEBT to TabDef(TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_DEBT, DEBT_METRICS),
                JSON_KEY_EFFICIENCY to TabDef(TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_EFFICIENCY, EFFICIENCY_METRICS),
                JSON_KEY_CASH_GENERATION to
                    TabDef(TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_CASH_GENERATION, CASH_GENERATION_METRICS),
                JSON_KEY_PRICE_PERFORMANCE to
                    TabDef(TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_PRICE_PERFORMANCE, PRICE_PERFORMANCE_METRICS),
            )

        private val TAB_BUILDERS: Map<String, (FundamentalAnalysisController, JSONObject) -> Tab> =
            buildMap {
                SIMPLE_TABS.forEach { (jsonKey, tabDef) ->
                    put(jsonKey) { c, json -> c.createSimpleTab(tabDef.titleKey, json, tabDef.metrics, jsonKey) }
                }
                put(JSON_KEY_GROWTH) { c, json -> c.createGrowthTab(json) }
            }
    }

    @FXML
    fun initialize() {
        periodComboBox.items.addAll(PeriodType.entries.toTypedArray())
        periodComboBox.value = PeriodType.ANNUAL

        UIUtils.configureComboBox(periodComboBox) { UIUtils.translatePeriodType(it) }

        periodComboBox.valueProperty().addListener { _, _, newVal ->
            if (newVal != null) {
                loadAnalysis(false)
            }
        }
    }

    @FXML
    fun handleRefresh() {
        loadAnalysis(true)
    }

    @FXML
    fun handleClose() {
        val stage = companyNameValueLabel.scene.window as Stage
        stage.close()
    }

    fun setTicker(ticker: Ticker) {
        this.ticker = ticker
    }

    private fun updateCacheStatusIndicators() {
        if (!::cacheStatusContainer.isInitialized) {
            return
        }

        cacheStatusContainer.children.removeIf { node ->
            node is Label &&
                node.text !=
                preferencesService.translate(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_LAST_UPDATE,
                )
        }

        val analyses = fundamentalAnalysisService.getAllAnalysesForTicker(ticker.id!!)

        for (periodType in PeriodType.entries) {
            val statusLabel = Label()
            statusLabel.styleClass.add(CACHE_STATUS_LABEL_CLASS)

            val analysis = analyses.firstOrNull { it.periodType == periodType }

            if (analysis != null) {
                val lastUpdate = UIUtils.formatDateTimeForDisplay(analysis.lastUpdate)
                val recommendedUpdate = analysis.isUpdateRecommended(clockProvider.now())

                statusLabel.text = "${UIUtils.translatePeriodType(periodType)}: $lastUpdate"

                if (recommendedUpdate) {
                    statusLabel.styleClass.add(CACHE_STATUS_LABEL_EXPIRED_CLASS)
                } else {
                    statusLabel.styleClass.add(CACHE_STATUS_LABEL_VALID_CLASS)
                }
            } else {
                statusLabel.text = "${UIUtils.translatePeriodType(periodType)}: --"
                statusLabel.styleClass.add(CACHE_STATUS_LABEL_UNAVAILABLE_CLASS)
            }

            cacheStatusContainer.children.add(statusLabel)
        }
    }

    fun loadAnalysis(forceRefresh: Boolean = false) {
        val period = periodComboBox.value

        showLoading(true)

        refreshButton.isDisable = true
        periodComboBox.isDisable = true

        FxUtils.launchOnFxThread {
            val result =
                runCatching {
                    FxUtils.withBackground {
                        val analysis = fundamentalAnalysisService.getAnalysis(ticker.id!!, period, forceRefresh)
                        analysis to buildInsights(analysis)
                    }
                }

            result
                .onSuccess { (analysis, insights) ->
                    currentAnalysis = analysis
                    currentInsights = insights
                    displayAnalysis()
                    updateCacheStatusIndicators()
                }.onFailure { exception ->
                    logger.error("Error loading fundamental analysis", exception)
                    handleAnalysisError(exception)
                }

            showLoading(false)
            refreshButton.isDisable = false
            periodComboBox.isDisable = false
        }
    }

    private fun effectiveDataJson(analysis: FundamentalAnalysis): String =
        FundamentalPriceOverlay.apply(analysis.dataJson, ticker.currentUnitValue, ticker.lastUpdate)

    private fun buildInsights(analysis: FundamentalAnalysis): AnalysisInsights? =
        runCatching {
            val snapshot = FundamentalParser.parse(effectiveDataJson(analysis))
            AnalysisInsights(
                snapshot,
                fundamentalScoringService.valuation(snapshot),
                fundamentalScoringService.quality(snapshot),
                fundamentalScoringService.sectorComparisons(analysis),
            )
        }.onFailure { logger.error("Could not compute the valuation and quality summary", it) }.getOrNull()

    private fun showInsights() {
        val insights = currentInsights
        val show = insights != null
        val view = FundamentalInsightsView(preferencesService::translate)

        insightsBox.children.clear()
        if (insights != null) {
            if (insights.snapshot.kind == AssetKind.COMPANY) {
                insightsBox.children.addAll(view.build(insights.snapshot, insights.verdict, insights.quality))
            } else {
                insightsBox.children.add(
                    view.notice(preferencesService.translate(TranslationKeys.FUNDAMENTAL_ANALYSIS_FUND_NOTICE)),
                )
            }
        }
        insightsBox.isVisible = show
        insightsBox.isManaged = show
    }

    private fun displayAnalysis() {
        if (currentAnalysis == null) {
            return
        }

        companyNameValueLabel.text =
            UIUtils.getOrDefault(currentAnalysis!!.companyName, Constants.NA_DATA).toString()
        sectorValueLabel.text =
            UIUtils.getOrDefault(currentAnalysis!!.sector, Constants.NA_DATA).toString()
        industryValueLabel.text =
            UIUtils.getOrDefault(currentAnalysis!!.industry, Constants.NA_DATA).toString()
        currencyValueLabel.text =
            UIUtils.getOrDefault(currentAnalysis!!.currency, Constants.NA_DATA).toString()

        val logo = UIUtils.loadTickerLogo(ticker, 80.0)
        logo?.let { logoView ->
            companyLogoImageView.image = logoView.image
        }

        runCatching {
            val data = JSONObject(effectiveDataJson(currentAnalysis!!))
            showInsights()
            populateMetricsTabs(data)

            // Force window to recalculate size after content is loaded
            val stage = companyNameValueLabel.scene?.window as? Stage
            stage?.sizeToScene()
        }.onFailure { e ->
            logger.error("Error parsing analysis JSON", e)
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.DIALOG_ERROR_TITLE),
                "${preferencesService.translate(TranslationKeys.FUNDAMENTAL_ANALYSIS_ERROR_TITLE)}: ${e.message}",
            )
        }
    }

    private fun populateMetricsTabs(data: JSONObject) {
        metricsTabPane.tabs.clear()

        logger.debug("Populating metrics tabs. Available keys in data: {}", data.keys().asSequence().toList())

        val snapshot = currentInsights?.snapshot
        if (snapshot != null && snapshot.kind != AssetKind.COMPANY) {
            populateFundTabs(data, snapshot)
            return
        }

        TAB_BUILDERS.forEach { (key, builder) ->
            val tabData = data.optJSONObject(key)
            if (tabData != null) {
                logger.debug("Creating tab for key: {}", key)
                metricsTabPane.tabs.add(builder(this, tabData))
            } else {
                logger.debug("No data found for key: {}", key)
            }
        }

        logger.debug("Total tabs created: {}", metricsTabPane.tabs.size)
    }

    private fun populateFundTabs(
        data: JSONObject,
        snapshot: FundamentalSnapshot,
    ) {
        data.optJSONObject(JSON_KEY_PRICE_PERFORMANCE)?.let {
            metricsTabPane.tabs.add(
                createSimpleTab(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_PRICE_PERFORMANCE,
                    it,
                    PRICE_PERFORMANCE_METRICS,
                ),
            )
        }
        metricsTabPane.tabs.add(createIncomeTab(data.optJSONObject(JSON_KEY_INCOME) ?: JSONObject(), snapshot))
        metricsTabPane.tabs.add(
            createSimpleTab(
                TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_VALUE,
                data.optJSONObject(JSON_KEY_VALUATION) ?: JSONObject(),
                FUND_VALUE_METRICS,
            ),
        )
    }

    private fun createIncomeTab(
        income: JSONObject,
        snapshot: FundamentalSnapshot,
    ): Tab {
        val tab = Tab(preferencesService.translate(TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_INCOME))
        tab.isClosable = false

        val metrics = FlowPane(15.0, 15.0)
        addMetrics(metrics, income, FUND_INCOME_METRICS)

        val content = VBox(15.0, metrics)
        content.padding = Insets(20.0)
        if (snapshot.dividendHistory.any { it.value.signum() > 0 }) {
            content.children.add(
                Label(preferencesService.translate(TranslationKeys.FUNDAMENTAL_ANALYSIS_FUND_DIVIDENDS_CHART_TITLE)),
            )
            content.children.add(DividendHistoryChart.create(snapshot.dividendHistory))
        }

        tab.content =
            ScrollPane(content).apply {
                isFitToWidth = true
                styleClass.add(TRANSPARENT_SCROLL_PANE_CLASS)
            }
        return tab
    }

    private fun createSimpleTab(
        titleKey: String,
        data: JSONObject,
        metrics: List<MetricDef>,
        section: String? = null,
    ): Tab {
        val tab = Tab(preferencesService.translate(titleKey))
        tab.isClosable = false

        val content = FlowPane(15.0, 15.0)
        content.padding = Insets(20.0)

        addMetrics(content, data, metrics, section)

        tab.content =
            ScrollPane(content).apply {
                isFitToWidth = true
                styleClass.add(TRANSPARENT_SCROLL_PANE_CLASS)
            }
        return tab
    }

    private fun addMetrics(
        container: FlowPane,
        data: JSONObject,
        metrics: List<MetricDef>,
        section: String? = null,
    ) {
        logger.debug("Adding {} metrics to container. Data keys: {}", metrics.size, data.keys().asSequence().toList())
        metrics.forEach { (labelKey, jsonKey, tooltipKey) ->
            logger.debug(
                "Processing metric - labelKey: {}, jsonKey: {}, has data: {}",
                labelKey,
                jsonKey,
                data.has(jsonKey),
            )
            val comparison =
                section?.let { currentInsights?.sector?.get(FundamentalScoringService.sectorKey(it, jsonKey)) }
            val node =
                addMetricToContainer(
                    container,
                    preferencesService.translate(labelKey),
                    data,
                    jsonKey,
                    comparison,
                    preferencesService.translate(tooltipKey),
                )
            if (node != null) {
                logger.debug("Successfully added metric: {}", labelKey)
            } else {
                logger.debug("Metric returned null: {}", labelKey)
            }
        }
        logger.debug("Container now has {} children", container.children.size)
    }

    private fun createGrowthTab(growth: JSONObject): Tab {
        val tab = Tab(preferencesService.translate(TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_GROWTH))
        tab.isClosable = false

        val content = FlowPane(15.0, 15.0)
        content.padding = Insets(20.0)

        val revenueGrowth = growth.optJSONObject(JSON_METRIC_REVENUE_GROWTH) ?: JSONObject()

        addMetrics(
            content,
            revenueGrowth,
            listOf(
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_YOY,
                    JSON_METRIC_YOY_GROWTH,
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_YOY_TOOLTIP,
                ),
                MetricDef(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_CAGR,
                    JSON_METRIC_CAGR,
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_CAGR_TOOLTIP,
                ),
            ),
        )

        if (revenueGrowth.has(JSON_METRIC_YEARS)) {
            val yearsMetric = JSONObject()
            yearsMetric.put(JSON_METRIC_VALUE, revenueGrowth.getInt(JSON_METRIC_YEARS))
            yearsMetric.put(JSON_METRIC_TYPE, JSON_VALUE_NUMBER)
            yearsMetric.put(JSON_METRIC_DATA_TEMPORALITY, JSON_VALUE_CALCULATED)

            addMetricToContainer(
                content,
                preferencesService.translate(TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_YEARS),
                yearsMetric,
                JSON_METRIC_VALUE,
                infoText =
                    preferencesService.translate(
                        TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_YEARS_TOOLTIP,
                    ),
            )
        }

        tab.content =
            ScrollPane(content).apply {
                isFitToWidth = true
                styleClass.add(TRANSPARENT_SCROLL_PANE_CLASS)
            }
        return tab
    }

    private fun addMetricToContainer(
        container: FlowPane,
        label: String,
        data: JSONObject,
        key: String,
        sectorComparison: SectorComparison? = null,
        infoText: String? = null,
    ): Node? {
        val metricObj = if (data.has(key)) data[key] else JSONObject.NULL
        logger.debug(
            "Loading metric pane for label: {}, key: {}, metricObj type: {}",
            label,
            key,
            metricObj?.javaClass?.simpleName,
        )

        return runCatching {
            val loader = FXMLLoader(javaClass.getResource(Files.FUNDAMENTAL_METRIC_PANE_FXML))
            loader.setControllerFactory { springContext.getBean(it) }

            val metricPane = loader.load<VBox>()
            val controller = loader.getController<FundamentalMetricPaneController>()

            val lastUpdateDate =
                currentAnalysis?.lastUpdate?.toLocalDate()?.toString()
            controller.updateMetricPane(label, metricObj, lastUpdateDate)
            controller.setInfo(infoText)
            controller.showSectorComparison(sectorComparison, metricObj, !currentAnalysis?.sector.isNullOrBlank())
            springContext.getBean<ThemeService>().applyIconsTo(metricPane)

            container.children.add(metricPane)
            logger.debug("Added metric pane to container. Container children count: {}", container.children.size)

            controller.getRoot()
        }.onFailure { e ->
            logger.error("Error loading metric pane for: $label", e)
        }.getOrNull()
    }

    private fun showLoading(show: Boolean) {
        loadingContainer.isVisible = show
        loadingContainer.isManaged = show
        metricsTabPane.isVisible = !show
    }

    private fun handleAnalysisError(exception: Throwable) {
        var errorMessage = exception.message
        var title = preferencesService.translate(TranslationKeys.FUNDAMENTAL_ANALYSIS_ERROR_TITLE)

        if (errorMessage != null &&
            (
                errorMessage.contains("Network is unreachable") ||
                    errorMessage.contains("Connection") ||
                    errorMessage.contains("Max retries exceeded") ||
                    errorMessage.contains("API exception after")
            )
        ) {
            title =
                preferencesService.translate(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_ERROR_CONNECTION_TITLE,
                )
            errorMessage =
                MessageFormat.format(
                    preferencesService.translate(
                        TranslationKeys.FUNDAMENTAL_ANALYSIS_ERROR_CONNECTION_MESSAGE,
                    ),
                    RetryConfig.FUNDAMENTAL_ANALYSIS.maxRetries,
                )
        }

        notificationService.send(
            type = NotificationType.ERROR,
            title = title,
            message = errorMessage ?: "",
        )
    }
}
