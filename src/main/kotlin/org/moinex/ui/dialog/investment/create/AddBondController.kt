/*
 * Filename: AddBondController.kt (original filename: AddBondController.java)
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.create

import com.jfoenix.controls.JFXButton
import jakarta.persistence.EntityExistsException
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.enums.BondType
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import org.moinex.model.investment.Bond
import org.moinex.service.PreferencesService
import org.moinex.service.investment.BondService
import org.springframework.stereotype.Controller

@Controller
open class AddBondController(
    private val bondService: BondService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var nameField: TextField

    @FXML
    private lateinit var symbolField: TextField

    @FXML
    private lateinit var bondTypeComboBox: ComboBox<BondType>

    @FXML
    private lateinit var issuerField: TextField

    @FXML
    private lateinit var maturityDatePicker: DatePicker

    @FXML
    private lateinit var interestTypeComboBox: ComboBox<InterestType>

    @FXML
    private lateinit var interestIndexComboBox: ComboBox<InterestIndex>

    @FXML
    private lateinit var interestRateField: TextField

    @FXML
    private lateinit var saveButton: JFXButton

    @FXML
    private lateinit var cancelButton: JFXButton

    @FXML
    fun initialize() {
        configureComboBoxes()
        populateComboBoxes()
        configureListeners()

        UIUtils.setDatePickerFormat(maturityDatePicker)
    }

    @FXML
    fun handleSave() {
        val name = nameField.text
        val symbol = symbolField.text
        val issuer = issuerField.text
        val bondType = bondTypeComboBox.value
        val maturityDate = maturityDatePicker.value
        val interestType = interestTypeComboBox.value
        val interestIndex = interestIndexComboBox.value
        val interestRateStr = interestRateField.text

        if (name.isNullOrBlank() || bondType == null || maturityDate == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.BOND_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.BOND_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        val interestRate =
            if (!interestRateStr.isNullOrBlank()) {
                runCatching {
                    interestRateStr.toBigDecimal()
                }.getOrElse {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.DIALOG_ERROR_TITLE),
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_MESSAGE),
                    )
                    return
                }
            } else {
                null
            }

        runCatching {
            bondService.createBond(
                Bond(
                    name = name,
                    symbol = symbol,
                    type = bondType,
                    issuer = issuer,
                    maturityDate = maturityDate,
                    interestType = interestType,
                    interestIndex = interestIndex,
                    interestRate = interestRate,
                ),
            )

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.BOND_DIALOG_ADDED_TITLE),
                preferencesService.translate(TranslationKeys.BOND_DIALOG_ADDED_MESSAGE),
            )

            (saveButton.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is EntityExistsException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.BOND_DIALOG_ALREADY_EXISTS_TITLE),
                        preferencesService.translate(TranslationKeys.BOND_DIALOG_ALREADY_EXISTS_MESSAGE),
                    )
                }
                else -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.DIALOG_ERROR_TITLE),
                        e.message ?: "",
                    )
                }
            }
        }
    }

    @FXML
    protected fun handleCancel() {
        (cancelButton.scene.window as Stage).close()
    }

    private fun configureComboBoxes() {
        UIUtils.configureComboBox(bondTypeComboBox, UIUtils::translateBondType)
        UIUtils.configureComboBox(interestTypeComboBox, UIUtils::translateInterestType)
        UIUtils.configureComboBox(interestIndexComboBox, InterestIndex::name)
    }

    private fun populateComboBoxes() {
        bondTypeComboBox.items.setAll(*BondType.entries.toTypedArray())
        interestTypeComboBox.items.setAll(*InterestType.entries.toTypedArray())
        interestIndexComboBox.items.setAll(*InterestIndex.entries.toTypedArray())
    }

    private fun configureListeners() {
        interestRateField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.INVESTMENT_VALUE_REGEX))) {
                interestRateField.text = oldValue
            }
        }
    }
}
