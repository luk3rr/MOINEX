/*
 * Filename: AddGoalController.kt (original filename: AddGoalController.java)
 * Created on: December 13, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.goal

import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.RadioButton
import javafx.scene.control.TitledPane
import javafx.scene.control.ToggleGroup
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.exception.MoinexException
import org.moinex.model.enums.GoalFundingStrategy
import org.moinex.model.goal.Goal
import org.moinex.service.PreferencesService
import org.moinex.service.goal.GoalService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.goal.base.BaseGoalManagement
import org.springframework.stereotype.Controller

@Controller
class AddGoalController(
    goalService: GoalService,
    walletService: WalletService,
    preferencesService: PreferencesService,
) : BaseGoalManagement(goalService, walletService, preferencesService) {
    @FXML
    private lateinit var goalFundingStrategyPane: TitledPane

    @FXML
    private lateinit var newDepositRadioButton: RadioButton

    @FXML
    private lateinit var allocateFromMasterWalletRadioButton: RadioButton

    private lateinit var strategyToggleGroup: ToggleGroup

    @FXML
    override fun initialize() {
        super.initialize()
        setupDynamicVisibilityListeners()
    }

    @FXML
    override fun handleSave() {
        val goalName = nameField.text.trim()
        val initialBalanceStr = balanceField.text
        val targetBalanceStr = targetBalanceField.text
        val targetDate = targetDatePicker.value
        val motivation = motivationTextArea.text
        val masterWallet = masterWalletComboBox.value

        if (goalName.isEmpty() || targetBalanceStr.isEmpty() || targetDate == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.GOAL_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.GOAL_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        var strategy: GoalFundingStrategy? = null

        if (goalFundingStrategyPane.isVisible) {
            val selectedToggle = strategyToggleGroup.selectedToggle
            if (selectedToggle == null) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(TranslationKeys.GOAL_DIALOG_STRATEGY_REQUIRED_TITLE),
                    preferencesService.translate(TranslationKeys.GOAL_DIALOG_STRATEGY_REQUIRED_MESSAGE),
                )
                return
            }
            strategy = selectedToggle.userData as GoalFundingStrategy
        }

        runCatching {
            val initialBalance = initialBalanceStr.ifEmpty { "0" }.toBigDecimal()
            val targetBalance = targetBalanceStr.toBigDecimal()

            val goalWalletType =
                walletService.getAllWalletTypes().first { it.name == Constants.GOAL_DEFAULT_WALLET_TYPE_NAME }

            goalService.createGoal(
                Goal(
                    initialBalance = initialBalance,
                    targetBalance = targetBalance,
                    targetDate = targetDate,
                    motivation = motivation,
                    name = goalName,
                    type = goalWalletType,
                    masterWallet = masterWallet,
                    isArchived = false,
                ),
                strategy,
            )

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.GOAL_DIALOG_GOAL_CREATED_TITLE),
                preferencesService.translate(TranslationKeys.GOAL_DIALOG_GOAL_CREATED_MESSAGE),
            )

            (nameField.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.GOAL_DIALOG_INVALID_BALANCE_TITLE),
                        preferencesService.translate(TranslationKeys.GOAL_DIALOG_INVALID_BALANCE_MESSAGE),
                    )
                }
                is IllegalArgumentException,
                is EntityExistsException,
                is EntityNotFoundException,
                is MoinexException.InsufficientResourcesException,
                -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.GOAL_DIALOG_ERROR_CREATING_GOAL_TITLE),
                        e.message ?: "Unknown error",
                    )
                }
                else -> throw e
            }
        }
    }

    private fun setupDynamicVisibilityListeners() {
        strategyToggleGroup = ToggleGroup()
        newDepositRadioButton.toggleGroup = strategyToggleGroup
        allocateFromMasterWalletRadioButton.toggleGroup = strategyToggleGroup

        newDepositRadioButton.userData = GoalFundingStrategy.NEW_DEPOSIT
        allocateFromMasterWalletRadioButton.userData = GoalFundingStrategy.ALLOCATE_FROM_EXISTING

        goalFundingStrategyPane.isVisible = false
        goalFundingStrategyPane.isManaged = false

        val updateVisibility = {
            val masterWalletSelected = masterWalletComboBox.value != null
            val initialBalanceFilled = balanceField.text?.isNotBlank() == true

            val showOptions = masterWalletSelected && initialBalanceFilled

            goalFundingStrategyPane.isVisible = showOptions
            goalFundingStrategyPane.isManaged = showOptions

            goalFundingStrategyPane.scene?.window?.sizeToScene()
        }

        masterWalletComboBox.valueProperty().addListener { _, _, _ -> updateVisibility() }
        balanceField.textProperty().addListener { _, _, _ -> updateVisibility() }
    }
}
