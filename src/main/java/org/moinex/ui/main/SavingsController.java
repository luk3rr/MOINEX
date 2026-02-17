/*
 * Filename: SavingsController.java
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import com.jfoenix.controls.JFXButton;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import lombok.NoArgsConstructor;
import org.moinex.chart.DoughnutChart;
import org.moinex.model.dto.AllocationDTO;
import org.moinex.model.dto.ProfitabilityMetricsDTO;
import org.moinex.model.dto.TickerPerformanceDTO;
import org.moinex.model.enums.AssetType;
import org.moinex.model.enums.BondType;
import org.moinex.model.enums.OperationType;
import org.moinex.model.enums.TickerType;
import org.moinex.model.investment.*;
import org.moinex.service.*;
import org.moinex.ui.dialog.investment.*;
import org.moinex.util.Animation;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller class for the Savings view
 */
@Controller
@NoArgsConstructor
public class SavingsController {
    @FXML private Text stocksFundsTabNetCapitalInvestedField;

    @FXML private Text stocksFundsTabCurrentValueField;

    @FXML private Text stocksFundsTabProfitLossField;

    @FXML private Text stocksFundsTabDividendsReceivedField;

    @FXML private TableView<Ticker> stocksFundsTabTickerTable;

    @FXML private TextField stocksFundsTabTickerSearchField;

    @FXML private ComboBox<TickerType> stocksFundsTabTickerTypeComboBox;

    @FXML private Text bondsTabTotalInvestedField;

    @FXML private Text bondsTabCurrentValueField;

    @FXML private Text bondsTabProfitLossField;

    @FXML private Text bondsTabInterestReceivedField;

    @FXML private TableView<Bond> bondsTabBondTable;

    @FXML private TextField bondsTabBondSearchField;

    @FXML private ComboBox<BondType> bondsTabBondTypeComboBox;

    @FXML private JFXButton updatePortfolioPricesButton;

    @FXML private ImageView updatePricesButtonIcon;

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

    @FXML private JFXButton graphPrevButton;

    @FXML private JFXButton graphNextButton;

    @FXML private Label overviewTabBottonPaneTitle;

    @FXML private Text overviewTotalInvestedField;

    @FXML private Text overviewTabGainsField;

    @FXML private Text overviewTabLossesField;

    @FXML private Text overviewTabTotalValueField;

    @FXML private HBox portfolioP2;

    @FXML private HBox portfolioP5;

    @FXML private HBox portfolioP4;

    private ConfigurableApplicationContext springContext;

    private boolean isUpdatingPortfolioPrices = false;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final Integer SCHEDULE_DELAY_IN_SECONDS = 30;

    private Integer scheduledUpdatingMarketQuotesRetries = 0;

    private boolean scheduledUpdatingMarketQuotes = false;

    private Integer scheduledUpdatingBrazilianIndicatorsRetries = 0;

    private boolean scheduledUpdatingBrazilianIndicators = false;

    private static final Integer MAX_RETRIES = 3;

    private TickerService tickerService;

    private MarketService marketService;

    private I18nService i18nService;

    private WalletService walletService;

    private InvestmentTargetService investmentTargetService;

    private BondService bondService;

    private List<Ticker> tickers;

    private List<Dividend> dividends;

    private BrazilianMarketIndicators brazilianMarketIndicators;

    private MarketQuotesAndCommodities marketQuotesAndCommodities;

    private BigDecimal netCapitalInvested;
    private BigDecimal currentValue;

    private static final int TOP_PERFORMERS_LIMIT = 5;

    private static final int ALLOCATION_PANEL_CONTAINER_SPACING = 10;
    private static final int ALLOCATION_PANEL_COLUMNS_SPACING = 20;
    private static final int ALLOCATION_PANEL_ITEMS_SPACING = 8;
    private static final int ALLOCATION_BAR_CONTAINER_SPACING = 3;
    private static final int ALLOCATION_INFO_BOX_SPACING = 10;
    private static final double ALLOCATION_PROGRESS_BAR_HEIGHT = 10.0;
    private static final double ALLOCATION_FILLED_BAR_HEIGHT = 20.0;
    private static final double PERCENTAGE_DIVISOR = 100.0;

    private Integer graphPaneCurrentPage = 0;

    private static final Logger logger = LoggerFactory.getLogger(SavingsController.class);

