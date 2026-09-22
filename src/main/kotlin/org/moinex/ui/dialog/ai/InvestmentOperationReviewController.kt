/*
 * Filename: InvestmentOperationReviewController.kt
 * Created on: September 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.ai

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TableColumn
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.scene.control.cell.ComboBoxTableCell
import javafx.scene.control.cell.TextFieldTableCell
import javafx.stage.Stage
import javafx.util.Callback
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.model.Category
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.model.dto.ai.ExtractedInvestmentOperationDTO
import org.moinex.model.dto.ai.InvestmentOperationKind
import org.moinex.model.enums.OperationType
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondOperation
import org.moinex.model.investment.Dividend
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPurchase
import org.moinex.model.investment.TickerSale
import org.moinex.model.wallettransaction.Wallet
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.common.DatePickerTableCell
import org.moinex.ui.common.bigDecimalTableConverter
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.text.MessageFormat
import java.time.LocalDate

class InvestmentOperationReviewRow(
    val kind: InvestmentOperationKind,
    val date: SimpleObjectProperty<LocalDate>,
    val assetLabel: SimpleStringProperty,
    val description: SimpleStringProperty,
    val quantity: SimpleObjectProperty<BigDecimal>,
    val unitPrice: SimpleObjectProperty<BigDecimal>,
    val fees: SimpleObjectProperty<BigDecimal>,
    val taxes: SimpleObjectProperty<BigDecimal>,
    val dividendAmount: SimpleObjectProperty<BigDecimal>,
    val categoryName: SimpleStringProperty,
    val walletName: SimpleStringProperty,
    val confirm: SimpleBooleanProperty,
    val unmatchedAsset: Boolean,
    val lowConfidence: Boolean,
)

/**
 * Each investment operation kind has its own tab/table with only the columns that apply to it
 * (e.g. a dividend has no unit price, a stock trade has no fees/taxes) instead of one table with
 * every field crammed together, most of them blank for any given row.
 */
