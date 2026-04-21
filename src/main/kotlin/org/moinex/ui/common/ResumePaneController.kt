/*
 * Filename: ResumePaneController.kt (original filename: ResumePaneController.java)
 * Created on: October 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 19/03/2026
 */

package org.moinex.ui.common

import javafx.fxml.FXML
import javafx.scene.control.Label
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isConfirmed
import org.moinex.common.extension.isExpense
import org.moinex.common.extension.isIncome
import org.moinex.common.util.UIUtils
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.creditcard.RecurringCreditCardDebtService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth

@Controller
@Scope("prototype")
class ResumePaneController(
    private val walletService: WalletService,
    private val recurringTransactionService: RecurringTransactionService,
    private val creditCardService: CreditCardService,
    private val recurringCreditCardDebtService: RecurringCreditCardDebtService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var incomesCurrentSign: Label

    @FXML
    private lateinit var incomesCurrentValue: Label

    @FXML
    private lateinit var incomesForeseenSign: Label

    @FXML
    private lateinit var incomesForeseenValue: Label

    @FXML
    private lateinit var expensesCurrentSign: Label

    @FXML
    private lateinit var expensesCurrentValue: Label

    @FXML
    private lateinit var expensesForeseenSign: Label

    @FXML
    private lateinit var expensesForeseenValue: Label

    @FXML
    private lateinit var balanceCurrentSign: Label

    @FXML
    private lateinit var balanceCurrentValue: Label

    @FXML
    private lateinit var balanceForeseenSign: Label

    @FXML
    private lateinit var balanceForeseenValue: Label

    @FXML
    private lateinit var savingsCurrentSign: Label

    @FXML
    private lateinit var savingsCurrentValue: Label

    @FXML
    private lateinit var savingsForeseenSign: Label

    @FXML
    private lateinit var savingsForeseenValue: Label

    @FXML
    private lateinit var savingsLabel: Label

    @FXML
    private lateinit var creditCardsCurrentSign: Label

    @FXML
    private lateinit var creditCardsCurrentValue: Label

    @FXML
    private lateinit var creditCardsForeseenSign: Label

    @FXML
    private lateinit var creditCardsForeseenValue: Label

    companion object {
        private const val DEFAULT_SIGN = " "
        private const val POSITIVE_SIGN = "+"
        private const val NEGATIVE_SIGN = "-"
        private const val EMPTY_SIGN = ""
        private val PERCENTAGE_MULTIPLIER = BigDecimal(100)
    }

    @FXML
    fun initialize() {
        val now = LocalDateTime.now()
        updateResumePane(Year.from(now))
    }

    fun updateResumePane(year: Year) {
        val allYearTransactions =
            walletService.getAllNonArchivedWalletTransactionsByYear(year)

        val futureTransactions =
            recurringTransactionService.getFutureRecurringTransactionsByYear(
                year,
                year,
            )

        val allTransactions = allYearTransactions + futureTransactions

        val projectedCrcAmount = recurringCreditCardDebtService.getTotalProjectedAmountForYear(year)

        val crcTotalDebtAmount = creditCardService.getTotalDebtAmountByYear(year) + projectedCrcAmount
        val crcPendingPayments = creditCardService.getTotalPendingPaymentsByYear(year) + projectedCrcAmount
        val crcPaidPayments = creditCardService.getTotalPaidPaymentsByYear(year)

        updateResumePane(allTransactions, crcTotalDebtAmount, crcPendingPayments, crcPaidPayments)
    }

    fun updateResumePane(yearMonth: YearMonth) {
        val transactions =
            walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(yearMonth)

        val futureTransactions =
            recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(
                yearMonth,
                yearMonth,
            )

        val allTransactions = transactions + futureTransactions

        val projectedCrcAmount = recurringCreditCardDebtService.getTotalProjectedAmountForMonth(yearMonth)

        val crcTotalDebtAmount = creditCardService.getTotalDebtAmountByMonth(yearMonth) + projectedCrcAmount
        val crcPendingPayments = creditCardService.getTotalPendingPaymentsByMonth(yearMonth) + projectedCrcAmount
        val crcPaidPayments = creditCardService.getTotalEffectivePaidPaymentsByMonth(yearMonth)

        updateResumePane(allTransactions, crcTotalDebtAmount, crcPendingPayments, crcPaidPayments)
    }

    private fun updateResumePane(
        transactions: List<WalletTransaction>,
        crcTotalDebtAmount: BigDecimal,
        crcTotalPendingPayments: BigDecimal,
        crcTotalPaidPayments: BigDecimal,
    ) {
        val totalConfirmedIncome =
            transactions
                .filter { it.isIncome() }
                .filter { it.isConfirmed() }
                .sumOf { it.amount }

        val totalConfirmedExpenses =
            transactions
                .filter { it.isExpense() }
                .filter { it.isConfirmed() }
                .sumOf { it.amount } + crcTotalPaidPayments

        val totalForeseenIncome =
            transactions
                .filter { it.isIncome() }
                .sumOf { it.amount }

        val totalForeseenExpenses =
            transactions
                .filter { it.isExpense() }
                .sumOf { it.amount } + crcTotalPendingPayments + crcTotalPaidPayments

        val balance = totalConfirmedIncome - totalConfirmedExpenses
        val foreseenBalance = totalForeseenIncome - totalForeseenExpenses

        updateIncomeLabels(totalConfirmedIncome, totalForeseenIncome)
        updateExpenseLabels(totalConfirmedExpenses, totalForeseenExpenses)
        updateBalanceLabels(balance, foreseenBalance)
        updateSavingsLabels(totalConfirmedIncome, totalConfirmedExpenses, totalForeseenIncome, totalForeseenExpenses)
        updateCreditCardLabels(crcTotalDebtAmount, crcTotalPendingPayments)
    }

    private fun updateIncomeLabels(
        confirmed: BigDecimal,
        foreseen: BigDecimal,
    ) {
        incomesCurrentValue.apply {
            text = UIUtils.formatCurrency(confirmed)
            styleClass.apply {
                clear()
                add(Styles.POSITIVE_BALANCE_STYLE)
            }
        }
        incomesCurrentSign.text = DEFAULT_SIGN

        incomesForeseenValue.text = UIUtils.formatCurrency(foreseen)
        incomesForeseenSign.text = DEFAULT_SIGN
    }

    private fun updateExpenseLabels(
        confirmed: BigDecimal,
        foreseen: BigDecimal,
    ) {
        expensesCurrentValue.apply {
            text = UIUtils.formatCurrency(confirmed)
            styleClass.apply {
                clear()
                add(Styles.NEGATIVE_BALANCE_STYLE)
            }
        }
        expensesCurrentSign.text = DEFAULT_SIGN

        expensesForeseenValue.text = UIUtils.formatCurrency(foreseen)
        expensesForeseenSign.text = DEFAULT_SIGN
    }

    private fun updateBalanceLabels(
        current: BigDecimal,
        foreseen: BigDecimal,
    ) {
        when {
            current > BigDecimal.ZERO -> {
                balanceCurrentValue.text = UIUtils.formatCurrency(current)
                balanceCurrentSign.text = POSITIVE_SIGN
                applyStyleToLabels(
                    listOf(balanceCurrentValue, balanceCurrentSign),
                    Styles.POSITIVE_BALANCE_STYLE,
                )
            }
            current < BigDecimal.ZERO -> {
                balanceCurrentValue.text = UIUtils.formatCurrency(current.abs())
                balanceCurrentSign.text = NEGATIVE_SIGN
                applyStyleToLabels(
                    listOf(balanceCurrentValue, balanceCurrentSign),
                    Styles.NEGATIVE_BALANCE_STYLE,
                )
            }
            else -> {
                balanceCurrentValue.text = UIUtils.formatCurrency(BigDecimal.ZERO)
                balanceCurrentSign.text = EMPTY_SIGN
                applyStyleToLabels(
                    listOf(balanceCurrentValue, balanceCurrentSign),
                    Styles.NEUTRAL_BALANCE_STYLE,
                )
            }
        }

        when {
            foreseen > BigDecimal.ZERO -> {
                balanceForeseenValue.text = UIUtils.formatCurrency(foreseen)
                balanceForeseenSign.text = POSITIVE_SIGN
            }
            foreseen < BigDecimal.ZERO -> {
                balanceForeseenValue.text = UIUtils.formatCurrency(foreseen.abs())
                balanceForeseenSign.text = NEGATIVE_SIGN
            }
            else -> {
                balanceForeseenValue.text = UIUtils.formatCurrency(BigDecimal.ZERO)
                balanceForeseenSign.text = DEFAULT_SIGN
            }
        }
    }

    private fun updateSavingsLabels(
        confirmedIncome: BigDecimal,
        confirmedExpenses: BigDecimal,
        foreseenIncome: BigDecimal,
        foreseenExpenses: BigDecimal,
    ) {
        val savingsPercentage =
            if (confirmedIncome <= BigDecimal.ZERO) {
                BigDecimal.ZERO
            } else {
                (
                    confirmedIncome.minus(
                        confirmedExpenses,
                    )
                ).divide(confirmedIncome, 2, RoundingMode.HALF_UP).multiply(PERCENTAGE_MULTIPLIER)
            }

        UIUtils.removeTooltipFromNode(savingsCurrentValue)

        when {
            savingsPercentage > BigDecimal.ZERO -> {
                savingsLabel.text = preferencesService.translate(TranslationKeys.COMMON_RESUME_SAVINGS)
                savingsCurrentValue.text = UIUtils.formatPercentage(savingsPercentage)
                savingsCurrentSign.text = POSITIVE_SIGN
                applyStyleToLabels(
                    listOf(savingsCurrentValue, savingsCurrentSign),
                    Styles.POSITIVE_BALANCE_STYLE,
                )
            }
            savingsPercentage < BigDecimal.ZERO -> {
                savingsLabel.text = preferencesService.translate(TranslationKeys.COMMON_RESUME_NO_SAVINGS)
                savingsCurrentValue.text = UIUtils.formatPercentage(savingsPercentage)

                if (savingsPercentage < Constants.NEGATIVE_PERCENTAGE_THRESHOLD.toBigDecimal()) {
                    savingsCurrentSign.text = DEFAULT_SIGN
                    UIUtils.addTooltipToNode(
                        savingsCurrentValue,
                        "- ${UIUtils.formatPercentage(-savingsPercentage)}",
                    )
                } else {
                    savingsCurrentSign.text = NEGATIVE_SIGN
                }

                applyStyleToLabels(
                    listOf(savingsCurrentValue, savingsCurrentSign),
                    Styles.NEGATIVE_BALANCE_STYLE,
                )
            }
            else -> {
                savingsLabel.text = preferencesService.translate(TranslationKeys.COMMON_RESUME_NO_SAVINGS)
                savingsCurrentValue.text = UIUtils.formatPercentage(BigDecimal.ZERO)
                savingsCurrentSign.text = DEFAULT_SIGN
                applyStyleToLabels(
                    listOf(savingsCurrentValue, savingsCurrentSign),
                    Styles.NEUTRAL_BALANCE_STYLE,
                )
            }
        }

        val foreseenSavingsPercentage =
            if (foreseenIncome > BigDecimal.ZERO) {
                (
                    foreseenIncome.minus(
                        foreseenExpenses,
                    )
                ).divide(foreseenIncome, 2, RoundingMode.HALF_UP).multiply(PERCENTAGE_MULTIPLIER)
            } else {
                BigDecimal.ZERO
            }

        when {
            foreseenSavingsPercentage > BigDecimal.ZERO -> {
                savingsForeseenValue.text = UIUtils.formatPercentage(foreseenSavingsPercentage)
                savingsForeseenSign.text = POSITIVE_SIGN
            }
            foreseenSavingsPercentage < BigDecimal.ZERO -> {
                savingsForeseenValue.text = UIUtils.formatPercentage(-foreseenSavingsPercentage)
                savingsForeseenSign.text = NEGATIVE_SIGN
            }
            else -> {
                savingsForeseenValue.text = UIUtils.formatPercentage(BigDecimal.ZERO)
                savingsForeseenSign.text = DEFAULT_SIGN
            }
        }
    }

    private fun updateCreditCardLabels(
        totalDebt: BigDecimal,
        pendingPayments: BigDecimal,
    ) {
        creditCardsCurrentValue.text = UIUtils.formatCurrency(totalDebt)
        creditCardsCurrentSign.text = DEFAULT_SIGN

        creditCardsForeseenValue.text = UIUtils.formatCurrency(pendingPayments)
        creditCardsForeseenSign.text = DEFAULT_SIGN
    }

    private fun applyStyleToLabels(
        labels: List<Label>,
        styleClass: String,
    ) {
        labels.forEach { label ->
            label.styleClass.apply {
                clear()
                add(styleClass)
            }
        }
    }
}
