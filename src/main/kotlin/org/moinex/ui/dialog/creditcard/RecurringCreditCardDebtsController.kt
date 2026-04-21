/*
 * Filename: RecurringCreditCardDebtsController.kt
 * Created on: April 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.creditcard

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
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.creditcard.RecurringCreditCardDebt
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.RecurringCreditCardDebtService
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller

@Controller
class RecurringCreditCardDebtsController(
    private val recurringCreditCardDebtService: RecurringCreditCardDebtService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var recurringDebtsTableView: TableView<RecurringCreditCardDebt>

    @FXML
    private lateinit var statusComboBox: ComboBox<RecurringTransactionStatus?>

    @FXML
    private lateinit var searchField: TextField

    private var recurringDebts: List<RecurringCreditCardDebt> = emptyList()

    @FXML
    fun initialize() {
        loadData()
        configureTableView()
        populateStatusComboBox()

        statusComboBox.value = RecurringTransactionStatus.ACTIVE
        updateTableView()

        statusComboBox.setOnAction { updateTableView() }
        searchField.textProperty().addListener { _, _, _ -> updateTableView() }
    }

    @FXML
    private fun handleCreate() {
        WindowUtils.openModalWindow(
            Files.ADD_RECURRING_CREDIT_CARD_DEBT_FXML,
            preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_ADD_TITLE),
            springContext,
            { _: AddRecurringCreditCardDebtController -> },
            listOf(
                Runnable {
                    loadData()
                    updateTableView()
                },
            ),
            preferencesService.bundle,
        )
    }

    @FXML
    private fun handleEdit() {
        val selected = recurringDebtsTableView.selectionModel.selectedItem

        if (selected == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_NO_SELECTION_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_NO_SELECTION_EDIT_MESSAGE,
                ),
            )
            return
        }

        WindowUtils.openModalWindow(
            Files.EDIT_RECURRING_CREDIT_CARD_DEBT_FXML,
            preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_EDIT_TITLE),
            springContext,
            { controller: EditRecurringCreditCardDebtController ->
                controller.setRecurringDebt(selected)
            },
            listOf(
                Runnable {
                    loadData()
                    updateTableView()
                },
            ),
            preferencesService.bundle,
        )
    }

    @FXML
    private fun handleDelete() {
        val selected = recurringDebtsTableView.selectionModel.selectedItem

        if (selected == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_NO_SELECTION_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_NO_SELECTION_DELETE_MESSAGE,
                ),
            )
            return
        }

        if (!WindowUtils.showConfirmationDialog(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_DELETE_CONFIRM_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_DELETE_CONFIRM_MESSAGE,
                ),
                preferencesService.bundle,
            )
        ) {
            return
        }

        runCatching {
            recurringCreditCardDebtService.deleteRecurring(selected.id!!)

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_DELETED_TITLE),
                preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_DELETED_MESSAGE),
            )

            loadData()
            updateTableView()
        }.onFailure { e ->
            when (e) {
                is IllegalStateException ->
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_DELETE_MATERIALIZED_TITLE,
                        ),
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_DELETE_MATERIALIZED_MESSAGE,
                        ),
                    )

                else ->
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_ERROR_TITLE),
                        e.message ?: "Unknown error",
                    )
            }
        }
    }

    @FXML
    private fun handleClose() {
        (recurringDebtsTableView.scene.window as Stage).close()
    }

    private fun loadData() {
        recurringDebts = recurringCreditCardDebtService.getAllRecurringDebts()
    }

    private fun updateTableView() {
        val query = searchField.text.lowercase()
        val selectedStatus = statusComboBox.value

        recurringDebtsTableView.items.clear()

        recurringDebts
            .filter { selectedStatus == null || it.status == selectedStatus }
            .filter { recurring ->
                if (query.isEmpty()) return@filter true
                val desc = recurring.description?.lowercase() ?: ""
                val card = recurring.creditCard.name.lowercase()
                val cat = recurring.category.name.lowercase()
                val id = recurring.id.toString()
                desc.contains(query) || card.contains(query) || cat.contains(query) || id.contains(query)
            }.forEach { recurringDebtsTableView.items.add(it) }

        recurringDebtsTableView.refresh()
    }

    private fun configureTableView() {
        val idColumn =
            TableColumn<RecurringCreditCardDebt, Int>(
                preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_TABLE_ID),
            ).apply {
                setCellValueFactory { SimpleObjectProperty(it.value.id) }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val creditCardColumn =
            TableColumn<RecurringCreditCardDebt, String>(
                preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_TABLE_CREDIT_CARD),
            ).apply {
                setCellValueFactory { SimpleStringProperty(it.value.creditCard.name) }
            }

        val descriptionColumn =
            TableColumn<RecurringCreditCardDebt, String>(
                preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_TABLE_DESCRIPTION),
            ).apply {
                setCellValueFactory { SimpleStringProperty(it.value.description) }
            }

        val amountColumn =
            TableColumn<RecurringCreditCardDebt, String>(
                preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_TABLE_AMOUNT),
            ).apply {
                setCellValueFactory {
                    SimpleObjectProperty(UIUtils.formatCurrency(it.value.amount))
                }
            }

        val categoryColumn =
            TableColumn<RecurringCreditCardDebt, String>(
                preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_TABLE_CATEGORY),
            ).apply {
                setCellValueFactory { SimpleStringProperty(it.value.category.name) }
            }

        val frequencyColumn =
            TableColumn<RecurringCreditCardDebt, String>(
                preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_TABLE_FREQUENCY),
            ).apply {
                setCellValueFactory {
                    SimpleStringProperty(UIUtils.translateCreditCardRecurringFrequency(it.value.frequency))
                }
            }

        val dayOfMonthColumn =
            TableColumn<RecurringCreditCardDebt, Int>(
                preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_TABLE_DAY_OF_MONTH),
            ).apply {
                setCellValueFactory { SimpleObjectProperty(it.value.dayOfMonth) }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val startDateColumn =
            TableColumn<RecurringCreditCardDebt, String>(
                preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_TABLE_START_DATE),
            ).apply {
                setCellValueFactory {
                    SimpleStringProperty(UIUtils.formatDateForDisplay(it.value.startDate))
                }
            }

        val endDateColumn =
            TableColumn<RecurringCreditCardDebt, String>(
                preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_TABLE_END_DATE),
            ).apply {
                setCellValueFactory {
                    SimpleStringProperty(UIUtils.formatDateForDisplay(it.value.endDate))
                }
                setCellFactory {
                    object : TableCell<RecurringCreditCardDebt, String>() {
                        override fun updateItem(
                            item: String?,
                            empty: Boolean,
                        ) {
                            super.updateItem(item, empty)
                            if (empty || tableRow == null || tableRow.item == null) {
                                text = null
                            } else {
                                val recurring = tableRow.item
                                text =
                                    if (recurring.endDate == Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE) {
                                        preferencesService.translate(
                                            TranslationKeys.CREDIT_CARD_RECURRING_TABLE_INDEFINITE,
                                        )
                                    } else {
                                        item
                                    }
                            }
                        }
                    }
                }
            }

        val statusColumn =
            TableColumn<RecurringCreditCardDebt, String>(
                preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_TABLE_STATUS),
            ).apply {
                setCellValueFactory {
                    SimpleStringProperty(UIUtils.translateRecurringTransactionStatus(it.value.status))
                }
            }

        val nextDueDateColumn =
            TableColumn<RecurringCreditCardDebt, String>(
                preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_TABLE_NEXT_INVOICE),
            ).apply {
                setCellValueFactory {
                    SimpleStringProperty(UIUtils.formatFullMonthYear(it.value.nextInvoiceMonth))
                }
            }

        recurringDebtsTableView.columns.addAll(
            idColumn,
            creditCardColumn,
            descriptionColumn,
            amountColumn,
            categoryColumn,
            frequencyColumn,
            statusColumn,
            startDateColumn,
            endDateColumn,
            nextDueDateColumn,
            dayOfMonthColumn,
        )
    }

    private fun populateStatusComboBox() {
        val statusesWithNull = FXCollections.observableArrayList<RecurringTransactionStatus?>()
        statusesWithNull.add(null)
        statusesWithNull.addAll(RecurringTransactionStatus.entries)

        statusComboBox.items = statusesWithNull

        statusComboBox.converter =
            object : StringConverter<RecurringTransactionStatus?>() {
                override fun toString(status: RecurringTransactionStatus?): String =
                    if (status != null) {
                        UIUtils.translateRecurringTransactionStatus(status)
                    } else {
                        preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_COMBOBOX_ALL)
                    }

                override fun fromString(string: String): RecurringTransactionStatus? = null
            }
    }
}
