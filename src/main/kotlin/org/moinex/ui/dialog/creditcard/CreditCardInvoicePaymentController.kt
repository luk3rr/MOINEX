/*
 * Filename: CreditCardInvoicePaymentController.kt (original filename: CreditCardInvoicePaymentController.java)
 * Created on: October 30, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.creditcard

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Files
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isZero
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.dto.CreditCardInvoicePaymentDTO
import org.moinex.model.wallettransaction.Wallet
import org.moinex.service.CalculatorService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.creditcard.RecurringCreditCardDebtService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.common.CalculatorController
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.time.YearMonth

@Controller
class CreditCardInvoicePaymentController(
    private val walletService: WalletService,
    private val creditCardService: CreditCardService,
    private val recurringCreditCardDebtService: RecurringCreditCardDebtService,
    private val calculatorService: CalculatorService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var crcNameLabel: Label

    @FXML
    private lateinit var crcInvoiceDueLabel: Label

    @FXML
    private lateinit var crcInvoiceMonthLabel: Label

    @FXML
    private lateinit var crcAvailableRebateLabel: Label

    @FXML
    private lateinit var crcProjectedAmountLabel: Label

    @FXML
    private lateinit var alreadyPaidValueLabel: Label

    @FXML
    private lateinit var walletAfterBalanceLabel: Label

    @FXML
    private lateinit var walletCurrentBalanceLabel: Label

    @FXML
    private lateinit var totalToPayLabel: Label

    @FXML
    private lateinit var walletComboBox: ComboBox<Wallet>

    @FXML
    private lateinit var useRebateValueField: TextField

    @FXML
    private lateinit var partialAmountField: TextField

    private var wallets: List<Wallet> = emptyList()
    private var creditCard: CreditCard? = null
    private var invoiceDate: YearMonth? = null

    companion object {
        private const val AVAILABLE_REBATE_STYLE = "-fx-text-fill: green;"
        private const val UNAVAILABLE_REBATE_STYLE = "-fx-text-fill: black;"
    }

    fun setCreditCard(
        crc: CreditCard,
        invoiceDate: YearMonth,
    ) {
        this.creditCard = crc
        this.invoiceDate = invoiceDate
        configureComboBoxes()

        crcNameLabel.text = creditCard!!.name

        val invoiceAmount =
            creditCardService.getTotalPendingPaymentsByCreditCardAndMonth(
                creditCard!!.id!!,
                invoiceDate,
            )

        val alreadyPaid =
            creditCardService.getTotalAdvancePaidForInvoice(
                creditCard!!.id!!,
                invoiceDate,
            )

        crcInvoiceDueLabel.text = UIUtils.formatCurrency(invoiceAmount + alreadyPaid)
        totalToPayLabel.text = UIUtils.formatCurrency(invoiceAmount)
        crcInvoiceMonthLabel.text = UIUtils.formatShortMonthYear(invoiceDate)
        crcAvailableRebateLabel.text = UIUtils.formatCurrency(creditCard!!.availableRebate)

        if (alreadyPaid > BigDecimal.ZERO) {
            alreadyPaidValueLabel.text = UIUtils.formatCurrency(alreadyPaid)
        } else {
            alreadyPaidValueLabel.text = "-"
        }

        val projectedAmount =
            recurringCreditCardDebtService
                .getProjectedOccurrencesForMonth(invoiceDate)
                .filter { it.recurringDebt.creditCard.id!! == creditCard!!.id!! }
                .sumOf { it.amount }

        crcProjectedAmountLabel.text = UIUtils.formatCurrency(projectedAmount)

        if (creditCard!!.availableRebate > BigDecimal.ZERO) {
            crcAvailableRebateLabel.style = AVAILABLE_REBATE_STYLE
        } else {
            crcAvailableRebateLabel.style = UNAVAILABLE_REBATE_STYLE
        }

        creditCard!!.defaultBillingWallet?.let { wallet ->
            walletComboBox.value = wallet
            UIUtils.updateWalletBalanceLabelStyle(walletComboBox.value, walletCurrentBalanceLabel)
            walletAfterBalance()
        }
    }

    @FXML
    private fun initialize() {
        configureComboBoxes()
        loadWalletsFromDatabase()
        populateWalletComboBox()

        UIUtils.resetLabel(walletAfterBalanceLabel)
        UIUtils.resetLabel(walletCurrentBalanceLabel)
        UIUtils.resetLabel(crcNameLabel)
        UIUtils.resetLabel(crcInvoiceDueLabel)
        UIUtils.resetLabel(crcInvoiceMonthLabel)

        walletComboBox.setOnAction {
            UIUtils.updateWalletBalanceLabelStyle(walletComboBox.value, walletCurrentBalanceLabel)
            walletAfterBalance()
        }

        useRebateValueField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                useRebateValueField.text = oldValue
            } else {
                walletAfterBalance()
            }
        }

        partialAmountField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                partialAmountField.text = oldValue
            } else {
                walletAfterBalance()
            }
        }
    }

    @FXML
    private fun handleCancel() {
        (crcNameLabel.scene.window as Stage).close()
    }

    @FXML
    private fun handleSave() {
        val wallet = walletComboBox.value

        if (wallet == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_WALLET_NOT_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_WALLET_NOT_SELECTED_MESSAGE),
            )
            return
        }

        val remainingAmount =
            creditCardService.getTotalPendingPaymentsByCreditCardAndMonth(
                creditCard!!.id!!,
                invoiceDate!!,
            )

        val rebateValue =
            if (useRebateValueField.text.isEmpty()) BigDecimal.ZERO else BigDecimal(useRebateValueField.text)

        if (remainingAmount.isZero()) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_INVOICE_ALREADY_PAID_TITLE),
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_INVOICE_ALREADY_PAID_MESSAGE),
            )
            return
        }

        val isPartial = partialAmountField.text.isNotEmpty()

        val amountToPay =
            if (isPartial) BigDecimal(partialAmountField.text) else remainingAmount

        runCatching {
            creditCardService.payInvoice(
                CreditCardInvoicePaymentDTO(
                    creditCardId = creditCard!!.id!!,
                    billingWalletId = wallet.id!!,
                    invoiceDate = invoiceDate!!,
                    amount = amountToPay,
                    rebate = rebateValue,
                ),
            )

            (crcNameLabel.scene.window as Stage).close()
        }.onFailure { e ->
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_ERROR_PAYING_INVOICE_TITLE),
                e.message ?: "Unknown error",
            )
        }
    }

    @FXML
    private fun handleOpenCalculator() {
        WindowUtils.openPopupWindow(
            Files.CALCULATOR_FXML,
            preferencesService.translate(TranslationKeys.MAIN_CALCULATOR),
            springContext,
            { _: CalculatorController -> },
            listOf(Runnable { calculatorService.updateComponentWithResult(useRebateValueField) }),
        )
    }

    @FXML
    private fun handleOpenPartialCalculator() {
        WindowUtils.openPopupWindow(
            Files.CALCULATOR_FXML,
            preferencesService.translate(TranslationKeys.MAIN_CALCULATOR),
            springContext,
            { _: CalculatorController -> },
            listOf(Runnable { calculatorService.updateComponentWithResult(partialAmountField) }),
        )
    }

    private fun walletAfterBalance() {
        val wallet = walletComboBox.value

        if (wallet == null) {
            UIUtils.resetLabel(walletAfterBalanceLabel)
            return
        }

        val rebateValue =
            if (useRebateValueField.text.isEmpty()) BigDecimal.ZERO else BigDecimal(useRebateValueField.text)

        val amountToPay =
            if (partialAmountField.text.isNotEmpty()) {
                BigDecimal(partialAmountField.text).subtract(rebateValue).coerceAtLeast(BigDecimal.ZERO)
            } else {
                creditCardService
                    .getTotalPendingPaymentsByCreditCardAndMonth(creditCard!!.id!!, invoiceDate!!)
                    .subtract(rebateValue)
                    .coerceAtLeast(BigDecimal.ZERO)
            }

        totalToPayLabel.text = UIUtils.formatCurrency(amountToPay)

        runCatching {
            val walletAfterBalanceValue = wallet.balance.subtract(amountToPay)

            if (walletAfterBalanceValue < BigDecimal.ZERO) {
                UIUtils.setLabelStyle(walletAfterBalanceLabel, Styles.NEGATIVE_BALANCE_STYLE)
            } else {
                UIUtils.setLabelStyle(walletAfterBalanceLabel, Styles.NEUTRAL_BALANCE_STYLE)
            }

            walletAfterBalanceLabel.text = UIUtils.formatCurrency(walletAfterBalanceValue)
        }.onFailure {
            UIUtils.resetLabel(walletAfterBalanceLabel)
        }
    }

    private fun loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName()
    }

    private fun populateWalletComboBox() {
        walletComboBox.items.addAll(wallets)
    }

    private fun configureComboBoxes() {
        UIUtils.configureComboBox(walletComboBox, Wallet::name)
    }
}
