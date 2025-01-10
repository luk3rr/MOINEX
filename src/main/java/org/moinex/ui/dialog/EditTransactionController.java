/*
 * Filename: EditTransactionController.java
 * Created on: October 18, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

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
import org.moinex.entities.Category;
import org.moinex.entities.Wallet;
import org.moinex.entities.WalletTransaction;
import org.moinex.services.CategoryService;
import org.moinex.services.WalletService;
import org.moinex.services.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.TransactionStatus;
import org.moinex.util.TransactionType;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Transaction dialog
 */
@Controller
public class EditTransactionController
{
    @FXML
    private Label walletAfterBalanceValueLabel;

    @FXML
    private Label walletCurrentBalanceValueLabel;

    @FXML
    private ComboBox<String> walletComboBox;

    @FXML
    private ComboBox<String> transactionTypeComboBox;
    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private ComboBox<String> categoryComboBox;

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

    private WalletTransaction transactionToUpdate;

    public EditTransactionController() { }

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

    public void SetTransaction(WalletTransaction wt)
    {
        transactionToUpdate = wt;

        walletComboBox.setValue(wt.GetWallet().GetName());
        statusComboBox.setValue(wt.GetStatus().toString());
        categoryComboBox.setValue(wt.GetCategory().GetName());
        transactionValueField.setText(wt.GetAmount().toString());
        descriptionField.setText(wt.GetDescription());
        transactionDatePicker.setValue(wt.GetDate().toLocalDate());
        transactionTypeComboBox.setValue(wt.GetType().toString());

        UpdateWalletBalance();
        WalletAfterBalance();
    }

