/*
 * Filename: BaseBondTransactionManagement.java
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

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
import org.moinex.model.investment.Bond;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.service.BondService;
import org.moinex.service.CategoryService;
import org.moinex.service.I18nService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.SuggestionsHandlerHelper;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;

@NoArgsConstructor
public abstract class BaseBondTransactionManagement {
    @FXML protected Label bondNameLabel;

    @FXML protected Label walletAfterBalanceValueLabel;

    @FXML protected Label walletCurrentBalanceValueLabel;

    @FXML protected TextField descriptionField;

    @FXML protected TextField unitPriceField;

    @FXML protected TextField quantityField;

    @FXML protected TextField feesField;

    @FXML protected TextField taxesField;

    @FXML protected Label totalPriceLabel;

    @FXML protected ComboBox<Wallet> walletComboBox;

    @FXML protected ComboBox<TransactionStatus> statusComboBox;

    @FXML protected ComboBox<Category> categoryComboBox;

    @FXML protected DatePicker transactionDatePicker;

    protected SuggestionsHandlerHelper<WalletTransaction> suggestionsHandler;

    protected WalletService walletService;

    protected WalletTransactionService walletTransactionService;

    protected CategoryService categoryService;

    protected BondService bondService;

    protected I18nService i18nService;

    protected List<Wallet> wallets;

    protected List<Category> categories;

    protected Bond bond = null;

    protected Wallet wallet = null;

    protected TransactionType transactionType;

    @Autowired
    protected BaseBondTransactionManagement(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            CategoryService categoryService,
            BondService bondService,
            I18nService i18nService) {
        this.walletService = walletService;
        this.walletTransactionService = walletTransactionService;
        this.categoryService = categoryService;
        this.bondService = bondService;
        this.i18nService = i18nService;
    }

    public void setWalletComboBox(Wallet wt) {
        if (wallets.stream().noneMatch(w -> w.getId().equals(wt.getId()))) {
            return;
        }

        this.wallet = wt;
        walletComboBox.setValue(wallet);
        UIUtils.updateWalletBalanceLabelStyle(wt, walletCurrentBalanceValueLabel);
    }

    public void setBond(Bond bond) {
        this.bond = bond;
        String symbol = bond.getSymbol();
        bondNameLabel.setText(
                bond.getName() + (symbol != null && !symbol.isBlank() ? " (" + symbol + ")" : ""));
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

        UIUtils.setDatePickerFormat(transactionDatePicker, i18nService);

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
        Stage stage = (Stage) bondNameLabel.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected abstract void handleSave();

    protected void updateTotalPrice() {
        String unitPriceStr = unitPriceField.getText();
        String quantityStr = quantityField.getText();
        String feesStr = feesField.getText();
        String taxesStr = taxesField.getText();

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
            BigDecimal fees =
                    (feesStr != null && !feesStr.isBlank())
                            ? new BigDecimal(feesStr)
                            : BigDecimal.ZERO;
            BigDecimal taxes =
                    (taxesStr != null && !taxesStr.isBlank())
                            ? new BigDecimal(taxesStr)
                            : BigDecimal.ZERO;

            BigDecimal baseAmount = unitPrice.multiply(quantity);

            if (transactionType == TransactionType.EXPENSE) {
                totalPrice = baseAmount.add(fees).add(taxes);
            } else {
                totalPrice = baseAmount.subtract(fees).subtract(taxes);
            }

            totalPriceLabel.setText(UIUtils.formatCurrency(totalPrice));
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_INVALID_NUMBER_CALCULATION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_INVALID_NUMBER_CALCULATION_MESSAGE));

            totalPrice = new BigDecimal("0.00");

            totalPriceLabel.setText(UIUtils.formatCurrency(totalPrice));
        }
    }

    protected void walletAfterBalance() {
        String unitPriceStr = unitPriceField.getText();
        String quantityStr = quantityField.getText();
        String feesStr = feesField.getText();
        String taxesStr = taxesField.getText();
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
            BigDecimal unitPrice = new BigDecimal(unitPriceStr);
            BigDecimal quantity = new BigDecimal(quantityStr);
            BigDecimal fees =
                    (feesStr != null && !feesStr.isBlank())
                            ? new BigDecimal(feesStr)
                            : BigDecimal.ZERO;
            BigDecimal taxes =
                    (taxesStr != null && !taxesStr.isBlank())
                            ? new BigDecimal(taxesStr)
                            : BigDecimal.ZERO;

            BigDecimal baseAmount = unitPrice.multiply(quantity);
            BigDecimal transactionValue;

            if (transactionType == TransactionType.EXPENSE) {
                transactionValue = baseAmount.add(fees).add(taxes);
            } else {
                transactionValue = baseAmount.subtract(fees).subtract(taxes);
            }

            if (transactionValue.compareTo(BigDecimal.ZERO) < 0) {
                UIUtils.resetLabel(walletAfterBalanceValueLabel);
                return;
            }

            BigDecimal walletAfterBalanceValue = getBigDecimal(wt, transactionValue);

            if (walletAfterBalanceValue.compareTo(BigDecimal.ZERO) < 0) {
                UIUtils.setLabelStyle(
                        walletAfterBalanceValueLabel, Constants.NEGATIVE_BALANCE_STYLE);
            } else {
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

    protected void loadSuggestionsFromDatabase() {
        suggestionsHandler.setSuggestions(walletTransactionService.getExpenseSuggestions());
    }

    protected void populateComboBoxes() {
        walletComboBox.getItems().setAll(wallets);
        statusComboBox.getItems().addAll(Arrays.asList(TransactionStatus.values()));
        categoryComboBox.getItems().setAll(categories);

        if (categories.isEmpty()) {
            UIUtils.addTooltipToNode(
                    categoryComboBox,
                    i18nService.tr(Constants.TranslationKeys.INVESTMENT_TOOLTIP_CATEGORY_REQUIRED));
        }
    }

    protected void configureComboBoxes() {
        UIUtils.configureComboBox(walletComboBox, Wallet::getName);
        UIUtils.configureComboBox(
                statusComboBox, t -> UIUtils.translateTransactionStatus(t, i18nService));
        UIUtils.configureComboBox(categoryComboBox, Category::getName);
    }

    protected void configureListeners() {
        unitPriceField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
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
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                quantityField.setText(oldValue);
                            } else {
                                updateTotalPrice();
                                walletAfterBalance();
                            }
                        });

        taxesField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                taxesField.setText(oldValue);
                            } else {
                                updateTotalPrice();
                                walletAfterBalance();
                            }
                        });

        feesField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                feesField.setText(oldValue);
                            } else {
                                updateTotalPrice();
                                walletAfterBalance();
                            }
                        });
    }

    protected void configureSuggestions() {
        Function<WalletTransaction, String> filterFunction = WalletTransaction::getDescription;

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
