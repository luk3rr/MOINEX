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
import org.moinex.model.enums.WalletTransactionStatus;
import org.moinex.model.enums.WalletTransactionType;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.service.PreferencesService;
import org.moinex.service.creditcard.CreditCardService;
import org.moinex.service.wallet.RecurringTransactionService;
import org.moinex.service.wallet.WalletService;
import org.moinex.util.Constants;
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
@NoArgsConstructor
public class ResumePaneController {
    @FXML private Label incomesCurrentSign;

    @FXML private Label incomesCurrentValue;

    @FXML private Label incomesForeseenSign;

    @FXML private Label incomesForeseenValue;

    @FXML private Label expensesCurrentSign;

    @FXML private Label expensesCurrentValue;

    @FXML private Label expensesForeseenSign;

    @FXML private Label expensesForeseenValue;

    @FXML private Label balanceCurrentSign;

    @FXML private Label balanceCurrentValue;

    @FXML private Label balanceForeseenSign;

    @FXML private Label balanceForeseenValue;

    @FXML private Label savingsCurrentSign;

    @FXML private Label savingsCurrentValue;

    @FXML private Label savingsForeseenSign;

    @FXML private Label savingsForeseenValue;

    @FXML private Label savingsLabel;

    @FXML private Label creditCardsCurrentSign;

    @FXML private Label creditCardsCurrentValue;

    @FXML private Label creditCardsForeseenSign;

    @FXML private Label creditCardsForeseenValue;

    private WalletService walletService;

    private RecurringTransactionService recurringTransactionService;

    private CreditCardService creditCardService;
    private PreferencesService preferencesService;

    /**
     * Constructor
     *
     * @param walletService WalletService
     * @param recurringTransactionService RecurringTransactionService
     * @param creditCardService CreditCardService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public ResumePaneController(
            WalletService walletService,
            RecurringTransactionService recurringTransactionService,
            CreditCardService creditCardService,
            PreferencesService preferencesService) {
        this.walletService = walletService;
        this.recurringTransactionService = recurringTransactionService;
        this.creditCardService = creditCardService;
        this.preferencesService = preferencesService;
    }

    @FXML
    public void initialize() {
        LocalDateTime now = LocalDateTime.now();
        updateResumePane(now.getMonthValue(), now.getYear());
    }

    /** Update the display of the resume */
    public void updateResumePane(Integer year) {
        List<WalletTransaction> allYearTransactions =
                walletService.getAllNonArchivedWalletTransactionsByYear(Year.of(year));

        List<WalletTransaction> futureTransactions =
                recurringTransactionService.getFutureRecurringTransactionsByYear(
                        Year.of(year), Year.of(year));

        allYearTransactions.addAll(futureTransactions);

        BigDecimal crcTotalDebtAmount = creditCardService.getTotalDebtAmountByYear(Year.of(year));

        BigDecimal crcPendingPayments =
                creditCardService.getTotalPendingPaymentsByYear(Year.of(year));

        BigDecimal crcPaidPayments = creditCardService.getTotalPaidPaymentsByYear(Year.of(year));

        updateResumePane(
                allYearTransactions, crcTotalDebtAmount, crcPendingPayments, crcPaidPayments);
    }

    /** Update the display of the month resume */
    public void updateResumePane(Integer month, Integer year) {
        // Get all transactions of the month that should be included in analysis
        List<WalletTransaction> transactions =
                walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(
                        YearMonth.of(year, month));

        List<WalletTransaction> futureTransactions =
                recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(
                        YearMonth.of(year, month), YearMonth.of(year, month));

        transactions.addAll(futureTransactions);

        BigDecimal crcTotalDebtAmount =
                creditCardService.getTotalDebtAmountByMonth(YearMonth.of(year, month));

        BigDecimal crcPendingPayments =
                creditCardService.getTotalPendingPaymentsByMonth(YearMonth.of(year, month));

        BigDecimal crcPaidPayments =
                creditCardService.getTotalEffectivePaidPaymentsByMonth(YearMonth.of(year, month));

        updateResumePane(transactions, crcTotalDebtAmount, crcPendingPayments, crcPaidPayments);
    }

    private void updateResumePane(
            List<WalletTransaction> transactions,
            BigDecimal crcTotalDebtAmount,
            BigDecimal crcTotalPendingPayments,
            BigDecimal crcTotalPaidPayments) {
        BigDecimal totalConfirmedIncome =
                transactions.stream()
                        .filter(t -> t.getType().equals(WalletTransactionType.INCOME))
                        .filter(t -> t.getStatus().equals(WalletTransactionStatus.CONFIRMED))
                        .map(WalletTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalConfirmedExpenses =
                transactions.stream()
                        .filter(t -> t.getType().equals(WalletTransactionType.EXPENSE))
                        .filter(t -> t.getStatus().equals(WalletTransactionStatus.CONFIRMED))
                        .map(WalletTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Consider the paid payments of the credit card as total expenses
        totalConfirmedExpenses = totalConfirmedExpenses.add(crcTotalPaidPayments);

        BigDecimal totalForeseenIncome =
                transactions.stream()
                        .filter(t -> t.getType() == WalletTransactionType.INCOME)
                        .map(WalletTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalForeseenExpenses =
                transactions.stream()
                        .filter(t -> t.getType() == WalletTransactionType.EXPENSE)
                        .map(WalletTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Consider the payments of the credit card as total of foreseen expenses
        totalForeseenExpenses =
                totalForeseenExpenses.add(crcTotalPendingPayments).add(crcTotalPaidPayments);

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
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            balanceCurrentValue.setText(UIUtils.formatCurrency(balance));
            balanceCurrentSign.setText("+");

            balanceCurrentValue.getStyleClass().clear();
            balanceCurrentValue.getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);

            balanceCurrentSign.getStyleClass().clear();
            balanceCurrentSign.getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);
        } else if (balance.compareTo(BigDecimal.ZERO) < 0) {
            balanceCurrentValue.setText(UIUtils.formatCurrency(balance.abs()));
            balanceCurrentSign.setText("-");

            balanceCurrentValue.getStyleClass().clear();
            balanceCurrentValue.getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);

            balanceCurrentSign.getStyleClass().clear();
            balanceCurrentSign.getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);
        } else {
            balanceCurrentValue.setText(UIUtils.formatCurrency(0.0));
            balanceCurrentSign.setText("");

            balanceCurrentValue.getStyleClass().clear();
            balanceCurrentValue.getStyleClass().add(Constants.NEUTRAL_BALANCE_STYLE);

            balanceCurrentSign.getStyleClass().clear();
            balanceCurrentSign.getStyleClass().add(Constants.NEUTRAL_BALANCE_STYLE);
        }

