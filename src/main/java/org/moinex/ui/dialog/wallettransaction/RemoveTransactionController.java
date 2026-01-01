/*
 * Filename: RemoveTransactionController.fxml
 * Created on: October 12, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

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
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.service.I18nService;
import org.moinex.service.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Remove Transaction dialog
 * @note Make sure to set the transaction type before calling the initialize method
 */
@Controller
@Scope("prototype") // Create a new instance each time it is requested
@NoArgsConstructor
public class RemoveTransactionController {
    @FXML private TableView<WalletTransaction> transactionsTableView;

    @FXML private TextField searchField;

    private List<WalletTransaction> incomes;

    private WalletTransactionService walletTransactionService;

    private I18nService i18nService;

    private TransactionType transactionType;

    /**
     * Constructor
     * @param walletTransactionService WalletTransactionService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public RemoveTransactionController(
            WalletTransactionService walletTransactionService, I18nService i18nService) {
        this.walletTransactionService = walletTransactionService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        // Still empty
    }

    /**
     * Initializes the controller with the transaction type
     * @param transactionType TransactionType
     */
    public void initializeWithTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;

        if (transactionType == null) {
            throw new IllegalStateException("Transaction type not set");
        }

        configureTableView();
        loadTransactionFromDatabase();

        updateTransactionTableView();

        // Add listener to the search field
        searchField
                .textProperty()
                .addListener((observable, oldValue, newValue) -> updateTransactionTableView());
    }

    @FXML
    public void handleDelete() {
        WalletTransaction selectedTransaction =
                transactionsTableView.getSelectionModel().getSelectedItem();

        // If no income is selected, do nothing
        if (selectedTransaction == null) {
            return;
        }

        // Create a message to show the user
        StringBuilder message = new StringBuilder();
        message.append(
                        i18nService.tr(
                                Constants.TranslationKeys.WALLETTRANSACTION_LABEL_DESCRIPTION))
                .append(" ")
                .append(selectedTransaction.getDescription())
                .append("\n")
                .append(i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_LABEL_AMOUNT))
                .append(" ")
                .append(UIUtils.formatCurrency(selectedTransaction.getAmount()))
                .append("\n")
                .append(i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_LABEL_DATE))
                .append(" ")
                .append(
                        UIUtils.formatDateTimeForDisplay(
                                selectedTransaction.getDate(), i18nService))
                .append("\n")
                .append(i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_LABEL_STATUS))
                .append(" ")
                .append(selectedTransaction.getStatus().toString())
                .append("\n")
                .append(i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_LABEL_WALLET))
                .append(" ")
                .append(selectedTransaction.getWallet().getName())
                .append("\n")
                .append(
                        i18nService.tr(
                                Constants.TranslationKeys.WALLETTRANSACTION_LABEL_WALLET_BALANCE))
                .append(" ")
                .append(UIUtils.formatCurrency(selectedTransaction.getWallet().getBalance()))
                .append("\n")
                .append(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_LABEL_WALLET_BALANCE_AFTER_DELETION))
                .append(" ");

        // if the transaction is confirmed, add the amount to the wallet balance
        // otherwise, the wallet balance remains the same
        message.append(
                        UIUtils.formatCurrency(
                                selectedTransaction
                                        .getWallet()
                                        .getBalance()
                                        .add(
                                                selectedTransaction
                                                                .getStatus()
                                                                .equals(TransactionStatus.CONFIRMED)
                                                        ? selectedTransaction.getAmount()
                                                        : BigDecimal.ZERO)))
                .append("\n");

        // Confirm deletion
        if (WindowUtils.showConfirmationDialog(
                i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_DIALOG_CONFIRM_DELETE)
                        + " "
                        + transactionType.toString().toLowerCase()
                        + "?",
                message.toString())) {
            walletTransactionService.deleteTransaction(selectedTransaction.getId());
            transactionsTableView.getItems().remove(selectedTransaction);
        }
    }

    @FXML
    public void handleCancel() {
        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.close();
    }

    private void loadTransactionFromDatabase() {
        if (transactionType == TransactionType.EXPENSE) {
            incomes = walletTransactionService.getNonArchivedExpenses();
        } else {
            incomes = walletTransactionService.getNonArchivedIncomes();
        }
    }

    private void updateTransactionTableView() {
        String similarTextOrId = searchField.getText().toLowerCase();

        transactionsTableView.getItems().clear();

        if (similarTextOrId.isEmpty()) {
            transactionsTableView.getItems().addAll(incomes);
            transactionsTableView.refresh();
            return;
        }

        incomes.stream()
                .filter(
                        t ->
                                t.getDescription().toLowerCase().contains(similarTextOrId)
                                        || t.getId().toString().contains(similarTextOrId))
                .forEach(transactionsTableView.getItems()::add);

        transactionsTableView.refresh();
    }

    /**
     * Configures the TableView to display the incomes.
     */
    private void configureTableView() {
        TableColumn<WalletTransaction, Integer> idColumn = getWalletTransactionLongTableColumn();

        TableColumn<WalletTransaction, String> categoryColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_CATEGORY));
        categoryColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getCategory().getName()));

        TableColumn<WalletTransaction, String> statusColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_STATUS));
        statusColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getStatus().name()));

        TableColumn<WalletTransaction, String> dateColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_DATE));
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.formatDateTimeForDisplay(
                                        param.getValue().getDate(), i18nService)));

        TableColumn<WalletTransaction, String> walletNameColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_WALLET));
        walletNameColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getWallet().getName()));

        TableColumn<WalletTransaction, String> amountColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_AMOUNT));
        amountColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(param.getValue().getAmount())));

        TableColumn<WalletTransaction, String> descriptionColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.WALLETTRANSACTION_TABLE_DESCRIPTION));
        descriptionColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getDescription()));

        transactionsTableView.getColumns().add(idColumn);
        transactionsTableView.getColumns().add(descriptionColumn);
        transactionsTableView.getColumns().add(amountColumn);
        transactionsTableView.getColumns().add(walletNameColumn);
        transactionsTableView.getColumns().add(dateColumn);
        transactionsTableView.getColumns().add(categoryColumn);
        transactionsTableView.getColumns().add(statusColumn);
    }

    private TableColumn<WalletTransaction, Integer> getWalletTransactionLongTableColumn() {
        TableColumn<WalletTransaction, Integer> idColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_ID));
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
                                    setStyle("-fx-padding: 0;"); // set padding to zero to ensure
                                    // the text is centered
                                }
                            }
                        });
        return idColumn;
    }
}
