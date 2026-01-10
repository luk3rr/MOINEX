/*
 * Filename: FundamentalAnalysisController.java
 * Created on: January  9, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the Fundamental Analysis dialog
 * Displays comprehensive fundamental analysis data for a ticker
 */
@Controller
@NoArgsConstructor
public class FundamentalAnalysisController {
    private static final Logger logger = LoggerFactory.getLogger(FundamentalAnalysisController.class);

    @FXML private Label companyNameValueLabel;
    @FXML private Label sectorValueLabel;
    @FXML private Label industryValueLabel;
    @FXML private Label currencyValueLabel;
    @FXML private HBox cacheStatusContainer;
    @FXML private ComboBox<PeriodType> periodComboBox;
    @FXML private TabPane metricsTabPane;
    @FXML private VBox loadingContainer;
    @FXML private ProgressIndicator loadingIndicator;

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

        // Add listener to period change
        periodComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
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

        Platform.runLater(() -> {
            // Clear existing status labels (keep only the title)
            cacheStatusContainer.getChildren().removeIf(node -> node instanceof Label && 
                !((Label) node).getText().equals("Última Atualização:"));

            // Get all analyses for this ticker
            List<FundamentalAnalysis> analyses = fundamentalAnalysisService.getAllAnalysesForTicker(ticker.getId());

            // Create status label for each period type
            for (PeriodType periodType : PeriodType.values()) {
                Label statusLabel = new Label();
                statusLabel.setStyle("-fx-font-size: 11px; -fx-padding: 2 8 2 8; -fx-background-radius: 3;");

                // Find analysis for this period type
                Optional<FundamentalAnalysis> analysis = analyses.stream()
                    .filter(a -> a.getPeriodType() == periodType)
                    .findFirst();

                if (analysis.isPresent()) {
                    String lastUpdate = UIUtils.formatDateTimeForDisplay(
                        analysis.get().getLastUpdate(), 
                        i18nService
                    );
                    boolean expired = fundamentalAnalysisService.isCacheExpired(analysis.get());
                    
                    statusLabel.setText(periodType.name() + ": " + lastUpdate);
                    if (expired) {
                        statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #fff3cd; -fx-text-fill: #856404;");
                    } else {
                        statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #d4edda; -fx-text-fill: #155724;");
                    }
                } else {
                    statusLabel.setText(periodType.name() + ": --");
                    statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #e2e3e5; -fx-text-fill: #383d41;");
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

        // Create background task
        Task<FundamentalAnalysis> task = new Task<>() {
            @Override
            protected FundamentalAnalysis call() throws Exception {
                PeriodType period = periodComboBox.getValue();
                return fundamentalAnalysisService.getAnalysis(
                    ticker.getId(),
                    period,
                    forceRefresh
                );
            }
        };

        task.setOnSucceeded(event -> {
            currentAnalysis = task.getValue();
            displayAnalysis();
            updateCacheStatusIndicators();
            showLoading(false);
        });

        task.setOnFailed(event -> {
            showLoading(false);
            Throwable exception = task.getException();
            logger.error("Error loading fundamental analysis", exception);
            
            String errorMessage = exception.getMessage();
            String title = "Erro ao Carregar Análise";
            
            // Check if it's a network/API error
            if (errorMessage != null && (errorMessage.contains("Network is unreachable") || 
                                        errorMessage.contains("Connection") ||
                                        errorMessage.contains("Max retries exceeded") ||
                                        errorMessage.contains("API error after"))) {
                title = "Erro de Conexão";
                errorMessage = "Não foi possível conectar à API do Yahoo Finance após 3 tentativas.\n\n" +
                              "Possíveis causas:\n" +
                              "• Sem conexão com a internet\n" +
                              "• API temporariamente indisponível\n" +
                              "• Firewall bloqueando a conexão\n\n" +
                              "Tente novamente em alguns minutos.";
            } else {
                errorMessage = "Erro ao carregar análise fundamentalista:\n\n" + errorMessage;
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

        // Update basic info
        companyNameValueLabel.setText(currentAnalysis.getCompanyName());
        sectorValueLabel.setText(currentAnalysis.getSector());
        industryValueLabel.setText(currentAnalysis.getIndustry());
        currencyValueLabel.setText(currentAnalysis.getCurrency());

        // Parse JSON and populate tabs
        try {
            JSONObject data = new JSONObject(currentAnalysis.getDataJson());
            populateMetricsTabs(data);
        } catch (Exception e) {
            logger.error("Error parsing analysis JSON", e);
            WindowUtils.showErrorDialog(
                "Erro",
                "Erro ao processar dados da análise: " + e.getMessage()
            );
        }
    }

    /**
     * Populate all metrics tabs with data
     */
    private void populateMetricsTabs(JSONObject data) {
        metricsTabPane.getTabs().clear();

        // Create tabs for each category
        if (data.has("profitability")) {
            metricsTabPane.getTabs().add(createProfitabilityTab(data.getJSONObject("profitability")));
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
            metricsTabPane.getTabs().add(createCashGenerationTab(data.getJSONObject("cash_generation")));
        }

        if (data.has("price_performance")) {
            metricsTabPane.getTabs().add(createPricePerformanceTab(data.getJSONObject("price_performance")));
        }
    }

    /**
     * Create Profitability tab
     */
    private Tab createProfitabilityTab(JSONObject profitability) {
        Tab tab = new Tab("Rentabilidade");
        tab.setClosable(false);

        FlowPane content = new FlowPane(15, 15);
        content.setPadding(new Insets(20));

        addMetricToContainer(content, "ROE (Return on Equity)", profitability, "roe");
        addMetricToContainer(content, "ROIC (Return on Invested Capital)", profitability, "roic");
        addMetricToContainer(content, "Margem Líquida", profitability, "net_margin");
        addMetricToContainer(content, "Margem EBITDA", profitability, "ebitda_margin");

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Create Valuation tab
     */
    private Tab createValuationTab(JSONObject valuation) {
        Tab tab = new Tab("Valuation");
        tab.setClosable(false);

        FlowPane content = new FlowPane(15, 15);
        content.setPadding(new Insets(20));

        addMetricToContainer(content, "Preço Atual", valuation, "current_price");
        addMetricToContainer(content, "Market Cap", valuation, "market_cap");
        addMetricToContainer(content, "Enterprise Value", valuation, "enterprise_value");
        addMetricToContainer(content, "P/L (Price to Earnings)", valuation, "pe_ratio");
        addMetricToContainer(content, "EV/EBITDA", valuation, "ev_to_ebitda");
        addMetricToContainer(content, "Earnings Yield", valuation, "earnings_yield");
        addMetricToContainer(content, "FCF Yield", valuation, "fcf_yield");
        addMetricToContainer(content, "Dividend Yield", valuation, "dividend_yield");
        addMetricToContainer(content, "Dividend Rate", valuation, "dividend_rate");
        addMetricToContainer(content, "Payout Ratio", valuation, "payout_ratio");
        addMetricToContainer(content, "Graham Number", valuation, "graham_number");
        addMetricToContainer(content, "Valor Justo Graham", valuation, "graham_fair_value");
        addMetricToContainer(content, "Margem de Segurança", valuation, "margin_of_safety");

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Create Growth tab
     */
    private Tab createGrowthTab(JSONObject growth) {
        Tab tab = new Tab("Crescimento");
        tab.setClosable(false);

        FlowPane content = new FlowPane(15, 15);
        content.setPadding(new Insets(20));

        // Revenue growth is nested
        if (growth.has("revenue_growth")) {
            JSONObject revenueGrowth = growth.getJSONObject("revenue_growth");
            addMetricToContainer(content, "Crescimento de Receita (YoY)", revenueGrowth, "yoy_growth");
            addMetricToContainer(content, "Crescimento de Receita (CAGR)", revenueGrowth, "cagr");
            
            if (revenueGrowth.has("years")) {
                // Create a simple metric object for years
                JSONObject yearsMetric = new JSONObject();
                yearsMetric.put("value", revenueGrowth.getInt("years"));
                yearsMetric.put("type", "number");
                yearsMetric.put("data_temporality", "calculated");
                addMetricToContainer(content, "Anos de Dados", yearsMetric, "value");
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Create Debt tab
     */
    private Tab createDebtTab(JSONObject debt) {
        Tab tab = new Tab("Dívida");
        tab.setClosable(false);

        FlowPane content = new FlowPane(15, 15);
        content.setPadding(new Insets(20));

        addMetricToContainer(content, "Dívida Total", debt, "total_debt");
        addMetricToContainer(content, "Dívida Líquida", debt, "net_debt");
        addMetricToContainer(content, "Dívida Líquida / EBITDA", debt, "net_debt_to_ebitda");
        addMetricToContainer(content, "Índice de Liquidez Corrente", debt, "current_ratio");

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Create Efficiency tab
     */
    private Tab createEfficiencyTab(JSONObject efficiency) {
        Tab tab = new Tab("Eficiência");
        tab.setClosable(false);

        FlowPane content = new FlowPane(15, 15);
        content.setPadding(new Insets(20));

        addMetricToContainer(content, "Giro de Ativos", efficiency, "asset_turnover");
        addMetricToContainer(content, "EBITDA", efficiency, "ebitda");

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Create Cash Generation tab
     */
    private Tab createCashGenerationTab(JSONObject cashGeneration) {
        Tab tab = new Tab("Geração de Caixa");
        tab.setClosable(false);

        FlowPane content = new FlowPane(15, 15);
        content.setPadding(new Insets(20));

        addMetricToContainer(content, "Fluxo de Caixa Livre", cashGeneration, "free_cash_flow");
        addMetricToContainer(content, "Fluxo de Caixa Operacional", cashGeneration, "operating_cash_flow");
        addMetricToContainer(content, "CAPEX", cashGeneration, "capex");
        addMetricToContainer(content, "FCF / Lucro Líquido", cashGeneration, "fcf_to_net_income");

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Create Price Performance tab
     */
    private Tab createPricePerformanceTab(JSONObject pricePerformance) {
        Tab tab = new Tab("Desempenho de Preço");
        tab.setClosable(false);

        FlowPane content = new FlowPane(15, 15);
        content.setPadding(new Insets(20));

        addMetricToContainer(content, "Preço Atual", pricePerformance, "current_price");
        addMetricToContainer(content, "Máxima do Dia", pricePerformance, "day_high");
        addMetricToContainer(content, "Mínima do Dia", pricePerformance, "day_low");
        addMetricToContainer(content, "Variação 1 Dia", pricePerformance, "change_1d");
        addMetricToContainer(content, "Variação 5 Dias", pricePerformance, "change_5d");
        addMetricToContainer(content, "Variação 1 Mês", pricePerformance, "change_1m");
        addMetricToContainer(content, "Variação 3 Meses", pricePerformance, "change_3m");
        addMetricToContainer(content, "Variação 6 Meses", pricePerformance, "change_6m");
        addMetricToContainer(content, "Variação 52 Semanas", pricePerformance, "change_52w");
        addMetricToContainer(content, "Variação YTD", pricePerformance, "change_ytd");
        addMetricToContainer(content, "Máxima 52 Semanas", pricePerformance, "week_52_high");
        addMetricToContainer(content, "Mínima 52 Semanas", pricePerformance, "week_52_low");
        addMetricToContainer(content, "Distância da Máxima 52s", pricePerformance, "distance_from_52w_high");
        addMetricToContainer(content, "Distância da Mínima 52s", pricePerformance, "distance_from_52w_low");

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        tab.setContent(scrollPane);
        return tab;
    }

    /**
     * Add a metric to a container using FundamentalMetricPaneController
     */
    private void addMetricToContainer(FlowPane container, String label, JSONObject data, String key) {
        if (!data.has(key)) {
            return;
        }

        Object metricObj = data.get(key);
        
        try {
            // Load the FXML and controller
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(Constants.FUNDAMENTAL_METRIC_PANE_FXML)
            );
            loader.setControllerFactory(springContext::getBean);
            
            VBox metricPane = loader.load();
            FundamentalMetricPaneController controller = loader.getController();
            
            // Update the pane with metric data
            controller.updateMetricPane(label, metricObj);
            
            // Add to container
            container.getChildren().add(metricPane);
            
        } catch (IOException e) {
            logger.error("Error loading metric pane for: " + label, e);
        }
    }

    /**
     * Show/hide loading indicator
     */
    private void showLoading(boolean show) {
        Platform.runLater(() -> {
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
     * Handle export CSV button click
     */
    @FXML
    private void handleExportCSV() {
        // TODO: Implement CSV export
        WindowUtils.showInformationDialog(
            "Em Desenvolvimento",
            "Funcionalidade de exportação CSV será implementada em breve."
        );
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
