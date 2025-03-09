/*
 * Filename: ResumePaneController.java
 * Created on: October 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import lombok.NoArgsConstructor;
import org.moinex.entities.wallettransaction.WalletTransaction;
import org.moinex.services.CreditCardService;
import org.moinex.services.RecurringTransactionService;
import org.moinex.services.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * Controller for the resume pane
 *
 * @note prototype is necessary so that each scene has its own controller
 */
@Controller
@Scope("prototype") // Each instance of this controller is unique
@NoArgsConstructor
public class ResumePaneController
{
    @FXML
    private Label incomesCurrentSign;

    @FXML
    private Label incomesCurrentValue;

    @FXML
    private Label incomesForeseenSign;

    @FXML
    private Label incomesForeseenValue;

    @FXML
    private Label expensesCurrentSign;

    @FXML
    private Label expensesCurrentValue;

    @FXML
    private Label expensesForeseenSign;

    @FXML
    private Label expensesForeseenValue;

    @FXML
    private Label balanceCurrentSign;

    @FXML
    private Label balanceCurrentValue;

    @FXML
    private Label balanceForeseenSign;

    @FXML
    private Label balanceForeseenValue;

    @FXML
    private Label savingsCurrentSign;

    @FXML
    private Label savingsCurrentValue;

    @FXML
    private Label savingsForeseenSign;

    @FXML
    private Label savingsForeseenValue;

    @FXML
    private Label savingsLabel;

    @FXML
    private Label creditCardsCurrentSign;

    @FXML
    private Label creditCardsCurrentValue;

    @FXML
    private Label creditCardsForeseenSign;

    @FXML
    private Label creditCardsForeseenValue;

    private WalletTransactionService walletTransactionService;

    private RecurringTransactionService recurringTransactionService;

    private CreditCardService creditCardService;

    /**
     * Constructor
     * @param walletTransactionService WalletTransactionService
     * @param recurringTransactionService RecurringTransactionService
     * @param creditCardService CreditCardService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public ResumePaneController(WalletTransactionService    walletTransactionService,
                                RecurringTransactionService recurringTransactionService,
                                CreditCardService           creditCardService)
    {
        this.walletTransactionService    = walletTransactionService;
        this.recurringTransactionService = recurringTransactionService;
        this.creditCardService           = creditCardService;
    }

    @FXML
    public void initialize()
    {
        LocalDateTime now = LocalDateTime.now();
        updateResumePane(now.getMonthValue(), now.getYear());
    }

    /**
     * Update the display of the resume
     */
    public void updateResumePane(Integer year)
    {
        List<WalletTransaction> allYearTransactions =
            walletTransactionService.getNonArchivedTransactionsByYear(year);

        List<WalletTransaction> futureTransactions =
            recurringTransactionService.getFutureTransactionsByYear(Year.of(year),
                                                                    Year.of(year));

        allYearTransactions.addAll(futureTransactions);

        BigDecimal crcTotalDebtAmount = creditCardService.getTotalDebtAmount(year);

        BigDecimal crcPendingPayments =
            creditCardService.getPendingPaymentsByYear(year);

        BigDecimal crcPaidPayments = creditCardService.getPaidPaymentsByYear(year);

        updateResumePane(allYearTransactions,
                         crcTotalDebtAmount,
                         crcPendingPayments,
                         crcPaidPayments);
    }

    /**
     * Update the display of the month resume
     */
    public void updateResumePane(Integer month, Integer year)
    {
        // Get all transactions of the month, including future transactions
        List<WalletTransaction> transactions =
            walletTransactionService.getNonArchivedTransactionsByMonth(month, year);

        List<WalletTransaction> futureTransactions =
            recurringTransactionService.getFutureTransactionsByMonth(
                YearMonth.of(year, month),
                YearMonth.of(year, month));

        transactions.addAll(futureTransactions);

        BigDecimal crcTotalDebtAmount =
            creditCardService.getTotalDebtAmount(month, year);

        BigDecimal crcPendingPayments =
            creditCardService.getPendingPaymentsByMonth(month, year);

        BigDecimal crcPaidPayments =
            creditCardService.getEffectivePaidPaymentsByMonth(month, year);

        updateResumePane(transactions,
                         crcTotalDebtAmount,
                         crcPendingPayments,
                         crcPaidPayments);
    }

    private void updateResumePane(List<WalletTransaction> transactions,
                                  BigDecimal              crcTotalDebtAmount,
                                  BigDecimal              crcTotalPendingPayments,
                                  BigDecimal              crcTotalPaidPayments)
    {
        BigDecimal totalConfirmedIncome =
            transactions.stream()
                .filter(t -> t.getType().equals(TransactionType.INCOME))
                .filter(t -> t.getStatus().equals(TransactionStatus.CONFIRMED))
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalConfirmedExpenses =
            transactions.stream()
                .filter(t -> t.getType().equals(TransactionType.EXPENSE))
                .filter(t -> t.getStatus().equals(TransactionStatus.CONFIRMED))
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Consider the paid payments of the credit card as total expenses
        totalConfirmedExpenses = totalConfirmedExpenses.add(crcTotalPaidPayments);

        BigDecimal totalForeseenIncome =
            transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalForeseenExpenses =
            transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Consider the payments of the credit card as total of foreseen expenses
        totalForeseenExpenses = totalForeseenExpenses.add(crcTotalPendingPayments)
                                    .add(crcTotalPaidPayments);

        BigDecimal balance = totalConfirmedIncome.subtract(totalConfirmedExpenses);

        incomesCurrentValue.setText(UIUtils.formatCurrency(totalConfirmedIncome));
        incomesCurrentSign.setText(" "); // default
        incomesCurrentValue.getStyleClass().clear();
        incomesCurrentValue.getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);

