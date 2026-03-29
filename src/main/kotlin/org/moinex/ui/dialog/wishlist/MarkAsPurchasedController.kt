/*
 * Filename: MarkAsPurchasedController.kt
 * Created on: March 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wishlist

import javafx.fxml.FXML
import javafx.scene.control.RadioButton
import javafx.scene.control.ToggleGroup
import javafx.stage.Stage
import org.moinex.common.constant.TranslationKeys
import org.moinex.model.wishlist.WishlistItem
import org.moinex.service.PreferencesService
import org.springframework.stereotype.Controller

@Controller
class MarkAsPurchasedController(
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var walletRadioButton: RadioButton

    @FXML
    private lateinit var creditCardRadioButton: RadioButton

    private val paymentMethodGroup = ToggleGroup()
    private var selectedItem: WishlistItem? = null
    private var onWalletSelectedCallback: (() -> Unit)? = null
    private var onCreditCardSelectedCallback: (() -> Unit)? = null

    @FXML
    fun initialize() {
        walletRadioButton.toggleGroup = paymentMethodGroup
        creditCardRadioButton.toggleGroup = paymentMethodGroup
        walletRadioButton.isSelected = true

        walletRadioButton.text = preferencesService.translate(TranslationKeys.WISHLIST_PAYMENT_METHOD_WALLET)
        creditCardRadioButton.text = preferencesService.translate(TranslationKeys.WISHLIST_PAYMENT_METHOD_CREDIT_CARD)
    }

    fun setItem(item: WishlistItem) {
        selectedItem = item
    }

    fun setOnWalletSelectedCallback(callback: () -> Unit) {
        onWalletSelectedCallback = callback
    }

    fun setOnCreditCardSelectedCallback(callback: () -> Unit) {
        onCreditCardSelectedCallback = callback
    }

    @FXML
    private fun handleConfirm() {
        when {
            walletRadioButton.isSelected -> onWalletSelectedCallback?.invoke()
            creditCardRadioButton.isSelected -> onCreditCardSelectedCallback?.invoke()
        }
        closeDialog()
    }

    @FXML
    private fun handleCancel() {
        closeDialog()
    }

    private fun closeDialog() {
        val stage = creditCardRadioButton.scene.window as Stage
        stage.close()
    }
}
