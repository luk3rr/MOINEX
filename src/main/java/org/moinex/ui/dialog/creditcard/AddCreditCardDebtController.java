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
import org.moinex.entities.Category;
import org.moinex.entities.creditcard.CreditCard;
import org.moinex.error.MoinexException;
import org.moinex.services.CalculatorService;
import org.moinex.services.CategoryService;
import org.moinex.services.CreditCardService;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Credit Card Debt dialog
 */
@Controller
@NoArgsConstructor
public final class AddCreditCardDebtController extends BaseCreditCardDebtManagement
{
    @Autowired
    public AddCreditCardDebtController(CategoryService   categoryService,
                                       CreditCardService creditCardService,
                                       CalculatorService calculatorService)
    {
        super(categoryService, creditCardService, calculatorService);
    }

    @Override
    @FXML
    protected void handleSave()
    {
        CreditCard crc             = crcComboBox.getValue();
        Category   category        = categoryComboBox.getValue();
        YearMonth  invoiceMonth    = invoiceComboBox.getValue();
        String     description     = descriptionField.getText().strip();
        String     valueStr        = valueField.getText();
        String     installmentsStr = installmentsField.getText();

        if (crc == null || category == null || description.isEmpty() ||
            valueStr.isEmpty() || invoiceMonth == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");
            return;
        }

        try
        {
            BigDecimal debtValue = new BigDecimal(valueStr);

            Integer installments =
                installmentsStr.isEmpty() ? 1 : Integer.parseInt(installmentsStr);

            creditCardService.addDebt(crc.getId(),
                                      category,
                                      LocalDateTime.now(), // register date
                                      invoiceMonth,
                                      debtValue,
                                      installments,
                                      description);

            WindowUtils.showSuccessDialog("Debt created", "Debt created successfully");

            Stage stage = (Stage)crcComboBox.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid expense value",
                                        "Debt value must be a number");
        }
        catch (EntityNotFoundException | IllegalArgumentException |
               MoinexException.InsufficientResourcesException e)
        {
            WindowUtils.showErrorDialog("Error creating debt", e.getMessage());
        }
    }
}
