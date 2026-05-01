/*
 * Filename: AddCreditCardController.kt (original filename: AddCreditCardController.java)
 * Created on: October 24, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.creditcard

import javafx.fxml.FXML
import javafx.stage.Stage
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.model.creditcard.CreditCard
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.creditcard.base.BaseCreditCardManagement
import org.springframework.stereotype.Controller
import java.math.BigDecimal

@Controller
class AddCreditCardController(
    creditCardService: CreditCardService,
    walletService: WalletService,
    private val preferencesService: PreferencesService,
) : BaseCreditCardManagement(creditCardService, walletService) {
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

            creditCardService.createCreditCard(
                CreditCard(
                    operator = formData.operator!!,
                    defaultBillingWallet = formData.defaultBillingWallet,
                    name = formData.name,
                    billingDueDay = crcDueDay,
                    closingDay = crcClosingDay,
                    maxDebt = crcLimit,
                    availableRebate = BigDecimal.ZERO,
                    lastFourDigits = formData.lastFourDigits,
                    isArchived = false,
                ),
            )

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
