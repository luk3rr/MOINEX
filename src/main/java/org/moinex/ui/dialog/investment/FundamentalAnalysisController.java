/*
 * Filename: FundamentalAnalysisController.java
 * Created on: January  9, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.json.JSONObject;
import org.moinex.model.enums.PeriodType;
import org.moinex.model.investment.FundamentalAnalysis;
import org.moinex.model.investment.Ticker;
import org.moinex.service.FundamentalAnalysisService;
import org.moinex.service.I18nService;
import org.moinex.ui.common.FundamentalMetricPaneController;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Fundamental Analysis dialog
 * Displays comprehensive fundamental analysis data for a ticker
 */
@Controller
@NoArgsConstructor
public class FundamentalAnalysisController {
    private static final Logger logger =
            LoggerFactory.getLogger(FundamentalAnalysisController.class);

    @FXML private Label companyNameValueLabel;
    @FXML private Label sectorValueLabel;
    @FXML private Label industryValueLabel;
    @FXML private Label currencyValueLabel;
    @FXML private HBox cacheStatusContainer;
    @FXML private ComboBox<PeriodType> periodComboBox;
    @FXML private TabPane metricsTabPane;
    @FXML private VBox loadingContainer;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button refreshButton;

    private FundamentalAnalysisService fundamentalAnalysisService;
    private I18nService i18nService;
    private ConfigurableApplicationContext springContext;
    private Ticker ticker;
    private FundamentalAnalysis currentAnalysis;

    @Autowired
    public FundamentalAnalysisController(
            FundamentalAnalysisService fundamentalAnalysisService,
            I18nService i18nService,
            ConfigurableApplicationContext springContext) {
        this.fundamentalAnalysisService = fundamentalAnalysisService;
        this.i18nService = i18nService;
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {
        periodComboBox.getItems().addAll(PeriodType.values());
        periodComboBox.setValue(PeriodType.ANNUAL);

        // Set custom cell factory and button cell for translated display
        periodComboBox.setCellFactory(
                param ->
                        new ListCell<>() {
                            @Override
                            protected void updateItem(PeriodType item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty || item == null) {
                                    setText(null);
                                } else {
                                    setText(UIUtils.translatePeriodType(item, i18nService));
                                }
                            }
                        });

        periodComboBox.setButtonCell(
                new javafx.scene.control.ListCell<PeriodType>() {
                    @Override
                    protected void updateItem(PeriodType item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(UIUtils.translatePeriodType(item, i18nService));
                        }
                    }
                });

