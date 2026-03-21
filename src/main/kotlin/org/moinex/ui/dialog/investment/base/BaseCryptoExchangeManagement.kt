/*
 * Filename: BaseCryptoExchangeManagement.kt (original filename: BaseCryptoExchangeManagement.java)
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.base

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Files
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.enums.AssetType
import org.moinex.model.investment.Ticker
import org.moinex.service.CalculatorService
import org.moinex.service.PreferencesService
import org.moinex.service.investment.TickerService
import org.moinex.ui.common.CalculatorController
import org.springframework.context.ConfigurableApplicationContext
import java.math.BigDecimal

abstract class BaseCryptoExchangeManagement(
    protected val tickerService: TickerService,
    protected val calculatorService: CalculatorService,
    protected val preferencesService: PreferencesService,
) {
    @FXML
    protected lateinit var cryptoSoldAfterBalanceValueLabel: Label

    @FXML
    protected lateinit var cryptoReceivedAfterBalanceValueLabel: Label

    @FXML
    protected lateinit var cryptoSoldCurrentBalanceValueLabel: Label

    @FXML
    protected lateinit var cryptoReceivedCurrentBalanceValueLabel: Label

    @FXML
    protected lateinit var cryptoSoldComboBox: ComboBox<Ticker>

    @FXML
    protected lateinit var cryptoReceivedComboBox: ComboBox<Ticker>

    @FXML
    protected lateinit var descriptionField: TextField

    @FXML
    protected lateinit var cryptoSoldQuantityField: TextField

    @FXML
    protected lateinit var cryptoReceivedQuantityField: TextField

    @FXML
    protected lateinit var exchangeDatePicker: DatePicker

    protected lateinit var springContext: ConfigurableApplicationContext
    protected var cryptos: List<Ticker> = emptyList()
    protected lateinit var fromCrypto: Ticker

    fun setFromCryptoComboBox(tk: Ticker) {
        if (cryptos.none { it.id!! == tk.id!! }) {
            return
        }

        fromCrypto = tk
        cryptoSoldComboBox.value = fromCrypto
        updateFromCryptoCurrentQuantity()
    }

    @FXML
    protected open fun initialize() {
        configureComboBoxes()

        loadCryptosFromDatabase()
        populateCryptoComboBoxes()

        UIUtils.setDatePickerFormat(exchangeDatePicker)

        UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel)
        UIUtils.resetLabel(cryptoReceivedAfterBalanceValueLabel)
        UIUtils.resetLabel(cryptoSoldCurrentBalanceValueLabel)
        UIUtils.resetLabel(cryptoReceivedCurrentBalanceValueLabel)

        cryptoSoldComboBox.setOnAction {
            updateFromCryptoCurrentQuantity()
            updateFromCryptoQuantityAfterExchange()
        }

        cryptoReceivedComboBox.setOnAction {
            updateToCryptoCurrentQuantity()
            updateToCryptoQuantityAfterExchange()
        }

        configureListeners()
    }

    @FXML
    protected abstract fun handleSave()

    @FXML
    protected open fun handleCancel() {
        (cryptoReceivedQuantityField.scene.window as Stage).close()
    }

    @FXML
    protected fun handleCryptoSoldOpenCalculator() {
        WindowUtils.openPopupWindow(
            Files.CALCULATOR_FXML,
            preferencesService.translate(TranslationKeys.INVESTMENT_LABEL_CALCULATOR),
            springContext,
            { _: CalculatorController -> },
            listOf(Runnable { calculatorService.updateComponentWithResult(cryptoSoldQuantityField) }),
        )
    }

    @FXML
    protected fun handleCryptoReceivedOpenCalculator() {
        WindowUtils.openPopupWindow(
            Files.CALCULATOR_FXML,
            preferencesService.translate(TranslationKeys.INVESTMENT_LABEL_CALCULATOR),
            springContext,
            { _: CalculatorController -> },
            listOf(Runnable { calculatorService.updateComponentWithResult(cryptoReceivedQuantityField) }),
        )
    }

    protected fun updateFromCryptoCurrentQuantity() {
        val cryptoSold = cryptoSoldComboBox.value ?: return

        if (cryptoSold.currentQuantity < BigDecimal.ZERO) {
            UIUtils.setLabelStyle(cryptoSoldCurrentBalanceValueLabel, Styles.NEGATIVE_BALANCE_STYLE)
        } else {
            UIUtils.setLabelStyle(cryptoSoldCurrentBalanceValueLabel, Styles.NEUTRAL_BALANCE_STYLE)
        }

        cryptoSoldCurrentBalanceValueLabel.text = cryptoSold.currentQuantity.toString()
    }

    protected fun updateToCryptoCurrentQuantity() {
        val cryptoReceived = cryptoReceivedComboBox.value ?: return

        if (cryptoReceived.currentQuantity < BigDecimal.ZERO) {
            UIUtils.setLabelStyle(cryptoReceivedCurrentBalanceValueLabel, Styles.NEGATIVE_BALANCE_STYLE)
        } else {
            UIUtils.setLabelStyle(cryptoReceivedCurrentBalanceValueLabel, Styles.NEUTRAL_BALANCE_STYLE)
        }

        cryptoReceivedCurrentBalanceValueLabel.text = cryptoReceived.currentQuantity.toString()
    }

    protected fun updateFromCryptoQuantityAfterExchange() {
        val cryptoSoldQuantityStr = cryptoSoldQuantityField.text
        val cryptoSold = cryptoSoldComboBox.value

        if (cryptoSoldQuantityStr.isNullOrBlank() || cryptoSold == null) {
            UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel)
            return
        }

        runCatching {
            val exchangeQuantity = cryptoSoldQuantityStr.toBigDecimal()

            if (exchangeQuantity < BigDecimal.ZERO) {
                UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel)
                return
            }

            val cryptoSoldAfterBalance = cryptoSold.currentQuantity.subtract(exchangeQuantity)

            if (cryptoSoldAfterBalance < BigDecimal.ZERO) {
                UIUtils.setLabelStyle(cryptoSoldAfterBalanceValueLabel, Styles.NEGATIVE_BALANCE_STYLE)
            } else {
                UIUtils.setLabelStyle(cryptoSoldAfterBalanceValueLabel, Styles.NEUTRAL_BALANCE_STYLE)
            }

            cryptoSoldAfterBalanceValueLabel.text = cryptoSoldAfterBalance.toString()
        }.onFailure {
            UIUtils.resetLabel(cryptoSoldAfterBalanceValueLabel)
        }
    }

    protected fun updateToCryptoQuantityAfterExchange() {
        val cryptoReceivedQuantityStr = cryptoReceivedQuantityField.text
        val cryptoReceived = cryptoReceivedComboBox.value

        if (cryptoReceivedQuantityStr.isNullOrBlank() || cryptoReceived == null) {
            UIUtils.resetLabel(cryptoReceivedAfterBalanceValueLabel)
            return
        }

        runCatching {
            val exchangeQuantity = cryptoReceivedQuantityStr.toBigDecimal()

            if (exchangeQuantity < BigDecimal.ZERO) {
                UIUtils.resetLabel(cryptoReceivedAfterBalanceValueLabel)
                return
            }

            val cryptoReceivedAfterBalance = cryptoReceived.currentQuantity.add(exchangeQuantity)

            if (cryptoReceivedAfterBalance < BigDecimal.ZERO) {
                UIUtils.setLabelStyle(cryptoReceivedAfterBalanceValueLabel, Styles.NEGATIVE_BALANCE_STYLE)
            } else {
                UIUtils.setLabelStyle(cryptoReceivedAfterBalanceValueLabel, Styles.NEUTRAL_BALANCE_STYLE)
            }

            cryptoReceivedAfterBalanceValueLabel.text = cryptoReceivedAfterBalance.toString()
        }.onFailure {
            UIUtils.resetLabel(cryptoReceivedAfterBalanceValueLabel)
        }
    }

    protected fun loadCryptosFromDatabase() {
        cryptos = tickerService.getAllNonArchivedTickersByType(AssetType.CRYPTOCURRENCY)
    }

    protected fun populateCryptoComboBoxes() {
        cryptoSoldComboBox.items.setAll(cryptos)
        cryptoReceivedComboBox.items.setAll(cryptos)
    }

    protected fun configureComboBoxes() {
        UIUtils.configureComboBox(cryptoSoldComboBox, Ticker::name)
        UIUtils.configureComboBox(cryptoReceivedComboBox, Ticker::name)
    }

    protected fun configureListeners() {
        cryptoSoldQuantityField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.INVESTMENT_VALUE_REGEX))) {
                cryptoSoldQuantityField.text = oldValue
            } else {
                updateFromCryptoQuantityAfterExchange()
                updateToCryptoQuantityAfterExchange()
            }
        }

        cryptoReceivedQuantityField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.INVESTMENT_VALUE_REGEX))) {
                cryptoReceivedQuantityField.text = oldValue
            } else {
                updateFromCryptoQuantityAfterExchange()
                updateToCryptoQuantityAfterExchange()
            }
        }
    }
}
