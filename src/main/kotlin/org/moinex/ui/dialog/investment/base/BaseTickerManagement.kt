/*
 * Filename: BaseTickerManagement.kt (original filename: BaseTickerManagement.java)
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.base

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.util.UIUtils
import org.moinex.model.enums.AssetType
import org.moinex.service.PreferencesService
import org.moinex.service.investment.TickerService

abstract class BaseTickerManagement(
    protected val tickerService: TickerService,
    protected val preferencesService: PreferencesService,
) {
    @FXML
    protected lateinit var nameField: TextField

    @FXML
    protected lateinit var symbolField: TextField

    @FXML
    protected lateinit var currentPriceField: TextField

    @FXML
    protected lateinit var quantityField: TextField

    @FXML
    protected lateinit var avgUnitPriceField: TextField

    @FXML
    protected lateinit var typeComboBox: ComboBox<AssetType>

    @FXML
    protected open fun initialize() {
        configureComboBoxes()
        populateTypeComboBox()
        configureListeners()
    }

    @FXML
    protected open fun handleCancel() {
        (nameField.scene.window as Stage).close()
    }

    @FXML
    protected abstract fun handleSave()

    protected fun populateTypeComboBox() {
        typeComboBox.items.setAll(*AssetType.entries.toTypedArray())
    }

    protected fun configureComboBoxes() {
        UIUtils.configureComboBox(typeComboBox, UIUtils::translateAssetType)
    }

    protected fun configureListeners() {
        currentPriceField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.INVESTMENT_VALUE_REGEX))) {
                currentPriceField.text = oldValue
            }
        }

        quantityField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.INVESTMENT_VALUE_REGEX))) {
                quantityField.text = oldValue
            }
        }

        avgUnitPriceField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.INVESTMENT_VALUE_REGEX))) {
                avgUnitPriceField.text = oldValue
            }
        }
    }
}
