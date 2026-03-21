/*
 * Filename: EditTickerPurchaseController.kt (original filename: EditTickerPurchaseController.java)
 * Created on: January 11, 2025
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
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
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
class EditTickerPurchaseController(
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
    private lateinit var purchase: TickerPurchase

    init {
        walletTransactionType = WalletTransactionType.EXPENSE
    }

    fun setPurchase(p: TickerPurchase) {
        purchase = p
        tickerNameLabel.text = "${purchase.ticker.name} (${purchase.ticker.symbol})"
        unitPriceField.text = purchase.ticker.currentUnitValue.toString()

        setWalletComboBox(purchase.walletTransaction!!.wallet)

        descriptionField.text = purchase.walletTransaction!!.description
        unitPriceField.text = purchase.unitPrice.toString()
        quantityField.text = purchase.quantity.toString()
        statusComboBox.value = purchase.walletTransaction!!.status
        categoryComboBox.value = purchase.walletTransaction!!.category
        transactionDatePicker.value = purchase.walletTransaction!!.date.toLocalDate()
        includeInAnalysisCheckBox.isSelected = purchase.walletTransaction!!.includeInAnalysis

        totalPriceLabel.text = UIUtils.formatCurrency(purchase.walletTransaction!!.amount)
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

            if (purchase.walletTransaction!!.wallet.id == wallet.id &&
                purchase.walletTransaction!!.description == description &&
                purchase.walletTransaction!!.status == status &&
                purchase.walletTransaction!!.category.id == category.id &&
                purchase.unitPrice.isEqual(unitPrice) &&
                purchase.quantity.isEqual(quantity) &&
                purchase.walletTransaction!!.date.toLocalDate() == buyDate &&
                purchase.walletTransaction!!.includeInAnalysis == includeInAnalysisCheckBox.isSelected
            ) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_CHANGES_TITLE),
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_NO_CHANGES_PURCHASE_MESSAGE),
                )
            } else {
                val currentTime = LocalTime.now()
                val dateTimeWithCurrentHour = buyDate.atTime(currentTime)

                purchase.walletTransaction!!.wallet = wallet
                purchase.walletTransaction!!.description = description
                purchase.walletTransaction!!.status = status
                purchase.walletTransaction!!.category = category
                purchase.unitPrice = unitPrice
                purchase.quantity = quantity
                purchase.walletTransaction!!.date = dateTimeWithCurrentHour
                purchase.walletTransaction!!.includeInAnalysis = includeInAnalysisCheckBox.isSelected

                tickerService.updateTickerPurchase(purchase)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_PURCHASE_UPDATED_TITLE),
                    preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_PURCHASE_UPDATED_MESSAGE),
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
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_ERROR_UPDATING_PURCHASE_TITLE),
                        e.message ?: "",
                    )
                }
                else -> throw e
            }
        }
    }
}
