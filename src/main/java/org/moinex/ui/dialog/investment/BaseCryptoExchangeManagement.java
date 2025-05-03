/*
 * Filename: BaseCryptoExchangeManagement.java
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import java.math.BigDecimal;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.investment.Ticker;
import org.moinex.service.CalculatorService;
import org.moinex.service.TickerService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TickerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Base class to implement the common behavior of the Add and Edit Crypto Exchange
 */
@NoArgsConstructor
public abstract class BaseCryptoExchangeManagement {
    @FXML protected Label cryptoSoldAfterBalanceValueLabel;

    @FXML protected Label cryptoReceivedAfterBalanceValueLabel;

    @FXML protected Label cryptoSoldCurrentBalanceValueLabel;

    @FXML protected Label cryptoReceivedCurrentBalanceValueLabel;

    @FXML protected ComboBox<Ticker> cryptoSoldComboBox;

    @FXML protected ComboBox<Ticker> cryptoReceivedComboBox;

    @FXML protected TextField descriptionField;

    @FXML protected TextField cryptoSoldQuantityField;

    @FXML protected TextField cryptoReceivedQuantityField;

    @FXML protected DatePicker exchangeDatePicker;

    protected ConfigurableApplicationContext springContext;

    protected TickerService tickerService;

    protected CalculatorService calculatorService;

    protected List<Ticker> cryptos;

    protected Ticker fromCrypto = null;

