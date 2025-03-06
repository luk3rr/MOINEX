/*
 * Filename: ChangeWalletTypeController.java
 * Created on: October  6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.entities.Wallet;
import org.moinex.entities.WalletType;
import org.moinex.services.WalletService;
import org.moinex.util.Constants;
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
    private ComboBox<String> walletComboBox;

    @FXML
    private ComboBox<String> newTypeComboBox;

    @FXML
    private Label currentTypeLabel;

    private List<Wallet> wallets;

    private List<WalletType> walletTypes;

    private WalletService walletService;

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
        if (wallets.stream().noneMatch(w -> w.getId() == wt.getId()))
        {
            return;
        }

        walletComboBox.setValue(wt.getName());

        updateCurrentTypeLabel(wt);
    }

    @FXML
    private void initialize()
    {
        loadWalletsFromDatabase();
        loadWalletTypesFromDatabase();

        walletComboBox.getItems().addAll(
            wallets.stream().map(Wallet::getName).toList());

        // Add wallet types, but remove the default goal wallet type
        newTypeComboBox.getItems().addAll(
            walletTypes.stream()
                .filter(
                    wt -> !wt.getName().equals(Constants.GOAL_DEFAULT_WALLET_TYPE_NAME))
                .map(WalletType::getName)
                .toList());

        // Set the current type label
        walletComboBox.setOnAction(e -> {
            String walletName = walletComboBox.getValue();
            Wallet wallet     = wallets.stream()
                                .filter(w -> w.getName().equals(walletName))
                                .findFirst()
                                .get();

            updateCurrentTypeLabel(wallet);
        });
    }

    @FXML
    private void handleSave()
    {
        String walletName       = walletComboBox.getValue();
        String walletNewTypeStr = newTypeComboBox.getValue();

        if (walletName == null || walletNewTypeStr == null)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all the fields.");
            return;
        }

        Wallet wallet =
            wallets.stream()
                .filter(w -> w.getName().equals(walletName))
                .findFirst()
                .orElseThrow(()
                                 -> new RuntimeException("Wallet with name " +
                                                         walletName + " not found"));

        WalletType walletNewType =
            walletTypes.stream()
                .filter(wt -> wt.getName().equals(walletNewTypeStr))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid wallet type"));

        try
        {
            walletService.changeWalletType(wallet.getId(), walletNewType);

            WindowUtils.showSuccessDialog("Success",
                                          "Wallet type changed",
                                          "The wallet type was successfully changed.");
        }
        catch (RuntimeException e)
        {
            WindowUtils.showErrorDialog("Error", "Invalid input", e.getMessage());
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

        String nameToMove = "Others";

        // Move the "Others" wallet type to the end of the list
        if (walletTypes.stream()
                .filter(n -> n.getName().equals(nameToMove))
                .findFirst()
                .isPresent())
        {
            WalletType walletType =
                walletTypes.stream()
                    .filter(wt -> wt.getName().equals(nameToMove))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Invalid wallet type"));

            walletTypes.remove(walletType);
            walletTypes.add(walletType);
        }
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
