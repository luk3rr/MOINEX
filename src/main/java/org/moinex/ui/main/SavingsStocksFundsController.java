/*
 * Filename: SavingsStocksFundsController.java
 * Created on: February 18, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import com.jfoenix.controls.JFXButton;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.StringConverter;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;
import lombok.NoArgsConstructor;
import org.moinex.model.enums.AssetType;
import org.moinex.model.investment.Dividend;
import org.moinex.model.investment.Ticker;
import org.moinex.service.PreferencesService;
import org.moinex.service.investment.FundamentalAnalysisService;
import org.moinex.service.investment.TickerService;
import org.moinex.ui.dialog.investment.AddCryptoExchangeController;
import org.moinex.ui.dialog.investment.AddDividendController;
import org.moinex.ui.dialog.investment.AddTickerController;
import org.moinex.ui.dialog.investment.AddTickerPurchaseController;
import org.moinex.ui.dialog.investment.AddTickerSaleController;
import org.moinex.ui.dialog.investment.ArchivedTickersController;
import org.moinex.ui.dialog.investment.EditTickerController;
import org.moinex.ui.dialog.investment.FundamentalAnalysisController;
import org.moinex.ui.dialog.investment.InvestmentTransactionsController;
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
public class SavingsStocksFundsController {

    @FXML private Text stocksFundsTabNetCapitalInvestedField;
    @FXML private Text stocksFundsTabCurrentValueField;
    @FXML private Text stocksFundsTabProfitLossField;
    @FXML private Text stocksFundsTabDividendsReceivedField;
    @FXML private TableView<Ticker> stocksFundsTabTickerTable;
    @FXML private TextField stocksFundsTabTickerSearchField;
    @FXML private ComboBox<AssetType> stocksFundsTabTickerTypeComboBox;
    @FXML private JFXButton updatePortfolioPricesButton;
    @FXML private ImageView updatePricesButtonIcon;

    private ConfigurableApplicationContext springContext;
    private TickerService tickerService;
    private PreferencesService preferencesService;

    private List<Ticker> tickers;
    private List<Dividend> dividends;
    private boolean isUpdatingPortfolioPrices = false;

    private static final Logger logger =
            LoggerFactory.getLogger(SavingsStocksFundsController.class);

    @Autowired
    public SavingsStocksFundsController(
            TickerService tickerService,
            ConfigurableApplicationContext springContext,
            PreferencesService preferencesService) {
        this.tickerService = tickerService;
        this.springContext = springContext;
        this.preferencesService = preferencesService;
    }

    @FXML
    private void initialize() {
        configureTableView();
        populateTickerTypeComboBox();

        updateTransactionTableView();
        updatePortfolioIndicators();

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
                preferencesService.translate(
                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_ADD_TICKER_TITLE),
                springContext,
                (AddTickerController controller) -> {},
                List.of(this::updatePortfolioIndicators));
    }

    @FXML
    private void handleBuyTicker() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_BUY_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.BUY_TICKER_FXML,
                preferencesService.translate(
                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_BUY_TICKER_TITLE),
                springContext,
                (AddTickerPurchaseController controller) -> controller.setTicker(selectedTicker),
                List.of(this::updatePortfolioIndicators));
    }

    @FXML
    private void handleSellTicker() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_SELL_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.SALE_TICKER_FXML,
                preferencesService.translate(
                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_SELL_TICKER_TITLE),
                springContext,
                (AddTickerSaleController controller) -> controller.setTicker(selectedTicker),
                List.of(this::updatePortfolioIndicators));
    }

    @FXML
    private void handleAddDividend() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_ADD_DIVIDEND_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.ADD_DIVIDEND_FXML,
                preferencesService.translate(
                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_ADD_DIVIDEND_TITLE),
                springContext,
                (AddDividendController controller) -> controller.setTicker(selectedTicker),
                List.of(this::updatePortfolioIndicators));
    }

    @FXML
    private void handleAddCryptoExchange() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        WindowUtils.openModalWindow(
                Constants.ADD_CRYPTO_EXCHANGE_FXML,
                preferencesService.translate(
                        Constants.TranslationKeys
                                .SAVINGS_STOCKS_FUNDS_DIALOG_ADD_CRYPTO_EXCHANGE_TITLE),
                springContext,
                (AddCryptoExchangeController controller) -> {
                    if (selectedTicker != null) controller.setFromCryptoComboBox(selectedTicker);
                },
                List.of(this::updatePortfolioIndicators));
    }

    @FXML
    private void handleOpenTickerArchive() {
        WindowUtils.openModalWindow(
                Constants.ARCHIVED_TICKERS_FXML,
                preferencesService.translate(
                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_TICKER_ARCHIVE_TITLE),
                springContext,
                (ArchivedTickersController controller) -> {},
                List.of(this::updatePortfolioIndicators));
    }

    @FXML
    private void handleShowTransactions() {
        WindowUtils.openModalWindow(
                Constants.INVESTMENT_TRANSACTIONS_FXML,
                preferencesService.translate(
                        Constants.TranslationKeys
                                .SAVINGS_STOCKS_FUNDS_DIALOG_INVESTMENT_TRANSACTIONS_TITLE),
                springContext,
                (InvestmentTransactionsController controller) -> {},
                List.of(this::updatePortfolioIndicators));
    }

    @FXML
    private void handleUpdatePortfolioPrices() {
        Platform.runLater(this::setOffUpdatePortfolioPricesButton);

        try {
            List<Ticker> failed =
                    BuildersKt.runBlocking(
                            Dispatchers.getIO(),
                            (scope, continuation) ->
                                    tickerService.updateTickersPriceFromApi(
                                            stocksFundsTabTickerTable.getItems(),
                                            (Continuation<? super List<? extends Ticker>>)
                                                    continuation));

            Platform.runLater(
                    () -> {
                        if (failed.isEmpty()) {
                            WindowUtils.showSuccessDialog(
                                    preferencesService.translate(
                                            Constants.TranslationKeys
                                                    .SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_SUCCESS_TITLE),
                                    preferencesService.translate(
                                            Constants.TranslationKeys
                                                    .SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_SUCCESS_MESSAGE));
                        } else if (failed.size() == stocksFundsTabTickerTable.getItems().size()) {
                            WindowUtils.showInformationDialog(
                                    preferencesService.translate(
                                            Constants.TranslationKeys
                                                    .SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_ERROR_TITLE),
                                    preferencesService.translate(
                                            Constants.TranslationKeys
                                                    .SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_ERROR_ALL_FAILED));
                        } else {
                            WindowUtils.showInformationDialog(
                                    preferencesService.translate(
                                            Constants.TranslationKeys
                                                    .SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_ERROR_TITLE),
                                    preferencesService.translate(
                                                    Constants.TranslationKeys
                                                            .SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_ERROR_SOME_FAILED)
                                            + "\n"
                                            + failed.stream()
                                                    .map(Ticker::getSymbol)
                                                    .reduce((a, b) -> a + ", " + b)
                                                    .orElse(""));
                        }
                        setOnUpdatePortfolioPricesButton();
                        updatePortfolioIndicators();
                    });
        } catch (Exception e) {
            Platform.runLater(
                    () -> {
                        WindowUtils.showErrorDialog(
                                preferencesService.translate(
                                        Constants.TranslationKeys.DIALOG_ERROR_TITLE),
                                e.getMessage());
                        setOnUpdatePortfolioPricesButton();
                    });
        }
    }

    @FXML
    private void handleEditTicker() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_EDIT_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_TICKER_FXML,
                preferencesService.translate(
                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_EDIT_TICKER_TITLE),
                springContext,
                (EditTickerController controller) -> controller.setTicker(selectedTicker),
                List.of(this::updatePortfolioIndicators));
    }

    @FXML
    private void handleViewFundamentalAnalysis() {
        Ticker selectedTicker = stocksFundsTabTickerTable.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_MESSAGE));
            return;
        }

        if (!FundamentalAnalysisService.Companion.isValidForFundamentalAnalysis(selectedTicker)) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .FUNDAMENTAL_ANALYSIS_ERROR_INVALID_TICKER_TYPE_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .FUNDAMENTAL_ANALYSIS_ERROR_INVALID_TICKER_TYPE_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.FUNDAMENTAL_ANALYSIS_FXML,
                MessageFormat.format(
                        preferencesService.translate(
                                Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_DIALOG_TITLE),
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
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_DELETE_MESSAGE));
            return;
        }

        if (tickerService.getTransactionCountByTicker(selectedTicker.getId()) > 0) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_HAS_TRANSACTIONS_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .SAVINGS_STOCKS_FUNDS_DIALOG_HAS_TRANSACTIONS_MESSAGE));
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                preferencesService.translate(
                                Constants.TranslationKeys
                                        .SAVINGS_STOCKS_FUNDS_DIALOG_CONFIRMATION_DELETE_TITLE)
                        + " "
                        + selectedTicker.getName()
                        + " ("
                        + selectedTicker.getSymbol()
                        + ")",
                "",
                preferencesService.getBundle())) {
            try {
                tickerService.deleteTicker(selectedTicker.getId());
                updatePortfolioIndicators();
            } catch (EntityNotFoundException | IllegalStateException e) {
                WindowUtils.showErrorDialog(
                        preferencesService.translate(Constants.TranslationKeys.DIALOG_ERROR_TITLE),
                        e.getMessage());
            }
        }
    }

    private void loadTickersFromDatabase() {
        tickers = tickerService.getAllNonArchivedTickers();
    }

    private void loadDividendsFromDatabase() {
        dividends = tickerService.getAllDividends();
    }

    private void updateNetCapitalInvestedField() {
        BigDecimal netCapitalInvested =
                tickers.stream()
                        .map(t -> t.getAverageUnitValue().multiply(t.getCurrentQuantity()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        stocksFundsTabNetCapitalInvestedField.setText(UIUtils.formatCurrency(netCapitalInvested));
    }

    private void updateCurrentValueField() {
        BigDecimal currentValue =
                tickers.stream()
                        .map(t -> t.getCurrentQuantity().multiply(t.getCurrentUnitValue()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        stocksFundsTabCurrentValueField.setText(UIUtils.formatCurrency(currentValue));
    }

    private void updateProfitLossField() {
        BigDecimal netCapitalInvested =
                tickers.stream()
                        .map(t -> t.getAverageUnitValue().multiply(t.getCurrentQuantity()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentValue =
                tickers.stream()
                        .map(t -> t.getCurrentQuantity().multiply(t.getCurrentUnitValue()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profitLoss = currentValue.subtract(netCapitalInvested);

        stocksFundsTabProfitLossField.setText(UIUtils.formatCurrency(profitLoss));
    }

    private void updateDividendsReceivedField() {
        BigDecimal dividendsReceived =
                dividends.stream()
                        .map(d -> d.getWalletTransaction().getAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        stocksFundsTabDividendsReceivedField.setText(UIUtils.formatCurrency(dividendsReceived));
    }

    private void updatePortfolioIndicators() {
        loadTickersFromDatabase();
        loadDividendsFromDatabase();

        updateNetCapitalInvestedField();
        updateCurrentValueField();
        updateProfitLossField();
        updateDividendsReceivedField();
        updateTransactionTableView();
    }

    private void updateTransactionTableView() {
        String similarTextOrId = stocksFundsTabTickerSearchField.getText().toLowerCase();
        AssetType selectedTickerType = stocksFundsTabTickerTypeComboBox.getValue();

        stocksFundsTabTickerTable.getItems().clear();

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

    private void populateTickerTypeComboBox() {
        ObservableList<AssetType> tickerTypesWithNull =
                FXCollections.observableArrayList(AssetType.values());

        tickerTypesWithNull.addFirst(null);

        stocksFundsTabTickerTypeComboBox.setItems(tickerTypesWithNull);

        stocksFundsTabTickerTypeComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(AssetType tickerType) {
                        return tickerType != null
                                ? UIUtils.translateAssetType(tickerType, preferencesService)
                                : preferencesService.translate(
                                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_FILTER_ALL);
                    }

                    @Override
                    public AssetType fromString(String string) {
                        return string.equals(
                                        preferencesService.translate(
                                                Constants.TranslationKeys
                                                        .SAVINGS_STOCKS_FUNDS_FILTER_ALL))
                                ? null
                                : AssetType.valueOf(string);
                    }
                });
    }

    private void configureListeners() {
        stocksFundsTabTickerSearchField
                .textProperty()
                .addListener((observable, oldValue, newValue) -> updateTransactionTableView());

        stocksFundsTabTickerTypeComboBox
                .valueProperty()
                .addListener((observable, oldValue, newValue) -> updateTransactionTableView());
    }

    private void configureTableView() {
        TableColumn<Ticker, Integer> idColumn = getTickerLongTableColumn();
        TableColumn<Ticker, ImageView> logoColumn = getTickerImageViewTableColumn();

        TableColumn<Ticker, String> nameColumn =
                new TableColumn<>(
                        preferencesService.translate(
                                Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_TABLE_HEADER_NAME));
        nameColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getName()));
        nameColumn.setCellFactory(createCenteredCellFactory(Pos.CENTER_LEFT));

        TableColumn<Ticker, String> symbolColumn =
                new TableColumn<>(
                        preferencesService.translate(
                                Constants.TranslationKeys
                                        .SAVINGS_STOCKS_FUNDS_TABLE_HEADER_SYMBOL));
        symbolColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getSymbol()));
        symbolColumn.setCellFactory(createCenteredCellFactory(Pos.CENTER_LEFT));

        TableColumn<Ticker, String> typeColumn =
                new TableColumn<>(
                        preferencesService.translate(
                                Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_TABLE_HEADER_TYPE));
        typeColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateAssetType(
                                        param.getValue().getType(), preferencesService)));
        typeColumn.setCellFactory(createCenteredCellFactory(Pos.CENTER_LEFT));

        TableColumn<Ticker, BigDecimal> quantityColumn =
                new TableColumn<>(
                        preferencesService.translate(
                                Constants.TranslationKeys
                                        .SAVINGS_STOCKS_FUNDS_TABLE_HEADER_QUANTITY_OWNED));
        quantityColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getCurrentQuantity()));
        quantityColumn.setCellFactory(createCenteredCellFactory(Pos.CENTER_LEFT));

        TableColumn<Ticker, String> unitColumn =
                new TableColumn<>(
                        preferencesService.translate(
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
                        preferencesService.translate(
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
                        preferencesService.translate(
                                Constants.TranslationKeys
                                        .SAVINGS_STOCKS_FUNDS_TABLE_HEADER_AVERAGE_UNIT_PRICE));
        avgUnitColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrencyDynamic(
                                        param.getValue().getAverageUnitValue())));
        avgUnitColumn.setCellFactory(createCenteredCellFactory(Pos.CENTER_LEFT));

        stocksFundsTabTickerTable.getColumns().add(idColumn);
        stocksFundsTabTickerTable.getColumns().add(logoColumn);
        stocksFundsTabTickerTable.getColumns().add(nameColumn);
        stocksFundsTabTickerTable.getColumns().add(symbolColumn);
        stocksFundsTabTickerTable.getColumns().add(typeColumn);
        stocksFundsTabTickerTable.getColumns().add(quantityColumn);
        stocksFundsTabTickerTable.getColumns().add(unitColumn);
        stocksFundsTabTickerTable.getColumns().add(totalColumn);
        stocksFundsTabTickerTable.getColumns().add(avgUnitColumn);

        stocksFundsTabTickerTable.setStyle(
                stocksFundsTabTickerTable.getStyle() + "-fx-fixed-cell-size: 45px;");
    }

    private TableColumn<Ticker, ImageView> getTickerImageViewTableColumn() {
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
        return logoColumn;
    }

    private TableColumn<Ticker, Integer> getTickerLongTableColumn() {
        TableColumn<Ticker, Integer> idColumn =
                new TableColumn<>(
                        preferencesService.translate(
                                Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_TABLE_HEADER_ID));
        idColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getId()));
        idColumn.setCellFactory(createCenteredCellFactory(Pos.CENTER));
        return idColumn;
    }

    private <T> Callback<TableColumn<Ticker, T>, TableCell<Ticker, T>> createCenteredCellFactory(
            Pos alignment) {
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

    private void setOffUpdatePortfolioPricesButton() {
        updatePricesButtonIcon.setImage(
                new Image(
                        Objects.requireNonNull(getClass().getResource(Constants.LOADING_GIF))
                                .toExternalForm()));
        updatePortfolioPricesButton.setDisable(true);
        updatePortfolioPricesButton.setText(
                preferencesService.translate(
                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_BUTTON_UPDATING));

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
                preferencesService.translate(
                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_BUTTON_UPDATE_PRICES));

        isUpdatingPortfolioPrices = false;
    }
}
