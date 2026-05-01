/*
 * Filename: ArchivedTickersController.kt (original filename: ArchivedTickersController.java)
 * Created on: January 10, 2025
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
import javafx.scene.image.ImageView
import javafx.stage.Stage
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.investment.Ticker
import org.moinex.service.PreferencesService
import org.moinex.service.investment.TickerService
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.text.MessageFormat

@Controller
class ArchivedTickersController(
    private val tickerService: TickerService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var tickerTableView: TableView<Ticker>

    @FXML
    private lateinit var searchField: TextField

    private var archivedTickers: MutableList<Ticker> = mutableListOf()

    @FXML
    fun initialize() {
        loadArchivedTickersFromDatabase()
        configureTableView()
        updateTickerTableView()

        searchField.textProperty().addListener { _, _, _ ->
            updateTickerTableView()
        }
    }

    @FXML
    private fun handleUnarchive() {
        val selectedTicker = tickerTableView.selectionModel.selectedItem

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_TICKER_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_TICKER_SELECTED_UNARCHIVE),
            )
            return
        }

        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_CONFIRM_UNARCHIVE_TICKER_TITLE),
                    "${selectedTicker.name} (${selectedTicker.symbol})",
                ),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_CONFIRM_UNARCHIVE_TICKER_MESSAGE),
                preferencesService.bundle,
            )
        ) {
            runCatching {
                tickerService.unarchiveTicker(selectedTicker.id!!)
                archivedTickers.remove(selectedTicker)
                updateTickerTableView()
            }.onFailure { e ->
                if (e is EntityNotFoundException) {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_ERROR_UNARCHIVING_TICKER_TITLE),
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
        val selectedTicker = tickerTableView.selectionModel.selectedItem

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_TICKER_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_TICKER_SELECTED_DELETE),
            )
            return
        }

        if (tickerService.getTransactionCountByTicker(selectedTicker.id!!) > 0) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_TICKER_HAS_TRANSACTIONS_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_TICKER_HAS_TRANSACTIONS_MESSAGE),
            )
            return
        }

        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_CONFIRM_DELETE_TICKER_TITLE),
                    "${selectedTicker.name} (${selectedTicker.symbol})",
                ),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_CONFIRM_DELETE_TICKER_MESSAGE),
                preferencesService.bundle,
            )
        ) {
            runCatching {
                tickerService.deleteTicker(selectedTicker.id!!)
                archivedTickers.remove(selectedTicker)
                updateTickerTableView()
            }.onFailure { e ->
                if (e is EntityNotFoundException || e is IllegalStateException) {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_ERROR_DELETING_TICKER_TITLE),
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

    private fun loadArchivedTickersFromDatabase() {
        archivedTickers = tickerService.getAllArchivedTickers().toMutableList()
    }

    private fun updateTickerTableView() {
        val similarTextOrId = searchField.text.lowercase()

        tickerTableView.items.clear()

        if (similarTextOrId.isEmpty()) {
            tickerTableView.items.setAll(archivedTickers)
        } else {
            archivedTickers
                .filter { t ->
                    val name = t.name.lowercase()
                    val symbol = t.symbol.lowercase()
                    val type = t.type.toString().lowercase()
                    val quantity = t.currentQuantity.toString()
                    val unitValue = t.currentUnitValue.toString()
                    val totalValue = t.currentQuantity.multiply(t.currentUnitValue).toString()
                    val avgPrice = t.averageUnitValue.toString()

                    name.contains(similarTextOrId) ||
                        symbol.contains(similarTextOrId) ||
                        type.contains(similarTextOrId) ||
                        quantity.contains(similarTextOrId) ||
                        unitValue.contains(similarTextOrId) ||
                        totalValue.contains(similarTextOrId) ||
                        avgPrice.contains(similarTextOrId)
                }.forEach { tickerTableView.items.add(it) }
        }

        tickerTableView.refresh()
    }

    private fun configureTableView() {
        val idColumn =
            TableColumn<Ticker, Int>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_ID),
            ).apply {
                setCellValueFactory { param -> SimpleObjectProperty(param.value.id) }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val logoColumn =
            TableColumn<Ticker, ImageView>("").apply {
                setCellValueFactory { param ->
                    val logo = UIUtils.loadTickerLogo(param.value, 32.0)
                    SimpleObjectProperty(logo)
                }
                prefWidth = 50.0
                maxWidth = 50.0
                minWidth = 50.0
                isResizable = false
                style = "-fx-alignment: CENTER;"
            }

        val nameColumn =
            TableColumn<Ticker, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_NAME),
            ).apply {
                setCellValueFactory { param -> SimpleStringProperty(param.value.name) }
            }

        val symbolColumn =
            TableColumn<Ticker, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_SYMBOL),
            ).apply {
                setCellValueFactory { param -> SimpleStringProperty(param.value.symbol) }
            }

        val typeColumn =
            TableColumn<Ticker, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_TYPE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(UIUtils.translateAssetType(param.value.type))
                }
            }

        val quantityColumn =
            TableColumn<Ticker, BigDecimal>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_QUANTITY_OWNED),
            ).apply {
                setCellValueFactory { param -> SimpleObjectProperty(param.value.currentQuantity) }
            }

        val unitColumn =
            TableColumn<Ticker, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_UNIT_PRICE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleObjectProperty(UIUtils.formatCurrencyDynamic(param.value.currentUnitValue))
                }
            }

        val totalColumn =
            TableColumn<Ticker, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_TOTAL_VALUE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleObjectProperty(
                        UIUtils.formatCurrencyDynamic(
                            param.value.currentQuantity.multiply(param.value.currentUnitValue),
                        ),
                    )
                }
            }

        val avgUnitColumn =
            TableColumn<Ticker, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_AVERAGE_UNIT_PRICE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleObjectProperty(UIUtils.formatCurrencyDynamic(param.value.averageUnitValue))
                }
            }

        tickerTableView.columns.addAll(
            idColumn,
            logoColumn,
            nameColumn,
            symbolColumn,
            typeColumn,
            quantityColumn,
            unitColumn,
            totalColumn,
            avgUnitColumn,
        )
    }
}
