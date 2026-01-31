/*
 * Filename: EditCreditCardDebtController.java
 * Created on: October 28, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.creditcard;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.creditcard.CreditCard;
import org.moinex.model.creditcard.CreditCardDebt;
import org.moinex.model.creditcard.CreditCardPayment;
import org.moinex.service.CalculatorService;
import org.moinex.service.CategoryService;
import org.moinex.service.CreditCardService;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Credit Card Debt dialog
 */
@Controller
@NoArgsConstructor
public final class EditCreditCardDebtController extends BaseCreditCardDebtManagement {
    private CreditCardDebt crcDebt = null;
    private I18nService i18nService;

    @Autowired
    public EditCreditCardDebtController(
            CategoryService categoryService,
            CreditCardService creditCardService,
            CalculatorService calculatorService,
            ConfigurableApplicationContext springContext,
            I18nService i18nService) {
        super(categoryService, creditCardService, calculatorService, springContext);
        this.i18nService = i18nService;
        setI18nService(i18nService);
    }

    public void setCreditCardDebt(CreditCardDebt crcDebt) {
        this.crcDebt = crcDebt;

        // Set the values of the expense to the fields
        crcComboBox.setValue(crcDebt.getCreditCard());
        crcLimitLabel.setText(UIUtils.formatCurrency(crcDebt.getCreditCard().getMaxDebt()));

        BigDecimal availableLimit =
                creditCardService.getAvailableCredit(crcDebt.getCreditCard().getId());

        crcAvailableLimitLabel.setText(UIUtils.formatCurrency(availableLimit));

        // The debt value has already been subtracted from the available limit, so
        // unless the user changes the debt value, the available limit after the
        // debt will be the same
        crcLimitAvailableAfterDebtLabel.setText(UIUtils.formatCurrency(availableLimit));

        // Deactivate the listener to avoid the event of changing the text of
        // the descriptionField from being triggered. After changing the text,
        // the listener is activated again
        suggestionsHandler.disable();
        descriptionField.setText(crcDebt.getDescription());
        suggestionsHandler.enable();

        valueField.setText(crcDebt.getAmount().toString());
        installmentsField.setText(crcDebt.getInstallments().toString());

        categoryComboBox.setValue(crcDebt.getCategory());

        CreditCardPayment firstPayment =
                creditCardService.getPaymentsByDebtId(crcDebt.getId()).getFirst();

        invoiceMonthComboBox.setValue(firstPayment.getDate().getMonthValue());
        invoiceYearComboBox.setValue(firstPayment.getDate().getYear());
    }

    @Override
    @FXML
    protected void handleSave() {
        CreditCard crc = crcComboBox.getValue();
        Category category = categoryComboBox.getValue();
        Integer invoiceMonth = invoiceMonthComboBox.getValue();
        Integer invoiceYear = invoiceYearComboBox.getValue();
        String description = descriptionField.getText().strip();
        String valueStr = valueField.getText();
        String installmentsStr = installmentsField.getText();

        if (crc == null
                || category == null
                || description.isEmpty()
                || valueStr.isEmpty()
                || invoiceMonth == null
                || invoiceYear == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(Constants.TranslationKeys.CREDITCARD_DIALOG_EMPTY_FIELDS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_EMPTY_FIELDS_MESSAGE));
            return;
        }

