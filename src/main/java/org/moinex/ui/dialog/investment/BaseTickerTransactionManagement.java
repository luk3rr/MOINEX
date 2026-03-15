/*
 * Filename: BaseTickerTransactionManagement.java
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.enums.WalletTransactionStatus;
import org.moinex.model.enums.WalletTransactionType;
import org.moinex.model.investment.Ticker;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.service.CategoryService;
import org.moinex.service.PreferencesService;
import org.moinex.service.TickerService;
import org.moinex.service.wallet.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.SuggestionsHandlerHelper;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;

/** Base class to implement common methods for BuyTickerController and SaleTickerController */
@NoArgsConstructor
public abstract class BaseTickerTransactionManagement {
    @FXML protected Label tickerNameLabel;

    @FXML protected Label walletAfterBalanceValueLabel;

    @FXML protected Label walletCurrentBalanceValueLabel;

    @FXML protected TextField descriptionField;

    @FXML protected TextField unitPriceField;

    @FXML protected TextField quantityField;

    @FXML protected Label totalPriceLabel;

    @FXML protected ComboBox<Wallet> walletComboBox;

    @FXML protected ComboBox<WalletTransactionStatus> statusComboBox;

    @FXML protected ComboBox<Category> categoryComboBox;

    @FXML protected DatePicker transactionDatePicker;

    @FXML protected CheckBox includeInAnalysisCheckBox;

    protected SuggestionsHandlerHelper<WalletTransaction> suggestionsHandler;

    protected WalletService walletService;

    protected CategoryService categoryService;

    protected TickerService tickerService;

    protected PreferencesService preferencesService;

    protected List<Wallet> wallets;

    protected List<Category> categories;

    protected Ticker ticker = null;

    protected Wallet wallet = null;

    protected WalletTransactionType walletTransactionType;

    /**
     * Constructor
     *
     * @param walletService Wallet service
     * @param categoryService Category service
     * @param tickerService Ticker service
     * @param preferencesService I18n service
     * @param walletService Wallet service
     * @param tickerService Ticker service
     * @param preferencesService I18n service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    protected BaseTickerTransactionManagement(
            WalletService walletService,
            CategoryService categoryService,
            TickerService tickerService,
            PreferencesService preferencesService) {
        this.walletService = walletService;
        this.categoryService = categoryService;
        this.tickerService = tickerService;
        this.preferencesService = preferencesService;
    }

    public void setWalletComboBox(Wallet wt) {
        if (wallets.stream().noneMatch(w -> w.getId().equals(wt.getId()))) {
            return;
        }

        this.wallet = wt;
        walletComboBox.setValue(wallet);
        UIUtils.updateWalletBalanceLabelStyle(wt, walletCurrentBalanceValueLabel);
    }

    public void setTicker(Ticker ticker) {
        this.ticker = ticker;
        tickerNameLabel.setText(ticker.getName() + " (" + ticker.getSymbol() + ")");
        unitPriceField.setText(ticker.getCurrentUnitValue().toString());
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
        UIUtils.setDatePickerFormat(transactionDatePicker, preferencesService);

        // Reset all labels
        UIUtils.resetLabel(walletAfterBalanceValueLabel);
        UIUtils.resetLabel(walletCurrentBalanceValueLabel);

        walletComboBox.setOnAction(
                e -> {
                    UIUtils.updateWalletBalanceLabelStyle(
                            walletComboBox.getValue(), walletCurrentBalanceValueLabel);
                    walletAfterBalance();
                });
    }

    @FXML
    protected void handleCancel() {
        Stage stage = (Stage) tickerNameLabel.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected abstract void handleSave();

    protected void updateTotalPrice() {
        String unitPriceStr = unitPriceField.getText();
        String quantityStr = quantityField.getText();

        BigDecimal totalPrice = new BigDecimal("0.00");

        if (unitPriceStr == null
                || quantityStr == null
                || unitPriceStr.isBlank()
                || quantityStr.isBlank()) {
            totalPriceLabel.setText(UIUtils.formatCurrency(totalPrice));
            return;
        }

        try {
            BigDecimal unitPrice = new BigDecimal(unitPriceStr);
            BigDecimal quantity = new BigDecimal(quantityStr);

            totalPrice = unitPrice.multiply(quantity);

            totalPriceLabel.setText(UIUtils.formatCurrency(totalPrice));
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_INVALID_NUMBER_CALCULATION_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_INVALID_NUMBER_CALCULATION_MESSAGE));

            totalPrice = new BigDecimal("0.00");

            totalPriceLabel.setText(UIUtils.formatCurrency(totalPrice));
        }
    }

    protected void walletAfterBalance() {
        String unitPriceStr = unitPriceField.getText();
        String quantityStr = quantityField.getText();
        Wallet wt = walletComboBox.getValue();

        if (unitPriceStr == null
                || unitPriceStr.isBlank()
                || quantityStr == null
                || quantityStr.isBlank()
                || wt == null) {
            UIUtils.resetLabel(walletAfterBalanceValueLabel);
            return;
        }

        try {
            BigDecimal transactionValue =
                    new BigDecimal(unitPriceStr).multiply(new BigDecimal(quantityStr));

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
        if (walletTransactionType == WalletTransactionType.EXPENSE) {
            return wallet.getBalance().subtract(transactionValue);
        } else if (walletTransactionType == WalletTransactionType.INCOME) {
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

    protected void loadSuggestionsFromDatabase() {
        suggestionsHandler.setSuggestions(
                walletService.getWalletTransactionSuggestionsByType(walletTransactionType));
    }

    protected void populateComboBoxes() {
        walletComboBox.getItems().setAll(wallets);
        statusComboBox.getItems().addAll(Arrays.asList(WalletTransactionStatus.values()));
        categoryComboBox.getItems().setAll(categories);

        // If there are no categories, add a tooltip to the categoryComboBox
        // to inform the user that a category is needed
        if (categories.isEmpty()) {
            UIUtils.addTooltipToNode(
                    categoryComboBox,
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_TOOLTIP_CATEGORY_REQUIRED));
        }
    }

    protected void configureComboBoxes() {
        UIUtils.configureComboBox(walletComboBox, Wallet::getName);
        UIUtils.configureComboBox(
                statusComboBox, t -> UIUtils.translateTransactionStatus(t, preferencesService));
        UIUtils.configureComboBox(categoryComboBox, Category::getName);
    }

    protected void configureListeners() {
        // Update wallet after balance when the value field changes
        unitPriceField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX)) {
                                unitPriceField.setText(oldValue);
                            } else {
                                updateTotalPrice();
                                walletAfterBalance();
                            }
                        });

        quantityField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX)) {
                                quantityField.setText(oldValue);
                            } else {
                                updateTotalPrice();
                                walletAfterBalance();
                            }
                        });

        unitPriceField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            updateTotalPrice();
                            walletAfterBalance();
                        });

        quantityField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            updateTotalPrice();
                            walletAfterBalance();
                        });
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

    protected void fillFieldsWithTransaction(WalletTransaction wt) {
        walletComboBox.setValue(wt.getWallet());

        // Deactivate the listener to avoid the event of changing the text of
        // the descriptionField from being triggered. After changing the text,
        // the listener is activated again
        suggestionsHandler.disable();
        descriptionField.setText(wt.getDescription());
        suggestionsHandler.enable();

        statusComboBox.setValue(wt.getStatus());
        categoryComboBox.setValue(wt.getCategory());

        UIUtils.updateWalletBalanceLabelStyle(
                walletComboBox.getValue(), walletCurrentBalanceValueLabel);
        walletAfterBalance();
    }
}
