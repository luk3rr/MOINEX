/*
 * Filename: ArchivedBondsController.kt (original filename: ArchivedBondsController.java)
 * Created on: January  3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.view

import jakarta.persistence.EntityNotFoundException
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.investment.Bond
import org.moinex.service.PreferencesService
import org.moinex.service.investment.BondService
import org.springframework.stereotype.Controller
import java.text.MessageFormat

@Controller
class ArchivedBondsController(
    private val bondService: BondService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var bondTableView: TableView<Bond>

    @FXML
    private lateinit var searchField: TextField

    private var archivedBonds: MutableList<Bond> = mutableListOf()

    @FXML
    fun initialize() {
        loadArchivedBondsFromDatabase()
        configureTableView()
        updateBondTableView()

        searchField.textProperty().addListener { _, _, _ ->
            updateBondTableView()
        }
    }

    @FXML
    private fun handleUnarchive() {
        val selectedBond = bondTableView.selectionModel.selectedItem

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.BOND_DIALOG_NO_BOND_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.BOND_DIALOG_NO_BOND_SELECTED_UNARCHIVE),
            )
            return
        }

        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.BOND_DIALOG_CONFIRM_UNARCHIVE_TITLE),
                    selectedBond.name,
                ),
                preferencesService.translate(TranslationKeys.BOND_DIALOG_CONFIRM_UNARCHIVE_MESSAGE),
                preferencesService.bundle,
            )
        ) {
            runCatching {
                bondService.unarchiveBond(selectedBond.id!!)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.BOND_DIALOG_BOND_UNARCHIVED_TITLE),
                    MessageFormat.format(
                        preferencesService.translate(TranslationKeys.BOND_DIALOG_BOND_UNARCHIVED_MESSAGE),
                        selectedBond.name,
                    ),
                )

                archivedBonds.remove(selectedBond)
                updateBondTableView()
            }.onFailure { e ->
                if (e is EntityNotFoundException) {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.BOND_DIALOG_ERROR_UNARCHIVING_TITLE),
                        e.message ?: "",
                    )
                } else {
                    throw e
                }
            }
        }
    }

    @FXML
    private fun handleDelete() {
        val selectedBond = bondTableView.selectionModel.selectedItem

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.BOND_DIALOG_NO_BOND_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.BOND_DIALOG_NO_BOND_SELECTED_DELETE),
            )
            return
        }

        if (bondService.getOperationCountByBond(selectedBond.id!!) > 0) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.BOND_DIALOG_HAS_OPERATIONS_TITLE),
                preferencesService.translate(TranslationKeys.BOND_DIALOG_HAS_OPERATIONS_MESSAGE),
            )
            return
        }

        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.BOND_DIALOG_CONFIRM_DELETE_TITLE),
                    selectedBond.name,
                ),
                preferencesService.translate(TranslationKeys.BOND_DIALOG_CONFIRM_DELETE_MESSAGE),
                preferencesService.bundle,
            )
        ) {
            runCatching {
                bondService.deleteBond(selectedBond.id!!)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.BOND_DIALOG_BOND_DELETED_TITLE),
                    MessageFormat.format(
                        preferencesService.translate(TranslationKeys.BOND_DIALOG_BOND_DELETED_MESSAGE),
                        selectedBond.name,
                    ),
                )

                archivedBonds.remove(selectedBond)
                updateBondTableView()
            }.onFailure { e ->
                if (e is EntityNotFoundException || e is IllegalStateException) {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.BOND_DIALOG_ERROR_DELETING_TITLE),
                        e.message ?: "",
                    )
                } else {
                    throw e
                }
            }
        }
    }

    @FXML
    private fun handleCancel() {
        (searchField.scene.window as Stage).close()
    }

    private fun loadArchivedBondsFromDatabase() {
        archivedBonds = bondService.getAllArchivedBonds().toMutableList()
    }

    private fun updateBondTableView() {
        val searchText = searchField.text.lowercase()

        bondTableView.items.clear()

        if (searchText.isEmpty()) {
            bondTableView.items.setAll(archivedBonds)
        } else {
            archivedBonds
                .filter { b ->
                    val name = b.name.lowercase()
                    val symbol = b.symbol?.lowercase() ?: ""
                    val type = b.type.toString().lowercase()
                    val issuer = b.issuer?.lowercase() ?: ""

                    name.contains(searchText) ||
                        symbol.contains(searchText) ||
                        type.contains(searchText) ||
                        issuer.contains(searchText)
                }.forEach { bondTableView.items.add(it) }
        }

        bondTableView.refresh()
    }

    private fun configureTableView() {
        val idColumn =
            TableColumn<Bond, Int>(
                preferencesService.translate(TranslationKeys.BOND_TABLE_ID),
            ).apply {
                setCellValueFactory { param -> SimpleObjectProperty(param.value.id) }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val nameColumn = TableColumn<Bond, String>(preferencesService.translate(TranslationKeys.BOND_TABLE_NAME))
        nameColumn.setCellValueFactory { param -> SimpleStringProperty(param.value.name) }

        val symbolColumn = TableColumn<Bond, String>(preferencesService.translate(TranslationKeys.BOND_TABLE_SYMBOL))
        symbolColumn.setCellValueFactory { param -> SimpleStringProperty(param.value.symbol) }

        val typeColumn = TableColumn<Bond, String>(preferencesService.translate(TranslationKeys.BOND_TABLE_TYPE))
        typeColumn.setCellValueFactory { param ->
            SimpleStringProperty(UIUtils.translateBondType(param.value.type))
        }

        val issuerColumn = TableColumn<Bond, String>(preferencesService.translate(TranslationKeys.BOND_TABLE_ISSUER))
        issuerColumn.setCellValueFactory { param -> SimpleStringProperty(param.value.issuer) }

        val quantityColumn =
            TableColumn<Bond, String>(preferencesService.translate(TranslationKeys.BOND_TABLE_QUANTITY))
        quantityColumn.setCellValueFactory { param ->
            val bond = param.value
            val quantity = bondService.getCurrentQuantity(bond)
            SimpleStringProperty(quantity.toString())
        }

        val avgPriceColumn =
            TableColumn<Bond, String>(preferencesService.translate(TranslationKeys.BOND_TABLE_UNIT_PRICE))
        avgPriceColumn.setCellValueFactory { param ->
            val bond = param.value
            val avgPrice = bondService.getAverageUnitPrice(bond)
            SimpleStringProperty(UIUtils.formatCurrency(avgPrice))
        }

        val investedValueColumn =
            TableColumn<Bond, String>(preferencesService.translate(TranslationKeys.BOND_TABLE_INVESTED_VALUE))
        investedValueColumn.setCellValueFactory { param ->
            val bond = param.value
            val invested = bondService.getTotalInvestedValue(bond)
            SimpleStringProperty(UIUtils.formatCurrency(invested))
        }

        val maturityDateColumn =
            TableColumn<Bond, String>(preferencesService.translate(TranslationKeys.BOND_MATURITY_DATE))
        maturityDateColumn.setCellValueFactory { param ->
            val bond = param.value
            if (bond.maturityDate != null) {
                SimpleStringProperty(UIUtils.formatDateForDisplay(bond.maturityDate))
            } else {
                SimpleStringProperty("-")
            }
        }

        bondTableView.columns.add(idColumn)
        bondTableView.columns.add(nameColumn)
        bondTableView.columns.add(symbolColumn)
        bondTableView.columns.add(typeColumn)
        bondTableView.columns.add(issuerColumn)
        bondTableView.columns.add(quantityColumn)
        bondTableView.columns.add(avgPriceColumn)
        bondTableView.columns.add(investedValueColumn)
        bondTableView.columns.add(maturityDateColumn)
    }
}
