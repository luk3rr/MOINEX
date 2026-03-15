/*
 * Filename: AddCreditCardDebtController.java
 * Created on: October 25, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.creditcard;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.error.MoinexException;
import org.moinex.model.Category;
import org.moinex.model.creditcard.CreditCard;
import org.moinex.model.creditcard.CreditCardDebt;
import org.moinex.service.CalculatorService;
import org.moinex.service.CategoryService;
import org.moinex.service.PreferencesService;
import org.moinex.service.creditcard.CreditCardService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/** Controller for the Add Credit Card Debt dialog */
@Controller
@NoArgsConstructor
public final class AddCreditCardDebtController extends BaseCreditCardDebtManagement {
    private PreferencesService preferencesService;

    @Autowired
    public AddCreditCardDebtController(
            CategoryService categoryService,
            CreditCardService creditCardService,
            CalculatorService calculatorService,
            ConfigurableApplicationContext springContext,
            PreferencesService preferencesService) {
        super(categoryService, creditCardService, calculatorService, springContext);
        this.preferencesService = preferencesService;
        setPreferencesService(preferencesService);
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
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_EMPTY_FIELDS_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_EMPTY_FIELDS_MESSAGE));
            return;
        }

        try {
            BigDecimal debtValue = new BigDecimal(valueStr);

            int installments = installmentsStr.isEmpty() ? 1 : Integer.parseInt(installmentsStr);

            YearMonth invoiceDateYearMonth = YearMonth.of(invoiceYear, invoiceMonth);

            creditCardService.createDebt(
                    new CreditCardDebt(
                            null, // id (auto-generated)
                            category, // category
                            installments, // installments
                            crc, // creditCard
                            LocalDateTime.now(), // date
                            debtValue, // amount
                            description // description
                            ),
                    invoiceDateYearMonth);

            WindowUtils.showSuccessDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_DEBT_CREATED_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_DEBT_CREATED_MESSAGE));

            Stage stage = (Stage) crcComboBox.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_INVALID_VALUE_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_INVALID_VALUE_MESSAGE));
        } catch (EntityNotFoundException
                | IllegalArgumentException
                | MoinexException.InsufficientResourcesException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_ERROR_CREATING_DEBT_TITLE),
                    e.getMessage());
        }
    }
}
