/*
 * Filename: AddTransferController.java
 * Created on: October  4, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityNotFoundException;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.error.MoinexException;
import org.moinex.model.wallettransaction.Transfer;
import org.moinex.model.wallettransaction.Wallet;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Controller for the Add Transfer dialog
 */
@Controller
@NoArgsConstructor
public class AddTransferController {
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

    /**
     * Constructor
     *
     * @param walletService            WalletService
     * @param walletTransactionService WalletTransactionService
     * @param calculatorService        CalculatorService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddTransferController(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            CalculatorService calculatorService,
            ConfigurableApplicationContext springContext) {
        this.walletService = walletService;
        this.walletTransactionService = walletTransactionService;
        this.calculatorService = calculatorService;
        this.springContext = springContext;
    }

    public void setSenderWalletComboBox(Wallet wt) {
        if (wallets.stream().noneMatch(w -> w.getId().equals(wt.getId()))) {
            return;
        }

        senderWalletComboBox.setValue(wt);
        updateSenderWalletBalance();
    }

    public void setReceiverWalletComboBox(Wallet wt) {
        if (wallets.stream().noneMatch(w -> w.getId().equals(wt.getId()))) {
            return;
        }

        receiverWalletComboBox.setValue(wt);
        updateReceiverWalletBalance();
    }

    @FXML
    private void initialize() {
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

        senderWalletComboBox.setOnAction(
                e -> {
                    updateSenderWalletBalance();
                    updateSenderWalletAfterBalance();
                });

        receiverWalletComboBox.setOnAction(
                e -> {
                    updateReceiverWalletBalance();
                    updateReceiverWalletAfterBalance();
                });
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) descriptionField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSave() {
        Wallet senderWt = senderWalletComboBox.getValue();
        Wallet receiverWt = receiverWalletComboBox.getValue();
        String transferValueString = transferValueField.getText();
        String description = descriptionField.getText();
        LocalDate transferDate = transferDatePicker.getValue();

        if (senderWt == null
                || receiverWt == null
                || transferValueString == null
                || transferValueString.isBlank()
                || description == null
                || description.isBlank()
                || transferDate == null) {
            WindowUtils.showInformationDialog(
                    "Empty fields", "Please fill all required fields before saving");
            return;
        }

        try {
            BigDecimal transferValue = new BigDecimal(transferValueString);

            LocalTime currentTime = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = transferDate.atTime(currentTime);

            walletTransactionService.transferMoney(
                    senderWt.getId(),
                    receiverWt.getId(),
                    dateTimeWithCurrentHour,
                    transferValue,
                    description);

            WindowUtils.showSuccessDialog(
                    "Transfer created", "The transfer was successfully created.");

            Stage stage = (Stage) descriptionField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    "Invalid transfer value", "Transfer value must be a number.");
        } catch (MoinexException.SameSourceDestinationException
                 | IllegalArgumentException
                 | EntityNotFoundException
                 | IllegalStateException
                 | MoinexException.InsufficientResourcesException
                 | MoinexException.TransferFromMasterToVirtualWalletException e) {
            WindowUtils.showErrorDialog("Error while creating transfer", e.getMessage());
        }
    }

    @FXML
    private void handleOpenCalculator() {
        WindowUtils.openPopupWindow(
                Constants.CALCULATOR_FXML,
                "Calculator",
                springContext,
                (CalculatorController controller) -> {
                },
                List.of(() -> calculatorService.updateComponentWithResult(transferValueField)));
    }

    private void updateSenderWalletBalance() {
        Wallet senderWt = senderWalletComboBox.getValue();

        if (senderWt == null) {
            return;
        }

        if (senderWt.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            UIUtils.setLabelStyle(
                    senderWalletCurrentBalanceValueLabel, Constants.NEGATIVE_BALANCE_STYLE);
        } else {
            UIUtils.setLabelStyle(
                    senderWalletCurrentBalanceValueLabel, Constants.NEUTRAL_BALANCE_STYLE);
        }

        senderWalletCurrentBalanceValueLabel.setText(UIUtils.formatCurrency(senderWt.getBalance()));
    }

    private void updateReceiverWalletBalance() {
        Wallet receiverWt = receiverWalletComboBox.getValue();

        if (receiverWt == null) {
            return;
        }

        if (receiverWt.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            UIUtils.setLabelStyle(
                    receiverWalletCurrentBalanceValueLabel, Constants.NEGATIVE_BALANCE_STYLE);
        } else {
            UIUtils.setLabelStyle(
                    receiverWalletCurrentBalanceValueLabel, Constants.NEUTRAL_BALANCE_STYLE);
        }

        receiverWalletCurrentBalanceValueLabel.setText(
                UIUtils.formatCurrency(receiverWt.getBalance()));
    }

    /**
     * Updates the projected balance of a wallet (sender or receiver) after a transfer,
     * considering the rules for virtual and master wallets.
     *
     * @param currentWallet The wallet whose balance is being calculated (sender or receiver)
     * @param otherWallet   The other wallet involved in the transaction
     * @param label         The UI Label to be updated
     * @param isSender      True if the currentWallet is the sender, false if it is the receiver
     */
    private void updateAfterBalance(
            Wallet currentWallet, Wallet otherWallet, Label label, boolean isSender) {
        String transferValueString = transferValueField.getText();

        if (transferValueString == null || transferValueString.isBlank() || currentWallet == null || currentWallet.equals(otherWallet)) {
            UIUtils.resetLabel(label);
            return;
        }

        try {
            BigDecimal transferValue = new BigDecimal(transferValueString);

            if (transferValue.compareTo(BigDecimal.ZERO) < 0) {
                UIUtils.resetLabel(label);
                return;
            }

            BigDecimal afterBalance;

            if (isSender) {
                if (currentWallet.isMaster() && otherWallet.isVirtual() && otherWallet.getMasterWallet().equals(currentWallet)) {
                    // If the current wallet is the SENDER and is a master wallet of the receiver,
                    // then an error is thrown... BUT be a buddy and show a message to the user :)
                    WindowUtils.showInformationDialog(
                            "Invalid Transfer",
                            "You cannot transfer money from a master wallet to its virtual wallet."
                    );
                    return;
                }

                afterBalance = currentWallet.getBalance().subtract(transferValue);
            } else {
                // If the current wallet is the RECEIVER, the logic depends on its relationship with
                // the sender
                // Scenario: Virtual -> Linked Master Wallet
                // If the sender is a virtual wallet and the receiver is its master wallet,
                // the money is already in the total fund. It's just an internal reorganization
                if (otherWallet != null
                        && otherWallet.isVirtual()
                        && otherWallet.getMasterWallet().equals(currentWallet)) {
                    // The total balance of the master wallet does not change.
                    afterBalance = currentWallet.getBalance();
                } else {
                    // For all other cases (Master -> Master, Master -> Virtual, etc.),
                    // the receiver's balance simply increases.
                    afterBalance = currentWallet.getBalance().add(transferValue);
                }
            }

            if (afterBalance.compareTo(BigDecimal.ZERO) < 0) {
                UIUtils.setLabelStyle(label, Constants.NEGATIVE_BALANCE_STYLE);
            } else {
                UIUtils.setLabelStyle(label, Constants.NEUTRAL_BALANCE_STYLE);
            }

            label.setText(UIUtils.formatCurrency(afterBalance));

        } catch (NumberFormatException e) {
            UIUtils.resetLabel(label);
        }
    }

