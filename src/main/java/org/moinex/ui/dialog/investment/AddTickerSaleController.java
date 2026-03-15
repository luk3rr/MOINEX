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
import org.moinex.model.dto.WalletTransactionContextDTO;
import org.moinex.model.enums.WalletTransactionStatus;
import org.moinex.model.enums.WalletTransactionType;
import org.moinex.model.investment.TickerSale;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.CategoryService;
import org.moinex.service.PreferencesService;
import org.moinex.service.investment.TickerService;
import org.moinex.service.wallet.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/** Controller for the Sale Ticker dialog */
@Controller
@NoArgsConstructor
public final class AddTickerSaleController extends BaseTickerTransactionManagement {
    /**
     * Constructor
     *
     * @param walletService Wallet service
     * @param categoryService Category service
     * @param tickerService Ticker service
     * @param preferencesService I18n service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddTickerSaleController(
            WalletService walletService,
            CategoryService categoryService,
            TickerService tickerService,
            PreferencesService preferencesService) {
        super(walletService, categoryService, tickerService, preferencesService);
        this.preferencesService = preferencesService;
        walletTransactionType = WalletTransactionType.INCOME;
    }

    @FXML
    @Override
    protected void handleSave() {
        Wallet wallet = walletComboBox.getValue();
        String description = descriptionField.getText();
        WalletTransactionStatus status = statusComboBox.getValue();
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
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_MESSAGE));

            return;
        }

        try {
            BigDecimal unitPrice = new BigDecimal(unitPriceStr);

            BigDecimal quantity = new BigDecimal(quantityStr);

            LocalTime currentTime = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = buyDate.atTime(currentTime);

            tickerService.createTickerSale(
                    new TickerSale(
                            null, ticker, ticker.getAverageUnitValue(), quantity, unitPrice, null),
                    new WalletTransactionContextDTO(
                            wallet,
                            dateTimeWithCurrentHour,
                            category,
                            description,
                            status,
                            includeInAnalysisCheckBox.isSelected()));

            WindowUtils.showSuccessDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_SALE_ADDED_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_SALE_ADDED_MESSAGE));

            Stage stage = (Stage) tickerNameLabel.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_MESSAGE));
        } catch (EntityNotFoundException
                | IllegalArgumentException
                | MoinexException.InsufficientResourcesException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_ERROR_SELLING_TICKER_TITLE),
                    e.getMessage());
        }
    }
}
