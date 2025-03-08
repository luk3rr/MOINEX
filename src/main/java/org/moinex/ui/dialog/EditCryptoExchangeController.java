/*
 * Filename: EditCryptoExchangeController.java
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
import org.moinex.entities.investment.CryptoExchange;
import org.moinex.entities.investment.Ticker;
import org.moinex.exceptions.SameSourceDestionationException;
import org.moinex.services.CalculatorService;
import org.moinex.services.TickerService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TickerType;
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
    private ComboBox<Ticker> cryptoSoldComboBox;

    @FXML
    private ComboBox<Ticker> cryptoReceivedComboBox;

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

    private CryptoExchange cryptoExchange = null;

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
        this.cryptoExchange = cryptoExchange;

        cryptoSoldComboBox.setValue(cryptoExchange.getSoldCrypto());
        cryptoReceivedComboBox.setValue(cryptoExchange.getReceivedCrypto());

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
        configureComboBoxes();

        loadCryptosFromDatabase();

        populateCryptoComboBoxes();

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
        Ticker    soldCrypto                = cryptoSoldComboBox.getValue();
        Ticker    receivedCrypto            = cryptoReceivedComboBox.getValue();
        String    cryptoSoldQuantityStr     = cryptoSoldQuantityField.getText();
        String    cryptoReceivedQuantityStr = cryptoReceivedQuantityField.getText();
        String    description               = descriptionField.getText().trim();
        LocalDate exchangeDate              = exchangeDatePicker.getValue();

        if (soldCrypto == null || receivedCrypto == null ||
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

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = exchangeDate.atTime(currentTime);

            // Check if has any modification
            if (cryptoExchange.getSoldCrypto().getSymbol().equals(
                    soldCrypto.getSymbol()) &&
                cryptoExchange.getReceivedCrypto().getSymbol().equals(
                    receivedCrypto.getSymbol()) &&
                cryptoExchange.getSoldQuantity().compareTo(cryptoSoldQuantity) == 0 &&
                cryptoExchange.getReceivedQuantity().compareTo(
                    cryptoReceivedQuantity) == 0 &&
                cryptoExchange.getDescription().equals(description) &&
                cryptoExchange.getDate().toLocalDate().equals(
                    dateTimeWithCurrentHour.toLocalDate()))
            {
                WindowUtils.showInformationDialog(
                    "No changes",
                    "No changes were made to the exchange");
            }
            else // If there is any modification, update the exchange
            {
                cryptoExchange.setSoldCrypto(soldCrypto);
                cryptoExchange.setReceivedCrypto(receivedCrypto);
                cryptoExchange.setSoldQuantity(cryptoSoldQuantity);
                cryptoExchange.setReceivedQuantity(cryptoReceivedQuantity);
                cryptoExchange.setDescription(description);
                cryptoExchange.setDate(dateTimeWithCurrentHour);

                tickerService.updateCryptoExchange(cryptoExchange);

                WindowUtils.showSuccessDialog("Exchange updated",
                                              "Exchange updated successfully");
            }

            Stage stage = (Stage)cryptoReceivedQuantityField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid exchange quantity",
                                        "The quantity must be a number");
        }
        catch (EntityNotFoundException | SameSourceDestionationException |
               IllegalArgumentException e)
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
        Ticker soldCrypto = cryptoSoldComboBox.getValue();

        if (soldCrypto == null)
        {
            return;
        }

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
        Ticker receivedCrypto = cryptoReceivedComboBox.getValue();

        if (receivedCrypto == null)
        {
            return;
        }

        if (receivedCrypto.getCurrentQuantity().compareTo(BigDecimal.ZERO) < 0)
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
            receivedCrypto.getCurrentQuantity().toString());
    }

    private void updateFromCryptoQuantityAfterExchange()
    {
        String cryptoSoldQuantityStr = cryptoSoldQuantityField.getText();
        Ticker soldCrypto            = cryptoSoldComboBox.getValue();

        if (cryptoSoldQuantityStr == null || cryptoSoldQuantityStr.strip().isEmpty() ||
            soldCrypto == null)
        {
            UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal oldSoldQuantity = cryptoExchange.getSoldQuantity();

            BigDecimal exchangeQuantity = new BigDecimal(cryptoSoldQuantityStr);

            if (exchangeQuantity.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel);
                return;
            }

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
        Ticker receivedCrypto            = cryptoReceivedComboBox.getValue();

        if (cryptoReceivedQuantityStr == null ||
            cryptoReceivedQuantityStr.strip().isEmpty() || receivedCrypto == null)
        {
            UIUtils.resetLabel(cryptoReceivedAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal oldReceivedQuantity = cryptoExchange.getReceivedQuantity();

            BigDecimal exchangeQuantity = new BigDecimal(cryptoReceivedQuantityStr);

            if (exchangeQuantity.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(cryptoReceivedAfterBalanceValueLabel);
                return;
            }

            BigDecimal cryptoReceivedAfterBalance = receivedCrypto.getCurrentQuantity()
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
    }

    private void populateCryptoComboBoxes()
    {
        cryptoSoldComboBox.getItems().addAll(cryptos);
        cryptoReceivedComboBox.getItems().addAll(cryptos);
    }

    private void configureComboBoxes()
    {
        UIUtils.configureComboBox(cryptoSoldComboBox, Ticker::getSymbol);
        UIUtils.configureComboBox(cryptoReceivedComboBox, Ticker::getSymbol);
    }
}
