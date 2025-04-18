/*
 * Filename: BaseCreditCardDebtManagement.java
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.creditcard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.creditcard.CreditCard;
import org.moinex.model.creditcard.CreditCardDebt;
import org.moinex.service.CalculatorService;
import org.moinex.service.CategoryService;
import org.moinex.service.CreditCardService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.SuggestionsHandlerHelper;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Base class for the Add Credit Card Debt and Edit Credit Card Debt dialogs
 */
@NoArgsConstructor
public abstract class BaseCreditCardDebtManagement
{
    @FXML
    protected ComboBox<CreditCard> crcComboBox;

    @FXML
    protected ComboBox<Category> categoryComboBox;

    @FXML
    protected ComboBox<YearMonth> invoiceComboBox;

    @FXML
    protected Label crcLimitLabel;

    @FXML
    protected Label crcAvailableLimitLabel;

    @FXML
    protected Label crcLimitAvailableAfterDebtLabel;

    @FXML
    protected Label msgLabel;

    @FXML
    protected TextField descriptionField;

    @FXML
    protected TextField valueField;

    @FXML
    protected TextField installmentsField;

    @Autowired
    protected ConfigurableApplicationContext springContext;

    protected SuggestionsHandlerHelper<CreditCardDebt> suggestionsHandler;

    protected List<Category> categories;

    protected List<CreditCard> creditCards;

    protected CategoryService categoryService;

    protected CreditCardService creditCardService;

    protected CalculatorService calculatorService;

    protected CreditCard creditCard = null;

    protected static final Logger logger =
        LoggerFactory.getLogger(BaseCreditCardDebtManagement.class);

    @Autowired
    protected BaseCreditCardDebtManagement(CategoryService   categoryService,
                                           CreditCardService creditCardService,
                                           CalculatorService calculatorService)
    {
        this.categoryService   = categoryService;
        this.creditCardService = creditCardService;
        this.calculatorService = calculatorService;
    }

    public void setCreditCard(CreditCard crc)
    {
        if (creditCards.stream().noneMatch(c -> c.getId().equals(crc.getId())))
        {
            return;
        }

        this.creditCard = crc;
        crcComboBox.setValue(creditCard);
        updateCreditCardLimitLabels();
    }

    @FXML
    protected void initialize()
    {
        configureListeners();
        configureSuggestions();
        configureComboBoxes();

        loadCategoriesFromDatabase();
        loadCreditCardsFromDatabase();
        loadSuggestionsFromDatabase();

        populateComboBoxes();

        // Reset all labels
        UIUtils.resetLabel(crcLimitLabel);
        UIUtils.resetLabel(crcAvailableLimitLabel);
        UIUtils.resetLabel(crcLimitAvailableAfterDebtLabel);
    }

    @FXML
    protected abstract void handleSave();

    @FXML
    protected void handleCancel()
    {
        Stage stage = (Stage)crcComboBox.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void handleOpenCalculator()
    {
        WindowUtils.openPopupWindow(
            Constants.CALCULATOR_FXML,
            "Calculator",
            springContext,
            (CalculatorController controller)
                -> {},
            List.of(() -> calculatorService.updateComponentWithResult(valueField)));
    }

    protected void loadCategoriesFromDatabase()
    {
        categories = categoryService.getNonArchivedCategoriesOrderedByName();
    }

    protected void loadCreditCardsFromDatabase()
    {
        creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByName();
    }

    protected void loadSuggestionsFromDatabase()
    {
        suggestionsHandler.setSuggestions(
            creditCardService.getCreditCardDebtSuggestions());
    }

    protected void updateCreditCardLimitLabels()
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

    protected void updateAvailableLimitAfterDebtLabel()
    {
        if (crcComboBox.getValue() == null)
        {
            return;
        }

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

            BigDecimal availableLimitAfterDebt =
                creditCardService.getAvailableCredit(crc.getId()).subtract(debtValue);

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

    protected void updateMsgLabel()
    {
        int installments = installmentsField.getText().isEmpty()
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

            // Show a message according to the value of each installment
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

    protected void populateComboBoxes()
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

        categoryComboBox.getItems().addAll(categories);

        crcComboBox.getItems().addAll(creditCards);
    }

    protected void configureComboBoxes()
    {
        UIUtils.configureComboBox(categoryComboBox, Category::getName);
        UIUtils.configureComboBox(crcComboBox, CreditCard::getName);
        UIUtils.configureComboBox(
            invoiceComboBox,
            yearMonth -> yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
    }

    protected void configureSuggestions()
    {
        Function<CreditCardDebt, String> filterFunction =
            CreditCardDebt::getDescription;

        // Format:
        //    Description
        //    Amount | CreditCard | Category | Installments
        Function<CreditCardDebt, String> displayFunction = ccd
            -> String.format("%s\n%s | %s | %s | %sx",
                             ccd.getDescription(),
                             UIUtils.formatCurrency(ccd.getAmount()),
                             ccd.getCreditCard().getName(),
                             ccd.getCategory().getName(),
                             ccd.getInstallments());

        Consumer<CreditCardDebt> onSelectCallback =
                this::fillFieldsWithTransaction;

        suggestionsHandler = new SuggestionsHandlerHelper<>(descriptionField,
                                                            filterFunction,
                                                            displayFunction,
                                                            onSelectCallback);

        suggestionsHandler.enable();
    }

    protected void configureListeners()
    {
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

    protected void fillFieldsWithTransaction(CreditCardDebt ccd)
    {
        crcComboBox.setValue(ccd.getCreditCard());

        // Deactivate the listener to avoid the event of changing the text of
        // the descriptionField from being triggered. After changing the text,
        // the listener is activated again
        suggestionsHandler.disable();

        descriptionField.setText(ccd.getDescription());

        suggestionsHandler.enable();

        valueField.setText(ccd.getAmount().toString());
        installmentsField.setText(ccd.getInstallments().toString());
        categoryComboBox.setValue(ccd.getCategory());

        updateAvailableLimitAfterDebtLabel();
        updateMsgLabel();
    }
}
