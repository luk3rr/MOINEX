/*
 * Filename: EditDividendController.java
 * Created on: January 11, 2025
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
import org.moinex.model.Category;
import org.moinex.model.investment.Dividend;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.CalculatorService;
import org.moinex.service.CategoryService;
import org.moinex.service.TickerService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Dividend dialog
 */
@Controller
@NoArgsConstructor
public final class EditDividendController extends BaseDividendManagement {
    private Dividend dividend = null;

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
    public EditDividendController(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            CategoryService categoryService,
            CalculatorService calculatorService,
            TickerService tickerService) {
        super(
                walletService,
                walletTransactionService,
                categoryService,
                calculatorService,
                tickerService);
    }

    public void setDividend(Dividend d) {
        this.dividend = d;
        tickerNameLabel.setText(
                dividend.getTicker().getName() + " (" + dividend.getTicker().getSymbol() + ")");

        setWalletComboBox(dividend.getWalletTransaction().getWallet());

        descriptionField.setText(dividend.getWalletTransaction().getDescription());
        dividendValueField.setText(dividend.getWalletTransaction().getAmount().toString());
        statusComboBox.setValue(dividend.getWalletTransaction().getStatus());
        categoryComboBox.setValue(dividend.getWalletTransaction().getCategory());
        dividendDatePicker.setValue(dividend.getWalletTransaction().getDate().toLocalDate());
    }

    @Override
    @FXML
    protected void handleSave() {
        Wallet wallet = walletComboBox.getValue();
        String description = descriptionField.getText();
        String dividendValueString = dividendValueField.getText();
        TransactionStatus status = statusComboBox.getValue();
        Category category = categoryComboBox.getValue();
        LocalDate dividendDate = dividendDatePicker.getValue();

        if (wallet == null
                || description == null
                || description.isBlank()
                || dividendValueString == null
                || dividendValueString.isBlank()
                || status == null
                || category == null
                || dividendDate == null) {
            WindowUtils.showInformationDialog(
                    "Empty fields", "Please fill all required fields before saving");
            return;
        }

        try {
            BigDecimal dividendValue = new BigDecimal(dividendValueString);

            // Check if it has any modification
            if (dividend.getWalletTransaction().getAmount().compareTo(dividendValue) == 0
                    && dividend.getWalletTransaction()
                            .getCategory()
                            .getId()
                            .equals(category.getId())
                    && dividend.getWalletTransaction().getStatus().equals(status)
                    && dividend.getWalletTransaction().getDate().toLocalDate().equals(dividendDate)
                    && dividend.getWalletTransaction().getDescription().equals(description)
                    && dividend.getWalletTransaction().getWallet().getId().equals(wallet.getId())) {
                WindowUtils.showInformationDialog(
                        "No changes", "No changes were made to the dividend");
            } else // If there is any modification, update the transaction
            {
                LocalTime currentTime = LocalTime.now();
                LocalDateTime dateTimeWithCurrentHour = dividendDate.atTime(currentTime);

                dividend.getWalletTransaction().setAmount(dividendValue);
                dividend.getWalletTransaction().setCategory(category);
                dividend.getWalletTransaction().setStatus(status);
                dividend.getWalletTransaction().setDate(dateTimeWithCurrentHour);
                dividend.getWalletTransaction().setDescription(description);
                dividend.getWalletTransaction().setWallet(wallet);

                tickerService.updateDividend(dividend);

                WindowUtils.showSuccessDialog("Dividend updated", "Dividend updated successfully");
            }

            Stage stage = (Stage) descriptionField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    "Invalid dividend value", "Dividend value must be a number.");
        } catch (EntityNotFoundException e) {
            WindowUtils.showErrorDialog("Error while updating dividend", e.getMessage());
        }
    }
}
