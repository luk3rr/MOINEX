/*
 * Filename: CreditCardPaneController.kt (original filename: CreditCardPaneController.java)
 * Created on: October 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 19/03/2026
 */

package org.moinex.ui.common

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isZero
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.creditcard.CreditCard
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.ui.dialog.creditcard.AddCreditCardCreditController
import org.moinex.ui.dialog.creditcard.AddCreditCardDebtController
import org.moinex.ui.dialog.creditcard.CreditCardCreditsController
import org.moinex.ui.dialog.creditcard.CreditCardInvoicePaymentController
import org.moinex.ui.dialog.creditcard.EditCreditCardController
import org.moinex.ui.main.CreditCardController
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.MessageFormat
import java.time.YearMonth

@Controller
@Scope("prototype")
class CreditCardPaneController(
    private val creditCardService: CreditCardService,
    private val springContext: ConfigurableApplicationContext,
    private val creditCardController: CreditCardController,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var rootVBox: VBox

    @FXML
    private lateinit var crcOperatorIcon: ImageView

    @FXML
    private lateinit var crcName: Label

    @FXML
    private lateinit var crcOperator: Label

    @FXML
    private lateinit var limitLabel: Label

    @FXML
    private lateinit var pendingPaymentsLabel: Label

    @FXML
    private lateinit var availableLimitLabel: Label

    @FXML
    private lateinit var availableRebateLabel: Label

    @FXML
    private lateinit var closureDayLabel: Label

    @FXML
    private lateinit var nextInvoiceLabel: Label

    @FXML
    private lateinit var dueDateLabel: Label

    @FXML
    private lateinit var invoiceStatusLabel: Label

    @FXML
    private lateinit var invoiceMonthLabel: Label

    @FXML
    private lateinit var limitProgressLabel: Label

    @FXML
    private lateinit var invoiceMonthNavigatorBarLabel: Label

    @FXML
    private lateinit var limitProgressBar: ProgressBar

    private var currentDisplayedMonth = YearMonth.now()
    private lateinit var creditCard: CreditCard

    companion object {
        private const val GREEN_LABEL_STYLE = "-fx-text-fill: green"
        private const val BLACK_LABEL_STYLE = "-fx-text-fill: black"
    }

    @FXML
    private fun initialize() {
        currentDisplayedMonth = YearMonth.now()
    }

    @FXML
    private fun handleAddDebt() {
        WindowUtils.openModalWindow(
            Files.ADD_CREDIT_CARD_DEBT_FXML,
            preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_MODAL_ADD_DEBT),
            springContext,
            { controller: AddCreditCardDebtController -> controller.setCreditCard(creditCard) },
            listOf(Runnable { creditCardController.updateDisplay() }),
        )
    }

    @FXML
    private fun handleAddCredit() {
        WindowUtils.openModalWindow(
            Files.ADD_CREDIT_CARD_CREDIT_FXML,
            preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_MODAL_ADD_CREDIT),
            springContext,
            { controller: AddCreditCardCreditController -> controller.setCreditCard(creditCard) },
            listOf(Runnable { creditCardController.updateDisplay() }),
        )
    }

    @FXML
    private fun handleEditCreditCard() {
        WindowUtils.openModalWindow(
            Files.EDIT_CREDIT_CARD_FXML,
            preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_MODAL_EDIT),
            springContext,
            { controller: EditCreditCardController -> controller.setCreditCard(creditCard) },
            listOf(Runnable { creditCardController.updateDisplay() }),
        )
    }

    @FXML
    private fun handleArchiveCreditCard() {
        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_DIALOG_ARCHIVE_TITLE),
                    creditCard.name,
                ),
                preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_DIALOG_ARCHIVE_MESSAGE),
                preferencesService.bundle,
            )
        ) {
            runCatching {
                creditCardService.archiveCreditCard(creditCard.id!!)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_DIALOG_ARCHIVE_SUCCESS_TITLE),
                    MessageFormat.format(
                        preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_DIALOG_ARCHIVE_SUCCESS_MESSAGE),
                        creditCard.name,
                    ),
                )

                creditCardController.updateDisplay()
            }.onFailure { e ->
                when (e) {
                    is EntityNotFoundException, is IllegalStateException ->
                        WindowUtils.showErrorDialog(
                            preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_DIALOG_ARCHIVE_ERROR),
                            e.message ?: "",
                        )
                    else -> throw e
                }
            }
        }
    }

    @FXML
    private fun handleDeleteCreditCard() {
        if (creditCardService.getDebtCountByCreditCard(creditCard.id!!) > 0) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_DIALOG_DELETE_HAS_DEBTS_TITLE),
                preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_DIALOG_DELETE_HAS_DEBTS_MESSAGE),
            )
            return
        }

        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_DIALOG_DELETE_TITLE),
                    creditCard.name,
                ),
                preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_DIALOG_DELETE_MESSAGE),
                preferencesService.bundle,
            )
        ) {
            runCatching {
                creditCardService.deleteCreditCard(creditCard.id!!)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_DIALOG_DELETE_SUCCESS_TITLE),
                    MessageFormat.format(
                        preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_DIALOG_DELETE_SUCCESS_MESSAGE),
                        creditCard.name,
                    ),
                )

                creditCardController.updateDisplay()
            }.onFailure { e ->
                when (e) {
                    is EntityNotFoundException, is IllegalStateException ->
                        WindowUtils.showErrorDialog(
                            preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_DIALOG_DELETE_ERROR),
                            e.message ?: "",
                        )
                    else -> throw e
                }
            }
        }
    }

    @FXML
    private fun handleShowRebates() {
        WindowUtils.openModalWindow(
            Files.CREDIT_CARD_CREDITS_FXML,
            preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_MODAL_SHOW_CREDITS),
            springContext,
            { _: CreditCardCreditsController -> },
            listOf(Runnable { creditCardController.updateDisplay() }),
        )
    }

    @FXML
    private fun handlePrevMonth() {
        currentDisplayedMonth = currentDisplayedMonth.minusMonths(1)
        updateInvoiceInfo()
    }

    @FXML
    private fun handleNextMonth() {
        currentDisplayedMonth = currentDisplayedMonth.plusMonths(1)
        updateInvoiceInfo()
    }

    @FXML
    private fun handleRegisterPayment() {
        WindowUtils.openModalWindow(
            Files.CREDIT_CARD_INVOICE_PAYMENT_FXML,
            preferencesService.translate(TranslationKeys.COMMON_CREDIT_CARD_MODAL_REGISTER_PAYMENT),
            springContext,
            { controller: CreditCardInvoicePaymentController ->
                controller.setCreditCard(creditCard, currentDisplayedMonth)
            },
            listOf(Runnable { creditCardController.updateDisplay(currentDisplayedMonth) }),
        )
    }

    fun updateCreditCardPane(
        crc: CreditCard,
        month: YearMonth,
    ): VBox {
        creditCard = crc
        currentDisplayedMonth = month

        crcName.text = crc.name
        crcOperator.text = crc.operator.name
        crcOperatorIcon.image = Image(Files.CRC_OPERATOR_ICONS_PATH + crc.operator.icon)

        val limit = crc.maxDebt
        val pendingPayments = creditCardService.getTotalPendingPaymentsByCreditCard(crc.id!!)
        val limitAvailable = creditCardService.getAvailableCredit(crc.id!!)
        val rebate = crc.availableRebate

        limitLabel.text = UIUtils.formatCurrency(limit)
        availableRebateLabel.text = UIUtils.formatCurrency(rebate)

        availableRebateLabel.style =
            when {
                rebate > BigDecimal.ZERO -> GREEN_LABEL_STYLE
                else -> BLACK_LABEL_STYLE
            }

        pendingPaymentsLabel.text = UIUtils.formatCurrency(pendingPayments)
        availableLimitLabel.text = UIUtils.formatCurrency(limitAvailable)

        val limitProgress =
            when {
                limit.isZero() -> BigDecimal.ZERO
                else -> pendingPayments.divide(limit, 2, RoundingMode.HALF_UP)
            }

        limitProgressBar.progress = limitProgress.toDouble()
        limitProgressLabel.text = UIUtils.formatPercentage(limitProgress.multiply(BigDecimal(100)))

        dueDateLabel.text = crc.billingDueDay.toString()
        closureDayLabel.text = crc.closingDay.toString()
        nextInvoiceLabel.text = UIUtils.formatShortMonthYear(creditCardService.getNextInvoiceDate(crc))

        updateInvoiceInfo()

        return rootVBox
    }

    fun updateInvoiceInfo() {
        invoiceMonthNavigatorBarLabel.text = UIUtils.formatShortMonthYear(currentDisplayedMonth)

        val totalDebts =
            creditCardService.getInvoiceAmount(creditCard.id!!, currentDisplayedMonth)

        invoiceMonthLabel.text = UIUtils.formatCurrency(totalDebts)

        invoiceStatusLabel.text =
            UIUtils.translateCreditCardInvoiceStatus(
                creditCardService.getInvoiceStatus(creditCard.id!!, currentDisplayedMonth),
            )
    }
}
