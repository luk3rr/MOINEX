/*
 * Filename: AddTransferController.java
 * Created on: October  4, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityNotFoundException;
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
import org.moinex.entities.wallettransaction.Transfer;
import org.moinex.entities.wallettransaction.Wallet;
import org.moinex.exceptions.InsufficientResourcesException;
import org.moinex.exceptions.SameSourceDestionationException;
import org.moinex.services.CalculatorService;
import org.moinex.services.WalletService;
import org.moinex.services.WalletTransactionService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Transfer dialog
 */
@Controller
@NoArgsConstructor
public class AddTransferController
{
    @FXML
    private Label senderWalletAfterBalanceValueLabel;

    @FXML
    private Label receiverWalletAfterBalanceValueLabel;

    @FXML
    private Label senderWalletCurrentBalanceValueLabel;

    @FXML
    private Label receiverWalletCurrentBalanceValueLabel;

    @FXML
    private ComboBox<Wallet> senderWalletComboBox;

    @FXML
    private ComboBox<Wallet> receiverWalletComboBox;

    @FXML
    private TextField transferValueField;

    @FXML
    private TextField descriptionField;

    @FXML
    private DatePicker transferDatePicker;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private Popup suggestionsPopup;

    private ListView<Transfer> suggestionListView;

    private WalletService walletService;

    private WalletTransactionService walletTransactionService;

    private CalculatorService calculatorService;

    private List<Wallet> wallets;

    private List<Transfer> suggestions;

    private ChangeListener<String> descriptionFieldListener;

    private Wallet senderWallet = null;

    private Wallet receiverWallet = null;

