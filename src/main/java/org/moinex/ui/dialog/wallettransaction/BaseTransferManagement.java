/*
 * Filename: AddTransferController.java
 * Created on: October  4, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.wallettransaction.Transfer;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.CalculatorService;
import org.moinex.service.CategoryService;
import org.moinex.service.I18nService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.SuggestionsHandlerHelper;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Controller for the Add Transfer dialog
 */
@NoArgsConstructor
public abstract class BaseTransferManagement {
    @FXML protected Label senderWalletAfterBalanceValueLabel;

    @FXML protected Label receiverWalletAfterBalanceValueLabel;

    @FXML protected Label senderWalletCurrentBalanceValueLabel;

    @FXML protected Label receiverWalletCurrentBalanceValueLabel;

    @FXML protected ComboBox<Wallet> senderWalletComboBox;

    @FXML protected ComboBox<Wallet> receiverWalletComboBox;

    @FXML protected TextField transferValueField;

    @FXML protected TextField descriptionField;

    @FXML protected DatePicker transferDatePicker;

    @FXML protected ComboBox<Category> categoryComboBox;

    protected ConfigurableApplicationContext springContext;

    protected WalletService walletService;

    protected WalletTransactionService walletTransactionService;

    protected CalculatorService calculatorService;

    protected CategoryService categoryService;

    protected I18nService i18nService;

    protected List<Wallet> wallets;

    protected List<Category> categories;

    private ChangeListener<String> transferValueListener;

    protected SuggestionsHandlerHelper<Transfer> suggestionsHandler;

    @Autowired
    protected BaseTransferManagement(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            CalculatorService calculatorService,
            CategoryService categoryService,
            I18nService i18nService,
            ConfigurableApplicationContext springContext) {
        this.walletService = walletService;
        this.walletTransactionService = walletTransactionService;
        this.calculatorService = calculatorService;
        this.categoryService = categoryService;
        this.i18nService = i18nService;
        this.springContext = springContext;
    }

    @FXML
    protected void initialize() {
        configureSuggestions();
        configureListeners();
        configureComboBoxes();

        loadWalletsFromDatabase();
        loadSuggestionsFromDatabase();
        loadCategoriesFromDatabase();

        populateComboBoxes();

        // Configure the date picker
        UIUtils.setDatePickerFormat(transferDatePicker, i18nService);

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
    protected void handleCancel() {
        Stage stage = (Stage) descriptionField.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected abstract void handleSave();

    protected abstract void updateAfterBalance(
            Wallet mainWallet, Wallet otherWallet, Label afterBalanceLabel, boolean isSender);

    @FXML
    protected void handleOpenCalculator() {
        WindowUtils.openPopupWindow(
                Constants.CALCULATOR_FXML,
                i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_LABEL_CALCULATOR),
                springContext,
                (CalculatorController controller) -> {},
                List.of(() -> calculatorService.updateComponentWithResult(transferValueField)));
    }

    protected void updateSenderWalletBalance() {
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

    protected void updateReceiverWalletBalance() {
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
     * Wrapper to update the projected balance of the sender wallet
     */
    protected void updateSenderWalletAfterBalance() {
        updateAfterBalance(
                senderWalletComboBox.getValue(),
                receiverWalletComboBox.getValue(),
                senderWalletAfterBalanceValueLabel,
                true);
    }

    /**
     * Wrapper to update the projected balance of the receiver wallet
     */
    protected void updateReceiverWalletAfterBalance() {
        updateAfterBalance(
                receiverWalletComboBox.getValue(),
                senderWalletComboBox.getValue(),
                receiverWalletAfterBalanceValueLabel,
                false);
    }

    protected void configureListeners() {
        // Update sender wallet after balance when transfer value changes
        transferValueListener =
                (observable, oldValue, newValue) -> {
                    if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                        transferValueField.setText(oldValue);
                    } else {
                        updateSenderWalletAfterBalance();
                        updateReceiverWalletAfterBalance();
                    }
                };

        transferValueField.textProperty().addListener(transferValueListener);
    }

    protected void loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();
    }

    protected void loadCategoriesFromDatabase() {
        categories = categoryService.getNonArchivedCategoriesOrderedByName();
    }

    protected void populateComboBoxes() {
        senderWalletComboBox.getItems().setAll(wallets);
        receiverWalletComboBox.getItems().setAll(wallets);

        // set the first as empty to allow no category
        categories.addFirst(null);

        categoryComboBox.getItems().setAll(categories);
    }

    protected void configureComboBoxes() {
        UIUtils.configureComboBox(senderWalletComboBox, Wallet::getName);
        UIUtils.configureComboBox(receiverWalletComboBox, Wallet::getName);
        UIUtils.configureComboBox(categoryComboBox, Category::getName);
    }

    private void loadSuggestionsFromDatabase() {
        suggestionsHandler.setSuggestions(walletTransactionService.getTransferSuggestions());
    }

    private void configureSuggestions() {
        Function<Transfer, String> filterFunction = Transfer::getDescription;

        // Format:
        //    Description
        //    Amount | From: Wallet | To: Wallet | Category
        Function<Transfer, String> displayFunction =
                tf ->
                        String.format(
                                "%s%n%s | "
                                        + i18nService.tr(
                                                Constants.TranslationKeys
                                                        .WALLETTRANSACTION_SUGGESTION_FROM)
                                        + " %s | "
                                        + i18nService.tr(
                                                Constants.TranslationKeys
                                                        .WALLETTRANSACTION_SUGGESTION_TO)
                                        + " %s | %s ",
                                tf.getDescription(),
                                UIUtils.formatCurrency(tf.getAmount()),
                                tf.getSenderWallet().getName(),
                                tf.getReceiverWallet().getName(),
                                tf.getCategory() != null
                                        ? tf.getCategory().getName()
                                        : i18nService.tr(
                                                Constants.TranslationKeys
                                                        .WALLETTRANSACTION_SUGGESTION_NO_CATEGORY));

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

        categoryComboBox.setValue(t.getCategory());

        updateSenderWalletBalance();
        updateSenderWalletAfterBalance();

        updateReceiverWalletBalance();
        updateReceiverWalletAfterBalance();
    }

    public void disableTransferValueListener() {
        if (transferValueListener != null) {
            transferValueField.textProperty().removeListener(transferValueListener);
        }
    }

    public void enableTransferValueListener() {
        if (transferValueListener != null) {
            transferValueField.textProperty().addListener(transferValueListener);
        }
    }
}