    /**
     * Constructor
     * @param tickerService TickerService
     * @param calculatorService CalculatorService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    protected BaseCryptoExchangeManagement(
            TickerService tickerService, CalculatorService calculatorService) {
        this.tickerService = tickerService;
        this.calculatorService = calculatorService;
    }

    public void setFromCryptoComboBox(Ticker tk) {
        if (cryptos.stream().noneMatch(c -> c.getId().equals(tk.getId()))) {
            return;
        }

        this.fromCrypto = tk;
        cryptoSoldComboBox.setValue(fromCrypto);
        updateFromCryptoCurrentQuantity();
    }

    @FXML
    protected void initialize() {
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

        cryptoSoldComboBox.setOnAction(
                e -> {
                    updateFromCryptoCurrentQuantity();
                    updateFromCryptoQuantityAfterExchange();
                });

        cryptoReceivedComboBox.setOnAction(
                e -> {
                    updateToCryptoCurrentQuantity();
                    updateToCryptoQuantityAfterExchange();
                });

        configureListeners();
    }

    @FXML
    protected abstract void handleSave();

    @FXML
    protected void handleCancel() {
        Stage stage = (Stage) cryptoReceivedQuantityField.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected void handleCryptoSoldOpenCalculator() {
        WindowUtils.openPopupWindow(
                Constants.CALCULATOR_FXML,
                "Calculator",
                springContext,
                (CalculatorController controller) -> {},
                List.of(
                        () ->
                                calculatorService.updateComponentWithResult(
                                        cryptoSoldQuantityField)));
    }

    @FXML
    protected void handleCryptoReceivedOpenCalculator() {
        WindowUtils.openPopupWindow(
                Constants.CALCULATOR_FXML,
                "Calculator",
                springContext,
                (CalculatorController controller) -> {},
                List.of(
                        () ->
                                calculatorService.updateComponentWithResult(
                                        cryptoReceivedQuantityField)));
    }

    protected void updateFromCryptoCurrentQuantity() {
        Ticker cryptoSold = cryptoSoldComboBox.getValue();

        if (cryptoSold == null) {
            return;
        }

        if (cryptoSold.getCurrentQuantity().compareTo(BigDecimal.ZERO) < 0) {
            // Must be unreachable
            UIUtils.setLabelStyle(
                    cryptoSoldCurrentBalanceValueLabel, Constants.NEGATIVE_BALANCE_STYLE);
        } else {
            UIUtils.setLabelStyle(
                    cryptoSoldCurrentBalanceValueLabel, Constants.NEUTRAL_BALANCE_STYLE);
        }

        cryptoSoldCurrentBalanceValueLabel.setText(cryptoSold.getCurrentQuantity().toString());
    }

    protected void updateToCryptoCurrentQuantity() {
        Ticker cryptoReceived = cryptoReceivedComboBox.getValue();

        if (cryptoReceived == null) {
            return;
        }

        if (cryptoReceived.getCurrentQuantity().compareTo(BigDecimal.ZERO) < 0) {
            // Must be unreachable
            UIUtils.setLabelStyle(
                    cryptoReceivedCurrentBalanceValueLabel, Constants.NEGATIVE_BALANCE_STYLE);
        } else {
            UIUtils.setLabelStyle(
                    cryptoReceivedCurrentBalanceValueLabel, Constants.NEUTRAL_BALANCE_STYLE);
        }

        cryptoReceivedCurrentBalanceValueLabel.setText(
                cryptoReceived.getCurrentQuantity().toString());
    }

    protected void updateFromCryptoQuantityAfterExchange() {
        String cryptoSoldQuantityStr = cryptoSoldQuantityField.getText();
        Ticker cryptoSold = cryptoSoldComboBox.getValue();

        if (cryptoSoldQuantityStr == null
                || cryptoSoldQuantityStr.isBlank()
                || cryptoSold == null) {
            UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel);
            return;
        }

        try {
            BigDecimal exchangeQuantity = new BigDecimal(cryptoSoldQuantityStr);

            if (exchangeQuantity.compareTo(BigDecimal.ZERO) < 0) {
                UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel);
                return;
            }

            BigDecimal cryptoSoldAfterBalance =
                    cryptoSold.getCurrentQuantity().subtract(exchangeQuantity);

            if (cryptoSoldAfterBalance.compareTo(BigDecimal.ZERO) < 0) {
                // Remove old style and add negative style
                UIUtils.setLabelStyle(
                        cryptoSoldAfterBalanceValueLabel, Constants.NEGATIVE_BALANCE_STYLE);
            } else {
                // Remove old style and add neutral style
                UIUtils.setLabelStyle(
                        cryptoSoldAfterBalanceValueLabel, Constants.NEUTRAL_BALANCE_STYLE);
            }

            cryptoSoldAfterBalanceValueLabel.setText(cryptoSoldAfterBalance.toString());
        } catch (NumberFormatException e) {
            UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel);
        }
    }

    protected void updateToCryptoQuantityAfterExchange() {
        String cryptoReceivedQuantityStr = cryptoReceivedQuantityField.getText();
        Ticker cryptoReceived = cryptoReceivedComboBox.getValue();

        if (cryptoReceivedQuantityStr == null
                || cryptoReceivedQuantityStr.isBlank()
                || cryptoReceived == null) {
            UIUtils.resetLabel(cryptoReceivedAfterBalanceValueLabel);
            return;
        }

        try {
            BigDecimal exchangeQuantity = new BigDecimal(cryptoReceivedQuantityStr);

            if (exchangeQuantity.compareTo(BigDecimal.ZERO) < 0) {
                UIUtils.resetLabel(cryptoReceivedAfterBalanceValueLabel);
                return;
            }

            BigDecimal cryptoReceivedAfterBalance =
                    cryptoReceived.getCurrentQuantity().add(exchangeQuantity);

            if (cryptoReceivedAfterBalance.compareTo(BigDecimal.ZERO) < 0) {
                // Remove old style and add negative style
                UIUtils.setLabelStyle(
                        cryptoReceivedAfterBalanceValueLabel, Constants.NEGATIVE_BALANCE_STYLE);
            } else {
                // Remove old style and add neutral style
                UIUtils.setLabelStyle(
                        cryptoReceivedAfterBalanceValueLabel, Constants.NEUTRAL_BALANCE_STYLE);
            }

            cryptoReceivedAfterBalanceValueLabel.setText(cryptoReceivedAfterBalance.toString());
        } catch (NumberFormatException e) {
            UIUtils.resetLabel(cryptoReceivedAfterBalanceValueLabel);
        }
    }

    protected void loadCryptosFromDatabase() {
        cryptos = tickerService.getAllNonArchivedTickersByType(TickerType.CRYPTOCURRENCY);
    }

    protected void populateCryptoComboBoxes() {
        cryptoSoldComboBox.getItems().setAll(cryptos);
        cryptoReceivedComboBox.getItems().setAll(cryptos);
    }

    protected void configureComboBoxes() {
        UIUtils.configureComboBox(cryptoSoldComboBox, Ticker::getName);
        UIUtils.configureComboBox(cryptoReceivedComboBox, Ticker::getName);
    }

    protected void configureListeners() {
        // Ensure that the user can only input numbers in the quantity field
        cryptoSoldQuantityField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX)) {
                                cryptoSoldQuantityField.setText(oldValue);
                            } else {
                                updateFromCryptoQuantityAfterExchange();
                                updateToCryptoQuantityAfterExchange();
                            }
                        });

        cryptoReceivedQuantityField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX)) {
                                cryptoReceivedQuantityField.setText(oldValue);
                            } else {
                                updateFromCryptoQuantityAfterExchange();
                                updateToCryptoQuantityAfterExchange();
                            }
                        });
    }
}
