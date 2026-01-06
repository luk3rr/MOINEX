/*
 * Filename: InvestmentTransactionsController.java
 * Created on: January 10, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.enums.TransactionStatus;
import org.moinex.model.enums.TransactionType;
import org.moinex.model.investment.CryptoExchange;
import org.moinex.model.investment.Dividend;
import org.moinex.model.investment.TickerPurchase;
import org.moinex.model.investment.TickerSale;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.service.I18nService;
import org.moinex.service.TickerService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Investment Transactions dialog
 */
@Controller
@NoArgsConstructor
public class InvestmentTransactionsController {
    @FXML private TableView<TickerPurchase> purchaseTableView;

    @FXML private TableView<TickerSale> saleTableView;

    @FXML private TableView<Dividend> dividendTableView;

    @FXML private TableView<CryptoExchange> cryptoExchangeTableView;

    @FXML private TextField searchField;

    @FXML private TabPane tabPane;

    private ConfigurableApplicationContext springContext;

    private List<TickerPurchase> purchases;

    private List<TickerSale> sales;

    private List<Dividend> dividends;

    private List<CryptoExchange> cryptoExchanges;

    private TickerService tickerService;
    private I18nService i18nService;

    /**
     * Constructor
     * @param tickerService tickerService
     * @param i18nService I18n service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public InvestmentTransactionsController(
            TickerService tickerService,
            ConfigurableApplicationContext springContext,
            I18nService i18nService) {
        this.tickerService = tickerService;
        this.springContext = springContext;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        loadPurchasesFromDatabase();
        loadSalesFromDatabase();
        loadDividendsFromDatabase();
        loadCryptoExchangesFromDatabase();

        configurePurchaseTableView();
        configureSaleTableView();
        configureDividendTableView();
        configureCryptoExchangeTableView();

        updatePurchaseTableView();
        updateSaleTableView();
        updateDividendTableView();
        updateCryptoExchangeTableView();

        // Add listener to the search field
        searchField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            updatePurchaseTableView();
                            updateSaleTableView();
                            updateDividendTableView();
                            updateCryptoExchangeTableView();
                        });
    }

    @FXML
    private void handleEdit() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();

        if (selectedTab == null) {
            return;
        }

        if (selectedTab == tabPane.getTabs().get(0)) {
            TickerPurchase selectedPurchase =
                    purchaseTableView.getSelectionModel().getSelectedItem();

            editPurchase(selectedPurchase);
        } else if (selectedTab == tabPane.getTabs().get(1)) {
            TickerSale selectedSale = saleTableView.getSelectionModel().getSelectedItem();

            editSale(selectedSale);
        } else if (selectedTab == tabPane.getTabs().get(2)) {
            Dividend selectedDividend = dividendTableView.getSelectionModel().getSelectedItem();

            editDividend(selectedDividend);
        } else if (selectedTab == tabPane.getTabs().get(3)) {
            CryptoExchange selectedCryptoExchange =
                    cryptoExchangeTableView.getSelectionModel().getSelectedItem();

            editCryptoExchange(selectedCryptoExchange);
        }
    }

    @FXML
    private void handleDelete() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();

        if (selectedTab == null) {
            return;
        }

        if (selectedTab == tabPane.getTabs().get(0)) {
            TickerPurchase selectedPurchase =
                    purchaseTableView.getSelectionModel().getSelectedItem();

            deletePurchase(selectedPurchase);
        } else if (selectedTab == tabPane.getTabs().get(1)) {
            TickerSale selectedSale = saleTableView.getSelectionModel().getSelectedItem();

            deleteSale(selectedSale);
        } else if (selectedTab == tabPane.getTabs().get(2)) {
            Dividend selectedDividend = dividendTableView.getSelectionModel().getSelectedItem();

            deleteDividend(selectedDividend);
        } else if (selectedTab == tabPane.getTabs().get(3)) {
            CryptoExchange selectedCryptoExchange =
                    cryptoExchangeTableView.getSelectionModel().getSelectedItem();

            deleteCryptoExchange(selectedCryptoExchange);
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.close();
    }

    /**
     * Loads the purchases from the database
     */
    private void loadPurchasesFromDatabase() {
        purchases = tickerService.getAllPurchases();
    }

