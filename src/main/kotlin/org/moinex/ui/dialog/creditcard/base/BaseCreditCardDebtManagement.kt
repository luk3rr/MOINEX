/*
 * Filename: BaseCreditCardDebtManagement.kt (original filename: BaseCreditCardDebtManagement.java)
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.creditcard.base

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Files
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.toRounded
import org.moinex.common.extension.toYearMonth
import org.moinex.common.helper.SuggestionsHandlerHelper
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.Category
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.model.dto.form.CreditCardDebtFormDTO
import org.moinex.service.CalculatorService
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.ui.common.CalculatorController
import org.springframework.context.ConfigurableApplicationContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Month
import java.time.YearMonth

abstract class BaseCreditCardDebtManagement(
    protected val categoryService: CategoryService,
    protected val creditCardService: CreditCardService,
    protected val calculatorService: CalculatorService,
    protected val springContext: ConfigurableApplicationContext,
    protected val preferencesService: PreferencesService,
) {
    @FXML
    protected lateinit var crcComboBox: ComboBox<CreditCard>

    @FXML
    protected lateinit var categoryComboBox: ComboBox<Category>

    @FXML
    protected lateinit var invoiceMonthComboBox: ComboBox<Int>

    @FXML
    protected lateinit var invoiceYearComboBox: ComboBox<Int>

    @FXML
    protected lateinit var crcLimitLabel: Label

    @FXML
    protected lateinit var crcAvailableLimitLabel: Label

    @FXML
    protected lateinit var crcLimitAvailableAfterDebtLabel: Label

    @FXML
    protected lateinit var msgLabel: Label

    @FXML
    protected lateinit var descriptionField: TextField

    @FXML
    protected lateinit var valueField: TextField

    @FXML
    protected lateinit var installmentsField: TextField

    protected lateinit var suggestionsHandler: SuggestionsHandlerHelper<CreditCardDebt>
    protected var categories: List<Category> = emptyList()
    protected var creditCards: List<CreditCard> = emptyList()

    companion object {
        private const val INVOICE_YEAR_RANGE = 2
        private const val MONTHS_IN_YEAR = 12
    }

    fun setCreditCard(creditCard: CreditCard) {
        crcComboBox.value = creditCard
    }

    @FXML
    protected open fun initialize() {
        configureListeners()
        configureSuggestions()
        configureComboBoxes()

        loadCategoriesFromDatabase()
        loadCreditCardsFromDatabase()
        loadSuggestionsFromDatabase()

        populateComboBoxes()

        UIUtils.resetLabel(crcLimitLabel)
        UIUtils.resetLabel(crcAvailableLimitLabel)
        UIUtils.resetLabel(crcLimitAvailableAfterDebtLabel)
    }

    @FXML
    protected abstract fun handleSave()

    @FXML
    protected open fun handleCancel() {
        (crcComboBox.scene.window as Stage).close()
    }

    @FXML
    protected open fun handleOpenCalculator() {
        WindowUtils.openPopupWindow(
            Files.CALCULATOR_FXML,
            preferencesService.translate(TranslationKeys.MAIN_CALCULATOR),
            springContext,
            { _: CalculatorController -> },
            listOf(Runnable { calculatorService.updateComponentWithResult(valueField) }),
        )
    }

    protected fun loadCategoriesFromDatabase() {
        categories = categoryService.getNonArchivedCategoriesOrderedByName()
    }

    protected fun loadCreditCardsFromDatabase() {
        creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByName()
    }

    protected fun loadSuggestionsFromDatabase() {
        suggestionsHandler.suggestions = creditCardService.getCreditCardDebtSuggestions()
    }

    protected fun updateCreditCardLimitLabels() {
        val crc = creditCards.find { it.id!! == crcComboBox.value?.id!! } ?: return

        crcLimitLabel.text = UIUtils.formatCurrency(crc.maxDebt)

        val availableLimit = creditCardService.getAvailableCredit(crc.id!!)
        crcAvailableLimitLabel.text = UIUtils.formatCurrency(availableLimit)
    }

    protected fun isUpdateAvailableLimitAfterDebtLabelValid(): Boolean {
        if (crcComboBox.value == null) {
            return false
        }

        val value = valueField.text

        if (value.isEmpty()) {
            UIUtils.resetLabel(crcLimitAvailableAfterDebtLabel)
            return false
        }

        return true
    }

    protected open fun updateAvailableLimitAfterDebtLabel() {
        if (!isUpdateAvailableLimitAfterDebtLabelValid()) return

        val crc = creditCards.find { it.id!! == crcComboBox.value?.id!! } ?: return

        runCatching {
            val debtValue = BigDecimal(valueField.text)

            if (debtValue <= BigDecimal.ZERO) {
                UIUtils.resetLabel(msgLabel)
                return
            }

            val availableLimitAfterDebt =
                creditCardService.getAvailableCredit(crc.id!!).subtract(debtValue)

            if (availableLimitAfterDebt < BigDecimal.ZERO) {
                UIUtils.setLabelStyle(crcLimitAvailableAfterDebtLabel, Styles.NEGATIVE_BALANCE_STYLE)
            } else {
                UIUtils.setLabelStyle(crcLimitAvailableAfterDebtLabel, Styles.NEUTRAL_BALANCE_STYLE)
            }

            crcLimitAvailableAfterDebtLabel.text = UIUtils.formatCurrency(availableLimitAfterDebt)
        }.onFailure {
            UIUtils.resetLabel(crcLimitAvailableAfterDebtLabel)
        }
    }

    protected fun updateMsgLabel() {
        val installments = installmentsField.text.ifEmpty { "1" }.toIntOrNull() ?: 1

        if (installments < 1) {
            msgLabel.text = preferencesService.translate(TranslationKeys.CREDITCARD_DEBT_INVALID_INSTALLMENTS)
            return
        }

        val valueStr = valueField.text

        if (valueStr.isEmpty()) {
            UIUtils.resetLabel(msgLabel)
            return
        }

        runCatching {
            val debtValue = BigDecimal(valueField.text)

            if (debtValue <= BigDecimal.ZERO) {
                UIUtils.resetLabel(msgLabel)
                return
            }

            val exactInstallmentValue =
                debtValue.divide(BigDecimal(installments), 2, RoundingMode.FLOOR)

            val remainder =
                debtValue.subtract(exactInstallmentValue.multiply(BigDecimal(installments)))

            val exactDivision = remainder.compareTo(BigDecimal.ZERO) == 0

            msgLabel.text =
                if (exactDivision) {
                    preferencesService
                        .translate(TranslationKeys.CREDITCARD_DEBT_REPEAT_MONTHS)
                        .replace("{0}", installments.toString())
                        .replace("{1}", UIUtils.formatCurrency(exactInstallmentValue))
                } else {
                    val adjustedRemainder = remainder.toRounded(2)
                    preferencesService
                        .translate(TranslationKeys.CREDITCARD_DEBT_REPEAT_MONTHS_UNEVEN)
                        .replace("{0}", installments.toString())
                        .replace("{1}", UIUtils.formatCurrency(exactInstallmentValue.add(adjustedRemainder)))
                        .replace("{2}", (installments - 1).toString())
                        .replace("{3}", UIUtils.formatCurrency(exactInstallmentValue))
                }
        }.onFailure {
            msgLabel.text = preferencesService.translate(TranslationKeys.CREDITCARD_DEBT_INVALID_VALUE)
        }
    }

    protected fun populateComboBoxes() {
        (1..MONTHS_IN_YEAR).forEach { month ->
            invoiceMonthComboBox.items.add(month)
        }

        val currentYear = YearMonth.now().year
        (currentYear - INVOICE_YEAR_RANGE..currentYear + INVOICE_YEAR_RANGE).forEach { year ->
            invoiceYearComboBox.items.add(year)
        }

        categoryComboBox.items.addAll(categories)
        crcComboBox.items.addAll(creditCards)
    }

    protected fun setDefaultInvoiceMonthAndYear() {
        val invoiceDate =
            crcComboBox.value?.let { creditCardService.getNextInvoiceDate(it) }?.toYearMonth() ?: YearMonth.now()
        invoiceMonthComboBox.value = invoiceDate.monthValue
        invoiceYearComboBox.value = invoiceDate.year
    }

    protected fun configureComboBoxes() {
        UIUtils.configureComboBox(categoryComboBox, Category::name)
        UIUtils.configureComboBox(crcComboBox, CreditCard::name)
        UIUtils.configureComboBox(invoiceMonthComboBox) { month ->
            UIUtils.getMonthDisplayName(Month.of(month))
        }
        UIUtils.configureComboBox(invoiceYearComboBox, Any::toString)
    }

    protected fun configureSuggestions() {
        val filterFunction: (CreditCardDebt) -> String = { it.description ?: "" }

        val displayFunction: (CreditCardDebt) -> String = { ccd ->
            "${ccd.description}\n${UIUtils.formatCurrency(
                ccd.amount,
            )} | ${ccd.creditCard.name} | ${ccd.category.name} | ${ccd.installments}x"
        }

        val onSelectCallback: (CreditCardDebt) -> Unit = ::fillFieldsWithTransaction

        suggestionsHandler =
            SuggestionsHandlerHelper(
                descriptionField,
                filterFunction,
                displayFunction,
                onSelectCallback,
            )

        suggestionsHandler.enable()
    }

    protected fun configureListeners() {
        crcComboBox.valueProperty().addListener { _, _, _ ->
            updateCreditCardLimitLabels()
            updateAvailableLimitAfterDebtLabel()
            setDefaultInvoiceMonthAndYear()
        }

        valueField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                valueField.text = oldValue
            } else {
                updateAvailableLimitAfterDebtLabel()
                updateMsgLabel()
            }
        }

        installmentsField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.getDigitsRegexUpTo(Constants.INSTALLMENTS_FIELD_MAX_DIGITS)))) {
                installmentsField.text = oldValue
            } else {
                updateMsgLabel()
            }
        }
    }

    protected fun fillFieldsWithTransaction(ccd: CreditCardDebt) {
        crcComboBox.value = ccd.creditCard

        suggestionsHandler.disable()
        descriptionField.text = ccd.description
        suggestionsHandler.enable()

        valueField.text = ccd.amount.toString()
        installmentsField.text = ccd.installments.toString()
        categoryComboBox.value = ccd.category

        updateAvailableLimitAfterDebtLabel()
        updateMsgLabel()
    }

    protected fun getFieldsFromInterface(): CreditCardDebtFormDTO =
        CreditCardDebtFormDTO(
            creditCard = crcComboBox.value,
            category = categoryComboBox.value,
            invoiceMonth = invoiceMonthComboBox.value,
            invoiceYear = invoiceYearComboBox.value,
            description = descriptionField.text.trim(),
            valueStr = valueField.text,
            installmentsStr = installmentsField.text,
        )
}
