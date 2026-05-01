/*
 * Filename: AddRecurringTransactionController.kt (original filename: AddRecurringTransactionController.java)
 * Created on: November 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.create

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.model.wallettransaction.RecurringTransaction
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.wallettransaction.base.BaseRecurringTransactionManagement
import org.springframework.stereotype.Controller

@Controller
class AddRecurringTransactionController(
    walletService: WalletService,
    recurringTransactionService: RecurringTransactionService,
    categoryService: CategoryService,
    preferencesService: PreferencesService,
) : BaseRecurringTransactionManagement(
        walletService,
        recurringTransactionService,
        categoryService,
        preferencesService,
    ) {
    @FXML
    override fun handleSave() {
        val wallet = walletComboBox.value
        val description = descriptionField.text
        val valueString = valueField.text
        val type = typeComboBox.value
        val category = categoryComboBox.value
        val startDate = startDatePicker.value
        var endDate = endDatePicker.value
        val frequency = frequencyComboBox.value

        if (wallet == null ||
            description.isNullOrBlank() ||
            valueString.isNullOrBlank() ||
            type == null ||
            category == null ||
            startDate == null ||
            frequency == null
        ) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            val transactionAmount = valueString.toBigDecimal()

            endDate = endDate ?: Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE

            recurringTransactionService.createRecurringTransaction(
                RecurringTransaction(
                    startDate = startDate,
                    endDate = endDate,
                    nextDueDate = startDate,
                    frequency = frequency,
                    status = RecurringTransactionStatus.ACTIVE,
                    includeInNetWorth = includeInNetWorthCheckBox.isSelected,
                    description = description,
                    wallet = wallet,
                    category = category,
                    type = type,
                    amount = transactionAmount,
                    includeInAnalysis = includeInAnalysisCheckBox.isSelected,
                ),
            )

            (descriptionField.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_RECURRING_VALUE_TITLE,
                        ),
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_RECURRING_VALUE_MESSAGE,
                        ),
                    )
                }
                is EntityNotFoundException, is IllegalArgumentException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_ERROR_CREATING_RECURRING_TITLE,
                        ),
                        e.message ?: "Unknown error",
                    )
                }
                else -> throw e
            }
        }
    }
}
