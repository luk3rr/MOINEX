/*
 * Filename: AddRecurringTransactionController.java
 * Created on: November 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.moinex.entities.Category;
import org.moinex.entities.Wallet;
import org.moinex.services.CategoryService;
import org.moinex.services.RecurringTransactionService;
import org.moinex.services.WalletService;
import org.moinex.services.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.RecurringTransactionFrequency;
import org.moinex.util.RecurringTransactionStatus;
import org.moinex.util.TransactionType;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Recurring Transaction dialog
 */
@Controller
public class AddRecurringTransactionController
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
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Label infoLabel;

    private WalletService walletService;

    private RecurringTransactionService recurringTransactionService;

    private CategoryService categoryService;

    private List<Wallet> wallets;

    private List<Category> categories;

    public AddRecurringTransactionController() { }

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
        this.walletService               = walletService;
        this.recurringTransactionService = recurringTransactionService;
        this.categoryService             = categoryService;
    }

    @FXML
    private void initialize()
    {
        LoadWallets();
        LoadCategories();

        // Configure date picker
        UIUtils.SetDatePickerFormat(startDatePicker);
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

        startDatePicker.setOnAction(e -> { UpdateInfoLabel(); });

        endDatePicker.setOnAction(e -> { UpdateInfoLabel(); });

        frequencyComboBox.setOnAction(e -> { UpdateInfoLabel(); });

        // Check if the value field is a valid monetary value
        valueField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
            {
                valueField.setText(oldValue);
            }
        });
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
        LocalDate startDate       = startDatePicker.getValue();
        LocalDate endDate         = endDatePicker.getValue();
        String    frequencyString = frequencyComboBox.getValue();

        if (walletName == null || description == null ||
            description.strip().isEmpty() || valueString == null ||
            valueString.strip().isEmpty() || typeString == null ||
            categoryString == null || startDate == null || frequencyString == null)
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

            if (endDate == null)
            {
                recurringTransactionService.CreateRecurringTransaction(
                    wallet.GetId(),
                    category,
                    type,
                    transactionAmount,
                    startDate,
                    description,
                    frequency);
            }
            else
            {
                recurringTransactionService.CreateRecurringTransaction(
                    wallet.GetId(),
                    category,
                    type,
                    transactionAmount,
                    startDate,
                    endDate,
                    description,
                    frequency);
            }

            WindowUtils.ShowSuccessDialog("Success",
                                          "Income created",
                                          "The income was successfully created.");

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
                                        "Error while creating recurring transaction",
                                        e.getMessage());
        }
    }

    private void UpdateInfoLabel()
    {
        LocalDate startDate       = startDatePicker.getValue();
        LocalDate endDate         = endDatePicker.getValue();
        String    frequencyString = frequencyComboBox.getValue();

        String msg = "";

        if (startDate != null && frequencyString != null)
        {
            RecurringTransactionFrequency frequency =
                RecurringTransactionFrequency.valueOf(frequencyString);

            if (endDate != null)
            {
                msg = "Starts on " + startDate + ", ends on " + endDate +
                      ", frequency " + frequencyString;

                try
                {

                    msg +=
                        "\nLast transaction: " +
                        recurringTransactionService.GetLastTransactionDate(startDate,
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
                msg = "Starts on " + startDate + ", frequency " +
                      frequencyString;
            }
        }

        infoLabel.setText(msg);
    }

    private void LoadWallets()
    {
        wallets = walletService.GetAllWallets();

        walletComboBox.getItems().addAll(
            wallets.stream().map(Wallet::GetName).toList());
    }

    private void LoadCategories()
    {
        categories = categoryService.GetNonArchivedCategories();

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
