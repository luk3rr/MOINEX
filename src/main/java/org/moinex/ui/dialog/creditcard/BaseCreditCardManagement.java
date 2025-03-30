/*
 * Filename: BaseCreditCardManagement.java
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.creditcard;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.entities.creditcard.CreditCardOperator;
import org.moinex.entities.wallettransaction.Wallet;
import org.moinex.services.CreditCardService;
import org.moinex.services.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class to implement the common behavior of the Add and Edit Credit Card
 */
@NoArgsConstructor
public abstract class BaseCreditCardManagement
{
    @FXML
    protected TextField nameField;

    @FXML
    protected TextField limitField;

    @FXML
    protected TextField lastFourDigitsField;

    @FXML
    protected ComboBox<String> closingDayComboBox;

    @FXML
    protected ComboBox<String> dueDayComboBox;

    @FXML
    protected ComboBox<CreditCardOperator> operatorComboBox;

    @FXML
    protected ComboBox<Wallet> defaultBillingWalletComboBox;

    protected CreditCardService creditCardService;

    protected WalletService walletService;

    protected List<CreditCardOperator> operators;

    protected List<Wallet> wallets;

    /**
     * Constructor
     * @param creditCardService The credit card service
     * @param walletService The wallet service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    protected BaseCreditCardManagement(CreditCardService creditCardService,
                                   WalletService     walletService)
    {
        this.creditCardService = creditCardService;
        this.walletService     = walletService;
    }

    @FXML
    protected void initialize()
    {
        configureComboBoxes();

        loadCreditCardOperatorsFromDatabase();
        loadWalletsFromDatabase();

        populateComboBoxes();

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
    protected abstract void handleSave();

    @FXML
    protected void handleCancel()
    {
        Stage stage = (Stage)nameField.getScene().getWindow();
        stage.close();
    }

    protected void loadCreditCardOperatorsFromDatabase()
    {
        operators = creditCardService.getAllCreditCardOperatorsOrderedByName();
    }

    protected void loadWalletsFromDatabase()
    {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();
    }

    protected void populateComboBoxes()
    {
        for (int i = 1; i <= Constants.MAX_BILLING_DUE_DAY; i++)
        {
            closingDayComboBox.getItems().add(String.valueOf(i));
            dueDayComboBox.getItems().add(String.valueOf(i));
        }

        operatorComboBox.getItems().setAll(operators);
        defaultBillingWalletComboBox.getItems().setAll(wallets);

        // Add a blank option to the default billing wallet combo box
        defaultBillingWalletComboBox.getItems().addFirst(null);
    }

    protected void configureComboBoxes()
    {
        UIUtils.configureComboBox(operatorComboBox, CreditCardOperator::getName);
        UIUtils.configureComboBox(defaultBillingWalletComboBox, Wallet::getName);
    }
}
