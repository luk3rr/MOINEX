/*
 * Filename: SavingsController.java
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import java.math.BigDecimal;
import java.util.List;
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
import javafx.util.StringConverter;
import org.moinex.entities.investment.Ticker;
import org.moinex.services.TickerService;
import org.moinex.ui.dialog.AddTickerController;
import org.moinex.util.Constants;
import org.moinex.util.TickerType;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller class for the Savings view
 */
@Controller
public class SavingsController
{
    @FXML
    private TableView<Ticker> stocksFundsTabTickerTable;

    @FXML
    private TextField stocksFundsTabTickerSearchField;

    @FXML
    private ComboBox<TickerType> stocksFundsTabTickerTypeComboBox;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private TickerService tickerService;

    /**
     * Constructor
     * @param tickerService The ticker service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public SavingsController(TickerService tickerService)
    {
        this.tickerService = tickerService;
    }

    @FXML
    private void initialize()
    {
        ConfigureTableView();
        PopulateTickerTypeComboBox();
        UpdateTransactionTableView();

        ConfigureListeners();
    }

    @FXML
    private void handleRegisterTicker()
    {
        WindowUtils.OpenModalWindow(Constants.ADD_TICKER_FXML,
                                    "Add Ticker",
                                    springContext,
                                    (AddTickerController controller)
                                        -> {},
                                    List.of(() -> {}));
    }

    @FXML
    private void handleBuyTicker()
    { }

    @FXML
    private void handleSellTicker()
    { }

    @FXML
    private void handleAddDividend()
    { }

    @FXML
    private void handleOpenTickerArchive()
    { }

    @FXML
    private void handleShowTransactions()
    { }

    @FXML
    private void handleUpdatePrices()
    { }

    @FXML
    private void handleEditTicker()
    { }

    @FXML
    private void handleDeleteTicker()
    { }

    /**
     * Update the transaction table view
     */
    private void UpdateTransactionTableView()
    {
        // Get the search text
        String similarTextOrId =
            stocksFundsTabTickerSearchField.getText().toLowerCase();

        // Get selected values from the comboboxes
        TickerType selectedTickerType = stocksFundsTabTickerTypeComboBox.getValue();

        // Clear the table view
        stocksFundsTabTickerTable.getItems().clear();

        // Fetch all tickers within the selected range and filter by ticker
        // type. If ticker type is null, all tickers are fetched
        if (similarTextOrId.isEmpty())
        {
            tickerService.GetAllNonArchivedTickers()
                .stream()
                .filter(t
                        -> selectedTickerType == null ||
                               t.GetType().equals(selectedTickerType))
                .forEach(stocksFundsTabTickerTable.getItems()::add);
        }
        else
        {
            tickerService.GetAllNonArchivedTickers()
                .stream()
                .filter(t
                        -> selectedTickerType == null ||
                               t.GetType().equals(selectedTickerType))
                .filter(t -> {
                    String name       = t.GetName().toLowerCase();
                    String symbol     = t.GetSymbol().toLowerCase();
                    String type       = t.GetType().toString().toLowerCase();
                    String quantity   = t.GetCurrentQuantity().toString();
                    String unitValue  = t.GetCurrentUnitValue().toString();
                    String totalValue = t.GetCurrentQuantity()
                                            .multiply(t.GetCurrentUnitValue())
                                            .toString();

                    return name.contains(similarTextOrId) ||
                        symbol.contains(similarTextOrId) ||
                        type.contains(similarTextOrId) ||
                        quantity.contains(similarTextOrId) ||
                        unitValue.contains(similarTextOrId) ||
                        totalValue.contains(similarTextOrId);
                })
                .forEach(stocksFundsTabTickerTable.getItems()::add);
        }

        stocksFundsTabTickerTable.refresh();
    }

    /**
     * Populate the ticker type combo box
     */
    private void PopulateTickerTypeComboBox()
    {
        // Make a copy of the list to add the 'All' option
        // Add 'All' option to the ticker type combo box
        // All is the first element in the list and is represented by a null value
        ObservableList<TickerType> tickerTypesWithNull =
            FXCollections.observableArrayList(TickerType.values());

        tickerTypesWithNull.add(0, null);

        stocksFundsTabTickerTypeComboBox.setItems(tickerTypesWithNull);

        stocksFundsTabTickerTypeComboBox.setConverter(
            new StringConverter<TickerType>() {
                @Override
                public String toString(TickerType tickerType)
                {
                    return tickerType != null ? tickerType.toString()
                                              : "ALL"; // Show "All" instead of null
                }

                @Override
                public TickerType fromString(String string)
                {
                    return string.equals("ALL")
                        ? null
                        : TickerType.valueOf(
                              string); // Return null if "All" is selected
                }
            });
    }

    /**
     * Configure the listeners
     */
    private void ConfigureListeners()
    {
        // Add a listener to the search field to update the table view
        stocksFundsTabTickerSearchField.textProperty().addListener(
            (observable, oldValue, newValue) -> UpdateTransactionTableView());

        // Add a listener to the ticker type combo box to update the table view
        stocksFundsTabTickerTypeComboBox.valueProperty().addListener(
            (observable, oldValue, newValue) -> UpdateTransactionTableView());
    }

    /**
     * Configure the table view columns
     */
    private void ConfigureTableView()
    {
        TableColumn<Ticker, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(
            param -> new SimpleObjectProperty<>(param.getValue().GetId()));

        // Align the ID column to the center
        idColumn.setCellFactory(column -> {
            return new TableCell<Ticker, Long>() {
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

        TableColumn<Ticker, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().GetName()));

        TableColumn<Ticker, String> SymbolColumn = new TableColumn<>("Symbol");
        SymbolColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().GetSymbol()));

        TableColumn<Ticker, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().GetType().toString()));

        TableColumn<Ticker, BigDecimal> quantityColumn =
            new TableColumn<>("Quantity Owned");
        quantityColumn.setCellValueFactory(
            param -> new SimpleObjectProperty<>(param.getValue().GetCurrentQuantity()));

        TableColumn<Ticker, String> unitColumn = new TableColumn<>("Unit Price");
        unitColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(
                UIUtils.FormatCurrency(param.getValue().GetCurrentUnitValue())));

        TableColumn<Ticker, String> totalColumn = new TableColumn<>("Total Value");
        totalColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(
                UIUtils.FormatCurrency(param.getValue().GetCurrentQuantity().multiply(
                    param.getValue().GetCurrentUnitValue()))));

        TableColumn<Ticker, String> avgUnitColumn =
            new TableColumn<>("Average Unit Price");
        avgUnitColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(
                UIUtils.FormatCurrency(param.getValue().GetAverageUnitValue())));

        // Add the columns to the table view
        stocksFundsTabTickerTable.getColumns().add(idColumn);
        stocksFundsTabTickerTable.getColumns().add(nameColumn);
        stocksFundsTabTickerTable.getColumns().add(SymbolColumn);
        stocksFundsTabTickerTable.getColumns().add(typeColumn);
        stocksFundsTabTickerTable.getColumns().add(quantityColumn);
        stocksFundsTabTickerTable.getColumns().add(unitColumn);
        stocksFundsTabTickerTable.getColumns().add(totalColumn);
        stocksFundsTabTickerTable.getColumns().add(avgUnitColumn);
    }
}
