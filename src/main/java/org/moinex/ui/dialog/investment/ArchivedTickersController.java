/*
 * Filename: ArchivedTickersController.java
 * Created on: January 10, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.investment.Ticker;
import org.moinex.service.PreferencesService;
import org.moinex.service.TickerService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/** Controller for the Archived Tickers dialog */
@Controller
@NoArgsConstructor
public class ArchivedTickersController {
    @FXML private TableView<Ticker> tickerTableView;

    @FXML private TextField searchField;

    private List<Ticker> archivedTickers;

    private TickerService tickerService;
    private PreferencesService preferencesService;

    /**
     * Constructor
     *
     * @param tickerService tickerService
     * @param preferencesService I18n service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public ArchivedTickersController(
            TickerService tickerService, PreferencesService preferencesService) {
        this.tickerService = tickerService;
        this.preferencesService = preferencesService;
    }

    @FXML
    public void initialize() {
        loadArchivedTickersFromDatabase();

        configureTableView();

        updateTickerTableView();

        // Add listener to the search field
        searchField
                .textProperty()
                .addListener((observable, oldValue, newValue) -> updateTickerTableView());
    }

    @FXML
    private void handleUnarchive() {
        Ticker selectedTicker = tickerTableView.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_NO_TICKER_SELECTED_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_NO_TICKER_SELECTED_UNARCHIVE));
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                        preferencesService.translate(
                                Constants.TranslationKeys
                                        .INVESTMENT_DIALOG_CONFIRM_UNARCHIVE_TICKER_TITLE),
                        selectedTicker.getName() + " (" + selectedTicker.getSymbol() + ")"),
                preferencesService.translate(
                        Constants.TranslationKeys
                                .INVESTMENT_DIALOG_CONFIRM_UNARCHIVE_TICKER_MESSAGE),
                preferencesService.getBundle())) {
            try {
                tickerService.unarchiveTicker(selectedTicker.getId());

                WindowUtils.showSuccessDialog(
                        preferencesService.translate(
                                Constants.TranslationKeys
                                        .INVESTMENT_DIALOG_TICKER_UNARCHIVED_TITLE),
                        MessageFormat.format(
                                preferencesService.translate(
                                        Constants.TranslationKeys
                                                .INVESTMENT_DIALOG_TICKER_UNARCHIVED_MESSAGE),
                                selectedTicker.getName()
                                        + " ("
                                        + selectedTicker.getSymbol()
                                        + ")"));

                // Remove this ticker from the list and update the table view
                archivedTickers.remove(selectedTicker);
                updateTickerTableView();
            } catch (EntityNotFoundException e) {
                WindowUtils.showErrorDialog(
                        preferencesService.translate(
                                Constants.TranslationKeys
                                        .INVESTMENT_DIALOG_ERROR_UNARCHIVING_TICKER_TITLE),
                        e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        Ticker selectedTicker = tickerTableView.getSelectionModel().getSelectedItem();

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_NO_TICKER_SELECTED_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_NO_TICKER_SELECTED_DELETE));
            return;
        }

        // Prevent the removal of a ticker with associated transactions
        if (tickerService.getTransactionCountByTicker(selectedTicker.getId()) > 0) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_TICKER_HAS_TRANSACTIONS_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_TICKER_HAS_TRANSACTIONS_MESSAGE));
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                        preferencesService.translate(
                                Constants.TranslationKeys
                                        .INVESTMENT_DIALOG_CONFIRM_DELETE_TICKER_TITLE),
                        selectedTicker.getName() + " (" + selectedTicker.getSymbol() + ")"),
                preferencesService.translate(
                        Constants.TranslationKeys.INVESTMENT_DIALOG_CONFIRM_DELETE_TICKER_MESSAGE),
                preferencesService.getBundle())) {
            try {
                tickerService.deleteTicker(selectedTicker.getId());

                WindowUtils.showSuccessDialog(
                        preferencesService.translate(
                                Constants.TranslationKeys.INVESTMENT_DIALOG_TICKER_DELETED_TITLE),
                        MessageFormat.format(
                                preferencesService.translate(
                                        Constants.TranslationKeys
                                                .INVESTMENT_DIALOG_TICKER_DELETED_MESSAGE),
                                selectedTicker.getName()
                                        + " ("
                                        + selectedTicker.getSymbol()
                                        + ")"));

                // Remove this ticker from the list and update the table view
                archivedTickers.remove(selectedTicker);
                updateTickerTableView();
            } catch (EntityNotFoundException | IllegalStateException e) {
                WindowUtils.showErrorDialog(
                        preferencesService.translate(
                                Constants.TranslationKeys
                                        .INVESTMENT_DIALOG_ERROR_DELETING_TICKER_TITLE),
                        e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.close();
    }

    /** Loads the archived tickers from the database */
    private void loadArchivedTickersFromDatabase() {
        archivedTickers = tickerService.getAllArchivedTickers();
    }

