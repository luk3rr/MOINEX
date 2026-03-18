/*
 * Filename: SavingsStocksFundsController.kt (original filename: SavingsStocksFundsController.java)
 * Created on: February 18, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 17/03/2026
 */

package org.moinex.ui.main

import com.jfoenix.controls.JFXButton
import jakarta.persistence.EntityNotFoundException
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
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.text.Text
import javafx.util.Callback
import javafx.util.StringConverter
import org.moinex.common.extension.isValidForFundamentalAnalysis
import org.moinex.model.enums.AssetType
import org.moinex.model.investment.Dividend
import org.moinex.model.investment.Ticker
import org.moinex.service.PreferencesService
import org.moinex.service.investment.TickerService
import org.moinex.ui.dialog.investment.AddCryptoExchangeController
import org.moinex.ui.dialog.investment.AddDividendController
import org.moinex.ui.dialog.investment.AddTickerController
import org.moinex.ui.dialog.investment.AddTickerPurchaseController
import org.moinex.ui.dialog.investment.AddTickerSaleController
import org.moinex.ui.dialog.investment.ArchivedTickersController
import org.moinex.ui.dialog.investment.EditTickerController
import org.moinex.ui.dialog.investment.FundamentalAnalysisController
import org.moinex.ui.dialog.investment.InvestmentTransactionsController
import org.moinex.util.Constants
import org.moinex.util.FxUtils
import org.moinex.util.UIUtils
import org.moinex.util.WindowUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.text.MessageFormat

