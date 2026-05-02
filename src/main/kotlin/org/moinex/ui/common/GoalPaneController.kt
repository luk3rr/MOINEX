/*
 * Filename: GoalPaneController.kt (original filename: GoalPaneController.java)
 * Created on: December 15, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 19/03/2026
 */

package org.moinex.ui.common

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import org.moinex.common.chart.ChartFactory
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.applyIconTheme
import org.moinex.common.extension.isZero
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.exception.MoinexException
import org.moinex.model.goal.Goal
import org.moinex.service.PreferencesService
import org.moinex.service.goal.GoalService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.goal.EditGoalController
import org.moinex.ui.dialog.wallettransaction.create.AddExpenseController
import org.moinex.ui.dialog.wallettransaction.create.AddIncomeController
import org.moinex.ui.dialog.wallettransaction.create.AddTransferController
import org.moinex.ui.main.GoalController
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.MessageFormat
import java.time.LocalDate

@Controller
@Scope("prototype")
class GoalPaneController(
    private val goalService: GoalService,
    private val springContext: ConfigurableApplicationContext,
    private val goalController: GoalController,
    private val walletService: WalletService,
    private val preferencesService: PreferencesService,
    private val chartFactory: ChartFactory,
) {
    @FXML
    private lateinit var rootVBox: VBox

    @FXML
    private lateinit var infosVBox: VBox

    @FXML
    private lateinit var goalIcon: ImageView

    @FXML
    private lateinit var goalName: Label

    @FXML
    private lateinit var goalMotivation: Label

    @FXML
    private lateinit var goalTargetAmount: Label

    @FXML
    private lateinit var goalCurrentAmount: Label

    @FXML
    private lateinit var dateTitleLabel: Label

    @FXML
    private lateinit var daysTitleLabel: Label

    @FXML
    private lateinit var goalTargetDate: Label

    @FXML
    private lateinit var goalIdealAMountPerMonth: Label

    @FXML
    private lateinit var missingDays: Label

    @FXML
    private lateinit var progressBarPane: StackPane

    @FXML
    private lateinit var toggleArchiveGoal: MenuItem

    @FXML
    private lateinit var toggleCompleteGoal: MenuItem

    @FXML
    private lateinit var currentHBox: HBox

    @FXML
    private lateinit var idealPerMonthHBox: HBox

    private lateinit var goal: Goal

    companion object {
        private const val PERCENTAGE_MULTIPLIER = 100.0
    }

    @FXML
    private fun initialize() {
        // Still empty
    }

    fun updateGoalPane(gl: Goal): VBox {
        goal = goalService.getGoalById(gl.id!!)

        val currentGoal = goal

        goalName.text = currentGoal.name
        goalMotivation.text = currentGoal.motivation

        goalIcon.image =
            Image(
                if (currentGoal.isCompleted()) {
                    Files.TROPHY_ICON
                } else {
                    Files.WALLET_TYPE_ICONS_PATH + currentGoal.type.icon
                },
            )
        goalIcon.applyIconTheme(preferencesService.isDarkMode())

        goalTargetAmount.text = UIUtils.formatCurrency(currentGoal.targetBalance)
        goalTargetDate.text = UIUtils.formatDateForDisplay(currentGoal.targetDate)

        UIUtils.addTooltipToNode(goalName, currentGoal.name)

        if (!currentGoal.motivation.isNullOrEmpty()) {
            UIUtils.addTooltipToNode(goalMotivation, currentGoal.motivation)
        }

        val progressBar =
            chartFactory.createCircularProgressBar(
                Constants.GOAL_PANE_PROGRESS_BAR_RADIUS,
                Constants.GOAL_PANE_PROGRESS_BAR_WIDTH,
            )

        if (currentGoal.isArchived) {
            toggleArchiveGoal.text =
                preferencesService.translate(TranslationKeys.COMMON_GOAL_UNARCHIVE_GOAL)
        }

        val percentage =
            if (currentGoal.isCompleted()) {
                handleCompletedGoal(currentGoal)
            } else {
                handleActiveGoal(currentGoal)
            }

        progressBar.draw(percentage)

        progressBarPane.children.apply {
            clear()
            add(progressBar)
        }

        return rootVBox
    }

    private fun handleCompletedGoal(currentGoal: Goal): Double {
        dateTitleLabel.text =
            preferencesService.translate(TranslationKeys.COMMON_GOAL_COMPLETION_DATE)
        goalTargetDate.text = UIUtils.formatDateForDisplay(currentGoal.completionDate)

        daysTitleLabel.text =
            preferencesService.translate(TranslationKeys.COMMON_GOAL_MISSING_DAYS)
        missingDays.text =
            Constants
                .calculateDaysUntilTarget(currentGoal.completionDate!!, currentGoal.targetDate)
                .toString()

        infosVBox.children.removeAll(currentHBox, idealPerMonthHBox)

        toggleCompleteGoal.text =
            preferencesService.translate(TranslationKeys.COMMON_GOAL_UNCOMPLETE_GOAL)

        return PERCENTAGE_MULTIPLIER
    }

    private fun handleActiveGoal(currentGoal: Goal): Double {
        goalCurrentAmount.text = UIUtils.formatCurrency(currentGoal.balance)

        val monthsUntilTarget =
            Constants.calculateMonthsUntilTarget(LocalDate.now(), currentGoal.targetDate)

        val idealAmountPerMonth =
            if (monthsUntilTarget <= 0) {
                currentGoal.targetBalance - currentGoal.balance
            } else {
                (currentGoal.targetBalance - currentGoal.balance).divide(
                    BigDecimal(monthsUntilTarget),
                    2,
                    RoundingMode.HALF_UP,
                )
            }

        goalIdealAMountPerMonth.text = UIUtils.formatCurrency(idealAmountPerMonth)

        val missingDaysValue =
            Constants.calculateDaysUntilTarget(LocalDate.now(), currentGoal.targetDate)
        missingDays.text = missingDaysValue.toString()

        return if (currentGoal.targetBalance.isZero()) {
            0.0
        } else {
            currentGoal.balance
                .divide(
                    currentGoal.targetBalance,
                    2,
                    RoundingMode.HALF_UP,
                ).multiply(BigDecimal(PERCENTAGE_MULTIPLIER))
                .toDouble()
        }
    }

    @FXML
    private fun handleAddIncome() {
        val currentGoal = goal

        if (currentGoal.isArchived) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_ARCHIVED_TITLE),
                preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_ARCHIVED_CANNOT_ADD_INCOME),
            )
            return
        }

        WindowUtils.openModalWindow(
            Files.ADD_INCOME_FXML,
            preferencesService.translate(TranslationKeys.COMMON_GOAL_MODAL_ADD_INCOME),
            springContext,
            { controller: AddIncomeController -> controller.setWalletComboBox(currentGoal) },
            listOf(Runnable { goalController.updateDisplay() }),
        )
    }

    @FXML
    private fun handleAddExpense() {
        val currentGoal = goal

        if (currentGoal.isArchived) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_ARCHIVED_TITLE),
                preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_ARCHIVED_CANNOT_ADD_EXPENSE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Files.ADD_EXPENSE_FXML,
            preferencesService.translate(TranslationKeys.COMMON_GOAL_MODAL_ADD_EXPENSE),
            springContext,
            { controller: AddExpenseController -> controller.setWalletComboBox(currentGoal) },
            listOf(Runnable { goalController.updateDisplay() }),
        )
    }

    @FXML
    private fun handleAddTransfer() {
        val currentGoal = goal

        if (currentGoal.isArchived) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_ARCHIVED_TITLE),
                preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_ARCHIVED_CANNOT_ADD_TRANSFER),
            )
            return
        }

        WindowUtils.openModalWindow(
            Files.ADD_TRANSFER_FXML,
            preferencesService.translate(TranslationKeys.COMMON_GOAL_MODAL_ADD_TRANSFER),
            springContext,
            { controller: AddTransferController -> controller.setReceiverWalletComboBox(currentGoal) },
            listOf(Runnable { goalController.updateDisplay() }),
        )
    }

    @FXML
    private fun handleEditGoal() {
        WindowUtils.openModalWindow(
            Files.EDIT_GOAL_FXML,
            preferencesService.translate(TranslationKeys.COMMON_GOAL_MODAL_EDIT_GOAL),
            springContext,
            { controller: EditGoalController -> controller.setGoal(goal) },
            listOf(Runnable { goalController.updateDisplay() }),
        )
    }

    @FXML
    private fun handleCompleteGoal() {
        val currentGoal = goal

        if (currentGoal.isCompleted()) {
            handleReopenGoal(currentGoal)
        } else {
            handleCompleteGoalConfirmation(currentGoal)
        }
    }

    private fun handleReopenGoal(currentGoal: Goal) {
        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_REOPEN_TITLE),
                    currentGoal.name,
                ),
                preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_REOPEN_MESSAGE),
                preferencesService.bundle,
            )
        ) {
            goalService.reopenGoal(currentGoal.id!!)
            goalController.updateDisplay()
        }
    }

    private fun handleCompleteGoalConfirmation(currentGoal: Goal) {
        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_COMPLETE_TITLE),
                    currentGoal.name,
                ),
                preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_COMPLETE_MESSAGE),
                preferencesService.bundle,
            )
        ) {
            runCatching {
                goalService.completeGoal(currentGoal.id!!)
            }.onFailure { e ->
                when (e) {
                    is EntityNotFoundException, is MoinexException.IncompleteGoalException -> {
                        WindowUtils.showErrorDialog(
                            preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_COMPLETE_ERROR),
                            e.message ?: "",
                        )
                        return
                    }
                    else -> throw e
                }
            }

            goalController.updateDisplay()
        }
    }

    @FXML
    private fun handleArchiveGoal() {
        val currentGoal = goal

        if (currentGoal.isArchived) {
            handleUnarchiveGoal(currentGoal)
        } else {
            handleArchiveGoalConfirmation(currentGoal)
        }
    }

    private fun handleUnarchiveGoal(currentGoal: Goal) {
        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_UNARCHIVE_TITLE),
                    currentGoal.name,
                ),
                preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_UNARCHIVE_MESSAGE),
                preferencesService.bundle,
            )
        ) {
            goalService.unarchiveGoal(currentGoal.id!!)
            goalController.updateDisplay()
        }
    }

    private fun handleArchiveGoalConfirmation(currentGoal: Goal) {
        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_ARCHIVE_TITLE),
                    currentGoal.name,
                ),
                preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_ARCHIVE_MESSAGE),
                preferencesService.bundle,
            )
        ) {
            goalService.archiveGoal(currentGoal.id!!)
            goalController.updateDisplay()
        }
    }

    @FXML
    private fun handleDeleteGoal() {
        val currentGoal = goal

        if (walletService.getWalletTransactionAndTransferCountByWallet(currentGoal.id!!) > 0) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_DELETE_HAS_TRANSACTIONS_TITLE),
                preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_DELETE_HAS_TRANSACTIONS_MESSAGE),
            )
            return
        }

        val message = buildDeleteConfirmationMessage(currentGoal)

        runCatching {
            if (WindowUtils.showConfirmationDialog(
                    preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_DELETE_TITLE),
                    message,
                    preferencesService.bundle,
                )
            ) {
                goalService.deleteGoal(currentGoal.id!!)
                goalController.updateDisplay()
            }
        }.onFailure { e ->
            when (e) {
                is EntityNotFoundException, is IllegalStateException ->
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_DELETE_ERROR),
                        e.message ?: "",
                    )
                else -> throw e
            }
        }
    }

    private fun buildDeleteConfirmationMessage(currentGoal: Goal): String {
        val baseMessage =
            listOf(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_DELETE_NAME),
                    currentGoal.name,
                ),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_DELETE_INITIAL_AMOUNT),
                    UIUtils.formatCurrency(currentGoal.initialBalance),
                ),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_DELETE_CURRENT_AMOUNT),
                    UIUtils.formatCurrency(currentGoal.balance),
                ),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_DELETE_TARGET_AMOUNT),
                    UIUtils.formatCurrency(currentGoal.targetBalance),
                ),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_DELETE_TARGET_DATE),
                    UIUtils.formatDateForDisplay(currentGoal.targetDate),
                ),
            ).joinToString("\n")

        val totalOfAssociatedVirtualWallets =
            walletService.getCountOfVirtualWalletsByMasterWalletId(currentGoal.id!!)

        return if (totalOfAssociatedVirtualWallets != 0) {
            val virtualWalletsMessage =
                "\n" +
                    MessageFormat.format(
                        preferencesService.translate(TranslationKeys.COMMON_GOAL_DIALOG_DELETE_VIRTUAL_WALLETS),
                        totalOfAssociatedVirtualWallets,
                    ) + "\n"
            baseMessage + virtualWalletsMessage
        } else {
            baseMessage
        }
    }
}
