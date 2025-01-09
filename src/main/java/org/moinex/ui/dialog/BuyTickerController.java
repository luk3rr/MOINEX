/*
 * Filename: BuyTicker.java
 * Created on: January  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.moinex.entities.investment.Ticker;
import org.moinex.services.TickerService;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Buy Ticker dialog
 */
@Controller
public class BuyTickerController
{
    @FXML
    private TextField nameField;

    @FXML
    private TextField symbolField;

    @FXML
    private TextField unitPriceField;

    @FXML
    private TextField quantityField;

    @FXML
    private Label totalPriceLabel;

    @FXML
    private DatePicker buyDatePicker;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private TickerService tickerService;

    private Ticker ticker;

    /**
     * Constructor
     * @param tickerService Ticker service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public BuyTickerController(TickerService tickerService)
    {
        this.tickerService = tickerService;
    }

    public void SetTicker(Ticker ticker)
    {
        this.ticker = ticker;

        nameField.setText(ticker.GetName());
        symbolField.setText(ticker.GetSymbol());
        unitPriceField.setText(ticker.GetCurrentUnitValue().toString());
    }

    @FXML
    private void initialize()
    {
        // Configure date picker
        UIUtils.SetDatePickerFormat(buyDatePicker);

        // Configure listeners
        unitPriceField.textProperty().addListener(
            (observable, oldValue, newValue) -> { UpdateTotalPrice(); });

        quantityField.textProperty().addListener(
            (observable, oldValue, newValue) -> { UpdateTotalPrice(); });
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
        String    name         = nameField.getText();
        String    symbol       = symbolField.getText();
        String    unitPriceStr = unitPriceField.getText();
        String    quantityStr  = quantityField.getText();
        LocalDate buyDate      = buyDatePicker.getValue();

        if (name == null || symbol == null || unitPriceStr == null ||
            name.strip().isEmpty() || symbol.strip().isEmpty() ||
            unitPriceStr.strip().isEmpty() || buyDate == null)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all the fields");

            return;
        }

        try
        {
            BigDecimal unitPrice = new BigDecimal(unitPriceStr);

            BigDecimal quantity = new BigDecimal(quantityStr);

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = buyDate.atTime(currentTime);

            tickerService.AddPurchase(ticker.GetId(),
                                      quantity,
                                      unitPrice,
                                      dateTimeWithCurrentHour);

            WindowUtils.ShowSuccessDialog("Success",
                                          "Purchase added",
                                          "Purchase added successfully");

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
    }

    private void UpdateTotalPrice()
    {
        String unitPriceStr = unitPriceField.getText();
        String quantityStr  = quantityField.getText();

        BigDecimal totalPrice = new BigDecimal("0.00");

        if (unitPriceStr == null || quantityStr == null ||
            unitPriceStr.strip().isEmpty() || quantityStr.strip().isEmpty())
        {
            totalPriceLabel.setText(UIUtils.FormatCurrency(totalPrice));
            return;
        }

        try
        {
            BigDecimal unitPrice = new BigDecimal(unitPriceStr);
            BigDecimal quantity  = new BigDecimal(quantityStr);

            totalPrice = unitPrice.multiply(quantity);

            totalPriceLabel.setText(UIUtils.FormatCurrency(totalPrice));
        }
        catch (NumberFormatException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Invalid number",
                                        "Invalid price or quantity");

            totalPrice = new BigDecimal("0.00");

            totalPriceLabel.setText(UIUtils.FormatCurrency(totalPrice));
        }
    }
}
