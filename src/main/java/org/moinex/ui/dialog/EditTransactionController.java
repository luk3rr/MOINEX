/*
 * Filename: EditTransactionController.java
 * Created on: October 18, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import org.moinex.entities.WalletTransaction;
import org.moinex.services.CategoryService;
import org.moinex.services.WalletService;
import org.moinex.services.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Transaction dialog
 */
@Controller
@NoArgsConstructor
public class EditTransactionController
{
    @FXML
    private Label walletAfterBalanceValueLabel;

    @FXML
    private Label walletCurrentBalanceValueLabel;

    @FXML
    private ComboBox<Wallet> walletComboBox;

    @FXML
    private ComboBox<TransactionType> typeComboBox;

    @FXML
    private ComboBox<TransactionStatus> statusComboBox;

    @FXML
    private ComboBox<Category> categoryComboBox;

    @FXML
    private TextField transactionValueField;

    @FXML
    private TextField descriptionField;

    @FXML
    private DatePicker transactionDatePicker;

    private WalletService walletService;

    private WalletTransactionService walletTransactionService;

    private CategoryService categoryService;

    private List<Wallet> wallets;

    private List<Category> categories;

    private WalletTransaction walletTransaction = null;

    /**
     * Constructor
     * @param walletService WalletService
     * @param walletTransactionService WalletTransactionService
     * @param categoryService CategoryService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public EditTransactionController(WalletService            walletService,
                                     WalletTransactionService walletTransactionService,
                                     CategoryService          categoryService)
    {
        this.walletService            = walletService;
        this.walletTransactionService = walletTransactionService;
        this.categoryService          = categoryService;
    }

    public void setTransaction(WalletTransaction wt)
    {
        walletTransaction = wt;
        walletComboBox.setValue(walletTransaction.getWallet());
        statusComboBox.setValue(walletTransaction.getStatus());
        categoryComboBox.setValue(walletTransaction.getCategory());
        transactionValueField.setText(walletTransaction.getAmount().toString());
        descriptionField.setText(walletTransaction.getDescription());
        transactionDatePicker.setValue(walletTransaction.getDate().toLocalDate());
        typeComboBox.setValue(walletTransaction.getType());

        updateWalletBalance();
        walletAfterBalance();
    }

    @FXML
    private void initialize()
    {
        configureComboBoxes();

        loadWalletsFromDatabase();
        loadCategoriesFromDatabase();

        populateComboBoxes();

        UIUtils.setDatePickerFormat(transactionDatePicker);

        // Reset all labels
        UIUtils.resetLabel(walletAfterBalanceValueLabel);
        UIUtils.resetLabel(walletCurrentBalanceValueLabel);

        walletComboBox.setOnAction(e -> {
            updateWalletBalance();
            walletAfterBalance();
        });

        typeComboBox.setOnAction(e -> walletAfterBalance());

        statusComboBox.setOnAction(e -> walletAfterBalance());

        transactionValueField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
                {
                    transactionValueField.setText(oldValue);
                }
                else
                {
                    walletAfterBalance();
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
        Wallet            wallet                 = walletComboBox.getValue();
        TransactionType   type                   = typeComboBox.getValue();
        String            description            = descriptionField.getText().trim();
        String            transactionValueString = transactionValueField.getText();
        TransactionStatus status                 = statusComboBox.getValue();
        Category          category               = categoryComboBox.getValue();
        LocalDate         transactionDate        = transactionDatePicker.getValue();

        if (wallet == null || type == null || description == null ||
            transactionValueString == null || status == null || category == null ||
            transactionDate == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");
            return;
        }

        try
        {
            BigDecimal transactionValue = new BigDecimal(transactionValueString);

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = transactionDate.atTime(currentTime);

            // Check if has any modification
            if (wallet.getName().equals(walletTransaction.getWallet().getName()) &&
                category.getName().equals(walletTransaction.getCategory().getName()) &&
                transactionValue.compareTo(walletTransaction.getAmount()) == 0 &&
                description.equals(walletTransaction.getDescription()) &&
                status == walletTransaction.getStatus() &&
                type == walletTransaction.getType() &&
                dateTimeWithCurrentHour.toLocalDate().equals(
                    walletTransaction.getDate().toLocalDate()))
            {
                WindowUtils.showInformationDialog(
                    "No changes",
                    "No changes were made to the transaction.");
            }
            else // If there is any modification, update the transaction
            {
                walletTransaction.setWallet(wallet);
                walletTransaction.setCategory(category);
                walletTransaction.setDate(dateTimeWithCurrentHour);
                walletTransaction.setAmount(transactionValue);
                walletTransaction.setDescription(description);
                walletTransaction.setStatus(status);
                walletTransaction.setType(type);

                walletTransactionService.updateTransaction(walletTransaction);

                WindowUtils.showSuccessDialog("Transaction updated",
                                              "Transaction updated successfully.");
            }

            Stage stage = (Stage)descriptionField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid income value",
                                        "Income value must be a number.");
        }
        catch (EntityNotFoundException | IllegalArgumentException |
               IllegalStateException e)
        {
            WindowUtils.showErrorDialog("Error while updating transaction",
                                        e.getMessage());
        }
    }

    private void updateWalletBalance()
    {
        Wallet wallet = walletComboBox.getValue();

        if (wallet == null)
        {
            return;
        }

        walletCurrentBalanceValueLabel.setText(
            UIUtils.formatCurrency(wallet.getBalance()));
    }

    private void walletAfterBalance()
    {
        String          transactionValueString = transactionValueField.getText();
        TransactionType type                   = typeComboBox.getValue();
        Wallet          wallet                 = walletComboBox.getValue();

        if (transactionValueString == null || transactionValueString.trim().isEmpty() ||
            wallet == null || type == null)
        {
            UIUtils.resetLabel(walletAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal transactionValue = new BigDecimal(transactionValueString);

            if (transactionValue.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(walletAfterBalanceValueLabel);
                return;
            }

            BigDecimal walletAfterBalanceValue = BigDecimal.ZERO;

            if (walletTransaction.getStatus().equals(TransactionStatus.CONFIRMED))
            {
                // If the transaction is confirmed, the balance will be updated
                // based on the difference between the new and the old value
                BigDecimal diff =
                    transactionValue.subtract(walletTransaction.getAmount());

                if (type.equals(TransactionType.EXPENSE))
                {
                    walletAfterBalanceValue = wallet.getBalance().subtract(diff);
                }
                else if (type.equals(TransactionType.INCOME))
                {
                    walletAfterBalanceValue = wallet.getBalance().add(diff);
                }
                else
                {
                    UIUtils.resetLabel(walletAfterBalanceValueLabel);
                    return;
                }
            }
            else
            {
                // If the transaction is not confirmed, the balance will be
                // updated based on the new value
                if (type.equals(TransactionType.EXPENSE))
                {
                    walletAfterBalanceValue =
                        wallet.getBalance().subtract(transactionValue);
                }
                else if (type.equals(TransactionType.INCOME))
                {
                    walletAfterBalanceValue = wallet.getBalance().add(transactionValue);
                }
                else
                {
                    UIUtils.resetLabel(walletAfterBalanceValueLabel);
                    return;
                }
            }

            // Episilon is used to avoid floating point arithmetic errors
            if (walletAfterBalanceValue.compareTo(BigDecimal.ZERO) < 0)
            {
                // Remove old style and add negative style
                UIUtils.setLabelStyle(walletAfterBalanceValueLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                UIUtils.setLabelStyle(walletAfterBalanceValueLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            walletAfterBalanceValueLabel.setText(
                UIUtils.formatCurrency(walletAfterBalanceValue));
        }
        catch (NumberFormatException e)
        {
            UIUtils.resetLabel(walletAfterBalanceValueLabel);
        }
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
        typeComboBox.getItems().addAll(Arrays.asList(TransactionType.values()));
        statusComboBox.getItems().addAll(Arrays.asList(TransactionStatus.values()));
        categoryComboBox.getItems().addAll(categories);

        // If there are no categories, add a tooltip to the categoryComboBox
        // to inform the user that a category is needed
        if (categories.isEmpty())
        {
            UIUtils.addTooltipToNode(
                categoryComboBox,
                "You need to add a category before editing a transaction");
        }
    }

    private void configureComboBoxes()
    {
        UIUtils.configureComboBox(walletComboBox, Wallet::getName);
        UIUtils.configureComboBox(typeComboBox, TransactionType::name);
        UIUtils.configureComboBox(statusComboBox, TransactionStatus::name);
        UIUtils.configureComboBox(categoryComboBox, Category::getName);
    }
}
