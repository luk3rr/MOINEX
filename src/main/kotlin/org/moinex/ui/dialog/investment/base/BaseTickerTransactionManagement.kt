/*
 * Filename: BaseTickerTransactionManagement.kt (original filename: BaseTickerTransactionManagement.java)
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.base

import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.helper.SuggestionsHandlerHelper
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.Category
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.investment.Ticker
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal

abstract class BaseTickerTransactionManagement(
    protected val walletService: WalletService,
    protected val categoryService: CategoryService,
    protected val tickerService: TickerService,
    protected val preferencesService: PreferencesService,
) {
    @FXML
    protected lateinit var tickerNameLabel: Label

    @FXML
    protected lateinit var walletAfterBalanceValueLabel: Label

    @FXML
    protected lateinit var walletCurrentBalanceValueLabel: Label

    @FXML
    protected lateinit var descriptionField: TextField

    @FXML
    protected lateinit var unitPriceField: TextField

    @FXML
    protected lateinit var quantityField: TextField

    @FXML
    protected lateinit var totalPriceLabel: Label

    @FXML
    protected lateinit var walletComboBox: ComboBox<Wallet>

    @FXML
    protected lateinit var statusComboBox: ComboBox<WalletTransactionStatus>

    @FXML
    protected lateinit var categoryComboBox: ComboBox<Category>

    @FXML
    protected lateinit var transactionDatePicker: DatePicker

    @FXML
    protected lateinit var includeInAnalysisCheckBox: CheckBox

    protected lateinit var suggestionsHandler: SuggestionsHandlerHelper<WalletTransaction>
    protected var wallets: List<Wallet> = emptyList()
    protected var categories: List<Category> = emptyList()
    protected lateinit var ticker: Ticker
    protected var wallet: Wallet? = null
    protected lateinit var walletTransactionType: WalletTransactionType

    fun setWalletComboBox(wt: Wallet) {
        if (wallets.none { it.id == wt.id }) {
            return
        }

        wallet = wt
        walletComboBox.value = wallet
        UIUtils.updateWalletBalanceLabelStyle(wt, walletCurrentBalanceValueLabel)
    }

    fun initializeTicker(ticker: Ticker) {
        this.ticker = ticker
        tickerNameLabel.text = "${ticker.name} (${ticker.symbol})"
        unitPriceField.text = ticker.currentUnitValue.toString()
    }

    @FXML
    protected open fun initialize() {
        configureSuggestions()
        configureListeners()
        configureComboBoxes()

        loadWalletsFromDatabase()
        loadCategoriesFromDatabase()
        loadSuggestionsFromDatabase()

        populateComboBoxes()

        UIUtils.setDatePickerFormat(transactionDatePicker)

        UIUtils.resetLabel(walletAfterBalanceValueLabel)
        UIUtils.resetLabel(walletCurrentBalanceValueLabel)

        walletComboBox.setOnAction {
            UIUtils.updateWalletBalanceLabelStyle(walletComboBox.value, walletCurrentBalanceValueLabel)
            walletAfterBalance()
        }
    }

    @FXML
    protected open fun handleCancel() {
        (tickerNameLabel.scene.window as Stage).close()
    }

    @FXML
    protected abstract fun handleSave()

    protected fun updateTotalPrice() {
        val unitPriceStr = unitPriceField.text
        val quantityStr = quantityField.text

        var totalPrice = BigDecimal("0.00")

        if (unitPriceStr.isNullOrBlank() || quantityStr.isNullOrBlank()) {
            totalPriceLabel.text = UIUtils.formatCurrency(totalPrice)
            return
        }

        runCatching {
            val unitPrice = unitPriceStr.toBigDecimal()
            val quantity = quantityStr.toBigDecimal()

            totalPrice = unitPrice.multiply(quantity)
            totalPriceLabel.text = UIUtils.formatCurrency(totalPrice)
        }.onFailure {
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_CALCULATION_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_CALCULATION_MESSAGE),
            )

            totalPrice = BigDecimal("0.00")
            totalPriceLabel.text = UIUtils.formatCurrency(totalPrice)
        }
    }

    protected fun walletAfterBalance() {
        val unitPriceStr = unitPriceField.text
        val quantityStr = quantityField.text
        val wt = walletComboBox.value

        if (unitPriceStr.isNullOrBlank() || quantityStr.isNullOrBlank() || wt == null) {
            UIUtils.resetLabel(walletAfterBalanceValueLabel)
            return
        }

        runCatching {
            val transactionValue = unitPriceStr.toBigDecimal().multiply(quantityStr.toBigDecimal())

            if (transactionValue < BigDecimal.ZERO) {
                UIUtils.resetLabel(walletAfterBalanceValueLabel)
                return
            }

            val walletAfterBalanceValue = calculateWalletBalance(wt, transactionValue)

            if (walletAfterBalanceValue < BigDecimal.ZERO) {
                UIUtils.setLabelStyle(walletAfterBalanceValueLabel, Styles.NEGATIVE_BALANCE_STYLE)
            } else {
                UIUtils.setLabelStyle(walletAfterBalanceValueLabel, Styles.NEUTRAL_BALANCE_STYLE)
            }

            walletAfterBalanceValueLabel.text = UIUtils.formatCurrency(walletAfterBalanceValue)
        }.onFailure {
            UIUtils.resetLabel(walletAfterBalanceValueLabel)
        }
    }

    private fun calculateWalletBalance(
        wallet: Wallet,
        transactionValue: BigDecimal,
    ): BigDecimal =
        when (walletTransactionType) {
            WalletTransactionType.EXPENSE -> wallet.balance.subtract(transactionValue)
            WalletTransactionType.INCOME -> wallet.balance.add(transactionValue)
        }

    protected fun loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName()
    }

    protected fun loadCategoriesFromDatabase() {
        categories = categoryService.getNonArchivedCategoriesOrderedByName()
    }

    protected fun loadSuggestionsFromDatabase() {
        suggestionsHandler.suggestions = walletService.getWalletTransactionSuggestionsByType(walletTransactionType)
    }

    protected fun populateComboBoxes() {
        walletComboBox.items.setAll(wallets)
        statusComboBox.items.addAll(*WalletTransactionStatus.entries.toTypedArray())
        categoryComboBox.items.setAll(categories)

        if (categories.isEmpty()) {
            UIUtils.addTooltipToNode(
                categoryComboBox,
                preferencesService.translate(TranslationKeys.INVESTMENT_TOOLTIP_CATEGORY_REQUIRED),
            )
        }
    }

    protected fun configureComboBoxes() {
        UIUtils.configureComboBox(walletComboBox, Wallet::name)
        UIUtils.configureComboBox(statusComboBox, UIUtils::translateTransactionStatus)
        UIUtils.configureComboBox(categoryComboBox, Category::name)
    }

    protected fun configureListeners() {
        unitPriceField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.INVESTMENT_VALUE_REGEX))) {
                unitPriceField.text = oldValue
            } else {
                updateTotalPrice()
                walletAfterBalance()
            }
        }

        quantityField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.INVESTMENT_VALUE_REGEX))) {
                quantityField.text = oldValue
            } else {
                updateTotalPrice()
                walletAfterBalance()
            }
        }
    }

    protected fun configureSuggestions() {
        val filterFunction: (WalletTransaction) -> String = { it.description ?: "" }

        val displayFunction: (WalletTransaction) -> String = { wt ->
            "${wt.description}\n${UIUtils.formatCurrency(wt.amount)} | ${wt.wallet.name} | ${wt.category.name} "
        }

        val onSelectCallback: (WalletTransaction) -> Unit = ::fillFieldsWithTransaction

        suggestionsHandler =
            SuggestionsHandlerHelper(
                descriptionField,
                filterFunction,
                displayFunction,
                onSelectCallback,
            )

        suggestionsHandler.enable()
    }

    protected fun fillFieldsWithTransaction(wt: WalletTransaction) {
        walletComboBox.value = wt.wallet

        suggestionsHandler.disable()
        descriptionField.text = wt.description
        suggestionsHandler.enable()

        statusComboBox.value = wt.status
        categoryComboBox.value = wt.category

        UIUtils.updateWalletBalanceLabelStyle(walletComboBox.value, walletCurrentBalanceValueLabel)
        walletAfterBalance()
    }
}
