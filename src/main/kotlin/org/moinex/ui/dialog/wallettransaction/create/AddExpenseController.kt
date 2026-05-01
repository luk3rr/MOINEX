/*
 * Filename: AddExpenseController.kt (original filename: AddExpenseController.java)
 * Created on: October  5, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.create

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.stage.Stage
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.CalculatorService
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.wallettransaction.base.BaseWalletTransactionManagement
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.time.LocalTime

@Controller
class AddExpenseController(
    walletService: WalletService,
    categoryService: CategoryService,
    calculatorService: CalculatorService,
    preferencesService: PreferencesService,
    springContext: ConfigurableApplicationContext,
) : BaseWalletTransactionManagement(
        walletService,
        categoryService,
        calculatorService,
        preferencesService,
        springContext,
    ) {
    private var onTransactionCreatedCallback: ((Int) -> Unit)? = null

    init {
        walletTransactionType = WalletTransactionType.EXPENSE
    }

    fun setOnTransactionCreatedCallback(callback: (Int) -> Unit) {
        this.onTransactionCreatedCallback = callback
    }

    @FXML
    override fun handleSave() {
        val wallet = walletComboBox.value
        val description = descriptionField.text
        val expenseValueString = transactionValueField.text
        val status = statusComboBox.value
        val category = categoryComboBox.value
        val expenseDate = transactionDatePicker.value

        if (wallet == null ||
            description.isNullOrBlank() ||
            expenseValueString.isNullOrBlank() ||
            status == null ||
            category == null ||
            expenseDate == null
        ) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            val expenseValue = expenseValueString.toBigDecimal()
            val currentTime = LocalTime.now()
            val dateTimeWithCurrentHour = expenseDate.atTime(currentTime)

            val transactionId =
                walletService.createWalletTransaction(
                    WalletTransaction(
                        date = dateTimeWithCurrentHour,
                        status = status,
                        description = description,
                        includeInAnalysis = includeInAnalysisCheckBox.isSelected,
                        wallet = wallet,
                        category = category,
                        type = walletTransactionType,
                        amount = expenseValue,
                    ),
                    publishNotification = true,
                )

            onTransactionCreatedCallback?.invoke(transactionId)

            (descriptionField.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_EXPENSE_VALUE_TITLE,
                        ),
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_EXPENSE_VALUE_MESSAGE,
                        ),
                    )
                }
                is EntityNotFoundException, is IllegalArgumentException, is IllegalStateException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_ERROR_CREATING_EXPENSE_TITLE,
                        ),
                        e.message ?: "Unknown error",
                    )
                }
                else -> throw e
            }
        }
    }

    override fun loadSuggestionsFromDatabase() {
        suggestionsHandler.suggestions = walletService.getWalletTransactionSuggestionsByType(walletTransactionType)
    }
}
