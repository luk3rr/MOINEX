/*
 * Filename: AddDividendController.kt (original filename: AddDividendController.java)
 * Created on: January  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.create

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.stage.Stage
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.model.investment.Dividend
import org.moinex.service.CalculatorService
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.investment.base.BaseDividendManagement
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.time.LocalTime

@Controller
class AddDividendController(
    walletService: WalletService,
    categoryService: CategoryService,
    calculatorService: CalculatorService,
    tickerService: TickerService,
    preferencesService: PreferencesService,
    springContext: ConfigurableApplicationContext,
) : BaseDividendManagement(
        walletService,
        categoryService,
        calculatorService,
        tickerService,
        preferencesService,
        springContext,
    ) {
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

            val currentTime = LocalTime.now()
            val dateTimeWithCurrentHour = dividendDate.atTime(currentTime)

            tickerService.createDividend(
                Dividend(ticker = ticker),
                dividendValue,
                WalletTransactionContextDTO(
                    wallet,
                    dateTimeWithCurrentHour,
                    category,
                    description,
                    status,
                    includeInAnalysisCheckBox.isSelected,
                ),
            )

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_DIVIDEND_CREATED_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_DIVIDEND_CREATED_MESSAGE),
            )

            (descriptionField.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_DIVIDEND_VALUE_TITLE),
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_DIVIDEND_VALUE_MESSAGE),
                    )
                }
                is EntityNotFoundException, is IllegalArgumentException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_ERROR_CREATING_DIVIDEND_TITLE),
                        e.message ?: "",
                    )
                }
                else -> throw e
            }
        }
    }
}
