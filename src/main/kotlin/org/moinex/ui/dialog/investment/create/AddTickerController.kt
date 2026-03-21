/*
 * Filename: AddTickerController.kt (original filename: AddTickerController.java)
 * Created on: January  8, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.create

import com.jfoenix.controls.JFXButton
import jakarta.persistence.EntityExistsException
import javafx.fxml.FXML
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.investment.Ticker
import org.moinex.service.PreferencesService
import org.moinex.service.investment.TickerService
import org.moinex.ui.dialog.investment.base.BaseTickerManagement
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.time.LocalDateTime

@Controller
class AddTickerController(
    tickerService: TickerService,
    preferencesService: PreferencesService,
) : BaseTickerManagement(tickerService, preferencesService) {
    @FXML
    private lateinit var yahooLookupButton: JFXButton

    @FXML
    override fun initialize() {
        super.initialize()

        UIUtils.addTooltipToNode(
            yahooLookupButton,
            preferencesService.translate(TranslationKeys.INVESTMENT_BUTTON_YAHOO_LOOKUP_TOOLTIP),
        )
    }

    @FXML
    override fun handleSave() {
        val name = nameField.text
        val symbol = symbolField.text
        val currentPriceStr = currentPriceField.text
        val type = typeComboBox.value
        val quantityStr = quantityField.text
        val avgUnitPriceStr = avgUnitPriceField.text

        if (name.isNullOrBlank() || symbol.isNullOrBlank() || currentPriceStr.isNullOrBlank() || type == null) {
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

            tickerService.createTicker(
                Ticker(
                    createdAt = LocalDateTime.now(),
                    lastUpdate = LocalDateTime.now(),
                    name = name,
                    symbol = symbol,
                    type = type,
                    currentQuantity = quantity,
                    currentUnitValue = currentPrice,
                    averageUnitValue = avgUnitPrice,
                ),
            )

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_TICKER_ADDED_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_TICKER_ADDED_MESSAGE),
            )

            (nameField.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_TITLE),
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_NUMBER_MESSAGE),
                    )
                }
                is IllegalArgumentException, is EntityExistsException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_ERROR_ADDING_TICKER_TITLE),
                        e.message ?: "",
                    )
                }
                else -> throw e
            }
        }
    }

    @FXML
    private fun goToYahooLookup() {
        WindowUtils.openUrl(Constants.YAHOO_LOOKUP_URL)
    }
}
