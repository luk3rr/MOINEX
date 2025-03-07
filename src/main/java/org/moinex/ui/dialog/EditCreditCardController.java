/*
 * Filename: EditCreditCardController.java
 * Created on: October 24, 2024
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
import org.moinex.entities.CreditCard;
import org.moinex.entities.CreditCardOperator;
import org.moinex.entities.Wallet;
import org.moinex.services.CreditCardService;
import org.moinex.services.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Credit Card dialog
 */
@Controller
@NoArgsConstructor
public class EditCreditCardController
{
    @FXML
    private TextField nameField;

    @FXML
    private TextField limitField;

    @FXML
    private TextField lastFourDigitsField;

    @FXML
    private ComboBox<String> closingDayComboBox;

    @FXML
    private ComboBox<String> dueDayComboBox;

    @FXML
    private ComboBox<String> operatorComboBox;

    @FXML
    private ComboBox<String> defaultBillingWalletComboBox;

    private CreditCardService creditCardService;

    private WalletService walletService;

    private List<CreditCardOperator> operators;

    private List<Wallet> wallets;

    private CreditCard crcToUpdate;

    /**
     * Constructor
     * @param creditCardService The credit card service
     * @param walletService The wallet service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public EditCreditCardController(CreditCardService creditCardService,
                                    WalletService     walletService)
    {
        this.creditCardService = creditCardService;
        this.walletService     = walletService;
    }

    public void setCreditCard(CreditCard crc)
    {
        crcToUpdate = crc;

        nameField.setText(crc.getName());
        limitField.setText(crc.getMaxDebt().toString());
        lastFourDigitsField.setText(crc.getLastFourDigits());
        operatorComboBox.setValue(crc.getOperator().getName());
        closingDayComboBox.setValue(crc.getClosingDay().toString());
        dueDayComboBox.setValue(crc.getBillingDueDay().toString());

        if (crc.getDefaultBillingWallet() != null)
        {
            defaultBillingWalletComboBox.setValue(
                crc.getDefaultBillingWallet().getName());
        }
        else
        {
            defaultBillingWalletComboBox.setValue(null);
        }
    }

    @FXML
    private void initialize()
    {
        populateComboBoxes();

        // Ensure that the limit field only accepts numbers and has a maximum of 4
        // digits
        lastFourDigitsField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.getDigitsRegexUpTo(4)))
                {
                    lastFourDigitsField.setText(oldValue);
                }
            });

        limitField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
            {
                limitField.setText(oldValue);
            }
        });
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)nameField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSave()
    {
        String crcName = nameField.getText();
        crcName        = crcName.strip(); // Remove leading and trailing whitespaces

        String crcLimitStr                 = limitField.getText();
        String crcLastFourDigitsStr        = lastFourDigitsField.getText();
        String crcClosingDayStr            = closingDayComboBox.getValue();
        String crcDueDayStr                = dueDayComboBox.getValue();
        String crcOperatorName             = operatorComboBox.getValue();
        String crcDefaultBillingWalletName = defaultBillingWalletComboBox.getValue();

        if (crcName.isEmpty() || crcLimitStr.isEmpty() ||
            crcLastFourDigitsStr.isEmpty() || crcOperatorName == null ||
            crcClosingDayStr == null || crcDueDayStr == null)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all required fields.");

            return;
        }

        CreditCardOperator crcOperator =
            operators.stream()
                .filter(op -> op.getName().equals(crcOperatorName))
                .findFirst()
                .orElseThrow(
                    ()
                        -> new RuntimeException(
                            "Operator with name: " + crcOperatorName + " not found"));

        try
        {
            BigDecimal crcLimit      = new BigDecimal(crcLimitStr);
            Integer    crcClosingDay = Integer.parseInt(crcClosingDayStr);
            Integer    crcDueDay     = Integer.parseInt(crcDueDayStr);

            Wallet crcDefaultBillingWallet =
                crcDefaultBillingWalletName != null &&
                        !crcDefaultBillingWalletName.isEmpty()
                    ? wallets.stream()
                          .filter(w -> w.getName().equals(crcDefaultBillingWalletName))
                          .findFirst()
                          .orElseThrow(
                              ()
                                  -> new RuntimeException("Wallet with name: " +
                                                          crcDefaultBillingWalletName +
                                                          " not found"))
                    : null;

            boolean defaultWalletChanged =
                (crcDefaultBillingWallet != null &&
                 crcToUpdate.getDefaultBillingWallet() != null &&
                 crcDefaultBillingWallet.getId().equals(
                     crcToUpdate.getDefaultBillingWallet().getId())) ||
                (crcDefaultBillingWallet == null &&
                 crcToUpdate.getDefaultBillingWallet() == null);

            // Check if has any modification
            if (crcToUpdate.getName().equals(crcName) &&
                crcLimit.compareTo(crcToUpdate.getMaxDebt()) == 0 &&
                crcToUpdate.getLastFourDigits().equals(crcLastFourDigitsStr) &&
                crcToUpdate.getClosingDay().equals(crcClosingDay) &&
                crcToUpdate.getBillingDueDay().equals(crcDueDay) &&
                crcToUpdate.getOperator().getId().equals(crcOperator.getId()) &&
                defaultWalletChanged)
            {
                WindowUtils.showInformationDialog(
                    "Information",
                    "No changes",
                    "No changes were made to the credit card.");
            }
            else // If there is any modification, update the credit card
            {
                crcToUpdate.setName(crcName);
                crcToUpdate.setMaxDebt(crcLimit);
                crcToUpdate.setLastFourDigits(crcLastFourDigitsStr);
                crcToUpdate.setClosingDay(crcClosingDay);
                crcToUpdate.setBillingDueDay(crcDueDay);
                crcToUpdate.setOperator(crcOperator);
                crcToUpdate.setDefaultBillingWallet(crcDefaultBillingWallet);

                creditCardService.updateCreditCard(crcToUpdate);

                WindowUtils.showSuccessDialog("Success",
                                              "Credit card updated",
                                              "The credit card updated successfully.");
            }

            Stage stage = (Stage)nameField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Invalid limit",
                                        "Please enter a valid limit");
        }
        catch (RuntimeException e)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Error creating credit card",
                                        e.getMessage());
        }
    }

    private void loadCreditCardOperatorsFromDatabase()
    {
        operators = creditCardService.getAllCreditCardOperatorsOrderedByName();
    }

    private void loadWalletsFromDatabase()
    {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();
    }

    private void populateComboBoxes()
    {
        for (int i = 1; i <= Constants.MAX_BILLING_DUE_DAY; i++)
        {
            closingDayComboBox.getItems().add(String.valueOf(i));
            dueDayComboBox.getItems().add(String.valueOf(i));
        }

        loadCreditCardOperatorsFromDatabase();

        for (CreditCardOperator operator : operators)
        {
            operatorComboBox.getItems().add(operator.getName());
        }

        loadWalletsFromDatabase();

        for (Wallet wallet : wallets)
        {
            defaultBillingWalletComboBox.getItems().add(wallet.getName());
        }

        // Add blank option to the default billing wallet combo box
        defaultBillingWalletComboBox.getItems().add("");
    }
}
