/*
 * Filename: InvestmentTransactionsController.java
 * Created on: January 10, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import java.math.BigDecimal;
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
import org.moinex.model.investment.CryptoExchange;
import org.moinex.model.investment.Dividend;
import org.moinex.model.investment.TickerPurchase;
import org.moinex.model.investment.TickerSale;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.service.TickerService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;
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

    /**
     * Constructor
     * @param tickerService tickerService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public InvestmentTransactionsController(
            TickerService tickerService, ConfigurableApplicationContext springContext) {

        this.tickerService = tickerService;
        this.springContext = springContext;
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
                                        d.getWalletTransaction()
                                                .getDate()
                                                .format(Constants.DATE_FORMATTER_WITH_TIME);
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
                                        p.getWalletTransaction()
                                                .getDate()
                                                .format(Constants.DATE_FORMATTER_WITH_TIME);
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
                                        s.getWalletTransaction()
                                                .getDate()
                                                .format(Constants.DATE_FORMATTER_WITH_TIME);
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
                                        ce.getDate().format(Constants.DATE_FORMATTER_WITH_TIME);
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
        TableColumn<TickerPurchase, Long> idColumn = getTickerPurchaseLongTableColumn();

        TableColumn<TickerPurchase, String> tickerNameColumn = new TableColumn<>("Ticker");
        tickerNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getTicker().getName()
                                        + " ("
                                        + param.getValue().getTicker().getSymbol()
                                        + ")"));

        TableColumn<TickerPurchase, String> tickerTypeColumn = new TableColumn<>("Type");
        tickerTypeColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getTicker().getType().name()));

        TableColumn<TickerPurchase, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue()
                                        .getWalletTransaction()
                                        .getDate()
                                        .format(Constants.DATE_FORMATTER_WITH_TIME)));

        TableColumn<TickerPurchase, String> quantityColumn = new TableColumn<>("Quantity");
        quantityColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getQuantity().toString()));

        TableColumn<TickerPurchase, String> unitPriceColumn = new TableColumn<>("Unit Price");
        unitPriceColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(param.getValue().getUnitPrice())));

        TableColumn<TickerPurchase, String> amountColumn = new TableColumn<>("Total Amount");
        amountColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(
                                        param.getValue().getWalletTransaction().getAmount())));

        TableColumn<TickerPurchase, String> walletNameColumn = new TableColumn<>("Wallet");
        walletNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getWalletTransaction().getWallet().getName()));

        TableColumn<TickerPurchase, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getWalletTransaction().getStatus().name()));

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

    private static TableColumn<TickerPurchase, Long> getTickerPurchaseLongTableColumn() {
        TableColumn<TickerPurchase, Long> idColumn = new TableColumn<>("ID");
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

    /**
     * Configure the sale table view columns
     */
    private void configureSaleTableView() {
        TableColumn<TickerSale, Long> idColumn = getTickerSaleLongTableColumn();

        TableColumn<TickerSale, String> tickerNameColumn = new TableColumn<>("Ticker");
        tickerNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getTicker().getName()
                                        + " ("
                                        + param.getValue().getTicker().getSymbol()
                                        + ")"));

        TableColumn<TickerSale, String> tickerTypeColumn = new TableColumn<>("Type");
        tickerTypeColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getTicker().getType().name()));

        TableColumn<TickerSale, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue()
                                        .getWalletTransaction()
                                        .getDate()
                                        .format(Constants.DATE_FORMATTER_WITH_TIME)));

        TableColumn<TickerSale, String> quantityColumn = new TableColumn<>("Quantity");
        quantityColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getQuantity().toString()));

        TableColumn<TickerSale, String> unitPriceColumn = new TableColumn<>("Unit Price");
        unitPriceColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(param.getValue().getUnitPrice())));

        TableColumn<TickerSale, String> amountColumn = new TableColumn<>("Total Amount");
        amountColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(
                                        param.getValue().getWalletTransaction().getAmount())));

        TableColumn<TickerSale, String> walletNameColumn = new TableColumn<>("Wallet");
        walletNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getWalletTransaction().getWallet().getName()));

        TableColumn<TickerSale, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getWalletTransaction().getStatus().name()));

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

    private static TableColumn<TickerSale, Long> getTickerSaleLongTableColumn() {
        TableColumn<TickerSale, Long> idColumn = new TableColumn<>("ID");
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

    /**
     * Configure the table view columns
     */
    private void configureDividendTableView() {
        TableColumn<Dividend, Long> idColumn = getDividendLongTableColumn();

        TableColumn<Dividend, String> tickerNameColumn = new TableColumn<>("Ticker");
        tickerNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getTicker().getName()
                                        + " ("
                                        + param.getValue().getTicker().getSymbol()
                                        + ")"));

        TableColumn<Dividend, String> tickerTypeColumn = new TableColumn<>("Type");
        tickerTypeColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getTicker().getType().name()));

        TableColumn<Dividend, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue()
                                        .getWalletTransaction()
                                        .getDate()
                                        .format(Constants.DATE_FORMATTER_WITH_TIME)));

        TableColumn<Dividend, String> amountColumn = new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(
                                        param.getValue().getWalletTransaction().getAmount())));

        TableColumn<Dividend, String> walletNameColumn = new TableColumn<>("Wallet");
        walletNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getWalletTransaction().getWallet().getName()));

        TableColumn<Dividend, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getWalletTransaction().getStatus().name()));

        // Add the columns to the table view
        dividendTableView.getColumns().add(idColumn);
        dividendTableView.getColumns().add(tickerNameColumn);
        dividendTableView.getColumns().add(tickerTypeColumn);
        dividendTableView.getColumns().add(amountColumn);
        dividendTableView.getColumns().add(walletNameColumn);
        dividendTableView.getColumns().add(dateColumn);
        dividendTableView.getColumns().add(statusColumn);
    }

    private static TableColumn<Dividend, Long> getDividendLongTableColumn() {
        TableColumn<Dividend, Long> idColumn = new TableColumn<>("ID");
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

    /**
     * Configure the table view columns
     */
    private void configureCryptoExchangeTableView() {
        TableColumn<CryptoExchange, Long> idColumn = getCryptoExchangeLongTableColumn();

        TableColumn<CryptoExchange, String> soldCryptoNameColumn = new TableColumn<>("Sold");
        soldCryptoNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getSoldCrypto().getName()
                                        + " ("
                                        + param.getValue().getSoldCrypto().getSymbol()
                                        + ")"));

        TableColumn<CryptoExchange, String> receivedCryptoNameColumn =
                new TableColumn<>("Received");
        receivedCryptoNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getReceivedCrypto().getName()
                                        + " ("
                                        + param.getValue().getReceivedCrypto().getSymbol()
                                        + ")"));

        TableColumn<CryptoExchange, BigDecimal> quantitySoldColumn =
                new TableColumn<>("Quantity Sold");
        quantitySoldColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getSoldQuantity()));

        TableColumn<CryptoExchange, BigDecimal> quantityReceivedColumn =
                new TableColumn<>("Quantity Received");
        quantityReceivedColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getReceivedQuantity()));

        TableColumn<CryptoExchange, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue()
                                        .getDate()
                                        .format(Constants.DATE_FORMATTER_WITH_TIME)));

        TableColumn<CryptoExchange, String> descriptionColumn = new TableColumn<>("Description");
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

    private static TableColumn<CryptoExchange, Long> getCryptoExchangeLongTableColumn() {
        TableColumn<CryptoExchange, Long> idColumn = new TableColumn<>("ID");
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

    private void editPurchase(TickerPurchase purchase) {
        if (purchase == null) {
            WindowUtils.showInformationDialog(
                    "No purchase selected", "Please select a purchase to edit");

            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_TICKER_PURCHASE_FXML,
                "Edit ticker purchase",
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
            WindowUtils.showInformationDialog("No sale selected", "Please select a sale to edit");

            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_TICKER_SALE_FXML,
                "Edit ticker sale",
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
                    "No dividend selected", "Please select a dividend to edit");

            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_DIVIDEND_FXML,
                "Edit dividend",
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
                    "No crypto exchange selected", "Please select a crypto exchange to edit");

            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_CRYPTO_EXCHANGE_FXML,
                "Edit crypto exchange",
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
                    "No purchase selected", "Please select a purchase to delete");
            return;
        }

        String message = deleteMessage(purchase.getWalletTransaction());

        if (WindowUtils.showConfirmationDialog(
                "Are you sure you want to delete the purchase?", message)) {
            tickerService.deletePurchase(purchase.getId());
            loadPurchasesFromDatabase();
            updatePurchaseTableView();
        }
    }

    private void deleteSale(TickerSale sale) {
        if (sale == null) {
            WindowUtils.showInformationDialog("No sale selected", "Please select a sale to delete");
            return;
        }

        String message = deleteMessage(sale.getWalletTransaction());

        if (WindowUtils.showConfirmationDialog(
                "Are you sure you want to delete the sale?", message)) {
            tickerService.deleteSale(sale.getId());
            loadSalesFromDatabase();
            updateSaleTableView();
        }
    }

    private void deleteDividend(Dividend dividend) {
        if (dividend == null) {
            WindowUtils.showInformationDialog(
                    "No dividend selected", "Please select a dividend to delete");
            return;
        }

        String message = deleteMessage(dividend.getWalletTransaction());

        if (WindowUtils.showConfirmationDialog(
                "Are you sure you want to delete the dividend?", message)) {
            tickerService.deleteDividend(dividend.getId());
            loadDividendsFromDatabase();
            updateDividendTableView();
        }
    }

    private void deleteCryptoExchange(CryptoExchange cryptoExchange) {
        if (cryptoExchange == null) {
            WindowUtils.showInformationDialog(
                    "No crypto exchange selected", "Please select a crypto exchange to delete");
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append("ID: ")
                .append(cryptoExchange.getId())
                .append("\n")
                .append("Source crypto: ")
                .append(cryptoExchange.getSoldCrypto().getName())
                .append(" (")
                .append(cryptoExchange.getSoldCrypto().getSymbol())
                .append(")\n")
                .append("Target crypto: ")
                .append(cryptoExchange.getReceivedCrypto().getName())
                .append(" (")
                .append(cryptoExchange.getReceivedCrypto().getSymbol())
                .append(")\n")
                .append("Source quantity: ")
                .append(cryptoExchange.getSoldQuantity())
                .append("\n")
                .append("Source quantity after deletion: ")
                .append(
                        cryptoExchange
                                .getSoldCrypto()
                                .getCurrentQuantity()
                                .add(cryptoExchange.getSoldQuantity()))
                .append("\n")
                .append("Target quantity: ")
                .append(cryptoExchange.getReceivedQuantity())
                .append("\n")
                .append("Target quantity after deletion: ")
                .append(
                        cryptoExchange
                                .getReceivedCrypto()
                                .getCurrentQuantity()
                                .subtract(cryptoExchange.getReceivedQuantity()))
                .append("\n")
                .append("Date: ")
                .append(cryptoExchange.getDate().format(Constants.DATE_FORMATTER_WITH_TIME))
                .append("\n")
                .append("Description: ")
                .append(cryptoExchange.getDescription())
                .append("\n");

        if (WindowUtils.showConfirmationDialog(
                "Are you sure you want to delete the crypto exchange?", message.toString())) {
            tickerService.deleteCryptoExchange(cryptoExchange.getId());
            loadCryptoExchangesFromDatabase();
            updateCryptoExchangeTableView();
        }
    }

    private String deleteMessage(WalletTransaction wt) {
        // Create a message to show to the user
        StringBuilder message = new StringBuilder();
        message.append("Description: ")
                .append(wt.getDescription())
                .append("\n")
                .append("Amount: ")
                .append(UIUtils.formatCurrency(wt.getAmount()))
                .append("\n")
                .append("Date: ")
                .append(wt.getDate().format(Constants.DATE_FORMATTER_WITH_TIME))
                .append("\n")
                .append("Status: ")
                .append(wt.getStatus().toString())
                .append("\n")
                .append("Wallet: ")
                .append(wt.getWallet().getName())
                .append("\n")
                .append("Wallet balance: ")
                .append(UIUtils.formatCurrency(wt.getWallet().getBalance()))
                .append("\n")
                .append("Wallet balance after deletion: ");

        if (wt.getStatus().equals(TransactionStatus.CONFIRMED)) {
            if (wt.getType().equals(TransactionType.EXPENSE)) {
                message.append(
                                UIUtils.formatCurrency(
                                        wt.getWallet().getBalance().add(wt.getAmount())))
                        .append("\n");
            } else {
                message.append(
                                UIUtils.formatCurrency(
                                        wt.getWallet().getBalance().subtract(wt.getAmount())))
                        .append("\n");
            }
        } else {
            message.append(UIUtils.formatCurrency(wt.getWallet().getBalance())).append("\n");
        }

        return message.toString();
    }
}
