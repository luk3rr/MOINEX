/*
 * Filename: EditCreditCardController.kt (original filename: EditCreditCardController.java)
 * Created on: October 24, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.creditcard

import javafx.fxml.FXML
import javafx.stage.Stage
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.model.creditcard.CreditCard
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.creditcard.base.BaseCreditCardManagement
import org.springframework.stereotype.Controller
import java.math.BigDecimal

@Controller
class EditCreditCardController(
    creditCardService: CreditCardService,
    walletService: WalletService,
    private val preferencesService: PreferencesService,
) : BaseCreditCardManagement(creditCardService, walletService) {
    private var creditCard: CreditCard? = null

    fun setCreditCard(crc: CreditCard) {
        creditCard = crc
        nameField.text = creditCard!!.name
        limitField.text = creditCard!!.maxDebt.toString()
        lastFourDigitsField.text = creditCard!!.lastFourDigits
        operatorComboBox.value = creditCard!!.operator
        closingDayComboBox.value = creditCard!!.closingDay.toString()
        dueDayComboBox.value = creditCard!!.billingDueDay.toString()
        defaultBillingWalletComboBox.value = creditCard!!.defaultBillingWallet
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
            val crcLimit = BigDecimal(formData.limitStr)
            val crcClosingDay = formData.closingDayStr!!.toInt()
            val crcDueDay = formData.dueDayStr!!.toInt()

            val defaultWalletChanged =
                (
                    formData.defaultBillingWallet != null &&
                        creditCard!!.defaultBillingWallet != null &&
                        formData.defaultBillingWallet.id == creditCard!!.defaultBillingWallet!!.id
                ) ||
                    (formData.defaultBillingWallet == null && creditCard!!.defaultBillingWallet == null)

            if (creditCard!!.name == formData.name &&
                crcLimit.compareTo(creditCard!!.maxDebt) == 0 &&
                creditCard!!.lastFourDigits == formData.lastFourDigits &&
                creditCard!!.closingDay == crcClosingDay &&
                creditCard!!.billingDueDay == crcDueDay &&
                creditCard!!.operator.id == formData.operator!!.id &&
                defaultWalletChanged
            ) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_NO_CHANGES_TITLE),
                    preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_NO_CHANGES_MESSAGE),
                )
            } else {
                creditCard!!.name = formData.name
                creditCard!!.maxDebt = crcLimit
                creditCard!!.lastFourDigits = formData.lastFourDigits
                creditCard!!.closingDay = crcClosingDay
                creditCard!!.billingDueDay = crcDueDay
                creditCard!!.operator = formData.operator!!
                creditCard!!.defaultBillingWallet = formData.defaultBillingWallet

                creditCardService.updateCreditCard(creditCard!!)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_UPDATED_TITLE),
                    preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_UPDATED_MESSAGE),
                )
            }

            (nameField.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_INVALID_LIMIT_TITLE),
                        preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_INVALID_LIMIT_MESSAGE),
                    )
                }
                else -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_ERROR_CREATING_TITLE),
                        e.message ?: "Unknown error",
                    )
                }
            }
        }
    }
}
