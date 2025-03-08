/*
 * Filename: EditDividendController.java
 * Created on: January 11, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import jakarta.persistence.EntityNotFoundException;
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
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
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

    public void setWalletComboBox(Wallet wt)
    {
        if (wallets.stream().noneMatch(w -> w.getId().equals(wt.getId())))
        {
            return;
        }

        walletComboBox.setValue(wt.getName());

        updateWalletBalance();
    }

    public void setDividend(Dividend d)
    {
        this.dividend = d;

        tickerNameLabel.setText(d.getTicker().getName() + " (" +
                                d.getTicker().getSymbol() + ")");

        setWalletComboBox(d.getWalletTransaction().getWallet());

        descriptionField.setText(d.getWalletTransaction().getDescription());
        dividendValueField.setText(d.getWalletTransaction().getAmount().toString());
        statusComboBox.setValue(d.getWalletTransaction().getStatus().name());
        categoryComboBox.setValue(d.getWalletTransaction().getCategory().getName());
        dividendDatePicker.setValue(d.getWalletTransaction().getDate().toLocalDate());
    }

    @FXML
    private void initialize()
    {
        loadWalletsFromDatabase();
        loadCategoriesFromDatabase();

        // Configure date picker
        UIUtils.setDatePickerFormat(dividendDatePicker);

        // For each element in enum TransactionStatus, add its name to the
        // statusComboBox
        statusComboBox.getItems().addAll(
            Arrays.stream(TransactionStatus.values()).map(Enum::name).toList());

        // Reset all labels
        UIUtils.resetLabel(walletAfterBalanceValueLabel);
        UIUtils.resetLabel(walletCurrentBalanceValueLabel);

        walletComboBox.setOnAction(e -> {
            updateWalletBalance();
            walletAfterBalance();
        });

        configureListeners();
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
            WindowUtils.showErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all the fields.");
            return;
        }

        try
        {
            BigDecimal dividendValue = new BigDecimal(dividendValueString);

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

            TransactionStatus status = TransactionStatus.valueOf(statusString);

            // Check if has any modification
            if (dividend.getWalletTransaction().getAmount().compareTo(dividendValue) ==
                    0 &&
                dividend.getWalletTransaction().getCategory().getId() ==
                    category.getId() &&
                dividend.getWalletTransaction().getStatus().equals(status) &&
                dividend.getWalletTransaction().getDate().toLocalDate().equals(
                    dividendDate) &&
                dividend.getWalletTransaction().getDescription().equals(description) &&
                dividend.getWalletTransaction().getWallet().getId() == wallet.getId())
            {
                WindowUtils.showInformationDialog(
                    "Info",
                    "No changes",
                    "No changes were made to the dividend");
            }
            else // If there is any modification, update the transaction
            {
                LocalTime     currentTime = LocalTime.now();
                LocalDateTime dateTimeWithCurrentHour =
                    dividendDate.atTime(currentTime);

                dividend.getWalletTransaction().setAmount(dividendValue);
                dividend.getWalletTransaction().setCategory(category);
                dividend.getWalletTransaction().setStatus(status);
                dividend.getWalletTransaction().setDate(dateTimeWithCurrentHour);
                dividend.getWalletTransaction().setDescription(description);
                dividend.getWalletTransaction().setWallet(wallet);

                tickerService.updateDividend(dividend);

                WindowUtils.showSuccessDialog("Success",
                                              "Dividend updated",
                                              "Dividend updated successfully");
            }

            Stage stage = (Stage)descriptionField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Invalid dividend value",
                                        "Dividend value must be a number.");
        }
        catch (EntityNotFoundException e)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Error while updating dividend",
                                        e.getMessage());
        }
    }

    @FXML
    private void handleOpenCalculator()
    {
        WindowUtils.openPopupWindow(Constants.CALCULATOR_FXML,
                                    "Calculator",
                                    springContext,
                                    (CalculatorController controller)
                                        -> {},
                                    List.of(() -> getResultFromCalculator()));
    }

    private void getResultFromCalculator()
    {
        // If the user saved the result, set it in the dividendValueField
        String result = calculatorService.getResult();

        if (result != null)
        {
            try
            {
                BigDecimal resultValue = new BigDecimal(result);

                if (resultValue.compareTo(BigDecimal.ZERO) < 0)
                {
                    WindowUtils.showErrorDialog("Error",
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
                WindowUtils.showErrorDialog("Error",
                                            "Invalid value",
                                            "The value must be a number");
            }
        }
    }

    private void updateWalletBalance()
    {
        String walletName = walletComboBox.getValue();

        if (walletName == null)
        {
            return;
        }

        Wallet wallet =
            wallets.stream()
                .filter(w -> w.getName().equals(walletName))
                .findFirst()
                .orElseThrow(()
                                 -> new EntityNotFoundException(
                                     "Wallet with name: " + walletName + " not found"));

        if (wallet.getBalance().compareTo(BigDecimal.ZERO) < 0)
        {
            UIUtils.setLabelStyle(walletCurrentBalanceValueLabel,
                                  Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            UIUtils.setLabelStyle(walletCurrentBalanceValueLabel,
                                  Constants.NEUTRAL_BALANCE_STYLE);
        }

        walletCurrentBalanceValueLabel.setText(
            UIUtils.formatCurrency(wallet.getBalance()));
    }

    private void walletAfterBalance()
    {
        String dividendValueString = dividendValueField.getText();
        String walletName          = walletComboBox.getValue();

        if (dividendValueString == null || dividendValueString.strip().isEmpty() ||
            walletName == null)
        {
            UIUtils.resetLabel(walletAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal dividendValue = new BigDecimal(dividendValueString);

            if (dividendValue.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(walletAfterBalanceValueLabel);
                return;
            }

            Wallet wallet = wallets.stream()
                                .filter(w -> w.getName().equals(walletName))
                                .findFirst()
                                .orElseThrow(()
                                                 -> new EntityNotFoundException(
                                                     "Wallet with name: " + walletName +
                                                     " not found"));

            BigDecimal walletAfterBalanceValue = BigDecimal.ZERO;

            if (dividend.getWalletTransaction().getStatus().equals(
                    TransactionStatus.CONFIRMED))
            {
                // If the transaction is confirmed, the balance will be updated
                // based on the difference between the new and the old value
                BigDecimal diff =
                    dividendValue.subtract(dividend.getWalletTransaction().getAmount());

                walletAfterBalanceValue = wallet.getBalance().add(diff);
            }
            else
            {
                // If the transaction is not confirmed, the balance will be
                // updated based on the new value
                walletAfterBalanceValue = wallet.getBalance().add(dividendValue);
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
                "You need to add a category before adding an dividend");
        }
    }

    private void configureListeners()
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
                    walletAfterBalance();
                }
            });
    }
}
