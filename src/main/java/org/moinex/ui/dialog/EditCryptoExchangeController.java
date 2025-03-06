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
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
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

    public void setCryptoExchange(CryptoExchange cryptoExchange)
    {
        this.cryptoExchangeToUpdate = cryptoExchange;

        cryptoSoldComboBox.setValue(cryptoExchange.getSoldCrypto().getSymbol());
        cryptoReceivedComboBox.setValue(cryptoExchange.getReceivedCrypto().getSymbol());

        cryptoSoldQuantityField.setText(cryptoExchange.getSoldQuantity().toString());
        cryptoReceivedQuantityField.setText(
            cryptoExchange.getReceivedQuantity().toString());
        descriptionField.setText(cryptoExchange.getDescription());
        exchangeDatePicker.setValue(cryptoExchange.getDate().toLocalDate());

        updateFromCryptoCurrentQuantity();
        updateToCryptoCurrentQuantity();
        updateFromCryptoQuantityAfterExchange();
        updateToCryptoQuantityAfterExchange();
    }

    @FXML
    private void initialize()
    {
        loadCryptosFromDatabase();

        // Configure the date picker
        UIUtils.setDatePickerFormat(exchangeDatePicker);

        // Reset all labels
        UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel);
        UIUtils.resetLabel(cryptoReceivedAfterBalanceValueLabel);
        UIUtils.resetLabel(cryptoSoldCurrentBalanceValueLabel);
        UIUtils.resetLabel(cryptoReceivedCurrentBalanceValueLabel);

        cryptoSoldComboBox.setOnAction(e -> {
            updateFromCryptoCurrentQuantity();
            updateFromCryptoQuantityAfterExchange();
        });

        cryptoReceivedComboBox.setOnAction(e -> {
            updateToCryptoCurrentQuantity();
            updateToCryptoQuantityAfterExchange();
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
                    updateFromCryptoQuantityAfterExchange();
                    updateToCryptoQuantityAfterExchange();
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
                    updateFromCryptoQuantityAfterExchange();
                    updateToCryptoQuantityAfterExchange();
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
            WindowUtils.showErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all the fields.");
            return;
        }

        try
        {
            BigDecimal cryptoSoldQuantity = new BigDecimal(cryptoSoldQuantityStr);
            BigDecimal cryptoReceivedQuantity =
                new BigDecimal(cryptoReceivedQuantityStr);

            Ticker soldCrypto =
                cryptos.stream()
                    .filter(c -> c.getSymbol().equals(soldCryptoSymbol))
                    .findFirst()
                    .orElseThrow(()
                                     -> new RuntimeException(
                                         "Crypto with symbol: " + soldCryptoSymbol +
                                         " not found"));

            Ticker cryptoReceived =
                cryptos.stream()
                    .filter(c -> c.getSymbol().equals(receivedCryptoSymbol))
                    .findFirst()
                    .orElseThrow(()
                                     -> new RuntimeException(
                                         "Crypto with symbol: " + receivedCryptoSymbol +
                                         " not found"));

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = exchangeDate.atTime(currentTime);

            // Check if has any modification
            if (cryptoExchangeToUpdate.getSoldCrypto().getSymbol().equals(
                    soldCryptoSymbol) &&
                cryptoExchangeToUpdate.getReceivedCrypto().getSymbol().equals(
                    receivedCryptoSymbol) &&
                cryptoExchangeToUpdate.getSoldQuantity().compareTo(
                    cryptoSoldQuantity) == 0 &&
                cryptoExchangeToUpdate.getReceivedQuantity().compareTo(
                    cryptoReceivedQuantity) == 0 &&
                cryptoExchangeToUpdate.getDescription().equals(description) &&
                cryptoExchangeToUpdate.getDate().toLocalDate().equals(
                    dateTimeWithCurrentHour.toLocalDate()))
            {
                WindowUtils.showInformationDialog(
                    "Information",
                    "No changes",
                    "No changes were made to the exchange");
            }
            else // If there is any modification, update the exchange
            {
                cryptoExchangeToUpdate.setSoldCrypto(soldCrypto);
                cryptoExchangeToUpdate.setReceivedCrypto(cryptoReceived);
                cryptoExchangeToUpdate.setSoldQuantity(cryptoSoldQuantity);
                cryptoExchangeToUpdate.setReceivedQuantity(cryptoReceivedQuantity);
                cryptoExchangeToUpdate.setDescription(description);
                cryptoExchangeToUpdate.setDate(dateTimeWithCurrentHour);

                tickerService.updateCryptoExchange(cryptoExchangeToUpdate);

                WindowUtils.showSuccessDialog("Success",
                                              "Exchange updated",
                                              "Exchange updated successfully");
            }

            Stage stage = (Stage)cryptoReceivedQuantityField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Invalid exchange quantity",
                                        "The quantity must be a number");
        }
        catch (RuntimeException e)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Error while creating exchange",
                                        e.getMessage());
            return;
        }
    }

    @FXML
    private void handleCryptoSoldOpenCalculator()
    {
        WindowUtils.openPopupWindow(
            Constants.CALCULATOR_FXML,
            "Calculator",
            springContext,
            (CalculatorController controller)
                -> {},
            List.of(() -> { getResultFromCalculator(cryptoSoldQuantityField); }));
    }

    @FXML
    private void handleCryptoReceivedOpenCalculator()
    {
        WindowUtils.openPopupWindow(
            Constants.CALCULATOR_FXML,
            "Calculator",
            springContext,
            (CalculatorController controller)
                -> {},
            List.of(() -> { getResultFromCalculator(cryptoReceivedQuantityField); }));
    }

    private void getResultFromCalculator(TextField field)
    {
        // If the user saved the result, set it in the field
        String result = calculatorService.getResult();

        if (result != null)
        {
            try
            {
                BigDecimal resultValue = new BigDecimal(result);

                if (resultValue.compareTo(BigDecimal.ZERO) < 0)
                {
                    WindowUtils.showErrorDialog(
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
                WindowUtils.showErrorDialog("Error",
                                            "Invalid quantity",
                                            "The quantity must be a number");
            }
        }
    }

    private void updateFromCryptoCurrentQuantity()
    {
        String soldCryptoSymbol = cryptoSoldComboBox.getValue();

        if (soldCryptoSymbol == null)
        {
            return;
        }

        Ticker soldCrypto =
            cryptos.stream()
                .filter(c -> c.getSymbol().equals(soldCryptoSymbol))
                .findFirst()
                .orElseThrow(
                    ()
                        -> new RuntimeException(
                            "Crypto with symbol: " + soldCryptoSymbol + " not found"));

        if (soldCrypto.getCurrentQuantity().compareTo(BigDecimal.ZERO) < 0)
        {
            // Must be unreachable
            UIUtils.setLabelStyle(cryptoSoldCurrentBalanceValueLabel,
                                  Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            UIUtils.setLabelStyle(cryptoSoldCurrentBalanceValueLabel,
                                  Constants.NEUTRAL_BALANCE_STYLE);
        }

        cryptoSoldCurrentBalanceValueLabel.setText(
            soldCrypto.getCurrentQuantity().toString());
    }

    private void updateToCryptoCurrentQuantity()
    {
        String receivedCryptoSymbol = cryptoReceivedComboBox.getValue();

        if (receivedCryptoSymbol == null)
        {
            return;
        }

        Ticker cryptoReceived =
            cryptos.stream()
                .filter(c -> c.getSymbol().equals(receivedCryptoSymbol))
                .findFirst()
                .orElseThrow(()
                                 -> new RuntimeException(
                                     "Crypto with symbol: " + receivedCryptoSymbol +
                                     " not found"));

        if (cryptoReceived.getCurrentQuantity().compareTo(BigDecimal.ZERO) < 0)
        {
            // Must be unreachable
            UIUtils.setLabelStyle(cryptoReceivedCurrentBalanceValueLabel,
                                  Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            UIUtils.setLabelStyle(cryptoReceivedCurrentBalanceValueLabel,
                                  Constants.NEUTRAL_BALANCE_STYLE);
        }

        cryptoReceivedCurrentBalanceValueLabel.setText(
            cryptoReceived.getCurrentQuantity().toString());
    }

    private void updateFromCryptoQuantityAfterExchange()
    {
        String cryptoSoldQuantityStr = cryptoSoldQuantityField.getText();
        String soldCryptoSymbol      = cryptoSoldComboBox.getValue();

        if (cryptoSoldQuantityStr == null || cryptoSoldQuantityStr.strip().isEmpty() ||
            soldCryptoSymbol == null)
        {
            UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal oldSoldQuantity = cryptoExchangeToUpdate.getSoldQuantity();

            BigDecimal exchangeQuantity = new BigDecimal(cryptoSoldQuantityStr);

            if (exchangeQuantity.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel);
                return;
            }

            Ticker soldCrypto =
                cryptos.stream()
                    .filter(c -> c.getSymbol().equals(soldCryptoSymbol))
                    .findFirst()
                    .orElseThrow(()
                                     -> new RuntimeException(
                                         "Crypto with symbol: " + soldCryptoSymbol +
                                         " not found"));

            BigDecimal soldCryptoAfterBalance = soldCrypto.getCurrentQuantity()
                                                    .add(oldSoldQuantity)
                                                    .subtract(exchangeQuantity);

            if (soldCryptoAfterBalance.compareTo(BigDecimal.ZERO) < 0)
            {
                // Remove old style and add negative style
                UIUtils.setLabelStyle(cryptoSoldAfterBalanceValueLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                UIUtils.setLabelStyle(cryptoSoldAfterBalanceValueLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            cryptoSoldAfterBalanceValueLabel.setText(soldCryptoAfterBalance.toString());
        }
        catch (NumberFormatException e)
        {
            UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel);
        }
    }

    private void updateToCryptoQuantityAfterExchange()
    {
        String cryptoReceivedQuantityStr = cryptoReceivedQuantityField.getText();
        String receivedCryptoSymbol      = cryptoReceivedComboBox.getValue();

        if (cryptoReceivedQuantityStr == null ||
            cryptoReceivedQuantityStr.strip().isEmpty() || receivedCryptoSymbol == null)
        {
            UIUtils.resetLabel(cryptoReceivedAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal oldReceivedQuantity =
                cryptoExchangeToUpdate.getReceivedQuantity();

            BigDecimal exchangeQuantity = new BigDecimal(cryptoReceivedQuantityStr);

            if (exchangeQuantity.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(cryptoReceivedAfterBalanceValueLabel);
                return;
            }

            Ticker cryptoReceived =
                cryptos.stream()
                    .filter(c -> c.getSymbol().equals(receivedCryptoSymbol))
                    .findFirst()
                    .orElseThrow(()
                                     -> new RuntimeException(
                                         "Crypto with symbol: " + receivedCryptoSymbol +
                                         " not found"));

            BigDecimal cryptoReceivedAfterBalance = cryptoReceived.getCurrentQuantity()
                                                        .subtract(oldReceivedQuantity)
                                                        .add(exchangeQuantity);

            if (cryptoReceivedAfterBalance.compareTo(BigDecimal.ZERO) < 0)
            {
                // Remove old style and add negative style
                UIUtils.setLabelStyle(cryptoReceivedAfterBalanceValueLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                UIUtils.setLabelStyle(cryptoReceivedAfterBalanceValueLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            cryptoReceivedAfterBalanceValueLabel.setText(
                cryptoReceivedAfterBalance.toString());
        }
        catch (NumberFormatException e)
        {
            UIUtils.resetLabel(cryptoReceivedAfterBalanceValueLabel);
        }
    }

    private void loadCryptosFromDatabase()
    {
        cryptos =
            tickerService.getAllNonArchivedTickersByType(TickerType.CRYPTOCURRENCY);

        cryptoSoldComboBox.getItems().addAll(
            cryptos.stream().map(Ticker::getSymbol).toList());

        cryptoReceivedComboBox.getItems().addAll(
            cryptos.stream().map(Ticker::getSymbol).toList());
    }
}
