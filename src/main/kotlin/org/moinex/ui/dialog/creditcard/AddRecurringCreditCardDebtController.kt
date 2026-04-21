/*
 * Filename: AddRecurringCreditCardDebtController.kt
 * Created on: April 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.creditcard

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.Category
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.RecurringCreditCardDebt
import org.moinex.model.enums.CreditCardRecurringFrequency
import org.moinex.service.CalculatorService
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.creditcard.RecurringCreditCardDebtService
import org.moinex.ui.common.CalculatorController
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Controller
class AddRecurringCreditCardDebtController(
    private val recurringCreditCardDebtService: RecurringCreditCardDebtService,
    private val creditCardService: CreditCardService,
    private val categoryService: CategoryService,
    private val calculatorService: CalculatorService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var crcComboBox: ComboBox<CreditCard>

    @FXML
    private lateinit var categoryComboBox: ComboBox<Category>

    @FXML
    private lateinit var frequencyComboBox: ComboBox<CreditCardRecurringFrequency>

    @FXML
    private lateinit var descriptionField: TextField

    @FXML
    private lateinit var valueField: TextField

    @FXML
    private lateinit var dayOfMonthField: TextField

    @FXML
    private lateinit var startDatePicker: DatePicker

    @FXML
    private lateinit var endDatePicker: DatePicker

    private var creditCards: List<CreditCard> = emptyList()
    private var categories: List<Category> = emptyList()

    @FXML
    fun initialize() {
        loadData()
        configureComboBoxes()
        configureListeners()

        startDatePicker.value = LocalDate.now().withDayOfMonth(1)
        dayOfMonthField.text = "1"
    }

    fun setCreditCard(creditCard: CreditCard) {
        crcComboBox.value = creditCard
    }

    @FXML
    private fun handleSave() {
        val creditCard = crcComboBox.value
        val category = categoryComboBox.value
        val frequency = frequencyComboBox.value
        val description = descriptionField.text.trim()
        val valueStr = valueField.text
        val dayStr = dayOfMonthField.text
        val startDate = startDatePicker.value

        if (creditCard == null ||
            category == null ||
            frequency == null ||
            valueStr.isEmpty() ||
            dayStr.isEmpty() ||
            startDate == null
        ) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_EMPTY_FIELDS_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_EMPTY_FIELDS_MESSAGE,
                ),
            )
            return
        }

        runCatching {
            val amount = BigDecimal(valueStr)
            val dayOfMonth = dayStr.toInt()
            val endDate = endDatePicker.value ?: Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE

            val recurring =
                RecurringCreditCardDebt(
                    creditCard = creditCard,
                    category = category,
                    amount = amount,
                    description = description.ifEmpty { null },
                    dayOfMonth = dayOfMonth,
                    frequency = frequency,
                    startDate = startDate,
                    endDate = endDate,
                    nextInvoiceMonth = YearMonth.from(startDate),
                )

            recurringCreditCardDebtService.createRecurring(recurring)

            WindowUtils.showSuccessDialog(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_CREATED_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_CREATED_MESSAGE,
                ),
            )

            (crcComboBox.scene.window as Stage).close()
        }.onFailure { e ->
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.CREDIT_CARD_RECURRING_DIALOG_ERROR_TITLE),
                e.message ?: "Unknown error",
            )
        }
    }

    @FXML
    private fun handleCancel() {
        (crcComboBox.scene.window as Stage).close()
    }

    @FXML
    private fun handleOpenCalculator() {
        WindowUtils.openPopupWindow(
            Files.CALCULATOR_FXML,
            preferencesService.translate(TranslationKeys.MAIN_CALCULATOR),
            springContext,
            { _: CalculatorController -> },
            listOf(Runnable { calculatorService.updateComponentWithResult(valueField) }),
        )
    }

    private fun loadData() {
        creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByName()
        categories = categoryService.getNonArchivedCategoriesOrderedByName()
    }

    private fun configureComboBoxes() {
        UIUtils.configureComboBox(crcComboBox, CreditCard::name)
        UIUtils.configureComboBox(categoryComboBox, Category::name)
        UIUtils.configureComboBox(frequencyComboBox) {
            UIUtils.translateCreditCardRecurringFrequency(it)
        }

        crcComboBox.items.addAll(creditCards)
        categoryComboBox.items.addAll(categories)
        frequencyComboBox.items.addAll(CreditCardRecurringFrequency.entries)
        frequencyComboBox.value = CreditCardRecurringFrequency.MONTHLY
    }

    private fun configureListeners() {
        valueField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                valueField.text = oldValue
            }
        }

        dayOfMonthField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.getDigitsRegexUpTo(2)))) {
                dayOfMonthField.text = oldValue
            }
        }
    }
}
