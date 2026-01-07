/*
 * Filename: EditTickerPurchaseController.java
 * Created on: January 11, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.enums.TransactionStatus;
import org.moinex.model.enums.TransactionType;
import org.moinex.model.investment.TickerPurchase;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.CategoryService;
import org.moinex.service.I18nService;
import org.moinex.service.TickerService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Ticker Purchase dialog
 */
@Controller
@NoArgsConstructor
public final class EditTickerPurchaseController extends BaseTickerTransactionManagement {
    private TickerPurchase purchase = null;

    /**
     * Constructor
     * @param walletService Wallet service
     * @param walletTransactionService Wallet transaction service
     * @param categoryService Category service
     * @param tickerService Ticker service
     * @param i18nService I18n service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public EditTickerPurchaseController(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            CategoryService categoryService,
            TickerService tickerService,
            I18nService i18nService) {
        super(walletService, walletTransactionService, categoryService, tickerService, i18nService);
        this.i18nService = i18nService;
        transactionType = TransactionType.EXPENSE;
    }

    public void setPurchase(TickerPurchase p) {
        this.purchase = p;
        tickerNameLabel.setText(
                purchase.getTicker().getName() + " (" + purchase.getTicker().getSymbol() + ")");
        unitPriceField.setText(purchase.getTicker().getCurrentUnitValue().toString());

        setWalletComboBox(purchase.getWalletTransaction().getWallet());

        descriptionField.setText(purchase.getWalletTransaction().getDescription());
        unitPriceField.setText(purchase.getUnitPrice().toString());
        quantityField.setText(purchase.getQuantity().toString());
        statusComboBox.setValue(purchase.getWalletTransaction().getStatus());
        categoryComboBox.setValue(purchase.getWalletTransaction().getCategory());
        transactionDatePicker.setValue(purchase.getWalletTransaction().getDate().toLocalDate());
        includeInAnalysisCheckBox.setSelected(
                purchase.getWalletTransaction().getIncludeInAnalysis());

        totalPriceLabel.setText(
                UIUtils.formatCurrency(purchase.getWalletTransaction().getAmount()));
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
                    i18nService.tr(Constants.TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_MESSAGE));

            return;
        }

        try {
            BigDecimal unitPrice = new BigDecimal(unitPriceStr);

            BigDecimal quantity = new BigDecimal(quantityStr);

            // Check if it has any modification
            if (purchase.getWalletTransaction().getWallet().getId().equals(wallet.getId())
                    && purchase.getWalletTransaction().getDescription().equals(description)
                    && purchase.getWalletTransaction().getStatus().equals(status)
                    && purchase.getWalletTransaction()
                            .getCategory()
                            .getId()
                            .equals(category.getId())
                    && purchase.getUnitPrice().compareTo(unitPrice) == 0
                    && purchase.getQuantity().compareTo(quantity) == 0
                    && purchase.getWalletTransaction().getDate().toLocalDate().equals(buyDate)
                    && purchase.getWalletTransaction()
                            .getIncludeInAnalysis()
                            .equals(includeInAnalysisCheckBox.isSelected())) {
                WindowUtils.showInformationDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.INVESTMENT_DIALOG_NO_CHANGES_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .INVESTMENT_DIALOG_NO_CHANGES_PURCHASE_MESSAGE));
            } else // If there is any modification, update the transaction
            {
                LocalTime currentTime = LocalTime.now();
                LocalDateTime dateTimeWithCurrentHour = buyDate.atTime(currentTime);

                purchase.getWalletTransaction().setWallet(wallet);
                purchase.getWalletTransaction().setDescription(description);
                purchase.getWalletTransaction().setStatus(status);
                purchase.getWalletTransaction().setCategory(category);
                purchase.setUnitPrice(unitPrice);
                purchase.setQuantity(quantity);
                purchase.getWalletTransaction().setDate(dateTimeWithCurrentHour);
                purchase.getWalletTransaction()
                        .setIncludeInAnalysis(includeInAnalysisCheckBox.isSelected());

                tickerService.updatePurchase(purchase);

                WindowUtils.showSuccessDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.INVESTMENT_DIALOG_PURCHASE_UPDATED_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .INVESTMENT_DIALOG_PURCHASE_UPDATED_MESSAGE));
            }

            Stage stage = (Stage) tickerNameLabel.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_MESSAGE));
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_ERROR_UPDATING_PURCHASE_TITLE),
                    e.getMessage());
        }
    }
}