    /**
     * Loads the sales from the database
     */
    private void loadSalesFromDatabase() {
        sales = tickerService.getAllSales();
    }

    /**
     * Loads the dividends from the database
     */
    private void loadDividendsFromDatabase() {
        dividends = tickerService.getAllDividends();
    }

    /**
     * Loads the crypto exchanges from the database
     */
    private void loadCryptoExchangesFromDatabase() {
        cryptoExchanges = tickerService.getAllCryptoExchanges();
    }

    /**
     * Updates dividend table view
     */
    private void updateDividendTableView() {
        String similarTextOrId = searchField.getText().toLowerCase();

        dividendTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty()) {
            dividendTableView.getItems().setAll(dividends);
        } else {
            dividends.stream()
                    .filter(
                            d -> {
                                String id = d.getId().toString();
                                String tickerName = d.getTicker().getName().toLowerCase();
                                String tickerSymbol = d.getTicker().getSymbol().toLowerCase();
                                String amount = d.getWalletTransaction().getAmount().toString();
                                String walletName =
                                        d.getWalletTransaction()
                                                .getWallet()
                                                .getName()
                                                .toLowerCase();
                                String date =
                                        UIUtils.formatDateTimeForDisplay(
                                                d.getWalletTransaction().getDate(), i18nService);
                                String status =
                                        d.getWalletTransaction().getStatus().name().toLowerCase();

                                return id.contains(similarTextOrId)
                                        || tickerName.contains(similarTextOrId)
                                        || tickerSymbol.contains(similarTextOrId)
                                        || amount.contains(similarTextOrId)
                                        || walletName.contains(similarTextOrId)
                                        || date.contains(similarTextOrId)
                                        || status.contains(similarTextOrId);
                            })
                    .forEach(dividendTableView.getItems()::add);
        }