    /**
     * Constructor
     * @param tickerService The ticker service
     * @param marketService The market service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public SavingsController(
            TickerService tickerService,
            MarketService marketService,
            ConfigurableApplicationContext springContext,
            I18nService i18nService,
            WalletService walletService,
            InvestmentTargetService investmentTargetService,
            BondService bondService) {
        this.tickerService = tickerService;
        this.marketService = marketService;
        this.springContext = springContext;
        this.i18nService = i18nService;
        this.walletService = walletService;
        this.investmentTargetService = investmentTargetService;
        this.bondService = bondService;
    }

    @FXML
    private void initialize() {
        loadBrazilianMarketIndicatorsFromDatabase();
        loadMarketQuotesAndCommoditiesFromDatabase();

        configureTableView();
        configureBondTableView();
        populateTickerTypeComboBox();

        updateTransactionTableView();
        updateBondTableView();
        updatePortfolioIndicators();
        updateBondTabFields();
        updateBrazilianMarketIndicators();
        updateMarketQuotesAndCommodities();
        updateOverviewTabFields();
        updateTopPerformersPanel();
        updateAllocationVsTargetPanel();
        updateProfitabilityMetricsPanel();
        updateDisplayGraphs();

        if (isUpdatingPortfolioPrices) {
            setOffUpdatePortfolioPricesButton();
        } else {
            setOnUpdatePortfolioPricesButton();
        }

        setGraphButtonsActions();
        configureListeners();
        configureBondListeners();
    }

    @FXML
    private void handleRegisterTicker() {
        WindowUtils.openModalWindow(
                Constants.ADD_TICKER_FXML,
                i18nService.tr(
                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_ADD_TICKER_TITLE),
                springContext,
                (AddTickerController controller) -> {},
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
                            updateInvestmentDistributionChart();
                            updateOverviewTabFields();
                            updateTopPerformersPanel();
                            updateAllocationVsTargetPanel();
                            updateProfitabilityMetricsPanel();
                        }));
    }

    @FXML
    private void handleBuyTicker() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_BUY_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.BUY_TICKER_FXML,
                i18nService.tr(
                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_BUY_TICKER_TITLE),
                springContext,
                (AddTickerPurchaseController controller) -> controller.setTicker(selectedTicker),
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
                            updateInvestmentDistributionChart();
                            updateOverviewTabFields();
                            updateTopPerformersPanel();
                            updateAllocationVsTargetPanel();
                            updateProfitabilityMetricsPanel();
                        }));
    }

    @FXML
    private void handleSellTicker() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_SELL_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.SALE_TICKER_FXML,
                i18nService.tr(
                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_SELL_TICKER_TITLE),
                springContext,
                (AddTickerSaleController controller) -> controller.setTicker(selectedTicker),
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
                            updateInvestmentDistributionChart();
                            updateOverviewTabFields();
                            updateTopPerformersPanel();
                            updateAllocationVsTargetPanel();
                            updateProfitabilityMetricsPanel();
                        }));
    }

    @FXML
    private void handleAddDividend() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_ADD_DIVIDEND_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.ADD_DIVIDEND_FXML,
                i18nService.tr(
                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_ADD_DIVIDEND_TITLE),
                springContext,
                (AddDividendController controller) -> controller.setTicker(selectedTicker),
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
                            updateInvestmentDistributionChart();
                            updateOverviewTabFields();
                            updateTopPerformersPanel();
                            updateAllocationVsTargetPanel();
                            updateProfitabilityMetricsPanel();
                        }));
    }

    @FXML
    private void handleAddCryptoExchange() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        WindowUtils.openModalWindow(
                Constants.ADD_CRYPTO_EXCHANGE_FXML,
                i18nService.tr(
                        Constants.TranslationKeys
                                .SAVINGS_STOCKS_FUNDS_DIALOG_ADD_CRYPTO_EXCHANGE_TITLE),
                springContext,
                (AddCryptoExchangeController controller) -> {
                    if (selectedTicker != null) controller.setFromCryptoComboBox(selectedTicker);
                },
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
                            updateInvestmentDistributionChart();
                            updateOverviewTabFields();
                            updateTopPerformersPanel();
                            updateAllocationVsTargetPanel();
                            updateProfitabilityMetricsPanel();
                        }));
    }

    @FXML
    private void handleOpenTickerArchive() {
        WindowUtils.openModalWindow(
                Constants.ARCHIVED_TICKERS_FXML,
                i18nService.tr(
                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_TICKER_ARCHIVE_TITLE),
                springContext,
                (ArchivedTickersController controller) -> {},
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
                            updateInvestmentDistributionChart();
                            updateOverviewTabFields();
                            updateTopPerformersPanel();
                            updateAllocationVsTargetPanel();
                            updateProfitabilityMetricsPanel();
                        }));
    }

    @FXML
    private void handleShowTransactions() {
        WindowUtils.openModalWindow(
                Constants.INVESTMENT_TRANSACTIONS_FXML,
                i18nService.tr(
                        Constants.TranslationKeys
                                .SAVINGS_STOCKS_FUNDS_DIALOG_INVESTMENT_TRANSACTIONS_TITLE),
                springContext,
                (InvestmentTransactionsController controller) -> {},
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
                            updateInvestmentDistributionChart();
                            updateOverviewTabFields();
                            updateTopPerformersPanel();
                            updateAllocationVsTargetPanel();
                            updateProfitabilityMetricsPanel();
                        }));
    }

    @FXML
    private void handleUpdatePortfolioPrices() {
        Platform.runLater(this::setOffUpdatePortfolioPricesButton);

        tickerService
                .updateTickersPriceFromApiAsync(stocksFundsTabTickerTable.getItems())
                .thenAccept(
                        failed ->
                                Platform.runLater(
                                        () -> {
                                            if (failed.isEmpty()) {
                                                WindowUtils.showSuccessDialog(
                                                        i18nService.tr(
                                                                Constants.TranslationKeys
                                                                        .SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_SUCCESS_TITLE),
                                                        i18nService.tr(
                                                                Constants.TranslationKeys
                                                                        .SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_SUCCESS_MESSAGE));
                                            } else if (failed.size()
                                                    == stocksFundsTabTickerTable
                                                            .getItems()
                                                            .size()) {
                                                WindowUtils.showInformationDialog(
                                                        i18nService.tr(
                                                                Constants.TranslationKeys
                                                                        .SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_ERROR_TITLE),
                                                        i18nService.tr(
                                                                Constants.TranslationKeys
                                                                        .SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_ERROR_ALL_FAILED));
                                            } else {
                                                WindowUtils.showInformationDialog(
                                                        i18nService.tr(
                                                                Constants.TranslationKeys
                                                                        .SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_ERROR_TITLE),
                                                        i18nService.tr(
                                                                        Constants.TranslationKeys
                                                                                .SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_ERROR_SOME_FAILED)
                                                                + "\n"
                                                                + failed.stream()
                                                                        .map(Ticker::getSymbol)
                                                                        .reduce(
                                                                                (a, b) ->
                                                                                        a + ", "
                                                                                                + b)
                                                                        .orElse(""));
                                            }
                                        }))
                .exceptionally(
                        e -> {
                            Platform.runLater(
                                    () -> {
                                        WindowUtils.showErrorDialog(
                                                i18nService.tr(
                                                        Constants.TranslationKeys
                                                                .DIALOG_ERROR_TITLE),
                                                e.getMessage());
                                        setOnUpdatePortfolioPricesButton();
                                    });
                            return null;
                        })
                .whenComplete(
                        (v, e) ->
                                Platform.runLater(
                                        () -> {
                                            setOnUpdatePortfolioPricesButton();
                                            updateTransactionTableView();
                                            updatePortfolioIndicators();
                                            updateInvestmentDistributionChart();
                                            updateOverviewTabFields();
                                            updateTopPerformersPanel();
                                            updateAllocationVsTargetPanel();
                                            updateProfitabilityMetricsPanel();
                                        }));
    }

    @FXML
    private void handleEditTicker() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_EDIT_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_TICKER_FXML,
                i18nService.tr(
                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_EDIT_TICKER_TITLE),
                springContext,
                (EditTickerController controller) -> controller.setTicker(selectedTicker),
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
                            updateInvestmentDistributionChart();
                            updateOverviewTabFields();
                            updateTopPerformersPanel();
                            updateAllocationVsTargetPanel();
                            updateProfitabilityMetricsPanel();
                        }));
    }

    @FXML
    private void handleViewFundamentalAnalysis() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_MESSAGE));
            return;
        }

        if (!FundamentalAnalysisService.tickerIsValidForFundamentalAnalysis(selectedTicker)) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FUNDAMENTAL_ANALYSIS_ERROR_INVALID_TICKER_TYPE_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FUNDAMENTAL_ANALYSIS_ERROR_INVALID_TICKER_TYPE_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.FUNDAMENTAL_ANALYSIS_FXML,
                MessageFormat.format(
                        i18nService.tr(Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_DIALOG_TITLE),
                        selectedTicker.getSymbol()),
                springContext,
                (FundamentalAnalysisController controller) -> {
                    controller.setTicker(selectedTicker);
                    controller.loadAnalysis();
                },
                List.of());
    }

    @FXML
    private void handleDeleteTicker() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_DELETE_MESSAGE));
            return;
        }

        // Prevent the removal of a ticker with associated transactions
        if (tickerService.getTransactionCountByTicker(selectedTicker.getId()) > 0) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_HAS_TRANSACTIONS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_HAS_TRANSACTIONS_MESSAGE));
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_STOCKS_FUNDS_DIALOG_CONFIRMATION_DELETE_TITLE)
                        + " "
                        + selectedTicker.getName()
                        + " ("
                        + selectedTicker.getSymbol()
                        + ")",
                "",
                i18nService.getBundle())) {
            try {
                tickerService.deleteTicker(selectedTicker.getId());
                updateTransactionTableView();
                updatePortfolioIndicators();
                updateInvestmentDistributionChart();
                updateOverviewTabFields();
                updateTopPerformersPanel();
                updateAllocationVsTargetPanel();
                updateProfitabilityMetricsPanel();
            } catch (EntityNotFoundException | IllegalStateException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(Constants.TranslationKeys.DIALOG_ERROR_TITLE),
                        e.getMessage());
            }
        }
    }

    /**
     * Load the tickers from the database
     */
    private void loadTickersFromDatabase() {
        tickers = tickerService.getAllNonArchivedTickers();
    }

