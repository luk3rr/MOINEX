/*
 * Filename: AddCryptoExchangeController.kt (original filename: AddCryptoExchangeController.java)
 * Created on: January 28, 2025
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
import org.moinex.exception.MoinexException
import org.moinex.model.investment.CryptoExchange
import org.moinex.service.CalculatorService
import org.moinex.service.PreferencesService
import org.moinex.service.investment.TickerService
import org.moinex.ui.dialog.investment.base.BaseCryptoExchangeManagement
import org.springframework.stereotype.Controller
import java.time.LocalTime

@Controller
class AddCryptoExchangeController(
    tickerService: TickerService,
    calculatorService: CalculatorService,
    preferencesService: PreferencesService,
) : BaseCryptoExchangeManagement(tickerService, calculatorService, preferencesService) {
    @FXML
    override fun handleSave() {
        val cryptoSold = cryptoSoldComboBox.value
        val cryptoReceived = cryptoReceivedComboBox.value
        val cryptoSoldQuantityStr = cryptoSoldQuantityField.text
        val cryptoReceivedQuantityStr = cryptoReceivedQuantityField.text
        val description = descriptionField.text
        val exchangeDate = exchangeDatePicker.value

        if (cryptoSold == null ||
            cryptoReceived == null ||
            cryptoSoldQuantityStr.isNullOrBlank() ||
            cryptoReceivedQuantityStr.isNullOrBlank() ||
            description.isNullOrBlank() ||
            exchangeDate == null
        ) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            val cryptoSoldQuantity = cryptoSoldQuantityStr.toBigDecimal()
            val cryptoReceivedQuantity = cryptoReceivedQuantityStr.toBigDecimal()

            val currentTime = LocalTime.now()
            val dateTimeWithCurrentHour = exchangeDate.atTime(currentTime)

            tickerService.createCryptoExchange(
                CryptoExchange(
                    soldCrypto = cryptoSold,
                    receivedCrypto = cryptoReceived,
                    soldQuantity = cryptoSoldQuantity,
                    receivedQuantity = cryptoReceivedQuantity,
                    date = dateTimeWithCurrentHour,
                    description = description,
                ),
            )

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EXCHANGE_CREATED_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EXCHANGE_CREATED_MESSAGE),
            )

            (cryptoReceivedQuantityField.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_EXCHANGE_QUANTITY_TITLE),
                        preferencesService.translate(
                            TranslationKeys.INVESTMENT_DIALOG_INVALID_EXCHANGE_QUANTITY_MESSAGE,
                        ),
                    )
                }
                is MoinexException.SameSourceDestinationException,
                is EntityNotFoundException,
                is MoinexException.InvalidTickerTypeException,
                is IllegalArgumentException,
                is MoinexException.InsufficientResourcesException,
                -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_ERROR_CREATING_EXCHANGE_TITLE),
                        e.message ?: "",
                    )
                }
                else -> throw e
            }
        }
    }
}
