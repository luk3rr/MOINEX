/*
 * Filename: EditCryptoExchangeController.kt (original filename: EditCryptoExchangeController.java)
 * Created on: January 28, 2025
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
import org.moinex.exception.MoinexException
import org.moinex.model.investment.CryptoExchange
import org.moinex.service.CalculatorService
import org.moinex.service.PreferencesService
import org.moinex.service.investment.TickerService
import org.moinex.ui.dialog.investment.base.BaseCryptoExchangeManagement
import org.springframework.stereotype.Controller
import java.time.LocalTime

@Controller
class EditCryptoExchangeController(
    tickerService: TickerService,
    calculatorService: CalculatorService,
    preferencesService: PreferencesService,
) : BaseCryptoExchangeManagement(tickerService, calculatorService, preferencesService) {
    private lateinit var cryptoExchange: CryptoExchange

    fun setCryptoExchange(cryptoExchange: CryptoExchange) {
        this.cryptoExchange = cryptoExchange

        cryptoSoldComboBox.value = cryptoExchange.soldCrypto
        cryptoReceivedComboBox.value = cryptoExchange.receivedCrypto

        cryptoSoldQuantityField.text = cryptoExchange.soldQuantity.toString()
        cryptoReceivedQuantityField.text = cryptoExchange.receivedQuantity.toString()
        descriptionField.text = cryptoExchange.description
        exchangeDatePicker.value = cryptoExchange.date.toLocalDate()

        updateFromCryptoCurrentQuantity()
        updateToCryptoCurrentQuantity()
        updateFromCryptoQuantityAfterExchange()
        updateToCryptoQuantityAfterExchange()
    }

    @FXML
    override fun handleSave() {
        val soldCrypto = cryptoSoldComboBox.value
        val receivedCrypto = cryptoReceivedComboBox.value
        val cryptoSoldQuantityStr = cryptoSoldQuantityField.text
        val cryptoReceivedQuantityStr = cryptoReceivedQuantityField.text
        val description = descriptionField.text.trim()
        val exchangeDate = exchangeDatePicker.value

        if (soldCrypto == null ||
            receivedCrypto == null ||
            cryptoSoldQuantityStr.isNullOrBlank() ||
            cryptoReceivedQuantityStr.isNullOrBlank() ||
            description.isBlank() ||
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

            if (cryptoExchange.soldCrypto.symbol == soldCrypto.symbol &&
                cryptoExchange.receivedCrypto.symbol == receivedCrypto.symbol &&
                cryptoExchange.soldQuantity.isEqual(cryptoSoldQuantity) &&
                cryptoExchange.receivedQuantity.isEqual(cryptoReceivedQuantity) &&
                cryptoExchange.description == description &&
                cryptoExchange.date.toLocalDate() == dateTimeWithCurrentHour.toLocalDate()
            ) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_CHANGES_TITLE),
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_CHANGES_EXCHANGE_MESSAGE),
                )
            } else {
                cryptoExchange.soldCrypto = soldCrypto
                cryptoExchange.receivedCrypto = receivedCrypto
                cryptoExchange.soldQuantity = cryptoSoldQuantity
                cryptoExchange.receivedQuantity = cryptoReceivedQuantity
                cryptoExchange.description = description
                cryptoExchange.date = dateTimeWithCurrentHour

                tickerService.updateCryptoExchange(cryptoExchange)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EXCHANGE_UPDATED_TITLE),
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_EXCHANGE_UPDATED_MESSAGE),
                )
            }

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
                is EntityNotFoundException,
                is MoinexException.SameSourceDestinationException,
                is IllegalArgumentException,
                -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_ERROR_UPDATING_EXCHANGE_TITLE),
                        e.message ?: "",
                    )
                }
                else -> throw e
            }
        }
    }
}
