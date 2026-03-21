/*
 * Filename: EditDividendController.kt (original filename: EditDividendController.java)
 * Created on: January 11, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.update

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.stage.Stage
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isEqual
import org.moinex.common.util.WindowUtils
import org.moinex.model.investment.Dividend
import org.moinex.service.CalculatorService
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.investment.base.BaseDividendManagement
import org.springframework.stereotype.Controller
import java.time.LocalTime

@Controller
class EditDividendController(
    walletService: WalletService,
    categoryService: CategoryService,
    calculatorService: CalculatorService,
    tickerService: TickerService,
    preferencesService: PreferencesService,
) : BaseDividendManagement(
        walletService,
        categoryService,
        calculatorService,
        tickerService,
        preferencesService,
    ) {
    private lateinit var dividend: Dividend

    fun setDividend(d: Dividend) {
        dividend = d
        tickerNameLabel.text = "${dividend.ticker.name} (${dividend.ticker.symbol})"

        setWalletComboBox(dividend.walletTransaction!!.wallet)

        descriptionField.text = dividend.walletTransaction!!.description
        dividendValueField.text = dividend.walletTransaction!!.amount.toString()
        statusComboBox.value = dividend.walletTransaction!!.status
        categoryComboBox.value = dividend.walletTransaction!!.category
        dividendDatePicker.value = dividend.walletTransaction!!.date.toLocalDate()
        includeInAnalysisCheckBox.isSelected = dividend.walletTransaction!!.includeInAnalysis
    }

    @FXML
    override fun handleSave() {
        val wallet = walletComboBox.value
        val description = descriptionField.text
        val dividendValueString = dividendValueField.text
        val status = statusComboBox.value
        val category = categoryComboBox.value
        val dividendDate = dividendDatePicker.value

        if (wallet == null ||
            description.isNullOrBlank() ||
            dividendValueString.isNullOrBlank() ||
            status == null ||
            category == null ||
            dividendDate == null
        ) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            val dividendValue = dividendValueString.toBigDecimal()

            if (dividend.walletTransaction!!.amount.isEqual(dividendValue) &&
                dividend.walletTransaction!!.category.id == category.id &&
                dividend.walletTransaction!!.status == status &&
                dividend.walletTransaction!!.date.toLocalDate() == dividendDate &&
                dividend.walletTransaction!!.description == description &&
                dividend.walletTransaction!!.wallet.id == wallet.id &&
                dividend.walletTransaction!!.includeInAnalysis == includeInAnalysisCheckBox.isSelected
            ) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_CHANGES_TITLE),
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_CHANGES_DIVIDEND_MESSAGE),
                )
            } else {
                val currentTime = LocalTime.now()
                val dateTimeWithCurrentHour = dividendDate.atTime(currentTime)

                dividend.walletTransaction!!.amount = dividendValue
                dividend.walletTransaction!!.category = category
                dividend.walletTransaction!!.status = status
                dividend.walletTransaction!!.date = dateTimeWithCurrentHour
                dividend.walletTransaction!!.description = description
                dividend.walletTransaction!!.wallet = wallet
                dividend.walletTransaction!!.includeInAnalysis = includeInAnalysisCheckBox.isSelected

                tickerService.updateDividend(dividend)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_DIVIDEND_UPDATED_TITLE),
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_DIVIDEND_UPDATED_MESSAGE),
                )
            }

            (descriptionField.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_DIVIDEND_VALUE_TITLE),
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_DIVIDEND_VALUE_MESSAGE),
                    )
                }
                is EntityNotFoundException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_ERROR_UPDATING_DIVIDEND_TITLE),
                        e.message ?: "",
                    )
                }
                else -> throw e
            }
        }
    }
}