    /** Updates the archived ticker table view */
    private void updateTickerTableView() {
        String similarTextOrId = searchField.getText().toLowerCase();

        tickerTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty()) {
            tickerTableView.getItems().setAll(archivedTickers);
        } else {
            archivedTickers.stream()
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
                    .forEach(tickerTableView.getItems()::add);
        }

        tickerTableView.refresh();
    }

    /** Configure the table view columns */
    private void configureTableView() {
        TableColumn<Ticker, Integer> idColumn = getTickerLongTableColumn();

        TableColumn<Ticker, ImageView> logoColumn = new TableColumn<>("");
        logoColumn.setCellValueFactory(
                param -> {
                    ImageView logo = UIUtils.loadTickerLogo(param.getValue(), 32.0);
                    return new SimpleObjectProperty<>(logo);
                });
        logoColumn.setPrefWidth(50);
        logoColumn.setMaxWidth(50);
        logoColumn.setMinWidth(50);
        logoColumn.setResizable(false);
        logoColumn.setStyle("-fx-alignment: CENTER;");

        TableColumn<Ticker, String> nameColumn =
                new TableColumn<>(
                        preferencesService.translate(
                                Constants.TranslationKeys.INVESTMENT_TABLE_NAME));
        nameColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getName()));

        TableColumn<Ticker, String> symbolColumn =
                new TableColumn<>(
                        preferencesService.translate(
                                Constants.TranslationKeys.INVESTMENT_TABLE_SYMBOL));
        symbolColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getSymbol()));

        TableColumn<Ticker, String> typeColumn =
                new TableColumn<>(
                        preferencesService.translate(
                                Constants.TranslationKeys.INVESTMENT_TABLE_TYPE));
        typeColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateAssetType(
                                        param.getValue().getType(), preferencesService)));

        TableColumn<Ticker, BigDecimal> quantityColumn =
                new TableColumn<>(
                        preferencesService.translate(
                                Constants.TranslationKeys.INVESTMENT_TABLE_QUANTITY_OWNED));
        quantityColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getCurrentQuantity()));

        TableColumn<Ticker, String> unitColumn =
                new TableColumn<>(
                        preferencesService.translate(
                                Constants.TranslationKeys.INVESTMENT_TABLE_UNIT_PRICE));
        unitColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrencyDynamic(
                                        param.getValue().getCurrentUnitValue())));

        TableColumn<Ticker, String> totalColumn =
                new TableColumn<>(
                        preferencesService.translate(
                                Constants.TranslationKeys.INVESTMENT_TABLE_TOTAL_VALUE));
        totalColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrencyDynamic(
                                        param.getValue()
                                                .getCurrentQuantity()
                                                .multiply(
                                                        param.getValue().getCurrentUnitValue()))));

        TableColumn<Ticker, String> avgUnitColumn =
                new TableColumn<>(
                        preferencesService.translate(
                                Constants.TranslationKeys.INVESTMENT_TABLE_AVERAGE_UNIT_PRICE));
        avgUnitColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrencyDynamic(
                                        param.getValue().getAverageUnitValue())));

        // Add the columns to the table view
        tickerTableView.getColumns().add(idColumn);
        tickerTableView.getColumns().add(logoColumn);
        tickerTableView.getColumns().add(nameColumn);
        tickerTableView.getColumns().add(symbolColumn);
        tickerTableView.getColumns().add(typeColumn);
        tickerTableView.getColumns().add(quantityColumn);
        tickerTableView.getColumns().add(unitColumn);
        tickerTableView.getColumns().add(totalColumn);
        tickerTableView.getColumns().add(avgUnitColumn);
    }

    private TableColumn<Ticker, Integer> getTickerLongTableColumn() {
        TableColumn<Ticker, Integer> idColumn =
                new TableColumn<>(
                        preferencesService.translate(
                                Constants.TranslationKeys.INVESTMENT_TABLE_ID));
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
}
