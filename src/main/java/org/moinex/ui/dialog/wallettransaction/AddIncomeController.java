/*
 * Filename: AddIncomeController.java
 * Created on: October  6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.CalculatorService;
import org.moinex.service.CategoryService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Income dialog
 */
@Controller
@NoArgsConstructor
public final class AddIncomeController extends BaseWalletTransactionManagement
{
    /**
     * Constructor
     * @param walletService WalletService
     * @param walletTransactionService WalletTransactionService
     * @param categoryService CategoryService
     * @param calculatorService CalculatorService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddIncomeController(WalletService            walletService,
                               WalletTransactionService walletTransactionService,
                               CategoryService          categoryService,
                               CalculatorService        calculatorService)
    {
        super(walletService,
              walletTransactionService,
              categoryService,
              calculatorService);

        transactionType = TransactionType.INCOME;
    }

    @FXML
    @Override
    protected void handleSave()
    {
        Wallet            wallet            = walletComboBox.getValue();
        String            description       = descriptionField.getText();
        String            incomeValueString = transactionValueField.getText();
        TransactionStatus status            = statusComboBox.getValue();
        Category          category          = categoryComboBox.getValue();
        LocalDate         incomeDate        = transactionDatePicker.getValue();

        if (wallet == null || description == null || description.isBlank() ||
            incomeValueString == null || incomeValueString.isBlank() ||
            status == null || category == null || incomeDate == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");
            return;
        }

        try
        {
            BigDecimal incomeValue = new BigDecimal(incomeValueString);

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = incomeDate.atTime(currentTime);

            walletTransactionService.addIncome(wallet.getId(),
                                               category,
                                               dateTimeWithCurrentHour,
                                               incomeValue,
                                               description,
                                               status);

            WindowUtils.showSuccessDialog("Income created",
                                          "The income was successfully created.");

            Stage stage = (Stage)descriptionField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid income value",
                                        "Income value must be a number.");
        }
        catch (EntityNotFoundException | IllegalArgumentException e)
        {
            WindowUtils.showErrorDialog("Error while creating income", e.getMessage());
        }
    }

    @Override
    protected void loadSuggestionsFromDatabase()
    {
        suggestionsHandler.setSuggestions(
            walletTransactionService.getIncomeSuggestions());
    }
}
