/*
 * Filename: BondTransactionsController.kt (original filename: BondTransactionsController.java)
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.view

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Files
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isAmountOnly
import org.moinex.common.ui.BondTableColumns
import org.moinex.common.ui.BondTableColumns.asQuantityColumn
import org.moinex.common.ui.BondTableColumns.asUnitPriceColumn
import org.moinex.common.ui.RowAction
import org.moinex.common.ui.RowContextMenu
import org.moinex.common.ui.closeDialog
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.enums.OperationType
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondOperation
import org.moinex.service.PreferencesService
import org.moinex.service.investment.BondService
import org.moinex.ui.dialog.investment.update.EditBondPurchaseController
import org.moinex.ui.dialog.investment.update.EditBondSaleController
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller

@Controller
class BondTransactionsController(
    private val bondService: BondService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var operationTableView: TableView<BondOperation>

    @FXML
    private lateinit var searchField: TextField

    @FXML
    private lateinit var closeButton: Button

    private var operations: MutableList<BondOperation> = mutableListOf()
    private var filterBond: Bond? = null
    private var year: Int? = null

    @FXML
    fun initialize() {
        RowContextMenu.install(
            operationTableView,
            listOf(
                RowAction(
                    preferencesService.translate(TranslationKeys.DIALOG_BUTTON_DELETE),
                ) { handleDeleteOperation() },
                RowAction(preferencesService.translate(TranslationKeys.DIALOG_BUTTON_EDIT)) { handleEditOperation() },
            ),
        )
        loadOperationsFromDatabase()
        configureOperationTableView()
        updateOperationTableView()

        searchField.textProperty().addListener { _, _, _ ->
            updateOperationTableView()
        }
    }

    @FXML
    private fun handleCancel() {
        searchField.closeDialog()
    }

    @FXML
    private fun handleEditOperation() {
        val selectedOperation = operationTableView.selectionModel.selectedItem

        if (selectedOperation == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.BOND_DIALOG_NO_OPERATION_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.BOND_DIALOG_NO_OPERATION_SELECTED_EDIT_MESSAGE),
            )
            return
        }

        if (selectedOperation.operationType == OperationType.BUY) {
            WindowUtils.openModalWindow(
                Files.EDIT_BOND_PURCHASE_FXML,
                preferencesService.translate(TranslationKeys.BOND_DIALOG_EDIT_PURCHASE_TITLE),
                springContext,
                { controller: EditBondPurchaseController ->
                    controller.setOperation(selectedOperation)
                },
                listOf(
                    Runnable {
                        loadOperationsFromDatabase()
                        updateOperationTableView()
                    },
                ),
            )
        } else {
            WindowUtils.openModalWindow(
                Files.EDIT_BOND_SALE_FXML,
                preferencesService.translate(TranslationKeys.BOND_DIALOG_EDIT_SALE_TITLE),
                springContext,
                { controller: EditBondSaleController ->
                    controller.setOperation(selectedOperation)
                },
                listOf(
                    Runnable {
                        loadOperationsFromDatabase()
                        updateOperationTableView()
                    },
                ),
            )
        }
    }

    @FXML
    private fun handleDeleteOperation() {
        val selectedOperation = operationTableView.selectionModel.selectedItem

        if (selectedOperation == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.BOND_DIALOG_NO_OPERATION_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.BOND_DIALOG_NO_OPERATION_SELECTED_DELETE_MESSAGE),
            )
            return
        }

        val operationType = UIUtils.translateOperationType(selectedOperation.operationType)
        val bondName = selectedOperation.bond.name
        val symbol = selectedOperation.bond.symbol
        val bondDisplay = bondName + if (!symbol.isNullOrBlank()) " ($symbol)" else ""

        val message =
            preferencesService
                .translate(TranslationKeys.BOND_DIALOG_CONFIRM_DELETE_OPERATION_MESSAGE)
                .replace("{operationType}", operationType)
                .replace("{bond}", bondDisplay)

        if (WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.BOND_DIALOG_CONFIRM_DELETE_OPERATION_TITLE),
                message,
                preferencesService.bundle,
                danger = true,
            )
        ) {
            runCatching {
                bondService.deleteBondOperation(selectedOperation.id!!)
                loadOperationsFromDatabase()
                updateOperationTableView()
            }.onFailure { e ->
                WindowUtils.showErrorDialog(
                    preferencesService.translate(TranslationKeys.DIALOG_ERROR_TITLE),
                    e.message ?: "",
                )
            }
        }
    }

    /**
     * Shows only the operations of [bond], inside another window: the columns of the bond and the close button, which
     * that window already has, are hidden and the table takes the width it is given.
     */
    fun setBond(bond: Bond) {
        filterBond = bond

        val bondColumns =
            listOf(
                preferencesService.translate(TranslationKeys.BOND_TABLE_BOND),
                preferencesService.translate(TranslationKeys.BOND_TABLE_TYPE),
            )
        operationTableView.columns.filter { it.text in bondColumns }.forEach { it.isVisible = false }
        operationTableView.minWidth = 0.0
        closeButton.isVisible = false
        closeButton.isManaged = false

        loadOperationsFromDatabase()
        updateOperationTableView()
    }

    /**
     * The years that have an operation, newest first.
     */
    fun years(): List<Int> = operations.map { it.localDate.year }.distinct().sortedDescending()

    /**
     * Shows only the operations of [selected], or all of them when it is null.
     */
    fun showYear(selected: Int?) {
        year = selected
        updateOperationTableView()
    }

    private fun loadOperationsFromDatabase() {
        operations =
            (filterBond?.let { bondService.getOperationsByBond(it) } ?: bondService.getAllOperations()).toMutableList()
    }

    private fun updateOperationTableView() {
        val similarTextOrId = searchField.text.lowercase()
        val inYear = operations.filter { year == null || it.localDate.year == year }

        operationTableView.items.clear()

        if (similarTextOrId.isEmpty()) {
            operationTableView.items.setAll(inYear)
        } else {
            inYear
                .filter { op ->
                    val id = op.id.toString()
                    val bondName = op.bond.name.lowercase()
                    val bondSymbol = op.bond.symbol?.lowercase() ?: ""
                    val date = UIUtils.formatDateForDisplay(op.walletTransaction!!.date)
                    val quantity = op.quantity.toString()
                    val unitPrice = op.unitPrice.toString()
                    val fees = op.fees.toString()
                    val taxes = op.taxes.toString()
                    val operationType = op.operationType.name.lowercase()
                    val walletName =
                        op.walletTransaction
                            ?.wallet
                            ?.name
                            ?.lowercase() ?: ""

                    id.contains(similarTextOrId) ||
                        bondName.contains(similarTextOrId) ||
                        bondSymbol.contains(similarTextOrId) ||
                        date.contains(similarTextOrId) ||
                        quantity.contains(similarTextOrId) ||
                        unitPrice.contains(similarTextOrId) ||
                        fees.contains(similarTextOrId) ||
                        taxes.contains(similarTextOrId) ||
                        operationType.contains(similarTextOrId) ||
                        walletName.contains(similarTextOrId)
                }.forEach { operationTableView.items.add(it) }
        }

        operationTableView.refresh()
    }

    private fun configureOperationTableView() {
        val idColumn =
            TableColumn<BondOperation, Int>(
                preferencesService.translate(TranslationKeys.BOND_TABLE_ID),
            ).apply {
                setCellValueFactory { param -> SimpleObjectProperty(param.value.id) }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val operationTypeColumn =
            TableColumn<BondOperation, String>(
                preferencesService.translate(TranslationKeys.BOND_TABLE_OPERATION_TYPE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(UIUtils.translateOperationType(param.value.operationType))
                }
            }

        val bondNameColumn =
            TableColumn<BondOperation, String>(
                preferencesService.translate(TranslationKeys.BOND_TABLE_BOND),
            ).apply {
                setCellValueFactory { param ->
                    val symbol = param.value.bond.symbol
                    SimpleStringProperty(
                        param.value.bond.name + if (!symbol.isNullOrBlank()) " ($symbol)" else "",
                    )
                }
            }

        val bondTypeColumn =
            TableColumn<BondOperation, String>(
                preferencesService.translate(TranslationKeys.BOND_TABLE_TYPE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(UIUtils.translateBondType(param.value.bond.type))
                }
            }

        val dateColumn =
            TableColumn<BondOperation, String>(
                preferencesService.translate(TranslationKeys.BOND_TABLE_DATE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(UIUtils.formatDateForDisplay(param.value.walletTransaction!!.date))
                }
            }

        val quantityColumn =
            TableColumn<BondOperation, String>(
                preferencesService.translate(TranslationKeys.BOND_TABLE_QUANTITY_OR_AMOUNT),
            ).apply {
                setCellValueFactory { param ->
                    val operation = param.value
                    SimpleObjectProperty(
                        if (operation.isAmountOnly()) {
                            UIUtils.formatCurrency(operation.quantity)
                        } else {
                            operation.quantity.toString()
                        },
                    )
                }
            }

        val unitPriceColumn =
            TableColumn<BondOperation, String>(
                preferencesService.translate(TranslationKeys.BOND_TABLE_UNIT_PRICE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleObjectProperty(
                        if (param.value.isAmountOnly()) "—" else UIUtils.formatCurrency(param.value.unitPrice),
                    )
                }
            }

        val feesColumn =
            TableColumn<BondOperation, String>(
                preferencesService.translate(TranslationKeys.BOND_TABLE_FEES),
            ).apply {
                setCellValueFactory { param ->
                    SimpleObjectProperty(UIUtils.formatCurrency(param.value.fees))
                }
            }

        val taxesColumn =
            TableColumn<BondOperation, String>(
                preferencesService.translate(TranslationKeys.BOND_TABLE_TAXES),
            ).apply {
                setCellValueFactory { param ->
                    SimpleObjectProperty(UIUtils.formatCurrency(param.value.taxes))
                }
            }

        val profitLossColumn =
            TableColumn<BondOperation, String>(
                preferencesService.translate(TranslationKeys.BOND_TABLE_PROFIT_LOSS),
            ).apply {
                setCellValueFactory { param ->
                    val op = param.value
                    if (op.operationType == OperationType.BUY) {
                        SimpleObjectProperty(Constants.NA_DATA)
                    } else {
                        SimpleObjectProperty(UIUtils.formatCurrencySigned(op.netProfit))
                    }
                }
                setCellFactory {
                    object : TableCell<BondOperation, String>() {
                        override fun updateItem(
                            item: String?,
                            empty: Boolean,
                        ) {
                            super.updateItem(item, empty)
                            if (item == null || empty) {
                                text = null
                                style = ""
                            } else {
                                text = item
                                styleClass.removeAll(
                                    Styles.POSITIVE_BALANCE_STYLE,
                                    Styles.NEGATIVE_BALANCE_STYLE,
                                    Styles.NEUTRAL_BALANCE_STYLE,
                                )

                                if (item != Constants.NA_DATA) {
                                    when {
                                        item.startsWith("+") -> styleClass.add(Styles.POSITIVE_BALANCE_STYLE)
                                        item.startsWith("-") -> styleClass.add(Styles.NEGATIVE_BALANCE_STYLE)
                                        else -> styleClass.add(Styles.NEUTRAL_BALANCE_STYLE)
                                    }
                                }
                            }
                        }
                    }
                }
            }

        val walletNameColumn =
            TableColumn<BondOperation, String>(
                preferencesService.translate(TranslationKeys.BOND_TABLE_WALLET),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(
                        if (param.value.walletTransaction != null) {
                            param.value.walletTransaction!!
                                .wallet.name
                        } else {
                            ""
                        },
                    )
                }
            }

        operationTableView.columns.addAll(
            idColumn,
            operationTypeColumn,
            bondNameColumn,
            bondTypeColumn,
            dateColumn,
            quantityColumn.asQuantityColumn(),
            unitPriceColumn.asUnitPriceColumn(),
            feesColumn,
            taxesColumn,
            profitLossColumn,
            walletNameColumn,
        )

        BondTableColumns.hideWhenAllHeldInReais(operationTableView) { it.isAmountOnly() }
    }
}
