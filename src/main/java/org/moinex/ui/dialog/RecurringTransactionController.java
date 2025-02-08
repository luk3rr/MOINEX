/*
 * Filename: RecurringTransactionController.java
 * Created on: November 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

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
public class RecurringTransactionController
{
    @FXML
    private TableView<RecurringTransaction> recurringTransactionTableView;

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
    public RecurringTransactionController(
        RecurringTransactionService recurringTransactionService)
    {
        this.recurringTransactionService = recurringTransactionService;
    }

    @FXML
    public void initialize()
    {
        LoadRecurringTransactionFromDatabase();

        ConfigureTableView();

        UpdateRecurringTransactionTableView();

        // Add listener to the search field
        searchField.textProperty().addListener(
            (observable, oldValue, newValue) -> UpdateRecurringTransactionTableView());
    }

    @FXML
    private void handleCreate()
    {
        WindowUtils.OpenModalWindow(Constants.ADD_RECURRING_TRANSACTION_FXML,
                                    "Create Recurring Transaction",
                                    springContext,
                                    (AddRecurringTransactionController controller)
                                        -> {},
                                    List.of(() -> {
                                        LoadRecurringTransactionFromDatabase();
                                        UpdateRecurringTransactionTableView();
                                    }));
    }

    @FXML
    private void handleEdit()
    {
        RecurringTransaction selectedRt =
            recurringTransactionTableView.getSelectionModel().getSelectedItem();

        if (selectedRt == null)
        {
            WindowUtils.ShowErrorDialog(
                "Error",
                "No recurring transaction selected",
                "Please select a recurring transaction to edit");
            return;
        }

        WindowUtils.OpenModalWindow(
            Constants.EDIT_RECURRING_TRANSACTION_FXML,
            "Edit Recurring Transaction",
            springContext,
            (EditRecurringTransactionController controller)
                -> controller.SetRecurringTransaction(selectedRt),
            List.of(() -> {
                LoadRecurringTransactionFromDatabase();
                UpdateRecurringTransactionTableView();
            }));
    }

    @FXML
    private void handleDelete()
    {
        RecurringTransaction selectedRt =
            recurringTransactionTableView.getSelectionModel().getSelectedItem();

        if (selectedRt == null)
        {
            WindowUtils.ShowErrorDialog(
                "Error",
                "No recurring transaction selected",
                "Please select a recurring transaction to delete");
            return;
        }

        if (WindowUtils.ShowConfirmationDialog(
                "Confirmation",
                "Remove recurring transaction with ID " + selectedRt.GetId(),
                "Are you sure you want to delete this recurring transaction?"))
        {
            recurringTransactionService.DeleteRecurringTransaction(selectedRt.GetId());
            LoadRecurringTransactionFromDatabase();
            UpdateRecurringTransactionTableView();
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
    private void LoadRecurringTransactionFromDatabase()
    {
        recurringTransactions =
            recurringTransactionService.GetAllRecurringTransactions();
    }

    /**
     * Updates the category table view
     */
    private void UpdateRecurringTransactionTableView()
    {
        String similarTextOrId = searchField.getText().toLowerCase();

        recurringTransactionTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty())
        {
            recurringTransactionTableView.getItems().setAll(recurringTransactions);
        }
        else
        {
            recurringTransactions.stream()
                .filter(rt -> {
                    String description = rt.GetDescription().toLowerCase();
                    String id          = rt.GetId().toString();
                    String category    = rt.GetCategory().GetName().toLowerCase();
                    String wallet      = rt.GetWallet().GetName().toLowerCase();
                    String type        = rt.GetType().name().toLowerCase();
                    String status      = rt.GetStatus().name().toLowerCase();
                    String frequency   = rt.GetFrequency().name().toLowerCase();
                    String amount      = UIUtils.FormatCurrency(rt.GetAmount());

                    return description.contains(similarTextOrId) ||
                        id.contains(similarTextOrId) ||
                        category.contains(similarTextOrId) ||
                        wallet.contains(similarTextOrId) ||
                        type.contains(similarTextOrId) ||
                        status.contains(similarTextOrId) ||
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
    private void ConfigureTableView()
    {
        TableColumn<RecurringTransaction, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(
            param -> new SimpleObjectProperty<>(param.getValue().GetId()));

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
            param -> new SimpleStringProperty(param.getValue().GetDescription()));

        TableColumn<RecurringTransaction, String> amountColumn =
            new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(
                UIUtils.FormatCurrency(param.getValue().GetAmount())));

        TableColumn<RecurringTransaction, String> walletColumn =
            new TableColumn<>("Wallet");
        walletColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().GetWallet().GetName()));

        TableColumn<RecurringTransaction, String> typeColumn =
            new TableColumn<>("Type");
        typeColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().GetType().name()));

        TableColumn<RecurringTransaction, String> categoryColumn =
            new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().GetCategory().GetName()));

        TableColumn<RecurringTransaction, String> statusColumn =
            new TableColumn<>("Status");
        statusColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().GetStatus().name()));

        TableColumn<RecurringTransaction, String> frequencyColumn =
            new TableColumn<>("Frequency");
        frequencyColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().GetFrequency().name()));

        TableColumn<RecurringTransaction, String> startDateColumn =
            new TableColumn<>("Start Date");
        startDateColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().GetStartDate().format(
                Constants.DATE_FORMATTER_NO_TIME)));

        TableColumn<RecurringTransaction, String> endDateColumn =
            new TableColumn<>("End Date");
        endDateColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().GetEndDate().format(
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
            if (param.getValue().GetStatus().equals(
                    RecurringTransactionStatus.INACTIVE))
            {
                return new SimpleStringProperty("-");
            }
            else
            {
                return new SimpleStringProperty(
                    param.getValue().GetNextDueDate().format(
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
                        setText(item.toString());
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
                    recurringTransactionService.CalculateExpectedRemainingAmount(
                        rt.GetId());
            }
            catch (RuntimeException e)
            {
                expectedRemainingAmount = 0.0;
            }

            return new SimpleStringProperty(
                UIUtils.FormatCurrency(expectedRemainingAmount));
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
}
