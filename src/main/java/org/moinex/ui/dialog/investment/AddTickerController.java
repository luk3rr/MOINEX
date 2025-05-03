/*
 * Filename: AddTickerController.java
 * Created on: January  8, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import jakarta.persistence.EntityExistsException;
import java.math.BigDecimal;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.service.TickerService;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TickerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Ticker dialog
 */
@Controller
@NoArgsConstructor
public final class AddTickerController extends BaseTickerManagement {
    /**
     * Constructor
     * @param tickerService Ticker service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddTickerController(TickerService tickerService) {
        super(tickerService);
    }

    @FXML
    @Override
    protected void handleSave() {
        String name = nameField.getText();
        String symbol = symbolField.getText();
        String currentPriceStr = currentPriceField.getText();
        TickerType type = typeComboBox.getValue();
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
                    "Empty fields", "Please fill all required fields before saving");

            return;
        }

        // If quantity is set, then avgUnitPrice must be set or vice versa
        if ((quantityStr == null || quantityStr.isBlank())
                        && !(avgUnitPriceStr == null || avgUnitPriceStr.isBlank())
                || (avgUnitPriceStr == null || avgUnitPriceStr.isBlank())
                        && !(quantityStr == null || quantityStr.isBlank())) {
            WindowUtils.showInformationDialog(
                    "Invalid fields",
                    "Quantity must be set if average unit price is set or vice-versa");

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

            WindowUtils.showSuccessDialog("Ticker added", "Ticker added successfully.");

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog("Invalid number", "Invalid price or quantity");
        } catch (IllegalArgumentException | EntityExistsException e) {
            WindowUtils.showErrorDialog("Error while adding ticker", e.getMessage());
        }
    }
}
