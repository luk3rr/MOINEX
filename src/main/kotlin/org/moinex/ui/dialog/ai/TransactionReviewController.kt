/*
 * Filename: TransactionReviewController.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.ai

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
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
import javafx.util.StringConverter
import javafx.util.converter.IntegerStringConverter
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.atEndOfDay
import org.moinex.common.extension.isEqual
import org.moinex.common.util.WindowUtils
import org.moinex.model.Category
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.model.dto.CreditCardInvoicePaymentDTO
import org.moinex.model.dto.ai.ExtractedTransactionDTO
import org.moinex.model.dto.ai.TransactionKind
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.common.DatePickerTableCell
import org.moinex.ui.common.bigDecimalTableConverter
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.text.MessageFormat
import java.time.LocalDate
import java.time.YearMonth

class TransactionReviewRow(
    val kind: TransactionKind,
    val date: SimpleObjectProperty<LocalDate>,
    val description: SimpleStringProperty,
    val amount: SimpleObjectProperty<BigDecimal>,
    val movementType: SimpleObjectProperty<WalletTransactionType>,
    val categoryName: SimpleStringProperty,
    val walletOrCardLabel: SimpleStringProperty,
    val confirm: SimpleBooleanProperty,
    val possibleDuplicate: Boolean,
    val lowConfidence: Boolean,
    val invoiceMonth: YearMonth?,
    val existingTransactionId: Int?,
    val installments: SimpleObjectProperty<Int>,
    val installmentNumber: Int,
)

/**
 * Wallet and credit card transactions each get their own tab/table - a credit card row needs
 * installments and no movement type, a wallet row needs movement type and no installments, so one
 * shared table would always carry columns that don't apply to half its rows.
 */
