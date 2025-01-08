/*
 * Filename: AddTickerController.java
 * Created on: January  8, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.math.BigDecimal;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.moinex.services.TickerService;
import org.moinex.util.TickerType;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Ticker dialog
 */
@Controller
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
    private ComboBox<String> typeComboBox;

    @Autowired
    private ConfigurableApplicationContext springContext;

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
        ConfigureTypeComboBox();
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

        if (name == null || symbol == null || currentPriceStr == null ||
            typeStr == null || name.strip().isEmpty() || symbol.strip().isEmpty() ||
            currentPriceStr.strip().isEmpty())
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all the fields.");

            return;
        }

        try
        {
            BigDecimal currentPrice = new BigDecimal(currentPriceStr);

            TickerType type = TickerType.valueOf(typeStr);

            BigDecimal quantity;

            if (quantityStr == null || quantityStr.strip().isEmpty())
            {
                quantity = BigDecimal.ZERO;
            }
            else
            {
                quantity = new BigDecimal(quantityStr);
            }

            tickerService.RegisterTicker(name, symbol, type, currentPrice, quantity);

            WindowUtils.ShowSuccessDialog("Success",
                                          "Ticker added",
                                          "Ticker added successfully.");

            Stage stage = (Stage)nameField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Invalid number",
                                        "Invalid price or quantity");
        }
        catch (RuntimeException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Error while adding ticker",
                                        e.getMessage());
        }

        Stage stage = (Stage)nameField.getScene().getWindow();
        stage.close();
    }

    private void ConfigureTypeComboBox()
    {
        for (TickerType type : TickerType.values())
        {
            typeComboBox.getItems().add(type.toString());
        }
    }
}
