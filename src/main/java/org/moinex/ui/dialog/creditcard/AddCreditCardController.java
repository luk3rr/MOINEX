/*
 * Filename: AddCreditCardController.java
 * Created on: October 24, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.creditcard;

import jakarta.persistence.EntityExistsException;
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

/** Controller for the Add Credit Card dialog */
@Controller
@NoArgsConstructor
public final class AddCreditCardController extends BaseCreditCardManagement {
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
    public AddCreditCardController(
            CreditCardService creditCardService,
            WalletService walletService,
            PreferencesService preferencesService) {
        super(creditCardService, walletService);
        this.preferencesService = preferencesService;
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

            creditCardService.createCreditCard(
                    new CreditCard(
                            null, // id (auto-generated)
                            crcOperator, // operator
                            crcDefaultBillingWallet, // defaultBillingWallet
                            crcName, // name
                            crcDueDay, // billingDueDay
                            crcClosingDay, // closingDay
                            crcLimit, // maxDebt
                            BigDecimal.ZERO, // availableRebate
                            crcLastFourDigitsStr, // lastFourDigits
                            false // isArchived
                            ));

            WindowUtils.showSuccessDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_CREATED_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_CREATED_MESSAGE));

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_INVALID_LIMIT_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_INVALID_LIMIT_MESSAGE));
        } catch (EntityExistsException | EntityNotFoundException | IllegalArgumentException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_ERROR_CREATING_TITLE),
                    e.getMessage());
        }
    }
}
