/*
 * Filename: ArchivedTickersController.java
 * Created on: January 10, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import jakarta.persistence.EntityNotFoundException;
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
import lombok.NoArgsConstructor;
import org.moinex.entities.investment.Ticker;
import org.moinex.services.TickerService;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Archived Tickers dialog
 */
@Controller
@NoArgsConstructor
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
    @Autowired
    public ArchivedTickersController(TickerService tickerService)

    {
        this.tickerService = tickerService;
    }

    @FXML
    public void initialize()
    {
        loadArchivedTickersFromDatabase();

        configureTableView();

        updateTickerTableView();

        // Add listener to the search field
        searchField.textProperty().addListener(
            (observable, oldValue, newValue) -> updateTickerTableView());
    }

    @FXML
    private void handleUnarchive()
    {
        Ticker selectedTicker = tickerTableView.getSelectionModel().getSelectedItem();

        if (selectedTicker == null)
        {
            WindowUtils.showInformationDialog("No ticker selected",
                                              "Please select a ticker to unarchive");
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                "Unarchive ticker " + selectedTicker.getName() + " ( " +
                    selectedTicker.getSymbol() + ")",
                "Are you sure you want to unarchive this ticker?"))
        {
            try
            {
                tickerService.unarchiveTicker(selectedTicker.getId());

                WindowUtils.showSuccessDialog("Ticker unarchived",
                                              "Ticker " + selectedTicker.getName() +
                                                  " ( " + selectedTicker.getSymbol() +
                                                  ") has been unarchived");

                // Remove this ticker from the list and update the table view
                archivedTickers.remove(selectedTicker);
                updateTickerTableView();
            }
            catch (EntityNotFoundException e)
            {
                WindowUtils.showErrorDialog("Error unarchiving ticker", e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete()
    {
        Ticker selectedTicker = tickerTableView.getSelectionModel().getSelectedItem();

        if (selectedTicker == null)
        {
            WindowUtils.showErrorDialog("No ticker selected",
                                        "Please select a ticker to delete");
            return;
        }

        // Prevent the removal of a ticker with associated transactions
        if (tickerService.getTransactionCountByTicker(selectedTicker.getId()) > 0)
        {
            WindowUtils.showInformationDialog(
                "Ticker has transactions",
                "Cannot delete a ticker with associated transactions. You can "
                    + "archive it instead");
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                "Remove ticker " + selectedTicker.getName() + " ( " +
                    selectedTicker.getSymbol() + ")",
                "Are you sure you want to remove this ticker?"))
        {
            try
            {
                tickerService.deleteTicker(selectedTicker.getId());

                WindowUtils.showSuccessDialog("Ticker deleted",
                                              "Ticker " + selectedTicker.getName() +
                                                  " (" + selectedTicker.getSymbol() +
                                                  ") has been deleted");

                // Remove this ticker from the list and update the table view
                archivedTickers.remove(selectedTicker);
                updateTickerTableView();
            }
            catch (EntityNotFoundException | IllegalStateException e)
            {
                WindowUtils.showErrorDialog("Error removing ticker", e.getMessage());
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
    private void loadArchivedTickersFromDatabase()
    {
        archivedTickers = tickerService.getAllArchivedTickers();
    }

    /**
     * Updates the archived tickers table view
     */
    private void updateTickerTableView()
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
                    String name       = t.getName().toLowerCase();
                    String symbol     = t.getSymbol().toLowerCase();
                    String type       = t.getType().toString().toLowerCase();
                    String quantity   = t.getCurrentQuantity().toString();
                    String unitValue  = t.getCurrentUnitValue().toString();
                    String totalValue = t.getCurrentQuantity()
                                            .multiply(t.getCurrentUnitValue())
                                            .toString();
                    String avgPrice = t.getAverageUnitValue().toString();

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
    private void configureTableView()
    {
        TableColumn<Ticker, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(
            param -> new SimpleObjectProperty<>(param.getValue().getId()));

        // Align the ID column to the center
        idColumn.setCellFactory(column -> new TableCell<Ticker, Long>() {
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
        });

        TableColumn<Ticker, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getName()));

        TableColumn<Ticker, String> symbolColumn = new TableColumn<>("Symbol");
        symbolColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getSymbol()));

        TableColumn<Ticker, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getType().toString()));

        TableColumn<Ticker, BigDecimal> quantityColumn =
            new TableColumn<>("Quantity Owned");
        quantityColumn.setCellValueFactory(
            param -> new SimpleObjectProperty<>(param.getValue().getCurrentQuantity()));

        TableColumn<Ticker, String> unitColumn = new TableColumn<>("Unit Price");
        unitColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(
                UIUtils.formatCurrencyDynamic(param.getValue().getCurrentUnitValue())));

        TableColumn<Ticker, String> totalColumn = new TableColumn<>("Total Value");
        totalColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(UIUtils.formatCurrencyDynamic(
                param.getValue().getCurrentQuantity().multiply(
                    param.getValue().getCurrentUnitValue()))));

        TableColumn<Ticker, String> avgUnitColumn =
            new TableColumn<>("Average Unit Price");
        avgUnitColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(
                UIUtils.formatCurrencyDynamic(param.getValue().getAverageUnitValue())));

        // Add the columns to the table view
        tickerTableView.getColumns().add(idColumn);
        tickerTableView.getColumns().add(nameColumn);
        tickerTableView.getColumns().add(symbolColumn);
        tickerTableView.getColumns().add(typeColumn);
        tickerTableView.getColumns().add(quantityColumn);
        tickerTableView.getColumns().add(unitColumn);
        tickerTableView.getColumns().add(totalColumn);
        tickerTableView.getColumns().add(avgUnitColumn);
    }
}
