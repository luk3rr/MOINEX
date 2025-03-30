/*
 * Filename: BaseRecurringTransactionManagement.java
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

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
import org.moinex.entities.wallettransaction.Wallet;
import org.moinex.services.CategoryService;
import org.moinex.services.RecurringTransactionService;
import org.moinex.services.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.enums.RecurringTransactionFrequency;
import org.moinex.util.enums.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Base class to manage recurring transactions
 */
@Controller
@NoArgsConstructor
public abstract class BaseRecurringTransactionManagement
{
    @FXML
    protected TextField descriptionField;

    @FXML
    protected TextField valueField;

    @FXML
    protected ComboBox<Wallet> walletComboBox;

    @FXML
    protected ComboBox<TransactionType> typeComboBox;

    @FXML
    protected ComboBox<Category> categoryComboBox;

    @FXML
    protected ComboBox<RecurringTransactionFrequency> frequencyComboBox;

    @FXML
    protected DatePicker startDatePicker;

    @FXML
    protected DatePicker endDatePicker;

    @FXML
    protected Label infoLabel;

    protected WalletService walletService;

    protected RecurringTransactionService recurringTransactionService;

    protected CategoryService categoryService;

    protected List<Wallet> wallets;

    protected List<Category> categories;

    /**
     * Constructor
     * @param walletService WalletService
     * @param recurringTransactionService RecurringTransactionService
     * @param categoryService CategoryService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    protected BaseRecurringTransactionManagement(
        WalletService               walletService,
        RecurringTransactionService recurringTransactionService,
        CategoryService             categoryService)
    {
        this.walletService               = walletService;
        this.recurringTransactionService = recurringTransactionService;
        this.categoryService             = categoryService;
    }

    @FXML
    protected void initialize()
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
    protected void handleCancel()
    {
        Stage stage = (Stage)descriptionField.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected abstract void handleSave();

    protected void updateInfoLabel()
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
                      ", frequency " + frequency;

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
                msg = "Starts on " + startDate + ", frequency " + frequency;
            }
        }

        infoLabel.setText(msg);
    }

    protected void loadWalletsFromDatabase()
    {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();
    }

    protected void loadCategoriesFromDatabase()
    {
        categories = categoryService.getNonArchivedCategoriesOrderedByName();
    }

    protected void populateComboBoxes()
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

    protected void configureComboBoxes()
    {
        UIUtils.configureComboBox(walletComboBox, Wallet::getName);
        UIUtils.configureComboBox(typeComboBox, TransactionType::name);
        UIUtils.configureComboBox(categoryComboBox, Category::getName);
    }
}
