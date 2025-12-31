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
import org.moinex.model.creditcard.CreditCardOperator;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.CreditCardService;
import org.moinex.service.I18nService;
import org.moinex.service.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Credit Card dialog
 */
@Controller
@NoArgsConstructor
public final class AddCreditCardController extends BaseCreditCardManagement {
    private I18nService i18nService;

    /**
     * Constructor
     * @param creditCardService The credit card service
     * @param walletService The wallet service
     * @param i18nService The internationalization service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddCreditCardController(
            CreditCardService creditCardService,
            WalletService walletService,
            I18nService i18nService) {
        super(creditCardService, walletService);
        this.i18nService = i18nService;
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
                    i18nService.tr(Constants.TranslationKeys.CREDITCARD_DIALOG_EMPTY_FIELDS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_EMPTY_FIELDS_MESSAGE));

            return;
        }

        try {
            BigDecimal crcLimit = new BigDecimal(crcLimitStr);

            Integer crcClosingDay = Integer.parseInt(crcClosingDayStr);
            Integer crcDueDay = Integer.parseInt(crcDueDayStr);

            Integer crcDefaultBillingWalletId =
                    crcDefaultBillingWallet != null ? crcDefaultBillingWallet.getId() : null;

            creditCardService.addCreditCard(
                    crcName,
                    crcDueDay,
                    crcClosingDay,
                    crcLimit,
                    crcLastFourDigitsStr,
                    crcOperator.getId(),
                    crcDefaultBillingWalletId);

            WindowUtils.showSuccessDialog(
                    i18nService.tr(Constants.TranslationKeys.CREDITCARD_DIALOG_CREATED_TITLE),
                    i18nService.tr(Constants.TranslationKeys.CREDITCARD_DIALOG_CREATED_MESSAGE));

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.CREDITCARD_DIALOG_INVALID_LIMIT_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_INVALID_LIMIT_MESSAGE));
        } catch (EntityExistsException | EntityNotFoundException | IllegalArgumentException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_ERROR_CREATING_TITLE),
                    e.getMessage());
        }
    }
}
