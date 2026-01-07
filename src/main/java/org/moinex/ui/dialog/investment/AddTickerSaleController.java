/*
 * Filename: AddTickerSaleController.java
 * Created on: January  9, 2025
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
import org.moinex.error.MoinexException;
import org.moinex.model.Category;
import org.moinex.model.enums.TransactionStatus;
import org.moinex.model.enums.TransactionType;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.CategoryService;
import org.moinex.service.I18nService;
import org.moinex.service.TickerService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Sale Ticker dialog
 */
@Controller
@NoArgsConstructor
public final class AddTickerSaleController extends BaseTickerTransactionManagement {
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
    public AddTickerSaleController(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            CategoryService categoryService,
            TickerService tickerService,
            I18nService i18nService) {
        super(walletService, walletTransactionService, categoryService, tickerService, i18nService);
        this.i18nService = i18nService;
        transactionType = TransactionType.INCOME;
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

            LocalTime currentTime = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = buyDate.atTime(currentTime);

            tickerService.addSale(
                    ticker.getId(),
                    wallet.getId(),
                    quantity,
                    unitPrice,
                    category,
                    dateTimeWithCurrentHour,
                    description,
                    status,
                    includeInAnalysisCheckBox.isSelected());

            WindowUtils.showSuccessDialog(
                    i18nService.tr(Constants.TranslationKeys.INVESTMENT_DIALOG_SALE_ADDED_TITLE),
                    i18nService.tr(Constants.TranslationKeys.INVESTMENT_DIALOG_SALE_ADDED_MESSAGE));

            Stage stage = (Stage) tickerNameLabel.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_MESSAGE));
        } catch (EntityNotFoundException
                | IllegalArgumentException
                | MoinexException.InsufficientResourcesException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_ERROR_SELLING_TICKER_TITLE),
                    e.getMessage());
        }
    }
}