@Controller
class InvestmentOperationReviewController(
    private val categoryService: CategoryService,
    private val walletService: WalletService,
    private val tickerService: TickerService,
    private val bondService: BondService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var operationsTabPane: TabPane

    private lateinit var stockTable: TableView<InvestmentOperationReviewRow>
    private lateinit var bondTable: TableView<InvestmentOperationReviewRow>
    private lateinit var dividendTable: TableView<InvestmentOperationReviewRow>

    private var categories: MutableList<Category> = mutableListOf()
    private var wallets: List<Wallet> = emptyList()
    private var tickers: List<Ticker> = emptyList()
    private var bonds: List<Bond> = emptyList()
    private val tickerPrefix = preferencesService.translate(TranslationKeys.AI_REVIEW_INV_ASSET_PREFIX_TICKER) + " "
    private val bondPrefix = preferencesService.translate(TranslationKeys.AI_REVIEW_INV_ASSET_PREFIX_BOND) + " "

    companion object {
        private const val LOW_CONFIDENCE_THRESHOLD = 0.6
        private const val STYLE_ROW_LOW_CONFIDENCE = "ai-review-row-low-confidence"
        private const val STYLE_ROW_UNMATCHED = "ai-review-row-unmatched"

        private const val COLUMN_WIDTH_CONFIRM = 70.0
        private const val COLUMN_WIDTH_DATE = 100.0
        private const val COLUMN_WIDTH_KIND = 130.0
        private const val COLUMN_WIDTH_ASSET = 170.0
        private const val COLUMN_WIDTH_DESCRIPTION = 200.0
        private const val COLUMN_WIDTH_NUMBER = 100.0
        private const val COLUMN_WIDTH_CATEGORY = 150.0
        private const val COLUMN_WIDTH_WALLET = 150.0
        private const val COLUMN_WIDTH_NOTES = 240.0

        private val STOCK_KINDS = setOf(InvestmentOperationKind.STOCK_PURCHASE, InvestmentOperationKind.STOCK_SALE)
        private val BOND_KINDS =
            setOf(InvestmentOperationKind.BOND_APPLICATION, InvestmentOperationKind.BOND_REDEMPTION)
    }

    @FXML
    private fun initialize() {
        categories = categoryService.getNonArchivedCategoriesOrderedByName().toMutableList()
        wallets = walletService.getAllNonArchivedWalletsOrderedByName()
        tickers = tickerService.getAllNonArchivedTickers()
        bonds = bondService.getAllNonArchivedBonds()

        stockTable = buildStockTable()
        bondTable = buildBondTable()
        dividendTable = buildDividendTable()
    }

    fun setProposedOperations(operations: List<ExtractedInvestmentOperationDTO>) {
        val rows = operations.map(::toRow)

        stockTable.items.setAll(rows.filter { it.kind in STOCK_KINDS })
        bondTable.items.setAll(rows.filter { it.kind in BOND_KINDS })
        dividendTable.items.setAll(rows.filter { it.kind == InvestmentOperationKind.DIVIDEND })

        refreshTabs()
    }

    /**
     * The rows still in the tables when the window closes - i.e. whatever wasn't confirmed yet
     * (unchecked, or checked but failed to persist), reflecting any edits the user made. The
     * caller uses this to update what a "Review" button reopens next time, so already-persisted
     * operations (removed from their table on a successful [handleConfirm]) are never shown again.
     */
    fun getRemainingOperations(): List<ExtractedInvestmentOperationDTO> =
        (stockTable.items + bondTable.items + dividendTable.items).map { row ->
            val ticker = if (row.kind !in BOND_KINDS) resolveTicker(row.assetLabel.value) else null
            val bond = if (row.kind in BOND_KINDS) resolveBond(row.assetLabel.value) else null
            val category = categories.firstOrNull { it.name.equals(row.categoryName.value, ignoreCase = true) }

            ExtractedInvestmentOperationDTO(
                date = row.date.value,
                kind = row.kind,
                description = row.description.value,
                quantity = row.quantity.value,
                unitPrice = row.unitPrice.value,
                fees = row.fees.value,
                taxes = row.taxes.value,
                dividendAmount = row.dividendAmount.value,
                suggestedTickerId = ticker?.id,
                suggestedBondId = bond?.id,
                suggestedAssetLabel = row.assetLabel.value,
                suggestedWalletId = resolveWallet(row.walletName.value)?.id,
                suggestedCategoryId = category?.id,
                suggestedCategoryName = row.categoryName.value,
                confidence = if (row.lowConfidence) 0.0 else 1.0,
            )
        }

    private fun refreshTabs() {
        val tabs = mutableListOf<Tab>()
        if (stockTable.items.isNotEmpty()) {
            tabs.add(Tab(preferencesService.translate(TranslationKeys.AI_REVIEW_INV_TAB_STOCKS), stockTable))
        }
        if (bondTable.items.isNotEmpty()) {
            tabs.add(Tab(preferencesService.translate(TranslationKeys.AI_REVIEW_INV_TAB_BONDS), bondTable))
        }
        if (dividendTable.items.isNotEmpty()) {
            tabs.add(Tab(preferencesService.translate(TranslationKeys.AI_REVIEW_INV_TAB_DIVIDENDS), dividendTable))
        }
        operationsTabPane.tabs.setAll(tabs)
    }

    private fun toRow(dto: ExtractedInvestmentOperationDTO): InvestmentOperationReviewRow {
        val matchedTicker = tickers.firstOrNull { it.id == dto.suggestedTickerId }
        val matchedBond = bonds.firstOrNull { it.id == dto.suggestedBondId }
        val assetLabel =
            matchedTicker?.let { tickerPrefix + it.symbol } ?: matchedBond?.let { bondPrefix + it.name } ?: ""
        val walletName = wallets.firstOrNull { it.id == dto.suggestedWalletId }?.name.orEmpty()

        return InvestmentOperationReviewRow(
            kind = dto.kind,
            date = SimpleObjectProperty(dto.date),
            assetLabel = SimpleStringProperty(assetLabel),
            description = SimpleStringProperty(dto.description),
            quantity = SimpleObjectProperty(dto.quantity),
            unitPrice = SimpleObjectProperty(dto.unitPrice),
            fees = SimpleObjectProperty(dto.fees),
            taxes = SimpleObjectProperty(dto.taxes),
            dividendAmount = SimpleObjectProperty(dto.dividendAmount),
            categoryName = SimpleStringProperty(dto.suggestedCategoryName),
            walletName = SimpleStringProperty(walletName),
            confirm = SimpleBooleanProperty(true),
            unmatchedAsset = matchedTicker == null && matchedBond == null,
            lowConfidence = dto.confidence < LOW_CONFIDENCE_THRESHOLD,
        )
    }

    private fun buildStockTable(): TableView<InvestmentOperationReviewRow> {
        val tickerLabels = FXCollections.observableArrayList(tickers.map { tickerPrefix + it.symbol })
        val table = newTableView()
        table.columns.setAll(
            listOf(
                confirmColumn(),
                dateColumn(),
                kindColumn(COLUMN_WIDTH_KIND),
                assetColumn(tickerLabels),
                descriptionColumn(),
                quantityColumn(),
                unitPriceColumn(),
                categoryColumn(),
                walletColumn(),
                notesColumn(),
            ),
        )
        return table
    }

    private fun buildBondTable(): TableView<InvestmentOperationReviewRow> {
        val bondLabels = FXCollections.observableArrayList(bonds.map { bondPrefix + it.name })
        val table = newTableView()
        table.columns.setAll(
            listOf(
                confirmColumn(),
                dateColumn(),
                kindColumn(COLUMN_WIDTH_KIND),
                assetColumn(bondLabels),
                descriptionColumn(),
                quantityColumn(),
                unitPriceColumn(),
                feesColumn(),
                taxesColumn(),
                categoryColumn(),
                walletColumn(),
                notesColumn(),
            ),
        )
        return table
    }

    private fun buildDividendTable(): TableView<InvestmentOperationReviewRow> {
        val tickerLabels = FXCollections.observableArrayList(tickers.map { tickerPrefix + it.symbol })
        val table = newTableView()
        table.columns.setAll(
            listOf(
                confirmColumn(),
                dateColumn(),
                assetColumn(tickerLabels),
                descriptionColumn(),
                dividendAmountColumn(),
                categoryColumn(),
                walletColumn(),
                notesColumn(),
            ),
        )
        return table
    }

    private fun newTableView(): TableView<InvestmentOperationReviewRow> =
        TableView<InvestmentOperationReviewRow>().apply {
            isEditable = true
            stylesheets.add(
                InvestmentOperationReviewController::class.java
                    .getResource(Files.TABLE_VIEW_STYLE_SHEET)!!
                    .toExternalForm(),
            )
            setRowFactory {
                TableRow<InvestmentOperationReviewRow>().apply {
                    itemProperty().addListener { _, _, row ->
                        styleClass.removeAll(STYLE_ROW_UNMATCHED, STYLE_ROW_LOW_CONFIDENCE)
                        if (row?.unmatchedAsset == true) styleClass.add(STYLE_ROW_UNMATCHED)
                        if (row?.lowConfidence == true) styleClass.add(STYLE_ROW_LOW_CONFIDENCE)
                    }
                }
            }
        }

    private fun confirmColumn(): TableColumn<InvestmentOperationReviewRow, Boolean> {
        val column =
            TableColumn<InvestmentOperationReviewRow, Boolean>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_CONFIRM),
            )
        column.cellValueFactory = Callback { it.value.confirm }
        column.cellFactory = CheckBoxTableCell.forTableColumn(column)
        column.prefWidth = COLUMN_WIDTH_CONFIRM
        return column
    }

    private fun dateColumn(): TableColumn<InvestmentOperationReviewRow, LocalDate> {
        val column =
            TableColumn<InvestmentOperationReviewRow, LocalDate>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_DATE),
            )
        column.cellValueFactory = Callback { it.value.date }
        column.cellFactory = Callback { DatePickerTableCell() }
        column.prefWidth = COLUMN_WIDTH_DATE
        return column
    }

    private fun kindColumn(width: Double): TableColumn<InvestmentOperationReviewRow, String> {
        val column =
            TableColumn<InvestmentOperationReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_INV_COLUMN_KIND),
            )
        column.cellValueFactory = Callback { SimpleStringProperty(kindLabel(it.value.kind)) }
        column.prefWidth = width
        return column
    }

    private fun assetColumn(options: ObservableList<String>): TableColumn<InvestmentOperationReviewRow, String> {
        val column =
            TableColumn<InvestmentOperationReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_INV_COLUMN_ASSET),
            )
        column.cellValueFactory = Callback { it.value.assetLabel }
        column.cellFactory = ComboBoxTableCell.forTableColumn(options)
        column.prefWidth = COLUMN_WIDTH_ASSET
        return column
    }

    private fun descriptionColumn(): TableColumn<InvestmentOperationReviewRow, String> {
        val column =
            TableColumn<InvestmentOperationReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_DESCRIPTION),
            )
        column.cellValueFactory = Callback { it.value.description }
        column.cellFactory = TextFieldTableCell.forTableColumn()
        column.prefWidth = COLUMN_WIDTH_DESCRIPTION
        return column
    }

    private fun quantityColumn(): TableColumn<InvestmentOperationReviewRow, BigDecimal> {
        val column =
            TableColumn<InvestmentOperationReviewRow, BigDecimal>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_INV_COLUMN_QUANTITY),
            )
        column.cellValueFactory = Callback { it.value.quantity }
        column.cellFactory = TextFieldTableCell.forTableColumn(bigDecimalTableConverter())
        column.prefWidth = COLUMN_WIDTH_NUMBER
        return column
    }

    private fun unitPriceColumn(): TableColumn<InvestmentOperationReviewRow, BigDecimal> {
        val column =
            TableColumn<InvestmentOperationReviewRow, BigDecimal>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_INV_COLUMN_UNIT_PRICE),
            )
        column.cellValueFactory = Callback { it.value.unitPrice }
        column.cellFactory = TextFieldTableCell.forTableColumn(bigDecimalTableConverter())
        column.prefWidth = COLUMN_WIDTH_NUMBER
        return column
    }

    private fun feesColumn(): TableColumn<InvestmentOperationReviewRow, BigDecimal> {
        val column =
            TableColumn<InvestmentOperationReviewRow, BigDecimal>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_INV_COLUMN_FEES),
            )
        column.cellValueFactory = Callback { it.value.fees }
        column.cellFactory = TextFieldTableCell.forTableColumn(bigDecimalTableConverter())
        column.prefWidth = COLUMN_WIDTH_NUMBER
        return column
    }

    private fun taxesColumn(): TableColumn<InvestmentOperationReviewRow, BigDecimal> {
        val column =
            TableColumn<InvestmentOperationReviewRow, BigDecimal>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_INV_COLUMN_TAXES),
            )
        column.cellValueFactory = Callback { it.value.taxes }
        column.cellFactory = TextFieldTableCell.forTableColumn(bigDecimalTableConverter())
        column.prefWidth = COLUMN_WIDTH_NUMBER
        return column
    }

    private fun dividendAmountColumn(): TableColumn<InvestmentOperationReviewRow, BigDecimal> {
        val column =
            TableColumn<InvestmentOperationReviewRow, BigDecimal>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_INV_COLUMN_DIVIDEND_AMOUNT),
            )
        column.cellValueFactory = Callback { it.value.dividendAmount }
        column.cellFactory = TextFieldTableCell.forTableColumn(bigDecimalTableConverter())
        column.prefWidth = COLUMN_WIDTH_NUMBER
        return column
    }

    private fun categoryColumn(): TableColumn<InvestmentOperationReviewRow, String> {
        val categoryNames = FXCollections.observableArrayList(categories.map { it.name })
        val column =
            TableColumn<InvestmentOperationReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_CATEGORY),
            )
        column.cellValueFactory = Callback { it.value.categoryName }
        column.cellFactory =
            Callback { _: TableColumn<InvestmentOperationReviewRow, String> ->
                ComboBoxTableCell<InvestmentOperationReviewRow, String>(
                    categoryNames,
                ).apply { isComboBoxEditable = true }
            }
        column.prefWidth = COLUMN_WIDTH_CATEGORY
        return column
    }

    private fun walletColumn(): TableColumn<InvestmentOperationReviewRow, String> {
        val walletNames = FXCollections.observableArrayList(wallets.map { it.name })
        val column =
            TableColumn<InvestmentOperationReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_INV_COLUMN_WALLET),
            )
        column.cellValueFactory = Callback { it.value.walletName }
        column.cellFactory = ComboBoxTableCell.forTableColumn(walletNames)
        column.prefWidth = COLUMN_WIDTH_WALLET
        return column
    }

    private fun notesColumn(): TableColumn<InvestmentOperationReviewRow, String> {
        val column =
            TableColumn<InvestmentOperationReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_NOTES),
            )
        column.cellValueFactory =
            Callback { data -> Bindings.createStringBinding({ buildNotes(data.value) }, data.value.categoryName) }
        column.prefWidth = COLUMN_WIDTH_NOTES
        return column
    }

    private fun kindLabel(kind: InvestmentOperationKind): String =
        preferencesService.translate(
            when (kind) {
                InvestmentOperationKind.STOCK_PURCHASE -> TranslationKeys.AI_REVIEW_INV_KIND_STOCK_PURCHASE
                InvestmentOperationKind.STOCK_SALE -> TranslationKeys.AI_REVIEW_INV_KIND_STOCK_SALE
                InvestmentOperationKind.DIVIDEND -> TranslationKeys.AI_REVIEW_INV_KIND_DIVIDEND
                InvestmentOperationKind.BOND_APPLICATION -> TranslationKeys.AI_REVIEW_INV_KIND_BOND_APPLICATION
                InvestmentOperationKind.BOND_REDEMPTION -> TranslationKeys.AI_REVIEW_INV_KIND_BOND_REDEMPTION
            },
        )

    private fun buildNotes(row: InvestmentOperationReviewRow): String {
        val notes = mutableListOf<String>()
        if (row.unmatchedAsset) notes.add(preferencesService.translate(TranslationKeys.AI_REVIEW_INV_UNMATCHED_ASSET))
        if (row.lowConfidence) notes.add(preferencesService.translate(TranslationKeys.AI_REVIEW_LOW_CONFIDENCE))
        if (categories.none { it.name.equals(row.categoryName.value, ignoreCase = true) }) {
            notes.add(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.AI_REVIEW_NEW_CATEGORY),
                    row.categoryName.value,
                ),
            )
        }
        return notes.joinToString("; ")
    }

    @FXML
    private fun handleConfirm() {
        val allTables = listOf(stockTable, bondTable, dividendTable)
        val rowsToConfirm = allTables.flatMap { table -> table.items.filter { it.confirm.value } }
        if (rowsToConfirm.isEmpty()) {
            closeWindow()
            return
        }

        val succeeded = mutableListOf<InvestmentOperationReviewRow>()
        val errors = mutableListOf<String>()

        rowsToConfirm.forEach { row ->
            runCatching { persist(row) }
                .onSuccess { succeeded.add(row) }
                .onFailure { e -> errors.add("${describeRow(row)}: ${e.message ?: "unknown error"}") }
        }

        allTables.forEach { table -> table.items.removeAll(succeeded) }
        refreshTabs()

        if (errors.isEmpty()) {
            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.AI_REVIEW_INV_TITLE),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.AI_REVIEW_INV_CONFIRMED_COUNT),
                    succeeded.size,
                ),
                preferencesService.bundle,
            )
            closeWindow()
        } else {
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.AI_REVIEW_INV_TITLE),
                errors.joinToString("\n"),
                preferencesService.bundle,
            )
        }
    }

    private fun describeRow(row: InvestmentOperationReviewRow): String =
        "${row.date.value} - ${row.assetLabel.value.ifBlank { row.description.value }}"

    private fun persist(row: InvestmentOperationReviewRow) {
        val wallet =
            resolveWallet(row.walletName.value)
                ?: error(preferencesService.translate(TranslationKeys.AI_REVIEW_INV_NO_WALLET_SELECTED))
        val category = resolveCategory(row.categoryName.value)
        val dateTime = row.date.value.atStartOfDay()
        val context =
            WalletTransactionContextDTO(
                wallet = wallet,
                date = dateTime,
                category = category,
                description = row.description.value,
                status = WalletTransactionStatus.CONFIRMED,
                includeInAnalysis = true,
            )
        val assetNotSelected = preferencesService.translate(TranslationKeys.AI_REVIEW_INV_NO_ASSET_SELECTED)

        when (row.kind) {
            InvestmentOperationKind.STOCK_PURCHASE -> {
                val ticker = resolveTicker(row.assetLabel.value) ?: error(assetNotSelected)
                tickerService.createTickerPurchase(
                    TickerPurchase(ticker = ticker, quantity = row.quantity.value, unitPrice = row.unitPrice.value),
                    context,
                )
            }
            InvestmentOperationKind.STOCK_SALE -> {
                val ticker = resolveTicker(row.assetLabel.value) ?: error(assetNotSelected)
                tickerService.createTickerSale(
                    TickerSale(
                        ticker = ticker,
                        averageCost = ticker.averageUnitValue,
                        quantity = row.quantity.value,
                        unitPrice = row.unitPrice.value,
                    ),
                    context,
                )
            }
            InvestmentOperationKind.DIVIDEND -> {
                val ticker = resolveTicker(row.assetLabel.value) ?: error(assetNotSelected)
                tickerService.createDividend(Dividend(ticker = ticker), row.dividendAmount.value, context)
            }
            InvestmentOperationKind.BOND_APPLICATION -> {
                val bond = resolveBond(row.assetLabel.value) ?: error(assetNotSelected)
                bondService.createBondOperation(
                    BondOperation(
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = row.quantity.value,
                        unitPrice = row.unitPrice.value,
                        fees = row.fees.value,
                        taxes = row.taxes.value,
                        netProfit = BigDecimal.ZERO,
                    ),
                    context,
                )
            }
            InvestmentOperationKind.BOND_REDEMPTION -> {
                val bond = resolveBond(row.assetLabel.value) ?: error(assetNotSelected)
                val averageUnitPrice = bondService.getAverageUnitPrice(bond)
                val netProfit =
                    row.unitPrice.value
                        .subtract(averageUnitPrice)
                        .multiply(row.quantity.value)
                        .subtract(row.fees.value)
                        .subtract(row.taxes.value)

                bondService.createBondOperation(
                    BondOperation(
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = row.quantity.value,
                        unitPrice = row.unitPrice.value,
                        fees = row.fees.value,
                        taxes = row.taxes.value,
                        netProfit = netProfit,
                    ),
                    context,
                )
            }
        }
    }

    private fun resolveCategory(name: String): Category {
        categories.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it }

        val newId = categoryService.createCategory(Category(name = name))
        val created = categoryService.getCategories().first { it.id == newId }
        categories.add(created)
        return created
    }

    private fun resolveWallet(name: String): Wallet? = wallets.firstOrNull { it.name == name }

    private fun resolveTicker(label: String): Ticker? = tickers.firstOrNull { tickerPrefix + it.symbol == label }

    private fun resolveBond(label: String): Bond? = bonds.firstOrNull { bondPrefix + it.name == label }

    private fun closeWindow() {
        val stage = operationsTabPane.scene.window as Stage
        stage.close()
    }

    @FXML
    private fun handleCancel() {
        closeWindow()
    }
}
