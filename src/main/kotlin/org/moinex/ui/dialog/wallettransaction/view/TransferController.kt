/*
 * Filename: TransferController.kt (original filename: TransferController.java)
 * Created on: October  4, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.view

import jakarta.persistence.EntityNotFoundException
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.DatePicker
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.wallettransaction.Transfer
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.wallettransaction.create.AddTransferController
import org.moinex.ui.dialog.wallettransaction.update.EditTransferController
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.text.MessageFormat
import java.time.LocalDate

@Controller
class TransferController(
    private val walletService: WalletService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var transfersTableView: TableView<Transfer>

    @FXML
    private lateinit var searchField: TextField

    @FXML
    private lateinit var transfersEndDatePicker: DatePicker

    @FXML
    private lateinit var transfersStartDatePicker: DatePicker

    private val masterData = FXCollections.observableArrayList<Transfer>()
    private lateinit var filteredData: FilteredList<Transfer>

    @FXML
    fun initialize() {
        loadTransfersFromDatabase()
        configureTableView()
        configureDatePickers()
        setupFilterListeners()

        filteredData = FilteredList(masterData) { true }
        updateTableView()
    }

    @FXML
    private fun handleAdd() {
        WindowUtils.openModalWindow(
            Files.ADD_TRANSFER_FXML,
            preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_ADD_NEW_TRANSFER_TITLE),
            springContext,
            { _: AddTransferController -> },
            listOf(
                Runnable {
                    loadTransfersFromDatabase()
                    updateTableView()
                },
            ),
        )
    }

    @FXML
    private fun handleEdit() {
        val selectedTransfer = transfersTableView.selectionModel.selectedItem
        if (selectedTransfer == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_NO_TRANSFER_SELECTED_TITLE),
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_DIALOG_NO_TRANSFER_SELECTED_EDIT_MESSAGE,
                ),
            )
            return
        }

        WindowUtils.openModalWindow(
            Files.EDIT_TRANSFER_FXML,
            preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EDIT_TRANSFER_TITLE),
            springContext,
            { controller: EditTransferController -> controller.setTransfer(selectedTransfer) },
            listOf(Runnable { loadTransfersFromDatabase() }),
        )
    }

    @FXML
    private fun handleDelete() {
        val selectedTransfer = transfersTableView.selectionModel.selectedItem
        if (selectedTransfer == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_NO_TRANSFER_SELECTED_TITLE),
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_DIALOG_NO_TRANSFER_SELECTED_DELETE_MESSAGE,
                ),
            )
            return
        }

        if (WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_DELETE_TRANSFER_TITLE),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_DELETE_TRANSFER_MESSAGE),
                    selectedTransfer.id,
                ),
                preferencesService.bundle,
            )
        ) {
            runCatching {
                walletService.deleteTransfer(selectedTransfer.id!!)
                loadTransfersFromDatabase()
                updateTableView()
            }.onFailure { e ->
                if (e is EntityNotFoundException) {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_ERROR_TITLE),
                        e.message ?: "Unknown error",
                    )
                } else {
                    throw e
                }
            }
        }
    }

    @FXML
    private fun handleCancel() {
        val stage = searchField.scene.window as Stage
        stage.close()
    }

    private fun configureDatePickers() {
        UIUtils.setDatePickerFormat(transfersStartDatePicker)
        UIUtils.setDatePickerFormat(transfersEndDatePicker)

        transfersStartDatePicker.value = LocalDate.now().withDayOfMonth(1)
        transfersEndDatePicker.value = LocalDate.now().plusMonths(1).withDayOfMonth(1)
    }

    private fun loadTransfersFromDatabase() {
        masterData.setAll(walletService.getAllTransfers())
    }

    private fun setupFilterListeners() {
        searchField.textProperty().addListener { _, _, _ -> updateTableView() }
        transfersStartDatePicker.valueProperty().addListener { _, _, _ -> updateTableView() }
        transfersEndDatePicker.valueProperty().addListener { _, _, _ -> updateTableView() }
    }

    private fun updateTableView() {
        val searchText = searchField.text.lowercase()
        val startDate = transfersStartDatePicker.value
        val endDate = transfersEndDatePicker.value

        filteredData.setPredicate { transfer ->
            val matchesSearch =
                searchText.isEmpty() ||
                    transfer.description?.lowercase()?.contains(searchText) ?: true ||
                    transfer.senderWallet.name
                        .lowercase()
                        .contains(searchText) ||
                    transfer.receiverWallet.name
                        .lowercase()
                        .contains(searchText) ||
                    transfer.id.toString().contains(searchText)

            var matchesDate = true
            val transferDate = transfer.date.toLocalDate()
            if (startDate != null && transferDate.isBefore(startDate)) {
                matchesDate = false
            }
            if (endDate != null && transferDate.isAfter(endDate)) {
                matchesDate = false
            }

            matchesSearch && matchesDate
        }

        transfersTableView.items = filteredData
    }

    private fun configureTableView() {
        val idColumn =
            TableColumn<Transfer, Int>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_ID),
            ).apply {
                setCellValueFactory { SimpleObjectProperty(it.value.id) }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val dateColumn =
            TableColumn<Transfer, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_DATE),
            ).apply {
                setCellValueFactory {
                    SimpleStringProperty(UIUtils.formatDateForDisplay(it.value.date))
                }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val descriptionColumn =
            TableColumn<Transfer, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_DESCRIPTION),
            ).apply {
                setCellValueFactory { SimpleStringProperty(it.value.description) }
            }

        val amountColumn =
            TableColumn<Transfer, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_AMOUNT),
            ).apply {
                setCellValueFactory {
                    SimpleStringProperty(UIUtils.formatCurrency(it.value.amount))
                }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val senderColumn =
            TableColumn<Transfer, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_SENDER),
            ).apply {
                setCellValueFactory { SimpleStringProperty(it.value.senderWallet.name) }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val receiverColumn =
            TableColumn<Transfer, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_RECEIVER),
            ).apply {
                setCellValueFactory { SimpleStringProperty(it.value.receiverWallet.name) }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val categoryColumn =
            TableColumn<Transfer, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_CATEGORY),
            ).apply {
                setCellValueFactory {
                    SimpleStringProperty(it.value.category?.name ?: "-")
                }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        transfersTableView.columns.addAll(
            idColumn,
            dateColumn,
            descriptionColumn,
            amountColumn,
            senderColumn,
            receiverColumn,
            categoryColumn,
        )
    }
}