@Controller
class SavingsStocksFundsController(
    private val tickerService: TickerService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var stocksFundsTabNetCapitalInvestedField: Text

    @FXML
    private lateinit var stocksFundsTabCurrentValueField: Text

    @FXML
    private lateinit var stocksFundsTabProfitLossField: Text

    @FXML
    private lateinit var stocksFundsTabDividendsReceivedField: Text

    @FXML
    private lateinit var stocksFundsTabTickerTable: TableView<Ticker>

    @FXML
    private lateinit var stocksFundsTabTickerSearchField: TextField

    @FXML
    private lateinit var stocksFundsTabTickerTypeComboBox: ComboBox<AssetType>

    @FXML
    private lateinit var updatePortfolioPricesButton: JFXButton

    @FXML
    private lateinit var updatePricesButtonIcon: ImageView

    private lateinit var tickers: List<Ticker>
    private lateinit var dividends: List<Dividend>
    private var isUpdatingPortfolioPrices = false

    companion object {
        private val logger = LoggerFactory.getLogger(SavingsStocksFundsController::class.java)
    }

    @FXML
    fun initialize() {
        configureTableView()
        populateTickerTypeComboBox()

        updateTransactionTableView()
        updatePortfolioIndicators()

        if (isUpdatingPortfolioPrices) {
            setOffUpdatePortfolioPricesButton()
        } else {
            setOnUpdatePortfolioPricesButton()
        }

        configureListeners()
    }

    @FXML
    fun handleRegisterTicker() {
        WindowUtils.openModalWindow(
            Constants.ADD_TICKER_FXML,
            preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_ADD_TICKER_TITLE),
            springContext,
            { _: AddTickerController -> },
            listOf(Runnable { updatePortfolioIndicators() }),
        )
    }

    @FXML
    fun handleBuyTicker() {
        val selectedTicker = stocksFundsTabTickerTable.selectionModel.selectedItem

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_BUY_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.BUY_TICKER_FXML,
            preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_BUY_TICKER_TITLE),
            springContext,
            { controller: AddTickerPurchaseController -> controller.setTicker(selectedTicker) },
            listOf(Runnable { updatePortfolioIndicators() }),
        )
    }

    @FXML
    fun handleSellTicker() {
        val selectedTicker = stocksFundsTabTickerTable.selectionModel.selectedItem

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_SELL_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.SALE_TICKER_FXML,
            preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_SELL_TICKER_TITLE),
            springContext,
            { controller: AddTickerSaleController -> controller.setTicker(selectedTicker) },
            listOf(Runnable { updatePortfolioIndicators() }),
        )
    }

    @FXML
    fun handleAddDividend() {
        val selectedTicker = stocksFundsTabTickerTable.selectionModel.selectedItem

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_ADD_DIVIDEND_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.ADD_DIVIDEND_FXML,
            preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_ADD_DIVIDEND_TITLE),
            springContext,
            { controller: AddDividendController -> controller.setTicker(selectedTicker) },
            listOf(Runnable { updatePortfolioIndicators() }),
        )
    }

    @FXML
    fun handleAddCryptoExchange() {
        val selectedTicker = stocksFundsTabTickerTable.selectionModel.selectedItem

        WindowUtils.openModalWindow(
            Constants.ADD_CRYPTO_EXCHANGE_FXML,
            preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_ADD_CRYPTO_EXCHANGE_TITLE),
            springContext,
            { controller: AddCryptoExchangeController ->
                selectedTicker?.let { controller.setFromCryptoComboBox(it) }
            },
            listOf(Runnable { updatePortfolioIndicators() }),
        )
    }

    @FXML
    fun handleOpenTickerArchive() {
        WindowUtils.openModalWindow(
            Constants.ARCHIVED_TICKERS_FXML,
            preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_TICKER_ARCHIVE_TITLE),
            springContext,
            { _: ArchivedTickersController -> },
            listOf(Runnable { updatePortfolioIndicators() }),
        )
    }

    @FXML
    fun handleShowTransactions() {
        WindowUtils.openModalWindow(
            Constants.INVESTMENT_TRANSACTIONS_FXML,
            preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_INVESTMENT_TRANSACTIONS_TITLE),
            springContext,
            { _: InvestmentTransactionsController -> },
            listOf(Runnable { updatePortfolioIndicators() }),
        )
    }

    @FXML
    fun handleUpdatePortfolioPrices() {
        setOffUpdatePortfolioPricesButton()

        FxUtils.launchBackgroundThenUI(
            background = {
                runCatching {
                    tickerService.updateTickersPriceFromApi(stocksFundsTabTickerTable.items)
                }
            },
            onUI = { result ->
                result
                    .onSuccess { failed ->
                        when {
                            failed.isEmpty() -> {
                                WindowUtils.showSuccessDialog(
                                    preferencesService.translate(
                                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_SUCCESS_TITLE,
                                    ),
                                    preferencesService.translate(
                                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_SUCCESS_MESSAGE,
                                    ),
                                )
                            }

                            failed.size == stocksFundsTabTickerTable.items.size -> {
                                WindowUtils.showInformationDialog(
                                    preferencesService.translate(
                                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_ERROR_TITLE,
                                    ),
                                    preferencesService.translate(
                                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_ERROR_ALL_FAILED,
                                    ),
                                )
                            }

                            else -> {
                                val failedSymbols = failed.joinToString(", ") { it.symbol }
                                WindowUtils.showInformationDialog(
                                    preferencesService.translate(
                                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_ERROR_TITLE,
                                    ),
                                    preferencesService.translate(
                                        Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_ERROR_SOME_FAILED,
                                    ) + "\n$failedSymbols",
                                )
                            }
                        }
                    }.onFailure { e ->
                        WindowUtils.showErrorDialog(
                            preferencesService.translate(Constants.TranslationKeys.DIALOG_ERROR_TITLE),
                            e.message ?: "",
                        )
                    }

                setOnUpdatePortfolioPricesButton()
                updatePortfolioIndicators()
            },
        )
    }

    @FXML
    fun handleEditTicker() {
        val selectedTicker = stocksFundsTabTickerTable.selectionModel.selectedItem

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_EDIT_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.EDIT_TICKER_FXML,
            preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_EDIT_TICKER_TITLE),
            springContext,
            { controller: EditTickerController -> controller.setTicker(selectedTicker) },
            listOf(Runnable { updatePortfolioIndicators() }),
        )
    }

    @FXML
    fun handleViewFundamentalAnalysis() {
        val selectedTicker = stocksFundsTabTickerTable.selectionModel.selectedItem

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_MESSAGE),
            )
            return
        }

        if (!selectedTicker.isValidForFundamentalAnalysis()) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_ERROR_INVALID_TICKER_TYPE_TITLE),
                preferencesService.translate(Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_ERROR_INVALID_TICKER_TYPE_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.FUNDAMENTAL_ANALYSIS_FXML,
            MessageFormat.format(
                preferencesService.translate(Constants.TranslationKeys.FUNDAMENTAL_ANALYSIS_DIALOG_TITLE),
                selectedTicker.symbol,
            ),
            springContext,
            { controller: FundamentalAnalysisController ->
                controller.setTicker(selectedTicker)
                controller.loadAnalysis()
            },
            emptyList(),
        )
    }

    @FXML
    fun handleDeleteTicker() {
        val selectedTicker = stocksFundsTabTickerTable.selectionModel.selectedItem

        if (selectedTicker == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE),
                preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_DELETE_MESSAGE),
            )
            return
        }

        if (tickerService.getTransactionCountByTicker(selectedTicker.id!!) > 0) {
            WindowUtils.showErrorDialog(
                preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_HAS_TRANSACTIONS_TITLE),
                preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_HAS_TRANSACTIONS_MESSAGE),
            )
            return
        }

        val confirmed =
            WindowUtils.showConfirmationDialog(
                "${preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_DIALOG_CONFIRMATION_DELETE_TITLE)} " +
                    "${selectedTicker.name} (${selectedTicker.symbol})",
                "",
                preferencesService.getBundle(),
            )

        if (confirmed) {
            runCatching {
                tickerService.deleteTicker(selectedTicker.id!!)
                updatePortfolioIndicators()
            }.onFailure { e ->
                when (e) {
                    is EntityNotFoundException, is IllegalStateException -> {
                        WindowUtils.showErrorDialog(
                            preferencesService.translate(Constants.TranslationKeys.DIALOG_ERROR_TITLE),
                            e.message ?: "",
                        )
                    }

                    else -> throw e
                }
            }
        }
    }

    private fun loadTickersFromDatabase() {
        tickers = tickerService.getAllNonArchivedTickers()
    }

    private fun loadDividendsFromDatabase() {
        dividends = tickerService.getAllDividends()
    }

    private fun updateNetCapitalInvestedField() {
        val netCapitalInvested =
            tickers
                .map { it.averageUnitValue.multiply(it.currentQuantity) }
                .fold(BigDecimal.ZERO, BigDecimal::add)

        stocksFundsTabNetCapitalInvestedField.text = UIUtils.formatCurrency(netCapitalInvested)
    }

    private fun updateCurrentValueField() {
        val currentValue =
            tickers
                .map { it.currentQuantity.multiply(it.currentUnitValue) }
                .fold(BigDecimal.ZERO, BigDecimal::add)

        stocksFundsTabCurrentValueField.text = UIUtils.formatCurrency(currentValue)
    }

    private fun updateProfitLossField() {
        val netCapitalInvested =
            tickers
                .map { it.averageUnitValue.multiply(it.currentQuantity) }
                .fold(BigDecimal.ZERO, BigDecimal::add)

        val currentValue =
            tickers
                .map { it.currentQuantity.multiply(it.currentUnitValue) }
                .fold(BigDecimal.ZERO, BigDecimal::add)

        val profitLoss = currentValue.subtract(netCapitalInvested)

        stocksFundsTabProfitLossField.text = UIUtils.formatCurrency(profitLoss)
    }

    private fun updateDividendsReceivedField() {
        val dividendsReceived =
            dividends
                .map { it.walletTransaction!!.amount }
                .fold(BigDecimal.ZERO, BigDecimal::add)

        stocksFundsTabDividendsReceivedField.text = UIUtils.formatCurrency(dividendsReceived)
    }

    private fun updatePortfolioIndicators() {
        loadTickersFromDatabase()
        loadDividendsFromDatabase()

        updateNetCapitalInvestedField()
        updateCurrentValueField()
        updateProfitLossField()
        updateDividendsReceivedField()
        updateTransactionTableView()
    }

    private fun updateTransactionTableView() {
        val similarTextOrId = stocksFundsTabTickerSearchField.text.lowercase()
        val selectedTickerType = stocksFundsTabTickerTypeComboBox.value

        val filteredTickers =
            tickerService
                .getAllNonArchivedTickers()
                .filter { ticker ->
                    selectedTickerType?.let { ticker.type == it } ?: true
                }.filter { ticker ->
                    if (similarTextOrId.isEmpty()) {
                        true
                    } else {
                        listOf(
                            ticker.name.lowercase(),
                            ticker.symbol.lowercase(),
                            ticker.type.toString().lowercase(),
                            ticker.currentQuantity.toString(),
                            ticker.currentUnitValue.toString(),
                            ticker.currentQuantity.multiply(ticker.currentUnitValue).toString(),
                            ticker.averageUnitValue.toString(),
                        ).any { it.contains(similarTextOrId) }
                    }
                }

        stocksFundsTabTickerTable.items.setAll(filteredTickers)
        stocksFundsTabTickerTable.refresh()
    }

    private fun populateTickerTypeComboBox() {
        val tickerTypesWithNull = FXCollections.observableArrayList(AssetType.entries)
        tickerTypesWithNull.addFirst(null)

        stocksFundsTabTickerTypeComboBox.items = tickerTypesWithNull

        stocksFundsTabTickerTypeComboBox.converter =
            object : StringConverter<AssetType>() {
                override fun toString(tickerType: AssetType?): String =
                    tickerType?.let {
                        UIUtils.translateAssetType(it, preferencesService)
                    } ?: preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_FILTER_ALL)

                override fun fromString(string: String): AssetType? =
                    if (string == preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_FILTER_ALL)) {
                        null
                    } else {
                        AssetType.valueOf(string)
                    }
            }
    }

    private fun configureListeners() {
        stocksFundsTabTickerSearchField.textProperty().addListener { _, _, _ -> updateTransactionTableView() }
        stocksFundsTabTickerTypeComboBox.valueProperty().addListener { _, _, _ -> updateTransactionTableView() }
    }

    private fun configureTableView() {
        stocksFundsTabTickerTable.columns.addAll(
            createIdColumn(),
            createLogoColumn(),
            createStringColumn(
                Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_TABLE_HEADER_NAME,
                Pos.CENTER_LEFT,
            ) { it.name },
            createStringColumn(
                Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_TABLE_HEADER_SYMBOL,
                Pos.CENTER_LEFT,
            ) { it.symbol },
            createStringColumn(
                Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_TABLE_HEADER_TYPE,
                Pos.CENTER_LEFT,
            ) { UIUtils.translateAssetType(it.type, preferencesService) },
            createObjectColumn(
                Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_TABLE_HEADER_QUANTITY_OWNED,
                Pos.CENTER_LEFT,
            ) { it.currentQuantity },
            createStringColumn(
                Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_TABLE_HEADER_UNIT_PRICE,
                Pos.CENTER_LEFT,
            ) { UIUtils.formatCurrencyDynamic(it.currentUnitValue) },
            createStringColumn(
                Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_TABLE_HEADER_TOTAL_VALUE,
                Pos.CENTER_LEFT,
            ) { UIUtils.formatCurrencyDynamic(it.currentQuantity.multiply(it.currentUnitValue)) },
            createStringColumn(
                Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_TABLE_HEADER_AVERAGE_UNIT_PRICE,
                Pos.CENTER_LEFT,
            ) { UIUtils.formatCurrencyDynamic(it.averageUnitValue) },
        )

        stocksFundsTabTickerTable.style = "${stocksFundsTabTickerTable.style}-fx-fixed-cell-size: 45px;"
    }

    private fun createIdColumn(): TableColumn<Ticker, Int> =
        TableColumn<Ticker, Int>(
            preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_TABLE_HEADER_ID),
        ).apply {
            setCellValueFactory { SimpleObjectProperty(it.value.id) }
            cellFactory = createCenteredCellFactory(Pos.CENTER)
        }

    private fun createLogoColumn(): TableColumn<Ticker, ImageView> =
        TableColumn<Ticker, ImageView>("").apply {
            setCellValueFactory { SimpleObjectProperty(UIUtils.loadTickerLogo(it.value, 40.0)) }
            cellFactory = createCenteredCellFactory(Pos.CENTER)
            prefWidth = 45.0
            maxWidth = 45.0
            minWidth = 45.0
            isResizable = false
        }

    private fun createStringColumn(
        headerKey: String,
        alignment: Pos,
        valueExtractor: (Ticker) -> String,
    ): TableColumn<Ticker, String> =
        TableColumn<Ticker, String>(preferencesService.translate(headerKey)).apply {
            setCellValueFactory { SimpleStringProperty(valueExtractor(it.value)) }
            cellFactory = createCenteredCellFactory(alignment)
        }

    private fun <T> createObjectColumn(
        headerKey: String,
        alignment: Pos,
        valueExtractor: (Ticker) -> T,
    ): TableColumn<Ticker, T> =
        TableColumn<Ticker, T>(preferencesService.translate(headerKey)).apply {
            setCellValueFactory { SimpleObjectProperty(valueExtractor(it.value)) }
            cellFactory = createCenteredCellFactory(alignment)
        }

    private fun <T> createCenteredCellFactory(alignment: Pos): Callback<TableColumn<Ticker, T>, TableCell<Ticker, T>> =
        Callback {
            object : TableCell<Ticker, T>() {
                override fun updateItem(
                    item: T?,
                    empty: Boolean,
                ) {
                    super.updateItem(item, empty)
                    when {
                        item == null || empty -> {
                            text = null
                            graphic = null
                        }

                        item is ImageView -> {
                            graphic = item
                            style = "-fx-padding: 0; -fx-alignment: CENTER;"
                        }

                        else -> {
                            text = item.toString()
                            val cssAlignment = if (alignment == Pos.CENTER) "CENTER" else "CENTER-LEFT"
                            style = "-fx-padding: 0 10px; -fx-alignment: $cssAlignment;"
                        }
                    }
                }
            }
        }

    private fun setOffUpdatePortfolioPricesButton() {
        updatePricesButtonIcon.image =
            Image(
                javaClass.getResource(Constants.LOADING_GIF)!!.toExternalForm(),
            )
        updatePortfolioPricesButton.isDisable = true
        updatePortfolioPricesButton.text =
            preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_BUTTON_UPDATING)

        isUpdatingPortfolioPrices = true
    }

    private fun setOnUpdatePortfolioPricesButton() {
        updatePortfolioPricesButton.isDisable = false
        updatePricesButtonIcon.image =
            Image(
                javaClass.getResource(Constants.SAVINGS_SCREEN_SYNC_PRICES_BUTTON_DEFAULT_ICON)!!.toExternalForm(),
            )
        updatePortfolioPricesButton.text =
            preferencesService.translate(Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_BUTTON_UPDATE_PRICES)

        isUpdatingPortfolioPrices = false
    }
}
