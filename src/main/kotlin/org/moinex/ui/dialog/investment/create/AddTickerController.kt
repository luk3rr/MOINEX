/*
 * Filename: AddTickerController.kt (original filename: AddTickerController.java)
 * Created on: January  8, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.create

import atlantafx.base.controls.Popover
import atlantafx.base.theme.Styles
import jakarta.persistence.EntityExistsException
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.FieldValidation
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.enums.AssetType
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
    private val validation = FieldValidation(preferencesService::translate)

    @FXML
    private lateinit var yahooLookupButton: Button

    @FXML
    private lateinit var symbolHintLabel: Label

    @FXML
    override fun initialize() {
        super.initialize()

        updateSymbolHint(typeComboBox.value)
        typeComboBox.valueProperty().addListener { _, _, type -> updateSymbolHint(type) }

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

        if (!validation.check {
                required(nameField)
                required(symbolField)
                required(currentPriceField)
                required(typeComboBox)
                number(currentPriceField)
            }
        ) {
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
    private fun showSymbolHelp() {
        Popover(buildSymbolHelpContent()).apply {
            title = preferencesService.translate(TranslationKeys.INVESTMENT_SYMBOL_HELP_TITLE)
            isDetachable = false
            arrowLocation = Popover.ArrowLocation.TOP_RIGHT
            show(yahooLookupButton)
        }
    }

    private fun updateSymbolHint(type: AssetType?) {
        val key =
            when (type) {
                AssetType.CRYPTOCURRENCY -> TranslationKeys.INVESTMENT_SYMBOL_HINT_CRYPTO
                null -> TranslationKeys.INVESTMENT_SYMBOL_HINT_DEFAULT
                else -> TranslationKeys.INVESTMENT_SYMBOL_HINT_EQUITY
            }
        symbolHintLabel.text = preferencesService.translate(key)
    }

    /**
     * Builds the reference table of Yahoo Finance symbol formats shown in the help popover.
     */
    private fun buildSymbolHelpContent(): VBox {
        val rows =
            listOf(
                Triple(
                    TranslationKeys.INVESTMENT_SYMBOL_HELP_MARKET_B3,
                    TranslationKeys.INVESTMENT_SYMBOL_HELP_FORMAT_SUFFIX_SA,
                    "PETR4.SA, HGLG11.SA, BOVA11.SA",
                ),
                Triple(
                    TranslationKeys.INVESTMENT_SYMBOL_HELP_MARKET_US,
                    TranslationKeys.INVESTMENT_SYMBOL_HELP_FORMAT_NO_SUFFIX,
                    "AAPL, VOO",
                ),
                Triple(
                    TranslationKeys.INVESTMENT_SYMBOL_HELP_MARKET_CRYPTO,
                    TranslationKeys.INVESTMENT_SYMBOL_HELP_FORMAT_PAIR,
                    "BTC-USD, ETH-BRL",
                ),
                Triple(
                    TranslationKeys.INVESTMENT_SYMBOL_HELP_MARKET_LONDON,
                    TranslationKeys.INVESTMENT_SYMBOL_HELP_FORMAT_SUFFIX_EXCHANGE,
                    "VOD.L, SAP.DE",
                ),
                Triple(
                    TranslationKeys.INVESTMENT_SYMBOL_HELP_MARKET_FX,
                    TranslationKeys.INVESTMENT_SYMBOL_HELP_FORMAT_FX,
                    "USDBRL=X",
                ),
            )

        val grid =
            GridPane().apply {
                hgap = 16.0
                vgap = 6.0
            }

        listOf(
            TranslationKeys.INVESTMENT_SYMBOL_HELP_HEADER_MARKET,
            TranslationKeys.INVESTMENT_SYMBOL_HELP_HEADER_FORMAT,
            TranslationKeys.INVESTMENT_SYMBOL_HELP_HEADER_EXAMPLE,
        ).forEachIndexed { column, key ->
            grid.add(
                Label(preferencesService.translate(key)).apply { styleClass.add(Styles.TEXT_BOLD) },
                column,
                0,
            )
        }

        rows.forEachIndexed { index, (marketKey, formatKey, example) ->
            grid.add(Label(preferencesService.translate(marketKey)), 0, index + 1)
            grid.add(Label(preferencesService.translate(formatKey)), 1, index + 1)
            grid.add(Label(example).apply { styleClass.add(Styles.TEXT_MUTED) }, 2, index + 1)
        }

        val intro =
            Label(preferencesService.translate(TranslationKeys.INVESTMENT_SYMBOL_HELP_INTRO)).apply {
                isWrapText = true
                maxWidth = 460.0
            }

        val link =
            Hyperlink(preferencesService.translate(TranslationKeys.INVESTMENT_SYMBOL_HELP_LINK)).apply {
                setOnAction { WindowUtils.openUrl(Constants.YAHOO_LOOKUP_URL) }
                WindowUtils.addCopyLinkMenu(
                    this,
                    Constants.YAHOO_LOOKUP_URL,
                    preferencesService.translate(TranslationKeys.WISHLIST_COPY_LINK),
                )
            }

        return VBox(10.0, intro, grid, link).apply { padding = Insets(4.0) }
    }
}