    /**
     * Constructor
     * @param walletService WalletService
     * @param walletTransactionService WalletTransactionService
     * @param calculatorService CalculatorService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddTransferController(WalletService            walletService,
                                 WalletTransactionService walletTransactionService,
                                 CalculatorService        calculatorService)
    {
        this.walletService            = walletService;
        this.walletTransactionService = walletTransactionService;
        this.calculatorService        = calculatorService;
    }

    public void setSenderWalletComboBox(Wallet wt)
    {
        if (wallets.stream().noneMatch(w -> w.getId().equals(wt.getId())))
        {
            return;
        }

        this.senderWallet = wt;
        senderWalletComboBox.setValue(senderWallet);
        updateSenderWalletBalance();
    }

    public void setReceiverWalletComboBox(Wallet wt)
    {
        if (wallets.stream().noneMatch(w -> w.getId().equals(wt.getId())))
        {
            return;
        }

        this.receiverWallet = wt;
        receiverWalletComboBox.setValue(receiverWallet);
        updateReceiverWalletBalance();
    }

    @FXML
    private void initialize()
    {
        configureComboBoxes();

        loadWalletsFromDatabase();
        loadSuggestionsFromDatabase();

        populateComboBoxes();

        // Configure the date picker
        UIUtils.setDatePickerFormat(transferDatePicker);

        // Reset all labels
        UIUtils.resetLabel(senderWalletAfterBalanceValueLabel);
        UIUtils.resetLabel(receiverWalletAfterBalanceValueLabel);
        UIUtils.resetLabel(senderWalletCurrentBalanceValueLabel);
        UIUtils.resetLabel(receiverWalletCurrentBalanceValueLabel);

        senderWalletComboBox.setOnAction(e -> {
            updateSenderWalletBalance();
            updateSenderWalletAfterBalance();
        });

        receiverWalletComboBox.setOnAction(e -> {
            updateReceiverWalletBalance();
            updateReceiverWalletAfterBalance();
        });

        configureSuggestionsListView();
        configureSuggestionsPopup();
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
        Wallet    senderWallet        = senderWalletComboBox.getValue();
        Wallet    receiverWallet      = receiverWalletComboBox.getValue();
        String    transferValueString = transferValueField.getText();
        String    description         = descriptionField.getText();
        LocalDate transferDate        = transferDatePicker.getValue();

        if (senderWallet == null || receiverWallet == null ||
            transferValueString == null || transferValueString.strip().isEmpty() ||
            description == null || description.strip().isEmpty() ||
            transferDate == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");
            return;
        }

        try
        {
            BigDecimal transferValue = new BigDecimal(transferValueString);

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = transferDate.atTime(currentTime);

            walletTransactionService.transferMoney(senderWallet.getId(),
                                                   receiverWallet.getId(),
                                                   dateTimeWithCurrentHour,
                                                   transferValue,
                                                   description);

            WindowUtils.showSuccessDialog("Transfer created",
                                          "The transfer was successfully created.");

            Stage stage = (Stage)descriptionField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid transfer value",
                                        "Transfer value must be a number.");
        }
        catch (SameSourceDestionationException | IllegalArgumentException |
               EntityNotFoundException | InsufficientResourcesException e)
        {
            WindowUtils.showErrorDialog("Error while creating transfer",
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
        // If the user saved the result, set it in the transferValueField
        String result = calculatorService.getResult();

        if (result != null)
        {
            try
            {
                BigDecimal resultValue = new BigDecimal(result);

                if (resultValue.compareTo(BigDecimal.ZERO) < 0)
                {
                    WindowUtils.showErrorDialog("Invalid value",
                                                "The value must be positive");
                    return;
                }

                // Round the result to 2 decimal places
                result = resultValue.setScale(2, RoundingMode.HALF_UP).toString();

                transferValueField.setText(result);
            }
            catch (NumberFormatException e)
            {
                // Must be unreachable
                WindowUtils.showErrorDialog("Invalid value",
                                            "The value must be a number");
            }
        }
    }

    private void updateSenderWalletBalance()
    {
        Wallet senderWallet = senderWalletComboBox.getValue();

        if (senderWallet == null)
        {
            return;
        }

        if (senderWallet.getBalance().compareTo(BigDecimal.ZERO) < 0)
        {
            UIUtils.setLabelStyle(senderWalletCurrentBalanceValueLabel,
                                  Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            UIUtils.setLabelStyle(senderWalletCurrentBalanceValueLabel,
                                  Constants.NEUTRAL_BALANCE_STYLE);
        }

        senderWalletCurrentBalanceValueLabel.setText(
            UIUtils.formatCurrency(senderWallet.getBalance()));
    }

    private void updateReceiverWalletBalance()
    {
        Wallet receiverWallet = receiverWalletComboBox.getValue();

        if (receiverWallet == null)
        {
            return;
        }

        if (receiverWallet.getBalance().compareTo(BigDecimal.ZERO) < 0)
        {
            UIUtils.setLabelStyle(receiverWalletCurrentBalanceValueLabel,
                                  Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            UIUtils.setLabelStyle(receiverWalletCurrentBalanceValueLabel,
                                  Constants.NEUTRAL_BALANCE_STYLE);
        }

        receiverWalletCurrentBalanceValueLabel.setText(
            UIUtils.formatCurrency(receiverWallet.getBalance()));
    }

    private void updateSenderWalletAfterBalance()
    {
        String transferValueString = transferValueField.getText();
        Wallet senderWallet        = senderWalletComboBox.getValue();

        if (transferValueString == null || transferValueString.strip().isEmpty() ||
            senderWallet == null)
        {
            UIUtils.resetLabel(senderWalletAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal transferValue = new BigDecimal(transferValueString);

            if (transferValue.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(senderWalletAfterBalanceValueLabel);
                return;
            }

            BigDecimal senderWalletAfterBalance =
                senderWallet.getBalance().subtract(transferValue);

            // Episilon is used to avoid floating point arithmetic errors
            if (senderWalletAfterBalance.compareTo(BigDecimal.ZERO) < 0)
            {
                // Remove old style and add negative style
                UIUtils.setLabelStyle(senderWalletAfterBalanceValueLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                UIUtils.setLabelStyle(senderWalletAfterBalanceValueLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            senderWalletAfterBalanceValueLabel.setText(
                UIUtils.formatCurrency(senderWalletAfterBalance));
        }
        catch (NumberFormatException e)
        {
            UIUtils.resetLabel(senderWalletAfterBalanceValueLabel);
        }
    }

    private void updateReceiverWalletAfterBalance()
    {
        String transferValueString = transferValueField.getText();
        Wallet receiverWallet      = receiverWalletComboBox.getValue();

        if (transferValueString == null || transferValueString.strip().isEmpty() ||
            receiverWallet == null)
        {
            UIUtils.resetLabel(receiverWalletAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal transferValue = new BigDecimal(transferValueString);

            if (transferValue.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(receiverWalletAfterBalanceValueLabel);
                return;
            }

            BigDecimal receiverWalletAfterBalance =
                receiverWallet.getBalance().add(transferValue);

            // Episilon is used to avoid floating point arithmetic errors
            if (receiverWalletAfterBalance.compareTo(BigDecimal.ZERO) < 0)
            {
                // Remove old style and add negative style
                UIUtils.setLabelStyle(receiverWalletAfterBalanceValueLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                UIUtils.setLabelStyle(receiverWalletAfterBalanceValueLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            receiverWalletAfterBalanceValueLabel.setText(
                UIUtils.formatCurrency(receiverWalletAfterBalance));
        }
        catch (NumberFormatException e)
        {
            UIUtils.resetLabel(receiverWalletAfterBalanceValueLabel);
        }
    }

    private void configureListeners()
    {
        // Update sender wallet after balance when transfer value changes
        transferValueField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
                {
                    transferValueField.setText(oldValue);
                }
                else
                {
                    updateSenderWalletAfterBalance();
                    updateReceiverWalletAfterBalance();
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
            List<Transfer> filteredSuggestions =
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

    private void loadWalletsFromDatabase()
    {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();
    }

    private void loadSuggestionsFromDatabase()
    {
        suggestions = walletTransactionService.getTransferSuggestions();
    }

    private void populateComboBoxes()
    {
        senderWalletComboBox.getItems().setAll(wallets);
        receiverWalletComboBox.getItems().setAll(wallets);
    }

    private void configureComboBoxes()
    {
        UIUtils.configureComboBox(senderWalletComboBox, Wallet::getName);
        UIUtils.configureComboBox(receiverWalletComboBox, Wallet::getName);
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
        //    Amount | From: Wallet | To: Wallet
        suggestionListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Transfer item, boolean empty)
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

                    String infoString =
                        UIUtils.formatCurrency(item.getAmount()) +
                        " | From: " + item.getSenderWallet().getName() +
                        " | To: " + item.getReceiverWallet().getName();

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

    private void fillFieldsWithTransaction(Transfer t)
    {
        senderWalletComboBox.setValue(t.getSenderWallet());
        receiverWalletComboBox.setValue(t.getReceiverWallet());

        // Deactivate the listener to avoid the event of changing the text of
        // the descriptionField from being triggered. After changing the text,
        // the listener is activated again
        descriptionField.textProperty().removeListener(descriptionFieldListener);

        descriptionField.setText(t.getDescription());

        descriptionField.textProperty().addListener(descriptionFieldListener);

        transferValueField.setText(t.getAmount().toString());

        updateSenderWalletBalance();
        updateSenderWalletAfterBalance();

        updateReceiverWalletBalance();
        updateReceiverWalletAfterBalance();
    }
}
