/*
 * Filename: AddExpenseController.java
 * Created on: October  5, 2024
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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Expense dialog
 */
@Controller
@NoArgsConstructor
public final class AddExpenseController extends BaseWalletTransactionManagement {
    /**
     * Constructor
     * @param walletService WalletService
     * @param walletTransactionService WalletTransactionService
     * @param categoryService CategoryService
     * @param calculatorService CalculatorService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddExpenseController(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            CategoryService categoryService,
            CalculatorService calculatorService,
            ConfigurableApplicationContext springContext) {
        super(
                walletService,
                walletTransactionService,
                categoryService,
                calculatorService,
                springContext);

        transactionType = TransactionType.EXPENSE;
    }

    @FXML
    @Override
    protected void handleSave() {
        Wallet wallet = walletComboBox.getValue();
        String description = descriptionField.getText();
        String expenseValueString = transactionValueField.getText();
        TransactionStatus status = statusComboBox.getValue();
        Category category = categoryComboBox.getValue();
        LocalDate expenseDate = transactionDatePicker.getValue();

        if (wallet == null
                || description == null
                || description.isBlank()
                || expenseValueString == null
                || expenseValueString.isBlank()
                || status == null
                || category == null
                || expenseDate == null) {
            WindowUtils.showInformationDialog(
                    "Empty fields", "Please fill all required fields before saving");
            return;
        }

        try {
            BigDecimal expenseValue = new BigDecimal(expenseValueString);

            LocalTime currentTime = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = expenseDate.atTime(currentTime);

            walletTransactionService.addExpense(
                    wallet.getId(),
                    category,
                    dateTimeWithCurrentHour,
                    expenseValue,
                    description,
                    status);

            WindowUtils.showSuccessDialog("Expense created", "Expense created successfully");

            Stage stage = (Stage) descriptionField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog("Invalid expense value", "Expense value must be a number");
        } catch (EntityNotFoundException | IllegalArgumentException | IllegalStateException e) {
            WindowUtils.showErrorDialog("Error creating expense", e.getMessage());
        }
    }

    @Override
    protected void loadSuggestionsFromDatabase() {
        suggestionsHandler.setSuggestions(walletTransactionService.getExpenseSuggestions());
    }
}
