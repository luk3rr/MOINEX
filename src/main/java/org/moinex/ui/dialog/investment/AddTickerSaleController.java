/*
 * Filename: AddTickerSaleController.java
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
import org.moinex.exceptions.InsufficientResourcesException;
import org.moinex.services.CategoryService;
import org.moinex.services.TickerService;
import org.moinex.services.WalletService;
import org.moinex.services.WalletTransactionService;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Sale Ticker dialog
 */
@Controller
@NoArgsConstructor
public final class AddTickerSaleController extends BaseTickerTransactionManagement
{
    /**
     * Constructor
     * @param walletService Wallet service
     * @param walletTransactionService Wallet transaction service
     * @param categoryService Category service
     * @param tickerService Ticker service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddTickerSaleController(WalletService            walletService,
                                   WalletTransactionService walletTransactionService,
                                   CategoryService          categoryService,
                                   TickerService            tickerService)
    {
        super(walletService, walletTransactionService, categoryService, tickerService);

        transactionType = TransactionType.INCOME;
    }

    @FXML
    @Override
    protected void handleSave()
    {
        Wallet            wallet       = walletComboBox.getValue();
        String            description  = descriptionField.getText();
        TransactionStatus status       = statusComboBox.getValue();
        Category          category     = categoryComboBox.getValue();
        String            unitPriceStr = unitPriceField.getText();
        String            quantityStr  = quantityField.getText();
        LocalDate         buyDate      = transactionDatePicker.getValue();

        if (wallet == null || description == null || description.isBlank() ||
            status == null || category == null || unitPriceStr == null ||
            unitPriceStr.isBlank() || quantityStr == null ||
            quantityStr.isBlank() || buyDate == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");

            return;
        }

        try
        {
            BigDecimal unitPrice = new BigDecimal(unitPriceStr);

            BigDecimal quantity = new BigDecimal(quantityStr);

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = buyDate.atTime(currentTime);

            tickerService.addSale(ticker.getId(),
                                  wallet.getId(),
                                  quantity,
                                  unitPrice,
                                  category,
                                  dateTimeWithCurrentHour,
                                  description,
                                  status);

            WindowUtils.showSuccessDialog("Sale added", "Sale added successfully");

            Stage stage = (Stage)tickerNameLabel.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid number", "Invalid price or quantity");
        }
        catch (EntityNotFoundException | IllegalArgumentException |
               InsufficientResourcesException e)
        {
            WindowUtils.showErrorDialog("Error while selling ticker", e.getMessage());
        }
    }
}
