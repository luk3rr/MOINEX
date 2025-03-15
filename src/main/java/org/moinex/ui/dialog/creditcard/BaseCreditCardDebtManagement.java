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
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.entities.Category;
import org.moinex.entities.creditcard.CreditCard;
import org.moinex.entities.creditcard.CreditCardDebt;
import org.moinex.services.CalculatorService;
import org.moinex.services.CategoryService;
import org.moinex.services.CreditCardService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
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

    protected Popup suggestionsPopup;

    protected ListView<CreditCardDebt> suggestionListView;

    protected List<CreditCardDebt> suggestions;

    protected ChangeListener<String> descriptionFieldListener;

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
        if (creditCards.stream().noneMatch(c -> c.getId() == crc.getId()))
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
        configureComboBoxes();

        loadCategoriesFromDatabase();
        loadCreditCardsFromDatabase();
        loadSuggestionsFromDatabase();

        populateComboBoxes();

        // Reset all labels
        UIUtils.resetLabel(crcLimitLabel);
        UIUtils.resetLabel(crcAvailableLimitLabel);
        UIUtils.resetLabel(crcLimitAvailableAfterDebtLabel);

        configureSuggestionsListView();
        configureSuggestionsPopup();
        configureListeners();
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
        suggestions = creditCardService.getCreditCardDebtSuggestions();
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

        // Store the listener in a variable to be able to disable and enable it
        // when needed
        descriptionFieldListener = (observable, oldValue, newValue) ->
        {
            if (newValue.strip().isEmpty())
            {
                suggestionsPopup.hide();
                return;
            }

            suggestionListView.getItems().clear();

            // Filter the suggestions list to show only the transfers that
            // contain similar descriptions to the one typed by the user
            List<CreditCardDebt> filteredSuggestions =
                suggestions.stream()
                    .filter(tx
                            -> tx.getDescription().toLowerCase().contains(
                                newValue.toLowerCase()))
                    .toList();

            if (filteredSuggestions.size() > Constants.SUGGESTIONS_MAX_ITEMS)
            {
                filteredSuggestions =
                    filteredSuggestions.subList(0, Constants.SUGGESTIONS_MAX_ITEMS);
            }

            suggestionListView.getItems().addAll(filteredSuggestions);

            try
            {
                if (!filteredSuggestions.isEmpty())
                {
                    adjustPopupWidth();
                    adjustPopupHeight();

                    suggestionsPopup.show(
                        descriptionField,
                        descriptionField.localToScene(0, 0).getX() +
                            descriptionField.getScene().getWindow().getX() +
                            descriptionField.getScene().getX(),
                        descriptionField.localToScene(0, 0).getY() +
                            descriptionField.getScene().getWindow().getY() +
                            descriptionField.getScene().getY() +
                            descriptionField.getHeight());
                }
                else
                {
                    suggestionsPopup.hide();
                }
            }
            catch (NullPointerException e)
            {
                logger.error("Error showing suggestions popup");
            }
        };

        descriptionField.textProperty().addListener(descriptionFieldListener);
    }

    protected void configureSuggestionsPopup()
    {
        if (suggestionsPopup == null)
        {
            configureSuggestionsListView();
        }

        suggestionsPopup = new Popup();
        suggestionsPopup.setAutoHide(true);
        suggestionsPopup.setHideOnEscape(true);
        suggestionsPopup.getContent().add(suggestionListView);
    }

    protected void adjustPopupWidth()
    {
        suggestionListView.setPrefWidth(descriptionField.getWidth());
    }

    protected void adjustPopupHeight()
    {
        Integer itemCount = suggestionListView.getItems().size();

        Double cellHeight = 45.0;

        itemCount = Math.min(itemCount, Constants.SUGGESTIONS_MAX_ITEMS);

        Double totalHeight = itemCount * cellHeight;

        suggestionListView.setPrefHeight(totalHeight);
    }

    protected void configureSuggestionsListView()
    {
        suggestionListView = new ListView<>();

        // Set the cell factory to display the description, amount, wallet and
        // category of the transaction
        // Format:
        //    Description
        //    Amount | CreditCard | Category | Installments
        suggestionListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(CreditCardDebt item, boolean empty)
            {
                super.updateItem(item, empty);
                if (empty || item == null)
                {
                    setText(null);
                }
                else
                {
                    VBox cellContent = new VBox();
                    cellContent.setSpacing(2);

                    Label descriptionLabel = new Label(item.getDescription());

                    String infoString = UIUtils.formatCurrency(item.getAmount()) +
                                        " | " + item.getCreditCard().getName() +
                                        " | " + item.getCategory().getName() + " | " +
                                        item.getInstallments() + "x";

                    Label infoLabel = new Label(infoString);

                    cellContent.getChildren().addAll(descriptionLabel, infoLabel);

                    setGraphic(cellContent);
                }
            }
        });

        suggestionListView.setPrefWidth(Region.USE_COMPUTED_SIZE);
        suggestionListView.setPrefHeight(Region.USE_COMPUTED_SIZE);

        // By default, the SPACE key is used to select an item in the ListView.
        // This behavior is not desired in this case, so the event is consumed
        suggestionListView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE)
            {
                event.consume(); // Do not propagate the event
            }
        });

        // Add a listener to the ListView to fill the fields with the selected
        suggestionListView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null)
                {
                    fillFieldsWithTransaction(newValue);
                    suggestionsPopup.hide();
                }
            });
    }

    protected void fillFieldsWithTransaction(CreditCardDebt ccd)
    {
        crcComboBox.setValue(ccd.getCreditCard());

        // Deactivate the listener to avoid the event of changing the text of
        // the descriptionField from being triggered. After changing the text,
        // the listener is activated again
        descriptionField.textProperty().removeListener(descriptionFieldListener);

        descriptionField.setText(ccd.getDescription());

        descriptionField.textProperty().addListener(descriptionFieldListener);

        valueField.setText(ccd.getAmount().toString());
        installmentsField.setText(ccd.getInstallments().toString());
        categoryComboBox.setValue(ccd.getCategory());

        updateAvailableLimitAfterDebtLabel();
        updateMsgLabel();
    }
}