        BigDecimal foreseenBalance = totalForeseenIncome.subtract(totalForeseenExpenses);

        if (foreseenBalance.compareTo(BigDecimal.ZERO) > 0) {
            balanceForeseenValue.setText(UIUtils.formatCurrency(foreseenBalance));
            balanceForeseenSign.setText("+");
        } else if (foreseenBalance.compareTo(BigDecimal.ZERO) < 0) {
            balanceForeseenValue.setText(UIUtils.formatCurrency(foreseenBalance.abs()));
            balanceForeseenSign.setText("-");
        } else {
            balanceForeseenValue.setText(UIUtils.formatCurrency(0.0));
            balanceForeseenSign.setText(" ");
        }

        // Mensal Economies
        double savingsPercentage;

        if (totalConfirmedIncome.compareTo(BigDecimal.ZERO) <= 0) {
            savingsPercentage = 0.0;
        } else {
            savingsPercentage =
                    totalConfirmedIncome.subtract(totalConfirmedExpenses).doubleValue()
                            / totalConfirmedIncome.doubleValue()
                            * 100;
        }

        // Set the economy label and sign label according to the economy value
        UIUtils.removeTooltipFromNode(savingsCurrentValue);

        if (savingsPercentage > 0) {
            savingsLabel.setText(
                    preferencesService.translate(Constants.TranslationKeys.COMMON_RESUME_SAVINGS));
            savingsCurrentValue.setText(
                    UIUtils.formatPercentage(savingsPercentage, preferencesService));
            savingsCurrentSign.setText("+");

            savingsCurrentValue.getStyleClass().clear();
            savingsCurrentValue.getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);

            savingsCurrentSign.getStyleClass().clear();
            savingsCurrentSign.getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);
        } else if (savingsPercentage < 0) {
            savingsLabel.setText(
                    preferencesService.translate(
                            Constants.TranslationKeys.COMMON_RESUME_NO_SAVINGS));
            savingsCurrentValue.setText(
                    UIUtils.formatPercentage(savingsPercentage, preferencesService));

            if (savingsPercentage < Constants.NEGATIVE_PERCENTAGE_THRESHOLD) {
                savingsCurrentSign.setText(" ");
                UIUtils.addTooltipToNode(
                        savingsCurrentValue,
                        "- " + UIUtils.formatPercentage(-savingsPercentage, preferencesService));
            } else {
                savingsCurrentSign.setText("-");
            }

            savingsCurrentValue.getStyleClass().clear();
            savingsCurrentValue.getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);

            savingsCurrentSign.getStyleClass().clear();
            savingsCurrentSign.getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);
        } else {
            savingsLabel.setText(
                    preferencesService.translate(
                            Constants.TranslationKeys.COMMON_RESUME_NO_SAVINGS));
            savingsCurrentValue.setText(UIUtils.formatPercentage(0.0, preferencesService));
            savingsCurrentSign.setText(" ");

            savingsCurrentValue.getStyleClass().clear();
            savingsCurrentValue.getStyleClass().add(Constants.NEUTRAL_BALANCE_STYLE);

            savingsCurrentSign.getStyleClass().clear();
            savingsCurrentSign.getStyleClass().add(Constants.NEUTRAL_BALANCE_STYLE);
        }

        double foreseenSavingsPercentage = 0.0;

        if (totalForeseenIncome.compareTo(BigDecimal.ZERO) > 0) {
            foreseenSavingsPercentage =
                    totalForeseenIncome.subtract(totalForeseenExpenses).doubleValue()
                            / totalForeseenIncome.doubleValue()
                            * 100;
        }

        if (foreseenSavingsPercentage > 0) {
            savingsForeseenValue.setText(
                    UIUtils.formatPercentage(foreseenSavingsPercentage, preferencesService));
            savingsForeseenSign.setText("+");
        } else if (foreseenSavingsPercentage < 0) {
            savingsForeseenValue.setText(
                    UIUtils.formatPercentage(-foreseenSavingsPercentage, preferencesService));
            savingsForeseenSign.setText("-");
        } else {
            savingsForeseenValue.setText(UIUtils.formatPercentage(0.0, preferencesService));
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
