/*
 * Filename: SavingsBondsController.kt (original filename: SavingsBondsController.java)
 * Created on: February 18, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 17/03/2026
 */

package org.moinex.ui.main

import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.text.Text
import javafx.util.StringConverter
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.enums.BondType
import org.moinex.model.investment.Bond
import org.moinex.service.PreferencesService
import org.moinex.service.investment.BondService
import org.moinex.ui.dialog.investment.AddBondController
import org.moinex.ui.dialog.investment.AddBondPurchaseController
import org.moinex.ui.dialog.investment.AddBondSaleController
import org.moinex.ui.dialog.investment.ArchivedBondsController
import org.moinex.ui.dialog.investment.BondInterestHistoryController
import org.moinex.ui.dialog.investment.BondTransactionsController
import org.moinex.ui.dialog.investment.EditBondController
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.text.MessageFormat

@Controller
class SavingsBondsController(
    private val bondService: BondService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var bondsTabTotalInvestedField: Text

    @FXML
    private lateinit var bondsTabCurrentValueField: Text

    @FXML
    private lateinit var bondsTabProfitLossField: Text

    @FXML
    private lateinit var bondsTabInterestReceivedField: Text

    @FXML
    private lateinit var bondsTabBondTable: TableView<Bond>

    @FXML
    private lateinit var bondsTabBondSearchField: TextField

    @FXML
    private lateinit var bondsTabBondTypeComboBox: ComboBox<BondType>

    private val currentMonthInterestCache = mutableMapOf<Int, BigDecimal>()
    private val totalAccumulatedInterestCache = mutableMapOf<Int, BigDecimal>()

    companion object {
        private val logger = LoggerFactory.getLogger(SavingsBondsController::class.java)
    }

    @FXML
    fun initialize() {
        configureBondTableView()
        updateBondTableView()
        updateBondTabFields()
        configureBondListeners()
    }

    @FXML
    fun handleRegisterBond() {
        WindowUtils.openModalWindow(
            Constants.ADD_BOND_FXML,
            preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_ADD_BOND_TITLE),
            springContext,
            { _: AddBondController -> },
            listOf(
                Runnable {
                    updateBondTableView()
                    updateBondTabFields()
                },
            ),
        )
    }

    @FXML
    fun handleEditBond() {
        val selectedBond = bondsTabBondTable.selectionModel.selectedItem

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_TITLE),
                preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_EDIT_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.EDIT_BOND_FXML,
            preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_EDIT_BOND_TITLE),
            springContext,
            { controller: EditBondController -> controller.setBond(selectedBond) },
            listOf(
                Runnable {
                    updateBondTableView()
                    updateBondTabFields()
                },
            ),
        )
    }

    @FXML
    fun handleDeleteBond() {
        val selectedBond = bondsTabBondTable.selectionModel.selectedItem

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_TITLE),
                preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_DELETE_MESSAGE),
            )
            return
        }

        if (bondService.getOperationCountByBond(selectedBond.id!!) > 0) {
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_HAS_OPERATIONS_TITLE),
                preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_HAS_OPERATIONS_MESSAGE),
            )
            return
        }

        val confirmed =
            WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_CONFIRM_DELETE_TITLE),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_CONFIRM_DELETE_MESSAGE),
                    selectedBond.name,
                ),
            )

        if (confirmed) {
            runCatching {
                bondService.deleteBond(selectedBond.id!!)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_BOND_DELETED_TITLE),
                    preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_BOND_DELETED_MESSAGE),
                )

                updateBondTableView()
                updateBondTabFields()
            }.onFailure { e ->
                val message =
                    when (e) {
                        is IllegalStateException ->
                            preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_ERROR_DELETING_BOND_TITLE)

                        else -> preferencesService.translate(TranslationKeys.DIALOG_ERROR_TITLE)
                    }
                WindowUtils.showErrorDialog(message, e.message ?: "")
            }
        }
    }

    @FXML
    fun handleOpenBondArchive() {
        WindowUtils.openModalWindow(
            Constants.ARCHIVED_BONDS_FXML,
            preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_BOND_ARCHIVE_TITLE),
            springContext,
            { _: ArchivedBondsController -> },
            listOf(
                Runnable {
                    updateBondTableView()
                    updateBondTabFields()
                },
            ),
        )
    }

    @FXML
    fun handleBuyBond() {
        val selectedBond = bondsTabBondTable.selectionModel.selectedItem

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_TITLE),
                preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_BUY_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.BUY_BOND_FXML,
            preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_BUY_BOND_TITLE),
            springContext,
            { controller: AddBondPurchaseController -> controller.setBond(selectedBond) },
            listOf(
                Runnable {
                    updateBondTableView()
                    updateBondTabFields()
                },
            ),
        )
    }

    @FXML
    fun handleSellBond() {
        val selectedBond = bondsTabBondTable.selectionModel.selectedItem

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_TITLE),
                preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_SELL_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.SALE_BOND_FXML,
            preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_SELL_BOND_TITLE),
            springContext,
            { controller: AddBondSaleController -> controller.setBond(selectedBond) },
            listOf(
                Runnable {
                    updateBondTableView()
                    updateBondTabFields()
                },
            ),
        )
    }

    @FXML
    fun handleShowBondTransactions() {
        WindowUtils.openModalWindow(
            Constants.BOND_TRANSACTIONS_FXML,
            preferencesService.translate(TranslationKeys.BOND_DIALOG_TRANSACTIONS_TITLE),
            springContext,
            { _: BondTransactionsController -> },
            listOf(
                Runnable {
                    updateBondTableView()
                    updateBondTabFields()
                },
            ),
        )
    }

    @FXML
    fun handleShowInterestHistory() {
        val selectedBond = bondsTabBondTable.selectionModel.selectedItem

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_TITLE),
                preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_HISTORY_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.BOND_INTEREST_HISTORY_FXML,
            MessageFormat.format(
                preferencesService.translate(TranslationKeys.BOND_DIALOG_INTEREST_HISTORY_TITLE),
                selectedBond.name,
            ),
            springContext,
            { controller: BondInterestHistoryController -> controller.setBond(selectedBond) },
            listOf(
                Runnable {
                    updateBondTableView()
                    updateBondTabFields()
                },
            ),
        )
    }

    private fun configureBondTableView() {
        bondsTabBondTable.columns.addAll(
            createColumn(
                TranslationKeys.SAVINGS_BONDS_TABLE_HEADER_NAME,
            ) { it.name },
            createColumn(
                TranslationKeys.SAVINGS_BONDS_TABLE_HEADER_SYMBOL,
            ) { it.symbol ?: "" },
            createColumn(
                TranslationKeys.SAVINGS_BONDS_TABLE_HEADER_TYPE,
            ) { UIUtils.translateBondType(it.type, preferencesService) },
            createColumn(
                TranslationKeys.BOND_TABLE_QUANTITY,
            ) { bondService.getCurrentQuantity(it).toString() },
            createColumn(
                TranslationKeys.BOND_TABLE_UNIT_PRICE,
            ) { UIUtils.formatCurrency(bondService.getAverageUnitPrice(it)) },
            createColumn(
                TranslationKeys.SAVINGS_BONDS_TABLE_HEADER_INVESTED_VALUE,
            ) { UIUtils.formatCurrency(bondService.getTotalInvestedValue(it)) },
            createColumn(
                TranslationKeys.SAVINGS_BONDS_TABLE_HEADER_CURRENT_VALUE,
            ) {
                UIUtils.formatCurrency(
                    bondService.getTotalInvestedValue(it).add(
                        bondService.getTotalAccumulatedInterestByBondId(it.id!!),
                    ),
                )
            },
            createColumn(
                TranslationKeys.BOND_MATURITY_DATE,
            ) { it.maturityDate?.let { date -> UIUtils.formatDateForDisplay(date, preferencesService) } ?: "-" },
            createColumn(
                TranslationKeys.BOND_INTEREST_RATE,
            ) { it.interestRate?.let { rate -> "$rate%" } ?: "-" },
            createColumn(
                TranslationKeys.SAVINGS_BONDS_TABLE_HEADER_CURRENT_MONTH_INTEREST,
            ) {
                UIUtils.formatCurrency(
                    currentMonthInterestCache.getOrDefault(it.id, BigDecimal.ZERO),
                )
            },
            createColumn(
                TranslationKeys.SAVINGS_BONDS_TABLE_HEADER_TOTAL_ACCUMULATED_INTEREST,
            ) {
                UIUtils.formatCurrency(
                    totalAccumulatedInterestCache.getOrDefault(it.id, BigDecimal.ZERO),
                )
            },
        )
    }

    private fun createColumn(
        headerKey: String,
        valueExtractor: (Bond) -> String,
    ): TableColumn<Bond, String> =
        TableColumn<Bond, String>(preferencesService.translate(headerKey)).apply {
            setCellValueFactory { SimpleStringProperty(valueExtractor(it.value)) }
        }

    private fun updateBondTableView() {
        val bonds = bondService.getAllNonArchivedBonds()

        currentMonthInterestCache.clear()
        totalAccumulatedInterestCache.clear()

        bonds.forEach { bond ->
            currentMonthInterestCache[bond.id!!] = bondService.getCurrentMonthInterest(bond.id!!)
            totalAccumulatedInterestCache[bond.id!!] = bondService.getTotalAccumulatedInterestByBondId(bond.id!!)
        }

        val filteredBonds =
            bonds
                .filter { bond ->
                    bondsTabBondTypeComboBox.value?.let { bond.type == it } ?: true
                }.filter { bond ->
                    val searchText = bondsTabBondSearchField.text.lowercase()
                    if (searchText.isEmpty()) {
                        true
                    } else {
                        listOf(
                            bond.name.lowercase(),
                            bond.symbol?.lowercase() ?: "",
                            UIUtils.translateBondType(bond.type, preferencesService).lowercase(),
                            bond.issuer?.lowercase() ?: "",
                            bondService.getCurrentQuantity(bond).toString(),
                            bondService.getAverageUnitPrice(bond).toString(),
                            bondService.getTotalInvestedValue(bond).toString(),
                            bondService.getTotalProfit(bond).toString(),
                            bond.maturityDate?.let {
                                UIUtils.formatDateForDisplay(it, preferencesService).lowercase()
                            } ?: "",
                            bond.interestRate?.toString() ?: "",
                        ).any { it.contains(searchText) }
                    }
                }

        bondsTabBondTable.items.setAll(filteredBonds)
    }

    private fun updateBondTabFields() {
        val bonds = bondService.getAllNonArchivedBonds()

        val totalInvested = bondService.getTotalInvestedValue()

        val profitLoss =
            bonds
                .map { bondService.getTotalProfit(it) }
                .fold(BigDecimal.ZERO, BigDecimal::add)

        val interestReceived = bondService.getAllBondsTotalAccumulatedInterest()

        val currentValue = totalInvested.add(interestReceived)

        bondsTabTotalInvestedField.text = UIUtils.formatCurrency(totalInvested)
        bondsTabCurrentValueField.text = UIUtils.formatCurrency(currentValue)
        bondsTabProfitLossField.text = UIUtils.formatCurrencySigned(profitLoss)
        bondsTabInterestReceivedField.text = UIUtils.formatCurrency(interestReceived)
    }

    private fun configureBondListeners() {
        bondsTabBondTypeComboBox.converter =
            object : StringConverter<BondType>() {
                override fun toString(bondType: BondType?): String =
                    bondType?.let {
                        UIUtils.translateBondType(it, preferencesService)
                    } ?: preferencesService.translate(TranslationKeys.SAVINGS_STOCKS_FUNDS_FILTER_ALL)

                override fun fromString(string: String): BondType? = null
            }

        bondsTabBondTypeComboBox.items.apply {
            clear()
            add(null)
            addAll(BondType.entries)
        }
        bondsTabBondTypeComboBox.value = null

        bondsTabBondTypeComboBox.valueProperty().addListener { _, _, _ -> updateBondTableView() }
        bondsTabBondSearchField.textProperty().addListener { _, _, _ -> updateBondTableView() }
    }
}
