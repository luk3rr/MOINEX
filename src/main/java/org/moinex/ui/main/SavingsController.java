/*
 * Filename: SavingsController.java
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import com.jfoenix.controls.JFXButton;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import lombok.NoArgsConstructor;
import org.moinex.model.investment.BrazilianMarketIndicators;
import org.moinex.model.investment.Dividend;
import org.moinex.model.investment.MarketQuotesAndCommodities;
import org.moinex.model.investment.Ticker;
import org.moinex.service.MarketService;
import org.moinex.service.TickerService;
import org.moinex.ui.dialog.investment.AddCryptoExchangeController;
import org.moinex.ui.dialog.investment.AddDividendController;
import org.moinex.ui.dialog.investment.AddTickerController;
import org.moinex.ui.dialog.investment.AddTickerPurchaseController;
import org.moinex.ui.dialog.investment.AddTickerSaleController;
import org.moinex.ui.dialog.investment.ArchivedTickersController;
import org.moinex.ui.dialog.investment.EditTickerController;
import org.moinex.ui.dialog.investment.InvestmentTransactionsController;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TickerType;
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

    @FXML private Label overviewTabBrazilianMarketIndicatorsLastUpdate;

    @FXML private Label overviewTabMarketQuotesLastUpdate;

    @FXML private Label overviewTabCommoditiesLastUpdate;

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

    private List<Ticker> tickers;

    private List<Dividend> dividends;

    private BrazilianMarketIndicators brazilianMarketIndicators;

    private MarketQuotesAndCommodities marketQuotesAndCommodities;

    private BigDecimal netCapitalInvested;
    private BigDecimal currentValue;

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
            ConfigurableApplicationContext springContext) {
        this.tickerService = tickerService;
        this.marketService = marketService;
        this.springContext = springContext;
    }

    @FXML
    private void initialize() {
        loadBrazilianMarketIndicatorsFromDatabase();
        loadMarketQuotesAndCommoditiesFromDatabase();

        configureTableView();
        populateTickerTypeComboBox();

        updateTransactionTableView();
        updatePortfolioIndicators();
        updateBrazilianMarketIndicators();
        updateMarketQuotesAndCommodities();

        if (isUpdatingPortfolioPrices) {
            setOffUpdatePortfolioPricesButton();
        } else {
            setOnUpdatePortfolioPricesButton();
        }

        configureListeners();
    }

    @FXML
    private void handleRegisterTicker() {
        WindowUtils.openModalWindow(
                Constants.ADD_TICKER_FXML,
                "Add Ticker",
                springContext,
                (AddTickerController controller) -> {},
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
                        }));
    }

    @FXML
    private void handleBuyTicker() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    "No ticker selected", "Please select a ticker to buy");
            return;
        }

        WindowUtils.openModalWindow(
                Constants.BUY_TICKER_FXML,
                "Buy Ticker",
                springContext,
                (AddTickerPurchaseController controller) -> controller.setTicker(selectedTicker),
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
                        }));
    }

    @FXML
    private void handleSellTicker() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    "No ticker selected", "Please select a ticker to sell");
            return;
        }

        WindowUtils.openModalWindow(
                Constants.SALE_TICKER_FXML,
                "Sell Ticker",
                springContext,
                (AddTickerSaleController controller) -> controller.setTicker(selectedTicker),
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
                        }));
    }

    @FXML
    private void handleAddDividend() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    "No ticker selected", "Please select a ticker to add a dividend");
            return;
        }

        WindowUtils.openModalWindow(
                Constants.ADD_DIVIDEND_FXML,
                "Add Dividend",
                springContext,
                (AddDividendController controller) -> controller.setTicker(selectedTicker),
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
                        }));
    }

    @FXML
    private void handleAddCryptoExchange() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        WindowUtils.openModalWindow(
                Constants.ADD_CRYPTO_EXCHANGE_FXML,
                "Add Crypto Exchange",
                springContext,
                (AddCryptoExchangeController controller) -> {
                    if (selectedTicker != null) controller.setFromCryptoComboBox(selectedTicker);
                },
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
                        }));
    }

    @FXML
    private void handleOpenTickerArchive() {
        WindowUtils.openModalWindow(
                Constants.ARCHIVED_TICKERS_FXML,
                "Ticker Archive",
                springContext,
                (ArchivedTickersController controller) -> {},
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
                        }));
    }

    @FXML
    private void handleShowTransactions() {
        WindowUtils.openModalWindow(
                Constants.INVESTMENT_TRANSACTIONS_FXML,
                "Investment Transactions",
                springContext,
                (InvestmentTransactionsController controller) -> {},
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
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
                                                        "Finished updating prices",
                                                        "All tickers were successfully updated");
                                            } else if (failed.size()
                                                    == stocksFundsTabTickerTable
                                                            .getItems()
                                                            .size()) {
                                                WindowUtils.showInformationDialog(
                                                        "Finished updating prices with errors",
                                                        "Failed to update all tickers");
                                            } else {
                                                WindowUtils.showInformationDialog(
                                                        "Finished updating prices with errors",
                                                        "Failed to update tickers:\n"
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
                                                "Error updating prices", e.getMessage());
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
                                        }));
    }

    @FXML
    private void handleEditTicker() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    "No ticker selected", "Please select a ticker to edit");
            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_TICKER_FXML,
                "Edit Ticker",
                springContext,
                (EditTickerController controller) -> controller.setTicker(selectedTicker),
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updatePortfolioIndicators();
                        }));
    }

    @FXML
    private void handleDeleteTicker() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    "No ticker selected", "Please select a ticker to delete");
            return;
        }

        // Prevent the removal of a ticker with associated transactions
        if (tickerService.getTransactionCountByTicker(selectedTicker.getId()) > 0) {
            WindowUtils.showErrorDialog(
                    "Ticker has transactions",
                    "Cannot delete a ticker with associated transactions. Remove the "
                            + "transactions first or archive the ticker.");
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                "Delete ticker "
                        + selectedTicker.getName()
                        + " ("
                        + selectedTicker.getSymbol()
                        + ")",
                "Are you sure you want to delete this ticker?")) {
            try {
                tickerService.deleteTicker(selectedTicker.getId());
                updateTransactionTableView();
            } catch (EntityNotFoundException | IllegalStateException e) {
                WindowUtils.showErrorDialog("Error deleting ticker", e.getMessage());
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
                                ? tickerType.toString()
                                : "ALL"; // Show "All" instead of null
                    }

                    @Override
                    public TickerType fromString(String string) {
                        return string.equals("ALL")
                                ? null
                                : TickerType.valueOf(string); // Return null if "All" is selected
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
        TableColumn<Ticker, Long> idColumn = getTickerLongTableColumn();

        TableColumn<Ticker, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getName()));

        TableColumn<Ticker, String> symbolColumn = new TableColumn<>("Symbol");
        symbolColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getSymbol()));

        TableColumn<Ticker, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getType().toString()));

        TableColumn<Ticker, BigDecimal> quantityColumn = new TableColumn<>("Quantity Owned");
        quantityColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getCurrentQuantity()));

        TableColumn<Ticker, String> unitColumn = new TableColumn<>("Unit Price");
        unitColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrencyDynamic(
                                        param.getValue().getCurrentUnitValue())));

        TableColumn<Ticker, String> totalColumn = new TableColumn<>("Total Value");
        totalColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrencyDynamic(
                                        param.getValue()
                                                .getCurrentQuantity()
                                                .multiply(
                                                        param.getValue().getCurrentUnitValue()))));

        TableColumn<Ticker, String> avgUnitColumn = new TableColumn<>("Average Unit Price");
        avgUnitColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrencyDynamic(
                                        param.getValue().getAverageUnitValue())));

        // Add the columns to the table view
        stocksFundsTabTickerTable.getColumns().add(idColumn);
        stocksFundsTabTickerTable.getColumns().add(nameColumn);
        stocksFundsTabTickerTable.getColumns().add(symbolColumn);
        stocksFundsTabTickerTable.getColumns().add(typeColumn);
        stocksFundsTabTickerTable.getColumns().add(quantityColumn);
        stocksFundsTabTickerTable.getColumns().add(unitColumn);
        stocksFundsTabTickerTable.getColumns().add(totalColumn);
        stocksFundsTabTickerTable.getColumns().add(avgUnitColumn);
    }

    private static TableColumn<Ticker, Long> getTickerLongTableColumn() {
        TableColumn<Ticker, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getId()));

        // Align the ID column to the center
        idColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(Long item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item == null || empty) {
                                    setText(null);
                                } else {
                                    setText(item.toString());
                                    setAlignment(Pos.CENTER);
                                    setStyle("-fx-padding: 0;"); // set padding to zero to
                                    // ensure the text is centered
                                }
                            }
                        });
        return idColumn;
    }

    private void setOffUpdatePortfolioPricesButton() {
        updatePricesButtonIcon.setImage(
                new Image(
                        Objects.requireNonNull(getClass().getResource(Constants.LOADING_GIF))
                                .toExternalForm()));
        updatePortfolioPricesButton.setDisable(true);
        updatePortfolioPricesButton.setText("Updating...");

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
        updatePortfolioPricesButton.setText("Update Prices");

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
                UIUtils.formatPercentage(brazilianMarketIndicators.getSelicTarget()));

        overviewTabIPCALastMonthValueField.setText(
                UIUtils.formatPercentage(brazilianMarketIndicators.getIpcaLastMonth()));

        overviewTabIPCALastMonthDescriptionField.setText(
                "IPCA " + brazilianMarketIndicators.getIpcaLastMonthReference());

        overviewTabIPCA12MonthsValueField.setText(
                UIUtils.formatPercentage(brazilianMarketIndicators.getIpca12Months()));

        overviewTabBrazilianMarketIndicatorsLastUpdate.setText(
                brazilianMarketIndicators.getLastUpdate().format(Constants.DATE_FORMATTER_NO_TIME));
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

        overviewTabMarketQuotesLastUpdate.setText(
                marketQuotesAndCommodities
                        .getLastUpdate()
                        .format(Constants.DATE_FORMATTER_NO_TIME));

        overviewTabCommoditiesLastUpdate.setText(
                marketQuotesAndCommodities
                        .getLastUpdate()
                        .format(Constants.DATE_FORMATTER_NO_TIME));
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
}
