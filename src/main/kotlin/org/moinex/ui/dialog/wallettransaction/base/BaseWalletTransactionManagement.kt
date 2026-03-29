/*
 * Filename: BaseWalletTransactionManagement.kt (original filename: BaseWalletTransactionManagement.java)
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.base

import javafx.beans.value.ChangeListener
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
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.CalculatorService
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.common.CalculatorController
import org.springframework.context.ConfigurableApplicationContext
import java.math.BigDecimal

/**
 * Base class for the wallet transaction dialog controllers
 */
abstract class BaseWalletTransactionManagement(
    protected val walletService: WalletService,
    protected val categoryService: CategoryService,
    protected val calculatorService: CalculatorService,
    protected val preferencesService: PreferencesService,
    protected val springContext: ConfigurableApplicationContext,
) {
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
    protected lateinit var transactionValueField: TextField

    @FXML
    protected lateinit var descriptionField: TextField

    @FXML
    protected lateinit var transactionDatePicker: DatePicker

    @FXML
    protected lateinit var includeInAnalysisCheckBox: CheckBox

    protected lateinit var suggestionsHandler: SuggestionsHandlerHelper<WalletTransaction>

    protected var wallets: List<Wallet> = emptyList()
    protected var categories: List<Category> = emptyList()
    protected lateinit var wallet: Wallet
    protected lateinit var walletTransactionType: WalletTransactionType

    private var transactionValueListener: ChangeListener<String>? = null

    @FXML
    protected abstract fun handleSave()

    protected abstract fun loadSuggestionsFromDatabase()

    fun setWalletComboBox(wt: Wallet) {
        if (wallets.none { it.id == wt.id }) {
            return
        }

        wallet = wt
        walletComboBox.value = wallet
        UIUtils.updateWalletBalanceLabelStyle(wallet, walletCurrentBalanceValueLabel)
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

        if (::includeInAnalysisCheckBox.isInitialized) {
            includeInAnalysisCheckBox.isSelected = true
        }

        UIUtils.resetLabel(walletAfterBalanceValueLabel)
        UIUtils.resetLabel(walletCurrentBalanceValueLabel)

        walletComboBox.setOnAction {
            UIUtils.updateWalletBalanceLabelStyle(
                walletComboBox.value,
                walletCurrentBalanceValueLabel,
            )
            walletAfterBalance()
        }
    }

    @FXML
    protected fun handleCancel() {
        val stage = descriptionField.scene.window as Stage
        stage.close()
    }

    @FXML
    protected fun handleOpenCalculator() {
        WindowUtils.openPopupWindow(
            Files.CALCULATOR_FXML,
            preferencesService.translate(TranslationKeys.WALLETTRANSACTION_LABEL_CALCULATOR),
            springContext,
            { _: CalculatorController -> },
            listOf(Runnable { calculatorService.updateComponentWithResult(transactionValueField) }),
        )
    }

    protected open fun walletAfterBalance() {
        val transactionValueString = transactionValueField.text
        val wt = walletComboBox.value

        if (transactionValueString.isNullOrBlank() || wt == null) {
            UIUtils.resetLabel(walletAfterBalanceValueLabel)
            return
        }

        runCatching {
            val transactionValue = BigDecimal(transactionValueString)

            if (transactionValue < BigDecimal.ZERO) {
                UIUtils.resetLabel(walletAfterBalanceValueLabel)
                return
            }

            val walletAfterBalanceValue = calculateBalanceAfterTransaction(wt, transactionValue)

            val style =
                if (walletAfterBalanceValue < BigDecimal.ZERO) {
                    Styles.NEGATIVE_BALANCE_STYLE
                } else {
                    Styles.NEUTRAL_BALANCE_STYLE
                }

            UIUtils.setLabelStyle(walletAfterBalanceValueLabel, style)
            walletAfterBalanceValueLabel.text = UIUtils.formatCurrency(walletAfterBalanceValue)
        }.onFailure {
            UIUtils.resetLabel(walletAfterBalanceValueLabel)
        }
    }

    private fun calculateBalanceAfterTransaction(
        wallet: Wallet,
        transactionValue: BigDecimal,
    ): BigDecimal =
        when (walletTransactionType) {
            WalletTransactionType.EXPENSE -> wallet.balance - transactionValue
            WalletTransactionType.INCOME -> wallet.balance + transactionValue
        }

    protected fun loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName()
    }

    protected fun loadCategoriesFromDatabase() {
        categories = categoryService.getNonArchivedCategoriesOrderedByName()
    }

    protected fun populateComboBoxes() {
        walletComboBox.items.setAll(wallets)
        statusComboBox.items.addAll(WalletTransactionStatus.entries)
        categoryComboBox.items.setAll(categories)

        if (categories.isEmpty()) {
            UIUtils.addTooltipToNode(
                categoryComboBox,
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_TOOLTIP_NEED_CATEGORY,
                ),
            )
        }
    }

    protected fun configureComboBoxes() {
        UIUtils.configureComboBox(walletComboBox) { it.name }
        UIUtils.configureComboBox(statusComboBox) { UIUtils.translateTransactionStatus(it) }
        UIUtils.configureComboBox(categoryComboBox) { it.name }
    }

    protected fun configureSuggestions() {
        val filterFunction: (WalletTransaction) -> String = { it.description ?: "" }

        val displayFunction: (WalletTransaction) -> String = { wt ->
            """
            ${wt.description}
            ${UIUtils.formatCurrency(wt.amount)} | ${wt.wallet.name} | ${wt.category.name}
            """.trimIndent()
        }

        val onSelectCallback: (WalletTransaction) -> Unit = { fillFieldsWithTransaction(it) }

        suggestionsHandler =
            SuggestionsHandlerHelper(
                descriptionField,
                filterFunction,
                displayFunction,
                onSelectCallback,
            )

        suggestionsHandler.enable()
    }

    protected fun configureListeners() {
        transactionValueListener =
            ChangeListener { _, oldValue, newValue ->
                if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                    transactionValueField.text = oldValue
                } else {
                    walletAfterBalance()
                }
            }

        transactionValueField.textProperty().addListener(transactionValueListener)
    }

    protected fun fillFieldsWithTransaction(wt: WalletTransaction) {
        walletComboBox.value = wt.wallet

        suggestionsHandler.disable()
        descriptionField.text = wt.description
        suggestionsHandler.enable()

        transactionValueField.text = wt.amount.toString()
        statusComboBox.value = wt.status
        categoryComboBox.value = wt.category

        UIUtils.updateWalletBalanceLabelStyle(
            walletComboBox.value,
            walletCurrentBalanceValueLabel,
        )
        walletAfterBalance()
    }

    fun disableTransactionValueListener() {
        transactionValueListener?.let {
            transactionValueField.textProperty().removeListener(it)
        }
    }

    fun enableTransactionValueListener() {
        transactionValueListener?.let {
            transactionValueField.textProperty().addListener(it)
        }
    }

    fun prefillDescription(text: String) {
        suggestionsHandler.disable()
        descriptionField.text = text
        suggestionsHandler.enable()
    }

    fun prefillTransactionValue(value: BigDecimal) {
        transactionValueField.text = value.toString()
        walletAfterBalance()
    }

    fun prefillCategory(category: Category) {
        if (categories.any { it.id == category.id }) {
            categoryComboBox.value = category
        }
    }
}
