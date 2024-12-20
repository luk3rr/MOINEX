/*
 * Filename: EditRecurringTransactionController.java
 * Created on: November 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

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

    public EditRecurringTransactionController() { }

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

    public void SetRecurringTransaction(RecurringTransaction rt)
    {
        rtToUpdate = rt;

        walletComboBox.setValue(rt.GetWallet().GetName());
        descriptionField.setText(rt.GetDescription());
        valueField.setText(rt.GetAmount().toString());
        typeComboBox.setValue(rt.GetType().name());
        categoryComboBox.setValue(rt.GetCategory().GetName());

        nextDueDatePicker.setValue(rt.GetNextDueDate().toLocalDate());

        if (rt.GetEndDate().toLocalDate().equals(
                Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE))
        {
            endDatePicker.setValue(null);
        }
        else
        {
            endDatePicker.setValue(rt.GetEndDate().toLocalDate());
        }

        frequencyComboBox.setValue(rt.GetFrequency().name());

        activeCheckBox.setSelected(rt.GetStatus() == RecurringTransactionStatus.ACTIVE);

        UpdateInfoLabel();
    }

    @FXML
    private void initialize()
    {
        LoadWallets();
        LoadCategories();

        // Configure date picker
        UIUtils.SetDatePickerFormat(nextDueDatePicker);
        UIUtils.SetDatePickerFormat(endDatePicker);

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

        nextDueDatePicker.setOnAction(e -> { UpdateInfoLabel(); });

        endDatePicker.setOnAction(e -> { UpdateInfoLabel(); });

        frequencyComboBox.setOnAction(e -> { UpdateInfoLabel(); });

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

        activeCheckBox.setOnAction(e -> { UpdateInfoLabel(); });
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
            WindowUtils.ShowErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill the required fields.");
            return;
        }

        try
        {
            BigDecimal transactionAmount = new BigDecimal(valueString);

            Wallet wallet = wallets.stream()
                                .filter(w -> w.GetName().equals(walletName))
                                .findFirst()
                                .get();

            Category category = categories.stream()
                                    .filter(c -> c.GetName().equals(categoryString))
                                    .findFirst()
                                    .get();

            TransactionType type = TransactionType.valueOf(typeString);

            RecurringTransactionFrequency frequency =
                RecurringTransactionFrequency.valueOf(frequencyString);

            Boolean endDateChanged =
                (endDate != null &&
                 !endDate.equals(rtToUpdate.GetEndDate().toLocalDate())) ||
                (endDate == null &&
                 !rtToUpdate.GetEndDate().toLocalDate().equals(
                     Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE));

            // Check if has any modification
            if (rtToUpdate.GetWallet().GetName().equals(walletName) &&
                rtToUpdate.GetDescription().equals(description) &&
                rtToUpdate.GetAmount().compareTo(transactionAmount) == 0 &&
                rtToUpdate.GetType().equals(type) &&
                rtToUpdate.GetCategory().GetName().equals(categoryString) &&
                rtToUpdate.GetNextDueDate().toLocalDate().equals(nextDueDate) &&
                !endDateChanged && rtToUpdate.GetFrequency().equals(frequency) &&
                rtToUpdate.GetStatus().equals(
                    activeCheckBox.isSelected() ? RecurringTransactionStatus.ACTIVE
                                                : RecurringTransactionStatus.INACTIVE))
            {
                WindowUtils.ShowInformationDialog(
                    "Information",
                    "No changes",
                    "No changes were made to the transaction.");
            }
            else // If there is any modification, update the transaction
            {
                rtToUpdate.SetWallet(wallet);
                rtToUpdate.SetDescription(description);
                rtToUpdate.SetAmount(transactionAmount);
                rtToUpdate.SetType(type);
                rtToUpdate.SetCategory(category);
                rtToUpdate.SetNextDueDate(
                    nextDueDate.atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME));

                // If the end date not set, set the default end date
                endDate = endDate == null
                              ? Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE
                              : endDate;

                rtToUpdate.SetEndDate(
                    endDate.atTime(Constants.RECURRING_TRANSACTION_DEFAULT_TIME));
                rtToUpdate.SetFrequency(frequency);
                rtToUpdate.SetStatus(activeCheckBox.isSelected()
                                         ? RecurringTransactionStatus.ACTIVE
                                         : RecurringTransactionStatus.INACTIVE);

                recurringTransactionService.UpdateRecurringTransaction(rtToUpdate);

                WindowUtils.ShowSuccessDialog(
                    "Success",
                    "Recurring transaction updated",
                    "Recurring transaction updated successfully.");
            }

            Stage stage = (Stage)descriptionField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Invalid transaction value",
                                        "Transaction value must be a number.");
        }
        catch (RuntimeException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Error while editing recurring transaction",
                                        e.getMessage());
        }
    }

    private void UpdateInfoLabel()
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
                        recurringTransactionService.GetLastTransactionDate(nextDueDate,
                                                                           endDate,
                                                                           frequency);
                }
                catch (RuntimeException e)
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

    private void LoadWallets()
    {
        wallets = walletService.GetAllNonArchivedWalletsOrderedByName();

        walletComboBox.getItems().addAll(
            wallets.stream().map(Wallet::GetName).toList());
    }

    private void LoadCategories()
    {
        categories = categoryService.GetNonArchivedCategoriesOrderedByName();

        categoryComboBox.getItems().addAll(
            categories.stream().map(Category::GetName).toList());

        // If there are no categories, add a tooltip to the categoryComboBox
        // to inform the user that a category is needed
        if (categories.size() == 0)
        {
            UIUtils.AddTooltipToNode(
                categoryComboBox,
                "You need to add a category before adding an transaction");
        }
    }
}
