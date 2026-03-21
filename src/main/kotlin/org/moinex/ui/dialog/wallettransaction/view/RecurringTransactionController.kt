/*
 * Filename: RecurringTransactionController.kt (original filename: RecurringTransactionController.java)
 * Created on: November 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.view

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.ComboBox
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.stage.Stage
import javafx.util.StringConverter
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.model.wallettransaction.RecurringTransaction
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.ui.dialog.wallettransaction.create.AddRecurringTransactionController
import org.moinex.ui.dialog.wallettransaction.update.EditRecurringTransactionController
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal

@Controller
class RecurringTransactionController(
    private val recurringTransactionService: RecurringTransactionService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var recurringTransactionTableView: TableView<RecurringTransaction>

    @FXML
    private lateinit var statusComboBox: ComboBox<RecurringTransactionStatus?>

    @FXML
    private lateinit var searchField: TextField

    private var recurringTransactions: List<RecurringTransaction> = emptyList()

    @FXML
    fun initialize() {
        loadRecurringTransactionsFromDatabase()
        configureTableView()
        populateRecurringTransactionStatusComboBox()

        statusComboBox.value = RecurringTransactionStatus.ACTIVE
        updateRecurringTransactionTableView()

        statusComboBox.setOnAction { updateRecurringTransactionTableView() }

        searchField.textProperty().addListener { _, _, _ ->
            updateRecurringTransactionTableView()
        }
    }

    @FXML
    private fun handleCreate() {
        WindowUtils.openModalWindow(
            Constants.ADD_RECURRING_TRANSACTION_FXML,
            preferencesService.translate(
                TranslationKeys.WALLETTRANSACTION_DIALOG_CREATE_RECURRING_TRANSACTION_TITLE,
            ),
            springContext,
            { _: AddRecurringTransactionController -> },
            listOf(
                Runnable {
                    loadRecurringTransactionsFromDatabase()
                    updateRecurringTransactionTableView()
                },
            ),
        )
    }

    @FXML
    private fun handleEdit() {
        val selectedRt = recurringTransactionTableView.selectionModel.selectedItem

        if (selectedRt == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_NO_RECURRING_SELECTED_TITLE),
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_DIALOG_NO_RECURRING_SELECTED_EDIT_MESSAGE,
                ),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.EDIT_RECURRING_TRANSACTION_FXML,
            preferencesService.translate(
                TranslationKeys.WALLETTRANSACTION_DIALOG_EDIT_RECURRING_TRANSACTION_TITLE,
            ),
            springContext,
            { controller: EditRecurringTransactionController ->
                controller.setRecurringTransaction(selectedRt)
            },
            listOf(
                Runnable {
                    loadRecurringTransactionsFromDatabase()
                    updateRecurringTransactionTableView()
                },
            ),
        )
    }

    @FXML
    private fun handleDelete() {
        val selectedRecurringTransaction = recurringTransactionTableView.selectionModel.selectedItem

        if (selectedRecurringTransaction == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_NO_RECURRING_SELECTED_TITLE),
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_DIALOG_NO_RECURRING_SELECTED_DELETE_MESSAGE,
                ),
            )
            return
        }

        if (WindowUtils.showConfirmationDialog(
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_DIALOG_REMOVE_RECURRING_TRANSACTION_TITLE,
                ) + " " + selectedRecurringTransaction.id,
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_DIALOG_REMOVE_RECURRING_TRANSACTION_MESSAGE,
                ),
                preferencesService.bundle,
            )
        ) {
            recurringTransactionService.deleteRecurringTransaction(selectedRecurringTransaction.id!!)
            loadRecurringTransactionsFromDatabase()
            updateRecurringTransactionTableView()
        }
    }

    @FXML
    private fun handleCancel() {
        val stage = searchField.scene.window as Stage
        stage.close()
    }

    private fun loadRecurringTransactionsFromDatabase() {
        recurringTransactions = recurringTransactionService.getAllRecurringTransactions()
    }

    private fun updateRecurringTransactionTableView() {
        val similarTextOrId = searchField.text.lowercase()
        val selectedStatus = statusComboBox.value

        recurringTransactionTableView.items.clear()

        if (similarTextOrId.isEmpty()) {
            recurringTransactions
                .filter { selectedStatus == null || it.status == selectedStatus }
                .forEach { recurringTransactionTableView.items.add(it) }
        } else {
            recurringTransactions
                .filter { selectedStatus == null || it.status == selectedStatus }
                .filter { rt ->
                    val description = rt.description?.lowercase() ?: ""
                    val id = rt.id.toString()
                    val category = rt.category.name.lowercase()
                    val wallet = rt.wallet.name.lowercase()
                    val type = rt.type.name.lowercase()
                    val frequency = rt.frequency.name.lowercase()
                    val amount = UIUtils.formatCurrency(rt.amount)

                    description.contains(similarTextOrId) ||
                        id.contains(similarTextOrId) ||
                        category.contains(similarTextOrId) ||
                        wallet.contains(similarTextOrId) ||
                        type.contains(similarTextOrId) ||
                        frequency.contains(similarTextOrId) ||
                        amount.contains(similarTextOrId)
                }.forEach { recurringTransactionTableView.items.add(it) }
        }

        recurringTransactionTableView.refresh()
    }

    private fun configureTableView() {
        val idColumn =
            TableColumn<RecurringTransaction, Int>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_ID),
            ).apply {
                setCellValueFactory { SimpleObjectProperty(it.value.id) }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val descriptionColumn =
            TableColumn<RecurringTransaction, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_DESCRIPTION),
            ).apply {
                setCellValueFactory { SimpleStringProperty(it.value.description) }
            }

        val amountColumn =
            TableColumn<RecurringTransaction, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_AMOUNT),
            ).apply {
                setCellValueFactory {
                    SimpleObjectProperty(UIUtils.formatCurrency(it.value.amount))
                }
            }

        val walletColumn =
            TableColumn<RecurringTransaction, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_WALLET),
            ).apply {
                setCellValueFactory { SimpleStringProperty(it.value.wallet.name) }
            }

        val typeColumn =
            TableColumn<RecurringTransaction, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_TYPE),
            ).apply {
                setCellValueFactory {
                    SimpleStringProperty(UIUtils.translateTransactionType(it.value.type))
                }
            }

        val categoryColumn =
            TableColumn<RecurringTransaction, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_CATEGORY),
            ).apply {
                setCellValueFactory { SimpleStringProperty(it.value.category.name) }
            }

        val statusColumn =
            TableColumn<RecurringTransaction, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_STATUS),
            ).apply {
                setCellValueFactory {
                    SimpleStringProperty(UIUtils.translateRecurringTransactionStatus(it.value.status))
                }
            }

        val frequencyColumn =
            TableColumn<RecurringTransaction, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_FREQUENCY),
            ).apply {
                setCellValueFactory {
                    SimpleStringProperty(UIUtils.translateRecurringTransactionFrequency(it.value.frequency))
                }
            }

        val startDateColumn =
            TableColumn<RecurringTransaction, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_START_DATE),
            ).apply {
                setCellValueFactory {
                    SimpleStringProperty(UIUtils.formatDateForDisplay(it.value.startDate))
                }
            }

        val endDateColumn =
            TableColumn<RecurringTransaction, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_END_DATE),
            ).apply {
                setCellValueFactory {
                    SimpleStringProperty(UIUtils.formatDateForDisplay(it.value.endDate))
                }
                setCellFactory {
                    object : TableCell<RecurringTransaction, String>() {
                        override fun updateItem(
                            item: String?,
                            empty: Boolean,
                        ) {
                            super.updateItem(item, empty)
                            if (empty || tableRow == null || tableRow.item == null) {
                                text = null
                            } else {
                                val rt = tableRow.item
                                text =
                                    if (rt.endDate == Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE) {
                                        preferencesService.translate(
                                            TranslationKeys.WALLETTRANSACTION_TABLE_INDEFINITE,
                                        )
                                    } else {
                                        item
                                    }
                            }
                        }
                    }
                }
            }

        val nextDueDateColumn =
            TableColumn<RecurringTransaction, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_NEXT_DUE_DATE),
            ).apply {
                setCellValueFactory {
                    if (it.value.status == RecurringTransactionStatus.INACTIVE) {
                        SimpleStringProperty("-")
                    } else {
                        SimpleStringProperty(UIUtils.formatDateForDisplay(it.value.nextDueDate))
                    }
                }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val expectedRemainingAmountColumn =
            TableColumn<RecurringTransaction, String>(
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_TABLE_EXPECTED_REMAINING_AMOUNT,
                ),
            ).apply {
                setCellValueFactory { param ->
                    val expectedRemainingAmount =
                        runCatching {
                            recurringTransactionService.getExpectedRemainingAmountFromRecurringTransaction(
                                param.value.id!!,
                            )
                        }.getOrElse { BigDecimal.ZERO }

                    if (expectedRemainingAmount == null) {
                        SimpleStringProperty("-")
                    } else {
                        SimpleStringProperty(UIUtils.formatCurrency(expectedRemainingAmount))
                    }
                }
            }

        recurringTransactionTableView.columns.addAll(
            idColumn,
            descriptionColumn,
            amountColumn,
            walletColumn,
            typeColumn,
            categoryColumn,
            statusColumn,
            frequencyColumn,
            startDateColumn,
            endDateColumn,
            nextDueDateColumn,
            expectedRemainingAmountColumn,
        )
    }

    private fun populateRecurringTransactionStatusComboBox() {
        val transactionTypesWithNull =
            FXCollections.observableArrayList<RecurringTransactionStatus?>()
        transactionTypesWithNull.add(null)
        transactionTypesWithNull.addAll(RecurringTransactionStatus.entries)

        statusComboBox.items = transactionTypesWithNull

        statusComboBox.converter =
            object : StringConverter<RecurringTransactionStatus?>() {
                override fun toString(transactionType: RecurringTransactionStatus?): String =
                    if (transactionType != null) {
                        UIUtils.translateRecurringTransactionStatus(transactionType)
                    } else {
                        preferencesService.translate(TranslationKeys.WALLETTRANSACTION_COMBOBOX_ALL)
                    }

                override fun fromString(string: String): RecurringTransactionStatus? {
                    if (string == preferencesService.translate(TranslationKeys.WALLETTRANSACTION_COMBOBOX_ALL)) {
                        return null
                    }

                    return RecurringTransactionStatus.entries.find {
                        UIUtils.translateRecurringTransactionStatus(it) == string
                    }
                }
            }
    }
}
