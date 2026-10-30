/*
 * Filename: AddTickerSaleController.kt (original filename: AddTickerSaleController.java)
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
import org.moinex.common.ui.FieldValidation
import org.moinex.common.util.WindowUtils
import org.moinex.exception.MoinexException
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.investment.TickerSale
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.investment.base.BaseTickerTransactionManagement
import org.springframework.stereotype.Controller
import java.time.LocalTime

@Controller
class AddTickerSaleController(
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
    private val validation = FieldValidation(preferencesService::translate)

    init {
        walletTransactionType = WalletTransactionType.INCOME
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

        if (!validation.check {
                required(walletComboBox)
                required(descriptionField)
                required(statusComboBox)
                required(categoryComboBox)
                required(unitPriceField)
                required(quantityField)
                required(transactionDatePicker)
                number(unitPriceField)
            }
        ) {
            return
        }

        runCatching {
            val unitPrice = unitPriceStr.toBigDecimal()
            val quantity = quantityStr.toBigDecimal()

            val currentTime = LocalTime.now()
            val dateTimeWithCurrentHour = buyDate.atTime(currentTime)

            tickerService.createTickerSale(
                TickerSale(
                    ticker = ticker,
                    averageCost = ticker.averageUnitValue,
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

            (tickerNameLabel.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_TITLE),
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_MESSAGE),
                    )
                }
                is EntityNotFoundException,
                is IllegalArgumentException,
                is MoinexException.InsufficientResourcesException,
                -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_ERROR_SELLING_TICKER_TITLE),
                        e.message ?: "",
                    )
                }
                else -> throw e
            }
        }
    }
}
