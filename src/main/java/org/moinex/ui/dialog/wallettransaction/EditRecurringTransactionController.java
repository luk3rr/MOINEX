/*
 * Filename: EditRecurringTransactionController.java
 * Created on: November 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.wallettransaction.RecurringTransaction;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.CategoryService;
import org.moinex.service.RecurringTransactionService;
import org.moinex.service.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.RecurringTransactionFrequency;
import org.moinex.util.enums.RecurringTransactionStatus;
import org.moinex.util.enums.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Recurring Transaction dialog
 */
@Controller
@NoArgsConstructor
public final class EditRecurringTransactionController
    extends BaseRecurringTransactionManagement
{
    @FXML
    private CheckBox activeCheckBox;

    private RecurringTransaction rt = null;

    /**
     * Constructor
     * @param walletService WalletService
     * @param recurringTransactionService RecurringTransactionService
     * @param categoryService CategoryService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public EditRecurringTransactionController(
        WalletService               walletService,
        RecurringTransactionService recurringTransactionService,
        CategoryService             categoryService)
    {
        super(walletService, recurringTransactionService, categoryService);
    }

    public void setRecurringTransaction(RecurringTransaction rt)
    {
        this.rt = rt;
        walletComboBox.setValue(rt.getWallet());
        descriptionField.setText(rt.getDescription());
        valueField.setText(rt.getAmount().toString());
        typeComboBox.setValue(rt.getType());
        categoryComboBox.setValue(rt.getCategory());

        startDatePicker.setValue(rt.getNextDueDate().toLocalDate());

        if (rt.getEndDate().toLocalDate().equals(
                Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE))
        {
            endDatePicker.setValue(null);
        }
        else
        {
            endDatePicker.setValue(rt.getEndDate().toLocalDate());
        }

        frequencyComboBox.setValue(rt.getFrequency());

        activeCheckBox.setSelected(rt.getStatus() == RecurringTransactionStatus.ACTIVE);

        updateInfoLabel();
    }

    @FXML
    protected void initialize()
    {
        super.initialize();

        activeCheckBox.setOnAction(e -> updateInfoLabel());
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
        LocalDate                     nextDueDate = startDatePicker.getValue();
        LocalDate                     endDate     = endDatePicker.getValue();
        RecurringTransactionFrequency frequency   = frequencyComboBox.getValue();

        if (wallet == null || description == null || description.isBlank() ||
            valueString == null || valueString.isBlank() || type == null ||
            category == null || nextDueDate == null || frequency == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");
            return;
        }

        try
        {
            BigDecimal transactionAmount = new BigDecimal(valueString);

            boolean endDateChanged =
                (endDate != null && !endDate.equals(rt.getEndDate().toLocalDate())) ||
                (endDate == null &&
                 !rt.getEndDate().toLocalDate().equals(
                     Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE));

            // Check if it has any modification
            if (rt.getWallet().getId().equals(wallet.getId()) &&
                rt.getDescription().equals(description) &&
                rt.getAmount().compareTo(transactionAmount) == 0 &&
                rt.getType().equals(type) &&
                rt.getCategory().getId().equals(category.getId()) &&
                rt.getNextDueDate().toLocalDate().equals(nextDueDate) &&
                !endDateChanged && rt.getFrequency().equals(frequency) &&
                rt.getStatus().equals(activeCheckBox.isSelected()
                                          ? RecurringTransactionStatus.ACTIVE
                                          : RecurringTransactionStatus.INACTIVE))
            {
                WindowUtils.showInformationDialog(
                    "No changes",
                    "No changes were made to the transaction.");
            }
            else // If there is any modification, update the transaction
            {
                rt.setWallet(wallet);
                rt.setDescription(description);
                rt.setAmount(transactionAmount);
                rt.setType(type);
                rt.setCategory(category);
                rt.setNextDueDate(
                    nextDueDate.atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME));

                // If the end date not set, set the default end date
                endDate = endDate == null
                              ? Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE
                              : endDate;

                rt.setEndDate(
                    endDate.atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME));
                rt.setFrequency(frequency);
                rt.setStatus(activeCheckBox.isSelected()
                                 ? RecurringTransactionStatus.ACTIVE
                                 : RecurringTransactionStatus.INACTIVE);

                recurringTransactionService.updateRecurringTransaction(rt);

                WindowUtils.showSuccessDialog(
                    "Recurring transaction updated",
                    "Recurring transaction updated successfully.");
            }

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
            WindowUtils.showErrorDialog("Error while editing recurring transaction",
                                        e.getMessage());
        }
    }

    protected void updateInfoLabel()
    {
        if (!activeCheckBox.isSelected())
        {
            infoLabel.setText("Recurring transaction is inactive");
        }
        else
        {
            super.updateInfoLabel();
        }
    }
}