    @FXML
    private void initialize()
    {
        LoadWallets();
        LoadCategories();

        UIUtils.SetDatePickerFormat(transactionDatePicker);

        // For each element in enum TransactionStatus, add its name to the
        // statusComboBox
        statusComboBox.getItems().addAll(
            Arrays.stream(TransactionStatus.values()).map(Enum::name).toList());

        // For each element in enum TransactionType, add its name to the
        // transactionTypeComboBox
        transactionTypeComboBox.getItems().addAll(
            Arrays.stream(TransactionType.values()).map(Enum::name).toList());

        // Reset all labels
        UIUtils.ResetLabel(walletAfterBalanceValueLabel);
        UIUtils.ResetLabel(walletCurrentBalanceValueLabel);

        walletComboBox.setOnAction(e -> {
            UpdateWalletBalance();
            WalletAfterBalance();
        });

        transactionTypeComboBox.setOnAction(e -> { WalletAfterBalance(); });

        statusComboBox.setOnAction(e -> { WalletAfterBalance(); });

        transactionValueField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
                {
                    transactionValueField.setText(oldValue);
                }
                else
                {
                    WalletAfterBalance();
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
        String    walletName             = walletComboBox.getValue();
        String    transactionTypeString  = transactionTypeComboBox.getValue();
        String    description            = descriptionField.getText().trim();
        String    transactionValueString = transactionValueField.getText();
        String    statusString           = statusComboBox.getValue();
        String    categoryString         = categoryComboBox.getValue();
        LocalDate transactionDate        = transactionDatePicker.getValue();

        if (walletName == null || transactionTypeString == null ||
            description == null || transactionValueString == null ||
            statusString == null || categoryString == null || transactionDate == null)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all the fields.");
            return;
        }

        try
        {
            BigDecimal transactionValue = new BigDecimal(transactionValueString);

            Wallet wallet = wallets.stream()
                                .filter(w -> w.GetName().equals(walletName))
                                .findFirst()
                                .get();

            Category category = categories.stream()
                                    .filter(c -> c.GetName().equals(categoryString))
                                    .findFirst()
                                    .get();

            TransactionStatus status = TransactionStatus.valueOf(statusString);
            TransactionType   type   = TransactionType.valueOf(transactionTypeString);

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = transactionDate.atTime(currentTime);

            // Check if has any modification
            if (wallet.GetName().equals(transactionToUpdate.GetWallet().GetName()) &&
                category.GetName().equals(
                    transactionToUpdate.GetCategory().GetName()) &&
                transactionValue.compareTo(transactionToUpdate.GetAmount()) == 0 &&
                description.equals(transactionToUpdate.GetDescription()) &&
                status == transactionToUpdate.GetStatus() &&
                type == transactionToUpdate.GetType() &&
                dateTimeWithCurrentHour.toLocalDate().equals(
                    transactionToUpdate.GetDate().toLocalDate()))
            {
                WindowUtils.ShowInformationDialog(
                    "Information",
                    "No changes",
                    "No changes were made to the transaction.");
            }
            else // If there is any modification, update the transaction
            {
                transactionToUpdate.SetWallet(wallet);
                transactionToUpdate.SetCategory(category);
                transactionToUpdate.SetDate(dateTimeWithCurrentHour);
                transactionToUpdate.SetAmount(transactionValue);
                transactionToUpdate.SetDescription(description);
                transactionToUpdate.SetStatus(status);
                transactionToUpdate.SetType(type);

                walletTransactionService.UpdateTransaction(transactionToUpdate);

                WindowUtils.ShowSuccessDialog("Success",
                                              "Transaction updated",
                                              "Transaction updated successfully.");
            }

            Stage stage = (Stage)descriptionField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Invalid income value",
                                        "Income value must be a number.");
        }
        catch (RuntimeException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Error while updating transaction",
                                        e.getMessage());
        }
    }

    private void UpdateWalletBalance()
    {
        String walletName = walletComboBox.getValue();

        if (walletName == null)
        {
            return;
        }

        Wallet wallet = wallets.stream()
                            .filter(w -> w.GetName().equals(walletName))
                            .findFirst()
                            .get();

        walletCurrentBalanceValueLabel.setText(
            UIUtils.FormatCurrency(wallet.GetBalance()));
    }

    private void WalletAfterBalance()
    {
        String transactionValueString = transactionValueField.getText();
        String transactionTypeString  = transactionTypeComboBox.getValue();
        String walletName             = walletComboBox.getValue();

        if (transactionValueString == null || transactionValueString.trim().isEmpty() ||
            walletName == null || transactionTypeString == null)
        {
            UIUtils.ResetLabel(walletAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal transactionValue = new BigDecimal(transactionValueString);

            if (transactionValue.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.ResetLabel(walletAfterBalanceValueLabel);
                return;
            }

            Wallet wallet = wallets.stream()
                                .filter(w -> w.GetName().equals(walletName))
                                .findFirst()
                                .get();

            BigDecimal walletAfterBalanceValue = BigDecimal.ZERO;

            if (transactionToUpdate.GetStatus().equals(TransactionStatus.CONFIRMED))
            {
                // If the transaction is confirmed, the balance will be updated
                // based on the difference between the new and the old value
                BigDecimal diff =
                    transactionValue.subtract(transactionToUpdate.GetAmount());

                if (transactionTypeString.equals(TransactionType.EXPENSE.toString()))
                {
                    walletAfterBalanceValue = wallet.GetBalance().subtract(diff);
                }
                else if (transactionTypeString.equals(
                             TransactionType.INCOME.toString()))
                {
                    walletAfterBalanceValue = wallet.GetBalance().add(diff);
                }
                else
                {
                    UIUtils.ResetLabel(walletAfterBalanceValueLabel);
                    return;
                }
            }
            else
            {
                // If the transaction is not confirmed, the balance will be
                // updated based on the new value
                if (transactionTypeString.equals(TransactionType.EXPENSE.toString()))
                {
                    walletAfterBalanceValue =
                        wallet.GetBalance().subtract(transactionValue);
                }
                else if (transactionTypeString.equals(
                             TransactionType.INCOME.toString()))
                {
                    walletAfterBalanceValue = wallet.GetBalance().add(transactionValue);
                }
                else
                {
                    UIUtils.ResetLabel(walletAfterBalanceValueLabel);
                    return;
                }
            }

            // Episilon is used to avoid floating point arithmetic errors
            if (walletAfterBalanceValue.compareTo(BigDecimal.ZERO) < 0)
            {
                // Remove old style and add negative style
                UIUtils.SetLabelStyle(walletAfterBalanceValueLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                UIUtils.SetLabelStyle(walletAfterBalanceValueLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            walletAfterBalanceValueLabel.setText(
                UIUtils.FormatCurrency(walletAfterBalanceValue));
        }
        catch (NumberFormatException e)
        {
            UIUtils.ResetLabel(walletAfterBalanceValueLabel);
        }
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
    }
}
