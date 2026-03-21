/*
 * Filename: BaseDividendManagement.kt (original filename: BaseDividendManagement.java)
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
import org.moinex.common.constant.Files
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
import org.moinex.service.CalculatorService
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.common.CalculatorController
import org.springframework.context.ConfigurableApplicationContext
import java.math.BigDecimal

abstract class BaseDividendManagement(
    protected val walletService: WalletService,
    protected val categoryService: CategoryService,
    protected val calculatorService: CalculatorService,
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
    protected lateinit var walletComboBox: ComboBox<Wallet>

    @FXML
    protected lateinit var statusComboBox: ComboBox<WalletTransactionStatus>

    @FXML
    protected lateinit var categoryComboBox: ComboBox<Category>

    @FXML
    protected lateinit var dividendValueField: TextField

    @FXML
    protected lateinit var descriptionField: TextField

    @FXML
    protected lateinit var dividendDatePicker: DatePicker

    @FXML
    protected lateinit var includeInAnalysisCheckBox: CheckBox

    protected lateinit var springContext: ConfigurableApplicationContext
    protected lateinit var suggestionsHandler: SuggestionsHandlerHelper<WalletTransaction>
    protected var wallets: List<Wallet> = emptyList()
    protected var categories: List<Category> = emptyList()
    protected lateinit var wallet: Wallet
    protected lateinit var ticker: Ticker

    fun setWalletComboBox(wt: Wallet) {
        if (wallets.none { it.id!! == wt.id!! }) {
            return
        }

        wallet = wt
        walletComboBox.value = wallet
        UIUtils.updateWalletBalanceLabelStyle(wallet, walletCurrentBalanceValueLabel)
    }

    fun initializeTicker(tk: Ticker) {
        ticker = tk
        tickerNameLabel.text = "${ticker.name} (${ticker.symbol})"
    }

    @FXML
    protected open fun initialize() {
        configureListeners()
        configureSuggestions()
        configureComboBoxes()

        loadWalletsFromDatabase()
        loadCategoriesFromDatabase()
        loadSuggestionsFromDatabase()

        populateComboBoxes()

        UIUtils.setDatePickerFormat(dividendDatePicker)

        UIUtils.resetLabel(walletAfterBalanceValueLabel)
        UIUtils.resetLabel(walletCurrentBalanceValueLabel)

        walletComboBox.setOnAction {
            UIUtils.updateWalletBalanceLabelStyle(walletComboBox.value, walletCurrentBalanceValueLabel)
            walletAfterBalance()
        }
    }

    @FXML
    protected open fun handleCancel() {
        (descriptionField.scene.window as Stage).close()
    }

    @FXML
    protected abstract fun handleSave()

    @FXML
    protected fun handleOpenCalculator() {
        WindowUtils.openPopupWindow(
            Files.CALCULATOR_FXML,
            preferencesService.translate(TranslationKeys.MAIN_CALCULATOR),
            springContext,
            { _: CalculatorController -> },
            listOf(Runnable { calculatorService.updateComponentWithResult(dividendValueField) }),
        )
    }

    protected fun walletAfterBalance() {
        val dividendValueString = dividendValueField.text
        val wt = walletComboBox.value

        if (dividendValueString.isNullOrBlank() || wt == null) {
            UIUtils.resetLabel(walletAfterBalanceValueLabel)
            return
        }

        runCatching {
            val dividendValue = dividendValueString.toBigDecimal()

            if (dividendValue < BigDecimal.ZERO) {
                UIUtils.resetLabel(walletAfterBalanceValueLabel)
                return
            }

            val walletAfterBalanceValue = wt.balance.add(dividendValue)

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

    protected fun loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName()
    }

    protected fun loadCategoriesFromDatabase() {
        categories = categoryService.getNonArchivedCategoriesOrderedByName()
    }

    protected fun loadSuggestionsFromDatabase() {
        suggestionsHandler.suggestions =
            walletService.getWalletTransactionSuggestionsByType(WalletTransactionType.INCOME)
    }

    protected fun populateComboBoxes() {
        walletComboBox.items.setAll(wallets)
        statusComboBox.items.addAll(*WalletTransactionStatus.entries.toTypedArray())
        categoryComboBox.items.setAll(categories)

        if (categories.isEmpty()) {
            UIUtils.addTooltipToNode(
                categoryComboBox,
                "You need to add a category before adding an dividend",
            )
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

    protected fun configureComboBoxes() {
        UIUtils.configureComboBox(walletComboBox, Wallet::name)
        UIUtils.configureComboBox(statusComboBox, UIUtils::translateTransactionStatus)
        UIUtils.configureComboBox(categoryComboBox, Category::name)
    }

    protected fun configureListeners() {
        dividendValueField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                dividendValueField.text = oldValue
            } else {
                walletAfterBalance()
            }
        }
    }

    protected fun fillFieldsWithTransaction(wt: WalletTransaction) {
        walletComboBox.value = wt.wallet

        suggestionsHandler.disable()
        descriptionField.text = wt.description
        suggestionsHandler.enable()

        dividendValueField.text = wt.amount.toString()
        statusComboBox.value = wt.status
        categoryComboBox.value = wt.category

        UIUtils.updateWalletBalanceLabelStyle(walletComboBox.value, walletCurrentBalanceValueLabel)
        walletAfterBalance()
    }
}
