/*
 * Filename: RecurringTransactionController.java
 * Created on: November 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

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
import org.moinex.entities.RecurringTransaction;
import org.moinex.services.RecurringTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.RecurringTransactionStatus;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Manage Recurring Transactions dialog
 */
@Controller
@NoArgsConstructor
public class RecurringTransactionController
{
    @FXML
    private TableView<RecurringTransaction> recurringTransactionTableView;

    @FXML
    private ComboBox<RecurringTransactionStatus> statusComboBox;

    @FXML
    private TextField searchField;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private List<RecurringTransaction> recurringTransactions;

    private RecurringTransactionService recurringTransactionService;

    /**
     * Constructor for the RecurringTransactionController
     * @param recurringTransactionService The recurring transaction service
     */
    @Autowired
    public RecurringTransactionController(
        RecurringTransactionService recurringTransactionService)
    {
        this.recurringTransactionService = recurringTransactionService;
    }

    @FXML
    public void initialize()
    {
        loadRecurringTransactionsFromdatabase();

        configureTableView();

        populateRecurringTransactionStatusComboBox();

        // Set default value for the status combo box (null = All)
        statusComboBox.setValue(null);

        updateRecurringTransactionTableView();

        statusComboBox.setOnAction(event -> updateRecurringTransactionTableView());

        // Add listener to the search field
        searchField.textProperty().addListener(
            (observable, oldValue, newValue) -> updateRecurringTransactionTableView());
    }

    @FXML
    private void handleCreate()
    {
        WindowUtils.openModalWindow(Constants.ADD_RECURRING_TRANSACTION_FXML,
                                    "Create Recurring Transaction",
                                    springContext,
                                    (AddRecurringTransactionController controller)
                                        -> {},
                                    List.of(() -> {
                                        loadRecurringTransactionsFromdatabase();
                                        updateRecurringTransactionTableView();
                                    }));
    }

    @FXML
    private void handleEdit()
    {
        RecurringTransaction selectedRt =
            recurringTransactionTableView.getSelectionModel().getSelectedItem();

        if (selectedRt == null)
        {
            WindowUtils.showInformationDialog(
                "No recurring transaction selected",
                "Please select a recurring transaction to edit");
            return;
        }

        WindowUtils.openModalWindow(
            Constants.EDIT_RECURRING_TRANSACTION_FXML,
            "Edit Recurring Transaction",
            springContext,
            (EditRecurringTransactionController controller)
                -> controller.setRecurringTransaction(selectedRt),
            List.of(() -> {
                loadRecurringTransactionsFromdatabase();
                updateRecurringTransactionTableView();
            }));
    }

