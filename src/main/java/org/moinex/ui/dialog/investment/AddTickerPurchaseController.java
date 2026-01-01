/*
 * Filename: AddTickerPurchaseController.java
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
import org.moinex.model.Category;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.CategoryService;
import org.moinex.service.I18nService;
import org.moinex.service.TickerService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Buy Ticker dialog
 */
@Controller
@NoArgsConstructor
public final class AddTickerPurchaseController extends BaseTickerTransactionManagement {
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
    public AddTickerPurchaseController(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            CategoryService categoryService,
            TickerService tickerService,
            I18nService i18nService) {
        super(walletService, walletTransactionService, categoryService, tickerService, i18nService);
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

            tickerService.addPurchase(
                    ticker.getId(),
                    wallet.getId(),
                    quantity,
                    unitPrice,
                    category,
                    dateTimeWithCurrentHour,
                    description,
                    status);

            WindowUtils.showSuccessDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_PURCHASE_ADDED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_PURCHASE_ADDED_MESSAGE));

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
                            Constants.TranslationKeys.INVESTMENT_DIALOG_ERROR_BUYING_TICKER_TITLE),
                    e.getMessage());
        }
    }
}
