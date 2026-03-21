/*
 * Filename: BondInterestHistoryController.kt (original filename: BondInterestHistoryController.java)
 * Created on: February 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.view

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextInputDialog
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondInterestCalculation
import org.moinex.service.PreferencesService
import org.moinex.service.investment.BondInterestCalculationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.text.MessageFormat

@Controller
class BondInterestHistoryController(
    private val bondInterestCalculationService: BondInterestCalculationService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var bondNameLabel: Label

    @FXML
    private lateinit var bondTypeLabel: Label

    @FXML
    private lateinit var bondInterestTypeLabel: Label

    @FXML
    private lateinit var historyTable: TableView<BondInterestCalculation>

    @FXML
    private lateinit var editButton: Button

    @FXML
    private lateinit var resetButton: Button

    @FXML
    private lateinit var recalculateButton: Button

    @FXML
    private lateinit var closeButton: Button

    private lateinit var bond: Bond

    companion object {
        private val logger = LoggerFactory.getLogger(BondInterestHistoryController::class.java)
    }

    fun setBond(b: Bond) {
        bond = b
        initialize()
    }

    @FXML
    private fun initialize() {
        if (!::bond.isInitialized) {
            return
        }

        setupBondInfo()
        setupTableColumns()
        loadHistoryData()
        setupTableListeners()
    }

    private fun setupBondInfo() {
        bondNameLabel.text = bond.name
        bondTypeLabel.text = bond.type.toString()
        bondInterestTypeLabel.text = "${bond.interestType} - ${bond.interestIndex}"
    }

    private fun setupTableColumns() {
        historyTable.columns.clear()

        val monthColumn =
            TableColumn<BondInterestCalculation, String>(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_TABLE_MONTH),
            ).apply {
                setCellValueFactory { cellData ->
                    SimpleStringProperty(cellData.value.referenceMonth.toString())
                }
            }

        val quantityColumn =
            TableColumn<BondInterestCalculation, String>(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_TABLE_QUANTITY),
            ).apply {
                setCellValueFactory { cellData ->
                    val quantity = cellData.value.quantity
                    SimpleStringProperty(UIUtils.formatCurrency(quantity))
                }
            }

        val investedAmountColumn =
            TableColumn<BondInterestCalculation, String>(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_TABLE_INVESTED_AMOUNT),
            ).apply {
                setCellValueFactory { cellData ->
                    val investedAmount = cellData.value.investedAmount
                    SimpleStringProperty(UIUtils.formatCurrency(investedAmount))
                }
            }

        val monthlyInterestColumn =
            TableColumn<BondInterestCalculation, String>(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_TABLE_MONTHLY_INTEREST),
            ).apply {
                setCellValueFactory { cellData ->
                    val monthlyInterest = cellData.value.monthlyInterest
                    SimpleStringProperty(UIUtils.formatCurrency(monthlyInterest))
                }
            }

        val accumulatedInterestColumn =
            TableColumn<BondInterestCalculation, String>(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_TABLE_ACCUMULATED_INTEREST),
            ).apply {
                setCellValueFactory { cellData ->
                    val accumulatedInterest = cellData.value.accumulatedInterest
                    SimpleStringProperty(UIUtils.formatCurrency(accumulatedInterest))
                }
            }

        val finalValueColumn =
            TableColumn<BondInterestCalculation, String>(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_TABLE_FINAL_VALUE),
            ).apply {
                setCellValueFactory { cellData ->
                    val finalValue = cellData.value.finalValue
                    SimpleStringProperty(UIUtils.formatCurrency(finalValue))
                }
            }

        val statusColumn =
            TableColumn<BondInterestCalculation, String>(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_TABLE_STATUS),
            ).apply {
                setCellValueFactory { cellData ->
                    if (cellData.value.isManuallyAdjusted()) {
                        SimpleStringProperty(
                            preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_STATUS_ADJUSTED),
                        )
                    } else {
                        SimpleStringProperty(
                            preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_STATUS_AUTOMATIC),
                        )
                    }
                }
            }

        historyTable.columns.addAll(
            monthColumn,
            quantityColumn,
            investedAmountColumn,
            monthlyInterestColumn,
            accumulatedInterestColumn,
            finalValueColumn,
            statusColumn,
        )
    }

    private fun loadHistoryData() {
        val history = bondInterestCalculationService.getMonthlyInterestHistory(bond)
        val observableList = FXCollections.observableArrayList(history)
        historyTable.items = observableList
    }

    private fun setupTableListeners() {
        historyTable.selectionModel.selectedItemProperty().addListener { _, _, newVal ->
            val hasSelection = newVal != null
            editButton.isDisable = !hasSelection
            resetButton.isDisable = !hasSelection || !newVal.isManuallyAdjusted()
        }

        editButton.isDisable = true
        resetButton.isDisable = true
    }

    @FXML
    private fun handleEditInterest() {
        val selected = historyTable.selectionModel.selectedItem
        if (selected == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_NO_MONTH_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_NO_MONTH_SELECTED_MESSAGE),
            )
            return
        }

        val dialog = TextInputDialog(selected.monthlyInterest.toString())
        dialog.title = preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_EDIT_INTEREST_TITLE)
        dialog.headerText =
            MessageFormat.format(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_EDIT_INTEREST_HEADER),
                selected.referenceMonth.toString(),
            )
        dialog.contentText =
            preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_EDIT_INTEREST_CONTENT)

        val result = dialog.showAndWait()
        if (result.isEmpty) {
            return
        }

        runCatching {
            val newValue = BigDecimal(result.get())

            if (newValue < BigDecimal.ZERO) {
                WindowUtils.showErrorDialog(
                    preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_INVALID_VALUE_TITLE),
                    preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_INVALID_VALUE_MESSAGE),
                )
                return
            }

            bondInterestCalculationService.adjustMonthlyInterest(
                bond.id!!,
                selected.referenceMonth,
                newValue,
            )

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_INTEREST_ADJUSTED_TITLE),
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_INTEREST_ADJUSTED_MESSAGE),
            )

            loadHistoryData()
            logger.info(
                "Interest adjusted for bond {} month {}: {}",
                bond.name,
                selected.referenceMonth,
                newValue,
            )
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_INVALID_NUMBER_TITLE),
                        preferencesService.translate(
                            TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_INVALID_NUMBER_MESSAGE,
                        ),
                    )
                    logger.warn("Invalid number format for interest adjustment", e)
                }
                else -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_ERROR_ADJUSTING_TITLE,
                        ),
                        e.message ?: "",
                    )
                    logger.error("Error adjusting interest", e)
                }
            }
        }
    }

    @FXML
    private fun handleResetToAutomatic() {
        val selected = historyTable.selectionModel.selectedItem
        if (selected == null || !selected.isManuallyAdjusted()) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_NO_ADJUSTED_MONTH_TITLE),
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_NO_ADJUSTED_MONTH_MESSAGE),
            )
            return
        }

        if (!WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_CONFIRM_RESET_HEADER),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_CONFIRM_RESET_MESSAGE),
                    selected.referenceMonth.toString(),
                ),
            )
        ) {
            return
        }

        runCatching {
            bondInterestCalculationService.resetToAutomaticCalculation(
                bond.id!!,
                selected.referenceMonth,
            )

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_INTEREST_RESET_TITLE),
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_INTEREST_RESET_MESSAGE),
            )

            loadHistoryData()
            logger.info(
                "Interest reset to automatic for bond {} month {}",
                bond.name,
                selected.referenceMonth,
            )
        }.onFailure { e ->
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_ERROR_RESETTING_TITLE),
                e.message ?: "",
            )
            logger.error("Error resetting interest to automatic", e)
        }
    }

    @FXML
    private fun handleRecalculateHistory() {
        if (!WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_CONFIRM_RECALCULATE_HEADER),
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_CONFIRM_RECALCULATE_MESSAGE),
            )
        ) {
            return
        }

        runCatching {
            bondInterestCalculationService.recalculateAllMonthlyInterest(bond.id!!)

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_HISTORY_RECALCULATED_TITLE),
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_HISTORY_RECALCULATED_MESSAGE),
            )

            loadHistoryData()
            logger.info("Interest history recalculated for bond {}", bond.name)
        }.onFailure { e ->
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_ERROR_RECALCULATING_TITLE),
                e.message ?: "",
            )
            logger.error("Error recalculating interest history", e)
        }
    }

    @FXML
    private fun handleClose() {
        closeButton.scene.window.hide()
    }
}
