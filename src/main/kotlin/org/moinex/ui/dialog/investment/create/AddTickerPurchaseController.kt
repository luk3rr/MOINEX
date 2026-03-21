/*
 * Filename: AddTickerPurchaseController.kt (original filename: AddTickerPurchaseController.java)
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
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.investment.TickerPurchase
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.investment.base.BaseTickerTransactionManagement
import org.springframework.stereotype.Controller
import java.time.LocalTime

@Controller
class AddTickerPurchaseController(
    walletService: WalletService,
    categoryService: CategoryService,
    tickerService: TickerService,
    preferencesService: PreferencesService,
) : BaseTickerTransactionManagement(
        walletService,
        categoryService,
        tickerService,
        preferencesService,
    ) {
    init {
        walletTransactionType = WalletTransactionType.EXPENSE
    }

    @FXML
    override fun handleSave() {
        val wallet = walletComboBox.value
        val description = descriptionField.text
        val status = statusComboBox.value
        val category = categoryComboBox.value
        val unitPriceStr = unitPriceField.text
        val quantityStr = quantityField.text
        val buyDate = transactionDatePicker.value

        if (wallet == null ||
            description.isNullOrBlank() ||
            status == null ||
            category == null ||
            unitPriceStr.isNullOrBlank() ||
            quantityStr.isNullOrBlank() ||
            buyDate == null
        ) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            val unitPrice = unitPriceStr.toBigDecimal()
            val quantity = quantityStr.toBigDecimal()

            val currentTime = LocalTime.now()
            val dateTimeWithCurrentHour = buyDate.atTime(currentTime)

            tickerService.createTickerPurchase(
                TickerPurchase(
                    ticker = ticker,
                    quantity = quantity,
                    unitPrice = unitPrice,
                ),
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
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_PURCHASE_ADDED_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_PURCHASE_ADDED_MESSAGE),
            )

            (tickerNameLabel.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_TITLE),
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_MESSAGE),
                    )
                }
                is EntityNotFoundException, is IllegalArgumentException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_ERROR_BUYING_TICKER_TITLE),
                        e.message ?: "",
                    )
                }
                else -> throw e
            }
        }
    }
}
