/*
 * Filename: BaseRecurringTransactionManagement.kt (original filename: BaseRecurringTransactionManagement.java)
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.base

import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.model.Category
import org.moinex.model.enums.RecurringTransactionFrequency
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.Wallet
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Controller

/**
 * Base class to manage recurring transactions
 */
@Controller
abstract class BaseRecurringTransactionManagement(
    protected val walletService: WalletService,
    protected val recurringTransactionService: RecurringTransactionService,
    protected val categoryService: CategoryService,
    protected val preferencesService: PreferencesService,
) {
    @FXML
    protected lateinit var descriptionField: TextField

    @FXML
    protected lateinit var valueField: TextField

    @FXML
    protected lateinit var walletComboBox: ComboBox<Wallet>

    @FXML
    protected lateinit var typeComboBox: ComboBox<WalletTransactionType>

    @FXML
    protected lateinit var categoryComboBox: ComboBox<Category>

    @FXML
    protected lateinit var frequencyComboBox: ComboBox<RecurringTransactionFrequency>

    @FXML
    protected lateinit var startDatePicker: DatePicker

    @FXML
    protected lateinit var endDatePicker: DatePicker

    @FXML
    protected lateinit var infoLabel: Label

    @FXML
    protected lateinit var includeInAnalysisCheckBox: CheckBox

    @FXML
    protected lateinit var includeInNetWorthCheckBox: CheckBox

    protected var wallets: List<Wallet> = emptyList()
    protected var categories: List<Category> = emptyList()

    @FXML
    protected fun initialize() {
        configureComboBoxes()

        loadWalletsFromDatabase()
        loadCategoriesFromDatabase()

        populateComboBoxes()

        UIUtils.setDatePickerFormat(startDatePicker)
        UIUtils.setDatePickerFormat(endDatePicker)

        startDatePicker.setOnAction { updateInfoLabel() }
        endDatePicker.setOnAction { updateInfoLabel() }
        frequencyComboBox.setOnAction { updateInfoLabel() }

        valueField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                valueField.text = oldValue
            }
        }

        endDatePicker.editor.textProperty().addListener { _, _, newValue ->
            if (newValue.isNullOrBlank()) {
                endDatePicker.value = null
            }
        }
    }

    @FXML
    protected fun handleCancel() {
        val stage = descriptionField.scene.window as Stage
        stage.close()
    }

    @FXML
    protected abstract fun handleSave()

    protected fun updateInfoLabel() {
        val startDate = startDatePicker.value
        val endDate = endDatePicker.value
        val frequency = frequencyComboBox.value

        val msg =
            buildString {
                if (startDate != null && frequency != null) {
                    append(preferencesService.translate(TranslationKeys.WALLETTRANSACTION_INFO_STARTS_ON))
                    append(" ")
                    append(startDate)
                    append(", ")

                    if (endDate != null) {
                        append(preferencesService.translate(TranslationKeys.WALLETTRANSACTION_INFO_ENDS_ON))
                        append(" ")
                        append(endDate)
                        append(", ")
                    }

                    append(preferencesService.translate(TranslationKeys.WALLETTRANSACTION_INFO_FREQUENCY))
                    append(" ")
                    append(UIUtils.translateRecurringTransactionFrequency(frequency))

                    if (endDate != null) {
                        runCatching {
                            val lastDate =
                                recurringTransactionService.getLastTransactionDate(
                                    startDate,
                                    endDate,
                                    frequency,
                                )
                            append("\n")
                            append(
                                preferencesService.translate(
                                    TranslationKeys.WALLETTRANSACTION_INFO_LAST_TRANSACTION,
                                ),
                            )
                            append(" ")
                            append(lastDate)
                        }
                    }
                }
            }

        infoLabel.text = msg
    }

    protected fun loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName()
    }

    protected fun loadCategoriesFromDatabase() {
        categories = categoryService.getNonArchivedCategoriesOrderedByName()
    }

    protected fun populateComboBoxes() {
        walletComboBox.items.setAll(wallets)
        typeComboBox.items.setAll(WalletTransactionType.entries)
        frequencyComboBox.items.setAll(RecurringTransactionFrequency.entries)
        categoryComboBox.items.setAll(categories)

        if (categories.isEmpty()) {
            UIUtils.addTooltipToNode(
                categoryComboBox,
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_TOOLTIP_NEED_CATEGORY_RECURRING,
                ),
            )
        }
    }

    protected fun configureComboBoxes() {
        UIUtils.configureComboBox(walletComboBox) { it.name }
        UIUtils.configureComboBox(typeComboBox) { UIUtils.translateTransactionType(it) }
        UIUtils.configureComboBox(frequencyComboBox) {
            UIUtils.translateRecurringTransactionFrequency(it)
        }
        UIUtils.configureComboBox(categoryComboBox) { it.name }
    }
}
