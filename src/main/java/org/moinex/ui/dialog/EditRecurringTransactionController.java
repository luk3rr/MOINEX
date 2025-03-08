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
import org.moinex.util.UIUtils;
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
public class EditRecurringTransactionController
{
    @FXML
    private TextField descriptionField;

    @FXML
    private TextField valueField;

    @FXML
    private ComboBox<Wallet> walletComboBox;

    @FXML
    private ComboBox<TransactionType> typeComboBox;

    @FXML
    private ComboBox<Category> categoryComboBox;

    @FXML
    private ComboBox<RecurringTransactionFrequency> frequencyComboBox;

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
        this.walletService               = walletService;
        this.recurringTransactionService = recurringTransactionService;
        this.categoryService             = categoryService;
    }

    public void setRecurringTransaction(RecurringTransaction rt)
    {
        this.rt = rt;
        walletComboBox.setValue(rt.getWallet());
        descriptionField.setText(rt.getDescription());
        valueField.setText(rt.getAmount().toString());
        typeComboBox.setValue(rt.getType());
        categoryComboBox.setValue(rt.getCategory());

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

        frequencyComboBox.setValue(rt.getFrequency());

        activeCheckBox.setSelected(rt.getStatus() == RecurringTransactionStatus.ACTIVE);

        updateInfoLabel();
    }

    @FXML
    private void initialize()
    {
        configureComboBoxes();

        loadWalletsFromDatabase();
        loadCategoriesFromDatabase();

        populateComboBoxes();

        // Configure date picker
        UIUtils.setDatePickerFormat(nextDueDatePicker);
        UIUtils.setDatePickerFormat(endDatePicker);

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
        Wallet                        wallet      = walletComboBox.getValue();
        String                        description = descriptionField.getText();
        String                        valueString = valueField.getText();
        TransactionType               type        = typeComboBox.getValue();
        Category                      category    = categoryComboBox.getValue();
        LocalDate                     nextDueDate = nextDueDatePicker.getValue();
        LocalDate                     endDate     = endDatePicker.getValue();
        RecurringTransactionFrequency frequency   = frequencyComboBox.getValue();

        if (wallet == null || description == null || description.strip().isEmpty() ||
            valueString == null || valueString.strip().isEmpty() || type == null ||
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

            // Check if has any modification
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

    private void updateInfoLabel()
    {
        LocalDate                     nextDueDate = nextDueDatePicker.getValue();
        LocalDate                     endDate     = endDatePicker.getValue();
        RecurringTransactionFrequency frequency   = frequencyComboBox.getValue();

        String msg = "";

        if (!activeCheckBox.isSelected())
        {
            msg = "Recurring transaction is inactive";
        }
        else if (nextDueDate != null && frequency != null)
        {
            if (endDate != null)
            {
                msg = "Starts on " + nextDueDate + ", ends on " + endDate +
                      ", frequency " + frequency.toString();

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
                msg =
                    "Starts on " + nextDueDate + ", frequency " + frequency.toString();
            }
        }

        infoLabel.setText(msg);
    }

    private void loadWalletsFromDatabase()
    {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();
    }

    private void loadCategoriesFromDatabase()
    {
        categories = categoryService.getNonArchivedCategoriesOrderedByName();
    }

    private void populateComboBoxes()
    {
        walletComboBox.getItems().addAll(wallets);
        categoryComboBox.getItems().addAll(categories);
        typeComboBox.getItems().addAll(Arrays.asList(TransactionType.values()));
        frequencyComboBox.getItems().addAll(
            Arrays.asList(RecurringTransactionFrequency.values()));
    }

    private void configureComboBoxes()
    {
        UIUtils.configureComboBox(walletComboBox, Wallet::getName);
        UIUtils.configureComboBox(categoryComboBox, Category::getName);
        UIUtils.configureComboBox(typeComboBox, TransactionType::name);
        UIUtils.configureComboBox(frequencyComboBox,
                                  RecurringTransactionFrequency::name);
    }
}
