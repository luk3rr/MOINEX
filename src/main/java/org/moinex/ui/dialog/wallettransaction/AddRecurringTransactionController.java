/*
 * Filename: AddRecurringTransactionController.java
 * Created on: November 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.CategoryService;
import org.moinex.service.RecurringTransactionService;
import org.moinex.service.WalletService;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.RecurringTransactionFrequency;
import org.moinex.util.enums.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Recurring Transaction dialog
 */
@Controller
@NoArgsConstructor
public final class AddRecurringTransactionController
    extends BaseRecurringTransactionManagement
{
    /**
     * Constructor
     * @param walletService WalletService
     * @param recurringTransactionService RecurringTransactionService
     * @param categoryService CategoryService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddRecurringTransactionController(
        WalletService               walletService,
        RecurringTransactionService recurringTransactionService,
        CategoryService             categoryService)
    {
        super(walletService, recurringTransactionService, categoryService);
    }

    @FXML
    @Override
    protected void handleSave()
    {
        Wallet                        wallet      = walletComboBox.getValue();
        String                        description = descriptionField.getText();
        String                        valueString = valueField.getText();
        TransactionType               type        = typeComboBox.getValue();
        Category                      category    = categoryComboBox.getValue();
        LocalDate                     startDate   = startDatePicker.getValue();
        LocalDate                     endDate     = endDatePicker.getValue();
        RecurringTransactionFrequency frequency   = frequencyComboBox.getValue();

        if (wallet == null || description == null || description.isBlank() ||
            valueString == null || valueString.isBlank() || type == null ||
            category == null || startDate == null || frequency == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");
            return;
        }

        try
        {
            BigDecimal transactionAmount = new BigDecimal(valueString);

            if (endDate == null)
            {
                recurringTransactionService.addRecurringTransaction(wallet.getId(),
                                                                    category,
                                                                    type,
                                                                    transactionAmount,
                                                                    startDate,
                                                                    description,
                                                                    frequency);
            }
            else
            {
                recurringTransactionService.addRecurringTransaction(wallet.getId(),
                                                                    category,
                                                                    type,
                                                                    transactionAmount,
                                                                    startDate,
                                                                    endDate,
                                                                    description,
                                                                    frequency);
            }

            WindowUtils.showSuccessDialog(
                "Recurring transaction created",
                "Recurring transaction created successfully.");

            Stage stage = (Stage)descriptionField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid transaction value",
                                        "Transaction value must be a number.");
        }
        catch (EntityNotFoundException | IllegalArgumentException e)
        {
            WindowUtils.showErrorDialog("Error while creating recurring transaction",
                                        e.getMessage());
        }
    }
}
