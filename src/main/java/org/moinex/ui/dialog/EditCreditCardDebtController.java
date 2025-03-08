/*
 * Filename: EditCreditCardDebtController.java
 * Created on: October 28, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import jakarta.persistence.EntityNotFoundException;
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
    private ComboBox<CreditCard> crcComboBox;

    @FXML
    private ComboBox<Category> categoryComboBox;

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

    private CreditCardDebt crcDebt = null;

    @Autowired
    public EditCreditCardDebtController(CategoryService   categoryService,
                                        CreditCardService creditCardService)
    {
        this.categoryService   = categoryService;
        this.creditCardService = creditCardService;
    }

    public void setCreditCardDebt(CreditCardDebt crcDebt)
    {
        this.crcDebt = crcDebt;

        // Set the values of the expense to the fields
        crcComboBox.setValue(crcDebt.getCreditCard());
        crcLimitLabel.setText(
            UIUtils.formatCurrency(crcDebt.getCreditCard().getMaxDebt()));

        BigDecimal availableLimit =
            creditCardService.getAvailableCredit(crcDebt.getCreditCard().getId());

        crcAvailableLimitLabel.setText(UIUtils.formatCurrency(availableLimit));

        // The debt value has already been subtracted from the available limit, so
        // unless the user changes the debt value, the available limit after the
        // debt will be the same
        crcLimitAvailableAfterDebtLabel.setText(UIUtils.formatCurrency(availableLimit));

        descriptionField.setText(crcDebt.getDescription());
        valueField.setText(crcDebt.getAmount().toString());
        installmentsField.setText(crcDebt.getInstallments().toString());

        categoryComboBox.setValue(crcDebt.getCategory());

        CreditCardPayment firstPayment =
            creditCardService.getPaymentsByDebtId(crcDebt.getId()).getFirst();

        invoiceComboBox.setValue(YearMonth.of(firstPayment.getDate().getYear(),
                                              firstPayment.getDate().getMonth()));
    }

    @FXML
    private void initialize()
    {
        configureComboBoxes();

        loadCategoriesFromDatabase();
        loadCreditCardsFromDatabase();

        populateComboBoxes();

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
        CreditCard crc             = crcComboBox.getValue();
        Category   category        = categoryComboBox.getValue();
        YearMonth  invoiceMonth    = invoiceComboBox.getValue();
        String     description     = descriptionField.getText().strip();
        String     valueStr        = valueField.getText();
        String     installmentsStr = installmentsField.getText();

        if (crc == null || category == null || description.isEmpty() ||
            valueStr.isEmpty() || invoiceMonth == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");
            return;
        }

        try
        {
            BigDecimal debtValue = new BigDecimal(valueStr);

            Integer installments =
                installmentsStr.isEmpty() ? 1 : Integer.parseInt(installmentsStr);

            // Get the date of the first payment to check if the invoice month is the
            // same
            CreditCardPayment firstPayment =
                creditCardService.getPaymentsByDebtId(crcDebt.getId()).getFirst();

            YearMonth invoice = YearMonth.of(firstPayment.getDate().getYear(),
                                             firstPayment.getDate().getMonth());

            // Check if has any modification
            if (crcDebt.getCreditCard().getId().equals(crc.getId()) &&
                crcDebt.getCategory().getId().equals(category.getId()) &&
                debtValue.compareTo(crcDebt.getAmount()) == 0 &&
                crcDebt.getInstallments().equals(installments) &&
                crcDebt.getDescription().equals(description) &&
                invoice.equals(invoiceMonth))
            {
                WindowUtils.showInformationDialog("No changes",
                                                  "No changes were made.");
            }
            else // If there is any modification, update the debt
            {
                crcDebt.setCreditCard(crc);
                crcDebt.setCategory(category);
                crcDebt.setDescription(description);
                crcDebt.setAmount(debtValue);
                crcDebt.setInstallments(installments);

                creditCardService.updateCreditCardDebt(crcDebt, invoiceMonth);

                WindowUtils.showSuccessDialog("Transaction updated",
                                              "Transaction updated successfully.");
            }

            Stage stage = (Stage)crcComboBox.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid expense value",
                                        "Debt value must be a number");
        }
        catch (EntityNotFoundException | IllegalArgumentException e)
        {
            WindowUtils.showErrorDialog("Error creating debt", e.getMessage());
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
        CreditCard crc =
            creditCards.stream()
                .filter(c -> c.getId().equals(crcComboBox.getValue().getId()))
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
        CreditCard crc =
            creditCards.stream()
                .filter(c -> c.getId().equals(crcComboBox.getValue().getId()))
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

            BigDecimal diff = debtValue.subtract(crcDebt.getAmount());

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

            boolean exactDivision = remainder.compareTo(BigDecimal.ZERO) == 0;

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

    private void populateComboBoxes()
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

    private void configureComboBoxes()
    {
        UIUtils.configureComboBox(crcComboBox, CreditCard::getName);
        UIUtils.configureComboBox(categoryComboBox, Category::getName);
        UIUtils.configureComboBox(
            invoiceComboBox,
            yearMonth -> yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
    }
}
