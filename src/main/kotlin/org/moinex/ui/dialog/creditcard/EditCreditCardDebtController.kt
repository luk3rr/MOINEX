/*
 * Filename: EditCreditCardDebtController.kt (original filename: EditCreditCardDebtController.java)
 * Created on: October 28, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.creditcard

import javafx.fxml.FXML
import javafx.stage.Stage
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.service.CalculatorService
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.ui.dialog.creditcard.base.BaseCreditCardDebtManagement
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.time.YearMonth

@Controller
class EditCreditCardDebtController(
    categoryService: CategoryService,
    creditCardService: CreditCardService,
    calculatorService: CalculatorService,
    springContext: ConfigurableApplicationContext,
    preferencesService: PreferencesService,
) : BaseCreditCardDebtManagement(
        categoryService,
        creditCardService,
        calculatorService,
        springContext,
        preferencesService,
    ) {
    private var crcDebt: CreditCardDebt? = null

    fun setCreditCardDebt(crcDebt: CreditCardDebt) {
        this.crcDebt = crcDebt

        crcComboBox.value = crcDebt.creditCard
        crcLimitLabel.text = UIUtils.formatCurrency(crcDebt.creditCard.maxDebt)

        val availableLimit = creditCardService.getAvailableCredit(crcDebt.creditCard.id!!)
        crcAvailableLimitLabel.text = UIUtils.formatCurrency(availableLimit)
        crcLimitAvailableAfterDebtLabel.text = UIUtils.formatCurrency(availableLimit)

        suggestionsHandler.disable()
        descriptionField.text = crcDebt.description
        suggestionsHandler.enable()

        valueField.text = crcDebt.amount.toString()
        installmentsField.text = crcDebt.installments.toString()
        categoryComboBox.value = crcDebt.category

        val firstPayment =
            creditCardService.getPaymentsByDebtOrderedByInstallment(crcDebt.id!!).first()

        invoiceMonthComboBox.value = firstPayment.date.monthValue
        invoiceYearComboBox.value = firstPayment.date.year
    }

    @FXML
    override fun handleSave() {
        val formData = getFieldsFromInterface()

        if (!formData.isValid()) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            val debtValue = BigDecimal(formData.valueStr)
            val installments = formData.installmentsStr.ifEmpty { "1" }.toInt()

            val firstPayment =
                creditCardService
                    .getPaymentsByDebtOrderedByInstallment(crcDebt!!.id!!)
                    .first()

            val invoice =
                YearMonth.of(
                    firstPayment.date.year,
                    firstPayment.date.monthValue,
                )

            val invoiceDateYearMonth = YearMonth.of(formData.invoiceYear!!, formData.invoiceMonth!!)

            if (crcDebt!!.creditCard.id == formData.creditCard!!.id &&
                crcDebt!!.category.id == formData.category!!.id &&
                debtValue.compareTo(crcDebt!!.amount) == 0 &&
                crcDebt!!.installments == installments &&
                crcDebt!!.description == formData.description &&
                invoice == invoiceDateYearMonth
            ) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_NO_CHANGES_DEBT_TITLE),
                    preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_NO_CHANGES_DEBT_MESSAGE),
                )
            } else {
                crcDebt!!.creditCard = formData.creditCard
                crcDebt!!.category = formData.category!!
                crcDebt!!.description = formData.description
                crcDebt!!.amount = debtValue
                crcDebt!!.installments = installments

                creditCardService.updateDebt(crcDebt!!, invoiceDateYearMonth)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_TRANSACTION_UPDATED_TITLE),
                    preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_TRANSACTION_UPDATED_MESSAGE),
                )
            }

            (crcComboBox.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_INVALID_VALUE_TITLE),
                        preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_INVALID_VALUE_MESSAGE),
                    )
                }
                else -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_ERROR_CREATING_DEBT_TITLE),
                        e.message ?: "Unknown error",
                    )
                }
            }
        }
    }

    override fun updateAvailableLimitAfterDebtLabel() {
        if (!isUpdateAvailableLimitAfterDebtLabelValid()) return

        val newCrc = creditCards.find { it.id!! == crcComboBox.value?.id!! } ?: return

        val newAmount = BigDecimal(valueField.text)

        if (newAmount <= BigDecimal.ZERO) {
            UIUtils.resetLabel(msgLabel)
            return
        }

        val oldCrc = crcDebt!!.creditCard
        val oldAmount = crcDebt!!.amount

        runCatching {
            val availableLimitAfterDebt =
                if (oldCrc.id == newCrc.id) {
                    if (oldAmount < newAmount) {
                        val diff = newAmount.subtract(oldAmount).abs()
                        creditCardService.getAvailableCredit(newCrc.id!!).subtract(diff)
                    } else {
                        creditCardService
                            .getAvailableCredit(newCrc.id!!)
                            .add(oldAmount.subtract(newAmount))
                    }
                } else {
                    val payments =
                        creditCardService.getPaymentsByDebtOrderedByInstallment(crcDebt!!.id!!)

                    val paidAmount =
                        payments
                            .filter { it.isPaid() }
                            .sumOf { it.amount }

                    val remainingAmountToPay = newAmount.subtract(paidAmount)

                    creditCardService
                        .getAvailableCredit(newCrc.id!!)
                        .subtract(remainingAmountToPay)
                }

            if (availableLimitAfterDebt < BigDecimal.ZERO) {
                UIUtils.setLabelStyle(crcLimitAvailableAfterDebtLabel, Constants.NEGATIVE_BALANCE_STYLE)
            } else {
                UIUtils.setLabelStyle(crcLimitAvailableAfterDebtLabel, Constants.NEUTRAL_BALANCE_STYLE)
            }

            crcLimitAvailableAfterDebtLabel.text = UIUtils.formatCurrency(availableLimitAfterDebt)
        }.onFailure {
            UIUtils.resetLabel(crcLimitAvailableAfterDebtLabel)
        }
    }
}
