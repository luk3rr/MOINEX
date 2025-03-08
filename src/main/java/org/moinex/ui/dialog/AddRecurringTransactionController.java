/*
 * Filename: AddRecurringTransactionController.java
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.entities.Category;
import org.moinex.entities.Wallet;
import org.moinex.services.CategoryService;
import org.moinex.services.RecurringTransactionService;
import org.moinex.services.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
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
public class AddRecurringTransactionController
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
        configureComboBoxes();

        loadWalletsFromDatabase();
        loadCategoriesFromDatabase();

        populateComboBoxes();

        // Configure date picker
        UIUtils.setDatePickerFormat(startDatePicker);
        UIUtils.setDatePickerFormat(endDatePicker);

        startDatePicker.setOnAction(e -> updateInfoLabel());

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
        LocalDate                     startDate   = startDatePicker.getValue();
        LocalDate                     endDate     = endDatePicker.getValue();
        RecurringTransactionFrequency frequency   = frequencyComboBox.getValue();

        if (wallet == null || description == null || description.strip().isEmpty() ||
            valueString == null || valueString.strip().isEmpty() || type == null ||
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

    private void updateInfoLabel()
    {
        LocalDate                     startDate = startDatePicker.getValue();
        LocalDate                     endDate   = endDatePicker.getValue();
        RecurringTransactionFrequency frequency = frequencyComboBox.getValue();

        String msg = "";

        if (startDate != null && frequency != null)
        {
            if (endDate != null)
            {
                msg = "Starts on " + startDate + ", ends on " + endDate +
                      ", frequency " + frequency.toString();

                try
                {

                    msg +=
                        "\nLast transaction: " +
                        recurringTransactionService.getLastTransactionDate(startDate,
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
                msg = "Starts on " + startDate + ", frequency " + frequency.toString();
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
        walletComboBox.getItems().setAll(wallets);
        typeComboBox.getItems().setAll(Arrays.asList(TransactionType.values()));
        frequencyComboBox.getItems().setAll(
            Arrays.asList(RecurringTransactionFrequency.values()));
        categoryComboBox.getItems().setAll(categories);

        // If there are no categories, add a tooltip to the categoryComboBox
        // to inform the user that a category is needed
        if (categories.isEmpty())
        {
            UIUtils.addTooltipToNode(
                categoryComboBox,
                "You need to add a category before adding an recurring transaction");
        }
    }

    private void configureComboBoxes()
    {
        UIUtils.configureComboBox(walletComboBox, Wallet::getName);
        UIUtils.configureComboBox(typeComboBox, TransactionType::name);
        UIUtils.configureComboBox(categoryComboBox, Category::getName);
    }
}