        dividendTableView.refresh();
    }

    /**
     * Updates purchase table view
     */
    private void updatePurchaseTableView() {
        String similarTextOrId = searchField.getText().toLowerCase();

        purchaseTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty()) {
            purchaseTableView.getItems().setAll(purchases);
        } else {
            purchases.stream()
                    .filter(
                            p -> {
                                String id = p.getId().toString();
                                String tickerName = p.getTicker().getName().toLowerCase();
                                String tickerSymbol = p.getTicker().getSymbol().toLowerCase();
                                String date =
                                        UIUtils.formatDateTimeForDisplay(
                                                p.getWalletTransaction().getDate(), i18nService);
                                String quantity = p.getQuantity().toString();
                                String unitPrice = p.getUnitPrice().toString();
                                String amount = p.getWalletTransaction().getAmount().toString();
                                String walletName =
                                        p.getWalletTransaction()
                                                .getWallet()
                                                .getName()
                                                .toLowerCase();
                                String status =
                                        p.getWalletTransaction().getStatus().name().toLowerCase();

                                return id.contains(similarTextOrId)
                                        || tickerName.contains(similarTextOrId)
                                        || tickerSymbol.contains(similarTextOrId)
                                        || date.contains(similarTextOrId)
                                        || quantity.contains(similarTextOrId)
                                        || unitPrice.contains(similarTextOrId)
                                        || amount.contains(similarTextOrId)
                                        || walletName.contains(similarTextOrId)
                                        || status.contains(similarTextOrId);
                            })
                    .forEach(purchaseTableView.getItems()::add);
        }

        purchaseTableView.refresh();
    }

    /**
     * Updates sale table view
     */
    private void updateSaleTableView() {
        String similarTextOrId = searchField.getText().toLowerCase();

        saleTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty()) {
            saleTableView.getItems().setAll(sales);
        } else {
            sales.stream()
                    .filter(
                            s -> {
                                String id = s.getId().toString();
                                String tickerName = s.getTicker().getName().toLowerCase();
                                String tickerSymbol = s.getTicker().getSymbol().toLowerCase();
                                String date =
                                        UIUtils.formatDateTimeForDisplay(
                                                s.getWalletTransaction().getDate(), i18nService);
                                String quantity = s.getQuantity().toString();
                                String unitPrice = s.getUnitPrice().toString();
                                String amount = s.getWalletTransaction().getAmount().toString();
                                String walletName =
                                        s.getWalletTransaction()
                                                .getWallet()
                                                .getName()
                                                .toLowerCase();
                                String status =
                                        s.getWalletTransaction().getStatus().name().toLowerCase();

                                return id.contains(similarTextOrId)
                                        || tickerName.contains(similarTextOrId)
                                        || tickerSymbol.contains(similarTextOrId)
                                        || date.contains(similarTextOrId)
                                        || quantity.contains(similarTextOrId)
                                        || unitPrice.contains(similarTextOrId)
                                        || amount.contains(similarTextOrId)
                                        || walletName.contains(similarTextOrId)
                                        || status.contains(similarTextOrId);
                            })
                    .forEach(saleTableView.getItems()::add);
        }

        saleTableView.refresh();
    }

    /**
     * Updates the crypto exchange table view
     */
    private void updateCryptoExchangeTableView() {
        String similarTextOrId = searchField.getText().toLowerCase();

        cryptoExchangeTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty()) {
            cryptoExchangeTableView.getItems().setAll(cryptoExchanges);
        } else {
            cryptoExchanges.stream()
                    .filter(
                            ce -> {
                                String id = ce.getId().toString();
                                String sourceCrypto = ce.getSoldCrypto().getName().toLowerCase();
                                String targetCrypto =
                                        ce.getReceivedCrypto().getName().toLowerCase();
                                String date =
                                        UIUtils.formatDateTimeForDisplay(ce.getDate(), i18nService);
                                String sourceQuantity = ce.getSoldQuantity().toString();
                                String targetQuantity = ce.getReceivedQuantity().toString();
                                String description = ce.getDescription().toLowerCase();

                                return id.contains(similarTextOrId)
                                        || sourceCrypto.contains(similarTextOrId)
                                        || targetCrypto.contains(similarTextOrId)
                                        || date.contains(similarTextOrId)
                                        || sourceQuantity.contains(similarTextOrId)
                                        || targetQuantity.contains(similarTextOrId)
                                        || description.contains(similarTextOrId);
                            })
                    .forEach(cryptoExchangeTableView.getItems()::add);
        }

        cryptoExchangeTableView.refresh();
    }

    /**
     * Configure the purchase table view columns
     */
    private void configurePurchaseTableView() {
        TableColumn<TickerPurchase, Integer> idColumn = getTickerPurchaseLongTableColumn();

        TableColumn<TickerPurchase, String> tickerNameColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_TICKER));
        tickerNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getTicker().getName()
                                        + " ("
                                        + param.getValue().getTicker().getSymbol()
                                        + ")"));

        TableColumn<TickerPurchase, String> tickerTypeColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_TYPE));
        tickerTypeColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateTickerType(
                                        param.getValue().getTicker().getType(), i18nService)));

        TableColumn<TickerPurchase, String> dateColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_DATE));
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.formatDateTimeForDisplay(
                                        param.getValue().getWalletTransaction().getDate(),
                                        i18nService)));

        TableColumn<TickerPurchase, String> quantityColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_QUANTITY));
        quantityColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getQuantity().toString()));

        TableColumn<TickerPurchase, String> unitPriceColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_UNIT_PRICE));
        unitPriceColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(param.getValue().getUnitPrice())));

        TableColumn<TickerPurchase, String> amountColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_TOTAL_AMOUNT));
        amountColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(
                                        param.getValue().getWalletTransaction().getAmount())));

        TableColumn<TickerPurchase, String> walletNameColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_WALLET));
        walletNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getWalletTransaction().getWallet().getName()));

        TableColumn<TickerPurchase, String> statusColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_STATUS));
        statusColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateTransactionStatus(
                                        param.getValue().getWalletTransaction().getStatus(),
                                        i18nService)));

        // Add the columns to the table view
        purchaseTableView.getColumns().add(idColumn);
        purchaseTableView.getColumns().add(tickerNameColumn);
        purchaseTableView.getColumns().add(tickerTypeColumn);
        purchaseTableView.getColumns().add(quantityColumn);
        purchaseTableView.getColumns().add(unitPriceColumn);
        purchaseTableView.getColumns().add(amountColumn);
        purchaseTableView.getColumns().add(walletNameColumn);
        purchaseTableView.getColumns().add(dateColumn);
        purchaseTableView.getColumns().add(statusColumn);
    }

    private TableColumn<TickerPurchase, Integer> getTickerPurchaseLongTableColumn() {
        TableColumn<TickerPurchase, Integer> idColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_ID));
        idColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getId()));

        // Align the ID column to the center
        idColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(Integer item, boolean empty) {
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

    /**
     * Configure the sale table view columns
     */
    private void configureSaleTableView() {
        TableColumn<TickerSale, Integer> idColumn = getTickerSaleLongTableColumn();

        TableColumn<TickerSale, String> tickerNameColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_TICKER));
        tickerNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getTicker().getName()
                                        + " ("
                                        + param.getValue().getTicker().getSymbol()
                                        + ")"));

        TableColumn<TickerSale, String> tickerTypeColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_TYPE));
        tickerTypeColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateTickerType(
                                        param.getValue().getTicker().getType(), i18nService)));

        TableColumn<TickerSale, String> dateColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_DATE));
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.formatDateTimeForDisplay(
                                        param.getValue().getWalletTransaction().getDate(),
                                        i18nService)));

        TableColumn<TickerSale, String> quantityColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_QUANTITY));
        quantityColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getQuantity().toString()));

        TableColumn<TickerSale, String> unitPriceColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_UNIT_PRICE));
        unitPriceColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(param.getValue().getUnitPrice())));

        TableColumn<TickerSale, String> amountColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_TOTAL_AMOUNT));
        amountColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(
                                        param.getValue().getWalletTransaction().getAmount())));

        TableColumn<TickerSale, String> walletNameColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_WALLET));
        walletNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getWalletTransaction().getWallet().getName()));

        TableColumn<TickerSale, String> statusColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_STATUS));
        statusColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateTransactionStatus(
                                        param.getValue().getWalletTransaction().getStatus(),
                                        i18nService)));

        // Add the columns to the table view
        saleTableView.getColumns().add(idColumn);
        saleTableView.getColumns().add(tickerNameColumn);
        saleTableView.getColumns().add(tickerTypeColumn);
        saleTableView.getColumns().add(quantityColumn);
        saleTableView.getColumns().add(unitPriceColumn);
        saleTableView.getColumns().add(amountColumn);
        saleTableView.getColumns().add(walletNameColumn);
        saleTableView.getColumns().add(dateColumn);
        saleTableView.getColumns().add(statusColumn);
    }

    private TableColumn<TickerSale, Integer> getTickerSaleLongTableColumn() {
        TableColumn<TickerSale, Integer> idColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_ID));
        idColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getId()));

        // Align the ID column to the center
        idColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(Integer item, boolean empty) {
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

    /**
     * Configure the table view columns
     */
    private void configureDividendTableView() {
        TableColumn<Dividend, Integer> idColumn = getDividendLongTableColumn();

        TableColumn<Dividend, String> tickerNameColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_TICKER));
        tickerNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getTicker().getName()
                                        + " ("
                                        + param.getValue().getTicker().getSymbol()
                                        + ")"));

        TableColumn<Dividend, String> tickerTypeColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_TYPE));
        tickerTypeColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateTickerType(
                                        param.getValue().getTicker().getType(), i18nService)));

        TableColumn<Dividend, String> dateColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_DATE));
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.formatDateTimeForDisplay(
                                        param.getValue().getWalletTransaction().getDate(),
                                        i18nService)));

        TableColumn<Dividend, String> amountColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_DIVIDEND_VALUE));
        amountColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(
                                        param.getValue().getWalletTransaction().getAmount())));

        TableColumn<Dividend, String> walletNameColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_WALLET));
        walletNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getWalletTransaction().getWallet().getName()));

        TableColumn<Dividend, String> statusColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_STATUS));
        statusColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateTransactionStatus(
                                        param.getValue().getWalletTransaction().getStatus(),
                                        i18nService)));

        // Add the columns to the table view
        dividendTableView.getColumns().add(idColumn);
        dividendTableView.getColumns().add(tickerNameColumn);
        dividendTableView.getColumns().add(tickerTypeColumn);
        dividendTableView.getColumns().add(amountColumn);
        dividendTableView.getColumns().add(walletNameColumn);
        dividendTableView.getColumns().add(dateColumn);
        dividendTableView.getColumns().add(statusColumn);
    }

    private TableColumn<Dividend, Integer> getDividendLongTableColumn() {
        TableColumn<Dividend, Integer> idColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_ID));
        idColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getId()));

        // Align the ID column to the center
        idColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(Integer item, boolean empty) {
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

    /**
     * Configure the table view columns
     */
    private void configureCryptoExchangeTableView() {
        TableColumn<CryptoExchange, Integer> idColumn = getCryptoExchangeLongTableColumn();

        TableColumn<CryptoExchange, String> soldCryptoNameColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_CRYPTO_SOLD));
        soldCryptoNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getSoldCrypto().getName()
                                        + " ("
                                        + param.getValue().getSoldCrypto().getSymbol()
                                        + ")"));

        TableColumn<CryptoExchange, String> receivedCryptoNameColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_CRYPTO_RECEIVED));
        receivedCryptoNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getReceivedCrypto().getName()
                                        + " ("
                                        + param.getValue().getReceivedCrypto().getSymbol()
                                        + ")"));

        TableColumn<CryptoExchange, BigDecimal> quantitySoldColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_QUANTITY_SOLD));
        quantitySoldColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getSoldQuantity()));

        TableColumn<CryptoExchange, BigDecimal> quantityReceivedColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.INVESTMENT_TABLE_QUANTITY_RECEIVED));
        quantityReceivedColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getReceivedQuantity()));

        TableColumn<CryptoExchange, String> dateColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_DATE));
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.formatDateTimeForDisplay(
                                        param.getValue().getDate(), i18nService)));

        TableColumn<CryptoExchange, String> descriptionColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_DESCRIPTION));
        descriptionColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getDescription()));

        // Add the columns to the table view
        cryptoExchangeTableView.getColumns().add(idColumn);
        cryptoExchangeTableView.getColumns().add(soldCryptoNameColumn);
        cryptoExchangeTableView.getColumns().add(receivedCryptoNameColumn);
        cryptoExchangeTableView.getColumns().add(quantitySoldColumn);
        cryptoExchangeTableView.getColumns().add(quantityReceivedColumn);
        cryptoExchangeTableView.getColumns().add(dateColumn);
        cryptoExchangeTableView.getColumns().add(descriptionColumn);
    }

    private TableColumn<CryptoExchange, Integer> getCryptoExchangeLongTableColumn() {
        TableColumn<CryptoExchange, Integer> idColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.INVESTMENT_TABLE_ID));
        idColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getId()));

        // Align the ID column to the center
        idColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(Integer item, boolean empty) {
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

    private void editPurchase(TickerPurchase purchase) {
        if (purchase == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_NO_PURCHASE_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_NO_PURCHASE_SELECTED_MESSAGE));

            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_TICKER_PURCHASE_FXML,
                i18nService.tr(Constants.TranslationKeys.INVESTMENT_DIALOG_EDIT_TICKER_PURCHASE),
                springContext,
                (EditTickerPurchaseController controller) -> controller.setPurchase(purchase),
                List.of(
                        () -> {
                            loadPurchasesFromDatabase();
                            updatePurchaseTableView();
                        }));
    }

    private void editSale(TickerSale sale) {
        if (sale == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_NO_SALE_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_NO_SALE_SELECTED_MESSAGE));

            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_TICKER_SALE_FXML,
                i18nService.tr(Constants.TranslationKeys.INVESTMENT_DIALOG_EDIT_TICKER_SALE),
                springContext,
                (EditTickerSaleController controller) -> controller.setSale(sale),
                List.of(
                        () -> {
                            loadSalesFromDatabase();
                            updateSaleTableView();
                        }));
    }

    private void editDividend(Dividend dividend) {
        if (dividend == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_NO_DIVIDEND_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_NO_DIVIDEND_SELECTED_MESSAGE));

            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_DIVIDEND_FXML,
                i18nService.tr(Constants.TranslationKeys.INVESTMENT_DIALOG_EDIT_DIVIDEND),
                springContext,
                (EditDividendController controller) -> controller.setDividend(dividend),
                List.of(
                        () -> {
                            loadDividendsFromDatabase();
                            updateDividendTableView();
                        }));
    }

    private void editCryptoExchange(CryptoExchange cryptoExchange) {
        if (cryptoExchange == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_NO_EXCHANGE_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_NO_EXCHANGE_SELECTED_MESSAGE));

            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_CRYPTO_EXCHANGE_FXML,
                i18nService.tr(Constants.TranslationKeys.INVESTMENT_DIALOG_EDIT_CRYPTO_EXCHANGE),
                springContext,
                (EditCryptoExchangeController controller) ->
                        controller.setCryptoExchange(cryptoExchange),
                List.of(
                        () -> {
                            loadCryptoExchangesFromDatabase();
                            updateCryptoExchangeTableView();
                        }));
    }

    private void deletePurchase(TickerPurchase purchase) {
        if (purchase == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_NO_PURCHASE_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_NO_PURCHASE_SELECTED_DELETE_MESSAGE));
            return;
        }

        String message = deleteMessage(purchase.getWalletTransaction());

        if (WindowUtils.showConfirmationDialog(
                i18nService.tr(
                        Constants.TranslationKeys.INVESTMENT_DIALOG_CONFIRM_DELETE_PURCHASE_TITLE),
                message,
                i18nService.getBundle())) {
            tickerService.deletePurchase(purchase.getId());
            loadPurchasesFromDatabase();
            updatePurchaseTableView();
        }
    }

    private void deleteSale(TickerSale sale) {
        if (sale == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_NO_SALE_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_NO_SALE_SELECTED_DELETE_MESSAGE));
            return;
        }

        String message = deleteMessage(sale.getWalletTransaction());

        if (WindowUtils.showConfirmationDialog(
                i18nService.tr(
                        Constants.TranslationKeys.INVESTMENT_DIALOG_CONFIRM_DELETE_SALE_TITLE),
                message,
                i18nService.getBundle())) {
            tickerService.deleteSale(sale.getId());
            loadSalesFromDatabase();
            updateSaleTableView();
        }
    }

    private void deleteDividend(Dividend dividend) {
        if (dividend == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_NO_DIVIDEND_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_NO_DIVIDEND_SELECTED_DELETE_MESSAGE));
            return;
        }

        String message = deleteMessage(dividend.getWalletTransaction());

        if (WindowUtils.showConfirmationDialog(
                i18nService.tr(
                        Constants.TranslationKeys.INVESTMENT_DIALOG_CONFIRM_DELETE_DIVIDEND_TITLE),
                message,
                i18nService.getBundle())) {
            tickerService.deleteDividend(dividend.getId());
            loadDividendsFromDatabase();
            updateDividendTableView();
        }
    }

    private void deleteCryptoExchange(CryptoExchange cryptoExchange) {
        if (cryptoExchange == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_NO_EXCHANGE_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_NO_EXCHANGE_SELECTED_DELETE_MESSAGE));
            return;
        }

        String message =
                MessageFormat.format(
                        "ID: {0}\n{1}\n{2}\n{3}\n{4}\n{5}\n{6}\n{7}\n{8}",
                        cryptoExchange.getId(),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys.INVESTMENT_DELETE_SOURCE_CRYPTO),
                                cryptoExchange.getSoldCrypto().getName(),
                                cryptoExchange.getSoldCrypto().getSymbol()),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys.INVESTMENT_DELETE_TARGET_CRYPTO),
                                cryptoExchange.getReceivedCrypto().getName(),
                                cryptoExchange.getReceivedCrypto().getSymbol()),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .INVESTMENT_DELETE_SOURCE_QUANTITY),
                                cryptoExchange.getSoldQuantity()),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .INVESTMENT_DELETE_SOURCE_QUANTITY_AFTER_DELETION),
                                cryptoExchange
                                        .getSoldCrypto()
                                        .getCurrentQuantity()
                                        .add(cryptoExchange.getSoldQuantity())),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .INVESTMENT_DELETE_TARGET_QUANTITY),
                                cryptoExchange.getReceivedQuantity()),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .INVESTMENT_DELETE_TARGET_QUANTITY_AFTER_DELETION),
                                cryptoExchange
                                        .getReceivedCrypto()
                                        .getCurrentQuantity()
                                        .subtract(cryptoExchange.getReceivedQuantity())),
                        MessageFormat.format(
                                i18nService.tr(Constants.TranslationKeys.INVESTMENT_DELETE_DATE),
                                UIUtils.formatDateTimeForDisplay(
                                        cryptoExchange.getDate(), i18nService)),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys.INVESTMENT_DELETE_DESCRIPTION),
                                cryptoExchange.getDescription()));

        if (WindowUtils.showConfirmationDialog(
                i18nService.tr(
                        Constants.TranslationKeys.INVESTMENT_DIALOG_CONFIRM_DELETE_EXCHANGE_TITLE),
                message,
                i18nService.getBundle())) {
            tickerService.deleteCryptoExchange(cryptoExchange.getId());
            loadCryptoExchangesFromDatabase();
            updateCryptoExchangeTableView();
        }
    }

    private String deleteMessage(WalletTransaction wt) {
        BigDecimal balanceAfterDeletion;
        if (wt.getStatus().equals(TransactionStatus.CONFIRMED)) {
            if (wt.getType().equals(TransactionType.EXPENSE)) {
                balanceAfterDeletion = wt.getWallet().getBalance().add(wt.getAmount());
            } else {
                balanceAfterDeletion = wt.getWallet().getBalance().subtract(wt.getAmount());
            }
        } else {
            balanceAfterDeletion = wt.getWallet().getBalance();
        }

        return MessageFormat.format(
                "{0}\n{1}\n{2}\n{3}\n{4}\n{5}\n{6}",
                MessageFormat.format(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_DELETE_DESCRIPTION),
                        wt.getDescription()),
                MessageFormat.format(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_DELETE_AMOUNT),
                        UIUtils.formatCurrency(wt.getAmount())),
                MessageFormat.format(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_DELETE_DATE),
                        UIUtils.formatDateTimeForDisplay(wt.getDate(), i18nService)),
                MessageFormat.format(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_DELETE_STATUS),
                        UIUtils.translateTransactionStatus(wt.getStatus(), i18nService)),
                MessageFormat.format(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_DELETE_WALLET),
                        wt.getWallet().getName()),
                MessageFormat.format(
                        i18nService.tr(Constants.TranslationKeys.INVESTMENT_DELETE_WALLET_BALANCE),
                        UIUtils.formatCurrency(wt.getWallet().getBalance())),
                MessageFormat.format(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .INVESTMENT_DELETE_WALLET_BALANCE_AFTER_DELETION),
                        UIUtils.formatCurrency(balanceAfterDeletion)));
    }
}
