/*
 * Filename: ArchivedWalletsController.java
 * Created on: October 15, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityNotFoundException;
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
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.I18nService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Archived Wallets dialog
 */
@Controller
@NoArgsConstructor
public class ArchivedWalletsController {
    @FXML private TableView<Wallet> walletTableView;

    @FXML private TextField searchField;

    private List<Wallet> archivedWallets;

    private WalletService walletService;

    private WalletTransactionService walletTransactionService;

    private I18nService i18nService;

    /**
     * Constructor
     * @param walletService WalletService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public ArchivedWalletsController(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            I18nService i18nService) {
        this.walletService = walletService;
        this.walletTransactionService = walletTransactionService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        loadArchivedWalletsFromDatabase();

        configureTableView();

        updateWalletTableView();

        // Add listener to the search field
        searchField
                .textProperty()
                .addListener((observable, oldValue, newValue) -> updateWalletTableView());
    }

    @FXML
    private void handleUnarchive() {
        Wallet selectedWallet = walletTableView.getSelectionModel().getSelectedItem();

        if (selectedWallet == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_NO_WALLET_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_NO_WALLET_SELECTED_UNARCHIVE_MESSAGE));
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_DIALOG_UNARCHIVE_WALLET_TITLE)
                        + " "
                        + selectedWallet.getName(),
                i18nService.tr(
                        Constants.TranslationKeys
                                .WALLETTRANSACTION_DIALOG_UNARCHIVE_WALLET_MESSAGE),
                i18nService.getBundle())) {
            try {
                walletService.unarchiveWallet(selectedWallet.getId());

                WindowUtils.showSuccessDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_DIALOG_WALLET_UNARCHIVED_TITLE),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .WALLETTRANSACTION_DIALOG_WALLET_UNARCHIVED_MESSAGE),
                                selectedWallet.getName()));

                // Remove this wallet from the list and update the table view
                archivedWallets.remove(selectedWallet);
                updateWalletTableView();
            } catch (EntityNotFoundException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_DIALOG_ERROR_UNARCHIVING_WALLET_TITLE),
                        e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        Wallet selectedWallet = walletTableView.getSelectionModel().getSelectedItem();

        if (selectedWallet == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_NO_WALLET_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_NO_WALLET_SELECTED_DELETE_MESSAGE));
            return;
        }

        // Prevent the removal of a wallet with associated transactions
        if (walletTransactionService.getTransactionCountByWallet(selectedWallet.getId()) > 0) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_WALLET_HAS_TRANSACTIONS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_WALLET_HAS_TRANSACTIONS_MESSAGE));
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_DIALOG_REMOVE_WALLET_TITLE)
                        + " "
                        + selectedWallet.getName(),
                i18nService.tr(
                        Constants.TranslationKeys.WALLETTRANSACTION_DIALOG_REMOVE_WALLET_MESSAGE),
                i18nService.getBundle())) {
            try {
                walletService.deleteWallet(selectedWallet.getId());

                WindowUtils.showSuccessDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_DIALOG_WALLET_DELETED_TITLE),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .WALLETTRANSACTION_DIALOG_WALLET_DELETED_MESSAGE),
                                selectedWallet.getName()));

                // Remove this wallet from the list and update the table view
                archivedWallets.remove(selectedWallet);
                updateWalletTableView();
            } catch (EntityNotFoundException | IllegalStateException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_DIALOG_ERROR_REMOVING_WALLET_TITLE),
                        e.getMessage());
            }
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
    private void loadArchivedWalletsFromDatabase() {
        archivedWallets = walletService.getAllArchivedWallets();
    }

    /**
     * Updates the category table view
     */
    private void updateWalletTableView() {
        String similarTextOrId = searchField.getText().toLowerCase();

        walletTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty()) {
            walletTableView.getItems().setAll(archivedWallets);
        } else {
            archivedWallets.stream()
                    .filter(
                            w -> {
                                String type = w.getType().getName().toLowerCase();
                                String name = w.getName().toLowerCase();
                                String id = w.getId().toString();

                                return type.contains(similarTextOrId)
                                        || name.contains(similarTextOrId)
                                        || id.contains(similarTextOrId);
                            })
                    .forEach(walletTableView.getItems()::add);
        }

        walletTableView.refresh();
    }

    /**
     * Configures the table view columns
     */
    private void configureTableView() {
        TableColumn<Wallet, Integer> idColumn = getWalletLongTableColumn();

        TableColumn<Wallet, String> walletColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_WALLET));
        walletColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getName()));

        TableColumn<Wallet, String> typeColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_LABEL_TYPE));
        typeColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateWalletType(
                                        param.getValue().getType(), i18nService)));

        TableColumn<Wallet, Integer> numOfTransactionsColumn = getLongTableColumn();

        walletTableView.getColumns().add(idColumn);
        walletTableView.getColumns().add(walletColumn);
        walletTableView.getColumns().add(typeColumn);
        walletTableView.getColumns().add(numOfTransactionsColumn);
    }

    private TableColumn<Wallet, Integer> getLongTableColumn() {
        TableColumn<Wallet, Integer> numOfTransactionsColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_TABLE_ASSOCIATED_TRANSACTIONS));
        numOfTransactionsColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                walletTransactionService.getTransactionCountByWallet(
                                        param.getValue().getId())));

        numOfTransactionsColumn.setCellFactory(
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
        return numOfTransactionsColumn;
    }

    private TableColumn<Wallet, Integer> getWalletLongTableColumn() {
        TableColumn<Wallet, Integer> idColumn =
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
}
