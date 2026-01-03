/*
 * Filename: EditBondSaleController.java
 * Created on: January  3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.investment.BondOperation;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.BondService;
import org.moinex.service.CategoryService;
import org.moinex.service.I18nService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@NoArgsConstructor
public final class EditBondSaleController extends BaseBondTransactionManagement {
    @FXML private TextField netProfitField;
    @FXML private TextField grossYieldField;

    private BondOperation operation = null;

    @Autowired
    public EditBondSaleController(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            CategoryService categoryService,
            BondService bondService,
            I18nService i18nService) {
        super(walletService, walletTransactionService, categoryService, bondService, i18nService);
        this.i18nService = i18nService;
        transactionType = TransactionType.INCOME;
    }

    public void setOperation(BondOperation op) {
        this.operation = op;
        this.bond = op.getBond();

        String symbol = bond.getSymbol();
        bondNameLabel.setText(
                bond.getName() + (symbol != null && !symbol.isBlank() ? " (" + symbol + ")" : ""));

        setWalletComboBox(operation.getWalletTransaction().getWallet());

        descriptionField.setText(operation.getWalletTransaction().getDescription());
        unitPriceField.setText(operation.getUnitPrice().toString());
        quantityField.setText(operation.getQuantity().toString());

        if (operation.getFees() != null) {
            feesField.setText(operation.getFees().toString());
        }

        if (operation.getTaxes() != null) {
            taxesField.setText(operation.getTaxes().toString());
        }

        if (operation.getNetProfit() != null) {
            netProfitField.setText(operation.getNetProfit().toString());
        }

        statusComboBox.setValue(operation.getWalletTransaction().getStatus());
        categoryComboBox.setValue(operation.getWalletTransaction().getCategory());
        transactionDatePicker.setValue(operation.getWalletTransaction().getDate().toLocalDate());

        updateTotalPrice();
    }

    @FXML
    @Override
    protected void initialize() {
        super.initialize();

        unitPriceField
                .textProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            if (!netProfitField.isFocused()) {
                                calculateProfit();
                            }
                        });
        quantityField
                .textProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            if (!netProfitField.isFocused()) {
                                calculateProfit();
                            }
                        });
        feesField
                .textProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            if (!netProfitField.isFocused()) {
                                calculateProfit();
                            }
                        });
        taxesField
                .textProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            if (!netProfitField.isFocused()) {
                                calculateProfit();
                            }
                        });

        grossYieldField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                grossYieldField.setText(oldValue);
                            } else {
                                if (!netProfitField.isFocused()) calculateProfit();
                                updateTotalPrice();
                                walletAfterBalance();
                            }
                        });

        netProfitField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.SIGNED_MONETARY_VALUE_REGEX)) {
                                netProfitField.setText(oldValue);
                            } else {
                                updateNetProfitColor(newValue);
                                updateTotalPrice();
                                walletAfterBalance();
                            }
                        });
    }

    @FXML
    @Override
    protected void handleSave() {
        Wallet wallet = walletComboBox.getValue();
        String description = descriptionField.getText();
        TransactionStatus status = statusComboBox.getValue();
        Category category = categoryComboBox.getValue();
        String unitPriceStr = unitPriceField.getText();
        String quantityStr = quantityField.getText();
        String feesStr = feesField.getText();
        String taxesStr = taxesField.getText();
        LocalDate saleDate = transactionDatePicker.getValue();

        if (wallet == null
                || description == null
                || description.isBlank()
                || status == null
                || category == null
                || unitPriceStr == null
                || unitPriceStr.isBlank()
                || quantityStr == null
                || quantityStr.isBlank()
                || saleDate == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_EMPTY_FIELDS_TITLE),
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_EMPTY_FIELDS_MESSAGE));

            return;
        }

        try {
            BigDecimal unitPrice = new BigDecimal(unitPriceStr);
            BigDecimal quantity = new BigDecimal(quantityStr);
            BigDecimal fees =
                    (feesStr != null && !feesStr.isBlank()) ? new BigDecimal(feesStr) : null;
            BigDecimal taxes =
                    (taxesStr != null && !taxesStr.isBlank()) ? new BigDecimal(taxesStr) : null;

            String netProfitStr = netProfitField.getText();
            BigDecimal netProfit =
                    (netProfitStr != null && !netProfitStr.isBlank())
                            ? new BigDecimal(netProfitStr)
                            : null;

            boolean feesEqual =
                    (operation.getFees() == null && fees == null)
                            || (operation.getFees() != null
                                    && fees != null
                                    && operation.getFees().compareTo(fees) == 0);
            boolean taxesEqual =
                    (operation.getTaxes() == null && taxes == null)
                            || (operation.getTaxes() != null
                                    && taxes != null
                                    && operation.getTaxes().compareTo(taxes) == 0);
            boolean netProfitEqual =
                    (operation.getNetProfit() == null && netProfit == null)
                            || (operation.getNetProfit() != null
                                    && netProfit != null
                                    && operation.getNetProfit().compareTo(netProfit) == 0);

            if (operation.getWalletTransaction().getWallet().getId().equals(wallet.getId())
                    && operation.getWalletTransaction().getDescription().equals(description)
                    && operation.getWalletTransaction().getStatus().equals(status)
                    && operation
                            .getWalletTransaction()
                            .getCategory()
                            .getId()
                            .equals(category.getId())
                    && operation.getUnitPrice().compareTo(unitPrice) == 0
                    && operation.getQuantity().compareTo(quantity) == 0
                    && feesEqual
                    && taxesEqual
                    && netProfitEqual
                    && operation.getWalletTransaction().getDate().toLocalDate().equals(saleDate)) {
                WindowUtils.showInformationDialog(
                        i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_NO_CHANGES_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys.BOND_DIALOG_NO_CHANGES_SALE_MESSAGE));
            } else {
                bondService.updateOperation(
                        operation.getId(),
                        wallet.getId(),
                        quantity,
                        unitPrice,
                        saleDate,
                        fees,
                        taxes,
                        netProfit,
                        category,
                        description,
                        status);

                WindowUtils.showSuccessDialog(
                        i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_SALE_UPDATED_TITLE),
                        i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_SALE_UPDATED_MESSAGE));
            }

            Stage stage = (Stage) bondNameLabel.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_INVALID_NUMBER_TITLE),
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_INVALID_NUMBER_MESSAGE));
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_ERROR_UPDATING_SALE_TITLE),
                    e.getMessage());
        }
    }

    @Override
    protected void updateTotalPrice() {
        String unitPriceStr = unitPriceField.getText();
        String quantityStr = quantityField.getText();
        String netProfitStr = netProfitField.getText();

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
            BigDecimal netProfit =
                    (netProfitStr != null && !netProfitStr.isBlank())
                            ? new BigDecimal(netProfitStr)
                            : BigDecimal.ZERO;

            totalPrice = unitPrice.multiply(quantity).add(netProfit);

            totalPriceLabel.setText(UIUtils.formatCurrency(totalPrice));
        } catch (NumberFormatException e) {
            totalPriceLabel.setText(UIUtils.formatCurrency(totalPrice));
        }
    }

    @Override
    protected void walletAfterBalance() {
        String unitPriceStr = unitPriceField.getText();
        String quantityStr = quantityField.getText();
        String netProfitStr = netProfitField.getText();
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
            BigDecimal netProfit =
                    (netProfitStr != null && !netProfitStr.isBlank())
                            ? new BigDecimal(netProfitStr)
                            : BigDecimal.ZERO;

            BigDecimal transactionValue = unitPrice.multiply(quantity).add(netProfit);

            if (transactionValue.compareTo(BigDecimal.ZERO) < 0) {
                UIUtils.resetLabel(walletAfterBalanceValueLabel);
                return;
            }

            BigDecimal walletAfterBalanceValue = wt.getBalance().add(transactionValue);

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

    private void updateNetProfitColor(String valueStr) {
        if (valueStr == null || valueStr.isBlank() || valueStr.equals("-")) {
            netProfitField
                    .getStyleClass()
                    .removeAll(
                            Constants.NEGATIVE_BALANCE_STYLE,
                            Constants.POSITIVE_BALANCE_STYLE,
                            Constants.NEUTRAL_BALANCE_STYLE);
            return;
        }

        try {
            BigDecimal value = new BigDecimal(valueStr);

            netProfitField
                    .getStyleClass()
                    .removeAll(
                            Constants.NEGATIVE_BALANCE_STYLE,
                            Constants.POSITIVE_BALANCE_STYLE,
                            Constants.NEUTRAL_BALANCE_STYLE);

            if (value.compareTo(BigDecimal.ZERO) > 0) {
                netProfitField.getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);
            } else if (value.compareTo(BigDecimal.ZERO) < 0) {
                netProfitField.getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);
            } else {
                netProfitField.getStyleClass().add(Constants.NEUTRAL_BALANCE_STYLE);
            }
        } catch (NumberFormatException e) {
            netProfitField
                    .getStyleClass()
                    .removeAll(
                            Constants.NEGATIVE_BALANCE_STYLE,
                            Constants.POSITIVE_BALANCE_STYLE,
                            Constants.NEUTRAL_BALANCE_STYLE);
        }
    }

    private void calculateProfit() {
        if (bond == null || netProfitField == null || grossYieldField == null) {
            return;
        }

        String unitPriceStr = unitPriceField.getText();
        String quantityStr = quantityField.getText();
        String interestStr = grossYieldField.getText();
        String feesStr = feesField.getText();
        String taxesStr = taxesField.getText();

        if (unitPriceStr == null
                || unitPriceStr.isBlank()
                || quantityStr == null
                || quantityStr.isBlank()) {
            netProfitField.setText("");
            return;
        }

        try {
            BigDecimal saleUnitPrice = new BigDecimal(unitPriceStr);
            BigDecimal quantity = new BigDecimal(quantityStr);
            BigDecimal interest = new BigDecimal(interestStr);
            BigDecimal fees =
                    (feesStr != null && !feesStr.isBlank())
                            ? new BigDecimal(feesStr)
                            : BigDecimal.ZERO;
            BigDecimal taxes =
                    (taxesStr != null && !taxesStr.isBlank())
                            ? new BigDecimal(taxesStr)
                            : BigDecimal.ZERO;

            BigDecimal averageUnitPrice = bondService.getAverageUnitPrice(bond);

            BigDecimal profit =
                    saleUnitPrice
                            .subtract(averageUnitPrice)
                            .multiply(quantity)
                            .add(interest)
                            .subtract(fees)
                            .subtract(taxes)
                            .setScale(2, RoundingMode.HALF_UP);

            netProfitField.setText(profit.toString());
        } catch (NumberFormatException e) {
            netProfitField.setText("");
        }
    }
}
