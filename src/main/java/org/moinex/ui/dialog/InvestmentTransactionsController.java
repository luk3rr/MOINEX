/*
 * Filename: InvestmentTransactionsController.java
 * Created on: January 10, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

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
import org.moinex.entities.WalletTransaction;
import org.moinex.entities.investment.Dividend;
import org.moinex.entities.investment.TickerPurchase;
import org.moinex.entities.investment.TickerSale;
import org.moinex.services.TickerService;
import org.moinex.util.Constants;
import org.moinex.util.TransactionStatus;
import org.moinex.util.TransactionType;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Investment Transactions dialog
 */
@Controller
public class InvestmentTransactionsController
{
    @FXML
    private TableView<TickerPurchase> purchaseTableView;

    @FXML
    private TableView<TickerSale> saleTableView;

    @FXML
    private TableView<Dividend> dividendTableView;

    @FXML
    private TextField searchField;

    @FXML
    private TabPane tabPane;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private List<TickerPurchase> purchases;

    private List<TickerSale> sales;

    private List<Dividend> dividends;

    private TickerService tickerService;

    /**
     * Constructor
     * @param TickerService tickerService
     * @note This constructor is used for dependency injection
     */
    public InvestmentTransactionsController(TickerService tickerService)

    {
        this.tickerService = tickerService;
    }

