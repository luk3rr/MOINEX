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
import org.moinex.service.WalletService;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Credit Card dialog
 */
@Controller
@NoArgsConstructor
public final class AddCreditCardController extends BaseCreditCardManagement {
    /**
     * Constructor
     * @param creditCardService The credit card service
     * @param walletService The wallet service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddCreditCardController(
            CreditCardService creditCardService, WalletService walletService) {
        super(creditCardService, walletService);
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
                    "Empty fields", "Please fill all required fields before saving");

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
                    "Credit card created", "The credit card was successfully created");

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog("Invalid limit", "Please enter a valid limit");
        } catch (EntityExistsException | EntityNotFoundException | IllegalArgumentException e) {
            WindowUtils.showErrorDialog("Error creating credit card", e.getMessage());
        }
    }
}
