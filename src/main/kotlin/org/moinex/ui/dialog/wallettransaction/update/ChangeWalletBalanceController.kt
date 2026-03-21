/*
 * Filename: ChangeWalletBalanceController.kt (original filename: ChangeWalletBalanceController.java)
 * Created on: October 30, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.update

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.extension.isEqual
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.wallettransaction.Wallet
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Controller
import java.math.BigDecimal

@Controller
class ChangeWalletBalanceController(
    private val walletService: WalletService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var walletComboBox: ComboBox<Wallet>

    @FXML
    private lateinit var balanceField: TextField

    private var wallets: List<Wallet> = emptyList()

    fun setWalletComboBox(wt: Wallet) {
        if (wallets.none { it.id == wt.id }) {
            return
        }
        walletComboBox.value = wt
        balanceField.text = wt.balance.toString()
    }

    @FXML
    private fun initialize() {
        configureComboBoxes()
        loadWalletsFromDatabase()
        populateWalletComboBox()

        balanceField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                balanceField.text = oldValue
            }
        }
    }

    @FXML
    private fun handleSave() {
        val wt = walletComboBox.value
        val newBalanceStr = balanceField.text

        if (wt == null || newBalanceStr.isBlank()) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_INPUT_TITLE),
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            val newBalance = BigDecimal(newBalanceStr)

            if (wt.balance.isEqual(newBalance)) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(
                        TranslationKeys.WALLETTRANSACTION_DIALOG_NO_CHANGES_MADE_TITLE,
                    ),
                    preferencesService.translate(
                        TranslationKeys.WALLETTRANSACTION_DIALOG_NO_CHANGES_BALANCE_MESSAGE,
                    ),
                )
                return
            }

            wt.balance = newBalance
            walletService.updateWalletBalance(wt)

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_UPDATED_TITLE),
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_BALANCE_UPDATED_MESSAGE,
                ),
            )

            (balanceField.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_INPUT_TITLE,
                        ),
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_BALANCE_MESSAGE,
                        ),
                    )
                }
                is EntityNotFoundException, is IllegalStateException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_ERROR_UPDATING_BALANCE_TITLE,
                        ),
                        e.message ?: "Unknown error",
                    )
                }
                else -> throw e
            }
        }
    }

    @FXML
    private fun handleCancel() {
        val stage = walletComboBox.scene.window as Stage
        stage.close()
    }

    private fun loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName()
    }

    private fun populateWalletComboBox() {
        walletComboBox.items.addAll(wallets)
    }

    private fun configureComboBoxes() {
        UIUtils.configureComboBox(walletComboBox) { it.name }
    }
}
