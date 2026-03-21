/*
 * Filename: RenameWalletController.kt (original filename: RenameWalletController.java)
 * Created on: October  6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.update

import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.wallettransaction.Wallet
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Controller

@Controller
class RenameWalletController(
    private val walletService: WalletService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var walletComboBox: ComboBox<Wallet>

    @FXML
    private lateinit var walletNewNameField: TextField

    private var wallets: List<Wallet> = emptyList()

    fun setWalletComboBox(wt: Wallet) {
        if (wallets.none { it.id == wt.id }) {
            return
        }
        walletComboBox.value = wt
    }

    @FXML
    private fun initialize() {
        configureComboBoxes()
        loadWalletsFromDatabase()
        populateWalletComboBox()
    }

    @FXML
    private fun handleSave() {
        val wt = walletComboBox.value
        val walletNewName = walletNewNameField.text

        if (wt == null || walletNewName.isBlank()) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            wt.name = walletNewName
            walletService.renameWallet(wt)
        }.onFailure { e ->
            when (e) {
                is IllegalArgumentException, is EntityNotFoundException, is EntityExistsException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_ERROR_RENAMING_WALLET_TITLE,
                        ),
                        e.message ?: "Unknown error",
                    )
                    return
                }
                else -> throw e
            }
        }

        val stage = walletComboBox.scene.window as Stage
        stage.close()
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