        incomesForeseenValue.setText(UIUtils.formatCurrency(totalForeseenIncome));
        incomesForeseenSign.setText(" "); // default

        // Total Expenses
        expensesCurrentValue.setText(UIUtils.formatCurrency(totalConfirmedExpenses));
        expensesCurrentSign.setText(" "); // default
        expensesCurrentValue.getStyleClass().clear();
        expensesCurrentValue.getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);

        expensesForeseenValue.setText(UIUtils.formatCurrency(totalForeseenExpenses));
        expensesForeseenSign.setText(" "); // default

        // Balance
        balanceCurrentValue.setText(UIUtils.formatCurrency(balance));

        // Set the balance label and sign label according to the balance value
        if (balance.compareTo(BigDecimal.ZERO) > 0)
        {
            balanceCurrentValue.setText(UIUtils.formatCurrency(balance));
            balanceCurrentSign.setText("+");

            balanceCurrentValue.getStyleClass().clear();
            balanceCurrentValue.getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);

            balanceCurrentSign.getStyleClass().clear();
            balanceCurrentSign.getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);
        }
        else if (balance.compareTo(BigDecimal.ZERO) < 0)
        {
            balanceCurrentValue.setText(UIUtils.formatCurrency(balance.abs()));
            balanceCurrentSign.setText("-");

            balanceCurrentValue.getStyleClass().clear();
            balanceCurrentValue.getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);

            balanceCurrentSign.getStyleClass().clear();
            balanceCurrentSign.getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            balanceCurrentValue.setText(UIUtils.formatCurrency(0.0));
            balanceCurrentSign.setText("");

            balanceCurrentValue.getStyleClass().clear();
            balanceCurrentValue.getStyleClass().add(Constants.NEUTRAL_BALANCE_STYLE);

            balanceCurrentSign.getStyleClass().clear();
            balanceCurrentSign.getStyleClass().add(Constants.NEUTRAL_BALANCE_STYLE);
        }

        BigDecimal foreseenBalance =
            totalForeseenIncome.subtract(totalForeseenExpenses);

        if (foreseenBalance.compareTo(BigDecimal.ZERO) > 0)
        {
            balanceForeseenValue.setText(UIUtils.formatCurrency(foreseenBalance));
            balanceForeseenSign.setText("+");
        }
        else if (foreseenBalance.compareTo(BigDecimal.ZERO) < 0)
        {
            balanceForeseenValue.setText(UIUtils.formatCurrency(foreseenBalance.abs()));
            balanceForeseenSign.setText("-");
        }
        else
        {
            balanceForeseenValue.setText(UIUtils.formatCurrency(0.0));
            balanceForeseenSign.setText(" ");
        }

        // Mensal Economies
        Double savingsPercentage = 0.0;

        if (totalConfirmedIncome.compareTo(BigDecimal.ZERO) <= 0)
        {
            savingsPercentage = 0.0;
        }
        else
        {
            savingsPercentage =
                totalConfirmedIncome.subtract(totalConfirmedExpenses).doubleValue() /
                totalConfirmedIncome.doubleValue() * 100;
        }

        // Set the economy label and sign label according to the economy value
        if (savingsPercentage > 0)
        {
            savingsLabel.setText("Savings");
            savingsCurrentValue.setText(UIUtils.formatPercentage(savingsPercentage));
            savingsCurrentSign.setText("+");

            savingsCurrentValue.getStyleClass().clear();
            savingsCurrentValue.getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);

            savingsCurrentSign.getStyleClass().clear();
            savingsCurrentSign.getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);
        }
        else if (savingsPercentage < 0)
        {
            savingsLabel.setText("No savings");
            savingsCurrentValue.setText(UIUtils.formatPercentage(-savingsPercentage));
            savingsCurrentSign.setText("-");

            savingsCurrentValue.getStyleClass().clear();
            savingsCurrentValue.getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);

            savingsCurrentSign.getStyleClass().clear();
            savingsCurrentSign.getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            savingsLabel.setText("No savings");
            savingsCurrentValue.setText(UIUtils.formatPercentage(0.0));
            savingsCurrentSign.setText(" ");

            savingsCurrentValue.getStyleClass().clear();
            savingsCurrentValue.getStyleClass().add(Constants.NEUTRAL_BALANCE_STYLE);

            savingsCurrentSign.getStyleClass().clear();
            savingsCurrentSign.getStyleClass().add(Constants.NEUTRAL_BALANCE_STYLE);
        }

        Double foreseenSavingsPercentage = 0.0;

        if (totalForeseenIncome.compareTo(BigDecimal.ZERO) > 0)
        {
            foreseenSavingsPercentage =
                totalForeseenIncome.subtract(totalForeseenExpenses).doubleValue() /
                totalForeseenIncome.doubleValue() * 100;
        }

        if (foreseenSavingsPercentage > 0)
        {
            savingsForeseenValue.setText(
                UIUtils.formatPercentage(foreseenSavingsPercentage));
            savingsForeseenSign.setText("+");
        }
        else if (foreseenSavingsPercentage < 0)
        {
            savingsForeseenValue.setText(
                UIUtils.formatPercentage(-foreseenSavingsPercentage));
            savingsForeseenSign.setText("-");
        }
        else
        {
            savingsForeseenValue.setText(UIUtils.formatPercentage(0.0));
            savingsForeseenSign.setText(" ");
        }

        // Credit Card
        creditCardsCurrentValue.setText(UIUtils.formatCurrency(crcTotalDebtAmount));
        creditCardsCurrentSign.setText(" "); // default

        creditCardsForeseenValue.setText(
            String.format(UIUtils.formatCurrency(crcTotalPendingPayments)));
        creditCardsForeseenSign.setText(" "); // default
    }
}
