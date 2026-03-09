/*
 * Filename: AddCreditCardCreditController.java
 * Created on: October 25, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.creditcard;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.creditcard.CreditCard;
import org.moinex.model.creditcard.CreditCardCredit;
import org.moinex.model.enums.CreditCardCreditType;
import org.moinex.service.CalculatorService;
import org.moinex.service.CreditCardService;
import org.moinex.service.PreferencesService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.SuggestionsHandlerHelper;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/** Controller for the Add Credit Card Credit dialog */
@Controller
@NoArgsConstructor
public class AddCreditCardCreditController {
    @FXML private ComboBox<CreditCard> crcComboBox;

    @FXML private TextField descriptionField;

    @FXML private TextField valueField;

    @FXML private DatePicker datePicker;

    @FXML private ComboBox<CreditCardCreditType> creditTypeComboBox;

    private ConfigurableApplicationContext springContext;

    private SuggestionsHandlerHelper<CreditCardCredit> suggestionsHandler;

    private List<CreditCard> creditCards;

    private CreditCardService creditCardService;

    private CalculatorService calculatorService;

    private PreferencesService preferencesService;

    @Autowired
    public AddCreditCardCreditController(
            CreditCardService creditCardService,
            CalculatorService calculatorService,
            ConfigurableApplicationContext springContext,
            PreferencesService preferencesService) {
        this.creditCardService = creditCardService;
        this.calculatorService = calculatorService;
        this.springContext = springContext;
        this.preferencesService = preferencesService;
    }

    public void setCreditCard(CreditCard crc) {
        if (creditCards.stream().noneMatch(c -> c.getId().equals(crc.getId()))) {
            return;
        }

        crcComboBox.setValue(crc);
    }

    @FXML
    private void initialize() {
        configureSuggestions();
        configureListeners();
        configureComboBoxes();

        loadCreditCardsFromDatabase();
        loadSuggestionsFromDatabase();

        populateCreditCardCreditTypeComboBox();
        populateCreditCardCombobox();

        // Configure date picker
        UIUtils.setDatePickerFormat(datePicker, preferencesService);
    }

    @FXML
    private void handleSave() {
        CreditCard crc = crcComboBox.getValue();
        String description = descriptionField.getText().strip();
        String valueStr = valueField.getText();
        CreditCardCreditType creditType = creditTypeComboBox.getValue();
        LocalDate date = datePicker.getValue();

        if (crc == null
                || creditType == null
                || date == null
                || description.isEmpty()
                || valueStr.isEmpty()) {

            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_EMPTY_FIELDS_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_EMPTY_FIELDS_MESSAGE));
            return;
        }

        try {
            BigDecimal creditValue = new BigDecimal(valueStr);

            LocalTime currentTime = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = date.atTime(currentTime);

            creditCardService.addRebate(
                    new CreditCardCredit(
                            null, // id (auto-generated)
                            creditType, // type
                            crc, // creditCard
                            dateTimeWithCurrentHour, // date
                            creditValue, // amount
                            description // description
                            ));

            WindowUtils.showSuccessDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_CREDIT_CREATED_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_CREDIT_CREATED_MESSAGE));

            Stage stage = (Stage) crcComboBox.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_INVALID_CREDIT_VALUE_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .CREDITCARD_DIALOG_INVALID_CREDIT_VALUE_MESSAGE));
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_ERROR_CREATING_DEBT_TITLE),
                    e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) crcComboBox.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleOpenCalculator() {

        WindowUtils.openPopupWindow(
                Constants.CALCULATOR_FXML,
                "Calculator",
                springContext,
                (CalculatorController controller) -> {},
                List.of(() -> calculatorService.updateComponentWithResult(valueField)));
    }

    private void loadCreditCardsFromDatabase() {
        creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByName();
    }

    private void loadSuggestionsFromDatabase() {
        suggestionsHandler.setSuggestions(creditCardService.getCreditCardCreditSuggestions());
    }

    private void populateCreditCardCreditTypeComboBox() {
        creditTypeComboBox.getItems().addAll(CreditCardCreditType.values());
    }

    private void populateCreditCardCombobox() {
        crcComboBox.getItems().addAll(creditCards);
    }

    private void configureComboBoxes() {
        UIUtils.configureComboBox(crcComboBox, CreditCard::getName);
        UIUtils.configureComboBox(
                creditTypeComboBox,
                type -> UIUtils.translateCreditCardCreditType(type, preferencesService));
    }

    private void configureSuggestions() {
        Function<CreditCardCredit, String> filterFunction = CreditCardCredit::getDescription;

        // Format:
        //    Description
        //    Amount | CreditCard | Rebate Type
        Function<CreditCardCredit, String> displayFunction =
                ccc ->
                        String.format(
                                "%s%n%s | %s | %s",
                                ccc.getDescription(),
                                UIUtils.formatCurrency(ccc.getAmount()),
                                ccc.getCreditCard().getName(),
                                ccc.getType());

        Consumer<CreditCardCredit> onSelectCallback = this::fillFieldsWithTransaction;

        suggestionsHandler =
                new SuggestionsHandlerHelper<>(
                        descriptionField, filterFunction, displayFunction, onSelectCallback);

        suggestionsHandler.enable();
    }

    private void configureListeners() {
        valueField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                valueField.setText(oldValue);
                            }
                        });
    }

    private void fillFieldsWithTransaction(CreditCardCredit ccc) {
        crcComboBox.setValue(ccc.getCreditCard());

        // Deactivate the listener to avoid the event of changing the text of
        // the descriptionField from being triggered. After changing the text,
        // the listener is activated again
        suggestionsHandler.disable();
        descriptionField.setText(ccc.getDescription());
        suggestionsHandler.enable();

        valueField.setText(ccc.getAmount().toString());

        creditTypeComboBox.setValue(ccc.getType());
    }
}
