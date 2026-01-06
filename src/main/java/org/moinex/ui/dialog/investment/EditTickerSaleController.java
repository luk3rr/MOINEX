/*
 * Filename: EditTickerSaleController.java
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
import org.moinex.model.investment.TickerSale;
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
 * Controller for the Edit Ticker Sale dialog
 */
@Controller
@NoArgsConstructor
public final class EditTickerSaleController extends BaseTickerTransactionManagement {
    private TickerSale sale = null;

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
    public EditTickerSaleController(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            CategoryService categoryService,
            TickerService tickerService,
            I18nService i18nService) {
        super(walletService, walletTransactionService, categoryService, tickerService, i18nService);
        this.i18nService = i18nService;
        transactionType = TransactionType.INCOME;
    }

    public void setSale(TickerSale s) {
        this.sale = s;
        tickerNameLabel.setText(
                sale.getTicker().getName() + " (" + sale.getTicker().getSymbol() + ")");
        unitPriceField.setText(sale.getTicker().getCurrentUnitValue().toString());

        setWalletComboBox(sale.getWalletTransaction().getWallet());

        descriptionField.setText(sale.getWalletTransaction().getDescription());
        unitPriceField.setText(sale.getUnitPrice().toString());
        quantityField.setText(sale.getQuantity().toString());
        statusComboBox.setValue(sale.getWalletTransaction().getStatus());
        categoryComboBox.setValue(sale.getWalletTransaction().getCategory());
        transactionDatePicker.setValue(sale.getWalletTransaction().getDate().toLocalDate());

        totalPriceLabel.setText(UIUtils.formatCurrency(sale.getWalletTransaction().getAmount()));
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
                    i18nService.tr(Constants.TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_MESSAGE));

            return;
        }

        try {
            BigDecimal unitPrice = new BigDecimal(unitPriceStr);

            BigDecimal quantity = new BigDecimal(quantityStr);

            // Check if it has any modification
            if (sale.getWalletTransaction().getWallet().getId().equals(wallet.getId())
                    && sale.getWalletTransaction().getDescription().equals(description)
                    && sale.getWalletTransaction().getStatus().equals(status)
                    && sale.getWalletTransaction().getCategory().getId().equals(category.getId())
                    && sale.getUnitPrice().compareTo(unitPrice) == 0
                    && sale.getQuantity().compareTo(quantity) == 0
                    && sale.getWalletTransaction().getDate().toLocalDate().equals(saleDate)) {
                WindowUtils.showInformationDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.INVESTMENT_DIALOG_NO_CHANGES_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .INVESTMENT_DIALOG_NO_CHANGES_SALE_MESSAGE));
            } else // If there is any modification, update the transaction
            {
                LocalTime currentTime = LocalTime.now();
                LocalDateTime dateTimeWithCurrentHour = saleDate.atTime(currentTime);

                sale.getWalletTransaction().setWallet(wallet);
                sale.getWalletTransaction().setDescription(description);
                sale.getWalletTransaction().setStatus(status);
                sale.getWalletTransaction().setCategory(category);
                sale.setUnitPrice(unitPrice);
                sale.setQuantity(quantity);
                sale.getWalletTransaction().setDate(dateTimeWithCurrentHour);

                tickerService.updateSale(sale);

                WindowUtils.showSuccessDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.INVESTMENT_DIALOG_SALE_UPDATED_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys.INVESTMENT_DIALOG_SALE_UPDATED_MESSAGE));
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
                            Constants.TranslationKeys.INVESTMENT_DIALOG_ERROR_UPDATING_SALE_TITLE),
                    e.getMessage());
        }
    }
}
