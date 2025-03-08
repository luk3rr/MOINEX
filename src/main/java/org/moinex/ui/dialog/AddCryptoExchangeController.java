/*
 * Filename: AddCryptoExchangeController.java
 * Created on: January 28, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import jakarta.persistence.EntityNotFoundException;
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
import org.moinex.entities.investment.Ticker;
import org.moinex.exceptions.InsufficientResourcesException;
import org.moinex.exceptions.InvalidTickerTypeException;
import org.moinex.exceptions.SameSourceDestionationException;
import org.moinex.services.CalculatorService;
import org.moinex.services.TickerService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.enums.TickerType;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the add crypto exchange dialog
 */
@Controller
@NoArgsConstructor
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

    public void setFromCryptoComboBox(Ticker tk)
    {
        if (cryptos.stream().noneMatch(c -> c.getId().equals(tk.getId())))
        {
            return;
        }

        cryptoSoldComboBox.setValue(tk.getSymbol());

        updateFromCryptoCurrentQuantity();
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
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");
            return;
        }

        try
        {
            BigDecimal cryptoSoldQuantity = new BigDecimal(cryptoSoldQuantityStr);
            BigDecimal cryptoReceivedQuantity =
                new BigDecimal(cryptoReceivedQuantityStr);

            Ticker cryptoSold =
                cryptos.stream()
                    .filter(c -> c.getSymbol().equals(cryptoSoldSymbol))
                    .findFirst()
                    .orElseThrow(
                        ()
                            -> new EntityNotFoundException(
                                "Crypto not found with symbol: " + cryptoSoldSymbol));

            Ticker cryptoReceived =
                cryptos.stream()
                    .filter(c -> c.getSymbol().equals(cryptoReceivedSymbol))
                    .findFirst()
                    .orElseThrow(()
                                     -> new EntityNotFoundException(
                                         "Crypto not found with symbol: " +
                                         cryptoReceivedSymbol));

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = exchangeDate.atTime(currentTime);

            tickerService.addCryptoExchange(cryptoSold.getId(),
                                            cryptoReceived.getId(),
                                            cryptoSoldQuantity,
                                            cryptoReceivedQuantity,
                                            dateTimeWithCurrentHour,
                                            description);

            WindowUtils.showSuccessDialog("Exchange created",
                                          "The exchange was successfully created");

            Stage stage = (Stage)cryptoReceivedQuantityField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid exchange quantity",
                                        "The quantity must be a number");
        }
        catch (SameSourceDestionationException | EntityNotFoundException |
               InvalidTickerTypeException | IllegalArgumentException |
               InsufficientResourcesException e)
        {
            WindowUtils.showErrorDialog("Error while creating exchange",
                                        e.getMessage());
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
            List.of(() -> getResultFromCalculator(cryptoSoldQuantityField)));
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
            List.of(() -> getResultFromCalculator(cryptoReceivedQuantityField)));
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
                    WindowUtils.showInformationDialog("Invalid quantity",
                                                      "The quantity must be positive");
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
                WindowUtils.showErrorDialog("Invalid quantity",
                                            "The quantity must be a number");
            }
        }
    }

    private void updateFromCryptoCurrentQuantity()
    {
        String cryptoSoldSymbol = cryptoSoldComboBox.getValue();

        if (cryptoSoldSymbol == null)
        {
            return;
        }

        Ticker cryptoSold = cryptos.stream()
                                .filter(c -> c.getSymbol().equals(cryptoSoldSymbol))
                                .findFirst()
                                .orElseThrow(()
                                                 -> new EntityNotFoundException(
                                                     "Crypto not found with symbol: " +
                                                     cryptoSoldSymbol));

        if (cryptoSold.getCurrentQuantity().compareTo(BigDecimal.ZERO) < 0)
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
            cryptoSold.getCurrentQuantity().toString());
    }

    private void updateToCryptoCurrentQuantity()
    {
        String cryptoReceivedSymbol = cryptoReceivedComboBox.getValue();

        if (cryptoReceivedSymbol == null)
        {
            return;
        }

        Ticker cryptoReceived =
            cryptos.stream()
                .filter(c -> c.getSymbol().equals(cryptoReceivedSymbol))
                .findFirst()
                .orElseThrow(
                    ()
                        -> new EntityNotFoundException(
                            "Crypto not found with symbol: " + cryptoReceivedSymbol));

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
        String cryptoSoldSymbol      = cryptoSoldComboBox.getValue();

        if (cryptoSoldQuantityStr == null || cryptoSoldQuantityStr.strip().isEmpty() ||
            cryptoSoldSymbol == null)
        {
            UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal exchangeQuantity = new BigDecimal(cryptoSoldQuantityStr);

            if (exchangeQuantity.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel);
                return;
            }

            Ticker cryptoSold =
                cryptos.stream()
                    .filter(c -> c.getSymbol().equals(cryptoSoldSymbol))
                    .findFirst()
                    .orElseThrow(
                        ()
                            -> new EntityNotFoundException(
                                "Crypto not found with symbol: " + cryptoSoldSymbol));

            BigDecimal cryptoSoldAfterBalance =
                cryptoSold.getCurrentQuantity().subtract(exchangeQuantity);

            if (cryptoSoldAfterBalance.compareTo(BigDecimal.ZERO) < 0)
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

            cryptoSoldAfterBalanceValueLabel.setText(cryptoSoldAfterBalance.toString());
        }
        catch (NumberFormatException e)
        {
            UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel);
        }
    }

    private void updateToCryptoQuantityAfterExchange()
    {
        String cryptoReceivedQuantityStr = cryptoReceivedQuantityField.getText();
        String cryptoReceivedSymbol      = cryptoReceivedComboBox.getValue();

        if (cryptoReceivedQuantityStr == null ||
            cryptoReceivedQuantityStr.strip().isEmpty() || cryptoReceivedSymbol == null)
        {
            UIUtils.resetLabel(cryptoReceivedAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal exchangeQuantity = new BigDecimal(cryptoReceivedQuantityStr);

            if (exchangeQuantity.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(cryptoReceivedAfterBalanceValueLabel);
                return;
            }

            Ticker cryptoReceived =
                cryptos.stream()
                    .filter(c -> c.getSymbol().equals(cryptoReceivedSymbol))
                    .findFirst()
                    .orElseThrow(()
                                     -> new EntityNotFoundException(
                                         "Crypto not found with symbol: " +
                                         cryptoReceivedSymbol));

            BigDecimal cryptoReceivedAfterBalance =
                cryptoReceived.getCurrentQuantity().add(exchangeQuantity);

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
