/*
 * Filename: AddCreditCardCreditController.java
 * Created on: October 25, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
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
import org.moinex.entities.CreditCard;
import org.moinex.entities.CreditCardCredit;
import org.moinex.services.CalculatorService;
import org.moinex.services.CreditCardService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.CreditCardCreditType;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Credit Card Credit dialog
 */
@Controller
@NoArgsConstructor
public class AddCreditCardCreditController
{
    @FXML
    private ComboBox<String> crcComboBox;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField valueField;

    @FXML
    private DatePicker datePicker;

    @FXML
    private ComboBox<CreditCardCreditType> creditTypeComboBox;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private Popup suggestionsPopup;

    private ListView<CreditCardCredit> suggestionListView;

    private List<CreditCardCredit> suggestions;

    private ChangeListener<String> descriptionFieldListener;

    private List<CreditCard> creditCards;

    private CreditCardService creditCardService;

    private CalculatorService calculatorService;

    @Autowired
    public AddCreditCardCreditController(CreditCardService creditCardService,
                                         CalculatorService calculatorService)
    {
        this.creditCardService = creditCardService;
        this.calculatorService = calculatorService;
    }

    public void setCreditCard(CreditCard crc)
    {
        if (creditCards.stream().noneMatch(c -> c.getId() == crc.getId()))
        {
            return;
        }

        crcComboBox.setValue(crc.getName());
    }

    @FXML
    private void initialize()
    {
        loadCreditCardsFromDatabase();
        loadSuggestionsFromDatabase();

        // Configure date picker
        UIUtils.setDatePickerFormat(datePicker);

        populateCreditCardCreditTypeComboBox();
        populateCreditCardCombobox();

        configureSuggestionsListView();
        configureSuggestionsPopup();
        configureListeners();
    }

    @FXML
    private void handleSave()
    {
        String               crcName     = crcComboBox.getValue();
        String               description = descriptionField.getText().strip();
        String               valueStr    = valueField.getText();
        CreditCardCreditType creditType  = creditTypeComboBox.getValue();
        LocalDate            date        = datePicker.getValue();

        if (crcName == null || crcName.isEmpty() || creditType == null ||
            date == null || description.isEmpty() || valueStr.isEmpty())

        {
            WindowUtils.showErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all the fields");
            return;
        }

        try
        {
            BigDecimal creditValue = new BigDecimal(valueStr);

            CreditCard crc = creditCards.stream()
                                 .filter(c -> c.getName().equals(crcName))
                                 .findFirst()
                                 .get();

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = date.atTime(currentTime);

            creditCardService.addCredit(crc.getId(),
                                        dateTimeWithCurrentHour,
                                        creditValue,
                                        creditType,
                                        description);

            WindowUtils.showSuccessDialog("Success",
                                          "Credit created",
                                          "Credit created successfully");

            Stage stage = (Stage)crcComboBox.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Invalid expense value",
                                        "Credit value must be a number");
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

    @FXML
    private void handleOpenCalculator()
    {

        WindowUtils.openPopupWindow(Constants.CALCULATOR_FXML,
                                    "Calculator",
                                    springContext,
                                    (CalculatorController controller)
                                        -> {},
                                    List.of(() -> { getResultFromCalculator(); }));
    }

    private void getResultFromCalculator()
    {
        // If the user saved the result, set it in the incomeValueField
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

                valueField.setText(result);
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

    private void loadCreditCardsFromDatabase()
    {
        creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByName();
    }

    private void loadSuggestionsFromDatabase()
    {
        suggestions = creditCardService.getCreditCardCreditSuggestions();
    }

    private void populateCreditCardCreditTypeComboBox()
    {
        creditTypeComboBox.getItems().addAll(CreditCardCreditType.values());
    }

    private void populateCreditCardCombobox()
    {
        crcComboBox.getItems().addAll(
            creditCards.stream().map(CreditCard::getName).toList());
    }

    private void configureListeners()
    {
        valueField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
            {
                valueField.setText(oldValue);
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
            List<CreditCardCredit> filteredSuggestions =
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
        };

        descriptionField.textProperty().addListener(descriptionFieldListener);
    }

    private void configureSuggestionsPopup()
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

    private void adjustPopupWidth()
    {
        suggestionListView.setPrefWidth(descriptionField.getWidth());
    }

    private void adjustPopupHeight()
    {
        Integer itemCount = suggestionListView.getItems().size();

        Double cellHeight = 45.0;

        itemCount = Math.min(itemCount, Constants.SUGGESTIONS_MAX_ITEMS);

        Double totalHeight = itemCount * cellHeight;

        suggestionListView.setPrefHeight(totalHeight);
    }

    private void configureSuggestionsListView()
    {
        suggestionListView = new ListView<>();

        // Set the cell factory to display the description, amount, wallet and
        // category of the transaction
        // Format:
        //    Description
        //    Amount | CreditCard | Rebate Type
        suggestionListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(CreditCardCredit item, boolean empty)
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
                                        " | " + item.getType();

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

    private void fillFieldsWithTransaction(CreditCardCredit ccd)
    {
        crcComboBox.setValue(ccd.getCreditCard().getName());

        // Deactivate the listener to avoid the event of changing the text of
        // the descriptionField from being triggered. After changing the text,
        // the listener is activated again
        descriptionField.textProperty().removeListener(descriptionFieldListener);

        descriptionField.setText(ccd.getDescription());

        descriptionField.textProperty().addListener(descriptionFieldListener);

        valueField.setText(ccd.getAmount().toString());

        creditTypeComboBox.setValue(ccd.getType());
    }
}
