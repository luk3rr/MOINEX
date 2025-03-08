/*
 * Filename: EditRecurringTransactionController.java
 * Created on: November 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.entities.Category;
import org.moinex.entities.RecurringTransaction;
import org.moinex.entities.Wallet;
import org.moinex.services.CategoryService;
import org.moinex.services.RecurringTransactionService;
import org.moinex.services.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.RecurringTransactionFrequency;
import org.moinex.util.RecurringTransactionStatus;
import org.moinex.util.TransactionType;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Recurring Transaction dialog
 */
@Controller
@NoArgsConstructor
public class EditRecurringTransactionController
{
    @FXML
    private ComboBox<String> walletComboBox;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField valueField;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private ComboBox<String> frequencyComboBox;

    @FXML
    private DatePicker nextDueDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private CheckBox activeCheckBox;

    @FXML
    private Label infoLabel;

    private WalletService walletService;

    private RecurringTransactionService recurringTransactionService;

    private CategoryService categoryService;

    private List<Wallet> wallets;

    private List<Category> categories;

    private RecurringTransaction rtToUpdate;

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
        this.walletService               = walletService;
        this.recurringTransactionService = recurringTransactionService;
        this.categoryService             = categoryService;
    }

    public void setRecurringTransaction(RecurringTransaction rt)
    {
        rtToUpdate = rt;

        walletComboBox.setValue(rt.getWallet().getName());
        descriptionField.setText(rt.getDescription());
        valueField.setText(rt.getAmount().toString());
        typeComboBox.setValue(rt.getType().name());
        categoryComboBox.setValue(rt.getCategory().getName());

        nextDueDatePicker.setValue(rt.getNextDueDate().toLocalDate());

        if (rt.getEndDate().toLocalDate().equals(
                Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE))
        {
            endDatePicker.setValue(null);
        }
        else
        {
            endDatePicker.setValue(rt.getEndDate().toLocalDate());
        }

        frequencyComboBox.setValue(rt.getFrequency().name());

        activeCheckBox.setSelected(rt.getStatus() == RecurringTransactionStatus.ACTIVE);

        updateInfoLabel();
    }

    @FXML
    private void initialize()
    {
        loadWalletsFromDatabase();
        loadCategoriesFromDatabase();

        // Configure date picker
        UIUtils.setDatePickerFormat(nextDueDatePicker);
        UIUtils.setDatePickerFormat(endDatePicker);

        // For each element in enum RecurringTransactionStatus, add its name to the
        // typeComboBox
        typeComboBox.getItems().addAll(
            Arrays.stream(TransactionType.values()).map(Enum::name).toList());

        // For each element in enum RecurringTransactionFrequency, add its name to the
        // frequencyComboBox
        frequencyComboBox.getItems().addAll(
            Arrays.stream(RecurringTransactionFrequency.values())
                .map(Enum::name)
                .toList());

        nextDueDatePicker.setOnAction(e -> updateInfoLabel());

        endDatePicker.setOnAction(e -> updateInfoLabel());

        frequencyComboBox.setOnAction(e -> updateInfoLabel());

        // Check if the value field is a valid monetary value
        valueField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
            {
                valueField.setText(oldValue);
            }
        });

        // Allow the user to set the value to null if the text is erased
        endDatePicker.getEditor().textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue == null || newValue.trim().isEmpty())
                {
                    endDatePicker.setValue(null);
                }
            });

        activeCheckBox.setOnAction(e -> updateInfoLabel());
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)descriptionField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSave()
    {
        String    walletName      = walletComboBox.getValue();
        String    description     = descriptionField.getText();
        String    valueString     = valueField.getText();
        String    typeString      = typeComboBox.getValue();
        String    categoryString  = categoryComboBox.getValue();
        LocalDate nextDueDate     = nextDueDatePicker.getValue();
        LocalDate endDate         = endDatePicker.getValue();
        String    frequencyString = frequencyComboBox.getValue();

        if (walletName == null || description == null ||
            description.strip().isEmpty() || valueString == null ||
            valueString.strip().isEmpty() || typeString == null ||
            categoryString == null || nextDueDate == null || frequencyString == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");
            return;
        }

        try
        {
            BigDecimal transactionAmount = new BigDecimal(valueString);

            Wallet wallet = wallets.stream()
                                .filter(w -> w.getName().equals(walletName))
                                .findFirst()
                                .orElseThrow(()
                                                 -> new EntityNotFoundException(
                                                     "Wallet with name: " + walletName +
                                                     " not found"));

            Category category =
                categories.stream()
                    .filter(c -> c.getName().equals(categoryString))
                    .findFirst()
                    .orElseThrow(()
                                     -> new EntityNotFoundException(
                                         "Category with name: " + categoryString +
                                         " not found"));

            TransactionType type = TransactionType.valueOf(typeString);

            RecurringTransactionFrequency frequency =
                RecurringTransactionFrequency.valueOf(frequencyString);

            boolean endDateChanged =
                (endDate != null &&
                 !endDate.equals(rtToUpdate.getEndDate().toLocalDate())) ||
                (endDate == null &&
                 !rtToUpdate.getEndDate().toLocalDate().equals(
                     Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE));

            // Check if has any modification
            if (rtToUpdate.getWallet().getName().equals(walletName) &&
                rtToUpdate.getDescription().equals(description) &&
                rtToUpdate.getAmount().compareTo(transactionAmount) == 0 &&
                rtToUpdate.getType().equals(type) &&
                rtToUpdate.getCategory().getName().equals(categoryString) &&
                rtToUpdate.getNextDueDate().toLocalDate().equals(nextDueDate) &&
                !endDateChanged && rtToUpdate.getFrequency().equals(frequency) &&
                rtToUpdate.getStatus().equals(
                    activeCheckBox.isSelected() ? RecurringTransactionStatus.ACTIVE
                                                : RecurringTransactionStatus.INACTIVE))
            {
                WindowUtils.showInformationDialog(
                    "No changes",
                    "No changes were made to the transaction.");
            }
            else // If there is any modification, update the transaction
            {
                rtToUpdate.setWallet(wallet);
                rtToUpdate.setDescription(description);
                rtToUpdate.setAmount(transactionAmount);
                rtToUpdate.setType(type);
                rtToUpdate.setCategory(category);
                rtToUpdate.setNextDueDate(
                    nextDueDate.atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME));

                // If the end date not set, set the default end date
                endDate = endDate == null
                              ? Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE
                              : endDate;

                rtToUpdate.setEndDate(
                    endDate.atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME));
                rtToUpdate.setFrequency(frequency);
                rtToUpdate.setStatus(activeCheckBox.isSelected()
                                         ? RecurringTransactionStatus.ACTIVE
                                         : RecurringTransactionStatus.INACTIVE);

                recurringTransactionService.updateRecurringTransaction(rtToUpdate);

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

    private void updateInfoLabel()
    {
        LocalDate nextDueDate     = nextDueDatePicker.getValue();
        LocalDate endDate         = endDatePicker.getValue();
        String    frequencyString = frequencyComboBox.getValue();

        String msg = "";

        if (!activeCheckBox.isSelected())
        {
            msg = "Recurring transaction is inactive";
        }
        else if (nextDueDate != null && frequencyString != null)
        {
            RecurringTransactionFrequency frequency =
                RecurringTransactionFrequency.valueOf(frequencyString);

            if (endDate != null)
            {
                msg = "Starts on " + nextDueDate + ", ends on " + endDate +
                      ", frequency " + frequencyString;

                try
                {

                    msg +=
                        "\nLast transaction: " +
                        recurringTransactionService.getLastTransactionDate(nextDueDate,
                                                                           endDate,
                                                                           frequency);
                }
                catch (IllegalArgumentException | IllegalStateException e)
                {
                    // Do nothing
                }
            }
            else
            {
                msg = "Starts on " + nextDueDate + ", frequency " + frequencyString;
            }
        }

        infoLabel.setText(msg);
    }

    private void loadWalletsFromDatabase()
    {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();

        walletComboBox.getItems().addAll(
            wallets.stream().map(Wallet::getName).toList());
    }

    private void loadCategoriesFromDatabase()
    {
        categories = categoryService.getNonArchivedCategoriesOrderedByName();

        categoryComboBox.getItems().addAll(
            categories.stream().map(Category::getName).toList());

        // If there are no categories, add a tooltip to the categoryComboBox
        // to inform the user that a category is needed
        if (categories.isEmpty())
        {
            UIUtils.addTooltipToNode(
                categoryComboBox,
                "You need to add a category before adding an transaction");
        }
    }
}
