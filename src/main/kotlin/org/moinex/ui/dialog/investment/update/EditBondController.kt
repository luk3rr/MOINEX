/*
 * Filename: EditBondController.kt (original filename: EditBondController.java)
 * Created on: January  3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.update

import com.jfoenix.controls.JFXButton
import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
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
class EditBondController(
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
    private lateinit var archivedCheckBox: CheckBox

    @FXML
    private lateinit var saveButton: JFXButton

    @FXML
    private lateinit var cancelButton: JFXButton

    private lateinit var bond: Bond

    @FXML
    fun initialize() {
        configureComboBoxes()
        populateComboBoxes()
        configureListeners()

        UIUtils.setDatePickerFormat(maturityDatePicker)
    }

    fun setBond(bond: Bond) {
        this.bond = bond
        nameField.text = bond.name
        symbolField.text = bond.symbol
        bondTypeComboBox.value = bond.type
        issuerField.text = bond.issuer

        bond.maturityDate?.let {
            maturityDatePicker.value = it
        }

        interestTypeComboBox.value = bond.interestType
        interestIndexComboBox.value = bond.interestIndex

        bond.interestRate?.let {
            interestRateField.text = it.toString()
        }

        archivedCheckBox.isSelected = bond.archived
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
        val archived = archivedCheckBox.isSelected

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

        val hasChanges =
            bond.name != name ||
                bond.type != bondType ||
                bond.maturityDate != maturityDate ||
                bond.archived != archived ||
                (if (!symbol.isNullOrBlank()) bond.symbol != symbol else bond.symbol != null) ||
                (if (!issuer.isNullOrBlank()) bond.issuer != issuer else bond.issuer != null) ||
                (if (interestType != null) bond.interestType != interestType else bond.interestType != null) ||
                (if (interestIndex != null) bond.interestIndex != interestIndex else bond.interestIndex != null) ||
                (
                    if (interestRate != null) {
                        bond.interestRate == null || interestRate.compareTo(bond.interestRate) != 0
                    } else {
                        bond.interestRate != null
                    }
                )

        if (!hasChanges) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_CHANGES_TITLE),
                preferencesService.translate(TranslationKeys.BOND_DIALOG_NO_CHANGES_MESSAGE),
            )
            return
        }

        runCatching {
            bond.name = name
            bond.symbol = symbol
            bond.type = bondType
            bond.issuer = issuer
            bond.maturityDate = maturityDate
            bond.interestType = interestType
            bond.interestIndex = interestIndex
            bond.interestRate = interestRate
            bondService.updateBond(bond)

            if (archived != bond.archived) {
                if (archived) {
                    bondService.archiveBond(bond.id!!)
                } else {
                    bondService.unarchiveBond(bond.id!!)
                }
            }

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.BOND_DIALOG_UPDATED_TITLE),
                preferencesService.translate(TranslationKeys.BOND_DIALOG_UPDATED_MESSAGE),
            )

            (saveButton.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is EntityNotFoundException, is IllegalArgumentException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.BOND_DIALOG_ERROR_UPDATING_TITLE),
                        e.message ?: "",
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
    fun handleCancel() {
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
