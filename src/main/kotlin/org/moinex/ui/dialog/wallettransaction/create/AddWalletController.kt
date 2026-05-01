/*
 * Filename: AddWalletController.kt (original filename: AddWalletController.java)
 * Created on: October  1, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.create

import jakarta.persistence.EntityExistsException
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletType
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Controller

@Controller
class AddWalletController(
    private val walletService: WalletService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var walletNameField: TextField

    @FXML
    private lateinit var walletBalanceField: TextField

    @FXML
    private lateinit var walletTypeComboBox: ComboBox<WalletType>

    private var walletTypes: List<WalletType> = emptyList()

    companion object {
        private const val DEFAULT_BALANCE = "0"
        private const val OTHER_WALLET_TYPE_NAME = "Others"
    }

    @FXML
    private fun initialize() {
        configureComboBoxes()
        loadWalletTypes()
        populateWalletTypeComboBox()

        walletBalanceField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                walletBalanceField.text = oldValue
            }
        }
    }

    @FXML
    private fun handleCancel() {
        val stage = walletNameField.scene.window as Stage
        stage.close()
    }

    @FXML
    private fun handleSave() {
        val walletName = walletNameField.text.trim()
        val walletBalanceStr = walletBalanceField.text
        val walletType = walletTypeComboBox.value

        if (walletName.isEmpty() || walletType == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            val walletBalance = walletBalanceStr.ifEmpty { DEFAULT_BALANCE }.toBigDecimal()

            walletService.createWallet(
                Wallet(
                    type = walletType,
                    name = walletName,
                    balance = walletBalance,
                ),
            )

            (walletNameField.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_BALANCE_TITLE,
                        ),
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_BALANCE_MESSAGE,
                        ),
                    )
                }
                is IllegalArgumentException, is EntityExistsException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_ERROR_CREATING_WALLET_TITLE,
                        ),
                        e.message ?: "Unknown error",
                    )
                }
                else -> throw e
            }
        }
    }

    private fun loadWalletTypes() {
        walletTypes = walletService.getAllWalletTypes()
    }

    private fun populateWalletTypeComboBox() {
        val mutableTypes = walletTypes.toMutableList()

        mutableTypes.find { it.name == OTHER_WALLET_TYPE_NAME }?.let { walletType ->
            mutableTypes.remove(walletType)
            mutableTypes.add(walletType)
        }

        mutableTypes.removeIf { it.name == Constants.GOAL_DEFAULT_WALLET_TYPE_NAME }

        walletTypeComboBox.items.setAll(mutableTypes)
    }

    private fun configureComboBoxes() {
        UIUtils.configureComboBox(walletTypeComboBox) { UIUtils.translateWalletType(it) }
    }
}
