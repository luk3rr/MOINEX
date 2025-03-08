/*
 * Filename: AddTickerController.java
 * Created on: January  8, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import jakarta.persistence.EntityExistsException;
import java.math.BigDecimal;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.services.TickerService;
import org.moinex.util.Constants;
import org.moinex.util.TickerType;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Ticker dialog
 */
@Controller
@NoArgsConstructor
public class AddTickerController
{
    @FXML
    private TextField nameField;

    @FXML
    private TextField symbolField;

    @FXML
    private TextField currentPriceField;

    @FXML
    private TextField quantityField;

    @FXML
    private TextField avgUnitPriceField;

    @FXML
    private ComboBox<String> typeComboBox;

    private TickerService tickerService;

    /**
     * Constructor
     * @param tickerService Ticker service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddTickerController(TickerService tickerService)
    {
        this.tickerService = tickerService;
    }

    @FXML
    private void initialize()
    {
        configureTypeComboBox();
        configureListeners();
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)nameField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSave()
    {
        String name            = nameField.getText();
        String symbol          = symbolField.getText();
        String currentPriceStr = currentPriceField.getText();
        String typeStr         = typeComboBox.getValue();
        String quantityStr     = quantityField.getText();
        String avgUnitPriceStr = avgUnitPriceField.getText();

        if (name == null || symbol == null || currentPriceStr == null ||
            typeStr == null || name.strip().isEmpty() || symbol.strip().isEmpty() ||
            currentPriceStr.strip().isEmpty())
        {
            WindowUtils.showErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all required fields");

            return;
        }

        // If quantity is set, then avgUnitPrice must be set or vice-versa
        if ((quantityStr == null || quantityStr.strip().isEmpty()) &&
                !(avgUnitPriceStr == null || avgUnitPriceStr.strip().isEmpty()) ||
            (avgUnitPriceStr == null || avgUnitPriceStr.strip().isEmpty()) &&
                !(quantityStr == null || quantityStr.strip().isEmpty()))
        {
            WindowUtils.showInformationDialog(
                "Info",
                "Invalid fields",
                "Quantity must be set if average unit price is set or vice-versa");

            return;
        }

        try
        {
            BigDecimal currentPrice = new BigDecimal(currentPriceStr);

            TickerType type = TickerType.valueOf(typeStr);

            BigDecimal quantity;
            BigDecimal avgUnitPrice;

            if ((quantityStr == null || quantityStr.strip().isEmpty()) &&
                (avgUnitPriceStr == null || avgUnitPriceStr.strip().isEmpty()))
            {
                quantity     = BigDecimal.ZERO;
                avgUnitPrice = BigDecimal.ZERO;
            }
            else
            {
                quantity     = new BigDecimal(quantityStr);
                avgUnitPrice = new BigDecimal(avgUnitPriceStr);
            }

            tickerService
                .addTicker(name, symbol, type, currentPrice, avgUnitPrice, quantity);

            WindowUtils.showSuccessDialog("Success",
                                          "Ticker added",
                                          "Ticker added successfully.");

            Stage stage = (Stage)nameField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Invalid number",
                                        "Invalid price or quantity");
        }
        catch (IllegalArgumentException | EntityExistsException e)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Error while adding ticker",
                                        e.getMessage());
        }
    }

    private void configureTypeComboBox()
    {
        for (TickerType type : TickerType.values())
        {
            typeComboBox.getItems().add(type.toString());
        }
    }

    private void configureListeners()
    {
        currentPriceField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX))
                {
                    currentPriceField.setText(oldValue);
                }
            });

        quantityField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX))
            {
                quantityField.setText(oldValue);
            }
        });

        avgUnitPriceField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX))
                {
                    avgUnitPriceField.setText(oldValue);
                }
            });
    }
}
