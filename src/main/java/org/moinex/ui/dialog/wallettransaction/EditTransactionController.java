/*
 * Filename: EditTransactionController.java
 * Created on: October 18, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.service.CalculatorService;
import org.moinex.service.CategoryService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Transaction dialog
 */
@Controller
@NoArgsConstructor
public final class EditTransactionController extends BaseWalletTransactionManagement
{
    @FXML
    private ComboBox<TransactionType> typeComboBox;

    private WalletTransaction walletTransaction = null;

    private static final Logger logger =
        LoggerFactory.getLogger(EditTransactionController.class);

    /**
     * Constructor
     * @param walletService WalletService
     * @param walletTransactionService WalletTransactionService
     * @param categoryService CategoryService
     * @param calculatorService CalculatorService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public EditTransactionController(WalletService            walletService,
                                     WalletTransactionService walletTransactionService,
                                     CategoryService          categoryService,
                                     CalculatorService        calculatorService)
    {
        super(walletService,
              walletTransactionService,
              categoryService,
              calculatorService);
    }

    public void setTransaction(WalletTransaction wt)
    {
        walletTransaction = wt;
        walletComboBox.setValue(walletTransaction.getWallet());
        statusComboBox.setValue(walletTransaction.getStatus());
        categoryComboBox.setValue(walletTransaction.getCategory());
        transactionValueField.setText(walletTransaction.getAmount().toString());

        // Deactivate the listener to avoid the event of changing the text of
        // the descriptionField from being triggered. After changing the text,
        // the listener is activated again
        suggestionsHandler.disable();
        descriptionField.setText(walletTransaction.getDescription());
        suggestionsHandler.enable();

        transactionDatePicker.setValue(walletTransaction.getDate().toLocalDate());
        typeComboBox.setValue(walletTransaction.getType());
        transactionType = walletTransaction.getType();

        UIUtils.updateWalletBalance(walletComboBox.getValue(),
                                    walletAfterBalanceValueLabel);
        walletAfterBalance();
        loadSuggestionsFromDatabase();
    }

    @Override
    @FXML
    protected void initialize()
    {
        super.initialize();

        typeComboBox.setOnAction(e -> {
            transactionType = typeComboBox.getValue();
            loadSuggestionsFromDatabase();
            walletAfterBalance();
        });

        typeComboBox.getItems().setAll(Arrays.asList(TransactionType.values()));
        UIUtils.configureComboBox(typeComboBox, TransactionType::name);
    }

    @FXML
    @Override
    protected void handleSave()
    {
        Wallet            wallet                 = walletComboBox.getValue();
        TransactionType   type                   = typeComboBox.getValue();
        String            description            = descriptionField.getText().trim();
        String            transactionValueString = transactionValueField.getText();
        TransactionStatus status                 = statusComboBox.getValue();
        Category          category               = categoryComboBox.getValue();
        LocalDate         transactionDate        = transactionDatePicker.getValue();

        if (wallet == null || type == null || transactionValueString == null || status == null || category == null || transactionDate == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");
            return;
        }

        try
        {
            BigDecimal transactionValue = new BigDecimal(transactionValueString);

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = transactionDate.atTime(currentTime);

            // Check if it has any modification
            if (wallet.getName().equals(walletTransaction.getWallet().getName()) &&
                category.getName().equals(walletTransaction.getCategory().getName()) &&
                transactionValue.compareTo(walletTransaction.getAmount()) == 0 &&
                description.equals(walletTransaction.getDescription()) &&
                status == walletTransaction.getStatus() &&
                type == walletTransaction.getType() &&
                dateTimeWithCurrentHour.toLocalDate().equals(
                    walletTransaction.getDate().toLocalDate()))
            {
                WindowUtils.showInformationDialog(
                    "No changes",
                    "No changes were made to the transaction.");
            }
            else // If there is any modification, update the transaction
            {
                walletTransaction.setWallet(wallet);
                walletTransaction.setCategory(category);
                walletTransaction.setDate(dateTimeWithCurrentHour);
                walletTransaction.setAmount(transactionValue);
                walletTransaction.setDescription(description);
                walletTransaction.setStatus(status);
                walletTransaction.setType(type);

                walletTransactionService.updateTransaction(walletTransaction);

                WindowUtils.showSuccessDialog("Transaction updated",
                                              "Transaction updated successfully.");
            }

            Stage stage = (Stage)descriptionField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid income value",
                                        "Income value must be a number.");
        }
        catch (EntityNotFoundException | IllegalArgumentException |
               IllegalStateException e)
        {
            WindowUtils.showErrorDialog("Error while updating transaction",
                                        e.getMessage());
        }
    }

    @Override
    protected void walletAfterBalance()
    {
        String          transactionValueString = transactionValueField.getText();
        TransactionType currentType            = typeComboBox.getValue();
        Wallet          wallet                 = walletComboBox.getValue();

        if (transactionValueString == null || transactionValueString.trim().isEmpty() ||
            wallet == null || currentType == null)
        {
            UIUtils.resetLabel(walletAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal newAmount = new BigDecimal(transactionValueString);

            if (newAmount.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(walletAfterBalanceValueLabel);
                return;
            }

            BigDecimal walletAfterBalanceValue = BigDecimal.ZERO;

            BigDecimal        oldAmount = walletTransaction.getAmount();
            BigDecimal        diff      = newAmount.subtract(oldAmount).abs();
            BigDecimal        balance   = wallet.getBalance();
            TransactionType   oldType   = walletTransaction.getType();
            TransactionStatus oldStatus = walletTransaction.getStatus();
            Wallet            oldWallet = walletTransaction.getWallet();

            if (wallet.equals(oldWallet))
            {
                if (oldStatus.equals(TransactionStatus.CONFIRMED))
                {
                    if (oldType.equals(TransactionType.EXPENSE))
                    {
                        if (currentType.equals(TransactionType.EXPENSE))
                        {
                            if (oldAmount.compareTo(newAmount) > 0)
                            {
                                walletAfterBalanceValue = balance.add(diff);
                            }
                            else
                            {
                                walletAfterBalanceValue = balance.subtract(diff);
                            }
                        }
                        else if (currentType.equals(TransactionType.INCOME))
                        {
                            walletAfterBalanceValue =
                                balance.add(oldAmount).add(newAmount);
                        }
                    }
                    else if (oldType.equals(TransactionType.INCOME))
                    {
                        if (currentType.equals(TransactionType.INCOME))
                        {
                            if (oldAmount.compareTo(newAmount) > 0)
                            {
                                walletAfterBalanceValue = balance.subtract(diff);
                            }
                            else
                            {
                                walletAfterBalanceValue = balance.add(diff);
                            }
                        }
                        else if (currentType.equals(TransactionType.EXPENSE))
                        {
                            walletAfterBalanceValue =
                                balance.subtract(oldAmount).subtract(newAmount);
                        }
                    }
                    else
                    {
                        // Type isn't mapped. Never should reach here
                        UIUtils.resetLabel(walletAfterBalanceValueLabel);
                        return;
                    }
                }
                else
                {
                    if (currentType.equals(TransactionType.EXPENSE))
                    {
                        walletAfterBalanceValue = balance.subtract(newAmount);
                    }
                    else if (currentType.equals(TransactionType.INCOME))
                    {
                        walletAfterBalanceValue = balance.add(newAmount);
                    }
                    else
                    {
                        // Type isn't mapped. Never should reach here
                        UIUtils.resetLabel(walletAfterBalanceValueLabel);
                        return;
                    }
                }
            }
            else // Wallet changed
            {
                if (currentType.equals(TransactionType.EXPENSE))
                {
                    walletAfterBalanceValue = balance.subtract(newAmount);
                }
                else if (currentType.equals(TransactionType.INCOME))
                {
                    walletAfterBalanceValue = balance.add(newAmount);
                }
                else
                {
                    // Type isn't mapped. Never should reach here
                    UIUtils.resetLabel(walletAfterBalanceValueLabel);
                    return;
                }
            }

            // Epsilon is used to avoid floating point arithmetic errors
            if (walletAfterBalanceValue.compareTo(BigDecimal.ZERO) < 0)
            {
                // Remove old style and add negative style
                UIUtils.setLabelStyle(walletAfterBalanceValueLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                UIUtils.setLabelStyle(walletAfterBalanceValueLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            walletAfterBalanceValueLabel.setText(
                UIUtils.formatCurrency(walletAfterBalanceValue));
        }
        catch (NumberFormatException e)
        {
            UIUtils.resetLabel(walletAfterBalanceValueLabel);
        }
    }

    @Override
    protected void loadSuggestionsFromDatabase()
    {
        if (typeComboBox.getValue() == TransactionType.EXPENSE)
        {
            suggestionsHandler.setSuggestions(
                walletTransactionService.getExpenseSuggestions());
        }
        else if (typeComboBox.getValue() == TransactionType.INCOME)
        {
            suggestionsHandler.setSuggestions(
                walletTransactionService.getIncomeSuggestions());
        }
        else if (typeComboBox.getValue() == null)
        {
            logger.warn("Type not selected. Suggestions will not be loaded.");
            suggestionsHandler.setSuggestions(null);
        }
        else
        {
            logger.warn("Type not mapped. Suggestions will not be loaded. Consider "
                        + "mapping the type");
            suggestionsHandler.setSuggestions(null);
        }
    }
}
