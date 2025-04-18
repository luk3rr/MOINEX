/*
 * Filename: ChangeWalletTypeController.java
 * Created on: October  6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.entities.wallettransaction.Wallet;
import org.moinex.entities.wallettransaction.WalletType;
import org.moinex.error.MoinexException;
import org.moinex.services.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Change Wallet Type dialog
 */
@Controller
@NoArgsConstructor
public class ChangeWalletTypeController
{
    @FXML
    private ComboBox<Wallet> walletComboBox;

    @FXML
    private ComboBox<WalletType> newTypeComboBox;

    @FXML
    private Label currentTypeLabel;

    private List<Wallet> wallets;

    private List<WalletType> walletTypes;

    private WalletService walletService;

    private Wallet wallet = null;

    /**
     * Constructor
     * @param walletService WalletService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public ChangeWalletTypeController(WalletService walletService)
    {
        this.walletService = walletService;
    }

    public void setWalletComboBox(Wallet wt)
    {
        if (wallets.stream().noneMatch(w -> w.getId().equals(wt.getId())))
        {
            return;
        }

        this.wallet = wt;
        walletComboBox.setValue(wallet);
        updateCurrentTypeLabel(wallet);
    }

    @FXML
    private void initialize()
    {
        configureComboBoxes();

        loadWalletsFromDatabase();
        loadWalletTypesFromDatabase();

        populateComboBoxes();

        // Set the current type label
        walletComboBox.setOnAction(e -> {
            Wallet wallet = walletComboBox.getValue();
            updateCurrentTypeLabel(wallet);
        });
    }

    @FXML
    private void handleSave()
    {
        Wallet     wallet        = walletComboBox.getValue();
        WalletType walletNewType = newTypeComboBox.getValue();

        if (wallet == null || walletNewType == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");
            return;
        }

        try
        {
            walletService.changeWalletType(wallet.getId(), walletNewType);

            WindowUtils.showSuccessDialog("Wallet type changed",
                                          "The wallet type was successfully changed.");
        }
        catch (EntityNotFoundException | MoinexException.AttributeAlreadySetException e)
        {
            WindowUtils.showErrorDialog("Invalid input", e.getMessage());
            return;
        }

        Stage stage = (Stage)walletComboBox.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)walletComboBox.getScene().getWindow();
        stage.close();
    }

    private void loadWalletsFromDatabase()
    {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();
    }

    private void loadWalletTypesFromDatabase()
    {
        walletTypes = walletService.getAllWalletTypes();
    }

    private void populateComboBoxes()
    {
        walletComboBox.getItems().addAll(wallets);

        String nameToMove = "Others";

        // Move the "Others" wallet type to the end of the list
        if (walletTypes.stream().anyMatch(n -> n.getName().equals(nameToMove)))
        {
            WalletType walletType =
                walletTypes.stream()
                    .filter(wt -> wt.getName().equals(nameToMove))
                    .findFirst()
                    .orElseThrow(
                        () -> new IllegalStateException("Invalid wallet type"));

            walletTypes.remove(walletType);
            walletTypes.add(walletType);
        }

        // Add wallet types, but remove the default goal wallet type
        newTypeComboBox.getItems().addAll(
            walletTypes.stream()
                .filter(
                    wt -> !wt.getName().equals(Constants.GOAL_DEFAULT_WALLET_TYPE_NAME))
                .toList());
    }

    private void configureComboBoxes()
    {
        UIUtils.configureComboBox(walletComboBox, Wallet::getName);
        UIUtils.configureComboBox(newTypeComboBox, WalletType::getName);
    }

    private void updateCurrentTypeLabel(Wallet wt)
    {
        if (wt == null)
        {
            currentTypeLabel.setText("-");
            return;
        }

        currentTypeLabel.setText(wt.getType().getName());
    }
}