        // Add listener to period change
        periodComboBox
                .valueProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            if (newVal != null && ticker != null) {
                                loadAnalysis(false);
                            }
                        });
    }

    /**
     * Set the ticker to analyze
     */
    public void setTicker(Ticker ticker) {
        this.ticker = ticker;
    }

    /**
     * Update cache status indicators for all period types
     */
    private void updateCacheStatusIndicators() {
        if (ticker == null || cacheStatusContainer == null) {
            return;
        }

        Platform.runLater(
                () -> {
                    // Clear existing status labels (keep only the title)
                    cacheStatusContainer
                            .getChildren()
                            .removeIf(
                                    node ->
                                            node instanceof Label
                                                    && !((Label) node)
                                                            .getText()
                                                            .equals(
                                                                    i18nService.tr(
                                                                            Constants
                                                                                    .TranslationKeys
                                                                                    .FUNDAMENTAL_ANALYSIS_LAST_UPDATE)));

                    // Get all analyses for this ticker
                    List<FundamentalAnalysis> analyses =
                            fundamentalAnalysisService.getAllAnalysesForTicker(ticker.getId());

                    // Create status label for each period type
                    for (PeriodType periodType : PeriodType.values()) {
                        Label statusLabel = new Label();
                        statusLabel.getStyleClass().add("cache-status-label");

                        // Find analysis for this period type
                        Optional<FundamentalAnalysis> analysis =
                                analyses.stream()
                                        .filter(a -> a.getPeriodType() == periodType)
                                        .findFirst();

                        if (analysis.isPresent()) {
                            String lastUpdate =
                                    UIUtils.formatDateTimeForDisplay(
                                            analysis.get().getLastUpdate(), i18nService);
                            boolean expired =
                                    fundamentalAnalysisService.isCacheExpired(analysis.get());

                            statusLabel.setText(
                                    UIUtils.translatePeriodType(periodType, i18nService)
                                            + ": "
                                            + lastUpdate);
                            if (expired) {
                                statusLabel.getStyleClass().add("cache-status-label-expired");
                            } else {
                                statusLabel.getStyleClass().add("cache-status-label-valid");
                            }
                        } else {
                            statusLabel.setText(
                                    UIUtils.translatePeriodType(periodType, i18nService) + ": --");
                            statusLabel.getStyleClass().add("cache-status-label-unavailable");
                        }

                        cacheStatusContainer.getChildren().add(statusLabel);
                    }
                });
    }

    /**
     * Load analysis data
     */
    public void loadAnalysis() {
        loadAnalysis(false);
    }

    /**
     * Load analysis data with optional force refresh
     */
    private void loadAnalysis(boolean forceRefresh) {
        if (ticker == null) {
            return;
        }

        showLoading(true);
        refreshButton.setDisable(true);
        periodComboBox.setDisable(true);

        // Create background task
        Task<FundamentalAnalysis> task =
                new Task<>() {
                    @Override
                    protected FundamentalAnalysis call() throws Exception {
                        PeriodType period = periodComboBox.getValue();
                        return fundamentalAnalysisService.getAnalysis(
                                ticker.getId(), period, forceRefresh);
                    }
                };

        task.setOnSucceeded(
                event -> {
                    currentAnalysis = task.getValue();
                    displayAnalysis();
                    updateCacheStatusIndicators();
                    showLoading(false);
                    refreshButton.setDisable(false);
                    periodComboBox.setDisable(false);
                });

        task.setOnFailed(
                event -> {
                    showLoading(false);
                    refreshButton.setDisable(false);
                    periodComboBox.setDisable(false);
                    Throwable exception = task.getException();
                    logger.error("Error loading fundamental analysis", exception);

                    String errorMessage = exception.getMessage();
                    String title =
                            i18nService.tr(
                                    Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_ERROR_TITLE);

                    // Check if it's a network/API error
                    if (errorMessage != null
                            && (errorMessage.contains("Network is unreachable")
                                    || errorMessage.contains("Connection")
                                    || errorMessage.contains("Max retries exceeded")
                                    || errorMessage.contains("API error after"))) {
                        title =
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .FUNDAMENTAL_ANALYSIS_ERROR_CONNECTION_TITLE);
                        errorMessage =
                                MessageFormat.format(
                                        i18nService.tr(
                                                Constants.TranslationKeys
                                                        .FUNDAMENTAL_ANALYSIS_ERROR_CONNECTION_MESSAGE),
                                        FundamentalAnalysisService.MAX_RETRIES);
                    } else {
                        errorMessage =
                                i18nService.tr(
                                                Constants.TranslationKeys
                                                        .FUNDAMENTAL_ANALYSIS_ERROR_TITLE)
                                        + ":\n\n"
                                        + errorMessage;
                    }

                    WindowUtils.showErrorDialog(title, errorMessage);
                });

        // Run task in background
        new Thread(task).start();
    }

    /**
     * Display analysis data in the UI
     */
    private void displayAnalysis() {
        if (currentAnalysis == null) {
            return;
        }

        // Update company info (use N/A for null values)
        companyNameValueLabel.setText(
                UIUtils.getOrDefault(currentAnalysis.getCompanyName(), Constants.NA_DATA)
                        .toString());
        sectorValueLabel.setText(
                UIUtils.getOrDefault(currentAnalysis.getSector(), Constants.NA_DATA).toString());
        industryValueLabel.setText(
                UIUtils.getOrDefault(currentAnalysis.getIndustry(), Constants.NA_DATA).toString());
        currencyValueLabel.setText(
                UIUtils.getOrDefault(currentAnalysis.getCurrency(), Constants.NA_DATA).toString());

        // Parse JSON and populate tabs
        try {
            JSONObject data = new JSONObject(currentAnalysis.getDataJson());
            populateMetricsTabs(data);
        } catch (Exception e) {
            logger.error("Error parsing analysis JSON", e);
            WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.DIALOG_ERROR_TITLE),
                    i18nService.tr(Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_ERROR_TITLE)
                            + ": "
                            + e.getMessage());
        }
    }

    /**
     * Populate all metrics tabs with data
     */
    private void populateMetricsTabs(JSONObject data) {
        metricsTabPane.getTabs().clear();

        // Create tabs for each category
        if (data.has("profitability")) {
            metricsTabPane
                    .getTabs()
                    .add(createProfitabilityTab(data.getJSONObject("profitability")));
        }

        if (data.has("valuation")) {
            metricsTabPane.getTabs().add(createValuationTab(data.getJSONObject("valuation")));
        }

        if (data.has("growth")) {
            metricsTabPane.getTabs().add(createGrowthTab(data.getJSONObject("growth")));
        }

        if (data.has("debt")) {
            metricsTabPane.getTabs().add(createDebtTab(data.getJSONObject("debt")));
        }

        if (data.has("efficiency")) {
            metricsTabPane.getTabs().add(createEfficiencyTab(data.getJSONObject("efficiency")));
        }

        if (data.has("cash_generation")) {
            metricsTabPane
                    .getTabs()
                    .add(createCashGenerationTab(data.getJSONObject("cash_generation")));
        }

        if (data.has("price_performance")) {
            metricsTabPane
                    .getTabs()
                    .add(createPricePerformanceTab(data.getJSONObject("price_performance")));
        }
    }

    /**
     * Create Profitability tab
     */
    private Tab createProfitabilityTab(JSONObject profitability) {
        Tab tab =
                new Tab(
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_PROFITABILITY));
        tab.setClosable(false);

        FlowPane content = new FlowPane(15, 15);
        content.setPadding(new Insets(20));

        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_ROE),
                        profitability,
                        "roe"),
                i18nService.tr(Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_ROE_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_ROIC),
                        profitability,
                        "roic"),
                i18nService.tr(Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_ROIC_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_NET_MARGIN),
                        profitability,
                        "net_margin"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_NET_MARGIN_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_EBITDA_MARGIN),
                        profitability,
                        "ebitda_margin"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_EBITDA_MARGIN_TOOLTIP));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll-pane");

        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Create Valuation tab
     */
    private Tab createValuationTab(JSONObject valuation) {
        Tab tab =
                new Tab(
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_VALUATION));
        tab.setClosable(false);

        FlowPane content = new FlowPane(15, 15);
        content.setPadding(new Insets(20));

        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_PRICE),
                        valuation,
                        "current_price"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_PRICE_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_MARKET_CAP),
                        valuation,
                        "market_cap"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_MARKET_CAP_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_ENTERPRISE_VALUE),
                        valuation,
                        "enterprise_value"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_ENTERPRISE_VALUE_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_PE_RATIO),
                        valuation,
                        "pe_ratio"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_PE_RATIO_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_EV_EBITDA),
                        valuation,
                        "ev_to_ebitda"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_EV_EBITDA_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_EARNINGS_YIELD),
                        valuation,
                        "earnings_yield"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_EARNINGS_YIELD_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_FCF_YIELD),
                        valuation,
                        "fcf_yield"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_FCF_YIELD_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_DIVIDEND_YIELD),
                        valuation,
                        "dividend_yield"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_DIVIDEND_YIELD_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_DIVIDEND_RATE),
                        valuation,
                        "dividend_rate"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_DIVIDEND_RATE_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_PAYOUT_RATIO),
                        valuation,
                        "payout_ratio"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_PAYOUT_RATIO_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_GRAHAM_NUMBER),
                        valuation,
                        "graham_number"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_GRAHAM_NUMBER_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_GRAHAM_FAIR_VALUE),
                        valuation,
                        "graham_fair_value"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_GRAHAM_FAIR_VALUE_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_MARGIN_OF_SAFETY),
                        valuation,
                        "margin_of_safety"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_MARGIN_OF_SAFETY_TOOLTIP));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll-pane");

        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Create Growth tab
     */
    private Tab createGrowthTab(JSONObject growth) {
        Tab tab =
                new Tab(i18nService.tr(Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_GROWTH));
        tab.setClosable(false);

        FlowPane content = new FlowPane(15, 15);
        content.setPadding(new Insets(20));

        // Revenue growth is nested
        if (growth.has("revenue_growth")) {
            JSONObject revenueGrowth = growth.getJSONObject("revenue_growth");
            UIUtils.addTooltipToNode(
                    addMetricToContainer(
                            content,
                            i18nService.tr(
                                    Constants.TranslationKeys
                                            .FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_YOY),
                            revenueGrowth,
                            "yoy_growth"),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_YOY_TOOLTIP));
            UIUtils.addTooltipToNode(
                    addMetricToContainer(
                            content,
                            i18nService.tr(
                                    Constants.TranslationKeys
                                            .FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_CAGR),
                            revenueGrowth,
                            "cagr"),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_CAGR_TOOLTIP));

            if (revenueGrowth.has("years")) {
                // Create a simple metric object for years
                JSONObject yearsMetric = new JSONObject();
                yearsMetric.put("value", revenueGrowth.getInt("years"));
                yearsMetric.put("type", "number");
                yearsMetric.put("data_temporality", "calculated");
                UIUtils.addTooltipToNode(
                        addMetricToContainer(
                                content,
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_YEARS),
                                yearsMetric,
                                "value"),
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_YEARS_TOOLTIP));
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll-pane");

        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Create Debt tab
     */
    private Tab createDebtTab(JSONObject debt) {
        Tab tab = new Tab(i18nService.tr(Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_DEBT));
        tab.setClosable(false);

        FlowPane content = new FlowPane(15, 15);
        content.setPadding(new Insets(20));

        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_TOTAL_DEBT),
                        debt,
                        "total_debt"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_TOTAL_DEBT_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_NET_DEBT),
                        debt,
                        "net_debt"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_NET_DEBT_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_NET_DEBT_EBITDA),
                        debt,
                        "net_debt_to_ebitda"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_NET_DEBT_EBITDA_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_RATIO),
                        debt,
                        "current_ratio"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_RATIO_TOOLTIP));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll-pane");

        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Create Efficiency tab
     */
    private Tab createEfficiencyTab(JSONObject efficiency) {
        Tab tab =
                new Tab(
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_TAB_EFFICIENCY));
        tab.setClosable(false);

        FlowPane content = new FlowPane(15, 15);
        content.setPadding(new Insets(20));

        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_ASSET_TURNOVER),
                        efficiency,
                        "asset_turnover"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_ASSET_TURNOVER_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_EBITDA),
                        efficiency,
                        "ebitda"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_EBITDA_TOOLTIP));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll-pane");

        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Create Cash Generation tab
     */
    private Tab createCashGenerationTab(JSONObject cashGeneration) {
        Tab tab =
                new Tab(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_TAB_CASH_GENERATION));
        tab.setClosable(false);

        FlowPane content = new FlowPane(15, 15);
        content.setPadding(new Insets(20));

        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_FREE_CASH_FLOW),
                        cashGeneration,
                        "free_cash_flow"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_FREE_CASH_FLOW_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_OPERATING_CASH_FLOW),
                        cashGeneration,
                        "operating_cash_flow"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_OPERATING_CASH_FLOW_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CAPEX),
                        cashGeneration,
                        "capex"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CAPEX_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_FCF_NET_INCOME),
                        cashGeneration,
                        "fcf_to_net_income"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_FCF_NET_INCOME_TOOLTIP));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll-pane");

        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Create Price Performance tab
     */
    private Tab createPricePerformanceTab(JSONObject pricePerformance) {
        Tab tab =
                new Tab(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_TAB_PRICE_PERFORMANCE));
        tab.setClosable(false);

        FlowPane content = new FlowPane(15, 15);
        content.setPadding(new Insets(20));

        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_PRICE),
                        pricePerformance,
                        "current_price"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_PRICE_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DAY_HIGH),
                        pricePerformance,
                        "day_high"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DAY_HIGH_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DAY_LOW),
                        pricePerformance,
                        "day_low"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_DAY_LOW_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1D),
                        pricePerformance,
                        "change_1d"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1D_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_5D),
                        pricePerformance,
                        "change_5d"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_5D_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1M),
                        pricePerformance,
                        "change_1m"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1M_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_3M),
                        pricePerformance,
                        "change_3m"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_3M_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_6M),
                        pricePerformance,
                        "change_6m"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_6M_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1Y),
                        pricePerformance,
                        "change_52w"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1Y_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_YTD),
                        pricePerformance,
                        "change_ytd"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_YTD_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_52W_HIGH),
                        pricePerformance,
                        "week_52_high"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_52W_HIGH_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_52W_LOW),
                        pricePerformance,
                        "week_52_low"),
                i18nService.tr(
                        Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_52W_LOW_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_DISTANCE_52W_HIGH),
                        pricePerformance,
                        "distance_from_52w_high"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_DISTANCE_52W_HIGH_TOOLTIP));
        UIUtils.addTooltipToNode(
                addMetricToContainer(
                        content,
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .FUNDAMENTAL_ANALYSIS_METRIC_DISTANCE_52W_LOW),
                        pricePerformance,
                        "distance_from_52w_low"),
                i18nService.tr(
                        Constants.TranslationKeys
                                .FUNDAMENTAL_ANALYSIS_METRIC_DISTANCE_52W_LOW_TOOLTIP));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll-pane");

        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Add a metric to a container using FundamentalMetricPaneController
     */
    private Node addMetricToContainer(
            FlowPane container, String label, JSONObject data, String key) {

        if (!data.has(key)) {
            return null;
        }

        Object metricObj = data.get(key);

        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource(Constants.FUNDAMENTAL_METRIC_PANE_FXML));
            loader.setControllerFactory(springContext::getBean);

            VBox metricPane = loader.load();
            FundamentalMetricPaneController controller = loader.getController();

            // Update the pane with metric data, passing last update date for real-time data
            String lastUpdateDate =
                    currentAnalysis != null
                            ? currentAnalysis.getLastUpdate().toLocalDate().toString()
                            : null;
            controller.updateMetricPane(label, metricObj, lastUpdateDate);

            // Add to container
            container.getChildren().add(metricPane);

            return controller.getRoot();
        } catch (IOException e) {
            logger.error("Error loading metric pane for: " + label, e);
            return null;
        }
    }

    /**
     * Show/hide loading indicator
     */
    private void showLoading(boolean show) {
        Platform.runLater(
                () -> {
                    loadingContainer.setVisible(show);
                    loadingContainer.setManaged(show);
                    metricsTabPane.setVisible(!show);
                    metricsTabPane.setManaged(!show);
                });
    }

    /**
     * Handle refresh button click
     */
    @FXML
    private void handleRefresh() {
        loadAnalysis(true);
    }

    /**
     * Handle close button click
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) companyNameValueLabel.getScene().getWindow();
        stage.close();
    }
}
