/*
 * Filename: ChangeWalletBalanceController.java
 * Created on: October 30, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.entities.Wallet;
import org.moinex.services.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Rename Wallet dialog
 */
@Controller
@NoArgsConstructor
public class ChangeWalletBalanceController
{
    @FXML
    private ComboBox<String> walletComboBox;

    @FXML
    private TextField balanceField;

    private List<Wallet> wallets;

    private WalletService walletService;

    /**
     * Constructor
     * @param walletService WalletService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public ChangeWalletBalanceController(WalletService walletService)
    {
        this.walletService = walletService;
    }

    public void setWalletComboBox(Wallet wt)
    {
        if (wallets.stream().noneMatch(w -> w.getId().equals(wt.getId())))
        {
            return;
        }

        walletComboBox.setValue(wt.getName());
        balanceField.setText(wt.getBalance().toString());
    }

    @FXML
    private void initialize()
    {
        loadWalletsFromDatabase();

        walletComboBox.getItems().addAll(
            wallets.stream().map(Wallet::getName).toList());

        balanceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
            {
                balanceField.setText(oldValue);
            }
        });
    }

    @FXML
    private void handleSave()
    {
        String walletName    = walletComboBox.getValue();
        String newBalanceStr = balanceField.getText();

        if (walletName == null || newBalanceStr.isBlank())
        {
            WindowUtils.showInformationDialog(
                "Invalid input",
                "Please fill all required fields before saving");
            return;
        }

        Wallet wallet =
            wallets.stream()
                .filter(w -> w.getName().equals(walletName))
                .findFirst()
                .orElseThrow(()
                                 -> new EntityNotFoundException(
                                     "Wallet with name " + walletName + " not found"));

        try
        {
            BigDecimal newBalance = new BigDecimal(newBalanceStr);

            // Check if has modification
            if (wallet.getBalance().compareTo(newBalance) == 0)
            {
                WindowUtils.showInformationDialog("No changes",
                                                  "The balance was not changed.");
                return;
            }
            else // Update balance
            {
                walletService.updateWalletBalance(wallet.getId(), newBalance);

                WindowUtils.showSuccessDialog("Wallet updated",
                                              "The balance was updated successfully.");
            }

            Stage stage = (Stage)balanceField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid input", "Balance must be a number");
            return;
        }
        catch (EntityNotFoundException e)
        {
            WindowUtils.showErrorDialog("Error renaming wallet", e.getMessage());
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
}