    /**
     * Wrapper to update the projected balance of the sender wallet
     */
    private void updateSenderWalletAfterBalance() {
        updateAfterBalance(
                senderWalletComboBox.getValue(),
                receiverWalletComboBox.getValue(),
                senderWalletAfterBalanceValueLabel,
                true);
    }

    /**
     * Wrapper to update the projected balance of the receiver wallet
     */
    private void updateReceiverWalletAfterBalance() {
        updateAfterBalance(
                receiverWalletComboBox.getValue(),
                senderWalletComboBox.getValue(),
                receiverWalletAfterBalanceValueLabel,
                false);
    }

    private void configureListeners() {
        // Update sender wallet after balance when transfer value changes
        transferValueField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                transferValueField.setText(oldValue);
                            } else {
                                updateSenderWalletAfterBalance();
                                updateReceiverWalletAfterBalance();
                            }
                        });
    }

    private void loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();
    }

    private void loadSuggestionsFromDatabase() {
        suggestionsHandler.setSuggestions(walletTransactionService.getTransferSuggestions());
    }

    private void populateComboBoxes() {
        senderWalletComboBox.getItems().setAll(wallets);
        receiverWalletComboBox.getItems().setAll(wallets);
    }

    private void configureComboBoxes() {
        UIUtils.configureComboBox(senderWalletComboBox, Wallet::getName);
        UIUtils.configureComboBox(receiverWalletComboBox, Wallet::getName);
    }

    private void configureSuggestions() {
        Function<Transfer, String> filterFunction = Transfer::getDescription;

        // Format:
        //    Description
        //    Amount | From: Wallet | To: Wallet
        Function<Transfer, String> displayFunction =
                tf ->
                        String.format(
                                "%s%n%s | From: %s | To: %s ",
                                tf.getDescription(),
                                UIUtils.formatCurrency(tf.getAmount()),
                                tf.getSenderWallet().getName(),
                                tf.getReceiverWallet().getName());

        Consumer<Transfer> onSelectCallback = this::fillFieldsWithTransaction;

        suggestionsHandler =
                new SuggestionsHandlerHelper<>(
                        descriptionField, filterFunction, displayFunction, onSelectCallback);

        suggestionsHandler.enable();
    }

    private void fillFieldsWithTransaction(Transfer t) {
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
