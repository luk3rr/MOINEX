/*
 * Filename: AddCreditCardController.java
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
import org.moinex.entities.CreditCardOperator;
import org.moinex.entities.Wallet;
import org.moinex.services.CreditCardService;
import org.moinex.services.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Credit Card dialog
 */
@Controller
public class AddCreditCardController
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

    public AddCreditCardController() { }

    /**
     * Constructor
     * @param creditCardService The credit card service
     * @param walletService The wallet service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddCreditCardController(CreditCardService creditCardService,
                                   WalletService     walletService)
    {
        this.creditCardService = creditCardService;
        this.walletService     = walletService;
    }

    @FXML
    private void initialize()
    {
        PopulateComboBoxes();

        // Ensure that the limit field only accepts numbers and has a maximum of 4
        // digits
        lastFourDigitsField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.DIGITS_ONLY_REGEX) ||
                    newValue.length() > 4)
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
            WindowUtils.ShowErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all required fields.");

            return;
        }

        CreditCardOperator crcOperator =
            operators.stream()
                .filter(op -> op.GetName().equals(crcOperatorName))
                .findFirst()
                .get();

        try
        {
            BigDecimal crcLimit = new BigDecimal(crcLimitStr);

            Integer crcClosingDay = Integer.parseInt(crcClosingDayStr);
            Integer crcDueDay     = Integer.parseInt(crcDueDayStr);

            Long crcDefaultBillingWalletId =
                crcDefaultBillingWalletName != null &&
                        !crcDefaultBillingWalletName.isEmpty()
                    ? wallets.stream()
                          .filter(w -> w.GetName().equals(crcDefaultBillingWalletName))
                          .findFirst()
                          .get()
                          .GetId()
                    : null;

            creditCardService.CreateCreditCard(crcName,
                                               crcDueDay,
                                               crcClosingDay,
                                               crcLimit,
                                               crcLastFourDigitsStr,
                                               crcOperator.GetId(),
                                               crcDefaultBillingWalletId);

            WindowUtils.ShowSuccessDialog("Success",
                                          "Credit card created",
                                          "The credit card was successfully created");

            Stage stage = (Stage)nameField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Invalid limit",
                                        "Please enter a valid limit");
        }
        catch (RuntimeException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Error creating credit card",
                                        e.getMessage());
        }
    }

    private void LoadCreditCardOperators()
    {
        operators = creditCardService.GetAllCreditCardOperatorsOrderedByName();
    }

    private void LoadWallets()
    {
        wallets = walletService.GetAllNonArchivedWalletsOrderedByName();
    }

    private void PopulateComboBoxes()
    {
        for (int i = 1; i <= Constants.MAX_BILLING_DUE_DAY; i++)
        {
            closingDayComboBox.getItems().add(String.valueOf(i));
            dueDayComboBox.getItems().add(String.valueOf(i));
        }

        LoadCreditCardOperators();

        for (CreditCardOperator operator : operators)
        {
            operatorComboBox.getItems().add(operator.GetName());
        }

        LoadWallets();

        for (Wallet wallet : wallets)
        {
            defaultBillingWalletComboBox.getItems().add(wallet.GetName());
        }

        // Add blank option to the default billing wallet combo box
        defaultBillingWalletComboBox.getItems().add("");
    }
}