    @FXML
    public void initialize()
    {
        LoadPurchasesFromDatabase();
        LoadSalesFromDatabase();
        LoadDividendsFromDatabase();

        ConfigurePurchaseTableView();
        ConfigureSaleTableView();
        ConfigureDividendTableView();

        UpdatePurchaseTableView();
        UpdateSaleTableView();
        UpdateDividendTableView();

        // Add listener to the search field
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            UpdatePurchaseTableView();
            UpdateSaleTableView();
            UpdateDividendTableView();
        });
    }

    @FXML
    private void handleEdit()
    {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();

        if (selectedTab == null)
        {
            return;
        }

        if (selectedTab == tabPane.getTabs().get(0))
        {
            TickerPurchase selectedPurchase =
                purchaseTableView.getSelectionModel().getSelectedItem();

            EditPurchase(selectedPurchase);
        }
        else if (selectedTab == tabPane.getTabs().get(1))
        {
            TickerSale selectedSale =
                saleTableView.getSelectionModel().getSelectedItem();

            EditSale(selectedSale);
        }
        else if (selectedTab == tabPane.getTabs().get(2))
        {
            Dividend selectedDividend =
                dividendTableView.getSelectionModel().getSelectedItem();

            EditDividend(selectedDividend);
        }
    }

    @FXML
    private void handleDelete()
    {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();

        if (selectedTab == null)
        {
            return;
        }

        if (selectedTab == tabPane.getTabs().get(0))
        {
            TickerPurchase selectedPurchase =
                purchaseTableView.getSelectionModel().getSelectedItem();

            DeletePurchase(selectedPurchase);
        }
        else if (selectedTab == tabPane.getTabs().get(1))
        {
            TickerSale selectedSale =
                saleTableView.getSelectionModel().getSelectedItem();

            DeleteSale(selectedSale);
        }
        else if (selectedTab == tabPane.getTabs().get(2))
        {
            Dividend selectedDividend =
                dividendTableView.getSelectionModel().getSelectedItem();

            DeleteDividend(selectedDividend);
        }
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)searchField.getScene().getWindow();
        stage.close();
    }

    /**
     * Loads the purchases from the database
     */
    private void LoadPurchasesFromDatabase()
    {
        purchases = tickerService.GetAllPurchases();
    }

    /**
     * Loads the sales from the database
     */
    private void LoadSalesFromDatabase()
    {
        sales = tickerService.GetAllSales();
    }

    /**
     * Loads the dividends from the database
     */
    private void LoadDividendsFromDatabase()
    {
        dividends = tickerService.GetAllDividends();
    }

    /**
     * Updates dividend table view
     */
    private void UpdateDividendTableView()
    {
        String similarTextOrId = searchField.getText().toLowerCase();

        dividendTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty())
        {
            dividendTableView.getItems().setAll(dividends);
        }
        else
        {
            dividends.stream()
                .filter(d -> {
                    String id           = d.GetId().toString();
                    String tickerName   = d.GetTicker().GetName().toLowerCase();
                    String tickerSymbol = d.GetTicker().GetSymbol().toLowerCase();
                    String amount = d.GetWalletTransaction().GetAmount().toString();
                    String walletName =
                        d.GetWalletTransaction().GetWallet().GetName().toLowerCase();
                    String date = d.GetWalletTransaction().GetDate().format(
                        Constants.DATE_FORMATTER_WITH_TIME);
                    String status =
                        d.GetWalletTransaction().GetStatus().name().toLowerCase();

                    return id.contains(similarTextOrId) ||
                        tickerName.contains(similarTextOrId) ||
                        tickerSymbol.contains(similarTextOrId) ||
                        amount.contains(similarTextOrId) ||
                        walletName.contains(similarTextOrId) ||
                        date.contains(similarTextOrId) ||
                        status.contains(similarTextOrId);
                })
                .forEach(dividendTableView.getItems()::add);
        }

        dividendTableView.refresh();
    }

    /**
     * Updates purchase table view
     */
    private void UpdatePurchaseTableView()
    {
        String similarTextOrId = searchField.getText().toLowerCase();

        purchaseTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty())
        {
            purchaseTableView.getItems().setAll(purchases);
        }
        else
        {
            purchases.stream()
                .filter(p -> {
                    String id           = p.GetId().toString();
                    String tickerName   = p.GetTicker().GetName().toLowerCase();
                    String tickerSymbol = p.GetTicker().GetSymbol().toLowerCase();
                    String date         = p.GetWalletTransaction().GetDate().format(
                        Constants.DATE_FORMATTER_WITH_TIME);
                    String quantity  = p.GetQuantity().toString();
                    String unitPrice = p.GetUnitPrice().toString();
                    String amount    = p.GetWalletTransaction().GetAmount().toString();
                    String walletName =
                        p.GetWalletTransaction().GetWallet().GetName().toLowerCase();
                    String status =
                        p.GetWalletTransaction().GetStatus().name().toLowerCase();

                    return id.contains(similarTextOrId) ||
                        tickerName.contains(similarTextOrId) ||
                        tickerSymbol.contains(similarTextOrId) ||
                        date.contains(similarTextOrId) ||
                        quantity.contains(similarTextOrId) ||
                        unitPrice.contains(similarTextOrId) ||
                        amount.contains(similarTextOrId) ||
                        walletName.contains(similarTextOrId) ||
                        status.contains(similarTextOrId);
                })
                .forEach(purchaseTableView.getItems()::add);
        }

        purchaseTableView.refresh();
    }

    /**
     * Updates sale table view
     */
    private void UpdateSaleTableView()
    {
        String similarTextOrId = searchField.getText().toLowerCase();

        saleTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty())
        {
            saleTableView.getItems().setAll(sales);
        }
        else
        {
            sales.stream()
                .filter(s -> {
                    String id           = s.GetId().toString();
                    String tickerName   = s.GetTicker().GetName().toLowerCase();
                    String tickerSymbol = s.GetTicker().GetSymbol().toLowerCase();
                    String date         = s.GetWalletTransaction().GetDate().format(
                        Constants.DATE_FORMATTER_WITH_TIME);
                    String quantity  = s.GetQuantity().toString();
                    String unitPrice = s.GetUnitPrice().toString();
                    String amount    = s.GetWalletTransaction().GetAmount().toString();
                    String walletName =
                        s.GetWalletTransaction().GetWallet().GetName().toLowerCase();
                    String status =
                        s.GetWalletTransaction().GetStatus().name().toLowerCase();

                    return id.contains(similarTextOrId) ||
                        tickerName.contains(similarTextOrId) ||
                        tickerSymbol.contains(similarTextOrId) ||
                        date.contains(similarTextOrId) ||
                        quantity.contains(similarTextOrId) ||
                        unitPrice.contains(similarTextOrId) ||
                        amount.contains(similarTextOrId) ||
                        walletName.contains(similarTextOrId) ||
                        status.contains(similarTextOrId);
                })
                .forEach(saleTableView.getItems()::add);
        }

        saleTableView.refresh();
    }

    /**
     * Configure the purchase table view columns
     */
    private void ConfigurePurchaseTableView()
    {
        TableColumn<TickerPurchase, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(
            param -> new SimpleObjectProperty<>(param.getValue().GetId()));

        // Align the ID column to the center
        idColumn.setCellFactory(column -> {
            return new TableCell<TickerPurchase, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty)
                {
                    super.updateItem(item, empty);
                    if (item == null || empty)
                    {
                        setText(null);
                    }
                    else
                    {
                        setText(item.toString());
                        setAlignment(Pos.CENTER);
                        setStyle("-fx-padding: 0;"); // set padding to zero to
                                                     // ensure the text is centered
                    }
                }
            };
        });

        TableColumn<TickerPurchase, String> tickerNameColumn =
            new TableColumn<>("Ticker");
        tickerNameColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().GetTicker().GetName() + " (" +
                                        param.getValue().GetTicker().GetSymbol() +
                                        ")"));

        TableColumn<TickerPurchase, String> tickerTypeColumn =
            new TableColumn<>("Type");
        tickerTypeColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().GetTicker().GetType().name()));

        TableColumn<TickerPurchase, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(
                param.getValue().GetWalletTransaction().GetDate().format(
                    Constants.DATE_FORMATTER_WITH_TIME)));

        TableColumn<TickerPurchase, String> quantityColumn =
            new TableColumn<>("Quantity");
        quantityColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(param.getValue().GetQuantity().toString()));

        TableColumn<TickerPurchase, String> unitPriceColumn =
            new TableColumn<>("Unit Price");
        unitPriceColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(
                UIUtils.FormatCurrency(param.getValue().GetUnitPrice())));

        TableColumn<TickerPurchase, String> amountColumn =
            new TableColumn<>("Total Amount");
        amountColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(UIUtils.FormatCurrency(
                param.getValue().GetWalletTransaction().GetAmount())));

        TableColumn<TickerPurchase, String> walletNameColumn =
            new TableColumn<>("Wallet");
        walletNameColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(
                param.getValue().GetWalletTransaction().GetWallet().GetName()));

        TableColumn<TickerPurchase, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(
                param.getValue().GetWalletTransaction().GetStatus().name()));

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

    /**
     * Configure the sale table view columns
     */
    private void ConfigureSaleTableView()
    {
        TableColumn<TickerSale, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(
            param -> new SimpleObjectProperty<>(param.getValue().GetId()));

        // Align the ID column to the center
        idColumn.setCellFactory(column -> {
            return new TableCell<TickerSale, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty)
                {
                    super.updateItem(item, empty);
                    if (item == null || empty)
                    {
                        setText(null);
                    }
                    else
                    {
                        setText(item.toString());
                        setAlignment(Pos.CENTER);
                        setStyle("-fx-padding: 0;"); // set padding to zero to
                                                     // ensure the text is centered
                    }
                }
            };
        });

        TableColumn<TickerSale, String> tickerNameColumn = new TableColumn<>("Ticker");
        tickerNameColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().GetTicker().GetName() + " (" +
                                        param.getValue().GetTicker().GetSymbol() +
                                        ")"));

        TableColumn<TickerSale, String> tickerTypeColumn = new TableColumn<>("Type");
        tickerTypeColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().GetTicker().GetType().name()));

        TableColumn<TickerSale, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(
                param.getValue().GetWalletTransaction().GetDate().format(
                    Constants.DATE_FORMATTER_WITH_TIME)));

        TableColumn<TickerSale, String> quantityColumn = new TableColumn<>("Quantity");
        quantityColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(param.getValue().GetQuantity().toString()));

        TableColumn<TickerSale, String> unitPriceColumn =
            new TableColumn<>("Unit Price");
        unitPriceColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(
                UIUtils.FormatCurrency(param.getValue().GetUnitPrice())));

        TableColumn<TickerSale, String> amountColumn =
            new TableColumn<>("Total Amount");
        amountColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(UIUtils.FormatCurrency(
                param.getValue().GetWalletTransaction().GetAmount())));

        TableColumn<TickerSale, String> walletNameColumn = new TableColumn<>("Wallet");
        walletNameColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(
                param.getValue().GetWalletTransaction().GetWallet().GetName()));

        TableColumn<TickerSale, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(
                param.getValue().GetWalletTransaction().GetStatus().name()));

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

    /**
     * Configure the table view columns
     */
    private void ConfigureDividendTableView()
    {
        TableColumn<Dividend, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(
            param -> new SimpleObjectProperty<>(param.getValue().GetId()));

        // Align the ID column to the center
        idColumn.setCellFactory(column -> {
            return new TableCell<Dividend, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty)
                {
                    super.updateItem(item, empty);
                    if (item == null || empty)
                    {
                        setText(null);
                    }
                    else
                    {
                        setText(item.toString());
                        setAlignment(Pos.CENTER);
                        setStyle("-fx-padding: 0;"); // set padding to zero to
                                                     // ensure the text is centered
                    }
                }
            };
        });

        TableColumn<Dividend, String> tickerNameColumn = new TableColumn<>("Ticker");
        tickerNameColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().GetTicker().GetName() + " (" +
                                        param.getValue().GetTicker().GetSymbol() +
                                        ")"));

        TableColumn<Dividend, String> tickerTypeColumn = new TableColumn<>("Type");
        tickerTypeColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().GetTicker().GetType().name()));

        TableColumn<Dividend, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(
                param.getValue().GetWalletTransaction().GetDate().format(
                    Constants.DATE_FORMATTER_WITH_TIME)));

        TableColumn<Dividend, String> amountColumn = new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(UIUtils.FormatCurrency(
                param.getValue().GetWalletTransaction().GetAmount())));

        TableColumn<Dividend, String> walletNameColumn = new TableColumn<>("Wallet");
        walletNameColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(
                param.getValue().GetWalletTransaction().GetWallet().GetName()));

        TableColumn<Dividend, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(
                param.getValue().GetWalletTransaction().GetStatus().name()));

        // Add the columns to the table view
        dividendTableView.getColumns().add(idColumn);
        dividendTableView.getColumns().add(tickerNameColumn);
        dividendTableView.getColumns().add(tickerTypeColumn);
        dividendTableView.getColumns().add(amountColumn);
        dividendTableView.getColumns().add(walletNameColumn);
        dividendTableView.getColumns().add(dateColumn);
        dividendTableView.getColumns().add(statusColumn);
    }

    private void EditPurchase(TickerPurchase purchase)
    {
        if (purchase == null)
        {
            WindowUtils.ShowInformationDialog("Info",
                                              "No purchase selected",
                                              "Please select a purchase to edit");

            return;
        }

        WindowUtils.OpenModalWindow(Constants.EDIT_TICKER_PURCHASE_FXML,
                                    "Edit ticker purchase",
                                    springContext,
                                    (EditTickerPurchaseController controller)
                                        -> controller.SetPurchase(purchase),
                                    List.of(() -> {
                                        LoadPurchasesFromDatabase();
                                        UpdatePurchaseTableView();
                                    }));
    }

    private void EditSale(TickerSale sale)
    {
        if (sale == null)
        {
            WindowUtils.ShowInformationDialog("Info",
                                              "No sale selected",
                                              "Please select a sale to edit");

            return;
        }

        WindowUtils.OpenModalWindow(Constants.EDIT_TICKER_SALE_FXML,
                                    "Edit ticker sale",
                                    springContext,
                                    (EditTickerSaleController controller)
                                        -> controller.SetSale(sale),
                                    List.of(() -> {
                                        LoadSalesFromDatabase();
                                        UpdateSaleTableView();
                                    }));
    }

    private void EditDividend(Dividend dividend)
    {
        if (dividend == null)
        {
            WindowUtils.ShowInformationDialog("Info",
                                              "No dividend selected",
                                              "Please select a dividend to edit");

            return;
        }

        WindowUtils.OpenModalWindow(Constants.EDIT_DIVIDEND_FXML,
                                    "Edit dividend",
                                    springContext,
                                    (EditDividendController controller)
                                        -> controller.SetDividend(dividend),
                                    List.of(() -> {
                                        LoadDividendsFromDatabase();
                                        UpdateDividendTableView();
                                    }));
    }

    private void DeletePurchase(TickerPurchase purchase)
    {
        if (purchase == null)
        {
            WindowUtils.ShowInformationDialog("Info",
                                              "No purchase selected",
                                              "Please select a purchase to delete");
            return;
        }

        String message = DeleteMessage(purchase.GetWalletTransaction());

        if (WindowUtils.ShowConfirmationDialog(
                "Confirm deletion",
                "Are you sure you want to delete the purchase?",
                message))
        {
            tickerService.DeletePurchase(purchase.GetId());
            LoadPurchasesFromDatabase();
            UpdatePurchaseTableView();
        }
    }

    private void DeleteSale(TickerSale sale)
    {
        if (sale == null)
        {
            WindowUtils.ShowInformationDialog("Info",
                                              "No sale selected",
                                              "Please select a sale to delete");
            return;
        }

        String message = DeleteMessage(sale.GetWalletTransaction());

        if (WindowUtils.ShowConfirmationDialog(
                "Delete sale",
                "Are you sure you want to delete the sale?",
                message))
        {
            tickerService.DeleteSale(sale.GetId());
            LoadSalesFromDatabase();
            UpdateSaleTableView();
        }
    }

    private void DeleteDividend(Dividend dividend)
    {
        if (dividend == null)
        {
            WindowUtils.ShowInformationDialog("Info",
                                              "No dividend selected",
                                              "Please select a dividend to delete");
            return;
        }

        String message = DeleteMessage(dividend.GetWalletTransaction());

        if (WindowUtils.ShowConfirmationDialog(
                "Delete dividend",
                "Are you sure you want to delete the dividend?",
                message))
        {
            tickerService.DeleteDividend(dividend.GetId());
            LoadDividendsFromDatabase();
            UpdateDividendTableView();
        }
    }

    private String DeleteMessage(WalletTransaction wt)
    {
        // Create a message to show to the user
        StringBuilder message = new StringBuilder();
        message.append("Description: ")
            .append(wt.GetDescription())
            .append("\n")
            .append("Amount: ")
            .append(UIUtils.FormatCurrency(wt.GetAmount()))
            .append("\n")
            .append("Date: ")
            .append(wt.GetDate().format(Constants.DATE_FORMATTER_WITH_TIME))
            .append("\n")
            .append("Status: ")
            .append(wt.GetStatus().toString())
            .append("\n")
            .append("Wallet: ")
            .append(wt.GetWallet().GetName())
            .append("\n")
            .append("Wallet balance: ")
            .append(UIUtils.FormatCurrency(wt.GetWallet().GetBalance()))
            .append("\n")
            .append("Wallet balance after deletion: ");

        if (wt.GetStatus().equals(TransactionStatus.CONFIRMED))
        {
            if (wt.GetType().equals(TransactionType.EXPENSE))
            {
                message
                    .append(UIUtils.FormatCurrency(
                        wt.GetWallet().GetBalance().add(wt.GetAmount())))
                    .append("\n");
            }
            else
            {
                message
                    .append(UIUtils.FormatCurrency(
                        wt.GetWallet().GetBalance().subtract(wt.GetAmount())))
                    .append("\n");
            }
        }
        else
        {
            message.append(UIUtils.FormatCurrency(wt.GetWallet().GetBalance()))
                .append("\n");
        }

        return message.toString();
    }
}
