/*
 * Filename: AddIncomeController.java
 * Created on: October  6, 2024
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
import org.moinex.services.CategoryService;
import org.moinex.services.WalletService;
import org.moinex.services.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.TransactionStatus;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Income dialog
 */
@Controller
public class AddIncomeController
{
    @FXML
    private Label walletAfterBalanceValueLabel;

    @FXML
    private Label walletCurrentBalanceValueLabel;

    @FXML
    private ComboBox<String> walletComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private TextField incomeValueField;

    @FXML
    private TextField descriptionField;

    @FXML
    private DatePicker incomeDatePicker;

    private WalletService walletService;

    private WalletTransactionService walletTransactionService;

    private CategoryService categoryService;

    private List<Wallet> wallets;

    private List<Category> categories;

    public AddIncomeController() { }

    /**
     * Constructor
     * @param walletService WalletService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddIncomeController(WalletService            walletService,
                               WalletTransactionService walletTransactionService,
                               CategoryService          categoryService)
    {
        this.walletService            = walletService;
        this.walletTransactionService = walletTransactionService;
        this.categoryService          = categoryService;
    }

    public void SetWalletComboBox(Wallet wt)
    {
        if (wallets.stream().noneMatch(w -> w.GetId() == wt.GetId()))
        {
            return;
        }

        walletComboBox.setValue(wt.GetName());

        UpdateWalletBalance();
    }

    @FXML
    private void initialize()
    {
        LoadWallets();
        LoadCategories();

        // Configure date picker
        UIUtils.SetDatePickerFormat(incomeDatePicker);

        // For each element in enum TransactionStatus, add its name to the
        // statusComboBox
        statusComboBox.getItems().addAll(
            Arrays.stream(TransactionStatus.values()).map(Enum::name).toList());

        // Reset all labels
        UIUtils.ResetLabel(walletAfterBalanceValueLabel);
        UIUtils.ResetLabel(walletCurrentBalanceValueLabel);

        walletComboBox.setOnAction(e -> {
            UpdateWalletBalance();
            WalletAfterBalance();
        });

        // Update wallet after balance when the value field changes
        incomeValueField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
                {
                    incomeValueField.setText(oldValue);
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
        String    walletName        = walletComboBox.getValue();
        String    description       = descriptionField.getText();
        String    incomeValueString = incomeValueField.getText();
        String    statusString      = statusComboBox.getValue();
        String    categoryString    = categoryComboBox.getValue();
        LocalDate incomeDate        = incomeDatePicker.getValue();

        if (walletName == null || description == null ||
            description.strip().isEmpty() || incomeValueString == null ||
            incomeValueString.strip().isEmpty() || statusString == null ||
            categoryString == null || incomeDate == null)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all the fields.");
            return;
        }

        try
        {
            BigDecimal incomeValue = new BigDecimal(incomeValueString);

            Wallet wallet = wallets.stream()
                                .filter(w -> w.GetName().equals(walletName))
                                .findFirst()
                                .get();

            Category category = categories.stream()
                                    .filter(c -> c.GetName().equals(categoryString))
                                    .findFirst()
                                    .get();

            TransactionStatus status = TransactionStatus.valueOf(statusString);

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = incomeDate.atTime(currentTime);

            walletTransactionService.AddIncome(wallet.GetId(),
                                               category,
                                               dateTimeWithCurrentHour,
                                               incomeValue,
                                               description,
                                               status);

            WindowUtils.ShowSuccessDialog("Success",
                                          "Income created",
                                          "The income was successfully created.");

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
                                        "Error while creating income",
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

        if (wallet.GetBalance().compareTo(BigDecimal.ZERO) < 0)
        {
            UIUtils.SetLabelStyle(walletCurrentBalanceValueLabel,
                                  Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            UIUtils.SetLabelStyle(walletCurrentBalanceValueLabel,
                                  Constants.NEUTRAL_BALANCE_STYLE);
        }

        walletCurrentBalanceValueLabel.setText(
            UIUtils.FormatCurrency(wallet.GetBalance()));
    }

    private void WalletAfterBalance()
    {
        String incomeValueString = incomeValueField.getText();
        String walletName        = walletComboBox.getValue();

        if (incomeValueString == null || incomeValueString.strip().isEmpty() ||
            walletName == null)
        {
            UIUtils.ResetLabel(walletAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal incomeValue = new BigDecimal(incomeValueString);

            if (incomeValue.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.ResetLabel(walletAfterBalanceValueLabel);
                return;
            }

            Wallet wallet = wallets.stream()
                                .filter(w -> w.GetName().equals(walletName))
                                .findFirst()
                                .get();

            BigDecimal walletAfterBalanceValue = wallet.GetBalance().add(incomeValue);

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
                "You need to add a category before adding an income");
        }
    }
}