        try {
            BigDecimal debtValue = new BigDecimal(valueStr);

            Integer installments =
                    installmentsStr.isEmpty() ? 1 : Integer.parseInt(installmentsStr);

            // Get the date of the first payment to check if the invoice month is the
            // same
            CreditCardPayment firstPayment =
                    creditCardService.getPaymentsByDebtId(crcDebt.getId()).getFirst();

            YearMonth invoice =
                    YearMonth.of(
                            firstPayment.getDate().getYear(),
                            firstPayment.getDate().getMonthValue());

            YearMonth invoiceDateYearMonth = YearMonth.of(invoiceYear, invoiceMonth);

            // Check if it has any modification
            if (crcDebt.getCreditCard().getId().equals(crc.getId())
                    && crcDebt.getCategory().getId().equals(category.getId())
                    && debtValue.compareTo(crcDebt.getAmount()) == 0
                    && crcDebt.getInstallments().equals(installments)
                    && crcDebt.getDescription().equals(description)
                    && invoice.equals(invoiceDateYearMonth)) {
                WindowUtils.showInformationDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.CREDITCARD_DIALOG_NO_CHANGES_DEBT_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CREDITCARD_DIALOG_NO_CHANGES_DEBT_MESSAGE));
            } else // If there is any modification, update the debt
            {
                crcDebt.setCreditCard(crc);
                crcDebt.setCategory(category);
                crcDebt.setDescription(description);
                crcDebt.setAmount(debtValue);
                crcDebt.setInstallments(installments);

                creditCardService.updateCreditCardDebt(crcDebt, invoiceDateYearMonth);

                WindowUtils.showSuccessDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CREDITCARD_DIALOG_TRANSACTION_UPDATED_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CREDITCARD_DIALOG_TRANSACTION_UPDATED_MESSAGE));
            }

            Stage stage = (Stage) crcComboBox.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.CREDITCARD_DIALOG_INVALID_VALUE_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_INVALID_VALUE_MESSAGE));
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_ERROR_CREATING_DEBT_TITLE),
                    e.getMessage());
        }
    }

    @Override
    protected void updateAvailableLimitAfterDebtLabel() {
        if (crcComboBox.getValue() == null) {
            return;
        }

        CreditCard newCrc =
                creditCards.stream()
                        .filter(c -> c.getId().equals(crcComboBox.getValue().getId()))
                        .findFirst()
                        .orElse(null);

        if (newCrc == null) {
            return;
        }

        String value = valueField.getText();

        if (value.isEmpty()) {
            UIUtils.resetLabel(crcLimitAvailableAfterDebtLabel);
            return;
        }

        BigDecimal newAmount = new BigDecimal(valueField.getText());

        if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            UIUtils.resetLabel(msgLabel);
            return;
        }

        CreditCard oldCrc = crcDebt.getCreditCard();
        BigDecimal oldAmount = crcDebt.getAmount();

        try {
            BigDecimal availableLimitAfterDebt;

            if (oldCrc.getId().equals(newCrc.getId())) {
                if (oldAmount.compareTo(newAmount) < 0) // Amount increased
                {
                    BigDecimal diff = newAmount.subtract(oldAmount).abs();

                    availableLimitAfterDebt =
                            creditCardService.getAvailableCredit(newCrc.getId()).subtract(diff);
                } else // Amount decreased
                {
                    availableLimitAfterDebt =
                            creditCardService
                                    .getAvailableCredit(newCrc.getId())
                                    .add(oldAmount.subtract(newAmount));
                }
            } else {
                List<CreditCardPayment> payments =
                        creditCardService.getPaymentsByDebtId(crcDebt.getId());

                BigDecimal paidAmount =
                        payments.stream()
                                .filter(p -> p.getWallet() != null)
                                .map(CreditCardPayment::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal remainingAmountToPay = newAmount.subtract(paidAmount);

                availableLimitAfterDebt =
                        creditCardService
                                .getAvailableCredit(newCrc.getId())
                                .subtract(remainingAmountToPay);
            }

            // Set the style according to the balance value after the expense
            if (availableLimitAfterDebt.compareTo(BigDecimal.ZERO) < 0) {
                UIUtils.setLabelStyle(
                        crcLimitAvailableAfterDebtLabel, Constants.NEGATIVE_BALANCE_STYLE);
            } else {
                UIUtils.setLabelStyle(
                        crcLimitAvailableAfterDebtLabel, Constants.NEUTRAL_BALANCE_STYLE);
            }

            crcLimitAvailableAfterDebtLabel.setText(
                    UIUtils.formatCurrency(availableLimitAfterDebt));
        } catch (NumberFormatException e) {
            UIUtils.resetLabel(crcLimitAvailableAfterDebtLabel);
        }
    }
}
