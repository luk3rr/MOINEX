/*
 * Filename: AddTransferController.java
 * Created on: October  4, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.wallettransaction.Transfer;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.error.MoinexException;
import org.moinex.service.CalculatorService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.SuggestionsHandlerHelper;
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

    private ConfigurableApplicationContext springContext;

    private SuggestionsHandlerHelper<Transfer> suggestionsHandler;

    private WalletService walletService;

    private WalletTransactionService walletTransactionService;

    private CalculatorService calculatorService;

    private List<Wallet> wallets;

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
                                 CalculatorService        calculatorService, ConfigurableApplicationContext springContext)
    {
        this.walletService            = walletService;
        this.walletTransactionService = walletTransactionService;
        this.calculatorService        = calculatorService;
        this.springContext = springContext;
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
        configureSuggestions();
        configureListeners();
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
            transferValueString == null || transferValueString.isBlank() ||
            description == null || description.isBlank() ||
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
        catch (MoinexException.SameSourceDestinationException | IllegalArgumentException |
               EntityNotFoundException | MoinexException.InsufficientResourcesException e)
        {
            WindowUtils.showErrorDialog("Error while creating transfer",
                                        e.getMessage());
        }
    }

    @FXML
    private void handleOpenCalculator()
    {
        WindowUtils.openPopupWindow(
            Constants.CALCULATOR_FXML,
            "Calculator",
            springContext,
            (CalculatorController controller)
                -> {},
            List.of(
                () -> calculatorService.updateComponentWithResult(transferValueField)));
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

        if (transferValueString == null || transferValueString.isBlank() ||
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

            // Epsilon is used to avoid floating point arithmetic errors
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

        if (transferValueString == null || transferValueString.isBlank() ||
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

            // Epsilon is used to avoid floating point arithmetic errors
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
    }

    private void loadWalletsFromDatabase()
    {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();
    }

    private void loadSuggestionsFromDatabase()
    {
        suggestionsHandler.setSuggestions(
            walletTransactionService.getTransferSuggestions());
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

    private void configureSuggestions()
    {
        Function<Transfer, String> filterFunction = Transfer::getDescription;

        // Format:
        //    Description
        //    Amount | From: Wallet | To: Wallet
        Function<Transfer, String> displayFunction = tf
            -> String.format("%s\n%s | From: %s | To: %s ",
                             tf.getDescription(),
                             UIUtils.formatCurrency(tf.getAmount()),
                             tf.getSenderWallet().getName(),
                             tf.getReceiverWallet().getName());

        Consumer<Transfer> onSelectCallback =
                this::fillFieldsWithTransaction;

        suggestionsHandler = new SuggestionsHandlerHelper<>(descriptionField,
                                                            filterFunction,
                                                            displayFunction,
                                                            onSelectCallback);

        suggestionsHandler.enable();
    }

    private void fillFieldsWithTransaction(Transfer t)
    {
        senderWalletComboBox.setValue(t.getSenderWallet());
        receiverWalletComboBox.setValue(t.getReceiverWallet());

        // Deactivate the listener to avoid the event of changing the text of
        // the descriptionField from being triggered. After changing the text,
        // the listener is activated again
        suggestionsHandler.disable();
        descriptionField.setText(t.getDescription());
        suggestionsHandler.enable();

        transferValueField.setText(t.getAmount().toString());

        updateSenderWalletBalance();
        updateSenderWalletAfterBalance();

        updateReceiverWalletBalance();
        updateReceiverWalletAfterBalance();
    }
}
