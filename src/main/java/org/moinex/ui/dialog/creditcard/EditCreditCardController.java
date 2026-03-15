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
import org.moinex.model.creditcard.CreditCard;
import org.moinex.model.creditcard.CreditCardOperator;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.PreferencesService;
import org.moinex.service.creditcard.CreditCardService;
import org.moinex.service.wallet.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/** Controller for the Edit Credit Card dialog */
@Controller
@NoArgsConstructor
public final class EditCreditCardController extends BaseCreditCardManagement {
    private CreditCard creditCard = null;
    private PreferencesService preferencesService;

    /**
     * Constructor
     *
     * @param creditCardService The credit card service
     * @param walletService The wallet service
     * @param preferencesService The internationalization service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public EditCreditCardController(
            CreditCardService creditCardService,
            WalletService walletService,
            PreferencesService preferencesService) {
        super(creditCardService, walletService);
        this.preferencesService = preferencesService;
    }

    public void setCreditCard(CreditCard crc) {
        creditCard = crc;
        nameField.setText(creditCard.getName());
        limitField.setText(creditCard.getMaxDebt().toString());
        lastFourDigitsField.setText(creditCard.getLastFourDigits());
        operatorComboBox.setValue(creditCard.getOperator());
        closingDayComboBox.setValue(String.valueOf(creditCard.getClosingDay()));
        dueDayComboBox.setValue(String.valueOf(creditCard.getBillingDueDay()));
        defaultBillingWalletComboBox.setValue(creditCard.getDefaultBillingWallet());
    }

    @FXML
    @Override
    protected void handleSave() {
        String crcName = nameField.getText();
        crcName = crcName.strip(); // Remove leading and trailing whitespaces

        String crcLimitStr = limitField.getText();
        String crcLastFourDigitsStr = lastFourDigitsField.getText();
        String crcClosingDayStr = closingDayComboBox.getValue();
        String crcDueDayStr = dueDayComboBox.getValue();
        CreditCardOperator crcOperator = operatorComboBox.getValue();
        Wallet crcDefaultBillingWallet = defaultBillingWalletComboBox.getValue();

        if (crcName.isEmpty()
                || crcLimitStr.isEmpty()
                || crcLastFourDigitsStr.isEmpty()
                || crcOperator == null
                || crcClosingDayStr == null
                || crcDueDayStr == null) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_EMPTY_FIELDS_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_EMPTY_FIELDS_MESSAGE));

            return;
        }

        try {
            BigDecimal crcLimit = new BigDecimal(crcLimitStr);
            int crcClosingDay = Integer.parseInt(crcClosingDayStr);
            int crcDueDay = Integer.parseInt(crcDueDayStr);

            boolean defaultWalletChanged =
                    (crcDefaultBillingWallet != null
                                    && creditCard.getDefaultBillingWallet() != null
                                    && crcDefaultBillingWallet
                                            .getId()
                                            .equals(creditCard.getDefaultBillingWallet().getId()))
                            || (crcDefaultBillingWallet == null
                                    && creditCard.getDefaultBillingWallet() == null);

            // Check if it has any modification
            if (creditCard.getName().equals(crcName)
                    && crcLimit.compareTo(creditCard.getMaxDebt()) == 0
                    && creditCard.getLastFourDigits().equals(crcLastFourDigitsStr)
                    && creditCard.getClosingDay() == crcClosingDay
                    && creditCard.getBillingDueDay() == crcDueDay
                    && creditCard.getOperator().getId().equals(crcOperator.getId())
                    && defaultWalletChanged) {
                WindowUtils.showInformationDialog(
                        preferencesService.translate(
                                Constants.TranslationKeys.CREDITCARD_DIALOG_NO_CHANGES_TITLE),
                        preferencesService.translate(
                                Constants.TranslationKeys.CREDITCARD_DIALOG_NO_CHANGES_MESSAGE));
            } else // If there is any modification, update the credit card
            {
                creditCard.setName(crcName);
                creditCard.setMaxDebt(crcLimit);
                creditCard.setLastFourDigits(crcLastFourDigitsStr);
                creditCard.setClosingDay(crcClosingDay);
                creditCard.setBillingDueDay(crcDueDay);
                creditCard.setOperator(crcOperator);
                creditCard.setDefaultBillingWallet(crcDefaultBillingWallet);

                creditCardService.updateCreditCard(creditCard);

                WindowUtils.showSuccessDialog(
                        preferencesService.translate(
                                Constants.TranslationKeys.CREDITCARD_DIALOG_UPDATED_TITLE),
                        preferencesService.translate(
                                Constants.TranslationKeys.CREDITCARD_DIALOG_UPDATED_MESSAGE));
            }

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_INVALID_LIMIT_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_INVALID_LIMIT_MESSAGE));
        } catch (EntityNotFoundException | IllegalArgumentException | IllegalStateException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_ERROR_CREATING_TITLE),
                    e.getMessage());
        }
    }
}
