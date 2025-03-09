/*
 * Filename: AddDividendController.java
 * Created on: January  9, 2025
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
import org.moinex.entities.Category;
import org.moinex.entities.wallettransaction.Wallet;
import org.moinex.services.CalculatorService;
import org.moinex.services.CategoryService;
import org.moinex.services.TickerService;
import org.moinex.services.WalletService;
import org.moinex.services.WalletTransactionService;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Dividend dialog
 */
@Controller
@NoArgsConstructor
public final class AddDividendController extends BaseDividendManagement
{
    /**
     * Constructor
     * @param walletService WalletService
     * @param walletTransactionService WalletTransactionService
     * @param categoryService CategoryService
     * @param calculatorService CalculatorService
     * @param tickerService TickerService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddDividendController(WalletService            walletService,
                                 WalletTransactionService walletTransactionService,
                                 CategoryService          categoryService,
                                 CalculatorService        calculatorService,
                                 TickerService            tickerService)
    {
        super(walletService,
              walletTransactionService,
              categoryService,
              calculatorService,
              tickerService);
    }

    @FXML
    @Override
    protected void handleSave()
    {
        Wallet            wallet              = walletComboBox.getValue();
        String            description         = descriptionField.getText();
        String            dividendValueString = dividendValueField.getText();
        TransactionStatus status              = statusComboBox.getValue();
        Category          category            = categoryComboBox.getValue();
        LocalDate         dividendDate        = dividendDatePicker.getValue();

        if (wallet == null || description == null || description.strip().isEmpty() ||
            dividendValueString == null || status == null ||
            dividendValueString.strip().isEmpty() || category == null ||
            dividendDate == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");
            return;
        }

        try
        {
            BigDecimal dividendValue = new BigDecimal(dividendValueString);

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = dividendDate.atTime(currentTime);

            tickerService.addDividend(ticker.getId(),
                                      wallet.getId(),
                                      category,
                                      dividendValue,
                                      dateTimeWithCurrentHour,
                                      description,
                                      status);

            WindowUtils.showSuccessDialog("Dividend created",
                                          "The dividend was successfully created.");

            Stage stage = (Stage)descriptionField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid dividend value",
                                        "Dividend value must be a number.");
        }
        catch (EntityNotFoundException | IllegalArgumentException e)
        {
            WindowUtils.showErrorDialog(

                "Error while creating dividend",
                e.getMessage());
        }
    }
}
