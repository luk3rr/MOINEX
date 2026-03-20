/*
 * Filename: AddCreditCardCreditController.kt (original filename: AddCreditCardCreditController.java)
 * Created on: October 25, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.creditcard

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.helper.SuggestionsHandlerHelper
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.CreditCardCredit
import org.moinex.model.enums.CreditCardCreditType
import org.moinex.service.CalculatorService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.ui.common.CalculatorController
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.time.LocalTime

@Controller
class AddCreditCardCreditController(
    private val creditCardService: CreditCardService,
    private val calculatorService: CalculatorService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var crcComboBox: ComboBox<CreditCard>

    @FXML
    private lateinit var descriptionField: TextField

    @FXML
    private lateinit var valueField: TextField

    @FXML
    private lateinit var datePicker: DatePicker

    @FXML
    private lateinit var creditTypeComboBox: ComboBox<CreditCardCreditType>

    private lateinit var suggestionsHandler: SuggestionsHandlerHelper<CreditCardCredit>
    private var creditCards: List<CreditCard> = emptyList()

    fun setCreditCard(crc: CreditCard) {
        if (creditCards.none { it.id!! == crc.id!! }) {
            return
        }

        crcComboBox.value = crc
    }

    @FXML
    private fun initialize() {
        configureSuggestions()
        configureListeners()
        configureComboBoxes()

        loadCreditCardsFromDatabase()
        loadSuggestionsFromDatabase()

        populateCreditCardCreditTypeComboBox()
        populateCreditCardCombobox()

        UIUtils.setDatePickerFormat(datePicker)
    }

    @FXML
    private fun handleSave() {
        val crc = crcComboBox.value
        val description = descriptionField.text.trim()
        val valueStr = valueField.text
        val creditType = creditTypeComboBox.value
        val date = datePicker.value

        if (crc == null ||
            creditType == null ||
            date == null ||
            description.isEmpty() ||
            valueStr.isEmpty()
        ) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            val creditValue = BigDecimal(valueStr)
            val currentTime = LocalTime.now()
            val dateTimeWithCurrentHour = date.atTime(currentTime)

            creditCardService.addRebate(
                CreditCardCredit(
                    type = creditType,
                    creditCard = crc,
                    date = dateTimeWithCurrentHour,
                    amount = creditValue,
                    description = description,
                ),
            )

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_CREDIT_CREATED_TITLE),
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_CREDIT_CREATED_MESSAGE),
            )

            (crcComboBox.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_INVALID_CREDIT_VALUE_TITLE),
                        preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_INVALID_CREDIT_VALUE_MESSAGE),
                    )
                }
                else -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_ERROR_CREATING_DEBT_TITLE),
                        e.message ?: "Unknown error",
                    )
                }
            }
        }
    }

    @FXML
    private fun handleCancel() {
        (crcComboBox.scene.window as Stage).close()
    }

    @FXML
    private fun handleOpenCalculator() {
        WindowUtils.openPopupWindow(
            Constants.CALCULATOR_FXML,
            "Calculator",
            springContext,
            { _: CalculatorController -> },
            listOf(Runnable { calculatorService.updateComponentWithResult(valueField) }),
        )
    }

    private fun loadCreditCardsFromDatabase() {
        creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByName()
    }

    private fun loadSuggestionsFromDatabase() {
        suggestionsHandler.suggestions = creditCardService.getCreditCardCreditSuggestions()
    }

    private fun populateCreditCardCreditTypeComboBox() {
        creditTypeComboBox.items.addAll(*CreditCardCreditType.entries.toTypedArray())
    }

    private fun populateCreditCardCombobox() {
        crcComboBox.items.addAll(creditCards)
    }

    private fun configureComboBoxes() {
        UIUtils.configureComboBox(crcComboBox, CreditCard::name)
        UIUtils.configureComboBox(creditTypeComboBox, UIUtils::translateCreditCardCreditType)
    }

    private fun configureSuggestions() {
        val filterFunction: (CreditCardCredit) -> String = { it.description ?: "" }

        val displayFunction: (CreditCardCredit) -> String = { ccc ->
            "${ccc.description}\n${UIUtils.formatCurrency(ccc.amount)} | ${ccc.creditCard.name} | ${ccc.type}"
        }

        val onSelectCallback: (CreditCardCredit) -> Unit = ::fillFieldsWithTransaction

        suggestionsHandler =
            SuggestionsHandlerHelper(
                descriptionField,
                filterFunction,
                displayFunction,
                onSelectCallback,
            )

        suggestionsHandler.enable()
    }

    private fun configureListeners() {
        valueField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                valueField.text = oldValue
            }
        }
    }

    private fun fillFieldsWithTransaction(ccc: CreditCardCredit) {
        crcComboBox.value = ccc.creditCard

        suggestionsHandler.disable()
        descriptionField.text = ccc.description
        suggestionsHandler.enable()

        valueField.text = ccc.amount.toString()
        creditTypeComboBox.value = ccc.type
    }
}
