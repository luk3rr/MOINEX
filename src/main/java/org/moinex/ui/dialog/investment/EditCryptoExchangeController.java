/*
 * Filename: EditCryptoExchangeController.java
 * Created on: January 28, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.investment.CryptoExchange;
import org.moinex.model.investment.Ticker;
import org.moinex.error.MoinexException;
import org.moinex.service.CalculatorService;
import org.moinex.service.TickerService;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the edit crypto exchange dialog
 */
@Controller
@NoArgsConstructor
public final class EditCryptoExchangeController extends BaseCryptoExchangeManagement
{
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
        super(tickerService, calculatorService);
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

    @Override
    @FXML
    protected void handleSave()
    {
        Ticker    soldCrypto                = cryptoSoldComboBox.getValue();
        Ticker    receivedCrypto            = cryptoReceivedComboBox.getValue();
        String    cryptoSoldQuantityStr     = cryptoSoldQuantityField.getText();
        String    cryptoReceivedQuantityStr = cryptoReceivedQuantityField.getText();
        String    description               = descriptionField.getText().trim();
        LocalDate exchangeDate              = exchangeDatePicker.getValue();

        if (soldCrypto == null || receivedCrypto == null || cryptoSoldQuantityStr == null || cryptoSoldQuantityStr.isBlank() || cryptoReceivedQuantityStr == null || cryptoReceivedQuantityStr.isBlank() || description.isBlank() || exchangeDate == null)
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

            // Check if it has any modification
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
        catch (EntityNotFoundException | MoinexException.SameSourceDestinationException |
               IllegalArgumentException e)
        {
            WindowUtils.showErrorDialog("Error while creating exchange",
                                        e.getMessage());
        }
    }
}
