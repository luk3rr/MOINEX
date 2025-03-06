/*
 * Filename: AddWalletController.java
 * Created on: October  1, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.math.BigDecimal;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.entities.WalletType;
import org.moinex.services.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Wallet dialog
 */
@Controller
@NoArgsConstructor
public class AddWalletController
{
    @FXML
    private TextField walletNameField;

    @FXML
    private TextField walletBalanceField;

    @FXML
    private ComboBox<String> walletTypeComboBox;

    private WalletService walletService;

    private List<WalletType> walletTypes;

    /**
     * Constructor
     * @param walletService WalletService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddWalletController(WalletService walletService)
    {
        this.walletService = walletService;
    }

    @FXML
    private void initialize()
    {
        loadWalletTypes();

        walletTypeComboBox.getItems().addAll(
            walletTypes.stream().map(WalletType::getName).toList());

        walletBalanceField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
                {
                    walletBalanceField.setText(oldValue);
                }
            });
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)walletNameField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSave()
    {
        String walletName = walletNameField.getText();
        walletName = walletName.strip(); // Remove leading and trailing whitespaces

        String walletBalanceStr = walletBalanceField.getText();
        String walletTypeStr    = walletTypeComboBox.getValue();

        if (walletName.isEmpty() || walletBalanceStr.isEmpty() || walletTypeStr == null)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all the fields.");
            return;
        }

        WalletType walletType =
            walletTypes.stream()
                .filter(wt -> wt.getName().equals(walletTypeStr))
                .findFirst()
                .orElseThrow((() -> new RuntimeException("Invalid wallet type")));

        try
        {
            BigDecimal walletBalance = new BigDecimal(walletBalanceStr);

            walletService.addWallet(walletName, walletBalance, walletType);

            WindowUtils.showSuccessDialog("Success",
                                          "Wallet created",
                                          "The wallet was successfully created");

            Stage stage = (Stage)walletNameField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Invalid balance",
                                        "Please enter a valid balance.");
        }
        catch (RuntimeException e)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Error creating wallet",
                                        e.getMessage());
        }
    }

    /**
     * Load the wallet types
     */
    private void loadWalletTypes()
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

        // Remove Goals wallet type to prevent the user from creating a goal wallet.
        // Goal wallets are created through the Add Goal dialog.
        walletTypes.removeIf(
            wt -> wt.getName().equals(Constants.GOAL_DEFAULT_WALLET_TYPE_NAME));
    }
}