    /**
     * Load the dividends from the database
     */
    private void loadDividendsFromDatabase() {
        dividends = tickerService.getAllDividends();
    }

    /**
     * Load the Brazilian market indicators from the database
     */
    private void loadBrazilianMarketIndicatorsFromDatabase() {
        try {
            brazilianMarketIndicators = marketService.getBrazilianMarketIndicators();
            logger.info("Loaded Brazilian market indicators from the database");
        } catch (EntityNotFoundException e) {
            // If the indicators are not found in the database, update them from the
            // API
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

    /**
     * Load the market quotes and commodities from the database
     */
    private void loadMarketQuotesAndCommoditiesFromDatabase() {
        try {
            marketQuotesAndCommodities = marketService.getMarketQuotesAndCommodities();
            logger.info("Loaded market quotes and commodities from the database");
        } catch (EntityNotFoundException e) {
            // If the market quotes and commodities are not found in the database,
            // update them from the API
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

    /**
     * Update the net capital invested field
     */
    private void updateNetCapitalInvestedField() {
        // Calculate the net capital invested
        // Net capital invested = sum of (average price * current quantity) for all
        // tickers
        netCapitalInvested =
                tickers.stream()
                        .map(t -> t.getAverageUnitValue().multiply(t.getCurrentQuantity()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        stocksFundsTabNetCapitalInvestedField.setText(UIUtils.formatCurrency(netCapitalInvested));
    }

    /**
     * Update the current value field
     */
    private void updateCurrentValueField() {
        currentValue =
                tickers.stream()
                        .map(t -> t.getCurrentQuantity().multiply(t.getCurrentUnitValue()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        stocksFundsTabCurrentValueField.setText(UIUtils.formatCurrency(currentValue));
    }

    /**
     * Update the profit/loss field
     */
    private void updateProfitLossField() {
        BigDecimal profitLoss = currentValue.subtract(netCapitalInvested);

        stocksFundsTabProfitLossField.setText(UIUtils.formatCurrency(profitLoss));
    }

    /**
     * Update the dividends received field
     */
    private void updateDividendsReceivedField() {
        BigDecimal dividendsReceived =
                dividends.stream()
                        .map(d -> d.getWalletTransaction().getAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        stocksFundsTabDividendsReceivedField.setText(UIUtils.formatCurrency(dividendsReceived));
    }

    /**
     * Update the portfolio indicators
     */
    private void updatePortfolioIndicators() {
        loadTickersFromDatabase();
        loadDividendsFromDatabase();

        updateNetCapitalInvestedField();
        updateCurrentValueField();
        updateProfitLossField();
        updateDividendsReceivedField();
    }

    /**
     * Update the transaction table view
     */
    private void updateTransactionTableView() {
        // Get the search text
        String similarTextOrId = stocksFundsTabTickerSearchField.getText().toLowerCase();

        // Get selected values from the combo boxes
        TickerType selectedTickerType = stocksFundsTabTickerTypeComboBox.getValue();

        // Clear the table view
        stocksFundsTabTickerTable.getItems().clear();

        // Fetch all tickers within the selected range and filter by ticker
        // type. If ticker type is null, all tickers are fetched
        if (similarTextOrId.isEmpty()) {
            tickerService.getAllNonArchivedTickers().stream()
                    .filter(
                            t ->
                                    selectedTickerType == null
                                            || t.getType().equals(selectedTickerType))
                    .forEach(stocksFundsTabTickerTable.getItems()::add);
        } else {
            tickerService.getAllNonArchivedTickers().stream()
                    .filter(
                            t ->
                                    selectedTickerType == null
                                            || t.getType().equals(selectedTickerType))
                    .filter(
                            t -> {
                                String name = t.getName().toLowerCase();
                                String symbol = t.getSymbol().toLowerCase();
                                String type = t.getType().toString().toLowerCase();
                                String quantity = t.getCurrentQuantity().toString();
                                String unitValue = t.getCurrentUnitValue().toString();
                                String totalValue =
                                        t.getCurrentQuantity()
                                                .multiply(t.getCurrentUnitValue())
                                                .toString();
                                String avgPrice = t.getAverageUnitValue().toString();

                                return name.contains(similarTextOrId)
                                        || symbol.contains(similarTextOrId)
                                        || type.contains(similarTextOrId)
                                        || quantity.contains(similarTextOrId)
                                        || unitValue.contains(similarTextOrId)
                                        || totalValue.contains(similarTextOrId)
                                        || avgPrice.contains(similarTextOrId);
                            })
                    .forEach(stocksFundsTabTickerTable.getItems()::add);
        }

        stocksFundsTabTickerTable.refresh();
    }

    /**
     * Populate the ticker type combo box
     */
    private void populateTickerTypeComboBox() {
        // Make a copy of the list to add the 'All' option
        // Add 'All' option to the ticker type combo box
        // All is the first element in the list and is represented by a null value
        ObservableList<TickerType> tickerTypesWithNull =
                FXCollections.observableArrayList(TickerType.values());

        tickerTypesWithNull.addFirst(null);

        stocksFundsTabTickerTypeComboBox.setItems(tickerTypesWithNull);

        stocksFundsTabTickerTypeComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(TickerType tickerType) {
                        return tickerType != null
                                ? UIUtils.translateTickerType(tickerType, i18nService)
                                : i18nService.tr(
                                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_FILTER_ALL);
                    }

                    @Override
                    public TickerType fromString(String string) {
                        return string.equals(
                                        i18nService.tr(
                                                Constants.TranslationKeys
                                                        .SAVINGS_STOCKS_FUNDS_FILTER_ALL))
                                ? null
                                : TickerType.valueOf(string);
                    }
                });
    }

    /**
     * Configure the listeners
     */
    private void configureListeners() {
        // Add a listener to the search field to update the table view
        stocksFundsTabTickerSearchField
                .textProperty()
                .addListener((observable, oldValue, newValue) -> updateTransactionTableView());

        // Add a listener to the ticker type combo box to update the table view
        stocksFundsTabTickerTypeComboBox
                .valueProperty()
                .addListener((observable, oldValue, newValue) -> updateTransactionTableView());
    }

    /**
     * Configure the table view columns
     */
    private void configureTableView() {
        TableColumn<Ticker, Integer> idColumn = getTickerLongTableColumn();

        TableColumn<Ticker, ImageView> logoColumn = new TableColumn<>("");
        logoColumn.setCellValueFactory(
                param -> {
                    ImageView logo = UIUtils.loadTickerLogo(param.getValue(), 40.0);
                    return new SimpleObjectProperty<>(logo);
                });
        logoColumn.setCellFactory(createCenteredCellFactory(Pos.CENTER));
        logoColumn.setPrefWidth(45);
        logoColumn.setMaxWidth(45);
        logoColumn.setMinWidth(45);
        logoColumn.setResizable(false);

        TableColumn<Ticker, String> nameColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_TABLE_HEADER_NAME));
        nameColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getName()));
        nameColumn.setCellFactory(createCenteredCellFactory(Pos.CENTER_LEFT));

        TableColumn<Ticker, String> symbolColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_STOCKS_FUNDS_TABLE_HEADER_SYMBOL));
        symbolColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getSymbol()));
        symbolColumn.setCellFactory(createCenteredCellFactory(Pos.CENTER_LEFT));