@Controller
class TransactionReviewController(
    private val categoryService: CategoryService,
    private val walletService: WalletService,
    private val creditCardService: CreditCardService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var transactionsTabPane: TabPane

    private lateinit var walletTable: TableView<TransactionReviewRow>
    private lateinit var creditCardTable: TableView<TransactionReviewRow>

    private var categories: MutableList<Category> = mutableListOf()
    private var wallets: List<Wallet> = emptyList()
    private var creditCards: List<CreditCard> = emptyList()

    companion object {
        private const val LOW_CONFIDENCE_THRESHOLD = 0.6
        private const val DUPLICATE_WINDOW_DAYS = 3L
        private const val STYLE_ROW_DUPLICATE = "ai-review-row-duplicate"
        private const val STYLE_ROW_LOW_CONFIDENCE = "ai-review-row-low-confidence"

        private const val COLUMN_WIDTH_CONFIRM = 70.0
        private const val COLUMN_WIDTH_DATE = 100.0
        private const val COLUMN_WIDTH_KIND = 130.0
        private const val COLUMN_WIDTH_DESCRIPTION = 200.0
        private const val COLUMN_WIDTH_AMOUNT = 100.0
        private const val COLUMN_WIDTH_INSTALLMENTS = 90.0
        private const val COLUMN_WIDTH_MOVEMENT_TYPE = 90.0
        private const val COLUMN_WIDTH_CATEGORY = 150.0
        private const val COLUMN_WIDTH_WALLET = 160.0
        private const val COLUMN_WIDTH_NOTES = 240.0

        private val CREDIT_CARD_KINDS = setOf(TransactionKind.CREDIT_CARD, TransactionKind.CREDIT_CARD_PAYMENT)
    }

    @FXML
    private fun initialize() {
        categories = categoryService.getNonArchivedCategoriesOrderedByName().toMutableList()
        wallets = walletService.getAllNonArchivedWalletsOrderedByName()
        creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByName()

        walletTable = buildWalletTable()
        creditCardTable = buildCreditCardTable()
    }

    fun setProposedTransactions(transactions: List<ExtractedTransactionDTO>) {
        val rows = transactions.map(::toRow)

        walletTable.items.setAll(rows.filter { it.kind == TransactionKind.WALLET })
        creditCardTable.items.setAll(rows.filter { it.kind in CREDIT_CARD_KINDS })

        refreshTabs()
    }

    /** Only shows a tab for a kind group that actually has rows to review. */
    private fun refreshTabs() {
        val tabs = mutableListOf<Tab>()
        if (walletTable.items.isNotEmpty()) {
            tabs.add(Tab(preferencesService.translate(TranslationKeys.AI_REVIEW_TAB_WALLET), walletTable))
        }
        if (creditCardTable.items.isNotEmpty()) {
            tabs.add(Tab(preferencesService.translate(TranslationKeys.AI_REVIEW_TAB_CREDIT_CARD), creditCardTable))
        }
        transactionsTabPane.tabs.setAll(tabs)
    }

    fun getRemainingTransactions(): List<ExtractedTransactionDTO> =
        (walletTable.items + creditCardTable.items).map { row ->
            val wallet = if (row.kind == TransactionKind.WALLET) resolveWallet(row.walletOrCardLabel.value) else null
            val creditCard =
                if (row.kind != TransactionKind.WALLET) resolveCreditCard(row.walletOrCardLabel.value) else null
            val category = categories.firstOrNull { it.name.equals(row.categoryName.value, ignoreCase = true) }

            ExtractedTransactionDTO(
                date = row.date.value,
                description = row.description.value,
                amount = row.amount.value,
                kind = row.kind,
                movementType = row.movementType.value,
                invoiceMonth = row.invoiceMonth,
                existingTransactionId = row.existingTransactionId,
                installments = row.installments.value,
                installmentNumber = row.installmentNumber,
                suggestedWalletId = wallet?.id,
                suggestedCreditCardId = creditCard?.id,
                suggestedCategoryId = category?.id,
                suggestedCategoryName = row.categoryName.value,
                confidence = if (row.lowConfidence) 0.0 else 1.0,
                possibleDuplicate = row.possibleDuplicate,
            )
        }

    private fun toRow(dto: ExtractedTransactionDTO): TransactionReviewRow {
        val walletOrCardLabel =
            when (dto.kind) {
                TransactionKind.WALLET ->
                    (wallets.firstOrNull { it.id == dto.suggestedWalletId } ?: wallets.firstOrNull())?.name
                TransactionKind.CREDIT_CARD, TransactionKind.CREDIT_CARD_PAYMENT ->
                    (creditCards.firstOrNull { it.id == dto.suggestedCreditCardId } ?: creditCards.firstOrNull())?.name
            }.orEmpty()

        return TransactionReviewRow(
            kind = dto.kind,
            date = SimpleObjectProperty(dto.date),
            description = SimpleStringProperty(dto.description),
            amount = SimpleObjectProperty(dto.amount),
            movementType = SimpleObjectProperty(dto.movementType),
            categoryName = SimpleStringProperty(dto.suggestedCategoryName),
            walletOrCardLabel = SimpleStringProperty(walletOrCardLabel),
            confirm = SimpleBooleanProperty(true),
            possibleDuplicate = detectPossibleDuplicate(dto),
            lowConfidence = dto.confidence < LOW_CONFIDENCE_THRESHOLD,
            invoiceMonth = dto.invoiceMonth,
            existingTransactionId = dto.existingTransactionId,
            installments = SimpleObjectProperty(dto.installments),
            installmentNumber = dto.installmentNumber,
        )
    }

    private fun detectPossibleDuplicate(dto: ExtractedTransactionDTO): Boolean {
        if (dto.existingTransactionId != null) return false

        if (dto.installmentNumber > 1) {
            return dto.kind == TransactionKind.CREDIT_CARD &&
                creditCardService
                    .getDebtsBetweenForAnalysis(
                        dto.date.minusDays(DUPLICATE_WINDOW_DAYS).atStartOfDay(),
                        dto.date.plusDays(DUPLICATE_WINDOW_DAYS).atEndOfDay(),
                    ).any { it.creditCard.id == dto.suggestedCreditCardId && it.installments > 1 }
        }

        val start = dto.date.minusDays(DUPLICATE_WINDOW_DAYS).atStartOfDay()
        val end = dto.date.plusDays(DUPLICATE_WINDOW_DAYS).atEndOfDay()

        return when (dto.kind) {
            TransactionKind.WALLET ->
                walletService
                    .getAllNonArchivedWalletTransactionsBetweenDates(start, end)
                    .any { it.amount.isEqual(dto.amount) }
            TransactionKind.CREDIT_CARD ->
                creditCardService
                    .getDebtsBetweenForAnalysis(start, end)
                    .any { it.amount.isEqual(dto.amount) }
            TransactionKind.CREDIT_CARD_PAYMENT -> false
        }
    }

    private fun buildWalletTable(): TableView<TransactionReviewRow> {
        val table = newTableView()
        table.columns.setAll(
            listOf(
                confirmColumn(),
                dateColumn(),
                descriptionColumn(),
                amountColumn(),
                movementTypeColumn(),
                categoryColumn(),
                walletColumn(),
                notesColumn(),
            ),
        )
        return table
    }

    private fun buildCreditCardTable(): TableView<TransactionReviewRow> {
        val table = newTableView()
        table.columns.setAll(
            listOf(
                confirmColumn(),
                dateColumn(),
                kindColumn(),
                descriptionColumn(),
                amountColumn(),
                installmentsColumn(),
                categoryColumn(),
                creditCardColumn(),
                notesColumn(),
            ),
        )
        return table
    }

    private fun newTableView(): TableView<TransactionReviewRow> =
        TableView<TransactionReviewRow>().apply {
            isEditable = true
            stylesheets.add(
                TransactionReviewController::class.java.getResource(Files.TABLE_VIEW_STYLE_SHEET)!!.toExternalForm(),
            )
            setRowFactory {
                TableRow<TransactionReviewRow>().apply {
                    itemProperty().addListener { _, _, row ->
                        styleClass.removeAll(STYLE_ROW_DUPLICATE, STYLE_ROW_LOW_CONFIDENCE)
                        if (row?.possibleDuplicate == true) styleClass.add(STYLE_ROW_DUPLICATE)
                        if (row?.lowConfidence == true) styleClass.add(STYLE_ROW_LOW_CONFIDENCE)
                    }
                }
            }
        }

    private fun confirmColumn(): TableColumn<TransactionReviewRow, Boolean> {
        val column =
            TableColumn<TransactionReviewRow, Boolean>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_CONFIRM),
            )
        column.cellValueFactory = Callback { it.value.confirm }
        column.cellFactory = CheckBoxTableCell.forTableColumn(column)
        column.prefWidth = COLUMN_WIDTH_CONFIRM
        return column
    }

    private fun dateColumn(): TableColumn<TransactionReviewRow, LocalDate> {
        val column =
            TableColumn<TransactionReviewRow, LocalDate>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_DATE),
            )
        column.cellValueFactory = Callback { it.value.date }
        column.cellFactory = Callback { DatePickerTableCell() }
        column.prefWidth = COLUMN_WIDTH_DATE
        return column
    }

    private fun kindColumn(): TableColumn<TransactionReviewRow, String> {
        val column =
            TableColumn<TransactionReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_KIND),
            )
        column.cellValueFactory = Callback { SimpleStringProperty(creditCardKindLabel(it.value.kind)) }
        column.prefWidth = COLUMN_WIDTH_KIND
        return column
    }

    private fun creditCardKindLabel(kind: TransactionKind): String =
        preferencesService.translate(
            when (kind) {
                TransactionKind.CREDIT_CARD_PAYMENT -> TranslationKeys.AI_REVIEW_KIND_CREDIT_CARD_PAYMENT
                else -> TranslationKeys.AI_REVIEW_KIND_CREDIT_CARD_PURCHASE
            },
        )

    private fun descriptionColumn(): TableColumn<TransactionReviewRow, String> {
        val column =
            TableColumn<TransactionReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_DESCRIPTION),
            )
        column.cellValueFactory = Callback { it.value.description }
        column.cellFactory = TextFieldTableCell.forTableColumn()
        column.prefWidth = COLUMN_WIDTH_DESCRIPTION
        return column
    }

    private fun amountColumn(): TableColumn<TransactionReviewRow, BigDecimal> {
        val column =
            TableColumn<TransactionReviewRow, BigDecimal>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_AMOUNT),
            )
        column.cellValueFactory = Callback { it.value.amount }
        column.cellFactory = TextFieldTableCell.forTableColumn(bigDecimalTableConverter())
        column.prefWidth = COLUMN_WIDTH_AMOUNT
        return column
    }

    private fun installmentsColumn(): TableColumn<TransactionReviewRow, Int> {
        val column =
            TableColumn<TransactionReviewRow, Int>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_INSTALLMENTS),
            )
        column.cellValueFactory = Callback { it.value.installments }
        column.cellFactory = TextFieldTableCell.forTableColumn(IntegerStringConverter())
        column.prefWidth = COLUMN_WIDTH_INSTALLMENTS
        return column
    }

    private fun movementTypeColumn(): TableColumn<TransactionReviewRow, WalletTransactionType> {
        val movementTypes =
            FXCollections.observableArrayList(
                WalletTransactionType.INCOME,
                WalletTransactionType.EXPENSE,
            )
        val column =
            TableColumn<TransactionReviewRow, WalletTransactionType>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_MOVEMENT_TYPE),
            )
        column.cellValueFactory = Callback { it.value.movementType }
        column.cellFactory = ComboBoxTableCell.forTableColumn(movementTypeConverter(), movementTypes)
        column.prefWidth = COLUMN_WIDTH_MOVEMENT_TYPE
        return column
    }

    private fun categoryColumn(): TableColumn<TransactionReviewRow, String> {
        val categoryNames = FXCollections.observableArrayList(categories.map { it.name })
        val column =
            TableColumn<TransactionReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_CATEGORY),
            )
        column.cellValueFactory = Callback { it.value.categoryName }
        column.cellFactory =
            Callback { _: TableColumn<TransactionReviewRow, String> ->
                ComboBoxTableCell<TransactionReviewRow, String>(categoryNames).apply { isComboBoxEditable = true }
            }
        column.prefWidth = COLUMN_WIDTH_CATEGORY
        return column
    }

    private fun walletColumn(): TableColumn<TransactionReviewRow, String> {
        val walletNames = FXCollections.observableArrayList(wallets.map { it.name })
        val column =
            TableColumn<TransactionReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_WALLET),
            )
        column.cellValueFactory = Callback { it.value.walletOrCardLabel }
        column.cellFactory = ComboBoxTableCell.forTableColumn(walletNames)
        column.prefWidth = COLUMN_WIDTH_WALLET
        return column
    }

    private fun creditCardColumn(): TableColumn<TransactionReviewRow, String> {
        val creditCardNames = FXCollections.observableArrayList(creditCards.map { it.name })
        val column =
            TableColumn<TransactionReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_CREDIT_CARD),
            )
        column.cellValueFactory = Callback { it.value.walletOrCardLabel }
        column.cellFactory = ComboBoxTableCell.forTableColumn(creditCardNames)
        column.prefWidth = COLUMN_WIDTH_WALLET
        return column
    }

    private fun notesColumn(): TableColumn<TransactionReviewRow, String> {
        val column =
            TableColumn<TransactionReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_NOTES),
            )
        column.cellValueFactory =
            Callback { data -> Bindings.createStringBinding({ buildNotes(data.value) }, data.value.categoryName) }
        column.prefWidth = COLUMN_WIDTH_NOTES
        return column
    }

    private fun buildNotes(row: TransactionReviewRow): String {
        val notes = mutableListOf<String>()
        if (row.existingTransactionId != null) {
            notes.add(preferencesService.translate(TranslationKeys.AI_REVIEW_CONFIRMS_PENDING))
        }
        if (row.installmentNumber > 1) {
            val totalInstallments = row.installmentNumber - 1 + row.installments.value
            notes.add(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.AI_REVIEW_INSTALLMENT_CONTINUATION),
                    row.installmentNumber,
                    totalInstallments,
                ),
            )
        }
        if (row.possibleDuplicate) notes.add(preferencesService.translate(TranslationKeys.AI_REVIEW_POSSIBLE_DUPLICATE))
        if (row.lowConfidence) notes.add(preferencesService.translate(TranslationKeys.AI_REVIEW_LOW_CONFIDENCE))
        if (row.kind != TransactionKind.CREDIT_CARD_PAYMENT &&
            categories.none { it.name.equals(row.categoryName.value, ignoreCase = true) }
        ) {
            notes.add(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.AI_REVIEW_NEW_CATEGORY),
                    row.categoryName.value,
                ),
            )
        }
        return notes.joinToString("; ")
    }

    private fun movementTypeConverter() =
        object : StringConverter<WalletTransactionType>() {
            override fun toString(value: WalletTransactionType?): String =
                when (value) {
                    WalletTransactionType.INCOME ->
                        preferencesService.translate(
                            TranslationKeys.TRANSACTION_TYPE_INCOME,
                        )
                    else -> preferencesService.translate(TranslationKeys.TRANSACTION_TYPE_EXPENSE)
                }

            override fun fromString(text: String?): WalletTransactionType =
                if (text == preferencesService.translate(TranslationKeys.TRANSACTION_TYPE_INCOME)) {
                    WalletTransactionType.INCOME
                } else {
                    WalletTransactionType.EXPENSE
                }
        }

    @FXML
    private fun handleConfirm() {
        val allTables = listOf(walletTable, creditCardTable)
        val rowsToConfirm = allTables.flatMap { table -> table.items.filter { it.confirm.value } }
        if (rowsToConfirm.isEmpty()) {
            closeWindow()
            return
        }

        val succeeded = mutableListOf<TransactionReviewRow>()
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
                preferencesService.translate(TranslationKeys.AI_REVIEW_TITLE),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.AI_REVIEW_CONFIRMED_COUNT),
                    succeeded.size,
                ),
                preferencesService.bundle,
            )
            closeWindow()
        } else {
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.AI_REVIEW_TITLE),
                errors.joinToString("\n"),
                preferencesService.bundle,
            )
        }
    }

    private fun describeRow(row: TransactionReviewRow): String = "${row.date.value} - ${row.description.value}"

    private fun persist(row: TransactionReviewRow) {
        val date = row.date.value.atStartOfDay()

        when (row.kind) {
            TransactionKind.WALLET -> {
                val wallet =
                    resolveWallet(row.walletOrCardLabel.value)
                        ?: error(preferencesService.translate(TranslationKeys.AI_REVIEW_NO_WALLET_SELECTED))
                val transaction =
                    WalletTransaction(
                        id = row.existingTransactionId,
                        date = date,
                        status = WalletTransactionStatus.CONFIRMED,
                        description = row.description.value,
                        includeInAnalysis = true,
                        wallet = wallet,
                        category = resolveCategory(row.categoryName.value),
                        type = row.movementType.value,
                        amount = row.amount.value,
                    )

                if (row.existingTransactionId != null) {
                    walletService.updateWalletTransaction(transaction)
                } else {
                    walletService.createWalletTransaction(transaction, publishNotification = true)
                }
            }
            TransactionKind.CREDIT_CARD -> {
                val creditCard =
                    resolveCreditCard(row.walletOrCardLabel.value)
                        ?: error(preferencesService.translate(TranslationKeys.AI_REVIEW_NO_WALLET_SELECTED))
                val invoiceMonth =
                    if (row.installmentNumber > 1) {
                        creditCardService.currentInvoiceMonth(creditCard)
                    } else {
                        creditCardService.invoiceMonthForPurchaseDate(creditCard, row.date.value)
                    }

                creditCardService.createDebt(
                    CreditCardDebt(
                        category = resolveCategory(row.categoryName.value),
                        installments = row.installments.value,
                        creditCard = creditCard,
                        date = date,
                        amount = row.amount.value,
                        description = row.description.value,
                    ),
                    invoiceMonth = invoiceMonth,
                    publishNotification = true,
                )
            }
            TransactionKind.CREDIT_CARD_PAYMENT -> {
                val creditCard =
                    resolveCreditCard(row.walletOrCardLabel.value)
                        ?: error(preferencesService.translate(TranslationKeys.AI_REVIEW_NO_WALLET_SELECTED))
                val billingWallet =
                    creditCard.defaultBillingWallet
                        ?: error(preferencesService.translate(TranslationKeys.AI_REVIEW_NO_BILLING_WALLET))

                creditCardService.payInvoice(
                    CreditCardInvoicePaymentDTO(
                        creditCardId = creditCard.id!!,
                        billingWalletId = billingWallet.id!!,
                        invoiceDate = row.invoiceMonth ?: YearMonth.from(row.date.value),
                        amount = row.amount.value,
                    ),
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

    private fun resolveCreditCard(name: String): CreditCard? = creditCards.firstOrNull { it.name == name }

    private fun closeWindow() {
        val stage = transactionsTabPane.scene.window as Stage
        stage.close()
    }

    @FXML
    private fun handleCancel() {
        closeWindow()
    }
}
