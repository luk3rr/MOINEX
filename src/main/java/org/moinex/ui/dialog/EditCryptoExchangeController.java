/*
 * Filename: EditCryptoExchangeController.java
 * Created on: January 28, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.moinex.entities.investment.CryptoExchange;
import org.moinex.entities.investment.Ticker;
import org.moinex.services.CalculatorService;
import org.moinex.services.TickerService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.TickerType;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the edit crypto exchange dialog
 */
@Controller
public class EditCryptoExchangeController
{
    @FXML
    private Label cryptoSoldAfterBalanceValueLabel;

    @FXML
    private Label cryptoReceivedAfterBalanceValueLabel;

    @FXML
    private Label cryptoSoldCurrentBalanceValueLabel;

    @FXML
    private Label cryptoReceivedCurrentBalanceValueLabel;

    @FXML
    private ComboBox<String> cryptoSoldComboBox;

    @FXML
    private ComboBox<String> cryptoReceivedComboBox;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField cryptoSoldQuantityField;

    @FXML
    private TextField cryptoReceivedQuantityField;

    @FXML
    private DatePicker exchangeDatePicker;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private TickerService tickerService;

    private CalculatorService calculatorService;

    private List<Ticker> cryptos;

    private CryptoExchange cryptoExchangeToUpdate;

    public EditCryptoExchangeController() { }

