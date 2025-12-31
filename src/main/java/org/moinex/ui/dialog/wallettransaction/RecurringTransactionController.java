/*
 * Filename: RecurringTransactionController.java
 * Created on: November 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityNotFoundException;
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
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.NoArgsConstructor;
import org.moinex.model.wallettransaction.RecurringTransaction;
import org.moinex.service.I18nService;
import org.moinex.service.RecurringTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.RecurringTransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Manage Recurring Transactions dialog
 */
@Controller
@NoArgsConstructor
public class RecurringTransactionController {
    @FXML private TableView<RecurringTransaction> recurringTransactionTableView;

    @FXML private ComboBox<RecurringTransactionStatus> statusComboBox;

    @FXML private TextField searchField;

    private ConfigurableApplicationContext springContext;

    private List<RecurringTransaction> recurringTransactions;

    private RecurringTransactionService recurringTransactionService;

    private I18nService i18nService;

    /**
     * Constructor for the RecurringTransactionController
     * @param recurringTransactionService The recurring transaction service
     */
    @Autowired
    public RecurringTransactionController(
            RecurringTransactionService recurringTransactionService,
            ConfigurableApplicationContext springContext,
            I18nService i18nService) {
        this.recurringTransactionService = recurringTransactionService;
        this.springContext = springContext;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        loadRecurringTransactionsFromDatabase();

        configureTableView();

        populateRecurringTransactionStatusComboBox();

        // Set the default value for the status combo box
        statusComboBox.setValue(RecurringTransactionStatus.ACTIVE);

        updateRecurringTransactionTableView();

        statusComboBox.setOnAction(event -> updateRecurringTransactionTableView());

        // Add listener to the search field
        searchField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> updateRecurringTransactionTableView());
    }

    @FXML
    private void handleCreate() {
        WindowUtils.openModalWindow(
                Constants.ADD_RECURRING_TRANSACTION_FXML,
                i18nService.tr(
                        Constants.TranslationKeys
                                .WALLETTRANSACTION_DIALOG_CREATE_RECURRING_TRANSACTION_TITLE),
                springContext,
                (AddRecurringTransactionController controller) -> {},
                List.of(
                        () -> {
                            loadRecurringTransactionsFromDatabase();
                            updateRecurringTransactionTableView();
                        }));
    }

    @FXML
    private void handleEdit() {
        RecurringTransaction selectedRt =
                recurringTransactionTableView.getSelectionModel().getSelectedItem();

        if (selectedRt == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_NO_RECURRING_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_NO_RECURRING_SELECTED_EDIT_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_RECURRING_TRANSACTION_FXML,
                i18nService.tr(
                        Constants.TranslationKeys
                                .WALLETTRANSACTION_DIALOG_EDIT_RECURRING_TRANSACTION_TITLE),
                springContext,
                (EditRecurringTransactionController controller) ->
                        controller.setRecurringTransaction(selectedRt),
                List.of(
                        () -> {
                            loadRecurringTransactionsFromDatabase();
                            updateRecurringTransactionTableView();
                        }));
    }

    @FXML
    private void handleDelete() {
        RecurringTransaction selectedRt =
                recurringTransactionTableView.getSelectionModel().getSelectedItem();

        if (selectedRt == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_NO_RECURRING_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_NO_RECURRING_SELECTED_DELETE_MESSAGE));
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_DIALOG_REMOVE_RECURRING_TRANSACTION_TITLE)
                        + " "
                        + selectedRt.getId(),
                i18nService.tr(
                        Constants.TranslationKeys
                                .WALLETTRANSACTION_DIALOG_REMOVE_RECURRING_TRANSACTION_MESSAGE),
                i18nService.getBundle())) {
            recurringTransactionService.deleteRecurringTransaction(selectedRt.getId());
            loadRecurringTransactionsFromDatabase();
            updateRecurringTransactionTableView();
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.close();
    }

    /**
     * Loads the categories from the database
     */
    private void loadRecurringTransactionsFromDatabase() {
        recurringTransactions = recurringTransactionService.getAllRecurringTransactions();
    }

    /**
     * Updates the category table view
     */
    private void updateRecurringTransactionTableView() {
        String similarTextOrId = searchField.getText().toLowerCase();

        // Get selected transaction status from combo box
        RecurringTransactionStatus selectedStatus = statusComboBox.getValue();

        recurringTransactionTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty()) {
            recurringTransactions.stream()
                    .filter(rt -> selectedStatus == null || rt.getStatus().equals(selectedStatus))
                    .forEach(recurringTransactionTableView.getItems()::add);
        } else {
            recurringTransactions.stream()
                    .filter(rt -> selectedStatus == null || rt.getStatus().equals(selectedStatus))
                    .filter(
                            rt -> {
                                String description = rt.getDescription().toLowerCase();
                                String id = rt.getId().toString();
                                String category = rt.getCategory().getName().toLowerCase();
                                String wallet = rt.getWallet().getName().toLowerCase();
                                String type = rt.getType().name().toLowerCase();
                                String frequency = rt.getFrequency().name().toLowerCase();
                                String amount = UIUtils.formatCurrency(rt.getAmount());

                                return description.contains(similarTextOrId)
                                        || id.contains(similarTextOrId)
                                        || category.contains(similarTextOrId)
                                        || wallet.contains(similarTextOrId)
                                        || type.contains(similarTextOrId)
                                        || frequency.contains(similarTextOrId)
                                        || amount.contains(similarTextOrId);
                            })
                    .forEach(recurringTransactionTableView.getItems()::add);
        }

        recurringTransactionTableView.refresh();
    }

    /**
     * Configures the table view columns
     */
    private void configureTableView() {
        TableColumn<RecurringTransaction, Integer> idColumn =
                getRecurringTransactionLongTableColumn();

        TableColumn<RecurringTransaction, String> descriptionColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.WALLETTRANSACTION_TABLE_DESCRIPTION));
        descriptionColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getDescription()));

        TableColumn<RecurringTransaction, String> amountColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_AMOUNT));
        amountColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(param.getValue().getAmount())));

        TableColumn<RecurringTransaction, String> walletColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_WALLET));
        walletColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getWallet().getName()));

        TableColumn<RecurringTransaction, String> typeColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_TYPE));
        typeColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateTransactionType(
                                        param.getValue().getType(), i18nService)));

        TableColumn<RecurringTransaction, String> categoryColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_CATEGORY));
        categoryColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getCategory().getName()));

        TableColumn<RecurringTransaction, String> statusColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_STATUS));
        statusColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateRecurringTransactionStatus(
                                        param.getValue().getStatus(), i18nService)));

        TableColumn<RecurringTransaction, String> frequencyColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.WALLETTRANSACTION_TABLE_FREQUENCY));
        frequencyColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateRecurringTransactionFrequency(
                                        param.getValue().getFrequency(), i18nService)));

        TableColumn<RecurringTransaction, String> startDateColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.WALLETTRANSACTION_TABLE_START_DATE));
        startDateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue()
                                        .getStartDate()
                                        .format(Constants.DATE_FORMATTER_NO_TIME)));

        TableColumn<RecurringTransaction, String> endDateColumn =
                getRecurringTransactionStringTableColumn();

        TableColumn<RecurringTransaction, String> nextDueDateColumn =
                getTransactionStringTableColumn();

        TableColumn<RecurringTransaction, String> expectedRemainingAmountColumn =
                getStringTableColumn();

        recurringTransactionTableView.getColumns().add(idColumn);
        recurringTransactionTableView.getColumns().add(descriptionColumn);
        recurringTransactionTableView.getColumns().add(amountColumn);
        recurringTransactionTableView.getColumns().add(walletColumn);
        recurringTransactionTableView.getColumns().add(typeColumn);
        recurringTransactionTableView.getColumns().add(categoryColumn);
        recurringTransactionTableView.getColumns().add(statusColumn);
        recurringTransactionTableView.getColumns().add(frequencyColumn);
        recurringTransactionTableView.getColumns().add(startDateColumn);
        recurringTransactionTableView.getColumns().add(endDateColumn);
        recurringTransactionTableView.getColumns().add(nextDueDateColumn);
        recurringTransactionTableView.getColumns().add(expectedRemainingAmountColumn);
    }

    private TableColumn<RecurringTransaction, String> getStringTableColumn() {
        TableColumn<RecurringTransaction, String> expectedRemainingAmountColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_TABLE_EXPECTED_REMAINING_AMOUNT));
        expectedRemainingAmountColumn.setCellValueFactory(
                param -> {
                    RecurringTransaction rt = param.getValue();

                    Double expectedRemainingAmount;

                    try {
                        expectedRemainingAmount =
                                recurringTransactionService.calculateExpectedRemainingAmount(
                                        rt.getId());
                    } catch (EntityNotFoundException e) {
                        expectedRemainingAmount = 0.0;
                    }

                    return new SimpleStringProperty(
                            UIUtils.formatCurrency(expectedRemainingAmount));
                });
        return expectedRemainingAmountColumn;
    }

    private TableColumn<RecurringTransaction, String> getTransactionStringTableColumn() {
        TableColumn<RecurringTransaction, String> nextDueDateColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.WALLETTRANSACTION_TABLE_NEXT_DUE_DATE));
        nextDueDateColumn.setCellValueFactory(
                param -> {
                    if (param.getValue().getStatus().equals(RecurringTransactionStatus.INACTIVE)) {
                        return new SimpleStringProperty("-");
                    } else {
                        return new SimpleStringProperty(
                                param.getValue()
                                        .getNextDueDate()
                                        .format(Constants.DATE_FORMATTER_NO_TIME));
                    }
                });

        nextDueDateColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item == null || empty) {
                                    setText(null);
                                } else {
                                    setText(item);
                                    setAlignment(Pos.CENTER);
                                    setStyle("-fx-padding: 0;");
                                }
                            }
                        });
        return nextDueDateColumn;
    }

    private TableColumn<RecurringTransaction, String> getRecurringTransactionStringTableColumn() {
        TableColumn<RecurringTransaction, String> endDateColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_END_DATE));
        endDateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue()
                                        .getEndDate()
                                        .format(Constants.DATE_FORMATTER_NO_TIME)));

        // If the end date is the default date, show "Indefinite"
        endDateColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item == null || empty) {
                                    setText(null);
                                } else {
                                    if (item.equals(
                                            Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE.format(
                                                    Constants.DATE_FORMATTER_NO_TIME))) {
                                        setText(
                                                i18nService.tr(
                                                        Constants.TranslationKeys
                                                                .WALLETTRANSACTION_TABLE_INDEFINITE));
                                    } else {
                                        setText(item);
                                    }
                                }
                            }
                        });
        return endDateColumn;
    }

    private TableColumn<RecurringTransaction, Integer> getRecurringTransactionLongTableColumn() {
        TableColumn<RecurringTransaction, Integer> idColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_ID));
        idColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getId()));

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
                                    setStyle("-fx-padding: 0;");
                                }
                            }
                        });
        return idColumn;
    }

    /**
     * Populate the transaction type combo box with the available transaction types
     */
    private void populateRecurringTransactionStatusComboBox() {
        // Make a copy of the list to add the 'All' option
        // Add 'All' option to the transaction type combo box
        // All is the first element in the list and is represented by a null value
        ObservableList<RecurringTransactionStatus> transactionTypesWithNull =
                FXCollections.observableArrayList(RecurringTransactionStatus.values());
        transactionTypesWithNull.addFirst(null);

        statusComboBox.setItems(transactionTypesWithNull);

        statusComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(RecurringTransactionStatus transactionType) {
                        return transactionType != null
                                ? UIUtils.translateRecurringTransactionStatus(
                                        transactionType, i18nService)
                                : i18nService.tr(
                                        Constants.TranslationKeys
                                                .WALLETTRANSACTION_COMBOBOX_ALL); // Show "All"
                        // instead of null
                    }

                    @Override
                    public RecurringTransactionStatus fromString(String string) {
                        if (string.equals(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .WALLETTRANSACTION_COMBOBOX_ALL))) {
                            return null;
                        }

                        // Try to match the translated string back to the enum
                        for (RecurringTransactionStatus status :
                                RecurringTransactionStatus.values()) {
                            if (UIUtils.translateRecurringTransactionStatus(status, i18nService)
                                    .equals(string)) {
                                return status;
                            }
                        }

                        return null;
                    }
                });
    }
}
