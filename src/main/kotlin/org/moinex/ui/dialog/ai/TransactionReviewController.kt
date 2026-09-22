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

@Controller
class TransactionReviewController(
    private val categoryService: CategoryService,
    private val walletService: WalletService,
    private val creditCardService: CreditCardService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var transactionsTable: TableView<TransactionReviewRow>

    private var categories: MutableList<Category> = mutableListOf()
    private var wallets: List<Wallet> = emptyList()
    private var creditCards: List<CreditCard> = emptyList()
    private val walletPrefix = preferencesService.translate(TranslationKeys.AI_REVIEW_WALLET_PREFIX) + " "
    private val creditCardPrefix = preferencesService.translate(TranslationKeys.AI_REVIEW_CREDIT_CARD_PREFIX) + " "

    companion object {
        private const val LOW_CONFIDENCE_THRESHOLD = 0.6
        private const val DUPLICATE_WINDOW_DAYS = 3L
        private const val STYLE_ROW_DUPLICATE = "ai-review-row-duplicate"
        private const val STYLE_ROW_LOW_CONFIDENCE = "ai-review-row-low-confidence"

        private const val COLUMN_WIDTH_CONFIRM = 70.0
        private const val COLUMN_WIDTH_DATE = 100.0
        private const val COLUMN_WIDTH_DESCRIPTION = 200.0
        private const val COLUMN_WIDTH_AMOUNT = 100.0
        private const val COLUMN_WIDTH_INSTALLMENTS = 90.0
        private const val COLUMN_WIDTH_MOVEMENT_TYPE = 90.0
        private const val COLUMN_WIDTH_CATEGORY = 150.0
        private const val COLUMN_WIDTH_WALLET = 160.0
        private const val COLUMN_WIDTH_NOTES = 240.0
    }

    @FXML
    private fun initialize() {
        categories = categoryService.getNonArchivedCategoriesOrderedByName().toMutableList()
        wallets = walletService.getAllNonArchivedWalletsOrderedByName()
        creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByName()

        transactionsTable.isEditable = true
        configureColumns()
    }

    fun setProposedTransactions(transactions: List<ExtractedTransactionDTO>) {
        transactionsTable.items.setAll(transactions.map(::toRow))
    }

    fun getRemainingTransactions(): List<ExtractedTransactionDTO> =
        transactionsTable.items.map { row ->
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
        val walletLabel =
            when (dto.kind) {
                TransactionKind.WALLET ->
                    (wallets.firstOrNull { it.id == dto.suggestedWalletId } ?: wallets.firstOrNull())
                        ?.let { walletPrefix + it.name }
                TransactionKind.CREDIT_CARD, TransactionKind.CREDIT_CARD_PAYMENT ->
                    (creditCards.firstOrNull { it.id == dto.suggestedCreditCardId } ?: creditCards.firstOrNull())
                        ?.let { creditCardPrefix + it.name }
            }.orEmpty()

        return TransactionReviewRow(
            kind = dto.kind,
            date = SimpleObjectProperty(dto.date),
            description = SimpleStringProperty(dto.description),
            amount = SimpleObjectProperty(dto.amount),
            movementType = SimpleObjectProperty(dto.movementType),
            categoryName = SimpleStringProperty(dto.suggestedCategoryName),
            walletOrCardLabel = SimpleStringProperty(walletLabel),
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

    private fun configureColumns() {
        val confirmColumn =
            TableColumn<TransactionReviewRow, Boolean>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_CONFIRM),
            )
        confirmColumn.cellValueFactory = Callback { it.value.confirm }
        confirmColumn.cellFactory = CheckBoxTableCell.forTableColumn(confirmColumn)
        confirmColumn.prefWidth = COLUMN_WIDTH_CONFIRM

        val dateColumn =
            TableColumn<TransactionReviewRow, LocalDate>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_DATE),
            )
        dateColumn.cellValueFactory = Callback { it.value.date }
        dateColumn.cellFactory = Callback { DatePickerTableCell() }
        dateColumn.prefWidth = COLUMN_WIDTH_DATE

        val descriptionColumn =
            TableColumn<TransactionReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_DESCRIPTION),
            )
        descriptionColumn.cellValueFactory = Callback { it.value.description }
        descriptionColumn.cellFactory = TextFieldTableCell.forTableColumn()
        descriptionColumn.prefWidth = COLUMN_WIDTH_DESCRIPTION

        val amountColumn =
            TableColumn<TransactionReviewRow, BigDecimal>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_AMOUNT),
            )
        amountColumn.cellValueFactory = Callback { it.value.amount }
        amountColumn.cellFactory = TextFieldTableCell.forTableColumn(bigDecimalTableConverter())
        amountColumn.prefWidth = COLUMN_WIDTH_AMOUNT

        val installmentsColumn =
            TableColumn<TransactionReviewRow, Int>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_INSTALLMENTS),
            )
        installmentsColumn.cellValueFactory = Callback { it.value.installments }
        installmentsColumn.cellFactory = TextFieldTableCell.forTableColumn(IntegerStringConverter())
        installmentsColumn.prefWidth = COLUMN_WIDTH_INSTALLMENTS

        val movementTypes =
            FXCollections.observableArrayList(
                WalletTransactionType.INCOME,
                WalletTransactionType.EXPENSE,
            )
        val movementTypeColumn =
            TableColumn<TransactionReviewRow, WalletTransactionType>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_MOVEMENT_TYPE),
            )
        movementTypeColumn.cellValueFactory = Callback { it.value.movementType }
        movementTypeColumn.cellFactory = ComboBoxTableCell.forTableColumn(movementTypeConverter(), movementTypes)
        movementTypeColumn.prefWidth = COLUMN_WIDTH_MOVEMENT_TYPE

        val categoryNames = FXCollections.observableArrayList(categories.map { it.name })
        val categoryColumn =
            TableColumn<TransactionReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_CATEGORY),
            )
        categoryColumn.cellValueFactory = Callback { it.value.categoryName }
        categoryColumn.cellFactory =
            Callback { _: TableColumn<TransactionReviewRow, String> ->
                ComboBoxTableCell<TransactionReviewRow, String>(categoryNames).apply { isComboBoxEditable = true }
            }
        categoryColumn.prefWidth = COLUMN_WIDTH_CATEGORY

        val walletLabels =
            FXCollections.observableArrayList(
                wallets.map { walletPrefix + it.name } + creditCards.map { creditCardPrefix + it.name },
            )
        val walletColumn =
            TableColumn<TransactionReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_WALLET),
            )
        walletColumn.cellValueFactory = Callback { it.value.walletOrCardLabel }
        walletColumn.cellFactory = ComboBoxTableCell.forTableColumn(walletLabels)
        walletColumn.prefWidth = COLUMN_WIDTH_WALLET

        val notesColumn =
            TableColumn<TransactionReviewRow, String>(
                preferencesService.translate(TranslationKeys.AI_REVIEW_COLUMN_NOTES),
            )
        notesColumn.cellValueFactory =
            Callback { data -> Bindings.createStringBinding({ buildNotes(data.value) }, data.value.categoryName) }
        notesColumn.prefWidth = COLUMN_WIDTH_NOTES

        transactionsTable.columns.setAll(
            listOf(
                confirmColumn,
                dateColumn,
                descriptionColumn,
                amountColumn,
                installmentsColumn,
                movementTypeColumn,
                categoryColumn,
                walletColumn,
                notesColumn,
            ),
        )

        transactionsTable.setRowFactory {
            TableRow<TransactionReviewRow>().apply {
                itemProperty().addListener { _, _, row ->
                    styleClass.removeAll(STYLE_ROW_DUPLICATE, STYLE_ROW_LOW_CONFIDENCE)
                    if (row?.possibleDuplicate == true) styleClass.add(STYLE_ROW_DUPLICATE)
                    if (row?.lowConfidence == true) styleClass.add(STYLE_ROW_LOW_CONFIDENCE)
                }
            }
        }
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
        val rowsToConfirm = transactionsTable.items.filter { it.confirm.value }
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

        transactionsTable.items.removeAll(succeeded)

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

    private fun resolveWallet(label: String): Wallet? = wallets.firstOrNull { walletPrefix + it.name == label }

    private fun resolveCreditCard(label: String): CreditCard? =
        creditCards.firstOrNull {
            creditCardPrefix + it.name ==
                label
        }

    private fun closeWindow() {
        val stage = transactionsTable.scene.window as Stage
        stage.close()
    }

    @FXML
    private fun handleCancel() {
        closeWindow()
    }
}
