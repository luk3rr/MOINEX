/*
 * Filename: SavingsOverviewController.java
 * Created on: February 18, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.NoArgsConstructor;
import org.moinex.chart.DoughnutChart;
import org.moinex.model.dto.AllocationDTO;
import org.moinex.model.dto.ProfitabilityMetricsDTO;
import org.moinex.model.dto.TickerPerformanceDTO;
import org.moinex.model.enums.AssetType;
import org.moinex.model.enums.TickerType;
import org.moinex.model.investment.Bond;
import org.moinex.model.investment.BrazilianMarketIndicators;
import org.moinex.model.investment.Dividend;
import org.moinex.model.investment.InvestmentTarget;
import org.moinex.model.investment.MarketQuotesAndCommodities;
import org.moinex.model.investment.Ticker;
import org.moinex.model.investment.TickerSale;
import org.moinex.service.BondService;
import org.moinex.service.I18nService;
import org.moinex.service.InvestmentPerformanceCalculationService;
import org.moinex.service.InvestmentTargetService;
import org.moinex.service.MarketService;
import org.moinex.service.TickerService;
import org.moinex.ui.common.ProfitabilityMetricsPaneController;
import org.moinex.ui.dialog.investment.EditInvestmentTargetController;
import org.moinex.util.Animation;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

@Controller
@NoArgsConstructor
public class SavingsOverviewController {

    @FXML private Text overviewTotalInvestedField;
    @FXML private Text overviewTabGainsField;
    @FXML private Text overviewTabLossesField;
    @FXML private Text overviewTabTotalValueField;
    @FXML private Label overviewTabSelicValueField;
    @FXML private Label overviewTabIPCALastMonthValueField;
    @FXML private Label overviewTabIPCALastMonthDescriptionField;
    @FXML private Label overviewTabIPCA12MonthsValueField;
    @FXML private Label overviewTabDollarValueField;
    @FXML private Label overviewTabEuroValueField;
    @FXML private Label overviewTabIbovespaValueField;
    @FXML private Label overviewTabBitcoinValueField;
    @FXML private Label overviewTabEthereumValueField;
    @FXML private Label overviewTabGoldValueField;
    @FXML private Label overviewTabSoybeanValueField;
    @FXML private Label overviewTabCoffeeValueField;
    @FXML private Label overviewTabWheatValueField;
    @FXML private Label overviewTabOilBrentValueField;
    @FXML private Label brazilianMarketIndicatorsLastUpdateValue;
    @FXML private Label marketQuotesLastUpdateValue;
    @FXML private Label commoditiesLastUpdateValue;
    @FXML private AnchorPane pieChartAnchorPane;
    @FXML private Button graphPrevButton;
    @FXML private Button graphNextButton;
    @FXML private Button recalculateInvestmentPerformanceButton;
    @FXML private ImageView recalculateInvestmentPerformanceButtonIcon;
    @FXML private Label overviewTabBottonPaneTitle;
    @FXML private HBox portfolioP2;
    @FXML private HBox portfolioP5;
    @FXML private javafx.scene.layout.VBox profitabilityMetricsPane;
    @FXML private ProfitabilityMetricsPaneController profitabilityMetricsPaneController;

    private ConfigurableApplicationContext springContext;
    private TickerService tickerService;
    private MarketService marketService;
    private I18nService i18nService;
    private InvestmentTargetService investmentTargetService;
    private BondService bondService;
    private InvestmentPerformanceCalculationService investmentPerformanceCalculationService;

    private List<Ticker> tickers;
    private List<Dividend> dividends;
    private BrazilianMarketIndicators brazilianMarketIndicators;
    private MarketQuotesAndCommodities marketQuotesAndCommodities;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Integer SCHEDULE_DELAY_IN_SECONDS = 30;
    private Integer scheduledUpdatingMarketQuotesRetries = 0;
    private boolean scheduledUpdatingMarketQuotes = false;
    private Integer scheduledUpdatingBrazilianIndicatorsRetries = 0;
    private boolean scheduledUpdatingBrazilianIndicators = false;
    private static final Integer MAX_RETRIES = 3;

    private Integer graphPaneCurrentPage = 0;
    private static final int TOP_PERFORMERS_LIMIT = 5;
    private static final int ALLOCATION_PANEL_CONTAINER_SPACING = 10;
    private static final int ALLOCATION_PANEL_COLUMNS_SPACING = 20;
    private static final int ALLOCATION_PANEL_ITEMS_SPACING = 8;
    private static final int ALLOCATION_BAR_CONTAINER_SPACING = 3;
    private static final int ALLOCATION_INFO_BOX_SPACING = 10;
    private static final double ALLOCATION_PROGRESS_BAR_HEIGHT = 10.0;
    private static final double ALLOCATION_FILLED_BAR_HEIGHT = 20.0;
    private static final double PERCENTAGE_DIVISOR = 100.0;

    private static final Logger logger = LoggerFactory.getLogger(SavingsOverviewController.class);

    @Autowired
    public SavingsOverviewController(
            TickerService tickerService,
            MarketService marketService,
            ConfigurableApplicationContext springContext,
            I18nService i18nService,
            InvestmentTargetService investmentTargetService,
            BondService bondService,
            InvestmentPerformanceCalculationService investmentPerformanceCalculationService) {
        this.tickerService = tickerService;
        this.marketService = marketService;
        this.springContext = springContext;
        this.i18nService = i18nService;
        this.investmentTargetService = investmentTargetService;
        this.bondService = bondService;
        this.investmentPerformanceCalculationService = investmentPerformanceCalculationService;
    }

    @FXML
    private void initialize() {
        loadBrazilianMarketIndicatorsFromDatabase();
        loadMarketQuotesAndCommoditiesFromDatabase();

        updateOverviewTabFields();
        updateBrazilianMarketIndicators();
        updateMarketQuotesAndCommodities();
        updateTopPerformersPanel();
        updateAllocationVsTargetPanel();
        updateProfitabilityMetricsPanel();
        updateDisplayGraphs();

        setGraphButtonsActions();
    }

    private void loadBrazilianMarketIndicatorsFromDatabase() {
        try {
            brazilianMarketIndicators = marketService.getBrazilianMarketIndicators();
            logger.info("Loaded Brazilian market indicators from the database");
        } catch (jakarta.persistence.EntityNotFoundException e) {
            marketService
                    .updateBrazilianMarketIndicatorsFromApiAsync()
                    .thenAccept(
                            bmi -> {
                                Platform.runLater(
                                        () -> {
                                            this.brazilianMarketIndicators = bmi;
                                            scheduledUpdatingBrazilianIndicatorsRetries = 0;
                                        });

                                logger.info("Updated Brazilian market indicators from the API");
                            })
                    .exceptionally(
                            ex -> {
                                Platform.runLater(
                                        this::schedulerRetryForUpdatingBrazilianIndicators);
                                logger.error(ex.getMessage());
                                return null;
                            });
        }
    }

    private void loadMarketQuotesAndCommoditiesFromDatabase() {
        try {
            marketQuotesAndCommodities = marketService.getMarketQuotesAndCommodities();
            logger.info("Loaded market quotes and commodities from the database");
        } catch (jakarta.persistence.EntityNotFoundException e) {
            marketService
                    .updateMarketQuotesAndCommoditiesFromApiAsync()
                    .thenAccept(
                            mqc -> {
                                Platform.runLater(
                                        () -> {
                                            this.marketQuotesAndCommodities = mqc;
                                            this.scheduledUpdatingMarketQuotesRetries = 0;
                                        });

                                logger.info("Updated market quotes and commodities from the API");
                            })
                    .exceptionally(
                            ex -> {
                                Platform.runLater(this::schedulerEntryForUpdatingMarketQuotes);
                                logger.error(ex.getMessage());
                                return null;
                            });
        }
    }

    private void loadTickersFromDatabase() {
        tickers = tickerService.getAllNonArchivedTickers();
    }

    private void loadDividendsFromDatabase() {
        dividends = tickerService.getAllDividends();
    }

    private void updateBrazilianMarketIndicators() {
        if (brazilianMarketIndicators == null) {
            return;
        }

        overviewTabSelicValueField.setText(
                UIUtils.formatPercentage(brazilianMarketIndicators.getSelicTarget(), i18nService));

        overviewTabIPCALastMonthValueField.setText(
                UIUtils.formatPercentage(
                        brazilianMarketIndicators.getIpcaLastMonth(), i18nService));

        overviewTabIPCALastMonthDescriptionField.setText(
                "IPCA " + brazilianMarketIndicators.getIpcaLastMonthReference());

        overviewTabIPCA12MonthsValueField.setText(
                UIUtils.formatPercentage(brazilianMarketIndicators.getIpca12Months(), i18nService));

        brazilianMarketIndicatorsLastUpdateValue.setText(
                UIUtils.formatDateTimeForDisplay(
                        brazilianMarketIndicators.getLastUpdate(), i18nService));
    }

    private void updateMarketQuotesAndCommodities() {
        if (marketQuotesAndCommodities == null) {
            return;
        }

        overviewTabDollarValueField.setText(
                UIUtils.formatCurrency(marketQuotesAndCommodities.getDollar()));

        overviewTabEuroValueField.setText(
                UIUtils.formatCurrency(marketQuotesAndCommodities.getEuro()));

        overviewTabIbovespaValueField.setText(
                UIUtils.formatCurrency(marketQuotesAndCommodities.getIbovespa()));

        overviewTabBitcoinValueField.setText(
                UIUtils.formatCurrency(marketQuotesAndCommodities.getBitcoin()));

        overviewTabEthereumValueField.setText(
                UIUtils.formatCurrency(marketQuotesAndCommodities.getEthereum()));

        overviewTabGoldValueField.setText(
                UIUtils.formatCurrency(marketQuotesAndCommodities.getGold()));

        overviewTabSoybeanValueField.setText(
                UIUtils.formatCurrency(marketQuotesAndCommodities.getSoybean()));

        overviewTabCoffeeValueField.setText(
                UIUtils.formatCurrency(marketQuotesAndCommodities.getCoffee()));

        overviewTabWheatValueField.setText(
                UIUtils.formatCurrency(marketQuotesAndCommodities.getWheat()));

        overviewTabOilBrentValueField.setText(
                UIUtils.formatCurrency(marketQuotesAndCommodities.getOilBrent()));

        marketQuotesLastUpdateValue.setText(
                UIUtils.formatDateTimeForDisplay(
                        marketQuotesAndCommodities.getLastUpdate(), i18nService));

        commoditiesLastUpdateValue.setText(
                UIUtils.formatDateTimeForDisplay(
                        marketQuotesAndCommodities.getLastUpdate(), i18nService));
    }

    private synchronized void schedulerEntryForUpdatingMarketQuotes() {
        if (scheduledUpdatingMarketQuotesRetries >= MAX_RETRIES) {
            logger.warn("Max retries reached for updating market quotes");
            return;
        }

        if (scheduledUpdatingMarketQuotes) {
            logger.warn("Already scheduled to update market quotes");
            return;
        }

        scheduledUpdatingMarketQuotes = true;
        scheduledUpdatingMarketQuotesRetries++;

        logger.info("Scheduling retry for updating market quotes");

        scheduler.schedule(
                () -> {
                    loadMarketQuotesAndCommoditiesFromDatabase();
                    scheduledUpdatingMarketQuotes = false;
                },
                SCHEDULE_DELAY_IN_SECONDS,
                TimeUnit.SECONDS);
    }

    private synchronized void schedulerRetryForUpdatingBrazilianIndicators() {
        if (scheduledUpdatingBrazilianIndicatorsRetries >= MAX_RETRIES) {
            logger.warn("Max retries reached for updating Brazilian market indicators");
            return;
        }

        if (scheduledUpdatingBrazilianIndicators) {
            logger.warn("Already scheduled to update Brazilian market indicators");
            return;
        }

        scheduledUpdatingBrazilianIndicators = true;
        scheduledUpdatingBrazilianIndicatorsRetries++;

        logger.info("Scheduling retry for updating Brazilian market indicators");

        scheduler.schedule(
                () -> {
                    loadBrazilianMarketIndicatorsFromDatabase();
                    scheduledUpdatingBrazilianIndicators = false;
                },
                SCHEDULE_DELAY_IN_SECONDS,
                TimeUnit.SECONDS);
    }

    private void updateInvestmentDistributionChart() {
        pieChartAnchorPane.getChildren().clear();

        Map<String, BigDecimal> investmentByType = calculateInvestmentDistributionByType();

        if (investmentByType.isEmpty()) {
            return;
        }

        BigDecimal totalInvestment =
                investmentByType.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (Map.Entry<String, BigDecimal> entry : investmentByType.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue().doubleValue()));
            }
        }

        DoughnutChart doughnutChart = new DoughnutChart(pieChartData);
        doughnutChart.setI18nService(i18nService);
        doughnutChart.setLabelsVisible(false);

        for (PieChart.Data data : doughnutChart.getData()) {
            Node node = data.getNode();

            BigDecimal value = BigDecimal.valueOf(data.getPieValue());
            BigDecimal percentage =
                    value.divide(totalInvestment, 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(PERCENTAGE_DIVISOR));

            String tooltipText =
                    data.getName()
                            + "\n"
                            + UIUtils.formatCurrency(value)
                            + " ("
                            + UIUtils.formatPercentage(percentage, i18nService)
                            + ")";

            UIUtils.addTooltipToNode(node, tooltipText);
        }

        UIUtils.applyDefaultChartStyle(doughnutChart);

        pieChartAnchorPane.getChildren().removeIf(this::isDoughnutChart);

        pieChartAnchorPane.getChildren().add(doughnutChart);

        AnchorPane.setTopAnchor(doughnutChart, 0.0);
        AnchorPane.setBottomAnchor(doughnutChart, 0.0);
        AnchorPane.setLeftAnchor(doughnutChart, 0.0);
        AnchorPane.setRightAnchor(doughnutChart, 0.0);
    }

    private Map<String, BigDecimal> calculateInvestmentDistributionByType() {
        Map<String, BigDecimal> investmentByType = new HashMap<>();

        List<Ticker> allTickers = tickerService.getAllNonArchivedTickers();
        List<Bond> allBonds = bondService.getAllNonArchivedBonds();

        for (Ticker ticker : allTickers) {
            BigDecimal tickerCurrentValue =
                    ticker.getCurrentQuantity().multiply(ticker.getCurrentUnitValue());

            String typeName = UIUtils.translateTickerType(ticker.getType(), i18nService);

            investmentByType.merge(typeName, tickerCurrentValue, BigDecimal::add);
        }

        for (Bond bond : allBonds) {
            BigDecimal bondInvestedValue = bondService.getInvestedValue(bond);
            BigDecimal bondAccumulatedInterest =
                    bondService.getTotalAccumulatedInterestByBondId(bond.getId());
            BigDecimal bondTotalValue = bondInvestedValue.add(bondAccumulatedInterest);
            String typeName = UIUtils.translateBondType(bond.getType(), i18nService);

            investmentByType.merge(typeName, bondTotalValue, BigDecimal::add);
        }

        return investmentByType;
    }

    private void setGraphButtonsActions() {
        graphPrevButton.setOnAction(
                event -> {
                    if (graphPaneCurrentPage > 0) {
                        graphPaneCurrentPage--;
                        updateDisplayGraphs();
                    }
                });

        graphNextButton.setOnAction(
                event -> {
                    if (graphPaneCurrentPage < 1) {
                        graphPaneCurrentPage++;
                        updateDisplayGraphs();
                    }
                });
    }

    private void updateDisplayGraphs() {
        pieChartAnchorPane.getChildren().clear();

        if (graphPaneCurrentPage == 0) {
            overviewTabBottonPaneTitle.setText(
                    i18nService.tr(Constants.TranslationKeys.SAVINGS_OVERVIEW_PORTFOLIO));
            updateInvestmentDistributionChart();
            recalculateInvestmentPerformanceButton.setVisible(false);
            recalculateInvestmentPerformanceButton.setManaged(false);
        } else if (graphPaneCurrentPage == 1) {
            overviewTabBottonPaneTitle.setText(
                    i18nService.tr(Constants.TranslationKeys.SAVINGS_INVESTMENT_PERFORMANCE));
            updateInvestmentPerformanceChart();
            recalculateInvestmentPerformanceButton.setVisible(true);
            recalculateInvestmentPerformanceButton.setManaged(true);
        }

        graphPrevButton.setDisable(graphPaneCurrentPage == 0);
        graphNextButton.setDisable(graphPaneCurrentPage >= 1);
    }

    @FXML
    public void handleRecalculateInvestmentPerformance() {
        logger.info("Starting investment performance recalculation...");

        setOffRecalculateInvestmentPerformanceButton();

        investmentPerformanceCalculationService
                .recalculateAllSnapshots()
                .thenRun(
                        () ->
                                Platform.runLater(
                                        () -> {
                                            logger.info(
                                                    "Investment performance recalculation"
                                                            + " completed, updating chart...");
                                            updateInvestmentPerformanceChart();
                                            setOnRecalculateInvestmentPerformanceButton();
                                        }))
                .exceptionally(
                        throwable -> {
                            logger.error(
                                    "Error during investment performance recalculation", throwable);
                            Platform.runLater(this::setOnRecalculateInvestmentPerformanceButton);
                            return null;
                        });
    }

    private void setOffRecalculateInvestmentPerformanceButton() {
        recalculateInvestmentPerformanceButtonIcon.setImage(
                new Image(
                        Objects.requireNonNull(getClass().getResource(Constants.LOADING_GIF))
                                .toExternalForm()));
        recalculateInvestmentPerformanceButton.setDisable(true);
        recalculateInvestmentPerformanceButton.setText(
                i18nService.tr(Constants.TranslationKeys.SAVINGS_BUTTON_RECALCULATING));
    }

    private void setOnRecalculateInvestmentPerformanceButton() {
        recalculateInvestmentPerformanceButton.setDisable(false);
        recalculateInvestmentPerformanceButtonIcon.setImage(
                new Image(
                        Objects.requireNonNull(getClass().getResource(Constants.RELOAD_ICON))
                                .toExternalForm()));
        recalculateInvestmentPerformanceButton.setText(
                i18nService.tr(Constants.TranslationKeys.SAVINGS_BUTTON_RECALCULATE));
    }

    private void updateInvestmentPerformanceChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        StackedBarChart<String, Number> stackedBarChart = new StackedBarChart<>(xAxis, yAxis);
        stackedBarChart.setLegendVisible(true);
        stackedBarChart.setVerticalGridLinesVisible(false);

        pieChartAnchorPane.getChildren().clear();
        pieChartAnchorPane.getChildren().add(stackedBarChart);

        AnchorPane.setTopAnchor(stackedBarChart, 0.0);
        AnchorPane.setBottomAnchor(stackedBarChart, 0.0);
        AnchorPane.setLeftAnchor(stackedBarChart, 0.0);
        AnchorPane.setRightAnchor(stackedBarChart, 0.0);

        stackedBarChart.getData().clear();

        XYChart.Series<String, Number> investedSeries = new XYChart.Series<>();
        investedSeries.setName(i18nService.tr(Constants.TranslationKeys.SAVINGS_INVESTED_VALUE));

        XYChart.Series<String, Number> capitalGainsSeries = new XYChart.Series<>();
        capitalGainsSeries.setName(
                i18nService.tr(Constants.TranslationKeys.SAVINGS_ACCUMULATED_CAPITAL_GAINS));

        var performanceData = investmentPerformanceCalculationService.getPerformanceData();

        Map<YearMonth, BigDecimal> monthlyInvested = performanceData.monthlyInvested();
        Map<YearMonth, BigDecimal> accumulatedGains = performanceData.accumulatedGains();
        Map<YearMonth, BigDecimal> monthlyGains = performanceData.monthlyGains();
        Map<YearMonth, BigDecimal> portfolioValues = performanceData.portfolioValues();

        YearMonth currentMonth = YearMonth.now();
        List<YearMonth> allMonths = new ArrayList<>();
        for (int i = Constants.XYBAR_CHART_MONTHS; i >= 0; i--) {
            allMonths.add(currentMonth.minusMonths(i));
        }

        DateTimeFormatter formatter = UIUtils.getShortMonthYearFormatter(i18nService.getLocale());

        BigDecimal lastInvested = BigDecimal.ZERO;
        BigDecimal lastAccumulatedGains = BigDecimal.ZERO;

        for (YearMonth month : allMonths) {
            String monthLabel = month.format(formatter);

            BigDecimal invested = monthlyInvested.getOrDefault(month, lastInvested);
            BigDecimal gains = accumulatedGains.getOrDefault(month, lastAccumulatedGains);

            if (monthlyInvested.containsKey(month)) {
                lastInvested = invested;
            }
            if (accumulatedGains.containsKey(month)) {
                lastAccumulatedGains = gains;
            }

            investedSeries.getData().add(new XYChart.Data<>(monthLabel, invested.doubleValue()));
            capitalGainsSeries.getData().add(new XYChart.Data<>(monthLabel, gains.doubleValue()));
        }

        stackedBarChart.getData().addAll(investedSeries, capitalGainsSeries);

        Double maxTotal =
                portfolioValues.values().stream()
                        .map(BigDecimal::doubleValue)
                        .max(Double::compare)
                        .orElse(0.0);

        Animation.setDynamicYAxisBounds(yAxis, maxTotal);

        UIUtils.formatCurrencyYAxis(yAxis);

        UIUtils.applyDefaultChartStyle(stackedBarChart);

        stackedBarChart.layout();

        BigDecimal previousPortfolio = BigDecimal.ZERO;

        for (int i = 0; i < investedSeries.getData().size(); i++) {
            XYChart.Data<String, Number> investedData = investedSeries.getData().get(i);
            XYChart.Data<String, Number> gainsData = capitalGainsSeries.getData().get(i);

            YearMonth yearMonth = allMonths.get(i);
            BigDecimal portfolio = portfolioValues.getOrDefault(yearMonth, BigDecimal.ZERO);
            BigDecimal invested = BigDecimal.valueOf((Double) investedData.getYValue());
            BigDecimal accumulatedGain = accumulatedGains.getOrDefault(yearMonth, BigDecimal.ZERO);
            BigDecimal monthlyGain = monthlyGains.getOrDefault(yearMonth, BigDecimal.ZERO);

            String tooltipText =
                    i18nService.tr(Constants.TranslationKeys.SAVINGS_OVERVIEW_PORTFOLIO)
                            + ": "
                            + UIUtils.formatCurrency(portfolio)
                            + "\n"
                            + i18nService.tr(Constants.TranslationKeys.SAVINGS_INVESTED_VALUE)
                            + ": "
                            + UIUtils.formatCurrency(invested)
                            + "\n"
                            + i18nService.tr(
                                    Constants.TranslationKeys.SAVINGS_ACCUMULATED_CAPITAL_GAINS)
                            + ": "
                            + UIUtils.formatCurrency(accumulatedGain)
                            + "\n"
                            + i18nService.tr(
                                    Constants.TranslationKeys.SAVINGS_MONTHLY_CAPITAL_GAINS)
                            + ": "
                            + UIUtils.formatCurrency(monthlyGain);

            if (i > 0 && previousPortfolio.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal change = portfolio.subtract(previousPortfolio);
                BigDecimal percentageChange =
                        change.divide(previousPortfolio, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(PERCENTAGE_DIVISOR));

                String sign = percentageChange.compareTo(BigDecimal.ZERO) >= 0 ? "+ " : "- ";
                tooltipText +=
                        "\n"
                                + i18nService.tr(Constants.TranslationKeys.SAVINGS_VARIATION)
                                + ": "
                                + sign
                                + UIUtils.formatPercentage(percentageChange, i18nService);
            }

            if (investedData.getNode() != null) {
                UIUtils.addTooltipToNode(investedData.getNode(), tooltipText);
            }
            if (gainsData.getNode() != null) {
                UIUtils.addTooltipToNode(gainsData.getNode(), tooltipText);
            }

            previousPortfolio = portfolio;
        }
    }

    private void updateOverviewTabFields() {
        loadTickersFromDatabase();
        loadDividendsFromDatabase();

        logger.info(
                "Overview calculation: tickers={}, dividends={}",
                tickers != null ? tickers.size() : 0,
                dividends != null ? dividends.size() : 0);

        BigDecimal totalInvested =
                tickers.stream()
                        .map(t -> t.getAverageUnitValue().multiply(t.getCurrentQuantity()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal portfolioCurrentValue =
                tickers.stream()
                        .map(t -> t.getCurrentQuantity().multiply(t.getCurrentUnitValue()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tickerUnrealizedGains = BigDecimal.ZERO;
        BigDecimal tickerUnrealizedLosses = BigDecimal.ZERO;

        for (Ticker ticker : tickers) {
            BigDecimal invested =
                    ticker.getAverageUnitValue().multiply(ticker.getCurrentQuantity());
            BigDecimal current = ticker.getCurrentQuantity().multiply(ticker.getCurrentUnitValue());
            BigDecimal profitLoss = current.subtract(invested);

            logger.debug(
                    "Overview ticker {} ({}): qty={}, avg={}, current={}, invested={}, value={},"
                            + " pl={}",
                    ticker.getSymbol(),
                    ticker.getName(),
                    ticker.getCurrentQuantity(),
                    ticker.getAverageUnitValue(),
                    ticker.getCurrentUnitValue(),
                    invested,
                    current,
                    profitLoss);

            if (profitLoss.compareTo(BigDecimal.ZERO) > 0) {
                tickerUnrealizedGains = tickerUnrealizedGains.add(profitLoss);
            } else {
                tickerUnrealizedLosses = tickerUnrealizedLosses.add(profitLoss.abs());
            }
        }

        logger.info(
                "Overview tickers unrealized: gains={}, losses={} (gross)",
                tickerUnrealizedGains,
                tickerUnrealizedLosses);

        BigDecimal bondsTotalInvested = bondService.getTotalInvestedValue();
        BigDecimal bondsAccumulatedInterest = bondService.getAllBondsTotalAccumulatedInterest();

        logger.info(
                "Overview bonds: investedValue={}, currentValueAssumed={}, accumulatedInterest={}",
                bondsTotalInvested,
                bondsTotalInvested,
                bondsAccumulatedInterest);

        totalInvested = totalInvested.add(bondsTotalInvested);
        portfolioCurrentValue =
                portfolioCurrentValue.add(bondsTotalInvested).add(bondsAccumulatedInterest);

        BigDecimal totalDividends =
                dividends.stream()
                        .map(d -> d.getWalletTransaction().getAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        logger.info("Overview dividends: count={}, total={}", dividends.size(), totalDividends);

        List<TickerSale> tickerSales = tickerService.getAllNonArchivedSales();
        BigDecimal tickerRealizedProfit =
                tickerSales.stream()
                        .map(
                                sale -> {
                                    BigDecimal saleValue =
                                            sale.getUnitPrice().multiply(sale.getQuantity());
                                    BigDecimal costBasis =
                                            sale.getAverageCost().multiply(sale.getQuantity());
                                    return saleValue.subtract(costBasis);
                                })
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        logger.info(
                "Overview ticker sales: count={}, realizedProfitLoss={}",
                tickerSales.size(),
                tickerRealizedProfit);

        BigDecimal totalGains = BigDecimal.ZERO;
        BigDecimal totalLosses = BigDecimal.ZERO;

        totalGains = totalGains.add(tickerUnrealizedGains);
        totalLosses = totalLosses.add(tickerUnrealizedLosses);

        totalGains = totalGains.add(totalDividends);

        totalGains = totalGains.add(bondsAccumulatedInterest);

        if (tickerRealizedProfit.compareTo(BigDecimal.ZERO) > 0) {
            totalGains = totalGains.add(tickerRealizedProfit);
        } else {
            totalLosses = totalLosses.add(tickerRealizedProfit.abs());
        }

        logger.info(
                "Overview result: totalInvested={}, totalValue={}, gains={}, losses={} (gross)",
                totalInvested,
                portfolioCurrentValue,
                totalGains,
                totalLosses);

        overviewTotalInvestedField.setText(UIUtils.formatCurrency(totalInvested));
        overviewTabGainsField.setText(UIUtils.formatCurrency(totalGains));
        overviewTabLossesField.setText(UIUtils.formatCurrency(totalLosses));
        overviewTabTotalValueField.setText(UIUtils.formatCurrency(portfolioCurrentValue));
    }

    private boolean isDoughnutChart(Node node) {
        return node instanceof DoughnutChart;
    }

    private ProfitabilityMetricsDTO calculateProfitabilityMetrics() {
        loadTickersFromDatabase();
        loadDividendsFromDatabase();

        BigDecimal totalInvested =
                tickers.stream()
                        .map(t -> t.getAverageUnitValue().multiply(t.getCurrentQuantity()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentValue =
                tickers.stream()
                        .map(t -> t.getCurrentQuantity().multiply(t.getCurrentUnitValue()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profitLoss = currentValue.subtract(totalInvested);

        BigDecimal returnPercentage =
                totalInvested.compareTo(BigDecimal.ZERO) > 0
                        ? profitLoss
                                .divide(totalInvested, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(PERCENTAGE_DIVISOR))
                        : BigDecimal.ZERO;

        BigDecimal totalDividends =
                dividends.stream()
                        .map(d -> d.getWalletTransaction().getAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dividendYield =
                totalInvested.compareTo(BigDecimal.ZERO) > 0
                        ? totalDividends
                                .divide(totalInvested, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(PERCENTAGE_DIVISOR))
                        : BigDecimal.ZERO;

        return new ProfitabilityMetricsDTO(
                totalInvested,
                currentValue,
                profitLoss,
                returnPercentage,
                dividendYield,
                totalDividends);
    }

    private Map<String, ProfitabilityMetricsDTO> calculateProfitabilityMetricsByType() {
        loadTickersFromDatabase();
        loadDividendsFromDatabase();

        BigDecimal variableInvested =
                tickers.stream()
                        .map(t -> t.getAverageUnitValue().multiply(t.getCurrentQuantity()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variableCurrentValue =
                tickers.stream()
                        .map(t -> t.getCurrentQuantity().multiply(t.getCurrentUnitValue()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variableProfitLoss = variableCurrentValue.subtract(variableInvested);

        BigDecimal variableReturnPercentage =
                variableInvested.compareTo(BigDecimal.ZERO) > 0
                        ? variableProfitLoss
                                .divide(variableInvested, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(PERCENTAGE_DIVISOR))
                        : BigDecimal.ZERO;

        BigDecimal totalDividends =
                dividends.stream()
                        .map(d -> d.getWalletTransaction().getAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variableDividendYield =
                variableInvested.compareTo(BigDecimal.ZERO) > 0
                        ? totalDividends
                                .divide(variableInvested, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(PERCENTAGE_DIVISOR))
                        : BigDecimal.ZERO;

        BigDecimal fixedInvested = bondService.getTotalInvestedValue();
        BigDecimal fixedAccumulatedInterest = bondService.getAllBondsTotalAccumulatedInterest();
        BigDecimal fixedCurrentValue = fixedInvested.add(fixedAccumulatedInterest);
        BigDecimal fixedProfitLoss = fixedAccumulatedInterest;

        BigDecimal fixedReturnPercentage =
                fixedInvested.compareTo(BigDecimal.ZERO) > 0
                        ? fixedProfitLoss
                                .divide(fixedInvested, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(PERCENTAGE_DIVISOR))
                        : BigDecimal.ZERO;

        BigDecimal totalInvested = variableInvested.add(fixedInvested);
        BigDecimal totalCurrentValue = variableCurrentValue.add(fixedCurrentValue);
        BigDecimal totalProfitLoss = variableProfitLoss.add(fixedProfitLoss);

        BigDecimal totalReturnPercentage =
                totalInvested.compareTo(BigDecimal.ZERO) > 0
                        ? totalProfitLoss
                                .divide(totalInvested, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(PERCENTAGE_DIVISOR))
                        : BigDecimal.ZERO;

        Map<String, ProfitabilityMetricsDTO> metrics = new HashMap<>();
        metrics.put(
                "variable",
                new ProfitabilityMetricsDTO(
                        variableInvested,
                        variableCurrentValue,
                        variableProfitLoss,
                        variableReturnPercentage,
                        variableDividendYield,
                        totalDividends));
        metrics.put(
                "fixed",
                new ProfitabilityMetricsDTO(
                        fixedInvested,
                        fixedCurrentValue,
                        fixedProfitLoss,
                        fixedReturnPercentage,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO));
        metrics.put(
                "total",
                new ProfitabilityMetricsDTO(
                        totalInvested,
                        totalCurrentValue,
                        totalProfitLoss,
                        totalReturnPercentage,
                        variableDividendYield,
                        totalDividends));

        return metrics;
    }

    private List<TickerPerformanceDTO> calculateTopPerformers(int limit, boolean best) {
        loadTickersFromDatabase();

        return tickers.stream()
                .filter(t -> t.getCurrentQuantity().compareTo(BigDecimal.ZERO) > 0)
                .map(
                        t -> {
                            BigDecimal invested =
                                    t.getAverageUnitValue().multiply(t.getCurrentQuantity());
                            BigDecimal current =
                                    t.getCurrentQuantity().multiply(t.getCurrentUnitValue());
                            BigDecimal profitLoss = current.subtract(invested);
                            BigDecimal percentage =
                                    invested.compareTo(BigDecimal.ZERO) > 0
                                            ? profitLoss
                                                    .divide(invested, 4, RoundingMode.HALF_UP)
                                                    .multiply(
                                                            BigDecimal.valueOf(PERCENTAGE_DIVISOR))
                                            : BigDecimal.ZERO;

                            return new TickerPerformanceDTO(
                                    t.getName(), t.getSymbol(), percentage, profitLoss, current);
                        })
                .sorted(
                        best
                                ? Comparator.comparing(TickerPerformanceDTO::profitLossPercentage)
                                        .reversed()
                                : Comparator.comparing(TickerPerformanceDTO::profitLossPercentage))
                .limit(limit)
                .toList();
    }

    private void updateProfitabilityMetricsPanel() {
        Map<String, ProfitabilityMetricsDTO> metrics = calculateProfitabilityMetricsByType();
        ProfitabilityMetricsDTO variableMetrics = metrics.get("variable");
        ProfitabilityMetricsDTO fixedMetrics = metrics.get("fixed");
        ProfitabilityMetricsDTO totalMetrics = metrics.get("total");

        profitabilityMetricsPaneController.setMetrics(variableMetrics, fixedMetrics, totalMetrics);
    }

    private void updateTopPerformersPanel() {
        portfolioP2.getChildren().clear();

        VBox container = new VBox(30);
        container.setAlignment(Pos.CENTER);

        List<TickerPerformanceDTO> bestPerformers =
                calculateTopPerformers(TOP_PERFORMERS_LIMIT, true);
        List<TickerPerformanceDTO> worstPerformers =
                calculateTopPerformers(TOP_PERFORMERS_LIMIT, false);

        VBox bestBox = new VBox(5);
        Label bestLabel =
                new Label(i18nService.tr(Constants.TranslationKeys.SAVINGS_TOP_PERFORMERS_BEST));
        bestLabel.getStyleClass().add(Constants.CUSTOM_TABLE_TITLE_STYLE);
        bestLabel.setAlignment(Pos.CENTER);
        bestBox.getChildren().add(bestLabel);
        bestBox.setAlignment(Pos.CENTER);

        bestBox.getChildren().add(createTableHeader());

        for (TickerPerformanceDTO performer : bestPerformers) {
            bestBox.getChildren().add(createPerformerRow(performer));
        }

        VBox worstBox = new VBox(5);
        Label worstLabel =
                new Label(i18nService.tr(Constants.TranslationKeys.SAVINGS_TOP_PERFORMERS_WORST));
        worstLabel.getStyleClass().add(Constants.CUSTOM_TABLE_TITLE_STYLE);
        worstLabel.setAlignment(Pos.CENTER);
        worstBox.getChildren().add(worstLabel);
        worstBox.setAlignment(Pos.CENTER);

        worstBox.getChildren().add(createTableHeader());

        for (TickerPerformanceDTO performer : worstPerformers) {
            worstBox.getChildren().add(createPerformerRow(performer));
        }

        container.getChildren().addAll(bestBox, worstBox);

        portfolioP2.getChildren().add(container);
        HBox.setHgrow(container, Priority.ALWAYS);
    }

    private HBox createTableHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label assetHeader =
                new Label(
                        i18nService.tr(
                                Constants.TranslationKeys.SAVINGS_TOP_PERFORMERS_HEADER_ASSET));
        assetHeader.getStyleClass().add(Constants.CUSTOM_TABLE_HEADER_STYLE);
        configureColumnWidth(assetHeader, Constants.TOP_PERFORMERS_ASSET_COLUMN_WIDTH);
        assetHeader.setAlignment(Pos.CENTER_LEFT);

        Region spacerA = new Region();
        HBox.setHgrow(spacerA, Priority.ALWAYS);

        Region spacerB = new Region();
        HBox.setHgrow(spacerB, Priority.ALWAYS);

        Label returnHeader =
                new Label(
                        i18nService.tr(
                                Constants.TranslationKeys.SAVINGS_TOP_PERFORMERS_HEADER_RETURN));
        returnHeader.getStyleClass().add(Constants.CUSTOM_TABLE_HEADER_STYLE);
        configureColumnWidth(returnHeader, Constants.TOP_PERFORMERS_RETURN_COLUMN_WIDTH);
        returnHeader.setAlignment(Pos.CENTER);

        Label valueHeader =
                new Label(
                        i18nService.tr(
                                Constants.TranslationKeys.SAVINGS_TOP_PERFORMERS_HEADER_VALUE));
        valueHeader.getStyleClass().add(Constants.CUSTOM_TABLE_HEADER_STYLE);
        configureColumnWidth(valueHeader, Constants.TOP_PERFORMERS_VALUE_COLUMN_WIDTH);
        valueHeader.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(assetHeader, spacerA, returnHeader, spacerB, valueHeader);

        return header;
    }

    private void configureColumnWidth(Label label, Double width) {
        label.setMinWidth(width);
        label.setMaxWidth(width);
    }

    private HBox createPerformerRow(TickerPerformanceDTO performer) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label symbolLabel = new Label(performer.symbol());
        symbolLabel.getStyleClass().add(Constants.CUSTOM_TABLE_CELL_STYLE);
        configureColumnWidth(symbolLabel, Constants.TOP_PERFORMERS_ASSET_COLUMN_WIDTH);
        symbolLabel.setAlignment(Pos.CENTER_LEFT);

        Region spacerA = new Region();
        HBox.setHgrow(spacerA, Priority.ALWAYS);

        Region spacerB = new Region();
        HBox.setHgrow(spacerB, Priority.ALWAYS);

        Label percentageLabel =
                new Label(
                        performer.getSign()
                                + UIUtils.formatPercentage(
                                        performer.profitLossPercentage(), i18nService));
        percentageLabel.getStyleClass().add(Constants.CUSTOM_TABLE_CELL_STYLE);

        if (performer.isPositive()) {
            percentageLabel.getStyleClass().add(Constants.INFO_LABEL_GREEN_STYLE);
        } else if (performer.isNegative()) {
            percentageLabel.getStyleClass().add(Constants.INFO_LABEL_RED_STYLE);
        } else {
            percentageLabel.getStyleClass().add(Constants.INFO_LABEL_NEUTRAL_STYLE);
        }

        configureColumnWidth(percentageLabel, Constants.TOP_PERFORMERS_RETURN_COLUMN_WIDTH);
        percentageLabel.setAlignment(Pos.CENTER);

        Label valueLabel = new Label(UIUtils.formatCurrencyDynamic(performer.currentValue()));
        valueLabel.getStyleClass().add(Constants.CUSTOM_TABLE_CELL_STYLE);
        configureColumnWidth(valueLabel, Constants.TOP_PERFORMERS_VALUE_COLUMN_WIDTH);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(symbolLabel, spacerA, percentageLabel, spacerB, valueLabel);

        return row;
    }

    private List<AllocationDTO> calculateAllocationVsTarget() {
        Map<TickerType, BigDecimal> currentAllocation = new HashMap<>();

        BigDecimal totalBondValue =
                bondService
                        .getTotalInvestedValue()
                        .add(bondService.getAllBondsTotalAccumulatedInterest());

        BigDecimal totalTickerValue = BigDecimal.ZERO;

        for (Ticker ticker : tickers) {
            BigDecimal value = ticker.getCurrentQuantity().multiply(ticker.getCurrentUnitValue());
            totalTickerValue = totalTickerValue.add(value);
            currentAllocation.merge(ticker.getType(), value, BigDecimal::add);
        }

        BigDecimal totalValue = totalTickerValue.add(totalBondValue);

        List<InvestmentTarget> targets = investmentTargetService.getAllActiveTargets();

        List<AllocationDTO> allocations = new ArrayList<>();

        for (InvestmentTarget target : targets) {
            AssetType assetType = target.getAssetType();
            BigDecimal currentValue;
            String typeName;

            if (assetType == AssetType.BOND) {
                currentValue = totalBondValue;
                typeName = i18nService.tr(Constants.TranslationKeys.ASSET_TYPE_BOND);
            } else {
                TickerType tickerType = TickerType.valueOf(assetType.name());
                currentValue = currentAllocation.getOrDefault(tickerType, BigDecimal.ZERO);
                typeName = UIUtils.translateTickerType(tickerType, i18nService);
            }

            BigDecimal currentPercentage =
                    totalValue.compareTo(BigDecimal.ZERO) > 0
                            ? currentValue
                                    .divide(totalValue, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(PERCENTAGE_DIVISOR))
                            : BigDecimal.ZERO;

            BigDecimal difference = currentPercentage.subtract(target.getTargetPercentage());

            allocations.add(
                    new AllocationDTO(
                            assetType,
                            typeName,
                            currentPercentage,
                            target.getTargetPercentage(),
                            currentValue,
                            difference));
        }

        return allocations;
    }

    private void updateAllocationVsTargetPanel() {
        portfolioP5.getChildren().clear();

        VBox container = new VBox(ALLOCATION_PANEL_CONTAINER_SPACING);
        container.setAlignment(Pos.CENTER);
        container.setStyle("-fx-padding: " + ALLOCATION_PANEL_CONTAINER_SPACING + ";");

        List<AllocationDTO> allocations = calculateAllocationVsTarget();

        GridPane gridPane = new GridPane();
        gridPane.setHgap(ALLOCATION_PANEL_COLUMNS_SPACING);
        gridPane.setAlignment(Pos.CENTER);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setMinWidth(20);
        col1.setHgrow(Priority.ALWAYS);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setMinWidth(20);
        col2.setHgrow(Priority.ALWAYS);

        gridPane.getColumnConstraints().addAll(col1, col2);

        VBox leftColumn = new VBox(ALLOCATION_PANEL_ITEMS_SPACING);
        leftColumn.setAlignment(Pos.CENTER_LEFT);

        VBox rightColumn = new VBox(ALLOCATION_PANEL_ITEMS_SPACING);
        rightColumn.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < allocations.size(); i++) {
            if (i % 2 == 0) {
                leftColumn.getChildren().add(createAllocationBar(allocations.get(i)));
            } else {
                rightColumn.getChildren().add(createAllocationBar(allocations.get(i)));
            }
        }

        gridPane.add(leftColumn, 0, 0);
        gridPane.add(rightColumn, 1, 0);

        container.getChildren().add(gridPane);

        portfolioP5.getChildren().add(container);
        HBox.setHgrow(container, Priority.ALWAYS);
    }

    private VBox createAllocationBar(AllocationDTO allocation) {
        VBox barContainer = new VBox(ALLOCATION_BAR_CONTAINER_SPACING);

        Label typeLabel = new Label(allocation.typeName());
        typeLabel.getStyleClass().add(Constants.ALLOCATION_TYPE_LABEL_STYLE);

        HBox progressBar = new HBox();
        progressBar.getStyleClass().add(Constants.ALLOCATION_PROGRESS_BAR_STYLE);
        progressBar.setPrefHeight(ALLOCATION_PROGRESS_BAR_HEIGHT);

        BigDecimal achievementPercentage = allocation.getAchievementPercentage();
        double fillPercentage = achievementPercentage.doubleValue();

        if (achievementPercentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
            fillPercentage = 100.0;
        }

        HBox filledBar = new HBox();

        if (allocation.isCriticalLow()) {
            filledBar.getStyleClass().add(Constants.ALLOCATION_FILLED_BAR_CRITICAL_LOW_STYLE);
        } else if (allocation.isWarningLow()) {
            filledBar.getStyleClass().add(Constants.ALLOCATION_FILLED_BAR_WARNING_LOW_STYLE);
        } else if (allocation.isOnTargetRange()) {
            filledBar.getStyleClass().add(Constants.ALLOCATION_FILLED_BAR_ON_TARGET_STYLE);
        } else if (allocation.isWarningHigh()) {
            filledBar.getStyleClass().add(Constants.ALLOCATION_FILLED_BAR_WARNING_HIGH_STYLE);
        } else if (allocation.isCriticalHigh()) {
            filledBar.getStyleClass().add(Constants.ALLOCATION_FILLED_BAR_CRITICAL_HIGH_STYLE);
        }

        filledBar.setPrefHeight(ALLOCATION_FILLED_BAR_HEIGHT);
        filledBar
                .prefWidthProperty()
                .bind(progressBar.widthProperty().multiply(fillPercentage / PERCENTAGE_DIVISOR));

        progressBar.getChildren().add(filledBar);

        HBox infoBox = new HBox(ALLOCATION_INFO_BOX_SPACING);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label currentLabel =
                new Label(
                        UIUtils.formatPercentage(allocation.currentPercentage(), i18nService)
                                + " / "
                                + UIUtils.formatPercentage(
                                        allocation.targetPercentage(), i18nService));
        currentLabel.getStyleClass().add(Constants.ALLOCATION_INFO_LABEL_STYLE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String statusText = getStatusText(allocation);
        Label statusLabel = new Label(statusText);
        statusLabel.getStyleClass().add(Constants.ALLOCATION_DIFF_LABEL_STYLE);

        if (!allocation.isNotInStrategy()) {
            if (allocation.isCriticalLow()) {
                statusLabel.getStyleClass().add(Constants.ALLOCATION_DIFF_CRITICAL_LOW_STYLE);
            } else if (allocation.isWarningLow()) {
                statusLabel.getStyleClass().add(Constants.ALLOCATION_DIFF_WARNING_LOW_STYLE);
            } else if (allocation.isOnTargetRange()) {
                statusLabel.getStyleClass().add(Constants.ALLOCATION_DIFF_ON_TARGET_STYLE);
            } else if (allocation.isWarningHigh()) {
                statusLabel.getStyleClass().add(Constants.ALLOCATION_DIFF_WARNING_HIGH_STYLE);
            } else if (allocation.isCriticalHigh()) {
                statusLabel.getStyleClass().add(Constants.ALLOCATION_DIFF_CRITICAL_HIGH_STYLE);
            }
        }

        infoBox.getChildren().addAll(currentLabel, spacer, statusLabel);

        barContainer.getChildren().addAll(typeLabel, progressBar, infoBox);

        return barContainer;
    }

    private String getStatusText(AllocationDTO allocation) {
        if (allocation.isNotInStrategy()) {
            return "";
        }

        if (allocation.isOnTargetRange()) {
            return i18nService.tr(Constants.TranslationKeys.SAVINGS_ALLOCATION_STATUS_ON_TARGET);
        }

        BigDecimal absDifference = allocation.difference().abs();
        String formattedDiff = UIUtils.formatPercentage(absDifference, i18nService);

        if (allocation.isCriticalLow()) {
            return i18nService.tr(Constants.TranslationKeys.SAVINGS_ALLOCATION_STATUS_CRITICAL_LOW)
                    + " "
                    + formattedDiff;
        } else if (allocation.isWarningLow()) {
            return i18nService.tr(Constants.TranslationKeys.SAVINGS_ALLOCATION_STATUS_WARNING_LOW)
                    + " "
                    + formattedDiff;
        } else if (allocation.isWarningHigh()) {
            return i18nService.tr(Constants.TranslationKeys.SAVINGS_ALLOCATION_STATUS_WARNING_HIGH)
                    + " "
                    + formattedDiff;
        } else if (allocation.isCriticalHigh()) {
            return i18nService.tr(Constants.TranslationKeys.SAVINGS_ALLOCATION_STATUS_CRITICAL_HIGH)
                    + " "
                    + formattedDiff;
        }

        return "";
    }

    @FXML
    private void handleEditInvestmentTarget() {
        WindowUtils.openModalWindow(
                Constants.EDIT_INVESTMENT_TARGET_FXML,
                i18nService.tr(Constants.TranslationKeys.INVESTMENT_DIALOG_EDIT_TARGET_TITLE),
                springContext,
                (EditInvestmentTargetController controller) -> {},
                List.of(this::updateAllocationVsTargetPanel));
    }
}
