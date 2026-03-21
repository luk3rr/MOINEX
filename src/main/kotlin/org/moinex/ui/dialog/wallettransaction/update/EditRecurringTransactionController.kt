/*
 * Filename: EditRecurringTransactionController.kt (original filename: EditRecurringTransactionController.java)
 * Created on: November 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.update

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isEqual
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
class EditRecurringTransactionController(
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
    private lateinit var activeCheckBox: CheckBox

    private var rt: RecurringTransaction? = null

    fun setRecurringTransaction(rt: RecurringTransaction) {
        this.rt = rt
        walletComboBox.value = rt.wallet
        descriptionField.text = rt.description
        valueField.text = rt.amount.toString()
        typeComboBox.value = rt.type
        categoryComboBox.value = rt.category

        startDatePicker.value = rt.nextDueDate

        if (rt.endDate == Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE) {
            endDatePicker.value = null
        } else {
            endDatePicker.value = rt.endDate
        }

        frequencyComboBox.value = rt.frequency

        activeCheckBox.isSelected = rt.status == RecurringTransactionStatus.ACTIVE
        includeInAnalysisCheckBox.isSelected = rt.includeInAnalysis
        includeInNetWorthCheckBox.isSelected = rt.includeInNetWorth

        updateInfoLabel()
    }

    @FXML
    override fun initialize() {
        super.initialize()
        activeCheckBox.setOnAction { updateInfoLabel() }
    }

    @FXML
    override fun handleSave() {
        val wallet = walletComboBox.value
        val description = descriptionField.text
        val valueString = valueField.text
        val type = typeComboBox.value
        val category = categoryComboBox.value
        val nextDueDate = startDatePicker.value
        var endDate = endDatePicker.value
        val frequency = frequencyComboBox.value

        if (wallet == null ||
            description.isNullOrBlank() ||
            valueString.isNullOrBlank() ||
            type == null ||
            category == null ||
            nextDueDate == null ||
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

            val endDateChanged =
                (endDate != null && endDate != rt!!.endDate) ||
                    (endDate == null && rt!!.endDate != Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE)

            if (rt!!.wallet.id == wallet.id &&
                rt!!.description == description &&
                rt!!.amount.isEqual(transactionAmount) &&
                rt!!.type == type &&
                rt!!.category.id == category.id &&
                rt!!.nextDueDate == nextDueDate &&
                !endDateChanged &&
                rt!!.frequency == frequency &&
                rt!!.status ==
                (
                    if (activeCheckBox.isSelected) {
                        RecurringTransactionStatus.ACTIVE
                    } else {
                        RecurringTransactionStatus.INACTIVE
                    }
                ) &&
                rt!!.includeInAnalysis == includeInAnalysisCheckBox.isSelected &&
                rt!!.includeInNetWorth == includeInNetWorthCheckBox.isSelected
            ) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_NO_CHANGES_MADE_TITLE),
                    preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_NO_CHANGES_MADE_MESSAGE),
                )
            } else {
                rt!!.wallet = wallet
                rt!!.description = description
                rt!!.amount = transactionAmount
                rt!!.type = type
                rt!!.category = category
                rt!!.nextDueDate = nextDueDate

                endDate = endDate ?: Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE

                rt!!.endDate = endDate
                rt!!.frequency = frequency
                rt!!.status =
                    if (activeCheckBox.isSelected) {
                        RecurringTransactionStatus.ACTIVE
                    } else {
                        RecurringTransactionStatus.INACTIVE
                    }
                rt!!.includeInAnalysis = includeInAnalysisCheckBox.isSelected
                rt!!.includeInNetWorth = includeInNetWorthCheckBox.isSelected

                recurringTransactionService.updateRecurringTransaction(rt!!)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(
                        TranslationKeys.WALLETTRANSACTION_DIALOG_RECURRING_TRANSACTION_UPDATED_TITLE,
                    ),
                    preferencesService.translate(
                        TranslationKeys.WALLETTRANSACTION_DIALOG_RECURRING_TRANSACTION_UPDATED_MESSAGE,
                    ),
                )
            }

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
                            TranslationKeys.WALLETTRANSACTION_DIALOG_ERROR_EDITING_RECURRING_TITLE,
                        ),
                        e.message ?: "Unknown error",
                    )
                }
                else -> throw e
            }
        }
    }

    override fun updateInfoLabel() {
        if (!activeCheckBox.isSelected) {
            infoLabel.text =
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_INFO_RECURRING_INACTIVE,
                )
        } else {
            super.updateInfoLabel()
        }
    }
}