    @FXML
    private void handleDelete()
    {
        RecurringTransaction selectedRt =
            recurringTransactionTableView.getSelectionModel().getSelectedItem();

        if (selectedRt == null)
        {
            WindowUtils.showInformationDialog(
                "No recurring transaction selected",
                "Please select a recurring transaction to delete");
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                "Remove recurring transaction with ID " + selectedRt.getId(),
                "Are you sure you want to delete this recurring transaction?"))
        {
            recurringTransactionService.deleteRecurringTransaction(selectedRt.getId());
            loadRecurringTransactionsFromdatabase();
            updateRecurringTransactionTableView();
        }
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)searchField.getScene().getWindow();
        stage.close();
    }

    /**
     * Loads the categories from the database
     */
    private void loadRecurringTransactionsFromdatabase()
    {
        recurringTransactions =
            recurringTransactionService.getAllRecurringTransactions();
    }

    /**
     * Updates the category table view
     */
    private void updateRecurringTransactionTableView()
    {
        String similarTextOrId = searchField.getText().toLowerCase();

        // Get selected transaction status from combo box
        RecurringTransactionStatus selectedStatus = statusComboBox.getValue();

        recurringTransactionTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty())
        {
            recurringTransactions.stream()
                .filter(rt
                        -> selectedStatus == null ||
                               rt.getStatus().equals(selectedStatus))
                .forEach(recurringTransactionTableView.getItems()::add);
        }
        else
        {
            recurringTransactions.stream()
                .filter(rt
                        -> selectedStatus == null ||
                               rt.getStatus().equals(selectedStatus))
                .filter(rt -> {
                    String description = rt.getDescription().toLowerCase();
                    String id          = rt.getId().toString();
                    String category    = rt.getCategory().getName().toLowerCase();
                    String wallet      = rt.getWallet().getName().toLowerCase();
                    String type        = rt.getType().name().toLowerCase();
                    String frequency   = rt.getFrequency().name().toLowerCase();
                    String amount      = UIUtils.formatCurrency(rt.getAmount());

                    return description.contains(similarTextOrId) ||
                        id.contains(similarTextOrId) ||
                        category.contains(similarTextOrId) ||
                        wallet.contains(similarTextOrId) ||
                        type.contains(similarTextOrId) ||
                        frequency.contains(similarTextOrId) ||
                        amount.contains(similarTextOrId);
                })
                .forEach(recurringTransactionTableView.getItems()::add);
        }

        recurringTransactionTableView.refresh();
    }

    /**
     * Configures the table view columns
     */
    private void configureTableView()
    {
        TableColumn<RecurringTransaction, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(
            param -> new SimpleObjectProperty<>(param.getValue().getId()));

        idColumn.setCellFactory(column -> new TableCell<RecurringTransaction, Long>() {
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
                    setStyle("-fx-padding: 0;");
                }
            }
        });

        TableColumn<RecurringTransaction, String> descriptionColumn =
            new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getDescription()));

        TableColumn<RecurringTransaction, String> amountColumn =
            new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(
                UIUtils.formatCurrency(param.getValue().getAmount())));

        TableColumn<RecurringTransaction, String> walletColumn =
            new TableColumn<>("Wallet");
        walletColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getWallet().getName()));

        TableColumn<RecurringTransaction, String> typeColumn =
            new TableColumn<>("Type");
        typeColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getType().name()));

        TableColumn<RecurringTransaction, String> categoryColumn =
            new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().getCategory().getName()));

        TableColumn<RecurringTransaction, String> statusColumn =
            new TableColumn<>("Status");
        statusColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getStatus().name()));

        TableColumn<RecurringTransaction, String> frequencyColumn =
            new TableColumn<>("Frequency");
        frequencyColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getFrequency().name()));

        TableColumn<RecurringTransaction, String> startDateColumn =
            new TableColumn<>("Start Date");
        startDateColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().getStartDate().format(
                Constants.DATE_FORMATTER_NO_TIME)));

        TableColumn<RecurringTransaction, String> endDateColumn =
            new TableColumn<>("End Date");
        endDateColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().getEndDate().format(
                Constants.DATE_FORMATTER_NO_TIME)));

        // If the end date is the default date, show "Indefinite"
        endDateColumn.setCellFactory(
            column -> new TableCell<RecurringTransaction, String>() {
                @Override
                protected void updateItem(String item, boolean empty)
                {
                    super.updateItem(item, empty);
                    if (item == null || empty)
                    {
                        setText(null);
                    }
                    else
                    {
                        if (item.equals(
                                Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE.format(
                                    Constants.DATE_FORMATTER_NO_TIME)))
                        {
                            setText("Indefinite");
                        }
                        else
                        {
                            setText(item);
                        }
                    }
                }
            });

        TableColumn<RecurringTransaction, String> nextDueDateColumn =
            new TableColumn<>("Next Due Date");
        nextDueDateColumn.setCellValueFactory(param -> {
            if (param.getValue().getStatus().equals(
                    RecurringTransactionStatus.INACTIVE))
            {
                return new SimpleStringProperty("-");
            }
            else
            {
                return new SimpleStringProperty(
                    param.getValue().getNextDueDate().format(
                        Constants.DATE_FORMATTER_NO_TIME));
            }
        });

        nextDueDateColumn.setCellFactory(
            column -> new TableCell<RecurringTransaction, String>() {
                @Override
                protected void updateItem(String item, boolean empty)
                {
                    super.updateItem(item, empty);
                    if (item == null || empty)
                    {
                        setText(null);
                    }
                    else
                    {
                        setText(item);
                        setAlignment(Pos.CENTER);
                        setStyle("-fx-padding: 0;");
                    }
                }
            });

        TableColumn<RecurringTransaction, String> expectedRemainingAmountColumn =
            new TableColumn<>("Expected Remaining Amount");
        expectedRemainingAmountColumn.setCellValueFactory(param -> {
            RecurringTransaction rt = param.getValue();

            Double expectedRemainingAmount;

            try
            {
                expectedRemainingAmount =
                    recurringTransactionService.calculateExpectedRemainingAmount(
                        rt.getId());
            }
            catch (EntityNotFoundException e)
            {
                expectedRemainingAmount = 0.0;
            }

            return new SimpleStringProperty(
                UIUtils.formatCurrency(expectedRemainingAmount));
        });

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

    /**
     * Populate the transaction type combo box with the available transaction types
     */
    private void populateRecurringTransactionStatusComboBox()
    {
        // Make a copy of the list to add the 'All' option
        // Add 'All' option to the transaction type combo box
        // All is the first element in the list and is represented by a null value
        ObservableList<RecurringTransactionStatus> transactionTypesWithNull =
            FXCollections.observableArrayList(RecurringTransactionStatus.values());
        transactionTypesWithNull.add(0, null);

        statusComboBox.setItems(transactionTypesWithNull);

        statusComboBox.setConverter(new StringConverter<RecurringTransactionStatus>() {
            @Override
            public String toString(RecurringTransactionStatus transactionType)
            {
                return transactionType != null ? transactionType.toString()
                                               : "ALL"; // Show "All" instead of null
            }

            @Override
            public RecurringTransactionStatus fromString(String string)
            {
                return string.equals("ALL")
                    ? null
                    : RecurringTransactionStatus.valueOf(
                          string); // Return null if "All" is selected
            }
        });
    }
}
