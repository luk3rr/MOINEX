/*
 * Filename: BaseCreditCardManagement.kt (original filename: BaseCreditCardManagement.java)
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.creditcard.base

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constants.Constants
import org.moinex.common.util.UIUtils
import org.moinex.model.creditcard.CreditCardOperator
import org.moinex.model.dto.form.CreditCardFormDTO
import org.moinex.model.wallettransaction.Wallet
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService

abstract class BaseCreditCardManagement(
    protected val creditCardService: CreditCardService,
    protected val walletService: WalletService,
) {
    @FXML
    protected lateinit var nameField: TextField

    @FXML
    protected lateinit var limitField: TextField

    @FXML
    protected lateinit var lastFourDigitsField: TextField

    @FXML
    protected lateinit var closingDayComboBox: ComboBox<String>

    @FXML
    protected lateinit var dueDayComboBox: ComboBox<String>

    @FXML
    protected lateinit var operatorComboBox: ComboBox<CreditCardOperator>

    @FXML
    protected lateinit var defaultBillingWalletComboBox: ComboBox<Wallet>

    protected var operators: List<CreditCardOperator> = emptyList()
    protected var wallets: List<Wallet> = emptyList()

    @FXML
    protected open fun initialize() {
        configureComboBoxes()
        loadCreditCardOperatorsFromDatabase()
        loadWalletsFromDatabase()
        populateComboBoxes()

        lastFourDigitsField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.DIGITS_ONLY_REGEX)) || newValue.length > 4) {
                lastFourDigitsField.text = oldValue
            }
        }

        limitField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                limitField.text = oldValue
            }
        }
    }

    @FXML
    protected abstract fun handleSave()

    @FXML
    protected open fun handleCancel() {
        (nameField.scene.window as Stage).close()
    }

    protected fun loadCreditCardOperatorsFromDatabase() {
        operators = creditCardService.getAllCreditCardOperatorsOrderedByName()
    }

    protected fun loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName()
    }

    protected fun populateComboBoxes() {
        (1..Constants.MAX_BILLING_DUE_DAY).forEach { i ->
            closingDayComboBox.items.add(i.toString())
            dueDayComboBox.items.add(i.toString())
        }

        operatorComboBox.items.setAll(operators)
        defaultBillingWalletComboBox.items.setAll(wallets)
        defaultBillingWalletComboBox.items.addFirst(null)
    }

    protected fun configureComboBoxes() {
        UIUtils.configureComboBox(operatorComboBox, CreditCardOperator::name)
        UIUtils.configureComboBox(defaultBillingWalletComboBox, Wallet::name)
    }

    protected fun getFieldsFromInterface(): CreditCardFormDTO =
        CreditCardFormDTO(
            name = nameField.text.trim(),
            limitStr = limitField.text,
            lastFourDigits = lastFourDigitsField.text,
            closingDayStr = closingDayComboBox.value,
            dueDayStr = dueDayComboBox.value,
            operator = operatorComboBox.value,
            defaultBillingWallet = defaultBillingWalletComboBox.value,
        )
}
