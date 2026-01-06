/*
 * Filename: AddBondPurchaseController.java
 * Created on: January  2, 2026
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
import org.moinex.model.enums.OperationType;
import org.moinex.model.enums.TransactionStatus;
import org.moinex.model.enums.TransactionType;
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
public final class AddBondPurchaseController extends BaseBondTransactionManagement {
    @Autowired
    public AddBondPurchaseController(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            CategoryService categoryService,
            BondService bondService,
            I18nService i18nService) {
        super(walletService, walletTransactionService, categoryService, bondService, i18nService);
        this.i18nService = i18nService;
        transactionType = TransactionType.EXPENSE;
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

            bondService.addOperation(
                    bond.getId(),
                    wallet.getId(),
                    OperationType.BUY,
                    quantity,
                    unitPrice,
                    buyDate,
                    fees,
                    taxes,
                    null, // netProfit not applicable for purchases
                    category,
                    description,
                    status);

            WindowUtils.showSuccessDialog(
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_PURCHASE_ADDED_TITLE),
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_PURCHASE_ADDED_MESSAGE));

            Stage stage = (Stage) bondNameLabel.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_INVALID_NUMBER_TITLE),
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_INVALID_NUMBER_MESSAGE));
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_ERROR_BUYING_TITLE),
                    e.getMessage());
        }
    }
}
