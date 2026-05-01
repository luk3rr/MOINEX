/*
 * Filename: EditTickerController.kt (original filename: EditTickerController.java)
 * Created on: January  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.update

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.stage.Stage
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isEqual
import org.moinex.common.util.WindowUtils
import org.moinex.model.investment.Ticker
import org.moinex.service.PreferencesService
import org.moinex.service.investment.TickerService
import org.moinex.ui.dialog.investment.base.BaseTickerManagement
import org.springframework.stereotype.Controller
import java.math.BigDecimal

@Controller
class EditTickerController(
    tickerService: TickerService,
    preferencesService: PreferencesService,
) : BaseTickerManagement(tickerService, preferencesService) {
    @FXML
    private lateinit var archivedCheckBox: CheckBox

    private lateinit var ticker: Ticker

    fun setTicker(tk: Ticker) {
        ticker = tk
        nameField.text = ticker.name
        symbolField.text = ticker.symbol
        currentPriceField.text = ticker.currentUnitValue.toString()
        quantityField.text = ticker.currentQuantity.toString()
        avgUnitPriceField.text = ticker.averageUnitValue.toString()
        typeComboBox.value = ticker.type

        archivedCheckBox.isSelected = ticker.isArchived
    }

    @FXML
    override fun handleSave() {
        val name = nameField.text.trim()
        val symbol = symbolField.text.trim()
        val currentPriceStr = currentPriceField.text
        val type = typeComboBox.value
        val quantityStr = quantityField.text
        val avgUnitPriceStr = avgUnitPriceField.text

        if (currentPriceStr.isNullOrBlank() || type == null || name.isBlank() || symbol.isBlank()) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        if (quantityStr.isNullOrBlank() xor avgUnitPriceStr.isNullOrBlank()) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            val currentPrice = currentPriceStr.toBigDecimal()

            val (quantity, avgUnitPrice) =
                if (quantityStr.isNullOrBlank() && avgUnitPriceStr.isNullOrBlank()) {
                    BigDecimal.ZERO to BigDecimal.ZERO
                } else {
                    quantityStr!!.toBigDecimal() to avgUnitPriceStr!!.toBigDecimal()
                }

            val archived = archivedCheckBox.isSelected

            if (ticker.name == name &&
                ticker.symbol == symbol &&
                ticker.currentUnitValue.isEqual(currentPrice) &&
                ticker.type == type &&
                ticker.currentQuantity.isEqual(quantity) &&
                ticker.averageUnitValue.isEqual(avgUnitPrice) &&
                ticker.isArchived == archived
            ) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_CHANGES_TITLE),
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_CHANGES_TICKER_MESSAGE),
                )
                return
            }

            ticker.name = name
            ticker.symbol = symbol
            ticker.currentUnitValue = currentPrice
            ticker.type = type
            ticker.currentQuantity = quantity
            ticker.averageUnitValue = avgUnitPrice
            ticker.isArchived = archived

            tickerService.updateTicker(ticker)

            (nameField.scene.window as Stage).close()
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
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_ERROR_UPDATING_TICKER_TITLE),
                        e.message ?: "",
                    )
                }
                else -> throw e
            }
        }
    }
}
