/*
 * Filename: RemoveTransactionController.fxml
 * Created on: October 12, 2024
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
import org.moinex.entities.WalletTransaction;
import org.moinex.services.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.TransactionStatus;
import org.moinex.util.TransactionType;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Remove Transaction dialog
 * @note Make sure to set the transaction type before calling the initialize method
 */
@Controller
@Scope("prototype") // Create a new instance each time it is requested
public class RemoveTransactionController
{
    @FXML
    private TableView<WalletTransaction> transactionsTableView;

    @FXML
    private TextField searchField;

    private List<WalletTransaction> incomes;

    private WalletTransactionService walletTransactionService;

    private TransactionType transactionType;

    /**
     * Constructor
     * @param walletTransactionService WalletTransactionService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public RemoveTransactionController(
        WalletTransactionService walletTransactionService)
    {
        this.walletTransactionService = walletTransactionService;
    }

    @FXML
    public void initialize()
    { }

    /**
     * Initializes the controller with the transaction type
     * @param transactionType TransactionType
     */
    public void InitializeWithTransactionType(TransactionType transactionType)
    {
        this.transactionType = transactionType;

        if (transactionType == null)
        {
            throw new IllegalStateException("Transaction type not set");
        }

        ConfigureTableView();
        LoadTransaction();

        UpdateTransactionTableView();

        // Add listener to the search field
        searchField.textProperty().addListener(
            (observable, oldValue, newValue) -> UpdateTransactionTableView());
    }

    @FXML
    public void handleDelete()
    {
        WalletTransaction selectedIncome =
            transactionsTableView.getSelectionModel().getSelectedItem();

        // If no income is selected, do nothing
        if (selectedIncome == null)
        {
            return;
        }

        // Create a message to show the user
        StringBuilder message = new StringBuilder();
        message.append("Description: ")
            .append(selectedIncome.GetDescription())
            .append("\n")
            .append("Amount: ")
            .append(UIUtils.FormatCurrency(selectedIncome.GetAmount()))
            .append("\n")
            .append("Date: ")
            .append(selectedIncome.GetDate().format(Constants.DATE_FORMATTER_WITH_TIME))
            .append("\n")
            .append("Status: ")
            .append(selectedIncome.GetStatus().toString())
            .append("\n")
            .append("Wallet: ")
            .append(selectedIncome.GetWallet().GetName())
            .append("\n")
            .append("Wallet balance: ")
            .append(UIUtils.FormatCurrency(selectedIncome.GetWallet().GetBalance()))
            .append("\n")
            .append("Wallet balance after deletion: ");

        if (selectedIncome.GetStatus().equals(TransactionStatus.CONFIRMED))
        {
            if (transactionType == TransactionType.EXPENSE)
            {
                message
                    .append(UIUtils.FormatCurrency(
                        selectedIncome.GetWallet().GetBalance().add(
                            selectedIncome.GetAmount())))
                    .append("\n");
            }
            else
            {
                message
                    .append(UIUtils.FormatCurrency(
                        selectedIncome.GetWallet().GetBalance().add(
                            selectedIncome.GetAmount())))
                    .append("\n");
            }
        }
        else
        {
            message
                .append(UIUtils.FormatCurrency(selectedIncome.GetWallet().GetBalance()))
                .append("\n");
        }

        // Confirm deletion
        if (WindowUtils.ShowConfirmationDialog(
                "Confirm Deletion",
                "Are you sure you want to remove this " +
                    transactionType.toString().toLowerCase() + "?",
                message.toString()))
        {
            walletTransactionService.DeleteTransaction(selectedIncome.GetId());
            transactionsTableView.getItems().remove(selectedIncome);
        }
    }

    @FXML
    public void handleCancel()
    {
        Stage stage = (Stage)searchField.getScene().getWindow();
        stage.close();
    }

    private void LoadTransaction()
    {
        if (transactionType == TransactionType.EXPENSE)
        {
            incomes = walletTransactionService.GetNonArchivedExpenses();
        }
        else
        {
            incomes = walletTransactionService.GetNonArchivedIncomes();
        }
    }

    private void UpdateTransactionTableView()
    {
        String similarTextOrId = searchField.getText().toLowerCase();

        transactionsTableView.getItems().clear();

        if (similarTextOrId.isEmpty())
        {
            transactionsTableView.getItems().addAll(incomes);
            transactionsTableView.refresh();
            return;
        }

        incomes.stream()
            .filter(t
                    -> t.GetDescription().toLowerCase().contains(similarTextOrId) ||
                           t.GetId().toString().contains(similarTextOrId))
            .forEach(transactionsTableView.getItems()::add);

        transactionsTableView.refresh();
    }

    /**
     * Configures the TableView to display the incomes.
     */
    private void ConfigureTableView()
    {
        TableColumn<WalletTransaction, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(
            param -> new SimpleObjectProperty<>(param.getValue().GetId()));

        // Align the ID column to the center
        idColumn.setCellFactory(column -> {
            return new TableCell<WalletTransaction, Long>() {
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
                        setStyle("-fx-padding: 0;"); // set padding to zero to ensure
                                                     // the text is centered
                    }
                }
            };
        });

        TableColumn<WalletTransaction, String> categoryColumn =
            new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().GetCategory().GetName()));

        TableColumn<WalletTransaction, String> statusColumn =
            new TableColumn<>("Status");
        statusColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().GetStatus().name()));

        TableColumn<WalletTransaction, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(
                param.getValue().GetDate().format(Constants.DATE_FORMATTER_WITH_TIME)));

        TableColumn<WalletTransaction, String> walletNameColumn =
            new TableColumn<>("Wallet");
        walletNameColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().GetWallet().GetName()));

        TableColumn<WalletTransaction, String> amountColumn =
            new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(
                UIUtils.FormatCurrency(param.getValue().GetAmount())));

        TableColumn<WalletTransaction, String> descriptionColumn =
            new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().GetDescription()));

        transactionsTableView.getColumns().add(idColumn);
        transactionsTableView.getColumns().add(descriptionColumn);
        transactionsTableView.getColumns().add(amountColumn);
        transactionsTableView.getColumns().add(walletNameColumn);
        transactionsTableView.getColumns().add(dateColumn);
        transactionsTableView.getColumns().add(categoryColumn);
        transactionsTableView.getColumns().add(statusColumn);
    }
}
