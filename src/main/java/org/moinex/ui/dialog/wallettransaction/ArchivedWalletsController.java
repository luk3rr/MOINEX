/*
 * Filename: ArchivedWalletsController.java
 * Created on: October 15, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityNotFoundException;
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
import org.moinex.entities.wallettransaction.Wallet;
import org.moinex.services.WalletService;
import org.moinex.services.WalletTransactionService;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Archived Wallets dialog
 */
@Controller
@NoArgsConstructor
public class ArchivedWalletsController
{
    @FXML
    private TableView<Wallet> walletTableView;

    @FXML
    private TextField searchField;

    private List<Wallet> archivedWallets;

    private WalletService walletService;

    private WalletTransactionService walletTransactionService;

    /**
     * Constructor
     * @param walletService WalletService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public ArchivedWalletsController(WalletService            walletService,
                                     WalletTransactionService walletTransactionService)
    {
        this.walletService            = walletService;
        this.walletTransactionService = walletTransactionService;
    }

    @FXML
    public void initialize()
    {
        loadArchivedWalletsFromDatabase();

        configureTableView();

        updateWalletTableView();

        // Add listener to the search field
        searchField.textProperty().addListener(
            (observable, oldValue, newValue) -> updateWalletTableView());
    }

    @FXML
    private void handleUnarchive()
    {
        Wallet selectedWallet = walletTableView.getSelectionModel().getSelectedItem();

        if (selectedWallet == null)
        {
            WindowUtils.showInformationDialog("No wallet selected",
                                              "Please select a wallet to unarchive");
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                "Unarchive wallet " + selectedWallet.getName(),
                "Are you sure you want to unarchive this wallet?"))
        {
            try
            {
                walletService.unarchiveWallet(selectedWallet.getId());

                WindowUtils.showSuccessDialog("Wallet unarchived",
                                              "Wallet " + selectedWallet.getName() +
                                                  " has been unarchived");

                // Remove this wallet from the list and update the table view
                archivedWallets.remove(selectedWallet);
                updateWalletTableView();
            }
            catch (EntityNotFoundException e)
            {
                WindowUtils.showErrorDialog("Error unarchiving wallet", e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete()
    {
        Wallet selectedWallet = walletTableView.getSelectionModel().getSelectedItem();

        if (selectedWallet == null)
        {
            WindowUtils.showInformationDialog("No wallet selected",
                                              "Please select a wallet to delete");
            return;
        }

        // Prevent the removal of a wallet with associated transactions
        if (walletTransactionService.getTransactionCountByWallet(
                selectedWallet.getId()) > 0)
        {
            WindowUtils.showInformationDialog(
                "Wallet has transactions",
                "Cannot delete a wallet with associated transactions. You can " +
                "archive it instead.");
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                "Remove wallet " + selectedWallet.getName(),
                "Are you sure you want to remove this wallet?"))
        {
            try
            {
                walletService.deleteWallet(selectedWallet.getId());

                WindowUtils.showSuccessDialog("Wallet deleted",
                                              "Wallet " + selectedWallet.getName() +
                                                  " has been deleted");

                // Remove this wallet from the list and update the table view
                archivedWallets.remove(selectedWallet);
                updateWalletTableView();
            }
            catch (EntityNotFoundException | IllegalStateException e)
            {
                WindowUtils.showErrorDialog("Error removing wallet", e.getMessage());
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
     * Loads the categories from the database
     */
    private void loadArchivedWalletsFromDatabase()
    {
        archivedWallets = walletService.getAllArchivedWallets();
    }

    /**
     * Updates the category table view
     */
    private void updateWalletTableView()
    {
        String similarTextOrId = searchField.getText().toLowerCase();

        walletTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty())
        {
            walletTableView.getItems().setAll(archivedWallets);
        }
        else
        {
            archivedWallets.stream()
                .filter(w -> {
                    String type = w.getType().getName().toLowerCase();
                    String name = w.getName().toLowerCase();
                    String id   = w.getId().toString();

                    return type.contains(similarTextOrId) ||
                        name.contains(similarTextOrId) || id.contains(similarTextOrId);
                })
                .forEach(walletTableView.getItems()::add);
        }

        walletTableView.refresh();
    }

    /**
     * Configures the table view columns
     */
    private void configureTableView()
    {
        TableColumn<Wallet, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(
            param -> new SimpleObjectProperty<>(param.getValue().getId()));

        idColumn.setCellFactory(column -> new TableCell<Wallet, Long>() {
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

        TableColumn<Wallet, String> walletColumn = new TableColumn<>("Wallet");
        walletColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getName()));

        TableColumn<Wallet, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getType().getName()));

        TableColumn<Wallet, Long> numOfTransactionsColumn =
            new TableColumn<>("Associated Transactions");
        numOfTransactionsColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(
                walletTransactionService.getTransactionCountByWallet(
                    param.getValue().getId())));

        numOfTransactionsColumn.setCellFactory(column -> new TableCell<Wallet, Long>() {
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

        walletTableView.getColumns().add(idColumn);
        walletTableView.getColumns().add(walletColumn);
        walletTableView.getColumns().add(typeColumn);
        walletTableView.getColumns().add(numOfTransactionsColumn);
    }
}
