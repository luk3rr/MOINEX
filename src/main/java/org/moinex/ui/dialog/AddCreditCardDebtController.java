/*
 * Filename: AddCreditCardDebtController.java
 * Created on: October 25, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.moinex.entities.Category;
import org.moinex.entities.CreditCard;
import org.moinex.services.CalculatorService;
import org.moinex.services.CategoryService;
import org.moinex.services.CreditCardService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

@Controller
public class AddCreditCardDebtController
{
    @FXML
    private ComboBox<String> crcComboBox;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private ComboBox<YearMonth> invoiceComboBox;

    @FXML
    private Label crcLimitLabel;

    @FXML
    private Label crcAvailableLimitLabel;

    @FXML
    private Label crcLimitAvailableAfterDebtLabel;

    @FXML
    private Label msgLabel;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField valueField;

    @FXML
    private TextField installmentsField;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private List<Category> categories;

    private List<CreditCard> creditCards;

    private CategoryService categoryService;

    private CreditCardService creditCardService;

    private CalculatorService calculatorService;

    @Autowired
    public AddCreditCardDebtController(CategoryService   categoryService,
                                       CreditCardService creditCardService,
                                        CalculatorService calculatorService)
    {
        this.categoryService   = categoryService;
        this.creditCardService = creditCardService;
        this.calculatorService = calculatorService;
    }

    public void SetCreditCard(CreditCard crc)
    {
        if (creditCards.stream().noneMatch(c -> c.GetId() == crc.GetId()))
        {
            return;
        }

        crcComboBox.setValue(crc.GetName());

        UpdateCreditCardLimitLabels();
    }

    @FXML
    private void initialize()
    {
        ConfigureInvoiceComboBox();
        PopulateInvoiceComboBox();

        LoadCategories();
        LoadCreditCards();

        PopulateCategoryComboBox();
        PopulateCreditCardComboBox();

        // Reset all labels
        UIUtils.ResetLabel(crcLimitLabel);
        UIUtils.ResetLabel(crcAvailableLimitLabel);
        UIUtils.ResetLabel(crcLimitAvailableAfterDebtLabel);

        crcComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            UpdateCreditCardLimitLabels();
            UpdateAvailableLimitAfterDebtLabel();
        });

        valueField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
            {
                valueField.setText(oldValue);
            }
            else
            {
                UpdateAvailableLimitAfterDebtLabel();
                UpdateMsgLabel();
            }
        });

        installmentsField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.GetDigitsRegexUpTo(
                        Constants.INSTALLMENTS_FIELD_MAX_DIGITS)))
                {
                    installmentsField.setText(oldValue);
                }
                else
                {
                    UpdateMsgLabel();
                }
            });
    }

    @FXML
    private void handleSave()
    {
        String    crcName         = crcComboBox.getValue();
        String    categoryName    = categoryComboBox.getValue();
        YearMonth invoiceMonth    = invoiceComboBox.getValue();
        String    description     = descriptionField.getText().strip();
        String    valueStr        = valueField.getText();
        String    installmentsStr = installmentsField.getText();

        if (crcName == null || crcName.isEmpty() || categoryName == null ||
            categoryName.isEmpty() || description.isEmpty() || valueStr.isEmpty() ||
            invoiceMonth == null)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all the fields");
            return;
        }

        try
        {
            BigDecimal debtValue = new BigDecimal(valueStr);

            Integer installments =
                installmentsStr.isEmpty() ? 1 : Integer.parseInt(installmentsStr);

            CreditCard crc = creditCards.stream()
                                 .filter(c -> c.GetName().equals(crcName))
                                 .findFirst()
                                 .get();

            Category category = categories.stream()
                                    .filter(c -> c.GetName().equals(categoryName))
                                    .findFirst()
                                    .get();

            creditCardService.RegisterDebt(crc.GetId(),
                                           category,
                                           LocalDateTime.now(), // register date
                                           invoiceMonth,
                                           debtValue,
                                           installments,
                                           description);

            WindowUtils.ShowSuccessDialog("Success",
                                          "Debt created",
                                          "Debt created successfully");

            Stage stage = (Stage)crcComboBox.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Invalid expense value",
                                        "Debt value must be a number");
        }
        catch (RuntimeException e)
        {
            WindowUtils.ShowErrorDialog("Error", "Error creating debt", e.getMessage());
        }
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)crcComboBox.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleOpenCalculator(){

        WindowUtils.OpenPopupWindow(Constants.CALCULATOR_FXML,
                                    "Calculator",
                                    springContext,
                                    (CalculatorController controller)
                                        -> {},
                                    List.of(() -> { GetResultFromCalculator(); }));
    }

    private void GetResultFromCalculator()
    {
        // If the user saved the result, set it in the incomeValueField
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

                valueField.setText(result);
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

    private void LoadCategories()
    {
        categories = categoryService.GetNonArchivedCategoriesOrderedByName();
    }

    private void LoadCreditCards()
    {
        creditCards = creditCardService.GetAllNonArchivedCreditCardsOrderedByName();
    }

    private void UpdateCreditCardLimitLabels()
    {
        CreditCard crc = creditCards.stream()
                             .filter(c -> c.GetName().equals(crcComboBox.getValue()))
                             .findFirst()
                             .orElse(null);

        if (crc == null)
        {
            return;
        }

        crcLimitLabel.setText(UIUtils.FormatCurrency(crc.GetMaxDebt()));

        BigDecimal availableLimit = creditCardService.GetAvailableCredit(crc.GetId());

        crcAvailableLimitLabel.setText(UIUtils.FormatCurrency(availableLimit));
    }

    private void UpdateAvailableLimitAfterDebtLabel()
    {
        CreditCard crc = creditCards.stream()
                             .filter(c -> c.GetName().equals(crcComboBox.getValue()))
                             .findFirst()
                             .orElse(null);

        if (crc == null)
        {
            return;
        }

        String value = valueField.getText();

        if (value.isEmpty())
        {
            UIUtils.ResetLabel(crcLimitAvailableAfterDebtLabel);
            return;
        }

        try
        {
            BigDecimal debtValue = new BigDecimal(valueField.getText());

            if (debtValue.compareTo(BigDecimal.ZERO) <= 0)
            {
                UIUtils.ResetLabel(msgLabel);
                return;
            }

            BigDecimal availableLimitAfterDebt =
                creditCardService.GetAvailableCredit(crc.GetId()).subtract(debtValue);

            // Set the style according to the balance value after the expense
            if (availableLimitAfterDebt.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.SetLabelStyle(crcLimitAvailableAfterDebtLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                UIUtils.SetLabelStyle(crcLimitAvailableAfterDebtLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            crcLimitAvailableAfterDebtLabel.setText(
                UIUtils.FormatCurrency(availableLimitAfterDebt));
        }
        catch (NumberFormatException e)
        {
            UIUtils.ResetLabel(crcLimitAvailableAfterDebtLabel);
        }
    }

    private void UpdateMsgLabel()
    {
        Integer installments = installmentsField.getText().isEmpty()
                                   ? 1
                                   : Integer.parseInt(installmentsField.getText());

        if (installments < 1)
        {
            msgLabel.setText("Invalid number of installments");
            return;
        }

        String valueStr = valueField.getText();

        if (valueStr.isEmpty())
        {
            UIUtils.ResetLabel(msgLabel);
            return;
        }

        try
        {
            BigDecimal debtValue = new BigDecimal(valueField.getText());

            if (debtValue.compareTo(BigDecimal.ZERO) <= 0)
            {
                UIUtils.ResetLabel(msgLabel);
                return;
            }

            // Show mensage according to the value of each installment
            BigDecimal exactInstallmentValue =
                debtValue.divide(new BigDecimal(installments), 2, RoundingMode.FLOOR);

            BigDecimal remainder = debtValue.subtract(
                exactInstallmentValue.multiply(new BigDecimal(installments)));

            Boolean exactDivision = remainder.compareTo(BigDecimal.ZERO) == 0;

            if (exactDivision)
            {
                String msgBase = "Repeat for %d months of %s";
                msgLabel.setText(
                    String.format(msgBase,
                                  installments,
                                  UIUtils.FormatCurrency(exactInstallmentValue)));
            }
            else
            {
                String msgBase =
                    "Repeat for %d months.\nFirst month of %s and the last "
                    + "%s of %s";

                remainder = remainder.setScale(2, RoundingMode.HALF_UP);

                msgLabel.setText(String.format(msgBase,
                                               installments,
                                               exactInstallmentValue.add(remainder),
                                               installments - 1,
                                               exactInstallmentValue));
            }
        }
        catch (NumberFormatException e)
        {
            msgLabel.setText("Invalid debt value");
        }
    }

    private void PopulateInvoiceComboBox()
    {
        YearMonth currentYearMonth = YearMonth.now();
        YearMonth startYearMonth   = currentYearMonth.minusMonths(12);
        YearMonth endYearMonth     = currentYearMonth.plusMonths(13);

        // Show the last 12 months and the next 12 months as options to invoice
        // date
        for (YearMonth yearMonth = startYearMonth; yearMonth.isBefore(endYearMonth);
             yearMonth           = yearMonth.plusMonths(1))
        {
            invoiceComboBox.getItems().add(yearMonth);
        }

        // Set default as next month
        invoiceComboBox.setValue(currentYearMonth.plusMonths(1));
    }

    private void PopulateCategoryComboBox()
    {
        categoryComboBox.getItems().addAll(
            categories.stream().map(Category::GetName).toList());
    }

    private void PopulateCreditCardComboBox()
    {
        crcComboBox.getItems().addAll(
            creditCards.stream().map(CreditCard::GetName).toList());
    }

    private void ConfigureInvoiceComboBox()
    {
        // Set the format to display the month and year
        invoiceComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(YearMonth yearMonth)
            {
                return yearMonth.format(DateTimeFormatter.ofPattern("yyyy MMMM"));
            }

            @Override
            public YearMonth fromString(String string)
            {
                return YearMonth.parse(string,
                                       DateTimeFormatter.ofPattern("MMMM yyyy"));
            }
        });
    }
}