    /**
     * Constructor
     * @param tickerService TickerService
     * @param calculatorService CalculatorService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public EditCryptoExchangeController(TickerService     tickerService,
                                        CalculatorService calculatorService)
    {
        this.tickerService     = tickerService;
        this.calculatorService = calculatorService;
    }

    public void SetCryptoExchange(CryptoExchange cryptoExchange)
    {
        this.cryptoExchangeToUpdate = cryptoExchange;

        cryptoSoldComboBox.setValue(cryptoExchange.GetSoldCrypto().GetSymbol());
        cryptoReceivedComboBox.setValue(cryptoExchange.GetReceivedCrypto().GetSymbol());

        cryptoSoldQuantityField.setText(cryptoExchange.GetSoldQuantity().toString());
        cryptoReceivedQuantityField.setText(
            cryptoExchange.GetReceivedQuantity().toString());
        descriptionField.setText(cryptoExchange.GetDescription());
        exchangeDatePicker.setValue(cryptoExchange.GetDate().toLocalDate());

        UpdateFromCryptoCurrentQuantity();
        UpdateToCryptoCurrentQuantity();
        UpdateFromCryptoQuantityAfterExchange();
        UpdateToCryptoQuantityAfterExchange();
    }

    @FXML
    private void initialize()
    {
        LoadCryptos();

        // Configure the date picker
        UIUtils.SetDatePickerFormat(exchangeDatePicker);

        // Reset all labels
        UIUtils.ResetLabel(cryptoSoldAfterBalanceValueLabel);
        UIUtils.ResetLabel(cryptoReceivedAfterBalanceValueLabel);
        UIUtils.ResetLabel(cryptoSoldCurrentBalanceValueLabel);
        UIUtils.ResetLabel(cryptoReceivedCurrentBalanceValueLabel);

        cryptoSoldComboBox.setOnAction(e -> {
            UpdateFromCryptoCurrentQuantity();
            UpdateFromCryptoQuantityAfterExchange();
        });

        cryptoReceivedComboBox.setOnAction(e -> {
            UpdateToCryptoCurrentQuantity();
            UpdateToCryptoQuantityAfterExchange();
        });

        // Ensure that the user can only input numbers in the quantity field
        cryptoSoldQuantityField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX))
                {
                    cryptoSoldQuantityField.setText(oldValue);
                }
                else
                {
                    UpdateFromCryptoQuantityAfterExchange();
                    UpdateToCryptoQuantityAfterExchange();
                }
            });

        cryptoReceivedQuantityField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX))
                {
                    cryptoReceivedQuantityField.setText(oldValue);
                }
                else
                {
                    UpdateFromCryptoQuantityAfterExchange();
                    UpdateToCryptoQuantityAfterExchange();
                }
            });
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)cryptoReceivedQuantityField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSave()
    {
        String    soldCryptoSymbol          = cryptoSoldComboBox.getValue();
        String    receivedCryptoSymbol      = cryptoReceivedComboBox.getValue();
        String    cryptoSoldQuantityStr     = cryptoSoldQuantityField.getText();
        String    cryptoReceivedQuantityStr = cryptoReceivedQuantityField.getText();
        String    description               = descriptionField.getText().trim();
        LocalDate exchangeDate              = exchangeDatePicker.getValue();

        if (soldCryptoSymbol == null || receivedCryptoSymbol == null ||
            cryptoSoldQuantityStr == null || cryptoSoldQuantityStr.strip().isEmpty() ||
            cryptoReceivedQuantityStr == null ||
            cryptoReceivedQuantityStr.strip().isEmpty() || description == null ||
            description.strip().isEmpty() || exchangeDate == null)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all the fields.");
            return;
        }

        try
        {
            BigDecimal cryptoSoldQuantity = new BigDecimal(cryptoSoldQuantityStr);
            BigDecimal cryptoReceivedQuantity =
                new BigDecimal(cryptoReceivedQuantityStr);

            Ticker soldCrypto = cryptos.stream()
                                    .filter(c -> c.GetSymbol().equals(soldCryptoSymbol))
                                    .findFirst()
                                    .get();

            Ticker cryptoReceived =
                cryptos.stream()
                    .filter(c -> c.GetSymbol().equals(receivedCryptoSymbol))
                    .findFirst()
                    .get();

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = exchangeDate.atTime(currentTime);

            // Check if has any modification
            if (cryptoExchangeToUpdate.GetSoldCrypto().GetSymbol().equals(
                    soldCryptoSymbol) &&
                cryptoExchangeToUpdate.GetReceivedCrypto().GetSymbol().equals(
                    receivedCryptoSymbol) &&
                cryptoExchangeToUpdate.GetSoldQuantity().compareTo(
                    cryptoSoldQuantity) == 0 &&
                cryptoExchangeToUpdate.GetReceivedQuantity().compareTo(
                    cryptoReceivedQuantity) == 0 &&
                cryptoExchangeToUpdate.GetDescription().equals(description) &&
                cryptoExchangeToUpdate.GetDate().toLocalDate().equals(
                    dateTimeWithCurrentHour.toLocalDate()))
            {
                WindowUtils.ShowInformationDialog(
                    "Information",
                    "No changes",
                    "No changes were made to the exchange");
            }
            else // If there is any modification, update the exchange
            {
                cryptoExchangeToUpdate.SetSoldCrypto(soldCrypto);
                cryptoExchangeToUpdate.SetReceivedCrypto(cryptoReceived);
                cryptoExchangeToUpdate.SetSoldQuantity(cryptoSoldQuantity);
                cryptoExchangeToUpdate.SetReceivedQuantity(cryptoReceivedQuantity);
                cryptoExchangeToUpdate.SetDescription(description);
                cryptoExchangeToUpdate.SetDate(dateTimeWithCurrentHour);

                tickerService.UpdateCryptoExchange(cryptoExchangeToUpdate);

                WindowUtils.ShowSuccessDialog("Success",
                                              "Exchange updated",
                                              "Exchange updated successfully");
            }

            Stage stage = (Stage)cryptoReceivedQuantityField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Invalid exchange quantity",
                                        "The quantity must be a number");
        }
        catch (RuntimeException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Error while creating exchange",
                                        e.getMessage());
            return;
        }
    }

    @FXML
    private void handleCryptoSoldOpenCalculator()
    {
        WindowUtils.OpenPopupWindow(
            Constants.CALCULATOR_FXML,
            "Calculator",
            springContext,
            (CalculatorController controller)
                -> {},
            List.of(() -> { GetResultFromCalculator(cryptoSoldQuantityField); }));
    }

    @FXML
    private void handleCryptoReceivedOpenCalculator()
    {
        WindowUtils.OpenPopupWindow(
            Constants.CALCULATOR_FXML,
            "Calculator",
            springContext,
            (CalculatorController controller)
                -> {},
            List.of(() -> { GetResultFromCalculator(cryptoReceivedQuantityField); }));
    }

    private void GetResultFromCalculator(TextField field)
    {
        // If the user saved the result, set it in the field
        String result = calculatorService.GetResult();

        if (result != null)
        {
            try
            {
                BigDecimal resultValue = new BigDecimal(result);

                if (resultValue.compareTo(BigDecimal.ZERO) < 0)
                {
                    WindowUtils.ShowErrorDialog(
                        "Error",
                        "Invalid quantity",
                        "The quantity must be a positive number");
                    return;
                }

                // Ensure that the result has the correct precision
                result = resultValue
                             .setScale(Constants.INVESTMENT_CALCULATION_PRECISION,
                                       RoundingMode.HALF_UP)
                             .stripTrailingZeros()
                             .toString();

                field.setText(result);
            }
            catch (NumberFormatException e)
            {
                // Must be unreachable
                WindowUtils.ShowErrorDialog("Error",
                                            "Invalid quantity",
                                            "The quantity must be a number");
            }
        }
    }

    private void UpdateFromCryptoCurrentQuantity()
    {
        String soldCryptoSymbol = cryptoSoldComboBox.getValue();

        if (soldCryptoSymbol == null)
        {
            return;
        }

        Ticker soldCrypto = cryptos.stream()
                                .filter(c -> c.GetSymbol().equals(soldCryptoSymbol))
                                .findFirst()
                                .get();

        if (soldCrypto.GetCurrentQuantity().compareTo(BigDecimal.ZERO) < 0)
        {
            // Must be unreachable
            UIUtils.SetLabelStyle(cryptoSoldCurrentBalanceValueLabel,
                                  Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            UIUtils.SetLabelStyle(cryptoSoldCurrentBalanceValueLabel,
                                  Constants.NEUTRAL_BALANCE_STYLE);
        }

        cryptoSoldCurrentBalanceValueLabel.setText(
            soldCrypto.GetCurrentQuantity().toString());
    }

    private void UpdateToCryptoCurrentQuantity()
    {
        String receivedCryptoSymbol = cryptoReceivedComboBox.getValue();

        if (receivedCryptoSymbol == null)
        {
            return;
        }

        Ticker cryptoReceived =
            cryptos.stream()
                .filter(c -> c.GetSymbol().equals(receivedCryptoSymbol))
                .findFirst()
                .get();

        if (cryptoReceived.GetCurrentQuantity().compareTo(BigDecimal.ZERO) < 0)
        {
            // Must be unreachable
            UIUtils.SetLabelStyle(cryptoReceivedCurrentBalanceValueLabel,
                                  Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            UIUtils.SetLabelStyle(cryptoReceivedCurrentBalanceValueLabel,
                                  Constants.NEUTRAL_BALANCE_STYLE);
        }

        cryptoReceivedCurrentBalanceValueLabel.setText(
            cryptoReceived.GetCurrentQuantity().toString());
    }

    private void UpdateFromCryptoQuantityAfterExchange()
    {
        String cryptoSoldQuantityStr = cryptoSoldQuantityField.getText();
        String soldCryptoSymbol      = cryptoSoldComboBox.getValue();

        if (cryptoSoldQuantityStr == null || cryptoSoldQuantityStr.strip().isEmpty() ||
            soldCryptoSymbol == null)
        {
            UIUtils.ResetLabel(cryptoSoldAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal oldSoldQuantity = cryptoExchangeToUpdate.GetSoldQuantity();

            BigDecimal exchangeQuantity = new BigDecimal(cryptoSoldQuantityStr);

            if (exchangeQuantity.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.ResetLabel(cryptoSoldAfterBalanceValueLabel);
                return;
            }

            Ticker soldCrypto = cryptos.stream()
                                    .filter(c -> c.GetSymbol().equals(soldCryptoSymbol))
                                    .findFirst()
                                    .get();

            BigDecimal soldCryptoAfterBalance = soldCrypto.GetCurrentQuantity()
                                                    .add(oldSoldQuantity)
                                                    .subtract(exchangeQuantity);

            if (soldCryptoAfterBalance.compareTo(BigDecimal.ZERO) < 0)
            {
                // Remove old style and add negative style
                UIUtils.SetLabelStyle(cryptoSoldAfterBalanceValueLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                UIUtils.SetLabelStyle(cryptoSoldAfterBalanceValueLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            cryptoSoldAfterBalanceValueLabel.setText(soldCryptoAfterBalance.toString());
        }
        catch (NumberFormatException e)
        {
            UIUtils.ResetLabel(cryptoSoldAfterBalanceValueLabel);
        }
    }

    private void UpdateToCryptoQuantityAfterExchange()
    {
        String cryptoReceivedQuantityStr = cryptoReceivedQuantityField.getText();
        String receivedCryptoSymbol      = cryptoReceivedComboBox.getValue();

        if (cryptoReceivedQuantityStr == null ||
            cryptoReceivedQuantityStr.strip().isEmpty() || receivedCryptoSymbol == null)
        {
            UIUtils.ResetLabel(cryptoReceivedAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal oldReceivedQuantity =
                cryptoExchangeToUpdate.GetReceivedQuantity();

            BigDecimal exchangeQuantity = new BigDecimal(cryptoReceivedQuantityStr);

            if (exchangeQuantity.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.ResetLabel(cryptoReceivedAfterBalanceValueLabel);
                return;
            }

            Ticker cryptoReceived =
                cryptos.stream()
                    .filter(c -> c.GetSymbol().equals(receivedCryptoSymbol))
                    .findFirst()
                    .get();

            BigDecimal cryptoReceivedAfterBalance = cryptoReceived.GetCurrentQuantity()
                                                        .subtract(oldReceivedQuantity)
                                                        .add(exchangeQuantity);

            if (cryptoReceivedAfterBalance.compareTo(BigDecimal.ZERO) < 0)
            {
                // Remove old style and add negative style
                UIUtils.SetLabelStyle(cryptoReceivedAfterBalanceValueLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                UIUtils.SetLabelStyle(cryptoReceivedAfterBalanceValueLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            cryptoReceivedAfterBalanceValueLabel.setText(
                cryptoReceivedAfterBalance.toString());
        }
        catch (NumberFormatException e)
        {
            UIUtils.ResetLabel(cryptoReceivedAfterBalanceValueLabel);
        }
    }

    private void LoadCryptos()
    {
        cryptos =
            tickerService.GetAllNonArchivedTickersByType(TickerType.CRYPTOCURRENCY);

        cryptoSoldComboBox.getItems().addAll(
            cryptos.stream().map(Ticker::GetSymbol).toList());

        cryptoReceivedComboBox.getItems().addAll(
            cryptos.stream().map(Ticker::GetSymbol).toList());
    }
}