        TableColumn<Ticker, String> typeColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_TABLE_HEADER_TYPE));
        typeColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateTickerType(
                                        param.getValue().getType(), i18nService)));
        typeColumn.setCellFactory(createCenteredCellFactory(Pos.CENTER_LEFT));

        TableColumn<Ticker, BigDecimal> quantityColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_STOCKS_FUNDS_TABLE_HEADER_QUANTITY_OWNED));
        quantityColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getCurrentQuantity()));
        quantityColumn.setCellFactory(createCenteredCellFactory(Pos.CENTER_LEFT));

        TableColumn<Ticker, String> unitColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_STOCKS_FUNDS_TABLE_HEADER_UNIT_PRICE));
        unitColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrencyDynamic(
                                        param.getValue().getCurrentUnitValue())));
        unitColumn.setCellFactory(createCenteredCellFactory(Pos.CENTER_LEFT));

        TableColumn<Ticker, String> totalColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_STOCKS_FUNDS_TABLE_HEADER_TOTAL_VALUE));
        totalColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrencyDynamic(
                                        param.getValue()
                                                .getCurrentQuantity()
                                                .multiply(
                                                        param.getValue().getCurrentUnitValue()))));
        totalColumn.setCellFactory(createCenteredCellFactory(Pos.CENTER_LEFT));

        TableColumn<Ticker, String> avgUnitColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_STOCKS_FUNDS_TABLE_HEADER_AVERAGE_UNIT_PRICE));
        avgUnitColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrencyDynamic(
                                        param.getValue().getAverageUnitValue())));
        avgUnitColumn.setCellFactory(createCenteredCellFactory(Pos.CENTER_LEFT));

        // Add the columns to the table view
        stocksFundsTabTickerTable.getColumns().add(idColumn);
        stocksFundsTabTickerTable.getColumns().add(logoColumn);
        stocksFundsTabTickerTable.getColumns().add(nameColumn);
        stocksFundsTabTickerTable.getColumns().add(symbolColumn);
        stocksFundsTabTickerTable.getColumns().add(typeColumn);
        stocksFundsTabTickerTable.getColumns().add(quantityColumn);
        stocksFundsTabTickerTable.getColumns().add(unitColumn);
        stocksFundsTabTickerTable.getColumns().add(totalColumn);
        stocksFundsTabTickerTable.getColumns().add(avgUnitColumn);

        // Apply specific styling for this table with icons
        stocksFundsTabTickerTable.setStyle(
                stocksFundsTabTickerTable.getStyle() + "-fx-fixed-cell-size: 45px;");
    }

    /**
     * Create a centered cell factory for text content
     */
    private <T>
            javafx.util.Callback<TableColumn<Ticker, T>, TableCell<Ticker, T>>
                    createCenteredCellFactory(Pos alignment) {
        return column ->
                new TableCell<>() {
                    @Override
                    protected void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            if (item instanceof ImageView) {
                                setGraphic((ImageView) item);
                                setStyle("-fx-padding: 0; -fx-alignment: CENTER;");
                            } else {
                                setText(item.toString());
                                String cssAlignment =
                                        alignment == Pos.CENTER ? "CENTER" : "CENTER-LEFT";
                                setStyle(
                                        "-fx-padding: 0 10px; -fx-alignment: "
                                                + cssAlignment
                                                + ";");
                            }
                        }
                    }
                };
    }

    private TableColumn<Ticker, Integer> getTickerLongTableColumn() {
        TableColumn<Ticker, Integer> idColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_TABLE_HEADER_ID));
        idColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getId()));
        idColumn.setCellFactory(createCenteredCellFactory(Pos.CENTER));
        return idColumn;
    }

    private void setOffUpdatePortfolioPricesButton() {
        updatePricesButtonIcon.setImage(
                new Image(
                        Objects.requireNonNull(getClass().getResource(Constants.LOADING_GIF))
                                .toExternalForm()));
        updatePortfolioPricesButton.setDisable(true);
        updatePortfolioPricesButton.setText(
                i18nService.tr(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_BUTTON_UPDATING));

        isUpdatingPortfolioPrices = true;
    }

    private void setOnUpdatePortfolioPricesButton() {
        updatePortfolioPricesButton.setDisable(false);
        updatePricesButtonIcon.setImage(
                new Image(
                        Objects.requireNonNull(
                                        getClass()
                                                .getResource(
                                                        Constants
                                                                .SAVINGS_SCREEN_SYNC_PRICES_BUTTON_DEFAULT_ICON))
                                .toExternalForm()));
        updatePortfolioPricesButton.setText(
                i18nService.tr(
                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_BUTTON_UPDATE_PRICES));

        isUpdatingPortfolioPrices = false;
    }

    /**
     * Update the Brazilian market indicators
     */
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

    /**
     * Schedules a retry for updating market quotes and commodities after a delay
     */
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

    /**
     * Schedules a retry for updating Brazilian market indicators after a delay
     */
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

    /**
     * Update the investment distribution pie chart
     */
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
                            .multiply(new BigDecimal("100"));

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

    /**
     * Calculate the investment distribution by type
     * Includes both ticker investments and savings account wallets
     */
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
            String typeName = UIUtils.translateBondType(bond.getType(), i18nService);

            investmentByType.merge(typeName, bondInvestedValue, BigDecimal::add);
        }

        return investmentByType;
    }

    /**
     * Set the actions for the graph navigation buttons
     */
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

    /**
     * Update the display of graphs based on the current page
     */
    private void updateDisplayGraphs() {
        pieChartAnchorPane.getChildren().clear();

        if (graphPaneCurrentPage == 0) {
            overviewTabBottonPaneTitle.setText(
                    i18nService.tr(Constants.TranslationKeys.SAVINGS_OVERVIEW_PORTFOLIO));
            updateInvestmentDistributionChart();
        } else if (graphPaneCurrentPage == 1) {
            overviewTabBottonPaneTitle.setText(
                    i18nService.tr(Constants.TranslationKeys.SAVINGS_INVESTMENT_PERFORMANCE));
            updateInvestmentPerformanceChart();
        }

        graphPrevButton.setDisable(graphPaneCurrentPage == 0);
        graphNextButton.setDisable(graphPaneCurrentPage >= 1);
    }

    /**
     * Update the investment performance chart with stacked bars showing invested value and capital gains by month
     */
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
        capitalGainsSeries.setName(i18nService.tr(Constants.TranslationKeys.SAVINGS_CAPITAL_GAINS));

        Map<YearMonth, BigDecimal> monthlyInvested = calculateMonthlyInvestedValue();
        Map<YearMonth, BigDecimal> monthlyGains = calculateMonthlyCapitalGains();

        YearMonth currentMonth = YearMonth.now();
        List<YearMonth> allMonths = new ArrayList<>();
        for (int i = Constants.XYBAR_CHART_MONTHS; i >= 0; i--) {
            allMonths.add(currentMonth.minusMonths(i));
        }

        DateTimeFormatter formatter = UIUtils.getShortMonthYearFormatter(i18nService.getLocale());

        Map<YearMonth, BigDecimal> monthlyTotals = new HashMap<>();

        BigDecimal lastInvested = BigDecimal.ZERO;
        BigDecimal lastGains = BigDecimal.ZERO;

        for (YearMonth month : allMonths) {
            String monthLabel = month.format(formatter);

            // Get accumulated value for this month, or use last known value
            BigDecimal invested = monthlyInvested.getOrDefault(month, lastInvested);
            BigDecimal gains = monthlyGains.getOrDefault(month, lastGains);

            if (monthlyInvested.containsKey(month)) {
                lastInvested = invested;
            }
            if (monthlyGains.containsKey(month)) {
                lastGains = gains;
            }

            BigDecimal total = invested.add(gains);
            monthlyTotals.put(month, total);

            investedSeries.getData().add(new XYChart.Data<>(monthLabel, invested.doubleValue()));
            capitalGainsSeries.getData().add(new XYChart.Data<>(monthLabel, gains.doubleValue()));
        }

        stackedBarChart.getData().addAll(investedSeries, capitalGainsSeries);

        // Calculate maximum total for Y-axis bounds
        Double maxTotal =
                monthlyTotals.values().stream()
                        .map(BigDecimal::doubleValue)
                        .max(Double::compare)
                        .orElse(0.0);

        Animation.setDynamicYAxisBounds(yAxis, maxTotal);

        // Format Y-axis labels as currency
        yAxis.setTickLabelFormatter(
                new StringConverter<>() {
                    @Override
                    public String toString(Number value) {
                        return UIUtils.formatCurrency(value);
                    }

                    @Override
                    public Number fromString(String string) {
                        return 0;
                    }
                });

        UIUtils.applyDefaultChartStyle(stackedBarChart);

        stackedBarChart.layout();

        BigDecimal previousTotal = BigDecimal.ZERO;

        for (int i = 0; i < investedSeries.getData().size(); i++) {
            XYChart.Data<String, Number> investedData = investedSeries.getData().get(i);
            XYChart.Data<String, Number> gainsData = capitalGainsSeries.getData().get(i);

            YearMonth yearMonth = allMonths.get(i);
            BigDecimal invested = BigDecimal.valueOf((Double) investedData.getYValue());
            BigDecimal gains = BigDecimal.valueOf((Double) gainsData.getYValue());
            BigDecimal total = monthlyTotals.getOrDefault(yearMonth, BigDecimal.ZERO);

            String tooltipText =
                    i18nService.tr(Constants.TranslationKeys.SAVINGS_OVERVIEW_PORTFOLIO)
                            + ": "
                            + UIUtils.formatCurrency(total)
                            + "\n"
                            + i18nService.tr(Constants.TranslationKeys.SAVINGS_INVESTED_VALUE)
                            + ": "
                            + UIUtils.formatCurrency(invested)
                            + "\n"
                            + i18nService.tr(Constants.TranslationKeys.SAVINGS_CAPITAL_GAINS)
                            + ": "
                            + UIUtils.formatCurrency(gains);

            if (i > 0 && previousTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal change = total.subtract(previousTotal);
                BigDecimal percentageChange =
                        change.divide(previousTotal, 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"));

                String sign = percentageChange.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
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

            previousTotal = total;
        }
    }

    /**
     * Calculate cumulative invested value over time (accumulated capital invested)
     */
    private Map<YearMonth, BigDecimal> calculateMonthlyInvestedValue() {
        Map<YearMonth, BigDecimal> monthlyInvested = new TreeMap<>();

        List<TickerPurchase> allPurchases = tickerService.getAllPurchases();
        for (TickerPurchase purchase : allPurchases) {
            YearMonth month = YearMonth.from(purchase.getWalletTransaction().getDate());
            BigDecimal purchaseValue = purchase.getUnitPrice().multiply(purchase.getQuantity());
            monthlyInvested.merge(month, purchaseValue, BigDecimal::add);
        }

        List<BondOperation> allOperations = bondService.getAllOperations();
        for (BondOperation operation : allOperations) {
            if (operation.getOperationType() == OperationType.BUY) {
                YearMonth month = YearMonth.from(operation.getWalletTransaction().getDate());
                BigDecimal operationValue =
                        operation.getUnitPrice().multiply(operation.getQuantity());
                monthlyInvested.merge(month, operationValue, BigDecimal::add);
            }
        }

        Map<YearMonth, BigDecimal> cumulativeInvested = new TreeMap<>();
        BigDecimal accumulated = BigDecimal.ZERO;
        for (Map.Entry<YearMonth, BigDecimal> entry : monthlyInvested.entrySet()) {
            accumulated = accumulated.add(entry.getValue());
            cumulativeInvested.put(entry.getKey(), accumulated);
        }

        return cumulativeInvested;
    }

    /**
     * Calculate cumulative capital gains over time (accumulated dividends and bond profits)
     */
    private Map<YearMonth, BigDecimal> calculateMonthlyCapitalGains() {
        Map<YearMonth, BigDecimal> monthlyGains = new TreeMap<>();

        List<Dividend> allDividends = tickerService.getAllDividends();
        for (Dividend dividend : allDividends) {
            YearMonth month = YearMonth.from(dividend.getWalletTransaction().getDate());
            BigDecimal dividendValue = dividend.getWalletTransaction().getAmount();
            monthlyGains.merge(month, dividendValue, BigDecimal::add);
        }

        List<BondOperation> allOperations = bondService.getAllOperations();
        for (BondOperation operation : allOperations) {
            if (operation.getOperationType() == OperationType.SELL
                    && operation.getNetProfit() != null
                    && operation.getNetProfit().compareTo(BigDecimal.ZERO) > 0) {
                YearMonth month = YearMonth.from(operation.getWalletTransaction().getDate());
                monthlyGains.merge(month, operation.getNetProfit(), BigDecimal::add);
            }
        }

        Map<YearMonth, BigDecimal> cumulativeGains = new TreeMap<>();
        BigDecimal accumulated = BigDecimal.ZERO;
        for (Map.Entry<YearMonth, BigDecimal> entry : monthlyGains.entrySet()) {
            accumulated = accumulated.add(entry.getValue());
            cumulativeGains.put(entry.getKey(), accumulated);
        }

        return cumulativeGains;
    }

    /**
     * Update the overview tab fields with investment totals
     */
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
        BigDecimal bondsRealizedProfitLoss =
                bondService.getAllNonArchivedBonds().stream()
                        .map(bondService::calculateProfit)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bondsCurrentValue = bondsTotalInvested;

        logger.info(
                "Overview bonds: investedValue={}, currentValueAssumed={}, realizedProfitLoss={}"
                        + " (SELL netProfit sum)",
                bondsTotalInvested,
                bondsCurrentValue,
                bondsRealizedProfitLoss);

        totalInvested = totalInvested.add(bondsTotalInvested);
        portfolioCurrentValue = portfolioCurrentValue.add(bondsCurrentValue);

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

        if (bondsRealizedProfitLoss.compareTo(BigDecimal.ZERO) > 0) {
            totalGains = totalGains.add(bondsRealizedProfitLoss);
        } else {
            totalLosses = totalLosses.add(bondsRealizedProfitLoss.abs());
        }

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

    /**
     * Calculate profitability metrics
     */
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
                                .divide(totalInvested, 4, java.math.RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"))
                        : BigDecimal.ZERO;

        BigDecimal totalDividends =
                dividends.stream()
                        .map(d -> d.getWalletTransaction().getAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dividendYield =
                totalInvested.compareTo(BigDecimal.ZERO) > 0
                        ? totalDividends
                                .divide(totalInvested, 4, java.math.RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"))
                        : BigDecimal.ZERO;

        return new ProfitabilityMetricsDTO(
                totalInvested,
                currentValue,
                profitLoss,
                returnPercentage,
                dividendYield,
                totalDividends);
    }

    /**
     * Calculate top performers (best and worst)
     */
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
                                                    .divide(
                                                            invested,
                                                            4,
                                                            java.math.RoundingMode.HALF_UP)
                                                    .multiply(new BigDecimal("100"))
                                            : BigDecimal.ZERO;

                            return new TickerPerformanceDTO(
                                    t.getName(), t.getSymbol(), percentage, profitLoss, current);
                        })
                .sorted(
                        best
                                ? java.util.Comparator.comparing(
                                                TickerPerformanceDTO::profitLossPercentage)
                                        .reversed()
                                : java.util.Comparator.comparing(
                                        TickerPerformanceDTO::profitLossPercentage))
                .limit(limit)
                .toList();
    }

    /**
     * Update the profitability metrics panel (P4)
     */
    private void updateProfitabilityMetricsPanel() {
        portfolioP4.getChildren().clear();

        ProfitabilityMetricsDTO metrics = calculateProfitabilityMetrics();

        VBox metricsContainer = new VBox(10);
        metricsContainer.setAlignment(Pos.CENTER);

        VBox metricsBox = new VBox(8);
        metricsBox.setAlignment(Pos.CENTER_LEFT);

        metricsBox
                .getChildren()
                .addAll(
                        createMetricRow(
                                i18nService.tr(
                                        Constants.TranslationKeys.SAVINGS_METRICS_TOTAL_RETURN),
                                metrics.returnPercentage(),
                                true,
                                true,
                                true),
                        createMetricRow(
                                i18nService.tr(
                                        Constants.TranslationKeys.SAVINGS_METRICS_DIVIDEND_YIELD),
                                metrics.dividendYield(),
                                true,
                                false,
                                true),
                        createMetricRow(
                                i18nService.tr(
                                        Constants.TranslationKeys.SAVINGS_METRICS_TOTAL_INVESTED),
                                metrics.totalInvested(),
                                true,
                                false,
                                false),
                        createMetricRow(
                                i18nService.tr(
                                        Constants.TranslationKeys.SAVINGS_METRICS_CURRENT_VALUE),
                                metrics.currentValue(),
                                true,
                                false,
                                false),
                        createMetricRow(
                                i18nService.tr(
                                        Constants.TranslationKeys.SAVINGS_METRICS_PROFIT_LOSS),
                                metrics.profitLoss(),
                                true,
                                true,
                                false),
                        createMetricRow(
                                i18nService.tr(
                                        Constants.TranslationKeys.SAVINGS_METRICS_TOTAL_DIVIDENDS),
                                metrics.totalDividends(),
                                true,
                                false,
                                false));

        metricsContainer.getChildren().addAll(metricsBox);

        portfolioP4.getChildren().add(metricsContainer);
        HBox.setHgrow(metricsContainer, javafx.scene.layout.Priority.ALWAYS);
    }

    /**
     * Create a metric row with label and value
     */
    private HBox createMetricRow(
            String label,
            BigDecimal value,
            boolean alwaysGreen,
            boolean dynamicColor,
            boolean isPercentage) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label labelNode = new Label(label);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        String sign = "";
        if (dynamicColor) {
            if (value.compareTo(BigDecimal.ZERO) > 0) {
                sign = "+ ";
            } else if (value.compareTo(BigDecimal.ZERO) < 0) {
                sign = "- ";
            }
        }

        String formattedValue =
                isPercentage
                        ? UIUtils.formatPercentage(value.abs(), i18nService)
                        : UIUtils.formatCurrency(value.abs());

        Label valueNode = new Label(sign + formattedValue);

        if (alwaysGreen && !dynamicColor) {
            valueNode.getStyleClass().add(Constants.INFO_LABEL_GREEN_STYLE);
        } else if (dynamicColor) {
            if (value.compareTo(BigDecimal.ZERO) < 0) {
                valueNode.getStyleClass().add(Constants.INFO_LABEL_RED_STYLE);
            } else if (value.compareTo(BigDecimal.ZERO) > 0) {
                valueNode.getStyleClass().add(Constants.INFO_LABEL_GREEN_STYLE);
            } else {
                valueNode.getStyleClass().add(Constants.INFO_LABEL_NEUTRAL_STYLE);
            }
        } else {
            valueNode.getStyleClass().add(Constants.INFO_LABEL_NEUTRAL_STYLE);
        }

        row.getChildren().addAll(labelNode, spacer, valueNode);

        return row;
    }

    /**
     * Update the top performers panel (P2)
     */
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

        // Add header row
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

        // Add header row
        worstBox.getChildren().add(createTableHeader());

        for (TickerPerformanceDTO performer : worstPerformers) {
            worstBox.getChildren().add(createPerformerRow(performer));
        }

        container.getChildren().addAll(bestBox, worstBox);

        portfolioP2.getChildren().add(container);
        HBox.setHgrow(container, javafx.scene.layout.Priority.ALWAYS);
    }

    /**
     * Create table header row
     */
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

        javafx.scene.layout.Region spacerA = new javafx.scene.layout.Region();
        HBox.setHgrow(spacerA, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.layout.Region spacerB = new javafx.scene.layout.Region();
        HBox.setHgrow(spacerB, javafx.scene.layout.Priority.ALWAYS);

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

    /**
     * Configure min and max width for a label column
     */
    private void configureColumnWidth(Label label, Double width) {
        label.setMinWidth(width);
        label.setMaxWidth(width);
    }

    /**
     * Create a performer row with ticker info
     */
    private HBox createPerformerRow(TickerPerformanceDTO performer) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label symbolLabel = new Label(performer.symbol());
        symbolLabel.getStyleClass().add(Constants.CUSTOM_TABLE_CELL_STYLE);
        configureColumnWidth(symbolLabel, Constants.TOP_PERFORMERS_ASSET_COLUMN_WIDTH);
        symbolLabel.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.layout.Region spacerA = new javafx.scene.layout.Region();
        HBox.setHgrow(spacerA, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.layout.Region spacerB = new javafx.scene.layout.Region();
        HBox.setHgrow(spacerB, javafx.scene.layout.Priority.ALWAYS);

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

        BigDecimal totalValue =
                tickers.stream()
                        .map(t -> t.getCurrentQuantity().multiply(t.getCurrentUnitValue()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBondValue = bondService.getTotalInvestedValue();
        totalValue = totalValue.add(totalBondValue);

        for (Ticker ticker : tickers) {
            BigDecimal value = ticker.getCurrentQuantity().multiply(ticker.getCurrentUnitValue());
            currentAllocation.merge(ticker.getType(), value, BigDecimal::add);
        }

        List<InvestmentTarget> targets = investmentTargetService.getAllActiveTargets();

        List<AllocationDTO> allocations = new ArrayList<>();

        for (InvestmentTarget target : targets) {
            AssetType assetType = target.getAssetType();
            BigDecimal currentValue = BigDecimal.ZERO;
            String typeName;

            if (assetType == AssetType.BOND) {
                currentValue = bondService.getTotalInvestedValue();
                typeName = i18nService.tr("Títulos de Renda Fixa");
            } else {
                TickerType tickerType = TickerType.valueOf(assetType.name());
                currentValue = currentAllocation.getOrDefault(tickerType, BigDecimal.ZERO);
                typeName = UIUtils.translateTickerType(tickerType, i18nService);
            }

            BigDecimal currentPercentage =
                    totalValue.compareTo(BigDecimal.ZERO) > 0
                            ? currentValue
                                    .divide(totalValue, 4, RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal("100"))
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
        col1.setHgrow(javafx.scene.layout.Priority.ALWAYS);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setMinWidth(20);
        col2.setHgrow(javafx.scene.layout.Priority.ALWAYS);

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
        HBox.setHgrow(container, javafx.scene.layout.Priority.ALWAYS);
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

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

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
    private void handleRegisterBond() {
        WindowUtils.openModalWindow(
                Constants.ADD_BOND_FXML,
                i18nService.tr(Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_ADD_BOND_TITLE),
                springContext,
                (AddBondController controller) -> {},
                List.of(
                        () -> {
                            updateBondTableView();
                            updateBondTabFields();
                            updateInvestmentDistributionChart();
                            updateOverviewTabFields();
                            updateAllocationVsTargetPanel();
                            updateProfitabilityMetricsPanel();
                        }));
    }

    @FXML
    private void handleEditBond() {
        Bond selectedBond = bondsTabBondTable.getSelectionModel().getSelectedItem();

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_BONDS_DIALOG_NO_SELECTION_EDIT_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_BOND_FXML,
                i18nService.tr(Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_EDIT_BOND_TITLE),
                springContext,
                (EditBondController controller) -> controller.setBond(selectedBond),
                List.of(
                        () -> {
                            updateBondTableView();
                            updateBondTabFields();
                            updateInvestmentDistributionChart();
                            updateOverviewTabFields();
                            updateAllocationVsTargetPanel();
                            updateProfitabilityMetricsPanel();
                        }));
    }

    @FXML
    private void handleDeleteBond() {
        Bond selectedBond = bondsTabBondTable.getSelectionModel().getSelectedItem();

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_BONDS_DIALOG_NO_SELECTION_DELETE_MESSAGE));
            return;
        }

        // Prevent the removal of a bond with associated operations
        if (bondService.getOperationCountByBond(selectedBond.getId()) > 0) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_HAS_OPERATIONS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_HAS_OPERATIONS_MESSAGE));
            return;
        }

        boolean confirmed =
                WindowUtils.showConfirmationDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_BONDS_DIALOG_CONFIRM_DELETE_TITLE),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .SAVINGS_BONDS_DIALOG_CONFIRM_DELETE_MESSAGE),
                                selectedBond.getName()));

        if (confirmed) {
            try {
                bondService.deleteBond(selectedBond.getId());

                WindowUtils.showSuccessDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_BOND_DELETED_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_BONDS_DIALOG_BOND_DELETED_MESSAGE));

                updateBondTableView();
                updateBondTabFields();
                updateInvestmentDistributionChart();
                updateOverviewTabFields();
                updateAllocationVsTargetPanel();
                updateProfitabilityMetricsPanel();
            } catch (IllegalStateException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_BONDS_DIALOG_ERROR_DELETING_BOND_TITLE),
                        e.getMessage());
            } catch (Exception e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(Constants.TranslationKeys.DIALOG_ERROR_TITLE),
                        e.getMessage());
            }
        }
    }

    @FXML
    private void handleOpenBondArchive() {
        WindowUtils.openModalWindow(
                Constants.ARCHIVED_BONDS_FXML,
                i18nService.tr(Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_BOND_ARCHIVE_TITLE),
                springContext,
                (ArchivedBondsController controller) -> {},
                List.of(
                        () -> {
                            updateBondTableView();
                            updateBondTabFields();
                            updateInvestmentDistributionChart();
                            updateOverviewTabFields();
                            updateAllocationVsTargetPanel();
                            updateProfitabilityMetricsPanel();
                        }));
    }

    @FXML
    private void handleBuyBond() {
        Bond selectedBond = bondsTabBondTable.getSelectionModel().getSelectedItem();

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_BONDS_DIALOG_NO_SELECTION_BUY_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.BUY_BOND_FXML,
                i18nService.tr(Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_BUY_BOND_TITLE),
                springContext,
                (AddBondPurchaseController controller) -> {
                    controller.setBond(selectedBond);
                },
                List.of(
                        () -> {
                            updateBondTableView();
                            updateBondTabFields();
                            updateInvestmentDistributionChart();
                            updateOverviewTabFields();
                            updateAllocationVsTargetPanel();
                            updateProfitabilityMetricsPanel();
                        }));
    }

    @FXML
    private void handleSellBond() {
        Bond selectedBond = bondsTabBondTable.getSelectionModel().getSelectedItem();

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_BONDS_DIALOG_NO_SELECTION_SELL_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.SALE_BOND_FXML,
                i18nService.tr(Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_SELL_BOND_TITLE),
                springContext,
                (AddBondSaleController controller) -> {
                    controller.setBond(selectedBond);
                },
                List.of(
                        () -> {
                            updateBondTableView();
                            updateBondTabFields();
                            updateInvestmentDistributionChart();
                            updateOverviewTabFields();
                            updateAllocationVsTargetPanel();
                            updateProfitabilityMetricsPanel();
                        }));
    }

    @FXML
    private void handleShowBondTransactions() {
        WindowUtils.openModalWindow(
                Constants.BOND_TRANSACTIONS_FXML,
                i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_TRANSACTIONS_TITLE),
                springContext,
                (BondTransactionsController controller) -> {},
                List.of(
                        () -> {
                            updateBondTableView();
                            updateBondTabFields();
                            updateInvestmentDistributionChart();
                            updateOverviewTabFields();
                            updateAllocationVsTargetPanel();
                            updateProfitabilityMetricsPanel();
                        }));
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

    private void configureBondTableView() {
        TableColumn<Bond, String> nameColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.SAVINGS_BONDS_TABLE_HEADER_NAME));
        nameColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getName()));

        TableColumn<Bond, String> symbolColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.SAVINGS_BONDS_TABLE_HEADER_SYMBOL));
        symbolColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getSymbol()));

        TableColumn<Bond, String> typeColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.SAVINGS_BONDS_TABLE_HEADER_TYPE));
        typeColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                UIUtils.translateBondType(
                                        cellData.getValue().getType(), i18nService)));

        TableColumn<Bond, String> quantityColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_QUANTITY));
        quantityColumn.setCellValueFactory(
                cellData -> {
                    Bond bond = cellData.getValue();
                    BigDecimal quantity = bondService.getCurrentQuantity(bond);
                    return new SimpleStringProperty(quantity.toString());
                });

        TableColumn<Bond, String> avgPriceColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_UNIT_PRICE));
        avgPriceColumn.setCellValueFactory(
                cellData -> {
                    Bond bond = cellData.getValue();
                    BigDecimal avgPrice = bondService.getAverageUnitPrice(bond);
                    return new SimpleStringProperty(UIUtils.formatCurrency(avgPrice));
                });

        TableColumn<Bond, String> currentValueColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_BONDS_TABLE_HEADER_CURRENT_VALUE));
        currentValueColumn.setCellValueFactory(
                cellData -> {
                    Bond bond = cellData.getValue();
                    BigDecimal currentValue = bondService.getInvestedValue(bond);
                    return new SimpleStringProperty(UIUtils.formatCurrency(currentValue));
                });

        TableColumn<Bond, String> investedValueColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_BONDS_TABLE_HEADER_INVESTED_VALUE));
        investedValueColumn.setCellValueFactory(
                cellData -> {
                    Bond bond = cellData.getValue();
                    BigDecimal invested = bondService.getInvestedValue(bond);
                    return new SimpleStringProperty(UIUtils.formatCurrency(invested));
                });

        TableColumn<Bond, String> profitLossColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.SAVINGS_BONDS_TABLE_HEADER_PROFIT_LOSS));
        profitLossColumn.setCellValueFactory(
                cellData -> {
                    Bond bond = cellData.getValue();
                    BigDecimal profitLoss = bondService.calculateProfit(bond);
                    return new SimpleStringProperty(UIUtils.formatCurrencySigned(profitLoss));
                });

        TableColumn<Bond, String> maturityDateColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_MATURITY_DATE));
        maturityDateColumn.setCellValueFactory(
                cellData -> {
                    Bond bond = cellData.getValue();
                    if (bond.getMaturityDate() != null) {
                        return new SimpleStringProperty(
                                UIUtils.formatDateForDisplay(bond.getMaturityDate(), i18nService));
                    }
                    return new SimpleStringProperty("-");
                });

        TableColumn<Bond, String> interestRateColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_INTEREST_RATE));
        interestRateColumn.setCellValueFactory(
                cellData -> {
                    Bond bond = cellData.getValue();
                    if (bond.getInterestRate() != null) {
                        return new SimpleStringProperty(bond.getInterestRate() + "%");
                    }
                    return new SimpleStringProperty("-");
                });

        bondsTabBondTable
                .getColumns()
                .addAll(
                        nameColumn,
                        symbolColumn,
                        typeColumn,
                        quantityColumn,
                        avgPriceColumn,
                        currentValueColumn,
                        profitLossColumn,
                        maturityDateColumn,
                        interestRateColumn);
    }

    private void updateBondTableView() {
        List<Bond> bonds = bondService.getAllNonArchivedBonds();

        // Filter by bond type if selected
        BondType selectedType = bondsTabBondTypeComboBox.getValue();
        if (selectedType != null) {
            bonds =
                    bonds.stream()
                            .filter(bond -> bond.getType().equals(selectedType))
                            .collect(Collectors.toList());
        }

        // Filter by search text considering all columns
        String searchText = bondsTabBondSearchField.getText().toLowerCase();
        if (!searchText.isEmpty()) {
            bonds =
                    bonds.stream()
                            .filter(
                                    bond -> {
                                        String name = bond.getName().toLowerCase();
                                        String symbol =
                                                bond.getSymbol() != null
                                                        ? bond.getSymbol().toLowerCase()
                                                        : "";
                                        String type =
                                                UIUtils.translateBondType(
                                                                bond.getType(), i18nService)
                                                        .toLowerCase();
                                        String issuer =
                                                bond.getIssuer() != null
                                                        ? bond.getIssuer().toLowerCase()
                                                        : "";
                                        String quantity =
                                                bondService.getCurrentQuantity(bond).toString();
                                        String avgPrice =
                                                bondService.getAverageUnitPrice(bond).toString();
                                        String investedValue =
                                                bondService.getInvestedValue(bond).toString();
                                        String profitLoss =
                                                bondService.calculateProfit(bond).toString();
                                        String maturityDate =
                                                bond.getMaturityDate() != null
                                                        ? UIUtils.formatDateForDisplay(
                                                                        bond.getMaturityDate(),
                                                                        i18nService)
                                                                .toLowerCase()
                                                        : "";
                                        String interestRate =
                                                bond.getInterestRate() != null
                                                        ? bond.getInterestRate().toString()
                                                        : "";

                                        return name.contains(searchText)
                                                || symbol.contains(searchText)
                                                || type.contains(searchText)
                                                || issuer.contains(searchText)
                                                || quantity.contains(searchText)
                                                || avgPrice.contains(searchText)
                                                || investedValue.contains(searchText)
                                                || profitLoss.contains(searchText)
                                                || maturityDate.contains(searchText)
                                                || interestRate.contains(searchText);
                                    })
                            .collect(Collectors.toList());
        }

        bondsTabBondTable.getItems().setAll(bonds);
    }

    private void updateBondTabFields() {
        List<Bond> bonds = bondService.getAllNonArchivedBonds();

        BigDecimal totalInvested = bondService.getTotalInvestedValue();

        BigDecimal profitLoss =
                bonds.stream()
                        .map(bondService::calculateProfit)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentValue = totalInvested;

        BigDecimal interestReceived = bondService.getTotalInterestReceived();

        bondsTabTotalInvestedField.setText(UIUtils.formatCurrency(totalInvested));
        bondsTabCurrentValueField.setText(UIUtils.formatCurrency(currentValue));
        bondsTabProfitLossField.setText(UIUtils.formatCurrencySigned(profitLoss));
        bondsTabInterestReceivedField.setText(UIUtils.formatCurrency(interestReceived));
    }

    private void configureBondListeners() {
        bondsTabBondTypeComboBox.setConverter(
                new StringConverter<BondType>() {
                    @Override
                    public String toString(BondType bondType) {
                        if (bondType == null) {
                            return i18nService.tr(
                                    Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_FILTER_ALL);
                        }
                        return UIUtils.translateBondType(bondType, i18nService);
                    }

                    @Override
                    public BondType fromString(String string) {
                        return null;
                    }
                });

        bondsTabBondTypeComboBox.getItems().clear();
        bondsTabBondTypeComboBox.getItems().add(null); // "All" option
        bondsTabBondTypeComboBox.getItems().addAll(BondType.values());
        bondsTabBondTypeComboBox.setValue(null); // Select "All" by default

        bondsTabBondTypeComboBox
                .valueProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            updateBondTableView();
                        });

        bondsTabBondSearchField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            updateBondTableView();
                        });
    }
}
