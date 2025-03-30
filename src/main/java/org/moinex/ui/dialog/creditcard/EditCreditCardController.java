/*
 * Filename: EditCreditCardController.java
 * Created on: October 24, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.creditcard;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.entities.creditcard.CreditCard;
import org.moinex.entities.creditcard.CreditCardOperator;
import org.moinex.entities.wallettransaction.Wallet;
import org.moinex.services.CreditCardService;
import org.moinex.services.WalletService;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Credit Card dialog
 */
@Controller
@NoArgsConstructor
public final class EditCreditCardController extends BaseCreditCardManagement
{
    private CreditCard creditCard = null;

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
        super(creditCardService, walletService);
    }

    public void setCreditCard(CreditCard crc)
    {
        creditCard = crc;
        nameField.setText(creditCard.getName());
        limitField.setText(creditCard.getMaxDebt().toString());
        lastFourDigitsField.setText(creditCard.getLastFourDigits());
        operatorComboBox.setValue(creditCard.getOperator());
        closingDayComboBox.setValue(creditCard.getClosingDay().toString());
        dueDayComboBox.setValue(creditCard.getBillingDueDay().toString());
        defaultBillingWalletComboBox.setValue(creditCard.getDefaultBillingWallet());
    }

    @FXML
    @Override
    protected void handleSave()
    {
        String crcName = nameField.getText();
        crcName        = crcName.strip(); // Remove leading and trailing whitespaces

        String             crcLimitStr          = limitField.getText();
        String             crcLastFourDigitsStr = lastFourDigitsField.getText();
        String             crcClosingDayStr     = closingDayComboBox.getValue();
        String             crcDueDayStr         = dueDayComboBox.getValue();
        CreditCardOperator crcOperator          = operatorComboBox.getValue();
        Wallet crcDefaultBillingWallet = defaultBillingWalletComboBox.getValue();

        if (crcName.isEmpty() || crcLimitStr.isEmpty() ||
            crcLastFourDigitsStr.isEmpty() || crcOperator == null ||
            crcClosingDayStr == null || crcDueDayStr == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");

            return;
        }

        try
        {
            BigDecimal crcLimit      = new BigDecimal(crcLimitStr);
            Integer    crcClosingDay = Integer.parseInt(crcClosingDayStr);
            Integer    crcDueDay     = Integer.parseInt(crcDueDayStr);

            boolean defaultWalletChanged =
                (crcDefaultBillingWallet != null &&
                 creditCard.getDefaultBillingWallet() != null &&
                 crcDefaultBillingWallet.getId().equals(
                     creditCard.getDefaultBillingWallet().getId())) ||
                (crcDefaultBillingWallet == null &&
                 creditCard.getDefaultBillingWallet() == null);

            // Check if it has any modification
            if (creditCard.getName().equals(crcName) &&
                crcLimit.compareTo(creditCard.getMaxDebt()) == 0 &&
                creditCard.getLastFourDigits().equals(crcLastFourDigitsStr) &&
                creditCard.getClosingDay().equals(crcClosingDay) &&
                creditCard.getBillingDueDay().equals(crcDueDay) &&
                creditCard.getOperator().getId().equals(crcOperator.getId()) &&
                defaultWalletChanged)
            {
                WindowUtils.showInformationDialog(
                    "No changes",
                    "No changes were made to the credit card.");
            }
            else // If there is any modification, update the credit card
            {
                creditCard.setName(crcName);
                creditCard.setMaxDebt(crcLimit);
                creditCard.setLastFourDigits(crcLastFourDigitsStr);
                creditCard.setClosingDay(crcClosingDay);
                creditCard.setBillingDueDay(crcDueDay);
                creditCard.setOperator(crcOperator);
                creditCard.setDefaultBillingWallet(crcDefaultBillingWallet);

                creditCardService.updateCreditCard(creditCard);

                WindowUtils.showSuccessDialog("Credit card updated",
                                              "The credit card updated successfully.");
            }

            Stage stage = (Stage)nameField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid limit", "Please enter a valid limit");
        }
        catch (EntityNotFoundException | IllegalArgumentException |
               IllegalStateException e)
        {
            WindowUtils.showErrorDialog("Error creating credit card", e.getMessage());
        }
    }
}
