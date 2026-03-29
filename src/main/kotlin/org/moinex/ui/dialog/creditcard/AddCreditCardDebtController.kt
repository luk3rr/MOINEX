/*
 * Filename: AddCreditCardDebtController.kt (original filename: AddCreditCardDebtController.java)
 * Created on: October 25, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.creditcard

import javafx.fxml.FXML
import javafx.stage.Stage
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.model.Category
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.service.CalculatorService
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.ui.dialog.creditcard.base.BaseCreditCardDebtManagement
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

@Controller
class AddCreditCardDebtController(
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
    private var onDebtCreatedCallback: ((Int) -> Unit)? = null

    fun setOnDebtCreatedCallback(callback: (Int) -> Unit) {
        this.onDebtCreatedCallback = callback
    }

    fun prefillDescription(description: String) {
        descriptionField.text = description
    }

    fun prefillAmount(amount: BigDecimal) {
        valueField.text = amount.toPlainString()
        updateAvailableLimitAfterDebtLabel()
        updateMsgLabel()
    }

    fun prefillCategory(category: Category) {
        categoryComboBox.value = category
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
            val invoiceDateYearMonth = YearMonth.of(formData.invoiceYear!!, formData.invoiceMonth!!)

            val debtId =
                creditCardService.createDebt(
                    CreditCardDebt(
                        category = formData.category!!,
                        installments = installments,
                        creditCard = formData.creditCard!!,
                        date = LocalDateTime.now(),
                        amount = debtValue,
                        description = formData.description,
                    ),
                    invoiceDateYearMonth,
                )

            onDebtCreatedCallback?.invoke(debtId)

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_DEBT_CREATED_TITLE),
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_DEBT_CREATED_MESSAGE),
            )

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
}
