/*
 * Filename: ChangeWalletTypeController.kt (original filename: ChangeWalletTypeController.java)
 * Created on: October  6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.update

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.exception.MoinexException
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletType
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Controller

@Controller
class ChangeWalletTypeController(
    private val walletService: WalletService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var walletComboBox: ComboBox<Wallet>

    @FXML
    private lateinit var newTypeComboBox: ComboBox<WalletType>

    @FXML
    private lateinit var currentTypeLabel: Label

    private var wallets: List<Wallet> = emptyList()
    private var walletTypes: List<WalletType> = emptyList()

    companion object {
        private const val OTHER_WALLET_TYPE_NAME = "Others"
    }

    fun setWalletComboBox(wt: Wallet) {
        if (wallets.none { it.id == wt.id }) {
            return
        }
        walletComboBox.value = wt
        updateCurrentTypeLabel(wt)
    }

    @FXML
    private fun initialize() {
        configureComboBoxes()
        loadWalletsFromDatabase()
        loadWalletTypesFromDatabase()
        populateComboBoxes()

        walletComboBox.setOnAction {
            val wt = walletComboBox.value
            updateCurrentTypeLabel(wt)
        }
    }

    @FXML
    private fun handleSave() {
        val wt = walletComboBox.value
        val walletNewType = newTypeComboBox.value

        if (wt == null || walletNewType == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            wt.type = walletNewType
            walletService.changeWalletType(wt)

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_TYPE_CHANGED_TITLE),
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_TYPE_CHANGED_MESSAGE),
            )
        }.onFailure { e ->
            when (e) {
                is EntityNotFoundException, is MoinexException.AttributeAlreadySetException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_INPUT_TITLE,
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

    private fun loadWalletTypesFromDatabase() {
        walletTypes = walletService.getAllWalletTypes()
    }

    private fun populateComboBoxes() {
        walletComboBox.items.addAll(wallets)

        val mutableTypes = walletTypes.toMutableList()

        mutableTypes.find { it.name == OTHER_WALLET_TYPE_NAME }?.let { walletType ->
            mutableTypes.remove(walletType)
            mutableTypes.add(walletType)
        }

        newTypeComboBox.items.addAll(
            mutableTypes.filter { it.name != Constants.GOAL_DEFAULT_WALLET_TYPE_NAME },
        )
    }

    private fun configureComboBoxes() {
        UIUtils.configureComboBox(walletComboBox) { it.name }
        UIUtils.configureComboBox(newTypeComboBox) { UIUtils.translateWalletType(it) }
    }

    private fun updateCurrentTypeLabel(wt: Wallet?) {
        if (wt == null) {
            currentTypeLabel.text = "-"
            return
        }
        currentTypeLabel.text = UIUtils.translateWalletType(wt.type)
    }
}
