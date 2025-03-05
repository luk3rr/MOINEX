/*
 * Filename: EditCreditCardDebtController.java
 * Created on: October 28, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.NoArgsConstructor;
import org.moinex.entities.Category;
import org.moinex.entities.CreditCard;
import org.moinex.entities.CreditCardDebt;
import org.moinex.entities.CreditCardPayment;
import org.moinex.services.CategoryService;
import org.moinex.services.CreditCardService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Credit Card Debt dialog
 */
@Controller
@NoArgsConstructor
public class EditCreditCardDebtController
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

    private List<Category> categories;

    private List<CreditCard> creditCards;

    private CategoryService categoryService;

    private CreditCardService creditCardService;

    private CreditCardDebt debtToUpdate;

    @Autowired
    public EditCreditCardDebtController(CategoryService   categoryService,
                                        CreditCardService creditCardService)
    {
        this.categoryService   = categoryService;
        this.creditCardService = creditCardService;
    }

    public void setCreditCardDebt(CreditCardDebt crcDebt)
    {
        debtToUpdate = crcDebt;

        // Set the values of the expense to the fields
        crcComboBox.setValue(crcDebt.getCreditCard().getName());
        crcLimitLabel.setText(
            UIUtils.formatCurrency(crcDebt.getCreditCard().getMaxDebt()));

        BigDecimal availableLimit =
            creditCardService.getAvailableCredit(crcDebt.getCreditCard().getId());

        crcAvailableLimitLabel.setText(UIUtils.formatCurrency(availableLimit));

        // The debt value has already been subtracted from the available limit, so
        // unless the user changes the debt value, the available limit after the debt
        // will be the same
        crcLimitAvailableAfterDebtLabel.setText(UIUtils.formatCurrency(availableLimit));

        descriptionField.setText(crcDebt.getDescription());
        valueField.setText(crcDebt.getAmount().toString());
        installmentsField.setText(crcDebt.getInstallments().toString());

        categoryComboBox.setValue(crcDebt.getCategory().getName());

        CreditCardPayment firstPayment =
            creditCardService.getPaymentsByDebtId(crcDebt.getId()).getFirst();

        invoiceComboBox.setValue(YearMonth.of(firstPayment.getDate().getYear(),
                                              firstPayment.getDate().getMonth()));
    }

    @FXML
    private void initialize()
    {
        configureInvoiceComboBox();
        populateInvoiceComboBox();

        loadCategoriesFromDatabase();
        loadCreditCardsFromDatabase();

        populateCategoryComboBox();
        populateCreditCardComboBox();

        // Reset all labels
        UIUtils.resetLabel(crcLimitLabel);
        UIUtils.resetLabel(crcAvailableLimitLabel);
        UIUtils.resetLabel(crcLimitAvailableAfterDebtLabel);

        // Add listeners
        crcComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateCreditCardLimitLabels();
            updateAvailableLimitAfterDebtLabel();
        });

        valueField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
            {
                valueField.setText(oldValue);
            }
            else
            {
                updateAvailableLimitAfterDebtLabel();
                updateMsgLabel();
            }
        });

        installmentsField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.getDigitsRegexUpTo(
                        Constants.INSTALLMENTS_FIELD_MAX_DIGITS)))
                {
                    installmentsField.setText(oldValue);
                }
                else
                {
                    updateMsgLabel();
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
            WindowUtils.showErrorDialog("Error",
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
                                 .filter(c -> c.getName().equals(crcName))
                                 .findFirst()
                                 .get();

            Category category = categories.stream()
                                    .filter(c -> c.getName().equals(categoryName))
                                    .findFirst()
                                    .get();

            // Get the date of the first payment to check if the invoice month is the
            // same
            CreditCardPayment firstPayment =
                creditCardService.getPaymentsByDebtId(debtToUpdate.getId()).getFirst();

            YearMonth invoice = YearMonth.of(firstPayment.getDate().getYear(),
                                             firstPayment.getDate().getMonth());

            // Check if has any modification
            if (debtToUpdate.getCreditCard().getId() == crc.getId() &&
                debtToUpdate.getCategory().getId() == category.getId() &&
                debtValue.compareTo(debtToUpdate.getAmount()) == 0 &&
                debtToUpdate.getInstallments() == installments &&
                debtToUpdate.getDescription().equals(description) &&
                invoice.equals(invoiceMonth))
            {
                WindowUtils.showInformationDialog("Info",
                                                  "No changes",
                                                  "No changes were made.");
            }
            else // If there is any modification, update the debt
            {
                debtToUpdate.setCreditCard(crc);
                debtToUpdate.setCategory(category);
                debtToUpdate.setDescription(description);
                debtToUpdate.setAmount(debtValue);
                debtToUpdate.setInstallments(installments);

                creditCardService.updateCreditCardDebt(debtToUpdate, invoiceMonth);

                WindowUtils.showSuccessDialog("Success",
                                              "Transaction updated",
                                              "Transaction updated successfully.");
            }

            Stage stage = (Stage)crcComboBox.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Invalid expense value",
                                        "Debt value must be a number");
        }
        catch (RuntimeException e)
        {
            WindowUtils.showErrorDialog("Error", "Error creating debt", e.getMessage());
        }
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)crcComboBox.getScene().getWindow();
        stage.close();
    }

    private void loadCategoriesFromDatabase()
    {
        categories = categoryService.getCategories();
    }

    private void loadCreditCardsFromDatabase()
    {
        creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByName();
    }

    private void updateCreditCardLimitLabels()
    {
        CreditCard crc = creditCards.stream()
                             .filter(c -> c.getName().equals(crcComboBox.getValue()))
                             .findFirst()
                             .orElse(null);

        if (crc == null)
        {
            return;
        }

        crcLimitLabel.setText(UIUtils.formatCurrency(crc.getMaxDebt()));

        BigDecimal availableLimit = creditCardService.getAvailableCredit(crc.getId());

        crcAvailableLimitLabel.setText(UIUtils.formatCurrency(availableLimit));
    }

    private void updateAvailableLimitAfterDebtLabel()
    {
        CreditCard crc = creditCards.stream()
                             .filter(c -> c.getName().equals(crcComboBox.getValue()))
                             .findFirst()
                             .orElse(null);

        if (crc == null)
        {
            return;
        }

        String value = valueField.getText();

        if (value.isEmpty())
        {
            UIUtils.resetLabel(crcLimitAvailableAfterDebtLabel);
            return;
        }

        try
        {
            BigDecimal debtValue = new BigDecimal(valueField.getText());

            if (debtValue.compareTo(BigDecimal.ZERO) <= 0)
            {
                UIUtils.resetLabel(msgLabel);
                return;
            }

            BigDecimal diff = debtValue.subtract(debtToUpdate.getAmount());

            BigDecimal availableLimitAfterDebt =
                creditCardService.getAvailableCredit(crc.getId()).subtract(diff);

            // Set the style according to the balance value after the expense
            if (availableLimitAfterDebt.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.setLabelStyle(crcLimitAvailableAfterDebtLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                UIUtils.setLabelStyle(crcLimitAvailableAfterDebtLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            crcLimitAvailableAfterDebtLabel.setText(
                UIUtils.formatCurrency(availableLimitAfterDebt));
        }
        catch (NumberFormatException e)
        {
            UIUtils.resetLabel(crcLimitAvailableAfterDebtLabel);
        }
    }

    private void updateMsgLabel()
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
            UIUtils.resetLabel(msgLabel);
            return;
        }

        try
        {
            BigDecimal debtValue = new BigDecimal(valueField.getText());

            if (debtValue.compareTo(BigDecimal.ZERO) <= 0)
            {
                UIUtils.resetLabel(msgLabel);
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
                                  UIUtils.formatCurrency(exactInstallmentValue)));
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

    private void populateInvoiceComboBox()
    {
        YearMonth currentYearMonth = YearMonth.now();
        YearMonth startYearMonth   = currentYearMonth.minusMonths(12);
        YearMonth endYearMonth     = currentYearMonth.plusMonths(13);

        // Show the last 12 months and the next 12 months as options to invoice date
        for (YearMonth yearMonth = startYearMonth; yearMonth.isBefore(endYearMonth);
             yearMonth           = yearMonth.plusMonths(1))
        {
            invoiceComboBox.getItems().add(yearMonth);
        }

        // Set default as next month
        invoiceComboBox.setValue(currentYearMonth.plusMonths(1));
    }

    private void populateCategoryComboBox()
    {
        categoryComboBox.getItems().addAll(
            categories.stream().map(Category::getName).toList());
    }

    private void populateCreditCardComboBox()
    {
        crcComboBox.getItems().addAll(
            creditCards.stream().map(CreditCard::getName).toList());
    }

    private void configureInvoiceComboBox()
    {
        // Set the format to display the month and year
        invoiceComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(YearMonth yearMonth)
            {
                return yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
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
