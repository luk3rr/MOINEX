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
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TickerType;
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
    private ComboBox<TickerType> typeComboBox;

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
        configureComboBoxes();
        populateTypeComboBox();
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
        String     name            = nameField.getText();
        String     symbol          = symbolField.getText();
        String     currentPriceStr = currentPriceField.getText();
        TickerType type            = typeComboBox.getValue();
        String     quantityStr     = quantityField.getText();
        String     avgUnitPriceStr = avgUnitPriceField.getText();

        if (name == null || symbol == null || currentPriceStr == null || type == null ||
            name.strip().isEmpty() || symbol.strip().isEmpty() ||
            currentPriceStr.strip().isEmpty())
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");

            return;
        }

        // If quantity is set, then avgUnitPrice must be set or vice-versa
        if ((quantityStr == null || quantityStr.strip().isEmpty()) &&
                !(avgUnitPriceStr == null || avgUnitPriceStr.strip().isEmpty()) ||
            (avgUnitPriceStr == null || avgUnitPriceStr.strip().isEmpty()) &&
                !(quantityStr == null || quantityStr.strip().isEmpty()))
        {
            WindowUtils.showInformationDialog(
                "Invalid fields",
                "Quantity must be set if average unit price is set or vice-versa");

            return;
        }

        try
        {
            BigDecimal currentPrice = new BigDecimal(currentPriceStr);

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

            WindowUtils.showSuccessDialog("Ticker added", "Ticker added successfully.");

            Stage stage = (Stage)nameField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid number", "Invalid price or quantity");
        }
        catch (IllegalArgumentException | EntityExistsException e)
        {
            WindowUtils.showErrorDialog("Error while adding ticker", e.getMessage());
        }
    }

    private void populateTypeComboBox()
    {
        typeComboBox.getItems().setAll(TickerType.values());
    }

    private void configureComboBoxes()
    {
        UIUtils.configureComboBox(typeComboBox, TickerType::name);
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
