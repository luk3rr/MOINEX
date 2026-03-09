/*
 * Filename: AddTickerController.java
 * Created on: January  8, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import com.jfoenix.controls.JFXButton;
import jakarta.persistence.EntityExistsException;
import java.math.BigDecimal;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.enums.AssetType;
import org.moinex.service.PreferencesService;
import org.moinex.service.TickerService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/** Controller for the Add Ticker dialog */
@Controller
@NoArgsConstructor
public final class AddTickerController extends BaseTickerManagement {
    @FXML private JFXButton yahooLookupButton;

    /**
     * Constructor
     *
     * @param tickerService Ticker service
     * @param preferencesService I18n service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddTickerController(TickerService tickerService, PreferencesService preferencesService) {
        super(tickerService, preferencesService);
    }

    @FXML
    @Override
    protected void initialize() {
        super.initialize();

        UIUtils.addTooltipToNode(
                yahooLookupButton,
                preferencesService.translate(
                        Constants.TranslationKeys.INVESTMENT_BUTTON_YAHOO_LOOKUP_TOOLTIP));
    }

    @FXML
    @Override
    protected void handleSave() {
        String name = nameField.getText();
        String symbol = symbolField.getText();
        String currentPriceStr = currentPriceField.getText();
        AssetType type = typeComboBox.getValue();
        String quantityStr = quantityField.getText();
        String avgUnitPriceStr = avgUnitPriceField.getText();

        if (name == null
                || symbol == null
                || currentPriceStr == null
                || type == null
                || name.isBlank()
                || symbol.isBlank()
                || currentPriceStr.isBlank()) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_MESSAGE));

            return;
        }

        // If quantity is set, then avgUnitPrice must be set or vice versa
        if ((quantityStr == null || quantityStr.isBlank())
                        && !(avgUnitPriceStr == null || avgUnitPriceStr.isBlank())
                || (avgUnitPriceStr == null || avgUnitPriceStr.isBlank())
                        && !(quantityStr == null || quantityStr.isBlank())) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_INVALID_FIELDS_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_INVALID_FIELDS_MESSAGE));

            return;
        }

        try {
            BigDecimal currentPrice = new BigDecimal(currentPriceStr);

            BigDecimal quantity;
            BigDecimal avgUnitPrice;

            if ((quantityStr == null || quantityStr.isBlank())
                    && (avgUnitPriceStr == null || avgUnitPriceStr.isBlank())) {
                quantity = BigDecimal.ZERO;
                avgUnitPrice = BigDecimal.ZERO;
            } else {
                assert quantityStr != null;
                quantity = new BigDecimal(quantityStr);
                assert avgUnitPriceStr != null;
                avgUnitPrice = new BigDecimal(avgUnitPriceStr);
            }

            tickerService.addTicker(name, symbol, type, currentPrice, avgUnitPrice, quantity);

            WindowUtils.showSuccessDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_TICKER_ADDED_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_TICKER_ADDED_MESSAGE));

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_MESSAGE));
        } catch (IllegalArgumentException | EntityExistsException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_ERROR_ADDING_TICKER_TITLE),
                    e.getMessage());
        }
    }

    @FXML
    private void goToYahooLookup() {
        WindowUtils.openUrl(Constants.YAHOO_LOOKUP_URL);
    }
}
