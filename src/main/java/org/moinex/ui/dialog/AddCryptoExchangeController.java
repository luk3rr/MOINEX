/*
 * Filename: AddCryptoExchangeController.java
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
 * Controller for the add crypto exchange dialog
 */
@Controller
public class AddCryptoExchangeController
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

    public AddCryptoExchangeController() { }

    /**
     * Constructor
     * @param tickerService TickerService
     * @param calculatorService CalculatorService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddCryptoExchangeController(TickerService     tickerService,
                                    CalculatorService calculatorService)
    {
        this.tickerService     = tickerService;
        this.calculatorService = calculatorService;
    }

    public void SetFromCryptoComboBox(Ticker tk)
    {
        if (cryptos.stream().noneMatch(c -> c.GetId() == tk.GetId()))
        {
            return;
        }

        cryptoSoldComboBox.setValue(tk.GetSymbol());

        UpdateFromCryptoCurrentQuantity();
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
        String    cryptoSoldSymbol          = cryptoSoldComboBox.getValue();
        String    cryptoReceivedSymbol      = cryptoReceivedComboBox.getValue();
        String    cryptoSoldQuantityStr     = cryptoSoldQuantityField.getText();
        String    cryptoReceivedQuantityStr = cryptoReceivedQuantityField.getText();
        String    description               = descriptionField.getText();
        LocalDate exchangeDate              = exchangeDatePicker.getValue();

        if (cryptoSoldSymbol == null || cryptoReceivedSymbol == null ||
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

            Ticker cryptoSold = cryptos.stream()
                                    .filter(c -> c.GetSymbol().equals(cryptoSoldSymbol))
                                    .findFirst()
                                    .get();

            Ticker cryptoReceived =
                cryptos.stream()
                    .filter(c -> c.GetSymbol().equals(cryptoReceivedSymbol))
                    .findFirst()
                    .get();

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = exchangeDate.atTime(currentTime);

            tickerService.AddCryptoExchange(cryptoSold.GetId(),
                                            cryptoReceived.GetId(),
                                            cryptoSoldQuantity,
                                            cryptoReceivedQuantity,
                                            dateTimeWithCurrentHour,
                                            description);

            WindowUtils.ShowSuccessDialog("Success",
                                          "Exchange created",
                                          "The exchange was successfully created");

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
        String cryptoSoldSymbol = cryptoSoldComboBox.getValue();

        if (cryptoSoldSymbol == null)
        {
            return;
        }

        Ticker cryptoSold = cryptos.stream()
                                .filter(c -> c.GetSymbol().equals(cryptoSoldSymbol))
                                .findFirst()
                                .get();

        if (cryptoSold.GetCurrentQuantity().compareTo(BigDecimal.ZERO) < 0)
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
            cryptoSold.GetCurrentQuantity().toString());
    }

    private void UpdateToCryptoCurrentQuantity()
    {
        String cryptoReceivedSymbol = cryptoReceivedComboBox.getValue();

        if (cryptoReceivedSymbol == null)
        {
            return;
        }

        Ticker cryptoReceived =
            cryptos.stream()
                .filter(c -> c.GetSymbol().equals(cryptoReceivedSymbol))
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
        String cryptoSoldSymbol      = cryptoSoldComboBox.getValue();

        if (cryptoSoldQuantityStr == null || cryptoSoldQuantityStr.strip().isEmpty() ||
            cryptoSoldSymbol == null)
        {
            UIUtils.ResetLabel(cryptoSoldAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal exchangeQuantity = new BigDecimal(cryptoSoldQuantityStr);

            if (exchangeQuantity.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.ResetLabel(cryptoSoldAfterBalanceValueLabel);
                return;
            }

            Ticker cryptoSold = cryptos.stream()
                                    .filter(c -> c.GetSymbol().equals(cryptoSoldSymbol))
                                    .findFirst()
                                    .get();

            BigDecimal cryptoSoldAfterBalance =
                cryptoSold.GetCurrentQuantity().subtract(exchangeQuantity);

            if (cryptoSoldAfterBalance.compareTo(BigDecimal.ZERO) < 0)
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

            cryptoSoldAfterBalanceValueLabel.setText(cryptoSoldAfterBalance.toString());
        }
        catch (NumberFormatException e)
        {
            UIUtils.ResetLabel(cryptoSoldAfterBalanceValueLabel);
        }
    }

    private void UpdateToCryptoQuantityAfterExchange()
    {
        String cryptoReceivedQuantityStr = cryptoReceivedQuantityField.getText();
        String cryptoReceivedSymbol      = cryptoReceivedComboBox.getValue();

        if (cryptoReceivedQuantityStr == null ||
            cryptoReceivedQuantityStr.strip().isEmpty() || cryptoReceivedSymbol == null)
        {
            UIUtils.ResetLabel(cryptoReceivedAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal exchangeQuantity = new BigDecimal(cryptoReceivedQuantityStr);

            if (exchangeQuantity.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.ResetLabel(cryptoReceivedAfterBalanceValueLabel);
                return;
            }

            Ticker cryptoReceived =
                cryptos.stream()
                    .filter(c -> c.GetSymbol().equals(cryptoReceivedSymbol))
                    .findFirst()
                    .get();

            BigDecimal cryptoReceivedAfterBalance =
                cryptoReceived.GetCurrentQuantity().add(exchangeQuantity);

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
