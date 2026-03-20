/*
 * Filename: BaseGoalManagement.kt (original filename: BaseGoalManagement.java)
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.goal.base

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constants.Constants
import org.moinex.common.util.UIUtils
import org.moinex.model.wallettransaction.Wallet
import org.moinex.service.PreferencesService
import org.moinex.service.goal.GoalService
import org.moinex.service.wallet.WalletService

abstract class BaseGoalManagement(
    protected val goalService: GoalService,
    protected val walletService: WalletService,
    protected val preferencesService: PreferencesService,
) {
    @FXML
    protected lateinit var masterWalletComboBox: ComboBox<Wallet>

    @FXML
    protected lateinit var nameField: TextField

    @FXML
    protected lateinit var balanceField: TextField

    @FXML
    protected lateinit var targetBalanceField: TextField

    @FXML
    protected lateinit var targetDatePicker: DatePicker

    @FXML
    protected lateinit var motivationTextArea: TextArea

    protected var masterWallets: List<Wallet> = emptyList()

    @FXML
    protected open fun initialize() {
        UIUtils.setDatePickerFormat(targetDatePicker)

        loadWalletsFromDatabase()
        configureComboBoxes()
        populateComboBoxes()

        balanceField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                balanceField.text = oldValue
            }
        }

        targetBalanceField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                targetBalanceField.text = oldValue
            }
        }
    }

    @FXML
    protected abstract fun handleSave()

    @FXML
    protected open fun handleCancel() {
        (nameField.scene.window as Stage).close()
    }

    private fun populateComboBoxes() {
        masterWalletComboBox.items.add(null)
        masterWalletComboBox.items.addAll(masterWallets)

        masterWalletComboBox.setCellFactory {
            UIUtils.createListCell { wallet -> wallet?.name ?: "" }
        }

        masterWalletComboBox.buttonCell = UIUtils.createListCell { wallet -> wallet?.name ?: "" }
    }

    private fun configureComboBoxes() {
        UIUtils.configureComboBox(masterWalletComboBox, Wallet::name)
    }

    private fun loadWalletsFromDatabase() {
        masterWallets =
            walletService
                .getAllNonArchivedWalletsOrderedByName()
                .filter { it.isMaster() }
    }
}
