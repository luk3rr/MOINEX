/*
 * Filename: EditBondPurchaseController.java
 * Created on: January  3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.enums.TransactionStatus;
import org.moinex.model.enums.TransactionType;
import org.moinex.model.investment.BondOperation;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.BondService;
import org.moinex.service.CategoryService;
import org.moinex.service.I18nService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@NoArgsConstructor
public final class EditBondPurchaseController extends BaseBondTransactionManagement {
    private BondOperation operation = null;

    @Autowired
    public EditBondPurchaseController(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            CategoryService categoryService,
            BondService bondService,
            I18nService i18nService) {
        super(walletService, walletTransactionService, categoryService, bondService, i18nService);
        this.i18nService = i18nService;
        transactionType = TransactionType.EXPENSE;
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

        statusComboBox.setValue(operation.getWalletTransaction().getStatus());
        categoryComboBox.setValue(operation.getWalletTransaction().getCategory());
        transactionDatePicker.setValue(operation.getWalletTransaction().getDate().toLocalDate());

        updateTotalPrice();
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
        LocalDate buyDate = transactionDatePicker.getValue();

        if (wallet == null
                || description == null
                || description.isBlank()
                || status == null
                || category == null
                || unitPriceStr == null
                || unitPriceStr.isBlank()
                || quantityStr == null
                || quantityStr.isBlank()
                || buyDate == null) {
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
                    && operation.getWalletTransaction().getDate().toLocalDate().equals(buyDate)) {
                WindowUtils.showInformationDialog(
                        i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_NO_CHANGES_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys.BOND_DIALOG_NO_CHANGES_PURCHASE_MESSAGE));
            } else {
                bondService.updateOperation(
                        operation.getId(),
                        wallet.getId(),
                        quantity,
                        unitPrice,
                        buyDate,
                        fees,
                        taxes,
                        null,
                        category,
                        description,
                        status);

                WindowUtils.showSuccessDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.BOND_DIALOG_PURCHASE_UPDATED_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys.BOND_DIALOG_PURCHASE_UPDATED_MESSAGE));
            }

            Stage stage = (Stage) bondNameLabel.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_INVALID_NUMBER_TITLE),
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_INVALID_NUMBER_MESSAGE));
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.BOND_DIALOG_ERROR_UPDATING_PURCHASE_TITLE),
                    e.getMessage());
        }
    }
}
