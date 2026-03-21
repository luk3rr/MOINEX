/*
 * Filename: BaseTransferManagement.kt (original filename: AddTransferController.java)
 * Created on: October  4, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.base

import javafx.beans.value.ChangeListener
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.helper.SuggestionsHandlerHelper
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.Category
import org.moinex.model.wallettransaction.Transfer
import org.moinex.model.wallettransaction.Wallet
import org.moinex.service.CalculatorService
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.common.CalculatorController
import org.springframework.context.ConfigurableApplicationContext
import java.math.BigDecimal

/**
 * Base class for transfer dialog controllers
 */
abstract class BaseTransferManagement(
    protected val walletService: WalletService,
    protected val calculatorService: CalculatorService,
    protected val categoryService: CategoryService,
    protected val preferencesService: PreferencesService,
    protected val springContext: ConfigurableApplicationContext,
) {
    @FXML
    protected lateinit var senderWalletAfterBalanceValueLabel: Label

    @FXML
    protected lateinit var receiverWalletAfterBalanceValueLabel: Label

    @FXML
    protected lateinit var senderWalletCurrentBalanceValueLabel: Label

    @FXML
    protected lateinit var receiverWalletCurrentBalanceValueLabel: Label

    @FXML
    protected lateinit var senderWalletComboBox: ComboBox<Wallet>

    @FXML
    protected lateinit var receiverWalletComboBox: ComboBox<Wallet>

    @FXML
    protected lateinit var transferValueField: TextField

    @FXML
    protected lateinit var descriptionField: TextField

    @FXML
    protected lateinit var transferDatePicker: DatePicker

    @FXML
    protected lateinit var categoryComboBox: ComboBox<Category?>

    protected lateinit var suggestionsHandler: SuggestionsHandlerHelper<Transfer>

    protected var wallets: List<Wallet> = emptyList()
    protected var categories: List<Category> = emptyList()

    private var transferValueListener: ChangeListener<String>? = null

    @FXML
    protected open fun initialize() {
        configureSuggestions()
        configureListeners()
        configureComboBoxes()

        loadWalletsFromDatabase()
        loadSuggestionsFromDatabase()
        loadCategoriesFromDatabase()

        populateComboBoxes()

        UIUtils.setDatePickerFormat(transferDatePicker)

        UIUtils.resetLabel(senderWalletAfterBalanceValueLabel)
        UIUtils.resetLabel(receiverWalletAfterBalanceValueLabel)
        UIUtils.resetLabel(senderWalletCurrentBalanceValueLabel)
        UIUtils.resetLabel(receiverWalletCurrentBalanceValueLabel)

        senderWalletComboBox.setOnAction {
            updateSenderWalletBalance()
            updateSenderWalletAfterBalance()
        }

        receiverWalletComboBox.setOnAction {
            updateReceiverWalletBalance()
            updateReceiverWalletAfterBalance()
        }
    }

    @FXML
    protected fun handleCancel() {
        val stage = descriptionField.scene.window as Stage
        stage.close()
    }

    @FXML
    protected abstract fun handleSave()

    protected abstract fun updateAfterBalance(
        mainWallet: Wallet?,
        otherWallet: Wallet?,
        afterBalanceLabel: Label,
        isSender: Boolean,
    )

    @FXML
    protected fun handleOpenCalculator() {
        WindowUtils.openPopupWindow(
            Constants.CALCULATOR_FXML,
            preferencesService.translate(TranslationKeys.WALLETTRANSACTION_LABEL_CALCULATOR),
            springContext,
            { _: CalculatorController -> },
            listOf(Runnable { calculatorService.updateComponentWithResult(transferValueField) }),
        )
    }

    protected fun updateSenderWalletBalance() {
        updateWalletBalance(
            senderWalletComboBox.value,
            senderWalletCurrentBalanceValueLabel,
        )
    }

    protected fun updateReceiverWalletBalance() {
        updateWalletBalance(
            receiverWalletComboBox.value,
            receiverWalletCurrentBalanceValueLabel,
        )
    }

    private fun updateWalletBalance(
        wallet: Wallet?,
        balanceLabel: Label,
    ) {
        wallet ?: return

        val style =
            if (wallet.balance < BigDecimal.ZERO) {
                Constants.NEGATIVE_BALANCE_STYLE
            } else {
                Constants.NEUTRAL_BALANCE_STYLE
            }

        UIUtils.setLabelStyle(balanceLabel, style)
        balanceLabel.text = UIUtils.formatCurrency(wallet.balance)
    }

    protected fun updateSenderWalletAfterBalance() {
        updateAfterBalance(
            senderWalletComboBox.value,
            receiverWalletComboBox.value,
            senderWalletAfterBalanceValueLabel,
            true,
        )
    }

    protected fun updateReceiverWalletAfterBalance() {
        updateAfterBalance(
            receiverWalletComboBox.value,
            senderWalletComboBox.value,
            receiverWalletAfterBalanceValueLabel,
            false,
        )
    }

    protected fun configureListeners() {
        transferValueListener =
            ChangeListener { _, oldValue, newValue ->
                if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                    transferValueField.text = oldValue
                } else {
                    updateSenderWalletAfterBalance()
                    updateReceiverWalletAfterBalance()
                }
            }

        transferValueField.textProperty().addListener(transferValueListener)
    }

    protected fun loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName()
    }

    protected fun loadCategoriesFromDatabase() {
        categories = categoryService.getNonArchivedCategoriesOrderedByName().toMutableList()
    }

    protected fun populateComboBoxes() {
        senderWalletComboBox.items.setAll(wallets)
        receiverWalletComboBox.items.setAll(wallets)

        val categoriesWithNull = mutableListOf<Category?>(null)
        categoriesWithNull.addAll(categories)

        categoryComboBox.items.setAll(categoriesWithNull)
    }

    protected fun configureComboBoxes() {
        UIUtils.configureComboBox(senderWalletComboBox) { it.name }
        UIUtils.configureComboBox(receiverWalletComboBox) { it.name }
        UIUtils.configureComboBox(categoryComboBox) { it?.name ?: "" }
    }

    private fun loadSuggestionsFromDatabase() {
        suggestionsHandler.suggestions = walletService.getTransferSuggestions()
    }

    private fun configureSuggestions() {
        val filterFunction: (Transfer) -> String = { it.description ?: "" }

        val displayFunction: (Transfer) -> String = { tf ->
            val categoryName =
                tf.category?.name
                    ?: preferencesService.translate(
                        TranslationKeys.WALLETTRANSACTION_SUGGESTION_NO_CATEGORY,
                    )

            """
            ${tf.description}
            ${UIUtils.formatCurrency(
                tf.amount,
            )} | ${preferencesService.translate(
                TranslationKeys.WALLETTRANSACTION_SUGGESTION_FROM,
            )} ${tf.senderWallet.name} | ${preferencesService.translate(
                TranslationKeys.WALLETTRANSACTION_SUGGESTION_TO,
            )} ${tf.receiverWallet.name} | $categoryName
            """.trimIndent()
        }

        val onSelectCallback: (Transfer) -> Unit = { fillFieldsWithTransaction(it) }

        suggestionsHandler =
            SuggestionsHandlerHelper(
                descriptionField,
                filterFunction,
                displayFunction,
                onSelectCallback,
            )

        suggestionsHandler.enable()
    }

    private fun fillFieldsWithTransaction(t: Transfer) {
        senderWalletComboBox.value = t.senderWallet
        receiverWalletComboBox.value = t.receiverWallet

        suggestionsHandler.disable()
        descriptionField.text = t.description
        suggestionsHandler.enable()

        transferValueField.text = t.amount.toString()
        categoryComboBox.value = t.category

        updateSenderWalletBalance()
        updateSenderWalletAfterBalance()

        updateReceiverWalletBalance()
        updateReceiverWalletAfterBalance()
    }

    fun disableTransferValueListener() {
        transferValueListener?.let {
            transferValueField.textProperty().removeListener(it)
        }
    }

    fun enableTransferValueListener() {
        transferValueListener?.let {
            transferValueField.textProperty().addListener(it)
        }
    }
}
