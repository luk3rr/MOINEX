/*
 * Filename: AddCryptoExchangeController.java
 * Created on: January 28, 2025
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
import org.moinex.model.investment.Ticker;
import org.moinex.service.CalculatorService;
import org.moinex.service.I18nService;
import org.moinex.service.TickerService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Crypto Exchange dialog
 */
@Controller
@NoArgsConstructor
public class AddCryptoExchangeController extends BaseCryptoExchangeManagement {
    private I18nService i18nService;

    /**
     * Constructor
     * @param tickerService TickerService
     * @param calculatorService CalculatorService
     * @param i18nService I18n service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddCryptoExchangeController(
            TickerService tickerService,
            CalculatorService calculatorService,
            I18nService i18nService) {
        super(tickerService, calculatorService);
        this.i18nService = i18nService;
        setI18nService(i18nService);
    }

    @FXML
    @Override
    protected void handleSave() {
        Ticker cryptoSold = cryptoSoldComboBox.getValue();
        Ticker cryptoReceived = cryptoReceivedComboBox.getValue();
        String cryptoSoldQuantityStr = cryptoSoldQuantityField.getText();
        String cryptoReceivedQuantityStr = cryptoReceivedQuantityField.getText();
        String description = descriptionField.getText();
        LocalDate exchangeDate = exchangeDatePicker.getValue();

        if (cryptoSold == null
                || cryptoReceived == null
                || cryptoSoldQuantityStr == null
                || cryptoSoldQuantityStr.isBlank()
                || cryptoReceivedQuantityStr == null
                || cryptoReceivedQuantityStr.isBlank()
                || description == null
                || description.isBlank()
                || exchangeDate == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(Constants.TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_MESSAGE));
            return;
        }

        try {
            BigDecimal cryptoSoldQuantity = new BigDecimal(cryptoSoldQuantityStr);
            BigDecimal cryptoReceivedQuantity = new BigDecimal(cryptoReceivedQuantityStr);

            LocalTime currentTime = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = exchangeDate.atTime(currentTime);

            tickerService.addCryptoExchange(
                    cryptoSold.getId(),
                    cryptoReceived.getId(),
                    cryptoSoldQuantity,
                    cryptoReceivedQuantity,
                    dateTimeWithCurrentHour,
                    description);

            WindowUtils.showSuccessDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_EXCHANGE_CREATED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_EXCHANGE_CREATED_MESSAGE));

            Stage stage = (Stage) cryptoReceivedQuantityField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_INVALID_EXCHANGE_QUANTITY_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_INVALID_EXCHANGE_QUANTITY_MESSAGE));
        } catch (MoinexException.SameSourceDestinationException
                | EntityNotFoundException
                | MoinexException.InvalidTickerTypeException
                | IllegalArgumentException
                | MoinexException.InsufficientResourcesException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_ERROR_CREATING_EXCHANGE_TITLE),
                    e.getMessage());
        }
    }
}
