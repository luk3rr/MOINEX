/*
 * Filename: ArchivedTickersController.java
 * Created on: January 10, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.math.BigDecimal;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.moinex.entities.investment.Ticker;
import org.moinex.services.TickerService;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Archived Tickers dialog
 */
@Controller
public class ArchivedTickersController
{
    @FXML
    private TableView<Ticker> tickerTableView;

    @FXML
    private TextField searchField;

    private List<Ticker> archivedTickers;

    private TickerService tickerService;

    /**
     * Constructor
     * @param TickerService tickerService
     * @note This constructor is used for dependency injection
     */
    public ArchivedTickersController(TickerService tickerService)

    {
        this.tickerService = tickerService;
    }

    @FXML
    public void initialize()
    {
        LoadArchivedTickersFromDatabase();

        ConfigureTableView();

        UpdateTickerTableView();

        // Add listener to the search field
        searchField.textProperty().addListener(
            (observable, oldValue, newValue) -> UpdateTickerTableView());
    }

    @FXML
    private void handleUnarchive()
    {
        Ticker selectedTicker = tickerTableView.getSelectionModel().getSelectedItem();

        if (selectedTicker == null)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "No ticker selected",
                                        "Please select a ticker to unarchive");
            return;
        }

        if (WindowUtils.ShowConfirmationDialog(
                "Confirmation",
                "Unarchive ticker " + selectedTicker.GetName() + " ( " +
                    selectedTicker.GetSymbol() + ")",
                "Are you sure you want to unarchive this ticker?"))
        {
            try
            {
                tickerService.UnarchiveTicker(selectedTicker.GetId());

                WindowUtils.ShowSuccessDialog("Success",
                                              "Ticker unarchived",
                                              "Ticker " + selectedTicker.GetName() +
                                                  " ( " + selectedTicker.GetSymbol() +
                                                  ") has been unarchived");

                // Remove this ticker from the list and update the table view
                archivedTickers.remove(selectedTicker);
                UpdateTickerTableView();
            }
            catch (RuntimeException e)
            {
                WindowUtils.ShowErrorDialog("Error",
                                            "Error unarchiving ticker",
                                            e.getMessage());
                return;
            }
        }
    }

    @FXML
    private void handleDelete()
    {
        Ticker selectedTicker = tickerTableView.getSelectionModel().getSelectedItem();

        if (selectedTicker == null)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "No ticker selected",
                                        "Please select a ticker to delete");
            return;
        }

        // Prevent the removal of a ticker with associated transactions
        if (tickerService.GetTransactionCountByTicker(selectedTicker.GetId()) > 0)
        {
            WindowUtils.ShowErrorDialog(
                "Error",
                "Ticker has transactions",
                "Cannot delete a ticker with associated transactions");
            return;
        }

        if (WindowUtils.ShowConfirmationDialog(
                "Confirmation",
                "Remove ticker " + selectedTicker.GetName() + " ( " +
                    selectedTicker.GetSymbol() + ")",
                "Are you sure you want to remove this ticker?"))
        {
            try
            {
                tickerService.DeleteTicker(selectedTicker.GetId());

                WindowUtils.ShowSuccessDialog("Success",
                                              "Ticker deleted",
                                              "Ticker " + selectedTicker.GetName() +
                                                  " (" + selectedTicker.GetSymbol() +
                                                  ") has been deleted");

                // Remove this ticker from the list and update the table view
                archivedTickers.remove(selectedTicker);
                UpdateTickerTableView();
            }
            catch (RuntimeException e)
            {
                WindowUtils.ShowErrorDialog("Error",
                                            "Error removing ticker",
                                            e.getMessage());
                return;
            }
        }
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)searchField.getScene().getWindow();
        stage.close();
    }

    /**
     * Loads the archived tickers from the database
     */
    private void LoadArchivedTickersFromDatabase()
    {
        archivedTickers = tickerService.GetAllArchivedTickers();
    }

    /**
     * Updates the archived tickers table view
     */
    private void UpdateTickerTableView()
    {
        String similarTextOrId = searchField.getText().toLowerCase();

        tickerTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty())
        {
            tickerTableView.getItems().setAll(archivedTickers);
        }
        else
        {
            archivedTickers.stream()
                .filter(t -> {
                    String name       = t.GetName().toLowerCase();
                    String symbol     = t.GetSymbol().toLowerCase();
                    String type       = t.GetType().toString().toLowerCase();
                    String quantity   = t.GetCurrentQuantity().toString();
                    String unitValue  = t.GetCurrentUnitValue().toString();
                    String totalValue = t.GetCurrentQuantity()
                                            .multiply(t.GetCurrentUnitValue())
                                            .toString();
                    String avgPrice = t.GetAveragePrice().toString();

                    return name.contains(similarTextOrId) ||
                        symbol.contains(similarTextOrId) ||
                        type.contains(similarTextOrId) ||
                        quantity.contains(similarTextOrId) ||
                        unitValue.contains(similarTextOrId) ||
                        totalValue.contains(similarTextOrId) ||
                        avgPrice.contains(similarTextOrId);
                })
                .forEach(tickerTableView.getItems()::add);
        }

        tickerTableView.refresh();
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
                UIUtils.FormatCurrencyDynamic(param.getValue().GetCurrentUnitValue())));

        TableColumn<Ticker, String> totalColumn = new TableColumn<>("Total Value");
        totalColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(UIUtils.FormatCurrencyDynamic(
                param.getValue().GetCurrentQuantity().multiply(
                    param.getValue().GetCurrentUnitValue()))));

        TableColumn<Ticker, String> avgUnitColumn =
            new TableColumn<>("Average Unit Price");
        avgUnitColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(
                UIUtils.FormatCurrencyDynamic(param.getValue().GetAveragePrice())));

        // Add the columns to the table view
        tickerTableView.getColumns().add(idColumn);
        tickerTableView.getColumns().add(nameColumn);
        tickerTableView.getColumns().add(SymbolColumn);
        tickerTableView.getColumns().add(typeColumn);
        tickerTableView.getColumns().add(quantityColumn);
        tickerTableView.getColumns().add(unitColumn);
        tickerTableView.getColumns().add(totalColumn);
        tickerTableView.getColumns().add(avgUnitColumn);
    }
}
