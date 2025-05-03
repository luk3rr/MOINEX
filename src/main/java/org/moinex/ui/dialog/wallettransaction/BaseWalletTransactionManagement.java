/*
 * Filename: BaseWalletTransactionManagement.java
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import java.math.BigDecimal;
import java.util.Arrays;
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
import org.moinex.model.Category;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.service.CalculatorService;
import org.moinex.service.CategoryService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.SuggestionsHandlerHelper;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Base class for the wallet transaction dialog controllers
 */
@NoArgsConstructor
public abstract class BaseWalletTransactionManagement {
    @FXML protected Label walletAfterBalanceValueLabel;

    @FXML protected Label walletCurrentBalanceValueLabel;

    @FXML protected ComboBox<Wallet> walletComboBox;

    @FXML protected ComboBox<TransactionStatus> statusComboBox;

    @FXML protected ComboBox<Category> categoryComboBox;

    @FXML protected TextField transactionValueField;

    @FXML protected TextField descriptionField;

    @FXML protected DatePicker transactionDatePicker;

    protected ConfigurableApplicationContext springContext;

    protected SuggestionsHandlerHelper<WalletTransaction> suggestionsHandler;

    protected WalletService walletService;

    protected WalletTransactionService walletTransactionService;

    protected CategoryService categoryService;

    protected CalculatorService calculatorService;

    protected List<Wallet> wallets;

    protected List<Category> categories;

    protected Wallet wallet = null;

    protected TransactionType transactionType;

