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
import org.moinex.entities.WalletTransaction;
import org.moinex.services.CreditCardService;
import org.moinex.services.RecurringTransactionService;
import org.moinex.services.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.TransactionStatus;
import org.moinex.util.TransactionType;
import org.moinex.util.UIUtils;
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
        UpdateResumePane(now.getMonthValue(), now.getYear());
    }

    /**
     * Update the display of the resume
     */
    public void UpdateResumePane(Integer year)
    {
        List<WalletTransaction> allYearTransactions =
            walletTransactionService.GetNonArchivedTransactionsByYear(year);

        List<WalletTransaction> futureTransactions =
            recurringTransactionService.GetFutureTransactionsByYear(Year.of(year),
                                                                    Year.of(year));

        allYearTransactions.addAll(futureTransactions);

        BigDecimal crcTotalDebtAmount = creditCardService.GetTotalDebtAmount(year);

        BigDecimal crcPendingPayments =
            creditCardService.GetPendingPaymentsByYear(year);

        BigDecimal crcPaidPayments = creditCardService.GetPaidPaymentsByYear(year);

        UpdateResumePane(allYearTransactions,
                         crcTotalDebtAmount,
                         crcPendingPayments,
                         crcPaidPayments);
    }

    /**
     * Update the display of the month resume
     */
    public void UpdateResumePane(Integer month, Integer year)
    {
        // Get all transactions of the month, including future transactions
        List<WalletTransaction> transactions =
            walletTransactionService.GetNonArchivedTransactionsByMonth(month, year);

        List<WalletTransaction> futureTransactions =
            recurringTransactionService.GetFutureTransactionsByMonth(
                YearMonth.of(year, month),
                YearMonth.of(year, month));

        transactions.addAll(futureTransactions);

        BigDecimal crcTotalDebtAmount =
            creditCardService.GetTotalDebtAmount(month, year);

        BigDecimal crcPendingPayments =
            creditCardService.GetPendingPaymentsByMonth(month, year);

        BigDecimal crcPaidPayments =
            creditCardService.GetPaidPaymentsByMonth(month, year);

        UpdateResumePane(transactions,
                         crcTotalDebtAmount,
                         crcPendingPayments,
                         crcPaidPayments);
    }

    private void UpdateResumePane(List<WalletTransaction> transactions,
                                  BigDecimal              crcTotalDebtAmount,
                                  BigDecimal              crcTotalPendingPayments,
                                  BigDecimal              crcTotalPaidPayments)
    {
        BigDecimal totalConfirmedIncome =
            transactions.stream()
                .filter(t -> t.GetType().equals(TransactionType.INCOME))
                .filter(t -> t.GetStatus().equals(TransactionStatus.CONFIRMED))
                .map(WalletTransaction::GetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalConfirmedExpenses =
            transactions.stream()
                .filter(t -> t.GetType().equals(TransactionType.EXPENSE))
                .filter(t -> t.GetStatus().equals(TransactionStatus.CONFIRMED))
                .map(WalletTransaction::GetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Consider the paid payments of the credit card as total expenses
        totalConfirmedExpenses = totalConfirmedExpenses.add(crcTotalPaidPayments);

        BigDecimal totalForeseenIncome =
            transactions.stream()
                .filter(t -> t.GetType() == TransactionType.INCOME)
                .map(WalletTransaction::GetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalForeseenExpenses =
            transactions.stream()
                .filter(t -> t.GetType() == TransactionType.EXPENSE)
                .map(WalletTransaction::GetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Consider the payments of the credit card as total of foreseen expenses
        totalForeseenExpenses = totalForeseenExpenses.add(crcTotalPendingPayments)
                                    .add(crcTotalPaidPayments);

        BigDecimal balance = totalConfirmedIncome.subtract(totalConfirmedExpenses);

        incomesCurrentValue.setText(UIUtils.FormatCurrency(totalConfirmedIncome));
        incomesCurrentSign.setText(" "); // default
        incomesCurrentValue.getStyleClass().clear();
        incomesCurrentValue.getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);

        incomesForeseenValue.setText(UIUtils.FormatCurrency(totalForeseenIncome));
        incomesForeseenSign.setText(" "); // default

        // Total Expenses
        expensesCurrentValue.setText(UIUtils.FormatCurrency(totalConfirmedExpenses));
        expensesCurrentSign.setText(" "); // default
        expensesCurrentValue.getStyleClass().clear();
        expensesCurrentValue.getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);

        expensesForeseenValue.setText(UIUtils.FormatCurrency(totalForeseenExpenses));
        expensesForeseenSign.setText(" "); // default

        // Balance
        balanceCurrentValue.setText(UIUtils.FormatCurrency(balance));

        // Set the balance label and sign label according to the balance value
        if (balance.compareTo(BigDecimal.ZERO) > 0)
        {
            balanceCurrentValue.setText(UIUtils.FormatCurrency(balance));
            balanceCurrentSign.setText("+");

            balanceCurrentValue.getStyleClass().clear();
            balanceCurrentValue.getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);

            balanceCurrentSign.getStyleClass().clear();
            balanceCurrentSign.getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);
        }
        else if (balance.compareTo(BigDecimal.ZERO) < 0)
        {
            balanceCurrentValue.setText(UIUtils.FormatCurrency(balance.abs()));
            balanceCurrentSign.setText("-");

            balanceCurrentValue.getStyleClass().clear();
            balanceCurrentValue.getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);

            balanceCurrentSign.getStyleClass().clear();
            balanceCurrentSign.getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            balanceCurrentValue.setText(UIUtils.FormatCurrency(0.0));
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
            balanceForeseenValue.setText(UIUtils.FormatCurrency(foreseenBalance));
            balanceForeseenSign.setText("+");
        }
        else if (foreseenBalance.compareTo(BigDecimal.ZERO) < 0)
        {
            balanceForeseenValue.setText(UIUtils.FormatCurrency(foreseenBalance.abs()));
            balanceForeseenSign.setText("-");
        }
        else
        {
            balanceForeseenValue.setText(UIUtils.FormatCurrency(0.0));
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
            savingsCurrentValue.setText(UIUtils.FormatPercentage(savingsPercentage));
            savingsCurrentSign.setText("+");

            savingsCurrentValue.getStyleClass().clear();
            savingsCurrentValue.getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);

            savingsCurrentSign.getStyleClass().clear();
            savingsCurrentSign.getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);
        }
        else if (savingsPercentage < 0)
        {
            savingsLabel.setText("No savings");
            savingsCurrentValue.setText(UIUtils.FormatPercentage(-savingsPercentage));
            savingsCurrentSign.setText("-");

            savingsCurrentValue.getStyleClass().clear();
            savingsCurrentValue.getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);

            savingsCurrentSign.getStyleClass().clear();
            savingsCurrentSign.getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            savingsLabel.setText("No savings");
            savingsCurrentValue.setText(UIUtils.FormatPercentage(0.0));
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
                UIUtils.FormatPercentage(foreseenSavingsPercentage));
            savingsForeseenSign.setText("+");
        }
        else if (foreseenSavingsPercentage < 0)
        {
            savingsForeseenValue.setText(
                UIUtils.FormatPercentage(-foreseenSavingsPercentage));
            savingsForeseenSign.setText("-");
        }
        else
        {
            savingsForeseenValue.setText(UIUtils.FormatPercentage(0.0));
            savingsForeseenSign.setText(" ");
        }

        // Credit Card
        creditCardsCurrentValue.setText(UIUtils.FormatCurrency(crcTotalDebtAmount));
        creditCardsCurrentSign.setText(" "); // default

        creditCardsForeseenValue.setText(
            String.format(UIUtils.FormatCurrency(crcTotalPendingPayments)));
        creditCardsForeseenSign.setText(" "); // default
    }
}
