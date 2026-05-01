/*
 * Filename: EditGoalController.kt (original filename: EditGoalController.java)
 * Created on: December 14, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.goal

import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.stage.Stage
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isEqual
import org.moinex.common.util.WindowUtils
import org.moinex.exception.MoinexException
import org.moinex.model.goal.Goal
import org.moinex.service.PreferencesService
import org.moinex.service.goal.GoalService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.goal.base.BaseGoalManagement
import org.springframework.stereotype.Controller
import java.time.LocalDate

@Controller
class EditGoalController(
    goalService: GoalService,
    walletService: WalletService,
    preferencesService: PreferencesService,
) : BaseGoalManagement(goalService, walletService, preferencesService) {
    @FXML
    private lateinit var archivedCheckBox: CheckBox

    @FXML
    private lateinit var completedCheckBox: CheckBox

    private lateinit var goal: Goal

    fun setGoal(goal: Goal) {
        this.goal = goal

        nameField.text = goal.name
        balanceField.text = goal.balance.toString()
        targetBalanceField.text = goal.targetBalance.toString()
        targetDatePicker.value = goal.targetDate
        motivationTextArea.text = goal.motivation
        archivedCheckBox.isSelected = goal.isArchived
        completedCheckBox.isSelected = goal.isCompleted()
        masterWalletComboBox.value = goal.masterWallet
    }

    @FXML
    override fun initialize() {
        super.initialize()
        setupDisableMasterWalletComboBox()
    }

    @FXML
    override fun handleSave() {
        val goalName = nameField.text.trim()
        val balanceStr = balanceField.text
        val targetBalanceStr = targetBalanceField.text
        val targetDate = targetDatePicker.value
        val motivation = motivationTextArea.text
        val archived = archivedCheckBox.isSelected
        val completed = completedCheckBox.isSelected
        val masterWallet = masterWalletComboBox.value

        if (goalName.isEmpty() || targetBalanceStr.isEmpty() || targetDate == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.GOAL_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.GOAL_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            val currentBalance = balanceStr.ifEmpty { "0" }.toBigDecimal()
            val targetBalance = targetBalanceStr.toBigDecimal()

            if (goal.name == goalName &&
                goal.balance.isEqual(currentBalance) &&
                (goal.isVirtual() && goal.masterWallet == masterWallet) &&
                goal.targetBalance.isEqual(targetBalance) &&
                goal.targetDate == targetDate &&
                goal.motivation == motivation &&
                goal.isArchived == archived &&
                goal.isCompleted() == completed
            ) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(TranslationKeys.GOAL_DIALOG_NO_CHANGES_TITLE),
                    preferencesService.translate(TranslationKeys.GOAL_DIALOG_NO_CHANGES_MESSAGE),
                )
            } else {
                goal.name = goalName
                goal.balance = currentBalance
                goal.targetBalance = targetBalance
                goal.targetDate = targetDate
                goal.motivation = motivation
                goal.isArchived = archived
                goal.masterWallet = masterWallet

                goal.completionDate =
                    if (completed && !goal.isCompleted()) {
                        LocalDate.now()
                    } else {
                        null
                    }

                goalService.updateGoal(goal)
            }

            (nameField.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.GOAL_DIALOG_INVALID_BALANCE_TITLE),
                        preferencesService.translate(TranslationKeys.GOAL_DIALOG_INVALID_BALANCE_MESSAGE),
                    )
                }
                is EntityNotFoundException,
                is IllegalArgumentException,
                is EntityExistsException,
                is MoinexException.IncompleteGoalException,
                -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.GOAL_DIALOG_ERROR_UPDATING_GOAL_TITLE),
                        e.message ?: "Unknown error",
                    )
                }
                else -> throw e
            }
        }
    }

    private fun setupDisableMasterWalletComboBox() {
        archivedCheckBox.selectedProperty().addListener { _, _, newValue ->
            masterWalletComboBox.value = null
            masterWalletComboBox.isDisable = newValue || completedCheckBox.isSelected
        }

        completedCheckBox.selectedProperty().addListener { _, _, newValue ->
            masterWalletComboBox.value = null
            masterWalletComboBox.isDisable = newValue || archivedCheckBox.isSelected
        }
    }
}