    /**
     * Constructor
     *
     * @param walletService            WalletService
     * @param walletTransactionService WalletTransactionService
     * @param categoryService          CategoryService
     * @param calculatorService        CalculatorService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    protected BaseWalletTransactionManagement(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            CategoryService categoryService,
            CalculatorService calculatorService) {
        this.walletService = walletService;
        this.walletTransactionService = walletTransactionService;
        this.categoryService = categoryService;
        this.calculatorService = calculatorService;
    }

    @FXML
    protected abstract void handleSave();

    protected abstract void loadSuggestionsFromDatabase();

    public void setWalletComboBox(Wallet wt) {
        if (wallets.stream().noneMatch(w -> w.getId().equals(wt.getId()))) {
            return;
        }

        wallet = wt;

        walletComboBox.setValue(wallet);
        UIUtils.updateWalletBalance(wallet, walletCurrentBalanceValueLabel);
    }

    @FXML
    protected void initialize() {
        configureSuggestions();
        configureListeners();
        configureComboBoxes();

        loadWalletsFromDatabase();
        loadCategoriesFromDatabase();
        loadSuggestionsFromDatabase();

        populateComboBoxes();

        // Configure date picker
        UIUtils.setDatePickerFormat(transactionDatePicker);

        // Reset all labels
        UIUtils.resetLabel(walletAfterBalanceValueLabel);
        UIUtils.resetLabel(walletCurrentBalanceValueLabel);

        walletComboBox.setOnAction(
                e -> {
                    UIUtils.updateWalletBalance(
                            walletComboBox.getValue(), walletCurrentBalanceValueLabel);
                    walletAfterBalance();
                });
    }

    @FXML
    protected void handleCancel() {
        Stage stage = (Stage) descriptionField.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void handleOpenCalculator() {
        WindowUtils.openPopupWindow(
                Constants.CALCULATOR_FXML,
                "Calculator",
                springContext,
                (CalculatorController controller) -> {},
                List.of(() -> calculatorService.updateComponentWithResult(transactionValueField)));
    }

    protected void walletAfterBalance() {
        String transactionValueString = transactionValueField.getText();
        Wallet wt = walletComboBox.getValue();

        if (transactionValueString == null || transactionValueString.isBlank() || wt == null) {
            UIUtils.resetLabel(walletAfterBalanceValueLabel);
            return;
        }

        try {
            BigDecimal transactionValue = new BigDecimal(transactionValueString);

            if (transactionValue.compareTo(BigDecimal.ZERO) < 0) {
                UIUtils.resetLabel(walletAfterBalanceValueLabel);
                return;
            }

            BigDecimal walletAfterBalanceValue = getBigDecimal(wt, transactionValue);

            // Set the style according to the balance value after the transaction
            if (walletAfterBalanceValue.compareTo(BigDecimal.ZERO) < 0) {
                // Remove old style and add negative style
                UIUtils.setLabelStyle(
                        walletAfterBalanceValueLabel, Constants.NEGATIVE_BALANCE_STYLE);
            } else {
                // Remove old style and add neutral style
                UIUtils.setLabelStyle(
                        walletAfterBalanceValueLabel, Constants.NEUTRAL_BALANCE_STYLE);
            }

            walletAfterBalanceValueLabel.setText(UIUtils.formatCurrency(walletAfterBalanceValue));
        } catch (NumberFormatException e) {
            UIUtils.resetLabel(walletAfterBalanceValueLabel);
        }
    }

    private BigDecimal getBigDecimal(Wallet wallet, BigDecimal transactionValue) {
        if (transactionType == TransactionType.EXPENSE) {
            return wallet.getBalance().subtract(transactionValue);
        } else if (transactionType == TransactionType.INCOME) {
            return wallet.getBalance().add(transactionValue);
        }
        throw new IllegalStateException("Invalid transaction type");
    }

    protected void loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();
    }

    protected void loadCategoriesFromDatabase() {
        categories = categoryService.getNonArchivedCategoriesOrderedByName();
    }

    protected void populateComboBoxes() {
        walletComboBox.getItems().setAll(wallets);
        statusComboBox.getItems().addAll(Arrays.asList(TransactionStatus.values()));
        categoryComboBox.getItems().setAll(categories);

        // If there are no categories, add a tooltip to the categoryComboBox
        // to inform the user that a category is needed
        if (categories.isEmpty()) {
            UIUtils.addTooltipToNode(
                    categoryComboBox, "You need to add a category before adding a transaction");
        }
    }

    protected void configureComboBoxes() {
        UIUtils.configureComboBox(walletComboBox, Wallet::getName);
        UIUtils.configureComboBox(statusComboBox, TransactionStatus::name);
        UIUtils.configureComboBox(categoryComboBox, Category::getName);
    }

    protected void configureSuggestions() {
        Function<WalletTransaction, String> filterFunction = WalletTransaction::getDescription;

        // Format:
        //    Description
        //    Amount | Wallet | Category
        Function<WalletTransaction, String> displayFunction =
                wt ->
                        String.format(
                                "%s%n%s | %s | %s ",
                                wt.getDescription(),
                                UIUtils.formatCurrency(wt.getAmount()),
                                wt.getWallet().getName(),
                                wt.getCategory().getName());

        Consumer<WalletTransaction> onSelectCallback = this::fillFieldsWithTransaction;

        suggestionsHandler =
                new SuggestionsHandlerHelper<>(
                        descriptionField, filterFunction, displayFunction, onSelectCallback);

        suggestionsHandler.enable();
    }

    protected void configureListeners() {
        // Update wallet after balance when the value field changes
        transactionValueField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                transactionValueField.setText(oldValue);
                            } else {
                                walletAfterBalance();
                            }
                        });
    }

    protected void fillFieldsWithTransaction(WalletTransaction wt) {
        walletComboBox.setValue(wt.getWallet());

        // Deactivate the listener to avoid the event of changing the text of
        // the descriptionField from being triggered. After changing the text,
        // the listener is activated again
        suggestionsHandler.disable();
        descriptionField.setText(wt.getDescription());
        suggestionsHandler.enable();

        transactionValueField.setText(wt.getAmount().toString());
        statusComboBox.setValue(wt.getStatus());
        categoryComboBox.setValue(wt.getCategory());

        UIUtils.updateWalletBalance(walletComboBox.getValue(), walletCurrentBalanceValueLabel);
        walletAfterBalance();
    }
}
