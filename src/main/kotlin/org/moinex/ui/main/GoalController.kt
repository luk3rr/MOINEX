/*
 * Filename: GoalController.kt (original filename: GoalController.java)
 * Created on: December 8, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 17/03/2026
 */

package org.moinex.ui.main

import com.jfoenix.controls.JFXButton
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.ComboBox
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.Tooltip
import javafx.scene.layout.AnchorPane
import org.moinex.common.extension.isZero
import org.moinex.common.extension.setAnchorPaneConstraints
import org.moinex.constants.TranslationKeys
import org.moinex.model.goal.Goal
import org.moinex.service.PreferencesService
import org.moinex.service.goal.GoalService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.common.GoalFullPaneController
import org.moinex.ui.dialog.goal.AddGoalController
import org.moinex.ui.dialog.goal.EditGoalController
import org.moinex.ui.dialog.wallettransaction.AddTransferController
import org.moinex.util.Constants
import org.moinex.util.UIUtils
import org.moinex.util.WindowUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.MessageFormat
import java.time.LocalDate

@Controller
class GoalController(
    private val goalService: GoalService,
    private val walletService: WalletService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var inProgressPane1: AnchorPane

    @FXML
    private lateinit var inProgressPane2: AnchorPane

    @FXML
    private lateinit var accomplishedPane1: AnchorPane

    @FXML
    private lateinit var accomplishedPane2: AnchorPane

    @FXML
    private lateinit var inProgressPrevButton: JFXButton

    @FXML
    private lateinit var inProgressNextButton: JFXButton

    @FXML
    private lateinit var accomplishedPrevButton: JFXButton

    @FXML
    private lateinit var accomplishedNextButton: JFXButton

    @FXML
    private lateinit var goalTableView: TableView<Goal>

    @FXML
    private lateinit var statusComboBox: ComboBox<String>

    @FXML
    private lateinit var goalSearchField: TextField

    private var goals: List<Goal> = emptyList()
    private var inProgressCurrentPage = 0
    private var accomplishedCurrentPage = 0

    companion object {
        private val logger = LoggerFactory.getLogger(GoalController::class.java)
        private const val IN_PROGRESS_ITEMS_PER_PAGE = 2
        private const val ACCOMPLISHED_ITEMS_PER_PAGE = 2
    }

    @FXML
    fun initialize() {
        populateStatusComboBox()
        configureTableView()
        loadGoalsFromDatabase()

        updateDisplayInProgressGoals()
        updateDisplayAccomplishedGoals()
        updateGoalTableView()

        statusComboBox.setOnAction { updateGoalTableView() }
        goalSearchField.textProperty().addListener { _, _, _ -> updateGoalTableView() }

        setButtonsActions()
    }

    @FXML
    private fun handleAddGoal() {
        WindowUtils.openModalWindow(
            Constants.ADD_GOAL_FXML,
            preferencesService.translate(TranslationKeys.GOAL_DIALOG_ADD_GOAL_TITLE),
            springContext,
            { _: AddGoalController -> },
            listOf(
                Runnable {
                    loadGoalsFromDatabase()
                    updateDisplayInProgressGoals()
                    updateDisplayAccomplishedGoals()
                    updateGoalTableView()
                },
            ),
        )
    }

    @FXML
    private fun handleAddDeposit() {
        val goal = goalTableView.selectionModel.selectedItem

        if (goal == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.GOAL_DIALOG_NO_SELECTION_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.GOAL_DIALOG_NO_SELECTION_ADD_DEPOSIT_MESSAGE,
                ),
            )
            return
        }

        if (goal.isArchived) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.GOAL_DIALOG_ARCHIVED_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.GOAL_DIALOG_ARCHIVED_MESSAGE,
                ),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.ADD_TRANSFER_FXML,
            preferencesService.translate(
                TranslationKeys.GOAL_DIALOG_ADD_TRANSFER_TITLE,
            ),
            springContext,
            { controller: AddTransferController -> controller.setReceiverWalletComboBox(goal) },
            listOf(
                Runnable {
                    loadGoalsFromDatabase()
                    updateDisplayInProgressGoals()
                    updateDisplayAccomplishedGoals()
                    updateGoalTableView()
                },
            ),
        )
    }

    @FXML
    private fun handleEditGoal() {
        val goal = goalTableView.selectionModel.selectedItem

        if (goal == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.GOAL_DIALOG_NO_SELECTION_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.GOAL_DIALOG_NO_SELECTION_EDIT_MESSAGE,
                ),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.EDIT_GOAL_FXML,
            preferencesService.translate(TranslationKeys.GOAL_DIALOG_EDIT_GOAL_TITLE),
            springContext,
            { controller: EditGoalController -> controller.setGoal(goal) },
            listOf(
                Runnable {
                    loadGoalsFromDatabase()
                    updateDisplayInProgressGoals()
                    updateDisplayAccomplishedGoals()
                    updateGoalTableView()
                },
            ),
        )
    }

    @FXML
    private fun handleDeleteGoal() {
        val goal = goalTableView.selectionModel.selectedItem

        if (goal == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.GOAL_DIALOG_NO_SELECTION_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.GOAL_DIALOG_NO_SELECTION_DELETE_MESSAGE,
                ),
            )
            return
        }

        if (walletService.getWalletTransactionAndTransferCountByWallet(goal.id!!) > 0) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.GOAL_DIALOG_HAS_TRANSACTIONS_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.GOAL_DIALOG_HAS_TRANSACTIONS_MESSAGE,
                ),
            )
            return
        }

        val message =
            buildString {
                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.GOAL_DIALOG_CONFIRMATION_DELETE_NAME,
                        ),
                        goal.name,
                    ),
                )
                append("\n")
                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.GOAL_DIALOG_CONFIRMATION_DELETE_INITIAL_AMOUNT,
                        ),
                        UIUtils.formatCurrency(goal.initialBalance),
                    ),
                )
                append("\n")
                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.GOAL_DIALOG_CONFIRMATION_DELETE_CURRENT_AMOUNT,
                        ),
                        UIUtils.formatCurrency(goal.balance),
                    ),
                )
                append("\n")
                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.GOAL_DIALOG_CONFIRMATION_DELETE_TARGET_AMOUNT,
                        ),
                        UIUtils.formatCurrency(goal.targetBalance),
                    ),
                )
                append("\n")
                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.GOAL_DIALOG_CONFIRMATION_DELETE_TARGET_DATE,
                        ),
                        UIUtils.formatDateForDisplay(goal.targetDate, preferencesService),
                    ),
                )
                append("\n")
            }

        runCatching {
            if (
                WindowUtils.showConfirmationDialog(
                    preferencesService.translate(
                        TranslationKeys.GOAL_DIALOG_CONFIRMATION_DELETE_TITLE,
                    ),
                    message,
                    preferencesService.getBundle(),
                )
            ) {
                goalService.deleteGoal(goal.id!!)
                goals = goals - goal

                updateDisplayInProgressGoals()
                updateDisplayAccomplishedGoals()
                updateGoalTableView()
            }
        }.onFailure { e ->
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.DIALOG_ERROR_TITLE),
                e.message ?: "",
            )
        }
    }

    fun updateDisplay() {
        loadGoalsFromDatabase()
        updateDisplayInProgressGoals()
        updateDisplayAccomplishedGoals()
        updateGoalTableView()
    }

    private fun loadGoalsFromDatabase() {
        goals = goalService.getAllGoals()
    }

    private fun updateDisplayInProgressGoals() {
        inProgressPane1.children.clear()
        inProgressPane2.children.clear()

        val inProgressGoals = goals.filter { !it.isCompleted() && !it.isArchived }

        val start = inProgressCurrentPage * IN_PROGRESS_ITEMS_PER_PAGE
        val end = minOf(start + IN_PROGRESS_ITEMS_PER_PAGE, inProgressGoals.size)

        (start until end).forEach { i ->
            val goal = inProgressGoals[i]
            loadGoalPane(goal, i % IN_PROGRESS_ITEMS_PER_PAGE, inProgressPane1, inProgressPane2)
        }

        inProgressPrevButton.isDisable = inProgressCurrentPage == 0
        inProgressNextButton.isDisable = end >= inProgressGoals.size
    }

    private fun updateDisplayAccomplishedGoals() {
        accomplishedPane1.children.clear()
        accomplishedPane2.children.clear()

        val accomplishedGoals = goals.filter { it.isCompleted() }

        val start = accomplishedCurrentPage * ACCOMPLISHED_ITEMS_PER_PAGE
        val end = minOf(start + ACCOMPLISHED_ITEMS_PER_PAGE, accomplishedGoals.size)

        (start until end).forEach { i ->
            val goal = accomplishedGoals[i]
            loadGoalPane(goal, i % ACCOMPLISHED_ITEMS_PER_PAGE, accomplishedPane1, accomplishedPane2)
        }

        accomplishedPrevButton.isDisable = accomplishedCurrentPage == 0
        accomplishedNextButton.isDisable = end >= accomplishedGoals.size
    }

    private fun loadGoalPane(
        goal: Goal,
        paneIndex: Int,
        pane1: AnchorPane,
        pane2: AnchorPane,
    ) {
        runCatching {
            val loader =
                FXMLLoader(
                    javaClass.getResource(Constants.GOAL_FULL_PANE_FXML),
                    preferencesService.getBundle(),
                )
            loader.setControllerFactory { springContext.getBean(it) }
            val newContent = loader.load<Parent>()

            newContent.stylesheets.add(
                javaClass.getResource(Constants.COMMON_STYLE_SHEET)!!.toExternalForm(),
            )

            val goalFullPaneController = loader.getController<GoalFullPaneController>()
            goalFullPaneController.updateGoalPane(goal)

            newContent.setAnchorPaneConstraints()

            when (paneIndex) {
                0 -> pane1.children.add(newContent)
                1 -> pane2.children.add(newContent)
                else -> logger.error("Invalid pane index: {}", paneIndex)
            }
        }.onFailure { e ->
            logger.error(
                "Error loading goal full pane FXML: '{}' for goal ID: {}",
                Constants.GOAL_FULL_PANE_FXML,
                goal.id,
                e,
            )
            logger.error(
                "Goal details - Name: '{}', Target: {}",
                goal.name,
                goal.targetBalance,
            )
        }
    }

    private fun filterGoalsByStatus(selectedGoalStatus: String): List<Goal> =
        goals.filter { goal ->
            when (selectedGoalStatus) {
                preferencesService.translate(TranslationKeys.GOAL_FILTER_ALL) -> true

                preferencesService.translate(TranslationKeys.GOAL_FILTER_COMPLETED) ->
                    goal.isCompleted() && !goal.isArchived

                preferencesService.translate(TranslationKeys.GOAL_FILTER_ACTIVE) ->
                    !goal.isCompleted() && !goal.isArchived

                preferencesService.translate(TranslationKeys.GOAL_FILTER_ARCHIVED) ->
                    goal.isArchived

                else -> true
            }
        }

    private fun updateGoalTableView() {
        val searchText = goalSearchField.text
        val selectedGoalStatus = statusComboBox.selectionModel.selectedItem

        goalTableView.items.clear()

        val filteredGoals =
            if (searchText.isEmpty()) {
                filterGoalsByStatus(selectedGoalStatus)
            } else {
                filterGoalsByStatus(selectedGoalStatus).filter { goal ->
                    val name = goal.name.lowercase()
                    val initialAmount = goal.initialBalance.toString()
                    val currentAmount = goal.balance.toString()
                    val targetAmount = goal.targetBalance.toString()
                    val targetDate = UIUtils.formatDateForDisplay(goal.targetDate, preferencesService)
                    val completionDate =
                        goal.completionDate?.let {
                            UIUtils.formatDateForDisplay(it, preferencesService)
                        } ?: "-"
                    val status = if (goal.isCompleted()) "completed" else "active"
                    val monthsUntilTarget =
                        Constants.calculateMonthsUntilTarget(LocalDate.now(), goal.targetDate).toString()
                    val recommendedMonthlyDeposit =
                        goal.targetBalance
                            .subtract(goal.balance)
                            .divide(
                                BigDecimal(
                                    Constants
                                        .calculateMonthsUntilTarget(
                                            LocalDate.now(),
                                            goal.targetDate,
                                        ).toLong(),
                                ),
                                2,
                                RoundingMode.HALF_UP,
                            ).toString()

                    val searchLower = searchText.lowercase()
                    name.contains(searchLower) ||
                        initialAmount.contains(searchLower) ||
                        currentAmount.contains(searchLower) ||
                        targetAmount.contains(searchLower) ||
                        targetDate.contains(searchLower) ||
                        monthsUntilTarget.contains(searchLower) ||
                        recommendedMonthlyDeposit.contains(searchLower) ||
                        completionDate.contains(searchLower) ||
                        status.contains(searchLower)
                }
            }

        goalTableView.items.addAll(filteredGoals)
        goalTableView.refresh()
    }

    private fun setButtonsActions() {
        val inProgressGoalsSize = goals.count { !it.isCompleted() && !it.isArchived }

        inProgressPrevButton.setOnAction {
            if (inProgressCurrentPage > 0) {
                inProgressCurrentPage--
                updateDisplayInProgressGoals()
            }
        }

        inProgressNextButton.setOnAction {
            if (inProgressCurrentPage < inProgressGoalsSize / IN_PROGRESS_ITEMS_PER_PAGE) {
                inProgressCurrentPage++
                updateDisplayInProgressGoals()
            }
        }

        val accomplishedGoalsSize = goals.count { it.isCompleted() }

        accomplishedPrevButton.setOnAction {
            if (accomplishedCurrentPage > 0) {
                accomplishedCurrentPage--
                updateDisplayAccomplishedGoals()
            }
        }

        accomplishedNextButton.setOnAction {
            if (accomplishedCurrentPage < accomplishedGoalsSize / ACCOMPLISHED_ITEMS_PER_PAGE) {
                accomplishedCurrentPage++
                updateDisplayAccomplishedGoals()
            }
        }
    }

    private fun configureTableView() {
        val idColumn = createIdColumn()
        val nameColumn = createNameColumn()
        val statusColumn = createStatusColumn()
        val initialAmountColumn = createInitialAmountColumn()
        val currentAmountColumn = createCurrentAmountColumn()
        val targetAmountColumn = createTargetAmountColumn()
        val progressColumn = createProgressColumn()
        val targetDateColumn = createTargetDateColumn()
        val completionDateColumn = createCompletionDateColumn()
        val monthsUntilTargetColumn = createMonthsUntilTargetColumn()
        val recommendedMonthlyDepositColumn = createRecommendedMonthlyDepositColumn()

        goalTableView.columns.addAll(
            idColumn,
            nameColumn,
            statusColumn,
            initialAmountColumn,
            currentAmountColumn,
            targetAmountColumn,
            progressColumn,
            targetDateColumn,
            completionDateColumn,
            monthsUntilTargetColumn,
            recommendedMonthlyDepositColumn,
        )

        goalTableView.setRowFactory {
            TableRow<Goal>().apply {
                itemProperty().addListener { _, _, newItem ->
                    newItem?.let {
                        val tooltip =
                            Tooltip(
                                MessageFormat.format(
                                    preferencesService.translate(
                                        TranslationKeys.GOAL_TABLE_TOOLTIP_MOTIVATION,
                                    ),
                                    it.motivation,
                                ),
                            )
                        Tooltip.install(this, tooltip)
                    }
                }
            }
        }
    }

    private fun createIdColumn(): TableColumn<Goal, Int> =
        TableColumn<Goal, Int>(
            preferencesService.translate(TranslationKeys.GOAL_TABLE_HEADER_ID),
        ).apply {
            setCellValueFactory { SimpleObjectProperty(it.value.id) }
            setCellFactory {
                object : TableCell<Goal, Int>() {
                    override fun updateItem(
                        item: Int?,
                        empty: Boolean,
                    ) {
                        super.updateItem(item, empty)
                        if (item == null || empty) {
                            text = null
                        } else {
                            text = item.toString()
                            alignment = Pos.CENTER
                            style = "-fx-padding: 0;"
                        }
                    }
                }
            }
        }

    private fun createNameColumn(): TableColumn<Goal, String> =
        TableColumn<Goal, String>(
            preferencesService.translate(TranslationKeys.GOAL_TABLE_HEADER_NAME),
        ).apply {
            setCellValueFactory { SimpleObjectProperty(it.value.name) }
        }

    private fun createStatusColumn(): TableColumn<Goal, String> =
        TableColumn<Goal, String>(
            preferencesService.translate(TranslationKeys.GOAL_TABLE_HEADER_STATUS),
        ).apply {
            setCellValueFactory { param ->
                val goal = param.value
                val status =
                    when {
                        goal.isArchived ->
                            preferencesService.translate(TranslationKeys.GOAL_STATUS_ARCHIVED)

                        goal.isCompleted() ->
                            preferencesService.translate(TranslationKeys.GOAL_STATUS_COMPLETED)

                        else ->
                            preferencesService.translate(TranslationKeys.GOAL_STATUS_ACTIVE)
                    }
                SimpleObjectProperty(status)
            }
        }

    private fun createInitialAmountColumn(): TableColumn<Goal, String> =
        TableColumn<Goal, String>(
            preferencesService.translate(TranslationKeys.GOAL_TABLE_HEADER_INITIAL_AMOUNT),
        ).apply {
            setCellValueFactory {
                SimpleObjectProperty(UIUtils.formatCurrency(it.value.initialBalance))
            }
        }

    private fun createCurrentAmountColumn(): TableColumn<Goal, String> =
        TableColumn<Goal, String>(
            preferencesService.translate(TranslationKeys.GOAL_TABLE_HEADER_CURRENT_AMOUNT),
        ).apply {
            setCellValueFactory {
                SimpleObjectProperty(UIUtils.formatCurrency(it.value.balance))
            }
        }

    private fun createTargetAmountColumn(): TableColumn<Goal, String> =
        TableColumn<Goal, String>(
            preferencesService.translate(TranslationKeys.GOAL_TABLE_HEADER_TARGET_AMOUNT),
        ).apply {
            setCellValueFactory {
                SimpleObjectProperty(UIUtils.formatCurrency(it.value.targetBalance))
            }
        }

    private fun createProgressColumn(): TableColumn<Goal, String> =
        TableColumn<Goal, String>(
            preferencesService.translate(TranslationKeys.GOAL_TABLE_HEADER_PROGRESS),
        ).apply {
            setCellValueFactory { param ->
                if (param.value.isCompleted()) {
                    SimpleObjectProperty(UIUtils.formatPercentage(100, preferencesService))
                } else {
                    val progress =
                        if (param.value.balance.isZero()) {
                            BigDecimal.ZERO
                        } else {
                            param.value.balance
                                .divide(param.value.targetBalance, 2, RoundingMode.HALF_UP)
                                .multiply(BigDecimal(100))
                        }
                    SimpleObjectProperty(UIUtils.formatPercentage(progress, preferencesService))
                }
            }
        }

    private fun createTargetDateColumn(): TableColumn<Goal, String> =
        TableColumn<Goal, String>(
            preferencesService.translate(TranslationKeys.GOAL_TABLE_HEADER_TARGET_DATE),
        ).apply {
            setCellValueFactory {
                SimpleStringProperty(
                    UIUtils.formatDateForDisplay(it.value.targetDate, preferencesService),
                )
            }
        }

    private fun createCompletionDateColumn(): TableColumn<Goal, String> =
        TableColumn<Goal, String>(
            preferencesService.translate(TranslationKeys.GOAL_TABLE_HEADER_COMPLETION_DATE),
        ).apply {
            setCellValueFactory { param ->
                if (param.value.isCompleted()) {
                    SimpleStringProperty(
                        UIUtils.formatDateForDisplay(
                            param.value.completionDate,
                            preferencesService,
                        ),
                    )
                } else {
                    SimpleObjectProperty("-")
                }
            }
        }

    private fun createMonthsUntilTargetColumn(): TableColumn<Goal, String> =
        TableColumn<Goal, String>(
            preferencesService.translate(TranslationKeys.GOAL_TABLE_HEADER_MONTHS_UNTIL_TARGET),
        ).apply {
            setCellValueFactory { param ->
                if (param.value.isCompleted()) {
                    SimpleObjectProperty("-")
                } else {
                    val monthsUntilTarget =
                        Constants.calculateMonthsUntilTarget(
                            LocalDate.now(),
                            param.value.targetDate,
                        )
                    SimpleObjectProperty(monthsUntilTarget.toString())
                }
            }
        }

    private fun createRecommendedMonthlyDepositColumn(): TableColumn<Goal, String> =
        TableColumn<Goal, String>(
            preferencesService.translate(
                TranslationKeys.GOAL_TABLE_HEADER_RECOMMENDED_MONTHLY_DEPOSIT,
            ),
        ).apply {
            setCellValueFactory { param ->
                if (param.value.isCompleted()) {
                    SimpleObjectProperty("-")
                } else {
                    val monthsUntilTarget =
                        Constants.calculateMonthsUntilTarget(
                            LocalDate.now(),
                            param.value.targetDate,
                        )

                    val recommendedMonthlyDeposit =
                        if (monthsUntilTarget <= 0) {
                            param.value.targetBalance.subtract(param.value.balance)
                        } else {
                            param.value.targetBalance
                                .subtract(param.value.balance)
                                .divide(
                                    BigDecimal(monthsUntilTarget.toLong()),
                                    2,
                                    RoundingMode.HALF_UP,
                                )
                        }

                    SimpleObjectProperty(UIUtils.formatCurrency(recommendedMonthlyDeposit))
                }
            }
        }

    private fun populateStatusComboBox() {
        statusComboBox.items.addAll(
            preferencesService.translate(TranslationKeys.GOAL_FILTER_ALL),
            preferencesService.translate(TranslationKeys.GOAL_FILTER_ACTIVE),
            preferencesService.translate(TranslationKeys.GOAL_FILTER_COMPLETED),
            preferencesService.translate(TranslationKeys.GOAL_FILTER_ARCHIVED),
        )
        statusComboBox.selectionModel.selectFirst()
    }
}
