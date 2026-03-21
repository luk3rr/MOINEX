/*
 * Filename: InvestmentTransactionsController.kt (original filename: InvestmentTransactionsController.java)
 * Created on: January 10, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.view

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.TabPane
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.investment.CryptoExchange
import org.moinex.model.investment.Dividend
import org.moinex.model.investment.TickerPurchase
import org.moinex.model.investment.TickerSale
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.PreferencesService
import org.moinex.service.investment.TickerService
import org.moinex.ui.dialog.investment.update.EditCryptoExchangeController
import org.moinex.ui.dialog.investment.update.EditDividendController
import org.moinex.ui.dialog.investment.update.EditTickerPurchaseController
import org.moinex.ui.dialog.investment.update.EditTickerSaleController
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.text.MessageFormat

@Controller
class InvestmentTransactionsController(
    private val tickerService: TickerService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var purchaseTableView: TableView<TickerPurchase>

    @FXML
    private lateinit var saleTableView: TableView<TickerSale>

    @FXML
    private lateinit var dividendTableView: TableView<Dividend>

    @FXML
    private lateinit var cryptoExchangeTableView: TableView<CryptoExchange>

    @FXML
    private lateinit var searchField: TextField

    @FXML
    private lateinit var tabPane: TabPane

    private var purchases: MutableList<TickerPurchase> = mutableListOf()
    private var sales: MutableList<TickerSale> = mutableListOf()
    private var dividends: MutableList<Dividend> = mutableListOf()
    private var cryptoExchanges: MutableList<CryptoExchange> = mutableListOf()

    @FXML
    fun initialize() {
        loadPurchasesFromDatabase()
        loadSalesFromDatabase()
        loadDividendsFromDatabase()
        loadCryptoExchangesFromDatabase()

        configurePurchaseTableView()
        configureSaleTableView()
        configureDividendTableView()
        configureCryptoExchangeTableView()

        updatePurchaseTableView()
        updateSaleTableView()
        updateDividendTableView()
        updateCryptoExchangeTableView()

        searchField.textProperty().addListener { _, _, _ ->
            updatePurchaseTableView()
            updateSaleTableView()
            updateDividendTableView()
            updateCryptoExchangeTableView()
        }
    }

    @FXML
    private fun handleEdit() {
        val selectedTab = tabPane.selectionModel.selectedItem ?: return

        when (selectedTab) {
            tabPane.tabs[0] -> {
                val selectedPurchase = purchaseTableView.selectionModel.selectedItem
                editPurchase(selectedPurchase)
            }
            tabPane.tabs[1] -> {
                val selectedSale = saleTableView.selectionModel.selectedItem
                editSale(selectedSale)
            }
            tabPane.tabs[2] -> {
                val selectedDividend = dividendTableView.selectionModel.selectedItem
                editDividend(selectedDividend)
            }
            tabPane.tabs[3] -> {
                val selectedCryptoExchange = cryptoExchangeTableView.selectionModel.selectedItem
                editCryptoExchange(selectedCryptoExchange)
            }
        }
    }

    @FXML
    private fun handleDelete() {
        val selectedTab = tabPane.selectionModel.selectedItem ?: return

        when (selectedTab) {
            tabPane.tabs[0] -> {
                val selectedPurchase = purchaseTableView.selectionModel.selectedItem
                deletePurchase(selectedPurchase)
            }
            tabPane.tabs[1] -> {
                val selectedSale = saleTableView.selectionModel.selectedItem
                deleteSale(selectedSale)
            }
            tabPane.tabs[2] -> {
                val selectedDividend = dividendTableView.selectionModel.selectedItem
                deleteDividend(selectedDividend)
            }
            tabPane.tabs[3] -> {
                val selectedCryptoExchange = cryptoExchangeTableView.selectionModel.selectedItem
                deleteCryptoExchange(selectedCryptoExchange)
            }
        }
    }

    @FXML
    private fun handleCancel() {
        (searchField.scene.window as Stage).close()
    }

    private fun loadPurchasesFromDatabase() {
        purchases = tickerService.getAllPurchases().toMutableList()
    }

    private fun loadSalesFromDatabase() {
        sales = tickerService.getAllSales().toMutableList()
    }

    private fun loadDividendsFromDatabase() {
        dividends = tickerService.getAllDividends().toMutableList()
    }

    private fun loadCryptoExchangesFromDatabase() {
        cryptoExchanges = tickerService.getAllCryptoExchanges().toMutableList()
    }

    private fun updateDividendTableView() {
        val similarTextOrId = searchField.text.lowercase()

        dividendTableView.items.clear()

        if (similarTextOrId.isEmpty()) {
            dividendTableView.items.setAll(dividends)
        } else {
            dividends
                .filter { d ->
                    val id = d.id.toString()
                    val tickerName = d.ticker.name.lowercase()
                    val tickerSymbol = d.ticker.symbol.lowercase()
                    val amount = d.walletTransaction!!.amount.toString()
                    val walletName =
                        d.walletTransaction!!
                            .wallet.name
                            .lowercase()
                    val date = UIUtils.formatDateTimeForDisplay(d.walletTransaction!!.date)
                    val status =
                        d.walletTransaction!!
                            .status.name
                            .lowercase()

                    id.contains(similarTextOrId) ||
                        tickerName.contains(similarTextOrId) ||
                        tickerSymbol.contains(similarTextOrId) ||
                        amount.contains(similarTextOrId) ||
                        walletName.contains(similarTextOrId) ||
                        date.contains(similarTextOrId) ||
                        status.contains(similarTextOrId)
                }.forEach { dividendTableView.items.add(it) }
        }

        dividendTableView.refresh()
    }

    private fun updatePurchaseTableView() {
        val similarTextOrId = searchField.text.lowercase()

        purchaseTableView.items.clear()

        if (similarTextOrId.isEmpty()) {
            purchaseTableView.items.setAll(purchases)
        } else {
            purchases
                .filter { p ->
                    val id = p.id.toString()
                    val tickerName = p.ticker.name.lowercase()
                    val tickerSymbol = p.ticker.symbol.lowercase()
                    val date = UIUtils.formatDateTimeForDisplay(p.walletTransaction!!.date)
                    val quantity = p.quantity.toString()
                    val unitPrice = p.unitPrice.toString()
                    val amount = p.walletTransaction!!.amount.toString()
                    val walletName =
                        p.walletTransaction!!
                            .wallet.name
                            .lowercase()
                    val status =
                        p.walletTransaction!!
                            .status.name
                            .lowercase()

                    id.contains(similarTextOrId) ||
                        tickerName.contains(similarTextOrId) ||
                        tickerSymbol.contains(similarTextOrId) ||
                        date.contains(similarTextOrId) ||
                        quantity.contains(similarTextOrId) ||
                        unitPrice.contains(similarTextOrId) ||
                        amount.contains(similarTextOrId) ||
                        walletName.contains(similarTextOrId) ||
                        status.contains(similarTextOrId)
                }.forEach { purchaseTableView.items.add(it) }
        }

        purchaseTableView.refresh()
    }

    private fun updateSaleTableView() {
        val similarTextOrId = searchField.text.lowercase()

        saleTableView.items.clear()

        if (similarTextOrId.isEmpty()) {
            saleTableView.items.setAll(sales)
        } else {
            sales
                .filter { s ->
                    val id = s.id.toString()
                    val tickerName = s.ticker.name.lowercase()
                    val tickerSymbol = s.ticker.symbol.lowercase()
                    val date = UIUtils.formatDateTimeForDisplay(s.walletTransaction!!.date)
                    val quantity = s.quantity.toString()
                    val unitPrice = s.unitPrice.toString()
                    val amount = s.walletTransaction!!.amount.toString()
                    val walletName =
                        s.walletTransaction!!
                            .wallet.name
                            .lowercase()
                    val status =
                        s.walletTransaction!!
                            .status.name
                            .lowercase()

                    id.contains(similarTextOrId) ||
                        tickerName.contains(similarTextOrId) ||
                        tickerSymbol.contains(similarTextOrId) ||
                        date.contains(similarTextOrId) ||
                        quantity.contains(similarTextOrId) ||
                        unitPrice.contains(similarTextOrId) ||
                        amount.contains(similarTextOrId) ||
                        walletName.contains(similarTextOrId) ||
                        status.contains(similarTextOrId)
                }.forEach { saleTableView.items.add(it) }
        }

        saleTableView.refresh()
    }

    private fun updateCryptoExchangeTableView() {
        val similarTextOrId = searchField.text.lowercase()

        cryptoExchangeTableView.items.clear()

        if (similarTextOrId.isEmpty()) {
            cryptoExchangeTableView.items.setAll(cryptoExchanges)
        } else {
            cryptoExchanges
                .filter { ce ->
                    val id = ce.id.toString()
                    val sourceCrypto = ce.soldCrypto.name.lowercase()
                    val targetCrypto = ce.receivedCrypto.name.lowercase()
                    val date = UIUtils.formatDateTimeForDisplay(ce.date)
                    val sourceQuantity = ce.soldQuantity.toString()
                    val targetQuantity = ce.receivedQuantity.toString()
                    val description = ce.description?.lowercase() ?: ""

                    id.contains(similarTextOrId) ||
                        sourceCrypto.contains(similarTextOrId) ||
                        targetCrypto.contains(similarTextOrId) ||
                        date.contains(similarTextOrId) ||
                        sourceQuantity.contains(similarTextOrId) ||
                        targetQuantity.contains(similarTextOrId) ||
                        description.contains(similarTextOrId)
                }.forEach { cryptoExchangeTableView.items.add(it) }
        }

        cryptoExchangeTableView.refresh()
    }

    private fun configurePurchaseTableView() {
        val idColumn =
            TableColumn<TickerPurchase, Int>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_ID),
            ).apply {
                setCellValueFactory { param -> SimpleObjectProperty(param.value.id) }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val tickerNameColumn =
            TableColumn<TickerPurchase, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_TICKER),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty("${param.value.ticker.name} (${param.value.ticker.symbol})")
                }
            }

        val tickerTypeColumn =
            TableColumn<TickerPurchase, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_TYPE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(UIUtils.translateAssetType(param.value.ticker.type))
                }
            }

        val dateColumn =
            TableColumn<TickerPurchase, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_DATE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(UIUtils.formatDateTimeForDisplay(param.value.walletTransaction!!.date))
                }
            }

        val quantityColumn =
            TableColumn<TickerPurchase, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_QUANTITY),
            ).apply {
                setCellValueFactory { param -> SimpleObjectProperty(param.value.quantity.toString()) }
            }

        val unitPriceColumn =
            TableColumn<TickerPurchase, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_UNIT_PRICE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleObjectProperty(UIUtils.formatCurrency(param.value.unitPrice))
                }
            }

        val amountColumn =
            TableColumn<TickerPurchase, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_TOTAL_AMOUNT),
            ).apply {
                setCellValueFactory { param ->
                    SimpleObjectProperty(UIUtils.formatCurrency(param.value.walletTransaction!!.amount))
                }
            }

        val walletNameColumn =
            TableColumn<TickerPurchase, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_WALLET),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(
                        param.value.walletTransaction!!
                            .wallet.name,
                    )
                }
            }

        val statusColumn =
            TableColumn<TickerPurchase, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_STATUS),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(UIUtils.translateTransactionStatus(param.value.walletTransaction!!.status))
                }
            }

        purchaseTableView.columns.addAll(
            idColumn,
            tickerNameColumn,
            tickerTypeColumn,
            quantityColumn,
            unitPriceColumn,
            amountColumn,
            walletNameColumn,
            dateColumn,
            statusColumn,
        )
    }

    private fun configureSaleTableView() {
        val idColumn =
            TableColumn<TickerSale, Int>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_ID),
            ).apply {
                setCellValueFactory { param -> SimpleObjectProperty(param.value.id) }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val tickerNameColumn =
            TableColumn<TickerSale, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_TICKER),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty("${param.value.ticker.name} (${param.value.ticker.symbol})")
                }
            }

        val tickerTypeColumn =
            TableColumn<TickerSale, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_TYPE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(UIUtils.translateAssetType(param.value.ticker.type))
                }
            }

        val dateColumn =
            TableColumn<TickerSale, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_DATE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(UIUtils.formatDateTimeForDisplay(param.value.walletTransaction!!.date))
                }
            }

        val quantityColumn =
            TableColumn<TickerSale, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_QUANTITY),
            ).apply {
                setCellValueFactory { param -> SimpleObjectProperty(param.value.quantity.toString()) }
            }

        val unitPriceColumn =
            TableColumn<TickerSale, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_UNIT_PRICE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleObjectProperty(UIUtils.formatCurrency(param.value.unitPrice))
                }
            }

        val amountColumn =
            TableColumn<TickerSale, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_TOTAL_AMOUNT),
            ).apply {
                setCellValueFactory { param ->
                    SimpleObjectProperty(UIUtils.formatCurrency(param.value.walletTransaction!!.amount))
                }
            }

        val walletNameColumn =
            TableColumn<TickerSale, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_WALLET),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(
                        param.value.walletTransaction!!
                            .wallet.name,
                    )
                }
            }

        val statusColumn =
            TableColumn<TickerSale, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_STATUS),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(UIUtils.translateTransactionStatus(param.value.walletTransaction!!.status))
                }
            }

        saleTableView.columns.addAll(
            idColumn,
            tickerNameColumn,
            tickerTypeColumn,
            quantityColumn,
            unitPriceColumn,
            amountColumn,
            walletNameColumn,
            dateColumn,
            statusColumn,
        )
    }

    private fun configureDividendTableView() {
        val idColumn =
            TableColumn<Dividend, Int>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_ID),
            ).apply {
                setCellValueFactory { param -> SimpleObjectProperty(param.value.id) }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val tickerNameColumn =
            TableColumn<Dividend, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_TICKER),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty("${param.value.ticker.name} (${param.value.ticker.symbol})")
                }
            }

        val tickerTypeColumn =
            TableColumn<Dividend, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_TYPE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(UIUtils.translateAssetType(param.value.ticker.type))
                }
            }

        val dateColumn =
            TableColumn<Dividend, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_DATE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(UIUtils.formatDateTimeForDisplay(param.value.walletTransaction!!.date))
                }
            }

        val amountColumn =
            TableColumn<Dividend, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_DIVIDEND_VALUE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleObjectProperty(UIUtils.formatCurrency(param.value.walletTransaction!!.amount))
                }
            }

        val walletNameColumn =
            TableColumn<Dividend, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_WALLET),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(
                        param.value.walletTransaction!!
                            .wallet.name,
                    )
                }
            }

        val statusColumn =
            TableColumn<Dividend, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_STATUS),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(UIUtils.translateTransactionStatus(param.value.walletTransaction!!.status))
                }
            }

        dividendTableView.columns.addAll(
            idColumn,
            tickerNameColumn,
            tickerTypeColumn,
            amountColumn,
            walletNameColumn,
            dateColumn,
            statusColumn,
        )
    }

    private fun configureCryptoExchangeTableView() {
        val idColumn =
            TableColumn<CryptoExchange, Int>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_ID),
            ).apply {
                setCellValueFactory { param -> SimpleObjectProperty(param.value.id) }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val soldCryptoNameColumn =
            TableColumn<CryptoExchange, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_CRYPTO_SOLD),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty("${param.value.soldCrypto.name} (${param.value.soldCrypto.symbol})")
                }
            }

        val receivedCryptoNameColumn =
            TableColumn<CryptoExchange, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_CRYPTO_RECEIVED),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty("${param.value.receivedCrypto.name} (${param.value.receivedCrypto.symbol})")
                }
            }

        val quantitySoldColumn =
            TableColumn<CryptoExchange, BigDecimal>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_QUANTITY_SOLD),
            ).apply {
                setCellValueFactory { param -> SimpleObjectProperty(param.value.soldQuantity) }
            }

        val quantityReceivedColumn =
            TableColumn<CryptoExchange, BigDecimal>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_QUANTITY_RECEIVED),
            ).apply {
                setCellValueFactory { param -> SimpleObjectProperty(param.value.receivedQuantity) }
            }

        val dateColumn =
            TableColumn<CryptoExchange, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_DATE),
            ).apply {
                setCellValueFactory { param ->
                    SimpleStringProperty(UIUtils.formatDateTimeForDisplay(param.value.date))
                }
            }

        val descriptionColumn =
            TableColumn<CryptoExchange, String>(
                preferencesService.translate(TranslationKeys.INVESTMENT_TABLE_DESCRIPTION),
            ).apply {
                setCellValueFactory { param -> SimpleStringProperty(param.value.description) }
            }

        cryptoExchangeTableView.columns.addAll(
            idColumn,
            soldCryptoNameColumn,
            receivedCryptoNameColumn,
            quantitySoldColumn,
            quantityReceivedColumn,
            dateColumn,
            descriptionColumn,
        )
    }

    private fun editPurchase(purchase: TickerPurchase?) {
        if (purchase == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_PURCHASE_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_PURCHASE_SELECTED_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Files.EDIT_TICKER_PURCHASE_FXML,
            preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EDIT_TICKER_PURCHASE),
            springContext,
            { controller: EditTickerPurchaseController -> controller.setPurchase(purchase) },
            listOf(
                Runnable {
                    loadPurchasesFromDatabase()
                    updatePurchaseTableView()
                },
            ),
        )
    }

    private fun editSale(sale: TickerSale?) {
        if (sale == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_SALE_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_SALE_SELECTED_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Files.EDIT_TICKER_SALE_FXML,
            preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EDIT_TICKER_SALE),
            springContext,
            { controller: EditTickerSaleController -> controller.setSale(sale) },
            listOf(
                Runnable {
                    loadSalesFromDatabase()
                    updateSaleTableView()
                },
            ),
        )
    }

    private fun editDividend(dividend: Dividend?) {
        if (dividend == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_DIVIDEND_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_DIVIDEND_SELECTED_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Files.EDIT_DIVIDEND_FXML,
            preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EDIT_DIVIDEND),
            springContext,
            { controller: EditDividendController -> controller.setDividend(dividend) },
            listOf(
                Runnable {
                    loadDividendsFromDatabase()
                    updateDividendTableView()
                },
            ),
        )
    }

    private fun editCryptoExchange(cryptoExchange: CryptoExchange?) {
        if (cryptoExchange == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_EXCHANGE_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_EXCHANGE_SELECTED_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Files.EDIT_CRYPTO_EXCHANGE_FXML,
            preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EDIT_CRYPTO_EXCHANGE),
            springContext,
            { controller: EditCryptoExchangeController -> controller.setCryptoExchange(cryptoExchange) },
            listOf(
                Runnable {
                    loadCryptoExchangesFromDatabase()
                    updateCryptoExchangeTableView()
                },
            ),
        )
    }

    private fun deletePurchase(purchase: TickerPurchase?) {
        if (purchase == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_PURCHASE_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_PURCHASE_SELECTED_DELETE_MESSAGE),
            )
            return
        }

        val message = deleteMessage(purchase.walletTransaction!!)

        if (WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_CONFIRM_DELETE_PURCHASE_TITLE),
                message,
                preferencesService.bundle,
            )
        ) {
            tickerService.deleteTickerPurchase(purchase.id!!)
            loadPurchasesFromDatabase()
            updatePurchaseTableView()
        }
    }

    private fun deleteSale(sale: TickerSale?) {
        if (sale == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_SALE_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_SALE_SELECTED_DELETE_MESSAGE),
            )
            return
        }

        val message = deleteMessage(sale.walletTransaction!!)

        if (WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_CONFIRM_DELETE_SALE_TITLE),
                message,
                preferencesService.bundle,
            )
        ) {
            tickerService.deleteTickerSale(sale.id!!)
            loadSalesFromDatabase()
            updateSaleTableView()
        }
    }

    private fun deleteDividend(dividend: Dividend?) {
        if (dividend == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_DIVIDEND_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_DIVIDEND_SELECTED_DELETE_MESSAGE),
            )
            return
        }

        val message = deleteMessage(dividend.walletTransaction!!)

        if (WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_CONFIRM_DELETE_DIVIDEND_TITLE),
                message,
                preferencesService.bundle,
            )
        ) {
            tickerService.deleteDividend(dividend.id!!)
            loadDividendsFromDatabase()
            updateDividendTableView()
        }
    }

    private fun deleteCryptoExchange(cryptoExchange: CryptoExchange?) {
        if (cryptoExchange == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_EXCHANGE_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_EXCHANGE_SELECTED_DELETE_MESSAGE),
            )
            return
        }

        val message =
            MessageFormat.format(
                "ID: {0}\n{1}\n{2}\n{3}\n{4}\n{5}\n{6}\n{7}\n{8}",
                cryptoExchange.id,
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DELETE_SOURCE_CRYPTO),
                    cryptoExchange.soldCrypto.name,
                    cryptoExchange.soldCrypto.symbol,
                ),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DELETE_TARGET_CRYPTO),
                    cryptoExchange.receivedCrypto.name,
                    cryptoExchange.receivedCrypto.symbol,
                ),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DELETE_SOURCE_QUANTITY),
                    cryptoExchange.soldQuantity,
                ),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DELETE_SOURCE_QUANTITY_AFTER_DELETION),
                    cryptoExchange.soldCrypto.currentQuantity.add(cryptoExchange.soldQuantity),
                ),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DELETE_TARGET_QUANTITY),
                    cryptoExchange.receivedQuantity,
                ),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DELETE_TARGET_QUANTITY_AFTER_DELETION),
                    cryptoExchange.receivedCrypto.currentQuantity.subtract(cryptoExchange.receivedQuantity),
                ),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DELETE_DATE),
                    UIUtils.formatDateTimeForDisplay(cryptoExchange.date),
                ),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DELETE_DESCRIPTION),
                    cryptoExchange.description,
                ),
            )

        if (WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_CONFIRM_DELETE_EXCHANGE_TITLE),
                message,
                preferencesService.bundle,
            )
        ) {
            tickerService.deleteCryptoExchange(cryptoExchange.id!!)
            loadCryptoExchangesFromDatabase()
            updateCryptoExchangeTableView()
        }
    }

    private fun deleteMessage(wt: WalletTransaction): String {
        val balanceAfterDeletion =
            if (wt.status == WalletTransactionStatus.CONFIRMED) {
                if (wt.type == WalletTransactionType.EXPENSE) {
                    wt.wallet.balance.add(wt.amount)
                } else {
                    wt.wallet.balance.subtract(wt.amount)
                }
            } else {
                wt.wallet.balance
            }

        return MessageFormat.format(
            "{0}\n{1}\n{2}\n{3}\n{4}\n{5}\n{6}",
            MessageFormat.format(
                preferencesService.translate(TranslationKeys.INVESTMENT_DELETE_DESCRIPTION),
                wt.description,
            ),
            MessageFormat.format(
                preferencesService.translate(TranslationKeys.INVESTMENT_DELETE_AMOUNT),
                UIUtils.formatCurrency(wt.amount),
            ),
            MessageFormat.format(
                preferencesService.translate(TranslationKeys.INVESTMENT_DELETE_DATE),
                UIUtils.formatDateTimeForDisplay(wt.date),
            ),
            MessageFormat.format(
                preferencesService.translate(TranslationKeys.INVESTMENT_DELETE_STATUS),
                UIUtils.translateTransactionStatus(wt.status),
            ),
            MessageFormat.format(
                preferencesService.translate(TranslationKeys.INVESTMENT_DELETE_WALLET),
                wt.wallet.name,
            ),
            MessageFormat.format(
                preferencesService.translate(TranslationKeys.INVESTMENT_DELETE_WALLET_BALANCE),
                UIUtils.formatCurrency(wt.wallet.balance),
            ),
            MessageFormat.format(
                preferencesService.translate(TranslationKeys.INVESTMENT_DELETE_WALLET_BALANCE_AFTER_DELETION),
                UIUtils.formatCurrency(balanceAfterDeletion),
            ),
        )
    }
}
