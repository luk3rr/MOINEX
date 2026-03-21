/*
 * Filename: EditTickerSaleController.kt (original filename: EditTickerSaleController.java)
 * Created on: January 11, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.update

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.stage.Stage
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.extension.isEqual
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
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
class EditTickerSaleController(
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
    private lateinit var sale: TickerSale

    init {
        walletTransactionType = WalletTransactionType.INCOME
    }

    fun setSale(s: TickerSale) {
        sale = s
        tickerNameLabel.text = "${sale.ticker.name} (${sale.ticker.symbol})"
        unitPriceField.text = sale.ticker.currentUnitValue.toString()

        setWalletComboBox(sale.walletTransaction!!.wallet)

        descriptionField.text = sale.walletTransaction!!.description
        unitPriceField.text = sale.unitPrice.toString()
        quantityField.text = sale.quantity.toString()
        statusComboBox.value = sale.walletTransaction!!.status
        categoryComboBox.value = sale.walletTransaction!!.category
        transactionDatePicker.value = sale.walletTransaction!!.date.toLocalDate()
        includeInAnalysisCheckBox.isSelected = sale.walletTransaction!!.includeInAnalysis

        totalPriceLabel.text = UIUtils.formatCurrency(sale.walletTransaction!!.amount)
    }

    @FXML
    override fun handleSave() {
        val wallet = walletComboBox.value
        val description = descriptionField.text
        val status = statusComboBox.value
        val category = categoryComboBox.value
        val unitPriceStr = unitPriceField.text
        val quantityStr = quantityField.text
        val saleDate = transactionDatePicker.value

        if (wallet == null ||
            description.isNullOrBlank() ||
            status == null ||
            category == null ||
            unitPriceStr.isNullOrBlank() ||
            quantityStr.isNullOrBlank() ||
            saleDate == null
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

            if (sale.walletTransaction!!.wallet.id == wallet.id &&
                sale.walletTransaction!!.description == description &&
                sale.walletTransaction!!.status == status &&
                sale.walletTransaction!!.category.id == category.id &&
                sale.unitPrice.isEqual(unitPrice) &&
                sale.quantity.isEqual(quantity) &&
                sale.walletTransaction!!.date.toLocalDate() == saleDate &&
                sale.walletTransaction!!.includeInAnalysis == includeInAnalysisCheckBox.isSelected
            ) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_CHANGES_TITLE),
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_CHANGES_SALE_MESSAGE),
                )
            } else {
                val currentTime = LocalTime.now()
                val dateTimeWithCurrentHour = saleDate.atTime(currentTime)

                sale.walletTransaction!!.wallet = wallet
                sale.walletTransaction!!.description = description
                sale.walletTransaction!!.status = status
                sale.walletTransaction!!.category = category
                sale.unitPrice = unitPrice
                sale.quantity = quantity
                sale.walletTransaction!!.date = dateTimeWithCurrentHour
                sale.walletTransaction!!.includeInAnalysis = includeInAnalysisCheckBox.isSelected

                tickerService.updateSale(sale)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_SALE_UPDATED_TITLE),
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_SALE_UPDATED_MESSAGE),
                )
            }

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
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_ERROR_UPDATING_SALE_TITLE),
                        e.message ?: "",
                    )
                }
                else -> throw e
            }
        }
    }
}
