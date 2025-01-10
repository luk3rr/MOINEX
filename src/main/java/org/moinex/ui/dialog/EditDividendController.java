/*
 * Filename: EditDividendController.java
 * Created on: January 11, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.moinex.entities.investment.Dividend;
import org.moinex.services.CalculatorService;
import org.moinex.services.CategoryService;
import org.moinex.services.TickerService;
import org.moinex.services.WalletService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.TransactionStatus;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Dividend dialog
 */
@Controller
public class EditDividendController
{
    @FXML
    private Label tickerNameLabel;

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
    private TextField dividendValueField;

    @FXML
    private TextField descriptionField;

    @FXML
    private DatePicker dividendDatePicker;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private WalletService walletService;

    private CategoryService categoryService;

    private CalculatorService calculatorService;

    private TickerService tickerService;

    private List<Wallet> wallets;

    private List<Category> categories;

    private Dividend dividend;

    public EditDividendController() { }

    /**
     * Constructor
     * @param walletService WalletService
     * @param categoryService CategoryService
     * @param calculatorService CalculatorService
     * @param tickerService TickerService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public EditDividendController(WalletService     walletService,
                                  CategoryService   categoryService,
                                  CalculatorService calculatorService,
                                  TickerService     tickerService)
    {
        this.walletService     = walletService;
        this.categoryService   = categoryService;
        this.calculatorService = calculatorService;
        this.tickerService     = tickerService;
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

    public void SetDividend(Dividend d)
    {
        this.dividend = d;

        tickerNameLabel.setText(d.GetTicker().GetName() + " (" +
                                d.GetTicker().GetSymbol() + ")");

        SetWalletComboBox(d.GetWalletTransaction().GetWallet());

        descriptionField.setText(d.GetWalletTransaction().GetDescription());
        dividendValueField.setText(d.GetWalletTransaction().GetAmount().toString());
        statusComboBox.setValue(d.GetWalletTransaction().GetStatus().name());
        categoryComboBox.setValue(d.GetWalletTransaction().GetCategory().GetName());
        dividendDatePicker.setValue(d.GetWalletTransaction().GetDate().toLocalDate());
    }

    @FXML
    private void initialize()
    {
        LoadWallets();
        LoadCategories();

        // Configure date picker
        UIUtils.SetDatePickerFormat(dividendDatePicker);

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

        ConfigureListeners();
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
        String    walletName          = walletComboBox.getValue();
        String    description         = descriptionField.getText();
        String    dividendValueString = dividendValueField.getText();
        String    statusString        = statusComboBox.getValue();
        String    categoryString      = categoryComboBox.getValue();
        LocalDate dividendDate        = dividendDatePicker.getValue();

        if (walletName == null || description == null ||
            description.strip().isEmpty() || dividendValueString == null ||
            dividendValueString.strip().isEmpty() || statusString == null ||
            categoryString == null || dividendDate == null)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all the fields.");
            return;
        }

        try
        {
            BigDecimal dividendValue = new BigDecimal(dividendValueString);

            Wallet wallet = wallets.stream()
                                .filter(w -> w.GetName().equals(walletName))
                                .findFirst()
                                .get();

            Category category = categories.stream()
                                    .filter(c -> c.GetName().equals(categoryString))
                                    .findFirst()
                                    .get();

            TransactionStatus status = TransactionStatus.valueOf(statusString);

            // Check if has any modification
            if (dividend.GetWalletTransaction().GetAmount().compareTo(dividendValue) ==
                    0 &&
                dividend.GetWalletTransaction().GetCategory().GetId() ==
                    category.GetId() &&
                dividend.GetWalletTransaction().GetStatus().equals(status) &&
                dividend.GetWalletTransaction().GetDate().toLocalDate().equals(
                    dividendDate) &&
                dividend.GetWalletTransaction().GetDescription().equals(description) &&
                dividend.GetWalletTransaction().GetWallet().GetId() == wallet.GetId())
            {
                WindowUtils.ShowInformationDialog(
                    "Info",
                    "No changes",
                    "No changes were made to the dividend");
            }
            else // If there is any modification, update the transaction
            {
                LocalTime     currentTime = LocalTime.now();
                LocalDateTime dateTimeWithCurrentHour =
                    dividendDate.atTime(currentTime);

                dividend.GetWalletTransaction().SetAmount(dividendValue);
                dividend.GetWalletTransaction().SetCategory(category);
                dividend.GetWalletTransaction().SetStatus(status);
                dividend.GetWalletTransaction().SetDate(dateTimeWithCurrentHour);
                dividend.GetWalletTransaction().SetDescription(description);
                dividend.GetWalletTransaction().SetWallet(wallet);

                tickerService.UpdateDividend(dividend);

                WindowUtils.ShowSuccessDialog("Success",
                                              "Dividend updated",
                                              "Dividend updated successfully");
            }

            Stage stage = (Stage)descriptionField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Invalid dividend value",
                                        "Dividend value must be a number.");
        }
        catch (RuntimeException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Error while updating dividend",
                                        e.getMessage());
        }
    }

    @FXML
    private void handleOpenCalculator()
    {
        WindowUtils.OpenPopupWindow(Constants.CALCULATOR_FXML,
                                    "Calculator",
                                    springContext,
                                    (CalculatorController controller)
                                        -> {},
                                    List.of(() -> { GetResultFromCalculator(); }));
    }

    private void GetResultFromCalculator()
    {
        // If the user saved the result, set it in the dividendValueField
        String result = calculatorService.GetResult();

        if (result != null)
        {
            try
            {
                BigDecimal resultValue = new BigDecimal(result);

                if (resultValue.compareTo(BigDecimal.ZERO) < 0)
                {
                    WindowUtils.ShowErrorDialog("Error",
                                                "Invalid value",
                                                "The value must be positive");
                    return;
                }

                // Round the result to 2 decimal places
                result = resultValue.setScale(2, RoundingMode.HALF_UP).toString();

                dividendValueField.setText(result);
            }
            catch (NumberFormatException e)
            {
                // Must be unreachable
                WindowUtils.ShowErrorDialog("Error",
                                            "Invalid value",
                                            "The value must be a number");
            }
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
        String dividendValueString = dividendValueField.getText();
        String walletName          = walletComboBox.getValue();

        if (dividendValueString == null || dividendValueString.strip().isEmpty() ||
            walletName == null)
        {
            UIUtils.ResetLabel(walletAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal dividendValue = new BigDecimal(dividendValueString);

            if (dividendValue.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.ResetLabel(walletAfterBalanceValueLabel);
                return;
            }

            Wallet wallet = wallets.stream()
                                .filter(w -> w.GetName().equals(walletName))
                                .findFirst()
                                .get();

            BigDecimal walletAfterBalanceValue = BigDecimal.ZERO;

            if (dividend.GetWalletTransaction().GetStatus().equals(
                    TransactionStatus.CONFIRMED))
            {
                // If the transaction is confirmed, the balance will be updated
                // based on the difference between the new and the old value
                BigDecimal diff =
                    dividendValue.subtract(dividend.GetWalletTransaction().GetAmount());

                walletAfterBalanceValue = wallet.GetBalance().add(diff);
            }
            else
            {
                // If the transaction is not confirmed, the balance will be
                // updated based on the new value
                walletAfterBalanceValue = wallet.GetBalance().add(dividendValue);
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

        // If there are no categories, add a tooltip to the categoryComboBox
        // to inform the user that a category is needed
        if (categories.size() == 0)
        {
            UIUtils.AddTooltipToNode(
                categoryComboBox,
                "You need to add a category before adding an dividend");
        }
    }

    private void ConfigureListeners()
    {
        // Update wallet after balance when the value field changes
        dividendValueField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
                {
                    dividendValueField.setText(oldValue);
                }
                else
                {
                    WalletAfterBalance();
                }
            });
    }
}
